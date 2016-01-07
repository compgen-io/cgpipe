package io.compgen.cgpipe.parser.variable;

import io.compgen.cgpipe.exceptions.MethodCallException;
import io.compgen.cgpipe.exceptions.MethodNotFoundException;
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

	public VarValue call(String method, VarValue[] args) throws MethodNotFoundException, MethodCallException {
		if (method.equals("split")) {
			if (args.length != 1) {
				throw new MethodCallException("Bad or missing argument! split(delim)");
			}

			String[] spl = ((String)obj).split(args[0].toString());

			VarList l = new VarList();
			for (String s:spl) {
				try {
					l.add(new VarString(s));
				} catch (VarTypeException e) {
					throw new MethodCallException(e);
				}
			}
			return l;
		} else if (method.equals("length")) {
			if (args.length != 0) {
				throw new MethodCallException("Bad or missing argument! length()");
			}
			return new VarInt(((String)obj).length());
		}
		throw new MethodNotFoundException("Method not found: "+method+" obj="+this);
	}


}
