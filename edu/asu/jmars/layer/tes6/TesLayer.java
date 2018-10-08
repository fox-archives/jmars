// Realization: tes6
//
// Version where the (boresight) center (lon,lat) are retrieved
// from the database. Detector corner offsets (from boresight
// center) are stored in the client. The center table is used
// only to determine whether an observation was in the
// ascending or descending node of the orbit.
//
//
package edu.asu.jmars.layer.tes6;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.event.EventListenerList;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import edu.asu.jmars.layer.DataReceiver;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;

public class TesLayer extends Layer implements RunningStateChangeListener {
	private static DebugLog log = DebugLog.instance();
	
	public TesLayer(){
		// Initialize contexts holder.
		contexts = new Vector<TesContext>();

        // Keep track of the active context.
		activeContext = null;

        // Initialize events list for listeners registered with the Layer.
        listenerList = new EventListenerList();

        // Initialize L1-cache.
        l1Cache = CacheKeeper.instance().checkout(this);

        // Listen to state change events to properly color the status LED.
        runningStateCoordinator = RunningStateCoordinator.getInstance();
        runningStateCoordinator.addRunningStateChangeListener(this);

        // get an instance of various database utilities and connection pool
        dbUtils = DbUtils.getInstance();

        // get revision of data and see if cache needs cleaning.
        try {
        	final String dataRevIdKey = "___DataRevisionId___";
        	String dbDataRevision = dbUtils.getDataRevision();
        	Element cacheDataRevElement = l1Cache.get(dataRevIdKey);
        	if (!((cacheDataRevElement != null && cacheDataRevElement.getValue().equals(dbDataRevision)) || 
        			(cacheDataRevElement == null && dbDataRevision == null))){
        		log.println("Data revision change detected. Clearing cache.");
            	l1Cache.removeAll();
            	if (dbDataRevision != null){
            		cacheDataRevElement = new Element(dataRevIdKey, dbDataRevision);
            		cacheDataRevElement.setEternal(true);
            		l1Cache.put(cacheDataRevElement);
            	}
        	}
        }
        catch(SQLException ex){
        	log.aprintln("WARNING! Unable to get data revision due to exception: "+ex);
        }
        
        // get polygon field encoding
        polyFieldEnc = dbUtils.getPolyFieldEnc();

        // create basic minimum field descriptors - TesLayer clients
        // may also use this data.
        createEssentialFieldDesc();

        // - intialize a foot-print calculator object
        tesFpCalculator = TesInterpolatedFpCalculator.getInstance();

		// init various static data values
        initStaticData();
	}
	
	protected void finalize() throws Throwable {
		if (l1Cache != null)
			CacheKeeper.instance().checkin(this);
		super.finalize();
	}

    private void createEssentialFieldDesc(){
        // TODO: table names etc should go into a preferences object
        detKeyField = new FieldDesc(
            "detid", "", dbUtils.dataTable, false, false, false,
            Integer.class);

        detEssFields = new FieldDesc[0];

        fpKeyField = new FieldDesc(
            "fpid", "", dbUtils.dataTable, true, false, false,
            Integer.class);

        fpEssFields = new FieldDesc[2];
        fpEssFields[0] = new FieldDesc(
            "blon", "", dbUtils.dataTable, true, false, false,
            Short.class);
        fpEssFields[1] = new FieldDesc(
            "blat", "", dbUtils.dataTable, true, false, false,
            Short.class);

        polyField = new FieldDesc(
            "poly", "", dbUtils.polyTable, false, false, false,
            Object.class);

        regionField = new FieldDesc(
            "region", "", dbUtils.dataTable, false, false, false,
            Short.class);

        ockField = new FieldDesc(
            "ock", "", dbUtils.dataTable, false, false, false,
            Short.class);
        
        ickField = new FieldDesc(
            "ick", "", dbUtils.dataTable, false, false, false,
            Short.class);

        detField = new FieldDesc(
            "det", "", dbUtils.dataTable, false, false, false,
            Byte.class);
        
        scanLenField = new FieldDesc(
    		"scan_len", "", dbUtils.dataTable, true, false, false,
    		String.class);
        
        colorField = new FieldDesc(
        		"C", "", null, false, false, false,
        		Color.class);
    }

    public void initStaticData(){
        // - get a list of regions from the database
        if (regionDescMap == null){
            regionDescMap = new HashMap<Integer,RegionDesc>();
            fillRegionDescMap();
        }

    }


    /**
     * Unused place-holder implementation of {@link Layer.receiveRequest}.
     */
    public void receiveRequest(Object o, DataReceiver r){ }


    /**
     * Creates a context with the specified criteria and add it to the layer.
     * <p>
     * Note: called by the swing thread only.
     * <p>
     *
     * @param title    Title of the context
     * @param fields   A list of fields that the user is interested in
     * @param selects  A list of selection criteria as field value ranges
     * @param orderBys A list of ordering criteria to order the displayed data
     * @param colorBy  A coloring criteria to color the data
     * @param drawReal Always draw real data
     * @param drawNull Draw null data as outlines
     * @return         A context object containing the information supplied.
     * 
     * @see FieldDesc
     * @see RangeDesc
     * @see OrderCriterion
     * @see ColorBy
     * @see TesContext
     */
    public TesContext addContext(
        String title,
        Vector<FieldDesc> fields,
        Vector<RangeDesc> selects,
        Vector<OrderCriterion> orderBys,
        ColorBy colorBy,
		Boolean drawReal,
		Boolean drawNull)
    {
        TesContext ctx = new TesContext(title);
        ctx.setFields(fields);
        ctx.setSelects(selects);
        ctx.setOrderCriteria(orderBys);
        ctx.setColorBy(colorBy);
        ctx.setDrawReal(drawReal);
        ctx.setDrawNull(drawNull);

        return addContext(ctx);
    }
    
    public TesContext addContext(TesContext ctx){
        // Add the given context to the list of contexts
        contexts.add(ctx);

        // notify everybody
        fireContextAddedEvent(ctx, ctx.getCtxImage());

        // Any changes made to the context must be done through the TesLayer.
        return ctx;
    }

    public void removeContext(TesContext ctx){
    	if (ctx == activeContext)
    		setActiveContext(null);
    	
        contexts.remove(ctx);
        fireContextRemovedEvent(ctx, ctx.getCtxImage());
    }

    /**
     * Makes the specified context active. This results in drawing the data for
     * this context. At any given time, there is only one active context.
     * <p>
     * Note: Called by swing thread only.
     * <p>
     *
     * @param ctx  The context to set active.
     * @see        TesContext
     */
    // called by the swing thread only: what happens when active context is deleted
    public void setActiveContext(TesContext ctx){
        // Broadcast this change
        TesContext oldCtx = activeContext;
        activeContext = ctx;

        if (ctx == null){
            log.println("No context set to active.");
        }
        else {
            log.println("Context "+ctx.toString()+" being activated.");
        }

        fireContextActivatedEvent(
            oldCtx,
            ((oldCtx != null)? oldCtx.getCtxImage(): null),
            activeContext,
            ((activeContext != null)? activeContext.getCtxImage(): null));
    }
    
    /**
     * Returns the currently active thread.
     * <p>
     * Node: Called by swing thread only.
     * <p>
     * @return The active context or <code>null</code>.
     */
    public TesContext getActiveContext(){
    	return activeContext;
    }

    /**
     * Notifies listeners that a new context has been added.
     *
     * @param ctx      The context that just got added.
     * @param ctxImage An unmutable instance of somewhat cooked
     *                 contents of the context.
     *
     * @see TesContext
     * @see TesContext.CtxImage
     */
    public void fireContextAddedEvent(TesContext ctx, TesContext.CtxImage ctxImage){
        internalFireContextEvent(ContextEvent.getContextAddedEventInstance(
                                     this, ctx, ctxImage));
    }

