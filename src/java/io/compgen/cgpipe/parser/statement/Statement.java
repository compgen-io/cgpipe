package io.compgen.cgpipe.parser.statement;

import io.compgen.cgpipe.exceptions.ASTParseException;
import io.compgen.cgpipe.parser.node.ASTNode;
import io.compgen.cgpipe.parser.tokens.TokenList;

public interface Statement {
	public ASTNode parse(ASTNode parent, TokenList tokens) throws ASTParseException;
	public String getName();
	
	public static final Statement PRINT = new Print();
	public static final Statement IF    = new If();
	public static final Statement ELSE  = new Else();
	public static final Statement ELIF  = new ElseIf();
	public static final Statement ENDIF = new EndIf();
	public static final Statement FOR = new For();
	public static final Statement DONE = new Done();
	public static final Statement INCLUDE = new Include();
	public static final Statement IMPORT = new Import();
	public static final Statement EVAL = new Eval();
	public static final Statement IN = new In();
	public static final Statement UNSET = new Unset();
	public static final Statement EXIT = new Exit();
	public static final Statement LOG = new WriteLog();
	public static final Statement DUMPVARS = new DumpVars();
	public static final Statement SLEEP = new Sleep();
	public static final Statement SHOWHELP = new ShowHelp();
	
	public static final Statement[] statements = {
		PRINT,
		IF,
		ELSE,
		ELIF,
		ENDIF,
		FOR,
		DONE,
		INCLUDE,
		IMPORT,
		EVAL,
		IN,
		UNSET,
		EXIT,
		LOG,
		DUMPVARS, 
		SLEEP,
		SHOWHELP
	};
}
