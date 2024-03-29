package io.compgen.cgpipe.parser.target;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.compgen.cgpipe.exceptions.ASTExecException;
import io.compgen.cgpipe.exceptions.ASTParseException;
import io.compgen.cgpipe.exceptions.VarTypeException;
import io.compgen.cgpipe.loader.NumberedLine;
import io.compgen.cgpipe.parser.TemplateParser;
import io.compgen.cgpipe.parser.context.RootContext;
import io.compgen.cgpipe.parser.variable.VarList;
import io.compgen.cgpipe.parser.variable.VarValue;
import io.compgen.cgpipe.runner.JobDef;
import io.compgen.cgpipe.runner.JobDependency;
import io.compgen.cgpipe.support.FileUtils;
import io.compgen.common.StringUtils;

public class BuildTarget {

	final private List<String> inputs;
	final private List<String> outputs;
	final private List<String> tempOutputs;
	final private String wildcard;
	final private Map<String, VarValue> capturedContext;
	final private List<NumberedLine> lines;
	
//	private Map<String, BuildTarget> skippable = new HashMap<String, BuildTarget>();
//	private long effectiveLastModified = -2; // -2 means we haven't been calculated yet. -1 means the job must be run.
	
	private Map<String, JobDependency> submittedJobDep=new HashMap<String, JobDependency>();

	// for each input (string), what is the target that builds it
	private Map<String, BuildTarget> deps = new HashMap<String, BuildTarget>();
	
	public BuildTarget(List<String> outputs, List<String> inputs, String wildcard, Map<String, VarValue> capturedContext, List<NumberedLine> lines) {
		this.outputs = new ArrayList<String>();
		this.tempOutputs = new ArrayList<String>();
		
		if (outputs != null) {
			for (String o: outputs) {
//				System.out.println(" >>> " + o);
				if (o.startsWith("^")) {
					this.outputs.add(o.substring(1));
					this.tempOutputs.add(o.substring(1));
				} else if (o.startsWith("\\^")) {
					this.outputs.add(o.substring(1));
				} else {
					this.outputs.add(o);
				}
			}
		}
		
		this.inputs = inputs;
		this.wildcard = wildcard;
		this.capturedContext = capturedContext;
		this.lines = lines;
	}

	public String toString() {
		return "<build-target> " + StringUtils.join(";", outputs) + " : " + StringUtils.join(";", inputs);
	}

	public List<String> getInputs() {
		return Collections.unmodifiableList(inputs);
	}

	public List<String> getOutputs() {
		return Collections.unmodifiableList(outputs);
	}

	public List<String> getTempOutputs() {
		return Collections.unmodifiableList(tempOutputs);
	}

	public List<NumberedLine> getLines() {
		return Collections.unmodifiableList(lines);
	}

	public Map<String, BuildTarget> getDepends() {
		return Collections.unmodifiableMap(deps);
	}

	public void addDep(String file, BuildTarget dep) {
		this.deps.put(file, dep);
	}

	public void addDeps(Map<String, BuildTarget> deps) {
		this.deps.putAll(deps);
	}

//	public JobDependency getExisting(String outputName) {
//		return null;
//	}
	
	public JobDef eval(List<NumberedLine> pre, List<NumberedLine> post, RootContext globalRoot) throws ASTParseException, ASTExecException {
		return eval(pre, post, globalRoot, null);
	}
	
