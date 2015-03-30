package io.compgen.cgpipe.parser;

import io.compgen.cgpipe.exceptions.ASTExecException;
import io.compgen.cgpipe.exceptions.ASTParseException;
import io.compgen.cgpipe.exceptions.VarTypeException;
import io.compgen.cgpipe.parser.context.ExecContext;
import io.compgen.cgpipe.parser.op.Operator;
import io.compgen.cgpipe.parser.tokens.Token;
import io.compgen.cgpipe.parser.tokens.TokenList;
import io.compgen.cgpipe.parser.tokens.Tokenizer;
import io.compgen.cgpipe.parser.variable.VarList;
import io.compgen.cgpipe.parser.variable.VarRange;
import io.compgen.cgpipe.parser.variable.VarString;
import io.compgen.cgpipe.parser.variable.VarValue;
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
	final private static Pattern varPattern = Pattern.compile("^(.*?)\\$\\{([A-Za-z_\\.][a-zA-Z0-9_\\.]*?\\??)\\}(.*?)$");
	final private static Pattern shellPattern = Pattern.compile("^(.*?)\\$\\(([A-Za-z_\\.][a-zA-Z0-9_\\.]*?\\??)\\)(.*?)$");
	final private static Pattern listPattern = Pattern.compile("^(.*?)([^ \t]*)@\\{([A-Za-z_\\.][a-zA-Z0-9_\\.]*?\\??)\\}([^ \t]*)(.*?)$");
	final private static Pattern rangePattern = Pattern.compile("^(.*?)([^ \t]*)@\\{([a-zA-Z0-9_\\.]*?)\\.\\.([a-zA-Z0-9_\\.]*?)\\}([^ \t]*)(.*?)$");

	final private static Pattern outputPattern = Pattern.compile("^(.*?)\\$>([0-9]*)(.*?)$");
	final private static Pattern inputPattern = Pattern.compile("^(.*?)\\$<([0-9]*)(.*?)$");
	private static Log log = LogFactory.getLog(Eval.class);

	public static VarValue evalTokenExpression(TokenList tokens, ExecContext context) throws ASTExecException {
//		System.err.println("TOKENS: " + tokens);
		if (tokens.size() == 0) {
			return null;
		}
		if (tokens.size() == 1) {
			if (tokens.get(0).isString()) {
				return new VarString(Eval.evalString(tokens.get(0).getStr(), context, tokens));
			}
			if (tokens.get(0).isValue()) {
				return tokens.get(0).getValue();
			}
			if (tokens.get(0).isVariable()) {
				return context.get(tokens.get(0).getStr());
			}
			if (tokens.get(0).isShell()) {
				return VarValue.parseStringRaw(evalShell(tokens.get(0).getStr(), context, tokens));
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
			
			return new VarList(elements);
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
			} catch (VarTypeException e) {
				throw new ASTExecException(e);
			}
		}
		
		
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
			VarValue innerVal = evalTokenExpression(new TokenList(inner, tokens.getLine()), context);
			left.add(Token.value(innerVal));
			left.addAll(right);
			return evalTokenExpression(new TokenList(left, tokens.getLine()), context);
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

	
	public static String evalString(String str, ExecContext context, TokenList tokens) throws ASTExecException {
		String tmp = "";
		tmp = evalStringVar(str, context, tokens);
		tmp = evalStringShell(tmp, context, tokens);
		tmp = evalStringList(tmp, context, tokens);
		tmp = evalStringRange(tmp, context, tokens);
		tmp = evalStringInputs(tmp, context, tokens);
		tmp = evalStringOutputs(tmp, context, tokens);
		log .trace("eval string: "+str+" => "+tmp);
		return tmp;
	}

	private static String evalStringVar(String str, ExecContext context, TokenList tokens) throws ASTExecException {
		String tmp = "";
		while (str.length() > 0) {
			Matcher m = varPattern.matcher(str);
			if (m.matches()) {
				if (m.group(1).endsWith("\\")) {
					tmp += m.group(1).substring(0,m.group(1).length()-1);
					tmp += "${" + m.group(2)+"}";
					str = m.group(3);
				} else {
					tmp += m.group(1);
					if (m.group(2).endsWith("?")) {
						String k = m.group(2).substring(0, m.group(2).length()-1);
						if (context.contains(k)) {
							tmp += context.get(k);
						}
					} else {
						if (context.contains(m.group(2))) {
							tmp += context.get(m.group(2));
						} else {
							throw new ASTExecException("Missing variable: "+m.group(2), tokens);
						}
					}
					str = m.group(3);
				}
			} else {
				tmp += str;
				break;
			}
		}
		return tmp;
	}
	
	private static String evalStringShell(String str, ExecContext context, TokenList tokens) throws ASTExecException {
		String tmp = "";
		while (str.length() > 0) {
			Matcher m = shellPattern.matcher(str);
			if (m.matches()) {
				if (m.group(1).endsWith("\\")) {
					tmp += m.group(1).substring(0,m.group(1).length()-1);
					tmp += "$(" + m.group(2)+")";
					str = m.group(3);
				} else {
					tmp += m.group(1);
					tmp += Eval.execScript(m.group(2));
					str = m.group(3);
				}
			} else {
				tmp += str;
				break;
			}
		}
		return tmp;
	}
	

	
	private static String evalStringList(String str, ExecContext context, TokenList tokens) throws ASTExecException {
		String tmp = "";
			
		while (str.length() > 0) {
			Matcher m = listPattern.matcher(str);
			if (m.matches()) {
				tmp += m.group(1);
				
				if (m.group(2).endsWith("\\")) {
					tmp += m.group(2).substring(0,m.group(2).length()-1);
					tmp += "@{" + m.group(3) + "}";
					str = m.group(4) + m.group(5);
				} else {
					boolean first = true;
					VarValue iter = null;
					if (m.group(3).endsWith("?")) {
						String k = m.group(3).substring(0, m.group(3).length()-1);
						if (context.contains(k)) {
							iter = context.get(k);
						}
					} else {
						if (!context.contains(m.group(3))) {
							throw new ASTExecException("Missing variable: "+m.group(3), tokens);
						}

						iter = context.get(m.group(3));
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

					str = m.group(5);
				}
				
			} else {
				tmp += str;
				break;
			}
		}
		return tmp;
	}
	
	private static String evalStringRange(String str, ExecContext context, TokenList tokens) throws ASTExecException {
		String tmp = "";
		// range check
		while (str.length() > 0) {
			Matcher m = rangePattern.matcher(str);
			if (m.matches()) {
				tmp += m.group(1);
				
				if (m.group(2).endsWith("\\")) {
					tmp += m.group(2).substring(0,m.group(2).length()-1);
					tmp += "@{" + m.group(3) +".." + m.group(4) + "}";
					str = m.group(5) + m.group(6);
				} else {
					try {
						TokenList fromTokens = Tokenizer.tokenize(new NumberedLine(tokens.getLine().filename, tokens.getLine().linenum, m.group(3)));
						TokenList toTokens = Tokenizer.tokenize(new NumberedLine(tokens.getLine().filename, tokens.getLine().linenum, m.group(4)));
						
						VarValue from = Eval.evalTokenExpression(fromTokens, context);
						VarValue to = Eval.evalTokenExpression(toTokens, context);
						
						VarValue range = VarRange.range(from, to);

						boolean first = true;
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
					} catch (VarTypeException | ASTParseException e) {
						throw new ASTExecException(e, tokens);
					}
					str = m.group(6);
				}
				
			} else {
				tmp += str;
				break;
			}
		}
		return tmp;
	}

	private static String evalStringOutputs(String str, ExecContext context, TokenList tokens) throws ASTExecException {
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
	private static String evalStringInputs(String str, ExecContext context, TokenList tokens) throws ASTExecException {
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

	
	public static String evalShell(String str, ExecContext context, TokenList tokens) throws ASTExecException {
		return Eval.execScript(evalString(str, context, tokens));
	}

	private static String execScript(String script) throws ASTExecException {
		try {
			Process proc = Runtime.getRuntime().exec(new String[] { "/bin/sh", "-c" , script});
			InputStream is = proc.getInputStream();
			InputStream es = proc.getErrorStream();

			int retcode = proc.waitFor();
			
			String out = StringUtils.readInputStream(is);
			String err = StringUtils.readInputStream(es);

			is.close();
			es.close();
			
			if (retcode == 0) {
				return StringUtils.rstrip(out);
			}
			throw new ASTExecException("Error processing shell command: "+script+" - "+err);

		} catch (IOException | InterruptedException e) {
			throw new ASTExecException(e);
		}
	}
}
