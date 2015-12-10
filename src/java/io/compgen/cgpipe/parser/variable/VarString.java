package io.compgen.cgpipe.parser.variable;

import io.compgen.cgpipe.exceptions.VarTypeException;

public class VarString extends VarValue {
	public VarString(String val) {
		super(val);
	}
	
//	public String toString() {
//		String s = (String) obj;
//		return s.substring(1, s.length()-1);
//	}
	public VarValue add(VarValue val) throws VarTypeException {
		return new VarString((String)obj + val.toString()); 
	}

	public VarValue lt(VarValue val) throws VarTypeException {
		return (((String)obj).compareTo(val.toString()) < 0) ? VarBool.TRUE: VarBool.FALSE;   
	}
	
	public boolean isTrue() {
		if (((String) obj).equals("")) {
			return false;
		}
		return true;
	}
	
	public int sizeInner() throws VarTypeException {
		return ((String) obj).length();
	}

	public VarValue sliceInner(int start, int end) {
		return new VarString(((String) obj).substring(start,  end));
	}
}
