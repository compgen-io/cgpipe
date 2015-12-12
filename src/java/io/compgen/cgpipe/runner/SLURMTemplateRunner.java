package io.compgen.cgpipe.runner;

import io.compgen.cgpipe.exceptions.RunnerException;
import io.compgen.cgpipe.parser.context.ExecContext;
import io.compgen.cgpipe.parser.variable.VarString;
import io.compgen.common.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class SLURMTemplateRunner extends TemplateRunner {
	private String account=null;

	@Override
	public String[] getSubCommand() {
		return new String[] {"sbatch", "--parsable"};
	}

	@Override
	public String[] getReleaseCommand(String jobId) {
		return new String[] {"scontrol", "release", jobId };
	}

	@Override
	public String[] getDelCommand(String jobId) {
		return new String[] {"scancel", jobId};
	}

	@Override
	public boolean isJobIdValid(String jobId) throws RunnerException {
		try {
			Process proc = Runtime.getRuntime().exec(new String[] {"scontrol", "-o", "show", "job", jobId});
			int retcode = proc.waitFor();
			if (retcode == 0) {
				InputStream is = proc.getInputStream();
				String stdout = StringUtils.readInputStream(is);
				String[] ar = stdout.split(" ");
				for (String el: ar) {
					String[] kv = el.split("=");
					if (kv.length == 2 && kv[0].equals("JobState")) {
						if (kv[1].equals("PENDING") || kv[1].equals("RUNNING")) {
							return true;
						}
					}
				}
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
		if (jobdef.getDependencies().size() > 0) {
		    List<String> depids = new ArrayList<String>();
		    
		    for (JobDependency dep: jobdef.getDependencies()) {
				if (!dep.getJobId().equals("")) {
				    depids.add(dep.getJobId());
				}
		    }
		    
		    cxt.set("job.slurm.depids", new VarString(StringUtils.join(":", depids).replaceAll("::", ":")));
		}

		// convert 4G to 4000; SLURM uses mem definitions in terms of megabytes.
		String mem = jobdef.getSetting("job.mem");
		
		String units = "";
		float memVal = -1;
		
		if (mem != null) {
			while (mem.length() > 0) {
			    try {
					memVal = Float.parseFloat(mem);
					break;
			    } catch (NumberFormatException e) {
					units = mem.substring(mem.length()-1) + units;
					mem = mem.substring(0, mem.length()-1);
			    }
			}
	
			if (memVal > 0) {
				mem = Float.toString(memVal);
				if (units.equalsIgnoreCase("G") || units.equalsIgnoreCase("GB")) {
					mem = ""+Float.valueOf(memVal * 1000).intValue();
				}
			}
	
			cxt.set("job.mem", new VarString(mem));
		}
		
		
		super.updateTemplateContext(cxt, jobdef);
	}
	
	@Override
	protected void setConfig(String k, String val) {
		switch(k) {
		case "cgpipe.runner.slurm.account":
			this.account = val;
			break;
		default:
			super.setConfig(k, val);
			break;
		}

	}

	@Override
	public String getConfigPrefix() {
		return "cgpipe.runner.slurm";
	}

	protected String buildGlobalHoldScript() {
        return 	"#!" + shell + "\n" +
        		"#SBATCH -H\n" +
        		"#SBATCH -J holding\n" +
        		"#SBATCH -o /dev/null\n" +
        		"#SBATCH -e /dev/null\n" +
        		"#SBATCH -t 00:00:30\n" +
        		"sleep 1\n";
	}

}
