package org.ngsutils.mvpipe.runner;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ngsutils.mvpipe.exceptions.RunnerException;
import org.ngsutils.mvpipe.exceptions.SyntaxException;

public class ShellScriptRunner extends JobRunner {
	public String bin = "/bin/bash";
	protected Log log = LogFactory.getLog(ShellScriptRunner.class);

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

		try {
			boolean header = false;
			String out = "";
			for (JobDefinition job: jobs) {
				if (!job.getSrc().equals("")) {
					if (!header) {
						out += "#!"+bin+"\n";
						header = true;
					}
					out += job.getJobId()+"() {\n";
					out += job.getSrc();
					out += "\n}\n\n";
				}
			}
			out += "\n";
			
			for (JobDefinition job: jobs) {
				if (!job.getSrc().equals("")) {
					out += job.getJobId()+"\n";
				}
			}
			System.out.println(out);
		} catch (SyntaxException e) {
			throw new RunnerException(e);
		}
	}

	@Override
	protected void setConfig(String k, String val) {
		if (k.equals("mvpipe.runner.shell.bin")) {
			bin = val;
		}
	}

}
