package io.compgen.cgpipe.parser.variable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.compgen.cgpipe.exceptions.MethodCallException;
import io.compgen.cgpipe.exceptions.MethodNotFoundException;
import io.compgen.cgpipe.exceptions.VarTypeException;

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

	@Override
	public int hashCode() {
		return 57 + obj.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) return false;
			if (!(obj instanceof VarValue))
				return false;
			if (obj == this) {
				return true;
			}
			return this.obj.equals(((VarValue)obj).getObject());
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

		/*
		 * The java parseDouble allows for a character at the end, which is strange.
		 * 
		 * For example, 19D => 19.0
		 * 
		 * This isn't what we want, so let's just be explicit.
		 * 
		 * Numbers can only be:
		 * 
		 * [+/-][0-9]+
		 * [+/-][0-9]*.?[0-9]*
		 * 
		 */
		
		boolean isInt = true;
		boolean isDouble = true;
		boolean dots = false;
		for (int i=0; i<val.length() && (isInt || isDouble); i++) {
			char c = val.charAt(i);
			
			if (i != 0) {
				// plus/minus allowed at the first character
				if (c == '-' || c == '+') {
					isInt = false;
					isDouble = false;
					break;
				}
			}
			
			if (c == '.') {
				// only one decimal is allowed, but only for ints.
				if (dots) {
					isInt = false;
					isDouble = false;
					break;
				}
				isInt = false;
				dots = true;
				continue;
			}
			
			switch(c) {
				case '0':
				case '1':
				case '2':
				case '3':
				case '4':
				case '5':
				case '6':
				case '7':
				case '8':
				case '9':
					// numbers are allowed
					continue;
				default:
					// everything else fails.
					isInt = false;
					isDouble = false;
			}
		}

		if (isInt) {
			try {
				long l = Long.parseLong(val);
				return new VarInt(l);
			} catch (NumberFormatException e) {
				// ignore
			}
		} 
		
		if (isDouble) {
			try {
				double d = Double.parseDouble(val);
				return new VarFloat(d);
			} catch (NumberFormatException e) {
				// ignore
			}
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
