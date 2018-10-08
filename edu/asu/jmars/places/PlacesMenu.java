package edu.asu.jmars.places;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import com.thoughtworks.xstream.XStream;

import edu.asu.jmars.LocationListener;
import edu.asu.jmars.Main;
import edu.asu.jmars.ZoomListener;
import edu.asu.jmars.layer.ProjectionEvent;
import edu.asu.jmars.layer.ProjectionListener;
import edu.asu.jmars.layer.util.features.FPath;
import edu.asu.jmars.layer.util.features.Feature;
import edu.asu.jmars.layer.util.features.FeatureCollection;
import edu.asu.jmars.layer.util.features.FeatureProviderCSV;
import edu.asu.jmars.layer.util.features.Field;
import edu.asu.jmars.layer.util.features.SingleFeatureCollection;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.Util;

/**
 * Creates a menu structure like this:
 * 
 * <pre>
 * JMenu: Places
 *   JMenu: Options
 *     JCheckBoxMenuItem: Restore zoom on selection
 *     JCheckBoxMenuItem: Restore location on selection
 *     JCheckBoxMenuItem: Restore projection on selection
 *   JMenuItem: Previous Place
 *   JMenuItem: Next Place
 *   JMenuItem: New Place...
 *   JMenu: labelN
 *     JMenuItem: placeN
 *     ...
 *   ...
 * </pre>
 * 
 * Where each <code>placeN</code> has a context menu like this:
 * 
 * <pre>
 * Edit
 * Delete
 * </pre>
 * 
 * The top of the menu is constant. Edits that change the structure of the menu
 * cause the top level menu's children to be removed and the menu contents to be
 * rebuilt, but the top level menu remains fixed so it doesn't impact any higher
 * up portion of the user interface.
 */
public class PlacesMenu {
	private static final DebugLog log = DebugLog.instance();
	private static final Field nameField = new Field("Name", String.class);
	private static final Field labelsField = new Field("Labels", String.class);
	private static final Field ppdField = new Field("PPD", Integer.class);
	private static final Field projLonField = new Field("ProjCenterLon", Double.class);
	private static final Field projLatField = new Field("ProjCenterLat", Double.class);
	private static final String recentFile = Main.getJMarsPath() + "recent.jpf";
	
	private final PlaceStore store;
	private final JMenu top = new JMenu("Places");
	private final PlaceListener placeHistory = new PlaceListener();
	private final JMenuItem backPlace = new JMenuItem("Previous Place");
	private final JMenuItem forwardPlace = new JMenuItem("Next Place");
	private final JMenu recentMenu = new JMenu("Recent");
	
	/**
	 * edit dialog, created when first needed so <code>top</code> can be added
	 * to a component hierarchy first
	 */
	private PlaceEditDialog dialog;
	
	private final PlaceSettingAdapter rescale = new PlaceSettingAdapter(
		"places.rescale", false, "Restore Zoom on Selection");
	private final PlaceSettingAdapter reproject = new PlaceSettingAdapter(
			"places.reproject", true, "Restore Projection on Selection");
	private final PlaceSettingAdapter[] settings = {
		rescale, reproject
	};
	
