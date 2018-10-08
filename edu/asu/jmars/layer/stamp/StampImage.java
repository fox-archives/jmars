package edu.asu.jmars.layer.stamp;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import edu.asu.jmars.Main;
import edu.asu.jmars.ProjObj.Projection_OC;
import edu.asu.jmars.layer.stamp.StampLayer.StampTask;
import edu.asu.jmars.layer.stamp.StampLayer.Status;
import edu.asu.jmars.layer.stamp.projection.EquiRectangular;
import edu.asu.jmars.layer.stamp.projection.SimpleCylindrical;
import edu.asu.jmars.layer.stamp.projection.PolarStereographic;
import edu.asu.jmars.layer.stamp.projection.Projection;
import edu.asu.jmars.layer.stamp.projection.Sinusoidal;
import edu.asu.jmars.layer.stamp.projection.Unprojected;
import edu.asu.jmars.util.HVector;
import edu.asu.jmars.util.IgnoreComposite;
import edu.asu.jmars.util.Util;

public class StampImage 
{ 	
	// A value that will be used throughout the stamp layer to indicate numeric pixels that should be drawn transparently
	// (For graphic images, 0 is frequently used to indicate transparent pixels)
	public static final int IGNORE_VALUE = Short.MIN_VALUE;
	
    protected String productID;
    protected String label;
    
    protected ImageFrame frames[];
    
    // These values represent how many lines, in the full resolution image, relate to each subFrame for the frames we generate ourselves.
    // The goal is for the scaled image pieces we receive from the server to be as close to 500x500 pixels as is reasonably possible.
    protected int linesPerFrame;
    protected int linesLastFrame;
    
    protected int samplesPerFrame;
    protected int samplesLastFrame;
    
    protected int projHash;
    protected int renderPPD;
        
    protected String imageType="";
     
    // Numeric variables
    protected double minValue=Double.NaN;
    protected double maxValue=Double.NaN;
    
    protected double autoMin=Double.NaN;
    protected double autoMax=Double.NaN;
    protected boolean autoValuesChanged=false;
    
    protected boolean isNumeric = false;
    protected String units = "";
    protected String unitDesc = "";
    
    protected boolean isPDSImage = false;
    protected boolean isTHEMISDCS = false;
    protected boolean realFramePoints = false;
    
    public String getImageType() {
    	return imageType;
    }
    
    public StampImage(StampShape shape, String productId, String instrument, String imageType, BufferedImage image, HashMap<String,String> params) {
    	myStamp=shape;
    	this.instrument = instrument;
    	this.imageType = imageType;
        this.productID = productId;
        label = null;                
        this.image = image;
        
        projectionParams = params;
        parseProjectionParams();
        	
    	if (instrument.equalsIgnoreCase("davinci")) {
    		fullImageLocal=true;
    		pts=new Point2D.Double[4];
    		pts[0]=new Point2D.Double(360-Double.parseDouble(params.get("lon0")), Double.parseDouble(params.get("lat0")));
    		pts[1]=new Point2D.Double(360-Double.parseDouble(params.get("lon1")), Double.parseDouble(params.get("lat1")));
    		pts[2]=new Point2D.Double(360-Double.parseDouble(params.get("lon3")), Double.parseDouble(params.get("lat3")));        	
    		pts[3]=new Point2D.Double(360-Double.parseDouble(params.get("lon2")), Double.parseDouble(params.get("lat2")));
    		
    		if (image!=null) {
    			numLines = image.getHeight();
    			numSamplesPerLine = image.getWidth();
    		}    		
    	} else {
    		// TODO: Do we need to call this for projected images?  Could we calculate points on our own?
    		getPoints();
    	}
    	
        if (map_projection_type.equalsIgnoreCase("EQUIRECTANGULAR")) {        	
       		imageProjection = new EquiRectangular(line_proj_offset, sample_proj_offset, center_lon, center_lat, map_resolution);
        } else if (map_projection_type.equalsIgnoreCase("POLAR_STEREO")) {
        	imageProjection = new PolarStereographic(line_proj_offset, sample_proj_offset, center_lon, center_lat, map_scale, radius);
        } else if (map_projection_type.equalsIgnoreCase("SINUSOIDAL")) { 
        	imageProjection = new Sinusoidal(line_proj_offset, sample_proj_offset, center_lon, center_lat, map_resolution);
        } else if (map_projection_type.equalsIgnoreCase("CYLINDRICAL")) {
        	imageProjection = new SimpleCylindrical(numLines, numSamplesPerLine, pts[0], pts[1], pts[2], pts[3]);
        } else {
        	imageProjection = new Unprojected(numLines, numSamplesPerLine, pts[0], pts[1], pts[2], pts[3]);
        }
    }
    
    BufferedImage image = null;
    
    StampShape myStamp = null;
    
    Projection imageProjection = null;
        
    protected String instrument;
    
    public String getInstrument() {
    	return instrument;
    }
        
    public String getLabel()
    {
        return  label;
    }
    
    public String getUnits() {
    	return units;
    }
    
    public String getUnitDesc() {
    	return unitDesc;
    }
    
    boolean fullImageLocal = false;
    /**
     * Returns true if the entire image is stored locally at full resolution in a single file.
     * Returns false by default, and if we are working with individual subsampled tiles of an image.
     * @return
     */
    public boolean isFullImageLocal() {
    	return fullImageLocal;
    }
    
