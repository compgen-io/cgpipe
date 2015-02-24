package org.ngsutils.mvpipe.parser.statement;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ngsutils.mvpipe.exceptions.SyntaxException;
import org.ngsutils.mvpipe.parser.Tokens;
import org.ngsutils.mvpipe.parser.context.ExecContext;
import org.ngsutils.mvpipe.parser.variable.VarValue;

public class DumpVars implements Statement {
	protected Log log = LogFactory.getLog(getClass());

	@Override
	public ExecContext eval(ExecContext context, Tokens tokens) throws SyntaxException {
		if (context.isActive()) {
			Map<String, VarValue> state = context.cloneValues();
			for (String k: state.keySet()) {
				log.debug(k + " => " + state.get(k));
				System.err.println(k + " => " + state.get(k));
			}
		}
		return context;
	}

}
