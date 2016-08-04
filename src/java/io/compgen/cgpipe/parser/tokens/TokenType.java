package io.compgen.cgpipe.parser.tokens;

public enum TokenType {
	RAW,
	VARIABLE,
	VALUE,
	STRING,
	OPERATOR, 
	STATEMENT,
	PAREN_OPEN,
	PAREN_CLOSE,
	COMMA,
	COLON,
	SHELL, 
	SLICE_OPEN, 
	SLICE_CLOSE, 
	SPLIT_LINE, 
	DOT, 
	EVAL
}
