package io.compgen.cgpipe.runner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.compgen.cgpipe.exceptions.ASTExecException;
import io.compgen.cgpipe.exceptions.ASTParseException;
import io.compgen.cgpipe.exceptions.RunnerException;
import io.compgen.cgpipe.exceptions.VarTypeException;
import io.compgen.cgpipe.parser.TemplateParser;
import io.compgen.cgpipe.parser.context.ExecContext;
import io.compgen.cgpipe.parser.context.RootContext;
import io.compgen.cgpipe.parser.variable.VarList;
import io.compgen.cgpipe.parser.variable.VarString;
import io.compgen.cgpipe.parser.variable.VarValue;
import io.compgen.common.IterUtils;
import io.compgen.common.IterUtils.EachPair;
import io.compgen.common.StringUtils;

public abstract class TemplateRunner extends JobRunner {
	protected Log log = LogFactory.getLog(getClass());

	protected boolean globalHold = false;
	protected String templateFilename = getClass().getCanonicalName().replaceAll("\\.", File.separator)+".template.cgp";
	protected String shell = ShellScriptRunner.defaultShell;

	private int dryRunJobCount = 0;
	private List<String> jobids = new ArrayList<String>();
	private List<String> jobOutputs = new ArrayList<String>();
	private List<String> globalHolds = new ArrayList<String>();

	public abstract String getConfigPrefix();
	public abstract String[] getSubCommand(boolean forceHold);
	public abstract String[] getReleaseCommand(String jobId);
	public abstract String[] getDelCommand(String jobId);
	
	public String[] getSubCommandEnv() {return null;}
	public String[] getReleaseCommandEnv() {return null;}
	public String[] getDelCommandEnv() {return null;}

	public String loadTemplate() throws IOException {
		InputStream is = getClass().getClassLoader().getResourceAsStream(templateFilename);
		if (is == null) {
			is = new FileInputStream(templateFilename);
		}

		return StringUtils.readInputStream(is);
	}
	
	protected void setConfig(String k, VarValue val) {
		if (k.equals(getConfigPrefix()+".template")) {
			this.templateFilename = val.toString();
		} else if (k.equals(getConfigPrefix()+".global_hold") && val.toBoolean()) {
			this.globalHold = true;
		} else if (k.equals(getConfigPrefix()+".shell")) {
			this.shell = val.toString();
		}
	}

	protected void updateTemplateContext(ExecContext cxt, JobDef jobdef) {
		if (!cxt.contains("job.shell")) {
			cxt.set("job.shell", new VarString(shell));
		}

		cxt.set("job._body", new VarString(jobdef.getBody()));
		
		VarList outputs = new VarList();
		for (String out:jobdef.getOutputs()) {
			try {
				outputs.add(new VarString(out));
			} catch (VarTypeException e) {
			}
		}
		
		cxt.set("job._outputs", outputs);
		
		if (!cxt.contains("job.wd")) {
			try {
				cxt.set("job.wd", new VarString(new File(".").getCanonicalPath()));
			} catch (IOException e) {
			}
		}
		
		if (!cxt.contains("job.name")) {
			cxt.set("job.name", new VarString(jobdef.getName()));
		}
		
	}
	
	public void init() {}

	@Override
	public boolean submit(JobDef jobdef) throws RunnerException {
		if (jobdef.getBody().equals("")) {
			jobdef.setJobId("");
			return false;
		}
		
		boolean globalHoldSet = false;
		if (globalHold) {
			if (!jobdef.getSettingBool("job.hold")) {
				globalHoldSet = true;
			}
		}
		
		String src = buildScriptString(jobdef);
		String jobid = submitScript(src, globalHoldSet);
		jobdef.setJobId(jobid);
		jobids.add(jobid);
		jobOutputs.add(StringUtils.join(",", jobdef.getOutputs()));

		log.info("SUBMIT JOB: "+jobid);
		for (String line: src.split("\n")) {
			log.debug(jobid + " " + line);
		}
	
		logJob(jobdef);
		
		if (globalHoldSet) {
			globalHolds.add(jobid);
		}
		
		if (jobdef.getSettingsMap().containsKey("job.src")) {
			String fname = jobdef.getSetting("job.src");
			fname = fname.replaceAll("%JOBID", jobid);
			fname = fname.replaceAll("%JOBNAME", jobdef.getName());
			try {
				OutputStream os = new FileOutputStream(fname);
				os.write(src.getBytes());
				os.close();
			} catch (IOException e) {
				throw new RunnerException(e);
			}
		}
		
		return true;
	}

