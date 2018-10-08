package edu.asu.jmars.layer.grid;

import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.LViewFactory;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.layer.LayerParameters;
import edu.asu.jmars.layer.SerializedParameters;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.layer.grid.GridLayer;

public class GridFactory extends LViewFactory {
    
	public GridFactory(){
		type = "llgrid";
	}
	
    public void createLView(boolean async, LayerParameters l) {
        LView view = realCreateLView(new GridParameters());
        view.setLayerParameters(l);
        LManager.receiveNewLView(view);
    }

    // called to provide default lat/lon grid layer on tool startup
    public Layer.LView createLView() {
        return realCreateLView(new GridParameters()); // use defaults
    }
    
	// used to restore a view from a save state
	public Layer.LView recreateLView(SerializedParameters parmBlock) {
        if (parmBlock instanceof GridParameters) {
            return realCreateLView((GridParameters)parmBlock);
        } else {
            return realCreateLView(new GridParameters()); // use defaults
        }
	}

	public String getName() {
		return "Lat/Lon Grid";
	}

	public String getDesc() {
		return "An adjustable grid of latitude/longitude lines.";
	}
	
    private Layer.LView realCreateLView(GridParameters params) {
        GridLayer layer = new GridLayer();
        layer.initialLayerData = params;
        Layer.LView view = new GridLView(true, layer, params);
        view.originatingFactory = this;
        view.setVisible(true);
        return view;
    }

}
