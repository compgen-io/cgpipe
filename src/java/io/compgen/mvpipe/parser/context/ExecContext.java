package io.compgen.mvpipe.parser.context;

import io.compgen.mvpipe.parser.variable.VarNull;
import io.compgen.mvpipe.parser.variable.VarValue;
import io.compgen.mvpipe.support.SimpleFileLoggerImpl;
import io.compgen.mvpipe.support.StringUtils;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ExecContext {
	protected final ExecContext parent;
	protected List<String> curPathList = new ArrayList<String>();

	protected ExecContext() {
		this.parent = null;
	}
	
	public ExecContext(ExecContext parent) {
		if (parent == null) {
			throw new RuntimeException("Parent context is null");
		}
		this.parent = parent;
	}

	protected Map<String, VarValue> vars = new HashMap<String, VarValue>();
	private Log log = LogFactory.getLog(getClass());

	public boolean contains(String name) {
		if (parent != null && parent.contains(name)) {
			return true;
		}	
		return vars.containsKey(name);
	}
	
	public VarValue get(String name) {
		if (!contains(name)) {
			return VarNull.NULL;
		}
		
 		if (parent != null && parent.contains(name)) {
			return parent.get(name);
		}
		return vars.get(name);
	}
	
	public String getString(String k) {
		if (!contains(k)) {
			return null;
		}
		Object obj = get(k);
		return obj.toString();
	}

	public void set(String name, VarValue val) {
		// we pass all of the values back to the root context
		// there are only two variable scopes: global or target
		
		if (parent != null) {
			parent.set(name,  val);
			return;
		}

		vars.put(name, val);
		// handle special cases...
		if (name.equals("mvpipe.log")) {
			try {
				SimpleFileLoggerImpl.setFilename(val.toString());
			} catch (FileNotFoundException e) {
				log .error(e);
			}
		}
	}

	public Map<String, VarValue> cloneValues() {
		return cloneValues(null);

	}
	
	public Map<String, VarValue> cloneValues(String prefix) {
		Map<String, VarValue> vars = new HashMap<String, VarValue>();

		ExecContext cur = this;
		while (cur != null) {
			for (String k:cur.vars.keySet()) {
				if (prefix==null || k.startsWith(prefix)) {
					vars.put(k, cur.vars.get(k));
				}
			}
			cur = cur.parent;
		}

		return vars;
	}
	
	public void update(Map<String, VarValue> vals) {
		for (String k: vals.keySet()) {
			set(k, vals.get(k));
		}
	}
	
	public RootContext getRoot() {
		return parent.getRoot();
	}

	public void dump() {
		System.err.println("[CONTEXT VALS] - " + this);
		for (String k: vars.keySet()) {
			System.err.println("  " + k + " => " + vars.get(k));
		}
		
		System.err.println("  paths:" + StringUtils.join(", ", curPathList));
		
		if (parent != null) {
			parent.dump();
		}
	}

}
