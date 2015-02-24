package org.ngsutils.mvpipe.parser.statement;

import java.io.IOException;

import org.ngsutils.mvpipe.exceptions.SyntaxException;
import org.ngsutils.mvpipe.parser.Eval;
import org.ngsutils.mvpipe.parser.Parser;
import org.ngsutils.mvpipe.parser.Tokens;
import org.ngsutils.mvpipe.parser.context.ExecContext;
import org.ngsutils.mvpipe.parser.variable.VarNull;
import org.ngsutils.mvpipe.parser.variable.VarValue;

public class Include implements Statement {

	@Override
	public ExecContext eval(ExecContext context, Tokens tokens) throws SyntaxException {
		if (context.isActive()) {
			Parser p = new Parser(context);
			try {
				VarValue file = Eval.evalTokenExpression(context, tokens);
				if (file == VarNull.NULL) {
					throw new SyntaxException("Unknown include file (missing quotes?)");
				}
				p.parseFile(file.toString());
			} catch (IOException e) {
				throw new SyntaxException(e);
			}
		}
		
		return context;
	}

}
