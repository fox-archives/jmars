package edu.asu.jmars.layer.tes6;

import java.awt.*;
import java.awt.geom.*;
import java.util.regex.*;
import java.nio.*;
import java.util.*;
import edu.asu.jmars.layer.*;

public class ShapeUtils {
	public static final int wkbGeomPoint = 1;
	public static final int wkbGeomLineString = 2;
	public static final int wkbGeomPolygon = 3;
	public static final int wkbGeomMultiPoint = 4;
	public static final int wkbGeomMultiLineString = 5;
	public static final int wkbGeomMultiPolygon = 6;
	public static final int wkbGeomGeometryCollection = 7;

	public static final byte byteOrderBigEndian = 0;
	public static final byte byteOrderLittleEndian = 1;


	public static Point2D getPointFromWkbRep(byte[] wkbRep){
		ByteBuffer bb = ByteBuffer.wrap(wkbRep);
		byte byteOrder = bb.get();

		if (byteOrder == byteOrderLittleEndian){
			bb.order(ByteOrder.LITTLE_ENDIAN);
		}
		else {
			bb.order(ByteOrder.BIG_ENDIAN);
		}

		int wkbType = bb.getInt();
		if (wkbType != wkbGeomPoint){
			System.err.println("Invalid wkbType for Point: "+ wkbType);
		//	throw();
		}

		return(getPoint(bb));
	}

	private static Point2D getPoint(ByteBuffer bb){
		return(new Point2D.Double(bb.getDouble(), bb.getDouble()));
	}

    private static double[] getXY(ByteBuffer bb){
        return(new double[] { bb.getDouble(), bb.getDouble() });
    }

    protected static Shape makeLineString(ByteBuffer bb){
		int npts = bb.getInt();

		if (npts == 2){
			return(new Line2D.Double(getPoint(bb), getPoint(bb)));
		}

		GeneralPath p = new GeneralPath();
		for(int i = 0; i < npts; i++){
			Point2D pt = getPoint(bb);
			if (i == 0){ p.moveTo((float)pt.getX(), (float)pt.getY()); }
			else       { p.lineTo((float)pt.getX(), (float)pt.getY()); }
		}

		return(p);
	}

	protected static GeneralPath makePolygon(ByteBuffer bb){
		int nRings = bb.getInt();
		GeneralPath p = new GeneralPath(); // polygon

		for(int j = 0; j < nRings; j++){
            int npts = bb.getInt();
			GeneralPath sp = new GeneralPath(); // sub-polygon

			for(int i = 0; i < npts; i++){
				Point2D pt = getPoint(bb);
				if (i == 0){ sp.moveTo((float)pt.getX(), (float)pt.getY()); }
				else       { sp.lineTo((float)pt.getX(), (float)pt.getY()); }
			}
			sp.closePath();

			p.append(sp, false);
		}

		return(p);
	}

	public static Shape getShapeFromWkbRep(byte[] wkbRep){
		ByteBuffer bb = ByteBuffer.wrap(wkbRep);
		Shape s = null;

		byte byteOrder = bb.get();
		if (byteOrder == byteOrderLittleEndian){
			bb.order(ByteOrder.LITTLE_ENDIAN);
		}
		else {
			bb.order(ByteOrder.BIG_ENDIAN);
		}

		int wkbType = bb.getInt();
		switch(wkbType){
		//case wkbGeomPoint:
		//	s = makePoint(bb);
		//	break;

		case wkbGeomLineString:
			s = makeLineString(bb);
			break;
		
		case wkbGeomPolygon:
			s = makePolygon(bb);
			break;
		}

		return(s);
	}

    public static GeneralPath getPolyFromWkbRep(byte[] wkbRep){
        ByteBuffer bb = ByteBuffer.wrap(wkbRep);
        bb.order((bb.get() == byteOrderLittleEndian)?
                 ByteOrder.LITTLE_ENDIAN:
                 ByteOrder.BIG_ENDIAN);
		int wkbType = bb.getInt();
		int nRings = bb.getInt();
        int i, j, npts;
        float x, y;
        GeneralPath p = new GeneralPath();

        for(j = 0; j < nRings; j++){
            npts = bb.getInt();

            for(i = 0; i < npts; i++){
                x = (float)bb.getDouble(); y = (float)bb.getDouble();
                if (i == 0){ p.moveTo(x, y); }
                else       { p.lineTo(x, y); }
            }
            p.closePath();
        }

        return p;
    }

