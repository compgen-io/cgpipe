package org.ngsutils.mvpipe.parser.context;

import java.util.ArrayList;
import java.util.List;

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

	public void build(String target) {
	}
}
