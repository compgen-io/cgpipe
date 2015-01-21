package org.ngsutils.mvpipe.parser.context;

import java.util.ArrayList;
import java.util.List;

import org.ngsutils.mvpipe.runner.JobRunner;

public class RootContext extends ExecContext {
	protected List<BuildTarget> targets = new ArrayList<BuildTarget>();
	protected boolean dryrun = false;
	
	public RootContext() {
		super();
	}

	public void setDryRun(boolean dryrun) {
		this.dryrun = dryrun;
	}
	
	public void addTarget(BuildTarget target) {
		this.targets.add(target);
	}

	public void build(String target, JobRunner runner) {
		if (target == null) {
			if (targets.size() > 0) {
				targets.get(0).build(null);
			}
			return;
		}
		for (BuildTarget tgt: targets) {
			String[] wildcards = null;
			if ((wildcards=tgt.matches(target)) != null) {
				// recurse on inputs...
				tgt.build(wildcards);
				return;
			}
		}
	}
}
