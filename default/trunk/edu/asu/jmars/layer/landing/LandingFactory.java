package edu.asu.jmars.layer.landing;

import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.LViewFactory;
import edu.asu.jmars.layer.LayerParameters;
import edu.asu.jmars.layer.SerializedParameters;
import edu.asu.jmars.layer.Layer.LView;

public class LandingFactory extends LViewFactory {
	public LandingFactory(){
		type = "landing_site";
	}
	
	public LView createLView() {
		return null;
	}

	
	public void createLView(boolean async, LayerParameters l, String layerName, String configEntry){
		// Create LView with defaults
		LandingLView lview = new LandingLView(new LandingLayer(l, layerName, configEntry), true);
		lview.setLayerParameters(l);
		lview.originatingFactory = this;
		LManager.receiveNewLView(lview);
	}
	
	public void createLView(boolean async, LayerParameters l) {
		createLView(async, l, null, "landing2020");
	}

	public LView recreateLView(SerializedParameters parmBlock) {
		LandingLayer landingLayer;
		
        if (parmBlock != null &&
                parmBlock instanceof LandingSiteSettings) {
                LandingSiteSettings settings = (LandingSiteSettings) parmBlock;
                landingLayer = new LandingLayer(settings);
        } else {
        	landingLayer = new LandingLayer();
        }
	
		LandingLView lview = new LandingLView(landingLayer, true);
		lview.setLayerParameters(((LandingSiteSettings)parmBlock).myLP);
		lview.originatingFactory = this;
		return lview;
	}

	public String getDesc() {
		return "Landing Site";
	}

	public String getName() {
		return "Landing Site";
	}

}
