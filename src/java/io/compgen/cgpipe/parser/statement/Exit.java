package io.compgen.cgpipe.parser.statement;

import io.compgen.cgpipe.exceptions.ASTExecException;
import io.compgen.cgpipe.exceptions.ASTParseException;
import io.compgen.cgpipe.exceptions.VarTypeException;
import io.compgen.cgpipe.parser.Eval;
import io.compgen.cgpipe.parser.context.ExecContext;
import io.compgen.cgpipe.parser.node.ASTNode;
import io.compgen.cgpipe.parser.tokens.TokenList;
import io.compgen.cgpipe.parser.variable.VarValue;

public class Exit implements Statement {

	@Override
	public ASTNode parse(ASTNode parent, final TokenList tokens) throws ASTParseException {
		return new ASTNode(parent, tokens) {
			@Override
			public ASTNode exec(ExecContext context) throws ASTExecException {
				VarValue val = Eval.evalTokenExpression(tokens, context);
				if (val == null) {
					System.exit(-1);
				}
				try {
					System.exit(val.toInt());
				} catch (VarTypeException e) {
					System.exit(-1);
				}
				return null;
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
