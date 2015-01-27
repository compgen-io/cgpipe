package org.ngsutils.mvpipe.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.ngsutils.mvpipe.parser.variable.VarString;
import org.ngsutils.mvpipe.parser.variable.VarValue;

public class Tokens {
//	public final int linenum;
//	public final String filename;
	protected final List<String> tokens = new ArrayList<String>();
	
	public Tokens(String filename, int linenum, String line) {
		// not sure if we need the filename / linenum here...
//		this.filename = filename;
//		this.linenum = linenum;
		this.tokens.addAll(Tokenizer.tokenize(line));
	}
	public Tokens(String filename, int linenum, List<String> tokens) {
//		this.filename = filename;
//		this.linenum = linenum;
		this.tokens.addAll(tokens);
	}
	
	public Tokens() {
	}
	public Tokens(List<String> tokens) {
		this.tokens.addAll(tokens);
	}
	public int size() {
		return tokens.size();
	}
	
	public String get(int i) {
		return tokens.get(i);
	}
	
//	public Tokens clone(List<String> sub) {
//		return new Tokens(filename, linenum, sub);
//	}

	public Tokens subList(int from, int to) {
		List<String> l = new ArrayList<String>();
		l.addAll(tokens.subList(from, to));
//		return new Tokens(filename, linenum, l);
		return new Tokens(l);
	}

	public List<String> getList() {
		return Collections.unmodifiableList(tokens);
	}
	public void add(VarValue val) {
		if (val.getClass().equals(VarString.class)) {
			tokens.add("\""+val.toString()+"\"");
		} else {
			tokens.add(val.toString());
		}
	}
	public void add(Tokens l) {
		tokens.addAll(l.getList());
	}
}
