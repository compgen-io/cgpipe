package org.ngsutils.mvpipe.runner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ngsutils.mvpipe.exceptions.MissingDependencyException;
import org.ngsutils.mvpipe.exceptions.RunnerException;
import org.ngsutils.mvpipe.exceptions.SyntaxException;
import org.ngsutils.mvpipe.parser.Eval;
import org.ngsutils.mvpipe.parser.context.RootContext;
import org.ngsutils.mvpipe.parser.variable.VarValue;
import org.ngsutils.mvpipe.support.IterUtils;
import org.ngsutils.mvpipe.support.StringUtils;

public abstract class JobRunner {
	abstract public boolean submit(JobDefinition jobdef) throws RunnerException, SyntaxException;
	abstract protected void setConfig(String k, String val);

	protected Log log = LogFactory.getLog(JobRunner.class);
	
	protected boolean dryrun;
	protected boolean done=false;
	protected RootContext globalContext;
	
	protected List<JobDefinition> pendingJobs = new ArrayList<JobDefinition>();
	protected JobDefinition setupJob = null;
	protected JobDefinition teardownJob = null;
	
	protected String preSrc = null;
	protected String postSrc = null;
	
	
	public void done() throws RunnerException, SyntaxException {
		try {
			submitAll(pendingJobs);
		} catch (RunnerException | SyntaxException e) {
			abort();
			throw e;
		}
		done=true;
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

	protected boolean areAllJobDepsSubmitted(JobDefinition job) {
		for (JobDependency dep: job.getDependencies()) {
			if (dep.getJobId() == null) {
				return false;
			}
		}
		
		return true;
	}

	protected void handleSubmit(JobDefinition job) throws RunnerException, SyntaxException {
		if (job.getSrc().equals("")) {
			job.setJobId("");
		} else if (job.getSettingBool("job.shexec", false)) {
			if (job.getDependencies().size()>0) {
				throw new RunnerException("Can not run job: "+ job.getName()+" with shexec! It has dependencies!");
			}
			try {
				shexec(job);
			} catch (SyntaxException e) {
				throw new RunnerException(e);
			}
			job.setJobId("");
			logJob(job);
		} else {
			if (!submit(job)) {
				abort();
				throw new RunnerException("Unable to submit job: "+job);
			} else {
				logJob(job);
			}
		}
	}
	
	protected void logJob(JobDefinition job) throws SyntaxException {
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
	
	public void submitAll(List<JobDefinition> jobs) throws RunnerException, SyntaxException {
		if (setupJob != null) {
			handleSubmit(setupJob);
			setupJob = null;
		}
		
		int jobsToSubmit = 1;
		boolean submittedAJob = false;

		while (jobsToSubmit > 0) {
			jobsToSubmit = 0;
			submittedAJob = false;
			
			for (JobDefinition job: jobs) {
				if (job.getJobId() != null) {
					continue;
				}
				if (areAllJobDepsSubmitted(job)) {
					handleSubmit(job);
					submittedAJob = true;
				} else {
					jobsToSubmit ++;
				}
			}
			
			if (!submittedAJob && jobsToSubmit > 0) {
				abort();
				throw new RunnerException("Unable to build dependency tree! Remaining jobs => " + StringUtils.join(",", IterUtils.<JobDefinition>filter(jobs, new IterUtils.Filter<JobDefinition>() {
					@Override
					public boolean filter(JobDefinition jobdef) {
						return jobdef.getJobId() == null;
					}})));
			}
		}
		if (teardownJob != null) {
			handleSubmit(teardownJob);
			teardownJob = null;
		}
		
	}

	public void buildSetup() throws RunnerException, SyntaxException {
		List<JobDefinition> jobdef = globalContext.findCandidateTarget("__setup__");
		if (setupJob == null && jobdef.size() > 0) {
			setupJob = jobdef.get(0);
		}
	}
	
	public void buildTeardown() throws RunnerException, SyntaxException {
		List<JobDefinition> jobdef = globalContext.findCandidateTarget("__teardown__");
		if (teardownJob == null && jobdef.size() > 0) {
			teardownJob = jobdef.get(0);
		}
	}

	
	public void build(String output) throws RunnerException, SyntaxException {
		buildSetup();
		buildTeardown();
		
		if (output == null) {
			List<String> outputs = globalContext.getDefaultOutputs();
			for (String out: outputs) {
				build(Eval.evalString(out, globalContext));
			}
		} else {
			buildJobTree(output);
		}
	}
	
	private JobDefinition buildJobTree(String target) throws RunnerException, SyntaxException, MissingDependencyException {
		log.info("Building: "+target);
		for (JobDefinition jd: pendingJobs) {
			if (jd.getOutputFilenames().contains(target)) {
				log.debug("Pending job found for: "+target);
				return jd;
			}
		}
		
		List<JobDefinition> jobdefs = globalContext.findCandidateTarget(target);

		if (jobdefs == null || jobdefs.size() == 0) {
			throw new MissingDependencyException("No build target available to build file: "+target);
		}
		
		boolean force = false;
		JobDefinition jobdef = null;
		for (JobDefinition jd: jobdefs) {
			try {
				force = false;
				for (String input: jd.getRequiredInputs()) {
					if (new File(input).exists()) {
						log.info("Input file exists: "+input);
					} else {
						log.trace("Looking for jobdep: "+input);
						JobDefinition dep = buildJobTree(input);
						if (dep != null) {
							jd.addDependency(dep);
							// we have a job dependency that will run... therefore, we need to as well.
							force = true;				
						}
					}
				}
			} catch (MissingDependencyException e) {
				continue;
			}
			jobdef = jd;
			break;
		}

//		List<JobDefinition> extraJobs = new ArrayList<JobDefinition>();
//		String extras = jobdef.getSetting("job.extras","");
//
//		if (extras != null && !extras.equals("")) {
//			for (String extra: extras.split(" ")) {
//				log.trace("Looking for extra job: "+extra);
//				JobDefinition extraJob = buildJobTree(extra);
//				extraJobs.add(extraJob);
//			}
//		}
//	
		// TODO: Check to see if target exists as a file / S3 / existing job / etc...
		if (!force) {
			boolean allfound = true;
			for (String out: jobdef.getOutputFilenames()) {
				if (new File(out).exists()) {
					log.info("Output file exists: "+out);
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

			if (setupJob != null) {
				jobdef.addDependency(setupJob);
			}
			if (teardownJob != null) {
				teardownJob.addDependency(jobdef);
			}
//			for (JobDefinition extra: extraJobs) {
//				if (extra != null) {
//					extra.addDependency(jobdef);
//				}
//			}
		}
		
		return jobdef;
	}
	
	protected void shexec(JobDefinition jobdef) throws SyntaxException {
		try {
			Process proc = Runtime.getRuntime().exec(new String[] { globalContext.contains("mvpipe.shell") ? globalContext.getString("mvpipe.shell"): "/bin/sh"});
			proc.getOutputStream().write(jobdef.getSrc().getBytes(Charset.forName("UTF8")));
			proc.getOutputStream().close();

			InputStream is = proc.getInputStream();
			InputStream es = proc.getErrorStream();

			int retcode = proc.waitFor();
			
			String out = StringUtils.slurp(is);
			String err = StringUtils.slurp(es);

			log.trace("retcode: "+retcode);
			log.trace("stdout: " + out);
			log.trace("stderr: " + err);
			
			is.close();
			es.close();
			
			if (retcode != 0) {
				throw new SyntaxException("Error running job via shexec: "+jobdef.getName());
			}

		} catch (IOException | InterruptedException e) {
			throw new SyntaxException(e);
		}
	}
}
