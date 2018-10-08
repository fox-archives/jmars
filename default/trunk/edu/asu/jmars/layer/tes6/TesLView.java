package edu.asu.jmars.layer.tes6;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import edu.asu.jmars.layer.FocusPanel;
import edu.asu.jmars.layer.LViewManager;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.layer.SerializedParameters;
import edu.asu.jmars.layer.WrappedMouseEvent;
import edu.asu.jmars.swing.ColorMapOp;
import edu.asu.jmars.swing.ColorMapper;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.HVector;
import edu.asu.jmars.util.Util;


public class TesLView extends Layer.LView implements ContextEventListener, MouseListener, MouseMotionListener {
	private static final long serialVersionUID = 1L;
	
	private DebugLog log = DebugLog.instance();
    
    public TesLView(Layer layer){
        // Create a canvas
        super(layer);

        // Initialize various data items
        activeContext = null;

        // pieces of data associated with a context, one entry per context
        assocDataMap = new HashMap<TesContext,ContextAssocData>();

        selDialog = new JDialog((JFrame)null, selDialogTitle);
        selDialog.getContentPane().setLayout(new BorderLayout());
        selDialog.setSize(new Dimension(500,600));//updated the width to fit the combo box
        selDialog.getContentPane().addContainerListener(new ContainerListener(){
                public void componentAdded(ContainerEvent evt){
                    selDialog.getContentPane().validate();
                    selDialog.repaint();
                }
                public void componentRemoved(ContainerEvent evt){
                    selDialog.getContentPane().validate();
                    selDialog.repaint();
                }
            });
        // don't pack - it resizes the dialog
        // selDialog.pack();

        // Create hunt and peck mode selector context menu item
        huntAndPeckMenuItem = new JCheckBoxMenuItem("Hunt & Peck Mode");

        tesLayer = (TesLayer)layer;
        tesLayer.addContextEventListener(this);

        // set max contexts - see comment on contextAdded(ContextEvent e)
        //setBufferCount(tesLayer.getMaxContexts()*buffersPerCtx);
        setBufferCount(3);

        addMouseListener(this);
        addMouseMotionListener(this);
        
        populateContextsFromLayer();
    }

    // By default disable panner-view
	public boolean pannerStartEnabled() {
		return false;
	}

    private void populateContextsFromLayer(){
    	for(TesContext ctx: tesLayer.getContexts()){
    		contextAddedImpl(ctx);
    	}
    	contextActivatedImpl(tesLayer.getActiveContext());
    }

    public String getName(){
    	return "TES";
    }
    
    public void viewCleanup() {
    	tesLayer.setActiveContext(null);
    	selDialog.setVisible(false);
    	if (tesFocusPanel != null)
    		tesFocusPanel.viewCleanup();
		super.viewCleanup();
	}

	/*
	 * (non-Javadoc)
	 * @see edu.asu.jmars.layer.Layer.LView#getContextMenu(java.awt.geom.Point2D)
	 * Adds ShapeLayer functionality to the main and the panner view.
	 */
	protected Component[] getContextMenu(Point2D worldPt){
		return new Component[]{ huntAndPeckMenuItem };
	}

    /**
     * implementation of MouseListener.mouseClicked
     */
    public void mouseClicked(MouseEvent e){
        int clickCount = e.getClickCount();
        int buttonId = e.getButton();
        int toggleMask = InputEvent.CTRL_DOWN_MASK;
        boolean toggleSelection = (e.getModifiersEx() & toggleMask) == toggleMask;

        if (buttonId == MouseEvent.BUTTON1 && clickCount == 1){
        	Point2D wp = getProj().screen.toWorld(e.getPoint());

            if (activeContext != null){

                // show the window if it is hidden
            	log.println("Unhiding selDialog");
            	selDialog.setVisible(true);
            	//selDialog.setLocationRelativeTo(Main.getLManager());

                // get various pieces of data associated with this context
                ContextAssocData assocData = (ContextAssocData)assocDataMap.get(activeContext);
                // Clear unlocked records if hunt & peck mode is enabled.
                if (huntAndPeckMenuItem.isSelected())
                	assocData.selPanel.clearUnlockedRecords();

                // set the dialog waiting
                //SelectionPanel4 selPanel = assocData.selPanel;
                // selPanel.setWaiting(true);

                // spawn a thread to get the data, set the dialog not waiting at end
                if (t == null || !t.isAlive()){
                    t = new FetchThread(assocData, wp, toggleSelection, activeCtxImage);
                    t.start();
                }
                else {
                    Toolkit.getDefaultToolkit().beep();
                    log.println("Click thread is busy.");
                }

                // Sending it to the same worker as the draw request 
                // is not a good idea. The click may wait an eternity!
                // TesLViewWorker w = getWorker();
                // w.putSelectRequest(activeContext, activeContext.getCtxImage(), p);
            }
        }
        
        // Don't treat a mouse click as rubber-banding.
        rb1 = null; rb2 = null;
    }

