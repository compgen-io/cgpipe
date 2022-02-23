package io.compgen.cgpipe.runner;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.compgen.cgpipe.exceptions.RunnerException;
import io.compgen.cgpipe.parser.variable.VarValue;
import io.compgen.common.StringUtils;

public class ShellScriptRunner extends JobRunner {
	protected Log log = LogFactory.getLog(ShellScriptRunner.class);

	private List<JobDef> jobs = new ArrayList<JobDef>();
	private String shellPath = defaultShell;
	private String scriptFilename = null;
	private boolean autoExec = false;

	private boolean loadedScript = false;
	private List<String> preLines = new ArrayList<String>(); 
	private List<String> postLines = new ArrayList<String>(); 
	int currentFuncNum = 0;
	
	protected void loadScript() throws RunnerException {
		loadedScript = true;
		try {
			if (scriptFilename != null) {
				File scriptFile = new File(scriptFilename);
				if (scriptFile.exists()) {
					boolean markerFound = false;
					String script = StringUtils.readFile(scriptFile);
					String[] scriptLines = script.split("\n");
					for (String line: scriptLines) {
						if (line.equals("#CGP_RUN")) {
							markerFound = true;
						} else if (!markerFound) {
							preLines.add(line);
							if (line.startsWith("cgpfunc_")) {
								currentFuncNum = Integer.parseInt(line.substring(8, line.indexOf("()")));
							}
						} else {
							postLines.add(line);
						}
					}
				}
				scriptFile.delete();
			}
		} catch (IOException e) {
			throw new RunnerException(e);
		}
	}
	
	@Override
	public boolean submit(JobDef jobdef) throws RunnerException {
		if (!loadedScript && scriptFilename!=null) {
			loadScript();
		}
		if (!jobdef.getBody().equals("")) {
			currentFuncNum++;
			jobdef.setJobId("cgpfunc_"+currentFuncNum);
			System.err.println(jobdef.getJobId());
			jobs.add(jobdef);
			if (scriptFilename != null) {
				logJob(jobdef);
			}
		} else {
			jobdef.setJobId("");
		}
		return true;
	}

	@Override
	public void runnerDone() throws RunnerException {
		for (JobDef job: jobs) {
			if (!job.getBody().equals("")) {
				if (preLines.size() == 0) {
					preLines.add("#!"+shellPath);
				}

				preLines.add("");
				preLines.add(job.getJobId() + "() {");
				preLines.add("JOB_ID=\""+job.getJobId()+"\"");
				preLines.add(job.getBody());
				preLines.add("}");
			}
		}
	
		if (currentFuncNum == 0) {
			return;
		}
		
		preLines.add("#CGP_RUN");

		for (String line: postLines) {
			preLines.add(line);
		}

		for (JobDef job: jobs) {
			preLines.add("##");
			if (!job.getJobId().equals("")) {
				for (String output: job.getOutputs()) {
					preLines.add("if [ ! -e \"" + output + "\" ]; then");
				}
				preLines.add(job.getJobId()+" || exit $?");
				List<String> outputs = job.getOutputs();
				for (int i = 0; i < outputs.size(); i++) {
					preLines.add("fi");
				}
			}
		}
			
		try {

			OutputStream os;
			File tmpFile = null;
			File scriptFile = null;
	
			if (scriptFilename != null) {
				scriptFile = new File(scriptFilename);
				os = new FileOutputStream(scriptFile);
			} else if (autoExec) {
				// write to a temp file an exec that.
				tmpFile = File.createTempFile("cgpipe_", ".sh");
				tmpFile.deleteOnExit();
				os = new FileOutputStream(tmpFile);
			} else {
				os = System.out;
			}
		
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));
		
			for (String line: preLines) {
				writer.write(line+"\n");
				if (autoExec) {
					System.err.println(line);
				}
			}
	
			writer.flush();
			writer.close();

			if (scriptFile != null) {
				scriptFile.setExecutable(true);
				if (autoExec) {
					Process p = new ProcessBuilder(scriptFile.getAbsolutePath())
				    .redirectError(Redirect.INHERIT)
				    .redirectOutput(Redirect.INHERIT)
				    .start();
	
					p.waitFor();
				}
			} else if (autoExec) {
				tmpFile.setExecutable(true);
				if (autoExec) {
					Process p = new ProcessBuilder(tmpFile.getAbsolutePath())
				    .redirectError(Redirect.INHERIT)
				    .redirectOutput(Redirect.INHERIT)
				    .start();
	
					p.waitFor();
				}
			}
			
			
		} catch (IOException e) {
			throw new RunnerException(e);
		} catch (InterruptedException e) {
			throw new RunnerException(e);
		}
	}

	@Override
	protected void setConfig(String k, VarValue val) {
		if (k.equals("cgpipe.runner.shell.bin")) {
			shellPath = val.toString();
		}
		if (k.equals("cgpipe.runner.shell.filename")) {
			scriptFilename = val.toString();
		}
		if (k.equals("cgpipe.runner.shell.autoexec")) {
			autoExec = val.toBoolean();
		}
	}

	@Override
	public boolean isJobIdValid(String jobId) {
		if (scriptFilename != null) {
			try {
				File scriptFile = new File(scriptFilename);
				if (scriptFile.exists()) {
					String script;
						script = StringUtils.readFile(scriptFile);
					String[] scriptLines = script.split("\n");
					for (String line: scriptLines) {
						if (line.startsWith(jobId+"()")) {
							return true;
						}
					}
				}
			} catch (IOException e) {
			}
		}

		return false;
	}

	@Override
	public boolean cancelJob(String jobId) {
		return false;
	}
}
