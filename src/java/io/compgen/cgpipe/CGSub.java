package io.compgen.cgpipe;

import io.compgen.cgpipe.exceptions.ASTExecException;
import io.compgen.cgpipe.exceptions.ASTParseException;
import io.compgen.cgpipe.exceptions.ExitException;
import io.compgen.cgpipe.exceptions.RunnerException;
import io.compgen.cgpipe.parser.Parser;
import io.compgen.cgpipe.parser.context.RootContext;
import io.compgen.cgpipe.parser.variable.VarBool;
import io.compgen.cgpipe.parser.variable.VarInt;
import io.compgen.cgpipe.parser.variable.VarString;
import io.compgen.cgpipe.parser.variable.VarValue;
import io.compgen.cgpipe.runner.ExistingJob;
import io.compgen.cgpipe.runner.JobDef;
import io.compgen.cgpipe.runner.JobRunner;
import io.compgen.cgpipe.support.SimpleFileLoggerImpl;
import io.compgen.cmdline.MainBuilder;
import io.compgen.cmdline.annotation.Command;
import io.compgen.cmdline.annotation.Exec;
import io.compgen.cmdline.annotation.Option;
import io.compgen.cmdline.annotation.UnnamedArg;
import io.compgen.cmdline.impl.AbstractCommand;
import io.compgen.common.StringUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Command(name="cgsub", desc="Dynamically submit jobs for multiple input files")
public class CGSub extends AbstractCommand{

	private List<String> cmds = new ArrayList<String>();
	private List<String> inputs = null;
	
	private int procs = -1;
	private String mem = null;
	private String walltime = null;
	private String stackMem = null;
	private String wd = null;
	private String stdout = null;
	private String stderr = null;
	private boolean dryrun = false;
	private List<String> dependencies = null;
	
	public CGSub() {
	}
	
	@UnnamedArg(name="commands -- input1 {input2...}")
	public void setArguments(List<String> args) {
		for (String arg: args) {
			if (inputs == null) {
				if (arg.equals("--")) {
					inputs = new ArrayList<String>();
				} else {
					cmds.add(arg);
				}
			} else {
				inputs.add(arg);
			}
		}
	}
	
	@Option(name="procs", charName="p", desc="Processors per job", defaultValue="1")
	public void setProcs(int procs) {
		this.procs = procs;
	}
	@Option(name="mem", charName="m", desc="Memory per job")
	public void setMem(String mem) {
		this.mem=mem;
	}
	
	@Option(name="walltime", charName="t", desc="Walltime per job", defaultValue="2:00:00")
	public void setWalltime(String walltime) {
		this.walltime = walltime;
	}

	@Option(name="stack", desc="Stack memory per job")
	public void setStackMem(String stackMem) {
		this.stackMem = stackMem;
	}

	@Option(name="wd", desc="Working directory", defaultValue=".", helpValue="dir")
	public void setWd(String wd) {
		this.wd = wd;
	}

	@Option(name="stdout", desc="Write stdout to file/dir")
	public void setStdout(String stdout) {
		this.stdout = stdout;
	}

	@Option(name="stderr", desc="Write stderr to file/dir")
	public void setStderr(String stderr) {
		this.stderr = stderr;
	}

	@Option(name="dr", desc="Dry-run")
	public void setDryrun(boolean dryrun) {
		this.dryrun = dryrun;
	}

	@Option(name="deps", desc="Job dependencies (comma-delimited)")
	public void setDependencies(String deps) {
		this.dependencies = new ArrayList<String>();
		for (String dep: deps.split(",")) {
			dependencies.add(dep);
		}
	}

	@Exec
	public void exec() {
		SimpleFileLoggerImpl.setSilent(true);
		JobRunner runner = null;
		Map<String, VarValue> confVals = new HashMap<String, VarValue>();

		confVals.put("job.procs", new VarInt(procs));
		confVals.put("job.env", VarBool.TRUE);
		
		if (mem != null) {
			confVals.put("job.mem", new VarString(mem));
		}
		if (stackMem != null) {
			confVals.put("job.stack", new VarString(stackMem));
		}
		if (walltime != null) {
			confVals.put("job.walltime", new VarString(walltime));
		}
		if (wd != null) {
			confVals.put("job.wd", new VarString(wd));
		}
		if (stdout != null) {
			confVals.put("job.stdout", new VarString(stdout));
		}
		if (stderr != null) {
			confVals.put("job.stderr", new VarString(stderr));
		}
		
		try {
			// Load config values from global config. 
			RootContext root = new RootContext();

			// Parse the default cgpiperc
			InputStream is = CGPipe.class.getClassLoader().getResourceAsStream("io/compgen/cgpipe/cgpiperc");
			if (is != null) {
				Parser.exec("io/compgen/cgpipe/cgpiperc", is,  root);
			}
			
			// Parse RC file
			if (CGPipe.RCFILE.exists()) {
				Parser.exec(CGPipe.RCFILE.getAbsolutePath(), root);
			}

			root.setOutputStream(null);
			runner = JobRunner.load(root, dryrun);

			// find a build-target, and submit the job(s) to a runner
		
			Pattern p = Pattern.compile("^(.*)\\{(\\^.*)?\\}(.*)$");
			for (String input: inputs) {
				List<String> inputcmds = new ArrayList<String>();
				for (String cmd: cmds) {
					Matcher m = p.matcher(cmd);
					while (m.matches()) {
						if (m.group(2) == null) {
							cmd = m.group(1)+input+m.group(3);						
						} else {
							String suf = m.group(2).substring(1);
							if (input.endsWith(suf)) {
								cmd = m.group(1)+input.substring(0,  input.length()-suf.length())+m.group(3);						
							} else {
								cmd = m.group(1)+input+m.group(3);
							}
						}
						m = p.matcher(cmd);
					}
					inputcmds.add(cmd);
				}
				JobDef jobdef = new JobDef(StringUtils.join(" ", inputcmds), confVals);
				if (dependencies != null) {
					for (String dep: dependencies) {
						jobdef.addDependency(new ExistingJob(dep));
					}
				}
				runner.submit(jobdef);
			}
			runner.done();

		} catch (ASTParseException | ASTExecException | RunnerException e) {
			if (runner != null) {
				runner.abort();
			}
			
			if (e.getClass().equals(ExitException.class)) {
				System.exit(((ExitException) e).getReturnCode());
			}
			
			System.out.println("CGSUB ERROR " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void main(String[] args) throws Exception {
		new MainBuilder().runClass(CGSub.class, args);
	}

}
