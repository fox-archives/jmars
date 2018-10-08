package edu.asu.jmars.layer.tes6;

import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;

import edu.asu.jmars.layer.Layer;

public class ShapeTransformer {

	private final Layer.LView lView;
	private final AffineTransform eastToWestLonAffineTransform = 
		new AffineTransform(-1f,0f,0f,1f,360f,0f);
	private GeneralPath tmpPoly = new GeneralPath();
	private float[] coords = new float[6];
	private Point2D p1, p2, p3;
	private PathIterator pi;
	

	public ShapeTransformer(Layer.LView lView) {
		super();
		this.lView = lView;
	}
	
	public void toWorldInPlace(GeneralPath polyInEastLon){
		tmpPoly.reset();
		pi = polyInEastLon.getPathIterator(eastToWestLonAffineTransform);
		
		int segType;
		
		while(!pi.isDone()){
			switch(segType = pi.currentSegment(coords)){
			case PathIterator.SEG_MOVETO:
                p1 = lView.getProj().spatial.toWorld(coords[0], coords[1]);
                tmpPoly.moveTo((float)p1.getX(),(float)p1.getY());
				break;
			case PathIterator.SEG_LINETO:
                p1 = lView.getProj().spatial.toWorld(coords[0], coords[1]);
                tmpPoly.lineTo((float)p1.getX(),(float)p1.getY());
				break;
            case PathIterator.SEG_QUADTO:
                p1 = lView.getProj().spatial.toWorld(coords[0], coords[1]);
                p2 = lView.getProj().spatial.toWorld(coords[2], coords[3]);
                tmpPoly.quadTo((float)p1.getX(),(float)p1.getY(),
                         (float)p2.getX(),(float)p2.getY());
                break;
            case PathIterator.SEG_CUBICTO:
                p1 = lView.getProj().spatial.toWorld(coords[0], coords[1]);
                p2 = lView.getProj().spatial.toWorld(coords[2], coords[3]);
                p3 = lView.getProj().spatial.toWorld(coords[4], coords[5]);
                tmpPoly.curveTo((float)p1.getX(),(float)p1.getY(),
                          (float)p2.getX(),(float)p2.getY(),
                          (float)p3.getX(),(float)p3.getY());
                break;
			case PathIterator.SEG_CLOSE:
				tmpPoly.closePath();
				break;
			default:
				System.err.println("toWorldInPlace: unhandled segment type: "+ segType);
				break;
			}
			
			pi.next();
		}
		
		polyInEastLon.reset();
		polyInEastLon.append(tmpPoly, false);
	}
}
