package io.compgen.mvpipe.parser.op;

import io.compgen.mvpipe.exceptions.ASTExecException;
import io.compgen.mvpipe.exceptions.VarTypeException;
import io.compgen.mvpipe.parser.context.ExecContext;
import io.compgen.mvpipe.parser.variable.VarValue;

public class Lt extends BasicOp {

	@Override
	public VarValue eval(ExecContext context, VarValue lval, VarValue rval) throws ASTExecException {
		try {
			return lval.lt(rval);
		} catch (VarTypeException e) {
			throw new ASTExecException(e);
		}
	}

	@Override
	public String getSymbol() {
		return "<";
	}

	@Override
	public int getPriority() {
		return 100;
	}

}
