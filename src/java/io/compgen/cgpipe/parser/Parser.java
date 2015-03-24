package io.compgen.cgpipe.parser;

import io.compgen.cgpipe.exceptions.ASTExecException;
import io.compgen.cgpipe.exceptions.ASTParseException;
import io.compgen.cgpipe.parser.context.ExecContext;
import io.compgen.cgpipe.parser.node.ASTNode;
import io.compgen.cgpipe.parser.node.NoOpNode;
import io.compgen.support.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Parser {
	private boolean readOnly = false;
	private ASTNode headNode = null;
	private ASTNode currentNode = null;
	private String path=null;
	
	public Parser() {
		headNode = new NoOpNode(null);
		currentNode = headNode;
	}
	
	private void load(InputStream is, String filename, String path) throws ASTParseException {
		if (readOnly) {
			throw new ASTParseException("AST set - can't add another line!");
		}

		this.path = path;

		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		String line;

		int linenum = 0;
		NumberedLine curLine = null;
		
		try {
			while ((line = reader.readLine()) != null) {
				line = StringUtils.rstrip(line);
				linenum++;
				curLine = new NumberedLine(filename, linenum, line);
				currentNode = currentNode.parseLine(curLine);
			}
		} catch (IOException e) {
			throw new ASTParseException(e, curLine);
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
	
		if (path != null) {
			context.getRoot().pushCWD(path);
		}
		
		currentNode = headNode;
		
		while (currentNode != null) {
			currentNode = currentNode.exec(context);
		}

		if (path!=null) {
			context.getRoot().popCWD();
		}
	}
	
	static public Parser parseAST(InputStream is, String filename, String path) throws ASTParseException {
		Parser parser =  new Parser();
		parser.load(is, filename, path);
		return parser;
	}

	static public Parser parseAST(String filename) throws ASTParseException {
		if (filename.equals("-")) {
			return parseAST(System.in, null, null);
		}
		return parseAST(new File(filename));
	}
	
	static public Parser parseAST(File file) throws ASTParseException {
		try {
			FileInputStream is = new FileInputStream(file);
			return parseAST(is, file.getCanonicalPath(), file.getParent());
		} catch (IOException e) {
			throw new ASTParseException(e);
		}
	}

	public static void exec(String filename, ExecContext context) throws ASTParseException, ASTExecException {
		Parser parser = parseAST(filename);
//		parser.dump();
		parser.exec(context);
	}
	
	public static void exec(File file, ExecContext context) throws ASTParseException, ASTExecException {
		Parser parser = parseAST(file);
//		parser.dump();
		parser.exec(context);
		
	}
	
}
