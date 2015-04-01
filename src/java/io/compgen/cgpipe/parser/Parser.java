package io.compgen.cgpipe.parser;

import io.compgen.cgpipe.exceptions.ASTExecException;
import io.compgen.cgpipe.exceptions.ASTParseException;
import io.compgen.cgpipe.parser.context.ExecContext;
import io.compgen.cgpipe.parser.node.ASTNode;
import io.compgen.cgpipe.parser.node.NoOpNode;
import io.compgen.cgpipe.pipeline.NumberedLine;
import io.compgen.cgpipe.pipeline.Pipeline;
import io.compgen.cgpipe.pipeline.PipelineLoader;
import io.compgen.common.StringUtils;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Parser {
	private boolean readOnly = false;
	private ASTNode headNode = null;
	private ASTNode currentNode = null;
	static private Log log = LogFactory.getLog(Parser.class);

	public Parser() {
		headNode = new NoOpNode(null);
		currentNode = headNode;
	}
	
	private void load(Pipeline pipeline) throws ASTParseException {
		if (readOnly) {
			throw new ASTParseException("AST set - can't add another line!");
		}

		for (NumberedLine curLine: pipeline.getLines()) {
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
		return parseAST(filename, PipelineLoader.getDefaultLoader());
	}

	static public Parser parseAST(String filename, PipelineLoader loader) throws ASTParseException {
		Pipeline pipeline;
		try {
			if (filename.equals("-")) {
				return parseAST("-", System.in, loader);
			} else {
				pipeline = loader.loadPipeline(filename);
			}
		} catch (IOException e) {
			log.error("Error loading file: "+filename);
			throw new ASTParseException(e);
		}

		if (pipeline == null) {
			log.error("Error loading file: "+filename);
			throw new ASTParseException("Error loading file: "+filename);
		}
		
		Parser parser = new Parser();
		parser.load(pipeline);
		return parser;
	}

	static public Parser parseAST(String name, InputStream is) throws ASTParseException {
		return parseAST(name, is, PipelineLoader.getDefaultLoader());
	}

	static public Parser parseAST(String name, InputStream is, PipelineLoader loader) throws ASTParseException {
		Pipeline pipeline;
		try {
			pipeline = loader.loadPipeline(is, name);
		} catch (IOException e) {
			log.error("Error loading file: "+name, e);
			throw new ASTParseException(e);
		}
		
		Parser parser = new Parser();
		parser.load(pipeline);
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

	public static void showHelp(String name) throws IOException {
		boolean first = true;
		Pipeline pipe = PipelineLoader.getDefaultLoader().loadPipeline(name);
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
			System.out.println(StringUtils.strip(line.getLine().substring(1)));
		}
	}
}
