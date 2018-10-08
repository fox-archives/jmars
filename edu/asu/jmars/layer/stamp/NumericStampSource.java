package edu.asu.jmars.layer.stamp;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferFloat;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.swing.SwingUtilities;

import edu.asu.jmars.Main;
import edu.asu.jmars.ProjObj;
import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.layer.map2.MapAttr;
import edu.asu.jmars.layer.map2.MapAttrReceiver;
import edu.asu.jmars.layer.map2.MapRequest;
import edu.asu.jmars.layer.map2.MapServer;
import edu.asu.jmars.layer.map2.MapSource;
import edu.asu.jmars.layer.map2.MapSourceDefault;
import edu.asu.jmars.layer.map2.MapSourceListener;
import edu.asu.jmars.layer.map2.NonRetryableException;
import edu.asu.jmars.layer.map2.RetryableException;
import edu.asu.jmars.layer.stamp.radar.FilledStampRadarType;
import edu.asu.jmars.util.Util;

/**
 * A source of map data produced by the stamp layer. The
 * {@link #fetchTile(MapRequest)} method is called by the map processing system,
 * and it creates an image for the requested area, calls the
 * {@link StampLView#doRender(edu.asu.jmars.layer.stamp.StampImage.Request, java.util.List, RenderProgress, Graphics2D)}
 * method to draw rendered stamps into the tile, and then returns it to the
 * caller.
 * 
 * Since use of this MapSource implementation causes a map layer to become
 * dependent on a stamp layer, serialization of the map layer must include
 * enough information to find the stamp layer later after reloading. This is
 * done by including the StampLayerSettings object in the serialized state for
 * this class. When fetchTile() needs to find the StampLView, the existing
 * layers will be searched, and {@link StampLView#doRender()} called on the
 * match.
 * 
 * StampSource instances are created through a static constructor, and
 * deserialized objects are passed through readResolve(), so that there can only
 * be one StampSource instance for each StampLayerSettings instance. This allows
 * adding listeners to StampSource, and ensures that all StampSource instances
 * point at the singleton StampServer and that that server contains all
 * StampSource instances.
 * 
 * Tiles can take awhile to render in the stamp layer, but the clipping system
 * used is pretty efficient, so once we have an answer back from the stamp
 * layer, we keep it and return cached tiles. When the stamp layer detects a
 * settings change, it will call {@link #clearCache()} to invalidate all of the
 * cache for that layer.
 */
public final class NumericStampSource extends MapSourceDefault implements MapSource, Serializable {
	private static long lastID = System.currentTimeMillis();
	private static final Map<CacheKey,BufferedImage> cache = new LinkedHashMap<CacheKey,BufferedImage>();
	private static Map<StampLayerSettings,NumericStampSource> instances = new WeakHashMap<StampLayerSettings,NumericStampSource>();
	private synchronized long getID() {
		return lastID = Math.max(lastID+1, System.currentTimeMillis());
	}
	
	private final StampLayerSettings settings;
	private long id = getID();
	private double maxPPD = MapSource.MAXPPD_MAX;
	private double[] ignore = null;
	/** will lazily create these if this instance was deserialized */
	private transient List<MapSourceListener> listeners = new ArrayList<MapSourceListener>();
	/** will lazily set this to the singleton StampServer instance */
	private transient StampServer server;
	
	/**
	 * Creates a new StampSouce or returns an existing instance, in both cases
	 * ensuring that the instance has a StampServer defined, and that the server
	 * contains this source.
	 */
	public static NumericStampSource create(StampLayerSettings settings) {
		NumericStampSource out = instances.get(settings);
		if (out == null) {
			// create new instance with these settings
			instances.put(settings, out = new NumericStampSource(settings));
		}
		// really just setting up the server
		merge(out,out);
		out.server.add(out);
		return out;
	}
	public static void clearSources() {
		instances = new WeakHashMap<StampLayerSettings,NumericStampSource>();
	}
	/** private to force using {@link #create(StampLayerSettings)}. */
	private NumericStampSource(StampLayerSettings settings) {
		this.settings = settings;
		double ignoreVal[] = new double[1];
		ignoreVal[0]=StampImage.IGNORE_VALUE;
		 
		setIgnoreValue(ignoreVal);
	}
	
	/**
	 * Does the same work as {@link #create(StampLayerSettings)}, but uses a
	 * deserialized instance rather than creating a new one.
	 */
	private Object readResolve() {
		NumericStampSource out = instances.get(settings);
		if (out == null) {
			// add this unserialized object to the instance hash
			instances.put(settings, out = this);
		}
		// copy maxppd/ignore/etc. values from the saved instance to a newly-created instance
		merge(out, this);
		out.server.add(out);
		return out;
	}
	
