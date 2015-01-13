package org.ngsutils.mvpipe.context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ngsutils.mvpipe.context.variable.VarValue;
import org.ngsutils.mvpipe.parser.Parser;
import org.ngsutils.mvpipe.parser.Tokenizer;

public class ExecContext {
	protected final ExecContext parent;
	protected Map<String, VarValue> vars = new HashMap<String, VarValue>();

	public ExecContext() {
		// this is the root / global context
		this.parent = null;
	}
	
	public ExecContext(ExecContext parent) {
		// this is a child context (for-loop)
		this.parent = parent;
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
	
	public void set(String name, String val) {
		vars.put(name, VarValue.parseString(val));
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
	
	/**
	 * Parse the line in this context
	 * @param line
	 * @param parser 
	 * @return 
	 */
	public ExecContext parseLine(String line, Parser parser) {
		List<String> tokens = Tokenizer.tokenize(line);
		if (parser.verbose) {
			for (int i=0; i<tokens.size(); i++) {
				if (i>0) {
					System.err.print(";");
				}
				System.err.print(tokens.get(i));
			}
			System.err.println();
		}
		
		return this;
	}
}
