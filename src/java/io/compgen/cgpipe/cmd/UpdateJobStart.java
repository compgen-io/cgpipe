package io.compgen.cgpipe.cmd;

import java.io.IOException;

import io.compgen.cgpipe.runner.joblog.JobLog;
import io.compgen.cmdline.MainBuilder;
import io.compgen.cmdline.annotation.Command;
import io.compgen.cmdline.annotation.Exec;
import io.compgen.cmdline.annotation.Option;
import io.compgen.cmdline.impl.AbstractCommand;

@Command(name = "cgpipe update-job-start", desc = "Update the job-log with start time of a job", doc = "")
public class UpdateJobStart extends AbstractCommand {
	private String jobLogFilename = null;
	private String jobId = null;

	@Option(charName = "f", desc = "Job log file", required = true)
	public void setJobLogFilename(final String jobLogFilename) {
		this.jobLogFilename = jobLogFilename;
	}

	@Option(charName = "j", desc = "Job ID", required = true)
	public void setJobId(final String jobId) {
		this.jobId = jobId;
	}

	@Exec
	public void exec() throws IOException {
		JobLog joblog = JobLog.open(jobLogFilename);
		joblog.writeStartTime(jobId);
		System.exit(0);
	}

	public static void main(final String[] args) throws Exception {
		new MainBuilder().runClass(UpdateJobStart.class, args);
	}
}