package io.compgen.cgpipe.parser.context;

import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.compgen.cgpipe.CGPipe;
import io.compgen.cgpipe.exceptions.VarTypeException;
import io.compgen.cgpipe.parser.variable.VarBool;
import io.compgen.cgpipe.parser.variable.VarInt;
import io.compgen.cgpipe.parser.variable.VarNull;
import io.compgen.cgpipe.parser.variable.VarString;
import io.compgen.cgpipe.parser.variable.VarValue;
import io.compgen.cgpipe.support.SimpleFileLoggerImpl;
import io.compgen.cgpipe.support.SimpleFileLoggerImpl.Level;

public class ExecContext {
	protected final ExecContext parent;
	public boolean sameBodyLine = false;

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
		// Read-only values
		//
		// Note: Some read-only variables are set in the Eval loop too (pipeline/runtime dependent)
		//
		
		if (name.equals("cgpipe.sys.scriptname")) {
			if (System.getProperty("io.compgen.cgpipe.scriptname")!=null) {
				return new VarString(System.getProperty("io.compgen.cgpipe.scriptname"));
			}
			return new VarString("");
		} else if (name.equals("cgpipe.sys.cwd")) {
			return new VarString(Paths.get("").toAbsolutePath().toString());
		} else if (name.equals("cgpipe.filename")) {
			return new VarString(CGPipe.getFilename());
		} else if (name.equals("cgpipe.dryrun")) {
			return CGPipe.isDryRun() ?  VarBool.TRUE : VarBool.FALSE;
		} else if (name.equals("cgpipe.procs")) {
			return new VarInt(Runtime.getRuntime().availableProcessors());
		}

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
		if (name.equals("cgpipe.log")) {
			try {
				SimpleFileLoggerImpl.setFilename(val.toString());
			} catch (FileNotFoundException e) {
				log.error(e);
			}
		}

		// These are read-only values
		if (name.equals("cgpipe.filename")) {
			return;
		} else if (name.equals("cgpipe.dryrun")) {
			if (val.toBoolean()) {
				CGPipe.setDryRun(); // this can be set once... once you're in dry-run mode, you're always in dry-run mode (no take-backs)
			}
			return;
		} else if (name.equals("cgpipe.procs")) {
			return;
		} else if (name.startsWith("cgpipe.sys.")) {
			return;
		}
		
		
		
		if (name.equals("cgpipe.loglevel")) {
			try {
				int verbosity = val.toInt();
				switch (verbosity) {
				case 0:
					SimpleFileLoggerImpl.setLevel(Level.INFO);
					break;
				case 1:
					SimpleFileLoggerImpl.setLevel(Level.DEBUG);
					break;
				case 2:
					SimpleFileLoggerImpl.setLevel(Level.TRACE);
					break;
				case 3:
				default:
					SimpleFileLoggerImpl.setLevel(Level.ALL);
					break;
				}
			} catch (VarTypeException e) {
				SimpleFileLoggerImpl.setLevel(Level.FATAL);
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

	public Map<String, String> cloneString() {
		return cloneString(null);
	}

	public Map<String, String> cloneString(String prefix) {
		Map<String, String> vars = new HashMap<String, String>();

		ExecContext cur = this;
		while (cur != null) {
			for (String k:cur.vars.keySet()) {
				if (prefix==null || k.startsWith(prefix)) {
					vars.put(k, cur.vars.get(k).toString());
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
		
		if (parent != null) {
			parent.dump();
		}
	}

	public VarValue remove(String varName) {
		if (parent != null && parent.contains(varName)) {
			return parent.remove(varName);
		}
		
		if (vars.containsKey(varName)) {
			return vars.remove(varName);
		}
		return null;
	}

}
