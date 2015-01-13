package org.ngsutils.mvpipe.context.variable;

public class VarInt extends VarValue {
	public VarInt(long val) {
		super(val);
	}
	
	public boolean isTrue() {
		if (((Long) obj) == 0l) {
			return false;
		}
		return true;
	}
	
	public VarInt add(VarInt val) {
		return new VarInt((Long)obj + (Long)val.obj);
	}
	public VarInt sub(VarInt val) {
		return new VarInt((Long)obj - (Long)val.obj);
	}
	public VarInt mul(VarInt val) {
		return new VarInt((Long)obj * (Long)val.obj);
	}
	public VarInt div(VarInt val) {
		return new VarInt((Long)obj / (Long)val.obj);
	}
	public VarInt rem(VarInt val) {
		return new VarInt((Long)obj % (Long)val.obj);
	}

	public VarFloat add(VarFloat val) {
		return new VarFloat((Long)obj + (Double)val.obj);
	}
	public VarFloat sub(VarFloat val) {
		return new VarFloat((Long)obj - (Double)val.obj);
	}
	public VarFloat mul(VarFloat val) {
		return new VarFloat((Long)obj * (Double)val.obj);
	}
	public VarFloat div(VarFloat val) {
		return new VarFloat((Long)obj / (Double)val.obj);
	}
	public VarFloat rem(VarFloat val) {
		return new VarFloat((Long)obj % (Double)val.obj);
	}

}
