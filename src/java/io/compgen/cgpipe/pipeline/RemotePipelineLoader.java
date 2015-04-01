package io.compgen.cgpipe.pipeline;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class RemotePipelineLoader extends PipelineLoader {
	private String baseUrl;
	private String username=null;
	private String password=null;
	
	private static Log log = LogFactory.getLog(RemotePipelineLoader.class);
	
	public RemotePipelineLoader(PipelineLoader parent) {
		super(parent);
	}

	public RemotePipelineLoader(RemotePipelineLoader parent, String newUrl) {
		super(parent);
		
		this.baseUrl = parent.baseUrl;
		this.username = parent.username;
		this.password = parent.password;
		
		String[] paths = newUrl.split("/");
		for (int i=0; i<paths.length-1; i++) {
			this.baseUrl +=  paths[i] + "/";
		}
	}
	
	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	public Pipeline loadPipeline(String filename, String hash) throws IOException  {
		if (baseUrl == null) {
			throw new IOException("Missing baseUrl for remote loader!");
		}
		InputStream is = null;
		URL url = new URL(baseUrl+filename);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setUseCaches(false);
		try{
	        is = (InputStream)connection.getInputStream();
		} catch (IOException e) {
	        log.debug("HTTP response code: " + connection.getResponseCode());
	        if (connection.getResponseCode() == 401 && (username != null || password != null)) {
	        	connection.disconnect();

	        	connection = (HttpURLConnection) url.openConnection();
	            connection.setUseCaches(false);
		        String encoding = new String(Base64.encodeBase64((((username!=null) ? username : "" )+":"+((password!=null) ? password : "" )).getBytes()));
		        connection.setRequestProperty  ("Authorization", "Basic " + encoding);
		        is = (InputStream)connection.getInputStream();
	        } else {
	        	is = null;
	        }
		}
		if (is != null) {
			RemotePipelineLoader loader = new RemotePipelineLoader(this, filename);
			return loader.loadPipeline(is, filename, hash);
		}
		return super.loadPipeline(filename,  hash);
	}
}