    public static Float[] getPolyDataFromWkbRep(byte[] wkbRep){
        ByteBuffer bb = ByteBuffer.wrap(wkbRep);
        bb.order((bb.get() == byteOrderLittleEndian)?
                 ByteOrder.LITTLE_ENDIAN:
                 ByteOrder.BIG_ENDIAN);
		int wkbType = bb.getInt();
		int nRings = bb.getInt();
        int i, j, npts;
        float x, y;
        Vector pts = new Vector();

        if (nRings > 1){
        	throw new RuntimeException("Multiple ringed polygons are not handled.");
        }
        for(j = 0; j < nRings; j++){
            npts = bb.getInt();

            for(i = 0; i < npts; i++){
                x = (float)bb.getDouble(); y = (float)bb.getDouble();
                pts.add(new Float(x)); pts.add(new Float(y));
            }
        }

        Float[] ptsArray = new Float[pts.size()];
        pts.toArray(ptsArray);
        
        return ptsArray;
    }

	// Returns GeneralPath from polygon string returned by postgres
	// of the form ((px1,py1),(px2,py2),...,(pxN,pyN)).
	public static GeneralPath getPolyFromString(String s){
		GeneralPath poly = new GeneralPath();
		Pattern ptPat = Pattern.compile("\\(([-+0-9.]+),([-+0-9.]+)\\)");
		Matcher m = ptPat.matcher(s);
		int n = 0;
		float px, py;

		while(m.find()){
			px = Float.parseFloat(m.group(1));
			py = Float.parseFloat(m.group(2));
			if (n++ == 0){ poly.moveTo(px,py); }
			else         { poly.lineTo(px,py); }
		}

		if (n > 0){ poly.closePath(); }

		return poly;
	}
	
	// Returns GeneralPath from polygon string returned by postgres
	// of the form ((px1,py1),(px2,py2),...,(pxN,pyN)).
	public static Float[] getPolyDataFromString(String s){
		Pattern ptPat = Pattern.compile("\\(([-+0-9.]+),([-+0-9.]+)\\)");
		Matcher m = ptPat.matcher(s);
		float px, py;
		Vector pts = new Vector();

		while(m.find()){
			px = Float.parseFloat(m.group(1));
			py = Float.parseFloat(m.group(2));
			pts.add(new Float(px));
			pts.add(new Float(py));
		}
		
		Float[] ptsArray = new Float[pts.size()];
		pts.toArray(ptsArray);

		return ptsArray;
	}

	public static GeneralPath getPolyFromSqlArray(java.sql.Array a)
		throws java.sql.SQLException
	{
		GeneralPath poly = new GeneralPath();
		java.sql.ResultSet rs = a.getResultSet();
		int count = 0, n=0;
		float pt[] = new float[2];
		
		while(rs.next()){
			pt[count%2] = rs.getFloat(2);
			
			if ((++count%2) == 0){
				if (n++ == 0){ poly.moveTo(pt[0], pt[1]); }
				else { poly.lineTo(pt[0],pt[1]); }
			}
		}
		poly.closePath();
		
		return poly;
	}

	public static Float[] getPolyDataFromSqlArray(java.sql.Array a)
		throws java.sql.SQLException
	{
		java.sql.ResultSet rs = a.getResultSet();
		Vector pts = new Vector();
	
		while(rs.next()){
			pts.add(new Float(rs.getFloat(2))); // index 1 is the point serial number
		}
		
		Float[] ptsArray = new Float[pts.size()];
		pts.toArray(ptsArray);
	
		return ptsArray;
	}
	
