package edu.asu.jmars.layer.stamp;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;



//import org.apache.http.HttpEntity;
//import org.apache.http.HttpResponse;
//import org.apache.http.HttpVersion;
//import org.apache.http.client.HttpClient;
//import org.apache.http.client.methods.HttpPost;
//import org.apache.http.entity.mime.MultipartEntity;
//import org.apache.http.entity.mime.content.ContentBody;
//import org.apache.http.entity.mime.content.FileBody;
//import org.apache.http.entity.mime.content.StringBody;
//import org.apache.http.impl.client.DefaultHttpClient;
//import org.apache.http.params.CoreProtocolPNames;






import org.apache.commons.io.IOUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.HttpVersion;

import edu.asu.jmars.Main;
import edu.asu.jmars.ProjObj;
import edu.asu.jmars.layer.DataReceiver;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.JmarsHttpRequest;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.util.HttpRequestType;
import edu.asu.msff.ResponseInfo;
import edu.asu.msff.StampInterface;

public class StampLayer extends Layer
{
    private static DebugLog log = DebugLog.instance();
  
    private static final String version = "3.11";
    private static final String jmarsVersion = Util.getVersionNumber();
    private static final String jreName = System.getProperty("java.runtime.name");
    private static final String jreVendor = System.getProperty("java.vm.vendor");
    private static final String osName = System.getProperty("os.name");
    private static final String osArch = System.getProperty("os.arch");
    private static final String osVersion = System.getProperty("os.version");
    public static final String versionStr="&version="+version+"&jmars_timestamp="+Main.ABOUT().SECS+"&jmars_version="+jmarsVersion +
    		"&java_runtime_name="+jreName+"&java_vm_vendor="+jreVendor+"&os_name="+osName+"&os_arch="+osArch+"&os_version="+osVersion;
    
    /** @GuardedBy this */
    private ArrayList<StampShape> cachedStamps=new ArrayList<StampShape>();
    /** @GuardedBy this */
    private Map<String,StampShape> stampMap = new HashMap<String,StampShape>();

    private ArrayList<StampShape> visibleStamps=new ArrayList<StampShape>();
        
    private ArrayList<StampShape> selectedStamps=new ArrayList<StampShape>();
    
    protected StampFactory stampFactory;
    
    /** @GuardedBy this */
    protected ProjObj originalPO;
    
    public static final String stampURL = Config.get("stamps.url");
    
    // Strings and parameters used by the StampLayer
    //  - these can potentially be changed by server side parameters
    public String RENDER_SELECTED_TEXT = "RENDER_SELECTED_TEXT";
    public String RENDER_TEXT = "RENDER_TEXT";
    public String NO_RENDER_OPTION_SINGLE = "NO_RENDER_OPTION_SINGLE";
    public String NO_RENDER_OPTION_MULTI = "NO_RENDER_OPTION_MULTI";
    public String WEBBROWSE1_TEXT = "WEBBROWSE1_TEXT";
    public String WEBBROWSE2_TEXT = "WEBBROWSE2_TEXT";
    public String QUICKVIEW_TEXT = "QUICKVIEW_TEXT";
    public String RENDER_MENU_SEPARATOR = "RENDER_MENU_SEPARATOR";
    public String PLOT_UNITS = "PLOT_UNITS";
    public String HELP_URL = "HELP_URL";
    public String SERVER_PLOT_TYPE = "SERVER_PLOT_TYPE";
    public String TOOLTIP_COLUMN = "TOOLTIP_COLUMN";
    public String TOOLTIP_UNITS = "TOOLTIP_UNITS";
    public String DISABLE_RENDER_OPTIONS = "DISABLE_RENDER_OPTIONS";
    public String DISABLE_WEB_OPTIONS = "DISABLE_WEB_OPTIONS";
    public String VECTOR_SHAPES = "VECTOR_SHAPES";
    public String POINT_SHAPES = "POINT_SHAPES";
    public String LINE_SHAPES = "LINE_SHAPES";
    public String SPECTRA_DATA = "SPECTRA_DATA";
    public String SPECTRA_PLOT_FIELDS = "SPECTRA_PLOT_FIELDS";
    public String SPOT_SIZE = "SPOT_SIZE";
    public String SPECTRA_AXIS_MAP = "SPECTRA_AXIS_MAP";
    public String SPECTRA_AXIS_POSTFIX_MAP = "SPECTRA_AXIS_POSTFIX_MAP";
    public String SPECTRA_AXIS_XUNITS = "SPECTRA_AXIS_XUNITS";
    public String SPECTRA_REVERSE_AXIS = "SPECTRA_REVERSE_AXIS";
    
    // end of dynamic parameters
    
    private HashMap<String, String> layerParams = new HashMap<String, String>();
    
