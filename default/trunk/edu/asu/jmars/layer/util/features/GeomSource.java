package edu.asu.jmars.layer.util.features;

import java.awt.Component;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import edu.asu.jmars.Main;
import edu.asu.jmars.ProjObj;
import edu.asu.jmars.ProjObj.Projection_OC;
import edu.asu.jmars.util.HVector;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.util.ellipse.geometry.Ellipse;

/**
 * Converts the given path into a circular outer path if the path was a point
 * a means of determining the kilometer radius has been set.
 */
public class GeomSource implements StyleSource<FPath> {
	private static final long serialVersionUID = 1L;
	
	//Circle variables
	/** The source of radius info, or null if circles should not be expanded. */
	private Field radiusField;
	/** The interpretation of the radius field value */
	private Units units;
	/** Creates new uninitialized style source */
	
	//Ellipse variables
	/** The source of a (is x-axis when roation is 0) axis. */
	private Field aAxisField;
	/** The source of b (is y-axis when roation is 0) axis. */
	private Field bAxisField;
	/** The source of angle. */
	private Field angleField;
	/** The latitude of the center point in degrees N */
	private Field centerLatField;
	/** The longitude of the center point in degrees E */
	private Field centerLonField;
	/** The interpretation of the axes field values */
	private LengthUnits lengthUnits;
	/** The interpretation of the angle field value */
	private AngleUnits angleUnits;
	
	/** Number of vertices used to define an ellipse,
	 * it is important to keep this number a multiple 
	 * of four, so there will always be a point at the
	 * perpendicular intersection of both axes from 
	 * the center point.
	 */
	private final static int vertices = 36;
	
	
	public GeomSource(Field radiusField, Units units, Field aAxisField, Field bAxisField, Field angleField, LengthUnits lUnits, AngleUnits aUnits, Field cenLat, Field cenLon){
		this.radiusField = radiusField;
		this.units = units;
		this.aAxisField = aAxisField;
		this.bAxisField = bAxisField;
		this.angleField = angleField;
		this.lengthUnits = lUnits;
		this.angleUnits = aUnits;
		centerLatField = cenLat;
		centerLonField = cenLon;
	}
	
	
	public void setRadiusField(Field radiusField) {
		this.radiusField = radiusField;
	}

	public void setUnits(Units units) {
		this.units = units;
	}

	public void setAAxisField(Field aAxisField) {
		this.aAxisField = aAxisField;
	}

	public void setBAxisField(Field bAxisField) {
		this.bAxisField = bAxisField;
	}

	public void setAngleField(Field angleField) {
		this.angleField = angleField;
	}

	public void setCenterLatField(Field centerLatField) {
		this.centerLatField = centerLatField;
	}

	public void setCenterLonField(Field centerLonField) {
		this.centerLonField = centerLonField;
	}

	public void setLengthUnits(LengthUnits lengthUnits) {
		this.lengthUnits = lengthUnits;
	}

	public void setAngleUnits(AngleUnits angleUnits) {
		this.angleUnits = angleUnits;
	}
	
	/** @return the field the user chose to store radius values. */
	public Field getRadiusField() {
		return radiusField;
	}
	
	/** @return the units of the field the user chose to store radius values. */
	public Units getUnits() {
		return units;
	}
	
	/** @return the field the user chose to store a axis values. */
	public Field getAAxisField() {
		return aAxisField;
	}
	
	/** @return the field the user chose to store b axis values. */
	public Field getBAxisField() {
		return bAxisField;
	}
	
	/** @return the field the user chose to store angle values. */
	public Field getAngleField() {
		return angleField;
	}
	
	/** @return the units of the field the user chose to store axes values. */
	public LengthUnits getAxesUnits() {
		return lengthUnits;
	}
	
	/** @return the units of the field the user chose to store angle value. */
	public AngleUnits getAngleUnits() {
		return angleUnits;
	}
	
	/** @return the field the user chose to store the center latitude */
	public Field getLatField(){
		return centerLatField;
	}
	
	/** @return the field the user chose to store the center longitude */
	public Field getLonField(){
		return centerLonField;
	}
	
	
	/**
	 * @returns either Field.FIELD_PATH if no radius field has been defined, or
	 *          Field.FIELD_PATH, the radius field, A axis field, B axis field,
	 *          angle field, center longitude field, and center latitude field 
	 *          chosen by the user.
	 */
	public Set<Field> getFields() {
		//build the set of fields that have already been set
		HashSet<Field> set = new HashSet<Field>();
		set.add(Field.FIELD_PATH);
		if(radiusField != null){
			set.add(radiusField);
		}
		if(aAxisField != null){
			set.add(aAxisField);
		}
		if(bAxisField != null){
			set.add(bAxisField);
		}
		if(angleField != null){
			set.add(angleField);
		}
		if(centerLonField != null){
			set.add(centerLonField);
		}
		if(centerLatField != null){
			set.add(centerLatField);
		}
		
		return set;
	}
	
