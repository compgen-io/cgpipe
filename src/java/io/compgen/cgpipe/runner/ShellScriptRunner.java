package io.compgen.cgpipe.runner;

import io.compgen.cgpipe.exceptions.RunnerException;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ShellScriptRunner extends JobRunner {
	protected Log log = LogFactory.getLog(ShellScriptRunner.class);

	private List<JobDef> jobs = new ArrayList<JobDef>();
	private String shellPath = defaultShell;
	
	@Override
	public boolean submit(JobDef jobdef) {
		if (!jobdef.getBody().equals("")) {
			jobs.add(jobdef);
			jobdef.setJobId("func_"+jobs.size());
		} else {
			jobdef.setJobId("");
		}
		return true;
	}

	@Override
	public void runnerDone() throws RunnerException {
		boolean header = false;
		List<String> funcNames = new ArrayList<String>();
		
		String out = "";
		for (JobDef job: jobs) {
			if (!job.getBody().equals("")) {
				if (!header) {
					out += "#!"+shellPath+"\n";
					header = true;
				}
				out += job.getJobId()+"() {\n";
				out += "JOB_ID=\""+job.getJobId()+"\"\n";
				out += job.getBody();
				out += "\n}\n\n";
				funcNames.add(job.getJobId());
			}
		}
		out += "\n";
		
		for (String func:funcNames) {
			out += func+"\n";
		}
		System.out.println(out);
	}

	@Override
	protected void setConfig(String k, String val) {
		if (k.equals("cgpipe.runner.shell.bin")) {
			shellPath = val;
		}
	}

	@Override
	public boolean isJobIdValid(String jobId) {
		return false;
	}

}
