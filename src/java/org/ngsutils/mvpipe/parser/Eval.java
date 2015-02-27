package org.ngsutils.mvpipe.parser;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ngsutils.mvpipe.exceptions.EvalException;
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
import org.ngsutils.mvpipe.parser.statement.Print;
import org.ngsutils.mvpipe.parser.statement.ElIf;
import org.ngsutils.mvpipe.parser.statement.Else;
import org.ngsutils.mvpipe.parser.statement.EndIf;
import org.ngsutils.mvpipe.parser.statement.ForLoop;
import org.ngsutils.mvpipe.parser.statement.If;
import org.ngsutils.mvpipe.parser.statement.Include;
import org.ngsutils.mvpipe.parser.statement.Statement;
import org.ngsutils.mvpipe.parser.statement.Unset;
import org.ngsutils.mvpipe.parser.variable.VarNull;
import org.ngsutils.mvpipe.parser.variable.VarRange;
import org.ngsutils.mvpipe.parser.variable.VarValue;
import org.ngsutils.mvpipe.support.StringUtils;

public class Eval {
	private static Log log = LogFactory.getLog(Eval.class);

	final public static Map<String, Operator> ops = new HashMap<String, Operator>();
	final public static List<String> opsOrder = new ArrayList<String>();
	final public static List<String> opsParseOrder = new ArrayList<String>();
	final public static Map<String, Statement> statements = new HashMap<String, Statement>();

	final private static Pattern varPattern = Pattern.compile("^(.*?)\\$\\{([A-Za-z_\\.][a-zA-Z0-9_\\.]*?\\??)\\}(.*?)$");
	final private static Pattern listPattern = Pattern.compile("^(.*?)([^ \t]*)@\\{([A-Za-z_\\.][a-zA-Z0-9_\\.]*?\\??)\\}([^ \t]*)(.*?)$");
	final private static Pattern rangePattern = Pattern.compile("^(.*?)([^ \t]*)@\\{([a-zA-Z0-9_\\.]*?)\\.\\.([a-zA-Z0-9_\\.]*?)\\}([^ \t]*)(.*?)$");

	final private static Pattern outputPattern = Pattern.compile("^(.*?)\\$>([0-9]*)(.*?)$");
	final private static Pattern inputPattern = Pattern.compile("^(.*?)\\$<([0-9]*)(.*?)$");
	final private static Pattern wildPattern = Pattern.compile("^(.*?)\\$%(.*?)$");

	private static void addOp(String op, Operator obj) {
		// Operations are added in priority
		opsOrder.add(op);
		ops.put(op, obj);
		
		// parsing from the ops list requires matching "==" before "="
		opsParseOrder.add(op);
		Collections.sort(opsParseOrder, Collections.reverseOrder());
	}
	
	static {
		statements.put("print", new Print());
		statements.put("unset", new Unset());
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
		addOp("==", new Eq());
		addOp("!=", new NotEq());
		addOp(">=", new Gte());
		addOp("<=", new Lte());
		addOp(">", new Gt());
		addOp("<", new Lt());
		addOp("&&", new And());
		addOp("||", new Or());
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
		if (tokens.size() == 0) {
			return context;
		}
		
		log.trace("#evalTokens: " + StringUtils.join(", ", tokens.getList()));
		if (statements.containsKey(tokens.get(0))) {
			log.trace("#statement: " + tokens.get(0));
			List<String> right = new ArrayList<String>();
			right.addAll(tokens.getList().subList(1, tokens.size()));
			
			// correct for starting with a neg number...
			if (right.size() > 0 && right.get(0).equals("-")) {
				right.remove(0);
				right.set(0, "-" + right.get(0));
			}
			
			return statements.get(tokens.get(0)).eval(context, new Tokens(right));
		} else if (context.isActive()) {
			VarValue ret = evalTokenExpression(context, tokens);
			if (print) {
				System.out.println(ret.toString());
			} else if (ret == VarNull.NULL) {
				// we only throw an error on NULL if we aren't in a print loop
				throw new SyntaxException("NULL expression: "+ StringUtils.join(" ", tokens.getList()));
			}
		}
		
		return context;
	}
	
