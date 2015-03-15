package io.compgen.mvpipe.parser;


public class NumberedLine {
	public final String filename;
	public final String line;
	public final int linenum;

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