	public static GeneralPath polyFromFloatArray(Float[] pts){
		GeneralPath poly = new GeneralPath();
		
		for(int i = 0; i < pts.length; i+=2){
			if (i == 0){
				poly.moveTo(pts[i].floatValue(), pts[i+1].floatValue());
			}
			else {
				poly.lineTo(pts[i].floatValue(), pts[i+1].floatValue());
			}
		}
		poly.closePath();
		
		return poly;
	}

	public static byte[] wkbEncodePoint(Point2D p){
		int len = 1+4+2*8;
		ByteBuffer bb = ByteBuffer.allocate(len);

		bb.put(byteOrderLittleEndian); bb.order(ByteOrder.LITTLE_ENDIAN);
		bb.putInt(wkbGeomPoint);
		bb.putDouble(p.getX());
		bb.putDouble(p.getY());

		return(bb.array());
	}

	public static byte[] wkbEncodeLine(Line2D l){
		int len = 1+4+4+4*8;
		ByteBuffer bb = ByteBuffer.allocate(len);

		bb.put(byteOrderLittleEndian); bb.order(ByteOrder.LITTLE_ENDIAN);
		bb.putInt(wkbGeomLineString);
		bb.putInt(2);
		bb.putDouble(l.getX1());
		bb.putDouble(l.getY1());
		bb.putDouble(l.getX2());
		bb.putDouble(l.getY2());

		return(bb.array());
	}

	public static byte[] wkbEncodePolygon(GeneralPath p){
		AffineTransform nullXform = new AffineTransform();
		PathIterator i;
		double coords[] = new double[6];
		int    segType;
		int    ringCount = 0;
		Vector ringLengths = new Vector();
		int    segCount = 0;

		i = p.getPathIterator(nullXform);
		while(!i.isDone()){
			segType = i.currentSegment(coords);
			switch(segType){
			case PathIterator.SEG_MOVETO:
				if (ringCount > 0 && segCount != 0){ // open polygon
					ringLengths.add(new Integer(segCount));
				}
				ringCount++;
				segCount = 0;
				break;
			case PathIterator.SEG_LINETO:
				segCount++;
				break;
			case PathIterator.SEG_CLOSE:
				segCount++;
				ringLengths.add(new Integer(segCount));
				segCount = 0;
				break;
			default:
				System.err.println("Unhandled polygon segment "+ segType);
				// throw()
				break;
			}
			i.next();
		}

		int len = 1 + 4 + 4;
		for(int j = 0; j < ringCount; j++){
			len += 4 + (((Integer)ringLengths.get(j)).intValue() + 1) * 8;
		}

		// WKB encode polygon(s)
		ByteBuffer bb = ByteBuffer.allocate(len);

		bb.put(byteOrderBigEndian); bb.order(ByteOrder.BIG_ENDIAN);
		bb.putInt(wkbGeomPolygon);
		bb.putInt(ringCount);

		i = p.getPathIterator(null);
		int ringNum = 0;
		double firstPoint[] = new double[2];
		while(!i.isDone()){
			segType = i.currentSegment(coords);
			switch(segType){
			case PathIterator.SEG_MOVETO:
				int ringLen = ((Integer)ringLengths.get(ringNum++)).intValue();
				bb.putInt(ringLen);
				bb.putDouble(coords[0]); bb.putDouble(coords[1]);
				firstPoint[0] = coords[0]; firstPoint[1] = coords[1];
				break;
			case PathIterator.SEG_LINETO:
				bb.putDouble(coords[0]); bb.putDouble(coords[1]);
				break;
			case PathIterator.SEG_CLOSE:
				bb.putDouble(firstPoint[0]); bb.putDouble(firstPoint[1]);
				break;
			}

			i.next();
		}

		return(bb.array());
	}

	private static AffineTransform eastToWestLonAffineTransform = 
		new AffineTransform(-1f,0f,0f,1f,360f,0f);
	
    public static Point2D eastToWestLon(Point2D p){
    	return new Point2D.Double(360.0 - p.getX(), p.getY());
    }

