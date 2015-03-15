package io.compgen.cgpipe.parser.target;

import io.compgen.cgpipe.exceptions.ASTExecException;
import io.compgen.cgpipe.exceptions.ASTParseException;
import io.compgen.cgpipe.parser.NumberedLine;
import io.compgen.cgpipe.parser.context.RootContext;
import io.compgen.cgpipe.parser.node.ASTNode;
import io.compgen.cgpipe.parser.node.JobNoOpNode;
import io.compgen.cgpipe.parser.variable.VarList;
import io.compgen.cgpipe.parser.variable.VarValue;
import io.compgen.cgpipe.runner.JobDef;
import io.compgen.cgpipe.runner.JobDependency;
import io.compgen.cgpipe.support.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BuildTarget {

	private List<String> inputs;
	private List<String> outputs;
	private Map<String, VarValue> capturedContext;
	private List<NumberedLine> lines;
	private boolean skipTarget = false;
	private JobDependency submittedJobDep=null;

	private Map<String, BuildTarget> deps = new HashMap<String, BuildTarget>();
	
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

	public Map<String, BuildTarget> getDepends() {
		return Collections.unmodifiableMap(deps);
	}

	public void addDep(String file, BuildTarget dep) {
		this.deps.put(file, dep);
	}

	public void addDeps(Map<String, BuildTarget> deps) {
		this.deps.putAll(deps);
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

	public boolean isSkipTarget() {
		return skipTarget;
	}

	public void setSkipTarget(boolean skipTarget) {
		this.skipTarget = skipTarget;
	}

	public void setSubmittedJobDep(JobDependency jobDep) {
		this.submittedJobDep = jobDep;
	}

	public JobDependency getJobDep() {
		return this.submittedJobDep;
	}

}
