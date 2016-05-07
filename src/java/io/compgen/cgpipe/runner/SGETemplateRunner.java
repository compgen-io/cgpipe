package io.compgen.cgpipe.runner;

import io.compgen.cgpipe.exceptions.RunnerException;
import io.compgen.cgpipe.parser.context.ExecContext;
import io.compgen.cgpipe.parser.variable.VarString;
import io.compgen.cgpipe.parser.variable.VarValue;
import io.compgen.common.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SGETemplateRunner extends TemplateRunner {
	private boolean hvmemIsTotal = false;
	private String account=null;
	private String parallelEnv = "shm";

	@Override
	public String[] getSubCommand() {
		return new String[] {"qsub"};
	}

	@Override
	public String[] getReleaseCommand(String jobId) {
		return new String[] {"qrls", jobId};
	}

	@Override
	public String[] getDelCommand(String jobId) {
		return new String[] {"qdel", jobId};
	}

	@Override
	public boolean isJobIdValid(String jobId) throws RunnerException {
		try {
			Process proc = Runtime.getRuntime().exec(new String[] {"qstat", "-j", jobId});
			int retcode = proc.waitFor();
			if (retcode == 0) {	
				return true;
			}
		} catch (IOException | InterruptedException e) {
		}
		return false;
	}

	protected void updateTemplateContext(ExecContext cxt, JobDef jobdef) {
		if (this.account != null && !cxt.contains("job.account")) {
			cxt.set("job.account",  new VarString(this.account));
		}
		cxt.set("job.sge.parallelenv",  new VarString(this.parallelEnv));

		// set the dep list
		if (jobdef.getDependencies().size() > 0) {
		    List<String> depids = new ArrayList<String>();
		    
		    for (JobDependency dep: jobdef.getDependencies()) {
				if (!dep.getJobId().equals("")) {
				    depids.add(dep.getJobId());
				}
		    }
		    
		    cxt.set("job.sge.depids", new VarString(StringUtils.join(",", depids).replaceAll(",,", ",")));
		}

		// set the proper memory setting
		if (jobdef.hasSetting("job.mem")) {
		    if (jobdef.getSettingInt("job.procs", 1) > 1 && hvmemIsTotal) {
				cxt.set("job.mem", new VarString(jobdef.getSetting("job.mem")));
		    } else {
				// convert hvmem to a per-slot amount
				String mem = jobdef.getSetting("job.mem");
				String units = "";
				float memVal = 1;

				while (mem.length() > 0) {
				    try {
						memVal = Float.parseFloat(mem);
						break;
				    } catch (NumberFormatException e) {
						units = mem.substring(mem.length()-1) + units;
						mem = mem.substring(0, mem.length()-1);
				    }
				}
				
				cxt.set("job.mem", new VarString((memVal / jobdef.getSettingInt("job.procs", 1)) + units));
		    }
		}
		super.updateTemplateContext(cxt, jobdef);
	}
	
	@Override
	protected void setConfig(String k, VarValue val) {
		switch(k) {
		case "cgpipe.runner.sge.account":
			this.account = val.toString();
			break;
		case "cgpipe.runner.sge.parallelenv":
			this.parallelEnv = val.toString();
			break;
		case "cgpipe.runner.sge.hvmem_total":
			this.hvmemIsTotal = val.isTrue();
			break;
		default:
			super.setConfig(k, val);
			break;
		}
	}

	@Override
	public String getConfigPrefix() {
		return "cgpipe.runner.sge";
	}

	protected String buildGlobalHoldScript() {
        return 	"#!" + shell + "\n" +
        		"#$ -h\n" +
        		"#$ -terse\n" +
        		"#$ -N holding\n" +
        		"#$ -o /dev/null\n" +
        		"#$ -e /dev/null\n" +
        		"#$ -l h_rt=00:00:10\n" +
        		"sleep 1\n";
	}

}
