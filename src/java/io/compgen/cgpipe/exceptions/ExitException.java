package io.compgen.cgpipe.exceptions;

public class ExitException extends ASTExecException {
	final private int retcode;
	
	public ExitException(int retcode) {
		this.retcode = retcode;
	}
	public ExitException() {
		this.retcode = -1;
	}
	
	public int getReturnCode() {
		return retcode;
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2240898950853627223L;

}