    private void initParams() {
    	layerParams.put(RENDER_SELECTED_TEXT, "Render Selected as ");
    	layerParams.put(RENDER_TEXT, "Render ");
    	layerParams.put(NO_RENDER_OPTION_SINGLE, "Sorry - there are no rendering options available for this stamp");
    	layerParams.put(NO_RENDER_OPTION_MULTI, "Sorry - there are no rendering options available for these stamps");
    	layerParams.put(WEBBROWSE1_TEXT, "Web browse ");
    	layerParams.put(WEBBROWSE2_TEXT, "");  // By default, only one web browse link
    	layerParams.put(QUICKVIEW_TEXT, "");   // By default, no quickview link
    	layerParams.put(PLOT_UNITS, "");  // By default, no plots
    	layerParams.put(RENDER_MENU_SEPARATOR, ""); // By default, no render submenus
    	layerParams.put(HELP_URL, "http://jmars.asu.edu/wiki/index.php/Instrument_Glossaries");
    	layerParams.put(SERVER_PLOT_TYPE, "");
    	layerParams.put(TOOLTIP_COLUMN, "ID");
    	layerParams.put(TOOLTIP_UNITS, "");
    	layerParams.put(DISABLE_RENDER_OPTIONS, "");  // Used for layers with no renderable options, like Wind, Political stamps, etc.
    	layerParams.put(DISABLE_WEB_OPTIONS, "");     // Used for layers with no summary page options, like Wind, Political stamps, etc.
    	layerParams.put(VECTOR_SHAPES, ""); // Used for layers with only vector data, like Wind
    	layerParams.put(POINT_SHAPES,  "");  // Used for layers with only point values, like MOLA Shots
    	layerParams.put(LINE_SHAPES,  ""); // Used for layers with only line outlines, like SHARAD
    	layerParams.put(SPECTRA_DATA, ""); // Used for layers with spectra data, like TES
    	layerParams.put(SPOT_SIZE,  ""); // Used to indicate an approximate area covered by a laser altimeter shot
    	layerParams.put(SPECTRA_PLOT_FIELDS, ""); // Used to define which columns should be plotted as spectra
    	layerParams.put(SPECTRA_AXIS_MAP, ""); // Used to map spectra columns to xaxis values
    	layerParams.put(SPECTRA_AXIS_POSTFIX_MAP,  ""); // Used to map spectra columns to xaxis value based on values within other columns (ie TES's scan_length)
    	layerParams.put(SPECTRA_AXIS_XUNITS, ""); // Used to label the xaxis of spectra plots
    	layerParams.put(SPECTRA_REVERSE_AXIS, ""); // Used to reverse or not reverse the xaxis of spectra plots
    }
    
    private void fetchParams() {
		String urlStr = "ParameterFetcher?instrument="+getInstrument()+"&format=JAVA";
		HashMap<String, String> newParams;
		
		try {
			ObjectInputStream ois = new ObjectInputStream(StampLayer.queryServer(urlStr));
			newParams = (HashMap<String, String>) ois.readObject();
			ois.close();
		} catch (Exception e) {
			log.aprintln("Unable to retrieve layer parameters");
			newParams = new HashMap<String,String>();
		}
		
		for (String key: newParams.keySet()) {
			layerParams.put(key, newParams.get(key));
		}
    }
    
    public String[] getSpectraColumns() {
    	String val = layerParams.get(SPECTRA_PLOT_FIELDS);
    	return val.split(",");
    }
    
    private HashMap<String,String> spectraAxisXUnitMap = null;
    
    public HashMap<String,String> getSpectraAxisXUnitMap() {
    	if (spectraAxisXUnitMap==null) {
    		spectraAxisXUnitMap = new HashMap<String,String>();
    		String vals = layerParams.get(SPECTRA_AXIS_XUNITS);
    		String pairs[] = vals.split(",");
    		if (pairs.length>=2) {
	    		for (int i=0; i<pairs.length; i+=2) {
	    			spectraAxisXUnitMap.put(pairs[i].trim(), pairs[i+1].trim());
	    		}	
    		}
    		
    	}
    	
    	return spectraAxisXUnitMap;	
    }
    
    private HashMap<String,String> spectraAxisReverseMap = null;
    
    public HashMap<String,String> getSpectraAxisReverseMap() {
    	if (spectraAxisReverseMap==null) {
    		spectraAxisReverseMap = new HashMap<String,String>();
    		String vals = layerParams.get(SPECTRA_REVERSE_AXIS);
    		String pairs[] = vals.split(",");
    		if (pairs.length>=2) {
	    		for (int i=0; i<pairs.length; i+=2) {
	    			spectraAxisReverseMap.put(pairs[i].trim(), pairs[i+1].trim());
	    		}
    		}
    	}
    	
    	return spectraAxisReverseMap;	
    }    
    
    private HashMap<String,String> spectraAxisMap = null;
    
    public HashMap<String,String> getSpectraAxisMap() {
    	if (spectraAxisMap==null) {
    		// TODO: What happens if any of this isn't perfectly right?
    		spectraAxisMap = new HashMap<String,String>();
    		String vals = layerParams.get(SPECTRA_AXIS_MAP);
    		String pairs[] = vals.split(",");
    		if (pairs.length>=2) {
	    		for (int i=0; i<pairs.length; i+=2) {
	    			spectraAxisMap.put(pairs[i].trim(), pairs[i+1].trim());
	    		}	
    		}
    		
    	}
    	
    	return spectraAxisMap;
    }

    private HashMap<String,String> spectraAxisPostMap = null;
    
