package edu.asu.jmars.layer.stamp;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import edu.asu.jmars.Main;
import edu.asu.jmars.layer.InvestigateData;
import edu.asu.jmars.layer.stamp.focus.OutlineFocusPanel;
import edu.asu.jmars.layer.stamp.focus.StampFocusPanel;
import edu.asu.jmars.layer.stamp.focus.OutlineFocusPanel.StampAccessor;
import edu.asu.jmars.util.HVector;
import edu.asu.msff.StampInterface;
import gnu.jel.CompiledExpression;

public class StampShape {

	protected StampInterface myStamp;
	StampLayer stampLayer;
	
	public StampShape(StampInterface stamp, StampLayer stampLayer) {
		myStamp=stamp;
		this.stampLayer = stampLayer;
	}
	
	public String getId() {
		return myStamp.getId();
	}
	
	public StampInterface getStamp() {
		return myStamp;
	}
	
	public String getColumnName(int index) {
		return stampLayer.getColumnName(index);
	}	
	
	public Object getData(int index) {
		int maxLength = myStamp.getData().length;
		if (index>=0 && index<maxLength) {
			// TODO: Add safety
			return myStamp.getData()[index];
		} else {
			if (index==maxLength) {
				return calculatedColor;
			} else if (index==maxLength+1) {
				return Boolean.FALSE;
			} else if (index==maxLength+2) {
				return calculatedValue;
			}
			return null;
		}
	}
	
	double calculatedValue=Double.NaN;
	public void setCalculatedValue(double newVal) {
		calculatedValue = newVal;
	}
	
	public double getCalculatedValue() {
		return calculatedValue;
	}
	
	Color calculatedColor=null;
	public void setCalculatedColor(Color newColor) {
		calculatedColor=newColor;
	}
	
	public Color getCalculatedColor() {
		return calculatedColor;
	}
	
	// TODO: StampShape is probably misnamed.
	public double[] getXValues(String columnName) {	
		HashMap<String,String> axisMap = stampLayer.getSpectraAxisMap();
		HashMap<String,String> axisPostMap = stampLayer.getSpectraAxisPostMap();
			
		String xAxisName = axisMap.get(columnName.trim());
				
		String postCol = axisPostMap.get(columnName.trim());
		if (postCol!=null && postCol.length()>0) {
			xAxisName += getVal(postCol);
		} 

		double axisVals[]=stampLayer.getAxis(xAxisName.trim());
		
		if (axisVals!=null) {
			return axisVals;
		}
		
		
		Object v = getVal((xAxisName.trim()));

		if (v!=null) {
			if (v instanceof float[]) {
				float f[]=(float[])v;
				
				double d[] = new double[f.length];
				
				for (int i=0; i<f.length; i++) {
					d[i]=f[i];
				}
				
				return d;
				
			} else {

				return (double[])v;
			}
		} else {
			System.out.println("Unknown value!");
			return new double[0];
		}
	}

	public String toString() {
		return myStamp.getId();
	}
	
	public Object getVal(String columnName) {
		int colNum = stampLayer.getColumnNum(columnName);

		if (colNum>getStamp().getData().length) {
			return "Nonsense";
		}
		
		if (colNum>=0) {
			return getStamp().getData()[colNum];
		}
		
		return null;
	}
	
	// -2 is our uninitialized value, because -1 represents that the column wasn't found
	private int tipIndex = -2;
	
	private void calcTooltipIndex() {
		String tipCol=stampLayer.getParam(stampLayer.TOOLTIP_COLUMN);
		
		if (tipCol==null || tipCol.length()==0) {
			tipIndex=-1;
		} else {
			tipIndex = stampLayer.getColumnNum(tipCol);
			if (tipIndex>=0) return;
		}
		
		tipIndex=-1;
	}
	
