package io.compgen.mvpipe.parser.statement;

import io.compgen.mvpipe.exceptions.ASTExecException;
import io.compgen.mvpipe.exceptions.ASTParseException;
import io.compgen.mvpipe.parser.Eval;
import io.compgen.mvpipe.parser.context.ExecContext;
import io.compgen.mvpipe.parser.node.ASTNode;
import io.compgen.mvpipe.parser.tokens.TokenList;
import io.compgen.mvpipe.parser.variable.VarValue;

public class Print implements Statement {

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
				context.getRoot().println(val.toString());
				return next;
			}
			public String dumpString() {
				return "[print]" + tokens.getLine();
			}
		};
	}

	@Override
	public String getName() {
		return "print";
	}
}
