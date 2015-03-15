package io.compgen.mvpipe.parser.statement;

import io.compgen.mvpipe.exceptions.ASTParseException;
import io.compgen.mvpipe.parser.node.ASTNode;
import io.compgen.mvpipe.parser.node.ConditionalNode;
import io.compgen.mvpipe.parser.tokens.TokenList;

public class EndIf implements Statement {

	@Override
	public ASTNode parse(ASTNode parent, final TokenList tokens) throws ASTParseException {
		ASTNode p = parent;
		while (p != null) {
			if (p instanceof ConditionalNode) {
				ConditionalNode condNode = (ConditionalNode) p;
				if (!condNode.isDone()) {
					condNode.endif();
					return null;
				}
			}
			p = p.getParent();
		}
		
		throw new ASTParseException("endif outside of if-then context!", tokens);
	}

	@Override
	public String getName() {
		return "endif";
	}
}
