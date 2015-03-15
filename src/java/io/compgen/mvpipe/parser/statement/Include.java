package io.compgen.mvpipe.parser.statement;

import io.compgen.mvpipe.exceptions.ASTParseException;
import io.compgen.mvpipe.parser.node.ASTNode;
import io.compgen.mvpipe.parser.node.IncludeNode;
import io.compgen.mvpipe.parser.tokens.TokenList;

public class Include implements Statement {
	
	@Override
	public ASTNode parse(ASTNode parent, final TokenList tokens) throws ASTParseException {
		return new IncludeNode(parent, tokens.subList(1));
	}

	@Override
	public String getName() {
		return "include";
	}
}
