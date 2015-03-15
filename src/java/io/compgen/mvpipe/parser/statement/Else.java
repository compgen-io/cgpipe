package io.compgen.mvpipe.parser.statement;

import io.compgen.mvpipe.exceptions.ASTParseException;
import io.compgen.mvpipe.parser.node.ASTNode;
import io.compgen.mvpipe.parser.node.ConditionalNode;
import io.compgen.mvpipe.parser.tokens.TokenList;

public class Else implements Statement {

	@Override
	public ASTNode parse(ASTNode parent, final TokenList tokens) throws ASTParseException {
		ASTNode p = parent;
		while (p != null) {
			if (p instanceof ConditionalNode) {
				ConditionalNode condNode = (ConditionalNode) p;
				if (!condNode.isDone()) {
					condNode.elseClause(null);
					return null;
//					return new NoOp();
				}
			}
			p = p.getParent();
		}
		
		throw new ASTParseException("else outside of if-then context! "+parent.getParent(), tokens);
	}

	@Override
	public String getName() {
		return "else";
	}
}
