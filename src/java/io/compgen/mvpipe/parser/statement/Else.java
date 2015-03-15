package org.ngsutils.mvpipe.parser.statement;

import org.ngsutils.mvpipe.exceptions.ASTParseException;
import org.ngsutils.mvpipe.parser.node.ConditionalNode;
import org.ngsutils.mvpipe.parser.node.ASTNode;
import org.ngsutils.mvpipe.parser.tokens.TokenList;

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
