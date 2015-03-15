package org.ngsutils.mvpipe.parser.context;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ngsutils.mvpipe.parser.target.BuildTarget;
import org.ngsutils.mvpipe.parser.target.BuildTargetTemplate;
import org.ngsutils.mvpipe.parser.target.FileExistsBuildTarget;
import org.ngsutils.mvpipe.parser.variable.VarValue;


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

	public void pushCWD(String path) {
		curPathList.add(0,path);
	}

	public void popCWD() {
		curPathList.remove(0);
	}

	public File findFile(String filename) {
		File f = new File(filename);
		if (f.exists()) {
			return f;
		}
		
		for (String path: curPathList) {
			f = new File(path, filename);
			if (f.exists()) {
				return f;
			}
		}
		
		return null;
	}
}	
