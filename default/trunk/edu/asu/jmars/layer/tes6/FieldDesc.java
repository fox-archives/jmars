package edu.asu.jmars.layer.tes6;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Field Descriptor of various fields available to the user.
 * Descriptors are loaded from the fields table.
 */
public class FieldDesc implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public FieldDesc(String name, String desc, String tableName,
                     boolean fpField, boolean arrayField, boolean spectralField, Class<?> type){
		this(name, desc, null, tableName, fpField, arrayField, spectralField, type, null);
	}
	
	public FieldDesc(String name, String desc, String toolTipText,
			String tableName,
            boolean fpField, boolean arrayField, boolean spectralField, Class<?> type){
		this(name, desc, toolTipText, tableName, fpField, arrayField, spectralField, type, null);
	}

	public FieldDesc(String name, String desc, String toolTipText, String tableName,
            boolean fpField, boolean arrayField, boolean spectralField, Class<?> type,
            int[] indexRange){
		this.name = name;
		this.desc = desc;
		this.tableName = tableName;
		this.fpField = fpField;
		this.arrayField = arrayField;
        this.spectralField = spectralField;
		this.type = type;
		this.tip = toolTipText;
		this.indexRange = indexRange;
		if (indexRange != null && indexRange.length != 2)
			throw new IllegalArgumentException("indexRange must be passed as an array of length two (2).");
	}
	public boolean equals(Object o){
        if (o != null && o.getClass().equals(this.getClass())){
            FieldDesc ox = (FieldDesc)o;
            return (getQualifiedFieldName().equals(ox.getQualifiedFieldName()));
        }
        return false;
	}

	public int hashCode(){
		return getQualifiedFieldName().hashCode();
	}

	public String toString(){
		return getQualifiedFieldName();
		//return getClass().getName()+"[qualName="+getQualifiedFieldName()+",desc="+desc+"]";
	}

    public Serializable valueOf(String s){
        if (type == String.class){ return s; }
        else if (type == Byte.class){ return Byte.valueOf(s); }
        else if (type == Short.class){ return Short.valueOf(s); }
        else if (type == Integer.class){ return Integer.valueOf(s); }
        else if (type == Long.class){ return Long.valueOf(s); }
        else if (type == Float.class){ return Float.valueOf(s); }
        else if (type == Double.class){ return Double.valueOf(s); }

        throw new IllegalArgumentException("Unhandled type exception");
    }

	public String getFieldName(){ return name; }
	public String getFieldDesc(){ return ((desc==null)? name: desc); }
	public String getTableName(){ return tableName; }
	public String getQualifiedFieldName(){
        // return ((tableName == null)? name: tableName+"."+name);
        return name;
	}
    public boolean isFpField(){ return fpField; }
    public boolean isArrayField(){ return arrayField; }
    public boolean isSpectralField(){ return spectralField; }
    public Class<?>   getFieldType(){ return type; }
    public String  getToolTipText(){ return tip; }
    public int[]   getIndexRange(){ return indexRange == null? null: indexRange.clone(); }

	private String name, desc, tableName, tip;
    private boolean fpField, arrayField, spectralField;
    private Class<?>  type;
    private int[] indexRange;

	// private int itemType;
	// private boolean isArray;
	// private int nItems;

	// public static int ITEM_TYPE_NUMERIC = 1;
	// public static int ITEM_TYPE_STRING  = 2;
    // public static int ITEM_TYPE_POLY = 3;
    
	public static class FieldComparatorByName implements Comparator<FieldDesc> {
		public int compare(FieldDesc fd1, FieldDesc fd2) {
			return fd1.getFieldName().compareToIgnoreCase(fd2.getFieldName());
		}

	}
}

