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

public class SBSTemplateRunner extends TemplateRunner {
	private String sbsHome=null;
	private String sbsPath="sbs";
	
	@Override
	public String[] getSubCommand(boolean forceHold) {
		if (sbsHome != null) {
			if (forceHold) {
				return new String[] {sbsPath, "-d", sbsHome, "submit", "-hold"};
			}
		}
		if (forceHold) {
			return new String[] {sbsPath, "submit", "-hold"};
		}
		return new String[] {sbsPath, "submit"};
	}

	@Override
	public String[] getReleaseCommand(String jobId) {
		if (sbsHome != null) {
			return new String[] {sbsPath, "-d", sbsHome, "release", jobId };
		}
		return new String[] {sbsPath, "release", jobId };
	}

	@Override
	public String[] getDelCommand(String jobId) {
		if (sbsHome != null) {
			return new String[] {sbsPath, "-d", sbsHome, "cancel", jobId};
		}
		return new String[] {sbsPath, "cancel", jobId};
	}

	@Override
	public boolean isJobIdValid(String jobId) throws RunnerException {
		try {
			String[] cmd;
			if (sbsHome!=null) {
				cmd = new String[] {sbsPath, "-d", sbsHome, "status", jobId};
			} else {
				cmd = new String[] {sbsPath, "status", jobId};
			}
			
			Process proc = Runtime.getRuntime().exec(cmd);
			int retcode = proc.waitFor();
			if (retcode == 0) {
				InputStream is = proc.getInputStream();
				String stdout = StringUtils.readInputStream(is);
				
				for (String line: stdout.split("\n")) {
					String[] spl = line.trim().split(" +");
					
					if (!spl[0].equals("job-id")) {
						if (spl.length > 3) {
							if (spl[2].equals("U") || spl[2].equals("Q") || spl[2].equals("H") || spl[2].equals("R")) {
								return true;
							}
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
	    	cxt.set("job.sbs.depids", new VarString(StringUtils.join(":", depids).replaceAll("::", ":")));
	    }

		super.updateTemplateContext(cxt, jobdef);
	}
	
	@Override
	protected void setConfig(String k, VarValue val) {
		switch(k) {
		case "cgpipe.runner.sbs.sbshome":
			this.sbsHome = val.toString();
			break;
		case "cgpipe.runner.sbs.path":
			this.sbsPath = val.toString();
			break;
		default:
			super.setConfig(k, val);
			break;
		}

	}

	@Override
	public String getConfigPrefix() {
		return "cgpipe.runner.sbs";
	}
}
