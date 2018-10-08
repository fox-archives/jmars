package edu.asu.jmars.layer.grid;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;

import edu.asu.jmars.Main;
import edu.asu.jmars.ProjObj;
import edu.asu.jmars.layer.FocusPanel;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.layer.scale.ScaleLView;
import edu.asu.jmars.layer.scale.ScaleParameters;
import edu.asu.jmars.swing.PasteField;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.HighResExport;

public class GridLView extends Layer.LView
 {
    private static DebugLog log = DebugLog.instance();
    
    private final Color majorLinesColor = new Color(Config.get("gridcolor", 0));
    
    GridParameters parms = null; // Used to save user grid selections across a save/restore of session or layer.
    GridSettings   major = new GridSettings(majorLinesColor, 10.0, true, true, 1);
    GridSettings   minor = new GridSettings(Color.gray, 2.0, false, false, 1);
    SettingsPanel  majorSettings = null;
    SettingsPanel  minorSettings = null;  

    // These constants make using the visibility member of GridSetings more understandable.
    static final boolean  MAIN   = true;
    static final boolean  PANNER = false;

    public GridLView(boolean main, Layer layerParent, GridParameters params) {
        super(layerParent);
        setBufferCount(1);
        this.parms = params;
        copyParmsToSettings();
    }

	private void copyParmsToSettings() {
        major.setColor(new Color(parms.majorColor));
        major.setLineWidth(parms.majorWidth);
        major.setSpacing(parms.majorSpacing);
        major.setVisible(MAIN, parms.majorMainVisible);
        major.setVisible(PANNER, parms.majorPannerVisible);
        
        minor.setColor(new Color(parms.minorColor));
        minor.setLineWidth(parms.minorWidth);
        minor.setSpacing(parms.minorSpacing);
        minor.setVisible(MAIN, parms.minorMainVisible);
        minor.setVisible(PANNER, parms.minorPannerVisible);
    }

    public void copySettingsToParms() {
        parms.majorColor         = major.getColor().getRGB();
        parms.majorWidth         = major.getLineWidth();
        parms.majorSpacing       = major.getSpacing();
        parms.majorMainVisible   = major.isVisible(MAIN);
        parms.majorPannerVisible = major.isVisible(PANNER);
        
        parms.minorColor         = minor.getColor().getRGB();
        parms.minorWidth         = minor.getLineWidth();
        parms.minorSpacing       = minor.getSpacing();
        parms.minorMainVisible   = minor.isVisible(MAIN);
        parms.minorPannerVisible = minor.isVisible(PANNER);        
    }

    private static class GridSettings
	 {
		private Color     color;
		private double    spacing;
		private boolean[] visible = new boolean[2]; // false=Main View; true=Panner View
		private int       lineWidth; 
		
		// Cached data - same for both main & panner views
		private double cachedSpacing = 0;
		private ProjObj cachedProj = null;
		private GeneralPath[] cachedLines = new GeneralPath[0];
		
		public GridSettings(Color color, double spacing, boolean mainVisible, boolean pannerVisible, int lineWidth){
			this.color = color;
			this.spacing = spacing;
			this.visible[0] = mainVisible;
			this.visible[1] = pannerVisible;
			this.lineWidth = lineWidth; 
		}
		
		public synchronized void setColor(Color newColor){
			color = newColor;
		}
		public synchronized Color getColor(){
			return color;
		}

		public synchronized void setSpacing(double newSpacing){
			spacing = newSpacing;
		}
		public synchronized double getSpacing(){
			return spacing;
		}
		
		public synchronized boolean isVisible(boolean main){
			return visible[main? 0: 1];
		}
		public synchronized void setVisible(boolean main, boolean visibility){
			visible[main? 0: 1] = visibility;
		}

		public synchronized void setLineWidth(int newLineWidth){
			lineWidth = newLineWidth;
		}
		public synchronized int getLineWidth(){
			return lineWidth;
		}

		public GeneralPath[] getLines(ProjObj proj){
			if (cachedSpacing != spacing || cachedProj != proj){
				if (spacing == 0)
					cachedLines = new GeneralPath[0];
				else
					cachedLines = generateLines(spacing);
				
				cachedSpacing = spacing;
				cachedProj = proj;
			}
			
			return cachedLines;
		}
	 }
	
	public GridLView()
	 {
		super(null);
	 }
	
	
	/**
	 * This is good - build the focus panel on demand instead of during the view's constructor
	 * That way, the saved session variables get propagated properly.
	 */
	public FocusPanel getFocusPanel() {
		if (focusPanel == null) {
			focusPanel = new FocusPanel(this, true);
			focusPanel.add("Adjustments",new GridPanel());
		}
		return focusPanel;
	}

	/**
	 ** Layer-less view; simply returns null, since it's never used.
	 **
	 ** @return null
	 **/
	protected Object createRequest(Rectangle2D where)
	 {
		return  null;
	 }

	/**
	 ** Layer-less view; simply returns without doing anything, since
	 ** it's never called.
	 **/
	public void receiveData(Object layerData)
	 {
		log.aprintln("PROGRAMMER: SOMETHING'S OFF!");
		log.aprintStack(5);
	 }

	protected Layer.LView _new()
	 {
		GridLView newLView = new GridLView();
		
		newLView.parms = parms;
		// Share the major/minor settings from the very first LView
		newLView.major = major;
		newLView.minor = minor;
		
		return newLView;
	 }

	/**
	 ** Draws in lat/lon lines offscreen.
	 **/
	protected void viewChangedPost()
	 {
		redrawGrid();
	 }

	/**
	 ** Maintains an id to keep track of redraws, so that as soon as a
	 ** new one is issued, the older ones die. Without this, we end up
	 ** overlapping our redraws when the user pans/zooms too quickly.
	 **/
	private void redrawGrid()
	 {
		int id;
		synchronized(viewChangeIDLock)
		 {
			id = ++lastViewChangeID;
		 }
		redrawGrid(id);
	 }
	private int lastViewChangeID = 0;
	private Object viewChangeIDLock = new Object();

	private boolean oldViewChange(int id)
	 {
		// Should really be synchronized over viewChangeIDLock, but
		// it's only ever called while under a redrawLock, so we'll
		// pretend it's safe.
		return  lastViewChangeID != id;
	 }

	private void redrawGrid(int id)
	 {
		synchronized(redrawLock)
		 {
			if(!oldViewChange(id)) {
				clearOffScreen();
			} else {
				return;
			}
			
			if (!isAlive()) {
				return;
			}
			
			// Determine how many periods to draw (360-degree horizontal boxes)
			double minX = viewman.getProj().getWorldWindow().getMinX();
			double maxX = viewman.getProj().getWorldWindow().getMaxX();

			long minPeriod = (long) Math.floor(minX / 360);
			long maxPeriod = (long) Math.ceil (maxX / 360);
			int count = (int) (maxPeriod - minPeriod);

			// Create graphics contexts for each period
			Graphics2D g2 = getOffScreenG2Raw();
			prepare(g2);
			if(g2 == null)
				return;

			Graphics2D[] copies = new Graphics2D[count];
			for(int i=0; i<count; i++)
			 {
				copies[i] = (Graphics2D) g2.create();
				copies[i].translate(360*(i+minPeriod), 0);
				copies[i].clip(new Rectangle(0, -90, 360, 180));
			 }

			// Actually draw the grid elements
			drawLatLon(id, copies, minor);
			drawLatLon(id, copies, major);

			if(!oldViewChange(id))
				repaint();
		 }
	 }
	private Object redrawLock = new Object();

	private static GeneralPath[] generateLines(double spacing){
		log.println("spacing = " + spacing);
		log.printStack(3);
		int expected = (int) Math.ceil(360/spacing + 180/spacing);
		log.println("Expecting " + expected + " paths");
		List<GeneralPath> paths = new ArrayList<GeneralPath>(expected);
		GeneralPath gp = new GeneralPath();

		final double prec = 2;

		// Lines of latitude
		for(double lat=-90+spacing; lat-0.001<=+90-spacing; lat+=spacing)
		 {
			gp.reset();

			Point2D start = worldPt(0, lat);
			gp.moveTo((float) start.getX(),
					  (float) start.getY());

			for(double lon=prec; lon-0.0000001<=360; lon+=prec)
				connectTo(gp, worldPt(lon, lat));

			paths.add((GeneralPath)gp.clone());
		 }

		// Lines of longitude
		for(double lon=0; lon<360; lon+=spacing)
		 {
			gp.reset();

			Point2D start = worldPt(lon, -90);
			gp.moveTo((float) start.getX(),
					  (float) start.getY());

			for(double lat=-90+prec; lat-0.0000001<=+90; lat+=prec)
				connectTo(gp, worldPt(lon, lat));

			paths.add((GeneralPath)gp.clone());
		 }

		log.println("Generated " + paths.size() + " paths");

		return  (GeneralPath[]) paths.toArray(new GeneralPath[0]);
	}

	private void drawLatLon(int id, Graphics2D[] g2s, GridSettings settings)
	 {

		int lineStroke = 1; // default to thinnest for the panner

		if(settings.isVisible(getChild() != null))
		 {
			log.printStack(3);
			GeneralPath[] lines = settings.getLines(Main.PO);

			int zoomFactor = 1;
			
			if (HighResExport.exporting) {
				zoomFactor = HighResExport.zoomFactor;;
			}
			
			// Determine line width stroke to use. Always use thinnest possible for the Panner.
			if (getChild() != null)
				lineStroke = settings.getLineWidth() * zoomFactor;
			
			// Draw them all
			log.println("Drawing " + lines.length +
						" paths to " + g2s.length + " graphics contexts");
			for(int i=0; i<g2s.length; i++)
			 {
				g2s[i].setColor(settings.getColor());
				g2s[i].setStroke(getProj().getWorldStroke(lineStroke));
				for(int j=0; j<lines.length; j++)
				 {
					if(oldViewChange(id))
						return;
					g2s[i].draw(lines[j]);
				 }
			 }
		 }
	 }

	private static void connectTo(GeneralPath gp, Point2D next)
	 {
		//log.println("------------------------------------------");
		//log.println("connectTo " + next);
		Point2D curr = gp.getCurrentPoint();
		if(next.distanceSq(curr) < 90*90)
			gp.lineTo((float)next.getX(),
					  (float)next.getY());
		else
		 {
			/**
			 ** We have a discontinuity, because we've drawn past the
			 ** current period's box. We solve this by drawing
			 ** twice... once from the real curr point to a fake next
			 ** point, and again from a fake curr point to the real
			 ** next point. These fake points are outside the box,
			 ** positioned to emulate wrap-around from one side of the
			 ** box to another.
			 **/

			// Fake x, if necessary
			double fakeCurrX = curr.getX();
			double fakeNextX = next.getX();
			if(Math.abs(fakeCurrX - fakeNextX) > 180)
			 {
				//log.println("Faking X");
				fakeCurrX += curr.getX()<180 ? 360 : -360;
				fakeNextX += next.getX()<180 ? 360 : -360;
			 }

			// Fake y, if necessary
			double fakeCurrY = curr.getY();
			double fakeNextY = next.getY();
			if(Math.abs(fakeCurrY - fakeNextY) > 90)
			 {
				//log.println("Faking Y");
				fakeCurrY += curr.getY()<0 ? 180 : -180;
				fakeNextY += next.getY()<0 ? 180 : -180;
			 }

			// Draw from the real curr to the fake next
			gp.lineTo((float) fakeNextX,
					  (float) fakeNextY);

			// Draw from the fake curr to the real next
			gp.moveTo((float) fakeCurrX,
					  (float) fakeCurrY);
			gp.lineTo((float) next.getX(),
					  (float) next.getY());

			//log.println("line from " + curr.getX() + ", " + curr.getY());
			//log.println("       to " + fakeNextX   + ", " + fakeNextY  );
			//log.println("  move to " + fakeCurrX   + ", " + fakeCurrY  );
			//log.println("  line to " + next.getX() + ", " + next.getY());
		 }
	 }

	// Handy utility
	private static Point2D worldPt(double lon, double lat)
	 {
		return  Main.PO.convSpatialToWorld(new Point2D.Double(lon, lat));
	 }

	private class SettingsPanel
	 extends JPanel
	 implements ActionListener
	 {
		GridSettings settings;

		JTextField txtSpacing;
		JButton    btnColor;
		JCheckBox  chkMain;
		JCheckBox  chkPanner;
		JComboBox  lineWidthCombo;

		SettingsPanel(final String title,
					  GridSettings settings)
		 {
			this.settings = settings;

			// Set up the layout and borders
			setLayout(new GridLayout(0, 1, 0, 5));
			setBorder(
				BorderFactory.createCompoundBorder(
					BorderFactory.createTitledBorder(title),
					BorderFactory.createEmptyBorder(5, 5, 5, 5)
					)
				);

			// Spacing text field
			add(new JLabel("Spacing:", JLabel.LEFT));
            txtSpacing = new PasteField(String.valueOf(settings.getSpacing()), 4);
			txtSpacing.addActionListener(this);
			add(txtSpacing);

			// Color chooser button
			add(new JLabel("Color:", JLabel.LEFT));
			btnColor = new JButton(" ");
			btnColor.setBackground(settings.getColor());
			btnColor.addActionListener(this);
			add(btnColor);
			
			// Main checkbox
			add(new JLabel("Visible on:", JLabel.LEFT));
			chkMain = new JCheckBox("Main Window", settings.isVisible(MAIN));
			chkMain.addActionListener(this);
			add(chkMain);

			// Panner checkbox
			chkPanner = new JCheckBox("Panner", settings.isVisible(PANNER));
			chkPanner.addActionListener(this);
			add(chkPanner);
			
			// Grid line width combo box
			Integer[] widthChoices = { 1,2,3,4,5,6,7,8,9,10 };

			lineWidthCombo = new JComboBox(widthChoices);
			lineWidthCombo.setSelectedIndex(settings.getLineWidth()-1);
			lineWidthCombo.addActionListener(this);

			// Place the label and the combo on the same row
			JPanel lineWidthPanel = new JPanel();
			lineWidthPanel.setLayout(new GridLayout(1,2,10,0));
			lineWidthPanel.add(new JLabel("Line width:", JLabel.LEFT));
			lineWidthPanel.add(lineWidthCombo);
			add(lineWidthPanel);

		 }	// Constructor

		// Update default settings on the SettingsPanel
		public void updatePanelSettings(GridSettings newSettings)
		 {
			
			// Spacing text field
			txtSpacing.setText(String.valueOf(newSettings.getSpacing()));

			// Color chooser button
			btnColor.setBackground(newSettings.getColor());
			
			// Main checkbox
            chkMain.setSelected(newSettings.isVisible(MAIN));

			// Panner checkbox
            chkPanner.setSelected(newSettings.isVisible(PANNER));
			
			// Grid line width combo box
			lineWidthCombo.setSelectedIndex(major.getLineWidth()-1);			
		 }
		
		// Handles all events from user
		public void actionPerformed(ActionEvent e)
		 {
			Object source = e.getSource();
			// Track what needs redrawing, based on what changed
			boolean redrawMain   = false;
			boolean redrawPanner = false;

			// Handle spacing value changes
			if(source == txtSpacing)
			 {
				double spacing;
				try
				 {
					// Note: parseDouble may throw NumberFormatException
					spacing = Double.parseDouble(txtSpacing.getText().trim());

					// Check to see if it divides evenly
					if(90.0 / spacing != Math.floor(90.0 / spacing))
						throw  new NumberFormatException();
				 }
				catch(NumberFormatException ex)
				 {
					// Handle problems by beeping and showing a dialog
					String oldValue = String.valueOf(settings.getSpacing());
					Toolkit.getDefaultToolkit().beep();
					JOptionPane.showMessageDialog(
						this,
						"Grid spacing value must be a number that divides " +
						"evenly into 90:\n\t" + txtSpacing.getText(),
						"Illegal value",
						JOptionPane.ERROR_MESSAGE);

					// Restore the old value and highlight it.
					txtSpacing.setText(oldValue);
					txtSpacing.setSelectionStart(0);
					txtSpacing.setSelectionEnd(oldValue.length());
					txtSpacing.requestFocus();

					return;
				 }

				// Spacing propagates to the main window and the panner
				settings.setSpacing(spacing);

				// Determine what needs to be redrawn, based on this change
				redrawMain = chkMain.isSelected();
				redrawPanner = chkPanner.isSelected();
			 }

			// Handle color chooser changes
			else if(source == btnColor)
			 {
				Color newColor = JColorChooser.showDialog(
					Main.mainFrame,
					"Choose a gridline color",
					settings.getColor());

				if(newColor != null)
				 {
					// Reflect the new color
					btnColor.setBackground(newColor);

					// Color propagates to the main window and the panner
					settings.setColor(newColor);

					// Only repaint those whose lines are visible
					redrawMain = chkMain.isSelected();
					redrawPanner = chkPanner.isSelected();
				 }
			 }

			// Handle main checkbox changes
			else if(source == chkMain)
			 {
				settings.setVisible(MAIN, chkMain.isSelected());
				redrawMain = true;
			 }

			// Handle panner checkbox changes
			else if(source == chkPanner)
			 {
				settings.setVisible(PANNER, chkPanner.isSelected());
				redrawPanner = true;
			 }

			// Handle line width ComboBox changes
			else if(source == lineWidthCombo)
			 {
				settings.setLineWidth( ((Integer) lineWidthCombo.getSelectedItem()).intValue() );
				redrawMain = true;				
			 }
			
			
			// Based on what changed, trigger appropriate redraws
			if(redrawMain)
				redrawGrid();
			if(redrawPanner)
				((GridLView) getChild()).redrawGrid();
			if(redrawMain || redrawPanner)
			    copySettingsToParms();  // If changes have been made, we need to save them to the save/load param object.
		 }
	 }



	private class GridPanel
	 extends JPanel
	 {
		JButton refresh;
		GridPanel()
		 { 
            majorSettings = new SettingsPanel("Major Lines", major);
            minorSettings = new SettingsPanel("Minor Lines", minor);
		    Color lightblue = UIManager.getColor("TabbedPane.selected");
			setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
			setBackground(lightblue);
			
			JPanel settings = new JPanel();
			settings.setLayout(new GridLayout(1, 2));
			settings.setBackground(lightblue);
			settings.add(majorSettings);
			settings.add(minorSettings);
			
			JPanel button = new JPanel();
			button.setBackground(lightblue);
			refresh = new JButton(refreshAct);
			button.add(refresh);
			
			add(settings);
			add(Box.createVerticalStrut(10));
			add(button);
			add(Box.createVerticalGlue());
		 }

	}
	
	
	Action refreshAct = new AbstractAction("Refresh View"){
		public void actionPerformed(ActionEvent e) {
			try{
				// Check major spacing
				double majorSpacing;
				majorSpacing = Double.parseDouble(majorSettings.txtSpacing.getText().trim());
				if(90.0 / majorSpacing != Math.floor(90.0 / majorSpacing))
					throw new NumberFormatException();
				majorSettings.settings.setSpacing(majorSpacing);
				
				//Check minor spacing
				double minorSpacing;
				minorSpacing = Double.parseDouble(minorSettings.txtSpacing.getText().trim());
				if(90.0 / minorSpacing != Math.floor(90.0 / minorSpacing))
					throw new NumberFormatException();
				minorSettings.settings.setSpacing(minorSpacing);
				
			}catch(NumberFormatException nfe){
				// Handle problems by showing a dialog
				Toolkit.getDefaultToolkit().beep();
				JOptionPane.showMessageDialog(
					majorSettings,
					"Grid spacing value must be a number that divides " +
					"evenly into 90.",
					"Illegal value",
					JOptionPane.ERROR_MESSAGE);

				return;
			}
			// Determine what needs to be redrawn, based on this change
			boolean redrawMain = minorSettings.chkMain.isSelected() || majorSettings.chkMain.isSelected();
			boolean redrawPanner = minorSettings.chkPanner.isSelected() || majorSettings.chkMain.isSelected();
			
			// Based on what changed, trigger appropriate redraws
			if(redrawMain)
				redrawGrid();
			if(redrawPanner)
				((GridLView) getChild()).redrawGrid();
            if(redrawMain || redrawPanner)
                copySettingsToParms(); // If changes have been made, we need to save them to the save/load param object.
		}
	};

	public String getName()
	{
		return "Lat/Lon Grid";
	}

    private static void prepare(Graphics2D g2)
     {
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
							RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
							RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
     }
    
//The following two methods are used to query for the
// info panel fields (description, citation, etc)	
   	public String getLayerKey(){
   		return "Lat/Lon Grid";
   	}
   	public String getLayerType(){
   		return "llgrid";
   	}
 }
