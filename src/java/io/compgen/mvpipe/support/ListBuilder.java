package io.compgen.mvpipe.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ListBuilder<T> {
	final private List<T> l;
	public ListBuilder() {
		 l = new ArrayList<T>();
	}
	public ListBuilder(List<T> l) {
		 this.l = l;
	}
	
	public ListBuilder<T> add(T item) {
		l.add(item);
		return this;
	}
	public ListBuilder<T> addAll(Collection<T> items) {
		l.addAll(items);
		return this;
	}
	public List<T> list() {
		return l;
	}
}
