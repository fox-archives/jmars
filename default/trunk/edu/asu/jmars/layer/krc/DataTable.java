package edu.asu.jmars.layer.krc;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseEvent;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;


public class DataTable extends JTable {
	private DataTableModel myModel;
	private Color missingPlot = new Color(200, 0, 0);
	
	
	public DataTable(DataTableModel model){
		super(model);
		myModel = model;
	}
	
	public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
		Component c = super.prepareRenderer(renderer, row, column);
		KRCDataPoint dp = myModel.getDataPoint(row);
		
		Color fgColor = getForeground();
		
		if(dp.getDayData() == null || dp.getYearData() == null){
			fgColor = missingPlot;
		}
		
		c.setForeground(fgColor);
		
		return c;
	}

	
	public String getToolTipText(MouseEvent event) {
		String tip = "";
	
		int row = rowAtPoint(event.getPoint());

		KRCDataPoint dp = myModel.getDataPoint(row);
		
		if(dp.getDayData() == null || dp.getYearData() == null){
			tip = "No KRC results exist for this data point with current inputs.";
		}
		
		return tip;
	}
	
}
