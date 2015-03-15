package io.compgen.mvpipe.parser.statement;

import io.compgen.mvpipe.exceptions.ASTParseException;
import io.compgen.mvpipe.parser.node.ASTNode;
import io.compgen.mvpipe.parser.tokens.TokenList;

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
	public static final Statement IN = new In();
	
	public static final Statement[] statements = {
		PRINT,
		IF,
		ELSE,
		ELIF,
		ENDIF,
		FOR,
		DONE,
		INCLUDE,
		IN
	};
}
