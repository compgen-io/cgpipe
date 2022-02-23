package io.compgen.cgpipe.parser.statement;

import io.compgen.cgpipe.exceptions.ASTExecException;
import io.compgen.cgpipe.exceptions.ASTParseException;
import io.compgen.cgpipe.parser.Eval;
import io.compgen.cgpipe.parser.context.ExecContext;
import io.compgen.cgpipe.parser.node.ASTNode;
import io.compgen.cgpipe.parser.tokens.TokenList;
import io.compgen.cgpipe.parser.variable.VarValue;

public class Sleep implements Statement {

	@Override
	public ASTNode parse(ASTNode parent, final TokenList tokens) throws ASTParseException {

		final TokenList expr = tokens.subList(1);

		return new ASTNode(parent, expr) {
			@Override
			public ASTNode exec(ExecContext context) throws ASTExecException {
				VarValue val = Eval.evalTokenExpression(expr, context);
				if (val == null) {
					throw new ASTExecException("Error evaluating expression: " + expr);
				}

				final double secToSleep = Double.parseDouble(val.toString());
				try {
					Thread.sleep((long) (secToSleep * 1000));
				} catch (InterruptedException e) {
				}

				return next;
			}
			public String dumpString() {
				return "[sleep]";
			}
		};
	}

	@Override
	public String getName() {
		return "sleep";
	}
}
