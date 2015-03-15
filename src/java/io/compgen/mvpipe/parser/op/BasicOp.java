package org.ngsutils.mvpipe.parser.op;

import org.ngsutils.mvpipe.exceptions.ASTExecException;
import org.ngsutils.mvpipe.parser.context.ExecContext;
import org.ngsutils.mvpipe.parser.tokens.Token;
import org.ngsutils.mvpipe.parser.variable.VarValue;

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
