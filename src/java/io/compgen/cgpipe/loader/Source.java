package io.compgen.cgpipe.loader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Source {
	private SourceLoader loader;
	private String name;
	protected List<NumberedLine> lines;
	private boolean finalized = false;
	private String hashDigest = null;
	
	public Source(String name, SourceLoader loader) {
		this.loader = loader;
		this.name = name;
		this.lines = new ArrayList<NumberedLine>();
	}
	
	public void addLine(String line, int linenum) {
		if (!finalized) {
			this.lines.add(new NumberedLine(this, linenum, line));
		}
	}
	
	public void finalize() {
		this.finalized = true;
	}

	public List<NumberedLine> getLines() {
		return Collections.unmodifiableList(lines);
	}
	
	public String toString() {
		return getName();
	}
	
	public String getName() {
		return name;
	}
	
	public SourceLoader getLoader() {
		return loader;
	}

	public void setHashDigest(String digestStr) {
		this.hashDigest  = digestStr;
	}
	
	public String getHashDigest() {
		return hashDigest;
	}
}
