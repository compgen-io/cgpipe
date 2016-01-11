package io.compgen.cgpipe.parser;

import io.compgen.cgpipe.exceptions.ASTExecException;
import io.compgen.cgpipe.exceptions.ASTParseException;
import io.compgen.cgpipe.exceptions.MethodCallException;
import io.compgen.cgpipe.exceptions.MethodNotFoundException;
import io.compgen.cgpipe.exceptions.VarTypeException;
import io.compgen.cgpipe.loader.NumberedLine;
import io.compgen.cgpipe.parser.context.ExecContext;
import io.compgen.cgpipe.parser.op.Operator;
import io.compgen.cgpipe.parser.tokens.Token;
import io.compgen.cgpipe.parser.tokens.TokenList;
import io.compgen.cgpipe.parser.tokens.Tokenizer;
import io.compgen.cgpipe.parser.variable.VarInt;
import io.compgen.cgpipe.parser.variable.VarList;
import io.compgen.cgpipe.parser.variable.VarString;
import io.compgen.cgpipe.parser.variable.VarValue;
import io.compgen.common.Pair;
import io.compgen.common.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Eval {
	final private static Pattern outputPattern = Pattern.compile("^(.*?)\\$>([0-9]*)(.*?)$");
	final private static Pattern inputPattern = Pattern.compile("^(.*?)\\$<([0-9]*)(.*?)$");
	private static Log log = LogFactory.getLog(Eval.class);

	public static VarValue evalTokenExpression(TokenList tokens, ExecContext context) throws ASTExecException {
		log.trace("TOKENS: " + tokens);
		if (tokens.size() == 0) {
			return null;
		}
		if (tokens.size() == 1) {
			if (tokens.get(0).isString()) {
				return new VarString(Eval.evalString(tokens.get(0).getStr(), context, tokens.getLine()));
			}
			if (tokens.get(0).isValue()) {
				return tokens.get(0).getValue();
			}
			if (tokens.get(0).isVariable()) {
				if (tokens.get(0).getStr().equals("cgpipe.current.filename")) {
					return new VarString(tokens.getLine().getPipeline().getName());
				} else if (tokens.get(0).getStr().equals("cgpipe.current.hash")) {
					return new VarString(tokens.getLine().getPipeline().getHashDigest());
				}
				
				return context.get(tokens.get(0).getStr());
			}
			if (tokens.get(0).isShell()) {
				return VarValue.parseStringRaw(evalShell(tokens.get(0).getStr(), context, tokens.getLine()));
			}
			throw new ASTExecException("Unknown token: "+tokens.get(0), tokens);
		}

		if (tokens.get(0).isSliceOpen() && tokens.get(tokens.size()-1).isSliceClose()) {
			List<VarValue> elements = new ArrayList<VarValue>();
			List<Token> inner = new ArrayList<Token>();
			for (Token tok: tokens.subList(1, tokens.size()-1)) {
				if (tok.isComma()) {
					elements.add(evalTokenExpression(new TokenList(inner, tokens.getLine()), context));
					inner.clear();
				} else {
					inner.add(tok);
				}
			}

			if (inner.size()>0) {
				elements.add(evalTokenExpression(new TokenList(inner, tokens.getLine()), context));
			}
			
			try {
				return new VarList(elements);
			} catch (VarTypeException e) {
				throw new ASTExecException(e);
			}
		}

		if ((tokens.get(0).isVariable() || tokens.get(0).isValue()) && tokens.get(1).isSliceOpen()) {
			List<Token> outer = new ArrayList<Token>();

			VarValue obj;
			if (tokens.get(0).isValue()) {
				obj = tokens.get(0).getValue();
			} else {
				obj = evalTokenExpression(tokens.subList(0, 1), context);
			}
			
			VarValue start = null;
			VarValue end = null;

			int colonIdx = -1;
			boolean inOuter = false;
			
			for (int i=1; i<tokens.size(); i++) {
				if (inOuter) {
					outer.add(tokens.get(i));
				} else if (tokens.get(i).isColon()) {
					start = evalTokenExpression(tokens.subList(2,i), context);
					colonIdx = i;
				} else if (tokens.get(i).isSliceClose()) {
					if (colonIdx == -1) {
						start = evalTokenExpression(tokens.subList(2,i), context);
					} else {
						end = evalTokenExpression(tokens.subList(colonIdx+1,i), context);
					}
					inOuter = true;
				}
			}
			
//			System.err.println("Slice: "+start+".."+end+ " ? "+(colonIdx > -1));
			
			try {
				VarValue val = obj.slice(start, end, colonIdx > -1);
				if (outer == null || outer.size() == 0) {
					return val;
				}
				outer.add(0, Token.value(val));
				return evalTokenExpression(new TokenList(outer, tokens.getLine()), context);
			} catch (Exception e) {
				throw new ASTExecException(e);
			}
		}
		
		// process logic with paren grouping
		List<Token> inner = new ArrayList<Token>();
		List<Token> left = new ArrayList<Token>();
		List<Token> right = new ArrayList<Token>();

		int parenCount = 0;
		boolean done = false;
		boolean found = false;
		
		// Look for parens...
		for (Token token: tokens) {
			if (done) {
				right.add(token);
			} else if (token.isParenOpen()) {
				if (parenCount > 0) {
					inner.add(token);
				}

				parenCount++;
				found = true;
			} else if (token.isParenClose()) {
				parenCount--;

				if (parenCount == 0) {
					done=true;
				} else {
					inner.add(token);
				}
			} else if (parenCount > 0) {
				inner.add(token);
			} else {
				left.add(token);
			}
		}
		
		if (found) {

			if (left.size() > 0 && left.get(left.size()-1).isVariable()) {
				// this is a method call

				List<VarValue> args = new ArrayList<VarValue>();
				int indentLevel = 0;
				List<Token> buf = new ArrayList<Token>();
				for (Token t:inner) {
					if (t.isParenOpen()) {
						indentLevel++;
						buf.add(t);
					} else if (t.isParenClose()) {
						indentLevel--;
						buf.add(t);
					} else if (t.isComma() && indentLevel == 0) {
						if (buf.size() > 0) {
							args.add(evalTokenExpression(new TokenList(buf, tokens.getLine()), context));
						}
						buf.clear();
					} else {
						buf.add(t);
					}
				}
				if (buf.size() > 0) {
					args.add(evalTokenExpression(new TokenList(buf, tokens.getLine()), context));
				}

				VarValue[] argv = args.toArray(new VarValue[args.size()]);
				Token methodToken = left.remove(left.size()-1);
				VarValue obj;
				String method;
				
				if (methodToken.getStr().charAt(0) == '.') {
					// this is a method on an existing value... calc that first.
//					System.err.println("left tokens: " + StringUtils.join(",",left));
//					System.err.println("method     : " + methodToken.getStr());

					if (left.get(left.size()-1).isValue()) {
						Token t = left.remove(left.size()-1);
						obj = t.getValue();
					} else if (left.get(left.size()-1).isParenClose()) {
						List<Token> methodBuf = new ArrayList<Token>();
						int level = 1;
						Token t = left.remove(left.size()-1);
						methodBuf.add(0, t);
						while (level > 0) {
							if (t.isParenClose()) {
								level++;
							} else if (t.isParenOpen()) {
								level--;
							}
							methodBuf.add(0, t);
						}
						t = left.remove(left.size()-1);
						methodBuf.add(0, t);
						
						obj = evalTokenExpression(new TokenList(methodBuf, tokens.getLine()), context);
					} else if (left.get(left.size()-1).isSliceClose()) {
						List<Token> methodBuf = new ArrayList<Token>();
						int level = 1;
						Token t = left.remove(left.size()-1);
						methodBuf.add(0, t);
						while (level > 0) {
							if (t.isSliceClose()) {
								level++;
							} else if (t.isSliceOpen()) {
								level--;
							}
							methodBuf.add(0, t);
						}
						t = left.remove(left.size()-1);
						methodBuf.add(0, t);
						
						obj = evalTokenExpression(new TokenList(methodBuf, tokens.getLine()), context);
					} else {
						throw new ASTExecException("Error trying to call method: "+methodToken);
					}
					
					//					Token t = left.remove(left.size()-1);
//					if (!t.isValue()) {
//					}
					
					log.trace("left: " + StringUtils.join(",",left));
//					left.add(Token.value(obj));
					
//					obj = t.getValue();
					method = methodToken.getStr().substring(1);
					
				} else {
					// this is a method on a variable... 
					String[] tmp = StringUtils.reverse(methodToken.getStr()).split("\\.");
					method = StringUtils.reverse(tmp[0]);
					String var = StringUtils.reverse(tmp[1]);
	
					obj = context.get(var);
				}
				
				log.trace("obj: " + obj + "/"+obj.getClass().getName());
				log.trace("method: " + method);
				log.trace("argv: [" + StringUtils.join(",",argv) +"]");
				log.trace("left: " + StringUtils.join(",",left));

				try {
					VarValue ret = obj.call(method, argv);
					left.add(Token.value(ret));
					left.addAll(right);
					log.trace("left: " + StringUtils.join(",",left));
					return evalTokenExpression(new TokenList(left, tokens.getLine()), context);
				} catch (MethodNotFoundException | MethodCallException e) {
					log.error(e);
					throw new ASTExecException(e);
				}
				
			} else {
				VarValue innerVal = evalTokenExpression(new TokenList(inner, tokens.getLine()), context);
				left.add(Token.value(innerVal));
				left.addAll(right);
				return evalTokenExpression(new TokenList(left, tokens.getLine()), context);
			}
		}

		left.clear();
		right.clear();
		
		// Find an operator...
		
		int maxOperatorPriority = -1;
		
		for (Token tok: tokens) {
			if (tok.isOperator() && tok.getOp().getPriority() > maxOperatorPriority) {
				maxOperatorPriority = tok.getOp().getPriority();
			}
		}
		
		
		Operator op = null;
		for (Token tok: tokens) {
			if (op == null) {
				if (tok.isOperator() && tok.getOp().getPriority() == maxOperatorPriority) {
					op = tok.getOp();
				} else {
					left.add(tok);
				}
			} else {
				right.add(tok);
			}
		}
 
		if (op == null) {
			throw new ASTExecException("Missing operator: " + tokens, tokens);
		}
		
		if (op.tokenLeft()) {
			if (left.size() != 1) {
				throw new ASTExecException("Too many tokens on line for operator: "+op.getSymbol()+ "("+left.size()+")");
			}
			return op.eval(context, left.get(0), evalTokenExpression(new TokenList(right, tokens.getLine()), context));
		} else {
			return op.eval(context, evalTokenExpression(new TokenList(left, tokens.getLine()), context), evalTokenExpression(new TokenList(right, tokens.getLine()), context));
		}
	
	}

	
	public static String evalString(String str, ExecContext context, NumberedLine line) throws ASTExecException {
		String tmp = "";
		log .trace("eval string (pre) : "+str);
		tmp = evalStringVar(str, context, line);
		log .trace("eval string (var) : "+str+" => "+tmp);
		tmp = evalStringList(tmp, context, line);
		log .trace("eval string (list) : "+str+" => "+tmp);
		tmp = evalStringShell(tmp, context, line);
		log .trace("eval string (shell) : "+str+" => "+tmp);
		tmp = evalStringInputs(tmp, context, line);
		log .trace("eval string (inputs) : "+str+" => "+tmp);
		tmp = evalStringOutputs(tmp, context, line);
		log .trace("eval string (post): "+str+" => "+tmp);
		return tmp;
	}

	private static String evalStringVar(String str, ExecContext context, NumberedLine line) throws ASTExecException {
		String out = "";
		String buf = "";
		
		boolean ineval = false;
		boolean inDoubleEval = false;
		
		for (int i=0; i<str.length(); i++) {
			if (inDoubleEval) {
				if (i < str.length()-1 && str.charAt(i) == '}' && str.charAt(i+1) == '}') {
					if (buf.endsWith("\\")) {
						buf = buf.substring(0, buf.length()-1) + "}}";
						i++;
					} else {
						inDoubleEval = false;
						boolean optional = false;
						if (buf.endsWith("\\?")) {
							buf = buf.substring(0, buf.length()-2) + "?";
						} else if (buf.endsWith("?")) {
							optional = true;
							buf = buf.substring(0, buf.length()-1);
						}
						try {
	 						TokenList tl = Tokenizer.tokenize(new NumberedLine(buf, line));
							VarValue val = Eval.evalTokenExpression(tl, context);
							for (String s: val.toString().split("\n")) {
								out += evalString(s, context, line) + "\n";
							}

						} catch (ASTExecException e) {
							if (!optional) {
								throw e;
							}
						} catch (ASTParseException e) {
							if (!optional) {
								throw new ASTExecException(e, line);
							}
						}
						
						buf = "";
						i++;

					}
				} else {
					buf += str.charAt(i);
				}
			} else if (ineval) {
				if (str.charAt(i) == '}') {
					if (buf.endsWith("\\")) {
						buf = buf.substring(0, buf.length()-1) + "}";
					} else {
						ineval = false;
						boolean optional = false;
						if (buf.endsWith("\\?")) {
							buf = buf.substring(0, buf.length()-2) + "?";
						} else if (buf.endsWith("?")) {
							optional = true;
							buf = buf.substring(0, buf.length()-1);
						}
						
						try {
	 						TokenList tl = Tokenizer.tokenize(new NumberedLine(buf, line));
							VarValue val = Eval.evalTokenExpression(tl, context);
							out += val.toString();
						} catch (ASTExecException e) {
							if (!optional) {
								throw e;
							}
						} catch (ASTParseException e) {
							if (!optional) {
								throw new ASTExecException(e, line);
							}
						}
						
						buf = "";
					}
				} else {
					buf += str.charAt(i);
				}
			} else if (i < str.length()-3 && str.substring(i, i+3).equals("${{")) {
				if (i==0 || str.charAt(i-1) != '\\') {
					inDoubleEval = true;
				} else {
					out = out.substring(0, out.length()-1) + "${{";
				}
				i+=2;
			} else if (i < str.length()-2 && str.substring(i, i+2).equals("${")) {
				if (i==0 || str.charAt(i-1) != '\\') {
					ineval = true;
				} else {
					out = out.substring(0, out.length()-1) + "${";
				}
				i++;
			} else {
				out += str.charAt(i);
			}
		}
		
		if (!buf.equals("")) {
			throw new ASTExecException("Missing closing '}' in string ("+buf+")", line);
		}
		
		return out;
	}
	private static String evalStringShell(String str, ExecContext context, NumberedLine line) throws ASTExecException {
		String out = "";
		String buf = "";
		
		boolean inshell = false;
		
		for (int i=0; i<str.length(); i++) {
			if (inshell) {
				if (str.charAt(i) == ')') {
					if (buf.endsWith("\\")) {
						buf = buf.substring(0, buf.length()-1) + ")";
					} else {
						inshell = false;
						out += evalShell(buf, context, line);
						buf = "";
					}
				} else {
					buf += str.charAt(i);
				}
			} else if (i < str.length()-2 && str.substring(i, i+2).equals("$(")) {
				if (i==0 || str.charAt(i-1) != '\\') {
					inshell = true;
				} else {
					out = out.substring(0, out.length()-1) + "$(";
				}
				i++;
			} else {
				out += str.charAt(i);
			}
		}
		
		if (!buf.equals("")) {
			throw new ASTExecException("Missing closing ')' in string", line);
		}
		
		return out;
	}

	private static List<String> innerEvalStringList(String str, ExecContext context, NumberedLine line) throws ASTExecException {
		List<String> out = new ArrayList<String>();
		String pre = "";
		String post = null;
		String buf = null;
		
		boolean ineval = false;
		
		for (int i=0; i<str.length(); i++) {
			if (ineval) {
				if (str.charAt(i) == '}') {
					if (buf.endsWith("\\")) {
						buf = buf.substring(0, buf.length()-1) + "}";
					} else {
						ineval = false;
						post = "";
					}
				} else {
					buf += str.charAt(i);
				}
			} else if (i < str.length()-2 && str.substring(i, i+2).equals("@{")) {
				if (i==0 || str.charAt(i-1) != '\\') {
					ineval = true;
					buf = "";
				} else {
					pre = pre.substring(0, pre.length()-1) + "@{";
				}
				i++;
			} else if (buf==null) {
				pre += str.charAt(i);
			} else {
				post += str.charAt(i);
			}
		}

		if (buf != null && post == null) {
			throw new ASTExecException("Missing closing '}' in string", line);
		}

		if (buf != null) {
			if (!buf.equals("")) {
				boolean optional = false;
				if (buf.endsWith("?")) {
					optional = true;
					buf = buf.substring(0, buf.length()-1);
				}
				try {
					TokenList tl = Tokenizer.tokenize(new NumberedLine(buf, line));
					VarValue val = Eval.evalTokenExpression(tl, context);
					for (VarValue v: val.iterate()) {
						out.add(pre+v.toString()+post);
					}
				} catch (ASTExecException e) {
					if (!optional) {
						throw e;
					}
				} catch (ASTParseException e) {
					if (!optional) {
						throw new ASTExecException(e, line);
					}
				}
			} else {
				out.add(pre+post);
			}
		} else {
			out.add(pre);
		}
		return out;
	}

	private static String evalStringList(String str, ExecContext context, NumberedLine line) throws ASTExecException {
		String out = "";
		String buf = "";
		
		for (int i=0; i<str.length(); i++) {
			if (str.charAt(i) == ' ' || str.charAt(i) == '\t' || str.charAt(i) == '\r' || str.charAt(i) == '\n') {
				if (!buf.equals("")) {
					out += StringUtils.join(" ", innerEvalStringList(buf, context, line));
					buf = "";
				}
				out += str.charAt(i);
			} else {
				buf += str.charAt(i);
			}
		}
		if (!buf.equals("")) {
			out += StringUtils.join(" ", innerEvalStringList(buf, context, line));
		}
		return out;
	}

	private static String evalStringOutputs(String str, ExecContext context, NumberedLine line) throws ASTExecException {
		if (context.getRoot().getOutputs() != null) {
			String tmp = "";
			while (str.length() > 0) {
				Matcher m = outputPattern.matcher(str);
				if (m.matches()) {
					if (m.group(1).endsWith("\\")) {
						tmp += m.group(1).substring(0,m.group(1).length()-1);
						tmp += "$>" + m.group(2);
						str = m.group(3);
					} else {
						tmp += m.group(1);
						if (m.group(2).equals("")) {
							tmp += StringUtils.join(" ", context.getRoot().getOutputs());
						} else {
							tmp += context.getRoot().getOutputs().get(Integer.parseInt(m.group(2))-1);
						}
						str = m.group(3);
					}
				} else {
					tmp += str;
					break;
				}
			}
			return tmp;
		} else {
			return str;
		}
	}
	private static String evalStringInputs(String str, ExecContext context, NumberedLine line) throws ASTExecException {
		if (context.getRoot().getInputs() != null) {
			String tmp = "";
			while (str.length() > 0) {
				Matcher m = inputPattern.matcher(str);
				if (m.matches()) {
					if (m.group(1).endsWith("\\")) {
						tmp += m.group(1).substring(0,m.group(1).length()-1);
						tmp += "$<" + m.group(2);
						str = m.group(3);
					} else {
						tmp += m.group(1);
						if (m.group(2).equals("")) {
							tmp += StringUtils.join(" ", context.getRoot().getInputs());
						} else {
							tmp += context.getRoot().getInputs().get(Integer.parseInt(m.group(2))-1);
						}
						str = m.group(3);
					}
				} else {
					tmp += str;
					break;
				}
			}
			return tmp;
		} else {
			return str;
		}
	}
	
	public static String evalShell(String str, ExecContext context, NumberedLine line) throws ASTExecException {
		log.trace("evalShell: "+str);
		Pair<String, Integer> ret = Eval.execScript(evalString(str, context, line));
		context.set("$?", new VarInt(ret.two));
		return ret.one;
	}

	private static Pair<String, Integer> execScript(String script) throws ASTExecException {
		try {
			log.trace("execScript: "+script);
			Process proc = Runtime.getRuntime().exec(new String[] { "/bin/sh", "-c" , script});
			InputStream is = proc.getInputStream();
//			InputStream es = proc.getErrorStream();

			int retcode = proc.waitFor();
			
			String out = StringUtils.readInputStream(is);
//			String err = StringUtils.readInputStream(es);

			is.close();
//			es.close();
			
			return new Pair<String, Integer>(StringUtils.rstrip(out), retcode);
//			if (retcode == 0) {
//			}
//			throw new ASTExecException("Error processing shell command: "+script+" - "+err);

		} catch (IOException | InterruptedException e) {
			throw new ASTExecException(e);
		}
	}
}