	/**
	 * @returns either Field.FIELD_PATH if no radius field has been defined, or
	 *          Field.FIELD_PATH and the radius field the user chose.
	 */
	public Set<Field> getCircleFields() {
		return radiusField == null ?
			Collections.singleton(Field.FIELD_PATH) :
			new HashSet<Field>(Arrays.asList(Field.FIELD_PATH, radiusField));
	}
	
	/**
	 * @returns either Field.FIELD_PATH if no radius field has been defined, or
	 *          Field.FIELD_PATH and the a axis, b axis, angle, center lat, center 
	 *          lon fields the user chose.
	 */
	public Set<Field> getEllipseFields() {
		if(aAxisField == null || bAxisField == null || angleField == null){
			return Collections.singleton(Field.FIELD_PATH);
		}else{
			return new HashSet<Field>(Arrays.asList(Field.FIELD_PATH, aAxisField, bAxisField, angleField, centerLonField, centerLatField));
		}
	}
	
	
	public FPath getValue(Feature f) {
		if (f == null) {
			return null;
		}
		FPath path = f.getPath();
		
		//circles
		if (path.getType() == FPath.TYPE_POINT && f.getAttribute(radiusField) != null) {
			Number radius = (Number)f.attributes.get(radiusField);
			if (radius != null) {
				double dblRadius = radius.doubleValue();
				if (dblRadius != 0) {
					path = getCirclePath(path, dblRadius * units.getScale(), 36);
				}
			}
		}
		
		//ellipses
		else if(FeatureUtil.isEllipseSource(this, f)){
			//if the coords length is two that means it's the center point,
			// not the entire spatial path, so calculate the path (like loading
			// from a shape file)

//			Double cenLon;
//			Double cenLat;
//			
//			if(path.getCoords(false).length == 2){
//				System.out.println("calculating ellipse path");
//				//get the center point off the path
//				cenLon = path.getCoords(false)[0];
//				cenLat = path.getCoords(false)[1];
//			}else{
//				cenLon = (Double)f.getAttribute(centerLonField);
//				cenLat = (Double)f.getAttribute(centerLatField);
//			}
//			
//			Double aAxis = (Double)f.attributes.get(aAxisField);
//			Double bAxis = (Double)f.attributes.get(bAxisField);
//			Double angle = (Double)f.attributes.get(angleField);
//			
//			if(cenLat!=null && cenLon!=null && aAxis!=null && bAxis!=null && angle!=null){
//				//pass in degrees W for calculation
//				path = getEllipticalPath(new Point2D.Double(360-cenLon, cenLat), scaleToKm(aAxis), scaleToKm(bAxis), angle*angleUnits.getScale());
//				//set the proper path, and populate the lat and lon fields for the ellipse
//				f.setAttributeQuiet(Field.FIELD_PATH, path);
//				f.setAttributeQuiet(centerLatField, cenLat);
//				//displaying degrees E so leave the same
//				f.setAttributeQuiet(centerLonField, cenLon);
//			}
		
			//Define the ellipse used for this feature
			double cenLon = (double)f.getAttribute(centerLonField);
			double cenLat = (double)f.getAttribute(centerLatField);
			double aLength = (double)f.getAttribute(aAxisField);
			double bLength = (double)f.getAttribute(bAxisField);
			double rotAngle = (double)f.getAttribute(angleField);
			Ellipse e = new Ellipse(cenLon, cenLat, aLength, bLength, rotAngle);
			
			//if nothing has changed just return the path without changing the feature
			//otherwise, recalculate the feature and attributes
			if(hasEllipseChanged(e, path)){
//				path = getEllipticalPath(e, path);
//				f.setAttributeQuiet(Field.FIELD_PATH, path);
				//TODO: implement this when editing ellipses works
//				updateEllipseFeature(f, e);
//				path = f.getPath();
			}
		}
		
		
		return path;
	}
	/**
	 * Given an FPath, returns a circular polygon with the given number of
	 * vertices the given kilometer distance from the center of the given path.
	 */
	public static FPath getCirclePath(FPath path, double kmRadius, int vertexCount) {
		// get center of the feature in special west coordinates
		HVector center = new HVector(path.getSpatialWest().getCenter()).unit();
		// get the point of intersection between the ellipsoid and a ray from the center of mass toward the center of the feature
		HVector hit = HVector.intersectMars(HVector.ORIGIN, center);
		// convert radius from kilometers to radians by dividing out the magnitude in kilometers of the ellipsoid hit
		// a point 'radius' radians away from 'center'
		HVector point = center.rotate(
			center.cross(HVector.Z_AXIS).unit(),
			kmRadius / hit.norm()).unit();
		// rotate 'point' around 'center' once per vertex
		float[] coords = new float[vertexCount*2];
		double toRad = Math.toRadians(360/(vertexCount*1.0));
		for (int i = 0; i < vertexCount; i++) {
			double omega = i*toRad;
			HVector vertex = point.rotate(center, omega);
			coords[2*i] = (float)vertex.lonW();
			coords[2*i+1] = (float)vertex.latC();
		}
		return new FPath(coords, false, FPath.SPATIAL_WEST, true);
	}
	
