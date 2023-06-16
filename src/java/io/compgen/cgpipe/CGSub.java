package io.compgen.cgpipe;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.compgen.cgpipe.exceptions.ASTExecException;
import io.compgen.cgpipe.exceptions.ASTParseException;
import io.compgen.cgpipe.exceptions.ExitException;
import io.compgen.cgpipe.exceptions.RunnerException;
import io.compgen.cgpipe.parser.context.RootContext;
import io.compgen.cgpipe.parser.variable.VarBool;
import io.compgen.cgpipe.parser.variable.VarInt;
import io.compgen.cgpipe.parser.variable.VarString;
import io.compgen.cgpipe.parser.variable.VarValue;
import io.compgen.cgpipe.runner.ExistingJob;
import io.compgen.cgpipe.runner.JobDef;
import io.compgen.cgpipe.runner.JobRunner;
import io.compgen.cgpipe.support.SimpleFileLoggerImpl;
import io.compgen.cgpipe.support.SimpleFileLoggerImpl.Level;
import io.compgen.cmdline.MainBuilder;
import io.compgen.cmdline.annotation.Command;
import io.compgen.cmdline.annotation.Exec;
import io.compgen.cmdline.annotation.Option;
import io.compgen.cmdline.annotation.UnknownArgs;
import io.compgen.cmdline.annotation.UnnamedArg;
import io.compgen.cmdline.impl.AbstractCommand;
import io.compgen.common.ListBuilder;
import io.compgen.common.StringUtils;

@Command(name="cgsub", desc="Dynamically submit jobs for multiple input files", doc=""
		+ "cgsub will submit N number of jobs, based on the number of given input files. For\n"
		+ "each input file, a separate script is built from the command string and submitted\n"
		+ "using the cgpipe default job runner (bash, SGE, SLURM, etc...).\n"
		+ "\n"
		+ "The command string is separated from the input files using the delimiter \"--\".\n"
		+ "\n"
		+ "In the command string, you can include the input filename using \"{}\" as a\n"
		+ "placeholder. You can also adapt the filename to remove a suffix using the \n"
		+ "{^suffix.to.remove} syntax. If you want to use only the basename or the file\n"
		+ "instead of the full pathname, you can use {@} or {@suffix.to.remove}. Filename\n"
		+ "subtitution can also be used for --name, --stdout, --stderr, and --wd cgsub options.\n"
		+ "\n"
		+ "    Notes:\n"
		+ "        \"--\" can be escaped in the command string as \"\\--\".\n"
		+ "\n"
		+ "        Pipe and redirection characters can also be used in the command string, but\n"
		+ "        they should be escaped with a backslash (eg. | => \\|).\n"
		+ "\n"
		+ "        Typical cgpipe mechanisms can be used for configuration or job-specific\n"
		+ "        settings.\n"
		+ "\n"
		+ "    Example:\n"
		+ "        cgsub gunzip -c {} \\| md5sum \\> {@.gz}.md5 -- sub/*.gz\n"
		+ "\n"
		+ "        This will calculate the MD5 sum of the uncompressed contents for all \"*.gz\"\n"
		+ "        files in the directory \"sub/\" and write the output to basename.md5. For\n"
		+ "        example, if there was a file called \"sub/foo.gz\", the command that would be\n"
		+ "        submitted is:\n"
		+ "\n"
		+ "        gunzip -c sub/foo.gz | md5sum > foo.md5"
		)
public class CGSub extends AbstractCommand{
	Map<String, VarValue> confVals = new HashMap<String, VarValue>();

	private List<String> cmds = new ArrayList<String>();
	private List<String> inputs = null;
	
	private String name = null;
	private int procs = -1;
	private String mem = null;
	private String mail = null;
	private String walltime = null;
	private String stackMem = null;
	private String queue = null;
	private String project = null;
	private String wd = null;
	private String stdout = null;
	private String stderr = null;
	private String joblog = null;
	private String jobLogOutputs = null;
	private String jobSrc = null;
	private boolean dryrun = false;
	private int nice = 0;
	private String resources = null;
	private String nodeProperty = null;
	private String nodeHostname = null;
	private List<String> dependencies = null;
	private String logFilename = null;
	int verbosity = 0;

	
	public CGSub() {
	}
	
	@UnknownArgs
	public void setUnknownArg(String k, String v) {
		confVals.put(k, VarValue.parseStringRaw(v));
	}
	
	@UnnamedArg(name="commands -- input1 [input2...]")
	public void setArguments(List<String> args) {
		for (String arg: args) {
			if (inputs == null) {
				if (arg.equals("--")) {
					inputs = new ArrayList<String>();
				} else if (arg.equals("\\--")) {
					cmds.add("--");
				} else {
					cmds.add(arg);
				}
			} else {
				inputs.add(arg);
			}
		}
	}
	
