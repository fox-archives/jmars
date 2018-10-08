package edu.asu.jmars.layer.krc;

import java.util.ArrayList;

import edu.asu.jmars.layer.DataReceiver;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.layer.map2.MapSource;

public class KRCLayer extends Layer{

	private ArrayList<KRCDataPoint> dataPoints;
	private MapSource elevSource;
	private MapSource albedoSource;
	private MapSource tiSource;
	private MapSource slopeSource;
	private MapSource azimuthSource;
	private int ppd;
	
	public KRCLayer(ArrayList<MapSource> mapSources){
		//instantiate the data arraylist
		dataPoints = new ArrayList<KRCDataPoint>();
		//set the sources for map sampling
		elevSource = mapSources.get(0);
		albedoSource = mapSources.get(1);
		tiSource = mapSources.get(2);
		slopeSource = mapSources.get(3);
		azimuthSource = mapSources.get(4);
		
		//set the ppd level to something reasonable
		ppd = 1024;
	}
	
	
	public MapSource getElevationSource(){
		return elevSource;
	}
	
	public MapSource getAlbedoSource(){
		return albedoSource;
	}
	
	public MapSource getThermalInertiaSource(){
		return tiSource;
	}
	
	public MapSource getSlopeSource(){
		return slopeSource;
	}
	
	public MapSource getAzimuthSource(){
		return azimuthSource;
	}
	
	public int getPPD(){
		return ppd;
	}
	
	@Override
	public void receiveRequest(Object layerRequest, DataReceiver requester) {
	}
	
	/**
	 * @return A list of all the KRC data points that have been
	 * created in this layer
	 */
	public ArrayList<KRCDataPoint> getKRCDataPoints(){
		return dataPoints;
	}
	
	/**
	 * Add a new KRC Data Point to the list of data points
	 * contained by this layer
	 * @param newPoint
	 */
	public void addDataPoint(KRCDataPoint newPoint){
		dataPoints.add(newPoint);
	}
	
	/**
	 * Remove the KRC Data Point from the list of data points
	 * contained by this layer
	 * @param pointToRemove
	 */
	public void removeDataPoint(KRCDataPoint pointToRemove){
		dataPoints.remove(pointToRemove);
	}
	
	/**
	 * Used when editing a krc data point. Replace the old point
	 * with the new point
	 * @param oldPoint  Point before editing
	 * @param newPoint  Point after editing
	 */
	public void replaceDataPoint(KRCDataPoint oldPoint, KRCDataPoint newPoint){
		int index = dataPoints.indexOf(oldPoint);
		dataPoints.remove(oldPoint);
		dataPoints.add(index, newPoint);
	}
	
	/**
	 * Checks to see if the name is already in use from another
	 * krc data point.  If the name is not in use, returns true
	 * otherwise, returns false.
	 * @param name Name to compare against existing data points
	 * 
	 * @return True if the name is not being used, false if it's
	 * already being used
	 */
	public boolean isUniqueName(String name){
		boolean result = true;
		
		for(KRCDataPoint dp : dataPoints){
			if(dp.getName().equals(name)){
				result = false;
				break;
			}
		}
		
		return result;
	}

}
