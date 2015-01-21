package org.ngsutils.mvpipe.parser.op;

import org.ngsutils.mvpipe.exceptions.SyntaxException;
import org.ngsutils.mvpipe.parser.Tokens;
import org.ngsutils.mvpipe.parser.context.ExecContext;
import org.ngsutils.mvpipe.parser.variable.VarValue;

public interface Operator {
	public VarValue eval(ExecContext context, VarValue lstr, VarValue rstr) throws SyntaxException;
	public VarValue eval(ExecContext context, Tokens ltok, VarValue rval) throws SyntaxException;
	public boolean evalLeft();
}
