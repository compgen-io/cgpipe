package io.compgen.cgpipe.parser.statement;

import io.compgen.cgpipe.exceptions.ASTExecException;
import io.compgen.cgpipe.exceptions.ASTParseException;
import io.compgen.cgpipe.parser.Eval;
import io.compgen.cgpipe.parser.context.ExecContext;
import io.compgen.cgpipe.parser.node.ASTNode;
import io.compgen.cgpipe.parser.tokens.TokenList;
import io.compgen.cgpipe.parser.variable.VarValue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class WriteLog implements Statement {
	Log log  = LogFactory.getLog(WriteLog.class);
	@Override
	public ASTNode parse(ASTNode parent, final TokenList tokens) throws ASTParseException {

		final TokenList expr = tokens.subList(1);
		
		return new ASTNode(parent, expr) {
			@Override
			public ASTNode exec(ExecContext context) throws ASTExecException {
				VarValue val = Eval.evalTokenExpression(expr, context);
				if (val == null) {
					throw new ASTExecException("Error evaluating expression: " + expr);
				}
				log.info(val.toString());
				return next;
			}
			public String dumpString() {
				return "[log]" + tokens.getLine();
			}
		};
	}

	@Override
	public String getName() {
		return "log";
	}
}
