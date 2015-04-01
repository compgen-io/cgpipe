package io.compgen.cgpipe.pipeline;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class HttpPipelineLoader extends PipelineLoader {
	public HttpPipelineLoader(PipelineLoader parent) {
		super(parent);
	}
	
	public Pipeline loadPipeline(String filename, String hash) throws IOException {
		try {
	        InputStream is = new URL(filename).openStream();
			if (is != null) {
				return loadPipeline(is, filename, hash);
			}
		} catch (IOException e) {
		}
		
		return super.loadPipeline(filename,  hash);
	}
}
