package org.ngsutils.mvpipe.runner;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ngsutils.mvpipe.exceptions.RunnerException;
import org.ngsutils.mvpipe.exceptions.SyntaxException;
import org.ngsutils.mvpipe.parser.context.RootContext;
import org.ngsutils.mvpipe.parser.variable.VarValue;
import org.ngsutils.mvpipe.support.StringUtils;

public abstract class JobRunner {
	abstract public boolean submit(JobDefinition jobdef) throws RunnerException;
	abstract protected void setConfig(String k, String val);

	protected Log log = LogFactory.getLog(getClass());
	
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

	public static JobRunner load(RootContext cxt, boolean dryrun) throws RunnerException {
		String runner = cxt.getString("mvpipe.runner");
		if (runner == null) {
			runner = "shell";
		}
		
		JobRunner obj = null;
		switch (runner) {
		case "shell":
			obj = new ShellScriptRunner();
			break;
		case "sge":
			obj = new SGERunner();
			break;
		default:
			throw new RunnerException("Can't load job runner: "+runner);
		}

		String prefix = "mvpipe.runner."+runner;
		Map<String, VarValue> cxtvals = cxt.cloneValues(prefix);
		for (String k: cxtvals.keySet()) {
			obj.setConfig(k, cxtvals.get(k).toString());
		}
		
		obj.dryrun = dryrun;
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
					if (job.getSrc().equals("")) {
						job.setJobId("");
					} else if (!submit(job)) {
						abort();
						throw new RunnerException("Unable to submit job: "+job);
					}
					submittedAJob = true;
					if (!job.getJobId().equals("")) {
						log.info("Submitted job: "+job.getJobId() +" "+ job.getName());
						for (String k:job.getSettings()) {
							if (k.startsWith("job.")) {
								log.debug("setting: "+k+" => "+job.getSetting(k));
							}
						}
						for (String out:job.getOutputFilenames()) {
							log.debug("output: "+out);
						}
						for (String inp:job.getRequiredInputs()) {
							log.debug("input: "+inp);
						}
						for (String s: job.getSrc().split("\n")) {
							log.info("src: "+StringUtils.strip(s));
						}
					}
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
		log.debug("Building job tree for: "+target);
		for (JobDefinition jd: pendingJobs) {
			if (jd.getOutputFilenames().contains(target)) {
				log.debug("Pending job found for: "+target);
				return jd;
			}
		}
		
		JobDefinition jobdef = globalContext.findBuildTarget(target);

		if (jobdef == null) {
			throw new RunnerException("No build target available to build file: "+target);
		}
		
		boolean force = false;
		
		for (String input: jobdef.getRequiredInputs()) {
			log.trace("Looking for jobdep: "+input);
			JobDefinition dep = buildJobTree(input);
			if (dep != null) {
				jobdef.addDependency(dep);
				
				// we have a job dependency that will run... therefore, we need to as well.
				force = true;				
			}
		}

		// TODO: Check to see if target exists as a file / S3 / existing job / etc...
		if (!force) {
			boolean allfound = true;
			for (String out: jobdef.getOutputFilenames()) {
				if (new File(out).exists()) {
					log.debug("Output file exists: "+out);
				} else {
					allfound = false;
					break;
				}
			}
			if (allfound) {
				log.debug("All output files found, not building job script");
				jobdef = null;
			}
		}

		if (jobdef != null) {
			pendingJobs.add(jobdef);
		
			for (String extra: jobdef.getExtraTargets()) {
				log.trace("Looking for extra job: "+extra);
				JobDefinition dep = buildJobTree(extra);
				if (dep != null && jobdef != null) {
					dep.addDependency(jobdef);
				}
			}
		}
		
		return jobdef;
	}
}
