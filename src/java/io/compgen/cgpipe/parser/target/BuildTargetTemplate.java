package io.compgen.cgpipe.parser.target;

import io.compgen.cgpipe.exceptions.ASTExecException;
import io.compgen.cgpipe.loader.NumberedLine;
import io.compgen.cgpipe.parser.Eval;
import io.compgen.cgpipe.parser.context.ExecContext;
import io.compgen.cgpipe.parser.tokens.TokenList;
import io.compgen.cgpipe.parser.variable.VarValue;
import io.compgen.common.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BuildTargetTemplate {
	final private List<String> outputs = new ArrayList<String>();
	final private List<String> inputs = new ArrayList<String>();
	final private List<NumberedLine> lines;
	final private Map<String, VarValue> capturedContext;
	final private boolean importable;
	
	private Map<String, BuildTarget> cache = new HashMap<String, BuildTarget>();
	
	public BuildTargetTemplate(List<String> outputs, List<String> inputs, ExecContext context, List<NumberedLine> lines, TokenList tokens, boolean importable) throws ASTExecException {
		// these are copied verbatim.
		this.capturedContext = context.cloneValues();
		// these will be eval'd only when needed (they might need to be executed)
		this.lines = lines; 

		for (String out: outputs) {
			if (out != null && !out.equals("")) {
				// The outputs might be a list, so we need to eval and split...
				for (String s: Eval.evalString(out, context, tokens.getLine()).split(" ")) {
					this.outputs.add(s);
				}
			}
		}

		for (String in: inputs) {
			if (in != null && !in.equals("")) {
				// The inputs might be a list, so we need to eval and split...
				for (String s: Eval.evalString(in, context, tokens.getLine()).split(" ")) {
					this.inputs.add(s);
				}
			}
		}
		
		if (this.outputs.size() == 0) {
			throw new ASTExecException("No outputs specified for build-target!");
		}
		
		this.importable = importable;
	}
	
	public String toString() {
		return "<build-target-templ> " + StringUtils.join(" ", outputs) + " : " + StringUtils.join(" ", inputs);
	}
	
	/**
	 * Find a target for a given output, without worrying about wildcard matching
	 * @param query
	 * @return
	 */
	
	public BuildTarget makeBuildTarget() {
		List<String> matchedOutputs = new ArrayList<String>();
		List<String> matchedInputs = new ArrayList<String>();

		// replace outputs w/ wildcard
		for (String output:outputs) {
			matchedOutputs.add(output);
		}
		if (inputs != null) {
			for (String input:inputs) {
				matchedInputs.add(input);
			}
		}

		// looks like we are good -- return a build target that can make the requested output, and populate the inputs/wildcard, etc...
		
		BuildTarget tgt = new BuildTarget(matchedOutputs, matchedInputs, "", capturedContext, lines);
		return tgt;

	}

	
	/**
	 * Find a target for a given output, taking into account wildcards in the output filenames
	 * @param query
	 * @return
	 */
	
	public BuildTarget matchOutput(String query) {
		
		if (query == null) {
			return null;
		}
		
		String wildcard = "";
//		if (testOutput != null) {

		// for the outputs this target can produce, check to see if we can make the query
		boolean matched = false;
		for (String outputName: outputs) {
			if (outputName.startsWith("^") || outputName.startsWith("\\^")) {
				outputName = outputName.substring(1);
			}
			Pattern regex = Pattern.compile("\\Q"+outputName.replace("%", "\\E(.*)\\Q")+"\\E");
			Matcher m = regex.matcher(query);
			if (m.matches()) {
				matched = true;
				if (m.groupCount()>0) {
					wildcard = m.group(1);
				}
				break;
			}
		}

		// we can't match -- bail.
		if (!matched) {
			return null;
		}

//		} else if (outputs.get(0).startsWith("__")) {
//			// these can't be the default target.
//			return null;
//		}
		
		// we want singletons for each wildcard
		if (cache.containsKey(wildcard)) {
			return cache.get(wildcard);
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
		if (inputs != null) {
			// replace inputs w/ wildcard
			for (String input:inputs) {
				Matcher m = wildPattern.matcher(input);
				if (m.matches()) {
					matchedInputs.add(m.group(1)+wildcard+m.group(2));
				} else {
					matchedInputs.add(input);
				}
			}
		}

		// looks like we are good -- return a build target that can make the requested output, and populate the inputs/wildcard, etc...
		
		BuildTarget tgt = new BuildTarget(matchedOutputs, matchedInputs, wildcard, capturedContext, lines);
		cache.put(wildcard, tgt);
		return tgt;
	}

	public String getFirstOutput() {
		return outputs.get(0);
	}

	public boolean isImportable() {
		return importable;
	}
	
	public String getImportName() {
		return outputs.get(0);
	}

	// setup an "importable" target (foo::)
	public BuildTarget importTarget() {
		BuildTarget tgt = new BuildTarget(null, null, null, capturedContext, lines);
		return tgt;
	}
	
}
