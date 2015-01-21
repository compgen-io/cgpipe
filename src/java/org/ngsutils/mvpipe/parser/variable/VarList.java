package org.ngsutils.mvpipe.parser.variable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.ngsutils.mvpipe.exceptions.VarTypeException;
import org.ngsutils.mvpipe.support.StringUtils;

public class VarList extends VarValue {
	List<VarValue> vals = new ArrayList<VarValue>();

	public VarList() {
		super(null);
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
}
