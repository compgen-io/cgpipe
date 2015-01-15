package org.ngsutils.mvpipe.parser.op;

import org.ngsutils.mvpipe.parser.SyntaxException;
import org.ngsutils.mvpipe.parser.context.ExecContext;
import org.ngsutils.mvpipe.parser.variable.VarValue;

public class Gt implements Operator {

	@Override
	public VarValue eval(ExecContext context, String lstr, VarValue rval)
			throws SyntaxException {
		VarValue left = VarValue.parseString(lstr, context);
		return left.gt(rval);
	}
}
