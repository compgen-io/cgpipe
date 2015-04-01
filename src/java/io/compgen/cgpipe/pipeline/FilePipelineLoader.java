package io.compgen.cgpipe.pipeline;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FilePipelineLoader extends PipelineLoader {
	final private File parentDir;
	public FilePipelineLoader(PipelineLoader parent, File parentDir) {
		super(parent);
		this.parentDir = parentDir;
	}
	public Pipeline loadPipeline(String filename, String hash) throws IOException {
		// absolute path loader
		File file = new File(parentDir, filename);
		if (file.exists()) {
			PipelineLoader loader = new FilePipelineLoader(this, file.getParentFile());
			return loader.loadPipeline(new FileInputStream(file), filename, hash);
		}
		return super.loadPipeline(filename, hash);
	}
}
