package org.ngsutils.mvpipe.parser.target;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ngsutils.mvpipe.exceptions.ASTExecException;
import org.ngsutils.mvpipe.parser.Eval;
import org.ngsutils.mvpipe.parser.NumberedLine;
import org.ngsutils.mvpipe.parser.context.ExecContext;
import org.ngsutils.mvpipe.parser.tokens.TokenList;
import org.ngsutils.mvpipe.parser.variable.VarValue;
import org.ngsutils.mvpipe.support.StringUtils;

public class BuildTargetTemplate {
	final private List<String> outputs = new ArrayList<String>();
	final private List<String> inputs = new ArrayList<String>();
	final private List<NumberedLine> lines;
	final private Map<String, VarValue> capturedContext;
	
	public BuildTargetTemplate(List<String> outputs, List<String> inputs, ExecContext context, List<NumberedLine> lines, TokenList tokens) throws ASTExecException {
		// these are copied verbatim.
		this.capturedContext = context.cloneValues();
		// these will be eval'd only when needed (they might need to be executed)
		this.lines = lines; 

		for (String out: outputs) {
			this.outputs.add(Eval.evalString(out, context, tokens));
		}

		for (String in: inputs) {
			this.inputs.add(Eval.evalString(in, context, tokens));
		}
		
		if (this.outputs.size() == 0) {
			throw new ASTExecException("No outputs specified for build-target!");
		}
	}
	
	public String toString() {
		return "<build-target-templ> " + StringUtils.join(" ", outputs) + " : " + StringUtils.join(" ", inputs);
	}
	
	public BuildTarget matchOutput(String testOutput) {
		String wildcard = "";
		if (testOutput != null) {
			boolean matched = false;
			for (String outputName: outputs) {
				Pattern regex = Pattern.compile("\\Q"+outputName.replace("%", "\\E(.*)\\Q")+"\\E");
				Matcher m = regex.matcher(testOutput);
				if (m.matches()) {
					matched = true;
					if (m.groupCount()>0) {
						wildcard = m.group(1);
					}
					break;
				}
			}
	
			if (!matched) {
				return null;
			}
		} else if (outputs.get(0).startsWith("__")) {
			// these can't be the default target.
			return null;
		}
		
		List<String> matchedOutputs = new ArrayList<String>();
		List<String> matchedInputs = new ArrayList<String>();

		Pattern wildPattern = Pattern.compile("^(.*?)%(.*?)$");

		// replace outputs w/ wildcard
		for (String output:outputs) {
			Matcher m = wildPattern.matcher(output);
			if (m.matches()) {
				matchedOutputs.add(m.group(1)+wildcard+m.group(2));
			} else {
				matchedOutputs.add(output);
			}
		}
		// replace inputs w/ wildcard
		for (String input:inputs) {
			Matcher m = wildPattern.matcher(input);
			if (m.matches()) {
				matchedInputs.add(m.group(1)+wildcard+m.group(2));
			} else {
				matchedInputs.add(input);
			}
		}

		return new BuildTarget(matchedOutputs, matchedInputs, capturedContext, lines);
	}

	public String getFirstOutput() {
		return outputs.get(0);
	}

}
