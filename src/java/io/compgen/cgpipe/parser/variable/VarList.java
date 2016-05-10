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
				for (VarValue v: ((VarList)val).vals) {
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
			}
			throw new MethodNotFoundException("Method not found: "+method+" obj="+this);
		}
	}

}
