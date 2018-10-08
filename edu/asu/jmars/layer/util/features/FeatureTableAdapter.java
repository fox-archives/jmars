package edu.asu.jmars.layer.util.features;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import edu.asu.jmars.layer.shape2.ShapeLayer;
import edu.asu.jmars.swing.STable;
import edu.asu.jmars.util.History;
import edu.asu.jmars.util.LineType;
import edu.asu.jmars.util.ListType;
import edu.asu.jmars.util.ObservableSet;
import edu.asu.jmars.util.stable.ListTypeCellEditor;
import edu.asu.jmars.util.stable.ColorCellEditor;
import edu.asu.jmars.util.stable.ColorCellRenderer;
import edu.asu.jmars.util.stable.FilteringColumnModel;
import edu.asu.jmars.util.stable.LineTypeCellEditor;
import edu.asu.jmars.util.stable.LineTypeTableCellRenderer;

/**
 * Produces an STable for viewing features. Editors and renderers are added for
 * color and line type. Column auto resizing is disabled. A FeatureTableModel is
 * created around the given FeatureCollection. A FeatureSelectionListener is
 * added to keep the STable's ListSelectionModel and the FIELD_SELECTED
 * attributes on Features in the FeatureCollection in sync. This will listen on
 * the FeatureCollection and send events on to the FeatureTableModel and then
 * the FeatureSelectionListener.
 */
public class FeatureTableAdapter implements FeatureListener
{
	private STable fst;
	private FeatureCollection fc;
	private FeatureSelectionListener fsl;
	private FeatureTableModel ftm;
	private History history;
	
	private ShapeLayer layer;
	
	public FeatureTableAdapter (FeatureCollection fc, ObservableSet<Feature> selections, History history, ShapeLayer sl) {
		this.fc = fc;
		this.history = history;
		this.layer = sl;
		
		// create and customize an STable for our needs
		fst = new STable () {
			private static final long serialVersionUID = 1L;
			private final Color rowhighlight = new Color(237, 243, 254);
			private final EmptyBorder border = new EmptyBorder(1,2,1,2);
			{
				setRowHeight(getRowHeight() + border.getBorderInsets().top + border.getBorderInsets().bottom);
			}
			
			public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
				// TODO: row highlight will need to be disabled under Synth
				// since it already does something similar
				Component c = super.prepareRenderer(renderer, row, column);
				if (!isRowSelected(row) && row%2==1) {
					c.setBackground(rowhighlight);
				}
				if (c instanceof JComponent) {
					((JComponent)c).setBorder(border);
				}
				return c;
			}
			
			// marks a history frame when the table cell editor is used
			public void editingStopped(ChangeEvent e) {
				FeatureTableAdapter.this.history.mark();
				super.editingStopped(e);
			}
		};
		
		fst.setIntercellSpacing(new Dimension(1,0));
		fst.setAutoCreateColumnsFromModel(false);
		fst.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		fst.setShowHorizontalLines(false);
		fst.setTypeSupport(Color.class, new ColorCellRenderer(), new ColorCellEditor());
		fst.setTypeSupport(LineType.class, new LineTypeTableCellRenderer(), new LineTypeCellEditor());
		fst.setTypeSupport(ListType.class, new DefaultTableCellRenderer(), new ListTypeCellEditor(layer));

		fsl = new FeatureSelectionListener (fst, fc, selections);
		ftm = new FeatureTableModel (fc, (FilteringColumnModel)fst.getColumnModel(), fsl);
		fst.setUnsortedTableModel(ftm);

		fsl.setFeatureSelectionsToTable();

		// This class receives events on behalf of the table and selection
		// listener and forwards to guarrantee that the table gets them first.
		fc.addListener (this);
	}

	/**
	 * @return The STable component produced by this instance.
	 */
	public STable getTable () {
		return fst;
	}

	/**
	 * Sends incoming events from the FeatureCollection on to the FeatureTableModel
	 * and FeatureSelectionListener (in that order, since JTable can't make selections
	 * on data that doesn't exist yet.)
	 */
	public void receive (FeatureEvent event) {
		ftm.receive(event);
	}

	/**
	 * Clear various listeners as part of the cleanup.
	 */
	public void disconnect() {
		fc.removeListener (this);
		fsl.disconnect ();
	}
}