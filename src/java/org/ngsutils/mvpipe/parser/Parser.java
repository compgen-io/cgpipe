package org.ngsutils.mvpipe.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.ngsutils.mvpipe.MVPipe;
import org.ngsutils.mvpipe.parser.context.BuildTarget;
import org.ngsutils.mvpipe.parser.context.ExecContext;
import org.ngsutils.mvpipe.support.StringUtils;

public class Parser {
	private static boolean verbose=false;

	public static void setVerbose(boolean val) {
		verbose = val;
	}
	
	private ExecContext currentContext;
	private BuildTarget curTarget = null;

	public ExecContext getContext() {
		return currentContext;
	}
	
	public Parser(ExecContext context) {
		this.currentContext = context;
	}

	protected File loadFile(String filename) throws IOException {
		// check absolute files first.
		
		File f = new File(filename);
		System.err.println("# Checking filename: "+f.getAbsolutePath());
		if (f.exists()) {
			return f;
		}
		
		// check relative files first.
		ExecContext cxt = currentContext;
		while (cxt != null) {
			String cwd = cxt.getCWD();
			if (cwd != null) {
				f = new File(cwd + File.separator + filename);
				System.err.println("# Checking filename: "+f.getAbsolutePath());
				if (f.exists()) {
					return f;
				}
			}
			cxt = cxt.getParent();
		}

		// check global path
		f = new File(new File(MVPipe.RCFILE).getParent() + File.separator + filename);
		System.err.println("# Checking filename: "+f.getAbsolutePath());
		if (f.exists()) {
			return f;
		}
		

		throw new IOException("File: "+filename+" was not found!");
	}
	
	public void parseFile(String filename) throws IOException, SyntaxException {
		File file = loadFile(filename);
		parseFile(file);
	}
	
	public void parseFile(File file) throws IOException, SyntaxException {
		currentContext.setCWD(file.getParent());
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		String priorLine=null;
		String line;
		int linenum = 0;
		while ((line = reader.readLine()) != null) {
			line = StringUtils.rstrip(line);

			linenum += 1;
			
			if (priorLine != null) {
				line = priorLine + line;
			}

			if (line.endsWith("\\")) {
				priorLine = line.substring(0, line.length()-1);
				continue;
			}
			
			if (curTarget != null) {
				if (StringUtils.strip(line).length() > 0) { // not blank
					if (BuildTarget.calcIndentLevel(line) <= curTarget.indentLevel) { // not whitespace at the first char
						// we aren't indented... and have something... must be at the end of the target
						curTarget = null;
					} else {
						curTarget.addLine(StringUtils.strip(line));
						continue;
					}
				}
			}
			
			// Next tokenize the line and attempt to execute it
			Tokens tokens = new Tokens(file.getAbsolutePath(), linenum, line);
			if (verbose) {
				System.err.println("#"+StringUtils.join(", ", tokens.getList()));
			}
			
			if (tokens.size() > 0) {
				// check for a new target (out1 out2 : in1 in2)
				List<String> pre = new ArrayList<String>();
				List<String> post = new ArrayList<String>();
				boolean istarget = false;
				String last = "";
				for (String tok: tokens.getList()) {
					if (tok.equals(":") && !last.endsWith("\\")) {
						istarget = true;
					} else if (istarget) {
						post.add(tok);
					} else {
						pre.add(tok);
						last = tok;
					}
				}
				
				if (istarget) {
					curTarget = new BuildTarget(pre, post, currentContext, BuildTarget.calcIndentLevel(line));
					currentContext.addTarget(curTarget);
					continue;
				} else {
					try {
						currentContext = currentContext.addTokenizedLine(tokens);
					} catch (SyntaxException e) {
						reader.close();
						throw e;
					}
				}
			}
		}
		
		reader.close();
	}
}
