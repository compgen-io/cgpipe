package io.compgen.cgpipe.parser.statement;

import java.io.IOException;

import io.compgen.cgpipe.CGPipe;
import io.compgen.cgpipe.exceptions.ASTExecException;
import io.compgen.cgpipe.exceptions.ASTParseException;
import io.compgen.cgpipe.parser.Parser;
import io.compgen.cgpipe.parser.context.ExecContext;
import io.compgen.cgpipe.parser.node.ASTNode;
import io.compgen.cgpipe.parser.tokens.TokenList;

public class ShowHelp implements Statement {

	@Override
	public ASTNode parse(ASTNode parent, final TokenList tokens) throws ASTParseException {

		final TokenList expr = tokens.subList(1);
		
		return new ASTNode(parent, expr) {
			@Override
			public ASTNode exec(ExecContext context) throws ASTExecException {
				try {
					context.getRoot().println(Parser.getHelp(CGPipe.getFilename()));
				} catch (IOException e) {
					throw new ASTExecException(e);
				}
				return next;
			}
			public String dumpString() {
				return "[showhelp]";
			}
		};
	}

	@Override
	public String getName() {
		return "showhelp";
	}
}
