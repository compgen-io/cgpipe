package io.compgen.cgpipe.parser.context;

import io.compgen.cgpipe.parser.target.BuildTarget;
import io.compgen.cgpipe.parser.target.BuildTargetTemplate;
import io.compgen.cgpipe.parser.target.FileExistsBuildTarget;
import io.compgen.cgpipe.parser.variable.VarNull;
import io.compgen.cgpipe.parser.variable.VarString;
import io.compgen.cgpipe.parser.variable.VarValue;
import io.compgen.common.StringUtils;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class RootContext extends ExecContext {
	
	private List<BuildTargetTemplate> targets = new ArrayList<BuildTargetTemplate>();
	private final List<String> outputs;
	private final List<String> inputs;
	private String body = "";

	private PrintStream outputStream = System.out;
	private Log log = LogFactory.getLog(getClass());

	public RootContext() {
		super();
		this.outputs = null;
		this.inputs = null;
	}

	public RootContext(Map<String, VarValue> init) {
		this(init, null, null);
	}

	public RootContext(Map<String, VarValue> init, List<String> outputs, List<String> inputs) {
		super();
		if (init != null) {
			for (String k: init.keySet()) {
				set(k, init.get(k));
			}
		}
		this.outputs = outputs;
		this.inputs = inputs;
	}

	public void addTarget(BuildTargetTemplate targetDef) {
		log.trace("Adding build-target: " + targetDef);
		this.targets.add(targetDef);
	}
	
	public RootContext getRoot() {
		return this;
	}

	public BuildTarget build() {
		return build(null);
	}
	
	public BuildTarget build(String output) {
		if (output != null) {
			log.debug("Looking for build-target: " + output);
		}

		BuildTarget tgt = null;
		
		for (BuildTargetTemplate tgtdef: targets) {
			tgt = tgtdef.matchOutput(output);
			if (tgt == null) {
				continue;
			}

			if (output == null) {
				output = tgt.getOutputs().get(0);
				log.debug("Looking for build-target: " + output);
			}
			
			Map<String, BuildTarget> deps = new HashMap<String, BuildTarget>();
			
			boolean foundAllInputs = true;
			
			for (String input: tgt.getInputs()) {
				if (input == null) {
					log.error("Required input is null: "+ input + " (from "+output+")");
					foundAllInputs = false;
					break;
				}
				log.debug("Looking for required input: "+ input + " (from "+output+")");
				BuildTarget dep = build(input);
				if (dep == null) {
					foundAllInputs = false;
					break;
				}
				deps.put(input, dep);
			}
			
			if (foundAllInputs) {
				tgt.addDeps(deps);
				log.debug("output: "+output+" provider: "+tgt);
				return tgt;
			} else {
				log.debug("Missing a required dependency - attempting to find alternative build path");
			}
		}
		
		if (output!=null && new File(output).exists()) {
			// If we have the build-target for an input, we'll find it above
			// otherwise, if the file exists on disk, we don't necessarily 
			// need to rebuild it. 
			log.debug("File exists on disk: " + output);
			return new FileExistsBuildTarget(output);
		}
		
		return null;
	}
	
	public void addBodyLine(String body) {
		this.body += body+"\n";
	}
	
	public String getBody() {
		return body;
	}
	
	public List<String> getOutputs() {
		return outputs == null ? null: Collections.unmodifiableList(outputs);
	}

	public List<String> getInputs() {
		return inputs == null ? null: Collections.unmodifiableList(inputs);
	}

	public void setOutputStream(PrintStream os) {
		this.outputStream = os;
	}
	
	public void println(String s) {
		if (outputStream != null) {
			outputStream.println(s);
		} else {
			addBodyLine(s);
		}
	}

	@Override
	public boolean contains(String name) {
		if (name.equals("$>") || name.equals("$<")) {
			return true;
		}
		
		if (name.startsWith("$>")) {
			int num = Integer.parseInt(name.substring(2));
			if (outputs.size()>=num) {
				return true;
			}
			return false;
		}
		
		if (name.startsWith("$<")) {
			int num = Integer.parseInt(name.substring(2));
			if (inputs.size()>=num) {
				return true;
			}
			return false;
		}
		
		return super.contains(name);
	}
	
	@Override
	public VarValue get(String name) {
		if (name.equals("$>")) {
			return new VarString(StringUtils.join(" ", outputs));
		}
		if (name.equals("$<")) {
			return new VarString(StringUtils.join(" ", inputs));
		} 
		
		if (name.startsWith("$>")) {
			int num = Integer.parseInt(name.substring(2));
			if (outputs.size()>=num) {
				return new VarString(outputs.get(num-1));
			}
			return VarNull.NULL;
		}
		
		if (name.startsWith("$<")) {
			int num = Integer.parseInt(name.substring(2));
			if (inputs.size()>=num) {
				return new VarString(inputs.get(num-1));
			}
			return VarNull.NULL;
		}
		
		return super.get(name);
	}
}	
