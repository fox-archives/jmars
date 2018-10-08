package edu.asu.jmars.layer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.apache.commons.collections.ReferenceMap;

import edu.asu.jmars.Main;
import edu.asu.jmars.TestDriverLayered.LManagerMode;
import edu.asu.jmars.ToolManager;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.layer.investigate.InvestigateFactory;
import edu.asu.jmars.lmanager.MainPanel;
import edu.asu.jmars.lmanager.Row;
import edu.asu.jmars.swing.DockableTabs;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.MovableList;
import edu.asu.jmars.util.Util;

/**
 *  The LManager is a GUI component that provides the user with an interface to
 *  manipulate the position and visibility of existing layer views. 
 *  
 *  The LManager encapsulates a tabbed interface that allows for optional adding of 
 *  FocusPanels for each Layer which can be torn off and redocked as desired.
 *  
 *  The entire LManager UI can be docked with the main JMARS window or torn off into
 *  its own independent JFrame.
 */

public class LManager extends JPanel {
	private static DebugLog log = DebugLog.instance();

	// There is only one instance of LManager for the entire JMARS application.  Some common functions are exposed as static methods for 
	// convenience, but nearly everything ultimately works against this single instance.
	private static LManager instance = null;
	public static LManager getLManager() {
		return instance;
	}

	// The list of LViews being managed by LManager, in top down order of how they're displayed to the user in the MainView.
	// This probably shouldn't be public.
	public MovableList<LView> viewList;
	
	// This frame is used when the LManager is torn away from the main JMARS window
	private JFrame parentFrame;	
	
	// The component for managing docked AddLayer and/or FocusPanels
	private DockableTabs dockTabs;
	
	// This is the panel with most of the LManager specific functionality, including the Add and Delete layer functionality
	private MainPanel mainPanel;
	
	// A variable that defines which style of adding layers will display (old - dropdown menu, new - AddLayer jframe)
	public String addLayerStyle;
	
	public LManager(LViewManager viewman) {
		//super("Layer Manager", Util.getAltDisplay());

		instance = this;

		viewList = viewman.viewList;

		// Configured the parentFrame for future use, although we start docked by default
		parentFrame = new JFrame("Layer Manager");
		parentFrame.setIconImage(Util.getJMarsIcon());
		parentFrame.setMinimumSize(new Dimension(375, 500));
		parentFrame.setVisible(false);
		parentFrame.addWindowListener(new WindowAdapter() {
		    public void windowClosing(WindowEvent e){ 		
			parentFrame.setVisible(false);

			setDocked(true);
			Main.testDriver.setLManagerMode(LManagerMode.Verti);
			} 
		});

		setLayout(new BorderLayout());

		
		addLayerStyle = Config.get("addLayerStyle", "new");
		mainPanel = new MainPanel();

		dockTabs = new DockableTabs();
		dockTabs.addTab("Main", null, mainPanel);
		
		add(dockTabs, BorderLayout.CENTER);
	}

	// Keeps track of whether LManager is docked with the main JMARS window or torn off into it's own JFrame
	public boolean isDocked = true;
	public boolean isDocked() {
		return isDocked;
	}
	
	public void setDocked(boolean newState) {
		isDocked=newState;
	}
		
	// When the LManager is torn away from the main JMARS window, this sets it up in it's own JFrame
    public void showInFrame(int x, int y) {
		parentFrame.setVisible(false);
		parentFrame.setContentPane(this);
		parentFrame.pack();
		// If it's minimized, this will bring the window back up.
		if(parentFrame.getState()!=parentFrame.NORMAL) { parentFrame.setState(parentFrame.NORMAL); }
		parentFrame.toFront();
		parentFrame.setLocation(x,y);
		parentFrame.setVisible(true);
		isDocked=false;
		Main.testDriver.setLManagerMode(LManagerMode.Off);
    }
	
    // This re-docks the LManager with the main JMARS window
    public void dock() {
    	parentFrame.setVisible(false);
    	isDocked=true;
		Main.testDriver.setLManagerMode(LManagerMode.Verti);
    }
    
