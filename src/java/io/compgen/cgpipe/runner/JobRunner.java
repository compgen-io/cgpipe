package io.compgen.cgpipe.runner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.compgen.cgpipe.exceptions.ASTExecException;
import io.compgen.cgpipe.exceptions.ASTParseException;
import io.compgen.cgpipe.exceptions.RunnerException;
import io.compgen.cgpipe.exceptions.VarTypeException;
import io.compgen.cgpipe.loader.NumberedLine;
import io.compgen.cgpipe.parser.context.RootContext;
import io.compgen.cgpipe.parser.target.BuildTarget;
import io.compgen.cgpipe.parser.variable.VarBool;
import io.compgen.cgpipe.parser.variable.VarList;
import io.compgen.cgpipe.parser.variable.VarString;
import io.compgen.cgpipe.parser.variable.VarValue;
import io.compgen.cgpipe.runner.joblog.JobLog;
import io.compgen.cgpipe.runner.joblog.JobLogRecord;
import io.compgen.cgpipe.support.StreamRedirect;
import io.compgen.common.MapBuilder;
import io.compgen.common.StringUtils;

public abstract class JobRunner {
	abstract public boolean submit(JobDef jobdef) throws RunnerException;
	abstract public boolean isJobIdValid(String jobId) throws RunnerException;
	abstract public void runnerDone() throws RunnerException;
	abstract protected void setConfig(String k, VarValue varValue);

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
	
	protected JobLog joblog = null;
	protected Map<String, JobDependency> submittedJobs = new HashMap<String, JobDependency>();	 // key = output-file, value = job-id
	protected List<JobDependency> submittedJobDefs = new ArrayList<JobDependency>();

	protected RootContext rootContext = null;
	
	protected JobDef setupJob = null;

	protected List<NumberedLine> prelines=null;
	protected List<NumberedLine> postlines=null;
//	private List<NumberedLine> postSubmitLines=null;
	
	protected List<String> outputFilesSubmitted = new ArrayList<String>();
	protected List<String> tempOutputFilesSubmitted = new ArrayList<String>();
	
	public static JobRunner load(RootContext cxt) throws RunnerException {
		
		boolean dryrun = (cxt.get("cgpipe.dryrun") == VarBool.TRUE);
		
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
		case "bash":
		case "sh":
			obj = new ShellScriptRunner();
			break;
		case "sge":
			obj = new SGETemplateRunner();
			break;
		case "slurm":
			obj = new SLURMTemplateRunner();
			break;
		case "pbs":
			obj = new PBSTemplateRunner();
			break;
		case "sbs":
			obj = new SBSTemplateRunner();
			break;
		case "graphviz":
			obj = new GraphvizRunner();
			break;
		default:
			throw new RunnerException("Can't load job runner: "+runner +" (valid options: shell, sge, slurm, pbs, sjq, graphviz)");
		}
		
		obj.rootContext = cxt;

		String prefix = "cgpipe.runner."+runner;
		Map<String, VarValue> cxtvals = cxt.cloneValues(prefix);
		for (String k: cxtvals.keySet()) {
			obj.setConfig(k, cxtvals.get(k));
		}
		
		obj.dryrun = dryrun;

		// Attempt to load a list of existing jobs
		
