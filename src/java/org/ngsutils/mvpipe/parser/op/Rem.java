package org.ngsutils.mvpipe.parser.op;

import org.ngsutils.mvpipe.exceptions.ASTExecException;
import org.ngsutils.mvpipe.exceptions.VarTypeException;
import org.ngsutils.mvpipe.parser.context.ExecContext;
import org.ngsutils.mvpipe.parser.variable.VarValue;

public class Rem extends BasicOp {

	@Override
	public VarValue eval(ExecContext context, VarValue lval, VarValue rval) throws ASTExecException {
		try {
			return lval.rem(rval);
		} catch (VarTypeException e) {
			throw new ASTExecException(e);
		}
	}

	@Override
	public String getSymbol() {
		return "%";
	}

	@Override
	public int getPriority() {
		return 10;
	}

}
