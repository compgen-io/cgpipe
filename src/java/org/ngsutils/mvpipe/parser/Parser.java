package org.ngsutils.mvpipe.parser;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import org.ngsutils.mvpipe.context.ExecContext;

public class Parser {
	final private ExecContext rootContext;
	final public boolean verbose;
	final public boolean dryrun;

	private ExecContext currentContext;

	public Parser(ExecContext context, boolean verbose, boolean dryrun) {
		this.rootContext = context;
		this.currentContext = context;
		this.verbose = verbose;
		this.dryrun = dryrun;
	}

	public void parse(String filename) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
		String priorLine="";
		String line;
		while ((line = reader.readLine()) != null) {
			if (priorLine.endsWith("\\")) {
				line = priorLine + line;
			}
			if (line.endsWith("\\")) {
				priorLine = line;
			} else {
				currentContext = currentContext.parseLine(line, this);
			}
		}
		
		reader.close();
	}

	public void build(String target) {
		
	}
}
