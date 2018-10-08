package edu.asu.jmars.layer.mosaics;

import java.util.HashSet;

import edu.asu.jmars.layer.DataReceiver;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.layer.ProjectionEvent;
import edu.asu.jmars.layer.util.features.Feature;
import edu.asu.jmars.layer.util.features.FeatureCollection;
import edu.asu.jmars.layer.util.features.FeatureProvider;
import edu.asu.jmars.layer.util.features.RefWorldCache;
import edu.asu.jmars.layer.util.features.WorldCache;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.ObservableSet;

public class MosaicsLayer extends Layer {
	FeatureCollection fc;
	ObservableSet<Feature> selections = new ObservableSet<Feature>(new HashSet<Feature>());
	private WorldCache worldCache = new RefWorldCache();
	
	public MosaicsLayer(){
		FeatureProvider fp = new FeatureProviderWMS();
		fc = fp.load(Config.get("mosaics.server.url"));
	}
	
	public void receiveRequest(Object layerRequest, DataReceiver requester) {
		broadcast(fc.getFeatures());
	}
	
	public FeatureCollection getFeatures(){
		return fc;
	}
	
	public void projectionChanged(ProjectionEvent e) {
		super.projectionChanged(e);
		worldCache = new RefWorldCache();
	}
	
	public WorldCache getCache() {
		return worldCache;
	}
}
