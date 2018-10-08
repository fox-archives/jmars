package edu.asu.jmars.util.stable;

import java.awt.event.MouseEvent;
import java.util.EventObject;

import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;

// Editor for Boolean-classed table columns.
public class BooleanCellEditor extends DefaultCellEditor {
	public BooleanCellEditor() {
		super (getCheckbox ());
	}

	public boolean isCellEditable(EventObject e) {
		if (e instanceof MouseEvent) {
			int clickCount = ((MouseEvent) e).getClickCount();
			return (clickCount > 1);
		} else {
			return false;
		}
	}

	private static JCheckBox getCheckbox() {
		JCheckBox checkBox = new JCheckBox();
		checkBox.setHorizontalAlignment(JCheckBox.CENTER);
		return checkBox;
	}
}
