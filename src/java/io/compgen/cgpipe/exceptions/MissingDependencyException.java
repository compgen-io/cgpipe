package io.compgen.cgpipe.exceptions;


public class MissingDependencyException extends RunnerException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7779440903149320629L;

	public MissingDependencyException(String s) {
		super(s);
	}

	public MissingDependencyException(Exception e) {
		super(e);
	}
}
