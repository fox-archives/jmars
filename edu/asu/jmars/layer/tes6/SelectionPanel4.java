package edu.asu.jmars.layer.tes6;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import org.jfree.chart.event.ChartChangeEvent;
import org.jfree.chart.event.ChartChangeListener;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;

import edu.asu.jmars.Main;
import edu.asu.jmars.swing.STable;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.util.stable.ColorCellEditor;
import edu.asu.jmars.util.stable.ColorCellRenderer;
import edu.asu.jmars.util.stable.Sorter;

public class SelectionPanel4 extends JPanel {
	/**
	 * to get eclipse to stop complaining 
	 */
	private static final long serialVersionUID = 1L;
	
	private static final DebugLog log = DebugLog.instance();
	
	public static final String COLOR_COLUMN_NAME = "C";
	public static final String LOCK_COLUMN_NAME = "L";
	
	TesLayer tesLayer;
	TesLView tesLView;
	TesContext.CtxImage ctxImage;
	
	Map<TesKey,Map<FieldDesc,Object>> dataMap;
	Vector<TesKey> keyVector;
	Set<TesKey> selectedRecs;
	Set<TesKey> lockedRecs;
	Vector<FieldDesc> defaultFields;
	Vector<FieldDesc> listFields;
	Vector<FieldDesc> graphFields;
	Vector<FieldDesc> fields;
	
	JPopupMenu listPopupMenu;
	BringToCenterMenuItem bringToCenterMenuItem;
	JMenuItem toggleLockMenuItem;
	STable list;
	AbstractTableModel listModel;
	JComboBox graphSelector;
	JComboBox saveSelector;
    JButton clearButton;
    JButton selectAllButton, selectNoneButton;
    JScrollPane listSp;
    JSplitPane splitPane;
    TesGraphPanel graphPanel;
    JPanel topPane, botPane;
    JPanel graphButtonBox, listButtonBox;
    
    
    JFileChooser fileChooser = null;
    CsvFileFilter csvFilter = new CsvFileFilter();
    TxtFileFilter txtFilter = new TxtFileFilter();
	SerializingThread updater = null;
	
    public SelectionPanel4(TesLView lview, TesContext ctx){
        this.tesLayer = (TesLayer)lview.getLayer();
        this.tesLView = lview;
        
       	ctxImage = ctx.getCtxImage();
        
        dataMap = new LinkedHashMap<TesKey,Map<FieldDesc,Object>>();
        keyVector = new Vector<TesKey>();
        selectedRecs = new LinkedHashSet<TesKey>();
        lockedRecs = new LinkedHashSet<TesKey>();
        
        updater = new SerializingThread(
        		"TesSelectionPanelUpdater-"+
        		((tesLView.getChild()==null)?"Panner":"Main"));
        updater.start();
        
        defaultFields = new Vector<FieldDesc>();
        defaultFields.add(tesLayer.getColorField());
        defaultFields.add(tesLayer.getOckField());
        defaultFields.add(tesLayer.getIckField());
        defaultFields.add(tesLayer.getDetField());
        
        fields = combineDefaultAndUserFields(defaultFields, Arrays.asList(ctxImage.getFields()));
        listFields = new Vector<FieldDesc>(filter(fields, nonArrayFieldsFilter));
       	graphFields = new Vector<FieldDesc>(filter(fields, arrayFieldsFilter));
        
        initialLayout();
        
        updateGraphPanelButtons(new Vector<FieldDesc>(), graphFields);
    }
    
    private Vector<FieldDesc> combineDefaultAndUserFields(Collection<FieldDesc> defaultFields, Collection<FieldDesc> userFields){
    	Set<FieldDesc> uniqueSet = new HashSet<FieldDesc>(defaultFields);
    	Vector<FieldDesc> unique = new Vector<FieldDesc>(defaultFields);
    	
    	for(FieldDesc fieldDesc: userFields){
    		if (!uniqueSet.contains(fieldDesc))
    			unique.add(fieldDesc);
    		uniqueSet.add(fieldDesc);
    	}
    	
    	return unique;
    }
    
    private void resetColumnWidths(JTable table){
        TableColumn tc;
        if ((tc = table.getColumn(COLOR_COLUMN_NAME)) != null){
        	tc.setPreferredWidth(12);
        	tc.setWidth(12);
        }
    	
        if ((tc = table.getColumn(LOCK_COLUMN_NAME)) != null){
        	tc.setPreferredWidth(12);
        	tc.setWidth(12);
        }
    }
    
