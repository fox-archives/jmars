package edu.asu.jmars.layer.map2;

import java.awt.Color;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;

import edu.asu.jmars.layer.DataReceiver;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.layer.map2.msd.MapSettingsDialog;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.Util;

public class MapLayer extends Layer {
	private static DebugLog log = DebugLog.instance();
	final MapSettingsDialog mapSettingsDialog;
	MapFocusPanel focusPanel;
	private String name;
	
	public MapLayer(MapSettingsDialog dialog) {
		this.mapSettingsDialog = dialog;
	}
	
	/**
	 * Current state map; receivers that go to the lowest state are REMOVED so
	 * we don't get thousands of accumulated, pointless entries here
	 */
	private Map<DataReceiver,Color> receiverStates = new HashMap<DataReceiver,Color>();
	/** Set legal colors here, listed from lowest priority to highest */
	private List<Color> orderedStates = Arrays.asList(Util.darkGreen, Color.yellow, Util.darkRed);
	
	public synchronized void monitoredSetStatus(DataReceiver r, Color state) {
		int order = orderedStates.indexOf(state);
		if (order == -1) {
			log.println("Invalid STATUS light update");
			return;
		}
		if (order == 0)
			receiverStates.remove(r);
		else
			receiverStates.put(r, state);
		int max = 0;
		for (DataReceiver dr: receiverStates.keySet()) {
			int cur = orderedStates.indexOf(receiverStates.get(dr));
			if (cur > max)
				max = cur;
		}
		final Color stateColor = (Color)orderedStates.get(max);
		
		// Set the status on the AWT thread.
		SwingUtilities.invokeLater(new Runnable(){
			public void run() {
				setStatus(stateColor);
			}
		});
	}
	
	/** Unused */
	public void receiveRequest(Object layerRequest, DataReceiver requester) {}
	
	public void setName(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}
}
