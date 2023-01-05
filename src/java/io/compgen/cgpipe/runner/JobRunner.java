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

import io.compgen.cgpipe.CGPipe;
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
import io.compgen.cgpipe.support.FileUtils;
import io.compgen.cgpipe.support.StreamRedirect;
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
	protected List<JobDependency> submittedJobDefs = new ArrayList<JobDependency>(); // this keeps track of the jobs submitted here, for the __teardown__ script

	protected RootContext rootContext = null;
	
	protected JobDef setupJob = null;

	protected List<NumberedLine> prelines=null;
	protected List<NumberedLine> postlines=null;
//	private List<NumberedLine> postSubmitLines=null;
	
//	protected Map<String, Boolean> fileExistsCache = new HashMap<String, Boolean>();
	
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

			obj.joblog = jl;
			JobRunner.log.debug("done reading job-log: " +joblogFilename);

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

	
	/**
	 * Directly loads the NumberedLines for a given target (hard coded, like __pre__)
	 * @param name
	 * @param context
	 * @param allowMissing
	 * @return
	 */
	private List<NumberedLine> getLinesForTarget(String name, RootContext context, boolean allowMissing) {
		BuildTarget tgt = context.build(name, allowMissing);
		List<NumberedLine> lines = null;
		
		if (tgt != null) {
			lines = tgt.getLines();
		}
		return lines;
	}
	
	/** 
	 * Try to find (and submit/execute) the __setup__ job
	 * @param context
	 * @throws RunnerException
	 */
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
//			markSkippable(initialTarget, context, initialTarget.getOutputs().get(0), "", null);
			submitTargets(initialTarget, context, initialTarget.getOutputs().get(0));
		}
		runOpportunistic(context);
	}

	private void runOpportunistic(RootContext context) throws RunnerException {
		List<BuildTarget> opp = context.getOpportunistic();

		for (BuildTarget tgt: opp) {
			boolean foundAll = true;
			log.debug("Checking opportunistic: "+tgt);
			List<String> missing = new ArrayList<String>();
			for (String k: tgt.getDepends().keySet()) {
//				System.out.println("Checking opportunistic dep: "+k);
				log.debug("Checking opportunistic dep: "+k);
				// if the file doesn't exist on disk
				if (!FileUtils.doesFileExist(k)) {
					// if we haven't submitted the job
					if (tgt.getDepends().get(k).getJobDep(k) == null || tgt.getDepends().get(k).getJobDep(k).getJobId()==null || tgt.getDepends().get(k).getJobDep(k).getJobId().equals("")) {
						// if the job hasn't been previously scheduled
						if (findJobProviding(k) == null) {
							// we can't run this opportunistic job
							log.debug("Checking opportunistic: "+k + " => failed");
							missing.add(k);
							foundAll = false;
							continue;
						} else {
							log.debug("Checking opportunistic: "+k + " => exists in job-log");
//							System.out.println("Checking opportunistic: "+k + " => exists in job-log");
						}
					} else {
						log.debug("Checking opportunistic: "+k + " => submitted job");
//						System.out.println("Checking opportunistic: "+k + " => submitted job jobid:\""+ tgt.getDepends().get(k).getJobDep(k).getJobId()+"\"");
					}
				} else {
					log.debug("Checking opportunistic: "+k + " => file exists on disk");
//					System.out.println("Checking opportunistic: "+k + " => file exists on disk");
				}
			}

			if (!foundAll) {
				log.debug("Missing a required input to opportunistic job... "+StringUtils.join(",", missing));
				continue;
			}
			
			log.debug("Submitting opportunistic job");
					
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
	
//	private boolean doesFileExist(File f) {
//		if (fileExistsCache.containsKey(f.getAbsolutePath())) {
//			log.debug("doesFileExist "+f.getAbsolutePath()+" => cached: " + fileExistsCache.get(f.getAbsolutePath()));
//			return fileExistsCache.get(f.getAbsolutePath());
//		}
////		boolean failed = false;
//		for (int i=0; i<3; i++) {
//			if (f.exists()) {
////				if (failed) {
//					log.debug("doesFileExist "+f.getAbsolutePath()+" => exists");
////				}
//				fileExistsCache.put(f.getAbsolutePath(), true);
//				return true;
//			}
//	
//			try {
//				f.toPath().getFileSystem().provider().checkAccess(f.toPath());
//			} catch (NoSuchFileException e) {
//				// force a directory read (this can be an issue on network shares)
//				log.debug("doesFileExist "+f.getAbsolutePath()+" => NoSuchFileException: "+ e);
//				if (f.getParentFile() != null) {
//					f.getParentFile().list();
//				}
////				fileExistsCache.put(f.getAbsolutePath(), false);
////				return false;
//				try {
//					Thread.sleep(100*(i+1));
//				} catch (InterruptedException e1) {
//				}
//			} catch (IOException e) {
//				log.debug("doesFileExist "+f.getAbsolutePath()+" => IOException: "+ e);
////				failed = true;
//				try {
//					Thread.sleep(100*(i+1));
//				} catch (InterruptedException e1) {
//				}
//			}
//		}
//		log.debug("doesFileExist "+f.getAbsolutePath()+" => does not exist (or has bad permissions)");
//		fileExistsCache.put(f.getAbsolutePath(), false);
//		return false;		
//	}
//	
//	private long markSkippable(BuildTarget target, RootContext context, String outputName, String tree, BuildTarget parentTarget) throws RunnerException {
//		if (target.getEffectiveLastModified() > -2) {
//			log.debug(tree + " => LAST MODIFIED: (CACHED) "+ target + " => " + target.getEffectiveLastModified());
//			return target.getEffectiveLastModified();
//		}
//
//		long lastModified = 0;
//		String lastModifiedDep = "";
//		
//		tree="";
//		
//		log.debug(tree + " => MARKING SKIPPABLE FOR: "+ target);
//		
//		for (String dep: target.getDepends().keySet()) {
//			long depLastMod = markSkippable(target.getDepends().get(dep), context, dep, tree+">"+outputName, target);
//			log.debug(tree + " =>   Checking dep: " + dep + " lastmod: "+depLastMod);
//			if (depLastMod == -1) {
//				lastModified = -1;
//			} else if (depLastMod > lastModified && lastModified > -1) {
//				lastModified = depLastMod;
//				lastModifiedDep = dep;
//			}
//		}
//		
//		log.debug(tree + " => LAST MODIFIED: "+ target + " => " + lastModified);
//
//		
//		long retval = 0;
//		if (lastModified > -1) {
//			
//			// if the dependencies return a lastModified > -1, then this target is potentially skip-able.
//			// look for the output on disk, or it might be a transient file.
//			// if lastModified == -1, then this target MUST be built (missing file, or the output is older than a dependency, etc).
//			
//			// Check to see if the outputName file exists on disk.
//			// Note: this could also be used to look for remote resources (S3, etc), but not implemented
//			for (String targetOutputFilename: target.getOutputs()) {
//				log.debug(tree + "     => CHECKING OUTPUT: "+ targetOutputFilename);
//				File targetOutputFile = new File(targetOutputFilename);
//
//				// Note: This can fail for NFS mounted folders
//				//       Hence the extra checks...
//				
//				if (doesFileExist(targetOutputFile)) {
//					if (targetOutputFile.lastModified() >= lastModified) {
//						log.debug(tree + " =>   Marking output-target as skippable: "+targetOutputFilename);
//						target.setSkippable(targetOutputFilename);
//						if (retval != -1 && targetOutputFile.lastModified() > retval) {
//							retval = targetOutputFile.lastModified();
//						}
//					} else {
//						log.debug(tree + " =>   Marking output-target as not skippable: " + targetOutputFilename + " is older than " + lastModifiedDep + " (" + targetOutputFile.lastModified() + " vs " + lastModified + ")");
//						retval = -1;
//					}
//				} else {
//					if (target.getTempOutputs().contains(targetOutputFilename)) {
//						log.debug(tree + " => " + targetOutputFile + " is a tmp file -- we can skip this (assuming downstream files are older than: "+lastModified+")");
//						/// this output is a tmp file, so we assume the output is the same as any of it's dependencies.
//						target.setSkippable(targetOutputFilename, parentTarget);
//						if (lastModified > retval) {
//							retval = lastModified;
//						}
//					} else {
//						log.debug(tree + " =>   Marking output-target as not skippable: " + targetOutputFilename + " doesn't exist! (" + targetOutputFile.getAbsolutePath()+")");
//						retval = -1;
//					}
//				}
//			}
//		} else {
//			log.debug(tree + " =>   Marking output-target as not skippable: "+outputName + " a dependency will be built");
//			retval = -1;
//		}
//		log.debug(tree + " => DONE: "+ target + " => " + retval);
//		target.setEffectiveLastModified(retval);
//		
//		return retval;
//	}

	private JobDependency submitTargets(BuildTarget target, RootContext context, String outputName) throws RunnerException {
		return submitTargets(target, context, outputName, true, true, -1);
	}

	
	/**
	 * Submit a target
	 * 
	 * This could be the initial/default target or a dependency, this is a recursive method
	 * 
	 * @param target - the build target to submit (this will build a job script)
	 * @param context - the environment (settings, etc)
	 * @param outputName - the name of the file we specifically want to build
	 * @param isRoot - is this the first job? (really, is this the first non-blank job, blank job scripts are not submitted)
	 * @param isParentSkippable - was the parent of this job able to be skipped? If so, then we might be able to skip this one too.
	 * @return
	 * @throws RunnerException
	 */
	private JobDependency submitTargets(BuildTarget target, RootContext context, String outputName, boolean isRoot, boolean isParentSkippable, long parentAge) throws RunnerException {
//		System.out.println("Submitting target: "+outputName+ " isParentSkippable? "+ isParentSkippable + " isRoot? " + isRoot);
		log.info("Submitting target: "+outputName);

//		// Can we skip this target (file exists)
//		if (target.isSkippable(outputName)) {
//			log.trace("Skipping target: "+outputName);
//			return null;
//		}
//		
		// Has t already been submitted in another part of the tree? (in this run)
		if (target.getJobDep(outputName) != null && target.getJobDep(outputName).getJobId() != null && !target.getJobDep(outputName).getJobId().equals("")) {
			// note: just because the jobdep is present doesn't mean it was submitted. We need to look for the 
			//       jobId as well to make sure we actually submitted the job.
			log.trace("Skipping target (already submitted): "+outputName + "(" + target.getJobDep(outputName).getJobId() +")");
			return target.getJobDep(outputName);
		}

		// Have we already submitted this job in a prior run?
		JobDependency depJob = findJobProviding(outputName);
		if (depJob != null) {
			log.trace("Skipping target (job queued): "+outputName);
			return depJob;
		}
		
//		if (target.getExisting(outputName) != null) {
//			// existing jobs or files
//			// they don't depend on anything else, so return verbatim
//			
//			return target.getExisting(outputName);
//		}
//		
		// Okay... we are submitting this job, start with submitting it's dependencies...
		
		List<JobDependency> deps = new ArrayList<JobDependency>();
		
		// my current output age (if this is older than all dependencies, we can skip me
		long outputAge = FileUtils.find(outputName).getLastModifiedTime();
		boolean isTemp = target.isTempOutput(outputName);


		try {
			// build my job definition (script, etc...)
			JobDef job = target.eval(prelines, postlines, context);
			
			if (job != null) {
				// this is a potential submitted job 
				boolean blankRoot = false;
				if (isRoot) {
					String tmp = job.getBody().replaceAll("[ \\t\\r\\n]", "");
					if (tmp.equals("")) {
						blankRoot = true;
					}
				}
	
				// try to submit dependencies...
				for (String input: target.getDepends().keySet()) {
					// is this input a "temp" file?
					log.debug("Submitting dependency: "+input+ "  isTemp?" + isTemp+ ", outputAge="+outputAge+", blankRoot?" + blankRoot);

					long effectiveAge = outputAge;
					
					if (isTemp) {
						if (outputAge == -1) {
							// if I'm a temp file, and I don't exist, then my "age" should be that of the parent file.
							effectiveAge = parentAge;
						}
					}
					
					// I am skippable *if* I exist (outputAge > -1), or if I am a temp file, or if I am a blank root (no body, no file)
					JobDependency dep = submitTargets(target.getDepends().get(input), context, input, blankRoot, isParentSkippable && (outputAge > -1 || isTemp || (outputAge==-1 && blankRoot)), effectiveAge);
					if (dep != null) {
						deps.add(dep);
					} else {
						log.debug("Dependency not found?: "+input);
					}
				}
			
				job.addDependencies(deps);

				boolean depSubmitted = false;
				for (JobDependency dep: deps) {
					if (dep.getJobId()!=null && !dep.getJobId().equals("")) {
						depSubmitted = true;
					}
				}
				
				boolean skip;
				
				
//				System.out.println("Dep submitted (" + outputName+") ? " + depSubmitted);
				if (depSubmitted) {
					skip = false;
				} else {
					skip = target.getEffectiveLastModified(outputName) <= outputAge && outputAge > -1;

					log.debug("Is skippable? " + outputName);
					log.debug("  My age: "+ outputAge);
					log.debug("  Parent age: "+ parentAge);
					log.debug("  isParentSkippable: "+ isParentSkippable);
					log.debug("  target.getEffectiveLastModified: "+ target.getEffectiveLastModified(outputName));
					log.debug("  target.isTempOutput: "+ target.isTempOutput(outputName));
					log.debug("  skip ? " + skip);
					
					if (target.isTempOutput(outputName) && target.getEffectiveLastModified(outputName) <= parentAge && parentAge > -1) {
						// I don't exist on disk, but I'm also a temp output, so, I'm skippable -- unless a dependency has been submitted
//						System.out.println("Temp output: "+outputName);
//						if (isParentSkippable) {
							skip = true;
//						}
						log.debug("  temp skip ? " + skip);
//						System.out.println("  skip ? " + skip);
					}

				}
				
				if (!blankRoot && !skip) {
					if (setupJob != null && setupJob.getJobId() != null) {
						job.addDependency(setupJob);
					}
					
					// if this job isn't blank *or* any previous job isn't blank
					if (job.getDependencies().size()==0 && job.getSettingBool("job.shexec", false)) {
						// we can only shexec a job if there are no dependencies!
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
//					System.out.println("Skipping: "+outputName);
					log.debug("Skipping empty/skippable target: "+target);
					job.setJobId("");
				}
				
				target.setSubmittedJobDep(job, job.getOutputs());
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

		String absInput = FileUtils.getAbsolutePath(input);
		
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
				
				Map<String, VarValue> mb = new HashMap<String, VarValue>();
				
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

				teardown = tdTgt.eval(null,  null, rootContext, mb);
				
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
			rec.setPipeline(CGPipe.getFilename());
			rec.setWorkingDirectory(CGPipe.getWorkingDirectory());
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
			for (String tmp:job.getTempOutputs()) {
				rec.addTempOutput(tmp);
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
					} else if (k.equals("job.setup")) {
							for (String s: job.getSettings("job.setup")) {
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
	abstract public boolean cancelJob(String jobId);

}
