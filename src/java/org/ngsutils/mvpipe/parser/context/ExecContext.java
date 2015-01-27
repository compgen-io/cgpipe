package org.ngsutils.mvpipe.parser.context;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ngsutils.mvpipe.exceptions.SyntaxException;
import org.ngsutils.mvpipe.parser.Eval;
import org.ngsutils.mvpipe.parser.Tokens;
import org.ngsutils.mvpipe.parser.variable.VarValue;

public class ExecContext {
	protected Log log = LogFactory.getLog(getClass());
	protected final ExecContext parent;
	protected String cwd = null;
			
	protected Map<String, VarValue> vars = new HashMap<String, VarValue>();

	private boolean active = true;
	private boolean everActive = true;

	public ExecContext() {
		// this is the root / global context
		this.parent = null;
	}
	
	public ExecContext(Map<String, VarValue> vars) {
		// this is a target context (for-loop)
		this.parent = null;
		this.vars.putAll(vars);
	}

	public ExecContext(ExecContext parent) {
		// this is a child context (for-loop)
		this.parent = parent;
	}

	protected ExecContext(ExecContext parent, boolean active) {
		// this is a child context (for-loop)
		this.parent = parent;
		this.active = active;
		if (!active) {
			this.everActive = false;
		}
	}

	public void setCWD(String cwd) {
		log.debug("set current working directory:"+cwd);
		this.cwd = cwd;
	}
	
	public String getCWD() {
		if (cwd !=null) {
			return cwd;
		} else if (parent != null) {
			return parent.getCWD();
		}
		return null;
	}
	
	public ExecContext getParent() {
		return parent;
	}

	public boolean isActive() {
		if (!active) {
			return false;
		}
		if (parent != null) {
			return parent.isActive();
		}

		return active;
	}

	public boolean wasCurrentLevelEverActive() {
		return everActive;
	}

	public void switchActive() {
		active = !active;
		if (active) {
			everActive = true;
		}
	}
	
	public boolean contains(String name) {
		if (parent != null) {
			if (parent.contains(name)) {
				return true;
			}
		}	
		return vars.containsKey(name);
	}
	
	public VarValue get(String name) {
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
		vars.put(name, val);
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

	public ExecContext addTokenizedLine(Tokens tokens) throws SyntaxException {
		return Eval.evalTokenLine(this,  tokens);
	}

	public void addTarget(BuildTarget target) {
		// this should get funneled to a RootContext
		if (this.parent!=null) {
			parent.addTarget(target);
		}
	}

	public Set<String> keys() {
		Set<String> s = new HashSet<String>();
		populateKeys(s);
		return s;	
	}

	private void populateKeys(Set<String> s) {
		s.addAll(vars.keySet());
		if (parent != null) {
			parent.populateKeys(s);
		}
	}
		
	public VarValue remove(String key) {
		return vars.remove(key);
	}

}