    boolean framePointsFaked=false;
    
    /**
     ** Generates a new set of geometry points to subdivide a single 
     ** frame of geometry points into the specified number of divisions.
     ** Frames are created along lower-to-upper axis.
     ** <p>
     ** NOTE: This method uses the approximate interpolation scheme
     **       present in the HVector class.  Should probably only be used
     **       for client-side image subtiling purposes when more accurate
     **       data is unavailable from an image geometry database, etc.
     **
     ** @param pts  an array of four points corresponding to the four
     **             corners of an image frame:
     ** 
     ** <ul>          
     ** <li>          points[0]   lower left corner coordinates
     ** <li>          points[1]   lower right corner coordinates
     ** <li>          points[2]   upper left corner coordinates
     ** <li>          points[3]   upper right corners coordinates
     ** </ul>
     **
     ** @return array of points organized as described for #getPoints(),
     **         whole image frame + subframes
     **
     **/
    protected Point2D[] getNewFakeFramePoints(Point2D[] pts)
    {
        if (pts == null ||
                pts.length != 4) {
        	// Nothing more we can do here.
            return pts;
        }

        double maxPPD = getMaxRenderPPD();

    	if (renderPPD > maxPPD) {
        	linesPerFrame = LINES_PER_FRAME;
    	} else {
        	linesPerFrame = (int)Math.ceil(LINES_PER_FRAME * (maxPPD/renderPPD));        		
    	}
    	
    	if (linesPerFrame > getNumLines()) {
    		linesPerFrame=getNumLines();
    	}
    	        	
    	// This is only the number of frames per column, we'll determine 
    	// how to split (or not split) horizontally later
        int numFrames = (int)Math.ceil(1.0* getNumLines() / linesPerFrame);
        
        Point2D[] newPoints = new Point2D[numFrames * 4];
        
        // If there's only 1 frame, this makes the linesLastFrame==linesPerFrame.
    	linesLastFrame = getNumLines() - linesPerFrame*(numFrames-1);

    	if (realFramePoints || numFrames == 1) {
    		newPoints = pts;
    		// TODO: Just because we didn't fake frames, doesn't mean we couldn't
    		// still act like we did.  THEMIS might be the only exception
    		// And not necessarily all THEMIS?
//    		framePointsFaked=false;
    	} else {    	
	        // Prepare uppper part of first subframe.
	        Point2D nextUL = pts[2];
	        Point2D nextUR = pts[3];
	        
	        // Create image subframe geometry points through
	        // interpolation from whole image.  Do this
	        // for all but the last subframe.  Frames start
	        // from upper part of image.
	        for (int i=0; i < numFrames-1; i++) {
	            // Store geometry for new subframe
	            newPoints[i*4]   = imageProjection.lonLat((int)(linesPerFrame * (i+1)), 1, new Point2D.Double());
	            newPoints[i*4+1] = imageProjection.lonLat((int)(linesPerFrame * (i+1)), numSamplesPerLine, new Point2D.Double());
	            newPoints[i*4+2] = nextUL;
	            newPoints[i*4+3] = nextUR;
	            
	            // Prepare upper part of next subframe
	            nextUL = newPoints[i*4];
	            nextUR = newPoints[i*4+1];
	        }
	        
	        // Create last subframe using residual part of whole image frame
	        newPoints[(numFrames-1) * 4]   = pts[0];
	        newPoints[(numFrames-1) * 4+1] = pts[1];
	        newPoints[(numFrames-1) * 4+2] = nextUL;
	        newPoints[(numFrames-1) * 4+3] = nextUR;
	                
	        framePointsFaked=true;
	        
	        for(int i=0; i<newPoints.length-4; i+=4) {
	            newPoints[i+0] = newPoints[i+6] = midpoint(newPoints[i+0], newPoints[i+6]);
	            newPoints[i+1] = newPoints[i+7] = midpoint(newPoints[i+1], newPoints[i+7]);
	        }
    	}
    	       return splitFrames(newPoints);
    }
        
    int horizontalSplitCnt=1;
    
    private static final int LINES_PER_FRAME=500;
    
