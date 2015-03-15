package io.compgen.mvpipe.parser.statement;

import io.compgen.mvpipe.exceptions.ASTParseException;
import io.compgen.mvpipe.parser.node.ASTNode;
import io.compgen.mvpipe.parser.node.IteratingNode;
import io.compgen.mvpipe.parser.tokens.TokenList;

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
