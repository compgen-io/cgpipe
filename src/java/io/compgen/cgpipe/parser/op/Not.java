package io.compgen.cgpipe.parser.op;

import io.compgen.cgpipe.exceptions.ASTExecException;
import io.compgen.cgpipe.parser.context.ExecContext;
import io.compgen.cgpipe.parser.variable.VarBool;
import io.compgen.cgpipe.parser.variable.VarValue;

public class Not extends BasicOp {

	@Override
	public VarValue eval(ExecContext context, VarValue lval, VarValue rval) throws ASTExecException {
		return (rval.toBoolean()) ? VarBool.FALSE : VarBool.TRUE;
	}

	@Override
	public String getSymbol() {
		return "!";
	}

	@Override
	public int getPriority() {
		return 150;
	}

}
