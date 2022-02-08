package io.compgen.cgpipe.cmd;

import java.io.IOException;

import io.compgen.cgpipe.runner.joblog.JobLog;
import io.compgen.cmdline.MainBuilder;
import io.compgen.cmdline.annotation.Command;
import io.compgen.cmdline.annotation.Exec;
import io.compgen.cmdline.annotation.UnnamedArg;
import io.compgen.cmdline.impl.AbstractCommand;

@Command(name = "cgpipe vaccuum", desc = "Clean up the job-log, keeping only the most recent records for an output", doc = "")
public class Vaccuum extends AbstractCommand {
	private String jobLogFilename = null;

	@UnnamedArg(required = true, name="FILE")
	public void setJobLogFilename(final String jobLogFilename) {
		this.jobLogFilename = jobLogFilename;
	}

	@Exec
	public void exec() throws IOException {
		JobLog.vaccuum(jobLogFilename);
		System.exit(0);
	}

	public static void main(final String[] args) throws Exception {
		new MainBuilder().runClass(Vaccuum.class, args);
	}
}