	public void resetHistory() {
		buildMenus();
	}
	/** replaces the contents of this menu from the 'recent' xml file in ~/jmars */
	public void load() {
		// do nothing if the file does not exist
		if (new File(recentFile).lastModified() == 0) {
			buildMenus();
			return;
		}
		Map<Object,Object> data = null;
		BufferedReader buf = null;
		try {
			XStream xs = new XStream();
			buf = new BufferedReader(new FileReader(recentFile));
			data = new HashMap<Object,Object>((Map<?,?>) xs.fromXML(buf));
			buf.close();
		} catch (Exception e) {
			log.aprintln(e);
		} finally {
			if (buf != null) {
				try {
					buf.close();
				} catch (IOException e) {
					log.aprintln(e);
				}
			}
		}
		
		if (data != null) {
			placeHistory.getPlaces().clear();
			Iterator<Object> iter = data.keySet().iterator();
			while(iter.hasNext()) {
				Object key = iter.next();
				Object value = data.get(key);
				//check here for older version of recent.jpf
				if (key instanceof String) {
					String keyStr = (String) key;
					if (keyStr.equalsIgnoreCase("places") || keyStr.equalsIgnoreCase("version")) {
						//here we have an older version
						placeHistory.addPlaceForBody(Main.getBody(), new Place());
						break;
					}
				} 	
				if (value instanceof Number) {
					//version number entry
					Number version = (Number) value;
					placeHistory.versionMap.put((String)key,new Integer(version.intValue()));
				} else if (value instanceof List) {
					//list of places
					for (Object o : (List<?>)value) {
						if (o instanceof Place) {
							placeHistory.addPlaceForBody((String) key, (Place) o);
						}
					}
				}
				
			}
			buildMenus();
		}
	}
	
	/** saves the contents of this menu to the 'recent' xml file in ~/jmars */
	public void save() {
		Map<Object,Object> data = new HashMap<Object,Object>();
		synchronized (placeHistory.bodyPlaceList) {
			Iterator<String> iter = placeHistory.bodyPlaceList.keySet().iterator();
			while (iter.hasNext()) {
				String key = iter.next();
				List<Place> placesCopy = (ArrayList<Place>)((ArrayList) placeHistory.bodyPlaceList.get(key)).clone();
				Integer version = placeHistory.versionMap.get(key); 
				data.put(key, placesCopy);
				data.put(key + "-version", version);
			}
		}
		XStream xs = new XStream();
		BufferedWriter buf = null;
		try {
			buf = new BufferedWriter(new FileWriter(recentFile));
			xs.toXML(data, buf);
		} catch (Exception e) {
			log.aprintln(e);
		} finally {
			if (buf != null) {
				try {
					buf.flush();
					buf.close();
				} catch (Exception e) {
					log.aprintln(e);
				}
			}
		}
	}
	
	/** Sets the current zoom, projection, and position to the currently selected place. */
	public void restoreLocation() {
		placeHistory.setVersion(placeHistory.getVersionByBody(), true, true, true);
	}
	
