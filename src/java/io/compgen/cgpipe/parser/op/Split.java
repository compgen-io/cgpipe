package io.compgen.cgpipe.parser.op;

import io.compgen.cgpipe.exceptions.ASTExecException;
import io.compgen.cgpipe.exceptions.VarTypeException;
import io.compgen.cgpipe.parser.context.ExecContext;
import io.compgen.cgpipe.parser.variable.VarList;
import io.compgen.cgpipe.parser.variable.VarValue;

public class Split extends BasicOp {

	@Override
	public VarValue eval(ExecContext context, VarValue lval, VarValue rval) throws ASTExecException {
		try {
			System.err.println(lval + " split '" + rval+"'");
			VarList list = new VarList();
			for (String el: lval.toString().split(rval.toString())) {
				System.err.println("*"+el+"*");
				list.add(VarValue.parseStringRaw(el));
			}
			return list;
		} catch (VarTypeException e) {
			throw new ASTExecException(e);
		}
	}

	@Override
	public String getSymbol() {
		return "~/";
	}

	@Override
	public int getPriority() {
		return 1000;
	}

}
