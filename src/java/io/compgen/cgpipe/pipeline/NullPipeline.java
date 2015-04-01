package io.compgen.cgpipe.pipeline;

import java.util.ArrayList;
import java.util.List;

public class NullPipeline extends Pipeline {
	List<NumberedLine> lines = new ArrayList<NumberedLine>();
	public NullPipeline(NumberedLine line) {
		super("<null>", null);
		lines.add(line);
		finalize();
	}
}
