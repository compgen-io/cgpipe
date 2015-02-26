package org.ngsutils.mvpipe.parser.statement;

import org.ngsutils.mvpipe.exceptions.SyntaxException;
import org.ngsutils.mvpipe.parser.Eval;
import org.ngsutils.mvpipe.parser.Tokens;
import org.ngsutils.mvpipe.parser.context.ExecContext;

public class Print implements Statement {

	@Override
	public ExecContext eval(ExecContext context, Tokens tokens) throws SyntaxException {
		if (context.isActive()) {
			System.out.println(Eval.evalTokenExpression(context, tokens));
		}
		return context;
	}
}
