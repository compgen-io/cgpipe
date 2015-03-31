package io.compgen.cgpipe;

import io.compgen.cgpipe.exceptions.ASTExecException;
import io.compgen.cgpipe.exceptions.ASTParseException;
import io.compgen.cgpipe.exceptions.ExitException;
import io.compgen.cgpipe.exceptions.RunnerException;
import io.compgen.cgpipe.exceptions.VarTypeException;
import io.compgen.cgpipe.parser.Parser;
import io.compgen.cgpipe.parser.context.RootContext;
import io.compgen.cgpipe.parser.target.BuildTarget;
import io.compgen.cgpipe.parser.variable.VarBool;
import io.compgen.cgpipe.parser.variable.VarList;
import io.compgen.cgpipe.parser.variable.VarString;
import io.compgen.cgpipe.parser.variable.VarValue;
import io.compgen.cgpipe.runner.JobRunner;
import io.compgen.cgpipe.support.SimpleFileLoggerImpl;
import io.compgen.cgpipe.support.SimpleFileLoggerImpl.Level;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CGPipe {
	public static final File CGPIPE_HOME = new File(System.getenv("CGPIPE_HOME") != null ? System.getenv("CGPIPE_HOME") : System.getProperty("user.home"));
	public static final File RCFILE = new File(CGPIPE_HOME,".cgpiperc");  

	public static void main(String[] args) {
		String fname = null;
		String remoteName = null;
		String logFilename = null;
		int verbosity = 0;
		boolean silent = false;
		boolean dryrun = false;
		boolean silenceStdErr = false;
		boolean help = false;
		
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
			} else if (args[i-1].equals("-p")) {
				remoteName = arg;
				continue;
			} else if (args[i-1].equals("-l")) {
				logFilename = arg;
				continue;
			}
			
			if (arg.equals("-h")) {
				help = true;
			} else if (arg.equals("-license")) {
				license();
				System.exit(1);
			} else if (arg.equals("-s")) {
				silent = true;
			} else if (arg.equals("-nolog")) {
				silenceStdErr = true;
			} else if (arg.equals("-v")) {
				verbosity++;
			} else if (arg.equals("-vv")) {
				verbosity += 2;
			} else if (arg.equals("-vvv")) {
				verbosity += 3;
			} else if (arg.equals("-dr")) {
				dryrun = true;
			} else if (arg.startsWith("--")) {
				if (k != null) {
					confVals.put(k, VarBool.TRUE);
				}
				k = arg.substring(2);
			} else if (k != null) {
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
		
		if ((fname == null && remoteName == null) || (fname != null && remoteName != null)) {
			usage();
			System.exit(1);
		}
		
		if (help) {
			if (fname == null) {			
				PipelineLoader.showHelp(remoteName);
			} else {
				PipelineLoader.showHelp(fname);
			}
			System.exit(0);
		}
		
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
		
		SimpleFileLoggerImpl.setSilent(silenceStdErr);

		Log log = LogFactory.getLog(CGPipe.class);
		log.info("Starting new run: "+fname);

		if (logFilename != null) {
			confVals.put("cgpipe.log", new VarString(logFilename));
		}
		JobRunner runner = null;
		try {
			// Load config values from global config. 
			RootContext root = new RootContext();
			root.pushCWD(CGPIPE_HOME.getAbsolutePath());

			// Parse RC file
			if (RCFILE.exists()) {
				Parser.exec(RCFILE.getAbsolutePath(), root);
			}

			// Set cmd-line arguments
			if (silent) {
				root.setOutputStream(null);
			}

			for (String k1:confVals.keySet()) {
				log.info("config: "+k1+" => "+confVals.get(k1).toString());
			}

			root.update(confVals);

			// Parse the AST and run it
			Parser.exec(fname, root);

			// Load the job runner *after* we execute the script to capture any config changes
			runner = JobRunner.load(root, dryrun);

			// find a build-target, and submit the job(s) to a runner
			if (targets.size() > 0) {
				for (String target: targets) {
					log.debug("building: "+target);

					BuildTarget initTarget = root.build(target);
					runner.submitAll(initTarget, root);
				}
			} else {
				BuildTarget initTarget = root.build();
				if (initTarget != null) {
					runner.submitAll(initTarget, root);
				}
			}
			runner.done();

		} catch (ASTParseException | ASTExecException | RunnerException e) {
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
			System.out.println(readFile("VERSION"));
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
}
