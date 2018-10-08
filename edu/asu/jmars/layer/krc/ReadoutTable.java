package edu.asu.jmars.layer.krc;

import java.awt.Color;

import javax.swing.JTable;

import edu.asu.jmars.util.stable.ColorCellEditor;
import edu.asu.jmars.util.stable.ColorCellRenderer;

public class ReadoutTable extends JTable {
	public ReadoutTable(ReadoutTableModel model){
		super(model);
		setDefaultRenderer(Color.class, new ColorCellRenderer());
		setDefaultEditor(Color.class, new ColorCellEditor());
	}

}