    public static Point2D westToEastLon(Point2D p){
    	return new Point2D.Double(360.0 - p.getX(), p.getY());
    }

	public static void eastToWestLonInPlace(GeneralPath p){
		p.transform(eastToWestLonAffineTransform);
	}

	public static GeneralPath eastToWestLon(GeneralPath p){
        p = (GeneralPath)p.clone();
		p.transform(eastToWestLonAffineTransform);

        return p;
	}
	
	public static SerializablePoly eastToWestLon(SerializablePoly p){
        p = (SerializablePoly)p.clone();
		p.transform(eastToWestLonAffineTransform);

        return p;
	}
        /*
	public static Shape eastToWestLon(Shape s){
		GeneralPath r = new GeneralPath();
		
		PathIterator i = s.getPathIterator(null);
		float coords[] = new float[6];
		int segType;
		
		while(!i.isDone()){
			
			segType = i.currentSegment(coords);
			
			switch(segType){
			case PathIterator.SEG_MOVETO:
				r.moveTo(360.0f-coords[0],coords[1]);
				break;
			case PathIterator.SEG_LINETO:
				r.lineTo(360.0f-coords[0],coords[1]);
				break;
			case PathIterator.SEG_CLOSE:
				r.closePath();
				break;
			default:
				System.err.println("eastToWestLon: unhandled segment type: "+ segType);
				break;
			}
			
			i.next();
		}
		
		return r;
	}
        */

    public static Point2D screenToSpatialWestLon(Layer.LView lView, Point2D p){
        return lView.getProj().screen.toSpatial(p.getX(), p.getY());
    }

    public static Point2D screenToSpatialEastLon(Layer.LView lView, Point2D p){
        Point2D pp = screenToSpatialWestLon(lView, p);
        pp.setLocation(360.0-pp.getX(),pp.getY());
        return pp;
    }

	public static Point2D screenToWorld(Layer.LView lView, Point2D p) {
		return lView.getProj().screen.toWorld(p);
	}

    public static Point2D spatialEastLonToWorld(Layer.LView lView, Point2D p){
        Point2D pWest = new Point2D.Double(360.0 - p.getX(), p.getY());
        return spatialWestLonToWorld(lView, pWest);
    }
    
    public static Point2D spatialWestLonToWorld(Layer.LView lView, Point2D p){
        return lView.getProj().spatial.toWorld(p.getX(), p.getY());
    }

	public static GeneralPath spatialWestLonPolyToWorldPoly(Layer.LView lView, GeneralPath polyInWestLon){
		GeneralPath r = new GeneralPath();
		
		PathIterator i = polyInWestLon.getPathIterator(null);
		float coords[] = new float[6];
		int segType;
        Point2D p1, p2, p3;
		
		while(!i.isDone()){
			switch(segType = i.currentSegment(coords)){
			case PathIterator.SEG_MOVETO:
                p1 = lView.getProj().spatial.toWorld(coords[0], coords[1]);
                r.moveTo((float)p1.getX(),(float)p1.getY());
				break;
			case PathIterator.SEG_LINETO:
                p1 = lView.getProj().spatial.toWorld(coords[0], coords[1]);
                r.lineTo((float)p1.getX(),(float)p1.getY());
				break;
            case PathIterator.SEG_QUADTO:
                p1 = lView.getProj().spatial.toWorld(coords[0], coords[1]);
                p2 = lView.getProj().spatial.toWorld(coords[2], coords[3]);
                r.quadTo((float)p1.getX(),(float)p1.getY(),
                         (float)p2.getX(),(float)p2.getY());
                break;
            case PathIterator.SEG_CUBICTO:
                p1 = lView.getProj().spatial.toWorld(coords[0], coords[1]);
                p2 = lView.getProj().spatial.toWorld(coords[2], coords[3]);
                p3 = lView.getProj().spatial.toWorld(coords[4], coords[5]);
                r.curveTo((float)p1.getX(),(float)p1.getY(),
                          (float)p2.getX(),(float)p2.getY(),
                          (float)p3.getX(),(float)p3.getY());
                break;
			case PathIterator.SEG_CLOSE:
				r.closePath();
				break;
			default:
				System.err.println("spatialPolyToWorldPoly: unhandled segment type: "+ segType);
				break;
			}
			
			i.next();
		}
		
		return r;
	}

