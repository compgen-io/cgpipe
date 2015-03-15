package org.ngsutils.mvpipe.parser.op;

import org.ngsutils.mvpipe.exceptions.ASTExecException;
import org.ngsutils.mvpipe.parser.context.ExecContext;
import org.ngsutils.mvpipe.parser.variable.VarBool;
import org.ngsutils.mvpipe.parser.variable.VarValue;

public class And extends BasicOp {

	@Override
	public VarValue eval(ExecContext context, VarValue lval, VarValue rval) throws ASTExecException {
		return (lval.isTrue() && rval.isTrue()) ? VarBool.TRUE : VarBool.FALSE;
	}

	@Override
	public String getSymbol() {
		return "&&";
	}

	@Override
	public int getPriority() {
		return 200;
	}

}
