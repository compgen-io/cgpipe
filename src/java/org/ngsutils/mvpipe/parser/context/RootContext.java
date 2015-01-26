package org.ngsutils.mvpipe.parser.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ngsutils.mvpipe.exceptions.SyntaxException;
import org.ngsutils.mvpipe.parser.context.BuildTarget.Output;
import org.ngsutils.mvpipe.runner.JobDefinition;

public class RootContext extends ExecContext {
	protected List<BuildTarget> targets = new ArrayList<BuildTarget>();
	
	// Map<output-filename, job-id>
	protected Map<String, String> submittedJobs = new HashMap<String, String>();
	
	public RootContext() {
		super();
	}

	public void addTarget(BuildTarget target) {
		this.targets.add(target);
	}

	public List<String> getDefaultOutputs() {
		List<String> l = new ArrayList<String>();
		if (targets.size() > 0) {
			for (Output out: targets.get(0).getOutputs()) {
				l.add(out.rawName);
			}
		}
		return l;
	}

	public JobDefinition findBuildTarget(String outputName) throws SyntaxException {
		for (BuildTarget tgt: targets) {
			JobDefinition match = tgt.matches(outputName);
			if (match != null) {
				return match;
			}
		}
		return null;
	}
}
