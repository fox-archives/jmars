package edu.asu.jmars.util.stable;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

public class ColorCellRenderer extends JLabel implements TableCellRenderer {
	private Color color;
	
	public ColorCellRenderer(){
		super();
		setOpaque(true);
	}
	
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		color = (Color)value;
		return this;
	}
	
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Insets insets = getInsets();
		int width = Math.max(2, getWidth()-insets.left-insets.right-8);
		int height = Math.max(2, getHeight()-insets.top-insets.bottom-8);
		g.setColor(color);
		g.fillRect((getWidth()-width)/2, (getHeight()-height)/2, width, height);
	}
}
