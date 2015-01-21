package org.ngsutils.mvpipe.parser.op;

import org.ngsutils.mvpipe.exceptions.SyntaxException;
import org.ngsutils.mvpipe.parser.Tokens;
import org.ngsutils.mvpipe.parser.context.ExecContext;
import org.ngsutils.mvpipe.parser.variable.VarValue;

public class Assign implements Operator {

	@Override
	public VarValue eval(ExecContext context, VarValue lval, VarValue rval) throws SyntaxException {
		throw new SyntaxException("Unsupported syntax for operator " + this.getClass().getSimpleName());
	}

	@Override
	public VarValue eval(ExecContext context, Tokens ltokens, VarValue rval) throws SyntaxException {
		context.set(ltokens.get(0), rval);
		return rval;
	}
	@Override
	public boolean evalLeft() {
		return false;
	}
}
