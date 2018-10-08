package edu.asu.jmars.layer.crater;

import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.LViewFactory;
import edu.asu.jmars.layer.LayerParameters;
import edu.asu.jmars.layer.SerializedParameters;
import edu.asu.jmars.layer.Layer.LView;

public class CraterFactory extends LViewFactory {
	public CraterFactory(){
		type = "crater_counting";
	}
	
	public LView createLView() {
		return null;
	}

	public void createLView(boolean async, LayerParameters l) {
		// Create LView with defaults
		CraterLView lview = new CraterLView(new CraterLayer());
		lview.setLayerParameters(l);
		lview.originatingFactory = this;
		LManager.receiveNewLView(lview);
	}

	public LView recreateLView(SerializedParameters parmBlock) {
		CraterLayer craterLayer;
		
        if (parmBlock != null &&
                parmBlock instanceof CraterSettings) {
                CraterSettings settings = (CraterSettings) parmBlock;
                craterLayer = new CraterLayer(settings);
        } else {
        	craterLayer = new CraterLayer();
        }
	
		CraterLView lview = new CraterLView(craterLayer);
		lview.setLayerParameters(((CraterSettings)parmBlock).myLP);
		lview.originatingFactory = this;
		return lview;
	}

	public String getDesc() {
		return "Crater Counting";
	}

	public String getName() {
		return "Crater Counting";
	}

}
