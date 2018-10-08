package edu.asu.jmars.layer.investigate;

import java.util.ArrayList;

import edu.asu.jmars.layer.*;

public class InvestigateLayer extends Layer{

	private ArrayList<DataSpike> dataSpikes;
	private ArrayList<DataProfile> dataProfiles;
	
	
	public InvestigateLayer(){
		this(new ArrayList<DataSpike>(), new ArrayList<DataProfile>());
	}
	
	//used in reloading sessions and layers
	public InvestigateLayer(ArrayList<DataSpike> ds, ArrayList<DataProfile> dp){
		dataSpikes = ds;
		dataProfiles = dp;
	}
	
	
	@Override
	public void receiveRequest(Object layerRequest, DataReceiver requester) {
		// TODO Auto-generated method stub
		
	}
	

	public ArrayList<DataSpike> getDataSpikes(){
		return dataSpikes;
	}
	
	public void addDataSpike(DataSpike newDS){
		dataSpikes.add(newDS);
	}
	
	public ArrayList<DataProfile> getDataProfiles(){
		return dataProfiles;
	}
}
