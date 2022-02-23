package io.compgen.cgpipe.loader;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.compgen.common.codec.Base64;

public class RemoteSourceLoader extends SourceLoader {
	private String baseUrl;
	private String username=null;
	private String password=null;
	
	private static Log log = LogFactory.getLog(RemoteSourceLoader.class);
	
	public RemoteSourceLoader(SourceLoader parent) {
		super(parent);
	}

	public RemoteSourceLoader(RemoteSourceLoader parent, String newUrl) {
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
	
	public Source loadPipeline(String filename, String hash) throws IOException  {
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
	            String userpass = ((username!=null) ? username : "" )+":"+((password!=null) ? password : "" );
		        String encoding = Base64.encodeBase64((userpass.getBytes()));
		        connection.setRequestProperty  ("Authorization", "Basic " + encoding);
		        is = (InputStream)connection.getInputStream();
	        } else {
	        	is = null;
	        }
		}
		if (is != null) {
			RemoteSourceLoader loader = new RemoteSourceLoader(this, filename);
			return loader.loadPipeline(is, filename, hash);
		}
		return super.loadPipeline(filename,  hash);
	}
}
