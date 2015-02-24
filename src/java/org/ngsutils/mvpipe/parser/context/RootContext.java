package org.ngsutils.mvpipe.parser.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ngsutils.mvpipe.exceptions.SyntaxException;
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
		for (BuildTarget tgt: targets) {
			if (tgt.getOutputs().get(0).startsWith("__")) {
				continue;
			}
			for (String out: tgt.getOutputs()) {
				l.add(out);
			}
			break;
		}
		return l;
	}

	public List<JobDefinition> findCandidateTarget(String outputName) throws SyntaxException {
		List<JobDefinition> jobdefs = new ArrayList<JobDefinition>();
		for (BuildTarget tgt: targets) {
			try{
				JobDefinition match = tgt.matches(outputName, this);
				if (match != null) {
					jobdefs.add(match);
				}
			} catch(SyntaxException e) {
			}
		}
		return jobdefs;
	}
}
