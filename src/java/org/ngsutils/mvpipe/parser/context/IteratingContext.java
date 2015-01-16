package org.ngsutils.mvpipe.parser.context;

import java.util.ArrayList;
import java.util.List;

import org.ngsutils.mvpipe.parser.SyntaxException;
import org.ngsutils.mvpipe.parser.Tokens;
import org.ngsutils.mvpipe.parser.variable.VarValue;
import org.ngsutils.mvpipe.support.StringUtils;

public class IteratingContext extends DeferredExecContext {
	List<Tokens> lines = new ArrayList<Tokens>();

	private String varname;
	private VarValue range;
	
	private int nestedCount = 0;
	
	public IteratingContext(ExecContext context, String varname, VarValue range) {
		super(context);
		this.varname = varname;
		this.range = range;
	}

	public void set(String name, VarValue val) {
		parent.set(name, val);
	}
	
	public ExecContext addTokenizedLine(Tokens tokens) throws SyntaxException {
		if (tokens.size() > 1 && tokens.get(0).equals("for")) {
			nestedCount ++;
		} else if (tokens.size() == 1 && tokens.get(0).equals("done")) {
			if (nestedCount == 0) {
				System.err.println("# STARTING ITERATION: " + varname + " => " + range);
				
				for (Tokens line:lines) {
					System.err.println("# ITERATING LINE: " + StringUtils.join(" ", line.getList()));
				}
				for (VarValue item: range.iterate()) {
					ExecContext cxt = new SubContext(this, true, false);
					cxt.set(varname, item);
					System.err.println("# " + varname + " => " + item);
					for (Tokens line: lines) {
						cxt = cxt.addTokenizedLine(line);
					}
				}
				return this.parent;			
			} else {
				nestedCount--;
			}
		}

		lines.add(tokens);
		return this;
	}
}
