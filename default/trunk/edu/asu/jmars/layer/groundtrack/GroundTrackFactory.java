package edu.asu.jmars.layer.groundtrack;

import java.util.HashMap;
import java.util.Map;

import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.LViewFactory;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.layer.LayerParameters;
import edu.asu.jmars.layer.SerializedParameters;

public class GroundTrackFactory extends LViewFactory {
	static final LayerParams[] SPACECRAFT = {
			new LayerParams(-53, "ODY", "Mars Odyssey"),
			new LayerParams(-94, "MGS", "Mars Global Surveyor"),
			new LayerParams(-41, "MEX", "Mars Express") };

	public GroundTrackFactory() {
		super("Groundtracks", "Orbital tracks showing the sub-spacecraft path");
		type = "groundtrack";
	}

	/**
	 ** Maps Strings (spacecraft names) to GroundTrackLayers.
	 **/
	private static Map layers;

	public Layer.LView createLView() {
		return null;
	}

	public void createLView(boolean async, LayerParameters l) {
		LView view = recreateLView(SPACECRAFT[0]);
		view.setLayerParameters(l);
		LManager.receiveNewLView(view);
	}

	public Layer.LView recreateLView(SerializedParameters sp) {
		LayerParams lp = (LayerParams) sp;

		GroundTrackLView view = new GroundTrackLView(lp);
		view.originatingFactory = this;
		return view;
	}

	static class LayerParams implements SerializedParameters {
		int id;
		String craft;
		String desc;

		LayerParams(int id, String craft, String desc) {
			this.id = id;
			this.craft = craft;
			this.desc = desc;
		}

		GroundTrackLayer getLayer() {
			if (layers == null)
				layers = new HashMap();

			GroundTrackLayer layer = (GroundTrackLayer) layers.get(craft);
			if (layer == null)
				layers.put(craft, layer = new GroundTrackLayer(id, this));

			return layer;
		}

		public String toString() {
			return craft + " - " + desc;
		}
	}
}
