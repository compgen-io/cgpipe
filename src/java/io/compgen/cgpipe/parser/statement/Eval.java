package io.compgen.cgpipe.parser.statement;

import io.compgen.cgpipe.exceptions.ASTParseException;
import io.compgen.cgpipe.parser.node.ASTNode;
import io.compgen.cgpipe.parser.node.EvalNode;
import io.compgen.cgpipe.parser.tokens.TokenList;

public class Eval implements Statement {
	
	@Override
	public ASTNode parse(ASTNode parent, final TokenList tokens) throws ASTParseException {
		return new EvalNode(parent, tokens.subList(1));
	}

	@Override
	public String getName() {
		return "eval";
	}
}