    public void setInitialState() {
		setActiveLView(viewList.size() - 1);
		if (dockTabs.getComponentCount() > 0) {
			dockTabs.setSelectedIndex(0);
		}
    }
    
    // This is used to dock a FocusPanel
    public void dockTab(String name, Icon icon, Component fp, boolean selectTab) {
		dockTabs.addTab(name, icon, fp);
		if (selectTab) {
			int index = dockTabs.getComponentCount()-1;
			dockTabs.activateTab(index);
		}
    }
    
    public void dockAddLayer(String name, Component c) {
    	dockTabs.insertTab(name, null, c, null, 1);
    }
	
    public void activateTab(int idx) {
    	dockTabs.activateTab(idx);
    }
    
    public void repaint() {
    	super.repaint();
    	if (dockTabs!=null) 
    		dockTabs.repaint();    	
    }
    
    
    // Rebuilds the old style AddMenu when things such as the MapLayer and SessionsLayer finish their asynchronous initialization
	public void refreshAddMenu(){
		refreshAddMenu(false);
	}
	
    public void refreshAddMenu(boolean initCat) {
		if (addLayerStyle.equals("old")){
			mainPanel.rebuildAddMenu();
		}else{	
			if(initCat){
				AddLayer.setInitialCategory();
			}
			AddLayer.getInstance().rebuildCategoryBox();
		}
	}
	
	private int selectedIdx = 0;

	/**
	 * Delete a layer with a check for unsaved data
	 */
	public void deleteSelectedLayer() {
		//don't do anything if there are no views left
		if(viewList.size()<=0){
			return;
		}
		
		LView view = (LView) viewList.get(selectedIdx);
		
		if(view.getName().equalsIgnoreCase("Investigate Layer")){
			//Cannot delete investigate layer if in investigate mode
			if(ToolManager.getToolMode() == ToolManager.INVESTIGATE){
				JOptionPane.showMessageDialog(getLManager(), "ERROR: Cannot delete Investigate Layer while in Investigate Mode",
											  "Investigate Layer Error", JOptionPane.ERROR_MESSAGE);
				return;
			}else{
				//Delete layer and set exists flag to false
				if (view.deleted()) {//check for unsaved data
					this.deleteLayerNoCheckForUnsavedData(view);//@since change bodies
				}
				InvestigateFactory.setLviewExists(false);
				Main.testDriver.mainWindow.requestFocus(true);
				return;
			}
		}
		if (view.deleted()) {//check for unsaved data
			this.deleteLayerNoCheckForUnsavedData(view);//@since change bodies
		}
		Main.testDriver.mainWindow.requestFocus(true);
	}
	
	public void deleteLayerNoCheckForUnsavedData(LView view) {
		// remove the custom view name if it is set.
		Main.testDriver.getCustomLayerNames().remove(view);

		// Remove the actual view
		int deleteIdx = viewList.indexOf(view);
		selectedIdx = deleteIdx;
		viewList.remove(selectedIdx);
		nameToLayer.remove(layerToName.remove(view));
		Main.testDriver.repaint();

		FocusPanel fp = view.getFocusPanel();
		int index = dockTabs.indexOfComponent(fp);
		if (index != -1)
			dockTabs.remove(index);

		// Remove the row... the row indices are reversed
		// relative to everything else.
		Row r = (Row) mainPanel.rows.remove(mainPanel.rows.size() - 1 - selectedIdx);
		mainPanel.rowsPanel.remove(r);
		mainPanel.rowsPanel.repaint();
		if (mainPanel.rowScrollPane != null) {
			mainPanel.rowScrollPane.revalidate();
			mainPanel.rowScrollPane.repaint();
		}

		// Finally: update the selected index, if we deleted
		// the highest one.
		if (selectedIdx >= mainPanel.rows.size())
			setActiveLView(mainPanel.rows.size() - 1);
		else
			mainPanel.updateRows();

		JFrame frame = fp.getFrame();
		if (frame != null) {
			frame.setVisible(false);
		}
	}

