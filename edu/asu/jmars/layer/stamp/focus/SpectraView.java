package edu.asu.jmars.layer.stamp.focus;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import edu.asu.jmars.ProjObj;
import edu.asu.jmars.layer.stamp.StampLView;
import edu.asu.jmars.layer.stamp.StampShape;

public class SpectraView extends JPanel {
	private static final long serialVersionUID = 1L;

	// Attached stampLView
	StampLView stampLView;
	
	ProjObj proj;
	
	// JFreeChart related stuff
	JFreeChart chart;
	ChartPanel chartPanel;
	
	// Readout table stuff
	JTable readoutTable;
	
	JComboBox<String> selectedPlotColumn;
	
	public SpectraView(final StampLView stampLView){
		this.stampLView = stampLView;
		
		setLayout(new BorderLayout());
		
		add(createChartPanel(), BorderLayout.CENTER);		
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout());
		buttonPanel.add(new JLabel("Plot type: "));
		
		selectedPlotColumn = new JComboBox(stampLView.stampLayer.getSpectraColumns());
		
		selectedPlotColumn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setDisplayData(stampLView.stampLayer.getSelectedStamps());
			}
		});
		
		buttonPanel.add(selectedPlotColumn);
		
		add(buttonPanel, BorderLayout.SOUTH);
	}
		
	private JPanel createChartPanel(){
		chart = ChartFactory.createXYLineChart(
				"",
				"wave number",
				"",
				new XYSeriesCollection(),
				PlotOrientation.VERTICAL,
				false,
				false,
				false);
		
		chart.getXYPlot().setDomainCrosshairVisible(true);
		//chart.getXYPlot().setDomainCrosshairLockedOnData(true);
		chart.getXYPlot().setDomainCrosshairPaint(Color.blue);
		chart.getXYPlot().setDomainCrosshairStroke(new BasicStroke(1.0f));
				
		chartPanel = new ChartPanel(chart, true);
								
		JPanel p = new JPanel(new BorderLayout());
		p.add(new JPanel(), BorderLayout.NORTH);
		p.add(chartPanel, BorderLayout.CENTER);
		
		return p;
	}
	
	public void setDisplayData(List<StampShape> stampData) {
			XYPlot plot = chart.getXYPlot();
			
			XYSeriesCollection data_series = new XYSeriesCollection();

			String plotName = selectedPlotColumn.getSelectedItem()+"";
			plotName = plotName.trim();

			// TODO: Don't hardcode this.  Handle this in the view somehow?
			int index = 0;
			
			if (plotName.contains("[radiance]")) {
				plotName = "osl3c_rec_ioverf";
				index=0;
			} else if (plotName.contains("[uncertainty]")) {
				plotName = "osl3c_rec_ioverf";
				index=1;				
			} else if (plotName.contains("[quality]")) {
				plotName = "osl3c_rec_ioverf";
				index=2;
			}
			
			
			int cnt = 0;
			for(StampShape stamp : stampData) {
				XYSeries stamp_data = new XYSeries(plotName + " " + cnt++);
			
				
				double xValues[] = stamp.getXValues(plotName);
				
				Object val = stamp.getVal(plotName);

				if (val instanceof float[]) {
					float[] vals = (float[]) val;

					if (vals!=null) {
						for (int i=0; i<vals.length; i++) {
							try {
								stamp_data.add(xValues[i], vals[i]);
							} catch (Exception e) {
								// TODO: Don't silently swallow exceptions - but frequently an exception here leads to thousands of errors.
							}						
						}
					}
					
				} else if (val instanceof double[]) {
					double[] vals = (double[]) val;
					
					if (vals!=null) {
						for (int i=0; i<vals.length; i++) {
							try {
								stamp_data.add(xValues[i], vals[i]);
							} catch (Exception e) {
								// TODO: Don't silently swallow exceptions - but frequently an exception here leads to thousands of errors.								
							}						
						}
					}
					
				} else if (val instanceof Float[]) {
					Float[] vals = (Float[]) val;
					
					if (vals!=null) {
						for (int i=0; i<vals.length; i++) {
							try {
								stamp_data.add(xValues[i], vals[i]);
							} catch (Exception e) {
								// TODO: Don't silently swallow exceptions - but frequently an exception here leads to thousands of errors.								
							}						
						}
					}
				} else if (val instanceof Double[]) {
					Double[] vals = (Double[]) val;
					
					if (vals!=null) {
						for (int i=0; i<vals.length; i++) {
							try {
								stamp_data.add(xValues[i], vals[i]);
							} catch (Exception e) {
								// TODO: Don't silently swallow exceptions - but frequently an exception here leads to thousands of errors.								
							}						
						}
					}
				} 
				else if (val instanceof float[][]) { 
					float[][] vals = (float[][]) val;

					if (vals!=null) {
						for (int i=0; i<vals[index].length; i++) {
							try {
								stamp_data.add(xValues[i], vals[index][i]);
							} catch (Exception e) {
								e.printStackTrace();
							}						
						}
					}
				}
				else if (val instanceof double[][]) { 
					double[][] vals = (double[][]) val;

					if (vals!=null) {
						for (int i=0; i<vals[index].length; i++) {
							try {
								stamp_data.add(xValues[i], vals[index][i]);
							} catch (Exception e) {
								e.printStackTrace();
							}						
						}
					}
				} else {
					// TODO: ??
				}
					
					
				data_series.addSeries(stamp_data);
			}
            
			plot.setDataset(data_series);
            			
			
			String units = stampLView.stampLayer.getSpectraAxisXUnitMap().get(plotName);
			if (units==null || units.trim().length()<1) {
				units = "<unknown>";
			}

			plot.getDomainAxis().setLabel(units);

			String reverse = stampLView.stampLayer.getSpectraAxisReverseMap().get(plotName);
			if (reverse!=null && reverse.trim().equalsIgnoreCase("true")) {
				plot.getDomainAxis().setInverted(true);			
			}
		}	
}


