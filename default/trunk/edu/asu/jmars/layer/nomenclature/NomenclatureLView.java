package edu.asu.jmars.layer.nomenclature;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Dimension2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import edu.asu.jmars.Main;
import edu.asu.jmars.graphics.SpatialGraphics2D;
import edu.asu.jmars.graphics.SpatialGraphicsCyl;
import edu.asu.jmars.layer.FocusPanel;
import edu.asu.jmars.layer.InvestigateData;
import edu.asu.jmars.layer.LViewSettings;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.HighResExport;
import edu.asu.jmars.util.Util;

/* Class for Nomenclature layerview */
public class NomenclatureLView extends Layer.LView {
	public static final String NOMENCLATURE_DIR = "nomenclature/";// @since change bodies
	private static final DecimalFormat f = new DecimalFormat("0.###");
	private static DebugLog log = DebugLog.instance();

	private List<MarsFeature> landmarks = new ArrayList<MarsFeature>();
	private Set<String> landmarkTypes = new HashSet<String>();
	private Map<MarsFeature,Rectangle2D> labelLocations = new HashMap<MarsFeature,Rectangle2D>();
	private Font labelFont = null;

	public NomenclatureLView(Layer parent) {
		super(parent);

		/**
		 * Read in the file of landmarks on mars - a file was chosen instead of
		 * a db table since the data is fairly static and it will make it a hell
		 * of a lot easier for the student version - which is supposed to
		 * operate without network connectivity.
		 * 
		 * The format of the file is comma separated and should contain the
		 * following fields:
		 * 
		 * lanmark type, name, latitude(N), longitude (W), diameter (km),
		 * origin/description
		 * 
		 * File is in east planetocentric. Stored internally as west.
		 */

		try {
			// @since change bodies
			File nomenclatureFile = NomenclatureLView.getFeaturesFile();
			FileInputStream fis = new FileInputStream(nomenclatureFile);
			InputStreamReader isr = new InputStreamReader(fis, "utf-8");
			BufferedReader in = new BufferedReader(isr);
			// end change bodies
			String lineIn = in.readLine();
			while (lineIn != null && lineIn.compareToIgnoreCase("STOP") != 0) {
				MarsFeature mf = new MarsFeature();

				try {
					StringTokenizer tok = new StringTokenizer(lineIn, "\t");

					mf.landmarkType = tok.nextToken().trim();
					mf.name = tok.nextToken().trim();
					mf.latitude = Double.parseDouble(tok.nextToken());
					// USER east lon => JMARS west lon
					mf.longitude = (360 - Double.parseDouble(tok.nextToken())) % 360;
					mf.diameter = Double.parseDouble(tok.nextToken());
					mf.origin = tok.nextToken().trim();

					landmarks.add(mf);
					landmarkTypes.add(mf.landmarkType);

				} catch (NoSuchElementException ne) {
					// ignore
				}

				lineIn = in.readLine();
			}

			in.close();
		} catch (Exception e) {
			log.aprintln(e);
		}

		// define default items to show
		if (settings.showLandmarkTypes.size() == 0) {
			// @since change bodies
			String defaultLandmarkType = Config.get(Util.getProductBodyPrefix()+Config.CONFIG_DEFAULT_LANDMARK_TYPE);
			if (defaultLandmarkType != null && !"".equals(defaultLandmarkType)) {
				settings.showLandmarkTypes.add(defaultLandmarkType);
			} else {
				if (landmarks != null && landmarks.size() > 0) {
					settings.showLandmarkTypes.add(landmarks.get(0).landmarkType);
				}
			}
			// end change bodies
		}

	}
	/**
	* @since change bodies
	*/
	public static File getFeaturesFile() {
		//check if there is a new version of the nomenclature for this body
		//with this check, we can eliminate the features.txt files from the jar file if we choose - KJR 1/4/12
		String featureFileName = Main.getBody().toLowerCase() + "_features.txt";
		String remoteUrl = Config.get(Config.CONTENT_SERVER) + NOMENCLATURE_DIR + featureFileName;
		return Util.getCachedFile(remoteUrl, true);
	}
	/**
	 * This is good - build the focus panel on demand instead of during the
	 * view's constructor. That way, the saved session variables get propogated
	 * properly.
	 */
	public FocusPanel getFocusPanel() {
		if (focusPanel == null){
			focusPanel = new FocusPanel(this,true);
			JScrollPane nomAdjustments = new JScrollPane(new NomenclaturePanel(), JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			nomAdjustments.getVerticalScrollBar().setUnitIncrement(10);
			focusPanel.add("Adjustments", nomAdjustments);
		}
		return focusPanel;
	}

	/** Event triggered whenever there is new data to (potentially) paint in */
	public synchronized void receiveData(Object layerData) {
		// do nothing
	}

	protected Object createRequest(Rectangle2D where) {
		return (null);
	}

	protected Layer.LView _new() {
		return new NomenclatureLView(getLayer());
	}

	public String getName() {
		return "Nomenclature";
	}

	protected void viewChangedPost() {
		redraw();
		repaint();
	}

	private void redrawAll() {
		redraw();
		repaint();

		if (getChild() != null) {
			((NomenclatureLView) getChild()).redraw();
			getChild().repaint();
		}
	}
	
	private synchronized void redraw() {
		labelFont = getFont().deriveFont(Font.BOLD);
		
		float zoomFactor = 1.0f;
		
		if (HighResExport.exporting) {
			zoomFactor = HighResExport.zoomFactor;
		}
		
		labelFont = labelFont.deriveFont((float)(labelFont.getSize()*zoomFactor));
		
		labelLocations.clear();
		clearOffScreen();
		
		Graphics2D g1 = getOffScreenG2Direct();
		if (g1 == null) {
			return;
		}
		
		g1.setFont(labelFont);
		g1.setColor(settings.labelColor);
		
		Graphics2D g2 = getOffScreenG2();
		SpatialGraphics2D spatialG2 = getProj().createSpatialGraphics(g2);
		if (spatialG2 == null) {
			log.println("Skipping redraw(), since there is no graphics to draw to");
			return;
		}
		
		for (int i = 0; i < landmarks.size(); i++) {
			MarsFeature mf = landmarks.get(i);
			if (settings.showLandmarkTypes.contains(mf.landmarkType)) {
				Point2D p = new Point2D.Double(mf.longitude, mf.latitude);
				Point2D[] worldPoints = spatialG2.spatialToWorlds(p);
				mf.draw(g1, g2, worldPoints);
			}
		}
	}

	protected Component[] getContextMenuTop(Point2D worldPt) {
		Component[] menuItems = super.getContextMenuTop(worldPt);

		String msg = getStringForPoint(worldPt, true);

		if (msg == null)
			return menuItems;

		Component[] descriptionItems = new Component[menuItems.length + 1];

		// transfer menus into the new parent
		descriptionItems[0] = new JMenuItem(msg);
		for (int i = 0; i < menuItems.length; i++)
			descriptionItems[i + 1] = menuItems[i];

		return descriptionItems;
	}

	protected Component[] getContextMenu(Point2D worldPt) {
		Component[] menuItems = super.getContextMenu(worldPt);

		final String msg = getStringForPoint(worldPt, false);

		if (msg == null)
			return menuItems;

		// transfer menus into the new parent
		Component[] newMenuItems = new Component[menuItems.length + 1];
		for (int i = 0; i < menuItems.length; i++) {
			newMenuItems[i] = menuItems[i];
		}

		newMenuItems[menuItems.length] = new JMenuItem(new AbstractAction(
				"Copy Nomenclature Description to Clipboard") {
			public void actionPerformed(ActionEvent e) {
				StringSelection sel = new StringSelection(msg);
				getToolkit().getSystemClipboard().setContents(sel, sel);
				Main.setStatus("Nomenclature description copied to clipboard");
			}
		});

		return newMenuItems;
	}

	public String getToolTipText(MouseEvent event) {
		if (!settings.enableLabelTips)
			return "";

		try {
			Point2D mouseWorld = getProj().screen.toWorld(event.getPoint());
			return getStringForPoint(mouseWorld, true);
		} catch (Exception ex) {
			// ignore
		}

		return "";
	}

// Used to display information for InvestigateTool
	public InvestigateData getInvestigateData(MouseEvent event){
		InvestigateData invData = new InvestigateData(getName());
		
		Point2D worldPoint = getProj().screen.toWorld(event.getPoint());
		Set<MarsFeature> copyKeys = new HashSet<MarsFeature>(labelLocations.keySet());
		
		for (MarsFeature mf: copyKeys) {
			Rectangle2D r = labelLocations.get(mf);

			if (settings.showLandmarkTypes.contains(mf.landmarkType)) {
				if (r != null
						&& r.contains(worldPoint)
						|| r.contains(new Point2D.Double(worldPoint.getX() - 360.0,
								worldPoint.getY()))) {
					String key = mf.name+" ("+f.format((360-mf.longitude)%360)+"E, "+f.format(mf.latitude)+"N)";
					String val = "Diameter="+mf.diameter+"km. Origin="+mf.origin;
					
					invData.add(key, val);
				}
			}
		}
		return invData;
	}
	
	protected String getStringForPoint(Point2D worldPoint, boolean showAsHTML) {
		for (MarsFeature mf: labelLocations.keySet()) {
			Rectangle2D r = labelLocations.get(mf);

			if (settings.showLandmarkTypes.contains(mf.landmarkType)) {
				if (r != null
						&& r.contains(worldPoint)
						|| r.contains(new Point2D.Double(worldPoint.getX() - 360.0,
								worldPoint.getY()))) {
					return mf.getPopupInfo(showAsHTML);
				}
			}
		}

		return null;
	}

	private static class NomenclatureSettings extends LViewSettings {
		private static final long serialVersionUID = 3853034040394687088L;
		boolean showMainPoints = true;
		boolean showPannerPoints = true;
		Color pointColor = Color.red;
		boolean showMainLabels = true;
		boolean showPannerLabels = false;
		Color labelColor = Color.white;
		boolean enableLabelTips = true;
		ArrayList<String> showLandmarkTypes = new ArrayList<String>();
	}

	private static NomenclatureSettings settings = new NomenclatureSettings();

	/**
	 * Override to update view specific settings
	 */
	protected void updateSettings(boolean saving) {
		if (saving) {
			viewSettings.put("nomenclature", settings);
		} else {
			if (viewSettings.containsKey("nomenclature")) {
				settings = (NomenclatureSettings) viewSettings.get("nomenclature");
			}
		}
	}

	/**
	 * Clear the settings so if the factory uses this object again, the settings do not remain. 
	 * @since change bodies
	 */
	public void clearSettings() {
		settings = new NomenclatureSettings();
	}
	
	private class NomenclaturePanel extends JPanel implements ActionListener, ListSelectionListener {
		JCheckBox chkShowMainPoints;
		JCheckBox chkShowPannerPoints;
		JButton btnPointColor;
		JCheckBox chkShowMainLabels;
		JCheckBox chkShowPannerLabels;
		JButton btnLabelColor;
		JCheckBox chkEnableLabelTips;

		JList listLandmarks;

		JComboBox comboLandmarks;
		JComboBox comboItems;
		JButton btnGoto;
		JButton btnAll;
		JButton btnClear;

		TreeSet<String> sortedList = new TreeSet<String>();

		NomenclaturePanel() {
			// Set up the layout and borders
			setLayout(new BorderLayout(10, 10));
			Box box = Box.createVerticalBox();
			add(box, BorderLayout.CENTER);

			JPanel p0 = new JPanel(new GridLayout(3, 0));
			p0.setBorder(BorderFactory.createCompoundBorder(BorderFactory
					.createTitledBorder("Points:"), BorderFactory
					.createEmptyBorder(5, 5, 5, 5)));
			box.add(p0);

			chkShowMainPoints = new JCheckBox("Show Points in Main",
					settings.showMainPoints);
			chkShowMainPoints.addActionListener(this);
			p0.add(chkShowMainPoints);

			chkShowPannerPoints = new JCheckBox("Show Points in Panner",
					settings.showPannerPoints);
			chkShowPannerPoints.addActionListener(this);
			p0.add(chkShowPannerPoints);

			btnPointColor = new JButton(" ");
			btnPointColor.setBackground(settings.pointColor);
			btnPointColor.addActionListener(this);
			p0.add(btnPointColor);

			JPanel p1 = new JPanel(new GridLayout(3, 0));
			p1.setBorder(BorderFactory.createCompoundBorder(BorderFactory
					.createTitledBorder("Labels:"), BorderFactory
					.createEmptyBorder(5, 5, 5, 5)));
			box.add(p1);

			chkShowMainLabels = new JCheckBox("Show Labels in Main",
					settings.showMainLabels);
			chkShowMainLabels.addActionListener(this);
			p1.add(chkShowMainLabels);

			chkShowPannerLabels = new JCheckBox("Show Labels in Panner",
					settings.showPannerLabels);
			chkShowPannerLabels.addActionListener(this);
			p1.add(chkShowPannerLabels);

			btnLabelColor = new JButton(" ");
			btnLabelColor.setBackground(settings.labelColor);
			btnLabelColor.addActionListener(this);
			p1.add(btnLabelColor);

			JPanel p2 = new JPanel(new GridLayout(1, 0));
			p2.setBorder(BorderFactory.createCompoundBorder(BorderFactory
					.createTitledBorder("Tooltips:"), BorderFactory
					.createEmptyBorder(5, 5, 5, 5)));
			box.add(p2);

			chkEnableLabelTips = new JCheckBox("Enable Label Tips",
					settings.enableLabelTips);
			chkEnableLabelTips.addActionListener(this);
			p2.add(chkEnableLabelTips);

			JPanel p3 = new JPanel(new BorderLayout());
			p3.setBorder(BorderFactory.createCompoundBorder(BorderFactory
					.createTitledBorder("Selected Landmarks Types:"), BorderFactory
					.createEmptyBorder(5, 5, 5, 5)));

			box.add(p3);

			Box b2 = Box.createVerticalBox();
			p3.add(b2);

			for (String key : landmarkTypes) {
				sortedList.add(key);
			}

			listLandmarks = new JList(sortedList.toArray());
			listLandmarks.setVisibleRowCount(8);
			listLandmarks.setPrototypeCellValue("this is a prototypical value");
			listLandmarks
					.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			listLandmarks.addListSelectionListener(this);
			listLandmarks
					.setToolTipText("This list indicates which landmarks appear. Use the shift and control keys with the mouse to make multiple selections.");
			JScrollPane listScrollPane = new JScrollPane(listLandmarks);
			b2.add(listScrollPane);

			JPanel panelX = new JPanel(new FlowLayout());
			b2.add(panelX);

			btnAll = new JButton("Select All");
			btnAll.addActionListener(this);
			btnAll.setToolTipText("Select all landmark types");
			panelX.add(btnAll);

			btnClear = new JButton("Clear");
			btnClear.addActionListener(this);
			btnClear.setToolTipText("Clear all landmark types");
			panelX.add(btnClear);

			JPanel p4 = new JPanel(new GridLayout(3, 2));
			p4.setBorder(BorderFactory.createCompoundBorder(BorderFactory
					.createTitledBorder("Navigation:"), BorderFactory
					.createEmptyBorder(5, 5, 5, 5)));
			box.add(p4);

			p4.add(new JLabel("Landmark Type: ", JLabel.RIGHT));

			comboLandmarks = new JComboBox();
			comboLandmarks.setPreferredSize(new Dimension(150, 20));
			comboLandmarks.addActionListener(this);
			p4.add(comboLandmarks);

			p4.add(new JLabel("Landmark: ", JLabel.RIGHT));
			comboItems = new JComboBox();
			comboItems.setPreferredSize(new Dimension(150, 20));
			comboItems.addActionListener(this);
			p4.add(comboItems);

			p4.add(new JLabel(" "));
			btnGoto = new JButton("Goto");
			btnGoto.addActionListener(this);
			btnGoto.setEnabled(false);
			btnGoto
					.setToolTipText("Select a landmark type and then specific landmark, then click here to goto that landmark.");
			p4.add(btnGoto);

			setSelectedItems();
			updateCombo();
		}

		protected void setSelectedItems() {
			// now mark all of the types which are not hidden
			int[] indexes = new int[settings.showLandmarkTypes.size()];

			int i = 0;
			int j = 0;
			for (String type: sortedList) {
				if (settings.showLandmarkTypes.contains(type)) {
					indexes[j++] = i;
				}
				i++;
			}

			listLandmarks.setSelectedIndices(indexes);
		}

		protected void updateCombo() {
			comboLandmarks.removeAllItems();

			if (settings.showLandmarkTypes.size() == 0) {
				comboLandmarks.setEnabled(false);
				comboItems.setEnabled(false);
				btnGoto.setEnabled(false);
				return;
			}

			for (String type: settings.showLandmarkTypes) {
				comboLandmarks.addItem(type);
			}

			comboLandmarks.setEnabled(true);
			comboItems.setEnabled(true);
			btnGoto.setEnabled(true);
		}

		public void valueChanged(ListSelectionEvent e) {
			if (e.getValueIsAdjusting())
				return;

			this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

			Object[] selVals = listLandmarks.getSelectedValues();
			List<Object> tmpList = new ArrayList<Object>();
			for (int i = 0; i < selVals.length; i++) {
				tmpList.add(selVals[i]);
			}

			settings.showLandmarkTypes.clear();
			for (String type: sortedList) {
				if (tmpList.contains(type)) {
					settings.showLandmarkTypes.add(type);
				}
			}

			updateCombo();
			redrawAll();
			this.setCursor(Cursor.getDefaultCursor());
		}

		// Handles all events from user
		public void actionPerformed(ActionEvent e) {
			Object source = e.getSource();

			// Handle color chooser changes
			if (source == btnPointColor) {
				Color newColor = JColorChooser.showDialog(Main.mainFrame,
						"Choose a point color", settings.pointColor);

				if (newColor != null) {
					// Reflect the new color
					btnPointColor.setBackground(newColor);

					// Color propagates to the main window and the panner
					settings.pointColor = newColor;
					redrawAll();

				}
			} else if (source == btnLabelColor) {
				Color newColor = JColorChooser.showDialog(Main.mainFrame,
						"Choose a label color", settings.labelColor);

				if (newColor != null) {
					// Reflect the new color
					btnLabelColor.setBackground(newColor);

					// Color propagates to the main window and the panner
					settings.labelColor = newColor;
					redrawAll();

				}
			} else if (source == chkShowMainPoints) {
				settings.showMainPoints = chkShowMainPoints.isSelected();
				redrawAll();
			} else if (source == chkShowMainLabels) {
				settings.showMainLabels = chkShowMainLabels.isSelected();
				redrawAll();
			} else if (source == chkShowPannerPoints) {
				settings.showPannerPoints = chkShowPannerPoints.isSelected();
				redrawAll();
			} else if (source == chkShowPannerLabels) {
				settings.showPannerLabels = chkShowPannerLabels.isSelected();
				redrawAll();
			} else if (source == chkEnableLabelTips) {
				settings.enableLabelTips = chkEnableLabelTips.isSelected();
			} else if (source == comboLandmarks) {
				comboItems.removeAllItems();
				btnGoto.setEnabled(false);

				if (comboLandmarks.getSelectedItem() == null)
					return;

				for (int i = 0; i < landmarks.size(); i++) {
					MarsFeature mf = (MarsFeature) landmarks.get(i);

					if (!settings.showLandmarkTypes.contains(mf.landmarkType))
						continue;

					if (mf.landmarkType.compareTo(comboLandmarks
							.getSelectedItem().toString()) == 0)
						comboItems.addItem(mf.name);

				}

				if (comboItems.getItemCount() > 0)
					btnGoto.setEnabled(true);
			} else if (source == comboItems) {
				comboItems.setToolTipText("");

				if (comboLandmarks.getSelectedItem() == null)
					return;

				if (comboItems.getSelectedItem() == null)
					return;

				for (int i = 0; i < landmarks.size(); i++) {
					MarsFeature mf = (MarsFeature) landmarks.get(i);

					if (!settings.showLandmarkTypes.contains(mf.landmarkType))
						continue;

					if (mf.landmarkType.compareTo(comboLandmarks
							.getSelectedItem().toString()) == 0
							&& mf.name.compareTo(comboItems.getSelectedItem()
									.toString()) == 0) {

						// JMARS west lon => USER east lon
						comboItems.setToolTipText(" (" + (360 - mf.longitude)
								% 360 + "E, " + mf.latitude + "N)");
						break;
					}
				}
			} else if (source == btnGoto) {
				this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

				for (int i = 0; i < landmarks.size(); i++) {
					MarsFeature mf = (MarsFeature) landmarks.get(i);

					if (!settings.showLandmarkTypes.contains(mf.landmarkType))
						continue;

					if (mf.landmarkType.compareTo(comboLandmarks
							.getSelectedItem().toString()) == 0
							&& mf.name.compareTo(comboItems.getSelectedItem()
									.toString()) == 0) {

						Graphics2D g2 = getOffScreenG2();
						SpatialGraphics2D spatialG2;
						spatialG2 = new SpatialGraphicsCyl(g2, getProj());

						Point2D[] worldPoints = spatialG2
								.spatialToWorlds(new Point2D.Double(
										mf.longitude, mf.latitude));
						if (worldPoints != null && worldPoints.length > 0)
							centerAtPoint(worldPoints[0]);

						break;
					}
				}

				this.setCursor(Cursor.getDefaultCursor());
			} else if (source == btnAll) {
				this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				settings.showLandmarkTypes.clear();
				settings.showLandmarkTypes.addAll(sortedList);
				setSelectedItems();
				updateCombo();
				redrawAll();
				this.setCursor(Cursor.getDefaultCursor());
			} else if (source == btnClear) {
				this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

				settings.showLandmarkTypes.clear();

				setSelectedItems();
				updateCombo();
				redrawAll();

				this.setCursor(Cursor.getDefaultCursor());
			}
		}
	}

	// ///// Utility function used to create the mars_feature page using data
	// from the USGS Momenclature web site -a
	// / after processing to remove HTML and leave only the data

	public static void main(String[] av) throws Throwable {
		try {
			String name = "";
			String lon = "";
			String lat = "";
			String size = "";
			String desc = "";

			String tmpLat = "";

			BufferedReader in = new BufferedReader(new FileReader(av[0]));
			String lineIn = in.readLine();

			String type = "";
			int pos = av[0].indexOf('.');
			if (pos > -1)
				type = av[0].substring(0, pos);

			int cnt = 0;
			while (lineIn != null) {
				if (lineIn.length() > 1)
					tmpLat = lineIn.substring(18, 23).trim();

				if (cnt > 0 && tmpLat.length() != 0) {
					System.out.println(type + " , " + name + " , " + lat
							+ " , " + lon + " , " + size + " , " + desc);
					name = "";
					lon = "";
					lat = "";
					size = "";
					desc = "";
					tmpLat = "";

				}

				if (lineIn.length() > 1) {
					name = name + " " + lineIn.substring(0, 17).trim();
					lat = lat + lineIn.substring(18, 23).trim();
					if (lat.endsWith("S"))
						lat = "-" + lat.substring(0, lat.length() - 1);
					if (lat.endsWith("N"))
						lat = lat.substring(0, lat.length() - 1);

					lon = lon + lineIn.substring(25, 30).trim();
					size = size + lineIn.substring(34, 42).trim();

					desc = desc + " " + lineIn.substring(89).trim();

					cnt++;
				}
				lineIn = in.readLine();
			}

			System.out.println(type + " , " + name + " , " + lat + " , " + lon + " , " + size + " , " + desc);
			in.close();
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	class MarsFeature {
		public String landmarkType = "";
		public String name = "";
		public double latitude;
		public double longitude;
		public double diameter;
		public String origin = "";
		// do not assume this to be the same value always
		public Point2D thisWorldPoint = new Point2D.Double(0, 0);

		public String getPopupInfo(boolean showAsHTML) {
			String info = "";

			if (showAsHTML)
				info += "<html>";

			try {

				info += name;

				if (name.indexOf(landmarkType) == -1)
					info += " " + landmarkType;

				// JMARS west lon => USER east lon
				info += " (" + f.format((360 - longitude) % 360) + "E, "
						+ f.format(latitude) + "N, " + f.format(this.diameter)
						+ "km )";

				if (showAsHTML)
					info += "<br>";

				info += origin;
			} catch (Exception ex) {
				// ignore
			}

			if (showAsHTML)
				info += "</html>";

			return info;
		}

		public void draw(Graphics2D g1, Graphics2D g2, Point2D[] worldPoints) {
			// nothing to draw
			if (worldPoints == null || worldPoints.length == 0)
				return;

			Rectangle2D worldwin = getProj().getWorldWindow();

			if (HighResExport.exporting) {
				worldwin = HighResExport.fullExportExtent;
			}
			
			for (int i = 0; i < worldPoints.length; i++) {

				if (!worldwin.contains(worldPoints[i]))
					continue;

				drawPoint(g2, worldPoints[i]);
				drawLabel(g1, g2, worldPoints[i]);

			}
		}

		protected void drawPoint(Graphics2D g2, Point2D worldPoint) {
			if (!settings.showMainPoints && getChild() != null)
				return;

			if (!settings.showPannerPoints && getChild() == null)
				return;

			Dimension2D pSize = getProj().getPixelSize();
			int zoomFactor = 1;
			
			if (HighResExport.exporting) {
				zoomFactor = HighResExport.zoomFactor;
			}
			
			pSize.setSize(pSize.getWidth()*zoomFactor, pSize.getHeight()*zoomFactor);
			
			g2.setPaint(settings.pointColor);
			g2.setStroke(new BasicStroke(0));
			Rectangle2D.Double box = new Rectangle2D.Double(worldPoint.getX()
					- pSize.getWidth() * 2, worldPoint.getY()
					+ pSize.getHeight() * 2, pSize.getWidth() * 4, pSize
					.getHeight() * 4);
			g2.fill(box);

			// store the labels location
			labelLocations.put(this, box);
		}

		protected boolean inConflict(Rectangle2D r) {
			// check for conflicts with other locations
			for (int i = 0; i < landmarks.size(); i++) {

				MarsFeature mf = (MarsFeature) landmarks.get(i);

				// skip this one
				if (mf.equals(MarsFeature.this))
					continue;

				if (r.contains(mf.thisWorldPoint))
					return true;
			}

			// check for conflicts with other labels
			for (Rectangle2D existingRect: labelLocations.values()) {
				if (r.intersects(existingRect))
					return true;
			}

			return false;
		}

		protected void drawLabel(Graphics2D g1, Graphics2D g2, Point2D pt) {

			if (!settings.showMainLabels && getChild() != null)
				return;

			if (!settings.showPannerLabels && getChild() == null)
				return;

			FontMetrics fontMetrics = g2.getFontMetrics(labelFont);

			// calculate new label location and see if it intersects with one
			// already.

			// get the width and enlarge by 20%
			double xWidth = fontMetrics.stringWidth(name) * getProj().getPixelSize().getWidth() * 1.2;
			double xLoc = pt.getX() - xWidth / 2;

			double yHeight = fontMetrics.getHeight() * getProj().getPixelSize().getHeight();
			// we want to include the point in the bounds
			double yLoc = pt.getY() - yHeight * .7;

			Rectangle2D.Double labelLoc = new Rectangle2D.Double(xLoc, yLoc, xWidth, yHeight);
			if (inConflict(labelLoc)) {
				// try some different locations if there is a conflict with
				// other labels or points
				yLoc = pt.getY() + yHeight * .3;
				labelLoc = new Rectangle2D.Double(xLoc, yLoc, xWidth, yHeight);

				if (inConflict(labelLoc)) {
					xLoc = pt.getX() + getProj().getPixelSize().getWidth() * 5;
					yLoc = pt.getY();
					labelLoc = new Rectangle2D.Double(xLoc, yLoc, xWidth, yHeight);
				}
			}

			// store the labels location - ok to overwrite point locations since
			// they are inclusive
			labelLocations.put(this, labelLoc);

			pt = getProj().world.toScreen(labelLoc.getX(), labelLoc.getY());

			// g2.draw(labelLoc);
			g1.drawString(name, (float) pt.getX(), (float) pt.getY());
		}
	}
	
//The following two methods are used to query for the
// info panel fields (description, citation, etc)	
 	public String getLayerKey(){
 		return "Nomenclature";
 	}
 	public String getLayerType(){
 		return "nomenclature";
 	}
}
