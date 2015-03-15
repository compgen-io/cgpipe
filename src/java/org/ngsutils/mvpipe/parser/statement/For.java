package org.ngsutils.mvpipe.parser.statement;

import org.ngsutils.mvpipe.exceptions.ASTParseException;
import org.ngsutils.mvpipe.parser.node.IteratingNode;
import org.ngsutils.mvpipe.parser.node.ASTNode;
import org.ngsutils.mvpipe.parser.tokens.TokenList;

public class For implements Statement {
	
	@Override
	public ASTNode parse(ASTNode parent, final TokenList tokens) throws ASTParseException {
		return new IteratingNode(parent, tokens.subList(1));
	}

	@Override
	public String getName() {
		return "for";
	}
}
