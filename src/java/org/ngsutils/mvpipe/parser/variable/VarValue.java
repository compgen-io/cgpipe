package org.ngsutils.mvpipe.parser.variable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.ngsutils.mvpipe.exceptions.SyntaxException;
import org.ngsutils.mvpipe.exceptions.VarTypeException;
import org.ngsutils.mvpipe.parser.Eval;
import org.ngsutils.mvpipe.parser.context.ExecContext;

public abstract class VarValue {
	final protected Object obj;
	protected VarValue(Object obj) {
		this.obj = obj;
	}

	public boolean isTrue() {
		return true;
	}

	public String toString() {
		return obj.toString();
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
	
	public static VarValue parseString(String val) throws VarTypeException {
		return parseString(val, null, false);
	}
	public static VarValue parseString(String val, boolean allowRaw) throws VarTypeException {
		return parseString(val, null, allowRaw);
	}
	public static VarValue parseString(String val, ExecContext cxt) throws VarTypeException {
		return parseString(val, cxt, false);
	}
	public static VarValue parseString(String val, ExecContext cxt, boolean allowRaw) throws VarTypeException {
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
				s = Eval.evalString(s, cxt);
			}
			return new VarString(s);
		}
		
		if (val.equals("[]")) {
			return new VarList();
		}
		
		if (cxt != null) {
			if (cxt.contains(val)) {
				return cxt.get(val);
			}
			// If the value is not set, then we'll assume it should be "NULL"
			return VarNull.NULL;
		} else if (allowRaw) {
			return new VarString(val);
		} else {
			throw new VarTypeException("Unknown variable: "+val);
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

	public Iterable<VarValue> iterate() {
		// the default for iterate is a single-member list
		List<VarValue> list = new ArrayList<VarValue>();
		list.add(this);
		return Collections.unmodifiableList(list);
	}

	public static VarValue range(VarValue from, VarValue to) throws SyntaxException {
		return new VarRange(from, to);
	}
}
