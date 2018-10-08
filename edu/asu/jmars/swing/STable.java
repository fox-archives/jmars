/**
 **  An extention to a JTable that allows the sorting of rows by multiple columns and the 
 **  selection of columns to display.
 **  Right-clicking anywhere on the header bar brings up a dialog with a checkbox for 
 **  each of the columns that may be displayed in the table. All currently displayed columns are 
 **  already checked.  Columns are displayed and hidden by checking and unchecking the 
 **  corresponding checkboxes. 
 *
 *  NOTE: SortingTableModel.forward is used to relay table model events to STable, however if it
 * receives 'unknown' table model events, it will generate a 'update everything' event instead.
 * This is due to various parts of the application sending incorrect events or event ranges. 
 * A side effect of the 'update everything' event is that user selections are discarded.  If you
 * are using STable and finding that your selections disappear when you don't think they should,
 * you could be sending incorrect (or at least unrecognized) table model events.
 *
 **/

package edu.asu.jmars.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.plaf.basic.BasicTableUI;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import edu.asu.jmars.Main;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.util.stable.BooleanCellEditor;
import edu.asu.jmars.util.stable.BooleanCellRenderer;
import edu.asu.jmars.util.stable.ColumnDialog;
import edu.asu.jmars.util.stable.DoubleCellEditor;
import edu.asu.jmars.util.stable.FilteringColumnModel;
import edu.asu.jmars.util.stable.IntegerCellEditor;
import edu.asu.jmars.util.stable.NumberCellRenderer;
import edu.asu.jmars.util.stable.Sorter;
import edu.asu.jmars.util.stable.SortingTableModel;
import edu.asu.jmars.util.stable.TextCellEditor;
import edu.asu.jmars.util.stable.TextCellRenderer;

/** 
 * A JTable that provides column filtering and sorting. There are several
 * differences from a normal JTable:
 * <ul>
 * <li>The TableColumnModel is initialized to FilteringColumnModel, so the user
 * can choose which columns they want to see.
 * <li>Client code should not call setModel(). Instead it should call
 * setUnsortedTableModel().
 * <li>ColumnClickListener is attached to the JTableHeader to display the
 * column dialog on a right click (only if the column model is an instance of
 * FilteringColumnModel), and change the sort on a left click.
 * <li>Default editors and renderers are set for Boolean, Integer, Double, and
 * String. Other types exist in the edu.asu.jmars.util.stable package, but they
 * are not general and must be added by the clients.
 * <li>The header renderers will always be used to reflect the current column
 * sorts, even on a custom JTableHeader.
 * </ul>
 */
public class STable extends JTable implements HierarchyListener {
	final private static DebugLog log = DebugLog.instance(); 
	
	private TableModel unsortedTableModel;
	private SortingTableModel sortedTableModel;
	private Sorter sorter;
	private ColumnClickListener clickListener;
	private NoColumnDialogListener noColumnClickHandler = new NoColumnDialogListener();
	private SortChangeHandler sortChangeHandler = new SortChangeHandler();
	private JDialog columnDialog;
	
	private TableCellRenderer defaultHeaderRenderer;
	private TableCellRenderer[] headerAsc = new TableCellRenderer[] {
		getHeaderRenderer("resources/FirstIncrease.png"),
		getHeaderRenderer("resources/SecondIncrease.png"),
	};
	private TableCellRenderer[] headerDesc = new TableCellRenderer[] {
		getHeaderRenderer("resources/FirstDecrease.png"),
		getHeaderRenderer("resources/SecondDecrease.png")
	};
	protected Color nullColor = new Color(144,144,144);
	protected JLabel nullRenderer = new JLabel("<null>");
	{
		nullRenderer.setOpaque(true);
		nullRenderer.setHorizontalAlignment(JLabel.CENTER);
	}
	protected Font nullFont = nullRenderer.getFont().deriveFont(Font.ITALIC);
	
	public STable() {
		this(true);
	}
	
