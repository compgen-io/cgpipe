package org.ngsutils.mvpipe.parser.op;

import org.ngsutils.mvpipe.parser.SyntaxException;
import org.ngsutils.mvpipe.parser.context.ExecContext;
import org.ngsutils.mvpipe.parser.variable.VarValue;

public interface Operator {
	public VarValue eval(ExecContext context, String lstr, VarValue rstr) throws SyntaxException;
}
