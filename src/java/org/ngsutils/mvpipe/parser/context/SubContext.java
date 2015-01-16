package org.ngsutils.mvpipe.parser.context;

import org.ngsutils.mvpipe.parser.variable.VarValue;

public class SubContext extends ExecContext {
	private final boolean passthru;
	
	public SubContext(ExecContext parent, boolean active, boolean passthru) {
		super(parent, active);
		this.passthru = passthru;
	}
	
	public void set(String name, VarValue val) {
		if (passthru) {
			parent.set(name, val);
		} else {
			super.set(name, val);
		}
	}
}
