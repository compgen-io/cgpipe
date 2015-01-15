package org.ngsutils.mvpipe.parser.statement;

import java.util.List;

import org.ngsutils.mvpipe.parser.Parser;
import org.ngsutils.mvpipe.parser.SyntaxException;
import org.ngsutils.mvpipe.parser.context.ExecContext;
import org.ngsutils.mvpipe.parser.context.IfThenContext;
import org.ngsutils.mvpipe.parser.variable.VarValue;

public class If implements Statement {

	@Override
	public void eval(Parser parser, List<String> tokens)
			throws SyntaxException {
		
		VarValue test = parser.evalTokens(tokens);
		System.err.println("#IF TEST RESULT: " + test);

		ExecContext nested = new IfThenContext(parser.getContext(), test.isTrue());
		parser.setContext(nested);
		
	}

}
