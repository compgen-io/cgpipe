package org.ngsutils.mvpipe;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ngsutils.mvpipe.exceptions.ASTExecException;
import org.ngsutils.mvpipe.exceptions.ASTParseException;
import org.ngsutils.mvpipe.exceptions.RunnerException;
import org.ngsutils.mvpipe.exceptions.VarTypeException;
import org.ngsutils.mvpipe.parser.Parser;
import org.ngsutils.mvpipe.parser.context.RootContext;
import org.ngsutils.mvpipe.parser.target.BuildTarget;
import org.ngsutils.mvpipe.parser.variable.VarBool;
import org.ngsutils.mvpipe.parser.variable.VarList;
import org.ngsutils.mvpipe.parser.variable.VarValue;
import org.ngsutils.mvpipe.runner.JobRunner;

public class MVPipe {
	public static final String MVPIPE_HOME = (System.getenv("MVPIPE_HOME") != null ? System.getenv("MVPIPE_HOME") : System.getProperty("user.home"));
	public static final String RCFILE = MVPIPE_HOME  + File.separator + ".mvpiperc";  

	public static void main(String[] args) {
		String fname = null;
		String logFilename = null;
		int verbosity = 0;
		boolean silent = false;
		boolean dryrun = false;
		
		List<String> targets = new ArrayList<String>();
		Map<String, VarValue> confVals = new HashMap<String, VarValue>();
		
		String k = null;

		for (int i=0; i<args.length; i++) {
			String arg = args[i];
			if (i == 0) {
				if (new File(arg).exists()) {
					fname = arg;
					continue;
				}
			} else if (args[i-1].equals("-f")) {
				fname = arg;
				continue;
			} else if (args[i-1].equals("-l")) {
				logFilename = arg;
				continue;
			}
			
			if (arg.equals("-h")) {
				usage();
				license();
				System.exit(1);
			} else if (arg.equals("-s")) {
				silent = true;
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
		
		if (fname == null) {
			usage();
			System.exit(1);
		}
		
		try {
			// Load config values from global config. 
			RootContext root = new RootContext();
			root.pushCWD(MVPIPE_HOME);
			File rc = new File(RCFILE);
			if (rc.exists()) {
				// Parse RC file
				Parser.exec(rc, root);
			}

			// Set cmd-line arguments
			if (silent) {
				root.setOutputStream(null);
			}
			root.update(confVals);

			// Load the job runner *before* we execute the script
			JobRunner runner = JobRunner.load(root, dryrun);
			
			// Parse the AST and run it
			Parser.exec(fname, root);
			
			if (targets.size() > 0) {
				for (String target: targets) {
					List<BuildTarget> targetList = root.build(target);
					runner.submitAll(targetList, root);
				}
			} else {
				List<BuildTarget> targetList = root.build();
				if (targetList != null) {
					runner.submitAll(targetList, root);
				}
			}
			runner.done();

		} catch (ASTParseException | ASTExecException | RunnerException e) {
			System.out.println("MVPIPE ERROR " + e.getMessage());
			if (verbosity > 0) {
				e.printStackTrace();
			}
			System.exit(1);
		}

		
//		switch (verbosity) {
//		case 0:
//			SimpleFileLoggerImpl.setLevel(Level.INFO);
//			break;
//		case 1:
//			SimpleFileLoggerImpl.setLevel(Level.DEBUG);
//			break;
//		case 2:
//			SimpleFileLoggerImpl.setLevel(Level.TRACE);
//			break;
//		case 3:
//		default:
//			SimpleFileLoggerImpl.setLevel(Level.ALL);
//			break;
//		}
//		
//		SimpleFileLoggerImpl.setSilent(silent);
//
//		Log log = LogFactory.getLog(MVPipe.class);
//		log.info("Starting new run");
//		
//		RootContext global = new RootContext();
//		for (String k1:confVals.keySet()) {
//			log.info("config: "+k1+" => "+confVals.get(k1).toString());
//			global.set(k1, confVals.get(k1));
//		}
//		if (logFilename != null) {
//			global.set("mvpipe.log", new VarString(logFilename));
//		}
		
//		Parser parser = new Parser(global);
//		try {
//			File rc = new File(RCFILE);
//			if (rc.exists()) {
//				parser.parseFile(rc);
//			}
//			if (fname.equals("-")) {
//				parser.parseInputStream(System.in);
//			} else {
//				parser.parseFile(fname);
//			}
//		} catch (IOException | SyntaxException e) {
//			log.fatal("MVPIPE ERROR", e);
//			System.exit(1);
//		}
//		
//		try {
//			JobRunner runner = JobRunner.load(global, dryrun);
//			if (targets.size() > 0) {
//				for (String target:targets) {
//					runner.build(target);
//				}
//			} else {
//				runner.build(null);
//			}
//			runner.done();
//		} catch (RunnerException e) {
//			log.fatal("MVPIPE SUBMIT ERROR", e);
//			System.err.flush();
//			System.out.flush();
//			System.exit(1);
//		}
	}

	private static void showFile(String fname) throws IOException {
		InputStream is = MVPipe.class.getClassLoader().getResourceAsStream(fname);
		int c;
		while ((c = is.read()) > -1) {
			System.err.print((char) c);
		}
		is.close();	
	}
	
	private static void usage() {
		try {
			showFile("org/ngsutils/mvpipe/USAGE.txt");
		} catch (IOException e) {
		}
	}

	private static void license() {
		try {
			showFile("LICENSE");
			showFile("INCLUDES");
		} catch (IOException e) {
		}
	}

}
