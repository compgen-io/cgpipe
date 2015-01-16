package org.ngsutils.mvpipe.parser.context;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.ngsutils.mvpipe.parser.variable.VarValue;

public class BuildTarget {
	private final List<String> outputs;
	private final List<String> inputs;
	private final Map<String, VarValue> capturedContext;
	public final int indentLevel;

	private final List<String> lines = new ArrayList<String>();

	public BuildTarget(List<String> outputs, List<String> inputs, ExecContext cxt, int indentLevel) {
		// this is a target context, capture the parent values
		this.outputs = outputs;
		this.inputs = inputs;
		capturedContext = cxt.cloneValues();
		this.indentLevel = indentLevel;
	}
	
	public void addLine(String line) {
		this.lines.add(line);
	}

	public static int calcIndentLevel(String line) {
		int acc = 0;
		for (int i=0; i<line.length(); i++) {
			if (line.charAt(i) == ' ') { 
				acc +=1;
			} else if (line.charAt(i) == '\t') { 
				acc += 4; 
			} else {
				return acc;
			}
		}
		return acc;
	}

}
