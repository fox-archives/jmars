// Copyright 2008, Arizona Board of Regents
// on behalf of Arizona State University
// 
// Prepared by the Mars Space Flight Facility, Arizona State University,
// Tempe, AZ.
// 
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
// 
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.


package edu.asu.jmars.layer.map2.msd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import edu.asu.jmars.util.Util;

public class PlotSourcesListModel implements ListModel, PipelineModelListener {
	PipelineModel pipelineModel;
	List listeners;
	
	public PlotSourcesListModel(PipelineModel pipelineModel){
		this.pipelineModel = pipelineModel;
		this.pipelineModel.addPipelineModelListener(this);
		this.listeners = new ArrayList();
	}
	
	public PipelineModel getBackingPipelineModel(){
		return pipelineModel;
	}
	
	public void addListDataListener(ListDataListener l) {
		listeners.add(l);
	}

	public void removeListDataListener(ListDataListener l) {
		listeners.add(l);
	}

	public Object getElementAt(int index) {
		return pipelineModel.getSource(index);
	}

	public int getSize() {
		return pipelineModel.getSourceCount();
	}

	public void fireContentsChanged(){
		ListDataEvent e = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, pipelineModel.getSourceCount());
		for(Iterator li=listeners.iterator(); li.hasNext(); ){
			ListDataListener l = (ListDataListener)li.next();
			l.contentsChanged(e);
		}
	}
	
	public void fireIntervalChanged(int index0, int index1){
		ListDataEvent e = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, index0, index1);
		for(Iterator li=listeners.iterator(); li.hasNext(); ){
			ListDataListener l = (ListDataListener)li.next();
			l.contentsChanged(e);
		}
	}
	
	public void fireIntervalAdded(int index0, int index1){
		ListDataEvent e = new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, index0, index1);
		for(Iterator li=listeners.iterator(); li.hasNext(); ){
			ListDataListener l = (ListDataListener)li.next();
			l.intervalAdded(e);
		}
	}
	
	public void fireIntervalRemoved(int index0, int index1){
		ListDataEvent e = new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, index0, index1);
		for(Iterator li=listeners.iterator(); li.hasNext(); ){
			ListDataListener l = (ListDataListener)li.next();
			l.intervalRemoved(e);
		}
	}
	
	//
	// PipelineModelListener
	//
	
	public void childrenAdded(PipelineModelEvent e) {
		int[] indices = e.getChildIndices();
		Arrays.sort(indices);
		int[][] binned = Util.binRanges(indices);
	
		for(int i=0; i<binned.length; i++){
			fireIntervalAdded(binned[i][0], binned[i][binned[i].length-1]);
		}
	}

	public void childrenChanged(PipelineModelEvent e) {
		int[] indices = e.getChildIndices();
		Arrays.sort(indices);
		int[][] binned = Util.binRanges(indices);
	
		for(int i=0; i<binned.length; i++){
			fireIntervalChanged(binned[i][0], binned[i][binned[i].length-1]);
		}
	}

	public void childrenRemoved(PipelineModelEvent e) {
		int[] indices = e.getChildIndices();
		Arrays.sort(indices);
		int[][] binned = Util.binRanges(indices);
	
		for(int i=binned.length-1; i>=0; i--){
			fireIntervalRemoved(binned[i][0], binned[i][binned[i].length-1]);
		}
	}

	public void compChanged(PipelineModelEvent e) {
		fireContentsChanged();
	}

	public void forwardedEventOccurred(PipelineModelEvent e) {
		// Unused
	}

}
