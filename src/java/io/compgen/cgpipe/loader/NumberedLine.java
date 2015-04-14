package io.compgen.cgpipe.loader;




public class NumberedLine {
	private final Source source;
	private final String line;
	private final int linenum;

	/**
	 * Embedded line for processing (don't use this ctor)
	 * @param line
	 */
	public NumberedLine(String line) {
		this.source = new NullSource(this);
		this.linenum = -1;
		this.line = line;
	}


	/**
	 * Embedded line for processing (don't use this ctor)
	 * @param line
	 */
	public NumberedLine(String line, int linenum) {
		this.source = new NullSource(this);
		this.linenum = linenum;
		this.line = line;
	}

	/**
	 * If possible, use this ctor - you'll have better error messages.
	 * @param line
	 */

	public NumberedLine(Source source, int linenum, String line) {
		this.source = source;
		this.linenum = linenum;
		this.line = line;
	}
	
	public NumberedLine(String line, NumberedLine parent) {
		this.source = parent.source;
		this.linenum = parent.linenum;
		this.line = line;
	}

	public String toString() {
		return source+"["+linenum+"] "+line;
	}

	public NumberedLine stripPrefix() {
		return new NumberedLine(line.replaceFirst(" *#\\$", ""), this);
	}
	
	public String getLine() {
		return line;
	}

	public Source getPipeline() {
		return source;
	}

	public int getLineNumber() {
		return linenum;
	}
}

