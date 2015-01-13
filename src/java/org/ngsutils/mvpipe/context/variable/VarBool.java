package org.ngsutils.mvpipe.context.variable;

public class VarBool extends VarValue {
	public VarBool(boolean val) {
		super(val);
	}
	
	public boolean isTrue() {
		if ((Boolean) obj) {
			return true;
		}
		return false;
	}
}
