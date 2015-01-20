package org.ngsutils.mvpipe.parser;

import java.util.Collections;
import java.util.List;

public class Tokens {
	public final int linenum;
	public final String filename;
	protected final List<String> tokens;
	
	public Tokens(String filename, int linenum, String line) {
		// not sure if we need the filename / linenum here...
		this.filename = filename;
		this.linenum = linenum;
		this.tokens = Tokenizer.tokenize(line);
	}
	public Tokens(String filename, int linenum, List<String> tokens) {
		this.filename = filename;
		this.linenum = linenum;
		this.tokens = tokens;
	}
	
	public int size() {
		return tokens.size();
	}
	
	public String get(int i) {
		return tokens.get(i);
	}
	
	public Tokens clone(List<String> sub) {
		return new Tokens(filename, linenum, sub);
	}

	public Tokens subList(int from, int to) {
		return new Tokens(filename, linenum, tokens.subList(from, to));
	}

	public List<String> getList() {
		return Collections.unmodifiableList(tokens);
	}
}
