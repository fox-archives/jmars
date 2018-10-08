package edu.asu.jmars.util.stable;

import java.awt.event.MouseEvent;
import java.util.EventObject;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;

import edu.asu.jmars.util.LineType;

// cell editor for the Line Type column
public class LineTypeCellEditor extends DefaultCellEditor {
	public LineTypeCellEditor(){
		super(getComboBox());
	}

	public boolean isCellEditable(EventObject e){
		if (e instanceof MouseEvent) {
			int clickCount = ((MouseEvent)e).getClickCount();
			return (clickCount>1);
		} else {
			return false;
		}
	}

	public Object getCellEditorValue() {
		String result = (String)delegate.getCellEditorValue();
		if (result==null){
			return null;
		}
		if (result.equals("Dotted")){
			return new LineType(1);
		} else if (result.equals("Dashed")){
			return new LineType(2);
		} else if (result.equals("Dash-dot")){
			return new LineType(3);
		} else {
			return new LineType(0);
		}
	}

	private static JComboBox getComboBox() {
		JComboBox comboBox = new JComboBox();
		comboBox.addItem("Solid");
		comboBox.addItem("Dotted");
		comboBox.addItem("Dashed");
		comboBox.addItem("Dash-dot");
		return comboBox;
	}
}
