package io.compgen.cgpipe.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamRedirect extends Thread {
	
	final protected InputStream in;
	final protected OutputStream out;

	public StreamRedirect(InputStream in, OutputStream out) {
		this.in = in;
		this.out = out;
	}
	
    public void run() {
    	byte[] buffer = new byte[16*1024];
    	
    	int read = 0;
    	
    	try {
			while ((read = in.read(buffer)) != -1) {
				out.write(buffer, 0, read);
			}
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}

    }
    
}
