package org.ngsutils.mvpipe.parser;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ngsutils.mvpipe.parser.context.BuildTarget;
import org.ngsutils.mvpipe.parser.context.ExecContext;
import org.ngsutils.mvpipe.parser.op.Add;
import org.ngsutils.mvpipe.parser.op.Assign;
import org.ngsutils.mvpipe.parser.op.CondAssign;
import org.ngsutils.mvpipe.parser.op.Div;
import org.ngsutils.mvpipe.parser.op.Eq;
import org.ngsutils.mvpipe.parser.op.Gt;
import org.ngsutils.mvpipe.parser.op.Gte;
import org.ngsutils.mvpipe.parser.op.Lt;
import org.ngsutils.mvpipe.parser.op.Lte;
import org.ngsutils.mvpipe.parser.op.Mul;
import org.ngsutils.mvpipe.parser.op.Not;
import org.ngsutils.mvpipe.parser.op.NotEq;
import org.ngsutils.mvpipe.parser.op.Operator;
import org.ngsutils.mvpipe.parser.op.Pow;
import org.ngsutils.mvpipe.parser.op.Rem;
import org.ngsutils.mvpipe.parser.op.Sub;
import org.ngsutils.mvpipe.parser.statement.DumpVars;
import org.ngsutils.mvpipe.parser.statement.Echo;
import org.ngsutils.mvpipe.parser.statement.ElIf;
import org.ngsutils.mvpipe.parser.statement.Else;
import org.ngsutils.mvpipe.parser.statement.EndIf;
import org.ngsutils.mvpipe.parser.statement.If;
import org.ngsutils.mvpipe.parser.statement.Statement;
import org.ngsutils.mvpipe.parser.variable.VarValue;
import org.ngsutils.mvpipe.support.StringUtils;

public class Parser {
	final public boolean verbose;
	final public boolean dryrun;

	final private List<BuildTarget> targets = new ArrayList<BuildTarget>();

	final private static Map<String, Statement> statements = new HashMap<String, Statement>();
	final private static Map<String, Operator> ops = new HashMap<String, Operator>();
	
	
	static {
		statements.put("echo", new Echo());
		statements.put("dump", new DumpVars());

		statements.put("if", new If());
		statements.put("else", new Else());
		statements.put("elif", new ElIf());
		statements.put("endif", new EndIf());

		ops.put("!", new Not());
		ops.put("=", new Assign());
		ops.put("?=", new CondAssign());
		ops.put("+", new Add());
		ops.put("-", new Sub());
		ops.put("*", new Mul());
		ops.put("/", new Div());
		ops.put("%", new Rem());
		ops.put("**", new Pow());
		ops.put("==", new Eq());
		ops.put("!=", new NotEq());
		ops.put(">", new Gt());
		ops.put(">=", new Gte());
		ops.put("<", new Lt());
		ops.put("<=", new Lte());
	}
	
	private ExecContext currentContext;
	private BuildTarget curTarget = null;

	public ExecContext getContext() {
		return currentContext;
	}
	
	public Parser(ExecContext context, boolean verbose, boolean dryrun) {
		this.currentContext = context;
		this.verbose = verbose;
		this.dryrun = dryrun;
	}

	public void parse(String filename) throws IOException, SyntaxException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
		String priorLine=null;
		String line;
		int linenum = 0;
		while ((line = reader.readLine()) != null) {
			line = StringUtils.rstrip(line);

			linenum += 1;
			
			if (priorLine != null) {
				line = priorLine + line;
			}

			if (line.endsWith("\\")) {
				priorLine = line.substring(0, line.length()-1);
				continue;
			}
			
			if (curTarget != null) {
				if (StringUtils.strip(line).length() > 0) { // not blank
					if (line.charAt(0) != ' ' && line.charAt(0) != '\t') { // not whitespace at the first char
						// we aren't indented... and have something... must be at the end of the target
						curTarget = null;
					} else {
						curTarget.addLine(StringUtils.strip(line));
						continue;
					}
				}
			}
			
			// First, do string replacement (this is a simple language)
//			line = currentContext.evalString(line);
			
			// Next tokenize the line and attempt to execute it
			List<String> tokens = Tokenizer.tokenize(line);
			if (verbose) {
				System.err.println("#"+StringUtils.join(", ", tokens));
			}
			
			if (tokens.size() > 0) {
				// check for a new target (out1 out2 : in1 in2)
				List<String> pre = new ArrayList<String>();
				List<String> post = new ArrayList<String>();
				boolean istarget = false;
				for (String tok: tokens) {
					if (tok.equals(":")) {
						istarget = true;
					} else if (istarget) {
						post.add(tok);
					} else {
						pre.add(tok);
					}
				}
				if (istarget) {
					curTarget = new BuildTarget(pre, post, currentContext);
					targets.add(curTarget);
					continue;
				}
				
				
				try {
					if (statements.containsKey(tokens.get(0))) {
						List<String> right = tokens.subList(1, tokens.size());
						
						// correct for starting with a neg number...
						if (right.size() > 0 && right.get(0).equals("-")) {
							right.remove(0);
							right.set(0, "-" + right.get(0));
						}
						statements.get(tokens.get(0)).eval(this, right);
					} else if (currentContext.isActive()) {
						VarValue val = evalTokens(tokens);
						if (verbose) {
							System.err.println("#>>> "+line);
							System.err.println("#" + val + " ("+val.getClass().getSimpleName()+")");
						}
					}
				} catch (SyntaxException e) {
					e.setErrorLine(filename, linenum);
					e.printStackTrace();
					reader.close();
					throw e;
				}
			}
		}
		
		reader.close();
	}

	public VarValue evalTokens(List<String> tokens) throws SyntaxException {
		if (verbose) {
			System.err.println("#EVAL TOKENS: "+StringUtils.join(", ", tokens));
		}

		if (tokens.get(0).equals("(")) {
			List<String> inner = new ArrayList<String>();
			List<String> outer = new ArrayList<String>();
			int count = 1;
			
			for (int i=1; i<tokens.size(); i++) {
				String s = tokens.get(i);
				if (count > 0) {
					if (s.equals("(")) {
						count++;
					} else if (s.equals(")")) {
						count--;
					}
					if (count > 0) {
						inner.add(s);
					}
				} else {
					outer.add(s);
				}
			}
			System.err.println("# INNER => " + StringUtils.join(",", inner));
			System.err.println("# OUTER => " + StringUtils.join(",", outer));
			VarValue innerVal = evalTokens(inner);
			outer.add(0, innerVal.toString());
			tokens = outer;
		}
		
		if (tokens.size() == 1) {
			return VarValue.parseString(tokens.get(0), getContext());
		}

		String op;
		String lval;
		List<String> right = null;
		if (ops.containsKey(tokens.get(0))) {
			lval = null;
			op = tokens.get(0);
			right = tokens.subList(1, tokens.size());
			System.err.println("#lval => \""+lval+"\"");
			System.err.println("#op => \""+op+"\"");
		} else if (ops.containsKey(tokens.get(1))) {
			lval = tokens.get(0);
			op = tokens.get(1);
			right = tokens.subList(2, tokens.size());
		} else {
			throw new SyntaxException("Unable to parse line!");
		}

		VarValue rval = evalTokens(right);
		VarValue ret = ops.get(op).eval(getContext(), lval, rval);
		System.err.println("# <<< "+ret);
		return ret;
	}
	
	public void build(String target) {
		
	}

	public void setContext(ExecContext cxt) {
		currentContext = cxt;	
	}
}
