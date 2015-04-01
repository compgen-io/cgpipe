package io.compgen.cgpipe.pipeline;




public class NumberedLine {
	private final Pipeline pipeline;
	private final String line;
	private final int linenum;

	/**
	 * Embedded line for processing (don't use this ctor)
	 * @param line
	 */
	public NumberedLine(String line) {
		this.pipeline = new NullPipeline(this);
		this.linenum = -1;
		this.line = line;
	}

	/**
	 * If possible, use this ctor - you'll have better error messages.
	 * @param line
	 */

	public NumberedLine(Pipeline pipeline, int linenum, String line) {
		this.pipeline = pipeline;
		this.linenum = linenum;
		this.line = line;
	}
	
	public NumberedLine(String line, NumberedLine parent) {
		this.pipeline = parent.pipeline;
		this.linenum = parent.linenum;
		this.line = line;
	}

	public String toString() {
		return pipeline+"["+linenum+"] "+line;
	}

	public NumberedLine stripPrefix() {
		return new NumberedLine(line.replaceFirst(" *#\\$", ""), this);
	}
	
	public String getLine() {
		return line;
	}

	public Pipeline getPipeline() {
		return pipeline;
	}

	public int getLineNumber() {
		return linenum;
	}
}

