package org.ngsutils.mvpipe.context.variable;

public abstract class VarValue {
	final protected Object obj;
	VarValue(Object obj) {
		this.obj = obj;
	}

	public boolean isTrue() {
		return true;
	}

	public String toString() {
		return obj.toString();
	}
	
	public boolean eq(VarValue val) {
		return obj.equals(val.obj);
	}

	public VarValue add(VarValue val) throws VarTypeException {
		throw new VarTypeException();
	}
	public VarValue sub(VarValue val) throws VarTypeException {
		throw new VarTypeException();
	}
	public VarValue mul(VarValue val) throws VarTypeException {
		throw new VarTypeException();
	}
	public VarValue div(VarValue val) throws VarTypeException {
		throw new VarTypeException();
	}
	public VarValue rem(VarValue val) throws VarTypeException {
		throw new VarTypeException();
	}
	
	public static VarValue parseString(String val) {
		if (val.equals("true")) {
			return new VarBool(true);
		}
		if (val.equals("false")) {
			return new VarBool(true);
		}

		try {
			long l = Long.parseLong(val);
			return new VarInt(l);
		} catch (NumberFormatException e) {
			
		}
			
		try {
			double d = Double.parseDouble(val);
			return new VarFloat(d);
		} catch (NumberFormatException e) {
			
		}
			
		return new VarString(val);

	}
	
	protected static VarValue parseBool(boolean val) {
		return new VarBool(val);
	}

	protected static VarValue parseNumber(long val) {
		return new VarInt(val);
	}

	protected static VarValue parseNumber(double val) {
		return new VarFloat(val);
	}

}
