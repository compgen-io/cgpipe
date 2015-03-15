package org.ngsutils.mvpipe.parser.statement;

import org.ngsutils.mvpipe.exceptions.ASTExecException;
import org.ngsutils.mvpipe.exceptions.ASTParseException;
import org.ngsutils.mvpipe.parser.Eval;
import org.ngsutils.mvpipe.parser.context.ExecContext;
import org.ngsutils.mvpipe.parser.node.ASTNode;
import org.ngsutils.mvpipe.parser.tokens.TokenList;
import org.ngsutils.mvpipe.parser.variable.VarValue;

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
