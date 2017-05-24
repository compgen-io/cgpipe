package io.compgen.cgpipe.parser.variable;

import io.compgen.cgpipe.exceptions.VarTypeException;

public class VarNull extends VarValue {
	public static final VarNull NULL = new VarNull();
	
	private VarNull() {
		super(null);
	}

	public String toString() {
		return "";
	}
	
	public boolean toBoolean() {
		return false;
	}
	public VarValue eq(VarValue val) throws VarTypeException {
		throw new VarTypeException("Invalid operation");
	}
}
