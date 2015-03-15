package io.compgen.mvpipe.parser.tokens;

import io.compgen.mvpipe.parser.op.Operator;
import io.compgen.mvpipe.parser.statement.Statement;
import io.compgen.mvpipe.parser.variable.VarString;
import io.compgen.mvpipe.parser.variable.VarValue;

public class Token {
	final private TokenType type;
	final private String str;
	final private VarValue value;
	final private Operator op;
	final private Statement statement;
	
	private Token(TokenType type, String str, VarValue value, Operator op, Statement statement) {
		this.type = type;
		this.str = str;
		this.value = value;
		this.op = op;
		this.statement = statement;
	}

	static public Token raw(String str) {
		return new Token(TokenType.RAW, str, null, null, null);
	}

//	static public Token quoted(String str) {
//		return new Token(TokenClass.QUOTED_STRING, str, null, null, null);
//	}

	static public Token var(String str) {
		return new Token(TokenType.VARIABLE, str, null, null, null);
	}

	static public Token value(VarValue val) {
		return new Token(TokenType.VALUE, null, val, null, null);
	}

	static public Token string(String str) {
		return new Token(TokenType.STRING, str, new VarString(str), null, null);
	}

	static public Token op(Operator op) {
		return new Token(TokenType.OPERATOR, null, null, op, null);
	}

	static public Token statement(Statement statement) {
		return new Token(TokenType.STATEMENT, null, null, null, statement);
	}

	static public Token parenOpen() {
		return new Token(TokenType.PAREN_OPEN, "(", null, null, null);
	}

	static public Token parenClose() {
		return new Token(TokenType.PAREN_CLOSE, ")", null, null, null);
	}
	
	public static Token colon() {
		return new Token(TokenType.COLON, ":", null, null, null);
	}
	
	public static Token shell(String str) {
		return new Token(TokenType.SHELL, str, null, null, null);
	}
	
	public static Token splitline() {
		return new Token(TokenType.SPLIT_LINE, "\\", null, null, null);
	}
	

	public String toString() {
		if (str != null) {
			return type+"|"+str;
		}
		
		if (op != null) {
			return "op|"+op.getSymbol();			
		}
		
		if (statement != null) {
			return "st|"+statement.getName();			
		}
		
		return "v|"+value.toString();
	}

	public boolean isValue() {
		return type == TokenType.VALUE || type == TokenType.STRING;
	}

	public boolean isOperator() {
		return type == TokenType.OPERATOR;
	}

	public boolean isVariable() {
		return type == TokenType.VARIABLE;
	}

	public boolean isStatement() {
		return type == TokenType.STATEMENT;
	}

	public boolean isRaw() {
		return type == TokenType.RAW;
	}

	public boolean isParenOpen() {
		return type == TokenType.PAREN_OPEN;
	}

	public boolean isParenClose() {
		return type == TokenType.PAREN_CLOSE;
	}

	public boolean isColon() {
		return type == TokenType.COLON;
	}

	public boolean isString() {
		return type == TokenType.STRING;
	}

	public boolean isShell() {
		return type == TokenType.SHELL;
	}

	public boolean isSplitLine() {
		return type == TokenType.SPLIT_LINE;
	}

	public TokenType getType() {
		return type;
	}

	public String getStr() {
		return str;
	}

	public VarValue getValue() {
		return value;
	}

	public Operator getOp() {
		return op;
	}

	public Statement getStatement() {
		return statement;
	}

}
