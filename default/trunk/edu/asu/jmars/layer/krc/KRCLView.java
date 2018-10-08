package edu.asu.jmars.layer.krc;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.layer.LayerParameters;

public class KRCLView extends LView{
	private boolean isMain;
	private KRCFocusPanel myFocusPanel;
	
	public KRCLView(KRCLayer layer, boolean isMainView, LayerParameters lp) {
		super(layer);
		setLayerParameters(lp);
		isMain = isMainView;
		if(isMainView){
			addMouseListener(new DrawingListener(this));
		}
	}

	@Override
	protected Object createRequest(Rectangle2D where) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void receiveData(Object layerData) {
		// TODO Auto-generated method stub
		
	}

	@Override
	//Used for creating a copy for the panner
	protected LView _new() {
		return new KRCLView((KRCLayer)getLayer(), false, null);
	}
	
	public String getName(){
		return "KRC Layer";
	}
	
	
	public synchronized void paintComponent(Graphics g){
		//TODO: do something with hi res export?
		
		//Don't draw unless visible
		if(!isVisible() || viewman == null){
			return;
		}
		
		clearOffScreen();
		
		//used for points
		Graphics2D g2 = getOffScreenG2();
		if(g2 == null){
			return;
		}
		g2 = viewman.wrapWorldGraphics(g2);
		g2.setStroke(new BasicStroke(0));
		
		//used for labels
		Graphics g2lbl = getOffScreenG2Direct();
		if(g2lbl == null){
			return;
		}
		
		//draw krc data points
		for(KRCDataPoint krcdp : ((KRCLayer)getLayer()).getKRCDataPoints()){
			//only draw points that aren't hidden
			Point2D spPt = krcdp.getPoint();
			Point2D wPt = getProj().spatial.toWorld(spPt);
			Point2D scPt = getProj().world.toScreen(wPt);

			if(krcdp.showPoint()){
				Point2D scTL = new Point2D.Double(scPt.getX()-4, scPt.getY()-4);
				Point2D scBR = new Point2D.Double(scPt.getX()+4, scPt.getY()+4);

				//Convert to world
				Point2D wTL = getProj().screen.toWorld(scTL);
				Point2D wBR = getProj().screen.toWorld(scBR);

				
				//display in world coordinates
				double height = wTL.getY() - wBR.getY();	
				double width = wBR.getX() - wTL.getX(); 
				
				//get shape style from ds and create shape to display
				//circle by default
				Shape shp = new Ellipse2D.Double(wPt.getX()-width/2, wPt.getY()-height/2, width, height);;
				g2.setColor(krcdp.getFillColor());
				g2.fill(shp);

				//if this is the selected data point, color the outline yellow
				if(getFocusPanel() != null && krcdp == getFocusPanel().getSelectedDataPoint()){
					g2.setColor(Color.YELLOW);
					g2.draw(shp);
				}else{
				//else color it by its settings
					g2.setColor(krcdp.getOutlineColor());
					g2.draw(shp);
				}
			}
			if(krcdp.showLabel()){
				Point2D lblPt = new Point2D.Double(scPt.getX()+2, scPt.getY()+15);
				
				Font labelFont = new Font("Arial", Font.BOLD, krcdp.getFontSize());
				g2lbl.setFont(labelFont);
				//if it's selected color the label yellow, else use it's settings
				if(getFocusPanel() != null && krcdp == getFocusPanel().getSelectedDataPoint()){
					g2lbl.setColor(Color.YELLOW);
				}else{
					g2lbl.setColor(krcdp.getLabelColor());
				}
				
				g2lbl.drawString(krcdp.getName(), (int)lblPt.getX(), (int)lblPt.getY());
			}
		}
		
		// super.paintComponent draws the back buffers onto the layer panel
		super.paintComponent(g);	
	}
	
	public KRCFocusPanel getFocusPanel(){
		//Do not create fp for the panner
		if(!isMain){
			return null;
		}
		if(focusPanel == null || myFocusPanel ==  null){
			focusPanel = myFocusPanel = new KRCFocusPanel(KRCLView.this);
		}
		return myFocusPanel;
	}

}
