package io.compgen.cgpipe.parser.variable;

import io.compgen.cgpipe.exceptions.VarTypeException;

import java.util.ArrayList;
import java.util.List;

public class VarInt extends VarValue {
	public VarInt(long val) {
		super(val);
	}
	
	public boolean toBoolean() {
		if (((Long) obj) == 0L) {
			return false;
		}
		return true;
	}

	public boolean isNumber() {
		return true;
	}

	public VarValue lt(VarValue val) throws VarTypeException {
		if (val.getClass().equals(VarInt.class)) {
			return ((Long)obj < (Long)val.obj) ? VarBool.TRUE: VarBool.FALSE;
		} else if (val.getClass().equals(VarFloat.class)) {
			return ((Long)obj < (Double)val.obj) ? VarBool.TRUE: VarBool.FALSE;
		}
		throw new VarTypeException("Invalid operation");
	}

	public VarValue add(VarValue val) throws VarTypeException {
		if (val.getClass().equals(VarInt.class)) {
			return new VarInt((Long)obj + (Long)val.obj);
		} else if (val.getClass().equals(VarFloat.class)) {
			return new VarFloat((Long)obj + (Double)val.obj);
		} else {
			return new VarString(toString() + val.toString());
		}
	}

	public VarValue sub(VarValue val) throws VarTypeException {
		if (val.getClass().equals(VarInt.class)) {
			return new VarInt((Long)obj - (Long)val.obj);
		} else if (val.getClass().equals(VarFloat.class)) {
			return new VarFloat((Long)obj - (Double)val.obj);
		}
		throw new VarTypeException("Invalid operation");
	}

	public VarValue mul(VarValue val) throws VarTypeException {
		if (val.getClass().equals(VarInt.class)) {
			return new VarInt((Long)obj * (Long)val.obj);
		} else if (val.getClass().equals(VarFloat.class)) {
			return new VarFloat((Long)obj * (Double)val.obj);
		} else if (val.getClass().equals(VarList.class)) {
			List<VarValue> tmp = new ArrayList<VarValue>();
			for (long i=0; i<(Long) obj; i++) {
				for (VarValue v: ((VarList)val).vals) {
					tmp.add(v);
				}
			}
			return new VarList(tmp);
		}
		throw new VarTypeException("Invalid operation");
	}

	public VarValue div(VarValue val) throws VarTypeException {
		if (val.getClass().equals(VarInt.class)) {
			return new VarInt((Long)obj / (Long)val.obj);
		} else if (val.getClass().equals(VarFloat.class)) {
			return new VarFloat((Long)obj / (Double)val.obj);
		}
		throw new VarTypeException("Invalid operation");
	}

	public VarValue rem(VarValue val) throws VarTypeException {
		if (val.getClass().equals(VarInt.class)) {
			return new VarInt((Long)obj % (Long)val.obj);
		} else if (val.getClass().equals(VarFloat.class)) {
			return new VarFloat((Long)obj % (Double)val.obj);
		}
		throw new VarTypeException("Invalid operation");
	}

	public VarValue pow(VarValue val) throws VarTypeException {
		if (val.getClass().equals(VarInt.class)) {
			return new VarFloat(Math.pow((Long)obj, (Long)val.obj));
		} else if (val.getClass().equals(VarFloat.class)) {
			return new VarFloat(Math.pow((Long)obj, (Double)val.obj));
		}
		throw new VarTypeException("Invalid operation");
	}

	public int toInt() throws VarTypeException {
		try {
			return ((Long) obj).intValue();
		} catch (Exception e) {
			throw new VarTypeException(e);
		}
	}
}
	