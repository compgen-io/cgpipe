package org.ngsutils.mvpipe.exceptions;

public class RunnerException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7779440903149320629L;

	public RunnerException(String s) {
		super(s);
	}

	public RunnerException(SyntaxException e) {
		super(e);
	}
}
