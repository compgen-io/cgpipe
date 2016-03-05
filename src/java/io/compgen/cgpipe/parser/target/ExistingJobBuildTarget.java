package io.compgen.cgpipe.parser.target;

import java.util.List;

import io.compgen.cgpipe.exceptions.ASTExecException;
import io.compgen.cgpipe.exceptions.ASTParseException;
import io.compgen.cgpipe.exceptions.RunnerException;
import io.compgen.cgpipe.loader.NumberedLine;
import io.compgen.cgpipe.parser.context.RootContext;
import io.compgen.cgpipe.runner.JobDef;
import io.compgen.cgpipe.runner.JobRunner;
import io.compgen.common.ListBuilder;

public class ExistingJobBuildTarget extends BuildTarget {
	final private String jobId;
	final private JobRunner runner;
	public ExistingJobBuildTarget(String output, String jobId, JobRunner runner) {
		super(new ListBuilder<String>().add(output).list(), null, null, null);
		this.jobId = jobId;
		this.runner = runner;
	}

	public boolean isJobValid() {
		try {
			return runner.isJobIdValid(jobId);
		} catch (RunnerException e) {
			return false;
		}
	}
		
	@Override
	public JobDef eval(List<NumberedLine> pre, List<NumberedLine> post, RootContext globalRoot) throws ASTParseException, ASTExecException {
		return null;
	}

	public String getJobId() {
		return jobId;
	}
}


