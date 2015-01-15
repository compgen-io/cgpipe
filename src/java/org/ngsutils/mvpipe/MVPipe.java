package org.ngsutils.mvpipe;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.ngsutils.mvpipe.parser.Parser;
import org.ngsutils.mvpipe.parser.SyntaxException;
import org.ngsutils.mvpipe.parser.context.ExecContext;
import org.ngsutils.mvpipe.parser.variable.VarBool;
import org.ngsutils.mvpipe.parser.variable.VarTypeException;
import org.ngsutils.mvpipe.parser.variable.VarValue;

public class MVPipe {

	public static void main(String[] args) throws VarTypeException {
		ExecContext global = new ExecContext();
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
			
			if (arg.equals("-v")) {
				verbose = true;
			} else if (arg.equals("-dr")) {
				dryrun = true;
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
		
		Parser parser = new Parser(global, verbose, dryrun);
		try {
			parser.parse(fname);
		} catch (IOException | SyntaxException e) {
			System.err.println("MVPIPE ERROR: " + e.getMessage());
			if (verbose) {
//				e.printStackTrace();
			}
			System.exit(1);
		}
		
		for (String target:targets) {
			parser.build(target);
		}
	}

}