	private String saveKey;
	/**
	 * A special constructor for STable that specifies a saveKey to use if persisting settings for this table
	 * to a user store (ie. Stamp layer column defaults)
	 * @param saveKey
	 */
	public STable(String saveKey) {
		this(true);
		this.saveKey = saveKey; 
	}
	
	public STable(boolean installClickListener) {
		super ();
		
		if (installClickListener) {
			clickListener = new ColumnClickListener();
		}
		
		setUI(new FixedUI());

		// Create the sorter
		sorter = new Sorter ();

		// Use FilteringColumnModel instead of the JTable default
		setColumnModel(new FilteringColumnModel());

		// Setting the table header attaches the click listener
		setTableHeader(getTableHeader());

		// Keeps a no-columns-shown mouse listener on the STable's parent
		this.addHierarchyListener(this);

		// Listens to key events and searches the first sorted column with the recently hit keys
		addKeyListener(getSearchListener());
		
		// Set up default support for displaying and editing
		setTypeSupport(Boolean.class, new BooleanCellRenderer(), new BooleanCellEditor());
		setTypeSupport(Integer.class, new NumberCellRenderer(), new IntegerCellEditor());
		setTypeSupport(Double.class, new NumberCellRenderer(), new DoubleCellEditor());
		setTypeSupport(String.class, new TextCellRenderer(), new TextCellEditor());
	}
	
