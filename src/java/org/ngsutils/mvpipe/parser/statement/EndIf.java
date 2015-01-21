package org.ngsutils.mvpipe.parser.statement;

import org.ngsutils.mvpipe.exceptions.SyntaxException;
import org.ngsutils.mvpipe.parser.Tokens;
import org.ngsutils.mvpipe.parser.context.ExecContext;

public class EndIf implements Statement {

	@Override
	public ExecContext eval(ExecContext context, Tokens tokens) throws SyntaxException {
		if (tokens.size() > 0) {
			throw new SyntaxException("Extra fields on endif line!");
		}
		return context.getParent();
	}

}
