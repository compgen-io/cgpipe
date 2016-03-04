package io.compgen.cgpipe.parser.statement;

import io.compgen.cgpipe.exceptions.ASTExecException;
import io.compgen.cgpipe.exceptions.ASTParseException;
import io.compgen.cgpipe.parser.context.ExecContext;
import io.compgen.cgpipe.parser.node.ASTNode;
import io.compgen.cgpipe.parser.tokens.TokenList;
import io.compgen.cgpipe.parser.variable.VarValue;

import java.util.SortedMap;
import java.util.TreeMap;

public class DumpVars implements Statement {

	@Override
	public ASTNode parse(ASTNode parent, final TokenList tokens) throws ASTParseException {

		final TokenList expr = tokens.subList(1);

		return new ASTNode(parent, expr) {
			@Override
			public ASTNode exec(ExecContext context) throws ASTExecException {
				if (expr.size()>0) {
					throw new ASTExecException("dumpvars should not have any arguments");
				}
				
				SortedMap<String, VarValue> vals = new TreeMap<String, VarValue>(context.getRoot().cloneValues());
				for (String k: vals.keySet()) {
					context.getRoot().println(k+"="+vals.get(k).toString());
				}

				return next;
			}
			public String dumpString() {
				return "[dumpvars]";
			}
		};
	}

	@Override
	public String getName() {
		return "dumpvars";
	}
}