    private Point2D[] splitFrames(Point2D pts[]) {
    	// TODO: Maybe this should be always calculated and returned by the server
    	double maxPPD = getMaxRenderPPD();
    	    	
    	if (renderPPD > maxPPD) {
        	samplesPerFrame = LINES_PER_FRAME;
    	} else {
        	samplesPerFrame = (int)Math.ceil(LINES_PER_FRAME * (maxPPD/renderPPD));        		
    	}

    	if (samplesPerFrame > getNumSamples()) {
    		samplesPerFrame=getNumSamples();
    	}    	

    	horizontalSplitCnt=(int)Math.ceil(1.0*getNumSamples()/samplesPerFrame);
    	
    	if (horizontalSplitCnt<1) {
    		horizontalSplitCnt=1;
    	}

        // If there's only 1 frame, this makes the linesLastFrame==linesPerFrame.
    	samplesLastFrame = getNumSamples() - samplesPerFrame*(horizontalSplitCnt-1);

    	if (horizontalSplitCnt==1) {
    		// No more work to do
    		return pts;
    	}
    	    	
        int numFrames =pts.length/4;
        
        Point2D[] newPoints = new Point2D[horizontalSplitCnt*numFrames * 4];
                
//        double framePercent = 1.0 / horizontalSplitCnt;
        
        int total_samples=getNumSamples();
        
        // Create image subframe geometry points through
        // interpolation from whole image.  Frames start
        // from upper part of image.
        for (int i=0; i < numFrames; i++) {        	
            Point2D[] topRow = new Point2D[horizontalSplitCnt+1];
            Point2D[] botRow = new Point2D[horizontalSplitCnt+1];
            
            for (int n=0; n<(horizontalSplitCnt+1); n++) {
            	if (n==horizontalSplitCnt) { // last column
	        		if (i==numFrames-1) { // last row
	            		topRow[n]=imageProjection.lonLat(i*linesPerFrame, total_samples, new Point2D.Double());
	            		botRow[n]=imageProjection.lonLat(numLines, total_samples, new Point2D.Double());
	        		} else {
	            		topRow[n]=imageProjection.lonLat(i*linesPerFrame, total_samples, new Point2D.Double());
	            		botRow[n]=imageProjection.lonLat((i+1)*linesPerFrame, total_samples, new Point2D.Double());
	        		}            		
            	} else {
	        		if (i==numFrames-1) { // last row
	            		topRow[n]=imageProjection.lonLat(i*linesPerFrame, (samplesPerFrame*n), new Point2D.Double());
	            		botRow[n]=imageProjection.lonLat(numLines, (samplesPerFrame*n), new Point2D.Double());
	        		} else {
	            		topRow[n]=imageProjection.lonLat(i*linesPerFrame, (samplesPerFrame*n), new Point2D.Double());
	            		botRow[n]=imageProjection.lonLat((i+1)*linesPerFrame, (samplesPerFrame*n), new Point2D.Double());
	        		}
            	}
            }            
            
            int newFrameCnt = topRow.length-1;
            
            for (int j=0; j<newFrameCnt; j++) {
            	int newFrameNum = horizontalSplitCnt*i+j;

            	newPoints[(4*newFrameNum)   ]	= botRow[j];
            	newPoints[(4*newFrameNum) +1]	= botRow[j+1];
            	newPoints[(4*newFrameNum) +2]	= topRow[j];
            	newPoints[(4*newFrameNum) +3]	= topRow[j+1];
            }            
        }

        return newPoints;    	
    }    

    
    
    
    Point2D[] pts = null;
    
    public Point2D[] getPoints() { 
    	if (pts==null) {
    		try {
    			String urlStr = "PointFetcher?id="+productID+"&instrument="+getInstrument();
    			
    			if (imageType!=null && imageType.length()>0) {
    				urlStr+="&imageType="+imageType;
    			}
            
    			ObjectInputStream ois = new ObjectInputStream(StampLayer.queryServer(urlStr));
        
    			double dpts[] = (double[])ois.readObject();
    		               		   
    			pts = new Point2D[dpts.length/2]; 
    	        
	    	    for (int i=0; i<pts.length; i++) {
	    	    	pts[i]=new Point2D.Double(dpts[2*i], dpts[2*i+1]);
	    	    }
	    	    
	    	    //skip this for loop if this is for a radar layer (SHARAD)
	    	    if(!myStamp.stampLayer.lineShapes()){
		    	    // This is used to try and blend real frame points for THEMIS images, 
		    	    // so that there isn't a gap between frames.
		    	    if (imageType!=null && imageType.length()>0 && pts.length>4) {
			            for(int i=0; i<pts.length-4; i+=4) {
			                pts[i+0] = pts[i+6] = midpoint(pts[i+0], pts[i+6]);
			                pts[i+1] = pts[i+7] = midpoint(pts[i+1], pts[i+7]);
			            }
		    	    }
	    	    }
	    	    
	    	    ois.close();
    		} catch (Exception e) {
    			e.printStackTrace();
    		}    		
    	}
    	
    	return pts;
    }
    
    static Point2D midpoint(Point2D a, Point2D b)
    {
        return  new HVector(a).add(new HVector(b)).toLonLat(null);
    }
    
    double maxPPD = -1;
    
    protected double getMaxRenderPPD() {
    	if (map_resolution!=0) return map_resolution;
    	
    	if (maxPPD==-1) {    	
			Point2D points[] = getPoints();
			
			double degrees = distance(points[0], 
					                      points[1]);
				
			int pixels = getNumSamples();
			
			maxPPD = pixels / degrees;
    	}
    	
    	return maxPPD;
    }
    
	public double distance(Point2D pointA, Point2D pointB)
	 {
		double lonA = Math.toRadians(pointA.getX());
		double latA = Math.toRadians(pointA.getY());

		double lonB = Math.toRadians(pointB.getX());
		double latB = Math.toRadians(pointB.getY());

		
		HVector a = new HVector(Math.cos(latA)*Math.cos(-lonA),
				Math.cos(latA)*Math.sin(-lonA),
				Math.sin(latA));
				
		HVector b = new HVector(Math.cos(latB)*Math.cos(-lonB),
				Math.cos(latB)*Math.sin(-lonB),
				Math.sin(latB));
		
		return  Math.toDegrees(a.separation(b));
	 }
    
