package edu.asu.jmars.layer.tes6;

// TODO: This should be made an instance of SerializingThread and
// stuff other than drawing should be taken out.

import java.util.*;
import java.awt.*;
import java.awt.geom.*;
import java.sql.*;

import edu.asu.jmars.Main;
import edu.asu.jmars.util.*;
import edu.asu.jmars.layer.tes6.*;

public class TesLViewWorker extends Thread {
    public TesLViewWorker(TesLView lView){
        super("TesLViewWorker-"+(lView.getChild() == null?"Panner":"Main"));
        this.lView = lView;
        this.layer = (TesLayer)lView.getLayer();
        outstanding = new LinkedList();
        eventScope = (lView.getChild() == null)?
            RunningStateChangeEvent.SCOPE_PANNER:
            RunningStateChangeEvent.SCOPE_MAIN;
        
        st = new ShapeTransformer(lView);

        runningStateCoordinator = layer.getRunningStateCoordinator();
    }


    public void run(){
        while(true){
            try {
                Object req = getNextRequest();
                runningStateCoordinator.fireRunningStateChangeEvent(
                    this,eventScope, RunningStateChangeEvent.EVENT_START);

                if (req instanceof DrawReq){
                    processDraw((DrawReq)req);
                }
                else {
                    throw new IllegalArgumentException("Unknown request type: "+req);
                }
            }
            catch(Exception ex){
                ex.printStackTrace();
            }
            finally{
                log.println("TesLViewWorker: Done with current request. About to get next request.");
                runningStateCoordinator.fireRunningStateChangeEvent(
                    this,eventScope, RunningStateChangeEvent.EVENT_END);
            }
        }
    }

    public void putDrawRequest(TesContext ctx, TesContext.CtxImage ctxImage, Rectangle2D viewport){
        log.println("Enequeued a draw request for ctx: "+ctxImage.getTitle());
        DrawReq drawReq = new DrawReq(ctx, ctxImage, viewport);
        enqueueRequest(drawReq);
    }

    private void processDraw(DrawReq req)
        throws SQLException
    {
        double ppd = lView.getPPD();

        if (req.ctxImage.getDrawReal().booleanValue()){
        	processDrawDetPolys(req, true);
        }
        else {
        	if (ppd <= TesLView.MAX_PPD_FP_CENTERS){
        		processDrawFpCenters(req);
        	}
        	else if (ppd <= TesLView.MAX_PPD_DET_CENTERS){
        		processDrawDetCenters(req);
        	}
        	else { // TesLView.PPD_INTERP_POLYS || TesLView.PPD_REAL_POLYS
        		processDrawDetPolys(req, (ppd <= TesLView.MAX_PPD_INTERP_POLYS));
        	}
        }
    }

    private boolean hilightRegions = Config.get("tes.showGrid", false);
    private static final Color regionColor = Color.yellow;
    private static final Color regionActiveColor = Color.magenta;
    private static final Color regionDoneColor = Color.white;


    private ColorData[] getColorDataInOrder(
        TesContext.CtxImage ctxImage,
        Color defaultColor,
        RegionDesc region,
        int nPolys
        )
        throws SQLException
    {
        ColorBy colorBy = ctxImage.getColorBy();
        ColorData[] colorData = new ColorData[nPolys];

        if (colorBy == null || colorBy.field == null){
            // return defaultColor for all polys
            for(int i = 0; i < nPolys; i++){
                colorData[i] = new ColorData(defaultColor, true);
            }
        }
        else {
            // get the color field data in order
            Object[] colorFieldData =
                layer.getFieldDataInOrder(ctxImage.getWhereClause(),
                                          ctxImage.getOrderByClause(),
                                          colorBy.field,
                                          region);
            DataToColor dataToColor =
                new DataToColor(colorBy.field, colorBy.minVal, colorBy.maxVal, defaultColor);
            
            for(int i = 0; i < nPolys; i++){
                if (colorFieldData == null || colorFieldData[i] == null){
                    colorData[i] = new ColorData(defaultColor, false);
                }
                else {
                    colorData[i] = new ColorData(dataToColor.getColor(colorFieldData[i]), true);
                }
            }
        }

        return colorData;
    }

