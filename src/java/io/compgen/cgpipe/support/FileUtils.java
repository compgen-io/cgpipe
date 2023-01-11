package io.compgen.cgpipe.support;

import java.io.File;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FileUtils {
	private static Map<String, FileUtils> fileCache = new HashMap<String, FileUtils>();
	private static Map<String, Boolean> existsCache = new HashMap<String, Boolean>();
	private static Set<String> listedDirs = new HashSet<String>();
	
	public static boolean doesFileExist(String raw) {
		if (raw == null) {
			return false;
		}
		String filename = getAbsolutePath(raw);
		if (existsCache.containsKey(filename)) {
			return existsCache.get(filename);
		}
		File f = new File(filename);

		if (f.getParentFile()!= null && !listedDirs.contains(f.getParentFile().getAbsolutePath())) {
			listedDirs.add(f.getParentFile().getAbsolutePath());
			
			//
			// On some network filesystems (*ahem* Gluster), a file might not appear
			// until the metadata on the directory is refreshed. This happens with
			// a fresh listing. However, this can be time consuming, so let's only try
			// this trick once for each parent directory.
			//
			// By doing this once per parent directory, we can avoid the process if there
			// are multiple missing files in a given directory.
			//
			// This can be a problem with "temporary" files that are used in a pipeline
			// and then deleted. If we keep checking for missing files, it can make the 
			// pipeline take forever. Instead, we can exhaustively check a directory
			// once. This way, if there are more missing files in the directory, we only
			// take the time hit once per directory, as opposed to once per file.

			for (int i=0; i<3; i++) {
				if (f.exists()) {
					existsCache.put(filename, true);
					return true;
				}
		
				try {
					f.toPath().getFileSystem().provider().checkAccess(f.toPath());
				} catch (NoSuchFileException e) {
					f.getParentFile().list();

					try {
						Thread.sleep(100*(i+1));
					} catch (InterruptedException e1) {
					}
				} catch (IOException e) {
					try {
						Thread.sleep(100*(i+1));
					} catch (InterruptedException e1) {
					}
				}
			}
		} else {
			if (f.exists()) {
				existsCache.put(filename, true);
				return true;
			}
	
			try {
				f.toPath().getFileSystem().provider().checkAccess(f.toPath());
				if (f.exists()) {
					existsCache.put(filename, true);
					return true;
				}
			} catch (IOException e) {
				// silent
			}
		}
		existsCache.put(filename, false);
		return false;
	}

	public static String getAbsolutePath(String filename) {
		if (filename == null) {
			return null;
		}
		return Paths.get(filename).toAbsolutePath().toString();
	}
	
	public static FileUtils find(String path) {
		String absPath = getAbsolutePath(path);
		if (!fileCache.containsKey(absPath)) {
			fileCache.put(absPath, new FileUtils(path, false));
		}
		return fileCache.get(absPath);
		
	}
	
	
	private final String filename;
	private final File f;
	private Boolean exists = null;
	private boolean temp = false;
	
//	private JobDependency dep = null;
	
	private FileUtils(String filename, boolean temp) {
		this.filename = getAbsolutePath(filename);
		this.f = new File(this.filename);
		this.temp = temp;
	}
	
	public boolean getTemp() {
		return this.temp;
	}
	
	public String getFilename() {
		return this.filename;
	}
	
	public long getLastModifiedTime() {
		if (exists()) {
			return f.lastModified();
		}
		return -1;
	}
	
//	public JobDependency getJobDep() {
//		if (this.dep != null) {
//			return this.dep;
//		}
//		if (exists()) {
//			return new ExistingFile(this.f);
//		}
//		return null;
//	}
//	
//	public void setJobDep(JobDependency dep) {
//		this.dep = dep;
//	}
	
	public boolean exists() {
		if (this.exists == null) {
			this.exists = doesFileExist(this.filename);
		}
		return this.exists;

	}
	
}
