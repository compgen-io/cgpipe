package io.compgen.cgpipe.runner;

import io.compgen.cgpipe.exceptions.ASTExecException;
import io.compgen.cgpipe.exceptions.ASTParseException;
import io.compgen.cgpipe.exceptions.RunnerException;
import io.compgen.cgpipe.exceptions.VarTypeException;
import io.compgen.cgpipe.parser.TemplateParser;
import io.compgen.cgpipe.parser.context.ExecContext;
import io.compgen.cgpipe.parser.context.RootContext;
import io.compgen.cgpipe.parser.variable.VarList;
import io.compgen.cgpipe.parser.variable.VarString;
import io.compgen.common.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class TemplateRunner extends JobRunner {
	protected Log log = LogFactory.getLog(getClass());

	protected boolean globalHold = false;
	protected String templateFilename = getClass().getCanonicalName().replaceAll("\\.", File.separator)+".template.cgp";
	protected String shell = ShellScriptRunner.defaultShell;

	private JobDependency globalHoldJob = null;
	private int dryRunJobCount = 0;
	private List<String> jobids = new ArrayList<String>();

	public abstract String getConfigPrefix();
	public abstract String[] getSubCommand();
	public abstract String[] getReleaseCommand();
	public abstract String[] getDelCommand();

	protected String buildGlobalHoldScript() {
		return null;
	}

	public String loadTemplate() throws IOException {
		InputStream is = getClass().getClassLoader().getResourceAsStream(templateFilename);
		if (is == null) {
			is = new FileInputStream(templateFilename);
		}

		return StringUtils.readInputStream(is);
	}
	
	protected void setConfig(String k, String val) {
		if (k.equals(getConfigPrefix()+".template")) {
			this.templateFilename = val;
		} else if (k.equals(getConfigPrefix()+".global_hold") && val.toUpperCase().equals("TRUE")) {
			this.globalHold = true;
		} else if (k.equals(getConfigPrefix()+".shell")) {
			this.shell = val;
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
	}
	
	public void init() {}

	@Override
	public boolean submit(JobDef jobdef) throws RunnerException {
		if (jobdef.getBody().equals("")) {
			jobdef.setJobId("");
			return false;
		}
		
		if (globalHold) {
			if (globalHoldJob == null) {
				submitGlobalHold();
			}
			jobdef.addDependency(globalHoldJob);
		}
		
		String src = buildScriptString(jobdef);
		String jobid = submitScript(src);
		jobdef.setJobId(jobid);
		jobids.add(jobid);

		log.info("SUBMIT JOB: "+jobid);
		for (String line: src.split("\n")) {
			log.debug(jobid + " " + line);
		}
	
		return true;
	}

	private void submitGlobalHold() throws RunnerException {
		String src = buildGlobalHoldScript();
		String jobid = submitScript(src);
		globalHoldJob = new ExistingJob(jobid);
		jobids.add(jobid);

		log.info("GLOBAL HOLD: "+jobid);
		for (String line: src.split("\n")) {
			log.debug(jobid + " " + line);
		}
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
	
	private String submitScript(String src) throws RunnerException {
		if (dryrun) {
			dryRunJobCount++;
			return "dryrun." + dryRunJobCount;
		}
		
		try {
			Process proc = Runtime.getRuntime().exec(getSubCommand());
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
				throw new RunnerException("Bad return code from qsub: "+retcode+" - "+err + "\n\n"+src);
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
			System.out.println(StringUtils.join("\n", jobids));
			
			if (!dryrun && globalHoldJob != null) {
				try {
					
					String[] cmd = buildCommandString(getReleaseCommand(),globalHoldJob.getJobId()); 
					int retcode = Runtime.getRuntime().exec(cmd).waitFor();
					if (retcode != 0) {
						throw new RunnerException("Unable to release global hold");
					}
				} catch (IOException | InterruptedException e) {
					throw new RunnerException(e);
				}
			}
		}
	}

	private String[] buildCommandString(String[] cmd, String...args) {
		String[] out = new String[cmd.length + args.length];
		for (int i=0; i<cmd.length;i++) {
			out[i] = cmd[i];
		}
		for (int i=0; i<args.length;i++) {
			out[cmd.length + i] = args[i];
		}
		return out;
	}
	
	@Override
	public void abort() {
		for (String jobid: jobids) {
			try {
				Runtime.getRuntime().exec(buildCommandString(getDelCommand(), jobid)).waitFor();
			} catch (InterruptedException | IOException e) {
			}
		}
	}
}