    /**
     * Create UI components and do their initial layout.
     *
     */
    private void initialLayout(){
    	/* Create the selection table */
    	listModel = new PickListTableModelAdapter();
        list = new STable();
        list.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        list.setUnsortedTableModel(listModel);
        list.getSelectionModel().addListSelectionListener(new ListSelectionListener(){
            public void valueChanged(ListSelectionEvent e){
            	processListSelectionEvent(e);
            }
        });
        list.addMouseListener(new MouseListener(){
        	public void mouseClicked(MouseEvent evt){
        		processListMouseClickedEvent(evt);
        	}
        	public void mousePressed(MouseEvent evt){}
        	public void mouseReleased(MouseEvent evt){}
        	public void mouseEntered(MouseEvent evt){}
        	public void mouseExited(MouseEvent evt){}
        });
        list.setTypeSupport(Color.class, new ColorCellRenderer(), new ColorCellEditor());
        list.setTypeSupport(Lock.class, new LockTableCellRenderer(), new LockTableCellEditor());
        resetColumnWidths(list);
        
        listSp = new JScrollPane(list);
        listSp.setPreferredSize(new Dimension(400, 200));
        
        /* Create the list popup menu */
        bringToCenterMenuItem = new BringToCenterMenuItem();
        bringToCenterMenuItem.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent evt){
        		processBringToCenterMenuItemActionEvent(evt);
        	}
        });
        
        toggleLockMenuItem = new JMenuItem("Lock/Unlock Selected Records");
        toggleLockMenuItem.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent evt){
        		processToggleLockMenuItem(evt);
        	}
        });
        
        listPopupMenu = new JPopupMenu();
        listPopupMenu.add(bringToCenterMenuItem);
        listPopupMenu.add(toggleLockMenuItem);
        
        /* Create a combo-box to select which field to plot. */
        graphSelector = new JComboBox();
        graphSelector.setRenderer(new FieldCellRenderer());
        graphSelector.addItemListener(new ItemListener(){
			public void itemStateChanged(ItemEvent e) {
				processGraphSelectorItemStateChangedEvent(e);
			}
        });
        
        graphButtonBox = new JPanel(new FlowLayout());
        graphButtonBox.add(new JLabel("Plot Field:"));
        graphButtonBox.add(graphSelector);

        /* Create select-all button */
        selectAllButton = new JButton("Select All");
        selectAllButton.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent evt){
        		selectAllButtonActionPerformed(evt);
        	}
        });
        
        /* Create select-none button */
        selectNoneButton = new JButton("Select None");
        selectNoneButton.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent evt){
        		selectNoneButtonActionPerformed(evt);
        	}
        });
        
        /* Create selected/all records clear button */
        clearButton = new JButton("Clear");
        clearButton.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent evt){
        		clearButtonActionPerformed(evt);
        	}
        });
        
        /* Create save combo box */
        String[] saveList = {"Select action...","Save Vanilla File", "Save CSV Shapefile"};
        saveSelector = new JComboBox(saveList);
        saveSelector.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent evt) {
        		saveSelectionActionPerformed(evt);
        	}
        });
        
        listButtonBox = new JPanel(new FlowLayout());
        listButtonBox.add(selectAllButton);
        listButtonBox.add(selectNoneButton);
        listButtonBox.add(new JSeparator(SwingConstants.VERTICAL));
        listButtonBox.add(clearButton);
        listButtonBox.add(new JSeparator(SwingConstants.VERTICAL));
        listButtonBox.add(saveSelector);
        listButtonBox.add(new JSeparator(SwingConstants.VERTICAL));
        

    	/* Create a graphing canvas */
        graphPanel = new TesGraphPanel();
        graphPanel.setPreferredSize(new Dimension(400, 200));
        graphPanel.setBackground(Color.white);
        graphPanel.getChart().addChangeListener(new ChartChangeListener(){
			public void chartChanged(ChartChangeEvent event) {
				//listModel.fireTableChanged(new TableModelEvent(listModel, 0, listModel.getRowCount(), 0, TableModelEvent.UPDATE));
				SwingUtilities.invokeLater(new Runnable(){
					public void run(){
						list.repaint();
					}
				});
				//list.repaint();
			}
        });

        /* Box it up into panels within a split pane */
        topPane = new JPanel(new BorderLayout());
        topPane.add(listSp, BorderLayout.CENTER);
        topPane.add(listButtonBox, BorderLayout.NORTH);

        botPane = new JPanel(new BorderLayout());
        botPane.add(graphButtonBox, BorderLayout.NORTH);
        botPane.add(graphPanel, BorderLayout.CENTER);

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setTopComponent(topPane);
        splitPane.setBottomComponent(botPane);
        
        setLayout(new BorderLayout());
        add(splitPane, BorderLayout.CENTER);
    }
    
    protected void processBringToCenterMenuItemActionEvent(ActionEvent evt) {
    	BringToCenterMenuItem menuItem = (BringToCenterMenuItem)evt.getSource();
    	TesKey key = menuItem.getKey();
    	//System.err.println("Bring to center called with "+key.getOck()+" "+key.getIck()+" "+key.getDet()+".");
    	
    	Map<FieldDesc,Object> recData = dataMap.get(key);
    	// NOTE: This poly is in East-Lon coordinates
    	SerializablePoly poly = (SerializablePoly)recData.get(tesLayer.getPolyField());
    	if (poly != null){
    		Rectangle2D bounds = poly.getBounds2D();
    		Point2D centerAt = new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());
    		centerAt = ShapeUtils.spatialEastLonToWorld(tesLView, centerAt);
    		//System.err.println("Centering to "+centerAt);
    		
    		// LView.centerAtPoint(p) requires p be in world coords
    		tesLView.centerAtPoint(centerAt);
    	}
	}
    
    protected void processToggleLockMenuItem(ActionEvent evt){
    	List<TesKey> keys = new ArrayList<TesKey>();
    	
    	int[] rows = list.getSorter().unsortRows(list.getSelectedRows());
    	for(int i=0; i<rows.length; i++)
    		keys.add(keyVector.get(rows[i]));
    	
    	toggleLockStatus(keys);
    }

    /**
     * "Bring To Center" menu item for the context menu. We initialize
     * only one such menu objects. The "key" in this menu object is
     * changed depending upon where we are hovering on the selection
     * list. This key determines the polygon which is brought to the
     * center of the screen.
     * 
     * @author saadat
     *
     */
	private class BringToCenterMenuItem extends JMenuItem {
    	/**
		 * added to stop eclipse from complaining
		 */
		private static final long serialVersionUID = 1L;
		
		public BringToCenterMenuItem(){
    		super();
			setEnabled(false);
    	}
    	
    	public TesKey getKey(){
    		return key;
    	}
    	public void setKey(TesKey key){
    		this.key = key;
    		setEnabled(key != null);
    	}
    	public String getText(){
    		if (key == null){
    			return basicText;
    		}
    		return basicText+"["+key.getOck()+","+key.getIck()+","+key.getDet()+"]";
    	}
    	public String getToolTipText(){
    		if (key == null){
    			return basicToolTip;
    		}
    		return "Centers JMars viewing window at ock "+key.getOck()+
    			" ick "+key.getIck()+" det "+key.getDet()+".";
    	}
    	
    	private TesKey key;
    	private String basicText = "Bring to center";
    	private String basicToolTip = "Centers the JMars viewing window at the data item under the mouse.";
    }

	/**
	 * Process the right mouse button click event that occurred
	 * on the selection list. As a result of such an event the
	 * "Bring to Center" menu item is updated with the key under
	 * the mouse and the popup menu is displayed.
	 *  
	 * @param e
	 */
	private void processListMouseClickedEvent(MouseEvent e){
    	int index = list.getSorter().unsortRow(list.rowAtPoint(e.getPoint()));
    	if (index > -1){
    		bringToCenterMenuItem.setKey((TesKey)keyVector.get(index));
    	}
    	else {
    		bringToCenterMenuItem.setKey(null);
    	}
    	
		if (SwingUtilities.isRightMouseButton(e)){
			listPopupMenu.show(e.getComponent(), e.getX(), e.getY());
		}
    	if (e.isPopupTrigger()){
    		System.err.println("Popup triggered.");
    	}
    }
    
    private void processListSelectionEvent(ListSelectionEvent e){
    	// Do nothing if the list is changing.
        if (e.getValueIsAdjusting()){ return; }

        // By the time we get this ListSelectionEvent, the sorter
        // hasn't gotton rid of the deleted row. This causes the
        // unsortRows() to return rows outside the indexing range 
        // of the keyVector, causing an index out of bounds exception.
        // This invokeLater allows the STable+Sorter to finish
        // organizing the indices, before selected records are
        // updated and pushed onto the plot.
        // An alternate way would have been to add an 
        //   if (rows[i] < keyVector.size() && rows[i] >= 0)
        // just before the selectedRecs.add(keyVector.get(rows[i]))
        SwingUtilities.invokeLater(new Runnable(){
        	public void run(){
                // and remove all currently highlighted polys
                selectedRecs.clear();
                
                int[] rows = list.getSorter().unsortRows(list.getSelectedRows());
                for(int i = 0; i < rows.length; i++){
                	selectedRecs.add(keyVector.get(rows[i]));
                }
                
                // Update the plot in the GraphPanel.
                updatePlottedRecords();

                // Repaint the selection outlines in the TesLView.
                tesLView.repaintSelection(true);
        	}
        });
    }
    
    private void updatePlottedRecords(){
    	Set<TesKey> prior = new LinkedHashSet<TesKey>(graphPanel.getRecs());
        graphPanel.removeAllRecs();
        
        Set<TesKey> current = new HashSet<TesKey>();
        current.addAll(lockedRecs);
        current.addAll(selectedRecs);
        
        prior.retainAll(current);
        current.removeAll(prior);
        prior.addAll(current);
        
        for(TesKey key: prior){
        	graphPanel.addRec(key);
        }
    }
    
    /**
     * 
     * @param g2 World Graphics2D
     */
    public void drawHighlightedPolys(Graphics2D g2, TesLView lview){
    	if (!lview.isAlive())
    		return;
    	
    	float[] dash = new float[]{ (float)(2.0/lview.getPPD()), (float)(2.0/lview.getPPD()) };
    	BasicStroke s1 = new BasicStroke((float)(1.0/lview.getPPD()), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, dash, 0);
    	BasicStroke s0 = new BasicStroke((float)(1.0/lview.getPPD()), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, dash, dash[0]);
    	BasicStroke s4 = new BasicStroke((float)(3.0/lview.getPPD()));
    	
    	for(TesKey key: lockedRecs){
    		GeneralPath gp = ((SerializablePoly)dataMap.get(key).get(tesLayer.getPolyField())).getBackingPoly();
    		gp = ShapeUtils.spatialEastLonPolyToWorldPoly(lview, gp);
    		
    		g2.setStroke(s4); g2.setColor(Color.yellow); g2.draw(gp);
    	}
    	
    	for(TesKey key: selectedRecs){
    		GeneralPath gp = ((SerializablePoly)dataMap.get(key).get(tesLayer.getPolyField())).getBackingPoly();
    		gp = ShapeUtils.spatialEastLonPolyToWorldPoly(lview, gp);
    		
    		//Color c = (Color)dataMap.get(key).get(tesLayer.getColorField());
    		
    		g2.setStroke(s1); g2.setColor(Color.white); g2.draw(gp);
    		g2.setStroke(s0); g2.setColor(Color.black); g2.draw(gp);
    	}
    }
    
    /**
     * Removes unlocked records. This is used in hunt and peck mode.
     * Must be called on the Swing thread.
     * Fires data change event on the table.
     * Updates graph panel with new records.
     * Repaints selected and locked records.
     */
    public void clearUnlockedRecords(){
        keyVector.retainAll(lockedRecs);
        dataMap.keySet().retainAll(lockedRecs);
        selectedRecs.retainAll(lockedRecs);

        // listModel.clearRows();
        listModel.fireTableDataChanged();
        
        updatePlottedRecords();
        
        tesLView.repaintSelection(true);
    }
    
    /**
     * Called when the graphed field selection is changed. It sets
     * the appropriate field in the graph panel for it to plot.
     * 
     * @param e
     */
    private void processGraphSelectorItemStateChangedEvent(ItemEvent e){
		FieldDesc field = (FieldDesc)e.getItem();
		
    	if (e.getStateChange() == ItemEvent.DESELECTED){
    		graphPanel.setPlotField(null);
    	}
    	else if (e.getStateChange() == ItemEvent.SELECTED){
    		graphPanel.setPlotField(field);
    	}
    }

    /**
     * Implementation of ActionEvent on selectAllButton.
     * @param evt
     */
    private void selectAllButtonActionPerformed(ActionEvent evt){
    	list.selectAll();
    }
    
    /**
     * Implementation of ActionEvent on selectNoneButton.
     * @param evt
     */
    private void selectNoneButtonActionPerformed(ActionEvent evt){
    	list.clearSelection();
    }
    
    /**
     * Implementation of ActionEvent on saveButton.
     * 
     * @param evt
     */
    private void saveSelectionActionPerformed(ActionEvent evt){
    	int selIndex = saveSelector.getSelectedIndex();
    	boolean saveGeometry = false;
		if (selIndex == 0) {
    		return;
    	} else if (selIndex == 2) {//index of 2 is save with Geometry
    		saveGeometry = true;
    	}
    	if (!updater.isIdle()){
    		// Warn user if the updater is active.
    		int rc = JOptionPane.showConfirmDialog(this, "Data is being pulled, "+
    					"you may get partial results. Shall I continue?");
    		if (rc == JOptionPane.CANCEL_OPTION || rc == JOptionPane.NO_OPTION){
    			saveSelector.setSelectedIndex(0);
    			return;
    		}
    	}
    	
    	if (fileChooser == null){
    		fileChooser = new JFileChooser();
    	}
    	if (saveGeometry) {
    		fileChooser.setFileFilter(csvFilter);
    	} else {
    		fileChooser.setFileFilter(txtFilter);
    	}
		int[] selection = list.getSorter().unsortRows(list.getSelectedRows());
		selection = (selection.length == 0)? null: selection;
		
    	fileChooser.setDialogTitle(
    			(selection==null)
    			?"Save all rows"
    			:"Save selected rows");
 
    	int rc = fileChooser.showSaveDialog(this);
    	if (rc == JFileChooser.APPROVE_OPTION){
    		String fileName =
    			fileChooser.getCurrentDirectory()+
    			System.getProperty("file.separator")+
    			fileChooser.getSelectedFile().getName();
    		if (!saveGeometry) {
	    		if (!fileName.endsWith(".txt")){
	    			fileName = fileName + ".txt";
	    		}
    		} else {
    			if (!fileName.endsWith(".csv")) {
    				fileName = fileName + ".csv";
    			}
    		}
    		
    		try {
    			Vector<TesKey> selectedKeys = keyVector;
    			
    			// Narrow to selected records if user specified a selection.
    			if (selection != null){
    				selectedKeys = new Vector<TesKey>();
    				selection = (int[])selection.clone();
    				Arrays.sort(selection);
    				for(int i=0; i<selection.length; i++){
    					selectedKeys.add(keyVector.get(selection[i]));
    				}
    			}
    			
    			int nRecs = saveRecsToFile(fileName, selectedKeys, saveGeometry);
    			JOptionPane.showMessageDialog(this, ""+nRecs+" records saved to "+fileName+".");
    		}
    		catch(IOException ex){
    			JOptionPane.showMessageDialog(this,"Error while writing file "+
    					fileName+". Reason: "+ex.getMessage());
    		}
    	}
    }
    
    /**
     * Implementation of ActionEvent on clearButton.
     * @param evt
     */
    private void clearButtonActionPerformed(ActionEvent evt){
    	int[] selection = list.getSelectedRows();
    	
    	if (selection.length == 0){
    		// If there is no selection made by the user then
    		// clear the entire table.
    		this.removeAll();
    	}
    	else {
    		for(int i=0; i<selection.length; i++)
    			selection[i] = list.getSorter().unsortRow(selection[i]);
    		
    		// If there is a selection made by the user only clear
    		// the selection.
    		this.removeRecAt(selection);
    	}
    	
    	if (list.getModel().getRowCount() == 0)
    		tesLView.getColorKeeper().resetColorIndex();
    }

    /**
     * Saves the records currently present in the selection list to a user
     * specified file. The output is written in a tab-delimited text form.
     * Null values are represented by having an empty string at that column
     * location. Null array values are represented by having a {}. Arrays
     * are enclosed in {}.
     * 
     * NOTE: Since this method is called on the AWT-Thread and the backing
     * records list is not updated on any other thread, the output is
     * consistent.
     * 
     * @param fileName
     * @return
     * @throws IOException
     */
    private int saveRecsToFile(String fileName, Collection<TesKey> selectedKeys, boolean saveGeometry)
    	throws IOException
    {
    	Map<FieldDesc,Integer> maxLength = new HashMap<FieldDesc,Integer>();

    	// determine max-length
    	for(TesKey key: selectedKeys){
			Map<FieldDesc,Object> recData = dataMap.get(key);
			
			for(int i=0; i<fields.size(); i++){
				FieldDesc field = (FieldDesc)fields.get(i);
				
				if (field.isArrayField()){
					Object dataVal = (Object)recData.get(field);
					int n = dataVal == null? 1: java.lang.reflect.Array.getLength(dataVal);
					maxLength.put(field, new Integer(Math.max(n, maxLength.get(field)==null? 1: ((Integer)maxLength.get(field)).intValue())));
				}
			}
    	}
    	
		// Open output file
		FileWriter fw = new FileWriter(fileName);
		
		List<String> data = new ArrayList<String>();
		
		if (saveGeometry) {
			//add titles for the detector outline
			data.add("geometry");
			data.add("Feature:string");
		}
		int maxXAxis = 0;//@x-axis - this is to keep track of the size of the array in order to know how many xaxis values there should be
		for(int i = 0; i < fields.size(); i++){
			FieldDesc field = (FieldDesc)fields.get(i);
			if (field.isArrayField()){
				int outLength = ((Integer)maxLength.get(field)).intValue();
				maxXAxis = Math.max(maxXAxis, outLength);//@x-axis
				
				for(int j=0; j<outLength; j++){
					data.add(((FieldDesc)fields.get(i)).getFieldName()+"["+(j+1)+"]");
				}
			}
			else {
				data.add(((FieldDesc)fields.get(i)).getFieldName());
			}
		}
		String delimiter = "\t";
		if (saveGeometry) {
			delimiter = ",";
		} 
		
		//@x-axis
		//this is for adding the x-axis columns to vanilla exports
		for (int x=0; x<maxXAxis; x++) {
			data.add("xaxis["+(x+1)+"]");
		}
			
		// Write header line with record delimiter at the end
		fw.write(Util.join(delimiter, data)+"\n");

		// Write data as text
		int nRecs = 0;
		for(TesKey key: selectedKeys){
			Map<FieldDesc,Object> recData = dataMap.get(key);
			nRecs++;
			data.clear();
			
			
			if (saveGeometry) {
				//add the detector outlines
				SerializablePoly poly = (SerializablePoly) recData.get(tesLayer.getPolyField());
				GeneralPath gp = poly.getBackingPoly();
				
				ArrayList<Point2D> outlineList = Util.generalPathToCoordinates(gp).get(0);//for TES stamps, there will only be one entry
				StringBuffer points = new StringBuffer();
				points.append("\"POLYGON((");
				boolean first = true;
				Point2D firstPoint = null;
				NumberFormat format = NumberFormat.getNumberInstance();
				format.setMaximumFractionDigits(4);
				for(Point2D onePoint : outlineList) {
					if (first) {
						firstPoint = onePoint;
						first = false;
					}
					String value =  format.format(onePoint.getX()) + " " + format.format(onePoint.getY());
					points.append(value);
					points.append(",");
				}
				points.append(format.format(firstPoint.getX()) + " " + format.format(firstPoint.getY()));
				points.append("))\"");
				data.add(points.toString());
				data.add("polygon");
			}
			
			for(int i = 0; i < fields.size(); i++){
				FieldDesc field = (FieldDesc)fields.get(i);
				Object dataVal = (Object)recData.get(field);
				
				if (field.isArrayField()){
					int outLength = ((Integer)maxLength.get(field)).intValue();
					
					for(int j=0; j<outLength; j++){
						Object v = dataVal == null? null: Array.get(dataVal, j);
						if(v != null) {
							if (saveGeometry) {
								//csv export
								if (v.toString().indexOf(',') > -1) {
									//there is a comma, let's quote it
									data.add("\""+v.toString()+"\"");
								} else {
									data.add(v.toString());
								}
							} else {
								//saving text, don't quote
								data.add(v.toString());
							}
						} else {
							//null v, add N/A
							data.add("N/A");
						}
					}
				}
				else {
					if (dataVal != null) {
						if (saveGeometry) {
							//csv export
							if (dataVal.toString().indexOf(',') > -1) {
								//there is a comma, let's quote it
								data.add("\""+dataVal.toString()+"\"");
							} else {
								//no comma, just put the value in
								data.add(dataVal.toString());
							}
						} else {
							//saving text, just put the value in
							data.add(dataVal.toString());
						}
					} else {
						//dataVal is null, put N/A
						data.add("N/A");
					}
				}
			}
			
			//@x-axis - add the extra columns here for export
			XYSeries series = this.graphPanel.getData(key);
			if (series != null) {//series can be null if the graph is not finished loading
				List seriesList = series.getItems();
				for (int x=0; x<maxXAxis; x++) {
					XYDataItem dItem = (XYDataItem) seriesList.get(x);
					data.add(dItem.getX().toString());
				}
			} else {
				for (int x=0; x<maxXAxis; x++) {
					data.add("N/A");
				}
			}
			
			// Add record
			fw.write(Util.join(delimiter, data)+"\n");
		}
		
		// Close output file
		fw.close();
		
		return nRecs;
    }

    /**
     * Adds a record (key,data)-pair to the Selection Panel.
     * If the key already exists in the Selection Panel, only
     * the data is updated, otherwise, the record is appended
     * to the list of records already selected.
     * 
     * If this is a duplicate entry of a previously added record
     * then the graph panel is also notified of this update.
     * 
     * NOTE: The method is called as a consequence of a user
     * click on the TES-Layer (viewport) which has detector
     * foot-prints displayed on it. The click translates into
     * the detectors that are displayed under the clicked
     * location. These get added to the Selection Panel on
     * the AWT-Thread.
     * 
     * @param key
     * @param recData Field data with essential fields such 
     *        as ock, ick, det, poly and scan_len populated.
     */
    public void addRec(TesKey key, Map<FieldDesc,Object> recData){
    	if (dataMap.containsKey(key)){
    		// Switch to key contained in the map already
    		int addedRecIndex = keyVector.indexOf(key);
            listModel.fireTableRowsUpdated(addedRecIndex, addedRecIndex);
            //graphPanel.updateRec(key);
        }
        else {
        	int addedRecIndex = keyVector.size();
            keyVector.add(key);
            dataMap.put(key, recData);
            listModel.fireTableRowsInserted(addedRecIndex, addedRecIndex);
        }
    }
    
    /**
     * Multiple element version of {@link #addRec(TesKey, Map)}.
     * @param addedRecs
     */
    public void addRecs(Map<TesKey,Map<FieldDesc,Object>> addedRecs){
    	Map<TesKey,Map<FieldDesc,Object>> reallyAdded = new HashMap<TesKey,Map<FieldDesc,Object>>(addedRecs);
    	int[] updated = new int[addedRecs.size()];
    	
    	int u = 0;
    	for(TesKey key: addedRecs.keySet()){
    		if (dataMap.containsKey(key)){
    			reallyAdded.remove(key);
    			updated[u++] = keyVector.indexOf(key);
    		}
    	}
    	
    	if (u > 0){
    		int[][] ranges = Util.binRanges(updated, u);
    		for(int i=0; i<ranges.length; i++)
    			listModel.fireTableRowsUpdated(ranges[i][0], ranges[i][1]);
    	}
    	
    	if (!reallyAdded.isEmpty()){
			keyVector.addAll(reallyAdded.keySet());
			dataMap.putAll(reallyAdded);
			listModel.fireTableRowsInserted(dataMap.size()-(addedRecs.size()-u), dataMap.size()-1);
    	}
    }

    /**
     * Select the record with the given key. Toggle selection if the toggle
     * flag is set.
     * @param key Key of the record to be selected.
     * @param toggle Changes behavior to toggling of selection with the given key.
     */
    public void selectRec(TesKey key, boolean toggle){
    	int idx = keyVector.indexOf(key);
    	if (idx < 0)
    		return;

    	idx = list.getSorter().sortRow(idx);
    	if (toggle && selectedRecs.contains(key)){
   			list.removeRowSelectionInterval(idx, idx);
    	}
    	else {
    		list.addRowSelectionInterval(idx, idx);
    	}
    }
    
    /**
     * Multiple element version of {@link #selectRec(TesKey, boolean)}
     * @param keys
     * @param toggle
     */
    public void selectRecs(Collection<TesKey> keys, boolean toggle){
		int[] select = new int[keys.size()];
		int[] deselect = new int[keys.size()];
		int s = 0, d = 0;
		Sorter sorter = list.getSorter();
		
		for(TesKey key: keys){
			int idx = sorter.sortRow(keyVector.indexOf(key));
			if (toggle && selectedRecs.contains(key)){
				deselect[d++] = idx;
			}
			else {
				select[s++] = idx;
			}
		}
    	
    	ListSelectionModel lsm = list.getSelectionModel();
    	try {
        	lsm.setValueIsAdjusting(true);
        	
        	int[][] ranges;
        	
        	ranges = Util.binRanges(deselect, d);
        	for(int i=0; i<ranges.length; i++)
        		lsm.removeSelectionInterval(ranges[i][0], ranges[i][1]);
        	
        	ranges = Util.binRanges(select, s);
        	for(int i=0; i<ranges.length; i++)
        		lsm.addSelectionInterval(ranges[i][0], ranges[i][1]);
    	}
    	finally {
    		lsm.setValueIsAdjusting(false);
    	}
    }
    
    /**
     * Toggle the locked-status of the specified records.
     * Issue table row changed events for updated records.
     * Issue a selected records repaint afterwards.
     * Must be called on the Swing thread.
     * @param keys
     */
    public void toggleLockStatus(List<TesKey> keys){
    	int[] indices = new int[keys.size()];
    	
    	int i=0;
    	for (TesKey key: keys){
    		if ((indices[i++] = keyVector.indexOf(key)) < 0)
    			throw new IllegalArgumentException("Key: "+key+" does appear in SelectionPanel's records.");

    		if (lockedRecs.contains(key))
    			lockedRecs.remove(key);
    		else
    			lockedRecs.add(key);
    	}
    	
    	// Notify table model of the changes.
    	int[][] ranges = Util.binRanges(indices, i);
    	for(i=0; i<ranges.length; i++)
    		listModel.fireTableRowsUpdated(ranges[i][0], ranges[i][1]);

    	// Update plotted records with the locked records.
		updatePlottedRecords();
		
        // Repaint the selection & locked outlines in the TesLView.
        tesLView.repaintSelection(true);
    }

    /**
     * This method is similar to the addRec method above. It updates 
     * a given record. The update is also propagated to the graph panel.
     * 
     * @param key
     * @param field
     * @param value
     */
    public void updateRec(TesKey key, FieldDesc field, Object value){
    	//System.err.println("updateRec called with "+key+" "+field+" "+value);
    	Map<FieldDesc,Object> data = (Map<FieldDesc,Object>)dataMap.get(key);
    	
    	if (data != null){
    		// update record
    		data.put(field, value);
    		
    		// broadcast change
    		int index = keyVector.indexOf(key);
    		log.println("Updating row "+index+" having key: "+key.toString()+
    				" with "+((value == null)?"(null)":value.toString()));
    		listModel.fireTableRowsUpdated(index, index);
    		if (selectedRecs.contains(key) || lockedRecs.contains(key)){
    			graphPanel.updateRec(key);
    		}
    	}
    }
    
    /**
     * Multi-record version of {@link #updateRec(TesKey, FieldDesc, Object)}
     * @param field
     * @param keyValue
     */
    public void updateRecs(FieldDesc field, Map<TesKey,Object> keyValue){
    	List<TesKey> graphed = new ArrayList<TesKey>(keyValue.size());
    	int updated[] = new int[keyValue.size()];
    	int u = 0;
    	
    	for(TesKey key: keyValue.keySet()){
    		Map<FieldDesc,Object> data = dataMap.get(key);
    		if (data != null){
    			data.put(field, keyValue.get(key));
    			updated[u++] = keyVector.indexOf(key);
    			
    			if (selectedRecs.contains(key) || lockedRecs.contains(key))
    				graphed.add(key);
    		}
    	}
    	
    	int[][] ranges = Util.binRanges(updated, u);
    	for(int i=0; i<ranges.length; i++)
    		listModel.fireTableRowsUpdated(ranges[i][0], ranges[i][1]);
    	
    	if (!graphed.isEmpty())
    		graphPanel.updateRecs(graphed);
    }

    /**
     * Removes the record identified by the given key from the selection list
     * as well as the graph panel.
     * 
     * @param key
     */
    public void removeRec(TesKey key){
        int index = keyVector.indexOf(key);

        // Remove the record from out list
        keyVector.remove(key);
        dataMap.remove(key);
        lockedRecs.remove(key);
        
        listModel.fireTableRowsDeleted(index, index);
        graphPanel.removeRec(key);
        selectedRecs.remove(key);
    }

    /**
     * Removes the record at the given selection list row location
     * from the graph panel. The row location is zero-based.
     * 
     * @param index
     */
    public void removeRecAt(int index){
        TesKey key = (TesKey)keyVector.get(index);
        keyVector.removeElementAt(index);
        dataMap.remove(key);
        lockedRecs.remove(key);

        listModel.fireTableRowsDeleted(index, index);
        graphPanel.removeRec(key);
        selectedRecs.remove(key);
    }

    /**
     * Remove records at the given selection list rows from the
     * graph panel. The row locations are zero-based.
     * 
     * @param indices
     */
    public void removeRecAt(int[] indices){
    	TesKey key;
    	
    	indices = (int[])indices.clone();
    	Arrays.sort(indices);

    	for(int i=indices.length-1; i>=0; i--){
    		key = (TesKey)keyVector.get(indices[i]);
    		keyVector.removeElementAt(indices[i]);
    		dataMap.remove(key);
    		lockedRecs.remove(key);
    		
    		listModel.fireTableRowsDeleted(indices[i], indices[i]);
    		graphPanel.removeRec(key);
    		selectedRecs.remove(key);
    	}
    }

    /**
     * Get rid of all the data.
     */
    public void removeAll(){
        keyVector.clear();
        dataMap.clear();
        lockedRecs.clear();

        // listModel.clearRows();
        listModel.fireTableDataChanged();
        
        graphPanel.clear();
        selectedRecs.clear();
    }

    /**
     * Update selection list structure as well the list of fields
     * available for graph display. This method is called as a consequence
     * of the user changing the list of fields in the associated
     * context. This method is called on the AWT-Thread as a
     * consequence of the user changing the context and the
     * updated context propagating through the entire setup.
     * 
     * NOTE: The data is not cleared during a structural update,
     * it is only cleared during a selection criteria update.
     * 
     * @param newFields
     */
    public void updateStructure(Vector<FieldDesc> newFields){
    	Vector<FieldDesc> oldFields = fields;
    	fields = this.combineDefaultAndUserFields(defaultFields, newFields);
    	
    	// Get difference between old and new fields
    	Set<FieldDesc> addedFieldsSet = new HashSet<FieldDesc>(newFields);
    	addedFieldsSet.removeAll(new HashSet<FieldDesc>(oldFields));
    	
    	// If a spectral field has been added add the scan_len field as well.
    	if (Utils.containsSpectralFields(addedFieldsSet))
    		addedFieldsSet.add(tesLayer.getScanLenField());
    	
    	// Request data for these newly added fields
		for(FieldDesc field: addedFieldsSet){
			for(TesKey key: keyVector){
    			requestUpdate(key, field);
    		}
    	}
    	
    	// Extract array fields which are considered graphable by themselves
    	Vector<FieldDesc> oldGraphFields = graphFields;
    	graphFields = new Vector<FieldDesc>(filter(fields, arrayFieldsFilter));
    	updateGraphPanelButtons(oldGraphFields,graphFields);
        
        Vector<FieldDesc> oldListFields = listFields;
        listFields = new Vector<FieldDesc>(filter(fields, nonArrayFieldsFilter));
        
        HashSet<FieldDesc> removeSet = new HashSet<FieldDesc>(oldListFields);
        removeSet.removeAll(listFields);
        
        HashSet<FieldDesc> addList = new HashSet<FieldDesc>(listFields);
        addList.removeAll(oldListFields);
        
        // Remove deleted columns and add inserted columns
        listModel.fireTableStructureChanged();
        SwingUtilities.invokeLater(new Runnable(){
        	public void run(){
                resetColumnWidths(list);
        	}
        });
    }

    // Kept for compatibility reasons with the previous setup.
    // It may get removed in the future.
    public void updateLayout(TesContext ctx){
    	ctxImage = ctx.getCtxImage();
    	updateStructure(new Vector<FieldDesc>(Arrays.asList(ctxImage.getFields())));
    }
    
    /**
     * Updates the graph panel buttons as a result of a structural
     * change that may have occurred on the context due to user
     * interaction.
     * 
     * @param oldGraphFields
     * @param newGraphFields
     */
    private void updateGraphPanelButtons(
    		Vector<FieldDesc> oldGraphFields,
    		Vector<FieldDesc> newGraphFields)
    {
    	/* Get currently active graphing field */
    	FieldDesc selectedField = null;
    	int index = graphSelector.getSelectedIndex();
    	if (index >= 0){
    		selectedField = (FieldDesc)oldGraphFields.get(index);
    	}
    	
    	boolean initiallyEmpty = graphSelector.getItemCount() == 0;
    	
    	/* Clear all graphing fields */
    	graphSelector.removeAllItems();
    	
    	/* Rebuild the list from scratch */
    	for(FieldDesc field: newGraphFields){
    		graphSelector.addItem(field);
    	}
    	
    	if (initiallyEmpty && newGraphFields.size() > 0)
    		selectedField = newGraphFields.firstElement();
    		
    	graphSelector.setSelectedItem(selectedField);
    }


    /**
     * Request update of the given field's data for the record
     * identified by the "key".
     * 
     * This operation is called by the list and the graphPanel
     * objects to request update of a data record for field
     * data they want but which does not exist within the data
     * map for the given key. The determination of whether the
     * data is available within the data map is done by checking
     * if the data map has the specified field as a key in it.
     * 
     * An UpdaterUow is generated as a result of this request,
     * which updates the data for the field within the data map
     * stored in this object.
     * 
     * @param field
     * @return
     */
    public void requestUpdate(TesKey key, FieldDesc field){
		log.println("Field ("+field.getFieldName()+") update requested for: "+key);
    	updater.add(new UpdaterUow(Collections.singletonList(key), field, this));
    }
    
    public void requestUpdate(Collection<TesKey> keys, FieldDesc field){
    	if (log.isActive())
    		log.println("Field ("+field.getFieldName()+") update requested for: "+keys);
    	
    	updater.add(new UpdaterUow(keys, field, this));
    }
    
    /**
     * A filter operation to be used in conjunction with the filter
     * method below.
     */
    // TODO: Move this out of this class
    private interface CollectionFilter<T> {
    	/**
    	 * Return true to keep the object during the filter
    	 * process or false to discard the object during the
    	 * filter object. The filter operation will only
    	 * retain those elements for whom this method returns
    	 * true.
    	 * 
    	 * @param obj
    	 * @return
    	 */
    	public boolean filter(T obj);
    }
    
    /**
     * Filters a given collection "c" on the basis of the filter "f".
     * @param c
     * @param f
     * @return
     */
    // TODO: Move this out of this class
    private <T> Collection<T> filter(Collection<T> c, CollectionFilter<T> f){
    	ArrayList<T> filtered = new ArrayList<T>(c.size());
    	
    	for(T o: c){
    		if (f.filter(o)){
    			filtered.add(o);
    		}
    	}
    	
    	return filtered;
    }

    /**
     * Lets non-array fields pass through.
     */
    private CollectionFilter<FieldDesc> nonArrayFieldsFilter = new CollectionFilter<FieldDesc>(){
    	public boolean filter(FieldDesc obj){
    		if (obj != null && !obj.isArrayField()){
    			return true;
    		}
    		return false;
    	}
    };
    
    /**
     * Lets array fields pass through.
     */
    private CollectionFilter<FieldDesc> arrayFieldsFilter = new CollectionFilter<FieldDesc>(){
    	public boolean filter(FieldDesc obj){
    		if (obj != null && obj.isArrayField()){
    			return true;
    		}
    		return false;
    	}
    };
    



    /**
     * Field update request to be used in conjunction with
     * the SerializingThread. An instance of this object
     * is generated as a result of requestUpdate. The end
     * result is to update the data for the given field in
     * the data record identified by the key. This update
     * is done on the AWT-Thread because of its cascading
     * effect.
     * 
     * @author saadat
     *
     */
    private class UpdaterUow implements Runnable {
    	public UpdaterUow(Collection<TesKey> keys, FieldDesc field, SelectionPanel4 selPanel){
    		this.keys = keys;
    		this.field = field;
    		this.selPanel = selPanel;
    	}
    	
    	public void run(){
    		
    		try {
    			final Map<TesKey,Object> data = tesLayer.getFieldData(keys,field);
    			SwingUtilities.invokeLater(new Runnable(){
    				public void run(){
    					selPanel.updateRecs(field,data);
    				}
    			});
    		}
    		catch(SQLException ex){
    			log.aprintln(ex);
    		}
    	}
    	
    	private Collection<TesKey> keys;
    	private FieldDesc field;
    	private SelectionPanel4 selPanel;
    }
    
    /**
     * Renderer for the list of plot fields.
     * 
     * @author saadat
     *
     */
    class FieldCellRenderer extends JLabel implements ListCellRenderer {
        /**
		 * eclipse keeps on complaining about this.
		 */
		private static final long serialVersionUID = 1L;

		public Component getListCellRendererComponent(
            JList list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus)
        {
            setOpaque(true);
            setPreferredSize(new Dimension(120,20));

            if (isSelected){
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            }
            else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }

            if (value == null){ setText("         "); }
            else {
                setText(((FieldDesc)value).getFieldName());
            }

            return this;
        }
    }

    /**
     * "*.txt" file filter to use with JFileChooser.
     * 
     * @author saadat
     *
     */
    class TxtFileFilter extends FileFilter {
        public boolean accept(File f){
            if (f.isFile()){
                String name = f.getName();
                if (name.matches("^.*\\.[Tt][Xx][Tt]$")){
                    return true;
                }
            }
            else if (f.isDirectory()){
                return true;
            }
            return false;
        }
        public String getDescription(){
            return "*.txt";
        }
    }
    
    /**
     * "*.csv" file filter to use with JFileChooser.
     * 
     * @author krios
     *
     */
    class CsvFileFilter extends FileFilter {
        public boolean accept(File f){
            if (f.isFile()){
                String name = f.getName();
                if (name.matches("^.*\\.[Cc][Ss][Vv]$")){
                    return true;
                }
            }
            else if (f.isDirectory()){
                return true;
            }
            return false;
        }
        public String getDescription(){
            return "*.csv";
        }
    }
    
    /**
     * Displays graph of rows selected from the selection 
     * list. The value plotted is for the selected array
     * field.
     * 
     * @author saadat
     *
     */
    public class TesGraphPanel extends GraphPanel {
    	/**
		 * eclipse complains about this. 
		 */
		private static final long serialVersionUID = 1L;
		
		Set<TesKey> keys = new LinkedHashSet<TesKey>();
        FieldDesc plotField = null;

        /**
         * Sets the field to be plotted/graphed. This field must be an array
         * field.
         * @param plotField An array field data for which will be plotted from
         * the records added to the plot list.
         * @see #addRec(TesKey)
         * @see FieldDesc#isArrayField()
         */
        public void setPlotField(FieldDesc plotField){
        	// Save the plot field
            this.plotField = plotField;

            // Clear data
            clearData();
            setXAxisLabel("data element");
            setYAxisLabel("value");
            if (plotField == null){ return; }

            // Set the new axis labels
            setXAxisLabel(plotField.isSpectralField()? "wavenumber": "data element");
            setDomainInverted(plotField.isSpectralField()? true: false);
            setYAxisLabel(plotField.getQualifiedFieldName());

            // Replot data
            replotData();
        }
        
        private void replotData(){
            for(TesKey key: keys){
            	plotArrayData(key);
            }
        }
        
        private String mkLabel(TesKey k){
            return "("+k.getOck()+","+k.getIck()+","+k.getDet()+")";
        }
        
        /*
         * Adopted from make_tesx(scan_len) in "/themis/lib/dav_lib/library/misc.dvrc"
         */
        private double[] mkSpecXaxis(FieldDesc field, int scanLen){
        	double tesRes = 10.5808820 / scanLen;
        	int firstPt = 14 * scanLen;
        	int[] indexRange = field.getIndexRange();
        	if (indexRange == null)
        		indexRange = new int[]{ 0, 143 * scanLen - 1 };
        	
        	int n = indexRange[1]-indexRange[0]+1;
        	double[] x = new double[n];
        	
        	for(int i=indexRange[0], j=0; i<=indexRange[1]; i++, j++)
        		x[j] = Math.rint((firstPt + i) * tesRes * 10000) / 10000;
        	
        	return x;
        }
        
        private void plotArrayData(TesKey key){
        	Map<FieldDesc,Object> recData = dataMap.get(key);
        	if (recData != null){
        		if (recData.containsKey(plotField)){
        			Object data = recData.get(plotField);
        			if (data != null){
        				double[] v = arrayValAsDouble(data);
        				if (plotField.isSpectralField()){
        					if (recData.get(tesLayer.getScanLenField()) != null){
        						int scanLen = Integer.parseInt(recData.get(tesLayer.getScanLenField()).toString());
        						addData(key, mkLabel(key), mkSpecXaxis(plotField, scanLen), v);
        						setPaint(key, (Color)recData.get(tesLayer.getColorField()));
        					}
        					else {
        						log.println("Skipping "+key+" from plot of "+plotField+" because scan_len field is missing.");
        					}
        				}
        				else {
        					addData(key, mkLabel(key), v);
    						setPaint(key, (Color)recData.get(tesLayer.getColorField()));
        				}
        			}
        		}
        		else {
        			// We do not have data for this field.
        			log.println("Skipping "+key+" from plot of "+plotField+" because field data is missing.");
        		}
        	}
        }
        
        /**
         * Add the record with the given key to the plotted records list.
         * @param key Key of the record to be plotted.
         */
        public void addRec(TesKey key){
        	keys.add(key);

            if (plotField != null){
            	plotArrayData(key);
            }
        }
        
        /**
         * Returns a read-only set of all the record keys being plotted.
         * @return A read-only set of all the plotted record keys.
         */
        public Set<TesKey> getRecs(){
        	return Collections.unmodifiableSet(keys);
        }

        /**
         * Remove a previously added record with the given key from the plotted records list.
         * @param key Key of the record to be removed from being plotted.
         * @see #addRec(TesKey)
         */
        public void removeRec(TesKey key){
            keys.remove(key);
            delData(key);
        }

        /**
         * Remove all records previously added to the plotted records list.
         * @see #addRec(TesKey)
         */
        public void removeAllRecs(){
            keys.clear();
            clearData();
        }

        /**
         * Updates the plotted data for the record previously added with the specified key.
         * @param key Key of the record for which the plotted data is to be updated.
         * @see #addRec(TesKey)
         */
        public void updateRec(TesKey key){
        	if (keys.contains(key)){
        		delData(key);
        		if (plotField != null){
        			plotArrayData(key);
        		}
        	}
        }
        
        /**
         * A multiple element version of {@link #updateRec(TesKey)}.
         * @param keys
         */
        public void updateRecs(Collection<TesKey> keys){
        	delData(keys);
        	if (plotField != null){
        		for(TesKey key: keys)
        			plotArrayData(key);
        	}
        }

        private double[] arrayValAsDouble(Object arrayVal){
        	double[] d;
        	
        	if (arrayVal instanceof double[]){
        		d = (double[])arrayVal;
        	}
        	else if (arrayVal instanceof float[]){
        		float[] f = (float[])arrayVal;
        		d = new double[f.length];
        		for(int i = 0; i < f.length; i++){ d[i] = f[i]; }
        	}
        	else if (arrayVal instanceof int[]){
        		int[] ii = (int[])arrayVal;
        		d = new double[ii.length];
        		for(int i = 0; i < ii.length; i++){ d[i] = ii[i]; }
        	}
        	else if (arrayVal instanceof short[]){
        		short[] s = (short[])arrayVal;
        		d = new double[s.length];
        		for(int i = 0; i < s.length; i++){ d[i] = s[i]; }
        	}
        	else if (arrayVal instanceof Number[]){
        		Number[] n = (Number[])arrayVal;
        		d = new double[n.length];
        		for(int i = 0; i < n.length; i++){ d[i] = n[i].doubleValue(); }
        	}
        	else {
        		throw new RuntimeException("Array value unsupported!");
        	}
        	
        	return d;
        }
    }
    
    /**
     * Returns the Paint used to paint the specified key in the chart panel.
     * @param key 
     * @return
     */
    private Paint getPaintForKey(TesKey key){
    	if (selectedRecs.contains(key) || lockedRecs.contains(key))
    		return graphPanel.getPaint(key);
    	return null;
    }
    

    /**
     * A light-weight TableModel adapter to the user's pick list.
     */
    class PickListTableModelAdapter extends AbstractTableModel {
		private static final long serialVersionUID = 1L;
		
		private final String[] predefColumns = new String[]{ LOCK_COLUMN_NAME };
		private final Class<?>[] predefColumnTypes = new Class<?>[]{ Lock.class };

		public Class<?> getColumnClass(int columnIndex) {
			if (columnIndex < predefColumns.length)
				return predefColumnTypes[columnIndex];
			
			return listFields.elementAt(columnIndex-predefColumns.length).getFieldType();
		}

		public String getColumnName(int columnIndex) {
			if (columnIndex < predefColumns.length)
				return predefColumns[columnIndex];
			
			return listFields.elementAt(columnIndex-predefColumns.length).getFieldName();
		}

		public boolean isCellEditable(int rowIndex, int columnIndex) {
			if (LOCK_COLUMN_NAME.equals(getColumnName(columnIndex)) || COLOR_COLUMN_NAME.equals(getColumnName(columnIndex)))
				return true;
			return false;
		}

		public void setValueAt(Object value, int rowIndex, int columnIndex) {
			if (LOCK_COLUMN_NAME.equals(getColumnName(columnIndex))){
				toggleLockStatus(Collections.singletonList(keyVector.elementAt(rowIndex)));
			}
			else {
				TesKey key = keyVector.get(rowIndex);
				dataMap.get(key).put(listFields.elementAt(columnIndex-predefColumns.length), (Color)value);
				graphPanel.setPaint(key, (Color)value);

				//super.setValueAt(value, rowIndex, columnIndex);
			}
		}

		public int getColumnCount() {
			return listFields.size()+predefColumns.length;
		}

		public int getRowCount() {
			return dataMap.size();
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			String colName = getColumnName(columnIndex);
			//if (COLOR_COLUMN_NAME.equals(colName))
				//return getPaintForKey(keyVector.get(rowIndex));
			
			if (LOCK_COLUMN_NAME.equals(colName))
				return new Lock(lockedRecs.contains(keyVector.get(rowIndex)));
				
			return dataMap.get(keyVector.get(rowIndex)).get(listFields.elementAt(columnIndex-predefColumns.length));
		}
    }

    /**
     * TableCellRenderer for Paint class.
     */
    private class PaintTableCellRenderer extends JLabel implements TableCellRenderer {
		private static final long serialVersionUID = 1L;
		
		Paint p = null;
    	
    	public PaintTableCellRenderer(){
    		setOpaque(true);
    	}
    	
    	public void paintComponent(Graphics g){
    		super.paintComponent(g);
    		Graphics2D g2 = (Graphics2D)g;
    		g2.setPaint(p);
    		g2.fillRect(0, 0, getWidth(), getHeight());
    	}
    	
    	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
    		setBorder(BorderFactory.createMatteBorder(4, 4, 4, 4, isSelected? table.getSelectionBackground(): table.getBackground()));
    		p = (Paint)value;
    		if (p == null)
    			p = isSelected? table.getSelectionBackground(): table.getBackground();
    		return this;
		}
    }

    static private class LockTableCellRenderer extends JLabel implements TableCellRenderer {
    	private static final long serialVersionUID = 1L;
    	private Icon lockIcon, unlockIcon;
    	
    	public LockTableCellRenderer(){
    		super();
    		setHorizontalAlignment(SwingConstants.CENTER);
    		setVerticalAlignment(SwingConstants.CENTER);
    		lockIcon = new ImageIcon(Main.getResource("resources/locked.gif"));
    		unlockIcon = null; //new ImageIcon(Main.getResource("resources/unlocked.gif"));
    	}
    	
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {
			
			setOpaque(table.isOpaque());
			setForeground(isSelected? table.getSelectionForeground(): table.getForeground());
			setBackground(isSelected? table.getSelectionBackground(): table.getBackground());

			Lock lock = (Lock)value;
			setIcon(lock != null && lock.isLocked()? lockIcon: unlockIcon);

			return this;
		}
    }
    
    static private class LockTableCellEditor extends JToggleButton implements TableCellEditor {
    	private static final long serialVersionUID = 1L;
    	
    	List<CellEditorListener> listeners = new ArrayList<CellEditorListener>();
    	private Icon lockIcon, unlockIcon;
    	
    	public LockTableCellEditor(){
    		super();
    		setHorizontalAlignment(SwingConstants.CENTER);
    		setVerticalAlignment(SwingConstants.CENTER);
    		setBorder(BorderFactory.createEmptyBorder());
    		addActionListener(new ActionListener(){
    			public void actionPerformed(ActionEvent e){
    				stopCellEditing();
    			}
    		});
    		lockIcon = new ImageIcon(Main.getResource("resources/locked.gif"));
    		unlockIcon = null; //new ImageIcon(Main.getResource("resources/unlocked.gif"));
    	}
    	
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
			setSelected(value == null? false: ((Lock)value).isLocked());
			setForeground(isSelected? table.getSelectionForeground(): table.getForeground());
			setBackground(isSelected? table.getSelectionBackground(): table.getBackground());
			setIcon(isSelected()? lockIcon: unlockIcon);
			
			return this;
		}

		public void addCellEditorListener(CellEditorListener l) {
			listeners.add(l);
		}

		public void removeCellEditorListener(CellEditorListener l) {
			listeners.remove(l);
		}

		public void cancelCellEditing() {
			ChangeEvent e = new ChangeEvent(this);
			for(CellEditorListener l: new ArrayList<CellEditorListener>(listeners)){
				l.editingCanceled(e);
			}
		}

		public Object getCellEditorValue() {
			return new Lock(isSelected());
		}

		public boolean isCellEditable(EventObject anEvent) {
			if (anEvent instanceof MouseEvent){
				MouseEvent me = (MouseEvent)anEvent;
				if (SwingUtilities.isLeftMouseButton(me) && me.getClickCount() == 2){
					return true;
				}
			}
			return false;
		}

		public boolean shouldSelectCell(EventObject anEvent) {
			return false;
		}

		public boolean stopCellEditing() {
			ChangeEvent e = new ChangeEvent(this);
			for(CellEditorListener l: new ArrayList<CellEditorListener>(listeners)){
				l.editingStopped(e);
			}
			return true;
		}
    	
    }
    
    static private class Lock {
    	private boolean locked;
    	
    	public Lock(){
    		this(false);
    	}
    	
    	public Lock(boolean locked){
    		this.locked = locked;
    	}
    	
    	public boolean isLocked(){
    		return locked;
    	}
    	
    	public void setLocked(boolean locked){
    		this.locked = locked;
    	}
    }
}


