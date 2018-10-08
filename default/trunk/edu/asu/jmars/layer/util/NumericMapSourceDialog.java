package edu.asu.jmars.layer.util;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import edu.asu.jmars.Main;
import edu.asu.jmars.layer.map2.CustomMapServer;
import edu.asu.jmars.layer.map2.MapServer;
import edu.asu.jmars.layer.map2.MapServerFactory;
import edu.asu.jmars.layer.map2.MapSource;
import edu.asu.jmars.layer.stamp.NumericStampSource;
import edu.asu.jmars.util.Util;
import edu.emory.mathcs.backport.java.util.Collections;

public class NumericMapSourceDialog extends JDialog{
	
	private JRadioButton allRad;
	private JRadioButton elevRad;
	private JRadioButton stampRad;
	private JRadioButton customRad;
	private JTextField filterTF;
	private final String filterPrompt = "Enter words to filter on";
	private JCheckBox titleBx;
	private JCheckBox abstractBx;
	private JButton searchBtn;
	private JTable resultsTbl;
	private JScrollPane tableSP;
	private JPanel tablePnl;
	private JPanel descPnl;
	private JScrollPane descSP;
	private JTextArea descTA;
	private JButton okBtn;
	private JButton cancelBtn;
	private ArrayList<MapSource> sources;
	
	private int pad = 1;
	private Insets in = new Insets(pad,pad,pad,pad);

	private boolean multiSources;
	private ArrayList<MapSource> selSources = new ArrayList<MapSource>();
	
	private NumericMapSourceDialog(JComponent relTo, boolean multiSelection){
		super(new Frame(), "Numeric Map Source Selection", true);
		setLocationRelativeTo(relTo);
		multiSources = multiSelection;

		//get sources to populate table to start with
		//by default, use all numeric sources
		sources = new ArrayList<MapSource>();
		if (MapServerFactory.getMapServers() != null) {
			for (MapServer server: MapServerFactory.getMapServers()) {
				for (MapSource source: server.getMapSources()) {
					if (source.hasNumericKeyword()) {
						sources.add(source);
					}
				}
			}
		}
		Collections.sort(sources, byTitle);
		
		//build UI
		buildUI();
		setMinimumSize(new Dimension(500,500));
		filterTF.requestFocusInWindow();
		setVisible(true);
	}
	
	private Comparator<MapSource> byTitle = new Comparator<MapSource>() {
		public int compare(MapSource o1, MapSource o2) {
			return o1.getTitle().compareTo(o2.getTitle());
		}
	};
	
