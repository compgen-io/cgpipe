package io.compgen.mvpipe.parser.node;

import io.compgen.mvpipe.exceptions.ASTExecException;
import io.compgen.mvpipe.exceptions.ASTParseException;
import io.compgen.mvpipe.parser.Eval;
import io.compgen.mvpipe.parser.NumberedLine;
import io.compgen.mvpipe.parser.context.ExecContext;
import io.compgen.mvpipe.parser.statement.Statement;
import io.compgen.mvpipe.parser.tokens.TokenList;
import io.compgen.mvpipe.parser.variable.VarValue;
import io.compgen.mvpipe.support.StringUtils;


public class IteratingNode extends ASTNode {
	private ASTNode headNode = new NoOpNode(this);
	private ASTNode curNext = headNode;
	
	private final String varName;
	private final TokenList iterTokens;
	
	public IteratingNode(ASTNode parent, TokenList tokens) throws ASTParseException {
		super(parent, tokens);
//		System.err.println("ITER: " + tokens);
		if (!tokens.get(0).isVariable()) {
			throw new ASTParseException("Invalid for-loop syntax!", tokens);
		}
		if (!tokens.get(1).isStatement() && tokens.get(1).getStatement() != Statement.IN) {
			throw new ASTParseException("Invalid for-loop syntax!", tokens);
		}
		varName = tokens.get(0).getStr();
		iterTokens = tokens.subList(2);
	}
	
	@Override
	public ASTNode parseLine(NumberedLine line) throws ASTParseException {
		if (curNext == null) {
			return super.parseLine(line);
		}

//		System.err.println(">+> nested for-currentNode " + curNext);
		ASTNode node = curNext.parseLine(line);
		if (node != null) {
			curNext = node;
		}
//		System.err.println(">-> nested for-currentNode " + curNext);
//		if (curNext != null) {
//			System.err.println("      > parent -> " + curNext.getParent());
//			System.err.println("      > parent.next -> " + curNext.getParent().next);
//		}

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
			throw new ASTExecException("Missing done for for-loop", tokens);
		}
		
		VarValue iterVal = Eval.evalTokenExpression(iterTokens, context);
		for (VarValue val: iterVal.iterate()) {
			ExecContext nested = new ExecContext(context);
			nested.set(varName, val);
			
			ASTNode currentNode = headNode;

			while (currentNode != null) {
				currentNode = currentNode.exec(nested);
			}
		}

		return next;
	}

	public void done() {
		if (curNext != null) {
//			System.err.println(" ==> done <== " + this);
			curNext = null;
		}
	}
	
	public boolean isDone() {
		return curNext == null;
	}

	@Override
	public String dumpString() {
		return "[for] " + tokens;
	}

	public void dump(int indent) {
		System.err.println(StringUtils.repeat("  ", indent)+dumpString());
		
		headNode.dump(indent + 1);
		
		if (next != null) {
			next.dump(indent);
		}
	}

}

