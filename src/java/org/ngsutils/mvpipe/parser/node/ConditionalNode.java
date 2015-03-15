package org.ngsutils.mvpipe.parser.node;

import java.util.ArrayList;
import java.util.List;

import org.ngsutils.mvpipe.exceptions.ASTExecException;
import org.ngsutils.mvpipe.exceptions.ASTParseException;
import org.ngsutils.mvpipe.parser.Eval;
import org.ngsutils.mvpipe.parser.NumberedLine;
import org.ngsutils.mvpipe.parser.context.ExecContext;
import org.ngsutils.mvpipe.parser.tokens.TokenList;
import org.ngsutils.mvpipe.support.Pair;
import org.ngsutils.mvpipe.support.StringUtils;


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
	public ASTNode parseBody(String str) {
		if (curNext == null) {
			return super.parseBody(str);
		}

		ASTNode node = curNext.parseBody(str);
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

