package io.compgen.cgpipe.runner.joblog;

import java.util.ArrayList;
import java.util.List;

import io.compgen.common.Pair;

public class JobLogRecord {

	private final String jobId;
	private String name = null;
	private Integer returnCode = null;
	private Long startTime = null;
	private Long endTime = null;
	private Long submitTime = null;
	private String user = null;
	private List<String> outputs = null;
	private List<String> deps = null;
	private List<String> inputs = null;
	private List<String> srcLines = null;
	private List<Pair<String, String>> settings = null;
	
	
	public JobLogRecord(String jobId) {
		this.jobId = jobId;
	}

	public List<String> getOutputs() {
		return outputs;
	}

	public String getJobId() {
		return jobId;
	}

	public String getName() {
		return name;
	}

	public Integer getReturnCode() {
		return returnCode;
	}

	public void setReturnCode(Integer returnCode) {
		this.returnCode = returnCode;
	}

	public Long getStartTime() {
		return startTime;
	}

	public void setStartTime(Long startTime) {
		this.startTime = startTime;
	}

	public Long getEndTime() {
		return endTime;
	}

	public void setEndTime(Long endTime) {
		this.endTime = endTime;
	}

	public Long getSubmitTime() {
		return submitTime;
	}

	public void setSubmitTime(Long submitTime) {
		this.submitTime = submitTime;
	}

	public List<String> getDeps() {
		return deps;
	}

	public List<String> getInputs() {
		return inputs;
	}

	public List<String> getSrcLines() {
		return srcLines;
	}

	public List<Pair<String, String>> getSettings() {
		return settings;
	}

	public String getUser() {
		return user;
	}


	public void setName(String name) {
		this.name = name;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public void addDep(String depId) {
		if (deps == null) {
			deps = new ArrayList<String>();
		}
		deps.add(depId);
	}

	public void addOutput(String out) {
		if (outputs == null) {
			outputs = new ArrayList<String>();
		}
		outputs.add(out);
	}

	public void addInput(String inp) {
		if (inputs == null) {
			inputs = new ArrayList<String>();
		}
		inputs.add(inp);
	}

	public void addSrcLine(String s) {
		if (srcLines == null) {
			srcLines = new ArrayList<String>();
		}
		srcLines.add(s);
	}

	public void addSetting(String k, String s) {
		if (settings == null) {
			settings = new ArrayList<Pair<String,String>>();
		}
		settings.add(new Pair<String, String>(k, s));
	}

}
