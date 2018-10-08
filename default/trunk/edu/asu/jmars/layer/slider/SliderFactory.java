package edu.asu.jmars.layer.slider;

import java.util.ArrayList;

import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.LViewFactory;
import edu.asu.jmars.layer.LayerParameters;
import edu.asu.jmars.layer.SerializedParameters;
import edu.asu.jmars.layer.Layer.LView;

public class SliderFactory extends LViewFactory {
	public SliderFactory(){
		type = "timeslider";
	}
	
	public LView createLView() {
		return null;
	}

	public void createLView(LayerParameters lp) {
		LView view = new SliderLView(new SliderLayer(), lp);
		// TODO: How dumb is this?
		view.originatingFactory=this;
		LManager.receiveNewLView(view);
	}
	
	public LView recreateLView(SerializedParameters parmBlock) {
		return null;
	}

	public String getName() {
		return "Map Time-Slider";
	}
}