	private final Map<String, LView> nameToLayer = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.WEAK);
	private final Map<LView, String> layerToName = new ReferenceMap(ReferenceMap.WEAK, ReferenceMap.HARD);

	/*
	 * Use this for Dialog boxes and other settings that want a frame to display relative to.
	 * This will return the LManager frame if there is a separate frame, or the Main window otherwise
	 */
	public static JFrame getDisplayFrame() {
		if (LManager.getLManager().isDocked) {
			return Main.mainFrame;
		} else {
			return instance.parentFrame;
		}		
	}

	/**
	* @since change bodies
	*/
	public MovableList getViewList() {
		return this.viewList;
	}
	/**
	 ** Returns a string representing the docking states of the LManager tabs,
	 * which can be fed to {@link #setDockingStates}.
	 **/
	public String getDockingStates() {
		Properties prop = new Properties();
		prop.setProperty("lmanagerVersion", "1");
		prop.setProperty("viewCount", Integer.toString(viewList.size()));
		for (int i = 0; i < viewList.size(); i++) {
			Layer.LView view = viewList.get(i);
			prop.setProperty("view" + i + ".title", getUniqueName(view));
			prop.setProperty("view" + i + ".type", view.getClass().getName());
			JFrame parentFrame = view.getFocusPanel().parentFrame;
			if (parentFrame != null && parentFrame.isVisible()) {
				prop.setProperty("view" + i + ".bounds", parentFrame.getX() + ","
						+ parentFrame.getY() + "," + parentFrame.getWidth() + ","
						+ parentFrame.getHeight());
			}
		}

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			prop.store(out, null);
		} catch (IOException e) {
			log.aprintln(e);
			throw new RuntimeException(
					"IMPOSSIBLE: I/O error writing to a string!", e);
		}

		String stateString = out.toString();

		log.println("--- Saving as:");
		log.println(stateString);

		return stateString;
	}

	public static void repaintAll() {
		if (instance==null) return;
		
		if (instance.viewList != null) {//prevent NullPointerException on autosave load
			for (LView view : instance.viewList) {
				FocusPanel fp = view.getFocusPanel();
				if (fp != null) {
					fp.repaint();
				}
			}
		}
	}

	/**
	 ** Restores the lmanager tabs' docking states to those stored in a string
	 * previously created from {@link #getDockingStates}.
	 **/
	public void setDockingStates(String stateString) {
		if (stateString == null)
			return;

		Properties prop = new Properties();
		try {
			prop.load(new ByteArrayInputStream(stateString.getBytes()));
		} catch (Exception e) {
			log.aprintln(e);
			log.aprintln("Failed to set layer manager tab positions");
			return;
		}

		log.println("Opening version " + prop.getProperty("lmanagerVersion"));
		int viewCount = Integer.parseInt(prop.getProperty("viewCount"));
		for (int i = 0; i < viewCount ; i++) {
			String boundStr = prop.getProperty("view" + i + ".bounds");
			if (boundStr != null) {
				String[] boundA = boundStr.split(",");
				viewList.get(i).getFocusPanel().restoreInFrame(Integer.parseInt(boundA[0]), Integer.parseInt(boundA[1]), Integer.parseInt(boundA[2]), Integer.parseInt(boundA[3]));				
			}
		}
	}

	public void setActiveLView(Layer.LView view) {
		int idx = viewList.indexOf(view);
		setActiveLView(idx);
	}

	/**
	 * Returns the custom view name if one is set, or the default name if a
	 * custom name is not set.
	 * 
	 * @param view
	 * @return a string containing the view name.
	 */
	private String getViewName(LView view) {
		String viewName = Main.testDriver.getCustomLayerNames().get(view);
		if (viewName == null || viewName.equals("")) {
			viewName = view.getName();
		}
		return viewName;
	}

	public void accessSelectedOptions(boolean openDocked) {
		LView view = (LView) viewList.get(selectedIdx);

		FocusPanel info = view.getFocusPanel();
		if (openDocked){
			info.dock(true);
			return;
		}
		
		JFrame myFrame = info.getFrame();
		int index = dockTabs.indexOfComponent(info);
		if (index != -1) {
			dockTabs.activateTab(index);
		} else {
			if (myFrame.isVisible()){
				myFrame.toFront();
			}else{
				info.showInFrame();
				myFrame.setLocationRelativeTo(LManager.getDisplayFrame());
			}
		}
	}

	public void renameSelectedLayer() {
		final LView view = (LView) viewList.get(selectedIdx);
		final JMenuItem changeLayerName = new JMenuItem("Rename");

		
		String newName = (String) JOptionPane
				.showInputDialog(
						changeLayerName,
						"Enter the new name (set to blank to return to the default layer name).",
						"Change Layer Name", JOptionPane.QUESTION_MESSAGE,
						null, null, getUniqueName(view));
		if (newName != null && !newName.equals(getViewName(view))) {
			Main.testDriver.getCustomLayerNames().put(view, newName);
			updateLabels();
		}
	}

	public void showTooltipForSelectedLayer(boolean show){
		final LView view = (LView) viewList.get(selectedIdx);
		view.tooltipsDisabled(!show);
	}
	
	public boolean getTooltipDisabledStatus(){
		return viewList.get(selectedIdx).tooltipsDisabled();
	}
	
	public void displayAddNewLayer() {
		//change tool mode to default
		ToolManager.setToolMode(ToolManager.SEL_HAND);
		AddLayer.displayAddLayer(LManager.instance);
	}

	/**
	 ** Sets the active view. Note: index references the views ordered top to
	 * bottom (as seen on screen in the lmanager).
	 **/
	public void setActiveLView(int newIdx) {
		selectedIdx = newIdx;
		mainPanel.updateRows();
		
		Layer.LView view = getActiveLView();
		if (view != null)
			view.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
	}

	public Layer.LView getActiveLView() {
		if (!Util.between(0, selectedIdx, viewList.size() - 1))
			return null;
		return viewList.get(selectedIdx);
	}

	public static void receiveNewLView(Layer.LView view) {
		LManager.getLManager().addNewLView(view);
	}
	
	public void addNewLView(Layer.LView view) {
		viewList.add(view);
		
		//need to do this before setting visible so that viewman is not null
		//It does not appear that there is a better way to verify that the view has been added to the LViewManager.
		//KJR - 6/6/12
		if (view.getParent() instanceof LViewManager) {
			view.viewman = (LViewManager) view.getParent();
			if (view.getChild() != null) {
				view.getChild().viewman = view.viewman.getChild();
			}
		}

		view.setVisible(view.mainStartEnabled(), view.pannerStartEnabled());
		mainPanel.addView(view);
		setActiveLView(viewList.size() - 1);
		updateLabels();
	}

	/**
	 * Returns a unique name for this view. Layers are disambiguated by by
	 * adding a suffix like ' (2)', ' (3)', etc. The numbers are added in the
	 * order receiveNewLView is called.
	 */
	public String getUniqueName(LView newView) {
		String newName = getViewName(newView);
		String oldName = layerToName.get(newView);
		if (oldName != null) {
			// there is an existing entry for this layer
			if (oldName.equals(newName)
					|| (oldName.startsWith(newName) && oldName.substring(
							newName.length()).matches(" ([0-9]+)$"))) {
				// the existing layer name has the same prefix so reuse it
				return oldName;
			} else {
				// the prefix changed so remove the layer here, and reinsert it
				// below
				nameToLayer.remove(layerToName.remove(newView));
			}
		}
		// at this point we must find a new name for this layer
		LView used = nameToLayer.get(newName);
		if (used == null) {
			// name is not in use
			nameToLayer.put(newName, newView);
			layerToName.put(newView, newName);
			return newName;
		} else {
			// name is in use by another layer. look for alternate endings that
			// are not used, but don't look past 50 so we can guarantee the
			// search ends quickly
			for (int suffix = 2; suffix <= 50; suffix++) {
				String suffixName = newName + " (" + suffix + ")";
				if (!nameToLayer.containsKey(suffixName)) {
					nameToLayer.put(suffixName, newView);
					layerToName.put(newView, suffixName);
					return suffixName;
				}
			}
			// if we got here there were MANY views with the same name
			return newName + " (...)";
		}
	}



	/**
	 ** Causes the LManager to notice any changes in the layer names (i.e. the
	 * value of LView.getName()), and to propagate those changes to the tabs,
	 * the view list, and the menus.
	 **/
	public void updateLabels() {
		for (int i = 0; i < mainPanel.rows.size(); i++) {
			Row r = (Row) mainPanel.rows.get(i);
			String name = getUniqueName(r.getView());
			r.setText(" " + name);
			JFrame frame = r.getView().getFocusPanel().getFrame();
			if (frame != null) {
				frame.setTitle(name + " Options");
				//To avoid a problem with the timeseries layer...make sure the 
				// focuspanel exists before setting the name.
				if(r.getView().focusPanel==null){
					r.getView().focusPanel = r.getView().getFocusPanel();
				}
				r.getView().focusPanel.infoPanel.updateName();
			}
			int idx = dockTabs.indexOfComponent(r.getView().getFocusPanel());
			if (idx != -1)
				dockTabs.setTitleAt(idx, name + " Options");
		}
	}

	public void updateVis() {
		for (Row r : mainPanel.rows) {
			r.updateVis();
		}
	}

	/** Restores the state of this LManager from the user properties on Main. */
	public void loadState() {
		// TODO: Review this for correctness
		setActiveLView(Main.userProps.getPropertyInt("activeViewIdx", 0));
		int selectedTab = Main.userProps.getPropertyInt("selectedTab", 0);
		//@since 3.0.3 - generally the check below will not be needed, but if a layer fails to loads for whatever reason,
		//the selectedTab can be greater than the number of tabs and throw an Exception. This will just prevent that situation.
		if (selectedTab >= dockTabs.getComponentCount()) {
			selectedTab = 0;
		}
		dockTabs.setSelectedIndex(selectedTab);
		boolean b = Main.userProps.getPropertyBool("lManagerDocked", true);
		if (!b) {
			dockTabs.remove(this);
			String boundStr = Main.userProps.getProperty("lManagerBounds");
			if (boundStr != null) {
				String[] boundA = boundStr.split(",");
				showInFrame(Integer.parseInt(boundA[0]), Integer.parseInt(boundA[1]));
				parentFrame.setSize(Integer.parseInt(boundA[2]), Integer.parseInt(boundA[3]));				
			}
			parentFrame.setVisible(true);
		} 
	}

   public void loadLManagerState() {
       if(!Main.userProps.setWindowPosition(parentFrame)) {
		   Dimension currSize = parentFrame.getSize();
		   parentFrame.setSize(currSize.width, 500);
		}
		
	    validate();

	    try {
		   LManager.getLManager().setDockingStates(
		       Main.userProps.getProperty("LManager.tabDocking"));
		} catch(Exception e) {
		   log.aprintln(e);
		   log.aprintln("Failed to set layer manager tab positions");
		}
		
       if(!Main.userProps.wasWindowShowing(parentFrame)) {
    	   parentFrame.setVisible(false);
       }
	}
	
	/** Adds the current state of this LManager to the user properties on Main. */
	public void saveState() {
		Main.userProps.setProperty("activeViewIdx", String.valueOf(selectedIdx));
		Main.userProps.setProperty("selectedTab", String.valueOf(dockTabs.getSelectedIndex()));
		Main.userProps.setProperty("lManagerDocked", Boolean.toString(isDocked));
		
		Component parent = mainPanel.getParent();
		while (parent.getParent()!=null) {
			parent=parent.getParent();
		}
		Main.userProps.setProperty("lManagerBounds", parent.getX() + ","
				+ parent.getY() + "," + parent.getWidth() + ","
				+ parent.getHeight());

	}
}
