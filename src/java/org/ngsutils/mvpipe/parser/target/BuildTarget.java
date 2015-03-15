package org.ngsutils.mvpipe.parser.target;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.ngsutils.mvpipe.exceptions.ASTExecException;
import org.ngsutils.mvpipe.exceptions.ASTParseException;
import org.ngsutils.mvpipe.parser.NumberedLine;
import org.ngsutils.mvpipe.parser.context.RootContext;
import org.ngsutils.mvpipe.parser.node.ASTNode;
import org.ngsutils.mvpipe.parser.node.JobNoOpNode;
import org.ngsutils.mvpipe.parser.variable.VarList;
import org.ngsutils.mvpipe.parser.variable.VarValue;
import org.ngsutils.mvpipe.runner.JobDef;
import org.ngsutils.mvpipe.support.StringUtils;

public class BuildTarget {

	private List<String> inputs;
	private List<String> outputs;
	private Map<String, VarValue> capturedContext;
	private List<NumberedLine> lines;

	public BuildTarget(List<String> outputs, List<String> inputs, Map<String, VarValue> capturedContext, List<NumberedLine> lines) {
		this.outputs = outputs;
		this.inputs = inputs;
		this.capturedContext = capturedContext;
		this.lines = lines;
	}

	public String toString() {
		return "<build-target> " + StringUtils.join(" ", outputs) + " : " + StringUtils.join(" ", inputs);
	}

	public List<String> getInputs() {
		return Collections.unmodifiableList(inputs);
	}

	public List<String> getOutputs() {
		return Collections.unmodifiableList(outputs);
	}

	public List<NumberedLine> getLines() {
		return Collections.unmodifiableList(lines);
	}

	public JobDef eval(List<NumberedLine> pre, List<NumberedLine> post) throws ASTParseException, ASTExecException {
		ASTNode headNode = new JobNoOpNode(null);
		ASTNode curNode = headNode;

		if (lines.size() > 0) {
			int indent = StringUtils.calcIndentLevel(lines.get(0).line);
			List<NumberedLine> running = new ArrayList<NumberedLine>();
			if (pre != null) {
				running.addAll(pre);
			}
			running.addAll(lines);
			if (post != null) {
				running.addAll(post);
			}
			
			// Parse AST
			for (NumberedLine line: running) {
				String l = StringUtils.stripIndent(line.line, indent);
				if (StringUtils.lstrip(l).startsWith("#$")) {
					curNode = curNode.parseLine(line.stripPrefix());
				} else {
					curNode = curNode.parseBody(l);
				}
			}
		}
		
//		headNode.dump();
		
		// Eval AST
		RootContext jobRoot = new RootContext(capturedContext, outputs, inputs);
		jobRoot.set("job.custom", new VarList());
		
		curNode = headNode;
		while (curNode != null) {
			curNode = curNode.exec(jobRoot);
		}

		return new JobDef(jobRoot.getBody(), jobRoot.cloneValues("job."), outputs);
	}

}