    private boolean reqSuperceeded(DrawReq req){
        // Make sure that our request has not been superceeded.
        if (!req.viewport.equals(lView.getViewport())){
            // a pan/zoom has occurred
            return true;
        }
        if (req.ctx != lView.getContext()){
            // active context has changed
            return true;
        }
        if (req.ctxImage.getLastDestructiveChangeTime() != req.ctx.getLastDestructiveChangeTime()){
            // something changed in the context which will require a
            // redraw of the entire viewable data
            return true;
        }
        if (hasMoreRequests()){
        	// new requests are outstanding, abandon the current request
        	return true;
        }

        return false;
    }

    private void processDrawDetPolys(DrawReq req, boolean realPolys)
        throws SQLException
    {
    	double ppd = lView.getPPD();
        long stTime = System.currentTimeMillis(); // get draw start time

        lView.clearOffScreenDataBuffer(req.ctx);
        lView.clearOffScreenGridBuffer(req.ctx);
        lView.repaintSelection(false);

        Graphics2D g2DataWorld = lView.getOffScreenDataBuffer(req.ctx);
        Graphics2D g2GridWorld = lView.getOffScreenGridBuffer(req.ctx);
        
        if (g2DataWorld == null || g2GridWorld == null){
            log.println("ctx: "+req.ctxImage.getTitle()+","+
                        " g2DataWorld: "+g2DataWorld+","+
                        " g2GridWorld: "+g2GridWorld);
            if (g2DataWorld != null){ g2DataWorld.dispose(); }
            if (g2GridWorld != null){ g2GridWorld.dispose(); }
            return;
        }
        
        // Get a set of regions that fall within this viewport.
        Set regionsInView = lView.getRegionsUnderViewport(lView.getViewport());
        ArrayList iterationSeq = new ArrayList(regionsInView);
        Collections.shuffle(iterationSeq);

        // don't get color data if we are in foot-print mode
        ColorBy colorBy = req.ctxImage.getColorBy();
        if (hilightRegions){
            // draw region grid -- for debugging purpose
            drawRegionSet(regionsInView, regionColor, g2GridWorld, ppd);
            lView.repaint();
        }

        // draw actual data records
        int nr = 0; // number of regions drawn
        int n = 0;  // number of polys drawn
        GeneralPath recyclablePoly = new GeneralPath(); // as the name suggests, recyclable poly
        long timeBefore, timeAfter;
        
        for(Iterator i = iterationSeq.iterator(); i.hasNext(); ){
            // Make sure that our request has not been superceeded.
            if (reqSuperceeded(req)){
                // CAUTION: Cannot do the following over here - the buffers
                // may have already been deleted in the layer due
                // to TesLayer.removeContext().
                // lView.clearOffScreenDataBuffer(req.ctx);
                // lView.clearOffScreenGridBuffer(req.ctx);
                break;
            }

            nr++; // increment the number of regions processed
            
            RegionDesc region = (RegionDesc)i.next();
            GeneralPath regionBoundary = region.getRegionBoundary();
            GeneralPath regionBoundaryWorld =
                ShapeUtils.spatialEastLonPolyToWorldPoly(
                    lView, regionBoundary);

            if (hilightRegions){
                // highlight the region -- for debugging purpose
                drawRegionInWorldCoords(
                    g2GridWorld,
                    regionActiveColor,
                    region.getRegionId(),
                    regionBoundaryWorld,
                    ppd);
                lView.repaint();
            }

            // set a clipping region so that nothing gets drawn outside the current region.
            g2DataWorld.clip(regionBoundaryWorld);

            // get the polygonal outlines of the data
            timeBefore = System.currentTimeMillis();
            SerializablePoly[] polys = layer.getPolysInOrder(req.ctxImage.getWhereClause(),
                                                        req.ctxImage.getOrderByClause(),
                                                        region,
														!req.ctxImage.getDrawReal().booleanValue());
            timeAfter = System.currentTimeMillis();
            log.println("["+(lView.getChild()==null?"panner":"main")+"] "+
            		(timeAfter-timeBefore)/1000f+" sec consumed by "+
            		"layer.getPolysInOrder("+
            		req.ctxImage.getWhereClause()+","+
            		req.ctxImage.getOrderByClause()+","+
            		region.getRegionId()+")");

            // get color data in order
            timeBefore = timeAfter;
            ColorData[] colorData = getColorDataInOrder(req.ctxImage,
                                                        defColor,
                                                        region,
                                                        polys.length);
            timeAfter = System.currentTimeMillis();
            log.println("["+(lView.getChild()==null?"panner":"main")+"] "+
            		(timeAfter-timeBefore)/1000f+" sec consumed by "+
            		"layer.getColorDataInOrder() for region="+region.getRegionId()+
            		" for "+polys.length+" polys");
            
            // actually draw the region data
            timeBefore = timeAfter;
            g2DataWorld.setStroke(singlePixelStroke);
            for(int j = 0; j < polys.length; j++){
                if (polys[j] != null){
               		g2DataWorld.setColor(colorData[j].getColor());

                    ShapeUtils.copyFrom(recyclablePoly, polys[j].getBackingPoly());
                    st.toWorldInPlace(recyclablePoly);
                    if (colorData[j].getFill()){
                        g2DataWorld.fill(recyclablePoly);
                    }
                    else {
                    	if (req.ctxImage.getDrawNull().booleanValue()){
                    		g2DataWorld.draw(recyclablePoly);
                    	}
                    }
                }
                n++; // increment the number of polygons processed
            }
            timeAfter = System.currentTimeMillis();
            log.println("["+(lView.getChild()==null?"panner":"main")+"] "+
            		(timeAfter-timeBefore)/1000f+" sec consumed by "+
            		" drawing of "+polys.length+" polys");

            // reset the clipping region
            g2DataWorld.setClip(null);

            if (hilightRegions){
                // unhighlight the region -- for debugging purpose
                drawRegionInWorldCoords(
                    g2GridWorld,
                    regionDoneColor,
                    region.getRegionId(),
                    regionBoundaryWorld,
                    ppd);
            }

            // Update the display so that the user sees some progress.
            // The full draw may take a while.
            lView.repaint();
        }

        // get rid of various resources in use
        g2DataWorld.dispose();
        g2GridWorld.dispose();
        lView.repaint();

        log.println("Draw of "+nr+"/"+regionsInView.size()+" regions "+
                    n+" polys done in "+
                    (System.currentTimeMillis()-stTime)/1000.0f+
                    " seconds");
    }
    
