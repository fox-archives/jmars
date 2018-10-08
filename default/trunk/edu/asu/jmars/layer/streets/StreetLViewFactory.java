package edu.asu.jmars.layer.streets;

import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.LViewFactory;
import edu.asu.jmars.layer.LayerParameters;
import edu.asu.jmars.layer.SerializedParameters;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.util.Util;

public class StreetLViewFactory extends LViewFactory {

	private int osmType;

	public StreetLViewFactory() {
		type = "open_street_map";
	}

	public void createLView(int osmType, LayerParameters l) {
		StreetLayer layer = new StreetLayer(osmType);
		this.osmType = osmType;
		layer.setOsmType(osmType);
		StreetLView lview = new StreetLView(layer,l);
		lview.getLayer().setStatus(Util.darkRed);
		lview.originatingFactory = this;
		LManager.receiveNewLView(lview);

	}

	public LView recreateLView(SerializedParameters parmBlock) {
		StreetLViewSettings settings = (StreetLViewSettings)parmBlock;
		StreetLView lview = new StreetLView(new StreetLayer(osmType), settings.layerParams);
		lview.getLayer().setStatus(Util.darkRed);
		lview.getLayer().setOsmType(settings.osmType);
		lview.originatingFactory = this;

		return lview;
	}

	public String getDesc() {
		return "A Layer that calls OpenStreetMap tiles";
	}

}
