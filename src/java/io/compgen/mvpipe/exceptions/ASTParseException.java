package io.compgen.mvpipe.exceptions;

import io.compgen.mvpipe.parser.NumberedLine;
import io.compgen.mvpipe.parser.tokens.TokenList;

public class ASTParseException extends ASTException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3870822617460035359L;
	public ASTParseException(String msg) {
		super(msg);
	}

	public ASTParseException(Exception e) {
		super(e);
	}

	public ASTParseException(String msg, TokenList tokens) {
		super(msg, tokens.getLine());
	}

	public ASTParseException(Exception e, TokenList tokens) {
		super(e, tokens.getLine());
	}

	public ASTParseException(String msg, NumberedLine line) {
		super(msg, line);
	}

	public ASTParseException(Exception e, NumberedLine line) {
		super(e, line);
	}
}