    public void fireContextRemovedEvent(TesContext ctx, TesContext.CtxImage ctxImage){
        internalFireContextEvent(ContextEvent.getContextRemovedEventInstance(
                                     this, ctx, ctxImage));
    }

    /**
     * Notifies listeners that a context has been updated.
     *
     * @param ctx         The context that has been updated.
     * @param ctxImage    Unmutable image of the context.
     * @param oldCtxImage Old contents of the unmutable context image.
     * @param whatChanged A mask of changes made to the context.
     *
     * @see TesContext 
     * @see TesContext.CtxImage
     */
    // notify subscribers that a context has been updated
    public void fireContextUpdatedEvent(
        TesContext ctx,
        TesContext.CtxImage ctxImage,
        TesContext.CtxImage oldCtxImage,
        int whatChanged
    )
    {
        internalFireContextEvent(ContextEvent.getContextUpdatedEventInstance(
                                     this, ctx, ctxImage,
                                     oldCtxImage, whatChanged));
    }

    /**
     * Notifies listeneres that a context has been activated.
     * Since there is only one active context at a given time, this
     * implicitly means that another context may have gotton 
     * deactivated.
     * <p>
     * If newCtx is <code>null</code> then active context got
     * deactivated.
     * <p>
     * @param oldCtx       Old context, which just got deactivated.
     * @param oldCtxImage  Unmutable image of the old context.
     * @param newCtx       New context, which just got activated.
     * @param newCtxImage  Unmutable image of the new context.
     *
     * @see TesContext
     * @see TesContextImage
     */
    // notify subscribers that the currently active context has changed
    public void fireContextActivatedEvent(
        TesContext oldCtx,
        TesContext.CtxImage oldCtxImage,
        TesContext newCtx,
        TesContext.CtxImage newCtxImage
    )
    {
        log.println("oldCtx: "+oldCtx);
        log.println("oldCtxImage: "+oldCtxImage);
        log.println("newCtx: "+newCtx);
        log.println("newCtxImage: "+newCtxImage);

        ContextEvent e = ContextEvent.getContextActivatedEventInstance(
            this, newCtx, newCtxImage, oldCtx, oldCtxImage);
        log.println(e);
        internalFireContextEvent(e);
    }

    /**
     * Internal function to transmit a {@link ContextEvent} to 
     * the appropriate {@link ContextEventListener}s.
     */
    private void internalFireContextEvent(ContextEvent e){
        Object[] listeners = listenerList.getListenerList();

        for(int i = 0; i < listeners.length; i+=2){
            if (listeners[i] == ContextEventListener.class){
                ContextEventListener l = (ContextEventListener)listeners[i+1];

                if (e.getType() == ContextEvent.CONTEXT_ADDED){
                    l.contextAdded(e);
                }
                else if (e.getType() == ContextEvent.CONTEXT_UPDATED){
                    l.contextUpdated(e);
                }
                else if (e.getType() == ContextEvent.CONTEXT_DELETED){
                    l.contextDeleted(e);
                }
                else if (e.getType() == ContextEvent.CONTEXT_ACTIVATED){
                    l.contextActivated(e);
                }
                else {
                    log.println("WARNING! "+e.toString()+": type="+
                                       e.getType()+" slipped by!");
                }
            }
        }
    }

    /**
     * Registers a {@link ContextEventListener}.
     */
    public void addContextEventListener(ContextEventListener l){
        listenerList.add(ContextEventListener.class, l);
    }

    /**
     * Deregisters a {@link ContextEventListener}.
     */
    public void removeContextEventListener(ContextEventListener l){
        listenerList.remove(ContextEventListener.class, l);
    }


    /**
     * Updates the specified context with non-null parameters.
     *
     * <p>
     * Note: This method is only called by the swing thread.
     * <p>
     *
     * @param ctx      The context to be updated
     * @param title    Updated title or <code>null</code>
     * @param fields   Update list of fields or <code>null</code>
     * @param selects  Updated list of selects or <code>null</code>
     * @param orderBys Updated list of ordering criteria or <code>null</code>
     * @param colorBy  Updated coloring criteria or <code>null</code>
     *
     * @see            TesContext
     */
    // Anything with a null value hasn't changed
    public void updateContext(
        TesContext ctx,
        String title,
        Vector<FieldDesc> fields,
        Vector<RangeDesc> selects,
        Vector<OrderCriterion> orderBys,
        ColorBy colorBy,
		Boolean drawReal,
		Boolean drawNull
    )
    {
        TesContext.CtxImage oldCtxImage = ctx.getCtxImage();

        synchronized(ctx){
            if (title    != null){ ctx.setTitle(title); }
            if (fields   != null){ ctx.setFields(fields); }
            if (selects  != null){ ctx.setSelects(selects); }
            if (orderBys != null){ ctx.setOrderCriteria(orderBys); }
            if (colorBy  != null){ ctx.setColorBy(colorBy); }
            if (drawReal != null){ ctx.setDrawReal(drawReal); }
            if (drawNull != null){ ctx.setDrawNull(drawNull); }
        }

        int whatChanged = 
            ContextEvent.makeWhatChanged(
                title!=null, fields!=null, selects!=null,
                orderBys!=null, colorBy!=null,
				drawReal!=null, drawNull!=null);

        fireContextUpdatedEvent(ctx, ctx.getCtxImage(), oldCtxImage, whatChanged);
    }

    /**
     * Returns a set of regions that intersect the specified
     * viewport.
     *
     * @param vp viewport regions under which are desired
     * @return   a set of regions
     * @deprecated
     */
	public Set<RegionDesc> getRegionsUnderViewport(Rectangle2D vp){

        // TODO: Once we finalize on how the regions are laid-out,
        // TODO: there is a better way of determining which regions
        // TODO: fall under the viewport without doing any intersections.
        // TODO: Since the numbering of regions is very regular.

		Iterator<Integer> i = regionDescMap.keySet().iterator();
		Set<RegionDesc> regionSet = new HashSet<RegionDesc>();
		Rectangle2D vp0Edge, vp360Edge;

		// if the bounding box straddles the 360-edge check the 0-edge as well
		vp0Edge = new Rectangle2D.Double(
			vp.getMinX()-360.0, vp.getMinY(),
			vp.getWidth(), vp.getHeight());

		// if the bounding box straddles the 0-edge check the 360-edge as well
		vp360Edge = new Rectangle2D.Double(
			vp.getMinX()+360.0, vp.getMinY(),
			vp.getWidth(), vp.getHeight());
		

		while(i.hasNext()){
			Integer id = i.next();
            RegionDesc regionDesc = regionDescMap.get(id);
			Shape bbox = (Shape)regionDesc.getRegionBoundaryShared();

			if (bbox.intersects(vp) || bbox.intersects(vp0Edge) || bbox.intersects(vp360Edge)){
				regionSet.add(regionDesc);
			}
		}

		return regionSet;
	}

    /**
     * Returns a set of regions that contain the specified point.
     *
     * @param p The point which should be contained in the regions
     *          returned.
     * @return  A set of regions.
     *
     * @see     RegionDesc
     */
	public Set<RegionDesc> getRegionsUnderPoint(Point2D p){
		Iterator<Integer> i = regionDescMap.keySet().iterator();
		Set<RegionDesc> regions = new HashSet<RegionDesc>();

		// normalize the point
		//Point2D q = getNormalizedPoint(p);

		// TODO: See comment in getRegionIdsUnderViewport above
		while(i.hasNext()){
			Integer id = i.next();
            RegionDesc regionDesc = regionDescMap.get(id);
			Shape bbox = (Shape)regionDesc.getRegionBoundaryShared();

			// The 0-edge and 360-edge test does not apply here
			// because the point does not have any width or height.
			if (bbox.contains(p)){
				regions.add(regionDesc);
			}
		}

		return regions;
	}

