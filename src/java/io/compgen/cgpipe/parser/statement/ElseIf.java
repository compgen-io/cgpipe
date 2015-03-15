package io.compgen.cgpipe.parser.statement;

import io.compgen.cgpipe.exceptions.ASTParseException;
import io.compgen.cgpipe.parser.node.ASTNode;
import io.compgen.cgpipe.parser.node.ConditionalNode;
import io.compgen.cgpipe.parser.tokens.TokenList;

public class ElseIf implements Statement {

	@Override
	public ASTNode parse(ASTNode parent, final TokenList tokens) throws ASTParseException {
		ASTNode p = parent;
		while (p != null) {
			if (p instanceof ConditionalNode) {
				ConditionalNode condNode = (ConditionalNode) p;
				if (!condNode.isDone()) {
					condNode.elseClause(tokens.subList(1));
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
		return "elif";
	}
}
