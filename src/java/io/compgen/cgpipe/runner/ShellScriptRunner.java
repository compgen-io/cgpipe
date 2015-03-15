package io.compgen.cgpipe.runner;

import io.compgen.cgpipe.exceptions.RunnerException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ShellScriptRunner extends JobRunner {
	private static String[] defaultShellPaths = new String[] {"/bin/bash", "/usr/bin/bash", "/usr/local/bin/bash", "/bin/sh"};
	
	public static String findDefaultShell() {
		for (String path: defaultShellPaths) {
			if (new File(path).exists()) {
				return path;
			}
		}
		return null;
	}

	public String bin = findDefaultShell();
	protected Log log = LogFactory.getLog(ShellScriptRunner.class);

	private List<JobDef> jobs = new ArrayList<JobDef>();
	
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
	public void innerDone() throws RunnerException {
		boolean header = false;
		List<String> funcNames = new ArrayList<String>();
		
		String out = "";
		for (JobDef job: jobs) {
			if (!job.getBody().equals("")) {
				if (!header) {
					out += "#!"+bin+"\n";
					header = true;
				}
				out += job.getJobId()+"() {\n";
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
			bin = val;
		}
	}

	@Override
	public boolean isJobIdValid(String jobId) {
		return false;
	}

}
