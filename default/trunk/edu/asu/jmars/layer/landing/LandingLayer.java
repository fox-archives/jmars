package edu.asu.jmars.layer.landing;

import edu.asu.jmars.layer.DataReceiver;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.layer.LayerParameters;

public class LandingLayer extends Layer {

	public String configEntry;
	public String layerName;
	
	public void receiveRequest(Object layerRequest, DataReceiver requester) {
		broadcast(layerRequest);
	}
	
	public LandingSiteSettings settings;
	
	
	public LandingLayer(LayerParameters l, LandingSiteSettings lss, String name, String config){
		super();
		
		layerName = name;
		if(layerName == null){
			layerName = "Landing Site Ellipse";
		}
		
		configEntry = config;
		if(configEntry == null){
			configEntry = "landing2020";
		}
		
		settings = lss;
		settings.myLP = l;
		settings.configEntry = configEntry;
		settings.layerName = layerName;
	}
	
	
	public LandingLayer(LayerParameters l, String layerName, String config) {
		this(l, new LandingSiteSettings(), layerName, config);
	}
	
	public LandingLayer() {
		this(null, new LandingSiteSettings(), null, null);
	}
	
	public LandingLayer(LandingSiteSettings newSettings) {
		this(newSettings.myLP, newSettings, newSettings.layerName, newSettings.configEntry);
	}
}
