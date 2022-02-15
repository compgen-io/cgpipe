package io.compgen.cgpipe.parser.node;

import io.compgen.cgpipe.exceptions.ASTExecException;
import io.compgen.cgpipe.loader.NumberedLine;
import io.compgen.cgpipe.parser.Eval;
import io.compgen.cgpipe.parser.context.ExecContext;



public class BodyNode extends ASTNode {
	private String src;
	final private NumberedLine line;
	final private boolean sameLine;
	public BodyNode(ASTNode parent, String src, NumberedLine line, boolean sameLine) {
		super(parent, null);
		this.src = src;
		this.line = line;
		this.sameLine = sameLine;
	}

	@Override
	public ASTNode exec(ExecContext context) throws ASTExecException {
		String evalBody = Eval.evalString(src, context, line);
		context.getRoot().addBodyLine(evalBody, sameLine);
		return next;
	}

	@Override
	protected String dumpString() {
		return "[src] " + src;
	}
}
