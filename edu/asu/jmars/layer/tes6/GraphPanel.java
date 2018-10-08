package edu.asu.jmars.layer.tes6;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Paint;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;


public class GraphPanel extends JPanel {
    public GraphPanel(){
        super();
        data = new LinkedHashMap<Object,XYSeries>();
        
        showLegend = false; showToolTips = true; showUrl = false;

        XYDataset dataset = new XYSeriesCollection();
        chart = ChartFactory.createXYLineChart(
            defTitle, defXAxisLabel, defYAxisLabel,
            dataset,
            PlotOrientation.VERTICAL,
            showLegend, showToolTips, showUrl);

        canvas = new ChartPanel(chart);

        setLayout(new BorderLayout());
        add(canvas, BorderLayout.CENTER);
    }
    
    public void setDomainInverted(boolean inverted){
    	chart.getXYPlot().getDomainAxis().setInverted(inverted);
    }

    public void setBackground(Color c){
        super.setBackground(c);
        if (canvas != null){
            canvas.setBackground(c);
        }
    }

    public void clear(){
        XYSeriesCollection sc = (XYSeriesCollection)chart.getXYPlot().getDataset();
        sc.removeAllSeries();
        data.clear();

        chart.setTitle(defTitle);
        chart.getXYPlot().getDomainAxis().setLabel(defXAxisLabel);
        chart.getXYPlot().getRangeAxis().setLabel(defYAxisLabel);
    }

    public void clearData(){
        XYSeriesCollection sc = (XYSeriesCollection)chart.getXYPlot().getDataset();
        sc.removeAllSeries();
        data.clear();
    }
    /**
     * This method exposes the XYSeries for an id
     * @param id
     * @return XYSeries
     * 
     */
    public XYSeries getData(Object id) {
    	return data.get(id);
    }
    public void addData(Object id, String label, double[] yVals){
        XYSeries series = new XYSeries(label);
        for(int i = 0; i < yVals.length; i++){ series.add(i, yVals[i]); }
        XYSeriesCollection sc = (XYSeriesCollection)chart.getXYPlot().getDataset();
        sc.addSeries(series);

        data.put(id, series);
    }
    
    public void addData(Object id, String label, double[] xVals, double[] yVals){
        XYSeries series = new XYSeries(label);
        for(int i = 0; i < yVals.length; i++){ series.add(xVals[i], yVals[i]); }
        XYSeriesCollection sc = (XYSeriesCollection)chart.getXYPlot().getDataset();
        sc.addSeries(series);

        data.put(id, series);
    }
    
    public Paint getPaint(Object id){
    	int seriesIndex = (new ArrayList<Object>(data.keySet())).indexOf(id);
    	if (seriesIndex < 0)
    		return null;
    	return chart.getXYPlot().getRenderer().getSeriesPaint(seriesIndex);
    }

    public void setPaint(Object id, Paint paint){
    	int seriesIndex = (new ArrayList<Object>(data.keySet())).indexOf(id);
    	if (seriesIndex < 0)
    		return;
    	chart.getXYPlot().getRenderer().setSeriesPaint(seriesIndex, paint);
    }

    public void delData(Object id){
        XYSeries series = (XYSeries)data.get(id);
        if (series != null){
            XYSeriesCollection sc = (XYSeriesCollection)chart.getXYPlot().getDataset();
            sc.removeSeries(series);
        }
    }
    
    public void delData(Collection<Object> ids){
        XYSeriesCollection sc = (XYSeriesCollection)chart.getXYPlot().getDataset();
    	for(Object id: ids){
    		XYSeries series = (XYSeries)data.get(id);
    		if (series != null)
                sc.removeSeries(series);
    	}
    }

    public void setTitle(String title){
        chart.setTitle(title);
    }

    public void setXAxisLabel(String xAxisLabel){
        chart.getXYPlot().getDomainAxis().setLabel(xAxisLabel);
    }

    public void setYAxisLabel(String yAxisLabel){
        chart.getXYPlot().getRangeAxis().setLabel(yAxisLabel);
    }

    public String getXAxisLabel(){
        return chart.getXYPlot().getDomainAxis().getLabel();
    }

    public String getYAxisLabel(){
        return chart.getXYPlot().getRangeAxis().getLabel();
    }
    
    public ChartPanel getChartPanel(){
    	return canvas;
    }
    
    public JFreeChart getChart(){
    	return chart;
    }


    private ChartPanel canvas;
    private JFreeChart chart;
    private boolean    showLegend;
    private boolean    showToolTips;
    private boolean    showUrl;
    private LinkedHashMap<Object,XYSeries> data;
    private String defTitle = " ";
    private String defXAxisLabel = " ";
    private String defYAxisLabel = " ";


    public static void main(String[] args){
        JFrame f = new JFrame("GraphPanel test");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        GraphPanel gp = new GraphPanel();
        gp.setPreferredSize(new Dimension(400, 400));
        gp.setBackground(Color.white);
        f.setContentPane(gp);

        double[] data1 = new double[] {1, 2, 3, 4, 5};
        double[] data2 = new double[] {10, 25, 100, 6, 2, 50, 4, 2};

        gp.addData("data1","data1",data1);
        gp.addData("data2","data2",data2);
        gp.setXAxisLabel("Channel");
        gp.setYAxisLabel("Value");

        f.pack();
        f.setVisible(true);
    }
}