	public String getTooltipText() {
		// TODO: Don't short circuit
		if (stampLayer.spectraData()) {
			return getData(myStamp.getData().length+2).toString();
		}

		String tipUnits=stampLayer.getParam(stampLayer.TOOLTIP_UNITS);
		if (tipIndex==-2) calcTooltipIndex();
				
		if (tipIndex==-1) return "ID: " + getId();
		
		String retStr = myStamp.getData()[tipIndex].toString();
		
		if (tipUnits!=null) {
			retStr+=" "+tipUnits;
		}
		
		return retStr;
	}
	
	
	public InvestigateData getInvestigateData(InvestigateData iData) {
		if (stampLayer.spectraData()) {
			tipIndex=myStamp.getData().length+2;
			Object val = getData(tipIndex).toString();
			
			String name;
			StampFocusPanel fp = stampLayer.viewToUpdate.myFocus;
			if(fp.outlinePanel.getExpression() == null){
				int nameIndex = fp.outlinePanel.getColorColumn();
				name = fp.table.getTableModel().getColumnName(nameIndex);
			}else{
				name = "Calculated value";
			}
			
			
			String key = getId()+"-"+name;
			
			String str = (String) val;
			if (str!=null) {
				String vals[] = str.split(",");
				
				for (String v : vals) {
					//TODO: figure out how to calculate units?
					iData.add(key, v, "", "ItalicSmallBlue","SmallBlue", true);
				}
			} else {
				iData.add(key, val.toString(), "TBD", false);
			}

			return iData;
		}		
		else{
			String tipUnits=stampLayer.getParam(stampLayer.TOOLTIP_UNITS);
			if (tipIndex==-2) calcTooltipIndex();
	
			if (tipIndex==-1) {
				iData.add("ID", myStamp.getId());
				return iData;
			}
			
			Object val = getStamp().getData()[tipIndex];
			
			try {
				if (val instanceof Number) {
					iData.add(stampLayer.viewToUpdate.myFocus.table.getTableModel().getColumnName(tipIndex), ""+getStamp().getData()[tipIndex], tipUnits, true);
				}
			} catch (NumberFormatException nfe) {
				iData.add(stampLayer.viewToUpdate.myFocus.table.getTableModel().getColumnName(tipIndex), ""+getStamp().getData()[tipIndex], tipUnits, false);			
			}
					
			return iData;
		}
	}
	
	/**
	 * Calculates and returns the center point (in lon/lat) for this stamp by averaging the points
	 */
    Point2D centerPoint=null;
    
    public Point2D getCenter() {
    	if (centerPoint==null) {
    		
    		HVector corner = new HVector();
    		
    		int validPts = 0;
    		for (int i=0; i<myStamp.getPoints().length; i=i+2) {
    			// If we have a multipolygon, there may be Double.NANs involved that will ruin our day.  Exclude them.
    			double x = myStamp.getPoints()[i];
    			double y = myStamp.getPoints()[i+1];
    			if (Double.isNaN(x) || Double.isNaN(y)) continue;
    			corner=corner.add(new HVector(new Point2D.Double(myStamp.getPoints()[i], myStamp.getPoints()[i+1])));
    			validPts++;
    		}
    		corner.div(validPts);
    		
    		centerPoint = corner.toLonLat(null);    		
    	}
    	
    	return centerPoint;
    }

	/**
	 * Returns the NW stamp corner spatial coordinates as
	 * a point in degrees: x = longitude, y = latitude.
	 */
    public Point2D getNW()
    {
    	if (nw==null) {
    		double pts[] = myStamp.getPoints();
    		nw=new Point2D.Double(pts[0], pts[1]);
    	}
    	return nw;
    }
        
    /**
     * Returns the SW stamp corner spatial coordinates as
     * a point in degrees: x = longitude, y = latitude.
     */
    
    private transient Point2D nw;
    private transient Point2D sw;
    
    public Point2D getSW()
    {
    	if (sw==null) {
    		double pts[] = myStamp.getPoints();
    		sw=new Point2D.Double(pts[pts.length-2], pts[pts.length-1]);
    	}
    	return sw;
    }
    
    private List<String> supportedTypes=null;
    
    public List<String> getSupportedTypes() {
		if (supportedTypes==null) {		
			try {
				String typeLookupStr = "ImageTypeLookup?id="+getId()+"&instrument="+stampLayer.getInstrument()+"&format=JAVA";
						
				ObjectInputStream ois =  new ObjectInputStream(StampLayer.queryServer(typeLookupStr));
				
				supportedTypes = (List<String>)ois.readObject();
				
				ois.close();
			} catch (Exception e) {
				supportedTypes=new ArrayList<String>();
				e.printStackTrace();
			}
		}
		return supportedTypes;
    }
        
    public final synchronized void clearProjectedData()
    {
        path = null;
        normalPath = null;
        bounds = null;
    }
    
    private Rectangle2D bounds;
    
    public synchronized Rectangle2D getBounds2D()
    {
        if(bounds != null)
            return  bounds;
        
        bounds = getPath().getBounds2D();
        double w = bounds.getWidth();
        if(w <= 180)
            return  bounds;
        
        double x = bounds.getX();
        double y = bounds.getY();
        double h = bounds.getHeight();
        
        x += w;
        w = 360 - w;
        
        bounds.setFrame(x, y, w, h);
        return  bounds;
    }

    GeneralPath path;

    /**
     ** Returns a (cached) normalized version of the stamp's path.
     ** @see Util#normalize360
     **/
    private Shape normalPath;
    
    public synchronized Shape getNormalPath()
    {
        if(normalPath == null)
            normalPath = normalize360(getPath());
        return  normalPath;
    }
    
