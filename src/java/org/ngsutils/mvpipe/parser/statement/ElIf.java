package org.ngsutils.mvpipe.parser.statement;

import org.ngsutils.mvpipe.parser.Eval;
import org.ngsutils.mvpipe.parser.SyntaxException;
import org.ngsutils.mvpipe.parser.Tokens;
import org.ngsutils.mvpipe.parser.context.ExecContext;
import org.ngsutils.mvpipe.parser.variable.VarValue;

public class ElIf implements Statement {

	@Override
	public ExecContext eval(ExecContext context, Tokens tokens) throws SyntaxException {

		// If previous context was ever active, we will not run
		
		if (!context.wasCurrentLevelEverActive()) {
			VarValue test = Eval.evalTokenExpression(context, tokens);
			System.err.println("#ELIF TEST RESULT: " + test);
			if (test.isTrue()) {
				context.switchActive();
			}
		} else {
			System.err.println("#ELIF SKIPPED!");
		}
		return context;
	}

}
