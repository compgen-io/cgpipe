package io.compgen.cgpipe.parser.tokens;

import io.compgen.cgpipe.exceptions.ASTParseException;
import io.compgen.cgpipe.exceptions.VarTypeException;
import io.compgen.cgpipe.parser.op.Operator;
import io.compgen.cgpipe.parser.statement.Statement;
import io.compgen.cgpipe.parser.variable.VarFloat;
import io.compgen.cgpipe.parser.variable.VarInt;
import io.compgen.cgpipe.parser.variable.VarValue;
import io.compgen.cgpipe.pipeline.NumberedLine;
import io.compgen.common.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class Tokenizer {

	public static TokenList tokenize(NumberedLine line) throws ASTParseException {
		try {
			List<Token> tokens = extractQuotedStrings(line.getLine(), '"', '#');
//			System.err.println("extractQuotedStrings: "+StringUtils.join(";", tokens));
			
			tokens = markSplitLine(tokens);
//			System.err.println("markSplitLine       : "+StringUtils.join(";", tokens));
	
			tokens = delimiterSplit(tokens, new char[]{' ', '\t'});
//			System.err.println("delimiterSplit      : "+StringUtils.join(";", tokens));

			tokens = markColonsAndSlices(tokens);
//			System.err.println("markColons          : "+StringUtils.join(";", tokens));
	
			// Look for target definitions here
			// if it exists, then we will skip the next two transformations
			// target definition lines should only be STRING, RAW, or COLON
			// because "1" is a valid filename, (and a valid VarInt), we need
			// to avoid parsing it out...
			
			boolean foundColon = false;
			boolean inSlice = false;
			for (Token tok: tokens) {
//				if (tok.isStatement()) {
//					// a statement is allowed before a colon (ex: for i:start..end)
//					// if you need to use the name of a statement for a filename, 
//					// then it can be quoted.
//					foundColon = false;
//					break;
//				}
				if (tok.isSliceOpen()) {
					if (inSlice) {
						throw new ASTParseException("Cannot embed []");
					}
					inSlice = true;
				}
				if (tok.isSliceClose()) {
					if (!inSlice) {
						throw new ASTParseException("Missing opening [");
					}
				}
				if (!inSlice && tok.isColon()) {
					foundColon = true;
				}
			}
			
			if (!foundColon) {
				tokens = markParens(tokens);
				tokens = markStatements(tokens);
				tokens = markOperators(tokens);
//				System.err.println("markOperators       : "+StringUtils.join(";", tokens));
		
				tokens = parseValues(tokens);
//				System.err.println("parseValues         : "+StringUtils.join(";", tokens));
		
				tokens = correctNegativeNum(tokens);
//				System.err.println("correctNegativeNum  : "+StringUtils.join(";", tokens));
			}
			return new TokenList(tokens, line);
			
		} catch(ASTParseException e) {
			e.setErrorLine(line);
			throw e;
		}
	}
	public static List<Token> markSplitLine(List<Token> tokens) throws ASTParseException {
		if (tokens.size() == 0) {
			return tokens;
		}
		List<Token> out = new ArrayList<Token>();
		out.addAll(tokens.subList(0,  tokens.size()-1));
		
		Token last = tokens.get(tokens.size()-1);
		if (!last.isRaw()) {
			out.add(last);
			return out;
		}

		String s = StringUtils.rstrip(last.getStr());

		if (!s.endsWith("\\")) {
			out.add(last);
			return out;
		}
		
		s = s.substring(0, s.length()-1);
		out.add(Token.raw(s));
		out.add(Token.splitline());
		
		return out;
	}
	
	public static List<Token> markColonsAndSlices(List<Token> tokens) throws ASTParseException {
		if (tokens.size() == 0) {
			return tokens;
		}

		List<Token> out = new ArrayList<Token>();
		for (Token tok: tokens) {
			if (!tok.isRaw()) {
				out.add(tok);
				continue;
			}
			
			String buf = "";
			for (int i=0; i<tok.getStr().length(); i++) {
				if (tok.getStr().charAt(i) == ':') {
					out.add(Token.raw(buf));
					out.add(Token.colon());
					buf = "";
				} else if (tok.getStr().charAt(i) == ',') {
					out.add(Token.raw(buf));
					out.add(Token.comma());
					buf = "";
				} else if (tok.getStr().charAt(i) == '[') {
					out.add(Token.raw(buf));
					out.add(Token.sliceOpen());
					buf = "";
				} else if (tok.getStr().charAt(i) == ']') {
					out.add(Token.raw(buf));
					out.add(Token.sliceClose());
					buf = "";
				} else {
					buf += tok.getStr().charAt(i);
				}
			}
			if (!buf.equals("")) {
				out.add(Token.raw(buf));
			}
		}

		return out;
	}

	public static List<Token> correctNegativeNum(List<Token> tokens) throws ASTParseException {
		List<Token> out = new ArrayList<Token>();

		Token twoback = null;
		Token oneback = null;
		for (Token tok: tokens) {
			boolean updated = false;
			if (tok.isValue() && tok.getValue().isNumber()) {
				if (oneback != null && oneback.isOperator() && oneback.getOp().equals(Operator.SUB)) {
					if (twoback == null || (!twoback.isValue() && !twoback.isVariable())) {
						if (tok.getValue().getClass().equals(VarInt.class)) {
							Long num = (Long) tok.getValue().getObject();
							out.remove(out.size()-1);
							out.add(Token.value(new VarInt(num * -1)));
							updated = true;
						} else if (tok.getValue().getClass().equals(VarFloat.class)) {
							Double num = (Double) tok.getValue().getObject();
							out.remove(out.size()-1);
							out.add(Token.value(new VarFloat(num * -1)));
							updated = true;
						}
					}
				}
			}
			
			if (!updated) {
				out.add(tok);
			}
			
			twoback = oneback;
			oneback = tok;
		}
		
		return out;
	}
	

	public static List<Token> parseValues(List<Token> tokens) throws ASTParseException {
		List<Token> out = new ArrayList<Token>();

		for (Token tok: tokens) {
			if (!tok.isRaw()) {
				out.add(tok);
			} else {
				try {
					out.add(Token.value(VarValue.parseString(tok.getStr())));
				} catch (VarTypeException e) {
					out.add(Token.var(tok.getStr()));
				}
			}
		}
		
		return out;
	}


	public static List<Token> delimiterSplit(List<Token> tokens, char[] delims) throws ASTParseException {
		List<Token> out = new ArrayList<Token>();

		for (Token tok: tokens) {
			if (tok.getType()!=TokenType.RAW) {
				out.add(tok);
				continue;
			}
			
			String buf = "";
			for (int i=0; i<tok.getStr().length(); i++ ) {
				boolean found = false;
				for (char delim: delims) {
					if (tok.getStr().charAt(i) == delim) {
						found = true;
						if (!buf.equals("")) {
							out.add(Token.raw(buf));
						}
						buf = "";
					}
				}
				if (!found) {
					buf += tok.getStr().charAt(i);
				}
			}
			if (!buf.equals("")) {
				out.add(Token.raw(buf));
			}
		}
		
		return out;
	}

	public static List<Token> markParens(List<Token> tokens) throws ASTParseException {
		List<Token> out = new ArrayList<Token>();

		for (Token tok: tokens) {
			if (!tok.isRaw()) {
				out.add(tok);
				continue;
			}
			String buf = "";
			int i=0;
			while (i<tok.getStr().length()) {
				boolean found = false;
				if (tok.getStr().substring(i,i+1).equals("(")) {
					if (!buf.equals("")) {
						out.add(Token.raw(buf));
					}
					out.add(Token.parenOpen());
					buf = "";
					found = true;
					i += 1;
				} else if (tok.getStr().substring(i,i+1).equals(")")) {
					if (!buf.equals("")) {
						out.add(Token.raw(buf));
					}
					out.add(Token.parenClose());
					buf = "";
					found = true;
					i += 1;
				}
				if (!found) {
					buf += tok.getStr().charAt(i);
					i += 1;
				}
			}
			if (!buf.equals("")) {
				out.add(Token.raw(buf));
			}
		}
		
		return out;
	}

	public static List<Token> markStatements(List<Token> tokens) throws ASTParseException {
		List<Token> out = new ArrayList<Token>();

		for (Token tok: tokens) {
			if (!tok.isRaw()) {
				out.add(tok);
				continue;
			}

			boolean found = false;
			for (Statement stmt: Statement.statements) {
				if (tok.getStr().equals(stmt.getName())) {
					out.add(Token.statement(stmt));
					found = true;
					break;
				}
			}
			if (!found) {
				out.add(tok);
			}
		}
		
		return out;
	}

	public static List<Token> markOperators(List<Token> tokens) throws ASTParseException {
		List<Token> out = new ArrayList<Token>();

		for (Token tok: tokens) {
			if (!tok.isRaw()) {
				out.add(tok);
				continue;
			}
			String buf = "";
			int i=0;
			while (i<tok.getStr().length()) {
				boolean found = false;
				// Statements must be followed by whitespace (or a paren?)
				for (Operator op: Operator.operators) {
					if (tok.getStr().length() >= i+op.getSymbol().length()) {
						if (op.getSymbol().equals(tok.getStr().substring(i,  i+op.getSymbol().length()))) {
							if (!buf.equals("")) {
								out.add(Token.raw(buf));
							}
							out.add(Token.op(op));
							buf = "";
							found = true;
							i += op.getSymbol().length();
							break;
						}
					}
				}
				if (!found) {
					buf += tok.getStr().charAt(i);
					i += 1;
				}
			}
			if (!buf.equals("")) {
				out.add(Token.raw(buf));
			}
		}
		
		return out;
	}

	public static List<Token> extractQuotedStrings(String line, char quoteChar, char commentChar) throws ASTParseException {
		String buf = "";
		List<Token> tokens = new ArrayList<Token>();
		boolean inquote = false;
		boolean inshell = false;
		for (int i=0; i<line.length(); i++) {
			if (inquote) {
				if (line.charAt(i) == quoteChar && !buf.endsWith("\\")) {
					tokens.add(Token.string(buf));
					inquote = false;
					buf = "";
				} else {
					buf += line.charAt(i);
				}
			} else if (inshell) {
				if (line.charAt(i) == ')' && !buf.endsWith("\\")) {
					tokens.add(Token.shell(buf));
					inshell = false;
					buf = "";
				} else {
					buf += line.charAt(i);
				}
			}  else {
				if (i < line.length()-2 && line.substring(i, i+2).equals("$(") && !buf.endsWith("\\")) {
					if (!buf.equals("")) {
						tokens.add(Token.raw(buf));
					}
					buf = "";
					inshell = true;
					i++;
				} else if (line.charAt(i) == quoteChar && !buf.endsWith("\\")) {
					if (!buf.equals("")) {
						tokens.add(Token.raw(buf));
					}
					buf = "";
					inquote = true;
				} else if (line.charAt(i) == commentChar) {
					if (!buf.equals("")) {
						tokens.add(Token.raw(buf));
					}
					buf = "";
					break;
				} else {
					buf += line.charAt(i);
				}
			}
		}
		
		if (inquote) {
			throw new ASTParseException("Error parsing line - missing quotes: "+line);
		}
		
		if (!buf.equals("")) {
			tokens.add(Token.raw(buf));
		}
		
		return tokens;
	}
	
}
