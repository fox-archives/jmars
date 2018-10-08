package edu.asu.jmars.layer.util.features;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;

import edu.asu.jmars.Main;
import edu.asu.jmars.ProjObj;
import edu.asu.jmars.ProjObj.Projection_OC;
import edu.asu.jmars.ToolManager;
import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.MultiProjection;
import edu.asu.jmars.layer.WrappedMouseEvent;
import edu.asu.jmars.layer.shape2.PixelExportDialog;
import edu.asu.jmars.layer.shape2.ShapeLView;
import edu.asu.jmars.layer.shape2.ShapeLayer;
import edu.asu.jmars.layer.stamp.StampFactory;
import edu.asu.jmars.layer.util.features.GeomSource.Units;
import edu.asu.jmars.layer.util.features.GeomSource.AngleUnits;
import edu.asu.jmars.layer.util.features.GeomSource.LengthUnits;
import edu.asu.jmars.util.HVector;
import edu.asu.jmars.util.ObservableSet;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.util.ellipse.geometry.Ellipse;
import edu.asu.jmars.util.ellipse.geometry.FitEllipse;


/**
 *  Defines mouse behavior in the JMARS Shape Framework. Layers may define what behaviors 
 *  are allowed by sending in the or'ed list of behaviors in "definedBehavior".
 *
 * @author James Winburn with copious help from Noel Gorelick, Saadat Anwar, and Eric Engle 
 *              5/2006 MSFF-ASU
 */

/**
 * @author srcheese Ellipse updates
 *
 */
 
public class FeatureMouseHandler extends MouseInputAdapter {
	private final ShapeLayer shapeLayer;
	private final ShapeLView         lview;

	// various and sundry variables used by the class.
	private Point2D             mouseLast       = null;
	private Point2D             mouseDown       = null;
	private Point2D             mouseCurr       = null;
	
	// if these are set, then we will be editing a vertex.
	private Feature             selectedVertexFeature = null;
	/** A specific point of editing control in spatial west coordinates */
	private Point2D             selectedVertex  = null;

	private boolean             drawSelectionRectangleOK  = false;
	private boolean             drawSelectionGhostOK      = false;
	private boolean             drawVertexBoundingLinesOK = false;

	private boolean             addPointsOK         = false;
	private boolean             addLinesOK          = false;
	private boolean             addPolysOK          = false;

	private boolean             moveFeaturesOK      = false;
	private boolean             deleteFeaturesOK    = false;

	private boolean             moveVertexOK        = false;
	private boolean             addVertexOK         = false;
	private boolean             deleteVertexOK      = false;

	private boolean             changeModeOK        = false;
	private boolean             zorderOK            = false;

	// context menu items.
	private JRadioButtonMenuItem addModeRadioButton;
	private JRadioButtonMenuItem addStreamModeRadioButton;
	private JRadioButtonMenuItem addPointsOnlyRadioButton;
	private JRadioButtonMenuItem addLinesOnlyRadioButton;
	private JRadioButtonMenuItem addPolygonsOnlyRadioButton;
	
	private JRadioButtonMenuItem addCircleRadioButton;
	private JRadioButtonMenuItem add5PtEllipseRadioButton;
	private JRadioButtonMenuItem selectModeRadioButton;    
	private JMenuItem            deletePointMenuItem;
	private JMenuItem            addPointMenuItem;
	private JMenuItem            zOrderMenuItem;
	private JMenuItem            deleteRowMenuItem;
	private JMenuItem			 intersectMenuItem;
	private JMenuItem			 subtractMenuItem;
	private JMenuItem			 mergeMenuItem;
	private JMenuItem			 duplicateMenuItem;
	private JMenuItem			 pixelExportMenuItem;
	private JMenu			 findStampsMenu;
	private JMenu                shapeFunctionsMenu;
	
	// variables used by the ContextMenu controllers.
	private Rectangle2D rect;
	private Point2D     worldPt;
	
	//export variables needed
	FPath path = null;
	
	// cursors to be set up.
	ImageIcon img1    = new ImageIcon(Main.getResource("resources/pencil.png"));
	Image     pointer = img1.getImage(); 
	ImageIcon img2    = new ImageIcon(Main.getResource("resources/vertex.png"));
	Image     vertex  = img2.getImage();
	Toolkit   tk = Toolkit.getDefaultToolkit();
	private final Cursor     ADD_CURSOR       = tk.createCustomCursor(pointer, new Point(0,30), "Add");    
	private final Cursor     VERTEX_CURSOR    = tk.createCustomCursor(vertex,  new Point(15,15), "Vertex");
	private final Cursor     DEFAULT_CURSOR   = new Cursor(Cursor.DEFAULT_CURSOR);
	private final Cursor     PERIMETER_CURSOR = new Cursor(Cursor.CROSSHAIR_CURSOR);
	private final Cursor     SELECT_CURSOR    = new Cursor(Cursor.HAND_CURSOR);
	private final int        TOLERANCE = 5; 
	private final int        PROXIMITY_BOX_SIDE = 5; // proxmity box side in pixels
	private final int        STROKE_WIDTH = 2; // stroke width in pixels
	
	private final Style<FPath> geomStyle;
	
	// If running in JMARS, this will set the cursor.
	// It does nothing otherwise. 
	private void setCursor( Cursor c){
		if (lview!=null){
			lview.setCursor( c);
		}
	}
	
	// If running in JMARS, this will cause paintComponent
	// to be called.  It does nothing otherwise.
	private void repaint(){
		if (lview!=null){
			lview.repaint();
		}
	}

	/**
	 * Defines the mode in which the layer is running.
	 * This is public as external classes will need this
	 * information.
	 * 
	 * Note that the module always starts in SELECT_FEATURE_MODE.
	 */
	public static final int SELECT_FEATURE_MODE     = 0;
	public static final int ADD_FEATURE_MODE        = 1;
	public static final int MOVE_FEATURE_MODE       = 2;
	public static final int ADD_CIRCLE_MODE         = 3;
	public static final int ADD_FEATURE_STREAM_MODE = 4;
	public static final int ADD_FIVE_PT_ELLIPSE_MODE =5;
	
	int mode = SELECT_FEATURE_MODE;
	
	public int getMode(){
		return mode;
	}
	
	public void setMode( int m){
		// TODO: must select the proper context item here
		switch (m) {
		case SELECT_FEATURE_MODE:
			setCursor( SELECT_CURSOR);
			selectModeRadioButton.setSelected(true);
			break;
		case ADD_FEATURE_MODE:
			setCursor( ADD_CURSOR);
			break;
		case ADD_CIRCLE_MODE:
			setCursor(ADD_CURSOR);
			addCircleRadioButton.setSelected(true);
			break;
		case ADD_FEATURE_STREAM_MODE:
			setCursor(ADD_CURSOR);
			addStreamModeRadioButton.setSelected(true);
			break;
		case ADD_FIVE_PT_ELLIPSE_MODE:
			setCursor(ADD_CURSOR);
			add5PtEllipseRadioButton.setSelected(true);
		default:
			setCursor( DEFAULT_CURSOR);
			break;
		}
		mode = m;
	}
	
	/**
	 * Allowable behavior in this class is defined
	 * by or'ing the behavior flags in the "definedBehavior"
	 * argument in the constructor.
	 */
	public static final int ALLOW_ADDING_POINTS     = 1;
	public static final int ALLOW_ADDING_LINES      = 2;
	public static final int ALLOW_ADDING_POLYS      = 4;
	
	public static final int ALLOW_MOVING_FEATURES   = 8;
	public static final int ALLOW_DELETE_FEATURES   = 16;
	
	public static final int ALLOW_MOVING_VERTEX     = 32;
	public static final int ALLOW_DELETING_VERTEX   = 64;
	public static final int ALLOW_ADDING_VERTEX     = 128;
	
	public static final int ALLOW_CHANGE_MODE       = 256;
	public static final int ALLOW_ZORDER            = 512;
	
	// Gets the multi-projection used. 
	private MultiProjection getProj() {
		if (lview != null)
			// Normal case, used in JMars.
			return lview.getProj();
		
		// Abnormal case, used for JUnit tests.
		return MultiProjection.getIdentity();
	}
	
	/**
	 * constructor
	 * @param fc - the FeatureCollection to be added to.     Cannot be null.
	 * @param lview - the LView associated with the module.  Null if not running in JMARS.
	 * @param selectColor - the color of the selection line.  This is abstract to allow users 
	 *            to change the color dynamically.
	 * @param definedBehavior - an or'ed combination of behaviors that this module should
	 *            be sensitive to.
	 * @param history - History or undo log.
	 */
	public FeatureMouseHandler(ShapeLayer shapeLayer, ShapeLView lview, int definedBehavior) {
		this.shapeLayer = shapeLayer;
		this.lview = lview;
		this.geomStyle = shapeLayer.getStylesLive().geometry;
		
		addPointsOK = (definedBehavior & ALLOW_ADDING_POINTS)==ALLOW_ADDING_POINTS;
		addLinesOK = (definedBehavior & ALLOW_ADDING_LINES)==ALLOW_ADDING_LINES;
		addPolysOK = (definedBehavior & ALLOW_ADDING_POLYS)==ALLOW_ADDING_POLYS;
		moveFeaturesOK = (definedBehavior & ALLOW_MOVING_FEATURES)==ALLOW_MOVING_FEATURES;
		deleteFeaturesOK = (definedBehavior & ALLOW_DELETE_FEATURES)==ALLOW_DELETE_FEATURES;
		moveVertexOK = (definedBehavior & ALLOW_MOVING_VERTEX)==ALLOW_MOVING_VERTEX;
		addVertexOK = (definedBehavior & ALLOW_ADDING_VERTEX)==ALLOW_ADDING_VERTEX;
		deleteVertexOK = (definedBehavior & ALLOW_DELETING_VERTEX)==ALLOW_DELETING_VERTEX;
		changeModeOK = (definedBehavior & ALLOW_CHANGE_MODE)==ALLOW_CHANGE_MODE;
		zorderOK = (definedBehavior & ALLOW_ZORDER)==ALLOW_ZORDER;
		
		setupContextMenu();
		setMode(mode);
	}


	// a list of the points created when drawing. The point could represent
	// just a point or the vertex of a polyline or polygon
	private List<Point2D> points = new ArrayList<Point2D>();
	
	private int mouseContext = -1;
	
	
	/**
	 * If there was a mouseClick in selection mode, as opposed to a mousePressed/mouseReleased pair,
	 * do a selection on the topmost feature.
	 */
	public void mouseClicked(MouseEvent e){
		if(ToolManager.getToolMode() == ToolManager.SEL_HAND){
			mouseContext = MouseEvent.MOUSE_CLICKED;
			switch(getMode()){
				case SELECT_FEATURE_MODE:
					mouseCurr = getProj().screen.toSpatial(((WrappedMouseEvent)e).getRealPoint());
					Rectangle2D rect = getProj().getClickBox(getProj().spatial.toWorld(mouseCurr), PROXIMITY_BOX_SIDE);    
					
					if ( (e.getModifiers() & InputEvent.CTRL_MASK) !=0)
						toggleTopmostFeature( rect);
					else
						selectTopmostFeature( rect);
					mouseLast = null;
					break;
					
				case MOVE_FEATURE_MODE:
					// If a mouse-press put us into move-feature mode, snap out of it 
					// if it turns out that we weren't moving at all.
					setMode(SELECT_FEATURE_MODE);
					break;
                case ADD_FEATURE_STREAM_MODE:
                    if (points.isEmpty()) {
                        this.points.add(mouseDown);
                    }
                    else {
                        if (SwingUtilities.isLeftMouseButton(e) && e.isControlDown()) {
                            points.add(points.get(0));
                            addFeature(true, points);
                        }
                        else if (SwingUtilities.isLeftMouseButton(e) && !e.isControlDown()){
                            addFeature(false, points);
                        }
                        points.clear();
                    }
                    break;
                case ADD_FIVE_PT_ELLIPSE_MODE:
                	//points array is cleared as soon as 5 point mode radio 
                	// button is selected
                	points.add(mouseDown);
                	repaint();
                	if(points.size() == 5){
                		//create and add the ellipse
                		addFeature(getCurrentEllipse());
                		//clear the points list
                		points.clear();
                	}
                	break;
			}
		}
	}
	
