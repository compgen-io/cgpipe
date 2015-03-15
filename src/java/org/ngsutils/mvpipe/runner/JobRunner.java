package org.ngsutils.mvpipe.runner;

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
import org.ngsutils.mvpipe.exceptions.ASTExecException;
import org.ngsutils.mvpipe.exceptions.ASTParseException;
import org.ngsutils.mvpipe.exceptions.RunnerException;
import org.ngsutils.mvpipe.parser.NumberedLine;
import org.ngsutils.mvpipe.parser.context.RootContext;
import org.ngsutils.mvpipe.parser.target.BuildTarget;
import org.ngsutils.mvpipe.parser.variable.VarValue;
import org.ngsutils.mvpipe.support.StringUtils;

public abstract class JobRunner {
	abstract public boolean submit(JobDef jobdef) throws RunnerException;
	abstract public boolean isJobIdValid(String jobId);
	abstract public void innerDone() throws RunnerException;
	abstract protected void setConfig(String k, String val);

	protected String defaultShell = "/bin/sh";
	
	protected Log log = LogFactory.getLog(JobRunner.class);
	
	protected boolean dryrun = false;
	protected boolean done = false;
	
	protected PrintStream joblog = null;
	protected Map<String, JobDependency> submittedJobs = new HashMap<String, JobDependency>();	 // key = output-file, value = job-id

	
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

		if (cxt.contains("mvpipe.shell")) {
			obj.defaultShell = cxt.getString("mvpipe.shell");
		}
		
		String prefix = "mvpipe.runner."+runner;
		Map<String, VarValue> cxtvals = cxt.cloneValues(prefix);
		for (String k: cxtvals.keySet()) {
			obj.setConfig(k, cxtvals.get(k).toString());
		}
		
		obj.dryrun = dryrun;

		// Attempt to load a list of existing jobs
		String joblog = cxt.getString("mvpipe.joblog");
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
		
			// Prune away failed jobs... (we do this after loading the file to avoid checking repeated job submissions)
			List<String> removeList = new ArrayList<String>();
			for (String output: obj.submittedJobs.keySet()) {
				JobDependency job = obj.submittedJobs.get(output);
				if (!obj.isJobIdValid(job.getJobId())) {
					removeList.add(output);
				}
			}
	
			for (String rem: removeList) {
				obj.submittedJobs.remove(rem);
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
			
			String out = StringUtils.slurp(is);
			String err = StringUtils.slurp(es);

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
		List<BuildTarget> list = context.build("__post__");
		List<NumberedLine> lines = null;
		
		if (list != null && list.size()==1) {
			lines = list.get(0).getLines();
		}
		return lines;
	}
	
	public void submitAll(List<BuildTarget> targetList, RootContext context) throws RunnerException {
		List<NumberedLine> prelines = getLinesForTarget("__pre__", context);
		List<NumberedLine> postlines = getLinesForTarget("__pre__", context);
		
		List<BuildTarget> working = new ArrayList<BuildTarget>(targetList);
		
		while (working.size() > 0) {
			int removeIdx = -1;
			for (int i=0; i<working.size(); i++) {
				BuildTarget tgt = working.get(i);
				
				boolean needToRun = false;
				for (String out: tgt.getOutputs()) {
					if (findJobProviding(out) == null) {
						needToRun = true;
						break;
					}
				}
				
				if (!needToRun) {
					removeIdx = i;
					break;
				}
				
				boolean good = true;
				List<JobDependency> deps = new ArrayList<JobDependency>();
				for (String input: tgt.getInputs()) {
					JobDependency depJob = findJobProviding(input);
					if (depJob == null) {
						good = false;
						break;
					}
					deps.add(depJob);
				}
				
				if (good) {
					try {
						JobDef job = tgt.eval(prelines, postlines);
						job.addDependencies(deps);
						submit(job);
						for (String out: tgt.getOutputs()) {
							submittedJobs.put(out, job);
						}
						removeIdx = i;
						break;
					} catch (ASTParseException | ASTExecException e) {
						throw new RunnerException(e);
					}
				}
			}
			if (removeIdx == -1) {
				throw new RunnerException("Could find find a valid dependency graph!");
			} else {
				working.remove(removeIdx);
			}
		}
	}
	
	private JobDependency findJobProviding(String input) {
		if (submittedJobs.containsKey(input)) {
			return submittedJobs.get(input);
		}
		
		File f = new File(input);
		if (f.exists()) {
			JobDependency existing = new ExistingFile(f);
			submittedJobs.put(input, existing);
			return existing;
		}
		
		return null;
	}
	public void done() throws RunnerException {
		innerDone();
	}
}
