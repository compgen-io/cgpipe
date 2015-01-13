package org.ngsutils.mvpipe.context;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.ngsutils.mvpipe.context.variable.VarValue;

public class TargetContext extends ExecContext {
	protected List<String> lines = new ArrayList<String>();
	
	public TargetContext(ExecContext root) {
		// this is a target context, capture the parent values
		super();
		
		Map<String, VarValue> clone = root.cloneValues();

		for (String k: clone.keySet()) {
			this.vars.put(k, clone.get(k));
		}
	}

	public void parseLine(String line) {
		// just add the lines at the moment, don't actually parse them unless required.
		this.lines.add(line);		
	}

}