	/** @return true if the geometry has been configured to produce circles. */
	private boolean isAddCirclesOkay() {
		StyleSource<?> source = shapeLayer.getStylesLive().geometry.getSource();
		return source instanceof GeomSource && ((GeomSource)source).getCircleFields().size() == 2;
	}
	
	/** @return true if the geometry has been configured to produce ellipses. */
	private boolean isAddEllipsesOkay() {
		StyleSource<?> source = shapeLayer.getStylesLive().geometry.getSource();
		return source instanceof GeomSource && ((GeomSource)source).getEllipseFields().size() == 6;
	}
	
	/**
	 * @return a spatial west vertex from the given circle feature, if it *is* a
	 *         circle feature and the given rectangle overlaps the line that
	 *         starts with the returned vertex.
	 */
	private Point2D getCircleEdgePoint(Feature f, Rectangle2D rect) {
		if (!FeatureUtil.isCircle(geomStyle, f)) {
			return null;
		}
		// get world coordinates to compare with 'rect'
		FPath path = shapeLayer.getIndex().getWorldPath(f);
		// compute the world coordinate distance we will allow rectangle
		// center to be from an edge
		double tolerance = Math.sqrt(
			Math.pow(getProj().getPixelWidth()*15, 2) +
			Math.pow(getProj().getPixelHeight()*15, 2));
		// circle feature, so use vertex cursor if the mouse is over the edge
		Point2D[] vertices = path.getVertices();
		int[] hitIndices = getBoundingIndices(vertices, rect);
		if (hitIndices != null) {
			// Rectangle hits this line, but if circle size is below
			// tolerance, only return hits *outside* the shape so
			// when the user is zoomed out and the circle is small,
			// user can at least resize it.
			Shape shape = path.getShape();
			Rectangle2D gpSize = shape.getBounds2D();
			if (Math.min(gpSize.getWidth(), gpSize.getHeight()) > tolerance ||
					!contains360(path.getShape(), rect.getCenterX(), rect.getCenterY())) {
				return Main.PO.convWorldToSpatial(vertices[hitIndices[0]]);
			}
		}
		return null;
	}
	
	private void updateCircleRadius(Feature f) {
		double km = Util.angularAndLinearDistanceWorld(
			f.getPath().getWorld().getCenter(),
			getProj().spatial.toWorld(mouseCurr))[1];
		((GeomSource)geomStyle.getSource()).setRadius(f, km);
	}
	
	/**
	 * Uses the first two points in {@link #points} as opposite edges of a
	 * circle and returns a new Feature describing that circle. Returns null if
	 * {@link #isAddCirclesOkay()} is false or there are not enough points.
	 */
	private Feature getCurrentCircle() {
		Feature temp = null;
		if (isAddCirclesOkay() && points.size() >= 2) {
			temp = new Feature();
			Point2D a = getProj().spatial.toWorld(points.get(0));
			Point2D b = getProj().spatial.toWorld(points.get(1));
			if (Math.abs(b.getX() - a.getX()) > 180) {
				b.setLocation(b.getX() + 360*Math.signum(a.getX() - b.getX()), b.getY());
			}
			Point2D mid = new Point2D.Double(
					a.getX()/2 + b.getX()/2,
					a.getY()/2 + b.getY()/2);
			temp.setPath(new FPath(new Point2D[]{mid}, FPath.WORLD, false).getSpatialWest());
			if (isAddCirclesOkay() && points.size() > 1) {
				double km = Util.angularAndLinearDistanceWorld(a, b)[1] / 2;
				((GeomSource)geomStyle.getSource()).setRadius(temp, km);
			}
		}
		return temp;
	}
	
	/**
	 * Uses the 5 points in {@link #points} as opposite edges of an ellipse
	 * and returns a new Feature describing that ellipse. Returns null if
	 * {@link #isAddCirclesOkay()} is false or there are not enough points.
	 */
	private Feature getCurrentEllipse() {
		Feature ellipse = null;
		
		if (isAddEllipsesOkay() && points.size() == 5) {
			ellipse = new Feature();
			
			//get the world ellipse from the points
			ArrayList info = getCurrentWorldEllipseAndProj();
			Ellipse worldE = (Ellipse)info.get(0);		
			ProjObj po = (ProjObj)info.get(1);
			
			//get the spatial path from the world ellipse
			FPath spPath = GeomSource.getSpatialPathFromWorlEllipse(worldE, po);
			
			//convert that ellipse to spatial values
			Ellipse spE = GeomSource.convertWorldEllipseToSpatialEllipse(worldE, po);
			
			//set everything on the feature object
			ellipse.setPath(spPath);
			
			GeomSource source = (GeomSource)geomStyle.getSource();
			source.setFieldValue(ellipse, source.getAAxisField(), spE.getALength());
			source.setFieldValue(ellipse, source.getBAxisField(), spE.getBLength());
			source.setFieldValue(ellipse, source.getAngleField(), spE.getRotationAngle());
			source.setFieldValue(ellipse, source.getLatField(), spE.getCenterLat());
			source.setFieldValue(ellipse, source.getLonField(), spE.getCenterLon());
		}
		
		return ellipse;
	}
	
	
	
	/**
	 * This method takes the current 5 spatial points from the points list
	 * and uses them to calculate a world ellipse with a projection centered
	 * on the first point in the list.  Since the center of the ellipse 
	 * isn't known till after it's calculation, it calculates another world
	 * ellipse with the center at the new center location. It then compares
	 * centers and when they stop significantly changing, returns the ellipse.
	 * It also returns the ProjObj centered at the calculated spatial center
	 * point.
	 * @return An arraylist with a World Ellipse and ProjObj centered on the 
	 * spatial center of that world ellipse.
	 */
	private ArrayList getCurrentWorldEllipseAndProj(){
		ArrayList result = new ArrayList();
		Ellipse e = null;
		ProjObj po = null;
		
		Point2D prevCenter = new Point2D.Double(450,10);
		//use the first point as the starting center
		Point2D curCenter = points.get(0);
		
		//keep looping until the points are very close together
		while(Point2D.distance(prevCenter.getX(), prevCenter.getY(), curCenter.getX(), curCenter.getY()) > 0.001){
		
			//convert all the points to world points
			List<Point2D> worldPts = new ArrayList<Point2D>();
			//keep track of the world points, to check for greater than 180 separation
			Point2D prevWorld = null;
			//use a new projection to recenter on, based on one of the 5 points
			//find the centroid to use as the center of the projection
			po = new Projection_OC(curCenter.getX(), curCenter.getY());
			for(Point2D spPt : points){
				Point2D wdPt = po.convSpatialToWorld(spPt);
				//check to see if this point is too far away (wrapped in world coords)
				if(prevWorld != null && Math.abs(wdPt.getX() - prevWorld.getX())>180){
					if(wdPt.getX()<prevWorld.getX()){
						wdPt = new Point2D.Double(wdPt.getX()+360, wdPt.getY());
					}else{
						wdPt = new Point2D.Double(wdPt.getX()-360, wdPt.getY());
					}
				}
				worldPts.add(wdPt);
				prevWorld = wdPt;
			}
			//then call 5-point ellipse calculation
		
			//create ellipse from world points
			e = FitEllipse.getEllipseFromPoints(worldPts);
			
			//reset the center points (in spatial)
			prevCenter = curCenter;
			curCenter = po.convWorldToSpatial(e.getCenterPt());
		}
		
		result.add(e);
		result.add(po);
		
		return result;
	}
	
	
	private void setCircleStatusFrom(Feature f) {
		Point2D center = f.getPath().getSpatialWest().getCenter();
		String radius = "";
		if (geomStyle.getSource() instanceof GeomSource) {
			Point2D v1 = geomStyle.getValue(f).getSpatialWest().getVertices()[0];
			double km = Util.angularAndLinearDistanceWorld(
				getProj().spatial.toWorld(center),
				getProj().spatial.toWorld(v1))[1];
			GeomSource source = (GeomSource) geomStyle.getSource();
			double scale = source.getUnits().getScale();
			radius = "    " + source.getUnits().toString() + ": " +
				Main.getFormatterKm(1/scale).format(km / scale);
		}
		Main.setStatus("Center: " + Main.statusFormat(center) + radius);
	}

	/**
	 * adding a feature is done here.
	 */
	public void mousePressed(MouseEvent e){
		if(ToolManager.getToolMode() == ToolManager.SEL_HAND){
			mouseContext = MouseEvent.MOUSE_PRESSED;
			mouseDown = getProj().screen.toSpatial(((WrappedMouseEvent)e).getRealPoint());
	
			if ( getMode()==ADD_FEATURE_MODE){
				// add mode processing happens in the mouseReleased portion.
			} else if (getMode() == ADD_CIRCLE_MODE) {
				// add circle mode is a press, drag, and release
				points.clear();
				points.add(mouseDown);
			}  else if (getMode()==SELECT_FEATURE_MODE && (e.getModifiers() & InputEvent.CTRL_MASK) == 0){
				// Find out if we need to go into MOVE_FEATURE_MODE
				Rectangle2D rect = getProj().getClickBox(getProj().spatial.toWorld(mouseDown), PROXIMITY_BOX_SIDE);
				MemoryFeatureIndex idx = shapeLayer.getIndex();
				for (Feature f: shapeLayer.getSelections()) {
					//TODO: need to implement moving/recalcuating of ellipses
					//if it's an ellipse, don't allow it to be moved
					StyleSource<FPath> source = shapeLayer.getStyles().geometry.getSource();
					if(source instanceof GeomSource){
						GeomSource gs = (GeomSource) source;
						if(FeatureUtil.isEllipseSource(gs, f)){
							break;
						}
					}
					
					Point2D vertex = null;
					if (moveVertexOK || deleteVertexOK) {
						// if the cursor is over a vertex, remember it so later mouse events
						// can move or delete it
						vertex = getIntersectingVertex(f, rect);
					}
					if (vertex == null) {
						// if the cursor is over a circle edge, remember the edge position
						// so later mouse events can manipulate it
						vertex = getCircleEdgePoint(f, rect);
					}
					if (vertex != null) {
						selectedVertex = vertex;
						selectedVertexFeature = f;
						break;
					} else {
						// cursor is NOT over a vertex, the entire selection will be moved.
						selectedVertex = null;
						selectedVertexFeature = null;
						if (moveFeaturesOK && idx.getWorldPath(f).intersects(rect)) {
							setMode( MOVE_FEATURE_MODE);
							break;
						}
					}	
				}
			}
			mouseLast = mouseCurr;
		}
	}
	
	private void updateLength() {
		Main.setStatusFromWorld(
			getProj().spatial.toWorld(mouseDown),
			getProj().spatial.toWorld(mouseCurr));
	}
    
	// all we need to do when moving the mouse is select the appropriate cursor.
	public void mouseMoved(MouseEvent e){
		if(ToolManager.getToolMode() == ToolManager.SEL_HAND){
			mouseContext = MouseEvent.MOUSE_MOVED;
			mouseCurr = getProj().screen.toSpatial(((WrappedMouseEvent)e).getRealPoint());
			switch( getMode()){
			case ADD_FEATURE_MODE:
				// If the mouse movement occurs above or below the edge of the projection, ignore it
				Point2D mouseWorld = getProj().screen.toWorld(((WrappedMouseEvent)e).getRealPoint());
				if (mouseWorld.getY()>90 || mouseWorld.getY()<-90) return;
	
				// If we're just moving the cursor around after having defined a point, 
				// update the panel showing the length of the line
				if (points.size() > 0 && mouseDown != null){
					updateLength();
				}
				setCursor( ADD_CURSOR);
				
				// redraw the feature built so far
				repaint();
				break;

			case ADD_FEATURE_STREAM_MODE:
				if (!this.points.isEmpty()) {
					this.points.add(mouseCurr);
					double ppd = getProj().getPPD();

					if (addPolysOK && points.size() > 10 && intersects(mouseCurr, points.get(0), TOLERANCE/ppd)) {
						this.addFeature(true, this.points);
						this.points.clear();
					}
					else {
						updateLength();
					}

					repaint();
				}
				break;
				
			case ADD_CIRCLE_MODE:
				if (!isAddCirclesOkay()) {
					setMode(ADD_FEATURE_MODE);
					mouseMoved(e);
				}
				break;
				
			case SELECT_FEATURE_MODE:
				// Change cursor according to the current proximity to the selected polygons.
				Rectangle2D rect = getProj().getClickBox(getProj().spatial.toWorld(mouseCurr), PROXIMITY_BOX_SIDE);
	
				// If moving vertices is permitted and any features are selected 
				// in the table, check if the cursor is hovering over a vertex or an outline.
				MemoryFeatureIndex idx = shapeLayer.getIndex();
				for (Feature f: shapeLayer.getSelections()) {
					FPath path = idx.getWorldPath(f);
					if (path.getType() != FPath.TYPE_POINT) {
						// if we don't hit the exterior but we do hit the interior,
						// use the badly named 'perimeter' cursor
						if (addVertexOK && path.intersects(rect) && getIntersectingVertex(f, rect) == null) {
							setCursor( PERIMETER_CURSOR);
							return;
						}
						
						//if this point is from an ellipse, don't change to vertex_cursor
						StyleSource<FPath> source = shapeLayer.getStyles().geometry.getSource();
						if(source instanceof GeomSource){
							if(FeatureUtil.isEllipseSource((GeomSource)source, f)){
								setCursor( SELECT_CURSOR);
								return;
							}
						}
						
						if (null != getCircleEdgePoint(f, rect) ||
								null != getIntersectingVertex(f, rect)) {
							setCursor(VERTEX_CURSOR);
							return;
						}
						
						if ((deleteVertexOK || moveVertexOK) && getIntersectingVertex(f, rect) != null) {
							setCursor( VERTEX_CURSOR);
							return;
						}
					}
				}
	
				// no vertex operation detected.  set cursor to a simple selection.
				setCursor( SELECT_CURSOR);
				break;
			}
			mouseLast = mouseCurr;
		}
	}
	