	public RegionDesc getRegionUnderPoint(Point2D p){
		Iterator<Integer> i = regionDescMap.keySet().iterator();

		// normalize the point
		//Point2D q = getNormalizedPoint(p);

		// TODO: See comment in getRegionIdsUnderViewport above
		while(i.hasNext()){
			Integer id = i.next();
            RegionDesc regionDesc = regionDescMap.get(id);
			Shape bbox = (Shape)regionDesc.getRegionBoundaryShared();

			// The 0-edge and 360-edge test does not apply here
			// because the point does not have any width or height.
			if (bbox.contains(p)){
				return regionDesc;
			}
		}

		return null;
	}

    /**
     * Returns the given (lat,lon) point within the 0-360 range.
     *
     * @param p the point to be normalized
     * @return the normalized point with longitude within the 0-360 range.
     */
	private Point2D getNormalizedPoint(Point2D p){
		Point2D q = new Point2D.Double();
		
		q.setLocation(p);
		if (q.getX() > 360.0){ q.setLocation(q.getX()-360.0, q.getY()); }
		if (q.getY() < 0.0){ q.setLocation(q.getX()+360.0, q.getY()); }

		return q;
	}

    

    public TesKey[] getKeysInOrder(
        String whereClause,
        String orderByClause,
        RegionDesc regionDesc
    )
        throws SQLException
    {
        TesKey[] keys = getKeysForRegion(whereClause, regionDesc);
        int[]  order = getOrderDataForRegion(whereClause, orderByClause, regionDesc);

        keys = (TesKey[])Utils.reorderObjects(keys, order);
        return keys;
    }

    public TesKey[] getKeysForRegion(
        String whereClause,
        RegionDesc regionDesc
        )
        throws SQLException
    {
        int[] detIds = getDetKeysForRegion(whereClause, regionDesc);
        TesKey[] keys = new TesKey[detIds.length];

        for(int i = 0; i < detIds.length; i++){
            keys[i] = new TesKey(detIds[i], regionDesc, whereClause);
        }

        return keys;
    }
    
    public LinkedHashMap<TesKey,SerializablePoly> getPolysInRegionInOrder(
    		String whereClause,
    		String orderByClause,
    		RegionDesc regionDesc,
    		boolean interpolatedPolys
    ) throws SQLException {
        int[] detIds = getDetKeysInOrder(whereClause, orderByClause, regionDesc);
        SerializablePoly[] polys = getPolysInOrder(whereClause, orderByClause, regionDesc, interpolatedPolys);
        
        LinkedHashMap<TesKey,SerializablePoly> out = new LinkedHashMap<TesKey,SerializablePoly>(detIds.length);
        for(int i = 0; i < detIds.length; i++){
        	TesKey key = new TesKey(detIds[i], regionDesc, whereClause);
        	out.put(key, polys[i]);
        }
        
        return out;
    }

    public int[] getDetKeysInOrder(String whereClause, String orderClause, RegionDesc region)
        throws SQLException
    {
        int[]   detIds = getDetKeysForRegion(whereClause, region);
        int[] order = getOrderDataForRegion(whereClause, orderClause, region);

        detIds = Utils.reorderObjects(detIds, order);
        
        return detIds;
    }

    /**
     * Returns polygons in the order determined by the orderClause.
     * There is one polygon returned per detector-id matching the 
     * whereClause. The polygon returned can be an empty GeneralPath
     * if there isn't sufficient information available to the 
     * interpolator. It can be null if it was being pulled from the
     * database and turned out to be null in the database.
     * The returned polygons are in west-leading coordinates.
     *
     * @param whereClause  SQL where clause to limit the data.
     * @param orderClause  SQL "order by" clause to order the data.
     * @param region       Returned data is restricted to this region only
     * @param interpolate  Get the data by way of interpolation when possible. 
     * @return             An array of properly ordered polygons.
     */
    public SerializablePoly[] getPolysInOrder(
        String whereClause,
        String orderClause,
        RegionDesc region,
        boolean interpolate
    )
        throws SQLException
    {
    	SerializablePoly[] polys = null;
    	
    	if (interpolate){
    		polys = getInterpolatedPolysForRegion(whereClause, region);
    	}
    	else {
    		polys = getPolysForRegion(whereClause, region);
    	}
    	
        int[] order = getOrderDataForRegion(whereClause, orderClause, region);
        polys = (SerializablePoly[])Utils.reorderObjects(polys, order);

        return polys;
    }
    
	public Point2D[] getDetCentersInOrder(
		String whereClause,
		String orderClause,
		RegionDesc region
	)
		throws SQLException
	{
		Point2D[] centers = getDetCentersForRegion(whereClause, region);
		int[] order = getOrderDataForRegion(whereClause, orderClause, region);
		
        centers = (Point2D[])Utils.reorderObjects(centers, order);

        return centers;
	}



    /**
     * Returns data for a given field in the order determined by the
     * orderClause.
     * There is one data value returned per detector-id matching the
     * whereClause. The field data returned can be null, if so stored
     * in the database. Data associated with footprint-ids is expanded
     * such that it matches the dimensions of the detector-ids that
     * would have been returned via the whereClause.
     *
     * @param whereClause  SQL where clause to limit the data.
     * @param orderClause  SQL "order by" clause to order the data.
     * @param field        Field for which the data is desired.
     * @param region       Returned data is restricted to this region only.
     * @return             An array of data objects.
     */
    public Object[] getFieldDataInOrder(
        String whereClause,
        String orderClause,
        FieldDesc field,
        RegionDesc region
    )
        throws SQLException
    {
        Object[] fieldData = getFieldDataForRegionForDetUse(whereClause, field, region);
        int[]  order     = getOrderDataForRegion(whereClause, orderClause, region);
        
        fieldData = Utils.reorderObjects(fieldData, order);

        return fieldData;
    }

    public Object getFieldData(TesKey key, FieldDesc field)
        throws SQLException
    {
        int[] detIds =
            getDetKeysForRegion(key.getWhereClause(), key.getRegionDesc());

        Object[] fieldData =
            getFieldDataForRegionForDetUse(
                key.getWhereClause(), field, key.getRegionDesc());

        int idx = Arrays.binarySearch(detIds, key.getDetId());
        if (idx > -1){
            return fieldData[idx];
        }

        return null;
    }
    
    public Map<TesKey, Object> getFieldData(Collection<TesKey> keys, FieldDesc field) throws SQLException {
    	Map<RegionDesc, Collection<TesKey>> keysByRegion = clusterKeysByRegion(keys);
    	Map<TesKey, Object> out = new HashMap<TesKey, Object>(keys.size());
    	
    	for(RegionDesc region: keysByRegion.keySet()){
    		TesKey sampleKey = keysByRegion.get(region).iterator().next();
        	int[] detIds = getDetKeysForRegion(sampleKey.getWhereClause(), sampleKey.getRegionDesc());
        	Object[] fieldData = getFieldDataForRegionForDetUse(sampleKey.getWhereClause(), field, region);
        	
        	String sampleKeyCond = sampleKey.getWhereClause();
        	for(TesKey key: keysByRegion.get(region)){
        		if (!sampleKeyCond.equals(key.getWhereClause()))
        			throw new IllegalStateException("Keys from two different contexts encountered.");
        			
            	int idx = Arrays.binarySearch(detIds, key.getDetId());
            	out.put(key, (idx > -1)? fieldData[idx]: null);
        	}
    	}

    	return out;
    }
    
