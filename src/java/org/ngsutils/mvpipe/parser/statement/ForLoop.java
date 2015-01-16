package org.ngsutils.mvpipe.parser.statement;

import org.ngsutils.mvpipe.parser.Eval;
import org.ngsutils.mvpipe.parser.SyntaxException;
import org.ngsutils.mvpipe.parser.Tokens;
import org.ngsutils.mvpipe.parser.context.ExecContext;
import org.ngsutils.mvpipe.parser.context.IteratingContext;
import org.ngsutils.mvpipe.parser.variable.VarValue;

public class ForLoop implements Statement {

	@Override
	public ExecContext eval(ExecContext context, Tokens tokens) throws SyntaxException {
		
		if (!tokens.get(1).equals("in")) {
			throw new SyntaxException("Unknown for-loop syntax (missing \"in\")!");	
		}
		
		String varname = tokens.get(0);
		VarValue range = Eval.evalTokenExpression(context, tokens.subList(2, tokens.size()));
		
		System.err.println("# new for-loop context! "+ range);
		
		return new IteratingContext(context, varname, range);
	}

}
