package io.compgen.cgpipe.parser.variable;

import io.compgen.cgpipe.exceptions.MethodCallException;
import io.compgen.cgpipe.exceptions.MethodNotFoundException;
import io.compgen.cgpipe.exceptions.VarTypeException;
import io.compgen.common.StringUtils;

import java.io.File;

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
	
	public boolean toBoolean() {
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
		try {
			return super.call(method, args);
		} catch (MethodNotFoundException e1) {
			if (method.equals("split")) {
				if (args.length != 1 && args.length!=0) {
					throw new MethodCallException("Bad or missing argument! split(delim)");
				}
	
				String[] spl;
				if (args.length == 1) {
					spl = ((String)obj).split(args[0].toString());
				} else {
					spl = ((String)obj).split("");
				}
	
				VarList l = new VarList();
				for (String s:spl) {
					try {
						l.add(new VarString(s));
					} catch (VarTypeException e) {
						throw new MethodCallException(e);
					}
				}
				return l;
			} else if (method.equals("sub")) {
				if (args.length != 2) {
					throw new MethodCallException("Bad or missing argument! sub(bait,replace)");
				}
				return new VarString(((String)obj).replaceAll(args[0].toString(), args[1].toString()));
			} else if (method.equals("upper")) {
				if (args.length != 0) {
					throw new MethodCallException("Bad or missing argument! upper()");
				}
				return new VarString(((String)obj).toUpperCase());
			} else if (method.equals("lower")) {
				if (args.length != 0) {
					throw new MethodCallException("Bad or missing argument! lower()");
				}
				return new VarString(((String)obj).toLowerCase());
			} else if (method.equals("length")) {
				if (args.length != 0) {
					throw new MethodCallException("Bad or missing argument! length()");
				}
				return new VarInt(((String)obj).length());
			} else if (method.equals("basename")) {
				if (args.length != 0) {
					throw new MethodCallException("Bad or missing argument! basename()");
				}
				
				File f = new File((String) obj);
				return new VarString(f.getName());
			} else if (method.equals("exists")) {
				if (args.length != 0) {
					throw new MethodCallException("Bad or missing argument! exists()");
				}
				
				File f = new File((String) obj);
				if (f.exists()) {
					return VarBool.TRUE;
				}
				return VarBool.FALSE;
			} else if (method.equals("dirname")) {
				if (args.length != 0) {
					throw new MethodCallException("Bad or missing argument! dirname()");
				}
				
				File f = new File((String) obj);
				String dirname = f.getAbsoluteFile().getParent();
				if (dirname == null) {
					return new VarString("");
				} else {
					return new VarString(dirname);
				}
			} else if (method.equals("contains")) {
				if (args.length != 1) {
					throw new MethodCallException("Bad or missing argument! contains(qstr)");
				}
				if (((String)obj).contains(args[0].toString())) {
					return VarBool.TRUE;
				}
				return VarBool.FALSE;
			} else if (method.equals("join")) {
				if (args.length != 1 || !args[0].isList()) {
					throw new MethodCallException("Bad or missing argument! join(list)");
				}
				
				String ret = StringUtils.join((String)obj, args[0].iterate());
				return new VarString(ret);
				
			}
			throw new MethodNotFoundException("Method not found: "+method+" obj="+this);
		}
	}

//	public Iterable<VarValue> iterate() {
//		
//		return new Iterable<VarValue>(){
//
//			@Override
//			public Iterator<VarValue> iterator() {
//				return new Iterator<VarValue>(){
//					
//					int i = 0;
//					VarValue next = new VarString(""+((String)obj).charAt(i));
//					@Override
//					public boolean hasNext() {
//						return next != null;
//					}
//
//					@Override
//					public VarValue next() {
//						VarValue ret = next;
//						i++;
//						if (i < ((String)obj).length()) {
//							next = new VarString(""+((String)obj).charAt(i));
//						} else {
//							next = null;
//						}
//						return ret;
//					}
//
//					@Override
//					public void remove() {
//					}};
//			}};
//	}
//
}
