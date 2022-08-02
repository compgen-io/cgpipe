package io.compgen.cgpipe.cmd;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import io.compgen.cgpipe.CGPipe;
import io.compgen.cgpipe.exceptions.ASTExecException;
import io.compgen.cgpipe.exceptions.ASTParseException;
import io.compgen.cgpipe.exceptions.RunnerException;
import io.compgen.cgpipe.parser.context.RootContext;
import io.compgen.cgpipe.runner.JobRunner;
import io.compgen.cgpipe.runner.joblog.JobLog;
import io.compgen.cgpipe.support.SimpleFileLoggerImpl;
import io.compgen.cmdline.MainBuilder;
import io.compgen.cmdline.annotation.Command;
import io.compgen.cmdline.annotation.Exec;
import io.compgen.cmdline.annotation.UnnamedArg;
import io.compgen.cmdline.impl.AbstractCommand;
import io.compgen.common.StringUtils;

@Command(name = "cgpipe status", desc = "For each output in the job-log, show the status (finished, pending, error)", doc = "")
public class JobStatus extends AbstractCommand {
	private String jobLogFilename = null;

	@UnnamedArg(required = true, name="FILE")
	public void setJobLogFilename(final String jobLogFilename) {
		this.jobLogFilename = jobLogFilename;
	}

	@Exec
	public void exec() throws IOException, ASTParseException, ASTExecException, RunnerException {
		SimpleFileLoggerImpl.setSilent(true);
		JobRunner runner = null;
		// Load config values from global config. 
		RootContext root = new RootContext();
		root.setOutputStream(null);
		CGPipe.loadInitFiles(root);


		// Load the job runner *after* we execute the script to capture any config changes
		runner = JobRunner.load(root);

		JobLog log = JobLog.open(jobLogFilename);
		
		// <JobId, Output>
		Map<String, List<String>> jobs = new TreeMap<String, List<String>> (StringUtils.naturalSorter());
		
		for (String output: log.getOutputJobIds().keySet()) {
			String jobid = log.getJobIdForOutput(output);
			if (!jobs.containsKey(jobid)) {
				jobs.put(jobid, new ArrayList<String>());
			}
			jobs.get(jobid).add(output);
		}
		
	
		for (String jobid: jobs.keySet()) {
			for (String file: jobs.get(jobid)) {
				String status = "MISSING"; 
				if (runner.isJobIdValid(jobid)) {
					status = "PENDING";
				} else {
					
					File f;
					if (file.startsWith("/")) {
						f = new File(file);
					} else {
						f = new File(log.getJob(jobid).getWorkingDirectory() + File.separator + file);
					}
					if (f.exists()) {
						if (f.lastModified() >= log.getJob(jobid).getSubmitTime()) {
							status = "OK";
						} else {
							status = "JOBFAIL";
						}
					}
					
				}

				if (status.equals("MISSING") && log.getJob(jobid).getTempOutputs() != null && log.getJob(jobid).getTempOutputs().contains(file)) {
					// missing files that don't exist and that are temp might not be errors
					// TODO - actually traverse the tree to see if the next non-temp file exists?
					
					status = "TEMP";
				}
				System.out.println(status + "\t" + jobid + "\t" + file);
			}			
		}
	}
	public static void main(final String[] args) throws Exception {
		new MainBuilder().runClass(JobStatus.class, args);
	}
}