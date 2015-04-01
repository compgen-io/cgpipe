package io.compgen.cgpipe.parser.node;

import io.compgen.cgpipe.exceptions.ASTExecException;
import io.compgen.cgpipe.exceptions.ASTParseException;
import io.compgen.cgpipe.parser.Eval;
import io.compgen.cgpipe.parser.context.ExecContext;
import io.compgen.cgpipe.parser.tokens.TokenList;
import io.compgen.cgpipe.pipeline.NumberedLine;
import io.compgen.common.Pair;
import io.compgen.common.StringUtils;

import java.util.ArrayList;
import java.util.List;


public class ConditionalNode extends ASTNode {
	private List<Pair<TokenList, ASTNode>> conditions = new ArrayList<Pair<TokenList, ASTNode>>();
	private ASTNode curNext = null;
	
	public ConditionalNode(ASTNode parent, TokenList tokens) throws ASTParseException {
		super(parent, tokens);
		curNext = new NoOpNode(this);
		conditions.add(new Pair<TokenList, ASTNode>(tokens, curNext));
	}
	
	@Override
	public ASTNode parseLine(NumberedLine line) throws ASTParseException {
		if (curNext == null) {
			return super.parseLine(line);
		}

//		System.err.println(">>> nested if-currentNode " + curNext);
		ASTNode node = curNext.parseLine(line);
		if (node != null) {
			curNext = node;
		}
//		System.err.println(">>> nested if-currentNode " + curNext);

		return this;
	}
	
	@Override
	public ASTNode parseBody(String str, NumberedLine line) {
		if (curNext == null) {
			return super.parseBody(str, line);
		}

		ASTNode node = curNext.parseBody(str, line);
		if (node != null) {
			curNext = node;
		}

		return this;
	}

	
	@Override
	public ASTNode exec(ExecContext context) throws ASTExecException {
		if (curNext != null) {
			throw new ASTExecException("Missing endif for if-then block", tokens);
		}
		
		ASTNode currentNode = null;
		for (Pair<TokenList, ASTNode> cond: conditions) {
			if (cond.one == null) {
				// else condition
				currentNode = cond.two;
			} else if (Eval.evalTokenExpression(cond.one, context).isTrue()) {
				currentNode = cond.two;
				break;
			}
		}

		while (currentNode != null) {
			currentNode = currentNode.exec(context);
		}
		return next;
	}

	public void elseClause(TokenList tokens) {
		if (curNext != null) {
			ASTNode node = new NoOpNode(this);
			curNext = node;
			
			conditions.add(new Pair<TokenList, ASTNode>(tokens, node));
		}	
	}

	public void endif() {
		if (curNext != null) {
//			System.err.println(" ==> endif <== " + this);
			curNext = null;
		}
	}
	
	public boolean isDone() {
		return curNext == null;
	}

	@Override
	public String dumpString() {
		return "[if] " + tokens;
	}

	public void dump(int indent) {
		System.err.println(StringUtils.repeat("  ", indent)+dumpString());
		
		boolean first = true;
		for (Pair<TokenList, ASTNode> pair: conditions){
			if (first) {
				System.err.println(StringUtils.repeat("  ", indent)+"[then]");
				first = false;
			} else if (pair.one != null) {
				System.err.println(StringUtils.repeat("  ", indent)+"[elif] " + pair.one);
			} else {
				System.err.println(StringUtils.repeat("  ", indent)+"[else]");
			}
			pair.two.dump(indent + 1);
		}
		
		if (next != null) {
			next.dump(indent);
		}
	}

}

