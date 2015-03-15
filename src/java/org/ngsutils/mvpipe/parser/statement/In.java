package org.ngsutils.mvpipe.parser.statement;

import org.ngsutils.mvpipe.exceptions.ASTParseException;
import org.ngsutils.mvpipe.parser.node.ASTNode;
import org.ngsutils.mvpipe.parser.tokens.TokenList;

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
