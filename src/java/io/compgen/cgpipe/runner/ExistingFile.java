package io.compgen.cgpipe.runner;

public class ExistingFile implements JobDependency {
	@SuppressWarnings("unused")
	private String filename;
	@SuppressWarnings("unused")
	private boolean isTemp;
	
	public ExistingFile(String filename) {
		this(filename, false);
	}
	public ExistingFile(String filename, boolean isTemp) {
			this.filename = filename;
			this.isTemp = isTemp;
	}

	@Override
	public String getJobId() {
		return "";
	}
//
//	public long getAge() {
//		return JobFile.find(filename).getLastModifiedTime();
//	}
//	@Override
//	public boolean isSkippable() {
//		return this.isTemp;
//	}
//	@Override
//	public List<JobDependency> getDependencies() {
//		return null;
//	}
//	
}
