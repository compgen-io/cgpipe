package io.compgen.cgpipe.cmd;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
import io.compgen.cmdline.annotation.Option;
import io.compgen.cmdline.annotation.UnnamedArg;
import io.compgen.cmdline.impl.AbstractCommand;
import io.compgen.common.StringUtils;

@Command(name = "cgpipe cancel-pending", desc = "Cancel any pending (or running) jobs. Requires cgpipe.runner to be set (in cgpiperc, etc)", doc = "")
public class CancelPending extends AbstractCommand {
	private String jobLogFilename = null;
	private boolean verbose = false;
	
	@UnnamedArg(required = true, name="FILE")
	public void setJobLogFilename(final String jobLogFilename) {
		this.jobLogFilename = jobLogFilename;
	}

	@Option(charName="v", desc="Verbose output")
	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
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
		
		List<String> jobIds = new ArrayList<String>(log.getOutputJobIds().values());
		jobIds = StringUtils.naturalSort(jobIds);
		
		for (String jobid: jobIds) {
			if (runner.isJobIdValid(jobid)) {
				if (verbose) {
					System.err.println("Cancelling job: "+jobid);
				}
				runner.cancelJob(jobid);
			} else if (verbose) {
				System.err.println("Job no longer pending: "+jobid);
			}
		}
	}

	public static void main(final String[] args) throws Exception {
		new MainBuilder().runClass(CancelPending.class, args);
	}
}