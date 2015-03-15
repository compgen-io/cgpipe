package io.compgen.cgpipe.parser.statement;

import io.compgen.cgpipe.exceptions.ASTExecException;
import io.compgen.cgpipe.exceptions.ASTParseException;
import io.compgen.cgpipe.parser.context.ExecContext;
import io.compgen.cgpipe.parser.node.ASTNode;
import io.compgen.cgpipe.parser.tokens.TokenList;

public class Unset implements Statement {

	@Override
	public ASTNode parse(ASTNode parent, final TokenList tokens) throws ASTParseException {

		final TokenList expr = tokens.subList(1);
		if (!expr.get(0).isVariable()) {
			throw new ASTParseException("Unable to unset a non-variable");
		}
		
		final String varName = expr.get(0).getStr();
		
		return new ASTNode(parent, expr) {
			@Override
			public ASTNode exec(ExecContext context) throws ASTExecException {
				
				if (!context.contains(varName)) {
					throw new ASTExecException("Variable not found: " + varName);
				}
				
				context.remove(varName);
				return next;
			}
			public String dumpString() {
				return "[print]" + tokens.getLine();
			}
		};
	}

	@Override
	public String getName() {
		return "unset";
	}
}