	/**
	 * Take a spatial ellipse and the FPath with spatial points defining
	 * that Ellipse.  Check to see if they match, if they don't return false;
	 * @param e Spatial ellipse
	 * @param path Spatial path for the ellipse
	 * @return True if they are the same
	 */
	private boolean hasEllipseChanged(Ellipse e, FPath path){
		//ellipse center point is in degrees E, convert to deg West
		Point2D ellipseSpCen = new Point2D.Double((360-e.getCenterLon()), e.getCenterLat());
		
		//convert points to world based on a projection at the center and compare centers
		ProjObj po = new Projection_OC(ellipseSpCen.getX(), ellipseSpCen.getY());
		double cenX = 0;
		double cenY = 0;
		//the first and last point are the same, so only include one
		for(int i = 0; i<vertices; i++){
			Point2D wdPt = po.convSpatialToWorld(path.getVertices()[i]);
			cenX += wdPt.getX();
			cenY += wdPt.getY();
		}
		cenX = cenX/(vertices);
		cenY = cenY/(vertices);
		Point2D pathWdCen = new Point2D.Double(cenX, cenY);
		Point2D pathSpCen = po.convWorldToSpatial(pathWdCen);
		
		//compare the centers, if they're not close, the ellipse has changed
		if(Point2D.Double.distance(pathSpCen.getX(), pathSpCen.getY(), ellipseSpCen.getX(), ellipseSpCen.getY()) > 0.001){
//			System.out.println("center has changed");
			return true;
		}
		
		
		//Need to get the A and B points (where the axes intersect perpendicularly from the center on the path)
		Point2D aSpPt = path.getVertices()[0];
		Point2D bSpPt = path.getVertices()[vertices/4]; //the b point is a quarter around the ellipse
		
		//compare the a-axis (if it's changed more than 0.1% the length, it's significant)
		double ellipseA = e.getALength();
		double pathA = Util.angularAndLinearDistanceS(pathSpCen, aSpPt, Main.testDriver.mainWindow.getProj())[1] * 2;
		if(Math.abs(ellipseA-pathA) > ellipseA*0.001){
//			System.out.println("a axis changed");
			return true;
		}
		
		//compare the b-axis (if it's changed more than 0.1% the length, it's significant)
		double ellipseB = e.getBLength();
		double pathB = Util.angularAndLinearDistanceS(pathSpCen, bSpPt, Main.testDriver.mainWindow.getProj())[1] * 2;
		if(Math.abs(ellipseB-pathB) > ellipseB*.001){
//			System.out.println("b axis changed");
			return true;
		}
		
		//compare the angle
		double ellipseAngle = e.getRotationAngle();
		Point2D shiftedCenSpPt = new Point2D.Double(pathSpCen.getX(), 0);
		Point2D shiftedASpPt = new Point2D.Double(aSpPt.getX(), Math.abs(pathSpCen.getY()-aSpPt.getY()));
		Point2D shiftedAXSpPt = new Point2D.Double(aSpPt.getX(), 0);
		double hypDist = Util.angularAndLinearDistanceS(shiftedCenSpPt, shiftedASpPt, Main.testDriver.mainWindow.getProj())[1];
		double adjDist = Util.angularAndLinearDistanceS(shiftedCenSpPt, shiftedAXSpPt, Main.testDriver.mainWindow.getProj())[1];
		double pathAngle = Math.acos(adjDist/hypDist);
		if(Math.abs(ellipseAngle-pathAngle)>0.001){
//			System.out.println("angle changed");
			return true;
		}
		
		
		return false;
	}
	
