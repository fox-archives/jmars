package edu.asu.jmars.layer.stamp.radar;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.GeneralPath;

import edu.asu.jmars.layer.stamp.StampLView;
import edu.asu.jmars.layer.stamp.StampLayer;

public class FullResHighlightListener extends MouseMotionAdapter{

	private StampLView myLView;
	private Shape highlightProfile = null;
	
	public FullResHighlightListener(StampLView view){
		myLView = view;
	}
	
	
	public void paintHighlight(Graphics2D g2){
		if(getProfileLine() !=null && highlightProfile != null){
			g2.setStroke(myLView.getProj().getWorldStroke(4));
			g2.setColor(new Color(255,255,255,150));
			g2.draw(highlightProfile);
		}
	}
	
	private Shape getProfileLine(){
		Shape profileLine = null;
		
		StampLayer sl = myLView.stampLayer;
		
		if(sl.lineShapes() && sl.getSelectedStamps().size()>0){
			profileLine = myLView.stampLayer.getSelectedStamps().get(0).getPath();
		}
		
		return profileLine;
	}
	
	public void setHighlightPath(GeneralPath worldPath){
		if(worldPath != highlightProfile){
		
			highlightProfile = worldPath;
			myLView.repaint();
		}
	}
}
