package edu.asu.jmars.layer.tes6;

import java.util.Comparator;

public class TesKey implements Comparable {
    public TesKey(int detId, RegionDesc regionDesc, String whereClause){
        this.detId = detId;
        this.regionDesc = regionDesc;
        this.whereClause = whereClause;
    }

    public int getDetId(){ return detId; }
    public RegionDesc getRegionDesc(){ return regionDesc; }
    public String getWhereClause(){ return whereClause; }

    public int getOck(){ return Common.getOck(detId); }
    public short getIck(){ return Common.getIck(detId); }
    public byte  getDet(){ return Common.getDet(detId); }

    public boolean equals(Object o){
        if (o instanceof TesKey){
            return (compareTo(o) == 0);
        }
        return false;
    }
    public int hashCode(){
        int hash = 0;

        hash ^= whereClause.hashCode();
        //hash ^= regionDesc.getRegionId().intValue();
        hash ^= detId;

        return hash;
    }

    public int compareTo(Object o){
        TesKey k = (TesKey)o;
        int result = 0;

        if (o == null){ result = 1; }
        else {
            result = (int)(detId - k.getDetId());
            //if (result == 0){
            //    result = regionDesc.getRegionId().intValue() -
            //        k.getRegionDesc().getRegionId().intValue();
            //}
            if (result == 0){
                result = whereClause.compareTo(k.getWhereClause());
            }
        }

        return result;
    }

    public String toString(){
    	return "TesKey["+"("+getOck()+","+getIck()+","+getDet()+"),detId="+detId+",region="+regionDesc.getRegionId()+")]";
    }

    private int detId;
    private RegionDesc regionDesc;
    private String whereClause;
}

class TesKeyIdComparator implements Comparator<TesKey> {
	public int compare(TesKey o1, TesKey o2) {
		return o2.getDetId() - o1.getDetId();
	}
}