	private void updateEllipseFeature(Feature f, Ellipse e){
		FPath oldPath = f.getPath();
		//get the center of the ellipse (convert from deg E to west)
		Point2D cenSpPt = new Point2D.Double((360-e.getCenterLon()), e.getCenterLat());
		//get the old A and B points
		Point2D aSpPt = oldPath.getVertices()[0];
		Point2D bSpPt = oldPath.getVertices()[vertices/4]; //the b point is a quarter around the ellipse
		
//		System.out.println("a Org, "+(360-aSpPt.getX())+", "+aSpPt.getY());
//		System.out.println("b Org, "+(360-bSpPt.getX())+", "+bSpPt.getY());
		
	
		//Use vectors to find proper points
		HVector start = new HVector(cenSpPt);
		//A direction
		HVector horDir = new HVector(aSpPt);
		HVector horAxis = start.cross(horDir);
		HVector horEnd = start.rotate(horAxis, (e.getALength()/2)/Util.MEAN_RADIUS);
		
		//B Direction
		HVector verDir = new HVector(bSpPt);
		HVector verAxis = start.cross(verDir);
		HVector verEnd = start.rotate(verAxis, (e.getBLength()/2)/Util.MEAN_RADIUS);
		
//		System.out.println("a new, "+horEnd.lonE()+", "+horEnd.lat());
//		System.out.println("b new, "+verEnd.lonE()+", "+verEnd.lat());
		
		
		//create world ellipse with proj centered on center of ellipse
		ProjObj po = new Projection_OC(cenSpPt.getX(), cenSpPt.getY());
		//get world coords to work with
		Point2D cenWdPt = po.convSpatialToWorld(start.lon(), start.lat());
		Point2D aWdPtEnd = po.convSpatialToWorld(horEnd.lon(), horEnd.lat());
		Point2D bWdPtEnd = po.convSpatialToWorld(verEnd.lon(), verEnd.lat());
					
		//check to make sure the a or b x values are 
		// within 180 degrees of the center point (they should never
		// be greater than 180 because that means the ellipse would
		// be greater than 360 degrees wide)
		double cenWdX = cenWdPt.getX();
		double aWdX = aWdPtEnd.getX();
		double bWdX = bWdPtEnd.getX();
		//Check A axis
		if(Math.abs(cenWdX - aWdX) > 180){
			//if the center point is greater, than add 360 to A
			if(cenWdX > aWdX){
				aWdX = aWdX + 360;
			}
			//otherwise subtract 360 from A
			else{
				aWdX = aWdX - 360;
			}
		}
		//Check B axis
		if(Math.abs(cenWdX - bWdX) > 180){
			//if the center point is greater, than add 360 to B
			if(cenWdX > bWdX){
				bWdX = bWdX + 360;
			}
			//otherwise subtract 360 from B
			else{
				bWdX = bWdX - 360;
			}
		}
					
		//calculate x and y components of distances between a and b to the start
		// and use to find width and height
		double x = cenWdPt.getX();
		double y = cenWdPt.getY();
						
		double aX = (aWdX-x)*2;
		double aY = (aWdPtEnd.getY()-y)*2;
		double width = Math.sqrt(Math.pow(aX, 2)+Math.pow(aY, 2));
		double bX = Math.abs(x-bWdX)*2;
		double bY = Math.abs(y-bWdPtEnd.getY())*2;
		double height = Math.sqrt(Math.pow(bX, 2)+Math.pow(bY, 2));
						
		//convert the angle from spatial to world
		double hyp = Math.sqrt(Math.pow(Math.abs(cenWdX - aWdX), 2) + Math.pow(Math.abs(cenWdPt.getY() - aWdPtEnd.getY()), 2));
		double adj = cenWdX - aWdX;
		double angle = Math.acos(adj/hyp);
		
		Ellipse worldE = new Ellipse(x, y, width, height, angle);

		//get spatial path for world ellipse
		FPath newPath = getSpatialPathFromWorlEllipse(worldE, po);
		
		
		//set new values on feature
		f.setAttributeQuiet(Field.FIELD_PATH, newPath);
		
	}
	
	/**
	 * Given an FPath, returns an elliptical polygon (in SPATIAL COORDS) with the given number of
	 * vertices the given kilometer distance and angle in radians from the center of the given path.
	 *
	 * @param cenPt  Center point in Spatial degrees west
	 * @param kmAAxis  Length of the A axis in km
	 * @param kmBAxis  Length of the B axis in km
	 * @param angle  Angle of rotation in radians
	 * @return
	 */
	public static FPath getEllipticalPath(Point2D cenPt, double kmAAxis, double kmBAxis, double angle) {
		//get the center of the ellipse
		HVector start = new HVector(cenPt);

		//Horizontal component
		//use the spatial angle to find the A direction
		HVector horDir = new HVector(start.lon()-Math.cos(angle), start.lat()+Math.sin(angle));
		HVector horAxis = start.cross(horDir);
		HVector horEnd = start.rotate(horAxis, (kmAAxis/2)/Util.MEAN_RADIUS);
		
		//Vertical component
		//use 90 degrees plus the A-angle to find the B angle and direction
		double theta = Math.PI/2+angle;
		HVector verDir = new HVector(start.lon()-Math.cos(theta), start.lat()+Math.sin(theta));
		HVector verAxis = start.cross(verDir);
		HVector verEnd = start.rotate(verAxis, (kmBAxis/2)/Util.MEAN_RADIUS);
		
		
//		System.out.println("Spatial cen, "+(360-start.lon())+", "+start.lat()+"\na, "+(360-horEnd.lon())+", "+horEnd.lat()+"\nb, "+(360-verEnd.lon()+", "+verEnd.lat()));
		
		//center world proj on the center point
		ProjObj po = new Projection_OC(cenPt.getX(), cenPt.getY());
		//get world coords to work with
		Point2D cenWdPt = po.convSpatialToWorld(start.lon(), start.lat());
		Point2D aWdPtEnd = po.convSpatialToWorld(horEnd.lon(), horEnd.lat());
		Point2D bWdPtEnd = po.convSpatialToWorld(verEnd.lon(), verEnd.lat());
		
		//check to make sure the a or b x values are 
		// within 180 degrees of the center point (they should never
		// be greater than 180 because that means the ellipse would
		// be greater than 360 degrees wide)
		double cenWdX = cenWdPt.getX();
		double aWdX = aWdPtEnd.getX();
		double bWdX = bWdPtEnd.getX();
		//Check A axis
		if(Math.abs(cenWdX - aWdX) > 180){
			//if the center point is greater, than add 360 to A
			if(cenWdX > aWdX){
				aWdX = aWdX + 360;
			}
			//otherwise subtract 360 from A
			else{
				aWdX = aWdX - 360;
			}
		}
		//Check B axis
		if(Math.abs(cenWdX - bWdX) > 180){
			//if the center point is greater, than add 360 to B
			if(cenWdX > bWdX){
				bWdX = bWdX + 360;
			}
			//otherwise subtract 360 from B
			else{
				bWdX = bWdX - 360;
			}
		}
		
		//calculate x and y components of distances between a and b to the start
		// and use to find width and height
		double x = cenWdPt.getX();
		double y = cenWdPt.getY();
		
		double aX = (aWdX-x)*2;
		double aY = (aWdPtEnd.getY()-y)*2;
		double width = Math.sqrt(Math.pow(aX, 2)+Math.pow(aY, 2));
		double bX = Math.abs(x-bWdX)*2;
		double bY = Math.abs(y-bWdPtEnd.getY())*2;
		double height = Math.sqrt(Math.pow(bX, 2)+Math.pow(bY, 2));
		
		//convert the angle from spatial to world
		double hyp = Math.sqrt(Math.pow(Math.abs(cenWdX - aWdX), 2) + Math.pow(Math.abs(cenWdPt.getY() - aWdPtEnd.getY()), 2));
		double adj = cenWdX - aWdX;
		
		
		angle = Math.acos(adj/hyp);


//		System.out.println("Ending World: cen: "+cenWdPt.getX()+", "+cenWdPt.getY()+" a1: "+aWdPtEnd.getX()+", "+aWdPtEnd.getY()+" b1: "+bWdPtEnd.getX()+", "+bWdPtEnd.getY()+" deg:"+Math.toDegrees(angle));

		
		//use all the components to create an ellipse with world values
		Ellipse worldEllipse = new Ellipse(x, y, width, height, angle);
		
//		System.out.println("Ending world ellipse: "+worldEllipse);
		
		//get the spatial path from the world ellipse and return
		return getSpatialPathFromWorlEllipse(worldEllipse, po);
	}
	
	
	