	/**
	 * Return the input point adjusted s.t. it is no more than 180 degrees from the given anchor point.
	 * @param pt Lon,lat point in degrees to be adjusted w.r.t. the given anchor
	 * @param anchor Anchor lon,lat point in degrees
	 * @return The input point or a point shifted by 360 degrees in lon
	 */
	private static Point2D adjWrtAnchor(Point2D pt, Point2D anchor){
		return new Point2D.Double(
				Math.abs(anchor.getX()-pt.getX())>180?
						Math.signum(anchor.getX()-pt.getX())*360+pt.getX():
						pt.getX(),
				pt.getY());
	}
	
	public static SerializablePoly spatialWestLonPolyToWorldPolySp(Layer.LView lView, SerializablePoly polyInWestLon){
		GeneralPath r = new GeneralPath();
		
		PathIterator i = polyInWestLon.getPathIterator(null);
		float coords[] = new float[6];
		int segType;
        Point2D p1, p2, p3;
        Point2D first = null;
		
		while(!i.isDone()){
			switch(segType = i.currentSegment(coords)){
			case PathIterator.SEG_MOVETO:
                first = p1 = lView.getProj().spatial.toWorld(coords[0], coords[1]);
                r.moveTo((float)p1.getX(),(float)p1.getY());
				break;
			case PathIterator.SEG_LINETO:
                p1 = lView.getProj().spatial.toWorld(coords[0], coords[1]);
                p1 = adjWrtAnchor(p1, first);
                r.lineTo((float)p1.getX(),(float)p1.getY());
				break;
            case PathIterator.SEG_QUADTO:
                p1 = lView.getProj().spatial.toWorld(coords[0], coords[1]);
                p2 = lView.getProj().spatial.toWorld(coords[2], coords[3]);
                p1 = adjWrtAnchor(p1, first);
                p2 = adjWrtAnchor(p2, first);
                r.quadTo((float)p1.getX(),(float)p1.getY(),
                         (float)p2.getX(),(float)p2.getY());
                break;
            case PathIterator.SEG_CUBICTO:
                p1 = lView.getProj().spatial.toWorld(coords[0], coords[1]);
                p2 = lView.getProj().spatial.toWorld(coords[2], coords[3]);
                p3 = lView.getProj().spatial.toWorld(coords[4], coords[5]);
                p1 = adjWrtAnchor(p1, first);
                p2 = adjWrtAnchor(p2, first);
                p3 = adjWrtAnchor(p3, first);
                r.curveTo((float)p1.getX(),(float)p1.getY(),
                          (float)p2.getX(),(float)p2.getY(),
                          (float)p3.getX(),(float)p3.getY());
                break;
			case PathIterator.SEG_CLOSE:
				r.closePath();
				break;
			default:
				System.err.println("spatialPolyToWorldPoly: unhandled segment type: "+ segType);
				break;
			}
			
			i.next();
		}
		
		return new SerializablePoly(r);
	}

	
	public static GeneralPath spatialEastLonPolyToWorldPoly(Layer.LView lView, GeneralPath polyInEastLon){
        return spatialWestLonPolyToWorldPoly(lView, eastToWestLon(polyInEastLon));
	}

	public static SerializablePoly spatialEastLonPolyToWorldPoly(Layer.LView lView, SerializablePoly polyInEastLon){
        return spatialWestLonPolyToWorldPolySp(lView, eastToWestLon(polyInEastLon));
	}
	
	public static Point2D worldToSpatialEastLon(Layer.LView lView, Point2D p){
		return westToEastLon(lView.getProj().world.toSpatial(p));
	}
	
