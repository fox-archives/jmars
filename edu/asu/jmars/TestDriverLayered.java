package edu.asu.jmars;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;

import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.LViewFactory;
import edu.asu.jmars.layer.LViewManager;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.layer.SavedLayer;
import edu.asu.jmars.layer.SerializedParameters;
import edu.asu.jmars.layer.stamp.StampFactory;
import edu.asu.jmars.ruler.BaseRuler;
import edu.asu.jmars.ruler.RulerManager;
import edu.asu.jmars.swing.TabLabel;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.Util;
import edu.emory.mathcs.backport.java.util.Collections;

public class TestDriverLayered extends JPanel
{
    private static DebugLog log = DebugLog.instance();
    
	public ToolManager toolMgr;
    public  LocationManager locMgr;
    public  LViewManager    mainWindow;
    private LViewManager    panner;

    // used to contain the panner on top of the main view
    protected TabLabel statusBar;
    // used to contain the panner below the main view
    protected JSplitPane splitPane;
	protected JSplitPane totalPane;
    
    public static final int INITIAL_MAIN_ZOOM = 32;
	public static final int INITIAL_PANNER_ZOOM = 8;
	// TODO: Seems like this should be overridden by a property....
    public static final int INITIAL_MAX_ZOOM_LOG2 = 20;
    
    boolean ignorePreviousState = false;
    
	/**
	 * If the user assigns a custom name to a layer then it is stored in the customLayerNames Map.
	 */
	private Map<LView,String> customLayerNames = new HashMap<LView, String>();
	
    public TestDriverLayered()
    {
	// location manager - look for a save initial value
	String initialX = Main.userProps.getProperty("Initialx", "");
	String initialY = Main.userProps.getProperty("Initialy", "");
	String serverOffset = Main.userProps.getProperty("ServerOffset", "");
	
	locMgr = new LocationManager(Main.initialWorldLocation);
	
	if (initialX != "" && initialY != "" ) {
	    Point2D.Double pt = null;

	    try {

		double offsetX = Double.parseDouble(serverOffset) - Main.PO.getServerOffsetX();
		double newX = Double.parseDouble(initialX) + offsetX;

		pt = new Point2D.Double(newX, (new Double(initialY)).doubleValue());
	    } catch ( Exception ex) {
		//ignore error so default is simply null
	    }
	    if ( pt != null )
		locMgr.setLocation(pt, false);
	}
	
	// LViewManager - first get saved values
	int mainZoomLog2 = Util.log2(Main.userProps.getPropertyInt("MainZoom", INITIAL_MAIN_ZOOM));
	int pannerZoomLog2 = Util.log2(Main.userProps.getPropertyInt("PannerZoom", INITIAL_PANNER_ZOOM));
	int maxZoomLog2 = Config.get("maxzoomlog2", INITIAL_MAX_ZOOM_LOG2);
	
	mainWindow = new LViewManager(locMgr, new ZoomManager(mainZoomLog2, maxZoomLog2), null);
	panner	   = new LViewManager(locMgr, new ZoomManager(pannerZoomLog2, maxZoomLog2), mainWindow);
	
	// Create the status bar
	statusBar = new TabLabel(" ");
	statusBar.setFont(new JTextField().getFont());
	statusBar.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
	
	// lay them out
	setLayout(new BorderLayout());
	setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
	
	mainWindow.setMinimumSize(new Dimension( 20,  20));
	panner	  .setMinimumSize(new Dimension( 20,  20));
	panner	.setPreferredSize(new Dimension(100, 100));
	
	splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
	splitPane.setLeftComponent(mainWindow);
	splitPane.setResizeWeight(1.0);
	splitPane.setBorder(new CompoundBorder(
			BorderFactory.createEmptyBorder(10,0,10,0),
			BorderFactory.createBevelBorder(BevelBorder.LOWERED))
	    );
	
	// use the session value if defined, else use the config setting if defined, else use the old horizontal mode
	int index = Main.userProps.getPropertyInt("PannerMode", Config.get("panner.mode", PannerMode.Horiz.ordinal()));
	if (index < 0 || index >= PannerMode.values().length) {
		index = PannerMode.Horiz.ordinal();
	}
	setPannerMode(PannerMode.values()[index]);
	
	int height = Main.userProps.getPropertyInt("SplitPaneHeight", 500);
	int width =  Main.userProps.getPropertyInt("SplitPaneWidth",  600);
	int div = Main.userProps.getPropertyInt("MainDividerLoc", splitPane.getDividerLocation());
	splitPane.setPreferredSize( new Dimension( width, height));
	splitPane.setDividerLocation(div);
	
	//places the lviews inside a larger split pane that will also
	// contain the lmanager
	totalPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
	totalPane.setRightComponent(splitPane);
	totalPane.setBorder(BorderFactory.createEmptyBorder());
	//creates a toolbar
	toolMgr = new ToolManager();
	
	//creates the top panel of the main view
	JPanel top = new JPanel(new BorderLayout());
	top.add(locMgr, BorderLayout.WEST);
	top.add(toolMgr, BorderLayout.CENTER);
	top.add(mainWindow.getZoomManager(), BorderLayout.EAST);
	
	//adds all the pieces to the window
	add(top,       BorderLayout.NORTH);
	add(totalPane, BorderLayout.CENTER);
	add(bottomRow, BorderLayout.SOUTH);
	setMetersVisible(Config.get("main.meters.enable", false));
    }
    