	@Option(name="project", charName="P", desc="Project for job (SGE)")
	public void setProject(String project) {
		this.project = project;
	}
	@Option(name="queue", charName="q", desc="Queue for jobs (partition)")
	public void setQueue(String queue) {
		this.queue = queue;
	}
	@Option(name="procs", charName="p", desc="Processors per job", defaultValue="1")
	public void setProcs(int procs) {
		this.procs = procs;
	}
	@Option(name="job-log", desc="Write to CGPipe job/audit log")
	public void setJobLog(String joblog) {
		this.joblog=joblog;
	}

	/* 
	 * This is kept to let --job-log or --joblog be used as an argument, however --job-log
	 * should be the method used.
	 */
	@Option(name="joblog", desc="Write to CGPipe job/audit log", hide=true)
	public void setJobLogOld(String joblog) {
		this.joblog=joblog;
	}
	
	@Option(name="job-output", desc="Output file(s) to add to the audit log")
	public void setJobLogOutputs(String jobLogOutputs) {
		this.jobLogOutputs=jobLogOutputs;
	}
	@Option(name="job-src", desc="Write the submit script to this file (replaces %JOBID and %JOBNAME)")
	public void setJobSrc(String jobSrc) {
		this.jobSrc=jobSrc;
	}
	@Option(name="mem", charName="m", desc="Memory per job")
	public void setMem(String mem) {
		this.mem=mem;
	}
	
	@Option(name="mail", charName="M", desc="Send email here after job completion (or error)")
	public void setMail(String mail) {
		this.mail=mail;
	}
	
	@Option(name="nice",desc="Set the \"nice\" level for this job (SLURM or PBS)")
	public void setNice(int nice) {
		this.nice=nice;
	}
	
	@Option(name="node-property", desc="Require a property for node (PBS)")
	public void setNodeProperty(String nodeProperty) {
		this.nodeProperty=nodeProperty;
	}
	
	@Option(name="node-hostname", desc="Require a specific node (PBS)")
	public void setNodeHostname(String nodeHostname) {
		this.nodeHostname=nodeHostname;
	}
	
	@Option(name="resource", charName="r", desc="Set other resource requests (PBS)")
	public void setResources(String resources) {
		this.resources=resources;
	}
	
	@Option(name="log", charName="l", desc="Log output to this file")
	public void setLogFilename(String logFilename) {
		this.logFilename=logFilename;
	}
	
	@Option(name="walltime", charName="t", desc="Walltime per job")
	public void setWalltime(String walltime) {
		this.walltime = walltime;
	}

	@Option(name="stack", desc="Stack memory per job")
	public void setStackMem(String stackMem) {
		this.stackMem = stackMem;
	}

	@Option(name="wd", desc="Working directory", defaultValue=".", helpValue="dir")
	public void setWd(String wd) {
		this.wd = new File(wd).getAbsolutePath();
	}

