package io.compgen.cgpipe.runner;

import io.compgen.cgpipe.exceptions.ASTExecException;
import io.compgen.cgpipe.exceptions.ASTParseException;
import io.compgen.cgpipe.exceptions.RunnerException;
import io.compgen.cgpipe.loader.NumberedLine;
import io.compgen.cgpipe.parser.context.RootContext;
import io.compgen.cgpipe.parser.target.BuildTarget;
import io.compgen.cgpipe.parser.variable.VarValue;
import io.compgen.common.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class JobRunner {
	abstract public boolean submit(JobDef jobdef) throws RunnerException;
	abstract public boolean isJobIdValid(String jobId) throws RunnerException;
	abstract public void runnerDone() throws RunnerException;
	abstract protected void setConfig(String k, String val);

	public static String defaultShell = null;
	static {
		for (String path: new String[] {"/bin/bash", "/usr/bin/bash", "/usr/local/bin/bash", "/bin/sh"}) {
			if (new File(path).exists()) {
				defaultShell=path;
				break;
			}
		}
	}
	
	static protected Log log = LogFactory.getLog(JobRunner.class);
	
	protected boolean dryrun = false;
	protected boolean done = false;
	
	protected PrintStream joblog = null;
	protected Map<String, JobDependency> submittedJobs = new HashMap<String, JobDependency>();	 // key = output-file, value = job-id

	private JobDef teardown = null;
	protected boolean setupRun = false;

	private List<NumberedLine> prelines=null;
	private List<NumberedLine> postlines=null;

	
	public static JobRunner load(RootContext cxt, boolean dryrun) throws RunnerException {
		String runner = cxt.getString("cgpipe.runner");
		if (runner == null) {
			runner = "shell";
		}

		if (cxt.contains("cgpipe.shell")) {
			defaultShell = cxt.getString("cgpipe.shell");
		}
		
		JobRunner.log.info("job-runner: " +runner);
		JobRunner obj = null;

		switch (runner) {
		case "shell":
			obj = new ShellScriptRunner();
			break;
		case "sge":
			obj = new SGETemplateRunner();
			break;
		case "slurm":
			obj = new SLURMTemplateRunner();
			break;
		case "sjq":
			obj = new SJQRunner();
			break;
		default:
			throw new RunnerException("Can't load job runner: "+runner);
		}
		
		String prefix = "cgpipe.runner."+runner;
		Map<String, VarValue> cxtvals = cxt.cloneValues(prefix);
		for (String k: cxtvals.keySet()) {
			obj.setConfig(k, cxtvals.get(k).toString());
		}
		
		obj.dryrun = dryrun;

		// Attempt to load a list of existing jobs
		String joblog = cxt.getString("cgpipe.joblog");
		JobRunner.log.info("job-log: " +joblog);
		if (joblog != null) {
			try {
				File jobfile = new File(joblog);
				if (jobfile.exists()) {
					BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(joblog)));
					String line;
					while ((line = reader.readLine()) != null) {
						String[] cols = line.split("\t");
						if (cols[1].equals("OUTPUT")) {
							obj.submittedJobs.put(cols[2], new ExistingJob(cols[0]));
						}
					}
					reader.close();
				}
				
				jobfile.getParentFile().mkdirs();
				obj.joblog = new PrintStream(new FileOutputStream(joblog, true));
			} catch (IOException e) {
				throw new RunnerException(e);
			}
		}
		return obj;
	}

	public void abort() {
	}

	protected void shexec(JobDef jobdef) throws RunnerException {
		try {
			Process proc = Runtime.getRuntime().exec(new String[] { defaultShell });
			proc.getOutputStream().write(jobdef.getBody().getBytes(Charset.forName("UTF8")));
			proc.getOutputStream().close();

			InputStream is = proc.getInputStream();
			InputStream es = proc.getErrorStream();

			int retcode = proc.waitFor();
			
			String out = StringUtils.readInputStream(is);
			String err = StringUtils.readInputStream(es);

			log.trace("retcode: "+retcode);
			log.trace("stdout: " + out);
			log.trace("stderr: " + err);
			
			is.close();
			es.close();
			
			if (retcode != 0) {
				throw new RunnerException("Error running job via shexec: "+jobdef.getName());
			}

		} catch (IOException | InterruptedException e) {
			throw new RunnerException(e);
		}
	}

	private List<NumberedLine> getLinesForTarget(String name, RootContext context) {
		BuildTarget tgt = context.build(name);
		List<NumberedLine> lines = null;
		
		if (tgt != null) {
			lines = tgt.getLines();
		}
		return lines;
	}
	
	private void setup(RootContext context) throws RunnerException {
		if (!setupRun) {
			setupRun = true;
			BuildTarget setupTgt = context.build("__setup__");
			if (setupTgt != null) {
				try {
					JobDef setup = setupTgt.eval(null,  null, null);
					if (setup.getSettingBool("job.shexec", false)) {
						if (!dryrun) {
							shexec(setup);
						}
					} else {
						submit(setup);
					}
				} catch (ASTParseException | ASTExecException e) {
					throw new RunnerException(e);
				}
			}

			BuildTarget tdTgt = context.build("__teardown__");
			if (tdTgt!=null) {
				try {
					teardown = tdTgt.eval(null,  null, null);
				} catch (ASTParseException | ASTExecException e) {
					throw new RunnerException(e);
				}
			}

			prelines = getLinesForTarget("__pre__", context);
			postlines = getLinesForTarget("__post__", context);
		}
	}
	
	public void submitAll(BuildTarget initialTarget, RootContext context) throws RunnerException {
		setup(context);
		markSkippable(initialTarget, context, initialTarget.getOutputs().get(0));
		submitTargets(initialTarget, context, initialTarget.getOutputs().get(0), true);
	}

	private long markSkippable(BuildTarget target, RootContext context, String outputName) throws RunnerException {
		long lastModified = 0;
		String lastModifiedDep = "";
		for (String dep: target.getDepends().keySet()) {
			long depLastMod = markSkippable(target.getDepends().get(dep), context, dep);
			if (depLastMod == -1) {
				lastModified = -1;
			} else if (depLastMod > lastModified) {
				lastModified = depLastMod;
				lastModifiedDep = dep;
			}
		}
		
		if (lastModified > -1) {
			// Check to see if the outputName file exists on disk.
			// Note: this could also be used to look for remote resources (S3, etc), but not implemented
			File outputFile = new File(outputName);
			if (outputFile.exists()) {
				if (outputFile.lastModified() > lastModified) {
					log.debug("Marking output-target as skippable: "+outputName);
					target.setSkippable(true);
					return outputFile.lastModified();
				} else {
					log.debug("Marking output-target as not skippable: " + outputName + " is older than " + lastModifiedDep + " (" + outputFile.lastModified() + " vs " + lastModified + ")");
				}
			} else {
				log.debug("Marking output-target as not skippable: " + outputName + " doesn't exist! (" + outputFile.getAbsolutePath()+")");
			}
		} else {
			log.debug("Marking output-target as not skippable: "+outputName + " a dependency will be built");
		}
		target.setSkippable(false);
		return -1;
	}

	private JobDependency submitTargets(BuildTarget target, RootContext context, String outputName, boolean isRoot) throws RunnerException {
		// Can we skip this target (file exists)
		if (target.isSkippable()) {
			return null;
		}
		
		// Has it already been submitted in another part of the tree?
		if (target.getJobDep() != null) {
			return target.getJobDep();
		}

		// Have we already submitted this job in a prior run?
		JobDependency depJob = findJobProviding(outputName);
		if (depJob != null) {
			return depJob;
		}
		
		// Okay... we are submitting this job, start with submitting it's dependencies...
		
		List<JobDependency> deps = new ArrayList<JobDependency>();
		
		try {
			JobDef job = target.eval(prelines, postlines, context);
			
			boolean blankRoot = false;
			if (isRoot) {
				String tmp = job.getBody().replaceAll("[ \\t\\r\\n]", "");
				if (tmp.equals("")) {
					blankRoot = true;
				}
			}

			for (String out: target.getDepends().keySet()) {
				JobDependency dep = submitTargets(target.getDepends().get(out), context, out, blankRoot);
				if (dep != null) {
					deps.add(dep);
				}
			}
		
			job.addDependencies(deps);
			if (!blankRoot) {
				submit(job);
			
				if (job.getJobId() == null) {
					abort();
					log.error("Error submitting job: "+ target);
					throw new RunnerException("Error submitting job: "+job);
				}
	
				logJob(job);
			} else {
				log.debug("Skipping empty target: "+target);
				job.setJobId("");
			}
			
			target.setSubmittedJobDep(job);
			for (String out: target.getOutputs()) {
				submittedJobs.put(out, job);
			}
			return job;
		} catch (ASTParseException | ASTExecException e) {
			abort();
			throw new RunnerException(e);
		}
	}	

	private JobDependency findJobProviding(String input) throws RunnerException {
		log.trace("Looking for output: "+ input);

		if (submittedJobs.containsKey(input)) {
			log.debug("Found existing job providing: "+ input + " ("+submittedJobs.get(input).getJobId()+")");
			JobDependency job = submittedJobs.get(input);
			
			if (isJobIdValid(job.getJobId())) {
				return job;
			}

			log.debug("Existing job: "+ job.getJobId()+" is no longer valid... resubmitting");
			submittedJobs.remove(input);
		}
		
		return null;
	}
	
	public void done() throws RunnerException {
		if (teardown != null) {
			if (teardown.getSettingBool("job.shexec", false)) {
				if (!dryrun) {
					shexec(teardown);
				}
			} else {
				submit(teardown);
			}
		}
		
		runnerDone();

		if (joblog!=null) {
			joblog.close();
		}
	}
	
	protected void logJob(JobDef job) {
		log.info("Submitted job: "+job.getJobId() +" "+ job.getName());
		for (String k:job.getSettings()) {
			if (k.startsWith("job.")) {
				log.debug("setting: "+k+" => "+job.getSetting(k));
			}
		}
		for (String out:job.getOutputs()) {
			log.debug("output: "+out);
		}
		for (String inp:job.getInputs()) {
			log.debug("input: "+inp);
		}
		for (String s: job.getBody().split("\n")) {
			log.info("src: "+StringUtils.strip(s));
		}

		if (joblog != null && job.getJobId() != null && !job.getJobId().equals("")) {
			joblog.println(job.getJobId()+"\t"+"NAME\t"+job.getName());
			joblog.println(job.getJobId()+"\t"+"SUBMIT\t"+System.currentTimeMillis());
			joblog.println(job.getJobId()+"\t"+"USER\t"+System.getProperty("user.name"));
			
			for (JobDependency dep:job.getDependencies()) {
				if (job.getJobId()!=null && !job.getJobId().equals("")) {
					joblog.println(job.getJobId()+"\t"+"DEP\t"+dep.getJobId());
				}
			}
			for (String out:job.getOutputs()) {
				joblog.println(job.getJobId()+"\t"+"OUTPUT\t"+out);
			}
			for (String inp:job.getInputs()) {
				joblog.println(job.getJobId()+"\t"+"INPUT\t"+inp);
			}
			for (String s: job.getBody().split("\n")) {
				joblog.println(job.getJobId()+"\t"+"SRC\t"+s);
			}
			for (String k:job.getSettings()) {
				if (k.startsWith("job.")) {
					if (k.equals("job.custom")) {
						for (String s: job.getSettings("job.custom")) {
							joblog.println(job.getJobId()+"\t"+"SETTING\t"+k+"\t"+s);
						}
					} else {
						joblog.println(job.getJobId()+"\t"+"SETTING\t"+k+"\t"+job.getSetting(k));
					}
				}
			}
		}
	}
}
