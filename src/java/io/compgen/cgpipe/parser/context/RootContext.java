package io.compgen.cgpipe.parser.context;

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

import io.compgen.cgpipe.parser.target.BuildTarget;
import io.compgen.cgpipe.parser.target.BuildTargetTemplate;
import io.compgen.cgpipe.parser.target.ExistingJobBuildTarget;
import io.compgen.cgpipe.parser.target.FileExistsBuildTarget;
import io.compgen.cgpipe.parser.variable.VarNull;
import io.compgen.cgpipe.parser.variable.VarString;
import io.compgen.cgpipe.parser.variable.VarValue;
import io.compgen.cgpipe.runner.JobRunner;
import io.compgen.common.StringUtils;


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

	// File.exists() lookups are expensive (and slow on NFS), so let's cache these requests.
	private Map<String, Boolean> fileCache = new HashMap<String, Boolean>();
	private Map<String, BuildTarget> buildTargetCache = new HashMap<String, BuildTarget>();
	
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
	
	private boolean cachedFileExists(String fname) {
		if (fname == null) {
			return false;
		}
		if (!fileCache.containsKey(fname)) {
			if (new File(fname).exists()) {
				fileCache.put(fname, true);
			} else {
				fileCache.put(fname, false);
			}
		}
		return fileCache.get(fname);
	}
	
	public String getAbsolutePath(String filename) {
		// done in a method so that we can swap in/out other endpoints (S3, etc)
		return Paths.get(filename).toAbsolutePath().toString();
	}
	
	public BuildTarget buildDependencies(BuildTarget tgt, String output, boolean allowMissing) {
		
		Map<String, BuildTarget> deps = new HashMap<String, BuildTarget>();
		
		boolean foundAllInputs = true;
		String missingInput = null;
		
		for (String input: tgt.getInputs()) {
			if (input == null) {
				log.error("Required input is null: "+ input + " (from "+output+")");
				foundAllInputs = false;
				break;
			}
			log.debug("Looking for required input: "+ input + " (from "+output+")");
			BuildTarget dep = cachedBuild(input, allowMissing);
			if (dep == null) {
				foundAllInputs = false;
				missingInput = input;
				break;
			}
			deps.put(input, dep);
		}
		
		if (foundAllInputs) {
			tgt.addDeps(deps);
			log.debug("output: "+output+" provider: "+tgt);
			return tgt;
		} else {
			log.debug("Missing a required dependency ("+missingInput+") - attempting to find alternative build path");
			return null;
		}
	}

	
	/**
	 * These are targets that don't have an output (getOutput.equals(""))
	 * These should always run if possible, but not submit dependencies
	 * @return
	 */
	public List<BuildTarget> getOpportunistic() {
		List<BuildTarget> opp = new ArrayList<BuildTarget>();
		for (BuildTargetTemplate tgtdef: targets) {
			if (tgtdef.getFirstOutput().equals("")) {
				BuildTarget tgt = tgtdef.makeBuildTarget();
				Map<String, BuildTarget> deps = new HashMap<String, BuildTarget>();
				boolean foundAll = true;

				log.debug("Looking for opportunistic inputs: " + StringUtils.join(",", tgt.getInputs()));
				
				for (String input: tgt.getInputs()) {
					if (cachedFileExists(input)) {
						log.debug("found: "+ input + " -- file exists");
						deps.put(input, cachedFileExistsBuildTarget(input));
					} else if (buildTargetCache.containsKey(input)) {
						deps.put(input, buildTargetCache.get(input));
						log.debug("found: "+ input + " -- job defined" + buildTargetCache.get(input).hashCode());
					}  else {
						log.debug("missing: "+ input + " -- no job defined to build and the file doesn't exist");
						
						foundAll = false;
					}
				}
				
				if (foundAll) {
					tgt.addDeps(deps);
					opp.add(tgt);
				}
			}
		}
		return opp;
	}

	
	/**
	 * 
	 * @param output - the output file we are trying to make
	 * @param allowMissing - allow missing input file(s)
	 * @return
	 */
	public BuildTarget build(String rawOutput, boolean allowMissing) {
		
		// temporary file will have '^' as a prefix. This needs to be trimmed
		// away before we actually do something with it.
		String output = null;
		if (rawOutput != null) {
			if (rawOutput.startsWith("^")) {
				output = rawOutput.substring(1);
			} else if (rawOutput.startsWith("\\^")) {
				output = rawOutput.substring(1);
			} else {
				output = rawOutput;
			}
		}

		if (output == null) {
			BuildTarget tgt = null;
			// we need to find the default target and run that.
			// first, we look for a target named "__default__".
			// if that fails, we use the first target that doesn't start with "__"

			boolean foundDefault = false;
			for (BuildTargetTemplate tgtdef: targets) {
				if (tgtdef.getFirstOutput().equals("__default__")) {
					foundDefault = true;
					tgt  = tgtdef.makeBuildTarget();
				}
			}

			if (!foundDefault) {
				for (BuildTargetTemplate tgtdef: targets) {
					if (!tgtdef.getFirstOutput().startsWith("__")) {
						tgt  = tgtdef.makeBuildTarget();
						break;
					}
				}
			}
			
			if (tgt == null) {
				// we don't have a default method...
				return null;
			}
			
			
			BuildTarget t2 = buildDependencies(tgt, output, allowMissing);
			if (t2 != null) {
				return t2;
			}
		}

//		targets.get(0).
		
		for (BuildTargetTemplate tgtdef: targets) {
			BuildTarget tgt = null;

			// for the targets we know about, does this one match the requested output?
			// 
			// This is what lets us have multiple jobs that can make the same outputs.
			// We will scan all targets that can produce an output for a valid input/dep tree.
			// The first target that can successfully produce an output file wins.
			
			tgt = tgtdef.matchOutput(output);
			
			if (tgt == null) {
				// this target doesn't match -- try the next one
				continue;
			}
		
			// Make sure that for this build-target, all inputs are available.
			BuildTarget t2 = buildDependencies(tgt, output, allowMissing);
			if (t2 != null) {
				return t2;
			}

		}
		
		if (cachedFileExists(output)) {
//			if (output!=null && new File(output).exists()) {

			// If any of the inputs required for this output are missing --
			// it will be captured above and we need to submit this job.
			//
			// But -- if the inputs are all present *and* this output exists
			// on disk, then we can just cache the output here.
			
			log.debug("File exists on disk: " + output);
			return cachedFileExistsBuildTarget(output);
		}
		
		if (output != null) {
			
			// If the inputs are valid, but the output file doesn't exist on disk, we might still
			// be able to avoid submitting the job *if* it has already been submitted.
			//
			// Check the submit log.
			
			String absOutput = getAbsolutePath(output);
			log.debug("Looking for build-target: " + output + " ("+absOutput+")");
			if (submittedOutputs.containsKey(absOutput)) {
				if (submittedOutputs.get(absOutput).isJobValid()) {
					log.debug("Found: " + absOutput + " provided by existing/valid job: " + submittedOutputs.get(absOutput).getJobId() );
					return submittedOutputs.get(absOutput);
				} else {
					log.debug("Found: " + absOutput + " provided by existing job: " + submittedOutputs.get(absOutput).getJobId() + ", but it is no longer valid!" );
				}
			}
		}

		if (!allowMissing && this.contains("cgpipe.ignore_missing_inputs")) {
			// If the inputs are valid, but the output file doesn't exist on disk, 
			// a job hasn't been submitted, we can skip the job if "allowMissing" is set

			log.debug("Ignoring missing dependency: " + output);
			return new FileExistsBuildTarget(output);
		}
		
		if (output != null) {
			log.info("Error finding a build path for file: "+output);
		}

		return null;
	}
	
	private BuildTarget cachedFileExistsBuildTarget(String output) {
		if (!buildTargetCache.containsKey(output)) {
			buildTargetCache.put(output, new FileExistsBuildTarget(output));
		}
		
		return buildTargetCache.get(output);
	}

	private BuildTarget cachedBuild(String input, boolean allowMissing) {
		if (input == null) {
			log.info("input is null???");
			return build(input, allowMissing);
		}
		if (!buildTargetCache.containsKey(input)) {
			BuildTarget tgt = build(input, allowMissing);
			if (tgt != null) {
				String foundOutput = null;
				for (String o: tgt.getOutputs()) {
					if (o.equals(input) || o.equals(getAbsolutePath(input))) {
						foundOutput = o;
					}
					buildTargetCache.put(o, tgt);
				}
				if (foundOutput!=null) {
					log.debug("Using fresh BuildTarget for input: " + input + " ? " + foundOutput + " / "+ buildTargetCache.get(foundOutput).hashCode());
					return buildTargetCache.get(foundOutput);
				} else {
					log.debug("Using fresh BuildTarget for input: " + input + ", but missing in output?!?!?!");
				}
			} else {
				log.warn("No build target found for input: "+ input);
			}
		} else {
			log.debug("Using cached BuildTarget for input: " + input + " ? " + buildTargetCache.get(input).hashCode());
		}
		return buildTargetCache.get(input);
	}

	public void addBodyLine(String body) {
		addBodyLine(body, false);
	}

	public void addBodyLine(String body, boolean sameLine) {
		
//		System.err.println("CURRENT BODY: " + this.body);
//		System.err.println("ADDING " + sameLine + ": " + body);
//		
		if (sameLine && this.body.endsWith("\n")) {
			this.body = this.body.substring(0, this.body.length()-1);
		}
		
		this.body += body + "\n";
	}
	
	public String getBody() {
		return body;
	}
	
	public String getWildcard() {
		return wildcard;
	}
	
	public List<String> getOutputs() {
		if (outputs == null) { 
			return null; 
		}

		// need to convert tmp-flagged files to normal names.
		
		List<String> tmp = null;
		for (String o:outputs) {
			if (tmp == null) {
				tmp = new ArrayList<String>();
			}
			if (o.startsWith("^")) {
				tmp.add(o.substring(1));
			} else if (o.startsWith("\\^")) {
					tmp.add(o.substring(1));
			} else {
				tmp.add(o);
			}
		}
		
		if (tmp == null) {
			return null;
		}

		return Collections.unmodifiableList(tmp);
//		return outputs == null ? null: Collections.unmodifiableList(outputs);
	}

	public List<String> getTempOutputs() {
		if (outputs == null) { 
			return null; 
		}
		
		List<String> tmpOutputs = null;
		for (String o:outputs) {
			if (o.startsWith("^")) {
				if (tmpOutputs == null) {
					tmpOutputs = new ArrayList<String>();
				}
				tmpOutputs.add(o.substring(1));
			}
		}

		return Collections.unmodifiableList(tmpOutputs);
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
