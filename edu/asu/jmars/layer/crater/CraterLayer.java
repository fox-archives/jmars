package edu.asu.jmars.layer.crater;


import edu.asu.jmars.layer.DataReceiver;
import edu.asu.jmars.layer.Layer;

public class CraterLayer extends Layer {

	public void receiveRequest(Object layerRequest, DataReceiver requester) {
		broadcast(layerRequest);
	}
	
	public CraterSettings settings;
	
	public CraterLayer() {
		super();
		settings=new CraterSettings();
	}
	
	public CraterLayer(CraterSettings newSettings) {
		super();
		settings=newSettings;
	}
}