	private String buildScriptString(JobDef jobdef) throws RunnerException {
		RootContext cxt = new RootContext(jobdef.getSettingsMap());
		updateTemplateContext(cxt, jobdef);
		
		try {
			String template = loadTemplate();
			return  TemplateParser.parseTemplateString(template, cxt);
		} catch (IOException | ASTExecException | ASTParseException e) {
			throw new RunnerException(e);
		}
	}
	
	protected String submitScript(String src) throws RunnerException {
		return submitScript(src, false);
	}
	protected String submitScript(String src, boolean forceHold) throws RunnerException {
		if (dryrun) {
			dryRunJobCount++;
			System.err.println("[dryrun." + dryRunJobCount+"]");
			System.err.println(src);
			return "dryrun." + dryRunJobCount;
		}
		
		try {
			Process proc = Runtime.getRuntime().exec(getSubCommand(forceHold), getSubCommandEnv());
			proc.getOutputStream().write(src.getBytes(Charset.forName("UTF8")));
			proc.getOutputStream().close();
			
			InputStream is = proc.getInputStream();
			InputStream es = proc.getErrorStream();

			int retcode = proc.waitFor();
			String out = StringUtils.readInputStream(is);
			String err = StringUtils.readInputStream(es);

			is.close();
			es.close();

			if (retcode != 0) {	
				throw new RunnerException("Bad return code from submit: "+StringUtils.join(" ", getSubCommand(forceHold))+"  => ("+retcode+") "+err + "\n\n"+src);
			}
			
			return StringUtils.strip(out);

		} catch (IOException | InterruptedException e) {
			throw new RunnerException(e);
		}
	}

	
	@Override
	public void runnerDone() throws RunnerException {
		if (jobids.size() > 0) {
			log.info("submitted jobs: "+StringUtils.join(",", jobids));
			
			if (rootContext.get("cgpipe.runner.include_output_filenames").toBoolean()) {
				IterUtils.zip(jobids, jobOutputs, new EachPair<String, String>() {
					@Override
					public void each(String jobid, String outputs) {
						if (outputs == null || outputs.equals("")) {
							System.out.println(jobid+" (no-output-files)");
						} else {
							System.out.println(jobid+" "+outputs);
						}
					}});
				
			} else {
				System.out.println(StringUtils.join("\n", jobids));
			}
			
			if (!dryrun && globalHold) {
				//System.out.println("Releasing hold on: " + StringUtils.join(",", globalHolds));
				for (String jobid: globalHolds) {
					try {
						int retcode = Runtime.getRuntime().exec(getReleaseCommand(jobid), getReleaseCommandEnv()).waitFor();
						if (retcode != 0) {
							throw new RunnerException("Unable to release default user-hold");
						}
					} catch (IOException | InterruptedException e) {
						throw new RunnerException(e);
					}
				}
					
			}
		}
	}
	
	@Override
	public void abort() {
		for (String jobid: jobids) {
			cancelJob(jobid);
		}
	}
	
	@Override
	public boolean cancelJob(String jobid) {
		try {
			Process proc = Runtime.getRuntime().exec(getDelCommand(jobid), getDelCommandEnv());
			int ret = proc.waitFor();
			return ret == 0;
		} catch (InterruptedException | IOException e) {
		}
		return false;
	}
}
