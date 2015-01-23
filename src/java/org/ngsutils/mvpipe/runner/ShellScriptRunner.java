package org.ngsutils.mvpipe.runner;

import java.util.ArrayList;
import java.util.List;

import org.ngsutils.mvpipe.exceptions.RunnerException;

public class ShellScriptRunner extends JobRunner {
	public String bin = "/bin/bash";
	
	private List<JobDefinition> jobs = new ArrayList<JobDefinition>();
	
	@Override
	public boolean submit(JobDefinition jobdef) {
		jobs.add(jobdef);
		jobdef.setJobId("func_"+jobs.size());
		return true;
	}

	@Override
	public void done() throws RunnerException {
		super.done();

		String out = "#!"+bin+"\n";
		for (JobDefinition job: jobs) {
			out += job.getJobId()+"() {\n";
			out += job.getSrc();
			out += "\n}\n\n";
		}

		out += "\n";
		
		for (JobDefinition job: jobs) {
			out += job.getJobId()+"\n";
		}
		System.out.println(out);
	}

	@Override
	protected void setConfig(String k, String val) {
		if (k.equals("mvpipe.runner.shell.bin")) {
			bin = val;
		}
	}

}