    public synchronized GeneralPath getPath()
    {
        if(path == null)
        {
            path = new GeneralPath();
            Point2D pt;
                        
            double pts[] = myStamp.getPoints();
            
            boolean moveNext=true;
            boolean closePath=true;
            
            for (int i=0; i<pts.length; i=i+2) {
            	if (Double.isNaN(pts[i]) || Double.isNaN(pts[i+1])) {
            		moveNext=true;
            		closePath=false;
            		continue;
            	} else {
            		pt = Main.PO.convSpatialToWorld(pts[i],
                        pts[i+1]);
            	}
            	
                if (moveNext) {
                	path.moveTo((float)pt.getX(),
                        (float)pt.getY());
                	moveNext=false;
                } else {
//            		System.out.println("Line");
                	float x = (float) pt.getX();
                	float y = (float) pt.getY();
               		path.lineTo(x, y);
                }
            }
            
            if (closePath && pts.length>0) {
            	path.closePath();
            }
        } 
        return  path;
    }

    public boolean intersects(Rectangle2D intersectBox, Rectangle2D intersectBox2) {

    	if (stampLayer.lineShapes()) {
	        
	        double pts[] = myStamp.getPoints();
	        
	        Point2D lastPt = null;
	        Point2D curPt = null;
	        	        
	        for (int i=0; i<pts.length; i=i+2) {
	        	if (Double.isNaN(pts[i]) || Double.isNaN(pts[i+1])) {
	        		return false;
	        	} else {
	        		curPt = Main.PO.convSpatialToWorld(pts[i], pts[i+1]);
	        	}
	        	
	        	if (lastPt!=null && curPt!=null) {
	        		Shape line = normalize360(new Line2D.Double(lastPt,  curPt));
	        		if (line.intersects(intersectBox)) {
	        			return true;
	        		}
	        		if (intersectBox2!=null && line.intersects(intersectBox2)) {
	        			return true;
	        		}
	        	}
            	
        		lastPt = curPt;
	        }
    	} else {  // Why is this being called?
    		return getNormalPath().intersects(intersectBox);
    	}
    	
    	
    	return false;
    }
    
    
/**
 ** Given a shape, iterates over it and performs the given
 ** coordinate modification to every point in the shape.
 **/
private static Shape modify(Shape s, CoordModifier cm)
 {
GeneralPath gp = new GeneralPath();
PathIterator iter = s.getPathIterator(null);
float[] coords = new float[6];

// NOTE: No loss of precision in coords. All of the
// GeneralPath.foobarTo() methods take FLOATS and not doubles.

while(!iter.isDone())
 {
    switch(iter.currentSegment(coords))
     {

     case PathIterator.SEG_CLOSE:
	gp.closePath();
	break;

     case PathIterator.SEG_LINETO:
	cm.modify(coords, 2);
	gp.lineTo(coords[0], coords[1]);
	break;

     case PathIterator.SEG_MOVETO:
	cm.modify(coords, 2);
	gp.moveTo(coords[0], coords[1]);
	break;

     case PathIterator.SEG_QUADTO:
	cm.modify(coords, 4);
	gp.quadTo(coords[0], coords[1],
		  coords[2], coords[3]);
	break;

     case PathIterator.SEG_CUBICTO:
	cm.modify(coords, 6);
	gp.curveTo(coords[0], coords[1],
		   coords[2], coords[3],
		   coords[4], coords[5]);
	break;

     default:
	//log.aprintln("INVALID GENERALPATH SEGMENT TYPE!");

     }
    iter.next();
 }
return	gp;
 }

/**
 ** ONLY FOR CYLINDRICAL: Given a shape in world coordinates,
 ** "normalizes" it. This ensures that its left-most x coordinate
 ** is within the x-range [0:360], and that there is no
 ** wrap-around (that is, the shape simply pushes past 360).
 **/
public static Shape normalize360(Shape s)
 {
double x = s.getBounds2D().getMinX();
if(x < 0  ||  x >= mod)
    s = modify(s, cmModulo);

if(s.getBounds2D().getWidth() >= mod/2)
    s = modify(s, cmWrapping);

return	s;
 }


// Quick hack to allow verbatim code-reuse from GraphicsWrapped.java
private static final double mod = 360;

/**
 ** Performs the modulo operation on a shape's coordinates.
 **/
private static final CoordModifier cmModulo =
new CoordModifier()
 {
    public void modify(float[] coords, int count)
     {
	for(int i=0; i<count; i+=2)
	    coords[i] -= Math.floor(coords[i]/mod)*mod;
     }
 };

    /**
     ** Takes care of wrap-around on a shape's coordinates.
     **/
    private static final CoordModifier cmWrapping =
	new CoordModifier()
	 {
	    public void modify(float[] coords, int count)
	     {
		for(int i=0; i<count; i+=2)
		    if(coords[i] < mod/2)
			coords[i] += mod;
	     }
	 };
	 
	    /**
	     ** Interface for modifying a single coordinate of a {@link
	     ** PathIterator}.
	     **/
	    private static interface CoordModifier
	     {
		/**
		 ** @param coords The coordinate array returned by a shape's
		 ** {@link PathIterator}.
		 ** @param count The number of coordinates in the array (as
		 ** determined by the point type of the {@link PathIterator}.
		 **/
		public void modify(float[] coords, int count);
	     }


}
