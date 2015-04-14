package io.compgen.cgpipe.parser.node;

import io.compgen.cgpipe.exceptions.ASTExecException;
import io.compgen.cgpipe.loader.NumberedLine;
import io.compgen.cgpipe.parser.Eval;
import io.compgen.cgpipe.parser.context.ExecContext;



public class BodyNode extends ASTNode {
	private String src;
	final private NumberedLine line;
	public BodyNode(ASTNode parent, String src, NumberedLine line) {
		super(parent, null);
		this.src = src;
		this.line = line;
	}

	@Override
	public ASTNode exec(ExecContext context) throws ASTExecException {
		String evalBody = Eval.evalString(src, context, line);
		context.getRoot().addBodyLine(evalBody);
		return next;
	}

	@Override
	protected String dumpString() {
		return "[src] " + src;
	}
}