	/**
	 * dragging the mouse involves the positioning of the "Selection Ghost" which can
	 * be a line or a box.
	 */
	public void mouseDragged(MouseEvent e){
		if(ToolManager.getToolMode() == ToolManager.SEL_HAND){
			mouseContext = MouseEvent.MOUSE_DRAGGED;
			mouseCurr = getProj().screen.toSpatial(((WrappedMouseEvent)e).getRealPoint());
			
			switch (getMode()){
			case ADD_FEATURE_MODE:
				// If the mouse drag occurs above or below the edge of the projection, ignore it
				Point2D mouseWorld = getProj().screen.toWorld(((WrappedMouseEvent)e).getRealPoint());
				if (mouseWorld.getY()>90 || mouseWorld.getY()<-90) return;
	
				// drag the point just added if there is one
				if (mouseDown != null) {
					updateLength();
					repaint();
				}
				break;
				
			case ADD_CIRCLE_MODE:
				if (mouseDown != null) {
					switch (points.size()) {
					case 1: points.add(mouseCurr); break;
					case 2: points.set(1, mouseCurr); break;
					}
					repaint();
				}
				break;
				
			case MOVE_FEATURE_MODE:
				drawSelectionGhostOK = true;
				repaint();
				break;
				
			case SELECT_FEATURE_MODE:
				// If the mouse drag occurs above or below the edge of the projection, ignore it
				mouseWorld = getProj().screen.toWorld(((WrappedMouseEvent)e).getRealPoint());
				if (mouseWorld.getY()>90 || mouseWorld.getY()<-90) return;
	
				// Feature/vertex is being dragged.
				if ( selectedVertex != null && selectedVertexFeature != null) {
					//if the feature is an ellipse, don't allow dragging of vertices
					StyleSource<FPath> source = shapeLayer.getStyles().geometry.getSource();
					if(source instanceof GeomSource){
						if(FeatureUtil.isEllipseSource((GeomSource)source, selectedVertexFeature)){
							return;
						};
					}
					
					// show a ghost of the new outline while user is dragging
					drawVertexBoundingLinesOK = true;
					repaint();
					mouseLast = mouseCurr;
				} else {
					// polygons are being dragged
					if (mouseDown == null){
						return;
					}
					drawSelectionRectangleOK = true;
					repaint();
					mouseLast = mouseCurr;
				}
				break;
			}
		}
	}
    
	// The actions for moving and selecting is done in mouseReleased. 
	public void mouseReleased(MouseEvent e){
		if(ToolManager.getToolMode() == ToolManager.SEL_HAND){
			mouseContext = MouseEvent.MOUSE_RELEASED;
			drawSelectionRectangleOK  = false;
			drawSelectionGhostOK      = false;
			drawVertexBoundingLinesOK = false;
			
			mouseCurr = getProj().screen.toSpatial(((WrappedMouseEvent)e).getRealPoint());
			double ppd = getProj().getPPD();
			
			switch (getMode()) {
			case ADD_FEATURE_MODE:
				// If the mouse release occurs above or below the edge of the projection, ignore it
				Point2D mouseWorld = getProj().screen.toWorld(((WrappedMouseEvent)e).getRealPoint());
				if (mouseWorld.getY()>90 || mouseWorld.getY()<-90) return;
				
				int featureType = FPath.TYPE_NONE;
				
				// If we can only add Points, then add them with a single click
				if (addPointsOK && !addLinesOK && !addPolysOK) {
					points.add(mouseCurr);
					featureType=FPath.TYPE_POINT;
				}
				// If there is only one point defined and the user clicked on it, insert a point.
				else if (addPointsOK && points.size()==1 && intersects(mouseCurr, points.get(0), TOLERANCE/ppd))
					featureType = FPath.TYPE_POINT; 
				
				// if the user clicked on the last point, insert a polyline.
				else if (addLinesOK && points.size() > 0 && intersects(mouseCurr, points.get(points.size()-1), TOLERANCE/ppd))
					featureType = FPath.TYPE_POLYLINE;
				
				// if the user clicked on the first point, complete the polygon.
				else if (addPolysOK && points.size() > 0 && intersects(mouseCurr, points.get(0), TOLERANCE/ppd))
					featureType = FPath.TYPE_POLYGON;
				
				// if we can add polys or lines, add the point to the array of points.
				else if (addLinesOK || addPolysOK || (addPointsOK && points.size()==0))
					points.add(mouseCurr);
				
				// barf at the user.
				else
					Toolkit.getDefaultToolkit().beep();
				
				// Make a new history frame.
				if (featureType != FPath.TYPE_NONE){
					if (shapeLayer.getHistory() != null)
						shapeLayer.getHistory().mark();
					addFeature(featureType == FPath.TYPE_POLYGON, points);
				}
				break;
				
			case ADD_CIRCLE_MODE:
				if (!points.isEmpty()) {
					Feature temp = getCurrentCircle();
					if (temp != null) {
						addFeature(temp);
					}
				}
				break;
				
			case MOVE_FEATURE_MODE:
				Map<Feature,Object> features = new HashMap<Feature,Object>();
				for (Feature f: shapeLayer.getSelections()) {				
					Point2D[] vertices = f.getPath().getSpatialWest().getVertices();
					vertices = offsetVertices(vertices, mouseDown, mouseCurr);
					FPath newPath = new FPath(vertices, FPath.SPATIAL_WEST, f.getPath().getClosed());
					features.put(f, newPath);
					
					//TODO: implement this when ellipses are editable
					//update center point for ellipses
//					StyleSource<FPath> source = shapeLayer.getStyles().geometry.getSource();
//					if(source instanceof GeomSource){
//						GeomSource gs = (GeomSource) source;
//						if(FeatureUtil.isEllipseSource(gs, f)){
//							Point2D cenPt = newPath.getCenter();
//							f.setAttribute(gs.getLatField(), cenPt.getY());
//							f.setAttribute(gs.getLonField(), 360-cenPt.getX());
//						}
//					}

				}
				if (features.size() > 0) {
					// Make a new history frame.
					if (shapeLayer.getHistory() != null)
						shapeLayer.getHistory().mark();
					shapeLayer.getFeatureCollection().setAttributes( Field.FIELD_PATH, features);
				}
				mouseLast = null;
				setMode( SELECT_FEATURE_MODE);
				break;
				
			case SELECT_FEATURE_MODE :
				// If we are editing a vertex, process appropriately.
				if (moveVertexOK && selectedVertex != null && selectedVertexFeature != null){
					// Make a new history frame.
					if (shapeLayer.getHistory() != null)
						shapeLayer.getHistory().mark();
					if (FeatureUtil.isCircle(geomStyle, selectedVertexFeature)) {
						// set radius field so the circle passes through the
						// current mouse position
						updateCircleRadius(selectedVertexFeature);
					} else {
						// move vertex of polyline or polygon -- point vertex moves
						// are converted to the MOVE_FEATURE_MODE and handled in
						// that case above.
						moveVertex(selectedVertexFeature, selectedVertex, mouseCurr);
					}
					selectedVertex = null;
					selectedVertexFeature = null;
					mouseLast = null;
					repaint();
					return;
				}
				
				// We have no selected vertex. continue processing as though this is a simple select.
				// features that intersect the selectRectangle are marked as selected and drawn as handles. 
				// If the mouse was pressed on a different point than it was released,
				// do a selectionRectangle selection, selecting ALL the features within
				// the extent of the rectangle.
				
				// If the two points are the same, mouseClick will alrady have processed 
				// the selection.
				// We can't do it here, because pressing the mouse button while over
				// a selected feature sends the app into move feature mode.
				if (!mouseLast.equals(mouseDown)){
					//selectRectangle.setFrameFromDiagonal( getProj().spatial.toScreen(mouseLast),
					//		getProj().spatial.toScreen(mouseDown));
					//Rectangle2D rect = getProj().screen2world(selectRectangle);
					Rectangle2D rect = new Rectangle2D.Double();
					rect.setFrameFromDiagonal(getProj().spatial.toWorld(mouseLast),
							getProj().spatial.toWorld(mouseDown));
					rect = Util.normalize360(rect).getBounds2D();
					if ((e.getModifiers() & MouseEvent.CTRL_MASK) == MouseEvent.CTRL_MASK)
						toggleFeatures(rect);
					else
						selectFeatures( rect);
					mouseLast = null;
				}
			}
		}
	}

	/**
	 * Offsets <code>vertices</code> according to the motion from the
	 * <code>from</code> point to the <code>to</code> point.<p>
	 * Vertices are offset horizontally around the <code>up</code> vector
	 * according to the signed angle between the from-up and to-up planes.<p>
	 * Vertices are offset vertically within the vertex-up plane according to
	 * the signed difference in the angle between the <code>from</code> 
	 * and <code>to</code> vectors from the <code>up</code> vector.<p>
	 * <em>Note:</em>The <code>from<code> and <code>to<code> are arbitrary
	 * points not linked with the <code>vertices</code> in any way.
	 * <em>Caveat:</em>Pole crossing polygons will get deformed.<p>
	 * <bold>This code is untested on polygons greater than 180 degrees.</bold>
	 * 
	 * @param vertices Vertices to offset (in spatial west coordinates). 
	 * @param from Start point of movement not linked to vertices (in spatial west).
	 * @param to End point of movement not linked to vertices (in spatial west).
	 * @return Offseted vertices (in spatial west).
	 */
	private Point2D[] offsetVertices(Point2D[] vertices, Point2D from, Point2D to){
		/*
		 * From & To vectors from the two (spatial) points determining the direction
		 * of motion.
		 * Up vector from the oblique-cylindrical projection.
		 * Left vector is the normal to the (From, Up) plane.
		 */
		HVector vFrom = getProj().spatial.toHVector(from).unit();
		HVector vTo = getProj().spatial.toHVector(to).unit();
		HVector up = ((Projection_OC)Main.PO).getUp().unit();
		HVector left = vFrom.cross(up).unit();
		
		/*
		 * From and To direction vectors in within the (from, up) and
		 * (to, up) frames respectively. Each of these vectors is perpendicular
		 * to the up vector. Both lie in the plane normal to the up vector. 
		 */
		HVector hFromDir = up.cross(vFrom.cross(up));
		HVector hToDir = up.cross(vTo.cross(up));
		
		/*
		 * Compute angular separation between from & up, and to & up vectors
		 * with proper sign w.r.t. the hemisphere they belong to. Their 
		 * difference will give us the vertical angle of rotation (which is
		 * around the left vector).
		 */
		double va1 = ((hFromDir.dot(vFrom)<0)?-1:1) * vFrom.separation(up);
		double va2 = ((hToDir.dot(vTo)<0)?-1:1) * vTo.separation(up);
		
		/*
		 * Vertical & horizontal separation angles between from & to vectors.
		 * These are signed rotation angles around the left vector and up vector
		 * respectively.
		 */
		double vAngle = -(va2-va1);
		double hAngle = ((left.dot(hToDir)>=0)?-1:1) * hFromDir.separation(hToDir);
		
		Point2D[] out = new Point2D[vertices.length]; // Output points.
		
		// Anchor point which determines which hemisphere other points falls in.
		HVector v0Dir = null;
		if (vertices.length > 0){
			/*
			 * Our anchor is in the (vertex[0], up) plane. It is exactly 90 degrees
			 * from the up vector. Assume it is coming out of the screen, then the
			 * screen partitions the two hemispheres. 
			 */
			HVector v = getProj().spatial.toHVector(vertices[0]).unit();
			v0Dir = up.cross(v.cross(up)).unit();
		}
		
		for(int i=0; i<vertices.length; i++){
			/*
			 * Rotate every vertex in the horizontal and vertical directions
			 * according to the angles determined above.
			 */
			HVector v = getProj().spatial.toHVector(vertices[i]).unit();
			out[i] = new Point2D.Double();
			
			/*
			 * If a point is in the hemisphere towards us (+ve dot product) then
			 * the vertical rotation is positive, otherwise, it is negative. This
			 * ensures that any polygon that goes over the up vector comes back
			 * over the up vector when pulled back.
			 */
			double vAngleSign = (v0Dir.dot(v) > 0)? 1: -1;
			v.rotate(v.cross(up).unit(), vAngleSign * vAngle).rotate(up, hAngle).toLonLat(out[i]);
		}
		
		return out;
	}
	
