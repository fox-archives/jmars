package edu.asu.jmars.layer.investigate;

import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.LViewFactory;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.layer.LayerParameters;
import edu.asu.jmars.layer.SerializedParameters;
import edu.asu.jmars.layer.investigate.InvestigateLView.InvestigateParms;

public class InvestigateFactory extends LViewFactory{

	static boolean lviewExists = false;
	
	public Layer.LView createLView(){
		return null;
	}
	
	public void createLView(boolean async, LayerParameters l) {
		setLviewExists(true);
		InvestigateLayer layer = new InvestigateLayer();
		InvestigateLView view = new InvestigateLView(layer, true);
		view.originatingFactory = this;
		LManager.receiveNewLView(view);
	}
	
	
	public Layer.LView recreateLView(SerializedParameters parmBlock){
		InvestigateLView view;
		if(parmBlock instanceof InvestigateParms){
			InvestigateParms ip = (InvestigateParms) parmBlock;
			InvestigateLayer il = new InvestigateLayer(ip.dataSpikes, ip.dataProfiles);
			view = new InvestigateLView(il,true);
		}else{
			view = new InvestigateLView(new InvestigateLayer(), true);
		}
		setLviewExists(true);
		view.originatingFactory = this;
		return view;
	}
	
	
	public static void setLviewExists(boolean b){
		lviewExists = b;
	}
	public static boolean getLviewExists(){
		return lviewExists;
	}
	
	public String getName(){
		return "Investigate Layer";
	}
}
