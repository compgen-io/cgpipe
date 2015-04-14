package io.compgen.cgpipe.parser.target;

import io.compgen.cgpipe.exceptions.ASTExecException;
import io.compgen.cgpipe.exceptions.ASTParseException;
import io.compgen.cgpipe.loader.NumberedLine;
import io.compgen.cgpipe.parser.TemplateParser;
import io.compgen.cgpipe.parser.context.RootContext;
import io.compgen.cgpipe.parser.variable.VarList;
import io.compgen.cgpipe.parser.variable.VarValue;
import io.compgen.cgpipe.runner.JobDef;
import io.compgen.cgpipe.runner.JobDependency;
import io.compgen.common.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BuildTarget {

	private List<String> inputs;
	private List<String> outputs;
	private Map<String, VarValue> capturedContext;
	private List<NumberedLine> lines;
	private boolean skippable = false;
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
		RootContext jobRoot = new RootContext(capturedContext, outputs, inputs);
		jobRoot.set("job.custom", new VarList());

		TemplateParser.parseTemplate(lines, pre, post, jobRoot);

		return new JobDef(jobRoot.getBody(), jobRoot.cloneValues("job."), outputs, inputs);
	}

	public boolean isSkippable() {
		return skippable;
	}

	public void setSkippable(boolean skippable) {
		this.skippable = skippable;
	}

	public void setSubmittedJobDep(JobDependency jobDep) {
		this.submittedJobDep = jobDep;
	}

	public JobDependency getJobDep() {
		return this.submittedJobDep;
	}

}
