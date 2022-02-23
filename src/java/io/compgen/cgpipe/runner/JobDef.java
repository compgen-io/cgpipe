package io.compgen.cgpipe.runner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.compgen.cgpipe.parser.variable.VarInt;
import io.compgen.cgpipe.parser.variable.VarValue;
import io.compgen.common.StringUtils;

public class JobDef implements JobDependency {
	private final String body;
	private final Map<String, VarValue> settings;
	private final List<String> outputs;
	private final List<String> inputs;
	private String jobId = null;
	private String name;
	private List<JobDependency> depends = new ArrayList<JobDependency>();
	
	public JobDef(String body, Map<String, VarValue> settings, List<String> outputs, List<String> inputs) {
		this.body = body;
		this.settings = settings;
		this.outputs = Collections.unmodifiableList(new ArrayList<String>(outputs));

		// inputs could be null...
		if (inputs == null) {
			this.inputs = Collections.unmodifiableList(new ArrayList<String>(){
				private static final long serialVersionUID = -1597169196436247445L;});
		} else {
			this.inputs = Collections.unmodifiableList(new ArrayList<String>(inputs));
		}
	}
	
	public JobDef(String body, Map<String, VarValue> settings, List<String> outputs) {
		this(body, settings, outputs, new ArrayList<String>());
	}
	
	public JobDef(String body, Map<String, VarValue> settings) {
		this(body, settings, new ArrayList<String>(), new ArrayList<String>());
	}
	
	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

	public String toString() {
		return "<jobdef> " + StringUtils.join(" ", outputs);
	}

	public String getJobId() {
		return jobId;
	}
	
	public String getBody() {
		return body;
	}
	public List<String> getInputs() {
		return inputs;
	}

	public List<String> getOutputs() {
		return outputs;
	}

	public Map<String, VarValue> getSettingsMap() {
		return Collections.unmodifiableMap(settings);
	}
	
	public boolean hasSetting(String k) {
		return settings.containsKey(k);
	}

	public String getSetting(String k) {
		return getSetting(k, null);
	}
	
	public String getSetting(String k, String defval) {
		if (settings.containsKey(k)) {
			return settings.get(k).toString();
		}
		return defval;		
	}
	
	public long getSettingInt(String k) {
		return getSettingInt(k, null);
	}
	
	public long getSettingInt(String k, Integer defval) {
		if (settings.containsKey(k)) {
			VarValue v = settings.get(k);
			if (v.isNumber()) {
				if (v.getClass().equals(VarInt.class)) {
					return (Long) ((VarInt) v).getObject();
				}
			}
		}
		return defval;		
	}

	public boolean getSettingBool(String k) {
		return getSettingBool(k, false);
	}
	
	public boolean getSettingBool(String k, boolean defval) {
		if (settings.containsKey(k)) {
			return settings.get(k).toBoolean();
		}
		return defval;
	}

	public Set<String> getSettings() {
		return settings.keySet();
	}
	
	public Iterable<String> getSettings(final String name) {
		return new Iterable<String>() {
			@Override
			public Iterator<String> iterator() {
				return new Iterator<String>() {
					Iterator<VarValue> it = settings.get(name).iterate().iterator();
					@Override
					public boolean hasNext() {
						return it.hasNext();
					}

					@Override
					public String next() {
						VarValue val = it.next();
						return val.toString();
					}

					@Override
					public void remove() {
						it.remove();
					}
				};
			}
		};
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		String n = this.name;
		
		if (n == null && hasSetting("job.name")) {
			n = getSetting("job.name");
		}
		if (n != null && n.length() > 0) {
			if ("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_".contains(""+n.charAt(0))) {
				return n;
			}
			return "cg_"+n;
		}
		return "cgjob_"+StringUtils.join("_", getOutputs());
	}

	public String getSafeName() {
		String ret = "";
		String name = getName();
		
		for (int i=0; i<name.length(); i++) {
			if ("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_".contains(""+name.charAt(i))) {
				ret += name.charAt(i);
			} else {
				if (ret.charAt(ret.length()-1) != '_') {
					ret += "_";
				}
			}
		}
		return ret;
	}
	
	public void addDependency(JobDependency dep) {
		this.depends.add(dep);
	}

	public void addDependencies(List<JobDependency> deps) {
		this.depends.addAll(deps);
	}

	public List<JobDependency> getDependencies() {
		return this.depends;
	}
//
//	@Override
//	public long getAge() {
//		// Return the minimum age of:
//		//     any dependencies (which likely aren't available yet),
//		//     any output files
//		//     -1
//		
//		long minAge = -2;
//		
//		for (JobDependency dep: depends) {
//			if (minAge == -2 || dep.getAge() < minAge) {
//				minAge = dep.getAge();
//			}
//		}
//		
//		for (String out: outputs) {
//			long fileAge = JobFile.find(out).getLastModifiedTime();
//			if (minAge == -2 || fileAge < minAge) {
//				minAge = fileAge;
//			}
//		}
//		
//		if (minAge == -2) {
//			return -1;
//		}
//		
//		return minAge;
//	}
//
//	@Override
//	public boolean isSkippable() {
//		for (JobDependency dep: depends) {
//			if (!dep.isSkippable()) {
//				return false;
//			}
//		}
//		
//		return false;
//	}
}
