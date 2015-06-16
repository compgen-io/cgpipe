package io.compgen.cgpipe.loader;

import io.compgen.cgpipe.CGPipe;
import io.compgen.cgpipe.exceptions.ASTParseException;
import io.compgen.common.StringUtils;
import io.compgen.common.codec.Hex;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SourceLoader {
	private static final SourceLoader defaultPipelineLoader = new SourceLoader();
	
	public static final SourceLoader getDefaultLoader() {
		return defaultPipelineLoader;
	}

	private static Log log = LogFactory.getLog(SourceLoader.class);
	
	final protected SourceLoader parent;

	private Map<String, RemoteSourceLoader> remotes = null;
	
	private SourceLoader() {
		this.parent = null;
	}
	public SourceLoader(SourceLoader parent) {
		this.parent = parent;
	}

	public Source loadPipeline(String name) throws IOException {
		String sha1Hash = null;
		if (name.indexOf('#') > -1) {
			sha1Hash = name.substring(name.indexOf('#')+1);
			name = name.substring(0, name.indexOf('#'));
		}
		return loadPipeline(name, sha1Hash);
	}

	public Source loadPipeline(String filename, String hash) throws IOException {
		if (hash != null) {
			log.info("Looking for file: "+filename+" hash:"+hash);
		} else {
			log.info("Looking for file: "+filename);
		}
		// absolute path loader
		File file = new File(filename);
		if (file.exists()) {
			SourceLoader loader = new FileSourceLoader(this, file.getParentFile());
			return loader.loadPipeline(new FileInputStream(file), filename, hash);
		}
		
		// CGHOME path loader
		file = new File(CGPipe.CGPIPE_HOME, filename);
		if (file.exists()) {
			SourceLoader loader = new FileSourceLoader(this, file.getParentFile());
			return loader.loadPipeline(new FileInputStream(file), filename, hash);
		}
		
		if (parent != null) {
			Source source = parent.loadPipeline(filename, hash);
			if (source != null) {
				return source;
			}
		}
		
		// attempt to find a remote loader
		if (filename.startsWith("http:") || filename.startsWith("https:")) {
			log.debug("Using HTTP loader: " + filename);
			SourceLoader loader = new HttpSourceLoader(this);
			return loader.loadPipeline(filename, hash);
		}

		if (remotes != null && filename.contains(":")) {
			String remoteName = filename.substring(0, filename.indexOf(":"));
			String url = filename.substring(filename.indexOf(":")+1);
			
			if (remotes.containsKey(remoteName)) {
				log.debug("Using Remote loader: " + remoteName+" => "+url);
				SourceLoader loader = remotes.get(remoteName);
				return loader.loadPipeline(url, hash);
			}
		}

		return null;
	}
	
	public Source loadPipeline(InputStream is, String name) throws IOException {
		return loadPipeline(is, name, null);
	}

	public Source loadPipeline(InputStream is, String name, String hash) throws IOException {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA1");
		} catch (NoSuchAlgorithmException e) {
			throw new IOException("No SHA1?");
		}

		DigestInputStream dis = new DigestInputStream(is, md);
		BufferedReader reader = new BufferedReader(new InputStreamReader(dis));
		
		Source source = new Source(name, this);
		
		String line;
		int linenum = 0;

		while ((line = reader.readLine()) != null) {
			source.addLine(StringUtils.rstrip(line), ++linenum);
		}

		reader.close();
		source.finalize();
		
		byte[] digest = md.digest();
		String digestStr = Hex.toHexString(digest).toLowerCase();
		
		source.setHashDigest(digestStr);
		log.debug("SHA-1 hash: "+digestStr);

		if (hash != null) {
			hash = hash.toLowerCase();
			log.debug("Expected: "+hash+" Got: "+digestStr);
			if (!digestStr.equals(hash)) {
				throw new IOException("Error loading: "+name+" - SHA1 hash doesn't match! Expected: "+hash+" Got: "+digestStr);
			}
		}

		return source;
	}

	public static void updateRemoteHandlers(Map<String, String> remoteConfig) throws ASTParseException {
		log.debug("updating remote handlers:"+StringUtils.join(", ", remoteConfig.keySet()));
		defaultPipelineLoader.innerUpdateRemoteHandlers(remoteConfig);
	}

	public void innerUpdateRemoteHandlers(Map<String, String> remoteConfig) throws ASTParseException {
		remotes = new HashMap<String, RemoteSourceLoader>();
		for (String k: remoteConfig.keySet()) {
			String val = remoteConfig.get(k);
			log.debug(k+" => "+val);
			
			if (k.startsWith("cgpipe.remote.")) {
				k = k.substring("cgpipe.remote.".length());
				String[] spl = k.split("\\.",2);
				if (spl.length != 2) {
					throw new ASTParseException("Invalid remote site configuration!");
				}
				
				if (!remotes.containsKey(spl[0])) {
					log.debug("Adding remote: " + spl[0]);
					remotes.put(spl[0], new RemoteSourceLoader(defaultPipelineLoader));
				}
				RemoteSourceLoader remote = remotes.get(spl[0]);
				if (spl[1].equals("baseurl")) {
					remote.setBaseUrl(val);
				} else if (spl[1].equals("username")) {
					remote.setUsername(val);
				} else if (spl[1].equals("password")) {
					remote.setPassword(val);
				}
			}
		}
	}
}
