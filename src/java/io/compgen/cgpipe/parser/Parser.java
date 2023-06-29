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
import io.compgen.cgpipe.parser.context.ExecContext;
import io.compgen.cgpipe.parser.node.ASTNode;
import io.compgen.cgpipe.parser.node.NoOpNode;
import io.compgen.common.StringUtils;

public class Parser {
	private boolean readOnly = false;
	private ASTNode headNode = null;
	private ASTNode currentNode = null;
	static private Log log = LogFactory.getLog(Parser.class);

	public Parser() {
		headNode = new NoOpNode(null);
		currentNode = headNode;
	}
	
	private void load(Source source) throws ASTParseException {
		if (readOnly) {
			throw new ASTParseException("AST set - can't add another line!");
		}

		for (NumberedLine curLine: source.getLines()) {
			log.debug(curLine);
			currentNode = currentNode.parseLine(curLine);
		}
		
		this.readOnly = true;
	}
	
	public void dump() throws ASTExecException {
		if (!readOnly) {
			throw new ASTExecException("AST not finalized - can't exec yet!");
		}
		
		headNode.dump();
	}

	public void dump(int indent) throws ASTExecException {
		if (!readOnly) {
			throw new ASTExecException("AST not finalized - can't exec yet!");
		}
		
		headNode.dump(indent);
	}

	public void exec(ExecContext context) throws ASTExecException {
		if (!readOnly) {
			throw new ASTExecException("AST not finalized - can't exec yet!");
		}
	
		currentNode = headNode;
		
		while (currentNode != null) {
			currentNode = currentNode.exec(context);
		}
	}
	

	
	static public Parser parseAST(String filename) throws ASTParseException {
		return parseAST(filename, SourceLoader.getDefaultLoader());
	}

	static public Parser parseAST(String filename, SourceLoader loader) throws ASTParseException {
		Source source;
		try {
			if (filename.equals("-")) {
				return parseAST("-", System.in, loader);
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
		
		Parser parser = new Parser();
		parser.load(source);
		return parser;
	}

	static public Parser parseAST(String name, InputStream is) throws ASTParseException {
		return parseAST(name, is, SourceLoader.getDefaultLoader());
	}

	static public Parser parseAST(String name, InputStream is, SourceLoader loader) throws ASTParseException {
		Source source;
		try {
			source = loader.loadPipeline(is, name);
		} catch (IOException e) {
			log.error("Error loading file: "+name, e);
			throw new ASTParseException(e);
		}
		
		Parser parser = new Parser();
		parser.load(source);
		return parser;
	}

	static public Parser parseASTEval(String lines, SourceLoader loader) throws ASTParseException {
		Source source = loader.loadPipelineDirect(lines.split("\n"));
		
		Parser parser = new Parser();
		parser.load(source);
		return parser;
	}

	static public Parser parseASTEval(String[] lines) throws ASTParseException {
		Source source = SourceLoader.getDefaultLoader().loadPipelineDirect(lines);
		Parser parser = new Parser();
		parser.load(source);
		return parser;
	}

	public static void exec(String filename, ExecContext context) throws ASTParseException, ASTExecException {
		Parser parser = parseAST(filename);
//		parser.headNode.dump();
		parser.exec(context);
	}

	public static void exec(String name, InputStream is, ExecContext context) throws ASTParseException, ASTExecException {
		Parser parser = parseAST(name, is);
//		parser.headNode.dump();
		parser.exec(context);
	}

	public static void eval(String[] lines, ExecContext context) throws ASTParseException, ASTExecException {
		Parser parser = parseASTEval(lines);
//		parser.headNode.dump();
		parser.exec(context);
	}

	public static void eval(String command, ExecContext context) throws ASTParseException, ASTExecException {
		String[] lines = new String[]{command};
		Parser parser = parseASTEval(lines);
//		parser.headNode.dump();
		parser.exec(context);
	}

	public static void showHelp(String name) throws IOException {
		System.out.println(getHelp(name));
	}
	public static String getHelp(String name) throws IOException {
		List<String> outlines = new ArrayList<String>();
		boolean first = true;
		Source pipe = SourceLoader.getDefaultLoader().loadPipeline(name);
		if (pipe == null) {
			throw new IOException("Error loading file: "+name);
		}
		for (NumberedLine line: pipe.getLines()) {
			if (first && line.getLine().startsWith("#!")) {
				first = false;
				continue;
			}
			first = false;
			
			if (!line.getLine().startsWith("#")) {
				break;
			}
			outlines.add(line.getLine().substring(1));
		}
		
		int indent = -1;
		for (String line:outlines) {
			if (StringUtils.strip(line).length()>0) {
				int lineIndent = 0;			
				for (int i=0; i<line.length(); i++) {
					if (line.charAt(i) == ' ') {
						lineIndent++;
					} else {
						break;
					}
				}
				if (indent == -1 || lineIndent < indent) {
					indent = lineIndent;
				}
			}
		}

		String ret = "";

		if (indent>0) {
			for (String line:outlines) {
				if (line.length()>indent) {
					ret += line.substring(indent) + "\n";
				} else {
					ret += "" + "\n";
				}
			}
		} else {
			for (String line:outlines) {
				ret += line + "\n";
			}
		}
		
		return ret;
	}
}
