package edu.asu.jmars.layer.north;

import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import edu.asu.jmars.Main;
import edu.asu.jmars.layer.FocusPanel;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.util.HighResExport;

public class NorthLView extends Layer.LView {

	protected NorthSettings settings;
	private NorthArrow arrow;
	
	private Point2D clickStartPoint;
	private Point2D clickEndPoint;
	
	private Point2D currentPoint;
	
	public NorthLView(Layer layerParent, NorthSettings params) {
		super(layerParent);
		//buffer for the Arrow
		setBufferCount(1);
		//settings defining the look of the arrow
		settings = params;
		//create north arrow
		arrow = new NorthArrow(settings.arrowSize);
	}

	@Override
	protected Object createRequest(Rectangle2D where) {
		repaint();
		return null;
	}

	@Override
	public void receiveData(Object layerData) {
	}

	@Override
	protected LView _new() {
		return new NorthLView(getLayer(), settings);
	}
	
	public String getName(){
		return "North Arrow";
	}
	
	public FocusPanel getFocusPanel(){
		if(focusPanel == null){
			focusPanel = new NorthFocusPanel(this);
		}
		return focusPanel;
	}
	
	
	public void paintComponent(Graphics g){
	    // If we're exporting, don't draw the scale bar on every tile
		if (HighResExport.exporting) return;
		
		// Don't try to draw unless the view is visible
		if (!isVisible() || viewman == null) {//@since remove viewman2
			return;
		}
		
		//dont draw for the panner
		if(getChild() == null){
			return;
		}
		clearOffScreen(0);
		Graphics2D g2 = getOffScreenG2Direct(0);
		
		if(g2 == null){
			return;
		}
		
		//create the north arrow based on the correct size
		Shape shape = arrow.getArrow(settings.arrowSize);
		currentPoint = new Point2D.Double(getWidth()-arrow.getXOffset(), getHeight()-arrow.getYOffset());
		
//		//center point of screen in world coords
		Point2D startPt = Main.testDriver.locMgr.getLoc();
//		System.out.println("start x,y: "+ startPt.getX()+", "+startPt.getY());
		Point2D spatialCenter = Main.PO.convWorldToSpatial(startPt);
//		System.out.println("sp x,y: "+ spatialCenter.getX()+", "+spatialCenter.getY());
		Point2D spatialEnd = new Point2D.Double(spatialCenter.getX(), spatialCenter.getY()+1);
//		System.out.println("sp end x,y: "+ spatialEnd.getX()+", "+spatialEnd.getY());
		Point2D end = Main.PO.convSpatialToWorld(spatialEnd);
		
		//difference between start and finish should never be very large 
		// (couple degrees at most) so if it's larger than that, it's 
		// because of wrapping world points, so add 360 to the smaller 
		// x value to compensate.
		double worldXDiff = Math.abs(startPt.getX() - end.getX());
		if(worldXDiff>180){
			if(startPt.getX()<end.getX()){
				startPt = new Point2D.Double(startPt.getX()+360, startPt.getY());
			}
			else if(end.getX()<startPt.getX()){
				end = new Point2D.Double(end.getX()+360, end.getY());
			}
		}
		
		Point2D screenStart = getProj().world.toScreen(startPt);
		Point2D screenEnd = getProj().world.toScreen(end);
		
		double angle = Math.atan2(screenStart.getX()-screenEnd.getX(), screenStart.getY()-screenEnd.getY());
		
		//startX is farthest right side of the lview, minus the
		// offset of the arrow, minus and difference created from
		// dragging the mouse
		int startX = (int) currentPoint.getX();
		if(clickStartPoint!= null && clickEndPoint != null){
			startX += (clickEndPoint.getX() - clickStartPoint.getX());
		}
		
		//startY is farthest bottom side of the lview, minus the
		// offset of the arrow, minus and difference created from
		// dragging the mouse
		int startY = (int)currentPoint.getY();
		if(clickStartPoint != null && clickEndPoint != null){
			startY += (clickEndPoint.getY() - clickStartPoint.getY());
		}

		Point2D start = new Point2D.Double(startX, startY);
		
		//translate for arrow
		g2.translate(start.getX(), start.getY());
		//rotate
		g2.rotate(-angle);
		//draw outline
		g2.setColor(settings.arrowColor);
		g2.setStroke(new BasicStroke(settings.outlineSize));
		//always draw the outline
		g2.draw(shape);
		//check booleans to fill left or right
		if(settings.fillLeft){
			g2.fill(arrow.getLeftArrow());
		}
		if(settings.fillRight){
			g2.fill(arrow.getRightArrow());
		}
		//draw text
		if(settings.showText){
			g2.setColor(settings.textColor);
			g2.setFont(arrow.getTextFont(settings.fontSize));
			g2.drawString("N", (int)arrow.getTextX(), (int)arrow.getTextY());
		}
		
		super.paintComponent(g);
	}
	
	
}
