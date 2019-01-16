package io.compgen.cgpipe.parser.variable;

import io.compgen.cgpipe.exceptions.MethodCallException;
import io.compgen.cgpipe.exceptions.MethodNotFoundException;
import io.compgen.cgpipe.exceptions.VarTypeException;
import io.compgen.common.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VarList extends VarValue {
	protected List<VarValue> vals = new ArrayList<VarValue>();
	
	public VarList() {
		super(null);
	}
	
	public VarList(VarValue[] vals) throws VarTypeException {
		super(null);
		for (VarValue val: vals) {
			add(val);
		}
	}

	public VarList(List<VarValue> vals) throws VarTypeException {
		super(null);
		for (VarValue val: vals) {
			add(val);
		}
	}

	public String toString() {
		return StringUtils.join(" ", vals);
	}

	public boolean isList() {
		return true;
	}
	
	public VarValue add(VarValue val) throws VarTypeException {
		for (VarValue v: val.iterate()) {
			vals.add(v);
		}
		return this;
	}

	public VarValue mul(VarValue val) throws VarTypeException {
		if (val.getClass().equals(VarInt.class)) {
			long len = ((VarInt)val).toInt();
			List<VarValue> tmp = new ArrayList<VarValue>();
			for (long i=0; i<len; i++) {
				for (VarValue v: vals) {
					tmp.add(v);
				}
			}
			return new VarList(tmp);
		}
		throw new VarTypeException("Invalid operation");
	}

	
	public Iterable<VarValue> iterate() {
		return Collections.unmodifiableList(vals);
	}
	
	public int sizeInner() {
		return vals.size();
	}
	
	public VarValue sliceInner(int start, int end) throws VarTypeException {
		if (end-start == 1) {
			return vals.get(start);
		}
		return new VarList(this.vals.subList(start,  end));
	}
	

	public VarValue call(String method, VarValue[] args) throws MethodNotFoundException, MethodCallException {
		try {
			return super.call(method, args);
		} catch (MethodNotFoundException e1) {
			if (method.equals("length")) {
				if (args.length != 0) {
					throw new MethodCallException("Bad or missing argument! length()");
				}
				return new VarInt(((List<VarValue>)vals).size());
			} else if (method.equals("contains")) {
				if (args.length != 1) {
					throw new MethodCallException("Bad or missing argument! contains(str)");
				}
				if (vals.contains(args[0])) {
					return VarBool.TRUE;
				}
				return VarBool.FALSE;
			} else if (method.equals("join")) {
				if (args.length != 1) {
					throw new MethodCallException("Bad or missing argument! join(str)");
				}
				
				String s = "";
				for (VarValue val: vals) {
					if (!s.equals("")) {
						s += args[0].toString();
					}
					s += val.toString();
				}
				
				return new VarString(s);
			}

			throw new MethodNotFoundException("Method not found: "+method+" list="+this);
		}
	}

}