	private void buildUI(){
		//search section
		//-source panel (all, elevation, custom)
		JPanel sourcePnl = new JPanel(new GridBagLayout());
		sourcePnl.setBackground(Util.lightBlue);
		JLabel sourceLbl = new JLabel("Numeric Sources: ");
		allRad = new JRadioButton("All");
		allRad.setBackground(Util.lightBlue);
		allRad.addActionListener(sourceListener);
		//set all option as default
		allRad.setSelected(true);
		elevRad = new JRadioButton("Elevation");
		elevRad.setBackground(Util.lightBlue);
		elevRad.addActionListener(sourceListener);
		stampRad = new JRadioButton("Stamp");
		stampRad.setBackground(Util.lightBlue);
		stampRad.addActionListener(sourceListener);
		customRad = new JRadioButton("Custom");
		customRad.setBackground(Util.lightBlue);
		customRad.addActionListener(sourceListener);
		//disable the custom radio button if the user is not logged in
		if(Main.USER == null || Main.USER.equals("")){
			customRad.setEnabled(false);
		}
		ButtonGroup sourceGrp = new ButtonGroup();
		sourceGrp.add(allRad);
		sourceGrp.add(elevRad);
		sourceGrp.add(stampRad);
		sourceGrp.add(customRad);
		sourcePnl.add(sourceLbl, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		sourcePnl.add(allRad, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		sourcePnl.add(elevRad, new GridBagConstraints(2, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		sourcePnl.add(stampRad, new GridBagConstraints(3, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		sourcePnl.add(customRad, new GridBagConstraints(4, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		//search panel
		JPanel searchPnl = new JPanel(new GridBagLayout());
		searchPnl.setBackground(Util.lightBlue);
		searchPnl.setBorder(new TitledBorder("Search"));
		JPanel filterPnl = new JPanel();
		filterPnl.setBackground(Util.lightBlue);
		JLabel filterLbl = new JLabel("Filter:");
		filterTF = new JTextField(25);
		filterTF.setText(filterPrompt);
		filterTF.addFocusListener(filterFieldListener);
		filterPnl.add(filterLbl);
		filterPnl.add(filterTF);
		titleBx = new JCheckBox("Source Title");
		//set just the title as default
		titleBx.setSelected(true);
		titleBx.setBackground(Util.lightBlue);
		titleBx.addActionListener(filterListener);
		abstractBx = new JCheckBox("Abstract/Citation");
		abstractBx.setBackground(Util.lightBlue);
		abstractBx.addActionListener(filterListener);
		searchBtn = new JButton(searchAct);
		JPanel searchBtnPnl = new JPanel();
		searchBtnPnl.setBackground(Util.lightBlue);
		searchBtnPnl.add(searchBtn);
		searchPnl.add(sourcePnl, new GridBagConstraints(0, 0, 2, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		searchPnl.add(filterPnl, new GridBagConstraints(0, 1, 2, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		searchPnl.add(titleBx, new GridBagConstraints(0, 2, 1, 1, 1, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
		searchPnl.add(abstractBx, new GridBagConstraints(1, 2, 1, 1, 1, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
		searchPnl.add(searchBtnPnl, new GridBagConstraints(0, 3, 2, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		
		//table section
		resultsTbl = new JTable();
		tableSP = new JScrollPane(resultsTbl, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		tablePnl = new JPanel();
		tablePnl.setLayout(new GridLayout(1, 1));
		tablePnl.setPreferredSize(new Dimension(300,250));
		tablePnl.add(tableSP);
		
		//description section
		descPnl = new JPanel(new GridLayout(1,1));
		descPnl.setBorder(new TitledBorder("Abstract/Citation"));
		descTA = new JTextArea();
		descTA.setBackground(Util.panelGrey);
		descTA.setLineWrap(true);
		descTA.setWrapStyleWord(true);
		descTA.setEditable(false);
		descSP = new JScrollPane(descTA);
		descSP.setBorder(BorderFactory.createEmptyBorder());
		descPnl.add(descSP);		
		
		//ok/cancel section
		okBtn = new JButton(okAct);
		//don't enable until a source is selected
		okBtn.setEnabled(false);
		cancelBtn = new JButton(cancelAct);
		JPanel btnPnl = new JPanel();
		btnPnl.setBackground(Util.lightBlue);
		btnPnl.add(okBtn);
		btnPnl.add(cancelBtn);
		
		//put it all together
		JPanel mainPnl = new JPanel(new GridBagLayout());
		mainPnl.setBackground(Util.lightBlue);
		mainPnl.setBorder(new EmptyBorder(5, 5, 5, 5));
		int row = 0;
		mainPnl.add(searchPnl, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, in, pad, pad));
		mainPnl.add(tablePnl, new GridBagConstraints(0, ++row, 1, 1, .7, .7, GridBagConstraints.CENTER, GridBagConstraints.BOTH, in, pad, pad));
		mainPnl.add(Box.createVerticalStrut(1), new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, in, pad, pad));
		mainPnl.add(descPnl, new GridBagConstraints(0, ++row, 1, 1, .4, .4, GridBagConstraints.CENTER, GridBagConstraints.BOTH, in, pad, pad));
		mainPnl.add(btnPnl, new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, in, pad, pad));
		
		refreshTable(sources);
		
		setContentPane(mainPnl);
		getRootPane().setDefaultButton(searchBtn);
	}
	
	private AbstractAction searchAct = new AbstractAction("Search") {
		public void actionPerformed(ActionEvent e) {
			refreshTable(filterSources());
		}
	};
	
	private AbstractAction okAct = new AbstractAction("Ok") {
		public void actionPerformed(ActionEvent e) {
			//clear selected list
			selSources.clear();
			//set the selected map
			MapSourceTableModel tm = (MapSourceTableModel)resultsTbl.getModel();
			if(multiSources){
				for(int row : resultsTbl.getSelectedRows()){
					selSources.add(tm.getSelectedSource(row));
				}
			}else{
				selSources.add(tm.getSelectedSource(resultsTbl.getSelectedRow()));
			}
			NumericMapSourceDialog.this.setVisible(false);
		}
	};
	
	private AbstractAction cancelAct = new AbstractAction("Cancel") {
		public void actionPerformed(ActionEvent e) {
			selSources = null;
			NumericMapSourceDialog.this.setVisible(false);
		}
	};
	
	private void refreshTable(ArrayList<MapSource> newSources){
		tablePnl.remove(tableSP);
		
		resultsTbl = loadTable(newSources);
		if(multiSources){
			resultsTbl.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		}else{
			resultsTbl.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		}
		resultsTbl.getSelectionModel().addListSelectionListener(rowListener);
		
		int nameCol = resultsTbl.getColumnModel().getColumnIndex(MapSourceTableModel.NAME_COL);
		resultsTbl.getColumnModel().getColumn(nameCol).setPreferredWidth(470);
		tableSP = new JScrollPane(resultsTbl, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		tablePnl.add(tableSP);
		tablePnl.revalidate();
		
		//also clear the descTA because the entire table has been
		// dropped and reloaded, so no selection has been made yet
		descTA.setText("");
		//also disable the ok button since no selection is made yet
		okBtn.setEnabled(false);
	}
	
	private JTable loadTable(ArrayList<MapSource> newSources){
		//sources has been filtered by the time this method is called
		MapSourceTableModel model = new MapSourceTableModel(newSources);
		return new JTable(model);
	}
	
	
	private ArrayList<MapSource> filterSources(){
		//get the words to filter on
		String filterStr = filterTF.getText();
		String[] words = filterStr.split(" ");
		//if no words were entered, don't do anything
		if(words.length == 0 || filterStr.equals(filterPrompt)){
			return sources;
		}
		//create a new list to keep track of sources that match the filter words
		ArrayList<MapSource> matches = new ArrayList<MapSource>();
		//cycle through list of sources (which might be modified from the 
		// sourceListener if the user changed the radiobutton selection
		for(MapSource ms : sources){
			boolean isMatch = false;
			//if title box is selected, look at title text first
			if(titleBx.isSelected()){
				for(String word : words){
					//make the search case insensitive
					word = word.toUpperCase();
					String titleStr = ms.getTitle().toUpperCase();
					//if the title does not contain any of the words, mark as 
					// not a match
					if(titleStr.contains(word)){
						isMatch = true;
					}else{
						isMatch = false;
						break;
					}
				}
			}
			//next, look through abstract text if that box is selected AND
			// if a match was not already found from the title
			if(abstractBx.isSelected() && isMatch == false){
				for(String word : words){
					//make the search case insensitive
					word = word.toUpperCase();
					String absStr = ms.getAbstract().toUpperCase();
					//if the title does not contain any of the words, mark as 
					// not a match
					if(absStr.contains(word)){
						isMatch = true;
					}else{
						isMatch = false;			
						break;
					}
				}
			}
			
			//if the source was a match, add it to the new list
			if(isMatch){
				matches.add(ms);
			}
		}
		//return the sources to the new limited list
		return matches;
	}
	
	/**
	 * This listener is used on the radio buttons which filter
	 * the sources shown to the user.
	 */
	private ActionListener sourceListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			//clear the source list
			sources.clear();
			//if custom radio button is selected, only grab the 
			// custom map server from the server factory
			if(customRad.isSelected()){
				CustomMapServer server = MapServerFactory.getCustomMapServer();
				if(server != null){
					for(MapSource source : server.getMapSources()){
						if(source != null && source.hasNumericKeyword()){
							sources.add(source);
						}
					}
				}
			}else{
				//Otherwise get ahold of all of the sources and 
				// then filter based on user radio button selection
				if(MapServerFactory.getMapServers() != null){
					for(MapServer server : MapServerFactory.getMapServers()){
						if(server != null){
							for(MapSource source : server.getMapSources()){
								if(source != null && source.hasNumericKeyword()){
									//elevation is selected
									if(elevRad.isSelected() && source.hasElevationKeyword()){
										sources.add(source);
									}
									//stamp is selected
									else if(stampRad.isSelected() && source instanceof NumericStampSource){
										sources.add(source);
									}
									//all is selected
									else if(allRad.isSelected()){
										sources.add(source);
									}
								}
							}
						}
					}
				}
			}
			//alphebetize the sources
			Collections.sort(sources, byTitle);
			
			//refresh the table
			refreshTable(filterSources());
		}
	};
	
	private ActionListener filterListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			//if neither title or abstract check boxes are selected,
			// disable the search button
			if(!titleBx.isSelected() && !abstractBx.isSelected()){
				searchBtn.setEnabled(false);
				searchBtn.setToolTipText("Select at least one filter checkbox");
			}else{
				searchBtn.setEnabled(true);
			}
			
		}
	};
	
	private ListSelectionListener rowListener = new ListSelectionListener() {
		public void valueChanged(ListSelectionEvent e) {
			int row = resultsTbl.getSelectedRow();
			if(row > -1){
				//get the selected mapsource from table model
				MapSource sel = ((MapSourceTableModel)resultsTbl.getModel()).getSelectedSource(row);
				//update description text
				descTA.setText(sel.getAbstract());
				//make sure the scroll bar is all the way to the top
				descTA.setCaretPosition(0);
				//enable the ok button because a source is selected
				okBtn.setEnabled(true);
			}else{
				//clear description text if no selection is made
				descTA.setText("");
				//disable the ok button because no source is selected
				okBtn.setEnabled(false);
			}
		}
	};
	
	private FocusListener filterFieldListener = new FocusListener() {
		public void focusLost(FocusEvent e) {
			//replace with the prompt if there is no text
			if(filterTF.getText() == null || filterTF.getText().equals("")){
				filterTF.setText(filterPrompt);
			}
		}
		public void focusGained(FocusEvent e) {
			//clear the prompt so the user can enter their filter words
			if(filterTF.getText().equals(filterPrompt)){
				filterTF.setText("");
			}
		}
	};
	
	/**
	 * Creates a NumericMapSourceDialog relative to the component passed
	 * in to this method.  Allows the option of single or multi source 
	 * selection.  Returns an ArrayList of the selected sources.  This
	 * List will only have one entry if false is passed for the multiSeleciton
	 * argument.  Can return null if this dialog is canceled or xed out of.
	 * @param relTo Component where to show this dialog relative to
	 * @param multiSelection Whether the user should be allowed to select multiple
	 * sources at once.  If false, only single selection in the source table is allowed.
	 * @return An ArrayList of the chosen MapSources from this dialog, will only
	 * have one element if false is passed for multiselection, and can return null
	 * if the dialog is canceled or closed out of.
	 */
	public static ArrayList<MapSource> getUserSelectedSources(JComponent relTo, boolean multiSelection){
		NumericMapSourceDialog nmsd = new NumericMapSourceDialog(relTo, multiSelection);
		//since the dialog is modal, it won't hit the next
		// line and return the source until it is closed
		return nmsd.selSources;
	}

	
	
	private class MapSourceTableModel extends AbstractTableModel{

		private ArrayList<MapSource> sources;
		
		private static final String NAME_COL = "Name";
		private static final String PPD_COL = "Max PPD";
		private final String columnNames[] = {NAME_COL, PPD_COL};
		private DecimalFormat df = new DecimalFormat("####");
		
		private MapSourceTableModel(ArrayList<MapSource> sources){
			this.sources = sources;
		}
		
		
		public int getRowCount() {
			if(sources == null) return 0;
			
			return sources.size();
		}

		public int getColumnCount() {
			return columnNames.length;
		}
		
	    public String getColumnName(int column) {
	    	return columnNames[column];
	    }

		public Object getValueAt(int rowIndex, int columnIndex) {
			MapSource ms = sources.get(rowIndex);
			
			switch(getColumnName(columnIndex)){
			case NAME_COL:
					return ms.getTitle();
			case PPD_COL:
					return df.format(ms.getMaxPPD());
			default:
				return null;
			}
		}
		
		private MapSource getSelectedSource(int row){
			return sources.get(row);
		}
	}
}
