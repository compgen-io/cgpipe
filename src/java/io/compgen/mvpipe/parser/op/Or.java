package io.compgen.mvpipe.parser.op;

import io.compgen.mvpipe.exceptions.ASTExecException;
import io.compgen.mvpipe.parser.context.ExecContext;
import io.compgen.mvpipe.parser.variable.VarBool;
import io.compgen.mvpipe.parser.variable.VarValue;

public class Or extends BasicOp {

	@Override
	public VarValue eval(ExecContext context, VarValue lval, VarValue rval) throws ASTExecException {
		return (lval.isTrue() || rval.isTrue()) ? VarBool.TRUE : VarBool.FALSE;
	}

	@Override
	public String getSymbol() {
		return "||";
	}

	@Override
	public int getPriority() {
		return 200;
	}

}
