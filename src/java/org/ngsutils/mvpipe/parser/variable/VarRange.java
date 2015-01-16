package org.ngsutils.mvpipe.parser.variable;

import java.util.Iterator;

import org.ngsutils.mvpipe.parser.SyntaxException;

public class VarRange extends VarValue {
	final private VarValue from;
	final private VarValue to;

	public VarRange(VarValue from, VarValue to) {
		super(null);
		this.from = from;
		this.to = to;
	}

	public String toString() {
		return "VarRange("+from+","+to+")";
	}
	
	public Iterable<VarValue> iterate() throws SyntaxException {
		final long start;
		final long end;
		
		if (from.getClass().equals(VarInt.class)) {
			start = (Long)from.obj;
		} else if (from.getClass().equals(VarFloat.class)) {
			start = ((Double)from.obj).longValue();
		} else {
			throw new SyntaxException("Range needs to be on a number!");
		}
		

		if (to.getClass().equals(VarInt.class)) {
			end = (Long)to.obj;
		} else if (to.getClass().equals(VarFloat.class)) {
			end = ((Double)to.obj).longValue();
		} else {
			throw new SyntaxException("Range needs to be on a number!");
		}
		
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
}
