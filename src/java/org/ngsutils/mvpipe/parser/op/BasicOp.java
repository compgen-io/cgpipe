package org.ngsutils.mvpipe.parser.op;

import org.ngsutils.mvpipe.exceptions.SyntaxException;
import org.ngsutils.mvpipe.parser.Tokens;
import org.ngsutils.mvpipe.parser.context.ExecContext;
import org.ngsutils.mvpipe.parser.variable.VarValue;

public abstract class BasicOp implements Operator {

	@Override	
	abstract public VarValue eval(ExecContext context, VarValue lstr, VarValue rval) throws SyntaxException;
	
	public VarValue eval(ExecContext context, Tokens ltokens, VarValue rval) throws SyntaxException {
		throw new SyntaxException("Unsupported syntax for operator " + this.getClass().getSimpleName());
	}

	@Override
	public boolean evalLeft() {
		return true;
	}
}