    private Map<RegionDesc, Collection<TesKey>> clusterKeysByRegion(Collection<TesKey> keys){
    	Map<RegionDesc, Collection<TesKey>> out = new HashMap<RegionDesc, Collection<TesKey>>();
    	
    	for(TesKey key: keys){
    		Collection<TesKey> regionKeys = out.get(key.getRegionDesc());
    		if (regionKeys == null){
    			regionKeys = new ArrayList<TesKey>();
    			out.put(key.getRegionDesc(), regionKeys);
    		}
			regionKeys.add(key);
    	}
    	
    	return out;
    }


    public SerializablePoly getPolyData(TesKey key, boolean interpolate)
    	throws SQLException
    {
        int[] detIds =
            getDetKeysForRegion(key.getWhereClause(), key.getRegionDesc());

        SerializablePoly[] polyData;
        if (interpolate){
        	polyData = getInterpolatedPolysForRegion(key.getWhereClause(), key.getRegionDesc());
        }
        else {
        	polyData = getPolysForRegion(key.getWhereClause(), key.getRegionDesc());
        }

        int idx = Arrays.binarySearch(detIds, key.getDetId());
        if (idx > -1){
            return polyData[idx];
        }
    	
    	return null;
    }

    /**
     * Returns centers of foot-prints (boresight), at least one of the
     * detectors for whom matches the whereClause. The returned center
     * points are in East-leading coordinates.
     *
     * @param whereClause  SQL where clause to limit the data
     * @param region       Returned data is restricted to this region only.
     * @return             An array of foot-print centers, one per footprint-id.
     */
    public Point2D[] getFpCenters(String whereClause, RegionDesc region)
        throws SQLException
    {
        short[]   bLats  = getBLatsForRegion(whereClause, region);
        short[]   bLons  = getBLonsForRegion(whereClause, region);
        Point2D[] fpCenters = new Point2D[bLats.length];
        float     clon, clat;
        Point2D   regionCentroid = region.getCentroid();
        
        for(int i = 0; i < fpCenters.length; i++){
            clon = (float)(bLons[i]*Common.BLON_BLAT_SCALE + regionCentroid.getX());
            clat = (float)(bLats[i]*Common.BLON_BLAT_SCALE + regionCentroid.getY());
            fpCenters[i] = new Point2D.Float(clon,clat);
        }

        return fpCenters;
    }


    private Point2D[] getDetCentersForRegion(
    	String whereClause,
    	RegionDesc region
    )
    	throws SQLException
    {
    	Point2D regionCentroid = region.getCentroid();
    	int[] detIds = getDetKeysForRegion(whereClause, region);
    	int[] fpIds = getFpKeysForRegion(whereClause, region);
    	short[] bLats = getBLatsForRegion(whereClause, region);
        short[] bLons  = getBLonsForRegion(whereClause, region);

        SerializablePoly[] offAxisPolys = null;
        Point2D[] centers = new Point2D[detIds.length];
        int ock, ick, det, fpArrayIdx;
        //boolean off16Deg;
        boolean offAxis, spHandling;
        float clon, clat;
        int i;

        for(i = 0; i < detIds.length; i++){
            ock = Common.getOck(detIds[i]);
            ick = Common.getIck(detIds[i]);
            det = Common.getDet(detIds[i]);

            fpArrayIdx = getCommonKeyIndex(detIds[i], fpIds);

            //off16Deg = Common.getOff16Deg(fpIds[fpArrayIdx]);
            offAxis  = Common.getOffAxis(fpIds[fpArrayIdx]);
            spHandling = Common.getSpHandling(fpIds[fpArrayIdx]);

            if (offAxis || spHandling){
                if (offAxisPolys == null){
                    offAxisPolys = getPolysForRegion(whereClause, region);
                }

                Rectangle2D bbox = offAxisPolys[i].getBounds2D();
                centers[i] = new Point2D.Float(
                		(float)bbox.getCenterX(),
                		(float)bbox.getCenterY());
            }
            else {
                clon = (float)(bLons[fpArrayIdx]*Common.BLON_BLAT_SCALE + regionCentroid.getX());
                clat = (float)(bLats[fpArrayIdx]*Common.BLON_BLAT_SCALE + regionCentroid.getY());

                try {
                	// TODO: pass 16-deg off flag
                	centers[i] = tesFpCalculator.getDetectorCenter(ock, ick, det, clon, clat);
                }
                catch(TesFpCalculator.NoInfoException ex){
                	// Fall-back on actual data
                	if (offAxisPolys == null){
                		offAxisPolys = getPolysForRegion(whereClause, region);
                	}
                    Rectangle2D bbox = offAxisPolys[i].getBounds2D();
                    centers[i] = new Point2D.Float(
                    		(float)bbox.getCenterX(),
                    		(float)bbox.getCenterY());
                }
            }
        }
        
        
    	return centers;
    }
    
    private Serializable getValue(Element element){
    	if (element == null)
    		return null;
    	
    	return element.getValue();
    }
    
    /**
     * Returns polygons preferrably by way of interpolation in the key-order.
     * If the observation is marked as being off-axis, the polygon boundary
     * is retrieved from the database and returned, otherwise, it is 
     * interpolated via {@link TesFpCalculator}.
     *
     * @param whereClause  SQL where clause to limit the data
     * @param region       Returned data is restricted to this region only.
     * @return             An array of polygons in west-leading coordinates.
     *
     * @see #getPolysInOrder
     */
    private SerializablePoly[] getInterpolatedPolysForRegion(
        String whereClause,
        RegionDesc region
    )
        throws SQLException
    {
        String dataName = "i"+polyField.getQualifiedFieldName();
        CacheKey cacheKey = new CacheKey(whereClause, region.getRegionId(), dataName);

        SerializablePoly[] data = null;
        try {
        	data = (SerializablePoly[])getValue(l1Cache.get(cacheKey));
        }
        catch(CacheException ex){
        	log.aprintln(ex);
        }
        
        if (data == null){
            data = buildInterpolatedPolysForRegion(whereClause, region);
            l1Cache.put(new Element(cacheKey, data));
        }
        
        return data;
    }
    
    private SerializablePoly[] buildInterpolatedPolysForRegion(
    	String whereClause,
    	RegionDesc region
    )
    	throws SQLException
    {
        Point2D  regionCentroid = region.getCentroid();
        int[]   detIds = getDetKeysForRegion(whereClause, region);
        int[]   fpIds  = getFpKeysForRegion(whereClause, region);
        short[] bLats  = getBLatsForRegion(whereClause, region);
        short[] bLons  = getBLonsForRegion(whereClause, region);
        SerializablePoly[] offAxisPolys = null;

        SerializablePoly[] polys = new SerializablePoly[detIds.length];
        int ock, ick, det, fpArrayIdx;
        //boolean off16Deg;
        boolean  offAxis, spHandling;
        float clon, clat;
        int i;

        for(i = 0; i < detIds.length; i++){
            ock = Common.getOck(detIds[i]);
            ick = Common.getIck(detIds[i]);
            det = Common.getDet(detIds[i]);

            // partialFpId = detIds[i] & PARTIAL_FP_ID_MASK;
            fpArrayIdx = getCommonKeyIndex(detIds[i], fpIds);

            //off16Deg = Common.getOff16Deg(fpIds[fpArrayIdx]);
            offAxis  = Common.getOffAxis(fpIds[fpArrayIdx]);
            spHandling = Common.getSpHandling(fpIds[fpArrayIdx]);

            if (offAxis || spHandling){
                if (offAxisPolys == null){
                    offAxisPolys = getPolysForRegion(whereClause, region);
                }

                polys[i] = offAxisPolys[i];
            }
            else {
                clon = (float)(bLons[fpArrayIdx]*Common.BLON_BLAT_SCALE + regionCentroid.getX());
                clat = (float)(bLats[fpArrayIdx]*Common.BLON_BLAT_SCALE + regionCentroid.getY());

                try {
                	// TODO: pass 16-deg off flag
                	polys[i] = new SerializablePoly(
                			tesFpCalculator.getDetectorOutlinePoints(ock, ick, det, clon, clat));
                	// if (polys[i] != null){ ShapeUtils.eastToWestLonInPlace(polys[i]); }
                }
                catch(TesFpCalculator.NoInfoException ex){
                	// Fall-back on real data
                	log.println("Falling back on real data for "+ock+","+ick+","+det);
                    if (offAxisPolys == null){
                        offAxisPolys = getPolysForRegion(whereClause, region);
                    }

                    polys[i] = offAxisPolys[i];
                }
            }
        }

        return polys;
    }