		String joblogFilename = cxt.getString("cgpipe.joblog");
		JobRunner.log.info("job-log: " +joblogFilename);
		if (joblogFilename != null) {
//			try {
				JobLog jl = null;
				try {
					jl = JobLog.open(joblogFilename);
				} catch (IOException e) {
					throw new RunnerException(e);
				}

				for (String output: jl.getOutputJobIds().keySet()) {
					String jobId = jl.getOutputJobIds().get(output);
					String absOutput = Paths.get(output).toAbsolutePath().toString();
					obj.submittedJobs.put(absOutput, new ExistingJob(jobId));
					cxt.getRoot().addPendingJobOutput(absOutput, jobId, obj);
					log.trace("Existing/pending output: "+ absOutput);
				}
				

//				File jobfile = new File(joblog);
//				if (jobfile.exists()) {
//					BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(joblog)));
//					String line;
//					while ((line = reader.readLine()) != null) {
//						String[] cols = line.split("\t");
//						if (cols[1].equals("OUTPUT")) {
//							String absOutput = Paths.get(cols[2]).toAbsolutePath().toString();
//							obj.submittedJobs.put(absOutput, new ExistingJob(cols[0]));
//							cxt.getRoot().addPendingJobOutput(absOutput, cols[0], obj);
//							log.trace("Existing/pending output: "+ absOutput);
//						}
//					}
//					reader.close();
//				} else if (jobfile.getParentFile() != null && !jobfile.getParentFile().exists()) {
//					jobfile.getParentFile().mkdirs();
//				}
				obj.joblog = jl;
				JobRunner.log.debug("done reading job-log: " +joblogFilename);
				//				new PrintStream(new FileOutputStream(joblog, true));
//			} catch (IOException e) {
//				throw new RunnerException(e);
//			}
		}
		return obj;
	}

	public void abort() {
	}

	protected void shexec(JobDef jobdef) throws RunnerException {
		if (dryrun) {
			System.err.println("[dryrun." + jobdef.getSafeName() +"]");
			for (String line: jobdef.getBody().split("\n")) {
				System.err.println("> " + line);
			}
		} else {
			try {
				log.trace("shexec: "+jobdef.getSafeName());
	
				Process proc = Runtime.getRuntime().exec(new String[] { defaultShell });
				proc.getOutputStream().write(jobdef.getBody().getBytes(Charset.forName("UTF8")));
				proc.getOutputStream().close();
	
				InputStream is = proc.getInputStream();
				InputStream es = proc.getErrorStream();
	
				StreamRedirect t1 = new StreamRedirect(is, System.out);
				t1.start();
	
				StreamRedirect t2 = new StreamRedirect(es, System.err);
				t2.start();
	
				int retcode = proc.waitFor();
				t1.join();
				t2.join();
				
				log.trace("retcode: "+retcode);
				
				is.close();
				es.close();
	
				if (retcode != 0) {
					throw new RunnerException("Error running job via shexec: "+jobdef.getName()+" $? = "+retcode+"\n"+jobdef.getBody());
				}
	
			} catch (IOException | InterruptedException e) {
				throw new RunnerException(e);
			}
		}
	}

