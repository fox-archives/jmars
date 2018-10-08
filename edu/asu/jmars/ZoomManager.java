package edu.asu.jmars;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jdesktop.swingx.combobox.ListComboBoxModel;

import edu.asu.jmars.util.DebugLog;

public final class ZoomManager extends JPanel {
	private static DebugLog log = DebugLog.instance();
	
	private final List<Integer> zoomFactors;
	private JLabel zoomLabel = null;                       // zoom-selector's label
	private JComboBox zoomSelector = null;                 // zoom factor selector
	private List<ZoomListener> listeners = new ArrayList<ZoomListener>();
	private int ppd;
	
	public ZoomManager(int defaultZoomLog2, int maxZoomLog2) {
		List<Integer> zoomFactors = new ArrayList<Integer>();
		for (int i = 0; i < maxZoomLog2; i++) {
			zoomFactors.add(1<<i);
		}
		this.zoomFactors = Collections.unmodifiableList(zoomFactors);
		
		/* create the hbox containing the zoom-selector and its label */
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		
		/* create the zoom-selector label */
		zoomLabel = new JLabel("Zoom: ");
		zoomLabel.setToolTipText("Select the desired zoom-factor from the list "+
		"or type one in and press Enter.");
		add(zoomLabel);
		
		/* create the zoom-selection combo-box */
		zoomSelector = new JComboBox(new ListComboBoxModel<Integer>(zoomFactors));
		zoomSelector.setMaximumRowCount(zoomFactors.size());
		zoomSelector.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (zoomSelector.getSelectedItem() != null) {
					setZoomPPD((Integer)zoomSelector.getSelectedItem(), true);
				}
			}
		});
		add(zoomSelector);
		
		setZoomPPD(1<<defaultZoomLog2, false);
	}
	
	public void addListener(ZoomListener l) {
		listeners.add(l);
	}
	
	public boolean removeListener(ZoomListener l) {
		return listeners.remove(l);
	}
	
	public List<Integer> getZoomFactors() {
		return zoomFactors;
	}
	
	public int getZoomPPD() {
		return ppd;
	}
	
	public void setZoomPPD(int ppd, boolean propogate)
	{
		int index = zoomFactors.indexOf(ppd);
		if(index == -1)
		{
			log.aprintln("BAD ZOOM FACTOR RECEIVED: " + ppd);
			return;
		}
		if (this.ppd != ppd) {
			this.ppd = ppd;
			zoomSelector.setSelectedIndex(index);
			if (propogate) {
				notifyListeners();
			}
		}
	}
	
	public String[] getExportZoomFactors() {
		int currentIndex = zoomFactors.indexOf(ppd);
		
		int numExportOptions = Math.min(3, zoomFactors.size() - currentIndex - 1);
		
		String zoomOptions[] = new String[numExportOptions];
		
    	for (int i=0; i<zoomOptions.length; i++) {
    		zoomOptions[i]=""+((int)(ppd*(Math.pow(2,i+1))));
    	}
    	
    	return zoomOptions;
	}
	
	private void notifyListeners() {
		for (ZoomListener l: listeners) {
			l.zoomChanged(ppd);
		}
	}
}
