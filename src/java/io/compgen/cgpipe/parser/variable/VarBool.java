package io.compgen.cgpipe.parser.variable;

import io.compgen.cgpipe.exceptions.VarTypeException;

public class VarBool extends VarValue {

	public static final VarBool TRUE = new VarBool(true);
	public static final VarBool FALSE = new VarBool(false);

	private VarBool(boolean val) {
		super(val);
	}

	public boolean toBoolean() {
		if ((Boolean) obj) {
			return true;
		}
		return false;
	}

	// Match the implicit-toString rule already in VarInt/VarFloat: when a
	// value is being added to a string, the non-string side becomes its
	// toString() form and the result is a VarString. Bool + non-string is
	// still an error — you can't sensibly add a bool to a number or another
	// bool, so we don't try.
	public VarValue add(VarValue val) throws VarTypeException {
		if (val.getClass().equals(VarString.class)) {
			return new VarString(toString() + val.toString());
		}
		throw new VarTypeException("Invalid operation");
	}
}
