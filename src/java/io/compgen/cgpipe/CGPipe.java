package io.compgen.cgpipe;

import io.compgen.cgpipe.exceptions.ASTExecException;
import io.compgen.cgpipe.exceptions.ASTParseException;
import io.compgen.cgpipe.exceptions.ExitException;
import io.compgen.cgpipe.exceptions.RunnerException;
import io.compgen.cgpipe.exceptions.VarTypeException;
import io.compgen.cgpipe.loader.SourceLoader;
import io.compgen.cgpipe.parser.Parser;
import io.compgen.cgpipe.parser.context.RootContext;
import io.compgen.cgpipe.parser.target.BuildTarget;
import io.compgen.cgpipe.parser.variable.VarBool;
import io.compgen.cgpipe.parser.variable.VarInt;
import io.compgen.cgpipe.parser.variable.VarList;
import io.compgen.cgpipe.parser.variable.VarString;
import io.compgen.cgpipe.parser.variable.VarValue;
import io.compgen.cgpipe.runner.JobRunner;
import io.compgen.cgpipe.support.SimpleFileLoggerImpl;
import io.compgen.cgpipe.support.SimpleFileLoggerImpl.Level;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CGPipe {
	
	private static Log log = LogFactory.getLog(CGPipe.class);

	public static final File CGPIPE_HOME = new File(System.getenv("CGPIPE_HOME") != null ? System.getenv("CGPIPE_HOME") : System.getProperty("user.home"));
	public static final File USER_INIT = new File(CGPIPE_HOME,".cgpiperc");
	public static final File GLOBAL_INIT = new File("/etc/cgpiperc");

//	public static final Map<String, VarValue> globalConfig = new HashMap<String, VarValue>(); 
	
	public static void main(String[] args) {
		String fname = null;
		String logFilename = null;
		String outputFilename = null;
		PrintStream outputStream = null;  

		int verbosity = 0;
		boolean silent = false;
		boolean silenceStdErr = false;
		boolean showHelp = false;
		
		List<String> targets = new ArrayList<String>();
		Map<String, VarValue> confVals = new HashMap<String, VarValue>();
		
		String k = null;

		for (int i=0; i<args.length; i++) {
			String arg = args[i];
			if (i == 0) {
				if (new File(arg).exists()) {
					fname = arg;
					silenceStdErr = true;
					continue;
				}
			} else if (args[i-1].equals("-f")) {
				fname = arg;
				continue;
			} else if (args[i-1].equals("-l")) {
				logFilename = arg;
				continue;
			} else if (args[i-1].equals("-o")) {
				outputFilename = arg;
				continue;
			}
			
			if (arg.equals("-h") || arg.equals("-help") || arg.equals("--help")) {
				if (k != null) {
					if (k.contains("-")) {
						k = k.replaceAll("-", "_");
					}
					confVals.put(k, VarBool.TRUE);
				}
				showHelp = true;
			} else if (arg.equals("-license")) {
				license();
				System.exit(1);
			} else if (arg.equals("-s")) {
				if (k != null) {
					if (k.contains("-")) {
						k = k.replaceAll("-", "_");
					}
					confVals.put(k, VarBool.TRUE);
				}
				silent = true;
			} else if (arg.equals("-nolog")) {
				if (k != null) {
					if (k.contains("-")) {
						k = k.replaceAll("-", "_");
					}
					confVals.put(k, VarBool.TRUE);
				}
				silenceStdErr = true;
			} else if (arg.equals("-v")) {
				if (k != null) {
					if (k.contains("-")) {
						k = k.replaceAll("-", "_");
					}
					confVals.put(k, VarBool.TRUE);
				}
				verbosity++;
			} else if (arg.equals("-vv")) {
				if (k != null) {
					if (k.contains("-")) {
						k = k.replaceAll("-", "_");
					}
					confVals.put(k, VarBool.TRUE);
				}
				verbosity += 2;
			} else if (arg.equals("-vvv")) {
				if (k != null) {
					if (k.contains("-")) {
						k = k.replaceAll("-", "_");
					}
					confVals.put(k, VarBool.TRUE);
				}
				verbosity += 3;
			} else if (arg.equals("-dr")) {
				if (k != null) {
					if (k.contains("-")) {
						k = k.replaceAll("-", "_");
					}
					confVals.put(k, VarBool.TRUE);
				}
				confVals.put("cgpipe.dryrun", VarBool.TRUE);
			} else if (arg.startsWith("--")) {
				if (k != null) {
					if (k.contains("-")) {
						k = k.replaceAll("-", "_");
					}
					confVals.put(k, VarBool.TRUE);
				}
				k = arg.substring(2);
			} else if (k != null) {
				if (k.contains("-")) {
					k = k.replaceAll("-", "_");
				}
				if (confVals.containsKey(k)) {
					try {
						VarValue val = confVals.get(k);
						if (val.getClass().equals(VarList.class)) {
							((VarList) val).add(VarValue.parseStringRaw(arg));
						} else {
							VarList list = new VarList();
							list.add(val);
							list.add(VarValue.parseStringRaw(arg));
							confVals.put(k, list);
						}
					} catch (VarTypeException e) {
						System.err.println("Error setting variable: "+k+" => "+arg);
						System.exit(1);;
					}
				} else {
					confVals.put(k, VarValue.parseStringRaw(arg));
				}
				k = null;
			} else if (arg.charAt(0) != '-'){
				targets.add(arg);
			}
		}
		if (k != null) {
			if (k.contains("-")) {
				k = k.replaceAll("-", "_");
			}
			confVals.put(k, VarBool.TRUE);
		}

		confVals.put("cgpipe.loglevel", new VarInt(verbosity));
		
		if (fname == null) {
			usage();
			System.exit(1);
		}
		
		if (!showHelp) {
			switch (verbosity) {
			case 0:
				SimpleFileLoggerImpl.setLevel(Level.INFO);
				break;
			case 1:
				SimpleFileLoggerImpl.setLevel(Level.DEBUG);
				break;
			case 2:
				SimpleFileLoggerImpl.setLevel(Level.TRACE);
				break;
			case 3:
			default:
				SimpleFileLoggerImpl.setLevel(Level.ALL);
				break;
			}
		} else {
			SimpleFileLoggerImpl.setLevel(Level.FATAL);
		}
		
		SimpleFileLoggerImpl.setSilent(silenceStdErr || showHelp);

		Log log = LogFactory.getLog(CGPipe.class);
		log.info("Starting new run: "+fname);

		if (logFilename != null) {
			confVals.put("cgpipe.log", new VarString(logFilename));
		}
		
		if (System.getenv("CGPIPE_DRYRUN") != null && !System.getenv("CGPIPE_DRYRUN").equals("")) {
			confVals.put("cgpipe.dryrun", VarBool.TRUE);

		}
		
		JobRunner runner = null;
		try {
			// Load config values from global config. 
			RootContext root = new RootContext();
			loadInitFiles(root);

			// Set cmd-line arguments
			if (silent) {
				root.setOutputStream(null);
			}
			
			if (outputFilename != null) {
				outputStream = new PrintStream(new FileOutputStream(outputFilename));
				root.setOutputStream(outputStream);
			}

			for (String k1:confVals.keySet()) {
				log.info("config: "+k1+" => "+confVals.get(k1).toString());
			}

			root.update(confVals);
			root.set("cgpipe.procs", new VarInt(Runtime.getRuntime().availableProcessors()));
			
			// update the URL Source loader configs
			SourceLoader.updateRemoteHandlers(root.cloneString("cgpipe.remote"));

			// Now check for help, only after we've setup the remote handlers...
			if (showHelp) {
				try {
					Parser.showHelp(fname);
					System.exit(0);
				} catch (IOException e) {
					System.err.println("Unable to find pipeline: "+fname);
					System.exit(1);
				}
			}

			// Set the global config values
//			globalConfig.putAll(root.cloneValues());
			
			// Parse the AST and run it
			Parser.exec(fname, root);

			// Load the job runner *after* we execute the script to capture any config changes
			runner = JobRunner.load(root);

			// find a build-target, and submit the job(s) to a runner
			if (targets.size() > 0) {
				for (String target: targets) {
					log.debug("building: "+target);

					BuildTarget initTarget = root.build(target);
					if (initTarget != null) {
						runner.submitAll(initTarget, root);
					} else {
						System.out.println("CGPIPE ERROR: Unable to find target: " + target);
					}
				}
			} else {
				BuildTarget initTarget = root.build();
				if (initTarget != null) {
					runner.submitAll(initTarget, root);
					// Leave this commented out - it should be allowed to run cgpipe scripts w/o a target defined (testing)
//				} else {
//					System.out.println("CGPIPE ERROR: Unable to find default target");
				}
			}
			runner.done();

			if (outputStream != null) {
				outputStream.close();
			}

		} catch (ASTParseException | ASTExecException | RunnerException | FileNotFoundException  e) {
			if (outputStream != null) {
				outputStream.close();
			}
			if (runner != null) {
				runner.abort();
			}
			
			if (e.getClass().equals(ExitException.class)) {
				System.exit(((ExitException) e).getReturnCode());
			}
			
			System.out.println("CGPIPE ERROR " + e.getMessage());
			if (verbosity > 0) {
				e.printStackTrace();
			}
			System.exit(1);
		}
	}

	private static String readFile(String fname) throws IOException {
		String s = "";
		InputStream is = CGPipe.class.getClassLoader().getResourceAsStream(fname);
		if (is == null) {
			throw new IOException("Can't load file: "+fname);
		}
		int c;
		while ((c = is.read()) > -1) {
			s += (char) c;
		}
		is.close();	
		return s;
	}
	
	private static void usage() {
		try {
			System.out.println(readFile("io/compgen/cgpipe/USAGE.txt"));
			System.out.println("http://compgen.io/cgpipe");
			System.out.println(readFile("io/compgen/cgpipe/VERSION"));
			System.out.println();
		} catch (IOException e) {
		}
	}

	private static void license() {
		try {
			System.out.println(readFile("LICENSE"));
			System.out.println(readFile("INCLUDES"));
		} catch (IOException e) {
		}
	}

	public static void loadInitFiles(RootContext root) throws ASTParseException, ASTExecException {
		// Parse the default cgpiperc
		InputStream is = CGPipe.class.getClassLoader().getResourceAsStream("io/compgen/cgpipe/cgpiperc");
		if (is != null) {
			Parser.exec("io/compgen/cgpipe/cgpiperc", is,  root);
		}

		// Parse /etc global RC file
		if (CGPipe.GLOBAL_INIT.exists()) {
			Parser.exec(CGPipe.GLOBAL_INIT.getAbsolutePath(), root);
		}

		// Parse install-level RC file (in same folder as JAR)
		try {
			String cwd = URLDecoder.decode(CGPipe.class.getProtectionDomain().getCodeSource().getLocation().getPath(), "UTF-8");
			File cwdFile = new File(new File(cwd).getParent(), ".cgpiperc");
			if (cwdFile.exists()) {
				Parser.exec(cwdFile.getAbsolutePath(), root);
			}
		} catch (UnsupportedEncodingException e) {
		}

		// Parse RC file
		if (CGPipe.USER_INIT.exists()) {
			Parser.exec(CGPipe.USER_INIT.getAbsolutePath(), root);
		}

		// Load settings from environment variables.
        Map<String, String> env = System.getenv();
		for (String k: env.keySet()) {
			if (k.equals("CGPIPE_ENV")) {
				Parser.eval(env.get(k).split(";"), root);
			}
		}
		log.trace("Init context:");
		Map<String,VarValue> tmp = root.cloneValues();
		for (String s: tmp.keySet()) {
			log.trace("  " + s + " => " + tmp.get(s).toString());
		}
	}
}
