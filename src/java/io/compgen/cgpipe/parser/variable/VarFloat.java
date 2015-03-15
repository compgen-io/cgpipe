package io.compgen.cgpipe.parser.variable;

import io.compgen.cgpipe.exceptions.VarTypeException;

public class VarFloat extends VarValue {
	public VarFloat(double val) {
		super(val);
	}
	
	public boolean isTrue() {
		if (((Double) obj) == 0.0d) {
			return false;
		}
		return true;
	}

	public boolean isNumber() {
		return true;
	}

	public String toString() {
		String s = obj.toString();
		if (s.endsWith(".0")) {
			return s.substring(0, s.length()-2);
		}
		return s;
	}
	
	public VarValue lt(VarValue val) throws VarTypeException {
		if (val.getClass().equals(VarInt.class)) {
			return ((Double)obj < (Long)val.obj) ? VarBool.TRUE: VarBool.FALSE;
		} else if (val.getClass().equals(VarFloat.class)) {
			return ((Double)obj < (Double)val.obj) ? VarBool.TRUE: VarBool.FALSE;
		}
		throw new VarTypeException("Invalid operation");
	}

	public VarValue add(VarValue val) throws VarTypeException {
		if (val.getClass().equals(VarInt.class)) {
			return new VarFloat((Double)obj + (Long)val.obj);
		} else if (val.getClass().equals(VarFloat.class)) {
			return new VarFloat((Double)obj + (Double)val.obj);
		} else {
			return new VarString(toString() + val.toString());
		}
	}
	
	public VarValue sub(VarValue val) throws VarTypeException {
		if (val.getClass().equals(VarInt.class)) {
			return new VarFloat((Double)obj - (Long)val.obj);
		} else if (val.getClass().equals(VarFloat.class)) {
			return new VarFloat((Double)obj - (Double)val.obj);
		}
		throw new VarTypeException("Invalid operation");
	}

	public VarValue mul(VarValue val) throws VarTypeException {
		if (val.getClass().equals(VarInt.class)) {
			return new VarFloat((Double)obj * (Long)val.obj);
		} else if (val.getClass().equals(VarFloat.class)) {
			return new VarFloat((Double)obj * (Double)val.obj);
		}
		throw new VarTypeException("Invalid operation");
	}

	public VarValue div(VarValue val) throws VarTypeException {
		if (val.getClass().equals(VarInt.class)) {
			return new VarFloat((Double)obj / (Long)val.obj);
		} else if (val.getClass().equals(VarFloat.class)) {
			return new VarFloat((Double)obj / (Double)val.obj);
		}
		throw new VarTypeException("Invalid operation");
	}

	public VarValue rem(VarValue val) throws VarTypeException {
		if (val.getClass().equals(VarInt.class)) {
			return new VarFloat((Double)obj % (Long)val.obj);
		} else if (val.getClass().equals(VarFloat.class)) {
			return new VarFloat((Double)obj % (Double)val.obj);
		}
		throw new VarTypeException("Invalid operation");
	}
	public VarValue pow(VarValue val) throws VarTypeException {
		if (val.getClass().equals(VarInt.class)) {
			return new VarFloat(Math.pow((Double)obj, (Long)val.obj));
		} else if (val.getClass().equals(VarFloat.class)) {
			return new VarFloat(Math.pow((Double)obj, (Double)val.obj));
		}
		throw new VarTypeException("Invalid operation");
	}
}
