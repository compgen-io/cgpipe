package org.ngsutils.mvpipe.parser;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.ngsutils.mvpipe.parser.context.BuildTarget;
import org.ngsutils.mvpipe.parser.context.ExecContext;
import org.ngsutils.mvpipe.support.StringUtils;

public class Parser {
	final public boolean verbose;
	final public boolean dryrun;

	final private List<BuildTarget> targets = new ArrayList<BuildTarget>();

	
	private ExecContext currentContext;
	private BuildTarget curTarget = null;

	public ExecContext getContext() {
		return currentContext;
	}
	
	public Parser(ExecContext context, boolean verbose, boolean dryrun) {
		this.currentContext = context;
		this.verbose = verbose;
		this.dryrun = dryrun;
	}

	public void parseFile(String filename) throws IOException, SyntaxException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
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
			Tokens tokens = new Tokens(filename, linenum, line);
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
					targets.add(curTarget);
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

	public void build(String target) {
		
	}

	public void setContext(ExecContext cxt) {
		currentContext = cxt;	
	}
}
