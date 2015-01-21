package org.ngsutils.mvpipe.parser.op;

import org.ngsutils.mvpipe.exceptions.SyntaxException;
import org.ngsutils.mvpipe.parser.context.ExecContext;
import org.ngsutils.mvpipe.parser.variable.VarBool;
import org.ngsutils.mvpipe.parser.variable.VarValue;

public class And extends BasicOp {

	@Override
	public VarValue eval(ExecContext context, VarValue lval, VarValue rval) throws SyntaxException {
		if (lval.isTrue() && rval.isTrue()) {
			return VarBool.TRUE;
		}
		return VarBool.FALSE;
	}
}
