package io.compgen.mvpipe.parser.node;

import io.compgen.mvpipe.exceptions.ASTExecException;
import io.compgen.mvpipe.exceptions.ASTParseException;
import io.compgen.mvpipe.parser.Eval;
import io.compgen.mvpipe.parser.Parser;
import io.compgen.mvpipe.parser.context.ExecContext;
import io.compgen.mvpipe.parser.tokens.TokenList;

import java.io.File;

public class IncludeNode extends ASTNode {
	private Parser nestedAST = null;
	public IncludeNode(ASTNode parent, TokenList tokens) {
		super(parent, tokens);
	}

	@Override
	public ASTNode exec(ExecContext context) throws ASTExecException {
		String filename = Eval.evalTokenExpression(tokens, context).toString();
		
		try {
			File file = context.getRoot().findFile(filename);
			if (file == null) {
//				context.dump();
				throw new ASTExecException("Could not file file: "+filename, tokens);
				
			}
			nestedAST = Parser.parseAST(file);
			nestedAST.exec(context);
		} catch (ASTParseException e) {
			throw new ASTExecException(e);
		}

		return next;
	}

	@Override
	protected String dumpString() {
		return "[include] "+tokens;
	}
}