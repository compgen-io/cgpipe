package io.compgen.cgpipe.parser.variable;

import io.compgen.cgpipe.exceptions.MethodCallException;
import io.compgen.cgpipe.exceptions.MethodNotFoundException;
import io.compgen.cgpipe.exceptions.VarTypeException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class VarValue {
	final protected Object obj;
	protected VarValue(Object obj) {
		this.obj = obj;
	}

	public boolean toBoolean() {
		return true;
	}

	public boolean isNumber() {
		return false;
	}

	public Object getObject() {
		return obj;
	}
	
	public String toString() {
		if (obj != null) {
			return obj.toString();
		}
		return super.toString();
	}
	
	public VarValue call(String method, VarValue[] args) throws MethodNotFoundException, MethodCallException {
		if (method.equals("type")) {
			return new VarString(this.getClass().getSimpleName().substring(3).toLowerCase());
		}
		throw new MethodNotFoundException("Method not found: "+method+" obj="+this + "/" + this.getClass().getSimpleName());
	}

	public VarValue eq(VarValue val) throws VarTypeException {
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
	
	public static VarValue parseStringRaw(String val) {
		try {
			return parseString(val);
		} catch (VarTypeException e) {
			return new VarString(val);
		}
	}
	
	public static VarValue parseString(String val) throws VarTypeException {
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
				
		if (val.equals("[]")) {
			return new VarList();
		}

		throw new VarTypeException("Unable to parse value: "+val);
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

	public Iterable<VarValue> iterate() {
		// the default for iterate is a single-member list
		List<VarValue> list = new ArrayList<VarValue>();
		list.add(this);
		return Collections.unmodifiableList(list);
	}

	public static VarValue range(VarValue from, VarValue to) throws VarTypeException {
		return new VarRange(from, to);
	}

	public VarInt size() throws VarTypeException {
		return new VarInt(sizeInner());
	}
	
	public int sizeInner() throws VarTypeException {
		throw new VarTypeException("Invalid operation");
	}
	
	public VarValue slice(VarValue startVal, VarValue endVal, boolean hasColon) throws VarTypeException {
		int start;
		int end;

		if (!hasColon) {
			start= startVal.toInt();
			if (start <0) {
	 			start = sizeInner() + start;
	 		}
			return sliceInner(start, start+1);
		}
		
		if (startVal == null) {
			start = 0;
		} else {
			start= startVal.toInt();
			if (start <0) {
	 			start = sizeInner() + start;
	 		}
		}
		
		if (endVal == null) {
			end = sizeInner();
		} else {
			end = endVal.toInt();
			if (end<0) {
				end = sizeInner() + end;
			}
		}


		return sliceInner(start, end);
	}

	public VarValue sliceInner(int start, int end) throws VarTypeException {
		throw new VarTypeException("Invalid operation");
	}
	public int toInt() throws VarTypeException {
		throw new VarTypeException("Invalid operation");
	}

	public boolean isList() {
		return false;
	}
}
