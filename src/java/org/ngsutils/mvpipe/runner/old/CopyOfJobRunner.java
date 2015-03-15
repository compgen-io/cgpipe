//package org.ngsutils.mvpipe.runner.old;
//
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.io.PrintStream;
//import java.nio.charset.Charset;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
//import org.ngsutils.mvpipe.exceptions.MissingDependencyException;
//import org.ngsutils.mvpipe.exceptions.RunnerException;
//import org.ngsutils.mvpipe.exceptions.SyntaxException;
//import org.ngsutils.mvpipe.parser.Eval;
//import org.ngsutils.mvpipe.parser.context.RootContext;
//import org.ngsutils.mvpipe.parser.variable.VarValue;
//import org.ngsutils.mvpipe.runner.ExistingJob;
//import org.ngsutils.mvpipe.runner.JobDef;
//import org.ngsutils.mvpipe.runner.JobDependency;
//import org.ngsutils.mvpipe.runner.SGERunner;
//import org.ngsutils.mvpipe.runner.ShellScriptRunner;
//import org.ngsutils.mvpipe.support.IterUtils;
//import org.ngsutils.mvpipe.support.StringUtils;
//
//public abstract class CopyOfJobRunner {
//	abstract public boolean submit(JobDefinition jobdef) throws RunnerException, SyntaxException;
//	abstract protected void setConfig(String k, String val);
//
//	protected String defaultShell = "/bin/sh";
//	
//	protected Log log = LogFactory.getLog(CopyOfJobRunner.class);
//	
//	protected boolean dryrun;
//	protected boolean done=false;
//	
//	protected List<JobDefinition> pendingJobs = new ArrayList<JobDefinition>();
//	protected JobDefinition setupJob = null;
//	protected JobDefinition teardownJob = null;
//	
//	protected String preSrc = null;
//	protected String postSrc = null;
//
//	protected PrintStream joblog = null;
//	protected Map<String, String> submittedJobIds = new HashMap<String,String>();	
//	
//	public void done() throws RunnerException {
//		try {
//			submitAll(pendingJobs);
//			if (joblog != null) {
//				joblog.close();
//			}
//
//		} catch (RunnerException e ) {
//			abort();
//			throw e;
//		} catch (SyntaxException e) {
//			abort();
//			throw new RunnerException(e);
//		}
//		done=true;
//	}
//	
//	public void abort() {
//		// no-op
//	}
//
//	public boolean isJobIdValid(String jobId) {
//		return false;
//	}
//
//	public static CopyOfJobRunner load(RootContext cxt, boolean dryrun) throws RunnerException {
//		String runner = cxt.getString("mvpipe.runner");
//		if (runner == null) {
//			runner = "shell";
//		}
//		
//		CopyOfJobRunner obj = null;
//		switch (runner) {
//		case "shell":
//			obj = new ShellScriptRunner();
//			break;
//		case "sge":
//			obj = new SGERunner();
//			break;
//		default:
//			throw new RunnerException("Can't load job runner: "+runner);
//		}
//
//		if (cxt.contains("mvpipe.shell")) {
//			obj.defaultShell = cxt.getString("mvpipe.shell");
//		}
//		
//		String prefix = "mvpipe.runner."+runner;
//		Map<String, VarValue> cxtvals = cxt.cloneValues(prefix);
//		for (String k: cxtvals.keySet()) {
//			obj.setConfig(k, cxtvals.get(k).toString());
//		}
//		
//		obj.dryrun = dryrun;
//
//		String joblog = cxt.getString("mvpipe.joblog");
//		if (joblog != null) {
//			try {
//				File jobfile = new File(joblog);
//				if (jobfile.exists()) {
//					BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(joblog)));
//					String line;
//					while ((line = reader.readLine()) != null) {
//						String[] cols = line.split("\t");
//						if (cols[1].equals("OUTPUT")) {
//							obj.submittedJobIds.put(cols[2], cols[0]);
//						}
//					}
//					reader.close();
//				}
//				
//				jobfile.getParentFile().mkdirs();
//				obj.joblog = new PrintStream(new FileOutputStream(joblog, true));
//			} catch (IOException e) {
//				throw new RunnerException(e);
//			}
//		}
//
//		return obj;
//	}
//
//	protected boolean areAllJobDepsSubmitted(JobDefinition job) {
//		for (JobDependency dep: job.getDependencies()) {
//			if (dep.getJobId() == null) {
//				return false;
//			}
//		}
//		
//		return true;
//	}
//
//	protected void handleSubmit(JobDefinition job) throws RunnerException, SyntaxException {
//		if (job.getSrc().equals("")) {
//			job.setJobId("");
//		} else if (job.getSettingBool("job.shexec", false)) {
//			if (job.getDependencies().size()>0) {
//				throw new RunnerException("Can not run job: "+ job.getName()+" with shexec! It has dependencies!");
//			}
//			try {
//				shexec(job);
//			} catch (SyntaxException e) {
//				throw new RunnerException(e);
//			}
//			job.setJobId("");
//			logJob(job);
//		} else {
//			if (!submit(job)) {
//				abort();
//				throw new RunnerException("Unable to submit job: "+job);
//			} else {
//				logJob(job);
//			}
//		}
//	}
//	
//	protected void logJob(JobDefinition job) throws SyntaxException {
//		log.info("Submitted job: "+job.getJobId() +" "+ job.getName());
//		for (String k:job.getSettings()) {
//			if (k.startsWith("job.")) {
//				log.debug("setting: "+k+" => "+job.getSetting(k));
//			}
//		}
//		for (String out:job.getOutputFilenames()) {
//			log.debug("output: "+out);
//		}
//		for (String inp:job.getRequiredInputs()) {
//			log.debug("input: "+inp);
//		}
//		for (String s: job.getSrc().split("\n")) {
//			log.info("src: "+StringUtils.strip(s));
//		}
//
//		if (joblog != null && job.getJobId() != null && !job.getJobId().equals("")) {
//			joblog.println(job.getJobId()+"\t"+"JOB\t"+job.getName());
//			for (JobDependency dep:job.getDependencies()) {
//				if (job.getJobId()!=null && job.getJobId() != "") {
//					joblog.println(job.getJobId()+"\t"+"DEP\t"+dep.getJobId());
//				}
//			}
//			for (String out:job.getOutputFilenames()) {
//				joblog.println(job.getJobId()+"\t"+"OUTPUT\t"+out);
//			}
//			for (String inp:job.getRequiredInputs()) {
//				joblog.println(job.getJobId()+"\t"+"INPUT\t"+inp);
//			}
//			for (String s: job.getSrc().split("\n")) {
//				joblog.println(job.getJobId()+"\t"+"SRC\t"+s);
//			}
//			for (String k:job.getSettings()) {
//				if (k.startsWith("job.")) {
//					joblog.println(job.getJobId()+"\t"+"SETTING\t"+k+"\t"+job.getSetting(k));
//				}
//			}
//		}
//	}
//	
//	public void submitAll(List<JobDefinition> jobs) throws RunnerException, SyntaxException {
//		if (setupJob != null) {
//			handleSubmit(setupJob);
//			setupJob = null;
//		}
//		
//		int jobsToSubmit = 1;
//		boolean submittedAJob = false;
//
//		while (jobsToSubmit > 0) {
//			jobsToSubmit = 0;
//			submittedAJob = false;
//			
//			for (JobDefinition job: jobs) {
//				if (job.getJobId() != null) {
//					continue;
//				}
//				if (areAllJobDepsSubmitted(job)) {
//					handleSubmit(job);
//					submittedAJob = true;
//				} else {
//					jobsToSubmit ++;
//				}
//			}
//			
//			if (!submittedAJob && jobsToSubmit > 0) {
//				abort();
//				throw new RunnerException("Unable to build dependency tree! Remaining jobs => " + StringUtils.join(",", IterUtils.<JobDefinition>filter(jobs, new IterUtils.FilterFunc<JobDefinition>() {
//					@Override
//					public boolean filter(JobDefinition jobdef) {
//						return jobdef.getJobId() == null;
//					}})));
//			}
//		}
//		if (teardownJob != null) {
//			handleSubmit(teardownJob);
//			teardownJob = null;
//		}
//		
//	}
//
//	public void buildSetup() throws RunnerException, SyntaxException {
//		List<JobDefinition> jobdef = globalContext.findCandidateTarget("__setup__");
//		if (setupJob == null && jobdef.size() > 0) {
//			setupJob = jobdef.get(0);
//		}
//	}
//	
//	public void buildTeardown() throws RunnerException, SyntaxException {
//		List<JobDefinition> jobdef = globalContext.findCandidateTarget("__teardown__");
//		if (teardownJob == null && jobdef.size() > 0) {
//			teardownJob = jobdef.get(0);
//		}
//	}
//
//	
//	public void build(String output) throws RunnerException, SyntaxException {
//		buildSetup();
//		buildTeardown();
//		
//		if (output == null) {
//			List<String> outputs = globalContext.getDefaultOutputs();
//			for (String out: outputs) {
//				parseAST(Eval.evalString(out, globalContext));
//			}
//		} else {
//			buildJobTree(output);
//		}
//	}
//	
//	private JobDefinition buildJobTree(String target) throws RunnerException, SyntaxException, MissingDependencyException {
//		log.info("Building: "+target);
//		for (JobDefinition jd: pendingJobs) {
//			if (jd.getOutputFilenames().contains(target)) {
//				log.debug("Pending job found for: "+target);
//				return jd;
//			}
//		}
//		
//		List<JobDefinition> jobdefs = globalContext.findCandidateTarget(target);
//
//		if (jobdefs == null || jobdefs.size() == 0) {
//			throw new MissingDependencyException("No build target available to build file: "+target);
//		}
//		
//		boolean force = false;
//		JobDefinition jobdef = null;
//		for (JobDefinition jd: jobdefs) {
//			try {
//				force = false;
//				for (String input: jd.getRequiredInputs()) {
//					if (new File(input).exists()) {
//						// file exists, no job needed
//						log.info("Input file exists: "+input);
//					} else {
//						log.trace("Looking for jobdep: "+input);
//						JobDependency dep = null;
//						for (String out: submittedJobIds.keySet()) {
//							if (out.equals(input)) {
//								String depid = submittedJobIds.get(out);
//								if (isJobIdValid(depid)) {
//									dep = new ExistingJob(depid);
//									log.info("Input file being supplied by existing job: "+depid);
//									break;
//								}
//							}
//						}
//						if (dep != null) {
//							jd.addDependency(dep);
//						} else {
//							// we have a job dependency that will run... therefore, we need to as well.
//							dep = buildJobTree(input);
//							jd.addDependency(dep);
//							force = true;				
//						}
//					}
//				}
//			} catch (MissingDependencyException e) {
//				continue;
//			}
//			jobdef = jd;
//			break;
//		}
//
////		List<JobDefinition> extraJobs = new ArrayList<JobDefinition>();
////		String extras = jobdef.getSetting("job.extras","");
////
////		if (extras != null && !extras.equals("")) {
////			for (String extra: extras.split(" ")) {
////				log.trace("Looking for extra job: "+extra);
////				JobDefinition extraJob = buildJobTree(extra);
////				extraJobs.add(extraJob);
////			}
////		}
////	
//		// TODO: Check to see if target exists as a file / S3 / existing job / etc...
//		if (!force) {
//			boolean allfound = true;
//			for (String out: jobdef.getOutputFilenames()) {
//				if (new File(out).exists()) {
//					log.info("Output file exists: "+out);
//				} else {
//					allfound = false;
//					break;
//				}
//			}
//			if (allfound) {
//				log.debug("All output files found, not building job script");
//				jobdef = null;
//			}
//		}
//
//		if (jobdef != null) {
//			pendingJobs.add(jobdef);
//
//			if (setupJob != null) {
//				jobdef.addDependency(setupJob);
//			}
//			if (teardownJob != null) {
//				teardownJob.addDependency(jobdef);
//			}
////			for (JobDefinition extra: extraJobs) {
////				if (extra != null) {
////					extra.addDependency(jobdef);
////				}
////			}
//		}
//		
//		return jobdef;
//	}
//	
//	protected void shexec(JobDef jobdef) throws SyntaxException {
//		try {
//			Process proc = Runtime.getRuntime().exec(new String[] { defaultShell });
//			proc.getOutputStream().write(jobdef.getBody().getBytes(Charset.forName("UTF8")));
//			proc.getOutputStream().close();
//
//			InputStream is = proc.getInputStream();
//			InputStream es = proc.getErrorStream();
//
//			int retcode = proc.waitFor();
//			
//			String out = StringUtils.slurp(is);
//			String err = StringUtils.slurp(es);
//
//			log.trace("retcode: "+retcode);
//			log.trace("stdout: " + out);
//			log.trace("stderr: " + err);
//			
//			is.close();
//			es.close();
//			
//			if (retcode != 0) {
//				throw new SyntaxException("Error running job via shexec: "+jobdef.getName());
//			}
//
//		} catch (IOException | InterruptedException e) {
//			throw new SyntaxException(e);
//		}
//	}
//}
