package io.compgen.mvpipe.parser.target;

import io.compgen.mvpipe.exceptions.ASTExecException;
import io.compgen.mvpipe.exceptions.ASTParseException;
import io.compgen.mvpipe.parser.NumberedLine;
import io.compgen.mvpipe.runner.JobDef;
import io.compgen.mvpipe.support.ListBuilder;

import java.util.List;

public class FileExistsBuildTarget extends BuildTarget {
	public FileExistsBuildTarget(String output) {
		super(new ListBuilder<String>().add(output).list(), null, null, null);
	}
	
	public JobDef eval(List<NumberedLine> pre, List<NumberedLine> post) throws ASTParseException, ASTExecException {
		return null;
	}

}
