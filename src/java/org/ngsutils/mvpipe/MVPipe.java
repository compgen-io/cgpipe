package org.ngsutils.mvpipe;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.ngsutils.mvpipe.exceptions.RunnerException;
import org.ngsutils.mvpipe.exceptions.SyntaxException;
import org.ngsutils.mvpipe.parser.Parser;
import org.ngsutils.mvpipe.parser.context.RootContext;
import org.ngsutils.mvpipe.parser.variable.VarBool;
import org.ngsutils.mvpipe.parser.variable.VarList;
import org.ngsutils.mvpipe.parser.variable.VarValue;
import org.ngsutils.mvpipe.runner.JobRunner;

public class MVPipe {
	public static final String RCFILE = (System.getenv("MVPIPE_HOME") != null ? System.getenv("MVPIPE_HOME") : System.getenv("user.home"))  + File.separator + ".mvpiperc";  

	public static void main(String[] args) throws IOException, RunnerException, SyntaxException {
		RootContext global = new RootContext();
		
		String fname = null;
		boolean verbose = false;
		boolean dryrun = false;
		
		List<String> targets = new ArrayList<String>();
		
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
			}
			
			if (arg.equals("-h")) {
				usage();
				license();
				System.exit(1);
			} else if (arg.equals("-v")) {
				verbose = true;
			} else if (arg.equals("-dr")) {
				dryrun = true;
			} else if (arg.startsWith("--")) {
				if (k != null) {
					global.set(k, VarBool.TRUE);
				}
				k = arg.substring(2);
			} else if (k != null) {
				if (global.contains(k)) {
					VarValue val = global.get(k);
					if (val.getClass().equals(VarList.class)) {
						((VarList) val).add(VarValue.parseString(arg, true));
					} else {
						VarList list = new VarList();
						list.add(val);
						list.add(VarValue.parseString(arg, true));
						global.set(k, list);
					}
				} else {
					global.set(k, VarValue.parseString(arg, true));
				}
				k = null;
			} else if (args[0].charAt(0) != '-'){
				targets.add(arg);
			}
		}
		
		if (fname == null) {
			usage();
			System.exit(1);
		}
		
		Parser.setVerbose(verbose);
		
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
			System.err.println("MVPIPE ERROR: " + e.getMessage());
//			if (verbose) {
//				e.printStackTrace();
//			}
			System.exit(1);
		}
		
		JobRunner runner = JobRunner.load(global, verbose, dryrun);
		
		if (targets.size() > 0) {
			for (String target:targets) {
				runner.build(target);
			}
		} else {
			runner.build(null);
		}

		runner.done();
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
		showFile("INCLUDED");
	}

}
