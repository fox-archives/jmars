package edu.asu.jmars.layer.north;

import java.awt.Font;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.HashMap;

import javax.swing.JLabel;

public class NorthArrow {
	private Shape arrow;
	private Shape leftSide;
	private Shape rightSide;
	private int height;
	private int width;
	private int midHeight;
	private double startX;
	private double startY;
	private Font textFont;
	private double scale;
   	
	private HashMap<Integer, Double> sizeToScale;
	

	/**
	 * Creates a north arrow object based on the sized passed in.
	 * This size (between 1-5) is translated to a scale (1 to 1.8)
	 * that is used to adjust the size of the arrow being displayed
	 * @param scale A value between 1-5.
	 */
	public NorthArrow(int size){
		//set defaults
		textFont = new Font("Dialog", Font.BOLD, 16);
		
		height = 50;
		width = 36;
		midHeight = 30;
		
		//set initial scale and build scale map (int of 1-5 to doubles 1-1.8)
		sizeToScale = new HashMap<Integer, Double>();
		sizeToScale.put(1, 1.0);
		sizeToScale.put(2, 1.2);
		sizeToScale.put(3, 1.4);
		sizeToScale.put(4, 1.6);
		sizeToScale.put(5, 1.8);
		//keep track of the current size, so that we know when
		// we need to recalculate the points for the arrow
		scale = sizeToScale.get(size);
		//start x and y define the top point of the arrow
		// (the arrow is centered around 0,0)
		buildArrow();
	}
	
	private void buildArrow(){
		startX = 0;
		startY = -height*scale/2;
		Point2D botLeft = new Point2D.Double(startX-(scale*width/2), startY+(scale*height));
		Point2D botCenter = new Point2D.Double(startX, startY+(scale*midHeight));
		Point2D botRight = new Point2D.Double(startX+(scale*width/2), startY+(scale*height));
		
		GeneralPath gp = new GeneralPath();
		gp.moveTo(botLeft.getX(), botLeft.getY());
		gp.lineTo(startX, startY);
		gp.lineTo(botRight.getX(), botRight.getY());
		gp.lineTo(botCenter.getX(), botCenter.getY());
		gp.closePath();
		
		arrow = gp;
	}
	

	/**
	 * Create a "half arrow" so that half
	 * the north arrow can be filled in the
	 * color, with the other half transparent
	 */
	private void buildLeftArrow(){
		Point2D botCenter = new Point2D.Double(startX, startY+(scale*midHeight));
		Point2D botLeft = new Point2D.Double(startX-(scale*width/2), startY+(scale*height));

		GeneralPath gp = new GeneralPath();
		gp.moveTo(botLeft.getX(), botLeft.getY());
		gp.lineTo(startX, startY);
		gp.lineTo(botCenter.getX(), botCenter.getY());
		gp.closePath();
		
		leftSide = gp;
	}
	
	/**
	 * Create a "half arrow" so that half
	 * the north arrow can be filled in the
	 * color, with the other half transparent
	 */
	private void buildRightArrow(){
		Point2D botCenter = new Point2D.Double(startX, startY+(scale*midHeight));
		Point2D botRight = new Point2D.Double(startX+(scale*width/2), startY+(scale*height));
		
		GeneralPath gp = new GeneralPath();
		gp.moveTo(startX, startY);
		gp.lineTo(botRight.getX(), botRight.getY());
		gp.lineTo(botCenter.getX(), botCenter.getY());
		gp.closePath();
		
		rightSide = gp;
	}
	
	
	/**
	 * Get the arrow shape to draw.  If the arrow was not 
	 * created using the same size specified, recalculate it
	 * and then return.
	 * @param size The size of the arrow.
	 * @return  arrow shape for drawing
	 */
	public Shape getArrow(int size){
		//if the arrow was not built at this size, rebuild
		// all peices before returning
		if(scale != sizeToScale.get(size)){
			scale = sizeToScale.get(size);
			buildArrow();
			buildLeftArrow();
			buildRightArrow();
		}
		return arrow;
	}
	
	/**
	 * @return A shape defining the left half of the arrow
	 */
	public Shape getLeftArrow(){
		if (leftSide == null){
			buildLeftArrow();
		}
		return leftSide;
	}
	
	/**
	 * @return A shape defining the right half of the arrow
	 */
	public Shape getRightArrow(){
		if (rightSide == null){
			buildRightArrow();
		}
		return rightSide;
	}
	
	/**
	 * @return The x position to draw the "N".  This is centered on
	 * the middle of the arrow, and then offset to the left by half
	 * the width of the "N", so that the "N" is centered with the
	 * arrow.
	 */
	public double getTextX(){
		//make a fake label to find the size of it to center it properly when drawing
		JLabel nString = new JLabel("N");
		nString.setFont(textFont);
		return -nString.getPreferredSize().width/2;
	}
	
	/**
	 * @return The y position to draw the "N".  This is just 
	 * underneath the arrow.  So it is half the height of the arrow
	 * plus half the height of the "N".
	 */
	public double getTextY(){
		//make a fake label to find the size of it to center it properly when drawing
		JLabel nString = new JLabel("N");
		nString.setFont(textFont);
		return height*scale/2 + nString.getPreferredSize().height/2;
	}
	
	/**
	 * @return The width of the arrow (which is width*scale) plus a 
	 * buffer of 20 pixels for the side of the screen.
	 */
	public int getXOffset(){
		return (int)(width*scale) + 20;
	}

	/**
	 * @return The height of the arrow (which is width*scale) plus a 
	 * buffer of 20 pixels for the bottom of the screen.
	 */
	public int getYOffset(){
		return (int)(height*scale) + 20;
	}
	
	/**
	 * Returns the Font used for the 'N' part of the north arrow.
	 * If the passed in font size is not the same size as the 
	 * current font object, a new font object will be created
	 * and then returned.
	 * @param size  Desired font size
	 * @return Font for the 'N' of the arrow
	 */
	public Font getTextFont(int size){
		//if the current font is not the same as the size, create
		// a new font with that size
		if(textFont.getSize() != size){
			textFont = new Font("Dialog", Font.BOLD, size);
		}
		return textFont;
	}
}
