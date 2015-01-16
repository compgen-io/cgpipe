package org.ngsutils.mvpipe.parser.op;

import org.ngsutils.mvpipe.parser.SyntaxException;
import org.ngsutils.mvpipe.parser.context.ExecContext;
import org.ngsutils.mvpipe.parser.variable.VarBool;
import org.ngsutils.mvpipe.parser.variable.VarValue;

public class Not extends BasicOp {

	@Override
	public VarValue eval(ExecContext context, VarValue lval, VarValue rval) throws SyntaxException {
		
		if (rval.isTrue()) {
			return VarBool.FALSE;
		}
		return VarBool.TRUE;
	}
}
