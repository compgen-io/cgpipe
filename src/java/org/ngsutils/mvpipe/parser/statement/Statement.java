package org.ngsutils.mvpipe.parser.statement;

import org.ngsutils.mvpipe.exceptions.SyntaxException;
import org.ngsutils.mvpipe.parser.Tokens;
import org.ngsutils.mvpipe.parser.context.ExecContext;

public interface Statement {
	public ExecContext eval(ExecContext context, Tokens tokens) throws SyntaxException;
}
