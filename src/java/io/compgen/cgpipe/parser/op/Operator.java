package io.compgen.cgpipe.parser.op;

import io.compgen.cgpipe.exceptions.ASTExecException;
import io.compgen.cgpipe.parser.context.ExecContext;
import io.compgen.cgpipe.parser.tokens.Token;
import io.compgen.cgpipe.parser.variable.VarValue;

public interface Operator {
	public VarValue eval(ExecContext context, VarValue left, VarValue right) throws ASTExecException;
	public VarValue eval(ExecContext context, Token left, VarValue rval) throws ASTExecException;
	public boolean tokenLeft();
	public String getSymbol();
	public int getPriority();

	public static final Operator RANGE = new Range(); // ..
	public static final Operator NOT = new Not(); // !
	public static final Operator EQ = new Eq(); // ==
	public static final Operator NOT_EQ = new NotEq(); // !=
	public static final Operator COND_ASSIGN = new CondAssign(); // ?=
	public static final Operator ADD_ASSIGN = new AddAssign(); // +=
	public static final Operator GTE = new Gte(); // >=
	public static final Operator LTE = new Lte(); // <=
	public static final Operator GT = new Gt(); // >
	public static final Operator LT = new Lt(); // <
	public static final Operator AND = new And(); // &&
	public static final Operator OR = new Or(); // ||
	public static final Operator POW = new Pow(); // **
	public static final Operator ASSIGN = new Assign(); // =
	public static final Operator ADD = new Add(); // +
	public static final Operator SUB = new Sub(); // -
	public static final Operator MUL = new Mul(); // *
	public static final Operator DIV = new Div(); // /
	public static final Operator REM = new Rem(); // %

	// TODO: add a list ctor operator? [] (also ',')
	// TODO: add a shell operator? $()
	
	public static final Operator[] operators = {
		RANGE,
		NOT_EQ,
		EQ,
		NOT,
		COND_ASSIGN,
		ADD_ASSIGN,
		GTE,
		LTE,
		GT,
		LT,
		AND,
		OR,
		POW,
		ASSIGN,
		ADD,
		SUB,
		MUL,
		DIV,
		REM
	};
}
