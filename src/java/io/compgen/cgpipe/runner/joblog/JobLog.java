package io.compgen.cgpipe.runner.joblog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.compgen.common.Pair;

public class JobLog {
	static Map<String, JobLog> instances = new HashMap<String, JobLog>();
	static protected Log log = LogFactory.getLog(JobLog.class);

	private File lockFile = null;

	private final String filename;
	private final String lockSecret = generateRandomString();
	
	// protected List<String> jobIds = new ArrayList<String>();
	// protected Map<String, JobLogRecord> records = new HashMap<String,JobLogRecord>();
	protected Map<String, String> outputs = new HashMap<String,String>(); // output, jobid
	
	protected JobLog(String filename) throws IOException {
		this.filename = filename;
		File jobfile = new File(filename);
		
		acquireLock();
		
		if (jobfile.exists()) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
			String line;
			
			while ((line = reader.readLine()) != null) {
				String[] cols = line.split("\t", 3);

				String jobid = cols[0];
				String key = cols[1];
				String arg1 = cols[2];

				if (key.equals("OUTPUT")) {
					outputs.put(arg1, jobid);
				}

				// String arg2 = null;
				// if (key.equals("SETTING")) {
				// 	cols = line.split("\t", 4);
				// 	arg2 = cols[3];

				// }

				// if (!records.containsKey(jobid)) {
				// 	records.put(jobid, new JobLogRecord(jobid));
				// 	jobIds.add(jobid);
				// }

				// JobLogRecord rec = records.get(jobid);
				// switch(key) {
				// 	case "NAME":
				// 		rec.setName(arg1);
				// 		break;
				// 	case "RETCODE":
				// 		rec.setReturnCode(Integer.parseInt(arg1));
				// 		break;
				// 	case "SUBMIT":
				// 		rec.setSubmitTime(Long.parseLong(arg1));
				// 		break;
				// 	case "START":
				// 		rec.setStartTime(Long.parseLong(arg1));
				// 		break;
				// 	case "END":
				// 		rec.setEndTime(Long.parseLong(arg1));
				// 		break;
				// 	case "SETTING":
				// 		rec.addSetting(arg1, arg2);
				// 		break;
				// 	case "OUTPUT":
				// 		outputs.put(arg1, jobid);
				// 		rec.addOutput(arg1);
				// 		break;
				// 	case "INPUT":
				// 		rec.addInput(arg1);
				// 		break;
				// 	case "DEP":
				// 		rec.addDep(arg1);
				// 		break;
				// 	case "SRC":
				// 		rec.addSrcLine(arg1);
				// 		break;
				// 	default:
				// 		break;
				// }

			}
			reader.close();
		} else if (jobfile.getParentFile() != null && !jobfile.getParentFile().exists()) {
			jobfile.getParentFile().mkdirs();
			jobfile.createNewFile();
		}
		
		
	}

	protected void releaseLock() {
		if (lockFile != null) {
			File child = new File(lockFile, "lock");
			if (child.exists()) {
				try {
					BufferedReader reader = new BufferedReader(new FileReader(child));
					String s = reader.readLine();
					reader.close();
					if (!lockSecret.equals(s)) {
						// we don't own the lock, don't release
						return;
					}
				} catch (IOException e) {
					return;
				}

				child.delete();
			}
			lockFile.delete();
			log.debug("job-log lock released");
		}
	}

	protected void acquireLock() throws IOException {
		acquireLock(30000);
	}

	protected void acquireLock(long wait_ms) throws IOException  {
		log.debug("Trying to get job-log lock: " + filename);
		if (lockFile != null) {
			log.trace("Lock already acquired!");
			return;
		}
		
		long ms = System.currentTimeMillis();
		long end = ms + wait_ms;
		
		long wait = 10;
		
		boolean first = true;
		while (lockFile == null && System.currentTimeMillis() < end) {
			if (!first) {
				try {
					log.trace("waiting to try to establish lock");
					Thread.sleep(wait);
				} catch (InterruptedException e) {
				}
				if (wait < 100) {
					wait = wait * 2;
				}
			}
			first = false;
			
			boolean good = false;
			File dir = new File(filename+".lock");
			File child = new File(dir, "lock");

			if (!dir.exists() ) {
				dir.mkdirs();
				try {
					if (!child.exists()) {
						child.createNewFile();
						
						PrintStream ps = new PrintStream(new FileOutputStream(child));
						ps.println(lockSecret);
						ps.flush();
						ps.close();
	
						Thread.sleep(100);
	
						BufferedReader reader = new BufferedReader(new FileReader(child));
						String s = reader.readLine();
						reader.close();
	
						if (lockSecret.equals(s)) {
							// we own the lock!
							good = true;
						} else {
							log.debug("tried to create the lock, but we got beat... waiting");
						}
					}
				} catch (IOException e) {
					good = false;
				} catch (InterruptedException e) {
					good = false;
				}
			}
			
			if (good) {
				log.debug("job-log lock acquired");
				lockFile = dir;
				Runtime.getRuntime().addShutdownHook(new Thread() {
					public void run() { 
						releaseLock();
					}
				});
				dir.deleteOnExit();
				child.deleteOnExit();
			}
		}
		
		if (lockFile == null) {
			log.error("Could not get a lock on job-log: "+filename);
			System.exit(2);
		}
	}

	public static JobLog open(String filename) throws IOException {
		if (instances.containsKey(filename)) {
			return instances.get(filename);
		} else {
			instances.put(filename, new JobLog(filename));
			return instances.get(filename);
		}		
	}

	public String getJobIdForOutput(String output) {
		if (outputs.containsKey(output)) {
			return outputs.get(output);
		}
		return null;
	}

	public Map<String, String> getOutputJobIds() {
		return Collections.unmodifiableMap(outputs);
	}

	public void close() {
		releaseLock();
		instances.remove(filename);
	}

	public void writeRecord(JobLogRecord rec) {
		PrintStream ps = null;
		try {
			ps = new PrintStream(new FileOutputStream(filename, true));
		} catch (FileNotFoundException e) {
			log.error("Missing job log??? (this should have been created) -- " + filename);
			return;
		}

		if (rec.getName()!=null) {
			ps.println(rec.getJobId()+"\tNAME\t"+rec.getName());
		}
		if (rec.getUser()!=null) {
			ps.println(rec.getJobId()+"\tUSER\t"+rec.getUser());
		}
		if (rec.getReturnCode()!=null) {
			ps.println(rec.getJobId()+"\tRETCODE\t"+rec.getReturnCode());
		}
		if (rec.getSubmitTime()!=null) {
			ps.println(rec.getJobId()+"\tSUBMIT\t"+rec.getSubmitTime());
		}
		if (rec.getStartTime()!=null) {
			ps.println(rec.getJobId()+"\tSTART\t"+rec.getStartTime());
		}
		if (rec.getEndTime()!=null) {
			ps.println(rec.getJobId()+"\tEND\t"+rec.getEndTime());
		}

		if (rec.getDeps() != null) {
			for (String dep: rec.getDeps()) {
				ps.println(rec.getJobId()+"\tDEP\t"+dep);
			}
		}
		if (rec.getOutputs() != null) {
			for (String out: rec.getOutputs()) {
				ps.println(rec.getJobId()+"\tOUTPUT\t"+out);
			}
		}
		if (rec.getInputs() != null) {
			for (String inp: rec.getInputs()) {
				ps.println(rec.getJobId()+"\tINPUT\t"+inp);
			}
		}
		if (rec.getSrcLines() != null) {
			for (String s: rec.getSrcLines()) {
				ps.println(rec.getJobId()+"\tSRC\t"+s);
			}
		}
		if (rec.getSettings() != null) {
			for (Pair<String, String> p: rec.getSettings()) {
				ps.println(rec.getJobId()+"\tSETTING\t"+p.one+"\t"+p.two);
			}
		}
		ps.flush();
		ps.close();
		
	}

	public void writeStartTime(String jobId) {

		PrintStream ps = null;
		try {
			ps = new PrintStream(new FileOutputStream(filename, true));
		} catch (FileNotFoundException e) {
			System.err.println("Missing job log??? (this should have been created) -- " + filename);
			return;
		}

		ps.println(jobId+"\tSTART\t"+System.currentTimeMillis());
		ps.flush();
		ps.close();
		
	}

	public void writeEndTime(String jobId, int retcode) {

		PrintStream ps = null;
		try {
			ps = new PrintStream(new FileOutputStream(filename, true));
		} catch (FileNotFoundException e) {
			System.err.println("Missing job log??? (this should have been created) -- " + filename);
			return;
		}

		ps.println(jobId+"\tEND\t"+System.currentTimeMillis());
		ps.println(jobId+"\tRETCODE\t"+retcode);
		ps.flush();
		ps.close();
		
	}

	public static final String UPPER="ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	public static final String LOWER="abcdefghijklmnopqrstuvwxyz";
	public static final String NUM="0123456789";
	
	public static final String generateRandomString() {
		return generateRandomString(24, UPPER+LOWER+NUM);
	}
	
	public static final String generateRandomString(int length, String pool) {
		
	    SecureRandom rand = new SecureRandom();
	
	    String s = "";
	    
	    while (s.length() < length) {
	    	int next = rand.nextInt(pool.length());
	    	s += pool.charAt(next);
	    }
	    
		return s;
	}

}