    /**
     * implementation of MouseListener.mouseEntered
     */
    public void mouseEntered(MouseEvent e){}
    public void mouseExited(MouseEvent e){}
    public void mousePressed(MouseEvent e){
    	if (SwingUtilities.isLeftMouseButton(e)){
			rb1 = (e instanceof WrappedMouseEvent)? ((WrappedMouseEvent)e).getRealPoint(): e.getPoint();
    		rbStroke = new BasicStroke((float)(2.0/getPPD()));
    	}
    }
    public void mouseReleased(MouseEvent e){
    	if (SwingUtilities.isLeftMouseButton(e)){
    		Rectangle2D selectionBox = null;
    		if (rb1 != null){
    			rb2 = (e instanceof WrappedMouseEvent)? ((WrappedMouseEvent)e).getRealPoint(): e.getPoint();
    			if (!rb2.equals(rb1))
    				selectionBox = mkRectangle(ShapeUtils.screenToWorld(this, rb1), ShapeUtils.screenToWorld(this,rb2));
    		}
   			rb1 = null; rb2 = null;
   			repaint(); // Get rid of the rubber-band box
   			
   			if (selectionBox != null){
   	            if (activeContext != null){
   	  				int toggleMask = InputEvent.CTRL_DOWN_MASK;
   	  				boolean toggleSelection = (e.getModifiersEx() & toggleMask) == toggleMask;
   	  				
   	  				//TODO: fill data based on this selection
   	  				Set<RegionDesc> regions = this.getRegionsUnderViewport(selectionBox);
   	  				if (regions.size() > 4){
   	  					int result = JOptionPane.showConfirmDialog(this,
   	  							"Fulfilling this request can require obscene amount of time and space. Proceed?",
   	  							"Warning! Expensive Operation.",
   	  							JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
   	  					if (result != JOptionPane.OK_OPTION)
   	  						return;
   	  				}
   	  				
   	                // show the window if it is hidden
   	            	log.println("Unhiding selDialog");
   	            	selDialog.setVisible(true);
   	  						
   	  				// get various pieces of data associated with this context
   	  				ContextAssocData assocData = (ContextAssocData)assocDataMap.get(activeContext);
   	                // Clear unlocked records if hunt & peck mode is enabled.
   	                if (huntAndPeckMenuItem.isSelected())
   	                	assocData.selPanel.clearUnlockedRecords();

   	  				// spawn a thread to get the data, set the dialog not waiting at end
   	  				if (t == null || !t.isAlive()){
   	  					t = new FetchThread(assocData, selectionBox, toggleSelection, activeCtxImage);
   	  					t.start();
   	  				}
   	  				else {
   	  					Toolkit.getDefaultToolkit().beep();
   	  					log.println("Click thread is busy.");
   	  				}
   	  				
   	  				// TODO: We run into the same issue as drawing. What if the user
   	  				// had selected the entire planet.
   	            }
   			}
    	}
    }
    public void mouseMoved(MouseEvent e){}
    public void mouseDragged(MouseEvent e){
    	if (SwingUtilities.isLeftMouseButton(e)){
    		if (rb1 != null){
    			rb2 = (e instanceof WrappedMouseEvent)? ((WrappedMouseEvent)e).getRealPoint(): e.getPoint();
    			repaint(); // Draw the rubber-band box
    		}
    	}
    }
    
    private Rectangle2D mkRectangle(Point2D p1, Point2D p2){
    	double x = Math.min(p1.getX(), p2.getX());
    	double y = Math.min(p1.getY(), p2.getY());
    	double w = Math.abs(p1.getX()-p2.getX());
    	double h = Math.abs(p1.getY()-p2.getY());
    	
    	return new Rectangle2D.Double(x,y,w,h);
    }
    
    
    private Rectangle2D cachedVpBBox = null;

    // implementation of abstract LView.createRequest(Rectangle2D where)
    protected Object createRequest(Rectangle2D where){
    	// A viewChange event has occurred:
    	// clear the cached viewport bounding box.
    	cachedVpBBox = null;
    	
    	// put in a request to redraw
        TesLViewWorker w = getWorker();

        if (activeContext != null){
            w.putDrawRequest(activeContext, activeContext.getCtxImage(), getViewport());
        }
        return null;
    }

    // implementation of abstract LView.receiveData(Object layerData)
    public void receiveData(Object layerData){ }

    // implementation of abstract LVIew._new()
    protected Layer.LView _new(){
        return new TesLView(getLayer());
    }

    public FocusPanel getFocusPanel(){
        if (focusPanel == null){
            focusPanel = new FocusPanel(this,false);
            tesFocusPanel = new TesFocusPanel(this);
            focusPanel.add("Adjustments", tesFocusPanel);
        }
        return focusPanel;
    }
    
    public void repaintSelection(boolean cascade){
    	TesContext ctx = getContext();
    	if (getSelectionBufferIndex(ctx) > -1){
    		clearOffScreenSelectionBuffer(ctx);
        	Graphics2D g2 = getOffScreenSelectionBuffer(ctx);
        	//g2.fill(getProj().getWorldWindow());//new Rectangle2D.Double(-100,-100,100,100));
    		ContextAssocData ad = (ContextAssocData)assocDataMap.get(ctx);
    		if (ad != null)
    			ad.selPanel.drawHighlightedPolys(g2, this);
    		repaint();
    	}
    	if (cascade && getChild() != null)
    		((TesLView)getChild()).repaintSelection(cascade);
    }

    private void contextDeactivatedImpl(TesContext deactivatedContext){
        if (deactivatedContext != null){
            int dataBuffIdx = getDataBufferIndex(deactivatedContext);
            int gridBuffIdx = getGridBufferIndex(deactivatedContext);
            int selBuffIdx = getSelectionBufferIndex(deactivatedContext);

            if (dataBuffIdx > -1 && gridBuffIdx > -1 && selBuffIdx > -1){
                setBufferVisible(dataBuffIdx, false);
                setBufferVisible(gridBuffIdx, false);
                setBufferVisible(selBuffIdx, false);
            }

            ContextAssocData assocData = (ContextAssocData)assocDataMap.get(deactivatedContext);
            if (assocData != null){
            	SelectionPanel4 selPanel = assocData.selPanel;
            	if (selPanel != null){
            		log.println("SelectionPanel4 is not null for "+deactivatedContext.getTitle());
            		selDialog.getContentPane().remove(selPanel);
            	}
            	else {
            		log.println("SelectionPanel4 is null for "+deactivatedContext.getTitle());
            	}
            }
            else {
            	log.println("Associated data was null for "+deactivatedContext.getTitle());
            }
        }

        // set the dialog to default
        // selDialog.setTitle(selDialogTitle);
        refreshDialogTitle();
    }
    
    private void contextActivatedImpl(TesContext ctx){
        activeContext = ctx;
        if (activeContext != null){
            activeCtxImage = activeContext.getCtxImage();
        }
        else {
            activeCtxImage = null;
        }


        if (activeContext != null){
            int dataBuffIdx = getDataBufferIndex(activeContext);
            int gridBuffIdx = getGridBufferIndex(activeContext);
            int selBuffIdx = getSelectionBufferIndex(activeContext);
            
            if (dataBuffIdx > -1 && gridBuffIdx > -1 && selBuffIdx > -1){
                setBufferVisible(dataBuffIdx, true);
                setBufferVisible(gridBuffIdx, true);
                setBufferVisible(selBuffIdx, true);
            }

            // sumbit a new draw request
            // TODO: figure out if we will ever need ctx, ctxImage seems to be enough
            if (isEnabled() && isAlive()){
                TesLViewWorker w = getWorker();
            	w.putDrawRequest(activeContext, activeContext.getCtxImage(), getViewport());
            }

            ContextAssocData assocData = (ContextAssocData)assocDataMap.get(activeContext);
            SelectionPanel4 selPanel = assocData.selPanel;
            if (selPanel != null){
                log.println("SelectionPanel4 is not null for "+activeContext.getTitle());
                selDialog.getContentPane().add(selPanel, BorderLayout.CENTER);
            }
            else {
                log.println("SelectionPanel4 is null for "+activeContext.getTitle());
            }
            
            // selDialog.setTitle(selDialogTitleFor+activeContext.getTitle());
            refreshDialogTitle();
        }
    }
    
    public void contextActivated(ContextEvent e){
    	// deactivate previously active context
        contextDeactivatedImpl(e.getOldContext());

        // activate new context
        contextActivatedImpl(e.getContext());
        
        repaint();
    }
    
    public void paintComponent(Graphics g){
    	super.paintComponent(g);
    	
    	// Overlay with selection box.
    	if (rb1 != null && rb2 != null){
    		Graphics2D g2 = (Graphics2D)g;
    		g2.setColor(rbColor);
    		g2.setStroke(rbStroke);
    		g2.draw(mkRectangle(rb1, rb2));
    	}
    }
    
    /**
     * TODO: This logic needs to be futher thought out.
     * Overloaded the LView.realPaintComponent, so that the
     * ColorMapOp is applied only to the data buffer but not
     * to either the selection buffer, or the grid buffer.
     */
    public void realPaintComponent(Graphics g){
    	int n = getBufferCount();
    	if(n <= 0)
    		return;

    	Graphics2D g2 = (Graphics2D) g;
    	float alpha = getAlpha();
    	
    	// TODO check here for incorrect coloring on opacity 0
    	for(int i=0; i<n; i++)
    		if(getBufferVisible(i)){

    			BufferedImage buffer = getBuffer(i);
    			
    			if (buffer == null)
    				continue;
    			
    			buffer.coerceData(false);
    			
    			ColorMapper cmap = tesFocusPanel.getColorMap();

    			ColorMapOp colorMapOp;
    			
    			if (cmap==null) {
    				colorMapOp = new ColorMapOp(buffer);
    			} else {
    				colorMapOp = cmap.getState().getColorMapOp(buffer);
    			}
    	    	
    	    	boolean simpleDraw = colorMapOp.isIdentity()  &&  alpha == 1.0;
    			
    			if(simpleDraw || i != dataBufferOffset){
    				g.drawImage(buffer, 0, 0, null);
    			}
    			else {
    				// g2.drawImage(buffer, colorMapOp.forAlpha(alpha), 0, 0);
    				// The above line stopped working in Java 7 on Mac OS. It worked
    				// just fine on Java 6,1.5,1.4 and still works on non-Mac OS
    				// platforms. My two suspicion was on change of image type, e.g
    				// RGBA vs. ARGB vs. AGBR and its relationship to LookupOp's band
    				// order. LookupOp's band order did change on Mac OS from Java 6
    				// to 7. However, the new order matched other platforms.
    				// The following replacement line works, but an explanation as
    				// to why it works vs. the original line is unknown.
//					g2.drawImage(colorMapOp.forAlpha(alpha).filter(buffer, null), 
//							0,0, new Color(0,0,0,0), null);
//    				g2.drawImage(buffer, colorMapOp.forAlpha(alpha), 0, 0);
    				// The above line stopped working on MacOS where drawImage() filters the image
    				// however, prefiltering the image (in the following line) appears to work
    				g2.drawImage(colorMapOp.forAlpha(alpha).filter(buffer, null), 0, 0, null);

    			}
    		}
    }


    public void contextUpdated(ContextEvent e){
        log.println("in TesLView.contextUpdated with whatChanged="+e.getWhatChanged());

        TesContext ctx = e.getContext();

        ContextAssocData assocData = (ContextAssocData)assocDataMap.get(ctx);
        SelectionPanel4 selPanel = null;
        if (assocData != null){
        	selPanel = assocData.selPanel;
        	selPanel.updateLayout(ctx);
        }
        
        // If the received event is not about the currently
        // active context, do nothing.
        if (ctx != activeContext){
            log.println("TesLView.contextUpdated: ctx != activeContext");
            return;
        }

        // Nothing to do when no active context.
        if (activeContext == null){
            return;
        }
        activeCtxImage = activeContext.getCtxImage();
        refreshDialogTitle();

        // Otherwise, submit appropriate requests to the worker
        TesLViewWorker w = getWorker();
        int whatChanged = e.getWhatChanged();
        
        if ((whatChanged & ContextEvent.SELECTS_CHANGED) != 0 ||
            (whatChanged & ContextEvent.ORDERBY_CHANGED) != 0 ||
            (whatChanged & ContextEvent.COLORBY_CHANGED) != 0 || 
			(whatChanged & ContextEvent.DRAWREAL_CHANGED) != 0 ||
			(whatChanged & ContextEvent.DRAWNULL_CHANGED) != 0){

        	if (isAlive()){
        		log.println("contextUpdated - selects/orderby/colorby/drawreal/drawnull changed");
        		w.putDrawRequest(activeContext, activeContext.getCtxImage(), getViewport());
        	}
        	else{
        		log.println("contextUpdated - discarding request since LView is inactive.");
        	}
        }
            
        if ((whatChanged & ContextEvent.SELECTS_CHANGED) != 0 ||
                (whatChanged & ContextEvent.ORDERBY_CHANGED) != 0){

                log.println("contextUpdated - selects/orderby changed");
                selPanel.removeAll();
        }
    }

    private void contextAddedImpl(TesContext addedContext){
        // add the context to the list of LView's recollection of contexts
        ctxs.add(addedContext);

        //int buffCount = tesLayer.getContextCount()*buffersPerCtx;
        //log.println("Setting new buffer count to: "+buffCount);
        // setBufferCount is not supposed to be called after view init
        // setBufferCount(buffCount);

        log.println("Adding a new dialog to the map for "+addedContext.getCtxImage().getTitle());
        SelectionPanel4 d = new SelectionPanel4(this, addedContext);

        ContextAssocData assocData = new ContextAssocData(d);
        assocDataMap.put(addedContext, assocData);
    }
    
    // We have a pair of buffers (data,grid) per context. The number
    // of buffers cannot be changed on the fly. Hence, this number
    // is fixed at LView creation.
    public void contextAdded(ContextEvent e){
    	contextAddedImpl(e.getContext());
    }
    public void contextDeleted(ContextEvent e){
        //int buffCount = tesLayer.getContextCount()*buffersPerCtx; 
        //log.println("Setting new buffer count to: "+buffCount);
        // setBufferCount is not supposed to be called after view init
        // setBufferCount(buffCount);
        
        if (e.getContext() == activeContext){
            // selDialog.setTitle(selDialogTitle);
            ContextAssocData assocData = (ContextAssocData)assocDataMap.get(e.getContext());
            SelectionPanel4 selPanel = assocData.selPanel;
            selDialog.getContentPane().remove(selPanel);


            // Hide the buffers for the activeContext.
            int dataBuffIdx = getDataBufferIndex(activeContext);
            int gridBuffIdx = getGridBufferIndex(activeContext);

            log.println("deleting context: "+activeContext.getTitle()+
                        "  dataBuffIdx: "+dataBuffIdx+
                        "  gridBuffIdx: "+gridBuffIdx);
            if (dataBuffIdx > -1 && gridBuffIdx > -1){
                setBufferVisible(dataBuffIdx, false);
                setBufferVisible(gridBuffIdx, false);
                repaint();
            }
            refreshDialogTitle();
        }

        // Update LView's recollection of what contexts are defined.
        assocDataMap.remove(e.getContext());
        ctxs.remove(e.getContext());
    }
    
    /**
     * Returns the active context.
     * @return the active context or <code>null</code> if there is none.
     */
    public TesContext getContext(){
        return activeContext;
    }

    public LViewManager getViewMan(){ return viewman; }

    public Rectangle2D getViewport(){
    	if (cachedVpBBox == null){
    		cachedVpBBox = getProj().getWorldWindow();
    	}
    	
    	return cachedVpBBox;
    }
    
	public Set<RegionDesc> getRegionsUnderViewport(Rectangle2D worldVp){
		log.println("worldVp: "+worldVp);
		
		HashSet<RegionDesc> s = new HashSet<RegionDesc>(); // selected regions
		Iterator<RegionDesc> ri = TesLayer.getRegionDescMap().values().iterator();
		while(ri.hasNext()){
			RegionDesc r = ri.next();
//			Rectangle2D regionBbox = r.getRegionBoundary().getBounds2D();
			
			Rectangle2D.Double wr = new Rectangle2D.Double();
//			wr.setFrameFromDiagonal(
//					getProj().spatial.toWorld(360.0-regionBbox.getMinX(), regionBbox.getMinY()),
//					getProj().spatial.toWorld(360.0-regionBbox.getMaxX(), regionBbox.getMaxY()));
//			wr.setFrame(Util.normalize360(wr).getBounds2D());
			Rectangle2D regionBbox2 = ShapeUtils.spatialEastLonPolyToWorldPoly(this, r.getRegionBoundary()).getBounds2D();
			wr.setFrame(regionBbox2);
			wr.x -= Math.floor(wr.x / 360) * 360;
			
			Rectangle2D.Double wr360 = new Rectangle2D.Double();
			wr360.setFrame(wr);
			wr360.x += 360;

			Rectangle2D.Double wr_360 = new Rectangle2D.Double();
			wr_360.setFrame(wr);
			wr_360.x -= 360;
			
			if (wr.intersects(worldVp) || wr360.intersects(worldVp) || wr_360.intersects(worldVp))
				s.add(r);
		}

		if (log.isActive())
			printRegionMask(s);
		
		return s;
	}
		
	private void printRegionMask(Set<RegionDesc> regions){
		int x = (int)Math.ceil(360.0/5.0);
		int y = (int)Math.ceil(180.0/5.0);
		boolean[][] mask = new boolean[x][y];

		int regionId, rx, ry;
		
		for(RegionDesc r: regions){
			regionId = r.getRegionId().intValue();
			rx = (int)((regionId & 0xFFFF0000L) >>> 16);
			ry = (int)((regionId & 0x0000FFFFL));
			
			mask[rx][ry] = true;
		}
		
		StringBuffer sbuf = new StringBuffer();
		sbuf.append("region mask ["+(getChild()==null?"panner":"main")+"]:\n");
		for(int j = 0; j < y; j++){
			for(int i = 0; i < x; i++){
				sbuf.append(mask[i][y-1-j]?'+':'.');
			}
			sbuf.append("\n");
		}
		sbuf.append("\n");
		log.println(sbuf);
	}

    public double getPPD(){
		return (1.0/getProj().getPixelSize().getWidth());
	}

    
    private synchronized TesLViewWorker getWorker(){
        // It so happens that at the time of creation of the LView
        // it is not known whether the LView is Main or Panner LView.
        // We would like to differentiate between worker threads 
        // responsible for the two views for the sake of debugging
        // in jdb. Hence, we create the worker threads lazily, by
        // which time the Main/FocusPanel behaviour has been resolved.
        
        if (worker == null){
            worker = new TesLViewWorker(this);
            int priOffset = 10;
            try {
            	if ((worker.getPriority()-priOffset) < Thread.MIN_PRIORITY){
            		worker.setPriority(Thread.MIN_PRIORITY+1);
            	}
            	else {
            		worker.setPriority(worker.getPriority()-priOffset);
            	}
            }
            catch(IllegalArgumentException ex){ log.aprintln(ex); }
            catch(SecurityException ex){ log.aprintln(ex); }
            worker.start();
        }

        return worker;
    }

    public int getDataBufferIndex(TesContext ctx){
    	return dataBufferOffset;
    }

    public int getGridBufferIndex(TesContext ctx){
    	return gridBufferOffset;
    }
    
    public int getSelectionBufferIndex(TesContext ctx){
    	return selectionBufferOffset;
    }

    public void clearOffScreenDataBuffer(TesContext ctx){
        int idx = getDataBufferIndex(ctx);

        if (idx > -1){
            clearOffScreen(idx);
        }
        else {
            log.println("Clear for a non existing context:"+ctx.getTitle());
        }
    }

    public void clearOffScreenGridBuffer(TesContext ctx){
        int idx = getGridBufferIndex(ctx);

        if (idx > -1){
            clearOffScreen(idx);
        }
        else {
            log.println("Clear for a non existing context:"+ctx.getTitle());
        }
    }

    public void clearOffScreenSelectionBuffer(TesContext ctx){
    	int idx = getSelectionBufferIndex(ctx);
    	
    	if (idx > -1)
    		clearOffScreen(idx);
    	else
    		log.println("Clear for a non existing context:"+ctx.getTitle());
    }
    
    public Graphics2D getOffScreenDataBuffer(TesContext ctx){
        int idx = getDataBufferIndex(ctx);

        if (idx > -1){
            return getOffScreenG2(idx);
        }
        return null;
    }

    public Graphics2D getOffScreenGridBuffer(TesContext ctx){
        int idx = getGridBufferIndex(ctx);
        if (idx > -1){
            return getOffScreenG2(idx);
        }
        return null;
    }
    
    public Graphics2D getOffScreenSelectionBuffer(TesContext ctx){
    	int idx = getSelectionBufferIndex(ctx);
    	
    	if (idx > -1)
    		return getOffScreenG2(idx);
    	
    	return null;
    }

    public void refreshDialogTitle(){
        ContextAssocData assocData = (ContextAssocData)assocDataMap.get(activeContext);
        String title = selDialogTitle;

        if (assocData != null){
            title += ":"+activeContext.getTitle();
            title += (assocData.isFetching? ": Fetching ...": "");
        }
        selDialog.setTitle(title);
    }

    public TesKey[] getKeysUnderPointInOrder(
    		String whereClause,
    		String orderByClause,
    		RegionDesc regionDesc,
    		boolean interpolatedPolys,
    		Point2D wPoint
    ) throws SQLException {
    	
    	Map<TesKey,SerializablePoly> data = tesLayer.getPolysInRegionInOrder(whereClause, orderByClause, regionDesc, interpolatedPolys);
    	Set<TesKey> keys = new LinkedHashSet<TesKey>(data.size());
    	
    	Point2D p = ShapeUtils.worldToSpatialEastLon(this, wPoint);
    	for(TesKey key: data.keySet()){
    		SerializablePoly poly = data.get(key);
    		if (poly != null && poly.contains(p))
    			keys.add(key);
    	}
    	
    	return keys.toArray(new TesKey[0]);
    }

    public TesKey[] getKeysUnderRectInOrder(
    		String whereClause,
    		String orderByClause,
    		RegionDesc regionDesc,
    		boolean interpolatedPolys,
    		Rectangle2D wRect
    ) throws SQLException {

    	Map<TesKey,SerializablePoly> data = tesLayer.getPolysInRegionInOrder(whereClause, orderByClause, regionDesc, interpolatedPolys);
    	Set<TesKey> keys = new LinkedHashSet<TesKey>(data.size());

    	for(TesKey key: data.keySet()){
    		SerializablePoly poly = data.get(key);
    		if (poly != null){
    			SerializablePoly wPoly = ShapeUtils.spatialEastLonPolyToWorldPoly(this, poly);
    			if (Util.intersects360(wRect, new Shape[]{ wPoly }).length > 0)
    				keys.add(key);
    		}
    	}

    	return keys.toArray(new TesKey[0]);
    }
    
	public SerializedParameters getInitialLayerData() {
		return new InitialParams(tesLayer.getContexts(), getContext());
	}


	// Hunt-and-peck selection mode selector
    JCheckBoxMenuItem huntAndPeckMenuItem;
    
    // Rubber-band selector
    private Point2D rb1 = null, rb2 = null;
    private Color  rbColor = Color.YELLOW;
    private Stroke rbStroke = null;

    // a singleton click-data fetch thread
    FetchThread t = null;

    // one entry per context
    private Map<TesContext,ContextAssocData> assocDataMap;

    // Main dialog window which holds one SelectionPanel4
    // at a time.
    private JDialog selDialog;

    // Maintain a list of contexts so that we can get buffer
    // indices correctly.
    private Vector<TesContext> ctxs = new Vector<TesContext>();

    
    private TesContext activeContext;
    private TesContext.CtxImage activeCtxImage;
    private TesLayer   tesLayer;
    private TesFocusPanel tesFocusPanel;
    private TesLViewWorker worker = null; // don't use directly use getWorker() instead

    // We have three buffers per ctx, first one for drawing foot-prints, 
    // second one for drawing selections, and third one for the region-grid.
    //private static final int buffersPerCtx = 3;
    private static final int dataBufferOffset = 0;
    private static final int selectionBufferOffset = 1;
    private static final int gridBufferOffset = 2;

    private static final String selDialogTitle = "Selection";
    //private static final String selDialogTitleFor = selDialogTitle+": ";

    /**
     * Pixel Per Degree resolutions at which various kinds of 
     * data is draw. For example, as a default, foot-print centers
     * are drawn at 16ppd or lower, detector centers are drawn at
     * 32ppd or lower, interpolated polygonal detectors are drawn
     * at 128ppd or lower, and real detector polygons are pulled
     * from the database and drawn at 256ppd. Precedence to what
     * is drawn is given in the aforementioned order, i.e. if two
     * of these values, say, foot-print centers, and detector centers
     * are set to 16ppd, then foot-print centers will be printed
     * and the next step up will be interpolated polygonal data.
     */
    public static final int MAX_PPD_FP_CENTERS = Config.get("tes.maxPpdDrawFpCenters",16);
    public static final int MAX_PPD_DET_CENTERS = Config.get("tes.maxPpdDrawDetectorCenters",32);
    public static final int MAX_PPD_INTERP_POLYS = Config.get("tes.maxPpdDrawInterpolatedPolys",128);
    public static final int MAX_PPD_REAL_POLYS = Config.get("tes.maxPpdDrawRealPolys",256);
    
    
    

    // Context's Associated Data.
    private class ContextAssocData {
		public ContextAssocData(SelectionPanel4 p){
            selPanel = p;
            isFetching = false;
            //keys = new HashSet<TesKey>();
        }
        private SelectionPanel4 selPanel;
        private boolean isFetching; // This panel is busy fetching data
        //private Set<TesKey> keys;
        // private RefreshThread t;
    }

    public static class ColorKeeper {
        private final Color[] colors = new Color[]{
            	Color.black,
            	Color.red,
            	Util.green3,
            	Color.blue,
            	Util.darkViolet,
            	Util.darkOrange,
            	Util.cyan3,
            	Color.magenta,
            	Util.chocolate4,
            	Util.maroon,
            	Util.yellow3,
            	Util.gray50,
            };
        
        private int colorIndex = 0;
    	
        public synchronized Color nextColor(){
        	Color c = colors[colorIndex];
        	colorIndex = (colorIndex + 1) % colors.length;
        	return c;
        }
        
        public synchronized void resetColorIndex(){
        	colorIndex = 0;
        }
    }
    
    ColorKeeper colorKeeper = new ColorKeeper();
    
    public ColorKeeper getColorKeeper(){
    	return colorKeeper;
    }
    
    // TODO: This thread is in the wrong place, it should be in the
    // selection panel instead.
    // Table data fetch thread, responsible for pulling data and populating
    // the selection-list table.
    private class FetchThread extends Thread{
        public FetchThread(
            ContextAssocData assocData,
            Point2D wPoint, boolean toggle,
            TesContext.CtxImage ctxImage)
        {
            super();
            this.assocData = assocData;
            this.selPanel = assocData.selPanel;
            this.point = wPoint;
            this.rect = null;
            this.toggle = toggle;
            this.ctxImage = ctxImage;

            //selPanel.setWaiting(true);
            assocData.isFetching = true;
            refreshDialogTitle();
        }
        
        public FetchThread(
                ContextAssocData assocData,
                Rectangle2D wRect, boolean toggle,
                TesContext.CtxImage ctxImage)
            {
                super();
                this.assocData = assocData;
                this.selPanel = assocData.selPanel;
                this.point = null;
                this.rect = wRect;
                this.toggle = toggle;
                this.ctxImage = ctxImage;

                //selPanel.setWaiting(true);
                assocData.isFetching = true;
                refreshDialogTitle();
            }
        
        public void run(){
            try {
                // Get the data in a new thread.
            	Set<RegionDesc> regions;
            	if (point != null)
            		regions = tesLayer.getRegionsUnderPoint(ShapeUtils.worldToSpatialEastLon(TesLView.this, point));
            	else
            		regions = getRegionsUnderViewport(rect);
                log.println("FetchThread found regions: "+ regions);

                data = new TreeMap<TesKey, Map<FieldDesc,Object>>(new TesKeyIdComparator());
                for(RegionDesc region: regions){
                	TesKey[] keys;
                	
                	if (point != null)
                		keys = getKeysUnderPointInOrder(
                				ctxImage.getWhereClause(), ctxImage.getOrderByClause(),
                				region, !ctxImage.getDrawReal().booleanValue(), point);
                	else
                		keys = getKeysUnderRectInOrder(
                				ctxImage.getWhereClause(), ctxImage.getOrderByClause(),
                				region, !ctxImage.getDrawReal().booleanValue(), rect);
                	

                    if (keys != null){
                        log.println("FetchThread found "+keys.length+" keys for region "+region);
                    }

                    List<FieldDesc> essFields = new ArrayList<FieldDesc>();
                    essFields.add(tesLayer.getOckField());
                    essFields.add(tesLayer.getIckField());
                    essFields.add(tesLayer.getDetField());
                    // Add scan-length to the record before plotting since we'll need it to figure out spectral-field's x-axis
                    if (Utils.containsSpectralFields(Arrays.asList(ctxImage.getFields())))
                    	essFields.add(tesLayer.getScanLenField());
                    //essFields.add(tesLayer.getPolyField());
                    
                    boolean interpolate = (getPPD() <= TesLView.MAX_PPD_INTERP_POLYS) && !ctxImage.getDrawReal().booleanValue();
                    
                    for(int i = 0; i < keys.length; i++){
                    	HashMap<FieldDesc,Object> rec = new HashMap<FieldDesc,Object>();
                        for(FieldDesc field: essFields){
                        	rec.put(field, tesLayer.getFieldData(keys[i], field));
                        }
                        
                        rec.put(tesLayer.getPolyField(), tesLayer.getPolyData(keys[i], interpolate));
                        rec.put(tesLayer.getColorField(), colorKeeper.nextColor());
                        
                        data.put(keys[i], rec);
                        
                    }
                }
            }
            catch(SQLException ex){
                ex.printStackTrace();
            }
            finally{
            	// Put the data in the UI component in the Swing Thread.
            	SwingUtilities.invokeLater(new Runnable(){
            		public void run(){
            			try {
            				selPanel.addRecs(data);
            				selPanel.selectRecs(data.keySet(), toggle);
            				//for(TesKey key: data.keySet()){
            				//	selPanel.addRec(key, data.get(key));
            				//	selPanel.selectRec(key, toggle);
            				//}
            				
            				// tell selection panel to update various data
            				// fields attached to the key
            				FieldDesc[] userFields = ctxImage.getFields();
            				for(int j = 0; j < userFields.length; j++){
            					selPanel.requestUpdate(data.keySet(), userFields[j]);
            				}
            			}
            			finally {
            				//selPanel.setWaiting(false);
            				assocData.isFetching = false;
            				refreshDialogTitle();
            			}
            		}
            	});
            }
        }
        
        ContextAssocData assocData;
        SelectionPanel4 selPanel;
        Point2D point;
        Rectangle2D rect;
        boolean toggle;
        TesContext.CtxImage ctxImage;
        Map<TesKey, Map<FieldDesc,Object>> data;
    }

    /**
     * Layer restore parameters for saving/restoring from ".jmars" file.
     */
	static class InitialParams implements SerializedParameters, Serializable {
		static final long serialVersionUID = 1L;
		
		private ArrayList<TesContext> contexts;
		private int selectedIndex;
		
		public InitialParams(List<TesContext> contexts, TesContext selectedContext){
			this.contexts = new ArrayList<TesContext>(contexts);
			this.selectedIndex = this.contexts.indexOf(selectedContext);
		}
		
		public List<TesContext> getContexts(){
			return Collections.unmodifiableList(contexts);
		}
		
		public int getSelectedIndex(){
			return selectedIndex;
		}
		
		public TesContext getSelectedContext(){
			if (selectedIndex < 0)
				return null;
			return contexts.get(selectedIndex);
		}
		
		private void writeObject(ObjectOutputStream out) throws IOException {
			out.defaultWriteObject();
		}
		
		private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
			in.defaultReadObject();
		}
	}
	
//The following two methods are used to query for the
// info panel fields (description, citation, etc)	
 	public String getLayerKey(){
 		return "TES";
 	}
 	public String getLayerType(){
 		return "tes";
 	}
}
