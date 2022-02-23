package io.compgen.cgpipe.parser.node;

import java.util.ArrayList;
import java.util.List;

import io.compgen.cgpipe.exceptions.ASTExecException;
import io.compgen.cgpipe.exceptions.ASTParseException;
import io.compgen.cgpipe.loader.NumberedLine;
import io.compgen.cgpipe.parser.context.ExecContext;
import io.compgen.cgpipe.parser.target.BuildTargetTemplate;
import io.compgen.cgpipe.parser.tokens.Token;
import io.compgen.cgpipe.parser.tokens.TokenList;
import io.compgen.common.StringUtils;


public class TargetNode extends ASTNode {
	private final int indentLevel;

	private List<NumberedLine> lines = new ArrayList<NumberedLine>();
	private List<String> outputs = new ArrayList<String>();
	private List<String> inputs = null;
	private boolean importable = false;
 
	public TargetNode(ASTNode parent, TokenList tokens) throws ASTParseException {
		super(parent, tokens);
		
		indentLevel = StringUtils.calcIndentLevel(tokens.getLine().getLine());
		
		ASTNode p = parent;
		while (p != null) {
			if (p.getClass().equals(JobNoOpNode.class)) {
				throw new ASTParseException("You can't nest build-targets!");
			}
			p = p.getParent();
		}
//		System.err.println("TOKENS: " + tokens);

		for (Token tok: tokens) {
			if (tok.isColon()) { 
				if (inputs != null) {
					if (!importable) {
						importable = true;
					} else {
						throw new ASTParseException("Too many colons!", tokens.getLine());
					}
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
		int indent = StringUtils.calcIndentLevel(line.getLine());

		if (indent > this.indentLevel) {
			lines.add(line);
			return this;
		}
		
		return super.parseLine(line);
	}
	
	@Override
	public ASTNode exec(ExecContext context) throws ASTExecException {
		context.getRoot().addTarget(new BuildTargetTemplate(outputs, inputs, context, lines, tokens, importable));
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
			System.err.println(StringUtils.repeat("  ", indent+1)+"[src] "+line.getLine());
		}
		if (next != null) {
			next.dump(indent);
		}

	}
}