    /**
     ** Renders the image onto the given (world-coordinate) graphics context.
     **/
    public synchronized void renderImage(final Graphics2D wg2, final FilledStampImageType fs,
                            final StampLView.DrawFilledRequest request, StampTask task, Point2D offset, BufferedImage target)
    {
        task.updateStatus(Status.RED);
        try {
            final Rectangle2D worldWin = request.getExtent();
            
            // If stamps have been nudged, we need to make sure that we take the nudge into account when determining whether or not a
            // particular imageframe intersects the visible part of the screen or not
            final Rectangle2D offsetWorldWin=new Rectangle2D.Double();;
            offsetWorldWin.setRect(worldWin.getX()-offset.getX(), worldWin.getY()-offset.getY(), worldWin.getWidth(), worldWin.getHeight());
            
            double maxRenderPPD=getMaxRenderPPD();
            
            int renderPPD = request.getPPD();
            
            // TODO: Revisit this
//            if (!map_projection_type.equalsIgnoreCase("UNPROJECTED") && renderPPD>maxRenderPPD) {
//            	renderPPD=(int)maxRenderPPD;
//            }
            
            if(frames == null || renderPPD != this.renderPPD || 
                    projHash != Main.PO.getProjectionSpecialParameters().hashCode()) {
            	recreateImageFrames(renderPPD);
            }

            List<ImageFrame> framesInView = new ArrayList<ImageFrame>();
            for(int i=0; i<frames.length; i++) {        
            	if (doesFrameIntersect(frames[i], offsetWorldWin)) {
            		framesInView.add(frames[i]);
            	}
            }

//            if (framePointsFaked && !getInstrument().equalsIgnoreCase("davinci")) {
//            	// Only expand if we're dealing with projected images
//            	boolean expand = !map_projection_type.equalsIgnoreCase("UNPROJECTED");
//
//            	// TODO: Review this code very closely.  It becomes very expensive for high resolution images zoomed way in, such as 
//            	// HiRISE at 262144 ppd.  Ends up being 14000 frames it works on, in an attempt to optimize network retrieval that may not even
//            	// be necessary.  Maybe check frames in view, then check if they have data locally, and then segment anything that remains somehow?
//            	ImageFrame frameSegmentsToFetch[][][] = FrameFetcher.segment(frames, horizontalSplitCnt, frames.length/horizontalSplitCnt, offsetWorldWin, expand);
//
//            	for (int i=0; i<frameSegmentsToFetch.length; i++) {        	
//            		FrameFetcher ff = new FrameFetcher(frameSegmentsToFetch[i]);
//            		ff.fetchFrames();
//            	}
//            }

            task.updateStatus(Status.YELLOW);

            int loopCnt=0;
            restart: while(true) {
            	loopCnt++;
            	if (loopCnt>framesInView.size()+1) {
            		System.out.println("Autoscale values didn't converge");
            		break;
            	}
	            for(ImageFrame f : framesInView) {
	            	if (!request.changed()) {
	            		autoValuesChanged=false;
	           			drawFrame(f, offsetWorldWin, wg2, fs, target, request);
	           			if (autoValuesChanged) {
	           				// Abort and restart this loop
	           				continue restart;
	           			}
	            	} else {
	            		// Parameters changed, abort frame draw
	            		return;
	            	}
	            }
	            break;
            }
        } finally {
            task.updateStatus(Status.DONE);
        }
    }
    
