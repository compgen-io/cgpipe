package io.compgen.cgpipe.parser.statement;

import io.compgen.cgpipe.exceptions.ASTParseException;
import io.compgen.cgpipe.parser.node.ASTNode;
import io.compgen.cgpipe.parser.tokens.TokenList;

public class In implements Statement {

	@Override
	public ASTNode parse(ASTNode parent, final TokenList tokens) throws ASTParseException {
		// this should never be called...
		throw new ASTParseException("in outside of for-loop context!", tokens);
	}

	@Override
	public String getName() {
		return "in";
	}
}
