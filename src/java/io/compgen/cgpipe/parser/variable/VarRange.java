package io.compgen.cgpipe.parser.variable;

import io.compgen.cgpipe.exceptions.MethodCallException;
import io.compgen.cgpipe.exceptions.MethodNotFoundException;
import io.compgen.cgpipe.exceptions.VarTypeException;
import io.compgen.common.StringUtils;

import java.util.Iterator;

public class VarRange extends VarValue {
	final private long start;
	final private long end;

	public VarRange(VarValue from, VarValue to) throws VarTypeException {
		super(null);
		
		if (from.getClass().equals(VarInt.class)) {
			start = (Long)from.obj;
		} else if (from.getClass().equals(VarFloat.class)) {
			start = ((Double)from.obj).longValue();
		} else {
			throw new VarTypeException("Range start is not a number!");
		}
		

		if (to.getClass().equals(VarInt.class)) {
			end = (Long)to.obj;
		} else if (to.getClass().equals(VarFloat.class)) {
			end = ((Double)to.obj).longValue();
		} else {
			throw new VarTypeException("Range end is not a number!");
		}
	}

	public String toString() {
		return StringUtils.join(" ", iterate());
	}
	
	public Iterable<VarValue> iterate() {
		
		return new Iterable<VarValue>(){

			@Override
			public Iterator<VarValue> iterator() {
				return new Iterator<VarValue>(){
					
					long i = start;
					VarValue current = new VarInt(start);
					@Override
					public boolean hasNext() {
						return i <= end;
					}

					@Override
					public VarValue next() {
						VarValue ret = current;
						try {
							current = current.add(new VarInt(1));
						} catch (VarTypeException e) {
						}
						i++ ;
						return ret;
					}

					@Override
					public void remove() {
					}};
			}};
	}
	public VarValue call(String method, VarValue[] args) throws MethodNotFoundException, MethodCallException {
		try {
			return super.call(method, args);
		} catch (MethodNotFoundException e1) {
			if (method.equals("length")) {
				if (args.length != 0) {
					throw new MethodCallException("Bad or missing argument! length()");
				}
				return new VarInt(end-start+1);
			}
			throw new MethodNotFoundException("Method not found: "+method+" obj="+this);
		}
	}
}