	/**
	 * Merges this StampSource with the given StampSource, and ensures that this
	 * source uses the singleton StampServer and is contained by it.
	 */
	private static void merge(NumericStampSource target, NumericStampSource source) {
		target.id = Math.min(target.id, source.id);
		target.ignore = source.ignore;
		target.maxPPD = source.maxPPD;
		if (target.server != StampServer.getInstance()) {
			target.server = StampServer.getInstance();
			target.server.add(target);
		}
	}
	
	/**
	 * Find a view at run time based on the settings, which is the most
	 * immediately usable serializable identifier between map and stamp layers.
	 */
	private StampLView getView() {
		for (LView loadedView: (List<LView>)Main.testDriver.mainWindow.viewList) {
			if (loadedView instanceof StampLView) {
				StampLView stamp = (StampLView)loadedView;
				if (stamp.stampLayer.getSettings() == settings) {
					return stamp;
				}
			}
		}
		return null;
	}
	
	/** Clears the cache for this StampSource, and initiates a StampSource change event to notify all listeners */
	public synchronized void clearCache() {
		boolean changed;
		synchronized(cache) {
			changed = cache.size() > 0;
			cache.clear();
		}
		if (changed) {
			changed();
		}
	}
	
	private static final class CacheKey {
		public Rectangle2D extent;
		public ProjObj po;
		public int ppd;
		public int hashCode() {
			return ppd*317 + extent.hashCode()*31 + po.hashCode();
		}
		public boolean equals(Object o) {
			if (o instanceof CacheKey) {
				CacheKey ck = (CacheKey)o;
				return ppd == ck.ppd && po == ck.po && extent.equals(ck.extent);
			} else {
				return false;
			}
		}
	}
	
	/**
	 * Cache is disabled right now, since it is causing rendering issues.
	 */
	private static final boolean useCache = false;
	
	public BufferedImage fetchTile(final MapRequest req) throws RetryableException, NonRetryableException {
		if (useCache) {
			return fetchCached(req);
		} else {
			return fetchDirect(req);
		}
	}
	
	private BufferedImage fetchCached(MapRequest req) throws RetryableException, NonRetryableException {
		CacheKey ck = new CacheKey();
		ck.extent = req.getExtent();
		ck.po = req.getProjection();
		ck.ppd = req.getPPD();
		
		// if item is in the cache, move it to end and return it
		synchronized(cache) {
			// check the cache
			BufferedImage bi = cache.remove(ck);
			
			// if not found, fetch it immediately
			if (bi == null) {
				bi = fetchDirect(req);
			}
			
			if (!req.isCancelled()) {
				// remove from the head on each request
				int toRemove = cache.size() - 200;
				if (toRemove > 0) {
					Iterator<?> it = cache.keySet().iterator();
					while (toRemove > 0) {
						it.next();
						it.remove();
						toRemove --;
					}
				}
				
				// append image to end of cache and return image
				cache.put(ck, bi);
				return bi;
			} else {
				return null;
			}
		}
	}
	
	private BufferedImage fetchDirect(MapRequest req) throws RetryableException, NonRetryableException {
		// otherwise fill out target image with data from the stamp layer
		Dimension size = req.getImageSize();
		
		SampleModel sm = new ComponentSampleModel(DataBuffer.TYPE_DOUBLE, size.width, size.height, 1, size.width, new int[] {0});
		
		DataBuffer db = new DataBufferFloat(size.width * size.height);
		WritableRaster wr = Raster.createWritableRaster(sm, db, null);
		ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
		ColorModel cm = new ComponentColorModel(cs, false, true, Transparency.OPAQUE, DataBuffer.TYPE_DOUBLE);
		
		BufferedImage bi = new BufferedImage(cm, wr, true, null);		
				
		double[] fdata = new double[size.width*size.height];
		
		for (int i=0;i<fdata.length;i++) {
			fdata[i]=StampImage.IGNORE_VALUE;  // Initialize with ignore value.  Anything not explicitly filled should be rendered transparent
		}
		
		bi.getRaster().setDataElements(0, 0, size.width, size.height, fdata);
		
		Graphics2D g2 = bi.createGraphics();
		g2.setTransform(Util.world2image(req.getExtent(), size.width, size.height));
		
		// check for a view, returning an empty image if there isn't one
		StampLView view = getView();
		if (view == null) {
			return bi;
		}
		
		// create rendering listener that will unlock the calling thread when the last stamp has
		// been rendered into this tile
		final boolean[] done = {false};
		RenderProgress progress = new RenderProgress() {
			public void update(int current, int max) {
				if (current == max) {
					synchronized(done) {
						done[0] = true;
						done.notifyAll();
					}
				}
			}
		};
		
		// do the rendering on this thread
		List<FilledStamp> stamps = view.getFilteredFilledStamps();
		for (Iterator<FilledStamp> it = stamps.iterator(); it.hasNext(); ) {
			FilledStampImageType fs = (FilledStampImageType)it.next();
			
			if (!fs.pdsi.isNumeric) {
				it.remove();
				continue;
			}
			
			Point2D offset=fs.getOffset();
			
			Rectangle2D imageBounds=fs.stamp.getPath().getBounds2D();
			imageBounds.setRect(imageBounds.getMinX()+offset.getX(), imageBounds.getMinY()+offset.getY(), imageBounds.getWidth(), imageBounds.getHeight());
			if (!hit(imageBounds, req.getExtent())) {
				it.remove();
			}
		}
		
		// if there are no stamps to render, don't call the stamp layer, and
		// don't cache the image since the lookup is more than fast enough that
		// we don't want to pollute the cache with empty images
		if (!stamps.isEmpty()) {
			
			view.doRender(new MapStampRequest(req), stamps, progress, g2, bi);
			
			// wait on completion of rendering thread, as caught by the RenderProgress above
			// there is no method to hand off exceptions from the rendering thread, but
			// we don't need to handle those intricacies at the moment
			synchronized(done) {
				while (!done[0]) {
					try {
						done.wait(30000);
					} catch (InterruptedException e) {
						e.printStackTrace();
						throw new RetryableException("Timed out", e);
					} catch (Throwable t) {
						t.printStackTrace();
						throw new NonRetryableException("Exception while waiting on numeric stamp rendering", t);
					}
				}
			}
		}
		
		return bi;
	}
	