	@Option(name="name", charName="n", desc="Job name")
	public void setName(String name) {
		this.name = name;
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

	@Option(charName="v", desc="Verbose output")
	public void setVerbosity() {
		verbosity++;
	}

	@Exec
	public void exec() {
		Log log = null;
		if (logFilename != null) {
			try {
				switch (verbosity) {
				case 0:
					SimpleFileLoggerImpl.setLevel(Level.INFO);
					break;
				case 1:
					SimpleFileLoggerImpl.setLevel(Level.DEBUG);
					break;
				case 2:
					SimpleFileLoggerImpl.setLevel(Level.TRACE);
					break;
				case 3:
				default:
					SimpleFileLoggerImpl.setLevel(Level.ALL);
					break;
				}
				SimpleFileLoggerImpl.setFilename(logFilename);
				log = LogFactory.getLog(CGPipe.class);
				confVals.put("cgpipe.log", new VarString(logFilename));
			} catch (FileNotFoundException e) {
				System.err.println("CGSUB ERROR " + e.getMessage());
				e.printStackTrace();
				System.exit(1);
			}
		} else {
			SimpleFileLoggerImpl.setSilent(true);
		}
		JobRunner runner = null;

		confVals.put("job.procs", new VarInt(procs));
		confVals.put("job.env", VarBool.TRUE);
		
		if (mem != null) {
			confVals.put("job.mem", new VarString(mem));
		}
		if (mail != null) {
			confVals.put("job.mail", new VarString(mail));
		}
		if (stackMem != null) {
			confVals.put("job.stack", new VarString(stackMem));
		}
		if (walltime != null) {
			confVals.put("job.walltime", new VarString(walltime));
		}
		if (queue != null) {
			confVals.put("job.queue", new VarString(queue));
		}
		if (project != null) {
			confVals.put("job.project", new VarString(project));
		}
		if (nice != 0) {
			confVals.put("job.nice", new VarInt(nice));
		}
		
		if (resources != null) {
			confVals.put("job.resources", new VarString(resources));
		}
		
		if (nodeProperty != null) {
			confVals.put("job.node.property", new VarString(nodeProperty));
		}

		if (nodeHostname != null) {
			confVals.put("job.node.hostname", new VarString(nodeHostname));
		}

		if (dryrun || (System.getenv("CGPIPE_DRYRUN") != null && !System.getenv("CGPIPE_DRYRUN").equals(""))) {
			confVals.put("cgpipe.dryrun", VarBool.TRUE);
		}

		try {
			// Load config values from global config. 
			RootContext root = new RootContext();
			CGPipe.loadInitFiles(root);
					
			root.setOutputStream(null);
			root.update(confVals);

			if (joblog != null) {
				root.set("cgpipe.joblog", new VarString(joblog));
			}

			runner = JobRunner.load(root);

			
			if (jobSrc != null) {
				root.set("job.src", new VarString(jobSrc));
			}

			if (inputs == null) {
				if (wd != null) {
					root.set("job.wd", new VarString(wd));
				}
				if (stdout != null) {
					root.set("job.stdout", new VarString(stdout));
				}
				if (stderr != null) {
					root.set("job.stderr", new VarString(stderr));
				}
					
				JobDef jobdef;
				if (jobLogOutputs==null) {
					jobdef = new JobDef(StringUtils.join(" ", cmds), root.cloneValues("job."));
				} else {
					jobdef = new JobDef(StringUtils.join(" ", cmds), root.cloneValues("job."), ListBuilder.build(jobLogOutputs.split(",")));
				}
				if (dependencies != null) {
					for (String dep: dependencies) {
						jobdef.addDependency(new ExistingJob(dep));
					}
				}
				
				if (name != null) {
					jobdef.setName(name);
				} else {
					jobdef.setName("cgsub");
				}
				runner.submit(jobdef);
			} else {			
				int i = 0;
				for (String input: inputs) {
					i++;
					List<String> inputcmds = new ArrayList<String>();
					for (String cmd: cmds) {
						inputcmds.add(convertStringForInput(cmd, input));
					}
	
					if (wd != null) {
						root.set("job.wd", new VarString(convertStringForInput(wd, input)));
					}
					if (stdout != null) {
						root.set("job.stdout", new VarString(convertStringForInput(stdout, input)));
					}
					if (stderr != null) {
						root.set("job.stderr", new VarString(convertStringForInput(stderr, input)));
					}
	
					JobDef jobdef;
					if (jobLogOutputs==null) {
						jobdef = new JobDef(StringUtils.join(" ", inputcmds), root.cloneValues("job."));
					} else {
						List<String> outs = new ArrayList<String>();
						for (String s: jobLogOutputs.split(",")) {
							outs.add(convertStringForInput(s, input));
						}
						jobdef = new JobDef(StringUtils.join(" ", inputcmds)+"\n", root.cloneValues("job."), outs, ListBuilder.build(new String[] {input}));
					}
					if (dependencies != null) {
						for (String dep: dependencies) {
							jobdef.addDependency(new ExistingJob(dep));
						}
					}
					
					if (name != null) {
						String n = convertStringForInput(name, input);
						if (n.equals(name)) {
							jobdef.setName(n+"."+i);
						} else {
							jobdef.setName(n);
						}
					} else {
						jobdef.setName("cgsub."+i);
					}
					runner.submit(jobdef);
				}
			}
			runner.done();

		} catch (ASTParseException | ASTExecException | RunnerException e) {
			if (runner != null) {
				runner.abort();
			}
			
			if (e.getClass().equals(ExitException.class)) {
				System.exit(((ExitException) e).getReturnCode());
			}
			
			if (log != null) {
				log.error(e);
				SimpleFileLoggerImpl.close();
			}
			
			System.err.println("CGSUB ERROR " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
	}

	protected String convertStringForInput(String str, String input) {
		Pattern p = Pattern.compile("^(.*)\\{([\\^@].*)?\\}(.*)$");
		Matcher m = p.matcher(str);
		while (m.matches()) {
			String basename = new File(input).getName();
			if (m.group(2) == null || m.group(2).equals("^")) {
				str = m.group(1)+input+m.group(3);					
			} else if (m.group(2).equals("@")) {
				str = m.group(1)+basename+m.group(3);					
			} else {
				String suf = m.group(2).substring(1);
				if (m.group(2).charAt(0) == '^') {
					if (input.endsWith(suf)) {
						str = m.group(1)+input.substring(0,  input.length()-suf.length())+m.group(3);						
					} else {
						str = m.group(1)+input+m.group(3);
					}
				} else if (m.group(2).charAt(0) == '@') {
					if (basename.endsWith(suf)) {
						str = m.group(1)+basename.substring(0,  basename.length()-suf.length())+m.group(3);						
					} else {
						str = m.group(1)+basename+m.group(3);
					}
				}
			}
			m = p.matcher(str);
		}
		
		return str;
	}
	
	public static void main(String[] args) throws Exception {
		new MainBuilder().runClass(CGSub.class, args);
	}
}
