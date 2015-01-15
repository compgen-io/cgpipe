package org.ngsutils.mvpipe.parser.statement;

import java.util.List;

import org.ngsutils.mvpipe.parser.Parser;
import org.ngsutils.mvpipe.parser.SyntaxException;

public class EndIf implements Statement {

	@Override
	public void eval(Parser parser, List<String> tokens)
			throws SyntaxException {
		if (tokens.size() > 0) {
			throw new SyntaxException("Extra fields on endif line!");
		}
		parser.setContext(parser.getContext().getParent());
	}

}