    private void processDrawDetCenters(DrawReq req)
    	throws SQLException
    {
    	double ppd = lView.getPPD();
    	long stTime = System.currentTimeMillis();
    	
        lView.clearOffScreenDataBuffer(req.ctx);
        lView.clearOffScreenGridBuffer(req.ctx);

        Graphics2D g2DataWorld = lView.getOffScreenDataBuffer(req.ctx);
        Graphics2D g2GridWorld = lView.getOffScreenGridBuffer(req.ctx);
        
        if (g2DataWorld == null || g2GridWorld == null){
            log.println("ctx: "+req.ctxImage.getTitle()+","+
                        " g2DataWorld: "+g2DataWorld+","+
                        " g2GridWorld: "+g2GridWorld);
            if (g2DataWorld != null){ g2DataWorld.dispose(); }
            if (g2GridWorld != null){ g2GridWorld.dispose(); }
            return;
        }

        Set regionsInView = lView.getRegionsUnderViewport(lView.getViewport());
        ArrayList iterationSeq = new ArrayList(regionsInView);
        Collections.shuffle(iterationSeq);


        if (hilightRegions){
            // draw region grid -- for debugging purpose
            drawRegionSet(regionsInView, regionColor, g2GridWorld, ppd);
            lView.repaint();
        }

        // draw actual data records
        int nr = 0; // number of regions drawn
        int n = 0;  // number of polys drawn
        for(Iterator i = iterationSeq.iterator(); i.hasNext(); ){
            // Make sure that our request has not been superceeded.
            if (reqSuperceeded(req)){ break; }

            nr++; // increment the number of regions processed

            RegionDesc region = (RegionDesc)i.next();
            GeneralPath regionBoundary = region.getRegionBoundary();
            GeneralPath regionBoundaryWorld = 
                ShapeUtils.spatialEastLonPolyToWorldPoly(lView, regionBoundary);

            // highlight the region -- for debugging purpose
            if (hilightRegions){
                drawRegionInWorldCoords(
                    g2GridWorld,
                    Color.magenta,
                    region.getRegionId(),
                    regionBoundaryWorld,
                    ppd);
                lView.repaint();
            }

            // set a clipping region so that nothing gets drawn outside the current region.
            g2DataWorld.clip(regionBoundaryWorld);

            // get the center points of the detectors
            Point2D[] detCenters = layer.getDetCentersInOrder(
            		req.ctxImage.getWhereClause(),
            		req.ctxImage.getOrderByClause(),
            		region);
            
            // get color data in order
            ColorData[] colorData = getColorDataInOrder(req.ctxImage,
                                                        defColor,
                                                        region,
                                                        detCenters.length);
            // actually draw the region data
            Point2D pWorld;
            g2DataWorld.setColor(defColor);
            for(int j = 0; j < detCenters.length; j++){
                if (detCenters[j] != null){
                    pWorld = ShapeUtils.spatialEastLonToWorld(lView, detCenters[j]);
                    
                    g2DataWorld.setColor(colorData[j].getColor());
                    g2DataWorld.setStroke(singlePixelStroke);

                    if (colorData[j].getFill()){
                        g2DataWorld.draw(new Line2D.Float(
                                (float)pWorld.getX(),
                                (float)pWorld.getY(),
                                (float)pWorld.getX(),
                                (float)pWorld.getY()));
                    }
                    else {
                    	if (req.ctxImage.getDrawNull().booleanValue()){
                            g2DataWorld.draw(new Line2D.Float(
                                    (float)pWorld.getX(),
                                    (float)pWorld.getY(),
                                    (float)pWorld.getX(),
                                    (float)pWorld.getY()));
                    	}
                    }
                }
                n++; // increment the number of polygons processed
            }

            // reset the clipping region
            g2DataWorld.setClip(null);

            // unhighlight the region -- for debugging purpose
            if (hilightRegions){
                drawRegionInWorldCoords(
                    g2GridWorld,
                    Color.white,
                    region.getRegionId(),
                    regionBoundaryWorld,
                    ppd);
            }

            // Update the display so that the user sees some progress.
            // The full draw may take a while.
            lView.repaint();

        }

        // get rid of various resources in use
        g2DataWorld.dispose();
        g2GridWorld.dispose();
        lView.repaint();

        log.println("Draw of "+nr+"/"+regionsInView.size()+" regions "+
                    n+" polys done in "+
                    (System.currentTimeMillis()-stTime)/1000.0f+
                    " seconds");
    }

