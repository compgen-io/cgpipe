package org.ngsutils.mvpipe.parser.statement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ngsutils.mvpipe.exceptions.SyntaxException;
import org.ngsutils.mvpipe.parser.Eval;
import org.ngsutils.mvpipe.parser.Tokens;
import org.ngsutils.mvpipe.parser.context.ExecContext;
import org.ngsutils.mvpipe.parser.context.NestedContext;
import org.ngsutils.mvpipe.parser.variable.VarValue;

public class If implements Statement {
	private Log log = LogFactory.getLog(getClass());

	@Override
	public ExecContext eval(ExecContext context, Tokens tokens) throws SyntaxException {
		if (context.isActive()) {
			VarValue test = Eval.evalTokenExpression(context, tokens);
			log.trace("IF TEST RESULT: " + test.isTrue());
			
			ExecContext nested = new NestedContext(context, test.isTrue(), true);
			return nested;		
		}
		else {
			// TODO: check this...
			ExecContext nested = new NestedContext(context, false, true);
			return nested;		
		}
	}

}
