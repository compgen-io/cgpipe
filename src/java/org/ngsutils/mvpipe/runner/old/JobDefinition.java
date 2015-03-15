//package org.ngsutils.mvpipe.runner.old;
//
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//
//import org.ngsutils.mvpipe.exceptions.SyntaxException;
//import org.ngsutils.mvpipe.parser.old.Eval;
//import org.ngsutils.mvpipe.parser.old.Tokens;
//import org.ngsutils.mvpipe.parser.old.context.BuildTarget_Old.NumberedLine;
//import org.ngsutils.mvpipe.parser.old.context.ExecContext;
//import org.ngsutils.mvpipe.parser.variable.VarValue;
//import org.ngsutils.mvpipe.runner.JobDependency;
//import org.ngsutils.mvpipe.support.StringUtils;
//
//public class JobDefinition implements JobDependency {
////	private Log log = LogFactory.getLog(getClass());
//	
//	private String jobId = null;
//	private String name = null;
//
//	final private Map<String, VarValue> capturedContext;
//	final private List<NumberedLine> lines;
//	final private String wildcard;
//	final private List<String> outputFilenames;
//	final private List<String> requiredInputs;
//
//	private String src = null;
//	private Map<String, String> settings = null;
////	private List<String> extraTargets = null;
//	
//	private List<JobDependency> dependencies = new ArrayList<JobDependency>();
//	
//	public JobDefinition(Map<String, VarValue> capturedContext, List<String> outputFilenames, List<String> inputFilenames, String wildcard, List<NumberedLine> lines) {
////		this.settings = Collections.unmodifiableMap(settings);
//		this.outputFilenames = Collections.unmodifiableList(outputFilenames);
//		this.requiredInputs = Collections.unmodifiableList(inputFilenames);
//		
////		for (String k: settings.keySet()) {
////			log.trace("job setting: "+k +" => " + settings.get(k));
////		}
////		 
////		this.src = src;
//		
//		this.capturedContext = capturedContext;
//		this.wildcard = wildcard;
//		this.lines = lines;
//	}
//
//	public List<String> getRequiredInputs() {
//		return requiredInputs;
//	}
//
//	public List<String> getOutputFilenames() {
//		return outputFilenames;
//	}
////
////	public List<String> getExtraTargets() {
////		return extraTargets;
////	}
//
//	public boolean hasSetting(String k) {
//		return settings.containsKey(k);
//	}
//
//	public String getSetting(String k) {
//		return getSetting(k, null);
//	}
//	
//	public String getSetting(String k, String defval) {
//		if (settings.containsKey(k)) {
//			return settings.get(k);
//		}
//		return defval;		
//	}
//	
//	public int getSettingInt(String k) {
//		return getSettingInt(k, null);
//	}
//	
//	public int getSettingInt(String k, Integer defval) {
//		if (settings.containsKey(k)) {
//			return Integer.parseInt(settings.get(k));
//		}
//		return defval;		
//	}
//
//	public boolean getSettingBool(String k) {
//		return getSettingBool(k, null);
//	}
//	
//	public boolean getSettingBool(String k, Boolean defval) {
//		if (settings.containsKey(k)) {
//			return settings.get(k).toLowerCase().equals("true");
//		}
//		return defval;		
//	}
//
//	public String getJobId() {
//		return jobId;
//	}
//	
//	public void setJobId(String jobId) {
//		this.jobId = jobId;
//	}
//	
//	public String getName() {
//		String n = name;
//		
//		if (n == null && hasSetting("job.name")) {
//			n = getSetting("job.name");
//		}
//		if (n != null && n.length() > 0) {
//			if ("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".contains(""+n.charAt(0))) {
//				return n;
//			}
//			return "mvp_"+n;
//		}
//		return "mvpjob";
//	}
//	
//	public String getSrc() throws SyntaxException {
//		if (src == null) {
//			eval();
//		}
//		return src;
//	}
//
//	private void eval() throws SyntaxException {
//		if (src == null) {
//			ExecContext jobcxt = new ExecContext(capturedContext, outputFilenames, requiredInputs, wildcard);
//			ExecContext cxt = jobcxt;
//			List<String> srcLines = new ArrayList<String>();
//			for (NumberedLine nl:lines) {
//				String stripped = StringUtils.strip(nl.line);
//				if (stripped.length()>2) {
//					if (stripped.startsWith("#$")) {
//						Tokens tokens = new Tokens(nl.filename, nl.linenum, stripped.substring(2));
//						cxt = cxt.addTokenizedLine(tokens);
//						continue;
//					}
//				}
//				if (cxt.isActive()) {
//					srcLines.add(Eval.evalString(nl.line, cxt));// matchedOutputs, matchedInputs));
//				}
//			}
//
//			this.src = StringUtils.join("\n", srcLines);
//			
//			settings = new HashMap<String, String>();
//			Map<String, VarValue> cxtvals = jobcxt.cloneValues("job.");
//			for (String k: cxtvals.keySet()) {
//				settings.put(k, cxtvals.get(k).toString());
//			}
//		}
//	}	
//	
//	public void addDependency(JobDependency dep) {
//		if (!dependencies.contains(dep)) {
//			dependencies.add(dep);
//		}
//	}
//
//	public List<JobDependency> getDependencies() {
//		return dependencies;
//	}
//
//	public Set<String> getSettings() {
//		return settings.keySet();
//	}
//}
