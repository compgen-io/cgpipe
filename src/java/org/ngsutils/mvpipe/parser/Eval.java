package org.ngsutils.mvpipe.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ngsutils.mvpipe.exceptions.SyntaxException;
import org.ngsutils.mvpipe.parser.context.ExecContext;
import org.ngsutils.mvpipe.parser.op.Add;
import org.ngsutils.mvpipe.parser.op.AddAssign;
import org.ngsutils.mvpipe.parser.op.And;
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
import org.ngsutils.mvpipe.parser.op.Or;
import org.ngsutils.mvpipe.parser.op.Pow;
import org.ngsutils.mvpipe.parser.op.Range;
import org.ngsutils.mvpipe.parser.op.Rem;
import org.ngsutils.mvpipe.parser.op.Sub;
import org.ngsutils.mvpipe.parser.statement.DumpVars;
import org.ngsutils.mvpipe.parser.statement.Echo;
import org.ngsutils.mvpipe.parser.statement.ElIf;
import org.ngsutils.mvpipe.parser.statement.Else;
import org.ngsutils.mvpipe.parser.statement.EndIf;
import org.ngsutils.mvpipe.parser.statement.ForLoop;
import org.ngsutils.mvpipe.parser.statement.If;
import org.ngsutils.mvpipe.parser.statement.Include;
import org.ngsutils.mvpipe.parser.statement.Statement;
import org.ngsutils.mvpipe.parser.variable.VarNull;
import org.ngsutils.mvpipe.parser.variable.VarValue;
import org.ngsutils.mvpipe.support.StringUtils;

public class Eval {
	final public static Map<String, Operator> ops = new HashMap<String, Operator>();
	final public static List<String> opsOrder = new ArrayList<String>();
	final public static List<String> opsParseOrder = new ArrayList<String>();
	final public static Map<String, Statement> statements = new HashMap<String, Statement>();

	final private static Pattern varPattern = Pattern.compile("^(.*?)\\$\\{([A-Za-z_\\.][a-zA-Z0-9_\\.]*?)\\}(.*?)$");
	final private static Pattern listPattern = Pattern.compile("^(.*?)([^ \t]*)@\\{([A-Za-z_\\.][a-zA-Z0-9_\\.]*?)\\}([^ \t]*)(.*?)$");
	
	private static void addOp(String op, Operator obj) {
		// Operations are added in priority
		opsOrder.add(op);
		ops.put(op, obj);
		
		// parsing from the ops list requires matching "==" before "="
		opsParseOrder.add(op);
		Collections.sort(opsParseOrder, Collections.reverseOrder());
	}
	
	static {
		statements.put("echo", new Echo());
		statements.put("dump", new DumpVars());

		statements.put("if", new If());
		statements.put("else", new Else());
		statements.put("elif", new ElIf());
		statements.put("endif", new EndIf());

		statements.put("for", new ForLoop());

		statements.put("include", new Include());

		addOp("+=", new AddAssign());
		addOp("?=", new CondAssign());
		addOp("=", new Assign());

		addOp("!", new Not());
		addOp("&&", new And());
		addOp("||", new Or());
		addOp("==", new Eq());
		addOp("!=", new NotEq());
		addOp(">=", new Gte());
		addOp("<=", new Lte());
		addOp(">", new Gt());
		addOp("<", new Lt());
		addOp("**", new Pow());
		addOp("*", new Mul());
		addOp("/", new Div());
		addOp("%", new Rem());
		addOp("+", new Add());
		addOp("-", new Sub());
		addOp("..", new Range());
		
		addOp(":", null); // placeholder to allow for tokenizing build targets on ':'
	}

	public static ExecContext evalTokenLine(ExecContext context, Tokens tokens) throws SyntaxException {
		return evalTokenLine(context, tokens, false);
	}
	public static ExecContext evalTokenLine(ExecContext context, Tokens tokens, boolean print) throws SyntaxException {
		System.err.println("#evalTokens: " + StringUtils.join(", ", tokens.getList()));
		if (statements.containsKey(tokens.get(0))) {
			System.err.println("#statement: " + tokens.get(0));
			List<String> right = new ArrayList<String>();
			right.addAll(tokens.getList().subList(1, tokens.size()));
			
			// correct for starting with a neg number...
			if (right.size() > 0 && right.get(0).equals("-")) {
				right.remove(0);
				right.set(0, "-" + right.get(0));
			}
			
			return statements.get(tokens.get(0)).eval(context, tokens.clone(right));
		} else if (context.isActive()) {
			VarValue ret = evalTokenExpression(context, tokens);
			if (print) {
				System.err.println(ret.toString());
			} else if (ret == VarNull.NULL) {
				// we only throw an error on NULL if we aren't in a print loop
				throw new SyntaxException("NULL expression: "+ StringUtils.join(" ", tokens.getList()));
			}
		}
		
		return context;
	}
	
