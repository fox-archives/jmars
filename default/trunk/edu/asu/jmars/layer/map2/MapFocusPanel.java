package edu.asu.jmars.layer.map2;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.event.EventListenerList;

import org.jdesktop.swingx.JXTaskPane;
import org.jdesktop.swingx.JXTaskPaneContainer;

import edu.asu.jmars.layer.FocusPanel;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.layer.map2.msd.PipelineLegModelEvent;
import edu.asu.jmars.layer.map2.msd.PipelineModel;
import edu.asu.jmars.layer.map2.msd.PipelineModelEvent;
import edu.asu.jmars.layer.map2.msd.PipelineModelListener;
import edu.asu.jmars.layer.map2.stages.composite.CompositeStage;

public class MapFocusPanel extends FocusPanel implements PipelineEventListener, PipelineProducer {
	private static final long serialVersionUID = 1L;
	private final Color lightBlue = UIManager.getColor("TabbedPane.selected");
	final String chartTabName = "Chart";	
	final MapLView mapLView;
	
	JScrollPane inputSP;
	JPanel topPanel = new JPanel();
	JPanel inputPanel = new JPanel();
	JXTaskPaneContainer inputTaskContainer = new JXTaskPaneContainer();
	
	JButton configButton;
	// Chart view attached to the main view.
	ChartView chartView;
	// PipelineEvent listeners list
	EventListenerList eventListenersList = new EventListenerList();
	// Current LView piplineModel
	private PipelineModel pipelineModel = new PipelineModel(new Pipeline[0]);
	
	public MapFocusPanel(LView parent) {
		super(parent);
		mapLView = (MapLView)parent;
		initialize();
		
		pipelineModel.addPipelineModelListener(new PipelineModelListener(){
			public void childrenAdded(PipelineModelEvent e) {}
			public void childrenChanged(PipelineModelEvent e) {}
			public void childrenRemoved(PipelineModelEvent e) {}
			public void compChanged(PipelineModelEvent e) {}

			public void forwardedEventOccurred(PipelineModelEvent e) {
				PipelineLegModelEvent le = e.getWrappedEvent();
				switch(le.getEventType()) {
					case PipelineLegModelEvent.STAGES_ADDED:
					case PipelineLegModelEvent.STAGES_REMOVED:
						firePipelineEvent(true, false);
						break;
					case PipelineLegModelEvent.STAGE_PARAMS_CHANGED:
						firePipelineEvent(true, true);
						break;
				}
			}
		});
		
		// Add ourselves to Source Configuration Panel's events so that we know of the
		// pipeline configuration changes sent down from the MapSettingsDialog.
		mapLView.getLayer().mapSettingsDialog.addPipelineEventListener(this);
		mapLView.getLayer().mapSettingsDialog.addPipelineEventListener(chartView);
		mapLView.getLayer().mapSettingsDialog.addPipelineEventListener(new PipelineEventListener(){
			public void pipelineEventOccurred(PipelineEvent e) {
				// By the time this particular listener is called, the chartView already
				// has the updated pipeline.
				int chartTabIdx = MapFocusPanel.this.indexOfTab(chartTabName); 

					MapFocusPanel.this.addTab(chartTabName, chartView);
			}
		});
		// TODO: This is not a good way of doing things.
		//mapLView.getLayer().mapSettingsDialog.firePipelineEvent();
	}
	
