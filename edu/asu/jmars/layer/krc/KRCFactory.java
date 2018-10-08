package edu.asu.jmars.layer.krc;

import java.util.ArrayList;

import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.LViewFactory;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.layer.map2.MapServer;
import edu.asu.jmars.layer.map2.MapServerFactory;
import edu.asu.jmars.layer.map2.MapSource;
import edu.asu.jmars.layer.LayerParameters;
import edu.asu.jmars.layer.SerializedParameters;

public class KRCFactory extends LViewFactory{

	public KRCFactory() {
		type = "krc";
	}
	
	/**
	 * Is called from the default block in the AddLayer dialog
	 * in the createButton method
	 */
	public void createLView(boolean async, LayerParameters lp){
		//parse out the map source info for default values
		//should be in the following order:
		// elevation, albedo, ti, slope, azimuth
		ArrayList<String> sourceNames = lp.options;
		ArrayList<MapSource> sources = new ArrayList<MapSource>();
		MapServer server = MapServerFactory.getServerByName("default");
		for(String name : sourceNames){
			sources.add(server.getSourceByName(name));
		}
		
		KRCLView lview = new KRCLView(new KRCLayer(sources), true, lp);
		lview.originatingFactory = this;
		LManager.receiveNewLView(lview);
	}
	
	
	/**
	 * Called when restoring sessions
	 */
	public LView recreateLView(SerializedParameters parmBlock) {
		return null;
	}

}
