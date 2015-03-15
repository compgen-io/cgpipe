package org.ngsutils.mvpipe.parser.node;

import org.ngsutils.mvpipe.exceptions.ASTExecException;
import org.ngsutils.mvpipe.parser.Eval;
import org.ngsutils.mvpipe.parser.context.ExecContext;



public class BodyNode extends ASTNode {
	private String src;
	public BodyNode(ASTNode parent, String src) {
		super(parent, null);
		this.src = src;
	}

	@Override
	public ASTNode exec(ExecContext context) throws ASTExecException {
		String evalBody = Eval.evalString(src, context, parent.tokens);
		context.getRoot().addBodyLine(evalBody);
		return next;
	}

	@Override
	protected String dumpString() {
		return "[src] " + src;
	}
}
