package org.ngsutils.mvpipe.parser.statement;

import java.util.List;
import java.util.Map;

import org.ngsutils.mvpipe.parser.Parser;
import org.ngsutils.mvpipe.parser.SyntaxException;
import org.ngsutils.mvpipe.parser.variable.VarValue;

public class DumpVars implements Statement {

	@Override
	public void eval(Parser parser, List<String> tokens)
			throws SyntaxException {
		
		Map<String, VarValue> state = parser.getContext().cloneValues();
		for (String k: state.keySet()) {
			System.err.println(k + " => " + state.get(k));
		}
	}

}