	public static VarValue evalTokenExpression(ExecContext context, Tokens tokens) throws SyntaxException {
		log.trace("evalTokenExpression: " + StringUtils.join(", ", tokens.getList()));

		if (tokens.size() == 1) {
			VarValue ret = VarValue.parseString(tokens.get(0), context);
			log.trace("parsing: " + tokens.get(0)+" => "+ret);
			if (ret.equals(VarNull.NULL)) {
				for (String k: context.keys()) {
					log.trace("  "+k+" => "+context.getString(k));
				}
			}
			return ret;
		}

		// look for ranges...
		if (tokens.size() == 3) {
			if (tokens.get(1).equals("..")) {
				VarValue lval = evalTokenExpression(context, tokens.subList(0,1));
				VarValue rval = evalTokenExpression(context, tokens.subList(2,3));
				return ops.get("..").eval(context, lval, rval);
			}
		}

		// look for shells
		for (int i=0; i<tokens.size(); i++) {
			if (tokens.get(i).equals("$(") && tokens.get(i+2).equals(")")) {
				log.trace("shell command: "+tokens.get(i+1));
				// shell command

				try {
					Process proc = Runtime.getRuntime().exec(new String[] { context.contains("mvpipe.shell") ? context.getString("mvpipe.shell"): "/bin/sh", "-c" , evalString(tokens.get(i+1), context)});
					InputStream is = proc.getInputStream();
					InputStream es = proc.getErrorStream();

					int retcode = proc.waitFor();
					
					String out = StringUtils.slurp(is);
					String err = StringUtils.slurp(es);

					log.trace("retcode: "+retcode);
					log.trace("stdout: " + out);
					log.trace("stderr: " + err);
					
					is.close();
					es.close();
					
					if (retcode == 0) {
						VarValue ret = VarValue.parseString(StringUtils.rstrip(out), true);
						Tokens rem;
						if (i > 1) {
							rem = tokens.subList(0, i);
						} else {
							rem = new Tokens();
						}

						log.trace("left: "+StringUtils.join(",", rem.getList()));
						log.trace("right: "+StringUtils.join(",", tokens.subList(i+3,  tokens.size()).getList()));

						rem.add(ret);
						rem.add(tokens.subList(i+3, tokens.size()));
						
						return evalTokenExpression(context, rem);
					}
					throw new SyntaxException("Error processing shell command: "+tokens.get(1) +" - " + err);

				} catch (IOException | InterruptedException e) {
					throw new SyntaxException(e);
				}
			}
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
			VarValue innerVal = evalTokenExpression(context, new Tokens(inner));
			leftTokens.add(innerVal.toString());
			leftTokens.addAll(rightTokens);
			return evalTokenExpression(context, new Tokens(leftTokens));
		}
		
		// otherwise, evaluate operations (in priority order!)
		for (String op: opsOrder) {
			log.trace("op test: "+op);
			for (int i=0; i<tokens.size(); i++) {
				if (tokens.get(i).equals(op)) {
					log.trace("found: "+op+" ("+i+")");
					
					if (ops.get(op).evalLeft()) {
						log.trace("tokens: "+StringUtils.join(",", tokens.getList()));
						
						VarValue lval;
						if (i > 0) {
							log.trace("left: "+StringUtils.join(",", tokens.subList(i-1, i).getList()));
							lval = evalTokenExpression(context, tokens.subList(i-1, i));
						} else {
							lval = VarNull.NULL;
						}
						log.trace("right: "+StringUtils.join(",", tokens.subList(i+1, i+2).getList()));
						VarValue rval = evalTokenExpression(context, tokens.subList(i+1, i+2));

						log.trace("lval: "+lval);
						log.trace("rval: "+lval);

						VarValue ret = ops.get(op).eval(context,  lval,  rval);
						log.trace("ret: "+lval);
						
						Tokens rem;
						if (i > 1) {
							rem = tokens.subList(0, i-1);
						} else {
							rem = new Tokens();
						}

						log.trace("left: "+StringUtils.join(",", rem.getList()));
						log.trace("right: "+StringUtils.join(",", tokens.subList(i+2,  tokens.size()).getList()));

						rem.add(ret);
						rem.add(tokens.subList(i+2, tokens.size()));
						
						return evalTokenExpression(context, rem);
					} else {
						
						VarValue rval = evalTokenExpression(context, tokens.subList(i+1, tokens.size()));
						Tokens left;
						if (i == 0) {
							left = new Tokens();
						} else {
							left = tokens.subList(0, i);
						}
						return ops.get(op).eval(context, left, rval);

					}
				}
			}
		}
		
		throw new SyntaxException("Unknown syntax: "+ StringUtils.join(" ", tokens.getList()));
	}

