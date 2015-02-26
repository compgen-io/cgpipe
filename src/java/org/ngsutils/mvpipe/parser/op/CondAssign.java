package org.ngsutils.mvpipe.parser.op;

import org.ngsutils.mvpipe.exceptions.SyntaxException;
import org.ngsutils.mvpipe.parser.Tokens;
import org.ngsutils.mvpipe.parser.context.ExecContext;
import org.ngsutils.mvpipe.parser.variable.VarValue;

public class CondAssign implements Operator {
	
	@Override
	public VarValue eval(ExecContext context, VarValue lval, VarValue rval) throws SyntaxException {
		throw new SyntaxException("Unsupported syntax for operator " + this.getClass().getSimpleName());
	}

	@Override
	public VarValue eval(ExecContext context, Tokens ltokens, VarValue rval) throws SyntaxException {
		String lname = ltokens.get(0);
		if (!context.contains(lname)) {
			context.set(lname, rval);
		}
		return context.get(lname);
	}
	@Override
	public boolean evalLeft() {
		return false;
	}
}
