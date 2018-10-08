package edu.asu.jmars.layer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.FocusManager;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputListener;

import edu.asu.jmars.LocationManager;
import edu.asu.jmars.Main;
import edu.asu.jmars.ToolManager;
import edu.asu.jmars.ZoomManager;

import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.HighResExport;
import edu.asu.jmars.util.ResizeMainView;

public final class MainGlass extends BaseGlass {
	private static DebugLog log = DebugLog.instance();

	protected void transferMenuItems(JPopupMenu popup,
			Component[] contextItems, boolean addSepBefore) {

		if (contextItems != null && contextItems.length > 0) {
			if (addSepBefore)
				popup.add(new JSeparator());

			for (int i = 0; i < contextItems.length; i++) {
				if (contextItems[i] != null)
					popup.add(contextItems[i]);
				else
					log.aprintln("Error: null context menu item.");
			}

			if (!addSepBefore)
				popup.add(new JSeparator());
		}
	}

	protected void transferInactiveMenuItemsTop(JPopupMenu popup,
			Point2D worldPt) {
		//now check for inactive layers
		Iterator iter = mainVMan.viewList.iterator();
		while (iter.hasNext()) {

			Layer.LView view = (Layer.LView) iter.next();

			//we have already handled the active view
			if (mainVMan.getActiveLView().equals(view))
				continue;

			if (view != null && view.isVisible() && view.getChild() != null) {
				Component[] contextItems = view.getContextMenuTop(worldPt);
				transferMenuItems(popup, contextItems, false);
			}
		}
	}

	protected void transferInactiveMenuItems(JPopupMenu popup, Point2D worldPt) {
		//now check for inactive layers
		Iterator iter = mainVMan.viewList.iterator();
		while (iter.hasNext()) {
			Layer.LView view = (Layer.LView) iter.next();
			//we have already handled the active view
			if (mainVMan.getActiveLView().equals(view))
				continue;

			if (view != null && view.isVisible() && view.getChild() != null) {
				Component[] contextItems = view.getContextMenu(worldPt);
				transferMenuItems(popup, contextItems, true);
			}
		}
	}