	/**
	 * Decides whether two rectangles overlap 'on the ground', by getting all
	 * parts of the [0,360] wrapped world coordinates covered by each rectangle
	 * and seeing if any part of the first rectangle touches any part of the
	 * second rectangle
	 */
	private static boolean hit(Rectangle2D r1, Rectangle2D r2) {
		for (Rectangle2D a: Util.toWrappedWorld(r1)) {
			for (Rectangle2D b: Util.toWrappedWorld(r2)) {
				if (a.intersects(b)) {
					return true;
				}
			}
		}
		return false;
	}
	
	public String getAbstract() {
		return "Numeric Stamp layer exposed as a map layer";
	}

	public String[][] getCategories() {
		return new String[][]{{}};
	}

	public double[] getIgnoreValue() {
		return ignore;
	}

	public Rectangle2D getLatLonBoundingBox() {
		return new Rectangle2D.Double(0,-90,360,180);
	}

	public MapAttr getMapAttr() {
		return MapAttr.SINGLE_BAND;
	}

	public void getMapAttr(MapAttrReceiver receiver) {
		receiver.receive(MapAttr.SINGLE_BAND);
	}

	public double getMaxPPD() {
		return maxPPD;
	}

	public String getMimeType() {
		return null;
	}

	public String getName() {
		return "numeric_stampsource_" + id;
	}
	
	public String getUnits() {
		StampLView view = getView();
		List<FilledStamp> stamps = view.getFilteredFilledStamps();
		if(stamps!=null && stamps.size()>0 ){
			return stamps.get(0).pdsi.units;
		}
		
		return null;
	}

	public Point2D getOffset() {
		return new Point(0,0);
	}

	public MapServer getServer() {
		return server;
	}

	public String getTitle() {
		StampLView view = getView();
		if (view == null || LManager.getLManager() == null) {
			return "undefined_numeric_stamp_source";
		} else {
			return LManager.getLManager().getUniqueName(view) + " (numeric)";
		}
	}

	public boolean hasNumericKeyword() {
		return true;
	}

	public boolean hasElevationKeyword() {
		return true;
	}
	
	public String getOwner() {
		return null;
	}

	public boolean isMovable() {
		return false;
	}

	public void setIgnoreValue(double[] ignoreValue) {
		ignore = ignoreValue;
		changed();
	}

	public void setMaxPPD(double maxPPD) throws IllegalArgumentException {
		this.maxPPD = maxPPD;
		changed();
	}
	
	public void setOffset(Point2D offset) {
		if (offset == null || offset.getX() != 0 || offset.getY() != 0) {
			throw new IllegalArgumentException("NumericStampSource#setOffset must not be called with a null or non-zero point");
		}
	}
	
	private List<MapSourceListener> getListeners() {
		if (listeners == null) {
			listeners = new ArrayList<MapSourceListener>();
		}
		return listeners;
	}
	
	public void addListener(MapSourceListener l) {
		getListeners().add(l);
	}

	public void removeListener(MapSourceListener l) {
		getListeners().remove(l);
	}
	
	private void changed() {
		final List<MapSourceListener> list = new ArrayList<MapSourceListener>(getListeners());
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				for (MapSourceListener l: list) {
					l.changed(NumericStampSource.this);
				}
			}
		});
	}
	
	public String toString(){
		return getTitle();
	}
	
	private static class MapStampRequest implements StampLView.DrawFilledRequest {
		private final MapRequest request;
		public MapStampRequest(MapRequest request) {
			this.request = request;
		}
		public boolean changed() {
			return request.isCancelled();
		}
		public Rectangle2D getExtent() {
			return request.getExtent();
		}
		public int getPPD() {
			return request.getPPD();
		}
	}
}

