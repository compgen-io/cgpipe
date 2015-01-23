package org.ngsutils.mvpipe.parser.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

	private final List<Output> outputs;
	private final List<String> inputs;
	private final Map<String, VarValue> capturedContext;
	private final String filename;

	private final List<NumberedLine> lines = new ArrayList<NumberedLine>();
	
	

	public BuildTarget(List<String> outputs, List<String> inputs, ExecContext cxt, String filename) {
		// this is a target context, capture the parent values
		if (inputs != null) {
			this.inputs = Collections.unmodifiableList(inputs);
		} else {
			this.inputs = null;
		}
		this.capturedContext = Collections.unmodifiableMap(cxt.cloneValues());
		this.filename = filename;

		List<Output> tmp = new ArrayList<Output>();
		for (String out:outputs) {
			tmp.add(new Output(out));
		}
		this.outputs = Collections.unmodifiableList(tmp);

		System.err.println("#inputs: " + StringUtils.join(",", this.inputs));
		System.err.println("#outputs: " + StringUtils.join(",", this.outputs));
		
		System.err.println("#context:");
		for (String k: capturedContext.keySet()) {
			System.err.println("#  "+k+" => "+capturedContext.get(k));
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
		 * TODO: This needs to be a regex match to replace wildcards (%_foo)
		 */
		
		System.err.println("# Looking for: "+outputName);
		boolean matched = false;
		String wildcard = "";
		
		for (Output out:outputs) {
			System.err.println("#   Testing: "+out.regex);
			Matcher m = out.regex.matcher(outputName);
			if (m.matches()) {
				matched = true;
				System.err.println("#   MATCH: "+m.group(0));
				if (m.groupCount()>0) {
					wildcard = m.group(1);
					System.err.println("#   Wildcard: "+m.group(1));
				}
				break;
			}
		}
		
		if (matched) {
			List<String> matchedOutputs = new ArrayList<String>();
			List<String> matchedInputs = new ArrayList<String>();

			for (Output out:outputs) {
				matchedOutputs.add(out.rawName.replace("%", wildcard));
			}
			if (inputs!=null) {
				for (String input:inputs) {
					matchedInputs.add(input.replace("%", wildcard));
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
				src.add(Eval.evalString(nl.line, cxt));
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
