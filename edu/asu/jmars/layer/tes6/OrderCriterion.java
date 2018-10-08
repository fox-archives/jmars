package edu.asu.jmars.layer.tes6;

import java.io.Serializable;


public class OrderCriterion implements Cloneable, Serializable {
	private static final long serialVersionUID = 1L;
	
    public OrderCriterion(FieldDesc field, boolean direction){
        this.field = field;
        this.direction = direction;
    }
    
    public FieldDesc getField(){ return field; }
    public boolean   getDirection(){ return direction; }
    public void      setDirection(boolean direction){ this.direction = direction; }
    
    public OrderCriterion  clone() {
        // don't use copy construtor for clone.
        // as per contract no constructurs of any objects
        // should be called during clone.
    	try {
    		OrderCriterion oc = (OrderCriterion)super.clone();
    		oc.field = this.field;
    		oc.direction = this.direction;
    		return oc;
    	}
    	catch(CloneNotSupportedException ex){
			throw new Error(getClass().getName()+".clone() threw the following unexpected exception: "+ex);
    	}
    }
    
    public String toString(){
    	return field.getFieldName()+":"+(direction?"asc":"dec");
    }

    private FieldDesc field;
    private boolean   direction;

    public static final boolean ASCENDING = true;
    public static final boolean DESCENDING = false;
}
