package io.compgen.mvpipe.parser.node;

import io.compgen.mvpipe.exceptions.ASTExecException;
import io.compgen.mvpipe.exceptions.ASTParseException;
import io.compgen.mvpipe.parser.NumberedLine;
import io.compgen.mvpipe.parser.context.ExecContext;
import io.compgen.mvpipe.parser.target.BuildTargetTemplate;
import io.compgen.mvpipe.parser.tokens.Token;
import io.compgen.mvpipe.parser.tokens.TokenList;
import io.compgen.mvpipe.support.StringUtils;

import java.util.ArrayList;
import java.util.List;


public class TargetNode extends ASTNode {
	private final int indentLevel;

	private List<NumberedLine> lines = new ArrayList<NumberedLine>();
	private List<String> outputs = new ArrayList<String>();
	private List<String> inputs = null;
	
 
	public TargetNode(ASTNode parent, TokenList tokens) throws ASTParseException {
		super(parent, tokens);
		
		indentLevel = StringUtils.calcIndentLevel(tokens.getLine().line);
		
		ASTNode p = parent;
		while (p != null) {
			if (p.getClass().equals(JobNoOpNode.class)) {
				throw new ASTParseException("You can't nest build-targets!");
			}
			p = p.getParent();
		}

		for (Token tok: tokens) {
			if (tok.isColon()) { 
				if (inputs != null) {
					throw new ASTParseException("Too many colons!", tokens.getLine());
				}
				
				inputs = new ArrayList<String>();
				
			} else if (inputs != null) {
				inputs.add(tok.getStr());
			} else {
				outputs.add(tok.getStr());
			}
		}
		

	}
	
	@Override
	public ASTNode parseLine(NumberedLine line) throws ASTParseException {
		int indent = StringUtils.calcIndentLevel(line.line);

		if (indent > this.indentLevel) {
			lines.add(line);
			return this;
		}
		
		return super.parseLine(line);
	}
	
	@Override
	public ASTNode exec(ExecContext context) throws ASTExecException {
		context.getRoot().addTarget(new BuildTargetTemplate(outputs, inputs, context, lines, tokens));
		return next;
	}

	@Override
	public String dumpString() {
		return "[build-target] " + StringUtils.join(" ", outputs) + " : " + StringUtils.join(" ", inputs);
	}
	
	@Override
	public void dump(int indent) {
		System.err.println(StringUtils.repeat("  ", indent)+dumpString());
		for (NumberedLine line: lines) {
			System.err.println(StringUtils.repeat("  ", indent+1)+"[src] "+line.line);
		}
		if (next != null) {
			next.dump(indent);
		}

	}
}

