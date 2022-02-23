package io.compgen.cgpipe.cmd;

import java.io.IOException;
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

@Command(name = "cgpipe show-pending", desc = "Show any pending (or running) jobs. Requires cgpipe.runner to be set (in cgpiperc, etc)", doc = "")
public class ShowPending extends AbstractCommand {
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
		Map<String, String> jobs = new TreeMap<String, String> (StringUtils.naturalSorter());
		
		for (String output: log.getOutputJobIds().keySet()) {
			String jobid = log.getJobIdForOutput(output);
			if (jobs.containsKey(jobid)) {
				jobs.put(jobid, jobs.get(jobid)+","+output);
			} else {
				jobs.put(jobid, output);
			}
		}
		
		
		for (String jobid: jobs.keySet()) {
			if (runner.isJobIdValid(jobid)) {
				System.out.println(jobid + ": " + jobs.get(jobid));
			}
		}
	}

	public static void main(final String[] args) throws Exception {
		new MainBuilder().runClass(ShowPending.class, args);
	}
}