	/**
	 * Takes in an {@link Ellipse} with world values, and the projection those world
	 * values were created in, and returns an ellipse with spatial values.
	 * Where the longitude is in degrees E.
	 * @param e An {@link Ellipse} with world values (center point in world coords, a and
	 * b axis length in world degree distances, angle in world space)
	 * @param po The projection that should be centered around this ellipse 
	 * (either using the center point, or one of the 5 points that defined it)
	 * @return An {@link Ellipse} with spatial values (center lon in degrees East,
	 * and center lat in degrees N).  A and B lengths in km. Angle in spatial space.
	 */
	public static Ellipse convertWorldEllipseToSpatialEllipse(Ellipse e, ProjObj po){
		//get center world point
		Point2D cenWdPt = e.getCenterPt();

		//find a point for the A Axis
		double angle = e.getRotationAngle();
		double aLength = e.getALength();
		//find the x and y components of the length
		double aX = Math.cos(angle)*aLength;
		double aY = Math.sin(angle)*aLength;
		//go halfway down/to the left and halfway up/to the right to get points to measure from
		Point2D aWdPt1 = new Point2D.Double(cenWdPt.getX()-aX/2, cenWdPt.getY()-aY/2);
		Point2D aWdPt2 = new Point2D.Double(cenWdPt.getX()+aX/2, cenWdPt.getY()+aY/2);

		//find a point for the B Axis
		double bLength = e.getBLength();
		//find the x and y components
		double bX = Math.sin(angle)*bLength;
		double bY = Math.cos(angle)*bLength;
		//find two points halfway down/right and up/left from the center
		Point2D bWdPt1 = new Point2D.Double(cenWdPt.getX()-bX/2, cenWdPt.getY()+bY/2);
		Point2D bWdPt2 = new Point2D.Double(cenWdPt.getX()+bX/2, cenWdPt.getY()-bY/2);
		
		//measure A and B lengths in km
		double aAxisKm = Util.angularAndLinearDistanceW(aWdPt1, aWdPt2, Main.testDriver.mainWindow.getProj())[1];
		double bAxisKm = Util.angularAndLinearDistanceW(bWdPt1, bWdPt2, Main.testDriver.mainWindow.getProj())[1];
		
		
		//convert the rotation angle from world coords to spatial
		//use A point and the center point to calculate rotation
		Point2D cenSpPt = po.convWorldToSpatial(cenWdPt);
		Point2D aSpPt = po.convWorldToSpatial(aWdPt2);
		
		//shift the spatial points back to the equator and use lines of 
		// latitude to calculate the angle relative to
		Point2D shiftedCenSpPt = new Point2D.Double(cenSpPt.getX(), 0);
		Point2D shiftedASpPt = new Point2D.Double(aSpPt.getX(), Math.abs(cenSpPt.getY()-aSpPt.getY()));
		Point2D shiftedAXSpPt = new Point2D.Double(aSpPt.getX(), 0);
		
		double hypDist = Util.angularAndLinearDistanceS(shiftedCenSpPt, shiftedASpPt, Main.testDriver.mainWindow.getProj())[1];
		double adjDist = Util.angularAndLinearDistanceS(shiftedCenSpPt, shiftedAXSpPt, Main.testDriver.mainWindow.getProj())[1];
		double theta = Math.acos(adjDist/hypDist);
		
		//create an ellipse and convert the degrees to degrees E for displaying to the user
		Ellipse newEllipse = new Ellipse(360-cenSpPt.getX(), cenSpPt.getY(), aAxisKm, bAxisKm, theta);
		
		return newEllipse;
	}
	
