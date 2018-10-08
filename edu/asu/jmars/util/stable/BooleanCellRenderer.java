package edu.asu.jmars.util.stable;

import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

// The cell editor for Boolean columns.
public class BooleanCellRenderer
	extends JCheckBox
	implements TableCellRenderer
{
	public BooleanCellRenderer(){
		super("");
		setOpaque(true);
		setHorizontalAlignment(JCheckBox.CENTER);
	}

	public Component getTableCellRendererComponent(JTable table, Object val, boolean isSelected, boolean hasFocus, int row, int column) {
		if (val != null) {
			setSelected(((Boolean)val).booleanValue());
		}
		return this;
	}
}