	public static Rectangle2D worldToWestLon(Layer.LView lView, Rectangle2D worldRect){
		Point2D p1 = lView.getProj().world.toSpatial(worldRect.getMinX(), worldRect.getMinY());
		Point2D p2 = lView.getProj().world.toSpatial(worldRect.getMaxX(), worldRect.getMaxY());
		Rectangle2D out = new Rectangle2D.Double();
		out.setFrameFromDiagonal(p1, p2);
		return out;
	}
	
	public static Rectangle2D worldToEastLon(Layer.LView lView, Rectangle2D worldRect){
		Rectangle2D westLon = worldToWestLon(lView, worldRect);
		westLon.setFrameFromDiagonal(360.0-westLon.getMinX(), westLon.getMinY(), 360.0-westLon.getMaxX(), westLon.getMaxY());
		return westLon;
	}
	
	public static void printPoly(Shape s){
        System.err.println(polyToString(s));
	}

	public static String polyToString(Shape s){
		PathIterator i = s.getPathIterator(null);
		float coords[] = new float[6];
		int segType;
		String str = "";
		
		while(!i.isDone()){
			
			segType = i.currentSegment(coords);
			
			if (!str.equals("")){ str+=","; }
			switch(segType){
			case PathIterator.SEG_MOVETO:
				str += "("+coords[0]+","+coords[1]+")";
				break;
			case PathIterator.SEG_LINETO:
				str += "->("+coords[0]+","+coords[1]+")";
				break;
            case PathIterator.SEG_QUADTO:
                str += "->(("+coords[0]+","+coords[1]+"),"+
                    "("+coords[2]+","+coords[3]+"))";
            case PathIterator.SEG_CUBICTO:
                str += "->(("+coords[0]+","+coords[1]+"),"+
                    "("+coords[2]+","+coords[3]+"),"+
                    "("+coords[4]+","+coords[5]+"))";
			case PathIterator.SEG_CLOSE:
				str += "->X";
				break;
			default:
				str += "unhandled segment type: "+ segType;
				break;
			}
			
			i.next();
		}
		return str;
	}
	
	public static void spatialEastLonPolyToWorldPolyInPlace(Layer.LView lView, GeneralPath polyInEastLon){
		GeneralPath r = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 5);
		
		PathIterator i = polyInEastLon.getPathIterator(eastToWestLonAffineTransform);
		float coords[] = new float[6];
		int segType;
        Point2D p1, p2, p3;
		
		while(!i.isDone()){
			switch(segType = i.currentSegment(coords)){
			case PathIterator.SEG_MOVETO:
                p1 = lView.getProj().spatial.toWorld(coords[0], coords[1]);
                r.moveTo((float)p1.getX(),(float)p1.getY());
				break;
			case PathIterator.SEG_LINETO:
                p1 = lView.getProj().spatial.toWorld(coords[0], coords[1]);
                r.lineTo((float)p1.getX(),(float)p1.getY());
				break;
            case PathIterator.SEG_QUADTO:
                p1 = lView.getProj().spatial.toWorld(coords[0], coords[1]);
                p2 = lView.getProj().spatial.toWorld(coords[2], coords[3]);
                r.quadTo((float)p1.getX(),(float)p1.getY(),
                         (float)p2.getX(),(float)p2.getY());
                break;
            case PathIterator.SEG_CUBICTO:
                p1 = lView.getProj().spatial.toWorld(coords[0], coords[1]);
                p2 = lView.getProj().spatial.toWorld(coords[2], coords[3]);
                p3 = lView.getProj().spatial.toWorld(coords[4], coords[5]);
                r.curveTo((float)p1.getX(),(float)p1.getY(),
                          (float)p2.getX(),(float)p2.getY(),
                          (float)p3.getX(),(float)p3.getY());
                break;
			case PathIterator.SEG_CLOSE:
				r.closePath();
				break;
			default:
				System.err.println("spatialPolyToWorldPoly: unhandled segment type: "+ segType);
				break;
			}
			
			i.next();
		}
		
		polyInEastLon.reset();
		polyInEastLon.append(r, false);
	}

	public static void copyFrom(GeneralPath to, GeneralPath from){
    	to.reset();
    	to.append(from, false);
    }

}


