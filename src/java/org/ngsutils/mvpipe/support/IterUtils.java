package org.ngsutils.mvpipe.support;

import java.util.ArrayList;
import java.util.List;

public class IterUtils {
	public interface Filter<T> {
		public boolean filter(T val);
	}

	public static <T> Iterable<T> filter(Iterable<T> l, Filter<T> filter) {
		List<T> ret = new ArrayList<T>();
		for (T val: l) {
			if (filter.filter(val)) {
				ret.add(val);
			}
		}
		return ret;
	}
	
}	
