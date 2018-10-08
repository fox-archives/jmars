package edu.asu.jmars.layer;

import edu.asu.jmars.*;
import edu.asu.jmars.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.SwingUtilities;

public class WrappedMouseEvent extends MouseEvent
 {
	private Point realPoint;

	public WrappedMouseEvent(MouseEvent e)
	 {
		super((Component)e.getSource(),
			  e.getID(),
			  e.getWhen(),
			  e.getModifiers(),
			  e.getX(),
			  e.getY(),
			  e.getClickCount(),
			  SwingUtilities.isRightMouseButton(e)
			);
		realPoint = (Point) e.getPoint().clone();
	 }

	public Point getRealPoint()
	 {
		return  (Point) realPoint.clone();
	 }
 }
