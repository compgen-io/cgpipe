package io.compgen.cgpipe.parser.variable;

import io.compgen.cgpipe.exceptions.VarTypeException;

public class VarFloat extends VarValue {
	public VarFloat(double val) {
		super(val);
	}
	
	public boolean toBoolean() {
		if (((Double) obj) == 0.0d) {
			return false;
		}
		return true;
	}

	public boolean isNumber() {
		return true;
	}

	public static String doubleToString(double d) {
		if (d == Double.NaN) {
			return "NaN";
		}
		
		if (Double.isInfinite(d)) {
			return "Infinity";
		}

		if (d == Double.POSITIVE_INFINITY) {
			return "Infinity";
		}

		if (d == Double.NEGATIVE_INFINITY) {
			return "-Infinity";
		}

		String s = Double.toString(d);
		
		if (s.contains("E")) {
			String[] spl = s.split("E");
			
			String buf = spl[0];
			int exp = Integer.parseInt(spl[1]);
			
			if (exp > 0) {
				while (buf.length()-2 < exp) {
					buf = buf + "0";
				}
				char[] b = buf.toCharArray();
				for (int i=1; i<=exp; i++) {
					if (b[i] != '.') {
						throw new RuntimeException("Don't know how to format number: "+d);
					}
					b[i] = b[i+1];
					b[i+1] = '.';
				}
				s = String.valueOf(b);
			} else {
				while (buf.length()-2 <= -exp) {
					buf = "0" + buf;
				}
				char[] b = buf.toCharArray();
				for (int i=1; i<=-exp; i++) {
					if (b[b.length-i-1] != '.') {
						throw new RuntimeException("Don't know how to format number: "+d);
					}
					
					b[b.length-i-1] = b[b.length-i-2];
					b[b.length-i-2] = '.';
				}
				s = String.valueOf(b);
				while (s.endsWith("0")) {
					s = s.substring(0, s.length()-1);
				}
			}
		}
		
		if (s.endsWith(".0")) {
			return s.substring(0, s.length()-2);
		} else if (s.endsWith(".")) {
			return s.substring(0, s.length()-1);
		}
		
		return s;
	}
	
	
	public String toString() {
		String s = obj.toString();
		if (s.endsWith(".0")) {
			return s.substring(0, s.length()-2);
		}
		
		return doubleToString((Double)obj);
	}
	
	public VarValue lt(VarValue val) throws VarTypeException {
		if (val.getClass().equals(VarInt.class)) {
			return ((Double)obj < (Long)val.obj) ? VarBool.TRUE: VarBool.FALSE;
		} else if (val.getClass().equals(VarFloat.class)) {
			return ((Double)obj < (Double)val.obj) ? VarBool.TRUE: VarBool.FALSE;
		}
		throw new VarTypeException("Invalid operation");
	}

	public VarValue add(VarValue val) throws VarTypeException {
		if (val.getClass().equals(VarInt.class)) {
			return new VarFloat((Double)obj + (Long)val.obj);
		} else if (val.getClass().equals(VarFloat.class)) {
			return new VarFloat((Double)obj + (Double)val.obj);
		} else {
			return new VarString(toString() + val.toString());
		}
	}
	
	public VarValue sub(VarValue val) throws VarTypeException {
		if (val.getClass().equals(VarInt.class)) {
			return new VarFloat((Double)obj - (Long)val.obj);
		} else if (val.getClass().equals(VarFloat.class)) {
			return new VarFloat((Double)obj - (Double)val.obj);
		}
		throw new VarTypeException("Invalid operation");
	}

	public VarValue mul(VarValue val) throws VarTypeException {
		if (val.getClass().equals(VarInt.class)) {
			return new VarFloat((Double)obj * (Long)val.obj);
		} else if (val.getClass().equals(VarFloat.class)) {
			return new VarFloat((Double)obj * (Double)val.obj);
		}
		throw new VarTypeException("Invalid operation");
	}

	public VarValue div(VarValue val) throws VarTypeException {
		if (val.getClass().equals(VarInt.class)) {
			return new VarFloat((Double)obj / (Long)val.obj);
		} else if (val.getClass().equals(VarFloat.class)) {
			return new VarFloat((Double)obj / (Double)val.obj);
		}
		throw new VarTypeException("Invalid operation");
	}

	public VarValue rem(VarValue val) throws VarTypeException {
		if (val.getClass().equals(VarInt.class)) {
			return new VarFloat((Double)obj % (Long)val.obj);
		} else if (val.getClass().equals(VarFloat.class)) {
			return new VarFloat((Double)obj % (Double)val.obj);
		}
		throw new VarTypeException("Invalid operation");
	}
	public VarValue pow(VarValue val) throws VarTypeException {
		if (val.getClass().equals(VarInt.class)) {
			return new VarFloat(Math.pow((Double)obj, (Long)val.obj));
		} else if (val.getClass().equals(VarFloat.class)) {
			return new VarFloat(Math.pow((Double)obj, (Double)val.obj));
		}
		throw new VarTypeException("Invalid operation");
	}
}
