package org.ngsutils.mvpipe.parser;

import org.ngsutils.mvpipe.parser.variable.VarTypeException;

public class SyntaxException extends Exception {

	private int linenum = -1;
	private String filename = "<unknown>";
	
	public SyntaxException(String msg) {
		super(msg);
	}

	public SyntaxException(VarTypeException e) {
		super(e);
	}

	public void setErrorLine(String filename, int linenum) {
		this.filename = filename;
		this.linenum = linenum;
	}

	public String getMessage() {
		return filename+"["+linenum+"]: "+ super.getMessage();
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -640444392072016776L;

}