    public HashMap<String,String> getSpectraAxisPostMap() {
    	if (spectraAxisPostMap==null) {
    		// TODO: What happens if any of this isn't perfectly right?
    		spectraAxisPostMap = new HashMap<String,String>();
    		String vals = layerParams.get(SPECTRA_AXIS_POSTFIX_MAP);
    		String pairs[] = vals.split(",");
    		if (pairs.length>=2) {
	    		for (int i=0; i<pairs.length; i+=2) {
	    			spectraAxisPostMap.put(pairs[i].trim(), pairs[i+1].trim());
	    		}	
    		}    		
    	}
    	
    	return spectraAxisPostMap;
    }
        
    public double[] getAxis(String axisName) {
    	String strVal = layerParams.get(axisName);
    	
    	if (strVal==null) return null;
    	
    	String vals[] = strVal.split(",");
    	double dvals[] = new double[vals.length];
    	
    	for (int i=0; i<vals.length; i++) {
    		dvals[i]=Double.parseDouble(vals[i]);
    	}
    	return dvals;
    }
    
    public String getParam(String key) {
    	String val = layerParams.get(key);
    	if (val==null) val="";
    	return val;
    }
    
    public boolean enableRender() {
        if (getParam(DISABLE_RENDER_OPTIONS).length()==0) {
        	return true;
        }
        return false;
    }

    public boolean enableWeb() {
        if (getParam(DISABLE_WEB_OPTIONS).length()==0) {
        	return true;
        }
        return false;
    }

    public boolean vectorShapes() {
    	String param = getParam(VECTOR_SHAPES);
    	if (param!=null && param.equalsIgnoreCase("true")) {
    		return true;
    	}
    	return false;
    }

    public boolean pointShapes() {
    	String param = getParam(POINT_SHAPES);
    	if (param!=null && param.equalsIgnoreCase("true")) {
    		return true;
    	}
    	return false;
    }

    public boolean lineShapes() {
    	String param = getParam(LINE_SHAPES);
    	if (param!=null && param.equalsIgnoreCase("true")) {
    		return true;
    	}
    	return false;
    }
    
    public boolean spectraData() {
    	String param = getParam(SPECTRA_DATA);
    	if (param!=null && param.equalsIgnoreCase("true")) {
    		return true;
    	}
    	return false;    	
    }
    
    public boolean colorByColumn() {
    	if (spectraData() || pointShapes()) return true;
    	return false;
    }

    public void dispose() {
    	stampMap.clear();
    	selectedStamps.clear();
    	cachedStamps=null;    	
    	visibleStamps=null;
    }
    
    private StampLayerSettings settings;
    
    public StampLayerSettings getSettings() {
    	if (settings==null) {
    		settings=new StampLayerSettings();
    	}
    	return settings;
    }
    
    public void setSettings(StampLayerSettings newSettings) {
    	settings=newSettings;
    	setQuery(newSettings.queryStr);
    	viewToUpdate.updateStampSource();
    }
    
    public StampLView viewToUpdate;
    
    private ArrayList<StampSelectionListener> selectionListeners = new ArrayList<StampSelectionListener>();
    
    public StampLayer(StampLayerSettings newSettings) {    	
    	settings = newSettings;
    	initParams();
    	
    	// This should occur somewhere else -
    	// a) nonblocking
    	// b) data could be used for help text for the filter query
    	fetchParams();
    }
    
    public void addSelectedStamp(StampShape newlySelectedStamp) {
    	if (!selectedStamps.contains(newlySelectedStamp)) {
	    	selectedStamps.add(newlySelectedStamp);
	    	ArrayList<StampShape> newStamps = new ArrayList<StampShape>();
	    	newStamps.add(newlySelectedStamp);
	    	notifySelectionListeners(newStamps);
    	}
    	notifySelectionListeners();
    }

    public void addSelectedStamps(List<StampShape> newlySelectedStamps) {
    	for (StampShape newStamp : newlySelectedStamps) {
    		if (!selectedStamps.contains(newStamp)) {
    			selectedStamps.add(newStamp);
    		}
    	}
    	notifySelectionListeners();
    }
    
    public void removeSelectedStamp(StampShape unselectedStamp) {
    	selectedStamps.remove(unselectedStamp);
    	notifySelectionListeners();
    }

    public void addAndRemoveSelectedStamps(List<StampShape> newlySelectedStamps, List<StampShape> unselectedStamps) {
    	for (StampShape newStamp : newlySelectedStamps) {
    		if (!selectedStamps.contains(newStamp)) {
    			selectedStamps.add(newStamp);
    		}
    	}
    	for (StampShape unselectedStamp : unselectedStamps) {
    		if (selectedStamps.contains(unselectedStamp)) {
    			selectedStamps.remove(unselectedStamp);
    		}
    	}
    	notifySelectionListeners();
    }

    public void clearSelectedStamps() {
    	selectedStamps.clear();
    	notifySelectionListeners();
    }
    
    public void toggleSelectedStamp(StampShape toggledStamp) {
    	if (selectedStamps.contains(toggledStamp)) {
    		selectedStamps.remove(toggledStamp);
    	} else {
    		addSelectedStamp(toggledStamp);
    	}
		notifySelectionListeners();
    }

