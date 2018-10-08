package edu.asu.jmars;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import edu.asu.jmars.places.PlacesMenu;
import edu.asu.jmars.swing.PasteField;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.Util;

public final class LocationManager extends JPanel
{
	public static final String actionCommandSetLocation = "set location";
	
	private static final DecimalFormat f = new DecimalFormat("0.###");
	private static DebugLog log = DebugLog.instance();
	
	public JButton backPlaceBtn;			// previous place
	public JButton forwardPlaceBtn;			// next place
	
	private JTextField locInputField = null;               // location input field
	private JPanel locInputFieldLabel = null;              // label of the input field
	private final Point2D loc = new Point2D.Double();      // default location
	private List<LocationListener> listeners = new ArrayList<LocationListener>(); // list of location listeners
	
	/**
	 * Public access point for externally setting the location.
	 *
	 * @param newLoc The new location point in WORLD COORDINATES, which will be reflected
	 * in the input field.
	 * @param propagate If true, the location change will be
	 * propagated to all location listeners; if false, the change
	 * will not be propagated.
	 */
	public void setLocation(Point2D newLoc, boolean propagate)
	{
		// set the new location
		loc.setLocation(newLoc);

		refreshLocationString();
		log.println("External location set: " + loc);

		if(propagate) {
			// dispatch a location and zoom update message to every LocationListener
			for (LocationListener ll: listeners) {
				ll.locationChanged(new Point2D.Double(loc.getX(), loc.getY()));
			}

			log.println("propagateLocationAndZoom(): new location+zoom propagated to " + listeners.size() + " LocationListeners.");
		}
	}
	
	/**
	 * Replaces whatever current text is in the location textbox with
	 * the programmatically-generated one.
	 */
	public void refreshLocationString()
	{
		locInputField.setText(getLocString());
	}

	private String getLocString()
	{
		return worldToText(loc);
	}

	/* Return the location in WORLD coordinates */
	public Point2D getLoc() {
		return new Point2D.Double(loc.getX(), loc.getY());
	}

	/**
	 * Attempts to initiate a reprojection based on the current
	 * contents of the location text field.
	 */
	public void reprojectFromText()
	{
		Point2D newWorld = reprojectFromText(locInputField.getText());
		log.println(newWorld);
		if(newWorld != null)
			setLocation(newWorld, true);
	}

