package org.ngsutils.mvpipe.parser.op;

import org.ngsutils.mvpipe.parser.SyntaxException;
import org.ngsutils.mvpipe.parser.context.ExecContext;
import org.ngsutils.mvpipe.parser.variable.VarTypeException;
import org.ngsutils.mvpipe.parser.variable.VarValue;

public class Rem implements Operator {

	@Override
	public VarValue eval(ExecContext context, String lstr, VarValue rval) 
			throws SyntaxException {
		VarValue left = VarValue.parseString(lstr, context);
			
		try {
			return left.rem(rval);
		} catch (VarTypeException e) {
			throw new SyntaxException(e);
		}
	}
}
