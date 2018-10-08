package edu.asu.jmars.layer.tes6;

import java.io.Serializable;


public class RangeDesc implements Cloneable, Serializable {
	private static final long serialVersionUID = 1L;
	
	public RangeDesc(FieldDesc field, Serializable minValue, Serializable maxValue){
		this.field = field;
		this.minValue = minValue;
		this.maxValue = maxValue;
	}

	public FieldDesc getField(){ return field; }
	public Serializable getMinValue(){ return minValue; }
	public Serializable getMaxValue(){ return maxValue; }
	public void setMinValue(Serializable minValue){
		this.minValue = minValue;
	}
	public void setMaxValue(Serializable maxValue){
		this.maxValue = maxValue;
	}
	
	public RangeDesc clone() {
		try {
			return (RangeDesc)super.clone();
		}
		catch(CloneNotSupportedException ex){
			throw new Error(getClass().getName()+".clone() threw the following unexpected exception: "+ex);
		}
	}

	public String toString(){
		return field.getFieldName()+"["+getMinValue()+".."+getMaxValue()+"]";
	}
	
	private FieldDesc field;
	private Serializable minValue, maxValue;
}