	/** Customizes the cell renderers to have the standard colors */
	public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
		Object value = getValueAt(row, column);
		Component c;
		if (value == null) {
			c = nullRenderer;
		} else {
			c = super.prepareRenderer(renderer, row, column);
		}
		if (isCellSelected(row, column)) {
			c.setBackground(getSelectionBackground());
			c.setForeground(getSelectionForeground());
		} else {
			c.setBackground(getBackground());
			if (value != null) {
				c.setForeground(getForeground());
			} else {
				c.setForeground(Color.gray);
			}
		}
		if (value != null) {
			c.setFont(getFont());
		} else {
			c.setFont(nullFont);
		}
		return c;
	}
	
	public void hierarchyChanged(HierarchyEvent e) {
		noColumnClickHandler.setParent (getParent());
	}

	public String getToolTipText(MouseEvent e) {
		String text = super.getToolTipText(e);
		if (text == null) {
			return text;
		} else {
			int width = Config.get("stable.tooltip.width",100);
			return "<html>" + Util.foldText(text, width, "<br>") + "</html>";
		}
	}
	
	/**
	 * Sets the table header as done by JTable, and also connects a
	 * MouseListener to show the column dialog on a right click, or change the
	 * sort on a left click.
	 * Don't pass null as the new header value.
	 */
	public void setTableHeader (JTableHeader header) {
		// move the listener from the old table header to the new one
		if (clickListener != null) {
			JTableHeader oldHeader = getTableHeader();
			if (oldHeader != null)
				oldHeader.removeMouseListener (clickListener);
			header.addMouseListener(clickListener);
		}

		// set the new header and get the new 'unsorted' header renderer
		super.setTableHeader (header);
		defaultHeaderRenderer = header.getDefaultRenderer();
	}

	/**
	 * Clients should not set the TableModel directly. Instead, call
	 * setUnsortedTableModel().
	 */
	public void setModel (TableModel model) {
		super.setModel (model);
	}

	/**
	 * When columns are added, the JTableHeader renderers are updated so the
	 * ascending/descending icons are correct (since a hidden sorted column
	 * needs the renderer reset.)
	 */
	public void columnAdded (TableColumnModelEvent e) {
		super.columnAdded(e);
		updateHeaders ();
	}

	/**
	 * When columns are removed, the sorted columns are intersected with the
	 * model columns, and any sorted columns lost there are removed from the
	 * sorter.
	 */
	public void columnRemoved (TableColumnModelEvent e) {
		super.columnRemoved(e);
		List modelColumns = getAllColumns();
		List sortColumns = new LinkedList(sorter.getSorts());
		sortColumns.removeAll(modelColumns);
		if (sortColumns.size() > 0)
			sorter.removeSorts(sortColumns);
	}

	/**
     * Override of JTable's createDefaultColumnsFromModel to handle the case when
     * the columns in the FilteringColumnModel change.
	 */
	public void createDefaultColumnsFromModel() {
		TableModel m = getModel();
		if (m != null) {
			// Remove any current columns
			TableColumnModel tcm = getColumnModel();

			if (tcm instanceof FilteringColumnModel) {
				FilteringColumnModel fcm = (FilteringColumnModel) tcm;
				fcm.removeAllColumns();
			}
			// Create new columns from the data model info
			for (int i = 0; i < m.getColumnCount(); i++) {
				TableColumn newColumn = new TableColumn(i);
				addColumn(newColumn);
			}
		}
	}

	/**
	 * * returns the current table model.
	 */
	public TableModel getUnsortedTableModel() {
		return unsortedTableModel;
	}

	/**
	 * Sets the unsorted TableModel. A SortingTableModel is created around it,
	 * and set on the JTable. The unsorted model is then shared with the Sorter.
	 * Finally, the SortedTableModel becomes a listener on sort changes.
	 */
	public void setUnsortedTableModel(TableModel tm) {
		// Remove prior sort change listeners
		if (sortedTableModel != null)
			sorter.removeListener(sortedTableModel);
		sorter.removeListener(sortChangeHandler);

		// Wrap a sorting model around the unsorted model
		unsortedTableModel = tm;
		sortedTableModel = new SortingTableModel(sorter);
		sorter.setModel(tm);
		sortedTableModel.setModel(tm);

		// Sort changes must be processed by STable last to retain selections
		// on a sort change
		sorter.addListener(sortedTableModel);
		sorter.addListener(sortChangeHandler);

		// Finally, tell the JTable about its new TableModel
		super.setModel(sortedTableModel);
	}

	/**
	 * Returns the Sorter used by this STable.
	 */
	public Sorter getSorter() {
		return sorter;
	}

	/**
	 * Convenience method for setting the renderer and editor for the given type.
	 * @param type Class of cell that will be affected.
	 * @param renderer Renderer to draw cells of the given type.
	 * @param editor Editor to edit cells of the given type.
	 */
	public void setTypeSupport (Class type, TableCellRenderer renderer, TableCellEditor editor) {
		setDefaultRenderer(type, renderer);
		setDefaultEditor(type, editor);
	}

	/**
	 * Returns an unmodifiable view of the Class -> TableCellEditor map.
	 */
	public Map getDefaultEditorsByColumnClass() {
		return Collections.unmodifiableMap(this.defaultEditorsByColumnClass);
	}

	/**
	 * Returns an unmodifiable view of the Class -> TableCellRenderer map.
	 */
	public Map getDefaultRenderersByColumnClass() {
		return Collections.unmodifiableMap(this.defaultRenderersByColumnClass);
	}

	/**
	 * Get a List view of all columns. If the TableColumnModel is an instance of
	 * FilteringColumnModel, that class' getAllColumns() method is used.
	 */
	private List getAllColumns() {
		if (columnModel instanceof FilteringColumnModel) {
			return ((FilteringColumnModel) columnModel).getAllColumns();
		} else {
			List ret = new LinkedList();
			for (Enumeration en = getColumnModel().getColumns(); en.hasMoreElements(); )
				ret.add(en.nextElement());
			return ret;
		}
	}

	/**
	 * If this table's column model is ever replaced, make sure any column
	 * dialog from an old filtering column model is disposed of properly.
	 */
	public void setColumnModel(TableColumnModel model) {
		TableColumnModel oldModel = getColumnModel();
		if (oldModel != model && oldModel instanceof FilteringColumnModel && columnDialog != null) {
			columnDialog.dispose();
		}
		super.setColumnModel(model);
	}

	/**
	 * @return the column filtering dialog, or null if a FilteringColumnModel is
	 *         not in use. The nearest Frame owner of this STable will be the
	 *         parent of this column dialog.
	 */
	public JDialog getColumnDialog() {
		if (columnDialog == null && getColumnModel() instanceof FilteringColumnModel) {
			Frame parent = (Frame)SwingUtilities.getAncestorOfClass(Frame.class, this);
			columnDialog = new ColumnDialog(parent, (FilteringColumnModel)getColumnModel(), saveKey);
			columnDialog.setLocationRelativeTo(parent);
		}
		return columnDialog;
	}

	/**
	 * Used to replace the default ColumnDialog with a new one, perhaps with layer specific functionality
	 * @param newColumnDialog
	 */
	public void setColumnDialog(JDialog newColumnDialog) {
		columnDialog = newColumnDialog;
	}
	
	/**
	 * Updates the header renderers to indicate the current sorts applied.
	 * 
	 * Should be called when the column model shows previously hidden columns
	 * since they could be sorted, and when a header is clicked to change the
	 * sort.
	 */
	private void updateHeaders() {
		for (int i = 0; i < getColumnModel().getColumnCount(); i++) {
			TableColumn column = getColumnModel().getColumn(i);
			int pos = sorter.getSorts().indexOf(column);
			switch (sorter.getDirection(column)) {
			case -1:
				column.setHeaderRenderer(headerDesc[pos]);
				break;
			case 0:
				column.setHeaderRenderer(defaultHeaderRenderer);
				break;
			case 1:
				column.setHeaderRenderer(headerAsc[pos]);
				break;
			}
		}
	}

	private TableCellRenderer getHeaderRenderer(final String resource) {
		return new TableCellRenderer() {
			public Component getTableCellRendererComponent(JTable table,
					Object value, boolean isSelected, boolean hasFocus,
					int row, int col) {
				Component c = defaultHeaderRenderer.getTableCellRendererComponent(
						table, value, isSelected, hasFocus, row, col);
				JPanel panel = new JPanel();
				panel.setLayout(new BorderLayout());
				panel.add(c, BorderLayout.CENTER);
				JLabel label = new JLabel(new ImageIcon(Main.getResource(resource)));
				panel.add(label, BorderLayout.EAST);
				return panel;
			}
		};
	}

	/**
	 * This key listener searches the first sorted column of the table for a
	 * sentence the user creates by simply typing while the table has focus and
	 * no editing is going on. New keys are added to the end of the old sentence
	 * if less than three seconds has gone by since the last key. Each key press
	 * triggers a binary search on the first sorted column using the existing
	 * index on {@link Sorter}. ISO control keys clear the sentence so the next
	 * regular key starts a new sentence. Additionally, the escape key clears the
	 * current selection and the backspace key removes the last key.
	 */
	private KeyListener getSearchListener() {
		return new KeyListener() {
			private static final int KEY_DELAY_MS = 3000;
			private long lastTime = 0;
			private String search = "";

			public void keyPressed(KeyEvent e) {
			}

			public void keyReleased(KeyEvent e) {
			}

			public void keyTyped(KeyEvent e) {
				append(System.currentTimeMillis(), e.getKeyChar());
			}

			private void append(long time, char key) {
				try {
					List<TableColumn> sorts = sorter.getSorts();
					if (sorts.isEmpty() || isEditing()) {
						return;
					}
					final int column = sorts.get(0).getModelIndex();

					// special handling of some keys
					if (key == KeyEvent.VK_ESCAPE) {
						search = "";
						getSelectionModel().clearSelection();
						return;
					} else if (key == KeyEvent.VK_BACK_SPACE) {
						if (search.length() > 0) {
							search = search.substring(0, search.length() - 1);
						}
					} else if (Character.isISOControl(key)) {
						lastTime = 0;
						return;
					} else {
						if (time - lastTime > KEY_DELAY_MS) {
							search = "";
						}
						lastTime = time;
						search += key;
					}

					// convert key to proper type, so we get expected ordering of e.g. numbers vs. strings
					Class<?> type = sortedTableModel.getColumnClass(column);
					Comparable<? extends Object> target = null;
					if (Integer.class.isAssignableFrom(type)) {
						target = new Scanner(search).nextInt();
					} else if (Float.class.isAssignableFrom(type)) {
						target = new Scanner(search).nextFloat();
					} else if (Double.class.isAssignableFrom(type)) {
						target = new Scanner(search).nextDouble();
					} else if (String.class.isAssignableFrom(type)) {
						target = search.trim().toLowerCase();
					} else {
						// bail, column type not supported for searching
						return;
					}

					// construct virtual list to search, assuming contents are comparable
					final int size = sortedTableModel.getRowCount();
					final boolean descending = sorter.getDirection(sorts.get(0)) == -1;
					List<Comparable<Object>> rows = new AbstractList<Comparable<Object>>() {
						public Comparable<Object> get(int index) {
							if (descending) {
								index = size - 1 - index;
							}
							Object out = sortedTableModel.getValueAt(index, column);
							if (out instanceof String) {
								out = ((String) out).trim().toLowerCase();
							}
							return (Comparable<Object>) out;
						}

						public int size() {
							return size;
						}
					};

					// select the nearest matching row
					int row = Collections.binarySearch(rows, target);
					if (row < 0) {
						row = -row - 1;
					}
					row = Math.max(0, Math.min(row, size - 1));
					if (descending) {
						row = size - 1 - row;
					}
					getSelectionModel().setSelectionInterval(row, row);
					scrollRectToVisible(getCellRect(row, column, false));
					sortedTableModel.getValueAt(key, column);
				} catch (Exception e) {
				}
			}
		};
	}

	/**
	 * Returns view settings of visible columns, sorts and
	 * column order in a serializable map. Columns are identified
	 * by their model indices. See {@link #setViewSettings(Map)}
	 * for how these settings are restored.
	 * @return A non-null map of view settings.
	 */
	public Map getViewSettings() {
		Map m = new HashMap();

		if (getColumnModel() instanceof FilteringColumnModel) {
			FilteringColumnModel fcm = (FilteringColumnModel) getColumnModel();
			int cc = fcm.getColumnCount();
			Set vis = new LinkedHashSet(cc);
			for (int i = 0; i < cc; i++) {
				Object id = fcm.getColumn(i).getIdentifier();
				vis.add(id);
			}
			m.put("visibleColumns", vis);

			Sorter s = getSorter();
			List sortedColumns = s.getSorts();
			Map sorts = new LinkedHashMap(sortedColumns.size());
			for (Iterator si = sortedColumns.iterator(); si.hasNext();) {
				TableColumn tc = (TableColumn) si.next();
				Integer id = new Integer(tc.getModelIndex());
				sorts.put(id, new Integer(s.getDirection(tc)));
			}
			m.put("sortOrder", sorts);
		}

		TableColumnModel tcm = getColumnModel();
		int cc = tcm.getColumnCount();

		Map columnOrder = new LinkedHashMap(cc);
		for (int i = 0; i < cc; i++) {
			TableColumn tc = tcm.getColumn(i);
			columnOrder.put(tc.getIdentifier(), new Integer(i));
		}
		m.put("columnOrder", columnOrder);

		Map widths = new HashMap(cc);
		for (int i = 0; i < cc; i++) {
			TableColumn tc = tcm.getColumn(i);
			Integer id = new Integer(tc.getModelIndex());
			widths.put(id, new Integer(tc.getWidth()));
		}
		m.put("columnWidths", widths);

		return m;
	}

	/**
	 * Apply settings of visible columns, sort order
	 * and column order to this table. Such settings would have been
	 * saved during a previous call to {@link #getViewSettings()}.
	 * @param m Non-null map of settings.
	 */
	public void setViewSettings(Map m) {
		TableColumnModel tcm = getColumnModel();

		// Create a reverse mapping from column model index to table column
		Map modelIdxToCol = new HashMap();
		if (tcm instanceof FilteringColumnModel) {
			for(Iterator it=((FilteringColumnModel)tcm).getAllColumns().iterator(); it.hasNext(); ){
				TableColumn tc = (TableColumn) it.next();
				modelIdxToCol.put(new Integer(tc.getModelIndex()), tc);
			}
		}
		else {
			Enumeration e = tcm.getColumns();
			while (e.hasMoreElements()) {
				TableColumn tc = (TableColumn) e.nextElement();
				modelIdxToCol.put(new Integer(tc.getModelIndex()), tc);
			}
		}

		Object o;
		if (tcm instanceof FilteringColumnModel) {
			FilteringColumnModel fcm = (FilteringColumnModel) tcm;

			o = m.get("visibleColumns");
			if (o instanceof Set) {
				Set vis = (Set) o;
				for (Iterator ci = fcm.getAllColumns().iterator(); ci.hasNext();) {
					TableColumn tc = (TableColumn) ci.next();
					Object id = tc.getIdentifier();

					fcm.setVisible(tc, vis.contains(id));
				}
			} else {
				System.err.println("Saved settings for visible columns not of expected type. Ignored!");
			}

			o = m.get("sortOrder");
			if (o instanceof Map) {
				Sorter s = getSorter();
				s.clearSorts();

				Map sorts = (Map) o;
				for (Iterator si = sorts.keySet().iterator(); si.hasNext();) {
					Object id = si.next();
					TableColumn tc = (TableColumn) modelIdxToCol.get(id);
					if (tc != null && sorts.get(id) instanceof Integer)
						s.setDirection(tc, ((Integer) sorts.get(id)).intValue());
					else
						System.err.println(tc == null? "1Unknown model column "+id: "Non-integer sort direction for model column "+id);
				}
				}
			else {
				System.err.println("Saved settings for sort order not of expected type. Ignored!");
			}
		}

		o = m.get("columnOrder");
		if (o instanceof Map) {
			Map columnOrder = (Map) o;

			for (Iterator oi = columnOrder.keySet().iterator(); oi.hasNext();) {
				Object id = oi.next();

				ArrayList<Object> myColumns = new ArrayList<Object>();
				for (int i = 0; i < tcm.getColumnCount(); i++) {
					myColumns.add(tcm.getColumn(i).getIdentifier());
				}

				if (!myColumns.contains(id)) {
					break;
				}

				int oldIndex = tcm.getColumnIndex(id);
				int newIndex = (int) columnOrder.get(id);

				tcm.moveColumn(oldIndex, newIndex);
			}

		} else {
			System.err.println("Saved settings for column order not of expected type. Ignored!");
		}

		o = m.get("columnWidths");
		if (o instanceof Map) {
			Map widths = (Map) o;

			for (Iterator wi = widths.keySet().iterator(); wi.hasNext();) {
				Object id = wi.next();
				TableColumn tc = (TableColumn) modelIdxToCol.get(id);
				if (tc != null && widths.get(id) instanceof Integer){
					tc.setPreferredWidth(((Integer) widths.get(id)).intValue());
				}else{
					log.println(tc == null? "2Unknown model column "+id: "Non-integer column width for model column "+id);
				}
			}
		}

		else {
			System.err.println("Saved settings for column widths not of expected type. Ignored!");
		}
	}

	/**
	 * Since ListSelectionModel is so complex, we implement a sort change
	 * listener instead that converts the sorted selection indices into unsorted
	 * indices, allows the sort to happen, and then converts them back into
	 * sorted indices and replaces the model contents.
	 * 
	 * The sortChanged() method also updates the table header renderers.
	 */
	class SortChangeHandler implements Sorter.Listener {
		int[] unsortSelectedRows;
		int size;

		/**
		 * Saves the unsorted selections for later
		 */
		public void sortChangePre() {
			// the sorter size at this point reflects the old row count
			size = sorter.getSize();

			// Get the unsorted selected row indices
			int[] selectedRows = getSelectedRows();
			unsortSelectedRows = new int[selectedRows.length];
			for (int i = 0; i < unsortSelectedRows.length; i++) {
				unsortSelectedRows[i] = sorter.unsortRow(selectedRows[i]);
			}
		}

		/**
		 * Converts the unsorted selections into the selections on the new
		 * sorts, replaces the selections on the model, and updates the table
		 * header.
		 */
		public void sortChanged() {
			// Only update the selections if the row count is the same
			// .. if it changed, deletes are handled and adds don't matter
			// here.
			if (size != sorter.getSize())
				return;

			// get the new sorted indices
			int[] selectedRows = new int[unsortSelectedRows.length];
			for (int i = 0; i < selectedRows.length; i++) {
				selectedRows[i] = sorter.sortRow(unsortSelectedRows[i]);
			}

			int[][] ranges = Util.binRanges(selectedRows);
			if (ranges.length > 0) {
				ListSelectionModel selModel = getSelectionModel();
				// set the selections
				selModel.setValueIsAdjusting(true);
				selModel.clearSelection();
				for (int i = 0; i < ranges.length; i++)
					selModel.addSelectionInterval(ranges[i][0], ranges[i][1]);
				selModel.setValueIsAdjusting(false);

				// ensure last selection is visible
				int row = ranges[ranges.length - 1][1];
				Rectangle rect = getCellRect(row, 0, true);
				// and ensure that the column positioning within the viewport does not get reset
				rect = rect.union(getCellRect(row, getColumnCount() - 1, true));
				scrollRectToVisible(rect);
			}

			// update the headers
			updateHeaders();
		}
	}

	/**
	 * Right clicks when no columns are shown will reveal the column dialog, if
	 * filtering.
	 */
	class NoColumnDialogListener extends MouseAdapter {
		private Container parent;

		public void setParent(Container p) {
			if (parent == p)
				return;
			if (parent != null)
				parent.removeMouseListener(this);
			parent = p;
			if (parent != null)
				parent.addMouseListener(this);
		}

		public void mouseClicked(MouseEvent e) {
			TableColumnModel m = getColumnModel();
			if (SwingUtilities.isRightMouseButton(e)
					&& m instanceof FilteringColumnModel
					&& m.getColumnCount() == 0)
				getColumnDialog().setVisible(true);
		}
	}

	/**
	 * Mouse header right clicks show the column dialog, if the column model
	 * filters. Left clicks adjust the sort.
	 */
	class ColumnClickListener extends MouseAdapter {
		public void mouseClicked(MouseEvent e) {
			TableColumnModel m = getColumnModel();
			if (SwingUtilities.isRightMouseButton(e)) {
				if (m instanceof FilteringColumnModel) {
					getColumnDialog().setLocationRelativeTo(e.getComponent());
					getColumnDialog().setVisible(true);
				}
			} else if (sorter != null) {
				boolean shiftPressed = (0 != (e.getModifiers() & InputEvent.SHIFT_MASK));
				int columnIndex = m.getColumnIndexAtX(e.getX());
				if (columnIndex < 0) // not a valid column location
					return;
				TableColumn column = m.getColumn(columnIndex);
				if (shiftPressed)
					sorter.addSort(column);
				else
					sorter.setSort(column);
			}
		}
	}

	/**
	 * The JTable appears to set the viewport outside the valid row range when
	 * processing a delete from the TableModel (or, equivalently, it doesn't
	 * always change the viewport prior to the first paint().)
	 * 
	 * BasicTableUI as of JRE 1.4.2_09 is bugged. There is a degenerate case in
	 * paint() where it will render every cell if the first row in the view and
	 * the last row in the view are outside the legal tableModel range. When
	 * this happens because of a delete, we repaint all the remaining rows,
	 * which can take a LONG time.
	 * 
	 * When the viewport is totally disjoint with the table rows, nothing should
	 * be drawn, not everything. So the fixed BasicTableUI's paint() method
	 * returns early if it detects this degenerate case.
	 */
	private static final class FixedUI extends BasicTableUI {
		public void paint(Graphics g, JComponent c) {
			if (table.getRowCount() <= 0 || table.getColumnCount() <= 0)
				return;
			Rectangle clip = g.getClipBounds();
			Point upperLeft = clip.getLocation();
			Point lowerRight = new Point(clip.x + clip.width - 1, clip.y
					+ clip.height - 1);
			int rMin = table.rowAtPoint(upperLeft);
			int rMax = table.rowAtPoint(lowerRight);
			if (rMin != -1 || rMax != -1)
				super.paint(g, c);
		}
	}

	
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction){
		return 16;
	}

} // end: class STable