    // Draw this frame onto the specified g2.  Draw it multiple times if
    // necessary due to worldwrap.  (How often are we really going to be
    // zoomed out enough to actually worry about this for stamps?)
    private void drawFrame(ImageFrame frame, Rectangle2D worldWin, Graphics2D wg2, FilledStampImageType fs, BufferedImage target, final StampLView.DrawFilledRequest request) {
        final double base = Math.floor(worldWin.getMinX() / 360.0) * 360.0;
        
        final int numWorldSegments = (int) Math.ceil (worldWin.getMaxX() / 360.0) - (int) Math.floor(worldWin.getMinX() / 360.0);
        
        Rectangle2D.Double where = new Rectangle2D.Double();
        
        where.setRect(frame.cell.getWorldBounds());
        
        double origX = where.x;
        
        int start = where.getMaxX() < 360.0 ? 0 : -1;
        
        for(int m=start; m<numWorldSegments; m++) {
            where.x = origX + base + 360.0*m;
            if(worldWin.intersects(where)) {
            	Graphics2D g2 = getFrameG2(wg2);
            	
        		int screenWidth = (int)(worldWin.getWidth()*renderPPD);
        		int screenHeight = (int)(worldWin.getHeight()*renderPPD);
        		
        		int dstW = (int) Math.ceil(where.getWidth()  * renderPPD);
        		int dstH = (int) Math.ceil(where.getHeight() * renderPPD);
        		
        		Rectangle2D regionToProject = null;
        		
        		// TODO: The current stamp projection mechanism is inefficient in that cell bounds get expanded to rectangles, which ultimately overlap
        		// with each other.  These overlapping areas then get processed for each frame, resulting in unnecessary work.  In some cases this excess
        		// work is trivial, in others is can be substantial.  Map layer style tiling may be appropriate, but it still has to be done on an image
        		// by image basis, taking individual image offsets into consideration, as the entire stamp layer is very dynamic in nature.
        		
        		int frame_limit = (int) Math.max(screenWidth, screenHeight)*4;   // This is completely arbitrary and should probably be tuned. 
        		
        		BufferedImage image;
        		        		
        		if (dstW > frame_limit || dstH > frame_limit) {
        			// When we're zoomed really far into a low resolution image (which can happen intentionally or inadvertently when multiple datasets 
        			// are in use), the buffered image for the frame can become gigantic, even though the user only sees a tiny fraction of it.
        			// Just project the bounds of the screen, as it's going to be significantly smaller than a frame
        			regionToProject = worldWin;  
        			image = frame.getProjectedImage(worldWin);
        		} else {
        			image = frame.getProjectedImage();
        			regionToProject = where;
        		}
        		
        		if (request.changed()) return;
        		
        		g2.transform(Util.image2world(image.getWidth(), image.getHeight(), regionToProject));
            	
				if (target!=null) {
					Point2D src = new Point2D.Float();
					Point2D dst = new Point2D.Float();
					
					AffineTransform at=null;
					
					try {
						at = g2.getTransform().createInverse();
					} catch (NoninvertibleTransformException e) {
						e.printStackTrace();
					}
					
					int width=target.getWidth();
					int height=target.getHeight();
					
					for (int i=0; i<width;i++) {
						for (int j=0; j<height;j++) {
							dst.setLocation(i, j);
							at.transform(dst, src);
														
							double srcX = src.getX();
							double srcY = src.getY();
							
							if (srcX<0 && srcX>-0.5) srcX=0;
							if (srcY<0 && srcY>-0.5) srcY=0;
							
							if (srcX>=image.getWidth() && srcX<image.getWidth()+0.5) srcX=image.getWidth()-1;
							if (srcY>=image.getHeight() && srcY<image.getHeight()+0.5) srcY=image.getHeight();
							
							if (srcX>=image.getWidth()) continue;
							if (srcY>=image.getHeight()) continue;
							if (srcX<0) continue;
							if (srcY<0) continue;

							try {
								double sample = image.getRaster().getSampleFloat((int)Math.floor(srcX), (int)Math.floor(srcY), 0);
									
								if (sample<-100000) sample=IGNORE_VALUE;
									
								if (Double.isNaN(sample)) sample=IGNORE_VALUE;

								if (sample==IGNORE_VALUE) {
									// In the case where we're drawing overlapping stamps, do NOT replace actual data from a previous stamp 
									// with transparent pixels from this one
									if (target.getRaster().getSample(i, j, 0)!=0) continue;
								}
								target.getRaster().setSample(i, j, 0, sample);
							} catch (Exception e3) {
								System.out.println("OUT OF BOUNDS: src.getX() " + src.getX() + " : " + src.getY());
								continue;
							}
						}
					}
				} else {
					if (request.changed()) return;
					
					if (isNumeric) {
						try {
							BufferedImage image3 = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);

							// Ignore all black pixels for numeric stamp data
							Graphics2D g3 = image3.createGraphics();
							g3.setComposite(new IgnoreComposite(Color.black));
							
							// Filter using the FloatingPointOp first
			                FloatingPointImageOp op2 = new FloatingPointImageOp(this);														
							g3.drawImage(image, op2, 0, 0);
							
							BufferedImageOp op = fs.getColorMapOp(image3).forAlpha(1);
							// Then apply the ColorOp to give the user the normal controls
							g2.drawImage(image3, op, 0, 0);
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else {
				    	if (ignore_value!=-1) {
							BufferedImage image2 = Util.createCompatibleImage(image, image.getWidth(), image.getHeight());
							
							Graphics2D g3 = image2.createGraphics();
							g3.setComposite(new IgnoreComposite(Color.black));
							g3.drawImage(image, null, 0, 0);
							image = image2;
				    	}
				    	
						BufferedImageOp op = fs.getColorMapOp(image).forAlpha(1);
						g2.drawImage(image, op, 0, 0);
					}					
				}
            }
        }                    	
    }
    
    public static boolean doesFrameIntersect(ImageFrame frame, Rectangle2D worldWin) {
    	if (frame==null) return false;  // Can occur when testing whether to expand tile requests
    	
        final double base = Math.floor(worldWin.getMinX() / 360.0) * 360.0;
        
        final int numWorldSegments =
            (int) Math.ceil (worldWin.getMaxX() / 360.0) -
            (int) Math.floor(worldWin.getMinX() / 360.0);
        
        Rectangle2D.Double where = new Rectangle2D.Double();
        
        where.setRect(frame.cell.getWorldBounds());
        double origX = where.x;
        
        int start = where.getMaxX() < 360.0 ? 0 : -1;
        
        for(int m=start; m<numWorldSegments; m++) {
            where.x = origX + base + 360.0*m;
            if(worldWin.intersects(where)) {
            	return true;
            }
        }                   
        
        return false;
    }
    
    // currently unused
    Area clipArea=null;
    
    // The area of this stamp generated using the render points
    Area realClipArea = null;
    
    // The realClipArea of this stamp minus the overlapping areas of any
    // other rendered stamps that are higher in the view stack
    // Needed for multithreaded rendering, unused at the moment
    Area currentClipArea = null;
    
    // Not sure this needs to be public
    public Area getRealClipArea() {
    	if (realClipArea==null) {
    		realClipArea=new Area(getNormalPath());
    	}
    	
    	return realClipArea;  	
    }
    
    public void clearCurrentClip() {
    	currentClipArea=null;
    }
    
    public void calculateCurrentClip(List<StampImage> higherImages) {
    	// Short circuited until multithreaded rendering is enabled.
    	if (true) return;
    	if (currentClipArea==null) {
	    	currentClipArea = getAdjustedClipArea(getRealClipArea());
	    	
	    	for (StampImage s : higherImages) { 		
	    		// Somehow we need to worry about +/- 360 issues here too
	    		if (s.getRealClipArea().intersects(realClipArea.getBounds2D())) {
	    			currentClipArea.subtract(s.getRealClipArea());
	    		}    		
	    	}    	
    	}
    }
    
    public Area getCurrentClipArea() {
    	return currentClipArea;
    }
    
    
    public Area getAdjustedClipArea(Area startingArea) {
		Area newArea = new Area(startingArea);
		
    	// Handle the cases where wrapped coordinates get us into +360 or -360
		// space.
        AffineTransform transformer2 = new AffineTransform();
        transformer2.translate(360, 0);
    	Area area2 = ((Area) newArea.clone());
    	area2.transform(transformer2);
    	
        AffineTransform transformer3 = new AffineTransform();
        transformer3.translate(-360, 0);
    	Area area3 = ((Area) newArea.clone());
    	area3.transform(transformer3);

        AffineTransform transformer4 = new AffineTransform();
        transformer4.translate(720, 0);
    	Area area4 = ((Area) newArea.clone());
    	area4.transform(transformer4);
    	
        AffineTransform transformer5 = new AffineTransform();
        transformer5.translate(-720, 0);
    	Area area5 = ((Area) newArea.clone());
    	area5.transform(transformer5);

        AffineTransform transformer6 = new AffineTransform();
        transformer6.translate(1080, 0);
    	Area area6 = ((Area) newArea.clone());
    	area6.transform(transformer6);
    	
        AffineTransform transformer7 = new AffineTransform();
        transformer7.translate(-1080, 0);
    	Area area7 = ((Area) newArea.clone());
    	area7.transform(transformer7);
	    	
    	Area returnArea = new Area(newArea);
    	returnArea.add(area2);
    	returnArea.add(area3);
    	returnArea.add(area4);
    	returnArea.add(area5);
    	returnArea.add(area6);
    	returnArea.add(area7);    		
    	
    	return returnArea;
    }
    
    /**
	 * Creates a copy of the given Graphics2D and prepares it for rendering
	 * frames of this image type.
	 */    
    protected final java.awt.Graphics2D getFrameG2(Graphics2D g2) {
    	g2 = (Graphics2D) g2.create();
    	
    	// This functionality is now implemetned slightly differently to avoid making data transparent when the grayscale slider is adjusted
//    	if (ignore_value!=-1) {
//    		g2.setComposite(new IgnoreComposite(Color.black));
//    	} else 
    	if (clip_to_path) {
    		Shape path=myStamp.getNormalPath();
    		
    		path=getAdjustedClipArea(new Area(path));
    		
    		g2.setClip(path);
    	}
    	
    	// Something like this is needed for multi-threaded rendering support.
    	// g2.setClip(getCurrentClipArea());
    	
    	return g2;
    }
    
    /**
     ** Not quite sure what this does: see implementation in
     ** PdsImage. -- Joel Hoff
     **/
    public int[] getHistogram() throws IOException
    {
        return null;
    }
                           
    public int getHeight()
    {
    	if (image!=null) {
            return image.getHeight();    		
    	} else {
    		return getNumLines();
    	}
    }
    
    public int getWidth()
    {
    	if (image!=null) {
    		return image.getWidth();
    	} else {
    		return getNumSamples();
    	}
    }
    
    // This block moved over from StampShape - it's tied to imageType,
    // not generic by Shape...
    private int numLines=Integer.MIN_VALUE;
    private int numSamplesPerLine=Integer.MIN_VALUE;
    
    public int getNumLines() {
    	if (numLines==Integer.MIN_VALUE) {
    		getFullImageSize();
    	}
    	return numLines;
    }
    
    public int getNumSamples() {
    	if (numSamplesPerLine==Integer.MIN_VALUE) {
    		getFullImageSize();
    	}
    	return numSamplesPerLine;
    }
    
    private void getFullImageSize() {
		try {
            String sizeLookupStr = "ImageSizeLookup?id="+productID+"&instrument="+getInstrument()+"&imageType="+imageType+"&format=JAVA";
					
			ObjectInputStream ois = new ObjectInputStream(StampLayer.queryServer(sizeLookupStr));
			
			Integer samples = (Integer)ois.readObject();
			Long lines = (Long)ois.readObject();
			
			// Not sure why I ever thought lines needed to be a Long.  An image that's 2 BILLION lines long is... big.
			numLines = lines.intValue();
			numSamplesPerLine = samples.intValue();
			
			ois.close();
		} catch (Exception e) {
			e.printStackTrace();
			numLines=0;
			numSamplesPerLine=0;
			e.printStackTrace();
		}
    }
    
    
    // Returns 32-bit RGB color value at specified pixel location; may
    // include alpha component.
    synchronized public int getRGB(int x, int y) throws Exception 
    {
        // TODO: THEMIS VIS image products, particularly BWS and RGB, don't
        // follow the normal rules for how long the last frame is, causing us to
        // throw an exception while rendering, but still display all of the data
        // correctly.  This check prevents that exception.  This should be
        // properly fixed at some point - probably when THEMIS images are
        // rendered via normal projection logic.
    	if (x>image.getWidth() || y>image.getHeight()) {
    		return 0;
    	}
        return image.getRGB(x, y);
    }
    
    synchronized public float getFloatVal(HVector ptVect)
    {
        if (frames==null) {
        	recreateImageFrames();
        }
        
        for (ImageFrame frame : frames) {
            Point2D.Double unitPt = new Point2D.Double();
            
            if (frame==null || frame.cell==null) {
            	continue; 
            }
            
            frame.cell.uninterpolate(ptVect, unitPt);
            
            // Check whether point falls within cell.
            if (unitPt.x >= 0  &&  unitPt.x <= 1  &&
                unitPt.y >= 0  &&  unitPt.y <= 1  )
            {
            	
            	Point2D worldPoint = new Point2D.Double(ptVect.toWorld().getX(), ptVect.toWorld().getY());
				int ppd = renderPPD;
								
				double pixelWidth = 1.0 / ppd;
				
				double x = worldPoint.getX();
				double y = worldPoint.getY();
				
				// get tile range of this wrapped piece				
				double xstart = Math.floor(x / pixelWidth) * pixelWidth;
				double ystart = Math.floor(y / pixelWidth) * pixelWidth;
					
				Rectangle2D tileExtent = new Rectangle2D.Double(xstart, ystart, 1d/ppd, 1d/ppd);
				
				BufferedImage image2 = frame.getProjectedImage(tileExtent);
				
				float v2 = image2.getRaster().getSampleFloat(0, 0,  0);
				return v2;            	
            }
        }
        
        return Float.NaN;
    }
                                           
    // Convenience method, to avoid scattering this silly string of object references all over the code just to get the current zoomPPD
    protected synchronized void recreateImageFrames() {
    	int zoomPPD = myStamp.stampLayer.viewToUpdate.viewman.getZoomManager().getZoomPPD();
    	recreateImageFrames(zoomPPD);
    	
    }
    
    protected synchronized void recreateImageFrames(int renderPPD)
    {
        projHash = Main.PO.getProjectionSpecialParameters().hashCode();
        this.renderPPD = renderPPD;
        autoMin=Double.NaN;
        autoMax=Double.NaN;
        autoValuesChanged=false;

        Point2D[] pts = getPoints();
                
   		if (pts.length>4 && realFramePoints) {
   			// TODO: Maybe these should also be server side paramters?
			if (myStamp.getId().startsWith("I")) {
				// Native resolution, IR images are all 256 pixels per frame.  Frame points are provided for us though
				linesPerFrame=256;
				linesLastFrame=getHeight()%256;
				if (linesLastFrame==0) linesLastFrame=256;
			} else {
				// VIS is 192 lines per frame, adjusted for summing mode 
				linesPerFrame=192/summing;
				linesLastFrame=linesPerFrame;
			}
			
			samplesPerFrame=getWidth();
			samplesLastFrame=getWidth();
   		} else {
   			pts = getNewFakeFramePoints(pts);   			
   		}
   		
        int frameCount = pts.length / 4;
        frames = new ImageFrame[frameCount];
                
    	for (int i=0; i<frameCount; i++) {	    		      
    		
    		// The last horizonalSplitCnt frames are the last row
    		boolean lastRow= i>=frames.length-horizontalSplitCnt;
    		
    		int frameHeight;
    		if (lastRow) {
    			frameHeight=(int)linesLastFrame;
    		} else {
    			frameHeight=(int)linesPerFrame;
    		}
    		
	        Rectangle srcRange;
	        
	        boolean lastCol= (i%horizontalSplitCnt)==horizontalSplitCnt-1;
	        
	        int frameWidth;
	        if (lastCol) {
	        	frameWidth = samplesLastFrame;
	        } else {
	        	frameWidth = samplesPerFrame;
	        }
	        
	        int startx = (i%horizontalSplitCnt) * samplesPerFrame;

	        int starty = (i/horizontalSplitCnt) * linesPerFrame;
	        
        	srcRange = new Rectangle(startx, starty, frameWidth, frameHeight);	        	
	        
	        Cell frameCell = new Cell(
	                 new HVector(pts[i*4]),
	                 new HVector(pts[i*4+1]),
	                 new HVector(pts[i*4+3]),
	                 new HVector(pts[i*4+2]), (Projection_OC)Main.PO);
	                
	        frames[i] = new ImageFrame(this, productID, imageType, frameCell, srcRange, i, renderPPD, projHash);	                             
    	}
    }
        
    double line_proj_offset = 0;
    double map_resolution = 0;
    double map_scale = 0;
    double sample_proj_offset = 0;
    double center_lon = 0;
    double center_lat = 0;
    double radius = 0;  // TODO Set this to an appropriate default
    String map_projection_type = "UNPROJECTED";

    boolean clip_to_path = false;
    long ignore_value = -1;
    
    // Used by THEMIS VIS for BWS images
    int summing=1;
    
    HashMap<String,String> projectionParams=null;
        
    /**
     * Return a copy of the projection params, for display to the user or other purposes
     * @return
     */
    public HashMap<String,String> getProjectionParams() {
    	return (HashMap<String,String>)projectionParams.clone();
    }
    
    public void parseProjectionParams() {
		// TODO: Make this not break if any of these parameters aren't present
    	   map_projection_type = "UNPROJECTED";
    	   
    	   if (projectionParams.containsKey("map_projection_type")) {
    		   map_projection_type = projectionParams.get("map_projection_type");
    	   }

    	   if (projectionParams.containsKey("clip_to_path")) {
    		   String ctp=projectionParams.get("clip_to_path");
    		   if (ctp.equalsIgnoreCase("true")) {
    			   clip_to_path=true;
    		   }
    	   }

    	   if (projectionParams.containsKey("ignore_value")) {
    		   String ignore_str=projectionParams.get("ignore_value");
    		   ignore_value = Long.parseLong(ignore_str);
    	   }
    	   
    	   if (projectionParams.containsKey("lines")) {
    		   numLines = Integer.parseInt(projectionParams.get("lines"));
    	   }
    	   
    	   if (projectionParams.containsKey("samples")) {
    		   numSamplesPerLine = Integer.parseInt(projectionParams.get("samples"));
    	   }
    	   
    	   if (projectionParams.containsKey("summing")) {
    		   summing = Integer.parseInt(projectionParams.get("summing"));
    	   }
    	   
    	   if (projectionParams.containsKey("numeric")) {
    		   isNumeric = true;
    	   }
    	   
    	   // New options, to avoid hardcoding things like 'THEMIS' - ABR, BTR, PBT
    	   if (projectionParams.containsKey("pdsImage")) {
    		   isPDSImage = true;
    	   }
    	   
    	   // Older THEMIS DCS images.  New images are properly projected... I think.
    	   if (projectionParams.containsKey("themisDCS")) {
    		   isTHEMISDCS = true;
    	   }    	   
    	   
    	   // Did we download the entire image, or just a subset
    	   // True for old THEMIS BWS images, and possibly others
    	   if (projectionParams.containsKey("fullImageLocal")) {
    		   fullImageLocal = true;
    	   }
    	   
    	   // We have tie points for every frame, so don't need to interpolate fake ones (usually means THEMIS VIS)
    	   if (projectionParams.containsKey("realFramePoints")) {
    		   realFramePoints = true;
    	   }

    	   if (projectionParams.containsKey("units")) {
    		   units = projectionParams.get("units");
    	   }

    	   if (projectionParams.containsKey("unit_desc")) {
    		   unitDesc = projectionParams.get("unit_desc");
    	   }

    	   if (projectionParams.containsKey("minValue")) {
    		   minValue = Double.parseDouble(projectionParams.get("minValue"));
    	   }

    	   if (projectionParams.containsKey("maxValue")) {
    		   maxValue = Double.parseDouble(projectionParams.get("maxValue"));
    	   }

    	   if (map_projection_type.equalsIgnoreCase("CYLINDRICAL")) {
    		   return;
    	   }

    	   if (map_projection_type.equalsIgnoreCase("UNPROJECTED")) {
    		   return;
    	   }
    	   
    	   // TODO: Add logic to behave gracefully if all of these parameters aren't present
    	   line_proj_offset = Double.parseDouble(projectionParams.get("line_projection_offset"));
    	   map_resolution = Double.parseDouble(projectionParams.get("map_resolution"));
    	   sample_proj_offset = Double.parseDouble(projectionParams.get("sample_projection_offset"));
    	   center_lon = Double.parseDouble(projectionParams.get("center_longitude"));
    	   map_scale = Double.parseDouble(projectionParams.get("map_scale"));
    	   center_lat = Double.parseDouble(projectionParams.get("center_latitude"));
    	   
    	   // TODO: If we get here and don't have a radius, should be abort somehow?
    	   radius = Double.parseDouble(projectionParams.get("radius"));
    }

    /**
     ** Returns a (cached) normalized version of the stamp's path.
     ** @see Util#normalize360
     **/
    private Shape normalPath;
    
    public synchronized Shape getNormalPath()
    {
        if(normalPath == null)
            normalPath = StampShape.normalize360(getPath());
        return  normalPath;
    }
    
    GeneralPath path;

	// This is the path of the rendered shape - note that this may go outside the
	// bounds of the stamp polygon, and may include more area than we want to 
	// make visible to the user.
	public synchronized GeneralPath getPath()
	{
	    if(path == null)
	    {
	        path = new GeneralPath();
	        Point2D pt;
	                    
	        Point2D pts[] = getPoints();
	
	        // trace a line down the left edge
	        for (int i=2; i<pts.length; i=i+4) {
	            pt = Main.PO.convSpatialToWorld(pts[i].getX(),
	                    pts[i].getY());
	
	            if (i==2) {
	            	path.moveTo((float)pt.getX(),
	                    (float)pt.getY());
	            } else {
	            	path.lineTo((float)pt.getX(),
	                        (float)pt.getY());                	
	            }
	        }
	
	        pt = pts[pts.length-4];
	        pt = Main.PO.convSpatialToWorld(pt.getX(),
	                pt.getY());
	        
	    	path.lineTo((float)pt.getX(),
	                (float)pt.getY());                	
	
	        // and then back up the right edge
	        for (int i=pts.length-3; i>0; i=i-4) {
	            pt = Main.PO.convSpatialToWorld(pts[i].getX(),
	                    pts[i].getY());
	
	           	path.lineTo((float)pt.getX(), (float)pt.getY());                	
	        }            
	
	        pt = pts[3];            
	        pt = Main.PO.convSpatialToWorld(pt.getX(),
	                pt.getY());
	        path.lineTo((float)pt.getX(), (float)pt.getY());
	
	        path.closePath();
	    } 
	    return  path;
	}	
}