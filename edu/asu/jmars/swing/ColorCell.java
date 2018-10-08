package edu.asu.jmars.swing;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/**
 * Ported from edu.asu.jmars.layer.NumBackFocus
 * @author whagee
 *
 */

public class ColorCell extends ColorCombo implements TableCellRenderer{
	public Component getTableCellRendererComponent(
			JTable table, Object color,
			boolean isSelected, boolean hasFocus,
			int row, int column)
	{

		ColorCombo combo = (ColorCombo)color;
		setColor(combo.getColor());
        setPreferredSize(new Dimension(73,15));
        setMaximumSize(new Dimension(73, 15));
        
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
		panel.add(Box.createVerticalGlue());
		panel.add(this);
		panel.add(Box.createVerticalGlue());
		
		if (isSelected) {
	         panel.setForeground(table.getSelectionForeground());
	         panel.setBackground(table.getSelectionBackground());
		} else {
	         panel.setForeground(table.getForeground());
	         panel.setBackground(table.getBackground());
		}
		


		return panel;
	}

}
