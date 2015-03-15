package org.ngsutils.mvpipe.exceptions;

import org.ngsutils.mvpipe.parser.NumberedLine;


public class ASTException extends Exception {
	private NumberedLine line = null;

	public ASTException(String msg) {
		super(msg);
	}

	public ASTException(Exception e) {
		super(e);
	}

	public ASTException(String msg, NumberedLine line) {
		super(msg);
		this.line = line;
	}

	public ASTException(Exception e, NumberedLine line) {
		super(e);
		this.line = line;
	}

	public void setErrorLine(NumberedLine line) {
		this.line = line;
	}
	
	public String getMessage() {
		if (line == null) {
			return "<unknown>: "+ super.getMessage();
		}
		return line.filename+"["+line.linenum+"]: "+ super.getMessage();
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -640444392072016776L;

}