    /**
     * Builds dataName part of a key as
     * <code>&lt;keyField,essentialField[1],essentialField[2],...&gt;</code>.
     *
     * @param keyField        Key field for the data.
     * @param essentialFields An array of essential fields that should be pulled every time
     *                        the key data is pulled.
     * @return                String made up of the names of the key and essential fields.
     *
     * @see #getDetKeysForRegion
     * @see #getFpKeysForRegion
     */
    private String makeKeyAndEssentialDataName(FieldDesc keyField, FieldDesc[] essentialFields){
        StringBuffer sbuff = new StringBuffer();

        sbuff.append(keyField.getQualifiedFieldName());
        for(int i = 0; i < essentialFields.length; i++){
            sbuff.append(",");
            sbuff.append(essentialFields[i].getQualifiedFieldName());
        }

        return sbuff.toString();
    }


    /**
     * Converts a Vector&lt;Integer&gt; into an int array.
     *
     * @param intObjVector   A vector of Integer objects.
     * @return               An array of integers corresponding
     *                       to the Integer objects.
     */
    private int[] vectorToInts(Vector<Integer> intObjVector){
        int[] intData = new int[intObjVector.size()];

        for(int i = 0; i < intData.length; i++){
            intData[i] = intObjVector.get(i).intValue();
        }

        return intData;
    }


    /**
     * Converts a Vector&lt;Short&gt; into an int array.
     *
     * @param shortObjVector   A vector of Integer objects.
     * @return                 An array of shorts corresponding
     *                         to the Integer objects.
     */
    private short[] vectorToShorts(Vector<Short> shortObjVector){
        short[] shortData = new short[shortObjVector.size()];
        
        for(int i = 0; i < shortData.length; i++){
            shortData[i] = shortObjVector.get(i).shortValue();
        }

        return shortData;
    }


    /**
     * Retrieves footprint-ids in key/footprint-order for the specified
     * where clause and region.
     *
     * @param whereClause SQL where clause.
     * @param region      The returned data is restricted to this region only.
     * @return            An array of footprint-ids in footprint-id order.
     */
    private int[] getFpKeysForRegion(String whereClause, RegionDesc region)
        throws SQLException
    {
        Object[] data = (Object[])getFpEssentialDataForRegion(whereClause, region);
        return (int[])((Object[])data)[0];
        
    }

    private short[] getBLonsForRegion(String whereClause, RegionDesc region)
        throws SQLException
    {
        Object[] data = (Object[])getFpEssentialDataForRegion(whereClause, region);
        return (short[])((Object[])data)[1];
    }

    private short[] getBLatsForRegion(String whereClause, RegionDesc region)
        throws SQLException
    {
        Object[] data = (Object[])getFpEssentialDataForRegion(whereClause, region);
        return (short[])((Object[])data)[2];
    }

    private Object[] getFpEssentialDataForRegion(String whereClause, RegionDesc region)
        throws SQLException
    {
        String dataName = makeKeyAndEssentialDataName(fpKeyField, fpEssFields);
        CacheKey cacheKey = new CacheKey(whereClause, region.getRegionId(), dataName);

        Object[] data = null;
        try {
        	data = (Object[])getValue(l1Cache.get(cacheKey));
        }
        catch(CacheException ex){
        	log.aprintln(ex);
        	// Run in degraded mode by querying the database
        }
        
    	if (data == null){
    		try {
    			data = getFpEssentialDataForRegionFromDb(whereClause, region.getRegionId());
    		}
    		finally {
    			l1Cache.put(new Element(cacheKey, data));
    		}
    	}
    	
        return data;
    }

    private int[] getDetKeysForRegion(String whereClause, RegionDesc region)
        throws SQLException
    {
        String dataName = makeKeyAndEssentialDataName(detKeyField, detEssFields);
        CacheKey cacheKey = new CacheKey(whereClause, region.getRegionId(), dataName);

        Object[] data = null;
        try {
        	data = (Object[])getValue(l1Cache.get(cacheKey));
        }
        catch(CacheException ex){
        	log.aprintln(ex);
        }
        
        if (data == null){
        	try {
        		data = getDetEssentialDataForRegionFromDb(whereClause, region.getRegionId());
        	}
        	finally {
        		l1Cache.put(new Element(cacheKey, data));
        	}
        }

        return (int[])(((Object[])data)[0]);
    }

    private Object[] getFieldDataForRegionForDetUse(String whereClause, FieldDesc field, RegionDesc region)
        throws SQLException
    {
        if (!field.isFpField()){
            return getFieldDataForRegion(whereClause, field, region);
        }
        
        int[]    detKeys = getDetKeysForRegion(whereClause, region);
        int[]    fpKeys  = getFpKeysForRegion(whereClause, region);
        Object[] data    = getFieldDataForRegion(whereClause, field, region);
        Object[] detData = new Object[detKeys.length];

        int i, j = 0;
        for(i = 0; i < detKeys.length && j < fpKeys.length; i++){
            while((j < fpKeys.length) && 
            		(Common.getDetIdFpIdCommonPart(fpKeys[j]) <
            				Common.getDetIdFpIdCommonPart(detKeys[i]))){
                j++;
            }
            
            if (Common.getDetIdFpIdCommonPart(detKeys[i]) == Common.getDetIdFpIdCommonPart(fpKeys[j])){
                detData[i] = data[j];
            }
        }
        
        return detData;
    }

    /*
    private Object[] getFieldDataForRegionForFpUse(String whereClause, FieldDesc field, RegionDesc region)
        throws SQLException
    {
        if (field.isFpField()){
            return getFieldDataForRegion(whereClause, field, region);
        }

        int[]    detKeys = getDetKeysForRegion(whereClause, region);
        int[]    fpKeys  = getFpKeysForRegion(whereClause, region);
        Object[] data    = getFieldDataForRegion(whereClause, field, region);
        Object[] fpData  = new Object[fpKeys.length];

        int i, j = 0;
        for(i = 0; i < fpKeys.length && j < detKeys.length; i++){
            while((j < detKeys.length) && (commonPartOfKey(detKeys[j]) < commonPartOfKey(fpKeys[i]))){
                j++;
            }

            // TODO: add averaging code
            if (commonPartOfKey(fpKeys[i]) == commonPartOfKey(detKeys[j])){
                fpData[i] = data[j];
            }
        }

        return fpData;
    }
    */

