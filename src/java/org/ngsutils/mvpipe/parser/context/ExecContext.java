package org.ngsutils.mvpipe.parser.context;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ngsutils.mvpipe.parser.Eval;
import org.ngsutils.mvpipe.parser.SyntaxException;
import org.ngsutils.mvpipe.parser.Tokens;
import org.ngsutils.mvpipe.parser.variable.VarValue;

public class ExecContext {
	protected final ExecContext parent;
	protected Map<String, VarValue> vars = new HashMap<String, VarValue>();

	private Pattern varPattern = Pattern.compile("^(.*?)\\$\\{([A-Za-z_\\.][a-zA-Z0-9_\\.]*?)\\}(.*?)$");
	private boolean active = true;
	private boolean everActive = true;

	public ExecContext() {
		// this is the root / global context
		this.parent = null;
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
	
	public void set(String name, VarValue val) {
		vars.put(name, val);
	}

	public Map<String, VarValue> cloneValues() {
		Map<String, VarValue> vars = new HashMap<String, VarValue>();

		ExecContext cur = this;
		while (cur != null) {
			for (String k:cur.vars.keySet()) {
				vars.put(k, cur.vars.get(k));
			}
			cur = cur.parent;
		}

		return vars;
	}

	public String evalString(String msg) {
		String out = "";
		while (msg.length() > 0) {
			Matcher m = varPattern.matcher(msg);
			m = varPattern.matcher(msg);
			if (m.matches()) {
				out += m.group(1);
				if (m.group(1).endsWith("\\")) {
					out += "$";
					msg = m.group(2).substring(1) + m.group(3);
				} else {
					out += get(m.group(2));
					msg = m.group(3);
				}
			} else {
				out += msg;
				msg = "";
			}
		}
		
		return out;
	}

	public ExecContext addTokenizedLine(Tokens tokens) throws SyntaxException {
		return Eval.evalTokenLine(this,  tokens);
	}
}