//	protected void shexec(String src) throws RunnerException {
//		try {
//			Process proc = Runtime.getRuntime().exec(new String[] { defaultShell });
//			proc.getOutputStream().write(src.getBytes(Charset.forName("UTF8")));
//			proc.getOutputStream().close();
//
//			InputStream is = proc.getInputStream();
//			InputStream es = proc.getErrorStream();
//
//			StreamRedirect t1 = new StreamRedirect(is, System.out);
//			t1.start();
//
//			StreamRedirect t2 = new StreamRedirect(is, System.err);
//			t2.start();
//
//			int retcode = proc.waitFor();
//			t1.join();
//			t2.join();
//			
//			log.trace("retcode: "+retcode);
//			
//			is.close();
//			es.close();
//
//			// don't close stdout/stderr, it stops the program.
//			//fout.close();
//			//ferr.close();
//			
//			if (retcode != 0) {
//				throw new RunnerException("Error running script!");
//			}
//
//		} catch (IOException | InterruptedException e) {
//			throw new RunnerException(e);
//		}
//	}

	private List<NumberedLine> getLinesForTarget(String name, RootContext context, boolean allowMissing) {
		BuildTarget tgt = context.build(name, allowMissing);
		List<NumberedLine> lines = null;
		
		if (tgt != null) {
			lines = tgt.getLines();
		}
		return lines;
	}
	
	private void setup(RootContext context) throws RunnerException {
		if (setupJob == null) {
			BuildTarget setupTgt = context.build("__setup__", true);
			if (setupTgt != null) {
				try {
					setupJob = setupTgt.eval(null,  null, context);
					if (setupJob.getSettingBool("job.shexec", false)) {
						shexec(setupJob);
					} else {
						submit(setupJob);
						if (setupJob.getJobId() == null) {
							abort();
							log.error("Error submitting job: __setup__");
							throw new RunnerException("Error submitting job: __setup__");
						}
						
						postSubmit(setupJob, context);
					}
				} catch (ASTParseException | ASTExecException e) {
					throw new RunnerException(e);
				}
			}

			prelines = getLinesForTarget("__pre__", context, true);
			postlines = getLinesForTarget("__post__", context, true);
		}
	}
	
	public void submitAll(BuildTarget initialTarget, RootContext context) throws RunnerException {
		if (initialTarget.getOutputs() != null && initialTarget.getOutputs().size() > 0) {
			setup(context);
			markSkippable(initialTarget, context, initialTarget.getOutputs().get(0));
			submitTargets(initialTarget, context, initialTarget.getOutputs().get(0), true);
		}
		runOpportunistic(context);
	}

	private void runOpportunistic(RootContext context) throws RunnerException {
		List<BuildTarget> opp = context.getOpportunistic();

		boolean foundAll = true;
		for (BuildTarget tgt: opp) {
			for (String k: tgt.getDepends().keySet()) {
				
				// if the file isn't skippable (exists on disk)
				if (!tgt.getDepends().get(k).isSkippable()) {
					// if we haven't submitted the job
					if (tgt.getDepends().get(k).getJobDep() == null) {
						// if the job hasn't been previously scheduled
						if (findJobProviding(k) == null) {
							// we can't run this opportunistic job
							foundAll = false;
							continue;
						}
					}
				}
			}

			if (!foundAll) {
				continue;
			}
			
			try {
				JobDef job = tgt.eval(null,  null, context);
				if (job.getSettingBool("job.shexec", false)) {
					shexec(job);
				} else {
					submit(job);
				}
			} catch (ASTParseException | ASTExecException e) {
				throw new RunnerException(e);
			}
		}
		
	}
	private long markSkippable(BuildTarget target, RootContext context, String outputName) throws RunnerException {
		long lastModified = 0;
		String lastModifiedDep = "";
		for (String dep: target.getDepends().keySet()) {
			long depLastMod = markSkippable(target.getDepends().get(dep), context, dep);
			log.debug("Checking dep: " + dep + " lastmod: "+depLastMod);
			if (depLastMod == -1) {
				lastModified = -1;
			} else if (depLastMod > lastModified) {
				lastModified = depLastMod;
				lastModifiedDep = dep;
			}
		}
		
		long retval = 0;
		if (lastModified > -1) {
			// Check to see if the outputName file exists on disk.
			// Note: this could also be used to look for remote resources (S3, etc), but not implemented
			for (String allout: target.getOutputs()) {
				File outputFile = new File(allout);
				if (outputFile.exists()) {
					if (outputFile.lastModified() >= lastModified) {
						log.debug("Marking output-target as skippable: "+allout);
						target.setSkippable(allout);
						if (retval != -1 && outputFile.lastModified() > retval) {
							retval = outputFile.lastModified();
						}
					} else {
						log.debug("Marking output-target as not skippable: " + allout + " is older than " + lastModifiedDep + " (" + outputFile.lastModified() + " vs " + lastModified + ")");
						retval = -1;
					}
				} else {
					if (target.getTempOutputs().contains(allout)) {
						log.debug(outputFile + " is a tmp file -- we can skip this (assuming downstream files are older than: "+lastModified+")");
						return lastModified;
					} else {
						log.debug("Marking output-target as not skippable: " + allout + " doesn't exist! (" + outputFile.getAbsolutePath()+")");
						retval = -1;
					}
				}
			}
		} else {
			log.debug("Marking output-target as not skippable: "+outputName + " a dependency will be built");
			retval = -1;
		}
		return retval;
	}

	private JobDependency submitTargets(BuildTarget target, RootContext context, String outputName, boolean isRoot) throws RunnerException {
		log.trace("Submitting target: "+outputName);

		// Can we skip this target (file exists)
		if (target.isSkippable()) {
			log.trace("Skipping target: "+outputName);
			return null;
		}
		
		// Has it already been submitted in another part of the tree?
		if (target.getJobDep() != null) {
			log.trace("Skipping target (already submitted): "+outputName);
			return target.getJobDep();
		}

		// Have we already submitted this job in a prior run?
		JobDependency depJob = findJobProviding(outputName);
		if (depJob != null) {
			log.trace("Skipping target (job queued): "+outputName);
			return depJob;
		}
		
		// Okay... we are submitting this job, start with submitting it's dependencies...
		
		List<JobDependency> deps = new ArrayList<JobDependency>();

		try {
			JobDef job = target.eval(prelines, postlines, context);
			if (job != null) {
				boolean blankRoot = false;
				if (isRoot) {
					String tmp = job.getBody().replaceAll("[ \\t\\r\\n]", "");
					if (tmp.equals("")) {
						blankRoot = true;
					}
				}
	
				for (String out: target.getDepends().keySet()) {
					log.info("Submitting dependency: "+out);
					JobDependency dep = submitTargets(target.getDepends().get(out), context, out, blankRoot);
					if (dep != null) {
						deps.add(dep);
					} else {
						log.debug("Dependency not found?: "+out);
					}
				}
			
				job.addDependencies(deps);
				
				if (setupJob != null && setupJob.getJobId() != null) {
					job.addDependency(setupJob);
				}
				
				if (!blankRoot) {
					if (job.getDependencies().size()==0 && job.getSettingBool("job.shexec", false)) {
						shexec(job);
					} else {
						submit(job);
	
						if (job.getJobId() == null) {
							abort();
							log.error("Error submitting job: "+ target);
							throw new RunnerException("Error submitting job: "+job);
						}
						
						postSubmit(job, context);
	
						this.outputFilesSubmitted.addAll(target.getOutputs());
						this.tempOutputFilesSubmitted.addAll(target.getTempOutputs());
	
						for (String out: target.getOutputs()) {
							submittedJobs.put(Paths.get(out).toAbsolutePath().toString(), job);
						}
					}
				} else {
					log.debug("Skipping empty target: "+target);
					job.setJobId("");
				}
				
				target.setSubmittedJobDep(job);
				this.submittedJobDefs.add(job);
			
			} else {
				log.debug("Empty job for target: "+target);
			}
			return job;
		} catch (ASTParseException | ASTExecException e) {
			abort();
			throw new RunnerException(e);
		}
	}

	private JobDependency findJobProviding(String input) throws RunnerException {
		log.trace("Looking for output: "+ input);

		String absInput = Paths.get(input).toAbsolutePath().toString();
		
		if (submittedJobs.containsKey(absInput)) {
			log.debug("Found existing job providing: "+ absInput + " ("+submittedJobs.get(absInput).getJobId()+")");
			JobDependency job = submittedJobs.get(absInput);
			
			if (isJobIdValid(job.getJobId())) {
				return job;
			}

			log.debug("Existing job: "+ job.getJobId()+" is no longer valid... resubmitting");
			submittedJobs.remove(absInput);
		}
		
		return null;
	}
	
	public void done() throws RunnerException {
		
		// look for a __teardown__ target and execute if found.
		JobDef teardown = null;
		
		// TODO: Move this lower? And add all of the job defs to the context?
		//       (Like -- show the final outputs and temp. files...)
		
		BuildTarget tdTgt = rootContext.build("__teardown__", true);
		if (tdTgt!=null) {
			boolean teardownBlank = false;
			try {
				//System.err.println("ALL OUTPUTS :  "+StringUtils.join(",",outputFilesSubmitted));
				//System.err.println("TEMP-OUTPUTS: "+StringUtils.join(",",tempOutputFilesSubmitted));
				
				MapBuilder<String, VarValue> mb = new MapBuilder<String, VarValue>();
				
				VarString[] tmpFileVar = new VarString[tempOutputFilesSubmitted.size()];
				for (int i=0; i<tempOutputFilesSubmitted.size(); i++) {
					tmpFileVar[i] = new VarString(tempOutputFilesSubmitted.get(i));
				}
				
				try {
					mb.put("cgpipe.tmpfiles", new VarList(tmpFileVar));
				} catch (VarTypeException e) {
					throw new RunnerException(e);
				}

				VarString[] tmpFileVar2 = new VarString[outputFilesSubmitted.size()];
				for (int i=0; i<outputFilesSubmitted.size(); i++) {
					tmpFileVar2[i] = new VarString(outputFilesSubmitted.get(i));
				}
					
				try {
					mb.put("cgpipe.outputfiles", new VarList(tmpFileVar2));
				} catch (VarTypeException e) {
					throw new RunnerException(e);
				}

				teardown = tdTgt.eval(null,  null, rootContext, mb.build());
				
				String tmp = teardown.getBody().replaceAll("[ \\t\\r\\n]", "");
				if (tmp.equals("")) {
					teardownBlank = true;
				}


			} catch (ASTParseException | ASTExecException e) {
				throw new RunnerException(e);
			}
			
			if (!teardownBlank) {
				if (teardown.getSettingBool("job.shexec", false)) {
					shexec(teardown);
				} else {
					teardown.addDependencies(submittedJobDefs);
					if (setupJob != null && setupJob.getJobId() != null) {
						teardown.addDependency(setupJob);
					}
					submit(teardown);
					if (teardown.getJobId() == null) {
						abort();
						log.error("Error submitting job: __teardown__");
						throw new RunnerException("Error submitting job: __teardown__");
					}
					
					postSubmit(teardown, rootContext);
				}
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
			log.debug("src: "+StringUtils.strip(s));
		}

		if (!dryrun && joblog != null && job.getJobId() != null && !job.getJobId().equals("")) {
			JobLogRecord rec = new JobLogRecord(job.getJobId());
			rec.setName(job.getName());
			rec.setSubmitTime(System.currentTimeMillis());
			rec.setUser(System.getProperty("user.name"));
			
			
			for (JobDependency dep:job.getDependencies()) {
				if (job.getJobId()!=null && !job.getJobId().equals("")) {
					rec.addDep(dep.getJobId());
				}
			}
			for (String out:job.getOutputs()) {
				rec.addOutput(out);
			}
			for (String inp:job.getInputs()) {
				rec.addInput(inp);
			}
			for (String s: job.getBody().split("\n")) {
				rec.addSrcLine(s);
			}
			for (String k:job.getSettings()) {
				if (k.startsWith("job.")) {
					if (k.equals("job.custom")) {
						for (String s: job.getSettings("job.custom")) {
							rec.addSetting(k, s);
						}
					} else {
						rec.addSetting(k, job.getSetting(k));
					}
				}
			}
			joblog.writeRecord(rec);
		}
	}
	public void postSubmit(JobDef jobdef, RootContext context) throws RunnerException {
		BuildTarget postSubmitTgt = context.build("__postsubmit__", true);
		if (postSubmitTgt != null) {
			try {
				RootContext jobRoot = new RootContext();
				for (String setting: jobdef.getSettings()) {
					if (setting.startsWith("job.")) {
						jobRoot.set(setting, jobdef.getSettingsMap().get(setting));
					}
				}
				jobRoot.set("job.id", new VarString(jobdef.getJobId()));
				String deps = "";
				for (JobDependency dep: jobdef.getDependencies()) {
					if (!deps.equals("")) {
						deps += ":";
					}
					deps += dep.getJobId();
				}
				jobRoot.set("job.depids", new VarString(deps));

				JobDef postSubmit = postSubmitTgt.eval(null,  null, null, jobRoot.cloneValues());
				//System.err.println(postSubmit.getBody());
				shexec(postSubmit);

			} catch (ASTParseException | ASTExecException e) {
				throw new RunnerException(e);
			}
		}
	
	}
}
