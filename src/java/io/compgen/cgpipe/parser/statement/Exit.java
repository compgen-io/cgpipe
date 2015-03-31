package io.compgen.cgpipe.parser.statement;

import io.compgen.cgpipe.exceptions.ASTExecException;
import io.compgen.cgpipe.exceptions.ASTParseException;
import io.compgen.cgpipe.exceptions.ExitException;
import io.compgen.cgpipe.exceptions.VarTypeException;
import io.compgen.cgpipe.parser.Eval;
import io.compgen.cgpipe.parser.context.ExecContext;
import io.compgen.cgpipe.parser.node.ASTNode;
import io.compgen.cgpipe.parser.tokens.TokenList;
import io.compgen.cgpipe.parser.variable.VarValue;

public class Exit implements Statement {

	@Override
	public ASTNode parse(ASTNode parent, TokenList tokens) throws ASTParseException {
		final TokenList expr = tokens.subList(1);

		return new ASTNode(parent, expr) {
			@Override
			public ASTNode exec(ExecContext context) throws ASTExecException {
				VarValue val = Eval.evalTokenExpression(expr, context);
				int retcode = -1;
				if (val != null) {
					try {
						retcode = val.toInt();
					} catch (VarTypeException e) {
						retcode = -1;
					}
				}
				throw new ExitException(retcode);
			}
			public String dumpString() {
				return "[exit] " + tokens;
			}
		};
	}

	@Override
	public String getName() {
		return "exit";
	}
}
