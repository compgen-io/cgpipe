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
	
	public VarList(VarValue[] vals) {
		super(null);
		for (VarValue val: vals) {
			this.vals.add(val);
		}
	}

	public VarList(List<VarValue> vals) {
		super(null);
		this.vals.addAll(vals);
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

	public Iterable<VarValue> iterate() {
		return Collections.unmodifiableList(vals);
	}
	
	public int sizeInner() {
		return vals.size();
	}
	
	public VarValue sliceInner(int start, int end) {
		if (end-start == 1) {
			return vals.get(start);
		}
		return new VarList(this.vals.subList(start,  end));
	}
	

	public VarValue call(String method, VarValue[] args) throws MethodNotFoundException, MethodCallException {
		if (method.equals("length")) {
			if (args.length != 0) {
				throw new MethodCallException("Bad or missing argument! length()");
			}
			return new VarInt(((List<VarValue>)vals).size());
		}
		throw new MethodNotFoundException("Method not found: "+method+" obj="+this);
	}

}
