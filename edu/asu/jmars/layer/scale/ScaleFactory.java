package edu.asu.jmars.layer.scale;

import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.LViewFactory;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.layer.LayerParameters;
import edu.asu.jmars.layer.SerializedParameters;
import edu.asu.jmars.layer.Layer.LView;

public class ScaleFactory extends LViewFactory {
	
	public ScaleFactory(){
		type = "scalebar";
	}
	
	public LView createLView() {
		return null;
	}

	public void createLView(boolean async, LayerParameters l) {
		LView view = realCreateLView(new ScaleParameters());
		view.setLayerParameters(l);
		LManager.receiveNewLView(view);
	}

	private Layer.LView realCreateLView(ScaleParameters params) {
		ScaleLayer layer = new ScaleLayer();
		layer.initialLayerData = params;
		Layer.LView view = new ScaleLView(true, layer, params);
		view.originatingFactory = this;
		view.setVisible(true);
		return view;
	}

	public String getName() {
		return ("Map Scalebar");
	}

	public String getDesc() {
		return ("A layer which provides a scale marker on the map");
	}

	public LView recreateLView(SerializedParameters parmBlock) {
		if (parmBlock instanceof ScaleParameters) {
			return realCreateLView((ScaleParameters)parmBlock);
		} else {
			return realCreateLView(new ScaleParameters());
		}
	}
}
