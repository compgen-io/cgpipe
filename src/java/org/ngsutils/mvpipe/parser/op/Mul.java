package org.ngsutils.mvpipe.parser.op;

import org.ngsutils.mvpipe.exceptions.SyntaxException;
import org.ngsutils.mvpipe.exceptions.VarTypeException;
import org.ngsutils.mvpipe.parser.context.ExecContext;
import org.ngsutils.mvpipe.parser.variable.VarValue;

public class Mul extends BasicOp {

	@Override
	public VarValue eval(ExecContext context, VarValue lval, VarValue rval) throws SyntaxException {
		try {
			return lval.mul(rval);
		} catch (VarTypeException e) {
			throw new SyntaxException(e);
		}
	}
}
