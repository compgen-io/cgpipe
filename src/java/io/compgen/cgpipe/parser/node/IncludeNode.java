package io.compgen.cgpipe.parser.node;

import io.compgen.cgpipe.exceptions.ASTExecException;
import io.compgen.cgpipe.exceptions.ASTParseException;
import io.compgen.cgpipe.parser.Eval;
import io.compgen.cgpipe.parser.Parser;
import io.compgen.cgpipe.parser.context.ExecContext;
import io.compgen.cgpipe.parser.tokens.TokenList;

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