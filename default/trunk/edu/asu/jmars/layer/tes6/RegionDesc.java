package edu.asu.jmars.layer.tes6;

import java.awt.*;
import java.awt.geom.*;

/**
 * Region Descriptor: describes a bin in (lon,lat) 5x5-degree grid.
 * <p>
 * The entire (lon,lat) range is divided into 5x5-degree bins or regions.
 * These regions give us chunkier access to the data from the database.
 * <p>
 * Foot-print data has boresight lon and lat values stored in it. These
 * values are offsets from the centeroid of the region that they fall
 * into.
 */
class RegionDesc {
    public RegionDesc(Integer regionId, GeneralPath bbox){
        this.regionId = regionId;
        this.bbox = (GeneralPath)bbox.clone();
        Rectangle2D bounds = bbox.getBounds2D();
        this.centroid = new Point2D.Float((float)bounds.getCenterX(), (float)bounds.getCenterY());
    }
    
    public String toString(){
        return "RegionDesc["+regionId.toString()+
            ",centroid=("+centroid.getX()+","+centroid.getY()+")]";
    }

    public  Integer getRegionId(){ return regionId; }

    public  GeneralPath getRegionBoundary(){
        return (GeneralPath)(bbox.clone());
    }

    public  GeneralPath getRegionBoundaryShared(){
        return bbox;
    }

    public  Point2D getCentroid(){
        Point2D p = new Point2D.Float();
        p.setLocation(centroid);
        return p;
    }
    
    public  double getCenterX(){ return centroid.getX(); }
    public  double getCenterY(){ return centroid.getY(); }

    private Integer     regionId;
    private GeneralPath bbox;
    private Point2D     centroid;
}