    private Object[] getFieldDataForRegionFromEssData(String whereClause, FieldDesc field, RegionDesc region)
        throws SQLException
    {
        int[]  keys;
        int    idx = -1; // data index

        if      (field.equals(ockField)){ idx = 0; }
        else if (field.equals(ickField)){ idx = 1; }
        else if (field.equals(detField)){ idx = 2; }

        keys = (field.isFpField())
            ? getFpKeysForRegion(whereClause, region)
            : getDetKeysForRegion(whereClause, region);
            
        Vector<Number> data = new Vector<Number>(keys.length);
        
        if (field.equals(detKeyField) || field.equals(fpKeyField)){
        	for(int i = 0; i < keys.length; i++){
        		data.add(new Integer(keys[i]));
        	}
        	return data.toArray();
        }
        
        for(int i = 0; i < keys.length; i++){
        	switch(idx){
        		case 0: data.add(new Integer(Common.getOck(keys[i]))); break;
        		case 1: data.add(new Short(Common.getIck(keys[i]))); break;
        		case 2: data.add(new Short(Common.getDet(keys[i]))); break;
        	}
        }

        return data.toArray();
    }

    private boolean isBuiltInField(FieldDesc field){
        return (field.equals(ockField)
                || field.equals(ickField)
                || field.equals(detField)
                || field.equals(detKeyField)
                || field.equals(fpKeyField));
    }

    private Object[] getFieldDataForRegion(String whereClause, FieldDesc field, RegionDesc region)
        throws SQLException
    {
        if (isBuiltInField(field)){
            return getFieldDataForRegionFromEssData(whereClause, field, region);
        }

        String dataName = field.getQualifiedFieldName();
        CacheKey cacheKey = new CacheKey(whereClause, region.getRegionId(), dataName);

        Object[] data = null;
        try {
        	data = (Object[])getValue(l1Cache.get(cacheKey));
        }
        catch(CacheException ex){
        	log.aprintln(ex);
        }
        
        if (data == null){
        	try {
        		data = getFieldDataForRegionFromDb(whereClause, region.getRegionId(), field);
        	}
        	finally {
        		l1Cache.put(new Element(cacheKey, data));
        	}
        }
        
        return (Object[])data;
    }

    /**
     * Returns polygons belonging to detectors falling in a given region.
     * If this data is not already cached, it is cached from the database and
     * then returned.
     * NOTE: In most circumstances the function one should use is 
     * getInterpolatedPolysForRegion() instead.
     */
    private SerializablePoly[] getPolysForRegion(String whereClause, RegionDesc region)
        throws SQLException
    {
        String dataName = polyField.getQualifiedFieldName();
        CacheKey cacheKey = new CacheKey(whereClause, region.getRegionId(), dataName);

        SerializablePoly[] data = null;
        try {
        	data = (SerializablePoly[])getValue(l1Cache.get(cacheKey));
        }
        catch(CacheException ex){
        	log.aprintln(ex);
        }
        
        if (data == null){
        	try {
        		data = getPolysForRegionFromDb(whereClause, region.getRegionId());
        	}
        	finally {
        		l1Cache.put(new Element(cacheKey, data));
        	}
        }
        
        return data;
    }

    private int[] getOrderDataForRegion(String whereClause, String orderByClause, RegionDesc region)
        throws SQLException
    {
        String dataName = orderByClause;
        CacheKey cacheKey = new CacheKey(whereClause, region.getRegionId(), "order by "+dataName);
        
        int[] data = null;
        
        try {
        	data = (int[])getValue(l1Cache.get(cacheKey));
        }
        catch(CacheException ex){
        	log.aprintln(ex);
        }
        
        if (data == null){
        	try {
        		data = getDetKeyOrderForRegionFromDb(whereClause, orderByClause, region.getRegionId());
        	}
        	finally {
        		l1Cache.put(new Element(cacheKey, data));
        	}
        }
        
        return data;
    }

    /**
     * Get keys and boresight lat,lon values for records that fall within
     * the specified region
     *
     * @param ctx context in question
     * @param regionId region-id
     * @return array of keys corresponding to the records in
     *         the specified region
     */
	private Object[] getFpEssentialDataForRegionFromDb(String whereClause, Integer regionId)
		throws SQLException
	{
		Connection c = null;
        Vector[] result = new Vector[1+fpEssFields.length];
        for(int i = 0; i < result.length; i++){ result[i] = new Vector(EXPECTED_RECS_PER_REGION); }

        StringBuffer sql = new StringBuffer();
        String columnsClause = makeKeyAndEssentialDataName(fpKeyField, fpEssFields);
        String regionCond = regionField.getQualifiedFieldName()+"="+regionId.toString();

        sql.append("select distinct "); sql.append(columnsClause);
        if (SPLIT_TABLE){
            sql.append(" from "); sql.append(fpKeyField.getTableName()+regionId);
        	if (!whereClause.equals("")){
        		sql.append(" where "); sql.append(whereClause);
        	}
        }
        else {
            sql.append(" from "); sql.append(fpKeyField.getTableName());
        	sql.append(" where "); sql.append(regionCond);
        	if (!whereClause.equals("")){
        		sql.append(" and "); sql.append(whereClause);
        	}
        }
        sql.append(" order by "); sql.append(fpKeyField.getQualifiedFieldName());

        try {
            runningStateCoordinator.fireDbReqStartEvent(this);
            c = dbUtils.createConnection();
            Statement stmt = c.createStatement();
            
            long stTime = System.currentTimeMillis();
            if (LOG_SQL){ log.println("Statement: "+sql); }

            ResultSet rs = stmt.executeQuery(sql.toString());
            while(rs.next()){
                for(int i = 0; i < result.length; i++){
                    result[i].add(rs.getObject(i+1));
                }
            }
            if (LOG_SQL){
                log.println("Statement: "+sql+" returned "+result[0].size()+" records in "+
                                   +(System.currentTimeMillis()-stTime)/1000.0f+" seconds.");
            }
        }
        finally {
            if (c != null){ c.close(); }
            runningStateCoordinator.fireDbReqEndEvent(this);
        }

        Object[] dataArray = new Object[result.length];
        dataArray[0] = vectorToInts(result[0]);   // fpid
        dataArray[1] = vectorToShorts(result[1]); // blon
        dataArray[2] = vectorToShorts(result[2]); // blat
        
        return dataArray;
	}


	private Object[] getDetEssentialDataForRegionFromDb(String whereClause, Integer regionId)
		throws SQLException
	{
		Connection c = null;
        Vector[] result = new Vector[1+detEssFields.length];
        for(int i = 0; i < result.length; i++){ result[i] = new Vector(); }

        StringBuffer sql = new StringBuffer();
        String columnsClause = makeKeyAndEssentialDataName(detKeyField, detEssFields);
        String regionCond = regionField.getQualifiedFieldName()+"="+regionId.toString();

        sql.append("select distinct "); sql.append(columnsClause);
        if (SPLIT_TABLE){
        	sql.append(" from "); sql.append(detKeyField.getTableName()+regionId);
        	if (!whereClause.equals("")){
        		sql.append(" where "); sql.append(whereClause);
        	}
        }
        else {
            sql.append(" from "); sql.append(detKeyField.getTableName());
            sql.append(" where "); sql.append(regionCond);
            if (!whereClause.equals("")){
                sql.append(" and "); sql.append(whereClause);
            }
        }
        sql.append(" order by "); sql.append(detKeyField.getQualifiedFieldName());

        try {
            runningStateCoordinator.fireDbReqStartEvent(this);
            c = dbUtils.createConnection();
            Statement stmt = c.createStatement();
            
            long stTime = System.currentTimeMillis();
            if (LOG_SQL){ log.println("Statement: "+sql); }

            ResultSet rs = stmt.executeQuery(sql.toString());
            while(rs.next()){
                for(int i = 0; i < result.length; i++){
                    result[i].add(rs.getObject(i+1));
                }
            }
            if (LOG_SQL){
                log.println("Statement: "+sql+" returned "+result[0].size()+" records in "+
                                   +(System.currentTimeMillis()-stTime)/1000.0f+" seconds.");
            }
        }
        finally {
            if (c != null){ try { c.close(); } catch(SQLException ex){ log.println(ex); }; }
            runningStateCoordinator.fireDbReqEndEvent(this);
        }

        Object[] dataArray = new Object[result.length];
        dataArray[0] = vectorToInts(result[0]);  // detid
        
        return dataArray;
	}