	/**
	 * Converts the value to kilometers based on what the units are set to
	 * @param value
	 * @return That value in km.
	 */
	private double scaleToKm(double value){
		return value * lengthUnits.getScale();
	}
	
	/**
	 * Given an {@link Ellipse} with world values, and a projection centered on
	 * the spatial center of that ellipse, return a spatial path that defines
	 * the edge of that ellipse.
	 * @param e {@link Ellipse} defined in world coordinates
	 * @param po A ProjObj centered at the spatial center of the ellipse
	 * @return A Path in spatial coordinates defining the edge of the ellipse
	 */
	public static FPath getSpatialPathFromWorlEllipse(Ellipse e, ProjObj po){
		//since everything is in world x and y are from the center point, 
		// and height == bLength and width == aLength
		Point2D cenWdPt = e.getCenterPt();
		double aLength = e.getALength();
		double bLength = e.getBLength();
		double angle = e.getRotationAngle();
		//find 36 (vertices value) points around the edge in world coords and create a path
		// set the spatial path on the ellipse by calling that path.getSpatialWest()
		Point2D[] spPts = new Point2D.Double[vertices+1];
		double x = cenWdPt.getX();
		double y = cenWdPt.getY();
		double width = aLength;
		double height = bLength;
		int index = 0;
		//step around the ellipse and calculate points on the edge
		for(double t=0; t<2*Math.PI; t+= Math.PI/(vertices/2)){
			double a = width/2;
			double b = height/2;
			double x_diff = a*Math.cos(t)*Math.cos(angle)-b*Math.sin(t)*Math.sin(angle);
			double y_diff = a*Math.cos(t)*Math.sin(angle)+b*Math.sin(t)*Math.cos(angle);
			
			double x_new = x + x_diff;
			double y_new = y + y_diff;
			Point2D pt_new = new Point2D.Double(x_new, y_new);
			//convert back to spatial
			Point2D spPt = po.convWorldToSpatial(pt_new);
			spPts[index] = spPt;
			index++;
		}
		
		return new FPath(spPts, FPath.SPATIAL_WEST, true);
	}
	
