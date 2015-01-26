package org.ngsutils.mvpipe.parser.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ngsutils.mvpipe.exceptions.SyntaxException;
import org.ngsutils.mvpipe.parser.Eval;
import org.ngsutils.mvpipe.parser.Tokens;
import org.ngsutils.mvpipe.parser.variable.VarValue;
import org.ngsutils.mvpipe.runner.JobDefinition;
import org.ngsutils.mvpipe.support.StringUtils;

public class BuildTarget {
	public class NumberedLine {
		public final String line;
		public final int linenum;
		
		public NumberedLine(String line, int linenum) {
			this.line = line;
			this.linenum = linenum;
		}
	}
	
	public class Output {
		public final String rawName;
		public final Pattern regex;
		public Output(String name) {
			this.rawName = name;
			this.regex = Pattern.compile("\\Q"+name.replace("%", "\\E(.*)\\Q")+"\\E");
		}
		
		public String toString() {
			return rawName;
		}
	}

	private int indentLevel=-1;

	private Log log = LogFactory.getLog(getClass());

	private final List<Output> outputs;
	private final List<String> inputs;
	private final Map<String, VarValue> capturedContext;
	private final String filename;

	private final List<NumberedLine> lines = new ArrayList<NumberedLine>();

	public BuildTarget(List<String> outputs, List<String> inputs, ExecContext cxt, String filename) {
		// TODO: Eval.parseString outputs / inputs
		
		// this is a target context, capture the parent values
		if (inputs != null && inputs.size() > 0) {
			List<String> tmp = new ArrayList<String>();
			for (String input: inputs) {
				tmp.add(Eval.evalString(input, cxt));
			}
			this.inputs = Collections.unmodifiableList(tmp);
		} else {
			this.inputs = null;
		}
		this.capturedContext = Collections.unmodifiableMap(cxt.cloneValues());
		this.filename = filename;

		List<Output> tmp = new ArrayList<Output>();
		for (String out:outputs) {
			tmp.add(new Output(Eval.evalString(out, cxt)));
		}
		this.outputs = Collections.unmodifiableList(tmp);


		log.debug("inputs: " + StringUtils.join(",", this.inputs));
		log.debug("outputs: " + StringUtils.join(",", this.outputs));
		
		for (String k: capturedContext.keySet()) {
			log.debug("context: "+k+" => "+capturedContext.get(k));
		}
		
	}
	
	public void addLine(String line, int linenum) {
		if (indentLevel == -1) {
			indentLevel = StringUtils.calcIndentLevel(line);
		}
		this.lines.add(new NumberedLine(StringUtils.removeIndent(line, indentLevel), linenum));
	}

	/**
	 * Can this build-target create the given output file?
	 * If so, what should be the wildcard values used to make the output names?
	 * @param outputName
	 * @return
	 * @throws SyntaxException 
	 */
	public JobDefinition matches(String outputName) throws SyntaxException {
		/* 
		 * TODO: This needs to be a regex match to replace wildcards in inputs (%.gzfoo)
		 */
		
		log.trace("Trying to match: "+outputName);
		boolean matched = false;
		String wildcard = "";
		
		for (Output out:outputs) {
			log.trace("testing regex: "+out.regex);
			Matcher m = out.regex.matcher(outputName);
			if (m.matches()) {
				matched = true;
				if (m.groupCount()>0) {
					wildcard = m.group(1);
					log.debug("Target match: "+m.group(0)+" ("+m.group(1)+")");
				} else {
					log.debug("Target match: "+m.group(0));
				}
				break;
			}
		}
		
		if (matched) {
			List<String> matchedOutputs = new ArrayList<String>();
			List<String> matchedInputs = new ArrayList<String>();

			for (Output out:outputs) {
				matchedOutputs.add(Eval.evalStringWildcard(out.rawName, wildcard));
			}
			if (inputs!=null) {
				for (String input:inputs) {
					matchedInputs.add(Eval.evalStringWildcard(input, wildcard));
				}
			}
			
			ExecContext cxt = new ExecContext(capturedContext);
			// TODO: add wildcards and outputs to context
			
			List<String> src = new ArrayList<String>();
			for (NumberedLine nl:lines) {
				String stripped = StringUtils.strip(nl.line);
				if (stripped.length()>2) {
					if (stripped.startsWith("#$")) {
						Tokens tokens = new Tokens(filename, nl.linenum, stripped.substring(2));
						cxt.addTokenizedLine(tokens);
						continue;
					}
				}
				src.add(Eval.evalString(nl.line, cxt, matchedOutputs, matchedInputs));
			}
			
			Map<String, String> settings = new HashMap<String, String>();
			Map<String, VarValue> cxtvals = cxt.cloneValues("job.");
			for (String k: cxtvals.keySet()) {
				settings.put(k, cxtvals.get(k).toString());
			}
			
			JobDefinition jobdef = new JobDefinition(settings, matchedOutputs, matchedInputs, StringUtils.join("\n", src));
			return jobdef;	
		}
		return null;
	}
	
	public List<Output> getOutputs() {
		return outputs;
	}
	
	public int getIndentLevel() {
		return indentLevel;
	}

}
