package org.ngsutils.mvpipe.parser.variable;

import org.ngsutils.mvpipe.parser.SyntaxException;

public class VarTypeException extends SyntaxException {

	public VarTypeException(String msg) {
		super(msg);
	}

	public VarTypeException(Exception e) {
		super(e);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 5650244650339480067L;

}
