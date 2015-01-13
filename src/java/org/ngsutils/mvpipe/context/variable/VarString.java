package org.ngsutils.mvpipe.context.variable;

public class VarString extends VarValue {
	public VarString(String val) {
		super(val);
	}
	
	public boolean isTrue() {
		if (((String) obj).equals("")) {
			return false;
		}
		return true;
	}
}
