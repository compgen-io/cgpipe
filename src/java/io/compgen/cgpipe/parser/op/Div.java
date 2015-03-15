package io.compgen.cgpipe.parser.op;

import io.compgen.cgpipe.exceptions.ASTExecException;
import io.compgen.cgpipe.exceptions.VarTypeException;
import io.compgen.cgpipe.parser.context.ExecContext;
import io.compgen.cgpipe.parser.variable.VarValue;

public class Div extends BasicOp {

	@Override
	public VarValue eval(ExecContext context, VarValue lval, VarValue rval) throws ASTExecException {
		try {
			return lval.div(rval);
		} catch (VarTypeException e) {
			throw new ASTExecException(e);
		}
	}

	@Override
	public String getSymbol() {
		return "/";
	}

	@Override
	public int getPriority() {
		return 10;
	}

}
