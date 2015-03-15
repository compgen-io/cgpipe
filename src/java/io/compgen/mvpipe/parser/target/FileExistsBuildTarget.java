package org.ngsutils.mvpipe.parser.target;

import java.util.List;

import org.ngsutils.mvpipe.exceptions.ASTExecException;
import org.ngsutils.mvpipe.exceptions.ASTParseException;
import org.ngsutils.mvpipe.parser.NumberedLine;
import org.ngsutils.mvpipe.runner.JobDef;
import org.ngsutils.mvpipe.support.ListBuilder;

public class FileExistsBuildTarget extends BuildTarget {
	public FileExistsBuildTarget(String output) {
		super(new ListBuilder<String>().add(output).list(), null, null, null);
	}
	
	public JobDef eval(List<NumberedLine> pre, List<NumberedLine> post) throws ASTParseException, ASTExecException {
		return null;
	}

}
