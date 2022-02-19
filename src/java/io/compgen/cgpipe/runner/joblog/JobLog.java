package io.compgen.cgpipe.runner.joblog;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import io.compgen.common.Pair;

public class JobLog {
	static Map<String, JobLog> instances = new HashMap<String, JobLog>();
	static protected Log log = LogFactory.getLog(JobLog.class);

//	private File lockFile = null;

	private final String filename;
//	private final String lockSecret = generateRandomString();
	
	// protected List<String> jobIds = new ArrayList<String>();
	// protected Map<String, JobLogRecord> records = new HashMap<String,JobLogRecord>();
	protected Map<String, String> outputs = new HashMap<String,String>(); // output, jobid
	
	protected JobLog(String filename) throws IOException {
		this.filename = filename;
		File jobfile = new File(filename);
		
		if (jobfile.exists()) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
			String line;
			
			while ((line = reader.readLine()) != null) {
				String[] cols = line.split("\t", 3);

				String jobid = cols[0];
				String key = cols[1];
				String arg1 = cols[2];

				if (key.equals("OUTPUT")) {
					log.debug("Existing output/job: " + arg1 + " => " + jobid);
					outputs.put(arg1, jobid);
				}

				//////////////////
				// THE REST OF THIS IS NOT NEEDED... when we read in the log, it is ONLY to look for the outputs/jobids. 
				//////////////////
				
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
				// 	case "PIPELINE":
				// 		rec.setPipeline(arg1);
				// 		break;
				// 	case "WORKINGDIR":
				// 		rec.setWorkingDirectory(arg1);
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
			log.debug("Looking for an existing job for file: "+output+", found job-id: "+outputs.get(output));
			return outputs.get(output);
		}
		log.debug("Looking for an existing job file: "+output+", not found.");
		return null;
	}

	public Map<String, String> getOutputJobIds() {
		return Collections.unmodifiableMap(outputs);
	}

	public void close() {
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

		ps.println(rec.getJobId()+"\tPIPELINE\t"+rec.getPipeline());
		ps.println(rec.getJobId()+"\tWORKINGDIR\t"+rec.getWorkingDirectory());

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
				if (dep!=null) {
					ps.println(rec.getJobId()+"\tDEP\t"+dep);
				}
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

	public static void vaccuum(String filename) throws IOException {
		File jobfile = new File(filename);
		
		if (jobfile.exists()) {
			// first pass, find valid jobs
			Map<String, String> outputJobID = new HashMap<String, String>();
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
			String line;
			
			while ((line = reader.readLine()) != null) {
				String[] cols = line.split("\t", 3);

				String jobid = cols[0];
				String key = cols[1];
				String arg1 = cols[2];

				if (key.equals("OUTPUT")) {
					log.debug("Existing output/job: " + arg1 + " => " + jobid);
					outputJobID.put(arg1, jobid);
				}
			}
			reader.close();

			Set<String> validJobIDs = new HashSet<String>();
			for (String k: outputJobID.keySet()) {
				validJobIDs.add(outputJobID.get(k));
			}
			
			// second pass, write valid jobs to tmp file
		    File tmp = File.createTempFile("temp", null);
		    //System.err.println("Temp file: " + tmp.getAbsolutePath());
			BufferedReader reader2 = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmp)));
			
			
			while ((line = reader2.readLine()) != null) {
				String[] cols = line.split("\t", 3);

				String jobid = cols[0];
				if (validJobIDs.contains(jobid)) {
					writer.write(line+"\n");
				}
			}

			reader2.close();
			writer.close();

			// last step -- move the tmp file to the original filename
			Files.move(tmp.toPath(), jobfile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			//tmp.renameTo(jobfile);
		
		} else if (jobfile.getParentFile() != null && !jobfile.getParentFile().exists()) {
			jobfile.getParentFile().mkdirs();
			jobfile.createNewFile();
		}
	}

}
