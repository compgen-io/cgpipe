package org.ngsutils.mvpipe.exceptions;


public class EvalException extends SyntaxException {

	public EvalException(Exception e) {
		super(e);
	}

	public EvalException(String s) {
		super(s);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 2131503481925650587L;

}
