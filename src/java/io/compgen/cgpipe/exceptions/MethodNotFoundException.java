package io.compgen.cgpipe.exceptions;


public class MethodNotFoundException extends ASTException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6791688823318993925L;

	public MethodNotFoundException(String msg) {
		super(msg);
	}

	public MethodNotFoundException(Exception e) {
		super(e);
	}
}