	/** Creates a new top level menu for this place store. */
	public PlacesMenu(PlaceStore store) {
		this.store = store;
		backPlace.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, ActionEvent.ALT_MASK));
		backPlace.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				placeHistory.back();
			}
		});
		
		forwardPlace.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, ActionEvent.ALT_MASK));
		forwardPlace.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				placeHistory.forward();
			}
		});
		
		// build menus before anything has been added
		buildMenus();

		// load any saved data, which will rebuild the menus if necessary
		load();
		
		Main.addProjectionListener(placeHistory);
		Main.testDriver.mainWindow.getLocationManager().addLocationListener(placeHistory);
		Main.testDriver.mainWindow.getZoomManager().addListener(placeHistory);
	}
	
	/** Returns the top level menu. Will never change. */
	public JMenuItem getMenu() {
		return top;
	}
	
	private void updateGUI() {
		//set visibility of the menu items
		backPlace.setEnabled(placeHistory.getVersionByBody() > 0);
		forwardPlace.setEnabled(placeHistory.getVersionByBody()+1 < placeHistory.getPlaces().size());
		ButtonGroup group = new ButtonGroup();
		recentMenu.removeAll();
		//refresh the buttons in the location manager as well
		Main.testDriver.locMgr.backPlaceBtn.setEnabled(placeHistory.getVersionByBody() > 0);
		Main.testDriver.locMgr.forwardPlaceBtn.setEnabled(placeHistory.getVersionByBody()+1 < placeHistory.getPlaces().size());
		

		
		// reverse so recent versions are at the top of the menu
		List<Place> places = new ArrayList<Place>(placeHistory.getPlaces());
		if (places.size() == 0) {
			placeHistory.addPlaceForBody(Main.getBody(), new Place());//set a default place
			placeHistory.versionMap.put(Main.getBody(),new Integer(0));
			places = new ArrayList<Place>(placeHistory.getPlaces());
		}
		int version = placeHistory.getVersionByBody();
		Collections.reverse(places);
		
		int ver = places.size();
		for (Place p: places) {
			ver --;
			Point2D ll = p.getLonLat();
			Point2D proj = p.getProjCenterLonLat();
			JRadioButtonMenuItem item = new JRadioButtonMenuItem(MessageFormat.format(
				"Location {0,number,#.###}E  {1,number,#.###}N, PPD {2,number}, " +
				"Projection {3,number,#.###}E {4,number,#.###}N",
				ll.getX(), ll.getY(), p.getPpd(), proj.getX(), proj.getY()));
			final int v = ver;
			item.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					placeHistory.setVersion(v);
				}
			});
			item.setSelected(version == ver);
			group.add(item);
			recentMenu.add(item);
		}
	}
	
	/**
	 * Replaces the menu structure inside {@link #top} with the current state of
	 * the place store
	 */
	private void buildMenus() {
		top.removeAll();
		
		top.add(backPlace);
		top.add(forwardPlace);
		updateGUI();
		top.add(recentMenu);
		top.addSeparator();
		
		for (final PlaceSettingAdapter adapter: settings) {
			final JCheckBoxMenuItem cb = new JCheckBoxMenuItem(adapter.label);
			cb.setSelected(adapter.getValue());
			cb.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					adapter.setValue(cb.isSelected());
				}
			});
			top.add(cb);
		}
		
		JMenuItem exportPlaces = new JMenuItem("Export to CSV...");
		exportPlaces.addActionListener(new ActionListener() {
			private final JFileChooser chooser = new JFileChooser();
			public void actionPerformed(ActionEvent e) {
				chooser.setAcceptAllFileFilterUsed(false);
				chooser.addChoosableFileFilter(new FileFilter() {
					public boolean accept(File f) {
						return f.isDirectory() || f.getName().toLowerCase().endsWith(".csv");
					}
					public String getDescription() {
						return "CSV shapefile (*.csv)";
					}
				});
				if (JFileChooser.APPROVE_OPTION == chooser.showDialog(Main.mainFrame, "Export")) {
					File file = chooser.getSelectedFile();
					if (!file.getPath().toLowerCase().endsWith(".csv")) {
						file = new File(file.getAbsolutePath() + ".csv");
					}
					if (!file.exists() || JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
							Main.mainFrame, "File '" + file.getName() + "' already exists.\n\nOverwrite existing file?", "File already exists",
							JOptionPane.YES_NO_OPTION)) {
						FeatureCollection fc = new SingleFeatureCollection();
						for (Place place: store) {
							Feature f = new Feature();
							f.setPath(new FPath(new Point2D[]{place.getLonLat()}, FPath.SPATIAL_EAST, false));
							f.setAttribute(nameField, place.getName());
							f.setAttribute(labelsField, Util.join(",", place.getLabels()));
							f.setAttribute(ppdField, place.getPpd());
							f.setAttribute(projLonField, place.getProjCenterLonLat().getX());
							f.setAttribute(projLatField, place.getProjCenterLonLat().getY());
							fc.addFeature(f);
						}
						new FeatureProviderCSV().save(fc, file.getAbsolutePath());
					}
				}
			}
		});
		top.add(exportPlaces);
		
		top.addSeparator();
		
		JMenuItem newPlace = new JMenuItem("Bookmark Current Place...");
		newPlace.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, ActionEvent.CTRL_MASK));
		newPlace.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				edit(new Place());
			}
		});
		top.add(newPlace);
		
		// group places by label
		Map<String,List<Place>> labelGroups = new TreeMap<String,List<Place>>();
		for (Place p: store) {
			for (String label: p.getLabels()) {
				List<Place> labelPlaces = labelGroups.get(label);
				if (labelPlaces == null) {
					labelGroups.put(label, labelPlaces = new ArrayList<Place>());
				}
				labelPlaces.add(p);
			}
		}
		
		// add each label, leaving the contents in original order so newer
		// places are at the bottom
		for (String label: labelGroups.keySet()) {
			JMenu labelMenu = new JMenu(label);
			for (final Place p: labelGroups.get(label)) {
				labelMenu.add(createPlaceMenu(p));
			}
			top.add(labelMenu);
		}
		
		// add items with no labels at the end, also in normal order so
		// newer items are the bottom
		for (Place p: store) {
			if (p.getLabels().size() == 0) {
				top.add(createPlaceMenu(p));
			}
		}
		
		top.invalidate();
		top.validate();
		top.repaint();
	}
	
	/** Creates a menu for this place, complete with tooltip and context menu */
	private JMenuItem createPlaceMenu(final Place p) {
		final JMenuItem pMenu = new JMenuItem(p.getName());
		pMenu.setToolTipText(MessageFormat.format(
			"<html><body>Location: {0}" +
			"<br>Projection: {1}" +
			"<br>PPD: {2}<br>Map Offsets: {3}" +
			"<br><br>Click to go to this place<br>Open context menu for other options</body></html>",
			toString(p.getLonLat()),
			toString(p.getProjCenterLonLat()),
			p.getPpd(),
			p.getMapOffsets().size()));
		
		final JPopupMenu popup = new JPopupMenu("Options for " + p.getName());
		JMenuItem editMenu = new JMenuItem("Edit");
		editMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				edit(p);
			}
		});
		popup.add(editMenu);
		JMenuItem delMenu = new JMenuItem("Delete");
		delMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				delete(p);
			}
		});
		popup.add(delMenu);
		
		pMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if ((e.getModifiers() & ActionEvent.META_MASK) == 0) {
					p.gotoPlace(reproject.getValue(), rescale.getValue(), reproject.getValue());
				}
			}
		});
		
		pMenu.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				handle(e);
			}
			public void mouseReleased(MouseEvent e) {
				handle(e);
			}
			public void handle(MouseEvent e) {
				if (e.isPopupTrigger()) {
					e = SwingUtilities.convertMouseEvent(Main.testDriver, e, null);
					popup.show(pMenu, e.getX(), 0);
				}
			}
		});
		
		return pMenu;
	}
	
	private static String toString(Point2D p) {
		if (p == null) {
			return "<none>";
		} else {
			return MessageFormat.format("{0,number,#.###}E, {1,number,#.###}N", p.getX(), p.getY());
		}
	}
	
	/**
	 * Edits the given place. Changes are only saved if the Save button is
	 * pressed in the dialog. Rebuilds the menu after the dialog closes.
	 */
	private void edit(Place place) {
		if (dialog == null) {
			dialog = new PlaceEditDialog(Main.mainFrame, store);
		}
		dialog.setLocationRelativeTo(Main.mainFrame);
		dialog.setPlace(place);
		dialog.pack();
		dialog.setVisible(true);
		buildMenus();
	}
	
	/** Deletes the given place and rebuilds the menu */
	private void delete(Place place) {
		if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
				Main.testDriver,
				"Are you sure you want to remove the place named " + place.getName(),
				"Confirm removal",
				JOptionPane.YES_NO_OPTION)) {
			try {
				store.remove(place);
			} catch (Exception ex) {
				ex.printStackTrace();
				JOptionPane.showMessageDialog(
					top,
					"Error occurred removing " + place + ":\n\n" +
						Util.foldText(ex.getMessage(), 60, "\n"),
					"Error removing place",
					JOptionPane.ERROR_MESSAGE);
			} finally {
				buildMenus();
			}
		}
	}
	
	
	//returns the placehistory object -- to be used on toolbar buttons
	public PlaceListener getPlaceHistory(){
		return placeHistory;
	}
	
	/**
	 * Listens to changes in location, zoom, and projection, and handles
	 * grouping them into changesets that then respond to undo/redo events. This
	 * is an inner class only to avoid the numerous interfaces being connected to
	 * PlacesMenu, which should not really show any of this externally.
	 */
	public class PlaceListener implements LocationListener, ZoomListener, ProjectionListener {
		private long lastMark = System.currentTimeMillis();
		private final HashMap<String,List<Place>> bodyPlaceList = new HashMap<String,List<Place>>();
		private HashMap<String,Integer> versionMap = new HashMap<String,Integer>();
		private boolean busy = false;
		
		public PlaceListener() {
			// configure this listener with the starting place
			log();
		}
		
		private List<Place> getPlaces() {
			if (!bodyPlaceList.containsKey(Main.getBody())) {
				bodyPlaceList.put(Main.getBody(), new ArrayList<Place>());
			}
			return bodyPlaceList.get(Main.getBody());
		}
		private void addPlaceForBody(String body, Place place) {
			if (!bodyPlaceList.containsKey(body)) {
				bodyPlaceList.put(body, new ArrayList<Place>());
			} else {
				ArrayList<Place> list = (ArrayList<Place>) bodyPlaceList.get(body);
				list.add(place);
			}
		}
		private int getVersionByBody() {
			if (!versionMap.containsKey(Main.getBody())) {
				versionMap.put(Main.getBody(), new Integer(0));
			}
			return versionMap.get(Main.getBody()).intValue();
		}
		private void setVersionByBody(int ver) {
			versionMap.put(Main.getBody(), new Integer(ver));
		}
		
		public void locationChanged(Point2D worldCenter) {
			log();
		}
		public void zoomChanged(int newPPD) {
			log();
		}
		public void projectionChanged(ProjectionEvent e) {
			log();
		}
		
		private synchronized void log() {
			if (busy) {
				return;
			}
			
			busy = true;
			try {
				// Avoid marking more than twice per second, as some chains of calls
				// will set the projection and then the zoom etc, but users will
				// feel they are logically one motion.
				Place place = new Place();
				if (System.currentTimeMillis() - lastMark > 500 || getPlaces().isEmpty()) {
					// trim versions after the current version
					if (getVersionByBody() + 1 < getPlaces().size()) {
						getPlaces().subList(getVersionByBody()+1, getPlaces().size()).clear();
					}
					// trim oldest versions if the list has grown too large
					if (getPlaces().size() == 20) {
						setVersionByBody(getVersionByBody() - 1);
						getPlaces().remove(0);
					}
					// add the place and move up to the latest version
					getPlaces().add(place);
					setVersion(getVersionByBody() + 1);
					lastMark = System.currentTimeMillis();
				} else {
					getPlaces().set(getPlaces().size()-1, place);
					setVersion(getVersionByBody());
				}
			} finally {
				busy = false;
			}
		}
		
		public synchronized void back() {
			setVersion(getVersionByBody() - 1);
		}
		
		public synchronized void forward() {
			setVersion(getVersionByBody() + 1);
		}
		
		public synchronized void setVersion(int ver) {
			setVersion(ver, true, true, false);
		}
		
		public synchronized void setVersion(int ver, boolean proj, boolean ppd, boolean maps) {
			if (ver >= 0 && ver < getPlaces().size()) {
				// any change after the version is updated, no matter how
				// quickly it arrives, should be applied
				lastMark = 0;
				setVersionByBody(ver);
				if (!busy) {
					busy = true;
					try {
						getPlaces().get(getVersionByBody()).gotoPlace(proj, ppd, maps);
					} finally {
						busy = false;
					}
				}
				updateGUI();
			}
		}
	}
	
	/** Ties together a jmars.config key, default value, and a user interface label */
	private static class PlaceSettingAdapter {
		public final String key;
		public final String label;
		private boolean defaultValue;
		public PlaceSettingAdapter(String key, boolean defaultValue, String label) {
			this.key = key;
			this.defaultValue = defaultValue;
			this.label = label;
		}
		public boolean getValue() {
			return Config.get(key, defaultValue);
		}
		public void setValue(boolean value) {
			if (getValue() != value) {
				Config.set(key, value);
			}
		}
	}
	
}