	public JPopupMenu getPopupMenu(final MouseEvent origEvent,
			final Point2D worldPt) {
		
		if (Main.MAC_OS_X && !(FocusManager.getCurrentKeyboardFocusManager().getActiveWindow()==Main.mainFrame)) {
			origEvent.consume();
			return null;
		}
		
		// Set up main menu area
		final JPopupMenu popup = new JPopupMenu();

		Layer.LView view = mainVMan.getActiveLView();

		// Set up top context menu area, if there is one
		if (view != null) {
			Component[] contextItems = view.getContextMenuTop(worldPt);
			transferMenuItems(popup, contextItems, false);
		}

		//look for any other views that might have something to add
		transferInactiveMenuItemsTop(popup, worldPt);

		JMenu sub;
		JMenuItem menuItem;

		menuItem = new JMenuItem("Recenter window");
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Point2D worldPt2 = mainVMan.getProj().screen.toWorld(origEvent.getPoint());
				mainVMan.getLocationManager().setLocation(worldPt2, true);
			}
		});
		popup.add(menuItem);

		sub = new JMenu("Zoom & recenter");
		ButtonGroup group = new ButtonGroup();
		for (final int zoom: myVMan.getZoomManager().getZoomFactors()) {
			menuItem = new JRadioButtonMenuItem(new AbstractAction(zoom + " Pix/Deg") {
				public void actionPerformed(ActionEvent e) {
					mainVMan.getLocationManager().setLocation(worldPt, true);
					mainVMan.getZoomManager().setZoomPPD(zoom, true);
				}
			});
			group.add(menuItem);
			sub.add(menuItem);
			menuItem.setSelected(zoom == mainVMan.getZoomManager().getZoomPPD());
		}
		popup.add(sub);

		// Set up context menu area, if there is one at the bottom of the menu
		if (view != null) {
			Component[] contextItems = view.getContextMenu(worldPt);
			transferMenuItems(popup, contextItems, true);
		}

		//look for any other views that might have something to add
		transferInactiveMenuItems(popup, worldPt);

		// Set up any ruler menus
		if (view != null) {
			Component[] contextItems = view.getRulerMenu();
			transferMenuItems(popup, contextItems, true);
		}

		return popup;
	}

	MainGlass(final LViewManager mainVMan) {
		super(mainVMan, mainVMan);
	}

	MouseInputListener createMouseHandler() {
		return new GlassMouseListener();
	}

	public void paintComponent(Graphics g) {
		fastPan.paintComponent(g);
	}

	private class GlassMouseListener implements MouseInputListener, MouseWheelListener {
		JPopupMenu popup = null;

		// All mouse events sequences start with a
		// mousePressed... in order to disable clicks that
		// occur while the popup menu is open, we: 1) record
		// whether the menu was visible during mousePressed
		// and 2) kill any successive events if menuWasVisible
		// is true during them. This works since no mouse
		// event handlers are called unless mousePressed has
		// been called first.
		boolean menuWasVisible = false;

		// used in 'pan' mode
		Point panStartPoint;
		
		boolean drawingRuler=false;
		// used in 'measure' mode 
		Point rulerStartPoint = null;		
		Point rulerEndPoint = null;
		Point rulerLastPoint = null;

		public void mouseClicked(MouseEvent e) {
			if (menuWasVisible)
				return;
			else if (!SwingUtilities.isRightMouseButton(e)){
				if(ToolManager.getToolMode()==ToolManager.ZOOM_IN){
					int currentZoom = Main.testDriver.mainWindow.getZoomManager().getZoomPPD();
					int listPos = -1;
					List<Integer>zoomFactors = Main.testDriver.mainWindow.getZoomManager().getZoomFactors();
					for(int i=0; i<zoomFactors.size(); i++){
						if (currentZoom == zoomFactors.get(i))
							listPos = i;
					}
					if(listPos<zoomFactors.size()-1){
						Main.testDriver.mainWindow.getLocationManager().setLocation(getWorldPt(e), false);
						Main.testDriver.mainWindow.getZoomManager().setZoomPPD(zoomFactors.get(listPos+1), true);
					}
					else{
						Main.testDriver.mainWindow.getLocationManager().setLocation(getWorldPt(e), false);
						Main.testDriver.mainWindow.getZoomManager().setZoomPPD(zoomFactors.get(listPos), true);
					}			
				}
				else if(ToolManager.getToolMode()==ToolManager.ZOOM_OUT){
					int currentZoom = Main.testDriver.mainWindow.getZoomManager().getZoomPPD();
					int listPos = -1;
					List<Integer>zoomFactors = Main.testDriver.mainWindow.getZoomManager().getZoomFactors();
					for(int i=0; i<zoomFactors.size(); i++){
						if (currentZoom == zoomFactors.get(i))
							listPos = i;
					}
					if(listPos>0){
						Main.testDriver.mainWindow.getZoomManager().setZoomPPD(zoomFactors.get(listPos-1), false);
						Main.testDriver.mainWindow.getLocationManager().setLocation(getWorldPt(e), true);
					}
					else{
						Main.testDriver.mainWindow.getLocationManager().setLocation(getWorldPt(e), false);
						Main.testDriver.mainWindow.getZoomManager().setZoomPPD(zoomFactors.get(listPos), true);
					}
				} else if (ToolManager.getToolMode()==ToolManager.MEASURE) {
					return;
				} else if (ToolManager.getToolMode()==ToolManager.INVESTIGATE){
					InvestigateDisplay.showInvDisplay(e.getX(),e.getY());
					proxy(e);
				}
				else
					proxy(e);
			}		
		}

	    protected boolean mouseDragged = false;
		protected Point mouseDown = null;
		protected Rectangle2D curSelectionRect = null;
		
	//This variable is used to keep track of the first mouse button pressed if more than one
	// are pressed at the same time.  It is used in mousePressed, mouseDragged and mouseReleased.	
		int mouseBtn = -1;
		public void mousePressed(MouseEvent e) {

			int btnCount = getButtonCount(e);
			if(btnCount>1)
				return;
			
			int buttonPressed = e.getButton();
			
			if (mouseBtn == -1){
				if (buttonPressed != MouseEvent.BUTTON3) {
					mouseBtn = buttonPressed;
				}
	
				menuWasVisible = (popup != null && popup.isVisible());
				wasDragged = false;
				if (SwingUtilities.isRightMouseButton(e)) {			
					popup = getPopupMenu(e, getWorldPt(e));
					if (popup!=null) {
						popup.show(MainGlass.this, e.getX(), e.getY());
						log.println("Menu shown");
					}
				} else if (SwingUtilities.isMiddleMouseButton(e) || ToolManager.getToolMode()==ToolManager.PAN_HAND){
						panStartPoint = e.getPoint();
						fastPan.beg();
						ToolManager.setToolMode(ToolManager.PAN_HAND);
						ToolManager.setGrabHand();
				} else if(ToolManager.getToolMode()==ToolManager.MEASURE){
					rulerStartPoint = e.getPoint();
					rulerEndPoint = e.getPoint();
					return;			
				} else if(ToolManager.getToolMode()==ToolManager.ZOOM_OUT || ToolManager.getToolMode()==ToolManager.ZOOM_IN){
					return;
				} else if(ToolManager.getToolMode()==ToolManager.INVESTIGATE){
					InvestigateDisplay.showInvDisplay(e.getX(),e.getY());
					proxy(e);
				} else if(ToolManager.getToolMode()==ToolManager.EXPORT) {
					mouseDown = e.getPoint();

					curSelectionRect = new Rectangle2D.Double(mouseDown.x, mouseDown.y, 0, 0);
					drawSelectionRect(curSelectionRect);
			        mouseDragged = false;
                } else if(ToolManager.getToolMode()==ToolManager.RESIZE) {
                    mouseDown = e.getPoint();

                    curSelectionRect = new Rectangle2D.Double(mouseDown.x, mouseDown.y, 0, 0);
                    drawSelectionRect(curSelectionRect);
                    mouseDragged = false;
				}
				else if (!menuWasVisible)
					
					proxy(e);
			}

		}

        // Lifted from the StampLView code for drawing a box to select stamps
		protected void drawSelectionRect(Rectangle2D rect)
		{
			if (rect==null) return;
			Graphics2D g2 = (Graphics2D) Main.testDriver.mainWindow.wrapScreenGraphics((Graphics2D) getGraphics());
			if (g2 != null) {
				g2.setStroke(new BasicStroke(2));
				g2.setXORMode(Color.gray);
				g2.draw(rect);

				log.println("drawing rectangle (" + rect.getMinX() + "," + rect.getMinY()+ ") to (" 
					    + rect.getMaxX() + "," + rect.getMaxY() + ")");
			}
		}
		
		public void mouseReleased(MouseEvent e) {
		//This is used in case multiple mouse buttons are pressed or released at once...	
			if (e.getButton() == mouseBtn){
				mouseBtn = -1;
				if (SwingUtilities.isMiddleMouseButton(e) || 
					ToolManager.getToolMode()==ToolManager.PAN_HAND 
					&& !SwingUtilities.isRightMouseButton(e)) {
					
					if(!wasDragged){
						//The following is simulating a drag for a fast pan, except it drags nowhere. 
						//Since there was actually no drag, this is to handle the middle
						//mouse button being clicked and released. Without this logic, it stays in the 
						//state of waiting for drag for fast pan. 
						Point mouseLast = e.getPoint();
						fastPan.panTo(mouseLast.x - panStartPoint.x, mouseLast.y - panStartPoint.y);
						fastPan.end(e.getX(), e.getY());
						//end simulating fast pan
						ToolManager.setToolMode(ToolManager.getPrevMode());
						return;
					}
					fastPan.end(e.getX(), e.getY());
					if(SwingUtilities.isMiddleMouseButton(e))
						ToolManager.setToolMode(ToolManager.getPrevMode());
					if(ToolManager.getToolMode()==ToolManager.PAN_HAND)
						ToolManager.setToolMode(ToolManager.PAN_HAND);
					return;
				} else if (ToolManager.getToolMode()==ToolManager.MEASURE){
					if(rulerEndPoint!=null) {
						drawLine(rulerStartPoint.x, rulerStartPoint.y, rulerEndPoint.x, rulerEndPoint.y);
						rulerLastPoint=null;
						repaint();
					}
				} else if (ToolManager.getToolMode()==ToolManager.INVESTIGATE){
					InvestigateDisplay.showInvDisplay(e.getX(), e.getY());
				} else if (ToolManager.getToolMode()==ToolManager.EXPORT) {
					drawSelectionRect(curSelectionRect);
		            
		            MultiProjection proj = Main.testDriver.mainWindow.getProj();
		            if (proj == null) {
		                log.aprintln("null projection");
		                return;
		            }
		            
		            Point curPoint = e.getPoint();
		            
		            Point2D worldPt1 = proj.screen.toWorld(mouseDown);
		            Point2D worldPt2 = proj.screen.toWorld(curPoint);
		            
		            worldPt1.setLocation(worldPt1.getX(), worldPt1.getY());
		            
		            worldPt2.setLocation(worldPt2.getX(), worldPt2.getY());
		            
		            final Rectangle2D worldBounds = new Rectangle2D.Double(worldPt1.getX(), worldPt1.getY(), 0, 0);
		            worldBounds.add(worldPt2);
		            
		            mouseDragged = false;
					mouseDown = null;
					curSelectionRect = null;	
					ToolManager.setToolMode(ToolManager.getPrevMode());
					if (worldBounds.getWidth()==0) {
						return;
					}
					HighResExport.exportHighResolution(worldBounds);
					return;
					
                } else if (ToolManager.getToolMode()==ToolManager.RESIZE) {
                    drawSelectionRect(curSelectionRect);
                    
                    MultiProjection proj = Main.testDriver.mainWindow.getProj();
                    if (proj == null) {
                        log.aprintln("null projection");
                        return;
                    }
                    
                    Point curPoint = e.getPoint();
                    
                    Point2D worldPt1 = proj.screen.toWorld(mouseDown);
                    Point2D worldPt2 = proj.screen.toWorld(curPoint);
                    
                    worldPt1.setLocation(worldPt1.getX(), worldPt1.getY());
                    
                    worldPt2.setLocation(worldPt2.getX(), worldPt2.getY());
                    
                    final Rectangle2D worldBounds = new Rectangle2D.Double(worldPt1.getX(), worldPt1.getY(), 0, 0);
                    worldBounds.add(worldPt2);
                    
                    mouseDragged = false;
                    mouseDown = null;
                    curSelectionRect = null;    
                    ToolManager.setToolMode(ToolManager.getPrevMode());
                    if (worldBounds.getWidth()==0) {
                        return;
                    }
                    ResizeMainView.resize(worldBounds);
                    return;
                }
	
				if (menuWasVisible)
					return;
				if (!SwingUtilities.isRightMouseButton(e))
					proxy(e);

			}
			
		}

		public void mouseEntered(MouseEvent e) {
			updateLocation(e.getPoint());
			if (ToolManager.getToolMode() == ToolManager.INVESTIGATE){
				InvestigateDisplay.getInstance().setInvData(e);
				InvestigateDisplay.showInvDisplay(e.getX(),e.getY());
			}
			proxy(e);
		}

		public void mouseExited(MouseEvent e) {
			if (ToolManager.getToolMode() == ToolManager.INVESTIGATE){
				InvestigateDisplay.getInstance().setVisible(false);
			}
			Main.setStatus(null);
			proxy(e);
		}

		public void mouseMoved(MouseEvent e) {
			updateLocation(e.getPoint());
			if (ToolManager.getToolMode() == ToolManager.INVESTIGATE){
				InvestigateDisplay.getInstance().setInvData(e);
				InvestigateDisplay.showInvDisplay(e.getX(),e.getY());
			}
			proxy(e);
		}
		
		boolean wasDragged = false;
		public void mouseDragged(MouseEvent e) {
			//These if statements are meant to check the make sure something is only activated if the 
			// button being dragged was the first button to be pressed (if more than one button is 
			// pressed at the same time)
			if (mouseBtn == MouseEvent.BUTTON1 && (e.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) != InputEvent.BUTTON1_DOWN_MASK){
				return;
			}
			if (mouseBtn == MouseEvent.BUTTON2 && (e.getModifiersEx() & InputEvent.BUTTON2_DOWN_MASK) != InputEvent.BUTTON2_DOWN_MASK){
				return;
			}
			if (mouseBtn == MouseEvent.BUTTON3 && (e.getModifiersEx() & InputEvent.BUTTON3_DOWN_MASK) != InputEvent.BUTTON3_DOWN_MASK){
				return;
			}

			if (SwingUtilities.isMiddleMouseButton(e) || ToolManager.getToolMode()==ToolManager.PAN_HAND) {
				Point mouseLast = e.getPoint();
				fastPan.panTo(mouseLast.x - panStartPoint.x, mouseLast.y - panStartPoint.y);
				wasDragged = true;
				return;
			} else if (ToolManager.getToolMode() == ToolManager.MEASURE){	
				rulerLastPoint = rulerEndPoint;
				
				rulerEndPoint = e.getPoint();
				
				if(rulerLastPoint != null)
					drawLine(rulerStartPoint.x, rulerStartPoint.y,
							 rulerLastPoint.x, rulerLastPoint.y);
				
				drawLine(rulerStartPoint.x, rulerStartPoint.y,
						rulerEndPoint.x, rulerEndPoint.y);
				
				Main.setStatusFromWorld(
					Main.testDriver.mainWindow.getProj().screen.toWorld(rulerStartPoint),
					Main.testDriver.mainWindow.getProj().screen.toWorld(rulerEndPoint));
				
				return;				
			} else if (ToolManager.getToolMode() == ToolManager.EXPORT || ToolManager.getToolMode() == ToolManager.RESIZE) {
				// Update drawing of rubberband stamp selection box.
				if (curSelectionRect != null && mouseDown != null) {
					Point curPoint = e.getPoint();

					drawSelectionRect(curSelectionRect);
					curSelectionRect.setRect(mouseDown.x, mouseDown.y, 0, 0);
					curSelectionRect.add(curPoint);
					drawSelectionRect(curSelectionRect);
		            mouseDragged = true;
				}
				return;
			}

			if (menuWasVisible)
				return;
			updateLocation(e.getPoint());
			if (!SwingUtilities.isRightMouseButton(e))
				proxy(e);
		}

		public void mouseWheelMoved(MouseWheelEvent e) {
			//don't pass any scroll events if scroll button is still pressed
			if(mouseBtn != -1){
				return;
			}
			if (ToolManager.getToolMode() == ToolManager.INVESTIGATE){
				InvestigateDisplay.scroll(e);
			}
			if (ToolManager.getToolMode() == ToolManager.ZOOM_IN //zoom in mode
					|| ToolManager.getToolMode() == ToolManager.ZOOM_OUT //zoom out mode
					|| ToolManager.getToolMode() == ToolManager.PAN_HAND ){//panner mode
			//Zoom In
				if (e.getWheelRotation()==-1){
					zoomAroundPoint(e, true);
				}//end zoom in
			//Zoom Out	
				if (e.getWheelRotation()==1){
					zoomAroundPoint(e, false);
				}//end zoom out
				
			}//end zoom if
			else{
				proxy(e);
			}
		}
	}
	
	
	private void drawLine(int x1, int y1,int x2, int y2){
		Graphics2D g2 = (Graphics2D) super.getGraphics();
		g2.transform(Main.testDriver.mainWindow.getProj().getWorldToScreen());
		g2 = Main.testDriver.mainWindow.wrapWorldGraphics(g2);
		
		prepare(g2);
		g2.setXORMode(Color.gray);
		g2.setStroke(new BasicStroke(0));
		Graphics2D g2s = Main.testDriver.mainWindow.getProj().createSpatialGraphics(g2);
		Point2D down = Main.testDriver.mainWindow.getProj().screen.toSpatial(x1, y1);
		Point2D curr = Main.testDriver.mainWindow.getProj().screen.toSpatial(x2, y2);
		g2s.draw(new Line2D.Double(down, curr));
		g2s.dispose();
	}
	
	 private static void prepare(Graphics2D g2)
     {
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
							RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
							RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
     }
	 
