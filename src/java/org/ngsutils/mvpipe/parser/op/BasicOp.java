package org.ngsutils.mvpipe.parser.op;

import org.ngsutils.mvpipe.parser.SyntaxException;
import org.ngsutils.mvpipe.parser.context.ExecContext;
import org.ngsutils.mvpipe.parser.variable.VarValue;

public abstract class BasicOp implements Operator {

	@Override	
	abstract public VarValue eval(ExecContext context, VarValue lstr, VarValue rval) throws SyntaxException;

	@Override
	public boolean evalLeft() {
		return true;
	}
}