	/**
	 * clears all the fields of the mouse so that lines will not be set up half way.
	 */
	private void  initializeSelectionLine(){
		points.clear();
		mouseLast=mouseDown=mouseCurr=null;
	}
	
	/**
	 *  Returns whether the points intersect each other within the specified tolarance.
	 *  All values are in spatial coordinates.
	 */
	private boolean intersects( Point2D p1, Point2D p2, double tolarance){
		HVector v1 = getProj().spatial.toHVector(p1);
		HVector v2 = getProj().spatial.toHVector(p2);
		
		if (Math.toDegrees(v1.separation(v2)) <= tolarance)
			return true;
		
		return false;
	}
	
	/**
	 * deletes the most-recently defined point in the selection line and repaints whatever 
	 * is left of the line.  If the selection line consists of a single point, the entire 
	 * selection line is deleted.
	 */
	public void deleteLastVertex(){
		if (points.size()>0){
			points.remove( points.size()-1);
			if (points.size()==0){
				mouseDown = mouseLast=null;
			} else {
				mouseDown = points.get( points.size()-1);
			}
			repaint();
		}
	}
	
	/**
	 * Removes the @param worldPoint from the feature.
	 * Assumes the point is in world coordinates.
	 * If the feature is a line it must have at least 2 points  after 
	 * the delete. If the feature is a polygon it must have at least 3 points
	 * after the delete.
	 * 
	 * @param spatialPoint
	 */
	private void deleteVertex(Feature f, Point2D spatialPoint)
	{
		// Get spatial vertices
		FPath path = f.getPath().getSpatialWest();
		Point2D[] vertices = path.getVertices();
		
		// Remove the deleted point
		Set<Point2D> orderedSet = new LinkedHashSet<Point2D>(Arrays.asList(vertices));
		orderedSet.remove(spatialPoint);
		vertices = orderedSet.toArray(new Point2D[orderedSet.size()]);
		
		// Make a new FPath
		path = new FPath(vertices, FPath.SPATIAL_WEST, path.getClosed());
		
		// Set the new FPath
		// Make a new history frame.
		if (shapeLayer.getHistory() != null)
			shapeLayer.getHistory().mark();
		f.setPath(path.getSpatialWest());
	}

	/**
	 * Adds a vertex to the feature at the specified worldPoint.
	 * "rect" specifies a small rectangle around the worldPoint.
	 * This is used in determining between which vertices the
	 * new vertex is to be placed.
	 */
	private void addVertex(Feature f, Point2D worldPoint, Rectangle2D rect)
	{
		// get line that we hit
		FPath path = shapeLayer.getIndex().getWorldPath(f);
		Point2D[] vertArray = path.getVertices();
		int[] indices = getBoundingIndices(vertArray, rect);
		if (indices==null)
			return;
		
		// insert given worldPoint into the line we hit and convert to spatial
		List<Point2D> vertices = new ArrayList<Point2D>(Arrays.asList(vertArray));
		vertices.add (indices[1], worldPoint);
		Point2D[] newPoints = vertices.toArray(new Point2D[0]);
		path = new FPath (newPoints, FPath.WORLD, path.getClosed()).getSpatialWest();
		
		// Make a new history frame.
		if (shapeLayer.getHistory() != null)
			shapeLayer.getHistory().mark();
		
		f.setPath(path);
	}
	
	/**
	 * Draws a ghost outline of the new edge of the current feature. For circles
	 * this computes an entirely new perimeter, for polylines and polygons this
	 * draws the two lines touching the manipulated vertex.
	 */
	public void drawVertexBoundingLines( Graphics2D g2world){
		if (!drawVertexBoundingLinesOK || selectedVertexFeature == null){
			return;
		}
		
		g2world.setColor(Color.DARK_GRAY);
		g2world.setStroke( new BasicStroke(((float)STROKE_WIDTH)/getProj().getPPD()));
		
		if (FeatureUtil.isCircle(geomStyle, selectedVertexFeature)) {
			Feature temp = new Feature();
			for (Field field: geomStyle.getSource().getFields()) {
				temp.setAttribute(field, selectedVertexFeature.getAttribute(field));
			}
			updateCircleRadius(temp);
			g2world.draw(geomStyle.getValue(temp).getWorld().getShape());
			setCircleStatusFrom(temp);
		} else {
			Point2D[] vertices = getBoundingVertices( selectedVertexFeature, selectedVertex);
			for(int i=0; i<vertices.length; i++)
				vertices[i] = getProj().spatial.toWorld(vertices[i]);
			
			g2world.setColor(shapeLayer.getStylesLive().lineColor.getValue(null));
			if (vertices.length==2){
				// figgur out what the current position should be. It might be 360 degrees off. 
				// This check must be done in world coordinates.
				Point2D midVert = getProj().spatial.toWorld(mouseCurr);
				if ( (vertices[0].getX() - midVert.getX() > 180)  || (vertices[1].getX() - midVert.getX() > 180) ){
					midVert.setLocation( midVert.getX() + 360, midVert.getY() );
				} else if ( (midVert.getX()- vertices[0].getX() > 180)  || (midVert.getX() - vertices[1].getX() > 180) ){
					midVert.setLocation( midVert.getX() - 360, midVert.getY() );
				} 
				
				Point2D left  = vertices[0];
				Point2D right = vertices[1];
				Point2D mid   = midVert;
				
				g2world.draw(new Line2D.Double(left, mid));
				g2world.draw(new Line2D.Double(mid, right));
			} else if (vertices.length==1) {
				// figgur out what the current position should be. It might be 360 degrees off. 
				// This check must be done in world coordinates.
				Point2D midVert = getProj().spatial.toWorld(mouseCurr);
				if ( (vertices[0].getX() - midVert.getX() > 180)){
					midVert.setLocation( midVert.getX() + 360, midVert.getY() );
				} else if ( (midVert.getX()- vertices[0].getX() > 180)){
					midVert.setLocation( midVert.getX() - 360, midVert.getY() );
				} 
				
				Point2D left  = vertices[0];
				Point2D mid   = midVert;
				
				g2world.draw(new Line2D.Double(left, mid));
			}
		}
	}
	
	/**
	 * draws a "ghost" representation of all the selected features translated by "point".
	 * This must be called by the LView if it desires to see a selection ghost during
	 * mouse drags.
	 */
	public void drawSelectionGhost(Graphics2D g2world){
		if (! drawSelectionGhostOK)
			return;
		
		g2world.setColor(Color.DARK_GRAY);
		g2world.setStroke( new BasicStroke(((float)STROKE_WIDTH)/getProj().getPPD()));
		
		for(Feature feature: shapeLayer.getSelections()) {
			// get translated spatial path
			FPath path = feature.getPath();
			Point2D[] vertices = offsetVertices(path.getSpatialWest().getVertices(), mouseDown, mouseCurr);
			path = new FPath(vertices, FPath.SPATIAL_WEST, path.getClosed());
			
			if (path.getType() == FPath.TYPE_POINT) {
				// could be an actual point, or a circle
				if (FeatureUtil.isCircle(geomStyle, feature)) {
					// compute new temporary circle and draw that
					Feature temp = new Feature();
					for (Field field: geomStyle.getSource().getFields()) {
						temp.setAttribute(field, feature.getAttribute(field));
					}
					temp.setPath(path);
					g2world.draw(geomStyle.getValue(temp).getWorld().getShape());
					setCircleStatusFrom(temp);
				} else {
					// compute new temporary point box and draw that
					Point2D p = path.getWorld().getVertices()[0];
					int pointSize = shapeLayer.getStylesLive().pointSize.getValue(feature).intValue();
					Rectangle2D box = getProj().getClickBox(p, pointSize);
					g2world.fill(box);
				}
			} else {
				//could still be a path from an ellipse, which the ghost should be
				// recalculated for
				StyleSource<FPath> source = shapeLayer.getStyles().geometry.getSource();
				if(source instanceof GeomSource){
					//TODO implement this when ellipses are editable
//					GeomSource gs = (GeomSource) source;
//					if(FeatureUtil.isEllipseSource(gs, feature)){
//						//compute new temporary ellipse and draw that
//						Feature temp = new Feature();
//						for (Field field: geomStyle.getSource().getFields()) {
//							if(field == gs.getLatField()){
//								temp.setAttribute(gs.getLatField(), path.getCenter().getY());
//							}
//							else if(field == gs.getLonField()){
//								temp.setAttribute(gs.getLonField(), 360 - path.getCenter().getX());
//							}
//							else{
//								temp.setAttribute(field, feature.getAttribute(field));
//							}
//						}
//						g2world.draw(geomStyle.getValue(temp).getWorld().getShape());
//					}
				}
				else{
					// must be a polygon or line, just draw normally
					g2world.draw(path.getWorld().getShape());
				}
			}
		}
	}
	
	/**
	 * draws the line that indicates the incomplete feature.  This includes a control point at the first
	 * point where a polygon or a point can be defined and another control point at the last clicked point
	 * where a polyline can be defined.
	 * This is called by ShapeLView's paintComponent().
	 */
	public void drawSelectionLine( Graphics2D g2world) {
		if (points.isEmpty()) {
			return;
		}
		
		Styles styles = shapeLayer.getStylesLive();
		Color lineColor = styles.lineColor.getValue(null);
		int pointSize = styles.pointSize.getValue(null).intValue();
		lineColor = new Color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue());
		g2world.setColor(lineColor);
		g2world.setStroke( new BasicStroke(((float)STROKE_WIDTH)/getProj().getPPD()));
		
