package org.ngsutils.mvpipe.parser.context;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.ngsutils.mvpipe.parser.variable.VarValue;
import org.ngsutils.mvpipe.support.StringUtils;

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

		System.err.println("inputs: " + StringUtils.join(" ", this.inputs));
		System.err.println("outputs: " + StringUtils.join(" ", this.outputs));
		
		System.err.println("context:");
		for (String k: capturedContext.keySet()) {
			System.err.println("  "+k+" => "+capturedContext.get(k));
		}
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

	public boolean matches(String outputName) {
		for (String out: outputs) {
			if (out.equals(outputName)) {
				return true;
			}
		}
		return false;
	}

	
	public void build() {
	}

}
