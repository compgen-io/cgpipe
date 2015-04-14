package io.compgen.cgpipe.loader;

import java.util.ArrayList;
import java.util.List;

public class NullSource extends Source {
	List<NumberedLine> lines = new ArrayList<NumberedLine>();
	public NullSource(NumberedLine line) {
		super("<null>", null);
		lines.add(line);
		finalize();
	}
}
