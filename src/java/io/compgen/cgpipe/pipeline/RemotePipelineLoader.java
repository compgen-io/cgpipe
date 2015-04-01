package io.compgen.cgpipe.pipeline;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.codec.binary.Base64;

public class RemotePipelineLoader extends PipelineLoader {
	private String baseUrl;
	private String username=null;
	private String password=null;
	
	public RemotePipelineLoader(PipelineLoader parent) {
		super(parent);
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
		
		try{
			URL url = new URL(baseUrl+filename);
	        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
	        connection.setRequestMethod("GET");
	
			if (username != null || password != null) {
				String encoding = new String(Base64.encodeBase64((((username!=null) ? username : "" )+":"+((password!=null) ? password : "" )).getBytes()));
		        connection.setDoOutput(true);
		        connection.setRequestProperty  ("Authorization", "Basic " + encoding);
			}
	
	        InputStream is = (InputStream)connection.getInputStream();
	
			if (is != null) {
				return loadPipeline(is, filename, hash);
			}
		} catch (IOException e) {
		}
		return super.loadPipeline(filename,  hash);
	}
}
