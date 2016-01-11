package io.compgen.cgpipe.parser.node;

import io.compgen.cgpipe.exceptions.ASTExecException;
import io.compgen.cgpipe.exceptions.ASTParseException;
import io.compgen.cgpipe.parser.TemplateParser;
import io.compgen.cgpipe.parser.context.ExecContext;
import io.compgen.cgpipe.parser.target.BuildTarget;
import io.compgen.cgpipe.parser.tokens.TokenList;

public class ImportNode extends ASTNode {
	public ImportNode(ASTNode parent, TokenList tokens) {
		super(parent, tokens);
	}

	@Override
	public ASTNode exec(ExecContext context) throws ASTExecException {
		try {
			BuildTarget tgt = context.getRoot().findImportTarget(tokens.get(0).getStr());
			if (tgt != null) {
				TemplateParser.parseTemplate(tgt.getLines(), null, null, context.getRoot());
			} else {
				throw new ASTExecException("Missing import target: "+tokens.get(0).getStr(), tokens.getLine());
			}
		} catch (ASTParseException e) {
			throw new ASTExecException(e);
		}
		return next;
	}

	@Override
	protected String dumpString() {
		return "[include] "+tokens;
	}
}