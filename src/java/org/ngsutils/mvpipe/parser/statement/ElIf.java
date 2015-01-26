package org.ngsutils.mvpipe.parser.statement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ngsutils.mvpipe.exceptions.SyntaxException;
import org.ngsutils.mvpipe.parser.Eval;
import org.ngsutils.mvpipe.parser.Tokens;
import org.ngsutils.mvpipe.parser.context.ExecContext;
import org.ngsutils.mvpipe.parser.variable.VarValue;

public class ElIf implements Statement {
	private Log log = LogFactory.getLog(getClass());
	@Override
	public ExecContext eval(ExecContext context, Tokens tokens) throws SyntaxException {

		// If previous context was ever active, we will not run
		
		if (!context.wasCurrentLevelEverActive()) {
			VarValue test = Eval.evalTokenExpression(context, tokens);
			log.trace("ELIF TEST RESULT: " + test);
			if (test.isTrue()) {
				context.switchActive();
			}
		} else {
			log.trace("ELIF SKIPPED!");
		}
		return context;
	}

}
