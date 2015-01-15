package org.ngsutils.mvpipe.parser.op;

import org.ngsutils.mvpipe.parser.SyntaxException;
import org.ngsutils.mvpipe.parser.context.ExecContext;
import org.ngsutils.mvpipe.parser.variable.VarBool;
import org.ngsutils.mvpipe.parser.variable.VarValue;

public class NotEq implements Operator {

	@Override
	public VarValue eval(ExecContext context, String lstr, VarValue rval)
			throws SyntaxException {
		VarValue left = VarValue.parseString(lstr, context);
		
		if (left.eq(rval) == VarBool.TRUE) {
			return VarBool.FALSE;
		}
		return VarBool.TRUE;
	}
}
