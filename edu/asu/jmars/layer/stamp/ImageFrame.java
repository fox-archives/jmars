package edu.asu.jmars.layer.stamp;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import edu.asu.jmars.Main;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.Util;


/* An instance of ImageFrame represents a portion of a rendered stamp at a specific zoom level and projection.
 *
 * This class contains the logic necessary to cache the raw, non-JMARS projected data as well as the logic necessary to cache
 * the final, JMARS projected data.
 */
public class ImageFrame {
	public StampImage wholeStamp;
	public String productID;
	public String imageType;
	public Cell cell;   // This defines the extent of this frame in spatial coordinates
	
	public int frameNum;
	public int startx;  // This is the starting x position of this frame in the original non-JMARS projected image
	public int starty;  // This is the starting y position of this frame in the original non-JMARS projected image
	public int height;  // This is the height of this frame in the original non-JMARS projected image
	public int width;   // This is the width of this frame in the original non-JMARS projected image
	
	public int renderPPD;
	public int projHash;
	
	ImageProjecter ip = null;
	
	private static final DebugLog log = DebugLog.instance();
    
    ImageFrame(StampImage stampImage, String productID, String imageType, Cell cell, Rectangle srcRange, int frameNum, int renderPPD, int projHash)
    {        	
    	this.cell = cell;
        this.wholeStamp = stampImage;
        this.productID = productID;
        this.imageType = imageType;
        this.frameNum = frameNum;
        this.renderPPD = renderPPD;
        this.projHash = projHash;
        
    	width = (int)srcRange.getWidth();
   		startx=(int)srcRange.getX();    	
    	starty = (int)srcRange.getY();
    	height = (int)srcRange.getHeight();
    }
    
    /*
     * This method allows for the creation of a projected image that isn't based on the normal cell boundaries of a frame.
     * 
     * There are currently two situations where this is useful:
     *   1) When a low resolution image is viewed at a high resolution, the frame sizes become enormous.  Instead of projecting the entire frame,
     *      we just project the worldWindow, which is a more manageable size.
     *   2) When we just need a single pixel, such as for the Investigate Tool.
     *   
     *   In both cases, we ignore the usual cache.
     */
    public synchronized BufferedImage getProjectedImage(Rectangle2D worldWin) {
//    	String tfileName = wholeStamp.getInstrument()+":"+wholeStamp.productID+":"+wholeStamp.imageType+":"+frameNum+":"+worldWin;
//    	BufferedImage tImage = StampCache.readProj(tfileName, wholeStamp.isNumeric);
//    	
//    	if (tImage!=null) return tImage;
    	
    	ImageProjecter tempProjecter = new ImageProjecter(this, worldWin);
    	
    	BufferedImage tempImage = tempProjecter.getProjectedImage();
    	
//        if (tempImage != null && !wholeStamp.getInstrument().equalsIgnoreCase("davinci")) {
//        	StampCache.writeProj(tempImage, tfileName);
//        }    	
    	
    	return tempImage;
    }

    public BufferedImage getProjectedImage() {
    	if (dstImage==null) {
    		dstImage = StampCache.readProj(getProjectedFileName(), wholeStamp.isNumeric);
    	}
    	    	
    	if (dstImage==null) {
	    	if (ip==null) {
	    		ip = new ImageProjecter(this, cell.getWorldBounds());
	    	}
	    	
	    	// TODO: Does this case fail for really large images trying to do numeric lookups?
	    	dstImage = ip.getProjectedImage();
	    	
    	    // Don't cache davinci images.  We might want to create new ones with the same id, and old cache will be invalid
	        if (dstImage != null && !wholeStamp.getInstrument().equalsIgnoreCase("davinci")) {
	        	StampCache.writeProj(dstImage, getProjectedFileName());
	        }
    	}

    	return dstImage;
    }

    
    public int getWidth() {
    	return width;
    }
    
    public int getHeight() {
    	return height;
    }
    
    public int getX() {
    	return startx;
    }
    
    public int getY() {
    	return starty;
    }
        
    // This is the filename of the JMARS-projected image for this frame
    String projectedFileName = null;
    protected String getProjectedFileName()
    {
    	if (projectedFileName == null) {    	
        	projectedFileName = productID + "_" + projHash + "_" + frameNum + "_" + imageType + "_" + renderPPD;
    	}        
    	
        return projectedFileName;
    }
    
    private String urlStr=null;
    
    public String getUrlStr() {
    	if (urlStr==null) {    		
			int scale =Main.testDriver.mainWindow.getZoomManager().getZoomPPD(); 
	
	    	StampShape s = wholeStamp.myStamp;
			String instrument = wholeStamp.getInstrument();
			String type = wholeStamp.imageType;
		
	    	urlStr="ImageServer?instrument="+instrument+"&id="+s.getId();
	    	
	    	if (type!=null && type.length()>=0) {
                urlStr+="&imageType="+type;
	    	}
	    	
	    	urlStr+="&zoom="+scale;
	    	urlStr+="&startx="+startx;
	    	urlStr+="&starty="+starty;
	    	urlStr+="&height="+height;
	    	urlStr+="&width="+width;
    	}
    	return urlStr;
    }
        
	public BufferedImage dstImage;
	
    synchronized boolean hasImageLocally() {
    	if (dstImage!=null) {
    		return true;
    	}
    	
        dstImage = StampCache.readProj(getProjectedFileName(), wholeStamp.isNumeric);
        
        if (dstImage != null) {
     	   return true;
        }
        
    	if (null!=StampCache.readSrc(getUrlStr(), wholeStamp.isNumeric)) {
    		return true;
    	}
    	    	    	
    	return false;
    }
    
    public BufferedImage image = null; 
    
	public BufferedImage loadSrcImage()
	{
	    try {
	    	if (image!=null) return image;
	    	
	    	boolean numeric = wholeStamp.isNumeric;
	    	
        	String cacheLoc=getUrlStr();
        	        	        	
        	image = StampCache.readSrc(getUrlStr(), numeric);
        	        	
        	if (image==null) {             
        		image = StampImageFactory.loadImage(cacheLoc, numeric);
        		if (image!=null) {
        			StampCache.writeSrc(image, cacheLoc);
        		}

                // Skipping this step makes non-raster images look washed out
        		if (!numeric && image!=null && image.getAlphaRaster()==null) {
        			image = Util.makeBufferedImage(image);
        		}
        	} 
	    }
	    catch (Throwable e) {
	        log.aprintln(e);
	    }
	    
	    return image;
	}
	
	
}