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
			boolean preRun = false;
			
			// Parse AST
			for (NumberedLine line: lines) {
				String l = StringUtils.stripIndent(line.line, indent);
				if (StringUtils.lstrip(l).startsWith("#$")) {
					curNode = curNode.parseLine(line.stripPrefix());
				} else {
					// Handle the pre- and post- blocks as conditionals, so that they can be selectively disabled.
					// Only handle the pre right before any body lines so that can disable it if needed.
					// pre only runs if there is a target body.
					if (pre != null && !preRun) {
						preRun = true;
						curNode = curNode.parseLine(new NumberedLine("if !job.nopre"));
						for (NumberedLine preLine: pre) {
							String l2 = StringUtils.stripIndent(preLine.line, indent);
							if (StringUtils.lstrip(l).startsWith("#$")) {
								curNode = curNode.parseLine(preLine.stripPrefix());
							} else {
								curNode = curNode.parseBody(l2);
							}
						}
						curNode = curNode.parseLine(new NumberedLine("endif"));
					}
					curNode = curNode.parseBody(l);
				}
			}

			// post only runs if there is a target body.
			if (post != null && lines.size() > 0) {
				curNode = curNode.parseLine(new NumberedLine("if !job.nopost"));
				for (NumberedLine postLine: post) {
					String l2 = StringUtils.stripIndent(postLine.line, indent);
					if (StringUtils.lstrip(l2).startsWith("#$")) {
						curNode = curNode.parseLine(postLine.stripPrefix());
					} else {
						curNode = curNode.parseBody(l2);
					}
				}
				curNode = curNode.parseLine(new NumberedLine("endif"));
			}
		}
		
		// Eval AST
		RootContext jobRoot = new RootContext(capturedContext, outputs, inputs);
		jobRoot.set("job.custom", new VarList());
		
		curNode = headNode;
		while (curNode != null) {
			curNode = curNode.exec(jobRoot);
		}

		return new JobDef(jobRoot.getBody(), jobRoot.cloneValues("job."), outputs, inputs);
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
