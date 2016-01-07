package io.compgen.cgpipe.exceptions;


public class MethodCallException extends ASTException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5921427230009309228L;

	public MethodCallException(String msg) {
		super(msg);
	}

	public MethodCallException(Exception e) {
		super(e);
	}

}