	private final JPanel bottomRow = new JPanel(new GridBagLayout());
	private final JProgressBar memmeter = new JProgressBar(0,100);
	{
		memmeter.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				log.println("gc running");
				System.gc();
				log.println("gc finished");
			}
		});
	}
	private Timer meterTimer;
	
	/**
	 * @param visible
	 *            If true, will make sure the meter component is next to the
	 *            status bar, otherwise will make sure the status bar has the
	 *            whole bottomRow component to itself.
	 */
	public void setMetersVisible(boolean visible) {
		Insets in = new Insets(0,0,0,0);
		// always cancel an existing timer and clear all components
		if (meterTimer != null) {
			meterTimer.cancel();
			meterTimer = null;
		}
		bottomRow.removeAll();
		if (visible) {
			// insert meter next to status bar and start new timer
			meterTimer = new Timer("memmeter-updater", true);
			final TimerTask meterUpdater = new TimerTask() {
				public void run() {
					Runtime r = Runtime.getRuntime();
					final long max = r.maxMemory();
					final long used = r.totalMemory() - r.freeMemory();
					final int percent = (int)Math.round(100d * used / max);
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							memmeter.setValue(percent);
							memmeter.setToolTipText(MessageFormat.format(
								"Memory: {0}% of {1} MB used, click to clean",
								percent, Math.round(max/1024/1024)));
						}
					});
				}
			};
			meterTimer.scheduleAtFixedRate(meterUpdater, new Date(), 1000);
			bottomRow.add(statusBar, new GridBagConstraints(0,0,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,new Insets(0,0,0,4),0,0));
			bottomRow.add(memmeter, new GridBagConstraints(1,0,1,1,0,0,GridBagConstraints.EAST,GridBagConstraints.NONE,in,0,0));
		} else {
			// ensure status bar has entire bottom row to itself
			bottomRow.add(statusBar, new GridBagConstraints(0,0,2,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,0,0));
		}
		Config.set("main.meters.enable", visible);
		bottomRow.validate();
		bottomRow.repaint();
	}
    
    public Dimension getMainLViewManagerSize() {
	return mainWindow.getSize();
    }
    
    public void setMainLViewManagerSize(Dimension d) {
	mainWindow.setSize(d);
	mainWindow.validate();
	mainWindow.repaintChildVMan();
    }
    
    public void dumpMainLViewManagerJpg(String filename) {
	mainWindow.dumpJpg(filename);
    }
    
    public void dumpMainLViewManagerPNG(String filename) {
        mainWindow.dumpPNG(filename);
    }

    public void dumpMainLViewManagerTif(String filename) {
        mainWindow.dumpTIF(filename);
    }

	// Get the properties of any defined rulers and general ruler properties.
	public void loadRulersState()
	{
		
		Hashtable<String,Object> allRulerSettings = (Hashtable<String,Object>)Main.userProps.loadUserObject( "AllRulerSettings");
		if (allRulerSettings != null){
			RulerManager.Instance.loadSettings( allRulerSettings);
		}

		int rulerCount = Main.userProps.getPropertyInt("RulerCount", 0);
		for (int j=0; j < rulerCount; j++) {
			String rulerLabel = "Ruler" + String.valueOf(j);
			String rulerName = Main.userProps.getProperty( rulerLabel, "");
			BaseRuler ruler = (BaseRuler)RulerManager.Instance.getRuler( rulerName);
			if (ruler!=null){
				Hashtable<String,Object> rulerSettings = (Hashtable<String,Object>)Main.userProps.loadUserObject( rulerLabel + "Settings");
				ruler.loadSettings( rulerSettings);
				if (j==0){
					Hashtable<String,Object> settings = (Hashtable<String,Object>)Main.userProps.loadUserObject( "BaseRulerSettings");
					ruler.loadBaseRulerSettings( settings);
				}
			} 
		}
	}
	
	// Save the properties of any defined rulers and general ruler properties.
	public void saveRulersState()
	{
		Hashtable<String,Object> allRulerSettings = RulerManager.Instance.saveSettings();
		if (allRulerSettings != null){
			Main.userProps.saveUserObject( "AllRulerSettings", allRulerSettings);
		}

		int rulerCount = RulerManager.Instance.rulerList.size();
		Main.userProps.setPropertyInt("RulerCount", rulerCount);
		for (int j=0; j < rulerCount; j++) {
			BaseRuler ruler = (BaseRuler)RulerManager.Instance.rulerList.get(j);
			Main.userProps.setProperty( "Ruler" + String.valueOf(j), ruler.getClass().getName());
			Hashtable<String,Object> rulerSettings = ruler.saveSettings();
			Main.userProps.saveUserObject( "Ruler" + String.valueOf(j) + "Settings", rulerSettings);
			if (j==0){
				Hashtable<String,Object> settings = ruler.saveBaseRulerSettings();
				Main.userProps.saveUserObject( "BaseRulerSettings", settings);
			}
		}
	}

	public void saveState() {
		Main.userProps.saveWindowPosition(Main.mainFrame);
		// save the general JMARS stuff.
		if (!LManager.getLManager().isDocked()) {
			Main.userProps.saveWindowPosition(LManager.getDisplayFrame());
		}
		try
		{
			Main.userProps.setProperty("LManager.tabDocking",
					LManager.getLManager().getDockingStates());
		}
		catch(Exception e)
		{
			log.aprintln(e);
			log.aprintln("Failed to save layer manager tab locations");
		}
		Main.userProps.setProperty("selectedBody", Main.getCurrentBody());
		Main.userProps.setProperty("versionNumber", Util.getVersionNumber());
		Main.userProps.setProperty(Main.SESSION_KEY_STR, Main.getSessionKey());
		Main.userProps.setProperty("SplitPaneHeight", String.valueOf(splitPane.getSize().height));
		Main.userProps.setProperty("SplitPaneWidth", String.valueOf(splitPane.getSize().width));
		Main.userProps.setProperty("jmars.user", Main.USER);
		Main.userProps.setPropertyInt("MainDividerLoc",	     splitPane.getDividerLocation());
		Main.userProps.setPropertyInt("MainZoom",	 mainWindow.getZoomManager().getZoomPPD());
		Main.userProps.setPropertyInt("PannerZoom",	 panner.getZoomManager().getZoomPPD());
		int pannerModeOrd = Config.get("panner.mode", -1);
		if (pannerModeOrd >= 0 && pannerModeOrd < PannerMode.values().length) {
			Main.userProps.setPropertyInt("PannerMode", pannerModeOrd);
		}
		Main.userProps.setProperty("Initialx",		 String.valueOf( locMgr.getLoc().getX() ));
		Main.userProps.setProperty("Initialy",		 String.valueOf( locMgr.getLoc().getY() ));
		// note: JMARS west lon => USER east lon
		Main.userProps.setProperty("Projection_lon",	     String.valueOf((360-Main.PO.getCenterLon())%360));
		Main.userProps.setProperty("Projection_lat",	     String.valueOf(Main.PO.getCenterLat()));
		Main.userProps.setProperty("ServerOffset",	 String.valueOf(Main.PO.getServerOffsetX()));
		LManager.getLManager().saveState();
		
		// Set the general ruler properties.
		RulerManager.Instance.saveSettings();

		// Set the properties of any defined rulers.
		saveRulersState();

		// Set the properties of any defined views. 
		Main.userProps.setPropertyInt("ViewCount", mainWindow.viewList.size());
		Iterator iterViews = mainWindow.viewList.iterator();
		int i=1;
		while(iterViews.hasNext()) {
			Layer.LView lview = (Layer.LView) iterViews.next();
			String basename = "View" + String.valueOf(i);
			if(lview.originatingFactory == null)
				continue;
			
			Main.userProps.setProperty(basename, lview.originatingFactory.getClass().getName());

			//Store the views starting parms in a file if available
			SerializedParameters parms = lview.getInitialLayerData();
			if ( parms != null ) {
				Main.userProps.saveUserObject(basename + "Parms", parms);
			}

			//Store the views current settings in a file if available
			Hashtable sparms = lview.getViewSettings();
			if ( sparms != null ) {
				Main.userProps.saveUserObject(basename + "Settings", sparms);
			}
			
			String customName = customLayerNames.get(lview);
			if (customName != null && customName.length() > 0) {
				Main.userProps.setProperty(basename + "Name", customName);
			}
			i++;
		}
		saveBodyLayerFiles();
	}
	private void saveBodyLayerFiles() {
		String path = Main.getBodyBaseDir();
		File directory = new File(Main.getJMarsPath()+path);
		if (directory.exists()) {
			File[] fileList = directory.listFiles();
			HashMap<String, ArrayList<SavedLayer>> bodyMap = new HashMap<String, ArrayList<SavedLayer>>();
			InputStream is = null;
			for(File oneFile : fileList) {
				try {
					 is = new FileInputStream(oneFile);
					ArrayList<SavedLayer> list = (ArrayList<SavedLayer>) SavedLayer.load(is);
					bodyMap.put(oneFile.getName(), list);
				} catch (Exception e) {
					log.aprintln(e);
					log.aprintln("Failed to save layer for "+oneFile.getName());
				} finally {
					try {
						is.close();
					} catch (IOException e) {
						log.println(e);
						e.printStackTrace();
					}
				}
			}
			Main.userProps.saveUserObject(Main.BODY_FILE_MAP_STR, bodyMap);
		}
	}
	// builds the views if there were any defined in the application properties, 
	public void buildViews()
	{
		//Determine if there were saved views
		int viewCnt = Main.userProps.getPropertyInt("ViewCount", 0);

		if (Main.savedLayers != null) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					Collections.reverse(Main.savedLayers);
					for (SavedLayer layer: Main.savedLayers) {
						layer.materialize();
					}
					// saved layers could be large, so let the GC reap away
					Main.savedLayers = null;
				}
			});
		} else if ( viewCnt == 0 || ignorePreviousState ) {
			// If we aren't getting things from an init file, build default views.
			// For right now: just create one default version of every
			// possible view
			Iterator iter = LViewFactory.factoryList.iterator();
			while(iter.hasNext()){
				Layer.LView view;
				LViewFactory factory = (LViewFactory) iter.next();
				view=factory.createLView();
				if(view != null) {
					LManager.receiveNewLView(view);
				}
			}
		} else {
			// If we ARE getting views from an init file, build them now.
			Layer.LView view = null;
			for ( int i=1; i <=viewCnt; i++ ) {
				String basename = "View" + String.valueOf(i);
				String factoryName = Main.userProps.getProperty(basename, "");
				try {
					// Look for a serialized initial parameter block and start the view with the
					// data if present.
					LViewFactory factory = LViewFactory.getFactoryObject(factoryName);
					// TODO: serialization formats change, and code to adapt from an
					// old form to a new form WILL ABSOLUTELY BE REQUIRED. We just
					// need a mechanism to make bolting in such code less of a hack.
					if (factory == null && factoryName.endsWith("StampFactory")) {
						factory = StampFactory.createAdapterFactory(factoryName);
					}
					
					if ( factory != null ) {
						SerializedParameters obj = (SerializedParameters) Main.userProps.loadUserObject(basename + "Parms");
						view = factory.recreateLView(obj);
						if (view != null) {
							LManager.receiveNewLView(view);
							Hashtable sobj =  (Hashtable) Main.userProps.loadUserObject(basename + "Settings");
							if ( sobj != null ){
								view.setViewSettings(sobj);
								LManager.getLManager().updateVis();
							}
							String customName = (String)Main.userProps.getProperty(basename + "Name");
							if (customName != null && customName.length() > 0) {
								customLayerNames.put(view, customName);
								LManager.getLManager().updateLabels();
							}
						}
					}
				} catch (Exception e) {
					log.aprintln("Failure recreating instance of " + factoryName + ", caused by:");
					log.aprintln(e);
				}
			}
			LManager.getLManager().loadState();
		}
	} // end: buildViews()

	/** Recenters all LViewManagers to a new location given by p */
	public void offsetToWorld(Point2D p) {
		mainWindow.getGlassPanel().offsetToWorld(p);
		panner.getGlassPanel().offsetToWorld(p);
		locMgr.setLocation(p, true);
	}
	
	public Map<LView,String> getCustomLayerNames() {
		return customLayerNames;
	}
	
	public void setPannerMode(PannerMode mode) {
		if (panner == null || mode == null) {
			log.aprintln("Null panner or mode, panner controls programmed incorrectly");
		} else {
			switch (mode) {
			case Horiz:
				splitPane.setRightComponent(panner);
				splitPane.setDividerSize(new JSplitPane().getDividerSize());
				panner.setSize(mainWindow.getWidth(), 150);
				break;
			case Off:
				splitPane.setRightComponent(null);
				splitPane.setDividerSize(0);
				break;
			}
			Config.set("panner.mode", ""+mode.ordinal());
		}
	}
	
	public static enum PannerMode {
		// Must append new options to end of this enum, to avoid
		// changing the ordinates of existing values.
		Horiz("Horizontal"), Off("Off");
		PannerMode(String title) {
			this.title = title;
		}
		public final String title;
	}
	
	public void setLManagerMode(LManagerMode mode) {
		if (LManager.getLManager() == null || mode == null) {
		} else {
			switch (mode) {
			case Verti:
				totalPane.setLeftComponent(LManager.getLManager());
				totalPane.setDividerSize(new JSplitPane().getDividerSize());
				break;
			case Off:
				totalPane.setLeftComponent(null);
				totalPane.setDividerSize(0);
				break;
			}
		}
	}

	public static enum LManagerMode {
		Verti("Vertical"), Off("Off");
		LManagerMode(String title) {
			this.title = title;
		}

		public final String title;
	}

} // end: class TestDriverLayered
