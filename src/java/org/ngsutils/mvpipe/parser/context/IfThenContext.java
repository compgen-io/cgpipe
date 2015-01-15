package org.ngsutils.mvpipe.parser.context;

import org.ngsutils.mvpipe.parser.variable.VarValue;

public class IfThenContext extends ExecContext {
	public IfThenContext(ExecContext parent, boolean active) {
		super(parent, active);
	}
	
	public void set(String name, VarValue val) {
		parent.set(name, val);
	}

}
