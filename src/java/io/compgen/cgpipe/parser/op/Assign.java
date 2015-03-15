package io.compgen.cgpipe.parser.op;

import io.compgen.cgpipe.exceptions.ASTExecException;
import io.compgen.cgpipe.parser.context.ExecContext;
import io.compgen.cgpipe.parser.tokens.Token;
import io.compgen.cgpipe.parser.variable.VarValue;

public class Assign implements Operator {

	@Override
	public VarValue eval(ExecContext context, VarValue lval, VarValue rval) throws ASTExecException {
		throw new ASTExecException("Unsupported syntax for operator " + this.getClass().getSimpleName());
	}

	@Override
	public VarValue eval(ExecContext context, Token ltoken, VarValue rval) throws ASTExecException {
		if (!ltoken.isVariable()) {
			throw new ASTExecException("Unsupported syntax for operator (missing var) " + this.getClass().getSimpleName());			
		}
		context.set(ltoken.getStr(), rval);
		return rval;
	}
	@Override
	public boolean tokenLeft() {
		return true;
	}

	@Override
	public String getSymbol() {
		return "=";
	}

	@Override
	public int getPriority() {
		return 1000;
	}
}
