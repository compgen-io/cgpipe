package org.ngsutils.mvpipe.runner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ngsutils.mvpipe.support.StringUtils;

public class JobDefinition implements JobDependency {
	private Log log = LogFactory.getLog(getClass());
	
	private String jobId = null;
	private String name = null;

	final private String src;
	final private Map<String, String> settings;
	final private List<String> outputFilenames;
	final private List<String> requiredInputs;
	final private List<String> extraTargets;
	
	private List<JobDependency> dependencies = new ArrayList<JobDependency>();
	
	public JobDefinition(Map<String, String>settings, List<String> outputFilenames, List<String> inputFilenames, String src) {
		this.settings = Collections.unmodifiableMap(settings);
		this.outputFilenames = Collections.unmodifiableList(outputFilenames);
		this.requiredInputs = Collections.unmodifiableList(inputFilenames);
		
		for (String k: settings.keySet()) {
			log.trace("job setting: "+k +" => " + settings.get(k));
		}
		 
		List<String> tmp = new ArrayList<String>();
		if (settings.containsKey("job.extras")) {
			for (String extra: settings.get("job.extras").split(",")) {
				log.trace("extra job: "+extra);
				tmp.add(StringUtils.strip(extra));
			}
		}
		extraTargets = Collections.unmodifiableList(tmp);
		this.src = src;		
	}
	
	public List<String> getRequiredInputs() {
		return requiredInputs;
	}

	public List<String> getOutputFilenames() {
		return outputFilenames;
	}

	public List<String> getExtraTargets() {
		return extraTargets;
	}

	public boolean hasSetting(String k) {
		return settings.containsKey(k);
	}

	public String getSetting(String k) {
		return getSetting(k, null);
	}
	
	public String getSetting(String k, String defval) {
		if (settings.containsKey(k)) {
			return settings.get(k);
		}
		return defval;		
	}
	
	public int getSettingInt(String k) {
		return getSettingInt(k, null);
	}
	
	public int getSettingInt(String k, Integer defval) {
		if (settings.containsKey(k)) {
			return Integer.parseInt(settings.get(k));
		}
		return defval;		
	}

	public boolean getSettingBool(String k) {
		return getSettingBool(k, null);
	}
	
	public boolean getSettingBool(String k, Boolean defval) {
		if (settings.containsKey(k)) {
			return settings.get(k).toLowerCase().equals("true");
		}
		return defval;		
	}

	public String getJobId() {
		return jobId;
	}
	
	public void setJobId(String jobId) {
		this.jobId = jobId;
	}
	
	public String getName() {
		String n = name;
		
		if (n == null && hasSetting("job.name")) {
			n = getSetting("job.name");
		}
		if (n != null && n.length() > 0) {
			if ("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".contains(""+n.charAt(0))) {
				return n;
			}
			return "mvp_"+n;
		}
		return "mvpjob";
	}
	
	public String getSrc() {
		return src;
	}

	public void addDependency(JobDependency dep) {
		if (!dependencies.contains(dep)) {
			dependencies.add(dep);
		}
	}

	public List<JobDependency> getDependencies() {
		return dependencies;
	}

	public Set<String> getSettings() {
		return settings.keySet();
	}
}
