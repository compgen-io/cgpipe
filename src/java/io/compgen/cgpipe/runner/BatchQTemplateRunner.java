package io.compgen.cgpipe.runner;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import io.compgen.cgpipe.exceptions.RunnerException;
import io.compgen.cgpipe.parser.context.ExecContext;
import io.compgen.cgpipe.parser.variable.VarString;
import io.compgen.cgpipe.parser.variable.VarValue;
import io.compgen.common.StringUtils;

public class BatchQTemplateRunner extends TemplateRunner {
	private String batchqHome=null;
	private String batchqPath="batchq";
	
	public String[] getDelCommandEnv() {
		return getEnv();
	}
	public String[] getRekeaseCommandEnv() {
		return getEnv();
	}
	public String[] getSubCommandEnv() {
		return getEnv();
	}
	public String[] getEnv() {
		if (batchqHome != null) {
			return new String[] {"BATCHQ_HOME="+batchqHome};
		}
		return null;
	}

	
	@Override
	public String[] getSubCommand(boolean forceHold) {
		if (batchqHome != null) {
			if (forceHold) {
				return new String[] {batchqPath, "-d", batchqHome, "submit", "--hold"};
			}
		}
		if (forceHold) {
			return new String[] {batchqPath, "submit", "--hold"};
		}
		return new String[] {batchqPath, "submit"};
	}

	@Override
	public String[] getReleaseCommand(String jobId) {
		if (batchqHome != null) {
			return new String[] {batchqPath, "-d", batchqHome, "release", jobId };
		}
		return new String[] {batchqPath, "release", jobId };
	}

	@Override
	public String[] getDelCommand(String jobId) {
		if (batchqHome != null) {
			return new String[] {batchqPath, "-d", batchqHome, "cancel", jobId};
		}
		return new String[] {batchqPath, "cancel", jobId};
	}

	@Override
	public boolean isJobIdValid(String jobId) throws RunnerException {
		try {
			String[] cmd;
			if (batchqHome!=null) {
				cmd = new String[] {batchqPath, "-d", batchqHome, "status", jobId};
			} else {
				cmd = new String[] {batchqPath, "status", jobId};
			}
			
			Process proc = Runtime.getRuntime().exec(cmd, getEnv());
			int retcode = proc.waitFor();
			if (retcode == 0) {
				InputStream is = proc.getInputStream();
				String stdout = StringUtils.readInputStream(is);
				
				for (String line: stdout.split("\n")) {
					String[] spl = line.trim().split(" +");
					if (spl[0].equals(jobId)) {
						if (spl[1].equals("QUEUED") || 
							spl[1].equals("WAITING") || 
							spl[1].equals("RUNNING") || 
							spl[1].equals("USERHOLD") ) {
							return true;
						}
					}
				}
				
				return false;
				
			}
		} catch (IOException | InterruptedException e) {
		}
		return false;
	}

	protected void updateTemplateContext(ExecContext cxt, JobDef jobdef) {
		// set the dep list
	    List<String> depids = new ArrayList<String>();
		if (jobdef.getDependencies().size() > 0) {
		    
		    for (JobDependency dep: jobdef.getDependencies()) {
				if (!dep.getJobId().equals("")) {
				    depids.add(dep.getJobId());
				}
		    }
		    
		}
	    if (jobdef.hasSetting("job.depends")) {
	    	for (String depid: jobdef.getSetting("job.depends").split(":")) {
	    		if (!depid.equals("")) {
	    			depids.add(depid);
	    		}
	    	}
	    }
	    
	    if (depids.size() > 0) {
	    	cxt.set("job.batchq.depids", new VarString(StringUtils.join(":", depids).replaceAll("::", ":")));
	    }

		super.updateTemplateContext(cxt, jobdef);
	}
	
	@Override
	protected void setConfig(String k, VarValue val) {
		switch(k) {
		case "cgpipe.runner.batchq.batchqhome":
			this.batchqHome = val.toString();
			break;
		case "cgpipe.runner.batchq.path":
			this.batchqPath = val.toString();
			break;
		default:
			super.setConfig(k, val);
			break;
		}

	}

	@Override
	public String getConfigPrefix() {
		return "cgpipe.runner.batchq";
	}
}
