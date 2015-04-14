package io.compgen.cgpipe.parser.node;

import io.compgen.cgpipe.exceptions.ASTExecException;
import io.compgen.cgpipe.exceptions.ASTParseException;
import io.compgen.cgpipe.loader.NumberedLine;
import io.compgen.cgpipe.parser.Eval;
import io.compgen.cgpipe.parser.context.ExecContext;
import io.compgen.cgpipe.parser.statement.Print;
import io.compgen.cgpipe.parser.statement.Statement;
import io.compgen.cgpipe.parser.tokens.Token;
import io.compgen.cgpipe.parser.tokens.TokenList;
import io.compgen.cgpipe.parser.tokens.Tokenizer;
import io.compgen.cgpipe.parser.variable.VarNull;
import io.compgen.cgpipe.parser.variable.VarValue;
import io.compgen.common.StringUtils;

import java.util.ArrayList;
import java.util.List;


public abstract class ASTNode {
	static List<Statement> statements = new ArrayList<Statement>();
	static {
		statements.add(new Print());
	}
	
	protected ASTNode next = null;

	final protected ASTNode parent;
	final protected TokenList tokens;
	
	public ASTNode(ASTNode parent, TokenList tokens) {
		this.parent = parent;
		this.tokens = tokens;
	}

	public ASTNode parseLine(NumberedLine line) throws ASTParseException {
		final TokenList tokens = Tokenizer.tokenize(line);
		if (tokens.size()==0) {
			return this;
		}
		
		if (tokens.get(tokens.size()-1).isSplitLine()) {
			this.next = new SplitLineNode(this, tokens.subList(0, tokens.size()-1));
			return this.next;
		}
		
		return parseTokens(tokens);
	}
	
	protected ASTNode parseTokens(TokenList tokens) throws ASTParseException {
		if (tokens.get(0).isStatement()) {
			ASTNode node = tokens.get(0).getStatement().parse(this, tokens);
			if (node != null) {
				this.next = node;
			}
			return this.next;
		}
		
		for (Token tok: tokens) {
			if (tok.isColon()) {
				this.next = new TargetNode(this, tokens);
				return this.next;
			}
		}
		
		// if we don't have a statement, then just eval the line.
		this.next = new ASTNode(this, tokens) {
			@Override
			public ASTNode exec(ExecContext context) throws ASTExecException {
//				System.err.println("Eval: " + StringUtils.join(",", tokens));
				VarValue val = Eval.evalTokenExpression(tokens, context);
				if (val == VarNull.NULL) {
					throw new ASTExecException("Null expression: " + tokens);
				}
//				System.err.println("<<< " + val.toString());
				return next;
			}
			public String dumpString() {
				return "[eval]" + tokens.getLine();
			}
		};
//		System.err.println(this+ " >>>>> EVAL-NEXT >>>>> "+next);
		return next;
	}

	public ASTNode parseBody(String l, NumberedLine line) {
		this.next = new BodyNode(this, l, line);
		return next;
	}

	abstract public ASTNode exec(ExecContext context) throws ASTExecException;
	abstract protected String dumpString();

	public void dump() {
		dump(0);
	}

	public void dump(int indent) {
		System.err.println(StringUtils.repeat("  ", indent)+dumpString());
		if (next != null) {
			next.dump(indent);
		}
	}

	public ASTNode getParent() {
		return parent;
	}
}

