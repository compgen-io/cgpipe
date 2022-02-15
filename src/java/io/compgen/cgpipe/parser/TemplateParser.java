package io.compgen.cgpipe.parser;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.compgen.cgpipe.exceptions.ASTExecException;
import io.compgen.cgpipe.exceptions.ASTParseException;
import io.compgen.cgpipe.loader.NumberedLine;
import io.compgen.cgpipe.loader.Source;
import io.compgen.cgpipe.loader.SourceLoader;
import io.compgen.cgpipe.parser.context.RootContext;
import io.compgen.cgpipe.parser.node.ASTNode;
import io.compgen.cgpipe.parser.node.JobNoOpNode;
import io.compgen.common.StringUtils;

public class TemplateParser {
	static private Log log = LogFactory.getLog(TemplateParser.class);
	
	private ASTNode headNode = new JobNoOpNode(null);
	private ASTNode curNode = headNode;
	private List<NumberedLine> pre = null;
	private List<NumberedLine> post = null;
	
	private boolean processedPre = false;
	private boolean inScript = false;
	private boolean firstScript = true;

//	private boolean inSingleLine = false; 
//	private boolean lastSingleLine = false; 
	
	private TemplateParser(List<NumberedLine> pre, List<NumberedLine> post) {
		this.pre = pre;
		this.post = post;
	}

	private void addScriptLine(NumberedLine line) throws ASTParseException {
		curNode = curNode.parseLine(line);
	}

	private void addBodyLine(String body, NumberedLine line, boolean endOfLine) throws ASTParseException {
//		if (inSingleLine) {
//			if (!lastSingleLine) {
//				curNode = new StartBodyNode(curNode);
//			}
//			lastSingleLine = true;
//		} else if (lastSingleLine) {
//			lastSingleLine = false;
//			curNode = new EndBodyNode(curNode);
//		}
//
		curNode = curNode.parseBody(body, line, endOfLine);
	}

	
	public void parseLine(NumberedLine line) throws ASTParseException {
		String l = line.getLine();
		String buf = "";
		boolean inSingleLine = false;
//		curNode = new StartBodyNode(curNode);

		while (l.length() > 0) {
			if (!inScript && l.startsWith("\\\\<%")) {
				buf += "\\";
				l = l.substring(2);
			} else if (!inScript && l.startsWith("\\<%")) {
					buf += "<%";
					l = l.substring(3);
			} else if (!inScript && l.startsWith("<%")) {
				if (!buf.equals("")) {
					parseString(buf, line, inSingleLine);
					buf = "";
				}
				inSingleLine = true;
				inScript = true;
				l = l.substring(2);
			} else if (inScript && l.startsWith("%>")) {
				if (!buf.equals("")) {
					parseString(buf, line, inSingleLine);
					buf = "";
				}
				firstScript = false;
				inScript = false;
				l = l.substring(2);
			} else {
				buf += l.charAt(0);
				l = l.substring(1);
			}
		}
		if (!buf.equals("")) {
			parseString(buf, line, inSingleLine);
		}
//		curNode = new EndBodyNode(curNode);
	}
	
	private void parseString(String s, NumberedLine line, boolean endOfLine) throws ASTParseException {
		if (!firstScript || !inScript) {
			processPre();
		}
		if (inScript) {
			addScriptLine(new NumberedLine(s, line));
		} else {
			if (StringUtils.strip(s).length() > 0) {
				addBodyLine(s, line, endOfLine);
			}
		}
	}

	private void processPre() throws ASTParseException {
		if (!processedPre && pre != null) {
			processedPre = true;
			boolean curInScript = inScript;
			inScript = false;
			curNode = curNode.parseLine(new NumberedLine("if !job.nopre"));
			int indent = -1;
			for (NumberedLine line: pre) {
				if (indent == -1) {
					indent = line.calcLinePrefixLength();
				}
				parseLine(line.stripPrefix(indent));
			}
			curNode = curNode.parseLine(new NumberedLine("endif"));
			inScript = curInScript;
		}
	}

	private void processPost() throws ASTParseException {
		if (curNode != headNode && post != null) {
			curNode = curNode.parseLine(new NumberedLine("if !job.nopost"));
			int indent = -1;
			for (NumberedLine line: post) {
				if (indent == -1) {
					indent = line.calcLinePrefixLength();
				}
				parseLine(line.stripPrefix(indent));
			}
			curNode = curNode.parseLine(new NumberedLine("endif"));
		}
	}

	public void exec(RootContext context) throws ASTExecException {
		// any print statements should add to the template body...
		context.setOutputStream(null);
//		headNode.dump();
		ASTNode current = headNode;
		while (current != null) {
			current = current.exec(context);
		}
	}

	static public String parseTemplateString(String src, RootContext context) throws ASTExecException, ASTParseException {
		List<NumberedLine> lines = new ArrayList<NumberedLine>();
		int i=1;
		for (String line: src.split("\n")) {
			if (line.trim().length() > 0) {
				lines.add(new NumberedLine(line, i++));
			}
		}
		return parseTemplate(lines, null, null, context);
	}

	static public String parseTemplate(String filename) throws ASTExecException, ASTParseException {
		return parseTemplate(filename, SourceLoader.getDefaultLoader());
	}

	static public String parseTemplate(String filename, SourceLoader loader) throws ASTExecException, ASTParseException {
		Source source;
		try {
			if (filename.equals("-")) {
				return parseTemplate("-", System.in, loader);
			} else {
				source = loader.loadPipeline(filename);
			}
		} catch (IOException e) {
			log.error("Error loading file: "+filename);
			throw new ASTParseException(e);
		}

		if (source == null) {
			log.error("Error loading file: "+filename);
			throw new ASTParseException("Error loading file: "+filename);
		}
		
		return parseTemplate(source);
	}

	static public String parseTemplate(String name, InputStream is) throws ASTExecException, ASTParseException {
		return parseTemplate(name, is, SourceLoader.getDefaultLoader());
	}

	static public String parseTemplate(String name, InputStream is, SourceLoader loader) throws ASTExecException, ASTParseException {
		Source source;
		try {
			source = loader.loadPipeline(is, name);
		} catch (IOException e) {
			log.error("Error loading file: "+name, e);
			throw new ASTParseException(e);
		}
		
		return parseTemplate(source);
	}
	
	static public String parseTemplate(Source source) throws ASTExecException, ASTParseException {
		return parseTemplate(source.getLines(), null, null, null);
	}

	static public String parseTemplate(List<NumberedLine> lines, List<NumberedLine> pre, List<NumberedLine> post, RootContext rootContext) throws ASTExecException, ASTParseException {
		TemplateParser parser = new TemplateParser(pre, post);
		int indent = -1;
		if (lines != null) {
			for (NumberedLine line: lines) {
				if (indent == -1) {
					indent = line.calcLinePrefixLength();
				}
				
				parser.parseLine(line.stripPrefix(indent));
			}
		}
		
		parser.processPost();
		
		// Eval AST
		RootContext jobRoot = rootContext;
		if (rootContext == null) {
			jobRoot = new RootContext();
		}

		parser.exec(jobRoot);
		return jobRoot.getBody();

	}
}
