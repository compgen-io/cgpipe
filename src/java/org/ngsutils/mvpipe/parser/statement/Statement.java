package org.ngsutils.mvpipe.parser.statement;

import java.util.List;

import org.ngsutils.mvpipe.parser.Parser;
import org.ngsutils.mvpipe.parser.SyntaxException;

public interface Statement {
	public void eval(Parser parser, List<String> tokens) throws SyntaxException;
}
