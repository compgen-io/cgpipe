package org.ngsutils.mvpipe.parser.op;

import org.ngsutils.mvpipe.parser.SyntaxException;
import org.ngsutils.mvpipe.parser.context.ExecContext;
import org.ngsutils.mvpipe.parser.variable.VarValue;

public class Assign implements Operator {

	@Override
	public VarValue eval(ExecContext context, VarValue lstr, VarValue rval) throws SyntaxException {
		context.set(lstr.toString(), rval);
		return rval;
	}
	@Override
	public boolean evalLeft() {
		return false;
	}
}
