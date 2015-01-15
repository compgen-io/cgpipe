package org.ngsutils.mvpipe.parser.op;

import org.ngsutils.mvpipe.parser.SyntaxException;
import org.ngsutils.mvpipe.parser.context.ExecContext;
import org.ngsutils.mvpipe.parser.variable.VarBool;
import org.ngsutils.mvpipe.parser.variable.VarValue;

public class Not implements Operator {

	@Override
	public VarValue eval(ExecContext context, String lstr, VarValue rval)
			throws SyntaxException {

		if (rval.isTrue()) {
			return VarBool.FALSE;
		}
		return VarBool.TRUE;
	}
}