    private void processDrawFpCenters(DrawReq req)
        throws SQLException
    {
    	double ppd = lView.getPPD();
        long stTime = System.currentTimeMillis();

        lView.clearOffScreenDataBuffer(req.ctx);
        lView.clearOffScreenGridBuffer(req.ctx);

        Graphics2D g2DataWorld = lView.getOffScreenDataBuffer(req.ctx);
        Graphics2D g2GridWorld = lView.getOffScreenGridBuffer(req.ctx);
        
        if (g2DataWorld == null || g2GridWorld == null){
            log.println("ctx: "+req.ctxImage.getTitle()+","+
                        " g2DataWorld: "+g2DataWorld+","+
                        " g2GridWorld: "+g2GridWorld);
            if (g2DataWorld != null){ g2DataWorld.dispose(); }
            if (g2GridWorld != null){ g2GridWorld.dispose(); }
            return;
        }

        Set regionsInView = lView.getRegionsUnderViewport(lView.getViewport());


        if (hilightRegions){
            // draw region grid -- for debugging purpose
            drawRegionSet(regionsInView, regionColor, g2GridWorld, ppd);
            lView.repaint();
        }

        // draw actual data records
        int nr = 0; // number of regions drawn
        int n = 0;  // number of polys drawn
        for(Iterator i = regionsInView.iterator(); i.hasNext(); ){
            // Make sure that our request has not been superceeded.
            if (reqSuperceeded(req)){ break; }

            nr++; // increment the number of regions processed

            RegionDesc region = (RegionDesc)i.next();
            GeneralPath regionBoundary = region.getRegionBoundary();
            GeneralPath regionBoundaryWorld = 
                ShapeUtils.spatialEastLonPolyToWorldPoly(lView, regionBoundary);

            // highlight the region -- for debugging purpose
            if (hilightRegions){
                drawRegionInWorldCoords(
                    g2GridWorld,
                    Color.magenta,
                    region.getRegionId(),
                    regionBoundaryWorld,
                    ppd);
                lView.repaint();
            }

            // set a clipping region so that nothing gets drawn outside the current region.
            g2DataWorld.clip(regionBoundaryWorld);

            // get the polygonal outlines of the data
            Point2D[] fpCenters = layer.getFpCenters(req.ctxImage.getWhereClause(), region);

            // actually draw the region data
            Point2D pWorld;
            g2DataWorld.setColor(defColor);
            g2DataWorld.setStroke(singlePixelStroke);
            for(int j = 0; j < fpCenters.length; j++){
                if (fpCenters[j] != null){
                    pWorld = ShapeUtils.spatialEastLonToWorld(lView, fpCenters[j]);
                    g2DataWorld.draw(new Line2D.Float(
                    		(float)pWorld.getX(),(float)pWorld.getY(),
                    		(float)pWorld.getX(),(float)pWorld.getY()));
                }
                n++; // increment the number of polygons processed
            }

            // reset the clipping region
            g2DataWorld.setClip(null);

            // unhighlight the region -- for debugging purpose
            if (hilightRegions){
                drawRegionInWorldCoords(
                    g2GridWorld,
                    Color.white,
                    region.getRegionId(),
                    regionBoundaryWorld,
                    ppd);
            }

            // Update the display so that the user sees some progress.
            // The full draw may take a while.
            lView.repaint();

        }

        // get rid of various resources in use
        g2DataWorld.dispose();
        g2GridWorld.dispose();
        lView.repaint();

        log.println("Draw of "+nr+"/"+regionsInView.size()+" regions "+
                    n+" polys done in "+
                    (System.currentTimeMillis()-stTime)/1000.0f+
                    " seconds");
    }

