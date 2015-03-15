package io.compgen.mvpipe.parser.statement;

import io.compgen.mvpipe.exceptions.ASTParseException;
import io.compgen.mvpipe.parser.node.ASTNode;
import io.compgen.mvpipe.parser.node.ConditionalNode;
import io.compgen.mvpipe.parser.tokens.TokenList;

public class If implements Statement {
	
	@Override
	public ASTNode parse(ASTNode parent, final TokenList tokens) throws ASTParseException {
		return new ConditionalNode(parent, tokens.subList(1));
	}

	@Override
	public String getName() {
		return "if";
	}
}
