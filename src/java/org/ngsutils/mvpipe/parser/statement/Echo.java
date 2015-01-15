package org.ngsutils.mvpipe.parser.statement;

import java.util.List;

import org.ngsutils.mvpipe.parser.Parser;
import org.ngsutils.mvpipe.parser.SyntaxException;

public class Echo implements Statement {

	@Override
	public void eval(Parser parser, List<String> tokens)
			throws SyntaxException {
		
		if (parser.getContext().isActive()) {
			System.err.println(parser.evalTokens(tokens));
		}
	}

}
