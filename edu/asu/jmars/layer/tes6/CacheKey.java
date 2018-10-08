package edu.asu.jmars.layer.tes6;

import java.io.Serializable;

/**
 * CacheKey is a data structure to hold various pieces needed
 * to reference the data associated with a particular field
 * or ordering. The data accessed using this key is limited
 * to just one region in the database and within that region
 * it is the sub-sample of data which satistfies the where-clause.
 * 
 * @author saadat
 *
 */
class CacheKey implements Serializable {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public CacheKey(String whereClause, Integer regionId, String dataName){
        this.whereClause = whereClause;
        this.regionId = regionId;
        this.dataName = dataName;
    }

    public boolean equals(Object o){
        if (o != null && o.getClass().equals(this.getClass())){
            CacheKey other = (CacheKey)o;
            return (other.whereClause.equals(whereClause) && 
                    other.regionId.equals(regionId) &&
                    other.dataName.equals(dataName));
        }
        return false;
    }

    public int hashCode(){
        return (dataName.hashCode()+regionId.hashCode()+whereClause.hashCode());
    }

    public String toString(){
        return "CacheKey[whereClause="+whereClause+",region="+regionId+",dataName="+dataName+"]";
    }

    private String whereClause;
    private Integer regionId;
    private String dataName;
}