	public LocationManager( Point2D initialLocation ){
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		log.println("initializing(): " + initialLocation);
		loc.setLocation(initialLocation);
		
		/* create back and forward buttons for location */
		ImageIcon backIcon = Util.loadIcon("resources/left_arrow.png");
		backPlaceBtn = new JButton(backIcon);
		backPlaceBtn.setToolTipText("Changes location to previous place.");
		backPlaceBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Main.places.getPlaceHistory().back();				
			}
		});
		ImageIcon fwdIcon = Util.loadIcon("resources/right_arrow.png");
		forwardPlaceBtn = new JButton(fwdIcon);
		forwardPlaceBtn.setToolTipText("Changes location to next place.");
		forwardPlaceBtn.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				Main.places.getPlaceHistory().forward();
			}
		});
		add(backPlaceBtn);
		add(forwardPlaceBtn);
		
		
		/* create the label for the location input field */
		locInputFieldLabel = new JPanel();		
		locInputFieldLabel.add(new JLabel("Lon, Lat "));
		
		locInputFieldLabel.setToolTipText("Specify new view center.");
		add(locInputFieldLabel);
		
		/* create the location input field */
		locInputField = new PasteField(getLocString(), 15);
		locInputField.setActionCommand(actionCommandSetLocation);
		locInputField.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				// reset the message line
				Main.setStatus(null);

				// read input from locInputField and propagate it
				readInputLoc();
			}
		});
		
		add(locInputField);
		add(Box.createHorizontalGlue());
	}

	/**
	 ** Returns the location bar's raw value (whether it's been
	 ** submitted by the user or not).
	 **
	 ** @return null on error
	 **/
	private Point2D getWorldPt()
	{
		return textToWorld(locInputField.getText());
	}

	// parse location input within the locInputField and propagate it
	protected void readInputLoc()
	{
		Point2D world = getWorldPt();
		if(world == null)
			return;

		setLocation(world, true);
	}

	/** Register a LocationListener with this LocationManager. */
	public void addLocationListener(LocationListener ll)
	{
		listeners.add(ll);
	}

	public boolean removeLocationListener(LocationListener ll){
		return listeners.remove((Object)ll);
	}
	
	public String worldToText(Point2D worldPt)
	{
		Point2D spatial = Main.PO.convWorldToSpatial(worldPt);

		double x = spatial.getX();
		double y = spatial.getY();

		x = (360 - (x % 360.)) % 360.; // JMARS west lon => USER east lon
		y = Util.roundToMultiple(y, 0.001);
		return  f.format(x) + "E, " + f.format(y);
	}

	public Point2D textToWorld(String text)
	{
		Point2D pt = textToSpatial(text);
		if(pt != null)
			pt = Main.PO.convSpatialToWorld(pt);
		return  pt;
	}

	private static Point2D textToSpatial(String text)
	{
		StringTokenizer t = new StringTokenizer(text, ",");
		int   nTokens = t.countTokens();

		final String illegalInputFormat =
			"Illegal input format. Should be \"lon [E|W], lat\", with abs(lat) <= 90.0";

		if(nTokens == 2  ||  nTokens == 3)
		{
			try
			{
				// center point specified as (lon, lat)
				String lonStr = t.nextToken().trim();
				String dirStr;
				if(nTokens == 3)
					dirStr = t.nextToken().trim();
				else
				{
					int dirStart = Math.max(lonStr.indexOf('W'),
							lonStr.indexOf('E'));
					// If omitted, default user input to east longitude
					if(dirStart == -1)
						dirStr = "E";
					else
					{
						dirStr = lonStr.substring(dirStart);
						lonStr = lonStr.substring(0, dirStart);
					}
				}

				double lon = Double.parseDouble(lonStr);
				if(dirStr.equalsIgnoreCase("E"))
					lon = -lon; // USER east lon => JMARS west lon
				else if(!dirStr.equalsIgnoreCase("W"))
				{
					Toolkit.getDefaultToolkit().beep();
					Main.setStatus(illegalInputFormat);
					return  null;
				}

				double lat = Double.parseDouble(t.nextToken().trim());
				if (Math.abs (lat) > 90.0) {
					Toolkit.getDefaultToolkit().beep();
					Main.setStatus(illegalInputFormat);
					return  null;
				}

				return  new Point2D.Double(lon, lat);
			}
			catch(NumberFormatException e)
			{
				log.println(e);
				Toolkit.getDefaultToolkit().beep();
				Main.setStatus("Illegal input. Numbers expected.");
				return  null;
			}
		}
		else
		{
			Toolkit.getDefaultToolkit().beep();
			Main.setStatus(illegalInputFormat);
			return  null;
		}
	}

	public Point2D reprojectFromText(String rawText)
	{
		Point2D ctrLonLat = textToSpatial(rawText);
		if(ctrLonLat == null)
			return  null;
		ProjObj po = new ProjObj.Projection_OC(ctrLonLat.getX(),
				ctrLonLat.getY());
		Main.setProjection(po);
		// JMARS west lon => USER east lon
		double lon = (360 - ctrLonLat.getX()) % 360;
		double lat = Util.roundToMultiple(ctrLonLat.getY(), 0.001);
		Main.setTitle(null,
				f.format(lon) + "E " +
				f.format(lat) + "N");
		Point2D newWorldPt = po.convSpatialToWorld(ctrLonLat);
		return  newWorldPt;
	}

}