    public void drawRegionInWorldCoords(
        Graphics2D g2World,
        Color c,
        Integer regionId,
        GeneralPath regionBoundaryWorld,
        double ppd
    )
    {
        g2World.setPaint(c);
        g2World.setStroke(singlePixelStroke);
        
        // GeneralPath bbox = (GeneralPath)regionBoundaryEastLon.clone();
        // ShapeUtils.eastToWestLon(bbox);
        // bbox = ShapeUtils.spatialPolyToWorldPoly(lView, bbox);
        
        GeneralPath bbox = regionBoundaryWorld;
        Rectangle2D r = bbox.getBounds2D();

        float desiredFontSize = 0.7f; // 1.0f;
        // float height = (float)(desiredFontSize/lView.getViewMan().getProj().getPixelSize().getHeight());
        float height = (float)(desiredFontSize*ppd);

        double cx = r.getCenterX();
        double cy = r.getCenterY();
        double h = r.getHeight();

        Font font = new Font("Helvetica", Font.PLAIN, (int)height);
        //Font font = new Font("Helvetica", Font.PLAIN, 24);
        g2World.setFont(font);

        g2World.draw(regionBoundaryWorld);
        g2World.drawString(regionId.toString(),
                           (float)cx-(desiredFontSize/2.0f)*regionId.toString().length(),
                           (float)cy-desiredFontSize/2.0f);
    }


    public void drawRegionSet(Set regionSet, Color color, Graphics2D g2World, double ppd){
        RegionDesc region;
        GeneralPath regionBoundary, regionBoundaryWorld;

        for(Iterator i = regionSet.iterator(); i.hasNext();){
            region = (RegionDesc)i.next();

            regionBoundaryWorld = 
                ShapeUtils.spatialEastLonPolyToWorldPoly(
                    lView, region.getRegionBoundary());

            drawRegionInWorldCoords(
                g2World,
                color,
                region.getRegionId(),
                regionBoundaryWorld,
                ppd);
        }
    }


    private void drawRegion(Graphics2D g2, Color c, Integer regionId, GeneralPath regionBoundaryEastLon){
        g2.setPaint(c);
        g2.setStroke(singlePixelStroke);
        
        GeneralPath bbox = (GeneralPath)regionBoundaryEastLon.clone();
        ShapeUtils.eastToWestLon(bbox);
        Rectangle2D r = bbox.getBounds2D();

        float desiredFontSize = 1.0f;
        float height = (float)(desiredFontSize/lView.getViewMan().getProj().getPixelSize().getHeight());

        double cx = r.getCenterX();
        double cy = r.getCenterY();
        double h = r.getHeight();

        Font font = new Font("Helvetica", Font.PLAIN, (int)height);
        g2.setFont(font);

        g2.draw(bbox);
        g2.drawString(regionId.toString(),
                      (float)cx+(desiredFontSize/2.0f)*regionId.toString().length(),
                      (float)cy-desiredFontSize/2.0f);
    }

    private synchronized void enqueueRequest(Object req){
        // Add the request to the outstanding requests queue.
        outstanding.add(req);

        // Signal the worker thread to pickup this request and run with it.
        notify();
    }

    private synchronized Object getNextRequest(){
        while(outstanding.size() == 0){
            try { wait(); }
            catch(InterruptedException ex){
                log.println("TesLViewWorker - Interrupted.");
            }
        }
        return outstanding.removeFirst();
    }
    
