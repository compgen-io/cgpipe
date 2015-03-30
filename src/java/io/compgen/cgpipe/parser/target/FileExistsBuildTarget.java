package io.compgen.cgpipe.parser.target;

import io.compgen.cgpipe.exceptions.ASTExecException;
import io.compgen.cgpipe.exceptions.ASTParseException;
import io.compgen.cgpipe.parser.NumberedLine;
import io.compgen.cgpipe.runner.JobDef;
import io.compgen.common.ListBuilder;

import java.util.List;

public class FileExistsBuildTarget extends BuildTarget {
	public FileExistsBuildTarget(String output) {
		super(new ListBuilder<String>().add(output).list(), null, null, null);
	}
	
	public JobDef eval(List<NumberedLine> pre, List<NumberedLine> post) throws ASTParseException, ASTExecException {
		return null;
	}

}
