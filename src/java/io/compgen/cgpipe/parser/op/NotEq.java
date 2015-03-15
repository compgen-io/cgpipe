package io.compgen.cgpipe.parser.op;

import io.compgen.cgpipe.exceptions.ASTExecException;
import io.compgen.cgpipe.exceptions.VarTypeException;
import io.compgen.cgpipe.parser.context.ExecContext;
import io.compgen.cgpipe.parser.variable.VarBool;
import io.compgen.cgpipe.parser.variable.VarValue;

public class NotEq extends BasicOp {

	@Override
	public VarValue eval(ExecContext context, VarValue lval, VarValue rval) throws ASTExecException {
		try {
			if (lval.eq(rval) == VarBool.TRUE) {
				return VarBool.FALSE;
			}
			return VarBool.TRUE;
		} catch (VarTypeException e) {
			throw new ASTExecException(e);
		}
	}

	@Override
	public String getSymbol() {
		return "!=";
	}

	@Override
	public int getPriority() {
		return 100;
	}

}
