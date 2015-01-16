package org.ngsutils.mvpipe.parser.context;

import java.util.ArrayList;
import java.util.List;

public class DeferredExecContext extends ExecContext {
	private List<String> lines = new ArrayList<String>();

	public DeferredExecContext(ExecContext parent) {
		super(parent);
	}

	public boolean isDeferred() {
		return true;
	}

	public void addLine(String line) {
		lines.add(line);
	}
	
	public void evalLines() {
		
	}
}
