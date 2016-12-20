package io.compgen.cgpipe.runner;

import io.compgen.cgpipe.exceptions.RunnerException;
import io.compgen.cgpipe.parser.variable.VarValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class GraphvizRunner extends JobRunner {
	protected Log log = LogFactory.getLog(GraphvizRunner.class);

	private Map<String, JobDef> jobs = new HashMap<String, JobDef>();
	
	@Override
	public boolean submit(JobDef jobdef) {
		if (!jobdef.getBody().equals("")) {
			String id = "func_"+jobs.size();
			jobdef.setJobId(id);
			jobs.put(id, jobdef);
		} else {
			jobdef.setJobId("");
		}
		return true;
	}

	@Override
	public void runnerDone() throws RunnerException {
		List<String> funcNames = new ArrayList<String>();
		
		String out = "";
		for (JobDef job: jobs.values()) {
			if (!job.getBody().equals("")) {
				
				for (JobDependency dep: job.getDependencies()) {
					out += jobs.get(dep.getJobId()).getSafeName() + " -> " + job.getSafeName()+";\n";
				}
				funcNames.add(job.getJobId());
			}
		}
		out += "\n";
		
		System.out.println("digraph G { \n"+out+"}\n");
	}

	@Override
	protected void setConfig(String k, VarValue val) {
	}

	@Override
	public boolean isJobIdValid(String jobId) {
		return false;
	}

}
