package io.compgen.cgpipe.loader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FileSourceLoader extends SourceLoader {
	final private File parentDir;
	public FileSourceLoader(SourceLoader parent, File parentDir) {
		super(parent);
		this.parentDir = parentDir;
	}
	public Source loadPipeline(String filename, String hash) throws IOException {
		// absolute path loader
		File file = new File(parentDir, filename);
		if (file.exists()) {
			SourceLoader loader = new FileSourceLoader(this, file.getParentFile());
			return loader.loadPipeline(new FileInputStream(file), filename, hash);
		}
		return super.loadPipeline(filename, hash);
	}
}
