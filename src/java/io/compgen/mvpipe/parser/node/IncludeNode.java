package org.ngsutils.mvpipe.parser.node;

import java.io.File;

import org.ngsutils.mvpipe.exceptions.ASTExecException;
import org.ngsutils.mvpipe.exceptions.ASTParseException;
import org.ngsutils.mvpipe.parser.Eval;
import org.ngsutils.mvpipe.parser.Parser;
import org.ngsutils.mvpipe.parser.context.ExecContext;
import org.ngsutils.mvpipe.parser.tokens.TokenList;

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