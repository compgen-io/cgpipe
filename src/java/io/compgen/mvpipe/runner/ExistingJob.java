package io.compgen.mvpipe.runner;


public class ExistingJob implements JobDependency {
	private String jobid;
	
	public ExistingJob(String jobid) {
		this.jobid = jobid;
	}

	@Override
	public String getJobId() {
		return jobid;
	}

}
