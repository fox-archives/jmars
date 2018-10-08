package edu.asu.jmars.layer.krc;

import java.awt.Color;
import java.awt.Paint;
import java.util.ArrayList;

import javax.swing.table.AbstractTableModel;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.general.DatasetUtilities;

public class ReadoutTableModel extends AbstractTableModel {
	protected static final String DAY = "Day";
	protected static final String YEAR = "Year";
	private static final String NAME_COL = "Name";
	private static final String COLOR_COL = "Color";
	private static final String TEMP_COL = "Temperature (K)";
	private static final String HOUR_COL = "Hour";
	private static final String LS_COL = "Ls";
	
	private ArrayList<String> cNames;
	private String myType;
	private ArrayList<KRCDataPoint> krcPoints;
	private JFreeChart myChart;
	private double x_value = 0;
	
	public ReadoutTableModel(String type, ArrayList<KRCDataPoint> data, JFreeChart chart){
		myType = type;
		krcPoints = data;
		myChart = chart;
		
		cNames = new ArrayList<String>();
		cNames.add(NAME_COL);
		cNames.add(COLOR_COL);
		cNames.add(TEMP_COL);
		if(myType.equals(DAY)){
			cNames.add(HOUR_COL);
		}else if(myType.equals(YEAR)){
			cNames.add(LS_COL);
		}
	}
	
	@Override
	public int getRowCount() {
		return krcPoints.size();
	}

	@Override
	public int getColumnCount() {
		return cNames.size();
	}
	
	@Override
	public String getColumnName(int col){
		return cNames.get(col);
	}
	
	/**
	 * Add this override so that the Table that uses this 
	 * table model can set the column editors and renderers
	 * appropriately
	 */
	@Override
	public Class<?> getColumnClass(int columnIndex){
		switch(getColumnName(columnIndex)){
			case NAME_COL:
				return String.class;
			case COLOR_COL: 
				return Color.class;
			case TEMP_COL:
			case HOUR_COL:
			case LS_COL:
				return Number.class;
		}
		
		return Object.class;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		
		KRCDataPoint dataPt = krcPoints.get(rowIndex);
		
		switch(getColumnName(columnIndex)){
		case NAME_COL:
			return dataPt.getName();
		case COLOR_COL:
			Paint p = myChart.getXYPlot().getRenderer().getSeriesPaint(rowIndex);
			if (p instanceof Color){
				return (Color)p;
			}
			break;
		case TEMP_COL:
			return DatasetUtilities.findYValue(myChart.getXYPlot().getDataset(), rowIndex, x_value);
		case HOUR_COL:
		case LS_COL:
			return x_value;
		}
	
		return null;
		
	}
	
	/**
	 * Takes x-value for the chart crosshair and displays it
	 * as the x value in the table, and uses it to find the y 
	 * value and displays that as well.
	 * @param x  The x-value that the chart crosshair is at
	 */
	public void setXValue(double x){
		x_value = x;
		fireTableDataChanged();
	}
	
	public boolean isCellEditable(int rowIndex, int colIndex){
		return (getColumnName(colIndex) == COLOR_COL);
	}

	public void setValueAt(Object value, int rowIndex, int columnIndex){
		if (!getColumnName(columnIndex).equals(COLOR_COL)){
			throw new IllegalArgumentException("Columns other than the color column are uneditable.");
		}
		
		XYPlot plot = myChart.getXYPlot();
		if (rowIndex < plot.getDataset().getSeriesCount()){
			myChart.getXYPlot().getRenderer().setSeriesPaint(rowIndex, (Color)value);
		}
	}
}
