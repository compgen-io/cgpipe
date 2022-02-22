package io.compgen.cgpipe.runner;

public class ExistingJob implements JobDependency {
	private String jobid;
	
	public ExistingJob(String jobid) {
		this.jobid = jobid;
	}

	@Override
	public String getJobId() {
		return jobid;
	}
//
//	@Override
//	public long getAge() {
//		// this is a submitted job, therefore anything downstream of this *MUST* run
//		return -1;
//	}
//
//	@Override
//	public boolean isSkippable() {
//		return false;
//	}
//
//	@Override
//	public List<JobDependency> getDependencies() {
//		return null;
//	}
}