	public static VarValue evalTokenExpression(ExecContext context, Tokens tokens) throws SyntaxException {
		System.err.println("#evalTokenExpression: " + StringUtils.join(", ", tokens.getList()));

		if (tokens.size() == 1) {
			return VarValue.parseString(tokens.get(0), context);
		}

		List<String> leftTokens = new ArrayList<String>();
		List<String> inner = new ArrayList<String>();
		List<String> rightTokens = new ArrayList<String>();

		int parenCount = 0;
		boolean done = false;
		boolean found = false;
		
		// Look for parens...
		for (String token: tokens.getList()) {
			if (done) {
				rightTokens.add(token);
			} else if (token.equals("(")) {
				if (parenCount > 0) {
					inner.add(token);
				}

				parenCount++;
				found = true;
			} else if (token.equals(")")) {
				parenCount--;

				if (parenCount == 0) {
					done=true;
				} else {
					inner.add(token);
				}
			} else if (parenCount > 0) {
				inner.add(token);
			} else {
				leftTokens.add(token);
			}
		}
		
		if (found) {
			VarValue innerVal = evalTokenExpression(context, tokens.clone(inner));
			leftTokens.add(innerVal.toString());
			leftTokens.addAll(rightTokens);
			return evalTokenExpression(context, tokens.clone(leftTokens));
		}
		
		for (String op: opsOrder) {
			for (int i=0; i<tokens.size(); i++) {
				if (tokens.get(i).equals(op)) {
					Tokens left;
					if (i == 0) {
						left = tokens.clone(new ArrayList<String>());
					} else {
						left = tokens.subList(0, i);
					}
					Tokens right;
					if (i < tokens.size() - 1) {
						right = tokens.subList(i+1, tokens.size());
					} else {
						right = tokens.clone(new ArrayList<String>());
					}

					VarValue rval = evalTokenExpression(context, right);
					VarValue ret;
					VarValue lval = null;
					if (ops.get(op).evalLeft()) {
						if (left.size() > 0) {
							lval = evalTokenExpression(context, left);
						}
						ret = ops.get(op).eval(context, lval, rval);
					} else {
						ret = ops.get(op).eval(context, left, rval);
					} 

					return ret;
				}
			}
		}
		
		throw new SyntaxException("Unknown syntax: "+ StringUtils.join(" ", tokens.getList()));
	}

	public static String evalString(String msg, ExecContext cxt) {
		System.err.println("#evalString: "+msg);
		String tmp = "";
		
		while (msg.length() > 0) {
			Matcher m = varPattern.matcher(msg);
			m = varPattern.matcher(msg);
			if (m.matches()) {
				if (m.group(1).endsWith("\\")) {
					tmp += m.group(1).substring(0,m.group(1).length()-1);
					tmp += "$";
					msg = m.group(2).substring(1) + m.group(3);
				} else {
					tmp += m.group(1);
					tmp += cxt.get(m.group(2));
					msg = m.group(3);
				}
			} else {
				tmp += msg;
				msg = "";
			}
		}
		
		msg = tmp;
		tmp = "";
		
		while (msg.length() > 0) {
			Matcher m = listPattern.matcher(msg);
			m = listPattern.matcher(msg);
			if (m.matches()) {
				System.err.println("# Match 1: \""+ m.group(1)+"\"");
				System.err.println("# Match 2: \""+ m.group(2)+"\"");
				System.err.println("# Match 3: \""+ m.group(3)+"\"");
				System.err.println("# Match 4: \""+ m.group(4)+"\"");
				System.err.println("# Match 5: \""+ m.group(5)+"\"");
				tmp += m.group(1);
				
				if (m.group(2).endsWith("\\")) {
					tmp += m.group(2).substring(0,m.group(2).length()-1);
					tmp += "@";
					msg = m.group(3).substring(1) + m.group(4) + m.group(5);
				} else {
					boolean first = true;
					for (VarValue v: cxt.get(m.group(3)).iterate()) {
						if (first) {
							first = false;
						} else {
							tmp += " ";
						}
						
						tmp += m.group(2);
						tmp += v;
						tmp += m.group(4);
					}
					
					msg = m.group(5);
				}
				
			} else {
				tmp += msg;
				msg = "";
			}
		}
		
		System.err.println("#         => "+tmp);
		return tmp;
	}

}
