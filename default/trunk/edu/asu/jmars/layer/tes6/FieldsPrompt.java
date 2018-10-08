package edu.asu.jmars.layer.tes6;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import edu.asu.jmars.swing.STable;


public class FieldsPrompt extends JDialog {

	/**
	 * Stop eclipse from complaining.
	 */
	private static final long serialVersionUID = 1L;
	
	private STable table;
	private FieldTableModel tableModel;
	private JScrollPane  tableSp;
	private JButton sendToFieldsButton;
	private JButton sendToSelectsButton;
	private JButton sendToOrderBysButton;
	private JPanel  buttonBox;
	private JTextPane fieldDetailPane;
	private JScrollPane fieldDetailSp;
	private JSplitPane mainSplitPane;
	private EventListenerList listenerList;
	private Vector fields;
	
	private static final Map columnWidths;
	static {
		Map m = new HashMap();
		m.put("Field", new Integer(150));
		m.put("Description", new Integer(300));
		
		columnWidths = m;
	}
	
	public FieldsPrompt(Frame owner){
		super(owner,"Fields Prompt");
		initialLayout();
		listenerList = new EventListenerList();
		populateInitialFieldsList();
	}
	
	/**
	 * Add field created afterwards by the user.
	 */
	public void addField(FieldDesc fieldDesc){
		tableModel.addField(fieldDesc);
	}
	
	private void initialLayout(){
		getContentPane().setLayout(new BorderLayout());
		table = new STable();
		tableModel = new FieldTableModel();
		table.setUnsortedTableModel(tableModel);
		
		//table.setCellSelectionEnabled(false);
		table.setColumnSelectionAllowed(false);
		table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener(){
			public void valueChanged(ListSelectionEvent e) {
		        int[] selectedRows = table.getSorter().unsortRows(table.getSelectedRows());
		        FieldDesc fd = null;
		        
		        if (selectedRows.length > 0){
		        	fd = tableModel.getField(selectedRows[0]);
		        }
		        if (fd != null && fd.getToolTipText() != null){
			        fieldDetailPane.setText(fd.getToolTipText());
					fieldDetailPane.setCaretPosition(0);
		        }
		        else {
		        	fieldDetailPane.setText("");
		        }
			}
		});
		
		for(int i=0; i<tableModel.getColumnCount(); i++){
			Integer colWidth = (Integer)columnWidths.get(tableModel.getColumnName(i));
			if (colWidth != null)
				table.getColumnModel().getColumn(i).setPreferredWidth(((Integer)colWidth).intValue());
		}
		
		tableSp = new JScrollPane(table);
		tableSp.setPreferredSize(new Dimension(400,500));

		fieldDetailPane = new JTextPane();
		fieldDetailPane.setContentType("text/html");
		fieldDetailSp = new JScrollPane(fieldDetailPane);
		fieldDetailSp.setPreferredSize(new Dimension(400,150));
		
		mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, tableSp, fieldDetailSp);
		mainSplitPane.setResizeWeight(0.7);
		getContentPane().add(mainSplitPane, BorderLayout.CENTER);
		
		sendToFieldsButton = new JButton("To Fields");
		sendToFieldsButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				fieldPushActionPerformed(e, FieldPushEvent.PUSH_TO_FIELDS);
			}
		});
		sendToSelectsButton = new JButton("To Selects");
		sendToSelectsButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				fieldPushActionPerformed(e, FieldPushEvent.PUSH_TO_SELECTS);
			}
		});
		sendToOrderBysButton = new JButton("To Order Bys");
		sendToOrderBysButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				fieldPushActionPerformed(e, FieldPushEvent.PUSH_TO_ORDERBYS);
			}
		});

		JPanel innerBox = new JPanel(new GridLayout(3,1));
		innerBox.add(sendToFieldsButton);
		innerBox.add(sendToSelectsButton);
		innerBox.add(sendToOrderBysButton);
		buttonBox = new JPanel(new BorderLayout());
		buttonBox.add(innerBox, BorderLayout.NORTH);
		getContentPane().add(buttonBox, BorderLayout.EAST);
		
		pack();
	}
	
	private void fieldPushActionPerformed(ActionEvent e, int context){
		int selRowIdx = table.getSelectedRow();
		if (selRowIdx > -1){
			FieldDesc fieldDesc = tableModel.getField(table.getSorter().unsortRow(selRowIdx));
			fireFieldPushEvent(context, fieldDesc);
		}
	}
	
	public void addFieldPushListener(FieldPushListener l){
        listenerList.add(FieldPushListener.class, l);
	}
	
	public void removeFieldPushListener(FieldPushListener l){
		listenerList.remove(FieldPushListener.class, l);
	}
	
	public void fireFieldPushEvent(int context, FieldDesc fieldDesc){
		EventListener[] listeners = 
			listenerList.getListeners(FieldPushListener.class);
		FieldPushEvent evt = null;
		
		//System.err.println("FieldPushEvent fired: "+fieldDesc.getFieldName());
		for(int i = 0; i < listeners.length; i++){
			if (evt == null){
				evt = new FieldPushEvent(this, context, fieldDesc); 
			}
			
			((FieldPushListener)listeners[i]).fieldPushed(evt);
		}
		
	}
	
	public static interface FieldPushListener extends EventListener {
		public void fieldPushed(FieldPushEvent evt);
	}
	
	public static class FieldPushEvent extends EventObject {
		/**
		 * Stop eclipse from complaining.
		 */
		private static final long serialVersionUID = 1L;
		
		/**
		 * Field Push Event sent to the ContextDetailPanel in a 
		 * response to the user selecting a field from the list
		 * and pressing the appropriate buttons from "Send to Fields",
		 * "Send to Selects" or "Send to OrderBy".
		 * 
		 * @param source
		 * @param context @see PUSH_TO_FIELDS, PUSH_TO_SELECTS, PUSH_TO_ORDERBYS
		 * @param fieldDesc
		 */
		public FieldPushEvent(Object source, int context, FieldDesc fieldDesc){
			super(source);
			
			this.context = context;
			this.fieldDesc = fieldDesc;
		}
		
		public int getContext(){ return context; }
		public FieldDesc getFieldDesc(){ return fieldDesc; }
		
		private int context;
		private FieldDesc fieldDesc;
		
		public static final int PUSH_TO_FIELDS = 1;
		public static final int PUSH_TO_SELECTS = 2;
		public static final int PUSH_TO_ORDERBYS = 3;
	}

	private void populateInitialFieldsList(){
		fields = FieldsPrompt.getFieldsFromDb();
		
		for(Iterator fi = fields.iterator(); fi.hasNext();){
			FieldDesc fd = (FieldDesc)fi.next();
			addField(fd);
		}
	}
	
	private static Vector getFieldsFromDb(){
		String name, desc, tableName, tip;
		boolean isFpField, isArrayField, isSpectralField;
		Class fieldType;
		FieldDesc fieldDesc;
		Vector fields = new Vector();
		Connection c = null;
		
		try {
			DbUtils dbUtils = DbUtils.getInstance();
			
			String sql =
				"select "+
					"field_name, field_desc, table_name, "+
					"is_fp_field, is_array_field, field_type, field_tip, is_spectral_field "+
				"from "+
					dbUtils.fieldDescTable+" "+
				"order by "+
					"field_name";
			
			c = dbUtils.createConnection();
			Statement stmt = c.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while(rs.next()){
				name = rs.getString(1);
				desc = rs.getString(2);
				tableName = rs.getString(3);
				isFpField = rs.getBoolean(4);
				isArrayField = rs.getBoolean(5);
				isSpectralField = rs.getBoolean(8);
				try {
					fieldType = Class.forName(rs.getString(6));
					tip = rs.getString(7);
					
					fieldDesc = new FieldDesc(
							name, desc, tip, tableName,
							isFpField, isArrayField, isSpectralField,
							fieldType);
					fields.add(fieldDesc);
				}
				catch(ClassNotFoundException ex){
					ex.printStackTrace();
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		finally {
			if (c != null){ try { c.close(); } catch(SQLException ex){ ex.printStackTrace(); } }
		}
		
		return fields;
	}
	
	public static class FieldTableModel extends AbstractTableModel {
		public String[] columnNames = { "Field", "Description" };
		List fields = new ArrayList();
		
		public int getColumnCount() {
			return columnNames.length;
		}
		
		public String getColumnName(int columnIndex){
			return columnNames[columnIndex];
		}

		public int getRowCount() {
			return fields.size();
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			FieldDesc fd = (FieldDesc)fields.get(rowIndex);
			switch(columnIndex){
			case 0: return fd.getFieldName();
			case 1: return fd.getFieldDesc();
			}
			return null;
		}
		
		public void addField(FieldDesc fd){
			fields.add(fd);
			fireTableRowsInserted(fields.size()-1, fields.size()-1);
		}
		
		public FieldDesc getField(int row){
			return (FieldDesc)fields.get(row);
		}
	}
	
	public static void main(String[] args){
		JFrame frame = new JFrame("Test");
		frame.setSize(200,200);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		final FieldsPrompt fl = new FieldsPrompt(frame);
		fl.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
		
		JButton popupButton = new JButton("Show Popup");
		popupButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				fl.setVisible(true);
			}
		});
		frame.getContentPane().add(popupButton);
		frame.setVisible(true);
	}
	
}