	private SerializablePoly[] getPolysForRegionFromDb(String whereClause, Integer regionId)
		throws SQLException
	{
		Connection c = null;
        Vector<SerializablePoly> polys = new Vector<SerializablePoly>();
        StringBuffer sql = new StringBuffer();
        String columnsClause = detKeyField.getQualifiedFieldName()+",";
        if (polyFieldEnc == DbUtils.POLY_ENC_STRING || polyFieldEnc == DbUtils.POLY_ENC_ARRAY){
            columnsClause += polyField.getQualifiedFieldName();
        }
        else { // POLY_ENC_WKB
            columnsClause += "AsBinary("+polyField.getQualifiedFieldName()+")";
        }
        String regionCond = regionField.getQualifiedFieldName()+"="+regionId.toString();

        // "select distinct" in the following statement replaced by "select"
        // select distinct does not work on polys under postgres
        sql.append("select "); sql.append(columnsClause);
        
        if (SPLIT_TABLE){
        	sql.append(" from "); sql.append(detKeyField.getTableName()+regionId);
            if (!polyField.getTableName().equals(detKeyField.getTableName())){
                sql.append(" natural join "); sql.append(polyField.getTableName());
            }
        	if (!whereClause.equals("")){
        		sql.append(" where "); sql.append(whereClause);
        	}
        }
        else {
            sql.append(" from "); sql.append(detKeyField.getTableName());
            if (!polyField.getTableName().equals(detKeyField.getTableName())){
                sql.append(" natural join "); sql.append(polyField.getTableName());
            }
            sql.append(" where "); sql.append(regionCond);
            if (!whereClause.equals("")){
                sql.append(" and "); sql.append(whereClause);
            }
        }
        
        sql.append(" order by "); sql.append(detKeyField.getQualifiedFieldName());

        try {
            runningStateCoordinator.fireDbReqStartEvent(this);
            c = dbUtils.createConnection();
            Statement stmt = c.createStatement();
            
            long stTime = System.currentTimeMillis();
            if (LOG_SQL){ log.println("Statement: "+sql); }

            ResultSet rs = stmt.executeQuery(sql.toString());
            while(rs.next()){
            	SerializablePoly poly;
                if (polyFieldEnc == DbUtils.POLY_ENC_STRING){
                    poly = new SerializablePoly(
                    		ShapeUtils.getPolyFromString(
                    				rs.getObject(2).toString()));
                }
                else if (polyFieldEnc == DbUtils.POLY_ENC_ARRAY){
                	poly = new SerializablePoly(
                			ShapeUtils.getPolyFromSqlArray(
                					rs.getArray(2)));
                }
                else { // POLY_ENC_WKB
                    poly = new SerializablePoly(
                    		ShapeUtils.getPolyFromWkbRep(
                    				rs.getBytes(2)));
                }
                polys.add(poly);
            }
            if (LOG_SQL){
                log.println("Statement: "+sql+" returned "+polys.size()+" records in "+
                                   +(System.currentTimeMillis()-stTime)/1000.0f+" seconds.");
            }
        }
        finally {
            if (c != null){ try { c.close(); } catch(SQLException ex){ log.println(ex); }; }
            runningStateCoordinator.fireDbReqEndEvent(this);
        }

        SerializablePoly[] polyArray = new SerializablePoly[polys.size()];
        polys.toArray(polyArray);
        
        return polyArray;
	}


	private Object[] getFieldDataForRegionFromDb(
        String whereClause,
        Integer regionId,
        FieldDesc field
    )
		throws SQLException
	{
		Connection c = null;
        Vector<Object> data = new Vector<Object>();
        StringBuffer sql = new StringBuffer();
        FieldDesc keyField = (field.isFpField())? fpKeyField: detKeyField;
        String columnsClause = keyField.getQualifiedFieldName()+","+field.getQualifiedFieldName();
        String regionCond = regionField.getQualifiedFieldName()+"="+regionId.toString();


        sql.append("select distinct "); sql.append(columnsClause);
        if (SPLIT_TABLE){
            sql.append(" from "); sql.append(keyField.getTableName()+regionId);
            if (!whereClause.equals("")){
                sql.append(" where "); sql.append(whereClause);
            }
        }
        else {
            sql.append(" from "); sql.append(keyField.getTableName());
            sql.append(" where "); sql.append(regionCond);
            if (!whereClause.equals("")){
                sql.append(" and "); sql.append(whereClause);
            }
        }
        sql.append(" order by "); sql.append(keyField.getQualifiedFieldName());

        try {
            runningStateCoordinator.fireDbReqStartEvent(this);
            c = dbUtils.createConnection();
            Statement stmt = c.createStatement();
            
            long stTime = System.currentTimeMillis();
            if (LOG_SQL){ log.println("Statement: "+sql); }

            ResultSet rs = stmt.executeQuery(sql.toString());
            while(rs.next()){
                Object dObj = rs.getObject(2);
                if (dObj instanceof java.sql.Array){ // handle arrays
                	/*
                    ResultSet srs = ((Array)dObj).getResultSet();
                    Vector vec = new Vector(200);
                    
                    while(srs.next()){ vec.add(srs.getObject(2)); }
                    
                    dObj = new Object[vec.size()];
                    vec.toArray((Object[])dObj);
                    */
                	dObj = ((java.sql.Array)dObj).getArray();
                }
                data.add(dObj);
            }
            if (LOG_SQL){
                log.println("Statement: "+sql+" returned "+data.size()+" records in "+
                                   +(System.currentTimeMillis()-stTime)/1000.0f+" seconds.");
            }
        }
        finally {
            if (c != null){ try { c.close(); } catch(SQLException ex){ log.println(ex); }; }
            runningStateCoordinator.fireDbReqEndEvent(this);
        }

        Object[] dataArray = new Object[data.size()];
        data.toArray(dataArray);
        
        return dataArray;
	}


	private int[] getDetKeyOrderForRegionFromDb(
        String whereClause,
        String orderByClause,
        Integer regionId
    )
		throws SQLException
	{
		Connection c = null;
        Vector<Object> keys = new Vector<Object>();
        StringBuffer sql = new StringBuffer();
        String columnsClause = detKeyField.getQualifiedFieldName();
        String regionCond = regionField.getQualifiedFieldName()+"="+regionId.toString();

        // Removed distinct from the select below, since detid's are distinct
        // in any given region.
        sql.append("select "); sql.append(columnsClause);
        if (SPLIT_TABLE){
            sql.append(" from "); sql.append(detKeyField.getTableName()+regionId);
            if (!whereClause.equals("")){
                sql.append(" where "); sql.append(whereClause);
            }
        }
        else {
            sql.append(" from "); sql.append(detKeyField.getTableName());
            sql.append(" where "); sql.append(regionCond);
            if (!whereClause.equals("")){
                sql.append(" and "); sql.append(whereClause);
            }
        }

        // TODO: handle trivial case by pulling the keys from within the cache
        //       thus avoiding the round trip to the database.
        if (!orderByClause.equals("")){
            sql.append(" order by "); sql.append(orderByClause);
        }
        else {
            sql.append(" order by "); sql.append(detKeyField.getQualifiedFieldName());
        }

        try {
            runningStateCoordinator.fireDbReqStartEvent(this);
            c = dbUtils.createConnection();
            Statement stmt = c.createStatement();
            
            long stTime = System.currentTimeMillis();
            if (LOG_SQL){ log.println("Statement: "+sql); }

            ResultSet rs = stmt.executeQuery(sql.toString());
            while(rs.next()){
                keys.add(rs.getObject(1));
            }
            if (LOG_SQL){
                log.println("Statement: "+sql+" returned "+keys.size()+" records in "+
                                   +(System.currentTimeMillis()-stTime)/1000.0f+" seconds.");
            }
        }
        finally {
            if (c != null){ try { c.close(); } catch(SQLException ex){ log.println(ex); }; }
            runningStateCoordinator.fireDbReqEndEvent(this);
        }

        
        int n = keys.size();
        Integer[] tmpKeyOrder = new Integer[n];
        Integer[] keyArray    = new Integer[n];
        int i;
        
        for(i = 0; i < n; i++){
            tmpKeyOrder[i] = new Integer(i);
            keyArray[i]    = (Integer)keys.get(i);
        }
        Arrays.sort(tmpKeyOrder, new IndexedComparator(keyArray));

        // Store the indices in the sorted order.
        int[] keyOrderArray = new int[n];
        for(i = 0; i < n; i++){
            keyOrderArray[tmpKeyOrder[i].intValue()] = i;
        }

        // Reverse the order, since the user expects the data at 
        // start of the sorted list to appear above (in depth) the
        // data that appears lower in the sorted list.
        int n_half = n/2;
        int t;
        for(i = 0; i < n_half; i++){
            t = keyOrderArray[i];
            keyOrderArray[i] = keyOrderArray[n-1-i];
            keyOrderArray[n-1-i] = t;
        }

        return keyOrderArray;
	}


