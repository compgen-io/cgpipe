package org.ngsutils.mvpipe.parser.context;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.ngsutils.mvpipe.parser.variable.VarValue;

public class BuildTarget {
	private final List<String> outputs;
	private final List<String> inputs;
	private final Map<String, VarValue> capturedContext;

	private final List<String> lines = new ArrayList<String>();

	public BuildTarget(List<String> outputs, List<String> inputs, ExecContext cxt) {
		// this is a target context, capture the parent values
		this.outputs = outputs;
		this.inputs = inputs;
		capturedContext = cxt.cloneValues();
	}
	
	public void addLine(String line) {
		this.lines.add(line);
	}

}
