package org.ngsutils.mvpipe.parser.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ngsutils.mvpipe.exceptions.EvalException;
import org.ngsutils.mvpipe.exceptions.SyntaxException;
import org.ngsutils.mvpipe.parser.Eval;
import org.ngsutils.mvpipe.parser.variable.VarValue;
import org.ngsutils.mvpipe.runner.JobDefinition;
import org.ngsutils.mvpipe.support.StringUtils;

public class BuildTarget {
	public class NumberedLine {
		public final String filename;
		public final String line;
		public final int linenum;
		
		public NumberedLine(String line, String filename, int linenum) {
			this.line = line;
			this.filename = filename;
			this.linenum = linenum;
		}
	}

	private final int indentLevel;

	private Log log = LogFactory.getLog(getClass());

	private final List<String> outputs;
	private final List<String> inputs;
	private final Map<String, VarValue> capturedContext;

	private final List<NumberedLine> lines = new ArrayList<NumberedLine>();
	private List<NumberedLine> preLines = null;
	private List<NumberedLine> postLines = null;

	public BuildTarget(List<String> outputs, List<String> inputs, ExecContext cxt, int indentLevel) throws EvalException {
		// this is a target context, capture the parent values
		if (inputs != null && inputs.size() > 0) {
			this.inputs = Collections.unmodifiableList(new ArrayList<String>(inputs));
		} else {
			this.inputs = null;
		}
		this.capturedContext = Collections.unmodifiableMap(cxt.cloneValues());
		this.indentLevel = indentLevel;
		
		this.outputs = Collections.unmodifiableList(new ArrayList<String>(outputs));

		log.debug("inputs: " + StringUtils.join(",", this.inputs));
		log.debug("outputs: " + StringUtils.join(",", this.outputs));
		
		for (String k: capturedContext.keySet()) {
			log.debug("context: "+k+" => "+capturedContext.get(k));
		}
		
	}
	
	public void addLine(String line, String filename, int linenum) {
		if (StringUtils.strip(line).length() > 0) { 
			log.debug("Adding line: "+line);
			lines.add(new NumberedLine(StringUtils.removeIndent(line, indentLevel), filename, linenum));
		}
	}

	/**
	 * Can this build-target create the given output file?
	 * If so, what should be the wildcard values used to make the output names?
	 * @param outputName
	 * @param cxt 
	 * @return
	 * @throws SyntaxException 
	 */
	public JobDefinition matches(String outputName, ExecContext rootContext) throws SyntaxException {
		/* 
		 * TODO: This needs to be a regex match to replace wildcards in inputs (%.gzfoo)
		 */
		
		log.trace("Trying to match: "+outputName);
		boolean matched = false;
		String wildcard = "";
		
		for (String out:outputs) {
			out = Eval.evalString(out,  rootContext);
			Pattern regex = Pattern.compile("\\Q"+out.replace("%", "\\E(.*)\\Q")+"\\E");

			log.trace("testing regex: "+regex);
			Matcher m = regex.matcher(outputName);
			if (m.matches()) {
				matched = true;
				if (m.groupCount()>0) {
					wildcard = m.group(1);
					log.debug("Target match: "+m.group(0)+" ("+m.group(1)+")");
				} else {
					log.debug("Target match: "+m.group(0));
				}
				log.trace("Matched wildcard: "+wildcard);
				break;
			}
		}

		if (matched) {
			List<String> matchedOutputs = new ArrayList<String>();
			for (String out:outputs) {
				out = Eval.evalString(out,  rootContext);
				Pattern wildPattern = Pattern.compile("^(.*?)%(.*?)$");

				Matcher m = wildPattern.matcher(out);
				if (m.matches()) {
					matchedOutputs.add(m.group(1)+wildcard+m.group(2));
				} else {
					matchedOutputs.add(out);
				}
			}


			List<String> matchedInputs = new ArrayList<String>();

			if (inputs!=null) {
				for (String input:inputs) {
					matchedInputs.add(Eval.evalString(input, rootContext, wildcard));
				}
			}

			JobDefinition jobdef = new JobDefinition(capturedContext, matchedOutputs, matchedInputs, wildcard, getLines());
			return jobdef;	
		}
		return null;
	}
	
	public List<String> getOutputs() {
		return outputs;
	}
	
	public int getIndentLevel() {
		return indentLevel;
	}

	public List<NumberedLine> getLines() {
		List<NumberedLine> src = new ArrayList<NumberedLine>();
		if (lines.size() > 0) {
			if (preLines != null) {
				src.addAll(preLines);
			}
			src.addAll(lines);
			if (postLines!=null) {
				src.addAll(postLines);
			}
		}
		return src;
	}

	public void addPreLines(List<NumberedLine> lines) {
		preLines = new ArrayList<NumberedLine>(lines);
	}

	public void addPostLines(List<NumberedLine> lines) {
		postLines = new ArrayList<NumberedLine>(lines);
	}

}