    // Does it make sense to toggle large numbers of stamps?
    public void toggleSelectedStamps(List<StampShape> toggledStamps) {
    	for (StampShape toggledStamp : toggledStamps) {
	    	if (selectedStamps.contains(toggledStamp)) {
	    		selectedStamps.remove(toggledStamp);
	    	} else {
	    		selectedStamps.add(toggledStamp);
	    	}
    	}
    	notifySelectionListeners();
    }

    /** Returns a copy of the selected stamps */
    public List<StampShape> getSelectedStamps() {
    	return new ArrayList<StampShape>(selectedStamps);
    }
    
    public boolean isSelected(StampShape stamp) {
    	return selectedStamps.contains(stamp);
    }
    
    public void addSelectionListener(StampSelectionListener newListener) {
    	selectionListeners.add(newListener);
    }

    private void notifySelectionListeners(List<StampShape> newStamps) {
    	for (StampSelectionListener listener : selectionListeners) {
    		listener.selectionsAdded(newStamps);
    	}
    }

    private void notifySelectionListeners() {
    	int cnt=0;
    	for (StampSelectionListener listener : selectionListeners) {
    		listener.selectionsChanged();
    		cnt++;
    	}
    }
    
    public void setQuery(String newQuery) {
    	settings.queryStr = newQuery;
    	loadStampData();
    }
    
    public void setViewToUpdate(StampLView newView) {
    	viewToUpdate=newView;
    }
    
    
    public synchronized ArrayList<StampShape> getStamps()
    {
   		if (originalPO != Main.PO)
   			reprojectStampData();
    	
    	return  cachedStamps;
    }
    
    public synchronized ArrayList<StampShape> getVisibleStamps()
    {
   		if (originalPO != Main.PO)
   			reprojectStampData();
    	
    	return  visibleStamps;
    }
    
    public synchronized StampShape getStamp(String stampID)
    {
        if (stampID == null)
            return null;
        else
            return stampMap.get(stampID.trim());
    }
    
    public void receiveRequest(Object layerRequest,
                               DataReceiver requester)
    {
    	Rectangle2D area = (Rectangle2D)layerRequest;
    	List requestedStamps = findStampsByWorldRect(getVisibleStamps(), area);
    	
        requester.receiveData(requestedStamps.toArray(new StampShape[requestedStamps.size()]));
    }
            
    public synchronized void updateVisibleStamps()
    {
        StampTask task = startTask();
        task.updateStatus(Status.YELLOW);

        ArrayList<StampShape> stamps = getStamps();

        ArrayList<StampShape> data = new ArrayList<StampShape>(200);

        List<StampFilter> filters = viewToUpdate.getFilters();
        				
		mainLoop: 		for (StampShape stamp : stamps)
		{			
			for (StampFilter filter : filters) {
				if (!filter.filterActive || filter.dataIndex==-1) {
					continue;
				}

				int min = filter.getMinValueToMatch();
				int max = filter.getMaxValueToMatch();

				if (filter.dataIndex2==-1) {
					Object obj = stamp.getData(filter.dataIndex);
					
					if (obj instanceof Float) {
    					float val = ((Float)(obj)).floatValue();

    					if (val<min || val>max) {
    						continue mainLoop;
    					} 
					} else if (obj instanceof Double) {
    					double val = ((Double)(obj)).doubleValue();

    					if (val<min || val>max) {
    						continue mainLoop;
    					} 
					} else if (obj instanceof Integer) {
    					int val = ((Integer)(obj)).intValue();

    					if (val<min || val>max) {
    						continue mainLoop;
    					} 						
					} else if (obj instanceof Short) {
    					int val = ((Short)(obj)).shortValue();

    					if (val<min || val>max) {
    						continue mainLoop;
    					} 						
					} else if (obj instanceof Long) {
    					long val = ((Long)(obj)).longValue();

    					if (val<min || val>max) {
    						continue mainLoop;
    					} 						
					} else if (obj instanceof BigDecimal) { // Why in the world does the moc database use BigDecimal?
    					float val = ((BigDecimal)(obj)).floatValue();

    					if (val<min || val>max) {
    						continue mainLoop;
    					} 						
					} else if (obj instanceof Timestamp) {
						Timestamp val = ((Timestamp)(obj));

						long valMs=val.getTime();
						long currMs = System.currentTimeMillis();
						
						long deltaMs=currMs-valMs;
						
						long deltaS = deltaMs/1000;
						long deltaM = deltaS / 60;
						long deltaH = deltaM / 60;
						long deltaD = deltaH / 24;
											
    					if (deltaD<min || deltaD>max) {
    						continue mainLoop;
    					} 						
					} else {
//						if (o==null) {
//							System.out.println("o is null..."); 
//						} else {
//							System.out.println("It's a : " + o.getClass());
//						}
					}
				} else {
					Object o = stamp.getData(filter.dataIndex);
					Object o2 = stamp.getData(filter.dataIndex2);
					
					if (o instanceof Float && o2 instanceof Float) {
    					float val = ((Float)(o)).floatValue();
    					float val2 = ((Float)(o2)).floatValue();
    					
    					if ((val<min || val>max) && (val2<min || val2>max)) {
    						continue mainLoop;
    					} 
					} else if (o instanceof Integer && o2 instanceof Integer) {
    					int val = ((Integer)(o)).intValue();
    					int val2 = ((Integer)(o2)).intValue();
    					
    					if ((val<min || val>max) && (val2<min || val2>max)) {
    						continue mainLoop;
    					} 					
					} else {
						System.out.println("Not a float value: " + filter.columnName + " " + filter.dataIndex);
						System.out.println("It's a : " + o.getClass());
					}    						
				}
			}
			
			data.add(stamp);    
		}

		visibleStamps = data;
		
		viewToUpdate.viewChanged();
	 
		if (viewToUpdate.getChild()!=null) {
			viewToUpdate.getChild().viewChanged();
		}
		
        task.updateStatus(Status.DONE);
    }

