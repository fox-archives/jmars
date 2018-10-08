package edu.asu.jmars.layer.shape2;

import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.LViewFactory;
import edu.asu.jmars.layer.LayerParameters;
import edu.asu.jmars.layer.SerializedParameters;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.layer.shape2.ShapeLView.ShapeParams;

public class ShapeFactory extends LViewFactory {
	public ShapeFactory() {
		super("Shapes", "Features shape drawing, loading, and saving.");
	}

	public ShapeFactory(String name, String desc) {
		super(name, desc);
	}

	public LView createLView() {
		return null;
	}
	
	public ShapeLView newInstance(boolean isReadOnly, LayerParameters lp) {
		ShapeLayer layer = new ShapeLayer(isReadOnly);
		ShapeLView lview = new ShapeLView(layer, true, isReadOnly, lp);
		lview.originatingFactory = this;
		return lview;
	}
	
	public ShapeLView newInstance(LayerParameters lp) {
	// the true passed into the shapelayer means it is a read only shape file	
		boolean isReadOnly = false;
		if (lp.name!=null)
			isReadOnly = true;
		ShapeLayer layer = new ShapeLayer(isReadOnly);
		ShapeLView lview = new ShapeLView(layer, true, isReadOnly, lp);
		lview.originatingFactory = this;
		return lview;
	}
	
	public void createLView(boolean async, LayerParameters lp) {
		LManager.receiveNewLView(newInstance(false, lp));
	}
	
	public LView recreateLView(SerializedParameters parmBlock) {
		boolean isReadOnly=false;
		String name = "";
		LayerParameters lp = null;
		if (parmBlock instanceof ShapeParams){
			lp = ((ShapeParams)parmBlock).layerParams;
			if(lp!=null)
				name = lp.name;

			if (!name.equals("Custom Shape Layer") && !name.equals("")){
				isReadOnly=true;
			}
		}
		ShapeLView slv = newInstance(isReadOnly, lp);
		slv.setName(name);
		return slv;
	}
	
	
}
