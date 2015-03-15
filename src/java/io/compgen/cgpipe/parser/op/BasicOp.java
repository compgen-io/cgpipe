package io.compgen.cgpipe.parser.op;

import io.compgen.cgpipe.exceptions.ASTExecException;
import io.compgen.cgpipe.parser.context.ExecContext;
import io.compgen.cgpipe.parser.tokens.Token;
import io.compgen.cgpipe.parser.variable.VarValue;

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
