package org.ngsutils.mvpipe.parser.statement;

import java.util.List;

import org.ngsutils.mvpipe.parser.Parser;
import org.ngsutils.mvpipe.parser.SyntaxException;
import org.ngsutils.mvpipe.parser.variable.VarValue;

public class ElIf implements Statement {

	@Override
	public void eval(Parser parser, List<String> tokens)
			throws SyntaxException {

		// If previous context was ever active, we will not run
		
		if (!parser.getContext().wasCurrentLevelEverActive()) {
			VarValue test = parser.evalTokens(tokens);
			System.err.println("#ELIF TEST RESULT: " + test);
			if (test.isTrue()) {
				parser.getContext().switchActive();
			}
		} else {
			System.err.println("#ELIF SKIPPED!");
		}
	}

}
