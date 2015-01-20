package org.ngsutils.mvpipe.parser.variable;

public class VarNull extends VarValue {
	public static final VarNull NULL = new VarNull();
	
	private VarNull() {
		super(null);
	}

	public String toString() {
		return "<NULL>";
	}
	
	public boolean isTrue() {
		return false;
	}
	public VarValue eq(VarValue val) throws VarTypeException {
		throw new VarTypeException("Invalid operation");
	}
}