		if (getMode() == ADD_CIRCLE_MODE) {
			Feature temp = getCurrentCircle();
			if (temp != null) {
				g2world.draw(geomStyle.getValue(temp).getWorld().getShape());
				setCircleStatusFrom(temp);
			}
		} else if(getMode() == ADD_FIVE_PT_ELLIPSE_MODE){
			//draw the points before the ellipse is created
			for(Point2D p : points){
				p = getProj().spatial.toWorld(p);
				g2world.fill(getProj().getClickBox(p, pointSize));
			}
		} else {
			// draw first click point (the one that defines a polygon.)
			Point2D p = getProj().spatial.toWorld(points.get(0));
			g2world.fill(getProj().getClickBox(p, pointSize));
			
			if (points.size()>1){
				// draw the polygonal outline.
				FPath path = new FPath(points.toArray(new Point2D[points.size()]), FPath.SPATIAL_WEST, false);
				g2world.draw(path.getWorld().getShape());
				
				// draw last click point (the one that defines a polyline.)
				Point2D p2 = getProj().spatial.toWorld(points.get(points.size()-1));
				g2world.fill(getProj().getClickBox(p2, pointSize));
			}
			
			g2world.draw(new Line2D.Double(getProj().spatial.toWorld(points.get(points.size()-1)),
				getProj().spatial.toWorld(mouseCurr)));
			
			if (mouseContext == MouseEvent.MOUSE_DRAGGED && getMode() != MOVE_FEATURE_MODE && !drawSelectionRectangleOK)
				g2world.fill(getProj().getClickBox(getProj().spatial.toWorld(mouseCurr), pointSize));
		}
	}

	/**
	 * draws a rubber band box allowing the user to select features
	 * This is called from the ShapeLView's paintComponent()
	 */
	public void drawSelectionRectangle( Graphics2D g2world)
	{
		if (drawSelectionRectangleOK==false){
			return;
		}
		
		Rectangle2D selectRectangle = new Rectangle2D.Double();
		selectRectangle.setFrameFromDiagonal(getProj().spatial.toWorld(mouseDown),
				getProj().spatial.toWorld(mouseCurr));
		
		g2world.setColor(Color.white);
		g2world.setStroke( new BasicStroke(((float)STROKE_WIDTH)/getProj().getPPD()));
		g2world.draw(selectRectangle);
	}
	
	// adds a polyline or polygon to the FeatureCollection.
	private void addFeature(boolean closed, List<Point2D> points){
		Feature feature = new Feature();
		
		// Convert List to array, and get world-coordinates if in JMars
		Point2D[] vertices = points.toArray(new Point2D[points.size()]);
		for (int i = 0; i < vertices.length; i++) {
			vertices[i] = getProj().spatial.toWorld(vertices[i]);
		}
		
		// Convert vertices to FPath and set spatial west version
		FPath path = new FPath (vertices, FPath.WORLD, closed);
		feature.setPath (path.getSpatialWest());
		String typeString = FeatureUtil.getFeatureTypeString(path.getType());
		feature.setAttribute( Field.FIELD_FEATURE_TYPE, typeString);
		addFeature(feature);
	}
	
	private void addFeature(Feature feature) {
		// Make a new history frame.
		if (shapeLayer.getHistory() != null)
			shapeLayer.getHistory().mark();

		try {
			shapeLayer.getFeatureCollection().addFeature( feature);
		}
		catch(UnsupportedOperationException ex){
			JOptionPane.showMessageDialog(lview,
					"Cannot add Feature, since no default FeatureCollection is set.",
					"Error!",
					JOptionPane.ERROR_MESSAGE);
		}
		initializeSelectionLine();
		repaint();
	}
	
	/**
	 * selects features via a bounding box.  The rectangle is a
	 * rubber band type rectangle drawn by the user. Any
	 * feature that intersects this rectangle is flagged as selected.
	 * @param rectangle (assumed never to be null)
	 */
	private void selectFeatures(Rectangle2D rectangle){
		_selectFeature( rectangle, true, false);
	}
	
	private void toggleFeatures(Rectangle2D rectangle){
		_selectFeature( rectangle, true, true);
	}

	// selects (via a bounding box) the topmost feature with respect to Z-order.
	// The rectangle is a rubber band type rectangle drawn by the user.
	// @param rectangle (assumed never to be null)
 	private  void selectTopmostFeature(Rectangle2D rectangle){
		_selectFeature( rectangle, false, false);
	}

	private void toggleTopmostFeature( Rectangle2D rectangle){
		_selectFeature( rectangle, false, true);
	}
	
	// selects features via a bounding box.  The rectangle is a
	// rubber band type rectangle drawn by the user. Any
	// feature that intersects this rectangle is flagged as selected.
	//
	// @param rectangle (assumed never to be null)
	private  void _selectFeature(Rectangle2D rectangle, boolean multipleSelections, boolean toggleIt)
	{
		Iterator<Feature> it = shapeLayer.getIndex().queryUnwrappedWorld(rectangle);
		List<Feature> hits = new ArrayList<Feature>();
		while (it.hasNext()) {
			hits.add(it.next());
		}
		
		if (!multipleSelections && hits.size() > 0) {
			Feature f = hits.get(hits.size()-1);
			hits.clear();
			hits.add(f);
		}
		
		Set<Feature> selections = shapeLayer.getSelections();
		
		if (toggleIt) {
			// remove hits that were selected
			Set<Feature> selectedHits = new HashSet<Feature>(hits);
			selectedHits.retainAll(selections);
			selections.removeAll(selectedHits);
			hits.removeAll(selectedHits);
		} else {
			// retain only hits
			selections.retainAll(hits);
		}
		
		// add hits that were not selected
		selections.addAll(hits);
		
		repaint();
	}
	
	/**
	 * Returns true if the mouse is over the vertex of a line or polygon.
	 * @param feature Feature object to search.
	 * @param worldRect Rectangle (in world coordinates) to use for proximity matching.
	 * @return intersecting vertex in spatial (west) coordinates as it exists in
	 *         the Feature object. This is important for Point2D.equal() match
	 *         elsewhere in the code, since conversion back and forth between
	 *         world and spatial coordinates looses precision.
	 */
	private Point2D getIntersectingVertex(Feature feature, Rectangle2D worldRect) {
		FPath path = feature.getPath();
		if (path.getType()== FPath.TYPE_POINT){
			return null;
		}
		Point2D[] vertices = shapeLayer.getIndex().getWorldPath(feature).getVertices();
		for (int i=0; i<vertices.length; i++) {
			if (contains360(worldRect, vertices[i].getX(), vertices[i].getY())) {
				return path.getSpatialWest().getVertices()[i];
			}
		}
		return null;
	}
	
	/**
	 * Replaces the "from" spatial vertex with the "to" spatial vertex.  
	 * @param spatialFrom Point to be replaced in Spatial West coordinates.
	 * @param spatialTo The replacement point in Spatial West coordinates.
	 */
	private void moveVertex(Feature f, Point2D spatialFrom, Point2D spatialTo) {
		FPath path = f.getPath().getSpatialWest();
		Point2D[] vertices = path.getVertices();
		int index = Arrays.asList(vertices).indexOf(spatialFrom);
		if (index >= 0) {
			vertices[index].setLocation(spatialTo);
			f.setPath(new FPath(vertices, FPath.SPATIAL_WEST, path.getClosed()).getSpatialWest());
		}
	}
	
	/**
	 * Finds the vertices on either side of the specified vertex of a polyline
	 * or polygon feature, or an empty array of vertices if the given vertex is
	 * not found in the given feature.
	 * 
	 * @param f
	 *            Feature to search.
	 * @param centerVertex
	 *            vertex to search in spatial (west) coordinates.
	 * @return zero/one/two vertices adjacent to the specified vertex.
	 */
	private Point2D[] getBoundingVertices( Feature f, Point2D centerVertex)
	{
		FPath path = f.getPath();
		if (path.getType() == FPath.TYPE_POINT)
			return null;
		
		Point2D[] vertices = path.getSpatialWest().getVertices();
		int index = Arrays.asList(vertices).indexOf(centerVertex);
		if (index == -1)
			return new Point2D[0];
		
		// The index is known so now get the vertices on either side.
		// Special cases if index is the first or last element of the array.
		if(index == 0) {
			if (path.getType()==FPath.TYPE_POLYLINE){
				Point2D[] boundingVertices = new Point2D[1];
				boundingVertices[0] = vertices[index+1];
				return boundingVertices;
			} else { // polygon
				Point2D[] boundingVertices = new Point2D[2];
				boundingVertices[0] = vertices[index+1];
				boundingVertices[1] = vertices[vertices.length-1];
				return boundingVertices;
			}
		} else if(index == vertices.length-1) {
			if (path.getType()== FPath.TYPE_POLYLINE){
				Point2D[] boundingVertices = new Point2D[1];
				boundingVertices[0] = vertices[index-1];
				return boundingVertices;
			} else { // polygon
				Point2D[] boundingVertices = new Point2D[2];
				boundingVertices[0] = vertices[index-1];
				boundingVertices[1] = vertices[0];
				return boundingVertices;
			}
		} else {
			// no special case.
			Point2D[] boundingVertices = new Point2D[2];
			boundingVertices[0] = vertices[index+1];
			boundingVertices[1] = vertices[index-1];
			return boundingVertices;
		}
	}
	
	/**
	 * If the feature is a line it must have at least 2 points after the delete.
	 * If the feature is a polygon it must have at least 3 points after the
	 * delete. If the feature is a point or circle this method always returns
	 * false.
	 */
	private boolean canDeleteVertex(Feature f) {
		if (f.getPath().getType() == FPath.TYPE_POINT) {
			return false;
		}
		FPath path = f.getPath();
		int numPoints = path.getVertices().length;
		switch (path.getType()) {
		case FPath.TYPE_POLYGON:
			return numPoints > 3;
		case FPath.TYPE_POLYLINE:
			return numPoints > 2;
		case FPath.TYPE_POINT:
		default:
			return false;
		}
	}
	
	/**
	 * @return the indices of the vertices which form the first line that
	 *         intersects the given rectangle.
	 */
	private int[] getBoundingIndices(Point2D[] vertices, Rectangle2D rect) {
		final Line2D line = new Line2D.Double();
		for (int i = 0; i < vertices.length; i++) {
			int j = (i+1)%vertices.length;
			line.setLine(vertices[i], vertices[j]);
			if (intersects360(line, rect)) {
				return new int[]{i,j};
			}
		}
		return null;
	}
	
	private boolean intersects360(Shape shape, Rectangle2D worldRect) {
		for (Rectangle2D r: Util.toWrappedWorld(worldRect)) {
			for (Rectangle2D r2: Util.toUnwrappedWorld(r, shape.getBounds2D())) {
				if (shape.intersects(r2)) {
					return true;
				}
			}
		}
		return false;
	}
	
	private boolean contains360(Shape shape, double x, double y) {
		Rectangle2D bounds = shape.getBounds2D();
		double start = Math.floor(bounds.getMinX() / 360.0) * 360.0 + x;
		//avoid the possibility of a really long while loop or 
		// infinite while loop by checking to make sure the 
		// two points aren't unrealistically far apart
		if(start-bounds.getMaxX()>20000){
			return false;
		}
		//it's possible the start could be greater than the bounds,
		// if they are bring them below the bounds and go from there
		while(start>bounds.getMaxX()){
			start = start - 360;
		}
		for (double x2 = start; x2 < bounds.getMaxX(); x2 += 360.0) {
			if (bounds.contains(x2,y)) {
				return true;
			}
		}
		return false;
	}
	private void getMinMaxCoord(double[] minMaxCoords, double[] coords, boolean latFirst) {
	    double x = 0;
	    for(int i=0; i<coords.length; i=i+2) {
	        if (latFirst) {
	            //ignore lat values
	            x = coords[i+1];
	        } else {
	            x = coords[i];
	        }
            if (Double.compare(x,minMaxCoords[0]) < 0) {
                //the value is less than the min, set it as the min
                minMaxCoords[0] = x;
            }
            if (Double.compare(x, minMaxCoords[1]) > 0) {
                //the value is greater than the max, set it as the max
                minMaxCoords[1] = x;
            }
         }
	}
	private double[] adjustArrayForPM(double[] coords, boolean latFirst) {
	    double[] newCoords = new double[coords.length];
	    double x = 0;
	    double y = 0;
	    for(int i=0; i<coords.length; i=i+2) {
	        if (latFirst) {
	            y = coords[i];
	            x = coords[i+1];
	        } else {
	            x = coords[i];
                y = coords[i+1];
	        } 
            //check and adjust lon values
            if (Double.compare(x, 180) < 0) {
                //this value is less than 180, add 360
                x = x + 360;
            }
            
            if (latFirst) {
                newCoords[i] = y;
                newCoords[i+1] = x;
            } else {
                newCoords[i] = x;
                newCoords[i+1] = y;
            }
            
	        
        }
	    return newCoords;
	}
	private GeneralPath coordsToGeneneralPath(double[] coords, boolean closed, boolean latFirst) {
	    Point2D[] points = new Point2D[coords.length/2];
        int x = (latFirst ? 1 : 0);
        int y = (latFirst ? 0 : 1);
        for (int i = 0; i < coords.length/2; i++) {
            points[i] = new Point2D.Double (coords[i*2 + x], coords[i*2 + y]);
        }
	    GeneralPath path = new GeneralPath();
	    if (points.length > 0) {
            path.moveTo(points[0].getX(), points[0].getY());
            for (int i = 1; i < points.length; i++) {
                path.lineTo(points[i].getX(), points[i].getY());
            }
            if (closed) {
                path.closePath();
            }
        }
	    return path;
    }
	// This is an overwrite of the standard layer getMenuItems() method.
	public Component [] getMenuItems( Point2D wp)
	{
		if (wp==null) {
			return null;
		}
		this.worldPt = wp;
		this.rect = getProj().getClickBox( worldPt, PROXIMITY_BOX_SIDE);
		
		// build the list of menu items.
		List<JMenuItem> menuList = new ArrayList<JMenuItem>();
		// selections are used multiple times to enable/disable options
		ObservableSet<Feature> selections = shapeLayer.getSelections();
		
		if (changeModeOK) {
			menuList.add(addModeRadioButton);
			menuList.add(addStreamModeRadioButton);
			menuList.add(addPointsOnlyRadioButton);
			menuList.add(addLinesOnlyRadioButton);
			menuList.add(addPolygonsOnlyRadioButton);
			menuList.add(addCircleRadioButton);
			menuList.add(add5PtEllipseRadioButton);
			menuList.add(selectModeRadioButton);
		}
		
		if (deleteFeaturesOK){
			// One should only be able to delete a selected row if there is
			// in fact at least one row selected.
			if (selections.size() >0){
				deleteRowMenuItem.setEnabled(true);
			} else {
				deleteRowMenuItem.setEnabled(false);
			}
			menuList.add( deleteRowMenuItem);
		}
		
		if (zorderOK){
			// The Zorder menu should only be enabled if there is one selection.
			if (selections.size() >0){
				zOrderMenuItem.setEnabled(true);
			} else {
				zOrderMenuItem.setEnabled(false);
			}
			menuList.add(zOrderMenuItem);
		}
		
		if (addVertexOK || deleteVertexOK) {
			boolean addPoints = false;
			boolean delPoints = false;
			Iterator<? extends Feature> it = shapeLayer.getIndex().queryUnwrappedWorld(rect);
			while (it.hasNext() && (!addPoints || !delPoints)) {
				Feature f = it.next();
				if (selections.contains(f) &&
						f.getPath().getType() != FPath.TYPE_POINT) {
					if (getIntersectingVertex(f, rect) == null) {
						addPoints = true;
					} else {
						delPoints = true;
					}
				}
			}
			if (addVertexOK) {
				addPointMenuItem.setEnabled(addPoints);
				menuList.add( addPointMenuItem);
			}
			if (deleteVertexOK) {
				deletePointMenuItem.setEnabled(delPoints);
				menuList.add( deletePointMenuItem);
			}
		}
		
		if (selections.size()>0) {
			findStampsMenu.setEnabled(true);
		} else {
			findStampsMenu.setEnabled(false);
		}
		menuList.add(findStampsMenu);
		
		//have all polygon functions enabled to start
		// and disable and add tooltips to some depending  
		// on the number of selected shapes
		pixelExportMenuItem.setEnabled(true);
		duplicateMenuItem.setEnabled(true);
		subtractMenuItem.setEnabled(true);
		intersectMenuItem.setEnabled(true);
		mergeMenuItem.setEnabled(true);
		//hide all tooltips to start
		pixelExportMenuItem.setToolTipText("");
		duplicateMenuItem.setToolTipText("");
		subtractMenuItem.setToolTipText("");;
		intersectMenuItem.setToolTipText("");;
		mergeMenuItem.setToolTipText("");;
		if(selections.size()>0){
			shapeFunctionsMenu.setEnabled(true);
			int size = selections.size();
			//pixel export needs exactly one selection
			if(size != 1){
				pixelExportMenuItem.setEnabled(false);
				pixelExportMenuItem.setToolTipText("Select 1 shape");
			}
			//interset and merge need at least two selection
			if(size<2){
				intersectMenuItem.setEnabled(false);
				intersectMenuItem.setToolTipText("Select at least overlapping 2 shapes");
				mergeMenuItem.setEnabled(false);
				mergeMenuItem.setToolTipText("Select at least overlapping 2 shapes");
			}
			if(size != 2){
				//subtract needs exactly two selections
				subtractMenuItem.setEnabled(false);
				subtractMenuItem.setToolTipText("Select 2 overlapping shapes");
			}
		}else{
			shapeFunctionsMenu.setEnabled(false);
		}
		menuList.add(shapeFunctionsMenu);
		Component[] menuItems = (Component [])menuList.toArray( new Component[0]);
		return menuItems;
	}
	
	// The constructor for the class that sets up the components and 
	// all the behavior for those components.
	private void setupContextMenu() {
		// set up the context menu items.
		addModeRadioButton       = new JRadioButtonMenuItem( "Add Points/Lines/Polygons");
		addStreamModeRadioButton = new JRadioButtonMenuItem("Stream Drawing");
		addPointsOnlyRadioButton       = new JRadioButtonMenuItem( "Add Points");
		addLinesOnlyRadioButton       = new JRadioButtonMenuItem( "Add Lines");
		addPolygonsOnlyRadioButton       = new JRadioButtonMenuItem( "Add Polygons");
		addCircleRadioButton     = new JRadioButtonMenuItem( "Add Circles");
		add5PtEllipseRadioButton = new JRadioButtonMenuItem("Add 5-Pt Ellipses");
		selectModeRadioButton    = new JRadioButtonMenuItem( "Select Features");
		zOrderMenuItem           = new ZOrderMenu("Z-order",
				shapeLayer.getFeatureCollection(),
				shapeLayer.getSelections(),
				lview.getFocusPanel().getFeatureTable().getSorter());
		
		deletePointMenuItem      = new JMenuItem( "Delete Point");
		addPointMenuItem         = new JMenuItem( "Add Point");
		deleteRowMenuItem        = new JMenuItem( "Delete Selected Features");
		findStampsMenu		 = new JMenu( "Find overlapping stamps");
		shapeFunctionsMenu = new JMenu("Polygon Functions");
		intersectMenuItem = new JMenuItem("Intersect Polygons");
		subtractMenuItem = new JMenuItem("Subtract Polygons");
		mergeMenuItem = new JMenuItem("Merge Polygons Together");
		duplicateMenuItem = new JMenuItem("Duplicate Polygons");
		pixelExportMenuItem = new JMenuItem("Export Pixel Data for Polygon...");
		
		
		ButtonGroup toolSelectButtonGroup = new ButtonGroup();
		toolSelectButtonGroup.add(addModeRadioButton);
		toolSelectButtonGroup.add(addStreamModeRadioButton);
		toolSelectButtonGroup.add(addPointsOnlyRadioButton);
		toolSelectButtonGroup.add(addLinesOnlyRadioButton);
		toolSelectButtonGroup.add(addPolygonsOnlyRadioButton);
		toolSelectButtonGroup.add(addCircleRadioButton);
		toolSelectButtonGroup.add(add5PtEllipseRadioButton);
		toolSelectButtonGroup.add(selectModeRadioButton);
		
		// defines the behavior of the right-click popup menu items. 
		addModeRadioButton.addItemListener( new ItemListener() {
			public void itemStateChanged(ItemEvent event){
				if (addModeRadioButton.isSelected()) {
					points.clear();
					addPointsOK=true;
					addLinesOK=true;
					addPolysOK=true;
					setMode( FeatureMouseHandler.ADD_FEATURE_MODE);
					shapeLayer.getSelections().clear();
					ToolManager.setToolMode(ToolManager.SEL_HAND);
				}
			}
		});

		addStreamModeRadioButton.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (addStreamModeRadioButton.isSelected()) {
					points.clear();
					addPointsOK = true;
					addLinesOK = true;
					addPolysOK = true;
					addStreamModeRadioButton.setSelected(true);
					shapeLayer.getSelections().clear();

					setMode(FeatureMouseHandler.ADD_FEATURE_STREAM_MODE);
					ToolManager.setToolMode(ToolManager.SEL_HAND);
				}
			}
		});
		
		addPointsOnlyRadioButton.addItemListener( new ItemListener() {
			public void itemStateChanged(ItemEvent event){
				if (addPointsOnlyRadioButton.isSelected()) {
					points.clear();
					addPointsOK=true;
					addLinesOK=false;
					addPolysOK=false;
					setMode( FeatureMouseHandler.ADD_FEATURE_MODE);
					shapeLayer.getSelections().clear();
					ToolManager.setToolMode(ToolManager.SEL_HAND);
				}
			}
		});

		addLinesOnlyRadioButton.addItemListener( new ItemListener() {
			public void itemStateChanged(ItemEvent event){
				if (addLinesOnlyRadioButton.isSelected()) {
					points.clear();
					addPointsOK=false;
					addLinesOK=true;
					addPolysOK=false;
					setMode( FeatureMouseHandler.ADD_FEATURE_MODE);
					shapeLayer.getSelections().clear();
					ToolManager.setToolMode(ToolManager.SEL_HAND);
				}
			}
		});

		addPolygonsOnlyRadioButton.addItemListener( new ItemListener() {
			public void itemStateChanged(ItemEvent event){
				if (addPolygonsOnlyRadioButton.isSelected()) {
					points.clear();
					addPointsOK=false;
					addLinesOK=false;
					addPolysOK=true;
					setMode( FeatureMouseHandler.ADD_FEATURE_MODE);
					shapeLayer.getSelections().clear();
					ToolManager.setToolMode(ToolManager.SEL_HAND);
				}
			}
		});

		
		addCircleRadioButton.addItemListener( new ItemListener() {
			public void itemStateChanged(ItemEvent event){
				if (addCircleRadioButton.isSelected()) {
					//grab the previous mode, in case the user cancels
					// out of the fields dialog
					int prevMode = getMode();
					
					shapeLayer.getSelections().clear();
					if (!isAddCirclesOkay()) {
						List<Field> fields = new ArrayList<Field>();
						for (Field f: shapeLayer.getFeatureCollection().getSchema()) {
							if (Integer.class.isAssignableFrom(f.type) ||
									Float.class.isAssignableFrom(f.type) ||
									Double.class.isAssignableFrom(f.type)) {
								fields.add(f);
							}
						}
						boolean found = false;
						for (Field f: fields) {
							if (f.name.equalsIgnoreCase("radius")) {
								found = true;
								break;
							}
						}
						if (!found) {
							fields.add(0, new Field("Radius", Double.class));
						}
						Object value = JOptionPane.showInputDialog(
							Main.mainFrame,
							"Choose the name of a column to hold radius in kilometers\n" +
								"(For more options, cancel and use 'Edit Circles' in the Feature menu)",
							"Circle Configuration",
							JOptionPane.OK_CANCEL_OPTION,
							null,
							fields.toArray(),
							fields.get(0));
						if (value instanceof Field) {
							
							StyleSource<FPath> source = shapeLayer.getStylesLive().geometry.getSource();
							//if the geom source already exists, edit the radius and units fields
							if(source instanceof GeomSource){
								GeomSource gs = (GeomSource) source;
								gs.setRadiusField((Field)value);
								gs.setUnits(Units.RadiusKm);
							}
							//if it doesn't exist, create a new one with just the circle components
							else{
								Style<FPath> geom = new Style<FPath>(
										shapeLayer.getStylesLive().geometry.getName(),
										new GeomSource((Field)value, Units.RadiusKm, 
											null, null, null, null, null, null, null));
								Set<Style<?>> styles = new HashSet<Style<?>>(1);
								styles.add(geom);
								shapeLayer.applyStyleChanges(styles);
							}
							
							setMode(FeatureMouseHandler.ADD_CIRCLE_MODE);
							ToolManager.setToolMode(ToolManager.SEL_HAND);
						}
						else{
							//reset the mode and the selected radio button
							switch (prevMode){
							case ADD_FEATURE_MODE:
								addModeRadioButton.setSelected(true);
								break;
							case ADD_CIRCLE_MODE:
								addCircleRadioButton.setSelected(true);
								break;
							case ADD_FEATURE_STREAM_MODE:
								addStreamModeRadioButton.setSelected(true);
								break;
							case SELECT_FEATURE_MODE:
								selectModeRadioButton.setSelected(true);
								break;
							}
						}
					}else{
						setMode(FeatureMouseHandler.ADD_CIRCLE_MODE);
						ToolManager.setToolMode(ToolManager.SEL_HAND);
					}
				}
			}
		});
		
		add5PtEllipseRadioButton.addItemListener( new ItemListener() {
			public void itemStateChanged(ItemEvent event){
				if (add5PtEllipseRadioButton.isSelected()) {
					//grab the previous mode, in case the user cancels
					// out of the fields dialog
					int prevMode = getMode();
					
					shapeLayer.getSelections().clear();
					if (!isAddEllipsesOkay()) {
						
						//TODO: get rid of this after editing of ellipses is allowed
						//Show a dialog warning the user ellipses have limited functionality
						JOptionPane.showMessageDialog(Main.mainFrame, 
								"Five point ellipses are currently in Beta form.\n"
								+ "Once an ellipse has been created, it cannot be\n"
								+ "modified.  This means it cannot be moved, and\n"
								+ "its columns cannot be edited in the table.\n",
								"Limited Support for 5-Pt Ellipses", JOptionPane.WARNING_MESSAGE);
						
						
						List<Field> fields = new ArrayList<Field>();
						for (Field f: shapeLayer.getFeatureCollection().getSchema()) {
							if (Integer.class.isAssignableFrom(f.type) ||
									Float.class.isAssignableFrom(f.type) ||
									Double.class.isAssignableFrom(f.type)) {
								fields.add(f);
							}
						}
						boolean aFound = false;
						boolean bFound = false;
						boolean angleFound = false;
						boolean latFound = false;
						boolean lonFound = false;
						Field aField = null;
						Field bField = null;
						Field angField = null;
						Field lonField = null;
						Field latField = null;
						for (Field f: fields) {
							if (f.name.equalsIgnoreCase("a axis")){
								aFound = true;
								aField = f;
							}
							else if(f.name.equalsIgnoreCase("b axis")){
								bFound = true;
								bField = f;
							}
							else if(f.name.equalsIgnoreCase("rotation angle")){
								angleFound = true;
								angField = f;
							}
							else if(f.name.equalsIgnoreCase("center longitude")){
								lonFound = true;
								lonField = f;
							}
							else if(f.name.equalsIgnoreCase("center latitude")){
								latFound = true;
								latField = f;
							}
							//if all the columns are found, no need to look at the
							// rest of the columns. Break the loop.
							if(aFound && bFound && angleFound && latFound && lonFound){
								break;
							}
						}
						
						if (!aFound) {
							aField = new Field("A Axis", Double.class);
							fields.add(0, aField);
						}
						if (!bFound) {
							bField = new Field("B Axis", Double.class);
							fields.add(0, bField);
						}
						if (!angleFound) {
							angField = new Field("Rotation Angle", Double.class);
							fields.add(0, angField);
						}
						if(!lonFound) {
							lonField = new Field("Longitude", Double.class);
							fields.add(0, lonField);
						}
						if(!latFound) {
							latField = new Field("Latitude", Double.class);
							fields.add(0, latField);
						}
						//create 3 labels and 3 dropboxes, put them on a panel and 
						// pass that to the option pane
						JLabel infoLbl = new JLabel("Choose the columns to hold the a and b axes (km),");
						JLabel info2Lbl = new JLabel("and rotation angle (deg).  To change these values later, use");
						JLabel info3Lbl = new JLabel("the 'Edit Ellipses...' option in the Feature menu.");
						JLabel aLbl = new JLabel("A Axis Field:");
						JLabel bLbl = new JLabel("B Axis Field:");
						JLabel angLbl = new JLabel("Rotation Angle Field:");
						JLabel lonLbl = new JLabel("Center Longitude Field:");
						JLabel latLbl = new JLabel("Center Latitude Field:");
						Vector<Field> fieldVec = new Vector<Field>(fields);
						JComboBox<Field> aBx = new JComboBox<Field>(fieldVec);
						aBx.setSelectedItem(aField);
						JComboBox<Field> bBx = new JComboBox<Field>(fieldVec);
						bBx.setSelectedItem(bField);
						JComboBox<Field> angBx = new JComboBox<Field>(fieldVec);
						angBx.setSelectedItem(angField);
						JComboBox<Field> latBx = new JComboBox<Field>(fieldVec);
						latBx.setSelectedItem(latField);
						JComboBox<Field> lonBx = new JComboBox<Field>(fieldVec);
						lonBx.setSelectedItem(lonField);
						JPanel optionPnl = new JPanel(new GridBagLayout());
						int pad = 1;
						Insets in = new Insets(pad,pad,pad,pad);
						int row = 0;
						optionPnl.add(infoLbl, new GridBagConstraints(0, row, 2, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
						optionPnl.add(info2Lbl, new GridBagConstraints(0, ++row, 2, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
						optionPnl.add(info3Lbl, new GridBagConstraints(0, ++row, 2, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
						optionPnl.add(Box.createVerticalStrut(8), new GridBagConstraints(0, ++row, 2, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
						optionPnl.add(aLbl, new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
						optionPnl.add(aBx, new GridBagConstraints(1, row, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
						optionPnl.add(bLbl, new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
						optionPnl.add(bBx, new GridBagConstraints(1, row, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
						optionPnl.add(angLbl, new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
						optionPnl.add(angBx, new GridBagConstraints(1, row, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
						optionPnl.add(lonLbl, new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
						optionPnl.add(lonBx, new GridBagConstraints(1, row, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
						optionPnl.add(latLbl, new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
						optionPnl.add(latBx, new GridBagConstraints(1, row, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));

						
						int val = JOptionPane.showConfirmDialog(Main.mainFrame, 
								optionPnl, "Ellipse Configuration", 
								JOptionPane.OK_CANCEL_OPTION);
						
						if(val == JOptionPane.OK_OPTION){
							
							StyleSource<FPath> source = shapeLayer.getStylesLive().geometry.getSource();
							
							//TODO when creating the GeomSource, set the ellipse fields
							// not editable for now. Come back and implement ellipse 
							// recalculation to allow for these fields to be edited in
							// the future.
							//Get the fields
							Field selAField = (Field) aBx.getSelectedItem();
							selAField.setEditable(false);
							Field selBField = (Field) bBx.getSelectedItem();
							selBField.setEditable(false);
							Field selAngField = (Field) angBx.getSelectedItem();
							selAngField.setEditable(false);
							Field selLatField = (Field) latBx.getSelectedItem();
							selLatField.setEditable(false);
							Field selLonField = (Field) lonBx.getSelectedItem();
							selLonField.setEditable(false);
							
							//if the geom source already exists, edit the ellipse fields
							if(source instanceof GeomSource){
								GeomSource gs = (GeomSource) source;
								gs.setAAxisField(selAField);
								gs.setBAxisField(selBField);
								gs.setAngleField(selAngField);
								gs.setLengthUnits(LengthUnits.AxesKm);
								gs.setAngleUnits(AngleUnits.Radians);
								gs.setCenterLatField(selLatField);
								gs.setCenterLonField(selLonField);
							}
							//otherwise, create a new one with the ellipse fields set
							else{
								Style<FPath> geom = new Style<FPath>(
									shapeLayer.getStylesLive().geometry.getName(),
									new GeomSource(null, null, selAField, selBField, selAngField,
									LengthUnits.AxesKm, AngleUnits.Radians, selLatField, selLonField));
								Set<Style<?>> styles = new HashSet<Style<?>>(1);
								styles.add(geom);
								shapeLayer.applyStyleChanges(styles);
							}
							//only change the mode if the user selects fields for the 
							// required values
							points.clear();
							setMode(FeatureMouseHandler.ADD_FIVE_PT_ELLIPSE_MODE);
							ToolManager.setToolMode(ToolManager.SEL_HAND);
						}else{
							//reset the mode and the selected radio button
							switch (prevMode){
							case ADD_FEATURE_MODE:
								addModeRadioButton.setSelected(true);
								break;
							case ADD_CIRCLE_MODE:
								addCircleRadioButton.setSelected(true);
								break;
							case ADD_FEATURE_STREAM_MODE:
								addStreamModeRadioButton.setSelected(true);
								break;
							case SELECT_FEATURE_MODE:
								selectModeRadioButton.setSelected(true);
								break;
							}
						}
					}else{
						points.clear();
						setMode(FeatureMouseHandler.ADD_FIVE_PT_ELLIPSE_MODE);
						ToolManager.setToolMode(ToolManager.SEL_HAND);
					}
				}
			}
		});
		
		
		
		selectModeRadioButton.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent event) {
				if (selectModeRadioButton.isSelected()) {
					setMode(FeatureMouseHandler.SELECT_FEATURE_MODE);
					// if we are changing to select mode, we need to get rid
					// of any selection line that is being drawn.
					initializeSelectionLine();
				}
				ToolManager.setToolMode(ToolManager.SEL_HAND);
			}
		});
			
		deleteRowMenuItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e){
				if (shapeLayer.getHistory() != null)
					shapeLayer.getHistory().mark();
				// if this action is legal for the dataset, it will cascade into
				// removing the features from the selections set as well
				shapeLayer.getFeatureCollection().removeFeatures(shapeLayer.getSelections());
			}
		});
		
		deletePointMenuItem.addActionListener( new ActionListener(){
			public void actionPerformed(ActionEvent e){
				if (rect==null){
					return;
				}
				MemoryFeatureIndex idx = shapeLayer.getIndex();
				for (Feature feature: shapeLayer.getSelections()) {
					FPath path = idx.getWorldPath(feature);
					if (path.intersects(rect)) {
						Point2D vertex = getIntersectingVertex( feature,rect);
						if (vertex!=null){
							if (canDeleteVertex( feature)) {
								deleteVertex( feature, vertex);
							} else {
								String message = "Cannot delete point. \n";
								switch (path.getType()) {
								case FPath.TYPE_POLYGON:
									message += "Polygons must have at least three vertices.";
									break;
								case FPath.TYPE_POLYLINE:
									message += "Polylines must have at least two vertices.";
									break;
								case FPath.TYPE_POINT:
									message += "Points must consist of one vertex";
									break;
								}
								JOptionPane.showMessageDialog(LManager.getDisplayFrame(), message);
							}
						}
					}
				}
			}
		});

		addPointMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (rect == null || worldPt == null)
					return;
				MemoryFeatureIndex idx = shapeLayer.getIndex();
				for (Feature feature: shapeLayer.getSelections()) {
					FPath path = idx.getWorldPath(feature);
					if (path.getType() != FPath.TYPE_POINT &&
							path.intersects(rect)) {
						addVertex(feature, worldPt, rect);
						break;
					}
				}
			}
		});

	    Set<String> layerTypes=StampFactory.getLayerTypes();
	    	    
	    for(final String type : layerTypes) {
			JMenuItem findOverlaps= new JMenuItem("Find intersecting " + type + " stamps");
			findOverlaps.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					ArrayList<GeneralPath> allStampPaths = new ArrayList<GeneralPath>();
					ArrayList<String> allStampIds = new ArrayList<String>();
					
					// When calculating intersections, do so according to how the shape is being presented to the user, not how it is represented internally.
					// Example: Points that are displayed to the user as Circles should intersect a much larger area than non-circular Points.
					int size=shapeLayer.getSelections().size();
					ShapeRenderer sr = lview.createRenderer(true, size);
					
					int cnt=1;
					for (Feature f : shapeLayer.getSelections()) {
						allStampPaths.add(new GeneralPath(sr.getPath(f).convertTo(FPath.WORLD).getShape()));
						Field label = new Field("Label", String.class);
						Object attr = f.getAttribute(label);
						allStampIds.add(attr!=null?(String)attr:"Unnamed Shape "+ cnt++);
					}

					StampFactory.createOverlappingStampLayer(type, allStampIds, allStampPaths);
				}
			});		 
			findStampsMenu.add(findOverlaps);
	    }		
	    
	    //create the polygon functions menu items
	    intersectMenuItem.addActionListener(new ActionListener(){
		    public void actionPerformed(ActionEvent e) {
                Area startingArea = new Area();
                ObservableSet<Feature> selections = shapeLayer.getSelections();
                        
                Feature[] selectedArr = selections.toArray(new Feature[]{});
                
                //need to initially go through each selected shape and see if we need to run them all through the adjustment for crossing the PM
                double minCoord = Double.MAX_VALUE;
                double maxCoord = Double.MIN_VALUE;
                double[] minMaxVals = new double[]{minCoord,maxCoord};
                boolean crossesPM = false;
                for (Feature feature : selectedArr) {
                    if (!(feature.getPath().getType() == FPath.TYPE_POLYGON)) {
                        JOptionPane.showMessageDialog(
                                Main.mainFrame,
                                "This function is only available for polygons.",
                                "Intersect results",
                                JOptionPane.PLAIN_MESSAGE);
                        return;
                    }
                    FPath fp = feature.getPath();
                    fp = fp.convertTo(FPath.WORLD);
                    double[] coords = fp.getCoords(false);

                    getMinMaxCoord(minMaxVals, coords, false);
                    
                    //now that we have the min and max coordinates for this shape, see if the difference is greater than 180
                    double difference = minMaxVals[1] - minMaxVals[0];
                    if (Double.compare(difference, 180) > 0) {
                        crossesPM = true;
                        break;
                    }
                }
                boolean firstTime = true;
                for (Feature feature : selectedArr) {
                    
                    //add 360 to all coordinates to avoid prime meridian issues
                    FPath fp = feature.getPath();
                    fp = fp.convertTo(FPath.WORLD);
                    double[] coords = fp.getCoords(false);
                    double[] newCoords = coords;
                    if (crossesPM) {
                        //our ending shape will cross the PM, so we need to adjust all values
                        newCoords = adjustArrayForPM(coords, false);
                    }
                    GeneralPath newPath = coordsToGeneneralPath(newCoords, feature.getPath().getClosed(), false);
                    if (firstTime) {
                        //first time through, we don't have anything in the startingArea, set it to the first shape and go from there
                        
                        startingArea.add(new Area(newPath));
                        firstTime = false;
                    }
                    startingArea.intersect(new Area(newPath));
                }
                
                FPath resultFP = new FPath (startingArea, FPath.WORLD);
                resultFP = resultFP.convertTo(FPath.SPATIAL_WEST);
                if (resultFP.getPathCount() > 0) {//avoid ArrayIndexOutOfBoundsException
                    int cnt = resultFP.getPathCount();
                    for (int i=0; i<cnt; i++) {
                        Feature feature = new Feature();
                        FPath p = new FPath(resultFP.getVertices(i), FPath.SPATIAL_WEST, true);
                        feature.setPath (p);
                        String typeString = FeatureUtil.getFeatureTypeString(resultFP.getType());
                        feature.setAttribute( Field.FIELD_FEATURE_TYPE, typeString);
                        addFeature(feature);
                    }
                } else {
                    JOptionPane.showMessageDialog(
                            Main.mainFrame,
                            selections.size() + " shapes were selected with no intersection returned.",
                            "Intersect results",
                            JOptionPane.PLAIN_MESSAGE);
                }
                        
            }
		});	
	    
	    subtractMenuItem.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                Area intersectArea = new Area();
                ObservableSet<Feature> selections = shapeLayer.getSelections();
     
                Feature[] selectedArr = selections.toArray(new Feature[]{});
	            Feature first = selectedArr[0];
	            if (!(selectedArr[0].getPath().getType() == FPath.TYPE_POLYGON) || !(selectedArr[1].getPath().getType() == FPath.TYPE_POLYGON)) {
	                JOptionPane.showMessageDialog(
	                        Main.mainFrame,
	                        "This function is only available for polygons.",
	                        "Intersect results",
	                        JOptionPane.PLAIN_MESSAGE);
	                return;
	            }
	            //add 360 to all coordinates to avoid prime meridian issues
	            FPath fp = first.getPath();
	            fp = fp.convertTo(FPath.WORLD);//convert to world before doing any coordinate manipulation
	            double[] coords = fp.getCoords(false);
	            double[] newCoords = coords;
	            double minCoord = Double.MAX_VALUE;
	            double maxCoord = Double.MIN_VALUE;
	            double[] minMaxCoords = new double[]{minCoord,maxCoord};
	            getMinMaxCoord(minMaxCoords, coords, false);
	            
	            //now that we have the min and max coordinates for this shape, see if the difference is greater than 180
	            double difference = minMaxCoords[1] - minMaxCoords[0];
	            if (Double.compare(difference, 180) > 0) {
	                //it is greater than 180, we need to add 360 to any value less than 180
	                newCoords = adjustArrayForPM(coords, false);
	            }
	            
	            GeneralPath firstNewPath = coordsToGeneneralPath(newCoords, first.getPath().getClosed(), false);
	            Area firstArea = new Area(firstNewPath);
	            
	            //add 360 to all coordinates to avoid prime meridian issues
	            Feature second = selectedArr[1];
	            FPath secondFP = second.getPath();
	            secondFP = secondFP.convertTo(FPath.WORLD);
	            double[] secondCoords = secondFP.getCoords(false);
	            double[] secondNewCoords = secondCoords;
	            minMaxCoords[0] = Double.MAX_VALUE;
	            minMaxCoords[1] = Double.MIN_VALUE;
	            getMinMaxCoord(minMaxCoords, secondCoords, false);
	                
	            //now that we have the min and max coordinates for this shape, see if the difference is greater than 180
	            difference = minMaxCoords[1] - minMaxCoords[0];
	            if (Double.compare(difference, 180) > 0) {
	                //it is greater than 180, we need to add 360 to any value less than 180
	                secondNewCoords = adjustArrayForPM(secondCoords, false);
	            }
	            GeneralPath secondNewPath = coordsToGeneneralPath(secondNewCoords, second.getPath().getClosed(), false);
	            //intersect first and second
	            Area secondArea = new Area(secondNewPath);
	            intersectArea.add(firstArea);
	            intersectArea.intersect(secondArea);
	            
	            if (intersectArea.isEmpty()) {
	                JOptionPane.showMessageDialog(
	                        Main.mainFrame,
	                        "No intersection of selected shapes was returned. No new shapes will be created.",
	                        "Subtract results",
	                        JOptionPane.PLAIN_MESSAGE);
	            } else if (intersectArea.equals(firstArea) || intersectArea.equals(secondArea)) {
	                JOptionPane.showMessageDialog(
	                        Main.mainFrame,
	                        "It appears that one shape is fully contained within the other shape. JMARS is not currently able to\n "
	                        + "create a shape with an inner polygon subtracted. No new shapes will be created.",
	                        "Subtract results",
	                        JOptionPane.PLAIN_MESSAGE);
	            } else {
	                firstArea.subtract(intersectArea);
	                secondArea.subtract(intersectArea);
	                
	                if (!firstArea.isEmpty()) {
	                    FPath firstResultPath = new FPath (firstArea, FPath.WORLD);
	                    firstResultPath = firstResultPath.convertTo(FPath.SPATIAL_WEST);
	                    int cnt = firstResultPath.getPathCount();
	                    for (int i=0; i<cnt; i++) {
	                        FPath p = new FPath(firstResultPath.getVertices(i),FPath.SPATIAL_WEST, first.getPath().getClosed());
	                        Feature feature = new Feature();
	                        feature.setPath (p);
	                        String typeString = FeatureUtil.getFeatureTypeString(firstResultPath.getType());
	                        feature.setAttribute( Field.FIELD_FEATURE_TYPE, typeString);
	                        addFeature(feature);
	                    }
	                }
	                if (!secondArea.isEmpty()) {
	                    FPath secondResultPath = new FPath(secondArea,FPath.WORLD);
	                    secondResultPath = secondResultPath.convertTo(FPath.SPATIAL_WEST);
	                    int cnt = secondResultPath.getPathCount();
	                    for (int i=0; i<cnt; i++) {
	                        FPath p = new FPath(secondResultPath.getVertices(i),FPath.SPATIAL_WEST,second.getPath().getClosed());
	                        Feature feature = new Feature();
	                        feature.setPath (p);
	                        String typeString = FeatureUtil.getFeatureTypeString(secondResultPath.getType());
	                        feature.setAttribute( Field.FIELD_FEATURE_TYPE, typeString);
	                        addFeature(feature);
	                    }
	                }
	            }
            }
        });
	    
	    mergeMenuItem.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                Area startingArea = new Area();
                ObservableSet<Feature> selections = shapeLayer.getSelections();
        
                Feature[] selectedArr = selections.toArray(new Feature[]{});
                
                //need to initially go through each selected shape and see if we need to run them all through the adjustment for crossing the PM
                double minCoord = Double.MAX_VALUE;
                double maxCoord = Double.MIN_VALUE;
                double[] minMaxCoords = new double[]{minCoord,maxCoord};
                boolean crossesPM = false;
                for (Feature feature : selectedArr) {
                    if (!(feature.getPath().getType() == FPath.TYPE_POLYGON)) {
                        JOptionPane.showMessageDialog(
                                Main.mainFrame,
                                "This function is only available for polygons.",
                                "Intersect results",
                                JOptionPane.PLAIN_MESSAGE);
                        return;
                    }
                    FPath fp = feature.getPath();
                    fp = fp.convertTo(FPath.WORLD);
                    double[] coords = fp.getCoords(false);

                    getMinMaxCoord(minMaxCoords, coords, false);
                    
                    //now that we have the min and max coordinates for this shape, see if the difference is greater than 180
                    double difference = minMaxCoords[1] - minMaxCoords[0];
                    if (Double.compare(difference, 180) > 0) {
                        crossesPM = true;
                        break;
                    }
                }
                Area intersectTest = new Area();
                boolean first = true;
                for (Feature feature : selectedArr) {
                    //add 360 to all coordinates to avoid prime meridian issues
                    FPath fp = feature.getPath();
                    fp = fp.convertTo(FPath.WORLD);
                    double[] coords = fp.getCoords(false);
                    double[] newCoords = coords;
                    if (crossesPM) {
                        //our ending shape will cross the PM, so we need to adjust all values
                        newCoords = adjustArrayForPM(coords, false);
                    }
                    GeneralPath newPath = coordsToGeneneralPath(newCoords, fp.getClosed(), false);
                    Area area = new Area(newPath);
                    startingArea.add(area);
                    if (first) {
                        intersectTest.add(area);
                        first = false;
                    }
                    intersectTest.intersect(area);
                }
                
                if (intersectTest.isEmpty()) {
                    //this means that we did not increase the coverage area by this merge. We should not create a new shape.
                    JOptionPane.showMessageDialog(
                            Main.mainFrame,
                            "There was no overlapping area for the selected polygons. No new polygon will be created.",
                            "Intersect results",
                            JOptionPane.PLAIN_MESSAGE);
                } else {
                    FPath resultFP = new FPath (startingArea, FPath.WORLD);
                    resultFP = resultFP.convertTo(FPath.SPATIAL_WEST);
                    if (resultFP.getPathCount() > 0) {//avoid ArrayIndexOutOfBoundsException
                        int cnt = resultFP.getPathCount();
                        for (int i=0; i<cnt; i++) {
                            Feature feature = new Feature();
                            FPath p = new FPath(resultFP.getVertices(i), FPath.SPATIAL_WEST, true);
                            feature.setPath (p);
                            String typeString = FeatureUtil.getFeatureTypeString(resultFP.getType());
                            feature.setAttribute( Field.FIELD_FEATURE_TYPE, typeString);
                            addFeature(feature);
                        }
                    }
                }
            }
          
        });
	    
	    duplicateMenuItem.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                ObservableSet<Feature> selections = shapeLayer.getSelections();
                Feature[] selectedArr = selections.toArray(new Feature[]{});
                for (Feature feature : selectedArr) {
                    if (!(feature.getPath().getType() == FPath.TYPE_POLYGON)) {
                        JOptionPane.showMessageDialog(
                                Main.mainFrame,
                                "This function is only available for polygons.",
                                "Intersect results",
                                JOptionPane.PLAIN_MESSAGE);
                        return;
                    }
                    FPath fp = feature.getPath();
                    if (fp.getPathCount() > 0) {//avoid ArrayIndexOutOfBoundsException
                        int cnt = fp.getPathCount();
                        for (int i=0; i<cnt; i++) {
                            Feature f = new Feature();
                            FPath p = new FPath(fp.getVertices(i), FPath.SPATIAL_WEST, fp.getClosed());
                            f.setPath (p);
                            String typeString = FeatureUtil.getFeatureTypeString(fp.getType());
                            f.setAttribute( Field.FIELD_FEATURE_TYPE, typeString);
                            addFeature(f);
                        }
                    }
                }
            }
        }); 
	    
        pixelExportMenuItem.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                ObservableSet<Feature> selections = shapeLayer.getSelections();
                //grab the first selection, since this option will only
                // be enabled when only one shape is selected
                path = ((Feature)selections.toArray()[0]).getPath();
        		new PixelExportDialog(Main.mainFrame, shapeFunctionsMenu, path).setVisible(true);
            }
        });
        
        //add items to the shape functions menu
        shapeFunctionsMenu.add(intersectMenuItem);
        shapeFunctionsMenu.add(subtractMenuItem);
        shapeFunctionsMenu.add(mergeMenuItem);
        shapeFunctionsMenu.add(duplicateMenuItem);
        shapeFunctionsMenu.add(pixelExportMenuItem);
	    
	} // end: private void setupContextMenu()
} // end: class FeatureMouseHandler
