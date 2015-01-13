package org.ngsutils.mvpipe.context.variable;

public class VarFloat extends VarValue {
	public VarFloat(double val) {
		super(val);
	}
	
	public boolean isTrue() {
		if (((Double) obj) == 0.0d) {
			return false;
		}
		return true;
	}
	
	public VarFloat add(VarFloat val) {
		return new VarFloat((Double)obj + (Double)val.obj);
	}
	public VarFloat sub(VarFloat val) {
		return new VarFloat((Double)obj - (Double)val.obj);
	}
	public VarFloat mul(VarFloat val) {
		return new VarFloat((Double)obj * (Double)val.obj);
	}
	public VarFloat div(VarFloat val) {
		return new VarFloat((Double)obj / (Double)val.obj);
	}
	public VarFloat rem(VarFloat val) {
		return new VarFloat((Double)obj % (Double)val.obj);
	}

	public VarFloat add(VarInt val) {
		return new VarFloat((Double)obj + (Long)val.obj);
	}
	public VarFloat sub(VarInt val) {
		return new VarFloat((Double)obj - (Long)val.obj);
	}
	public VarFloat mul(VarInt val) {
		return new VarFloat((Double)obj * (Long)val.obj);
	}
	public VarFloat div(VarInt val) {
		return new VarFloat((Double)obj / (Long)val.obj);
	}
	public VarFloat rem(VarInt val) {
		return new VarFloat((Double)obj % (Long)val.obj);
	}

}
