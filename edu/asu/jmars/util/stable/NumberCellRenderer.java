package edu.asu.jmars.util.stable;

import java.awt.Color;
import java.awt.Component;
import java.text.DecimalFormat;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

public class NumberCellRenderer extends JLabel implements TableCellRenderer {
	private static DecimalFormat decimalValueFormatter = new DecimalFormat("0.###");

	public NumberCellRenderer(){
		super();
		setOpaque(true);
		setHorizontalAlignment(JLabel.RIGHT);
	}
	
	public NumberCellRenderer(DecimalFormat df){
		this();
		if(df!=null){
			decimalValueFormatter = df;
		}
	}

	public Component getTableCellRendererComponent(JTable table, Object val, boolean isSelected, boolean hasFocus, int row, int col) {
		setText(val == null ? "" : decimalValueFormatter.format(val));
		if(isSelected){
			this.setBackground(table.getSelectionBackground());
		}else{
			this.setBackground(table.getBackground());
		}
		return this;
	}
	
	public void setDigits(int newDigits) {
		StringBuffer tmp = new StringBuffer(newDigits);
		for (int i=0; i<newDigits; i++) {
			tmp.append("#");
		}
		decimalValueFormatter = new DecimalFormat("0." + tmp.toString());
	}
}
