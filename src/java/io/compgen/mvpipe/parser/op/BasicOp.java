package io.compgen.mvpipe.parser.op;

import io.compgen.mvpipe.exceptions.ASTExecException;
import io.compgen.mvpipe.parser.context.ExecContext;
import io.compgen.mvpipe.parser.tokens.Token;
import io.compgen.mvpipe.parser.variable.VarValue;

public abstract class BasicOp implements Operator {

	@Override	
	abstract public VarValue eval(ExecContext context, VarValue lval, VarValue rval) throws ASTExecException;
	
	public VarValue eval(ExecContext context, Token ltoken, VarValue rval) throws ASTExecException {
		throw new ASTExecException("Unsupported syntax for operator " + this.getClass().getSimpleName());
	}

	@Override
	public boolean tokenLeft() {
		return false;
	}
}
