package org.ngsutils.mvpipe.parser.statement;

import java.util.Map;

import org.ngsutils.mvpipe.exceptions.SyntaxException;
import org.ngsutils.mvpipe.parser.Tokens;
import org.ngsutils.mvpipe.parser.context.ExecContext;
import org.ngsutils.mvpipe.parser.variable.VarValue;

public class DumpVars implements Statement {

	@Override
	public ExecContext eval(ExecContext context, Tokens tokens) throws SyntaxException {
		Map<String, VarValue> state = context.cloneValues();
		for (String k: state.keySet()) {
			System.err.println(k + " => " + state.get(k));
		}
		return context;
	}

}