	private List<StampShape> findStampsByWorldRect(List<StampShape> stamps, Rectangle2D proximity)
	{
		if (stamps == null || proximity == null)
			return null;

		List<StampShape> list = new ArrayList<StampShape>();
		double w = proximity.getWidth();
		double h = proximity.getHeight();
		double x = proximity.getX();
		double y = proximity.getY();

		x -= Math.floor(x/360.0) * 360.0;

		Rectangle2D proximity1 = new Rectangle2D.Double(x, y, w, h);
		Rectangle2D proximity2 = null;
		log.println("proximity1 = " + proximity1);

		// Handle the two cases involving x-coordinate going past
		// 360 degrees:
		// Proximity rectangle extends past 360...
		if (proximity1.getMaxX() >= 360) {
			proximity2 = new Rectangle2D.Double(x-360, y, w, h);
			log.println("proximity2 = " + proximity2);
		}
		// Normalized stamp extends past 360 but
		// proximity rectangle does not...
		else if (proximity1.getMaxX() <= 180) {
			proximity2 = new Rectangle2D.Double(x+360, y, w, h);
			log.println("proximity2 = " + proximity2);
		}

		// Perform multiple proximity tests at the same time
		// to avoid re-sorting resulting stamp list.
		for (StampShape ss : stamps) {
			// TODO: Update this to compare against fillAreas instead
			Shape shape = ss.getNormalPath();
			Rectangle2D stampBounds = shape.getBounds2D();
			
			// Do a fast compare with the Rectangle bounds, then do a second
			// more accurate compare if the areas overlap.
			if (stampBounds.intersects(proximity1) ||
        		( proximity2 != null && stampBounds.intersects(proximity2)))
			{
				if (shape.intersects(proximity1) ||
		        		( proximity2 != null && shape.intersects(proximity2)))
					list.add(ss);				
			}
		}

		return list;
	}

    public String getInstrument() {
    	return settings.instrument;
    }
    
    private static String authStr = null;
    
    public static String getAuthString() {
    	if (authStr==null) {   		
    		if (Config.get("sendStampCredentials", true)) {
		        String user = Main.USER;
		        String pass = Main.PASS;
		        String domain = Main.AUTH_DOMAIN;
		        String product = Main.PRODUCT;
		
		        // No longer hash passwords.  They're sent via https instead.
		        // This allows our verification scripts to authenticate against
		        // the various sources we have to try for authentication.  ie. 
		        // mysql, LDAP, etc.	
		        authStr = "&user="+user+"&password="+pass+"&product="+product+"&domain="+domain;
    		} else {
    			// For environments such as the LROC SOC, the backend doesn't check credentials, so don't bother sending
    			// 'plaintext' passwords across the network.  The data should be communicated via https and as a result be
    			// secure, but error messages on one side or the other can inadvertently output passwords.  We still send
    			// the real userid to allow for debugging of issues in the logfiles.
    			authStr = "&user="+Main.USER+"&password="+"dummy";
    		}
    	}
        return authStr;
    }
    
    enum Status {
		RED,
		YELLOW,
		PINK,
		GREEN,
		DONE
	}
    
    public class StampTask {
    	StampLayer myLayer;
    	private StampTask(StampLayer newLayer) {
    		myLayer=newLayer;
    	}
    	
    	Status currentStatus=Status.RED;
    	
    	public void updateStatus(Status newStatus) {
    		currentStatus=newStatus;
    		myLayer.updateStatus();
    	}
    }
    
    public synchronized StampTask startTask() {
    	StampTask newTask = new StampTask(this);
    	activeTasks.add(newTask);
    	return newTask;
    }
    
    List<StampTask> activeTasks = new ArrayList<StampTask>();
    
