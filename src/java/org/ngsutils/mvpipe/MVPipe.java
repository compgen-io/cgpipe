package org.ngsutils.mvpipe;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.ngsutils.mvpipe.parser.Parser;
import org.ngsutils.mvpipe.parser.SyntaxException;
import org.ngsutils.mvpipe.parser.context.RootContext;
import org.ngsutils.mvpipe.parser.variable.VarBool;
import org.ngsutils.mvpipe.parser.variable.VarTypeException;
import org.ngsutils.mvpipe.parser.variable.VarValue;

public class MVPipe {
	public static final String RCFILE = (System.getenv("MVPIPE_HOME") != null ? System.getenv("MVPIPE_HOME") : System.getenv("user.home"))  + File.separator + ".mvpiperc";  

	public static void main(String[] args) throws VarTypeException {
		RootContext global = new RootContext();

		String fname = null;
		boolean verbose = false;
		
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
			
			if (arg.equals("-v")) {
				verbose = true;
			} else if (arg.equals("-dr")) {
				global.setDryRun(true);
			} else if (arg.startsWith("--")) {
				if (k != null) {
					global.set(k, VarBool.TRUE);
				}
				k = arg.substring(2);
			} else if (k != null) {
				global.set(k, VarValue.parseString(arg));
			} else if (args[0].charAt(0) != '-'){
				targets.add(arg);
			}
		}
		
		Parser.setVerbose(verbose);
		
		Parser parser = new Parser(global);
		try {
			File rc = new File(RCFILE);
			if (rc.exists()) {
				parser.parseFile(rc);
			}
			parser.parseFile(fname);
		} catch (IOException | SyntaxException e) {
			System.err.println("MVPIPE ERROR: " + e.getMessage());
			if (verbose) {
//				e.printStackTrace();
			}
			System.exit(1);
		}
		
		for (String target:targets) {
			global.build(target);
		}
	}

}
