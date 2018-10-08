package edu.asu.jmars.layer.tes6;

import java.io.Serializable;

public class ColorBy implements Cloneable, Serializable {
	private static final long serialVersionUID = 1L;
	
    public ColorBy(FieldDesc field, Serializable minVal, Serializable maxVal){
        this.field = field;
        this.minVal = minVal;
        this.maxVal = maxVal;
    }
    
    public boolean isEmpty(){
    	return (field == null) || (minVal == null) || (maxVal == null);
    }
    
    public ColorBy clone(){
    	try {
    		return (ColorBy)super.clone();
    	}
    	catch(CloneNotSupportedException ex){
    		throw new Error(getClass().getName()+".clone() threw the following unexpected exception: "+ex);
    	}
    }
    
    public String toString(){
    	return (field==null?"no-coloring":field.getFieldName()+"["+minVal+","+maxVal+"]");
    }
    
    public FieldDesc field;
    public Serializable    minVal;
    public Serializable    maxVal;
}
