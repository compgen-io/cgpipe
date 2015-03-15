package org.ngsutils.mvpipe.support;

import java.util.HashMap;
import java.util.Map;

public class MapBuilder<T,U> {
	private Map<T,U> map = new HashMap<T,U>();
	
	public MapBuilder<T,U> put(T foo, U bar) {
		map.put(foo,  bar);
		return this;
	}
	
	public Map<T,U> build() {
		return map;
	}
}
