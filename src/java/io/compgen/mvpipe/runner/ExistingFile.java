package org.ngsutils.mvpipe.runner;

import java.io.File;


public class ExistingFile implements JobDependency {
	private File file;
	
	public ExistingFile(File file) {
		this.file = file;
	}

	@Override
	public String getJobId() {
		return "";
	}

	public long getLastModified() {
		return file.lastModified();
	}
	
}
