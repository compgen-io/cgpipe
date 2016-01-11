package io.compgen.cgpipe.parser.node;

import io.compgen.cgpipe.exceptions.ASTExecException;
import io.compgen.cgpipe.exceptions.ASTParseException;
import io.compgen.cgpipe.parser.Eval;
import io.compgen.cgpipe.parser.Parser;
import io.compgen.cgpipe.parser.context.ExecContext;
import io.compgen.cgpipe.parser.tokens.TokenList;

public class EvalNode extends ASTNode {
	private Parser nestedAST = null;
	public EvalNode(ASTNode parent, TokenList tokens) {
		super(parent, tokens);
	}

	@Override
	public ASTNode exec(ExecContext context) throws ASTExecException {
		String src = Eval.evalTokenExpression(tokens, context).toString();
		
		try {
			nestedAST = Parser.parseASTEval(src, tokens.getLine().getPipeline().getLoader());
			nestedAST.exec(context);
		} catch (ASTParseException e) {
			throw new ASTExecException(e);
		}

		return next;
	}

	@Override
	protected String dumpString() {
		return "[eval-src] "+tokens;
	}
}