	private void initialize(){
		// add configure button and call to show map sources dialog
		configButton = new JButton("Configure");
		configButton.setMaximumSize(new Dimension(100, 0));
		configButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				try {
					/*
					 * Push the current pipeline back to the MapSettingsDialog before
					 * showing it to get new map parameters. Note that we are not
					 * pusing the ChartPipeline back. This is because we don't modify
					 * the ChartPipeline locally, so the configuration within the
					 * MapSettingsDialog is current.
					 */ 
					Pipeline[] pipeline = pipelineModel.buildPipeline();
					mapLView.getLayer().mapSettingsDialog.setLViewPipeline(pipeline);
					mapLView.getLayer().mapSettingsDialog.dialog.setVisible(true);
				}
				catch(Exception ex){
					ex.printStackTrace();
				}
			}
		});
		
		
		// Add tab for the chart view
		chartView = new ChartView(mapLView);
		if (!chartView.hasEmptyPipeline()){
			this.add(chartTabName, chartView);
		}
		
		this.addTab("Input", inputPanel);

	}
	
	public ChartView getChartView() {
		return chartView;
	}

	/*
	 * We receive these events from MapSettingsDialog in response to an Ok button push.
	 * This means that we have a new pipeline, we should update our pipelineModel
	 * and the dialogs showing the stage panels.
	 * 
	 * This event is cascaded to both the main and panner views via their registered
	 * listeners.
	 * 
	 * (non-Javadoc)
	 * @see edu.asu.jmars.layer.map2.PipelineEventListener#pipelineEventOccurred(edu.asu.jmars.layer.map2.PipelineEvent)
	 */
	public void pipelineEventOccurred(PipelineEvent e) {
		Pipeline[] newPipeline;
		
		try {
			newPipeline = Pipeline.getDeepCopy(e.source.buildLViewPipeline());
		}
		catch(CloneNotSupportedException ex){
			ex.printStackTrace();
			newPipeline = new Pipeline[0];
		}
		
		pipelineModel.setFromPipeline(newPipeline, Pipeline.getCompStage(newPipeline));
		
		CompositeStage aggStage = pipelineModel.getCompStage();
		
		topPanel.removeAll();
		inputTaskContainer.removeAll();
		topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.PAGE_AXIS));
		for(int i=0; i<pipelineModel.getSourceCount(); i++){
			PipelinePanel pp = new PipelinePanel(pipelineModel.getPipelineLeg(i));
			
			JXTaskPane inputTaskPane = new JXTaskPane();
			inputTaskPane.add(pp);
			inputTaskPane.setTitle(aggStage.getInputName(i));
	
			inputTaskContainer.add(inputTaskPane);
			
			topPanel.add(inputTaskContainer);
		}
		
		inputTaskContainer.setBackground(lightBlue);
		inputTaskContainer.repaint();
		topPanel.repaint();
		inputSP = new JScrollPane(topPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		inputSP.getVerticalScrollBar().setUnitIncrement(15);
		inputPanel.removeAll();
		inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.PAGE_AXIS));
		inputPanel.setBackground(lightBlue);
		inputPanel.add(inputSP);
		configButton.setAlignmentX(CENTER_ALIGNMENT);
		inputPanel.add(Box.createRigidArea(new Dimension(0,8)));
		inputPanel.add(configButton);
		inputPanel.add(Box.createRigidArea(new Dimension(0,8)));
		inputTaskContainer.repaint();
		inputPanel.repaint();
		if (inputSP!=null) {
			this.remove(inputSP);
		}
		
		this.addTab("Input", inputPanel);
		firePipelineEvent(e.userInitiated, e.settingsChange);
	}
	
	public void addPipelineEventListener(PipelineEventListener l){
		eventListenersList.add(PipelineEventListener.class, l);
	}
	public void removePipelineEventListener(PipelineEventListener l){
		eventListenersList.remove(PipelineEventListener.class, l);
	}
	
	/**
	 * Fires a new pipeline event from this PipelineProducer.
	 * @param user If true, this is a user-initiated change
	 * @param settings If true, this is a stage settings change, otherwise a pipeline structure change.
	 */
	public void firePipelineEvent(boolean user, boolean settings) {
		PipelineEvent e = new PipelineEvent(this, user, settings);
		EventListener[] listeners = eventListenersList.getListeners(PipelineEventListener.class);
		for(int i=0; i<listeners.length; i++){
			((PipelineEventListener)listeners[i]).pipelineEventOccurred(e);
		}
	}
	
	// Unused, this does not produce chart pipeline
	public Pipeline[] buildChartPipeline() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Produces a pipeline suitable for ONE view. Even if the views are using
	 * the same conceptual pipeline, this method must still be called once for
	 * each view.
	 */
	public Pipeline[] buildLViewPipeline() {
		Pipeline[] pipeline = pipelineModel.buildPipeline();
		return Pipeline.replaceAggStage(pipeline, 
				CompStageFactory.instance().getStageInstance(Pipeline.getCompStage(pipeline).getStageName()));
	}
	
	public PipelineModel getLViewPipelineModel(){
		return pipelineModel;
	}
}
