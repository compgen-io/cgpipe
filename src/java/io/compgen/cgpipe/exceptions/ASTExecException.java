package io.compgen.cgpipe.exceptions;

import io.compgen.cgpipe.parser.NumberedLine;
import io.compgen.cgpipe.parser.tokens.TokenList;

public class ASTExecException extends ASTException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3870822617460035359L;
	public ASTExecException(String msg) {
		super(msg);
	}

	public ASTExecException(Exception e) {
		super(e);
	}

	public ASTExecException(String msg, TokenList tokens) {
		super(msg, tokens.getLine());
	}

	public ASTExecException(Exception e, TokenList tokens) {
		super(e, tokens.getLine());
	}

	public ASTExecException(String msg, NumberedLine line) {
		super(msg, line);
	}

	public ASTExecException(Exception e, NumberedLine line) {
		super(e, line);
	}

}
