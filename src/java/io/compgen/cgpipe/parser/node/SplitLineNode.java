package io.compgen.cgpipe.parser.node;

import io.compgen.cgpipe.exceptions.ASTExecException;
import io.compgen.cgpipe.exceptions.ASTParseException;
import io.compgen.cgpipe.parser.context.ExecContext;
import io.compgen.cgpipe.parser.tokens.TokenList;
import io.compgen.cgpipe.parser.tokens.Tokenizer;
import io.compgen.cgpipe.pipeline.NumberedLine;



public class SplitLineNode extends ASTNode {
	public SplitLineNode(ASTNode parent, TokenList tokens) {
		super(parent, tokens);
	}

	@Override
	public ASTNode parseLine(NumberedLine line) throws ASTParseException {
		TokenList newtokens = Tokenizer.tokenize(line);
		
		if (newtokens.size()==0) {
			return this;
		}
		
		if (newtokens.get(newtokens.size()-1).isSplitLine()) {
			tokens.append(newtokens.subList(0, newtokens.size()-1));
			return this;
		} else {
			tokens.append(newtokens);
			this.next = super.parseTokens(tokens);
			return this.next;
		}
	}

	@Override
	public ASTNode exec(ExecContext context) throws ASTExecException {
		return next.exec(context);
	}

	@Override
	protected String dumpString() {
		return "[split-line]";
	}

}
