package io.compgen.cgpipe.loader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class HttpSourceLoader extends SourceLoader {
	public HttpSourceLoader(SourceLoader parent) {
		super(parent);
	}
	
	public Source loadPipeline(String filename, String hash) throws IOException {
		try {
			InputStream is = URI.create(filename).toURL().openStream();
//	        InputStream is = new URL(filename).openStream();
			if (is != null) {
				return loadPipeline(is, filename, hash);
			}
		} catch (IOException e) {
		}
		
		return super.loadPipeline(filename,  hash);
	}
}
