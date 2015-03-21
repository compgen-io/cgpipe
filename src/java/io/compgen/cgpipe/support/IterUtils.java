package io.compgen.cgpipe.support;

import java.util.ArrayList;
import java.util.List;

public class IterUtils {
	public interface MapFunc<T, V> {
		public V map(T obj);
	}

	public interface FilterFunc<T> {
		public boolean filter(T val);
	}

	public static <T> Iterable<T> filter(Iterable<T> l, FilterFunc<T> filter) {
		List<T> ret = new ArrayList<T>();
		for (T val: l) {
			if (filter.filter(val)) {
				ret.add(val);
			}
		}
		return ret;
	}

	public static <T, V> Iterable<V> map(Iterable<T> l, MapFunc<T,V> func) {
		List<V> ret = new ArrayList<V>();
		for (T val: l) {
			V retval = func.map(val);
			if (retval != null) {
				ret.add(retval);
			}
		}
		return ret;
	}

}	
