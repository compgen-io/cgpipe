package io.compgen.cgpipe.parser.node;

import io.compgen.cgpipe.parser.context.ExecContext;



public class NoOpNode extends ASTNode {
	public NoOpNode(ASTNode parent) {
		super(parent, null);
	}

	@Override
	public ASTNode exec(ExecContext context) {
		return next;
	}

	@Override
	protected String dumpString() {
		return "[no-op]";
	}
}
