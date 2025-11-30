package io.compgen.cgpipe.parser.node;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.compgen.cgpipe.exceptions.ASTExecException;
import io.compgen.cgpipe.exceptions.ASTParseException;
import io.compgen.cgpipe.loader.NumberedLine;
import io.compgen.cgpipe.parser.Eval;
import io.compgen.cgpipe.parser.context.ExecContext;
import io.compgen.cgpipe.parser.statement.Statement;
import io.compgen.cgpipe.parser.tokens.Token;
import io.compgen.cgpipe.parser.tokens.TokenList;
import io.compgen.cgpipe.parser.variable.VarNull;
import io.compgen.cgpipe.parser.variable.VarValue;
import io.compgen.common.StringUtils;


public class IteratingNode extends ASTNode {
	private ASTNode headNode = new NoOpNode(this);
	private ASTNode curNext = headNode;
	
	private final String[] varName;
	private final TokenList[] iterTokens;
	
	public IteratingNode(ASTNode parent, TokenList tokens) throws ASTParseException {
		super(parent, tokens);
//		System.err.println("ITER: " + tokens);
		
		int preCount = 0;
		int postCount = 0;
		boolean pre = true;
		boolean in = false;
		for (int i=0; i<tokens.size(); i++) {
			if (pre) {
				if (tokens.get(i).isStatement() && tokens.get(i).getStatement() == Statement.IN) {
					pre = false;
					in = true;
				} else if (tokens.get(i).isComma()) {
					preCount++;
				}
			} else {
				if (tokens.get(i).isComma()) {
					postCount++;
				}
			}
		}
		
		if (in) {		
			if (preCount != postCount || pre) {
				throw new ASTParseException("Invalid for-loop syntax!", tokens);
			}
		
			varName = new String[preCount+1];
			iterTokens = new TokenList[postCount+1];
	
			pre=true;
			int preIdx = 0;
			int postIdx = 0;
			List<Token> buf = new ArrayList<Token>();
			for (int i=0; i<tokens.size(); i++) {
				if (pre) {
					if (tokens.get(i).isStatement() && tokens.get(i).getStatement() == Statement.IN) {
						pre = false;
					} else if (tokens.get(i).isComma()) {
						//skip
					} else if (tokens.get(i).isVariable()) {
						varName[preIdx++] = tokens.get(i).getStr();
					} else {
						throw new ASTParseException("Invalid for-loop syntax!", tokens);
					}
				} else {
					if (tokens.get(i).isComma()) {
						iterTokens[postIdx++] = new TokenList(buf, tokens.getLine());
						buf.clear();
					} else {
						buf.add(tokens.get(i));
					}
				}
			}
			
			if (buf.size()>0) {
				iterTokens[postIdx++] = new TokenList(buf, tokens.getLine());
			}
		} else {
			// no 'IN' so we must be a for-while loop
			iterTokens = new TokenList[1];
			iterTokens[0] = tokens;
			varName = null;

		}
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
	public ASTNode parseBody(String str, NumberedLine line, boolean sameLine) {
		if (curNext == null) {
			return super.parseBody(str, line, sameLine);
		}

		ASTNode node = curNext.parseBody(str, line, sameLine);
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
		
		if (varName != null) {
			// for xx in yy loop
			List<Iterator<VarValue>> iterVals = new ArrayList<Iterator<VarValue>>();
			
			for (int i=0; i<iterTokens.length; i++) {
				VarValue val = Eval.evalTokenExpression(iterTokens[i], context);
				if (val == VarNull.NULL) {
					return next;
				}
				iterVals.add(val.iterate().iterator());
			}
			
			while (true) {
				VarValue[] vals = new VarValue[iterVals.size()];
				for (int i=0; i<vals.length; i++) {
					if (!iterVals.get(i).hasNext()) {
						return next;
					}
					vals[i] = iterVals.get(i).next();
				}
	
				ExecContext nested = new ExecContext(context);
	
				for (int i=0; i<varName.length;i++) {
					nested.set(varName[i], vals[i]);
				}
	
				ASTNode currentNode = headNode;
				
				while (currentNode != null) {
					currentNode = currentNode.exec(nested);
				}
	
				for (int i=0; i<varName.length;i++) {
					nested.remove(varName[i]);
				}
	
			}
		} else {
			// for test while loop
			while (true) {
				VarValue val = Eval.evalTokenExpression(iterTokens[0], context);
//				System.err.println(" ==> evaluating: "+iterTokens[0]+" <== " + val + " | "+this);
				if (!val.toBoolean()) {
					return next;
				}				
				ExecContext nested = new ExecContext(context);
				ASTNode currentNode = headNode;
				while (currentNode != null) {
					currentNode = currentNode.exec(nested);
				}
			}
		}
		
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