    // more requests are queued in
    private synchronized boolean hasMoreRequests(){
    	return (outstanding.size() > 0);
    }

    private TesLView   lView = null;
    private TesLayer   layer = null;
	private ShapeTransformer st;
	
    private LinkedList outstanding = null;

    RunningStateCoordinator runningStateCoordinator = null;
    private int        eventScope; // status tracking

    private static final Stroke singlePixelStroke = new BasicStroke(0.0f);
    private static final Stroke o25Stroke = new BasicStroke(0.25f);
    private static Color defColor = new Color(1.0f,1.0f,1.0f);
    private static DebugLog log = DebugLog.instance();


    private final class DrawReq {
        public DrawReq(TesContext ctx, TesContext.CtxImage ctxImage, Rectangle2D viewport){
            this.ctx = ctx;
            this.ctxImage = ctxImage;
            this.viewport = viewport;
        }
        
        TesContext ctx;
        TesContext.CtxImage ctxImage;
        Rectangle2D viewport;

        public String toString(){
            return "DrawReq["+ctxImage.getTitle()+","+viewport.toString()+"]";
        }
    }

    private final class SelectReq {
        public SelectReq(TesContext ctx, TesContext.CtxImage ctxImage, Point2D p){
            this.ctx = ctx;
            this.ctxImage = ctxImage;
            this.p = p;
        }

        TesContext ctx;
        TesContext.CtxImage ctxImage;
        Point2D p;

        public String toString(){
            return "SelectReq["+ctxImage.getTitle()+","+p.toString()+"]";
        }
    }


}

class ColorData {
    public ColorData(Color c, boolean fill){
        this.c = c;
        this.fill = fill;
    }
    
    public Color getColor(){ return c; }
    public boolean getFill(){ return fill; }
    
    private Color c;
    private boolean fill;
}

class DataToColor {
    public DataToColor(FieldDesc field, Object minVal, Object maxVal, Color defaultColor){
        this.field = field;
        this.minVal = minVal;
        this.maxVal = maxVal;
        this.defaultColor = defaultColor;
        
        if (this.field.getFieldType() == Byte.class ||
            this.field.getFieldType() == Short.class ||
            this.field.getFieldType() == Integer.class ||
            this.field.getFieldType() == Long.class || 
            this.field.getFieldType() == Float.class ||
            this.field.getFieldType() == Double.class){

            isNumeric = true;
            if (minVal != null){ minValNumeric = ((Number)minVal).doubleValue(); }
            else { minValNumeric = 0; }
            if (maxVal != null){ maxValNumeric = ((Number)maxVal).doubleValue(); }
            else { maxValNumeric = 0; }
        }
        else {
            isNumeric = false;
            if (minVal != null){ minValNumeric = stringVal(minVal.toString()); }
            else { minValNumeric = 0; }
            if (maxVal != null){ maxValNumeric = stringVal(maxVal.toString()); }
            else { maxValNumeric = 0; }
        }
    }
    
    public Color getColor(Object val){
        if (isNumeric){
            if ((maxValNumeric-minValNumeric) == 0) {
                return defaultColor;
            }
            double v = ((Number)val).doubleValue();
            double c = v-minValNumeric;
            c = (v-minValNumeric) / (maxValNumeric-minValNumeric);
            
            // clamp values
            if (c < 0){ c = 0; }
            if (c > 1){ c = 1; }
            
            // generate new color value
            return new Color((float)c,(float)c,(float)c);
        }
        else {
            if (maxValNumeric == minValNumeric){
                return defaultColor;
            }
            double v = stringVal(val.toString());
            double c = v-minValNumeric;
            c = (v-minValNumeric) / (maxValNumeric-minValNumeric);
            
            // clamp values
            if (c < 0){ c = 0; }
            if (c > 1){ c = 1; }

            // generate a new color value
            return new Color((float)c,(float)c,(float)c);
        }
    }

    private double stringVal(String s){
        double val = 0;
        byte[] b = s.getBytes();
        
        // TODO: deal with possibly negative bytes in the string value
        //       which should not happen in standard ascii text.
        for(int i = 0; i < b.length; i++){
            val += (((double)b[i])/Math.pow(2.0,((i+1)*8)));
        }

        return val;
    }

    
    FieldDesc field;
    Object minVal;
    Object maxVal;
    Color defaultColor;
    
    boolean isNumeric;
    double  minValNumeric, maxValNumeric;

}

