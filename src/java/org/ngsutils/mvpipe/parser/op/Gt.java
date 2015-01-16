package org.ngsutils.mvpipe.parser.op;

import org.ngsutils.mvpipe.parser.SyntaxException;
import org.ngsutils.mvpipe.parser.context.ExecContext;
import org.ngsutils.mvpipe.parser.variable.VarValue;

public class Gt extends BasicOp {

	@Override
	public VarValue eval(ExecContext context, VarValue lval, VarValue rval) throws SyntaxException {
		return lval.gt(rval);
	}
}
