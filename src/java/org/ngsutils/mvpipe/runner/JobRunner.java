package org.ngsutils.mvpipe.runner;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.ngsutils.mvpipe.exceptions.RunnerException;
import org.ngsutils.mvpipe.exceptions.SyntaxException;
import org.ngsutils.mvpipe.parser.context.RootContext;
import org.ngsutils.mvpipe.parser.variable.VarValue;

public abstract class JobRunner {
	abstract public boolean submit(JobDefinition jobdef);
	abstract protected void setConfig(String k, String val);
	
	protected boolean verbose;
	protected boolean dryrun;
	protected RootContext globalContext;
	
	protected List<JobDefinition> pendingJobs = new ArrayList<JobDefinition>();
	
	public void done() throws RunnerException {
		submitAll(pendingJobs);
	}
	
	public void abort() {
		// no-op
	}

	public boolean isJobIdValid(String jobId) {
		return true;
	}

	public static JobRunner load(RootContext cxt, boolean verbose, boolean dryrun) throws RunnerException {
		String runner = cxt.getString("mvpipe.runner");
		if (runner == null) {
			runner = "bash";
		}
		
		JobRunner obj = null;
		if (runner.equals("shell")) {
			obj = new ShellScriptRunner();
		}

		if (obj == null) {
			throw new RunnerException("Can't load job runner: "+runner);
		}

		String prefix = "mvpipe.runner."+runner;
		Map<String, VarValue> cxtvals = cxt.cloneValues(prefix);
		for (String k: cxtvals.keySet()) {
			obj.setConfig(k, cxtvals.get(k).toString());
		}
		
		obj.dryrun = dryrun;
		obj.verbose = verbose;
		obj.globalContext = cxt;
		
		return obj;
	}

	protected boolean jobDepsSubmitted(JobDefinition job) {
		for (JobDependency dep: job.getDependencies()) {
			if (dep.getJobId() == null) {
				return false;
			}
		}
		
		return true;
	}

	public void submitAll(List<JobDefinition> jobs) throws RunnerException {
		int jobsToSubmit = 1;
		boolean submittedAJob = false;

		while (jobsToSubmit > 0) {
			jobsToSubmit = 0;
			submittedAJob = false;
			
			for (JobDefinition job: jobs) {
				if (job.getJobId() != null) {
					continue;
				}
				if (jobDepsSubmitted(job)) {
					if (!submit(job)) {
						abort();
						throw new RunnerException("Unable to submit job: "+job);
					}
					submittedAJob = true;
				} else {
					jobsToSubmit ++;
				}
			}
			
			if (!submittedAJob && jobsToSubmit > 0) {
				abort();
				throw new RunnerException("Unable to build dependency tree!");
			}
		}
	}
	
	public void build(String output) throws RunnerException, SyntaxException {
		if (output == null) {
			List<String> outputs = globalContext.getDefaultOutputs();
			for (String out: outputs) {
				build(out);
			}
			return;
		}
		
		buildJobTree(output);
	}
	
	private JobDefinition buildJobTree(String target) throws RunnerException, SyntaxException {
		// TODO: Check to see if target exists as a file / S3 / existing job / etc...
		if (new File(target).exists()) {
			return null;
		}

		for (JobDefinition jd: pendingJobs) {
			if (jd.getOutputFilenames().contains(target)) {
				return jd;
			}
		}
		
		JobDefinition jobdef = globalContext.findBuildTarget(target);

		if (jobdef == null) {
			throw new RunnerException("No build target available to build file: "+target);
		}
		
		for (String input: jobdef.getRequiredInputs()) {
			System.err.println("#Looking for jobdep: "+input);
			JobDefinition dep = buildJobTree(input);
			if (dep != null) {
				jobdef.addDependency(dep);
			}
		}

		pendingJobs.add(jobdef);
		
		for (String extra: jobdef.getExtraTargets()) {
			System.err.println("#Looking for extra: "+extra);
			JobDefinition dep = buildJobTree(extra);
			if (dep != null) {
				dep.addDependency(jobdef);
			}
		}
		
		return jobdef;
	}

}
