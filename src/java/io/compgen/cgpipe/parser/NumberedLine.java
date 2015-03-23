package io.compgen.cgpipe.parser;


public class NumberedLine {
	public final String filename;
	public final String line;
	public final int linenum;

	/**
	 * Embedded line for processing (don't use this ctor)
	 * @param line
	 */
	public NumberedLine(String line) {
		this.filename = "<none>";
		this.linenum = -1;
		this.line = line;
	}

	/**
	 * If possible, use this ctor - you'll have better error messages.
	 * @param line
	 */

	public NumberedLine(String filename, int linenum, String line) {
		this.filename = filename;
		this.linenum = linenum;
		this.line = line;
	}
	
	public String toString() {
		return filename+"["+linenum+"] "+line;
	}

	public NumberedLine stripPrefix() {
		return new NumberedLine(filename, linenum, line.replaceFirst(" *#\\$", ""));
	}
}
