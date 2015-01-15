package org.ngsutils.mvpipe.parser.variable;

import org.ngsutils.mvpipe.parser.context.ExecContext;

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
	
	public VarValue eq(VarValue val) {
		return obj.equals(val.obj) ? VarBool.TRUE: VarBool.FALSE;
	}

	public VarValue add(VarValue val) throws VarTypeException {
		throw new VarTypeException("Invalid operation");
	}
	public VarValue sub(VarValue val) throws VarTypeException {
		throw new VarTypeException("Invalid operation");
	}
	public VarValue mul(VarValue val) throws VarTypeException {
		throw new VarTypeException("Invalid operation");
	}
	public VarValue div(VarValue val) throws VarTypeException {
		throw new VarTypeException("Invalid operation");
	}
	public VarValue rem(VarValue val) throws VarTypeException {
		throw new VarTypeException("Invalid operation");
	}
	public VarValue pow(VarValue val) throws VarTypeException {
		throw new VarTypeException("Invalid operation");
	}
	public VarValue lt(VarValue val) throws VarTypeException {
		throw new VarTypeException("Invalid operation");
	}
	public VarValue gt(VarValue val) throws VarTypeException {
		if (lt(val) == VarBool.FALSE && eq(val) == VarBool.FALSE) {
			return VarBool.TRUE;
		}
		return VarBool.FALSE;
	}
	public VarValue lte(VarValue val) throws VarTypeException {
		if (lt(val) == VarBool.TRUE || eq(val) == VarBool.TRUE) {
			return VarBool.TRUE;
		}
		return VarBool.FALSE;
	}
	public VarValue gte(VarValue val) throws VarTypeException {
		if (lt(val) == VarBool.FALSE || eq(val) == VarBool.TRUE) {
			return VarBool.TRUE;
		}
		return VarBool.FALSE;
	}
	
	public static VarValue parseString(String val) throws VarTypeException {
		return parseString(val, null);
	}
	public static VarValue parseString(String val, ExecContext cxt) throws VarTypeException {
		if (val.equals("true")) {
			return VarBool.TRUE;
		}
		if (val.equals("false")) {
			return VarBool.FALSE;
		}

		try {
			long l = Long.parseLong(val);
			return new VarInt(l);
		} catch (NumberFormatException e) {
			// ignore
		}
			
		try {
			double d = Double.parseDouble(val);
			return new VarFloat(d);
		} catch (NumberFormatException e) {
			// ignore
		}
		
		if (val.charAt(0) == '"' && val.charAt(val.length()-1) == '"') {
			String s = val.substring(1, val.length()-1);
			if (cxt != null) {
				s = cxt.evalString(s);
			}
			return new VarString(s);
		}
		
		if (cxt != null) {
			if (cxt.contains(val)) {
				return cxt.get(val);
			}
			// If the value is not set, then we'll assume it should be "false"
			return VarBool.FALSE;
		} else {
			return new VarString(val);
		}
	}
	
	protected static VarValue parseBool(boolean val) {
		if (val) {
			return VarBool.TRUE;
		}
		return VarBool.FALSE;
	}

	protected static VarValue parseNumber(long val) {
		return new VarInt(val);
	}

	protected static VarValue parseNumber(double val) {
		return new VarFloat(val);
	}

}
