package edu.asu.jmars.layer.mosaics;

import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.LViewFactory;
import edu.asu.jmars.layer.LayerParameters;
import edu.asu.jmars.layer.SerializedParameters;
import edu.asu.jmars.layer.Layer.LView;

public class MosaicsLViewFactory extends LViewFactory {
	public MosaicsLViewFactory(){
		super("Mosaics", "Displays blended mosaics of many images over areas of interest");
		type = "mosaic";
	}
	
	public LView createLView() {
		return null;
	}

	public void createLView(boolean async, LayerParameters l) {
		MosaicsLView lview = new MosaicsLView(new MosaicsLayer());
		lview.setLayerParameters(l);
		lview.originatingFactory = this;
		LManager.receiveNewLView(lview);
	}

	public LView recreateLView(SerializedParameters parmBlock) {
		MosaicsLView lview = new MosaicsLView(new MosaicsLayer());
		lview.originatingFactory = this;
		return lview;
	}

}
