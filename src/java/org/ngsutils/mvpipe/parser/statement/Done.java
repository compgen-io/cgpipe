package org.ngsutils.mvpipe.parser.statement;

import org.ngsutils.mvpipe.exceptions.ASTParseException;
import org.ngsutils.mvpipe.parser.node.IteratingNode;
import org.ngsutils.mvpipe.parser.node.ASTNode;
import org.ngsutils.mvpipe.parser.tokens.TokenList;

public class Done implements Statement {

	@Override
	public ASTNode parse(ASTNode parent, final TokenList tokens) throws ASTParseException {
		ASTNode p = parent;
		while (p != null) {
			if (p instanceof IteratingNode) {
				IteratingNode iterNode = (IteratingNode) p;
				if (!iterNode.isDone()) {
					iterNode.done();
					return null;
				}
			}
			p = p.getParent();
		}
		
		throw new ASTParseException("done outside of for-loop context!", tokens);
	}

	@Override
	public String getName() {
		return "done";
	}
}