	public JobDef eval(List<NumberedLine> pre, List<NumberedLine> post, RootContext globalRoot, Map<String, VarValue> extraVals) throws ASTParseException, ASTExecException {
		RootContext jobRoot = new RootContext(capturedContext, outputs, inputs, wildcard);
		if (!jobRoot.contains("job.custom") || !jobRoot.get("job.custom").isList()) {
			
			VarList l = new VarList();

			if (jobRoot.contains("job.custom")) {
				VarValue tmp = jobRoot.get("job.custom");
				try {
					l.add(tmp);
				} catch (VarTypeException e) {
					throw new ASTExecException(e);
				}
			}
			jobRoot.set("job.custom", l);
			
		}
		if (!jobRoot.contains("job.setup") || !jobRoot.get("job.setup").isList()) {
			
			VarList l = new VarList();

			if (jobRoot.contains("job.setup")) {
				VarValue tmp = jobRoot.get("job.setup");
				try {
					l.add(tmp);
				} catch (VarTypeException e) {
					throw new ASTExecException(e);
				}
			}
			jobRoot.set("job.setup", l);
			
		}

		if (extraVals != null) {
			for (String k: extraVals.keySet()) {
				jobRoot.set(k,  extraVals.get(k));
			}
		}

		if (globalRoot != null) {
			for (BuildTargetTemplate btg: globalRoot.getImportableTargets()) {
				jobRoot.addImportTarget(btg);
			}
		}

		TemplateParser.parseTemplate(lines, pre, post, jobRoot);
		List<String> tempOutputs = new ArrayList<String>();
		for (String output: outputs) {
			if (isTempOutput(output)) {
				tempOutputs.add(output);
			}
		}

		return new JobDef(jobRoot.getBody(), jobRoot.cloneValues(), outputs, inputs, tempOutputs);
	}
//
//	public boolean isSkippable(String out) {
//		if (!this.outputs.contains(out)) {
//			return true;
//		}
//		
//		for (String k : skippable.keySet()) {
//			LogFactory.getLog(BuildTarget.class).trace("++++++++++++ skip: " + k + " => " + skippable.get(k));			
//		}
//		
//		boolean canSkip = true;
//		if (!skippable.containsKey(out)) {
//			LogFactory.getLog(BuildTarget.class).trace("++++++++++++ Skippable in build-target? NO =>  " + this + " (missing:" + out+")");
//			canSkip = false;
//		} else if (skippable.get(out) != null) {
//			BuildTarget tgt = skippable.get(out);
//			
//			for (String inp: inputs) {
//				if (!tgt.isSkippable(inp)) {
//					LogFactory.getLog(BuildTarget.class).trace("++++++++++++ Skippable in build-target? NO =>  " + this + " (missing: "+ out +" and input is required: "+inp+")");
//					canSkip = false;
//				}
//			}
//		}
//
//		//		LogFactory.getLog(BuildTarget.class).debug("++++++++++++ Skippable in build-target? yes " + StringUtils.join(",", outputs) + " ? " + this.hashCode());
//
//		return canSkip;
//	}

//	public void setSkippable(String output) {
//		LogFactory.getLog(BuildTarget.class).debug(this + " setting output as skippable: " + output);
//		
//		this.skippable.put(output, null);
//	}
//
//	public void setSkippable(String output, BuildTarget parent) {
//		LogFactory.getLog(BuildTarget.class).debug(this + " setting output as skippable: " + output + " (depends on target: "+ parent+ ")");
//		
//		this.skippable.put(output, parent);
//	}

	public void setSubmittedJobDep(JobDependency jobDep, List<String> outputs) {
		for (String output: outputs) {
			this.submittedJobDep.put(output,  jobDep);
		}
	}

	public JobDependency getJobDep(String output) {
		return this.submittedJobDep.get(output);
	}

	public long getEffectiveLastModified(String output) {
		long age = -2;
		
		for (String input: deps.keySet()) {
			long depAge = deps.get(input).getEffectiveLastModified(input);
			if (age == -2 || depAge > age) {
				age = depAge;
			}
		}

		if (!tempOutputs.contains(output)) {
			long fileAge = FileUtils.find(output).getLastModifiedTime();
			if (age == -2 || fileAge > age) {
				age = fileAge;
			}
		}
		
//		System.out.println("getEffectiveLastModified("+output+")="+age);

	
		if (age == -2) {
			return age = -1;
		}
		
		return age;
		
	}

	public boolean isTempOutput(String output) {
//		System.out.println("Is temp? " + output + " ? " + tempOutputs.contains(output));
//		System.out.println("Temps:" + StringUtils.join(",",tempOutputs) );
		return tempOutputs.contains(output);

	}
	
//	public void setEffectiveLastModified(long effectiveLastModified) {
//		this.effectiveLastModified = effectiveLastModified;
//	}
//
}