	/**
	 * Edits the given geometry source in place, and returns a new GeomSource if
	 * the values were changed and ok was hit.
	 */
	public static StyleSource<FPath> editCircleSource(Frame parent, Collection<Field> fields,
			final GeomSource geomSource) {
		final List<StyleSource<FPath>> out = new ArrayList<StyleSource<FPath>>(1);
		final Field nullField = new Field("<None>", String.class);
		
		// create units cb and default it to the first unit with equal scale
		final JComboBox unitBox = new JComboBox(Units.values());
		unitBox.setSelectedItem(geomSource == null ? null : geomSource.units);
		
		// create field cb and default it to the current field
		Set<Field> numberFields = new LinkedHashSet<Field>();
		numberFields.add(nullField);
		for (Field f: fields) {
			if (Integer.class.isAssignableFrom(f.type) ||
					Float.class.isAssignableFrom(f.type) ||
					Double.class.isAssignableFrom(f.type)) {
				numberFields.add(f);
			}
		}
		final JComboBox fieldBox = new JComboBox(numberFields.toArray(
			new Field[numberFields.size()]));
		fieldBox.setSelectedItem(geomSource == null ? nullField : geomSource.radiusField);
		
		final JDialog frame = new JDialog(parent, "Geometry Options", true);
		
		// cancel button just hides the dialog
		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				frame.setVisible(false);
			}
		});
		
		// ok button copies in the results
		JButton ok = new JButton("Okay");
		ok.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Field f = (Field) fieldBox.getSelectedItem();
				Units u = (Units)unitBox.getSelectedItem();
				if (f == nullField || u == null) {
					out.add(new StyleFieldSource<FPath>(Field.FIELD_PATH, null));
				} else if (geomSource == null || geomSource.radiusField != f || geomSource.units != u) {
					out.add(new GeomSource(f, u, geomSource.aAxisField, geomSource.bAxisField,
							geomSource.angleField, geomSource.lengthUnits, geomSource.angleUnits,
							geomSource.centerLatField, geomSource.centerLonField));
				}
				frame.setVisible(false);
			}
		});
		
		// create and show frame
		JPanel p = new JPanel(new GridBagLayout());
		int pad = 4;
		p.setBorder(new EmptyBorder(pad,pad,pad,pad));
		frame.setContentPane(p);
		Insets in = new Insets(0, pad, pad, 0);
		Box buttons = Box.createHorizontalBox();
		buttons.add(cancel);
		buttons.add(Box.createHorizontalStrut(pad));
		buttons.add(ok);
		Component[][] parts = {
				{new JLabel("Circle Radius Field"), fieldBox},
				{new JLabel("Circle Radius Scale"), unitBox},
				{new JLabel(), buttons}
		};
		for (int y = 0; y < parts.length; y++) {
			for (int x = 0; x < parts[y].length; x++) {
				int wx = parts[y][x] instanceof JLabel ? 0 : 1;
				p.add(parts[y][x], new GridBagConstraints(
					x,y, 1,1, wx,0, GridBagConstraints.NORTHWEST,
					GridBagConstraints.HORIZONTAL, in, pad, pad));
			}
		}
		
		frame.pack();
		frame.setLocationRelativeTo(parent);
		frame.setVisible(true);
		
		return out.isEmpty() ? null : out.get(0);
	}
	
	/**
	 * Edits the given geometry source in place, and returns a new GeomSource if
	 * the values were changed and ok was hit.
	 */
	public static StyleSource<FPath> editEllipseSource(Frame parent, Collection<Field> fields,
			final GeomSource geomSource) {
		final List<StyleSource<FPath>> out = new ArrayList<StyleSource<FPath>>(1);
		final Field nullField = new Field("<None>", String.class);
		
		// create units cb and default it to the first unit with equal scale
		final JComboBox<LengthUnits> axesUnitBox = new JComboBox<LengthUnits>(LengthUnits.values());
		axesUnitBox.setSelectedItem(geomSource == null ? null : geomSource.lengthUnits);
		
		final JComboBox<AngleUnits> angleUnitBox = new JComboBox<AngleUnits>(AngleUnits.values());
		angleUnitBox.setSelectedItem(geomSource == null ? null : geomSource.angleUnits);
		
		// create field cb and default it to the current field
		Set<Field> numberFields = new LinkedHashSet<Field>();
		numberFields.add(nullField);
		for (Field f: fields) {
			if (Integer.class.isAssignableFrom(f.type) ||
					Float.class.isAssignableFrom(f.type) ||
					Double.class.isAssignableFrom(f.type)) {
				numberFields.add(f);
			}
		}
		
		Field[] numberFieldsArray = numberFields.toArray(new Field[numberFields.size()]);
		
		final JComboBox<Field> aAxisBox = new JComboBox<Field>(numberFieldsArray);
		aAxisBox.setSelectedItem(geomSource == null ? nullField : geomSource.aAxisField);
		
		final JComboBox<Field> bAxisBox = new JComboBox<Field>(numberFieldsArray);
		bAxisBox.setSelectedItem(geomSource == null ? nullField : geomSource.bAxisField);
			
		final JComboBox<Field> angleBox = new JComboBox<Field>(numberFieldsArray);
		angleBox.setSelectedItem(geomSource == null ? nullField : geomSource.angleField);
		
		final JComboBox<Field> cenLatBox = new JComboBox<Field>(numberFieldsArray);
		cenLatBox.setSelectedItem(geomSource == null ? nullField : geomSource.centerLatField);
		
		final JComboBox<Field> cenLonBox = new JComboBox<Field>(numberFieldsArray);
		cenLonBox.setSelectedItem(geomSource == null ? nullField : geomSource.centerLonField);
		
		final JDialog frame = new JDialog(parent, "Geometry Options", true);
		
		// cancel button just hides the dialog
		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				frame.setVisible(false);
			}
		});
		
		// ok button copies in the results
		JButton ok = new JButton("Okay");
		ok.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Field aAxisField = (Field) aAxisBox.getSelectedItem();
				Field bAxisField = (Field) bAxisBox.getSelectedItem();
				Field angleField = (Field) angleBox.getSelectedItem();
				LengthUnits lUnits = (LengthUnits)axesUnitBox.getSelectedItem();
				AngleUnits aUnits = (AngleUnits)angleUnitBox.getSelectedItem();
				Field cenLatField = (Field) cenLatBox.getSelectedItem();
				Field cenLonField = (Field) cenLonBox.getSelectedItem();
				
				if (aAxisField == nullField || bAxisField == nullField || 
					angleField == nullField || lUnits == null || aUnits == null) {
					
					out.add(new StyleFieldSource<FPath>(Field.FIELD_PATH, null));
				} 
				else if (geomSource == null || geomSource.aAxisField != aAxisField 
				||geomSource.bAxisField != bAxisField ||geomSource.angleField != angleField 
				|| geomSource.lengthUnits != lUnits || geomSource.angleUnits != aUnits) {
					
					out.add(new GeomSource(geomSource.radiusField, geomSource.units, aAxisField, 
							bAxisField, angleField, lUnits, aUnits, cenLatField, cenLonField));
				}
				
				frame.setVisible(false);
			}
		});
		
		// create and show frame
		JPanel p = new JPanel(new GridBagLayout());
		int pad = 4;
		p.setBorder(new EmptyBorder(pad,pad,pad,pad));
		frame.setContentPane(p);
		Insets in = new Insets(0, pad, pad, 0);
		Box buttons = Box.createHorizontalBox();
		buttons.add(cancel);
		buttons.add(Box.createHorizontalStrut(pad));
		buttons.add(ok);
		Component[][] parts = {
				{new JLabel("A Axis Field"), aAxisBox},
				{new JLabel("B Axis Field"), bAxisBox},
				{new JLabel("Rotation Angle Field"), angleBox},
				{new JLabel("Axes Scale"), axesUnitBox},
				{new JLabel("Angle Units"), angleUnitBox},
				{new JLabel(), buttons}
		};
		for (int y = 0; y < parts.length; y++) {
			for (int x = 0; x < parts[y].length; x++) {
				int wx = parts[y][x] instanceof JLabel ? 0 : 1;
				p.add(parts[y][x], new GridBagConstraints(
					x,y, 1,1, wx,0, GridBagConstraints.NORTHWEST,
					GridBagConstraints.HORIZONTAL, in, pad, pad));
			}
		}
		
		frame.pack();
		frame.setLocationRelativeTo(parent);
		frame.setVisible(true);
		
		return out.isEmpty() ? null : out.get(0);
	}
	
	/**
	 * Sets the given radius onto this feature. Handles converting from
	 * kilometer radius to the units and type stored on the feature.
	 */
	public void setRadius(Feature f, double km) {
		km /= units.getScale();
		Number radius;
		if (Integer.class.isAssignableFrom(radiusField.type)) {
			radius = (int)(km);
		} else if (Float.class.isAssignableFrom(radiusField.type)) {
			radius = (float)(km);
		} else if (Double.class.isAssignableFrom(radiusField.type)) {
			radius = (double)(km);
		} else {
			throw new IllegalStateException("Radius has unsupported type " + radiusField.type.getName());
		}
		f.setAttribute(radiusField, radius);
	}
	
	/**
	 * Sets the given value onto this feature (either a axis, b axis or angle).
	 * Handles converting from kilometer radius to the units and type stored on the feature.
	 */
	public void setFieldValue(Feature f, Field field, double val) {
		//only scale for the axes, not the angle
		if(aAxisField == field || bAxisField == field){
			val /= lengthUnits.getScale();
		}
		Number numberValue;
		
		if (Integer.class.isAssignableFrom(field.type)) {
			numberValue = (int)(val);
		} else if (Float.class.isAssignableFrom(field.type)) {
			numberValue = (float)(val);
		} else if (Double.class.isAssignableFrom(field.type)) {
			numberValue = (double)(val);
		} else {
			throw new IllegalStateException(field.toString()+" has unsupported type " + field.type.getName());
		}
		

		if (aAxisField == field) {
			f.setAttribute(aAxisField, numberValue);
		} else if (bAxisField == field) {
			f.setAttribute(bAxisField, numberValue);
		} else if (angleField == field) {
			f.setAttribute(angleField, numberValue);
		} else if (centerLonField == field){
			f.setAttribute(centerLonField, numberValue);
		} else if (centerLatField == field){
			f.setAttribute(centerLatField, numberValue);
		}
	}
	
	
	/** Provides converters from common methods of describing circle size to the one internal form that we need. */
	public static enum Units implements Serializable {
		RadiusKm(1.0, "Radius (km)"),
		RadiusMeters(.001, "Radius (m)"),
		RadiusMiles(1.609344, "Radius (mi)"),
		RadiusFeet(0.0003048, "Radius (ft)"),
		DiameterKm(1.0/2, "Diameter (km)"),
		DiameterMeters(.001/2, "Diameter (m)"),
		DiameterMiles(1.609344/2, "Diameter (mi)"),
		DiameterFeet(0.0003048/2, "Diameter (ft)");
		private final double scale;
		private final String name;
		private Units(double scale, String name) {
			this.scale = scale;
			this.name = name;
		}
		public double getScale() {
			return scale;
		}
		public String toString() {
			return name;
		}
	}
	
	public static enum LengthUnits implements Serializable {
		SemiAxesKm(2.0, "Semi Axes Length (km)"),
		SemiAxesMeters(.002, "Semi Axes Length (m)"),
		SemiAxesMiles(1.609344*2, "Semi Axes Length (mi)"),
		SemiAxesFeet(0.0003048*2, "Semi Axes Length (ft)"),
		AxesKm(1.0, "Axes Length (km)"),
		AxesMeters(.001, "Axes Length (m)"),
		AxesMiles(1.609344, "Axes Length (mi)"),
		AxesFeet(0.0003048, "Axes Length (ft)");
		private final double scale;
		private final String name;
		private LengthUnits(double scale, String name) {
			this.scale = scale;
			this.name = name;
		}
		public double getScale() {
			return scale;
		}
		public String toString() {
			return name;
		}
	}
	
	public static enum AngleUnits implements Serializable {
		Radians(1.0, "Rotation Angle (rad)"),
		Degrees(180/Math.PI, "Rotation Angle (deg)");
		private final double scale;
		private final String name;
		private AngleUnits(double scale, String name) {
			this.scale = scale;
			this.name = name;
		}
		public double getScale() {
			return scale;
		}
		public String toString() {
			return name;
		}
	}
}

