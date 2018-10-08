package edu.asu.jmars.layer.north;

import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.LViewFactory;
import edu.asu.jmars.layer.LayerParameters;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.layer.SerializedParameters;

public class NorthFactory extends LViewFactory{
	
	public NorthFactory(){
		type = "north_arrow";
	}

	@Override
	public LView recreateLView(SerializedParameters parmBlock) {
		if (parmBlock instanceof NorthSettings) {
			return buildLView((NorthSettings)parmBlock);
		} else {
			return buildLView(new NorthSettings());
		}
	}

	public String getName(){
		return "North Arrow";
	}
	
	
	public LView buildLView(NorthSettings parmBlock){
		NorthLayer layer = new NorthLayer();
		layer.initialLayerData = parmBlock;
		LView lview = new NorthLView(layer, parmBlock);
		lview.originatingFactory = this;
		return lview;
	}
	
	public void createLView(boolean async, LayerParameters lp){
		LView lview = buildLView(new NorthSettings());
		lview.setLayerParameters(lp);
		LManager.receiveNewLView(lview);
	}
	
}
