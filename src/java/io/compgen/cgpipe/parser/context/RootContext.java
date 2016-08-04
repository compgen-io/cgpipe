package io.compgen.cgpipe.parser.context;

import io.compgen.cgpipe.exceptions.ASTExecException;
import io.compgen.cgpipe.exceptions.ASTParseException;
import io.compgen.cgpipe.parser.Parser;
import io.compgen.cgpipe.parser.target.BuildTarget;
import io.compgen.cgpipe.parser.target.BuildTargetTemplate;
import io.compgen.cgpipe.parser.target.ExistingJobBuildTarget;
import io.compgen.cgpipe.parser.target.FileExistsBuildTarget;
import io.compgen.cgpipe.parser.variable.VarNull;
import io.compgen.cgpipe.parser.variable.VarString;
import io.compgen.cgpipe.parser.variable.VarValue;
import io.compgen.cgpipe.runner.JobRunner;
import io.compgen.common.StringUtils;

import java.io.File;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class RootContext extends ExecContext {
	
	private List<BuildTargetTemplate> targets = new ArrayList<BuildTargetTemplate>();
	private List<BuildTargetTemplate> importTargets = new ArrayList<BuildTargetTemplate>();
	
	private Map<String, ExistingJobBuildTarget> submittedOutputs = new HashMap<String, ExistingJobBuildTarget>();
	
	private final List<String> outputs;
	private final List<String> inputs;
	private final String wildcard;
	private String body = "";

	private PrintStream outputStream = System.out;
	private Log log = LogFactory.getLog(getClass());

	public RootContext() {
		this(null, null, null, null);
	}

	public RootContext(Map<String, VarValue> init) {
		this(init, null, null, null);
	}

	public RootContext(Map<String, VarValue> init, List<String> outputs, List<String> inputs, String wildcard) {
		super();
		if (init != null) {
			for (String k: init.keySet()) {
				set(k, init.get(k));
			}
		}
		this.outputs = outputs;
		this.inputs = inputs;
		this.wildcard = wildcard;
	}

	public void addTarget(BuildTargetTemplate targetDef) {
		log.info("Adding build-target: " + targetDef);
		this.targets.add(targetDef);
	}
	
	public void addImportTarget(BuildTargetTemplate targetDef) {
		log.info("Adding import-build-target: " + targetDef);
		this.importTargets.add(targetDef);
	}
	
	public RootContext getRoot() {
		return this;
	}

	public BuildTarget build() {
		return build(null, false);
	}

	public BuildTarget build(String output) {
		return build(output, false);
	}
	
	public void addPendingJobOutput(String output, String jobId, JobRunner runner) {
		submittedOutputs.put(output, new ExistingJobBuildTarget(output, jobId, runner));
	}
	
	public BuildTarget build(String output, boolean allowMissing) {
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
				BuildTarget dep = build(input, allowMissing);
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
		
		if (output != null) {
			String absOutput = Paths.get(output).toAbsolutePath().toString();
			log.debug("Looking for build-target: " + output + " ("+absOutput+")");
			if (submittedOutputs.containsKey(absOutput)) {
				if (submittedOutputs.get(absOutput).isJobValid()) {
					log.debug("Found: " + absOutput + " provided by existing/valid job: " + submittedOutputs.get(absOutput).getJobId() );
					return submittedOutputs.get(absOutput);
				} else {
					log.debug("Found: " + absOutput + " provided by existing job: " + submittedOutputs.get(absOutput).getJobId() + ", but it is not a valid job!" );
				}
			}
		}

		if (!allowMissing && this.contains("cgpipe.ignore_missing_inputs")) {
			log.debug("Ignoring missing dependency: " + output);
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
	
	public String getWildcard() {
		return wildcard;
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


	public void dump() {
		super.dump();
		
		System.err.println("[BUILD TARGETS] - " + this);
		for (BuildTargetTemplate tgt: targets) {
			System.err.println("  => " + tgt.getFirstOutput());
		}
		System.err.println("[IMPORT TARGETS] - " + this);
		for (BuildTargetTemplate tgt: importTargets) {
			System.err.println("  => " + tgt.getFirstOutput());
		}
	}

	public List<BuildTargetTemplate> getImportableTargets() {
		List<BuildTargetTemplate> importableTargets = new ArrayList<BuildTargetTemplate>();
		for (BuildTargetTemplate tgt: targets) {
			if (tgt.isImportable()) {
				importableTargets.add(tgt);
			}
		}
		return importableTargets;
	}

	public BuildTarget findImportTarget(String str) {
		for (BuildTargetTemplate tgt: importTargets) {
			if (tgt.isImportable() && tgt.getImportName().equals(str)) {
				return tgt.importTarget();
			}
		}
		return null;
	}
	
}	
