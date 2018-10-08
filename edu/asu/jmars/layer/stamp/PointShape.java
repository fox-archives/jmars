package edu.asu.jmars.layer.stamp;

import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;

import edu.asu.jmars.Main;
import edu.asu.msff.StampInterface;

public class PointShape extends StampShape {

	public PointShape(StampInterface stamp, StampLayer stampLayer) {
		super(stamp, stampLayer);
	}
	    
    private Point2D origin = null;
    
    public synchronized Point2D getOrigin() {
    	if (origin==null) {
    		getPath();
    	}
    	return origin;
    }	
    
    public synchronized GeneralPath getPath()
    {
        if(path == null)
        {
            path = new GeneralPath();
                        
            double pts[] = myStamp.getPoints();

            origin = Main.PO.convSpatialToWorld(pts[0],pts[1]);
            
        	path.moveTo((float)origin.getX(),
                    (float)origin.getY());
        	
        	path.lineTo((float)(origin.getX()+0.0001),
                    (float)origin.getY()+0.0001);
        } 
        return  path;
    }

}
