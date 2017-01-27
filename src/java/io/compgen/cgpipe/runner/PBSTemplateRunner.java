package io.compgen.cgpipe.runner;

import io.compgen.cgpipe.exceptions.RunnerException;
import io.compgen.cgpipe.parser.context.ExecContext;
import io.compgen.cgpipe.parser.variable.VarString;
import io.compgen.cgpipe.parser.variable.VarValue;
import io.compgen.common.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class PBSTemplateRunner extends TemplateRunner {
	private String account=null;
	private boolean trimJobId = false;
	
	@Override
	public String[] getSubCommand() {
		return new String[] {"qsub"};
	}

	@Override
	public String[] getReleaseCommand(String jobId) {
		return new String[] {"qrls", jobId };
	}

	@Override
	public String[] getDelCommand(String jobId) {
		return new String[] {"qdel", jobId};
	}

	@Override
	public boolean isJobIdValid(String jobId) throws RunnerException {
		try {
			Process proc = Runtime.getRuntime().exec(new String[] {"qstat", "-f", jobId});
			int retcode = proc.waitFor();
			if (retcode == 0) {
				InputStream is = proc.getInputStream();
				String stdout = StringUtils.readInputStream(is);
				String[] kv = stdout.trim().split(" = ");
				
				if (kv.length == 2 && kv[0].equals("job_state")) {
					if (kv[1].equals("Q") || kv[1].equals("R") || kv[1].equals("H")) {
						return true;
					}
				}
				
				return false;
				
			}
		} catch (IOException | InterruptedException e) {
		}
		return false;
	}

	protected void updateTemplateContext(ExecContext cxt, JobDef jobdef) {
		if (this.account != null && !cxt.contains("job.account")) {
			cxt.set("job.account",  new VarString(this.account));
		}

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
	    	cxt.set("job.pbs.depids", new VarString(StringUtils.join(":", depids).replaceAll("::", ":")));
	    }

//		// convert 4G to 4000; SLURM uses mem definitions in terms of megabytes.
//		String mem = jobdef.getSetting("job.mem");
//		
//		String units = "";
//		double memVal = -1;
//		
//		if (mem != null) {
//			while (mem.length() > 0) {
//			    try {
//					memVal = Double.parseDouble(mem);
//					break;
//			    } catch (NumberFormatException e) {
//					units = mem.substring(mem.length()-1) + units;
//					mem = mem.substring(0, mem.length()-1);
//			    }
//			}
//	
//			if (memVal > 0) {
//				if (units.equalsIgnoreCase("G") || units.equalsIgnoreCase("GB")) {
//					mem = "" + (int) Math.ceil(Double.valueOf(memVal * 1000));
//				} else {
//					mem = "" + (int) Math.ceil(memVal);
//				}
//			}
//	
//			cxt.set("job.mem", new VarString(mem));
//		}
		
		
		super.updateTemplateContext(cxt, jobdef);
	}
	
	@Override
	protected String submitScript(String src) throws RunnerException {
		String jobId = super.submitScript(src);
		if (trimJobId) {
			jobId = jobId.split("\\.")[0];
		}
		return jobId;
	}

	@Override
	protected void setConfig(String k, VarValue val) {
		switch(k) {
		case "cgpipe.runner.pbs.account":
			this.account = val.toString();
			break;
		case "cgpipe.runner.pbs.trim_jobid":
			this.trimJobId = true;
			break;
		default:
			super.setConfig(k, val);
			break;
		}

	}

	@Override
	public String getConfigPrefix() {
		return "cgpipe.runner.pbs";
	}

	protected String buildGlobalHoldScript() {
        return 	"#!" + shell + "\n" +
        		"#PBS -h\n" +
        		"#PBS -N holding\n" +
        		"#PBS -k n\n" +
        		"#PBS -t 00:00:30\n" +
        		"sleep 1\n";
	}

}