	public synchronized void updateStatus() {
		Color layerStatus = Util.darkGreen;
		List<StampTask> doneTasks = new ArrayList<StampTask>();
		for (StampTask task : activeTasks) {
			switch(task.currentStatus) {
			   case GREEN :
			   case DONE :
				   doneTasks.add(task);
				   continue;
			   case YELLOW :
				   if (layerStatus==Util.darkGreen) {
					   layerStatus=Color.yellow;
				   }
				   break;
			   case PINK :  // not sure how pink should be used
				   if (layerStatus==Util.darkGreen) {
					   layerStatus=Color.pink;
				   }
				   break;
			   case RED :
				   layerStatus = Color.red;
				   break;			
			}
		}
		
		activeTasks.removeAll(doneTasks);
		log.println("Status updated to : " + layerStatus);
		
		final Color newStatus = layerStatus;
		
		// get on the AWT thread to update the GUI
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				setStatus(newStatus);		
			}
		});
				
	}
	
	private static final int DEFAULT_TIMEOUT = 60;

	public static InputStream queryServer(String urlStr) throws IOException, URISyntaxException {
		return queryServer(urlStr, DEFAULT_TIMEOUT);
	}

	public static InputStream queryServer(String urlStr, int TIMEOUT) throws IOException, URISyntaxException {
		int idx = urlStr.indexOf("?");
		
		String connStr = urlStr.substring(0,idx+1);
		
		String data = urlStr.substring(idx+1);

		// Strip off a passed in stampURL, just in case we have one saved from an old save file
		connStr = connStr.replace(stampURL, "");
		
    	connStr = connStr.replaceAll(".*StampFetcher\\?","StampFetcher?");
    	connStr = connStr.replace("?", "");

		return queryServer(connStr, data, TIMEOUT);

	}

	public static InputStream queryServer(String urlStr, String data) throws IOException, URISyntaxException {
		return queryServer(urlStr, data, DEFAULT_TIMEOUT);
	}
	
	public static void postToServer(String urlStr, File file) throws IOException, URISyntaxException {
		int idx = urlStr.indexOf("?");

		String connStr = stampURL + urlStr.substring(0,idx);
		
		String data = urlStr.substring(idx+1)+getAuthString()+versionStr;
		
		postToServer(connStr, data, file);
	}

	public static InputStream queryServer(String urlStr, String data, int TIMEOUT) throws IOException, URISyntaxException {
		urlStr = stampURL + urlStr;
		data = data + getAuthString() + versionStr;
		
//		URL url = new URL(urlStr);
//		URLConnection conn = url.openConnection();               // TODO (PW) use JmarsHttprequest instead through return statement.
        JmarsHttpRequest request = new JmarsHttpRequest(urlStr, HttpRequestType.POST);
		
		// Connect timeout and SO_TIMEOUT
//		conn.setConnectTimeout(TIMEOUT*1000);
//		conn.setReadTimeout(TIMEOUT*1000);
        request.setConnectionTimeout(TIMEOUT*1000);
        request.setReadTimeout(TIMEOUT*1000);
		
//		conn.setDoOutput(true);
//		OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
//		wr.write(data);
//		wr.flush();
//		wr.close();
        
        for (String param : data.split("&")) {
        	String[] nvp = param.split("=");
        	String key = nvp[0];
        	String val = (nvp.length >= 2) ? nvp[1] : ""; 
        	request.addRequestParameter(key, val);
        }
        
        request.send();

        /*
         * We are going to read the entire input so that we can properly close the HTTP
         * connection.  Then, we will return the captured input as a stream.
         */
        byte[] capturedInputStream = IOUtils.toByteArray(request.getResponseAsStream());
        request.close();
        
		return new ByteArrayInputStream(capturedInputStream);
	}
	
	public static void postToServer(String urlStr, String data, File file) throws IOException, URISyntaxException {
		//
//	    HttpClient httpclient = new DefaultHttpClient();                                                  // TODO (PW) Use JmarsHttpRequest
//	    httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);

//	    HttpPost httppost = new HttpPost(urlStr);

	    JmarsHttpRequest request = new JmarsHttpRequest(urlStr, HttpRequestType.POST);
	    request.setProtocolVersion(HttpVersion.HTTP_1_1);
	    
//	    MultipartEntity mpEntity = new MultipartEntity();
//	    ContentBody cbFile = new FileBody(file, "image/png");
//	    mpEntity.addPart("userfile", cbFile);
	    request.addUploadFile("userfile", "image/png", file);
	    
	    String dataStrings[]=data.split("&");
	    
	    for(String s: dataStrings) {
	    	if (s==null || s.length()==0) continue;
	    	String parts[] = s.split("=", 2);
	    	request.addRequestParameter(new BasicNameValuePair(parts[0], parts[1]));
//		    mpEntity.addPart(parts[0], new StringBody(parts[1]));	    	
	    }

///	    httppost.setParams(params);
//	    httppost.setEntity(mpEntity);
///	    System.out.println("executing request " + httppost.getRequestLine());
//	    HttpResponse response = httpclient.execute(httppost);
//	    HttpEntity resEntity = response.getEntity();

	    request.send();

///	    System.out.println(response.getStatusLine());
///	    if (resEntity != null) {
///	      System.out.println(EntityUtils.toString(resEntity));
///	    }
//	    if (resEntity != null) {
//	      resEntity.consumeContent();
//	    }
        request.close();

        ///	    httpclient.getConnectionManager().shutdown();		
		//
	    	
///		return new ObjectInputStream(conn.getInputStream());
	}

	// These are used to keep track of the data type and name of each column of metadata being displayed in the layer
	Class[] columnTypes=null;
	String[] columnNames=null;
	
	/**
	 * Returns the number of columns of metadata in this stamp layer
	 * @return integer number of columns, or -1 if metadata hasn't been loaded
	 */
	public int getColumnCount() {
		if (columnNames!=null) {
			return columnNames.length;
		}
		return -1;
	}
	
	public String getColumnName(int columnNum) {
		if (columnNames!=null) {
			return columnNames[columnNum];
		}
		return null;
	}
	
	public int getColumnNum(String columnName) {
		int cnt = getColumnCount();
		
		if (cnt<0) return -1;
		
		for (int i=0 ; i<cnt; i++) {
			String colName=getColumnName(i);
			if (colName.equalsIgnoreCase(columnName)) {
				return i;
			}
		}
		
		return -1;
	}
		
	public Class getColumnClass(int columnNum) {
		if (columnTypes!=null) {
			return columnTypes[columnNum];
		}
		return null;
	}
	
	// This is used to keep track of what column, if any, defines the
	// list of render options per stamp.  In some cases this can remove
	// the need for additional server queries.
	public int renderColumn=-1;
    /**
     * Loads stamp data from the main database.
     */
	private synchronized void loadStampData()
	{
		Runnable runme = new Runnable() {

			public void run() {

				StampTask task = startTask();
				task.updateStatus(Status.RED);

				ArrayList<StampInterface> newStamps = new ArrayList<StampInterface>();
				Class[] newColumnClasses=new Class[0];
				String[] newColumnNames=new String[0];

				ProgressDialog dialog=null;
				ObjectInputStream ois=null;

				try
				{       
					log.println("start of main database query: " + settings.queryStr);

					dialog = new ProgressDialog(Main.mainFrame, StampLayer.this); 

					String urlStr = settings.queryStr;

					dialog.updateStatus("Requesting stamps from server....");

					GZIPInputStream zipStream = new GZIPInputStream(StampLayer.queryServer(urlStr, 120));

					ois = new ObjectInputStream(zipStream);

					ResponseInfo info = (ResponseInfo)ois.readObject();
					if (info.getStatus()!=0) {
						JOptionPane.showMessageDialog(
								Main.mainFrame,
								info.getMessage(),
								"Query Result",
								JOptionPane.INFORMATION_MESSAGE
						);
					}

					dialog.updateStatus("Server completed query");

					newColumnClasses = (Class[])ois.readObject();
					newColumnNames = (String[])ois.readObject();

					dialog.updateStatus("Retrieving " + info.getRecordCnt() + " stamps...");

					int recordsToRead=info.getRecordCnt();            
					int recordsRead=0;

					newStamps = new ArrayList<StampInterface>(recordsToRead);

					dialog.startDownload(0, recordsToRead);

					while (recordsToRead>recordsRead && !dialog.isCanceled()) {
						StampInterface newStamp = (StampInterface)ois.readObject();
						newStamps.add(newStamp);
						recordsRead++;
						if (recordsRead%100==0) {
							dialog.downloadStatus(recordsRead);
						}

						dialog.setNote("Retrieving: " + recordsRead + " of " + recordsToRead);
					}

					dialog.downloadStatus(recordsRead);

					ois.close();
					zipStream.close();

					if (dialog.isCanceled()) {
						dialog.close();
						return;
					}

					int numRecords = newStamps.size();
					log.println("main database: read " + numRecords + " records");

					if (numRecords < 1)
					{
						String msg = "No stamps match the specified filter";
						throw new NoStampsException(msg);
					}
				}
				catch (NoStampsException ex) {
					log.println(ex);
					String msg = "No records matched your search criteria.  " +
					"Please try again with a different query to create the view.";
					msg = Util.lineWrap(msg, 55);
					JOptionPane.showMessageDialog(
							Main.mainFrame,
							msg,
							"Query Result",
							JOptionPane.INFORMATION_MESSAGE
					);
				}
				catch (Exception e) {
					log.aprintln("Error occurred while downloading " + settings.instrument + " stamps");
					log.aprintln(e);
					String msg =
						newStamps.size() > 0
						? "Only able to retrieve " + newStamps.size()
								: "Unable to retrieve any";

						msg += " " + settings.instrument + " stamps from the database, due to:\n" + e;
						msg = Util.lineWrap(msg, 55);
						
						JOptionPane.showMessageDialog(
								Main.mainFrame,
								msg,
								"Database Error",
								JOptionPane.ERROR_MESSAGE
						);
				}
				finally {                    	
					task.updateStatus(Status.DONE);

					if (!dialog.isCanceled()) {
						dialog.close();
						
						// lock the StampLayer before updating it
						synchronized(StampLayer.this) {
							// update the projection
							originalPO = Main.PO;
							
							// Whatever the user query returns, include the currently rendered stamps in the new resultset
							List<FilledStamp> renderedStamps = viewToUpdate.getFocusPanel().getRenderedView().getFilled();
							
							stampMap.clear();
							cachedStamps = new ArrayList<StampShape>();
							
							for (int i=0; i<newStamps.size(); i++) {
								StampInterface s = newStamps.get(i);

								StampShape shape = null;
								
								if (vectorShapes()) {
									shape=new WindShape(s, StampLayer.this);
								} else if (pointShapes()) {
									shape=new PointShape(s, StampLayer.this);
								} else {
									shape=new StampShape(s, StampLayer.this);
								}

								cachedStamps.add(shape);
								stampMap.put(shape.getId().trim(), shape);
							}
							
							for (int i=0; i<renderedStamps.size(); i++) {
								FilledStamp fs = renderedStamps.get(i);

								StampShape shape = fs.stamp;
						
								// If a rendered stamp is in the newly returned result
								// set, remove the new value and store the rendered
								// one.  This ensures proper selection linkage between the outline
								// and rendered tabs.
								Object o=stampMap.get(shape.getId().trim());
								if (o==null) {
									cachedStamps.add(shape);
									stampMap.put(shape.getId().trim(), shape);
								} else {
									cachedStamps.remove(o);
									stampMap.remove(shape.getId().trim());
									
									cachedStamps.add(shape);
									stampMap.put(shape.getId().trim(), shape);									
								}
							}
								 
							// ADD IN ADDITIONAL FIELDS HERE FOR CALCULATED VALUES, COLORS, ETC?
							
							if (spectraData()) {
								Class[] expandedColumnClasses = new Class[newColumnClasses.length+3];
								String[] expandedColumnNames = new String[newColumnNames.length+3];
								
								System.arraycopy(newColumnClasses, 0, expandedColumnClasses, 0, newColumnClasses.length);
								System.arraycopy(newColumnNames, 0, expandedColumnNames, 0, newColumnNames.length);
								
								expandedColumnClasses[expandedColumnClasses.length-3]=Color.class;
								expandedColumnClasses[expandedColumnClasses.length-2]=Boolean.class;
								expandedColumnClasses[expandedColumnClasses.length-1]=Double.class;
								
								expandedColumnNames[expandedColumnNames.length-3]="Calculated Color";
								expandedColumnNames[expandedColumnNames.length-2]="Lock";
								expandedColumnNames[expandedColumnNames.length-1]="Calculated Value";
								
								newColumnClasses=expandedColumnClasses;
								newColumnNames=expandedColumnNames;
							}
						} 

						final Class[] colTypes = newColumnClasses;
						final String[] colNames = newColumnNames;
												
				        List<StampFilter> filters = viewToUpdate.getFilters();

				        renderColumn=-1;
						for (int i=0; i<newColumnNames.length; i++) {
							if (newColumnNames[i].equalsIgnoreCase("renderOptions")) {
								renderColumn=i;
							}
							for (StampFilter filter : filters) {
								if (newColumnNames[i].equalsIgnoreCase(filter.columnName+"_min")) {
									filter.dataIndex=i;
								} else if (newColumnNames[i].equalsIgnoreCase(filter.columnName+"_max")) {
									filter.dataIndex2=i;
								} else if (newColumnNames[i].equalsIgnoreCase(filter.columnName)) {
				    				filter.dataIndex=i;
								}
							}    				
						}

						// get on the AWT thread to update the GUI
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								// update the columns
								columnTypes=colTypes;
								columnNames=colNames;
								
								//check for null and set to empty arrays instead
								if(columnTypes == null || columnNames == null){
									columnTypes = new Class[0];
									columnNames = new String[0];
								}
								
								viewToUpdate.myFocus.updateData(columnTypes, columnNames, settings.initialColumns);								

								viewToUpdate.restoreStamps(settings.getStampStateList());
								updateVisibleStamps();
								
								queryThread=null;
							}
						});
						log.println("End of stamp data load");
					} else {
						log.println("Stamp data load cancelled");
					}
					if (ois!=null) {
						try {
							ois.close();
						} catch (Exception e) {

						}
					}
					
					if (viewToUpdate!=null) {
						viewToUpdate.viewChanged();
						Layer.LView childLView = viewToUpdate.getChild();
						if (childLView != null)
							childLView.viewChanged();
					}

				}
				
			}
		};
		
		queryThread = new Thread(runme);
		queryThread.start();
	}
	
    public Thread queryThread = null;
    
    private synchronized void reprojectStampData()
    {
        Iterator<StampShape> iterator = stampMap.values().iterator();
        while (iterator.hasNext()) {
            StampShape s = iterator.next();
            if (s != null)
                s.clearProjectedData();
        }
               
        originalPO = Main.PO;
    }

    public interface StampSelectionListener {
    	public void selectionsChanged();
    	public void selectionsAdded(List<StampShape> newStamps);
    }
    
    private class NoStampsException extends Exception
	{
    	NoStampsException(String msg)
		{
    		super(msg);
		}
	}
        
    public String getQuery() {
    	return settings.queryStr;
    }

	/**
	 * Loads stamp data from the main database.
	 */
	public synchronized void addStampData(StampShape newStamp)
	{
		// lock the StampLayer before updating it
		synchronized(StampLayer.this) {
			// update the projection
			originalPO = Main.PO;
						
			String id = newStamp.getId().trim();
			
			StampShape oldShape=stampMap.get(id);
			
			// remove old stamp if we have a new one with the same id
			if (oldShape!=null) {
				cachedStamps.remove(oldShape);
				stampMap.remove(id);
			}
			
			cachedStamps.add(newStamp);
			stampMap.put(newStamp.getId().trim(), newStamp);
			updateVisibleStamps();
		}
		 				
		// get on the AWT thread to update the GUI
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				viewToUpdate.restoreStamps(settings.getStampStateList());
			}
		});
			
		if (viewToUpdate!=null) {
			viewToUpdate.viewChanged();
			Layer.LView childLView = viewToUpdate.getChild();
			if (childLView != null)
				childLView.viewChanged();
		}			
	}
}