	private static String evalStringWildcard(String msg, String wildcard) {
		String tmp = "";
		while (msg.length() > 0) {
			Matcher m = wildPattern.matcher(msg);
			if (m.matches()) {
				if (m.group(1).endsWith("\\")) {
					tmp += m.group(1).substring(0,m.group(1).length()-1);
					tmp += "$%";
					msg = m.group(2);
				} else {
					tmp += m.group(1) + wildcard;
					msg = m.group(2);
				}
			} else {
				tmp += msg;
				msg = "";
			}

		}
		
		log.trace("    wild => "+tmp + "("+wildcard+")");

		return tmp;
	}

	public static String evalString(String msg, ExecContext cxt) throws EvalException {
		return evalString(msg, cxt, null);
	}
	public static String evalString(String msg, ExecContext cxt, String wildcard) throws EvalException {
		log.trace("evalString: "+msg + " / "+ cxt.isActive());
		String tmp = "";
		
		if (cxt != null && cxt.isActive()) {
			while (msg.length() > 0) {
				Matcher m = varPattern.matcher(msg);
				if (m.matches()) {
					if (m.group(1).endsWith("\\")) {
						tmp += m.group(1).substring(0,m.group(1).length()-1);
						tmp += "${" + m.group(2)+"}";
						msg = m.group(3);
					} else {
						tmp += m.group(1);
						if (m.group(2).endsWith("?")) {
							String k = m.group(2).substring(0, m.group(2).length()-1);
							if (cxt.contains(k)) {
								tmp += cxt.get(k);
							}
						} else {
							if (cxt.contains(m.group(2))) {
								tmp += cxt.get(m.group(2));
							} else {
								throw new EvalException("Missing variable: "+m.group(2));
							}
						}
						msg = m.group(3);
					}
				} else {
					tmp += msg;
					msg = "";
				}
			}
	
			log.trace("var => "+tmp);
			
			msg = tmp;
			tmp = "";
			
			while (msg.length() > 0) {
				Matcher m = listPattern.matcher(msg);
				if (m.matches()) {
					log.trace("Match 1: \""+ m.group(1)+"\"");
					log.trace("Match 2: \""+ m.group(2)+"\"");
					log.trace("Match 3: \""+ m.group(3)+"\"");
					log.trace("Match 4: \""+ m.group(4)+"\"");
					log.trace("Match 5: \""+ m.group(5)+"\"");
					tmp += m.group(1);
					
					if (m.group(2).endsWith("\\")) {
						tmp += m.group(2).substring(0,m.group(2).length()-1);
						tmp += "@{" + m.group(3) + "}";
						msg = m.group(4) + m.group(5);
					} else {
						boolean first = true;
						VarValue iter = null;
						if (m.group(3).endsWith("?")) {
							String k = m.group(3).substring(0, m.group(3).length()-1);
							if (cxt.contains(k)) {
								iter = cxt.get(k);
							}
						} else {
							if (!cxt.contains(m.group(3))) {
								throw new EvalException("Missing variable: "+m.group(3));
							}

							iter = cxt.get(m.group(3));
						}

						for (VarValue v: iter.iterate()) {
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
			log.trace("list => "+tmp);
	
			msg = tmp;
			tmp = "";
			

			// range check
			while (msg.length() > 0) {
				Matcher m = rangePattern.matcher(msg);
				if (m.matches()) {
					log.trace("Match 1: \""+ m.group(1)+"\"");
					log.trace("Match 2: \""+ m.group(2)+"\"");
					log.trace("Match 3: \""+ m.group(3)+"\"");
					log.trace("Match 4: \""+ m.group(4)+"\"");
					log.trace("Match 5: \""+ m.group(5)+"\"");
					log.trace("Match 6: \""+ m.group(6)+"\"");
					tmp += m.group(1);
					
					if (m.group(2).endsWith("\\")) {
						tmp += m.group(2).substring(0,m.group(2).length()-1);
						tmp += "@{" + m.group(3) +".." + m.group(4) + "}";
						msg = m.group(5) + m.group(6);
					} else {
						boolean first = true;
						
						
						VarValue range;
						try {
							List<String> l1 = new ArrayList<String> ();
							l1.add(m.group(3));
							VarValue from = evalTokenExpression(cxt, new Tokens(l1));
							List<String> l2 = new ArrayList<String> ();
							l2.add(m.group(4));
							VarValue to = evalTokenExpression(cxt, new Tokens(l2));

							range = VarRange.range(from, to);
						} catch (SyntaxException e) {
							throw new EvalException(e);
						}

						for (VarValue v: range.iterate()) {
							if (first) {
								first = false;
							} else {
								tmp += " ";
							}
							
							tmp += m.group(2);
							tmp += v;
							tmp += m.group(5);
						}							

						msg = m.group(6);
					}
					
				} else {
					tmp += msg;
					msg = "";
				}
			}
			log.trace("list => "+tmp);
	
			msg = tmp;
			tmp = "";
		}
		
		if (cxt.getMatchedOutputs() != null) {
			while (msg.length() > 0) {
				Matcher m = outputPattern.matcher(msg);
				if (m.matches()) {
					for (int i=1; i<=m.groupCount(); i++) {
						log.trace("m.group("+i+") => "+m.group(i));
					}
					if (m.group(1).endsWith("\\")) {
						tmp += m.group(1).substring(0,m.group(1).length()-1);
						tmp += "$>" + m.group(2);
						msg = m.group(3);
					} else {
						tmp += m.group(1);
						if (m.group(2).equals("")) {
							tmp += StringUtils.join(" ", cxt.getMatchedOutputs());
						} else {
							tmp += cxt.getMatchedOutputs().get(Integer.parseInt(m.group(2))-1);
						}
						msg = m.group(3);
					}
				} else {
					tmp += msg;
					msg = "";
				}
			}
	
			log.trace("outputs => "+tmp);

			msg = tmp;
			tmp = "";
		}

		if (cxt.getMatchedInputs() != null) {
			while (msg.length() > 0) {
				Matcher m = inputPattern.matcher(msg);
				if (m.matches()) {
					if (m.group(1).endsWith("\\")) {
						tmp += m.group(1).substring(0,m.group(1).length()-1);
						tmp += "$<" + m.group(2);
						msg = m.group(3);
					} else {
						tmp += m.group(1);
						if (m.group(2).equals("")) {
							tmp += StringUtils.join(" ", cxt.getMatchedInputs());
						} else {
							tmp += cxt.getMatchedInputs().get(Integer.parseInt(m.group(2))-1);
						}
						msg = m.group(3);
					}
				} else {
					tmp += msg;
					msg = "";
				}
			}
	
			log.trace("inputs => "+tmp);

			msg = tmp;
			tmp = "";
		}
		
		if (wildcard!=null) {
			msg = evalStringWildcard(msg, wildcard);
		} else if (cxt.getWildcard()!=null) {
			msg = evalStringWildcard(msg, cxt.getWildcard());
		}
		
		return msg;
	}
}
