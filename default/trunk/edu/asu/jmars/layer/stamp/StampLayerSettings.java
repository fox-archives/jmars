package edu.asu.jmars.layer.stamp;

import java.awt.Color;
import java.awt.geom.GeneralPath;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import edu.asu.jmars.layer.LayerParameters;
import edu.asu.jmars.layer.SerializedParameters;
import edu.asu.jmars.swing.ColorCombo;
import edu.asu.jmars.swing.ColorMapper.State;

public class StampLayerSettings implements SerializedParameters {
	static final long serialVersionUID = 8742030623145671825L;
	
	public String instrument;
	
	public String name;
	public String queryStr;
	public String[] initialColumns;
	public Color  unsColor = new ColorCombo().getColor();
	public Color  filColor = new Color(new ColorCombo().getColor().getRGB() & 0xFFFFFF, true);
	
	// wind Vectors
	private Color  originColor = new ColorCombo().getColor();
	private double magnitude_scale=1.0;
	private double origin_magnitude_scale=1.0;
	
	public int port;  // Used for communication with davinci
	
	private FilledStamp.State[] stampStateList;
	private boolean hideOutlines=false;
	private boolean renderSelectedOnly=false;
	
	// Used for intersect queries
	public ArrayList<GeneralPath> paths = null;
	public ArrayList<String> srcItems = null;
	public String srcName = null;
	
	private LayerParameters myLP = null;
	
	// Used for styling Point based stamps (ie. MOLA shots)	
	public String colorColumn = null;
	public double colorMin = Double.NaN;
	public double colorMax = Double.NaN;
	public State colorState = null;
	
	// Used for styling Spectra based stamps (ie. TES)
	public boolean hideValuesOutsideRange = false;
	public boolean expressionSelected = false;
	public String expressionText = "";
	
	// Used for Radar Horizons
	private Color horizonColor = Color.RED;
	private int fullResWidth = 1;
	private int browseWidth = 1;
	private int lviewWidth = 1;
	private Map<Color, Boolean> horizonColorDisplayMap = new HashMap<Color, Boolean>();
	
	
	
	public StampLayerSettings() {
		setColorMap();
	}
	
	public StampLayerSettings(String instrument, String[] initialColumns) {
		this.instrument = instrument;
		this.initialColumns = initialColumns;
		
		setColorMap();
	}
	
	private void setColorMap(){
		//populate the color index map (used for the radar layer)
		for(Color c :ColorCombo.getColorList()){
			horizonColorDisplayMap.put(c, true);
		}
	}
	
	public LayerParameters getLayerParams(){
		return myLP;
	}
	public void setLayerParams(LayerParameters lp){
		myLP = lp;
	}
	
	public Color getUnselectedStampColor() {
		return unsColor;
	}
	
	public void setUnselectedStampColor(Color newColor) {
		unsColor=newColor;
	}
	
	public Color getFilledStampColor() {
		return filColor;
	}
	
	public void setFilledStampColor(Color newColor) {
		filColor=newColor;
	}
	
	// Start Wind Vector options
	public Color getOriginColor() {
		return originColor;
	}
	
	public void setOriginColor(Color newColor) {
		originColor=newColor;
	}
	
	public double getMagnitude() {
		return magnitude_scale;
	}
	
	public void setMagnitude(double newMagnitude) {
		magnitude_scale=newMagnitude;
	}

	public double getOriginMagnitude() {
		return origin_magnitude_scale;
	}
	
	public void setOriginMagnitude(double newMagnitude) {
		origin_magnitude_scale=newMagnitude;
	}
	//
	
	
	public boolean hideOutlines() {
		return hideOutlines;
	}
	
	public boolean renderSelectedOnly() {
		return renderSelectedOnly;
	}
	
	public void setHideOutlines(boolean newSetting) {
		hideOutlines=newSetting;
	}

	public void setRenderSelectedOnly(boolean newSetting) {
		renderSelectedOnly=newSetting;
	}

	public String getName() {
		return name;
	}
	
	public void setName(String newName) {
		name=newName;
	}
	
	public String getInstrument() {
		return instrument;
	}
	
	public FilledStamp.State[] getStampStateList() {
		return stampStateList;
	}
	
	public void setStampStateList(FilledStamp.State[] newStates) {
		stampStateList=newStates;
	}
	
	public void setInitialColumns(String[] newCols) {
		initialColumns=newCols;
	}
	
	// does the normal object read, but once all the necessary info is gathered,
	// fills in fields that may have been missing in older session files
	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		ois.defaultReadObject();
		if (stampStateList != null) {
			for (FilledStamp.State state: stampStateList) {
				if (state.imageType == null) {
					if (instrument.equals("THEMIS")) {
						if (state.id.startsWith("I")) {
							state.imageType = "BTR";
						} else if (state.id.startsWith("V")) {
							state.imageType = "ABR";
						} else {
							state.imageType = "BTR";
						}
					} else {
						state.imageType = instrument.toUpperCase();
					}
				}
			}
		}
	}
	
	
	//Settings used for the radar horizons
	public Color getHorizonColor(){
		return horizonColor;
	}
	public int getFullResWidth(){
		return fullResWidth;
	}
	public int getBrowseWidth(){
		return browseWidth;
	}
	public int getLViewWidth(){
		return lviewWidth;
	}
	public void setHorizonColor(Color color){
		horizonColor = color;
	}
	public void setFullResWidth(int size){
		fullResWidth = size;
	}
	public void setBrowseWidth(int size){
		browseWidth = size;
	}
	public void setLViewWidth(int size){
		lviewWidth = size;
	}
	public Map<Color, Boolean> getHorizonColorDisplayMap(){
	//can possibly be null if loading an older session/layer file
		if(horizonColorDisplayMap == null){
			horizonColorDisplayMap = new HashMap<Color, Boolean>();
			setColorMap();
		}
		return horizonColorDisplayMap;
	}
}
