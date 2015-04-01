package io.compgen.cgpipe.parser.context;

import io.compgen.cgpipe.parser.target.BuildTarget;
import io.compgen.cgpipe.parser.target.BuildTargetTemplate;
import io.compgen.cgpipe.parser.target.FileExistsBuildTarget;
import io.compgen.cgpipe.parser.variable.VarValue;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class RootContext extends ExecContext{
	
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
		for (String k: init.keySet()) {
			set(k, init.get(k));
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
		log.trace("Looking for build-target: " + output);

		BuildTarget tgt = null;
		
		for (BuildTargetTemplate tgtdef: targets) {
			tgt = tgtdef.matchOutput(output);
			if (tgt == null) {
				continue;
			}

			if (output == null) {
				output = tgt.getOutputs().get(0);
			}
			
			Map<String, BuildTarget> deps = new HashMap<String, BuildTarget>();
			
			for (String input: tgt.getInputs()) {
				BuildTarget dep = build(input);
				if (dep == null) {
					log.error("Couldn't find target to build: "+ input);
					return null;
				}
				deps.put(input, dep);
			}
			tgt.addDeps(deps);
			log.debug("output: "+output+" provider: "+tgt);
			return tgt;
		}
		
		if (output!=null && new File(output).exists()) {
			// If we have the build-target for an input, we'll find it above
			// otherwise, if the file exists on disk, we don't necessarily 
			// need to rebuild it. 
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
		}
	}
}	