// Takes a point from the screen and turns it into a world point which can 
// be passed back into the location manager to change the location.	 
	private Point2D getWorldPt(MouseEvent e){
		 MouseEvent e1 = new MouseEvent((Component) e.getSource(), e
					.getID(), e.getWhen(), e.getModifiers(), e.getX(), e
					.getY(), e.getClickCount(), SwingUtilities
					.isRightMouseButton(e));
			double worldX = mainVMan.getProj().screen.toWorld(e1.getPoint()).getX();
			int scale = 360 * mainVMan.getZoomManager().getZoomPPD();
			e1.translatePoint(-(int) Math.floor(worldX / 360) * scale, 0);
			return mainVMan.getProj().screen.toWorld(e1.getPoint());
	}

	public void zoomAroundPoint(MouseEvent e, boolean in){
		//Get a location and zoom manager to zoom and recenter with
		ZoomManager zoomMgr = Main.testDriver.mainWindow.getZoomManager();
		LocationManager locMgr = Main.testDriver.mainWindow.getLocationManager();
		// zoom variables used to change zoom level
		int zoom = zoomMgr.getZoomPPD();
		int newZoom = -1;
		// location variables used to zoom in on mouse point
		Point2D latlon = getWorldPt(e);
		Point2D center = locMgr.getLoc();
		
		// Zooming
		if (in){
			//double the ppd to zoom in
			newZoom = zoom*2;
			//if already max zoom, stay there
			int idx = zoomMgr.getZoomFactors().size()-1;
			if(newZoom > zoomMgr.getZoomFactors().get(idx)){
				newZoom = zoomMgr.getZoomFactors().get(idx);
			}
		}else{
			//zoom won't go below 1ppd
			if(zoom%2==0){
				newZoom=zoom/2;
			}
		}
		//Maybe unnecessary? -- as long as zoom is set, rezoom.
		if(newZoom!=-1){
			zoomMgr.setZoomPPD(newZoom, false);
		}
		
		//Recentering (point under mouse stays the same)
		locMgr.setLocation(latlon, false);  //set mouse latlon at center
		Point2D newlatlon = getWorldPt(e); //find the new latlon under mouse
		double x = latlon.getX()-(newlatlon.getX()-latlon.getX());	//use the differences to calculate new center
		double y = latlon.getY()-(newlatlon.getY()-latlon.getY());
		center.setLocation(x, y);			//set new center
		locMgr.setLocation(center, true);		//recenter at new center

	}

//This method is used to determine how many buttons are being pressed at once...maybe
// not the best way to do this if there are ever more than three buttons defined in java.	
	public int getButtonCount(MouseEvent e){
		int mask = e.getModifiersEx();
		int btnCount = 0;
		if ((mask & InputEvent.BUTTON1_DOWN_MASK) == InputEvent.BUTTON1_DOWN_MASK){
			btnCount++;
		}
		if ((mask & InputEvent.BUTTON2_DOWN_MASK) == InputEvent.BUTTON2_DOWN_MASK){
			btnCount++;
		}
		if ((mask & InputEvent.BUTTON3_DOWN_MASK) == InputEvent.BUTTON3_DOWN_MASK){
			btnCount++;
		}
		return btnCount;
	}
}

