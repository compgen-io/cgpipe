package io.compgen.cgpipe.parser.op;

import io.compgen.cgpipe.exceptions.ASTExecException;
import io.compgen.cgpipe.parser.context.ExecContext;
import io.compgen.cgpipe.parser.variable.VarBool;
import io.compgen.cgpipe.parser.variable.VarValue;

public class Or extends BasicOp {

	@Override
	public VarValue eval(ExecContext context, VarValue lval, VarValue rval) throws ASTExecException {
		return (lval.toBoolean() || rval.toBoolean()) ? VarBool.TRUE : VarBool.FALSE;
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
