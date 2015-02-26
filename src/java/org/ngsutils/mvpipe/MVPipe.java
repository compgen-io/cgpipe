package org.ngsutils.mvpipe;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ngsutils.mvpipe.exceptions.RunnerException;
import org.ngsutils.mvpipe.exceptions.SyntaxException;
import org.ngsutils.mvpipe.parser.Parser;
import org.ngsutils.mvpipe.parser.context.RootContext;
import org.ngsutils.mvpipe.parser.variable.VarBool;
import org.ngsutils.mvpipe.parser.variable.VarList;
import org.ngsutils.mvpipe.parser.variable.VarString;
import org.ngsutils.mvpipe.parser.variable.VarValue;
import org.ngsutils.mvpipe.runner.JobRunner;
import org.ngsutils.mvpipe.support.SimpleFileLoggerImpl;
import org.ngsutils.mvpipe.support.SimpleFileLoggerImpl.Level;

public class MVPipe {
	public static final String RCFILE = (System.getenv("MVPIPE_HOME") != null ? System.getenv("MVPIPE_HOME") : System.getProperty("user.home"))  + File.separator + ".mvpiperc";  

	public static void main(String[] args) throws IOException, SyntaxException {
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
					// default to silent mode when executing as a script
					silent = true;
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
					VarValue val = confVals.get(k);
					if (val.getClass().equals(VarList.class)) {
						((VarList) val).add(VarValue.parseString(arg, true));
					} else {
						VarList list = new VarList();
						list.add(val);
						list.add(VarValue.parseString(arg, true));
						confVals.put(k, list);
					}
				} else {
					confVals.put(k, VarValue.parseString(arg, true));
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
		
		SimpleFileLoggerImpl.setSilent(silent);

		Log log = LogFactory.getLog(MVPipe.class);
		log.info("Starting new run");
		
		RootContext global = new RootContext();
		for (String k1:confVals.keySet()) {
			log.info("config: "+k1+" => "+confVals.get(k1).toString());
			global.set(k1, confVals.get(k1));
		}
		if (logFilename != null) {
			global.set("mvpipe.log", new VarString(logFilename));
		}
		
		Parser parser = new Parser(global);
		try {
			File rc = new File(RCFILE);
			if (rc.exists()) {
				parser.parseFile(rc);
			}
			if (fname.equals("-")) {
				parser.parseInputStream(System.in);
			} else {
				parser.parseFile(fname);
			}
		} catch (IOException | SyntaxException e) {
			log.fatal("MVPIPE ERROR", e);
			System.exit(1);
		}
		
		try {
			JobRunner runner = JobRunner.load(global, dryrun);
			if (targets.size() > 0) {
				for (String target:targets) {
					runner.build(target);
				}
			} else {
				runner.build(null);
			}
			runner.done();
		} catch (RunnerException e) {
			log.fatal("MVPIPE SUBMIT ERROR", e);
			System.err.flush();
			System.out.flush();
			System.exit(1);
		}
	}

	private static void showFile(String fname) throws IOException {
		InputStream is = MVPipe.class.getClassLoader().getResourceAsStream(fname);
		int c;
		while ((c = is.read()) > -1) {
			System.err.print((char) c);
		}
		is.close();	
	}
	
	private static void usage() throws IOException {
		showFile("org/ngsutils/mvpipe/USAGE.txt");
	}

	private static void license() throws IOException {
		showFile("LICENSE");
		showFile("INCLUDES");
	}

}
