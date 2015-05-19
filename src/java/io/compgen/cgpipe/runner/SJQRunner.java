package io.compgen.cgpipe.runner;

import io.compgen.cgpipe.CGPipe;
import io.compgen.cgpipe.exceptions.RunnerException;
import io.compgen.cmdline.exceptions.CommandArgumentException;
import io.compgen.common.IterUtils;
import io.compgen.common.IterUtils.MapFunc;
import io.compgen.common.StringUtils;
import io.compgen.sjq.client.AuthException;
import io.compgen.sjq.client.ClientException;
import io.compgen.sjq.client.SJQClient;
import io.compgen.sjq.server.SJQServer;
import io.compgen.sjq.server.SJQServerException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SJQRunner extends JobRunner {
	protected Log log = LogFactory.getLog(SJQRunner.class);

	private SJQServer server = null;
	private SJQClient client = null;
	
	private File socketFile = new File(CGPipe.CGPIPE_HOME, ".sjqserv");
	private File passwdFile = new File(CGPipe.CGPIPE_HOME, ".sjqpass");
	private File jobLogFile = new File(CGPipe.CGPIPE_HOME, ".sjqjobs");
	private int maxProcs = Runtime.getRuntime().availableProcessors();
	private String maxMem = null;
	private String tempDir = null;
	private int port = 0;
	
	private List<String> submittedJobs = new ArrayList<String>();

	private int dryRunJobCount;
	
	public void connect() throws IOException, CommandArgumentException, SJQServerException {
		// protect this connection with a random 50 char string.
		String passwd;
		
		if (!socketFile.exists()) {
			passwdFile.createNewFile();
			// chmod 600
			passwdFile.setReadable(false,  false);
			passwdFile.setWritable(false,  false);
			passwdFile.setExecutable(false,  false);
			passwdFile.setReadable(true,  true);
			passwdFile.setWritable(true,  true);
			// remove me when this server is done
			passwdFile.deleteOnExit();
			
			passwd = StringUtils.randomString(50);
			StringUtils.writeFile(passwdFile, passwd);
			
			server = new SJQServer();
			server.setMaxMemory(maxMem);
			server.setMaxProcs(maxProcs);
			server.setConnFilename(socketFile.getAbsolutePath());
			server.setPasswdFile(passwdFile.getAbsolutePath());
			server.setTempDir(tempDir);
			server.setSilent(false);
			server.setPort(port);
			server.setTimeout(30);
			server.setJobLogFile(jobLogFile.getAbsolutePath());
			server.start();
		} else {
			passwd = StringUtils.strip(StringUtils.readFile(passwdFile)); 
		}
		
		try {
			String addr = StringUtils.strip(StringUtils.readFile(socketFile));
			if (addr != null && !addr.equals("")) {
				String ip = addr.split(":")[0];
				int port = Integer.parseInt(addr.split(":")[1]);
					client = new SJQClient(ip, port, passwd);
			}
		} catch (IOException e) {
			log.debug("Error connecting to server - trying again. "+ e.getMessage());
			try {
				String addr = StringUtils.strip(StringUtils.readFile(socketFile));
				if (addr != null && !addr.equals("")) {
					String ip = addr.split(":")[0];
					int port = Integer.parseInt(addr.split(":")[1]);
							client = new SJQClient(ip, port, passwd);
				}
			} catch (IOException e1) {
				log.debug("Error connecting to server - again - Failing... "+ e.getMessage());
				throw e1;
			}
		}
	}
	
	@Override
	public boolean submit(JobDef jobdef) throws RunnerException {
		if (dryrun) {
			dryRunJobCount++;
			String jobId = "dryrun." + dryRunJobCount;
			jobdef.setJobId(jobId);
			submittedJobs.add(jobId);
			return true;
		}

		if (client == null) {
			try {
				connect();
			} catch (IOException | CommandArgumentException | SJQServerException e) {
				e.printStackTrace();
				throw new RunnerException(e);
			}
		}
		
		try {
			String jobId = client.submitJob(jobdef.getName(), "#!"+ShellScriptRunner.defaultShell+"\n"+jobdef.getBody(), 
					((int)jobdef.getSettingInt("job.procs", 1)), jobdef.getSetting("job.mem"), 
					jobdef.getSetting("job.stderr"), jobdef.getSetting("job.stdout"), 
					jobdef.getSetting("job.wd", new File(".").getCanonicalPath()), null, 
					IterUtils.map(jobdef.getDependencies(), new MapFunc<JobDependency, String>() {
						@Override
						public String map(JobDependency dep) {
							return dep.getJobId();
						}}), false);
			jobdef.setJobId(jobId);
			submittedJobs.add(jobId);
			log.info("SUBMIT JOB: "+jobId);
			for (String line: jobdef.getBody().split("\n")) {
				log.debug(jobId + " " + line);
			}
			return jobId != null;
		} catch (ClientException | AuthException | IOException e) {
			e.printStackTrace();
			throw new RunnerException(e);
		}
	}

	@Override
	public boolean isJobIdValid(String jobId) throws RunnerException {
		if (client == null) {
			try {
				connect();
			} catch (IOException | CommandArgumentException | SJQServerException e) {
				e.printStackTrace();
				throw new RunnerException(e);
			}
		}
		try {
			String[] status = client.getStatus(jobId).split(" ");
			return (status[1].equals("H") || status[1].equals("Q")  || status[1].equals("R")); 
		} catch (ClientException | AuthException e) {
			return false;
		}
	}

	@Override
	public void runnerDone() throws RunnerException {
		log.info("submitted jobs: "+StringUtils.join(",", submittedJobs));
		if (submittedJobs.size() > 0) {
			System.out.println(StringUtils.join("\n", submittedJobs));
		}
		try {
			if (client != null) {
				client.close();
			}
		} catch (ClientException e) {
		}
		if (server != null) {
			server.join();
		}
	}

	@Override
	protected void setConfig(String k, String val) {
		if (k.equals("cgpipe.runner.sjq.maxmem")) {
			this.maxMem = val;
		} else if (k.equals("cgpipe.runner.sjq.tempdir")) {
			this.tempDir = val;
		} else if (k.equals("cgpipe.runner.sjq.sockfile")) {
			this.socketFile = new File(val);
		} else if (k.equals("cgpipe.runner.sjq.passwdfile")) {
			this.passwdFile = new File(val);
		} else if (k.equals("cgpipe.runner.sjq.joblog")) {
			this.jobLogFile = new File(val);
		} else if (k.equals("cgpipe.runner.sjq.maxprocs")) {
			this.maxProcs = Integer.parseInt(val);
		} else if (k.equals("cgpipe.runner.sjq.port")) {
			this.port = Integer.parseInt(val);
		}
	}

	@Override
	public void abort() {
		if (client != null) {
			for (String jobId: submittedJobs) {
				try {
					client.killJob(jobId);
				} catch (ClientException | AuthException e) {
				}
			}
		}
	}
}