    /**
     * Derives the fpKey from the detKey. Note that the fpKey is <ock,ick,flags>
     * while the detKey is <ock,ick,det,flags>. Both of them are 32-bits in length
     * with ock being 16-bits, ick being 12-bits, det being 3-bits and flags
     * the rest.
     */
    private int getCommonKeyIndex(int key, int keys[]){
        int st = 0, ed = keys.length-1, mid = -1;
        // int partialSearchKey = commonPartOfKey(key);
        int currentCommonKey;
        int searchCommonKey = Common.getDetIdFpIdCommonPart(key);

        while(ed >= st){
            mid = (st+ed)/2;
            currentCommonKey = Common.getDetIdFpIdCommonPart(keys[mid]);

            if (searchCommonKey > currentCommonKey){ st = mid+1; }
            else if (searchCommonKey < currentCommonKey){ ed = mid-1; }
            else { break; }
        }

        return mid;
    }


    public static Map<Integer, RegionDesc> getRegionDescMap(){
        return regionDescMap;
    }
    
    public Vector<TesContext> getContexts(){
        return new Vector<TesContext>(contexts);
    }

    public int getContextIndex(TesContext ctx){
        return contexts.indexOf(ctx);
    }

    public int getContextCount(){
        return contexts.size();
    }

	private void fillRegionDescMap(){
        Connection c = null;
		try {
			c = dbUtils.createConnection();
			String sql;
            if (polyFieldEnc == DbUtils.POLY_ENC_STRING || polyFieldEnc == DbUtils.POLY_ENC_ARRAY){
                sql = DbUtils.buildSqlSimple(new String[] {"region","bbox"}, regionsTable);
            }
            else {
                sql = DbUtils.buildSqlSimple(new String[] {"region","AsBinary(bbox)"}, regionsTable);
            }
			Statement s = c.createStatement();
			ResultSet rs = s.executeQuery(sql);

			Integer id;
			GeneralPath bbox;

			while(rs.next()){
				id = new Integer(rs.getInt(1));
                if (polyFieldEnc == DbUtils.POLY_ENC_STRING){
                    bbox = ShapeUtils.getPolyFromString(rs.getObject(2).toString());
                }
                else if (polyFieldEnc == DbUtils.POLY_ENC_ARRAY){
                	bbox = ShapeUtils.getPolyFromSqlArray(rs.getArray(2));
                }
                else {
                    bbox = ShapeUtils.getPolyFromWkbRep(rs.getBytes(2));
                }

                RegionDesc rd = new RegionDesc(id, bbox);
                regionDescMap.put(id, rd);
			}
		}
		catch(SQLException ex){
			log.println(ex);
			//ex.printStackTrace();
		}
        finally {
            if (c != null){ try { c.close(); } catch(SQLException ex){ log.println(ex); }; }
        }
	}

    public int getMaxContexts(){
        return MAX_CONTEXTS;
    }

    // status LED tracking
    public synchronized void runningStateChanged(RunningStateChangeEvent evt){
        int scope = evt.getScope();
        int event = evt.getEvent();

        if (event == RunningStateChangeEvent.EVENT_START){
            busyCounters[scope]++;
        }
        else if (event == RunningStateChangeEvent.EVENT_END){
            busyCounters[scope] --;
        }

        if (busyCounters[RunningStateChangeEvent.SCOPE_DB_IO] > 0){
            setStatus(statusDbIo);
        }
        else if (busyCounters[RunningStateChangeEvent.SCOPE_MAIN] > 0){
            setStatus(statusMainBusy);
        }
        else if (busyCounters[RunningStateChangeEvent.SCOPE_PANNER] > 0){
            setStatus(statusPannerBusy);
        }
        else {
            setStatus(statusIdle);
        }
    }

    public RunningStateCoordinator getRunningStateCoordinator(){
        return runningStateCoordinator;
    }


    private static final Color statusDbIo = Color.red;
    private static final Color statusMainBusy = Color.orange;
    private static final Color statusPannerBusy = Color.blue;
    private static final Color statusIdle = edu.asu.jmars.util.Util.darkGreen;


    public FieldDesc getPolyField(){ return polyField; }
    public FieldDesc getOckField(){ return ockField; }
    public FieldDesc getIckField(){ return ickField; }
    public FieldDesc getDetField(){ return detField; }
    public FieldDesc getScanLenField(){ return scanLenField; }
    public FieldDesc getColorField(){ return colorField; }


	// STATE
	private Vector<TesContext> contexts;
	private TesContext activeContext;
    private EventListenerList listenerList;
    public  DbUtils dbUtils;
    
    // SHARED CACHE
    private static Ehcache l1Cache;

    // various essential fields
    public FieldDesc   detKeyField;
    public FieldDesc[] detEssFields;
    public FieldDesc   fpKeyField;
    public FieldDesc[] fpEssFields;
    public FieldDesc   polyField;
    public FieldDesc   regionField;
    public FieldDesc   ockField, ickField, detField, scanLenField;
    public FieldDesc   colorField; // fake color field

    // encoding of polygon field
    private int polyFieldEnc;

    // status LED tracking
    RunningStateCoordinator runningStateCoordinator;
    private int[] busyCounters = new int[3]; // [scope] counts

	// STATIC DATA
    private static Map<Integer,RegionDesc> regionDescMap;
    private static TesFpCalculator tesFpCalculator;

    // Maximum number of contexts that a layer can have. This is
    // required because Layer.LView does not support the setBufferCount()
    // after construction.
    private static final int   MAX_CONTEXTS = Config.get("tes.maxContexts", 10);

    private static String regionsTable = Config.get("tes.db.regionDescTable");
    private static final boolean LOG_SQL = Config.get("tes.db.logSql",false);
    private static final boolean SPLIT_TABLE = Config.get("tes.db.regionSplitTable",false);

    private final int EXPECTED_RECS_PER_REGION = 200000;

}


/**
 * Comparator used by Arrays.sort() to sort an array of indices
 * pointing to integer keys.
 */
class IndexedComparator implements Comparator<Integer>{
    public IndexedComparator(Comparable[] vals){
        this.vals = vals;
    }
    public int compare(Integer i1, Integer i2){
        return (vals[i1.intValue()].compareTo(vals[i2.intValue()]));
    }
    private Comparable[] vals;
}


