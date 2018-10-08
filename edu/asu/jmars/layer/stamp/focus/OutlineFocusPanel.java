package edu.asu.jmars.layer.stamp.focus;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;

import org.jdesktop.swingx.JXTaskPane;
import org.jdesktop.swingx.JXTaskPaneContainer;

import edu.asu.jmars.layer.stamp.StampImage;
import edu.asu.jmars.layer.stamp.StampLView;
import edu.asu.jmars.layer.stamp.StampLayer;
import edu.asu.jmars.layer.stamp.StampLayerSettings;
import edu.asu.jmars.layer.stamp.StampShape;
import edu.asu.jmars.layer.util.features.FieldFormulaMethods;
import edu.asu.jmars.swing.FancyColorMapper;
import edu.asu.jmars.util.Util;
import gnu.jel.CompiledExpression;
import gnu.jel.DVMap;
import gnu.jel.Evaluator;
import gnu.jel.Library;

@SuppressWarnings("serial")
public class OutlineFocusPanel extends JPanel {

	private StampLayer myLayer;
	private StampLayerSettings mySettings;
	private StampTable myStampTable;
	private JLabel countLbl;
	private String recordCountStr = "Total records in current view: ";
	private JCheckBox limitToMainViewCBx;
	private JComboBox<String> columnBx;
	private JCheckBox hideOutofRangeCBx;
	private JRadioButton columnRBtn;
	private JRadioButton expRBtn;
	private JTextArea expTA;
	private JLabel errorsLbl;
	private String noErrorsStr = "No errors - valid expression";
	private JTextField scaleTF;
	private JLabel minValueLbl;
	private JLabel maxValueLbl;
	private String minValStr = "Column min value: ";
	private String maxValStr = "Column max value: ";
	private JTextField absMinTF;
	private JTextField absMaxTF;
	private JTextField minTF;
	private JTextField maxTF;
	private FancyColorMapper mapper;
	private JXTaskPane spotPnl;
	
	private CompiledExpression compiledExpression = null;
	
	private StampAccessor stampMap;
	private Library lib;
	
	//UI constraints
	private DecimalFormat format = new DecimalFormat();
    private int row = 0;
    private int pad = 2;
    private Insets in = new Insets(pad,pad,pad,pad);
    
	// creates and returns a default unfilled stamp panel.
	public OutlineFocusPanel(final StampLayer stampLayer, final StampTable table)
	{
		myLayer = stampLayer;
		myStampTable = table;
		mySettings = myLayer.getSettings();
		
		format.setMaximumFractionDigits(4);
		format.setGroupingUsed(false);
		
		//for expression listener
	    //// Silliness
	    stampMap = new StampAccessor(myLayer, null);
		lib = new Library(
			new Class[]{Math.class, FieldFormulaMethods.class},
			new Class[]{StampAccessor.class},
			new Class[]{},
			stampMap,
			null);					
		/// Maybe end of Silliness
		
		buildUI();
	}
	
	private void buildUI(){
		//top panel has find and export buttons
		JPanel top = new JPanel();
	    JButton findStamp = new JButton(findAct);

	    final JFileChooser fc = new JFileChooser();
        FileFilter ff = new FileFilter(){				
			public String getDescription() {
				return "Tab delimited file (.tab, txt)";
			}
			
			public boolean accept(File f) {
				if (f.isDirectory()) return true;
				if (f.getName().endsWith(".txt")) return true;
				if (f.getName().endsWith(".tab")) return true;

				return false;
			}
		};
		fc.setDialogTitle("Export Stamp Table");
        fc.setAcceptAllFileFilterUsed(false);
        fc.setFileFilter(ff);
        
        JButton exportBtn = new JButton(new AbstractAction("Export Table...") {
	        public void actionPerformed(ActionEvent e){
	            File f;
	            
	            do {
	                if (fc.showSaveDialog(OutlineFocusPanel.this)
	                        != JFileChooser.APPROVE_OPTION)
	                    return;
	                f = fc.getSelectedFile();
	                
	                if (!f.getName().endsWith(".txt")&&!f.getName().endsWith(".tab")) {
	                	f=new File(f.getAbsolutePath()+".txt");
	                } 	
	            }
	            while( f.exists() && 
	                    JOptionPane.NO_OPTION == JOptionPane.showConfirmDialog(
	                    		OutlineFocusPanel.this,
                               "File already exists, overwrite?\n" + f,
                               "FILE EXISTS",
                               JOptionPane.YES_NO_OPTION,
                               JOptionPane.WARNING_MESSAGE
	                    )
	            );
	            try {
	                PrintStream fout =
	                    new PrintStream(new FileOutputStream(f));
	                
	                synchronized(myStampTable) {
	                    int rows = myStampTable.getRowCount();
	                    int cols = myStampTable.getColumnCount();
	                    
	                    // Output the header line
	                    for (int j=0; j<cols; j++)
	                        fout.print(myStampTable.getColumnName(j)
	                                   + (j!=cols-1 ? "\t" : "\n"));
	                    
	                    // Output the data
	                    for (int i=0; i<rows; i++)
	                        for (int j=0; j<cols; j++)
	                            fout.print(myStampTable.getValueAt(i, j)
	                                       + (j!=cols-1 ? "\t" : "\n"));
	                }
	            } 
	            catch(FileNotFoundException ex){
	                JOptionPane.showMessageDialog(
	                		OutlineFocusPanel.this,	                                              
	                		"Unable to open file!\n" + f,
                          "FILE OPEN ERROR",
                          JOptionPane.ERROR_MESSAGE
	                );
	            }
	        }
	    });
	    
	    /// NEW EXPORT        
	    JButton newExport = new JButton( new SaveAction("Export Table as CSV", ",")); 
	    /// NEW EXPORT
	    
	    //add to top panel
		top.add(findStamp);
	    top.add(exportBtn);
		top.add(newExport);
		
	    
		//bot panel has limit check box and count label
	    JPanel bot = new JPanel();
	    bot.setLayout(new GridBagLayout());
	    
	    countLbl = new JLabel(recordCountStr);
	    limitToMainViewCBx = new JCheckBox(limitMainViewAct);
	    limitToMainViewCBx.setSelected(myStampTable.limitToMainView);
	    
	    //add to bottom panel
	    row = 0;
	    bot.add(countLbl, new GridBagConstraints(0, row, 2, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
	    bot.add(limitToMainViewCBx, new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));

	    //center panel has table and bot panel
	    JPanel newCenterPanel = new JPanel(new BorderLayout());
	    newCenterPanel.add(new JScrollPane(myStampTable), BorderLayout.CENTER);	    
	    newCenterPanel.add(bot, BorderLayout.SOUTH);
	    
	    //add everything to main panel
	    setLayout(new BorderLayout());
	    add(top,    BorderLayout.NORTH);
	    add(newCenterPanel, BorderLayout.CENTER);
	    
	    // if either of these conditions are met, there will be 
	    // spot panel added to the BorderLayout.SOUTH of the main panel
	    if (myLayer.spectraData() || myLayer.pointShapes()) {
	    	
	    	//the hide based on range goes back in the 
	    	// "bot" panel from above
	    	//only show hide for spectra, not points 
	    	//TODO: make this work for points?
	    	if(myLayer.spectraData()){
	    		hideOutofRangeCBx = new JCheckBox("Hide values outside of range");
				hideOutofRangeCBx.addActionListener(valuesChanged);
				hideOutofRangeCBx.setSelected(mySettings.hideValuesOutsideRange);
				bot.add(hideOutofRangeCBx, new GridBagConstraints(1, row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
	    	}
			
	    //spot color panel -- includes color basis, min, max fields, and colorMapper
			spotPnl = new JXTaskPane();
			spotPnl.setLayout(new GridBagLayout());
			spotPnl.setTitle("Spot Color");
			spotPnl.setAnimated(false);
			spotPnl.addComponentListener(spotPaneListener);
	    	
	    	//base color column (or expression if tes layer)
		    //column names to choose from
		    columnBx = new JComboBox<String>();
		    columnBx.addActionListener(columnBoxListener);
		    
		    //if this is a spectra layer create components 
		    // switching between column or expression
		    JPanel colorPnl = new JPanel(new GridBagLayout());
	    	if(myLayer.spectraData()){
	    		//base color on column or expression text
	    		columnRBtn = new JRadioButton("Column Value:");
	    		columnRBtn.addActionListener(radioButtonListener);
		    	expRBtn = new JRadioButton("Expression:");
		    	expRBtn.addActionListener(radioButtonListener);
				
		    	ButtonGroup colorBasis = new ButtonGroup();
		    	colorBasis.add(columnRBtn);
		    	colorBasis.add(expRBtn);
		    	
		    	//expression area to type formula
				expTA = new JTextArea();
				expTA.setLineWrap(true);
	//			text.setWrapStyleWord(true); //TODO: maybe not?
				expTA.setText(mySettings.expressionText);
				expTA.getDocument().addDocumentListener(expressionListener);
				JScrollPane expSp = new JScrollPane(expTA);
				expSp.setPreferredSize(new Dimension(150,50));
				expSp.setMinimumSize(new Dimension(0, 50));
		        //error label for expression
				errorsLbl = new JLabel(noErrorsStr);
				
				//set starting states for expression ui components
		    	if (mySettings.expressionSelected) {
		    		expRBtn.setSelected(true);
					columnBx.setEnabled(false);
					expTA.setEnabled(true);
					errorsLbl.setEnabled(true);
		    	} else {
		    		columnRBtn.setSelected(true);
					columnBx.setEnabled(true);
					expTA.setEnabled(false);
					errorsLbl.setEnabled(false);
					compiledExpression=null;
					expTA.setBackground(Util.panelGrey);
		    	}
		    	
		    	//add everything to color panel
		    	row = 0;
		    	colorPnl.add(columnRBtn, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
		    	colorPnl.add(columnBx, new GridBagConstraints(1, row, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
		    	row++;
		    	colorPnl.add(expRBtn, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
		    	colorPnl.add(expSp, new GridBagConstraints(1, row, 1, 2, 1, 0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, in, pad, pad));
		    	row++; row++;
		    	colorPnl.add(errorsLbl, new GridBagConstraints(0, row, 2, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
	    	}
		    	
			
		    //min, max labels and text fields
		    Dimension minDim = new Dimension(120,19);
		    minValueLbl = new JLabel(minValStr);
		    maxValueLbl = new JLabel(maxValStr);
			Color absValCol = new Color(100,100,100);
			absMinTF = new JTextField(10);
			absMinTF.setEditable(false);
			absMinTF.setForeground(absValCol);
			absMinTF.setMinimumSize(minDim);
			absMaxTF = new JTextField(10);
			absMaxTF.setEditable(false);
			absMaxTF.setForeground(absValCol);
			absMaxTF.setMinimumSize(minDim);
			minTF = new JTextField(10);
			minTF.setMinimumSize(minDim);
			minTF.addActionListener(valuesChanged);
			minTF.addFocusListener(focusChanged);
			maxTF = new JTextField(10);
			maxTF.setMinimumSize(minDim);
			maxTF.addActionListener(valuesChanged);
			maxTF.addFocusListener(focusChanged);
		    double min = mySettings.colorMin;
		    double max = mySettings.colorMax;
		    if (!Double.isNaN(min)) {
		    	minTF.setText(format.format(mySettings.colorMin));
		    }
		    if (!Double.isNaN(max)) {
		    	maxTF.setText(format.format(mySettings.colorMax));
		    }
		    //put min/max labels and textfields in a panel
		    JPanel valuePnl = new JPanel(new GridBagLayout());
		    row = 0;
		    valuePnl.add(minValueLbl, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
		    valuePnl.add(absMinTF, new GridBagConstraints(1, row, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
		    valuePnl.add(maxValueLbl, new GridBagConstraints(2, row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
		    valuePnl.add(absMaxTF, new GridBagConstraints(3, row, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));

		    row++;
		    valuePnl.add(new JLabel("Min value: "), new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
		    valuePnl.add(minTF, new GridBagConstraints(1, row, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
		    valuePnl.add(new JLabel("Max value: "), new GridBagConstraints(2, row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
		    valuePnl.add(maxTF, new GridBagConstraints(3, row, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
	    	//if this is point shapes stamp layer, add a scale option
	    	if (myLayer.pointShapes()) {
	    		scaleTF = new JTextField(10);
		  		scaleTF.setText(""+mySettings.getOriginMagnitude());
			    scaleTF.addActionListener(scaleListener);
				scaleTF.addActionListener(valuesChanged);
				scaleTF.addFocusListener(focusChanged);
				row++;
	    		valuePnl.add(new JLabel("Scale factor: "),new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
	    		valuePnl.add(scaleTF,new GridBagConstraints(1, row, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
	    	}
			
	    	//color mapper
	    	mapper = new FancyColorMapper();
			if (mySettings.colorState!=null) {
				mapper.setState(mySettings.colorState);
			}
		    mapper.addChangeListener(mapperListener);
	    	

		    //Add everything to spot panel
			//always add the base color label
			row = 0;
	    	spotPnl.add(new JLabel("Base color on:"),new GridBagConstraints(0, row, 1, 2, 0, 0, GridBagConstraints.FIRST_LINE_END, GridBagConstraints.NONE, in, pad, pad));
	    	//if spectra, add color panel
	    	if(myLayer.spectraData()){
	    		spotPnl.add(colorPnl, new GridBagConstraints(1, row, 1, 1, 1, 0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, in, pad, pad));
	    	}
	    	//if point shapes, just add column box
	    	if(myLayer.pointShapes()){
	    		spotPnl.add(columnBx, new GridBagConstraints(1, row, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
	    	}
	       	//min,max,scale values
	    	row++;
	    	spotPnl.add(valuePnl, new GridBagConstraints(0, row, 3, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
	    	//color mapper
	    	row++;
		    spotPnl.add(mapper, new GridBagConstraints(0, row, 4, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, in, pad, pad));

		    //put the spot panel into a jxtastpanecontainer
		    JXTaskPaneContainer spotContainer = new JXTaskPaneContainer();
		    spotContainer.setBackground(Util.panelGrey);
		    spotContainer.add(spotPnl);
		    
		    //add panel to main panel
		    add(spotContainer, BorderLayout.SOUTH);
	    }
	}
	
	private Action findAct = new AbstractAction("Find stamp...") {
		public void actionPerformed(ActionEvent e) {
			String id = JOptionPane.showInputDialog(OutlineFocusPanel.this,
                    "Enter a stamp id:", "Find stamp...", JOptionPane.QUESTION_MESSAGE);

                if (id == null) {
                    return;
                }
                
                StampShape stamp = myLayer.getStamp(id.trim());
                if (stamp!=null) {
                	myLayer.clearSelectedStamps();
                	myLayer.viewToUpdate.panToStamp(stamp);
                    return;
                }
                    
                JOptionPane.showMessageDialog(OutlineFocusPanel.this,
                                              "Can't find the stamp \"" + id
                                              + "\", are you sure\n"
                                              + "it meets your layer's selection criteria?",
                                              "Find stamp...",
                                              JOptionPane.ERROR_MESSAGE);
			
		}
	};
	
	private Action limitMainViewAct = new AbstractAction("Only show records in Main View") {
		public void actionPerformed(ActionEvent e) {
			myStampTable.limitToMainView=!myStampTable.limitToMainView;
			myStampTable.dataRefreshed();
			dataRefreshed();
		}
	};
	
    private ActionListener valuesChanged = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			updateChanges();
		}
	};
	
	private FocusListener focusChanged = new FocusListener() {
		public void focusLost(FocusEvent e) {
			updateChanges();
		}
		public void focusGained(FocusEvent e) {
			// Nothing to do here
		}
	};
	
	private void updateChanges(){
		recalculateColors();
		updateSettings();
		refreshViews();
	}
	
	private ActionListener radioButtonListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			boolean isColumn = (e.getSource() == columnRBtn);
			columnBx.setEnabled(isColumn);
			expTA.setEnabled(!isColumn);
			errorsLbl.setEnabled(!isColumn);
			mySettings.expressionSelected=!isColumn;
			if(isColumn){
				compiledExpression=null;
				expTA.setBackground(Util.panelGrey);
			}else{
				mySettings.expressionText=expTA.getText();
				expTA.setBackground(Color.WHITE);
			}
			recalculateMinMaxValues();
			refreshViews();		
			
		}
	};
	
	private ActionListener columnBoxListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			compiledExpression = null;
			recalculateMinMaxValues();
			mySettings.colorColumn=(String)columnBx.getSelectedItem();
			refreshViews();		
		}
	};
	
	private ActionListener scaleListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			//in order to make sure that the LView and the focus panel are using the same settings object, we will get the 
      		//settings object out of the LView instead of using the final instance that is passed in
			mySettings.setOriginMagnitude(Double.parseDouble(scaleTF.getText()));
       		StampLView child = (StampLView) myLayer.viewToUpdate.getChild();
       		myLayer.viewToUpdate.clearLastOutlines();
   			myLayer.viewToUpdate.drawOutlines();
   			child.clearLastOutlines();
  			child.drawOutlines();
		}
	};
	
	private DocumentListener expressionListener = new DocumentListener() {
		public void changedUpdate(DocumentEvent e) {
			change();
		}
		public void insertUpdate(DocumentEvent e) {
			change();
		}
		public void removeUpdate(DocumentEvent e) {
			change();
		}
		private void change() {
			try {

				CompiledExpression exp = null;
				exp = Evaluator.compile("convertReturnType("+expTA.getText()+")", lib, Object.class);
				compiledExpression = exp;

				recalculateMinMaxValues();
				
				if((expTA.getText()!=null)&&(expTA.getText().length()>0)){
					errorsLbl.setForeground(Color.DARK_GRAY);
					errorsLbl.setText(noErrorsStr);
					myLayer.viewToUpdate.clearLastOutlines();
					myLayer.viewToUpdate.drawOutlines();
				} else {
					errorsLbl.setForeground(Color.DARK_GRAY);
					errorsLbl.setText(noErrorsStr);
				}
				mySettings.expressionText=expTA.getText();
			} catch (Throwable e) {
				compiledExpression=null;
				
				// make no change on error, just whine about it
				String msg = e.getMessage();
				while (e.getCause() != null) {
					e = e.getCause();
					msg += "\n  Caused by: " + e.getMessage();
				}
				if(msg!=null){
					errorsLbl.setForeground(Color.RED);
					errorsLbl.setText(msg);
				}else{
					errorsLbl.setForeground(Color.DARK_GRAY);
					errorsLbl.setText(noErrorsStr);
				}
			}
			refreshViews();					
		}
	};
	
	
	private ChangeListener mapperListener = new ChangeListener() {
		public void stateChanged(ChangeEvent e) {
            if (mapper.isAdjusting()){
                return;
            }
            recalculateColors();
			mySettings.colorState=mapper.getState();
			refreshViews();			
		}
	};
	
	private ComponentListener spotPaneListener = new ComponentListener() {
		public void componentShown(ComponentEvent e) {}
		public void componentMoved(ComponentEvent e) {}
		public void componentHidden(ComponentEvent e) {}
		
		@Override
		public void componentResized(ComponentEvent e) {
			//only reset split pane if the spot panel is being expanded
			if(!spotPnl.isCollapsed()){
				if(OutlineFocusPanel.this.getParent() instanceof JSplitPane){
					((JSplitPane)OutlineFocusPanel.this.getParent()).resetToPreferredSizes();
				}
			}
		}
	};
	
	
	public boolean hideOutofRange() {
		if (hideOutofRangeCBx==null) return false;
		return hideOutofRangeCBx.isSelected();
	}
	
	
	
	public double getMinValue() {
		String val = minTF.getText();
		
		if (val==null || val.length()==0) {
			recalculateMinMaxValues();
		}
		return parseTextToDouble(minTF.getText());
	}
	
	public double getMaxValue() {
		return parseTextToDouble(maxTF.getText());
	}

	public void setMinValue(double newVal) {
		if (!Double.isNaN(newVal)) {
			minTF.setText(format.format(newVal));
		} else {
			minTF.setText("Undefined");
		}
	}
	
	public void setMaxValue(double newVal) {
		if (!Double.isNaN(newVal)) {
			maxTF.setText(format.format(newVal));
		} else {
			maxTF.setText("Undefined");
		}		
	}
	
	public void recalculateMinMaxValues() {
		// This synchronized block is required to prevent a race condition, where the minField or maxField can be set multiple times.
		// Apparently the setText on a TextField is NOT thread-safe, and you can end up with the value being duplicated multiple times.
		synchronized(minTF) {
			double min = Double.MAX_VALUE;
			double max = Double.NEGATIVE_INFINITY;
		
			ArrayList<StampShape> stamps = myLayer.getStamps();
		
			if (stamps==null) {
				return;
			}
			
			int columnToColor = getColorColumn();
			try {
				for (StampShape s : stamps) {
					double v=Double.NaN;
					if (compiledExpression!=null) {
						StampAccessor sa[] = new StampAccessor[1];
						sa[0]=new StampAccessor(myLayer, s);
						
						try {
							Object o = compiledExpression.evaluate(sa);
							v = Double.parseDouble(o+"");
						} catch (Throwable e) {
//							System.err.println("exception on stamp: " + s.getId());
//							e.printStackTrace();
							// DO NOT BREAK, or the calculated value does not get set on the stamp shape
//    							break;
						}	
					} else {
						try {
							Object o = s.getStamp().getData()[columnToColor];
							v = Double.parseDouble(o+"");	
						} catch (Exception nfe) {
							// Ignore any number format exceptions
							// DO NOT CONTINUE, or the calculated value does not get set on the stamp shape
							//continue;
						}
					}
				
					s.setCalculatedValue(v);
					
					if (v == StampImage.IGNORE_VALUE) continue;
					if (v == -Float.MAX_VALUE) continue;   
					//	if (v == 0) continue;   // treat as an ignore value
					if (v>max) max = v;
					if (v<min) min = v;
				}
			
				if (min == Double.MAX_VALUE) {
					min = Double.NaN;
					absMinTF.setText("Undefined");
				} else {
					absMinTF.setText(format.format(min));					
				}
				
				if (max == Double.NEGATIVE_INFINITY) {
					max = Double.NaN;
					absMaxTF.setText("Undefined");
				} else {
					absMaxTF.setText(format.format(max));					
				}
				
				setMinValue(min);
				setMaxValue(max);
				
				//to avoid a really bad loop, only recalculate colors if 
				// min and max values are not NaN
				if(!Double.isNaN(min) && !Double.isNaN(max)){
					recalculateColors();
				}
			} catch (Exception e) {
				e.printStackTrace();
				
				// TODO: Do something sensible here
			}
		}
	}
	
	
	public Color[] getColorMap() {
		return mapper.getColorScale().getColorMap();
	}
	
	private double parseTextToDouble(String valStr) {
		double value = Double.NaN;
				
		if (valStr!=null && valStr.length()>0) {
			try {
				value=Double.parseDouble(valStr);
			} catch (Exception e) {
				
			}
		}
		
		return value;	
	}
	
	public void setColumnColorOptions(String newNames[]) {
		if (!myLayer.colorByColumn()) return;
		
		// Only do this the first time we actually get column data
//	    if (columnToColor.getModel().getSize()>0) return;
	    
	    ArrayList<String> validNames = new ArrayList<String>();
	    String defaultColumn = "";
		String tipCol=myLayer.getParam(myLayer.TOOLTIP_COLUMN);
		
	    for (int i=0; i<newNames.length; i++) {
			Class columnType =  myLayer.getColumnClass(i);
			if (columnType==null) continue;
			if (Number.class.isAssignableFrom(columnType)) {
				validNames.add(newNames[i]);
			}
	    }
	    
	    if (mySettings.colorColumn!=null && mySettings.colorColumn.length()>0) {
	    	defaultColumn = mySettings.colorColumn;
	    } else if (tipCol!=null && tipCol.length()>0) {
	    	defaultColumn = tipCol;
	    }
	    
	    columnBx.setModel(new DefaultComboBoxModel(newNames));
	   
	    if (defaultColumn.length()>0) {
	    	columnBx.setSelectedItem(defaultColumn);
	    	
	    	// Changing the selected column will overwrite the values in the user's session.  So re-overwrite them to the proper values again
	    	double min = mySettings.colorMin;
	    	double max = mySettings.colorMax;
	    	
	    	if (!Double.isNaN(min)) {
	    		minTF.setText(""+min);
	    	} 
	    	
	    	if (!Double.isNaN(max)) {
	    		maxTF.setText(""+max);
	    	}
	    } 
	    
	    recalculateMinMaxValues();
	    
	    columnBx.setModel(new DefaultComboBoxModel(validNames.toArray()));
	    
	    if (mySettings.expressionSelected) {
		    //// Silliness
		    StampAccessor stampMap = new StampAccessor(myLayer, null);
			final Library lib = new Library(
				new Class[]{Math.class, FieldFormulaMethods.class},
				new Class[]{StampAccessor.class},
				new Class[]{},
				stampMap,
				null);					
			/// Maybe end of Silliness
			
			// Try to compile the expression restored from a saved session.  Ignore any exceptions.
			try {
				CompiledExpression exp = null;
				exp = Evaluator.compile("convertReturnType("+mySettings.expressionText+")", lib, Object.class);
				compiledExpression = exp;
			} catch (Exception e) {
				e.printStackTrace();
			}
	    } else if (mySettings.colorColumn!=null && mySettings.colorColumn.length()>0) {
	    	columnBx.setSelectedItem(mySettings.colorColumn);
	    } 
	    
	    recalculateMinMaxValues();

    	// Changing the selected column will overwrite the values in the user's session.  So re-overwrite them to the proper values again
    	double min = mySettings.colorMin;
    	double max = mySettings.colorMax;
    	
    	if (!Double.isNaN(min)) {
    		minTF.setText(""+min);
    	} 
    	
    	if (!Double.isNaN(max)) {
    		maxTF.setText(""+max);
    	}
	}
	
	public int getColorColumn() {
		if (columnBx==null) return -1;

		String columnName = (String) columnBx.getSelectedItem();

		int cnt = myLayer.viewToUpdate.myFocus.table.getTableModel().getColumnCount();
			
		for (int i=0 ; i<cnt; i++) {
			String colName=myLayer.viewToUpdate.myFocus.table.getTableModel().getColumnName(i);
			if (colName.equalsIgnoreCase(columnName)) {
				return i;
			}
		}		
		return -1;		
	}
	
	public void dataRefreshed() {
		if (limitToMainViewCBx.isSelected()) {
			updateRecordCount(myLayer.viewToUpdate.stamps.length);
		} else {
			updateRecordCount(myLayer.getVisibleStamps().size());
		}

	}
	
	public CompiledExpression getExpression() {
		return compiledExpression;
	}

	private void updateRecordCount(int newCnt) {
		countLbl.setText(recordCountStr + newCnt);
	}
	
public static final class StampAccessor extends DVMap implements Serializable{
	private static final long serialVersionUID = 1L;
	
	private final StampLayer stampLayer;
	private final StampShape stamp;
	
	/** Creates the name->field mapping for all possible fields. */
	public StampAccessor(StampLayer thisLayer, StampShape stamp) {
		stampLayer = thisLayer;
		this.stamp = stamp;
	}
		
	/**
	 * Called by the compiler to get the variable type, which the JEL
	 * assembler will use to determine which get<Type>Property() method to
	 * call. We fail any type for which this class does not have such a
	 * get<Type>Property() method.
	 */
	public String getTypeName(String name) {
		int cnt = stampLayer.getColumnCount();
		
		for (int i=0 ; i<cnt; i++) {
			String colName=stampLayer.getColumnName(i);
			if (colName.equalsIgnoreCase(name)) {				
				Class columnType =  stampLayer.getColumnClass(i);

				if (columnType==null) continue;
				
				if (String.class.isAssignableFrom(columnType)) {
					return "String";
				} else if (Boolean.class.isAssignableFrom(columnType)) {
					return "Boolean";
				} else if (Color.class.isAssignableFrom(columnType)) {
					return "Color";
				} else if (Byte.class.isAssignableFrom(columnType)) {
					return "Byte";
				} else if (Short.class.isAssignableFrom(columnType)) {
					return "Short";
				} else if (Integer.class.isAssignableFrom(columnType)) {
					return "Integer";
				} else if (Long.class.isAssignableFrom(columnType)) {
					return "Long";
				} else if (Float.class.isAssignableFrom(columnType)) {
					return "Float";
				} else if (Double.class.isAssignableFrom(columnType)) {
					return "Double";
				} else if (double[].class.isAssignableFrom(columnType)) {
					return "DoubleArray";
				} else if (BigDecimal.class.isAssignableFrom(columnType)) {
					return "BigDecimal";
				} else {
//					System.out.println("type = " + columnType);
					// TODO: We get back an Object, even though it's ultimately a Float
					return "Object";
				}
				
			}
		}	
		return null;
	}
	
	/**
	 * Called by the compiler to convert variable names into field indices,
	 * matching in a case-insensitive way.
	 */
	public Object translate(String name) {
		int cnt = stampLayer.getColumnCount();
		
		for (int i=0 ; i<cnt; i++) {
			String colName=stampLayer.getColumnName(i);
			if (colName.equalsIgnoreCase(name)) {
				return i;
			}
		}
		throw new IllegalArgumentException("Name " + name + " not found");
	}
	
	/**
	 * Called by the evaluator to get the value at the given column
	 * position. We don't optimize access to attributes, beyond the
	 * name->field lookup, because a Feature can contain hundreds of
	 * columns, and the time to optimize will greatly exceed the cost of a
	 * single lookup for a single column, which is probably the common case.
	 */
	public Object getProperty(int column) {
		Object o = stamp.getData(column);
		return o; 
	}
	public String getStringProperty(int column) {
		return (String)getProperty(column);
	}
	
	// TODO: Return as 1 or 0?
	public Boolean getBooleanProperty(int column) {
		return (Boolean)getProperty(column);
	}
	public Color getColorProperty(int column) {
		return (Color)getProperty(column);
	}
	public Byte getByteProperty(int column) {
		return (Byte)getProperty(column);
	}
	public Short getShortProperty(int column) {
		return (Short)getProperty(column);
	}
	public Integer getIntegerProperty(int column) {
		return (Integer)getProperty(column);
	}
	public Long getLongProperty(int column) {
		return (Long)getProperty(column);
	}
	public Float getFloatProperty(int column) {
		return (Float)getProperty(column);
	}
	public Double getDoubleProperty(int column) {
		return (Double)getProperty(column);
	}
	
	// Any BigDecimals we have as parameters need to be converted into Doubles, because JEL doesn't support BigDecimals
	public Double getBigDecimalProperty(int column) {
		// TODO: Does this test for null need to be done in every one of these methods?  Can the others actually be null?
		Object o = getProperty(column);
		if (o==null) return Double.NaN;
		return(Double)((BigDecimal)o).doubleValue();
	}
	
	public Object getObjectProperty(int column) {
		return (Object)getProperty(column);
	}
	
	public Double[] getDoubleArrayProperty(int column) {
		Double doubles[] = null;

		Object o = getProperty(column);

		if (o==null) {
			doubles = new Double[0];
			return doubles;
		}

		if (o instanceof double[]) {
			double[] vals = (double[])o;
			
			doubles = new Double[vals.length];
			
			for (int i=0; i<vals.length; i++) {
				doubles[i]=vals[i];
			}
			return doubles;
		} 
		if (o instanceof Double[]) {
			Double[] vals = (Double[])o;

			doubles = new Double[vals.length];

			for (int i=0; i<vals.length; i++) {
				doubles[i]=vals[i];
			}
			return doubles;
		}

		if (o instanceof float[]) {
			float[] vals = (float[])o;

			doubles = new Double[vals.length];

			for (int i=0; i<vals.length; i++) {
				doubles[i]=(double)vals[i];
			}
			return doubles;
		} 

		if (o instanceof Float[]) {
			Float[] vals = (Float[])o;

			doubles = new Double[vals.length];

			for (int i=0; i<vals.length; i++) {
				doubles[i]=new Double(vals[i]);
			}
			return doubles;
		}
		else {
			// TODO: Hokey
			//System.out.println("bleh: " + o);
		}
		
		return doubles;
	}
}

	FileFilter textFormatFilter = new FileFilter(){				
		public String getDescription() {
			return "Tab delimited file (.tab, txt)";
		}
		
		public boolean accept(File f) {
			if (f.isDirectory()) return true;
			if (f.getName().endsWith(".txt")) return true;
			if (f.getName().endsWith(".tab")) return true;

			return false;
		}
	};

	class SaveAction extends AbstractAction {
		String delimiter = "\t";
		
		SaveAction(String windowText) {
			super(windowText);
		}
		
		SaveAction(String windowText, String delimiterToUse) {
			super(windowText);
			delimiter=delimiterToUse;
		}
		//new AbstractAction("Export Table as CSV") {
		  
		public void actionPerformed(ActionEvent e){
		    final JFileChooser fileChooser = new JFileChooser();
			fileChooser.setDialogTitle("Export Stamp Table");
	        fileChooser.setAcceptAllFileFilterUsed(false);
	        fileChooser.setFileFilter(textFormatFilter);
			
	        File selectedFile;
	        
	        while(true) {
	        	int optionChose = fileChooser.showSaveDialog(OutlineFocusPanel.this); 
	        	
	        	// If the user didn't click the OK button, abort this whole process
	        	if (optionChose != JFileChooser.APPROVE_OPTION) {
	        		return;
	        	}
	        	
	        	selectedFile = fileChooser.getSelectedFile();

	            if (!selectedFile.getName().endsWith(".txt")&& !selectedFile.getName().endsWith(".tab")) {
	            	selectedFile=new File(selectedFile.getAbsolutePath()+".txt");
	            } 	

	            if (selectedFile.exists()) {
	            	int overwrite=JOptionPane.showConfirmDialog(
	                		OutlineFocusPanel.this,
	                       "File already exists, overwrite?\n" + selectedFile,
	                       "FILE EXISTS",
	                       JOptionPane.YES_NO_OPTION,
	                       JOptionPane.WARNING_MESSAGE
	                );
	            	
	            	if (overwrite == JOptionPane.NO_OPTION) {
	            		continue;
	            	}
	            }
	            
	            break;
	        }
	        
	        PrintStream fout=null;
	        try {
	            fout = new PrintStream(new FileOutputStream(selectedFile));
	            
	            synchronized(myStampTable) {
	            	HashMap<String,Integer> maxRecordsPerColumn = new HashMap<String,Integer>();	
	            	
	                int rows = myStampTable.getRowCount();
	                int cols = myStampTable.getColumnCount();
	                
	                // Determine how many elements will be exported from each column.  Arrays will expand into multiple columns
	                for (int j=0; j<cols; j++) {
	                	String columnName = myStampTable.getColumnName(j);
	                	
	                	// Start with the assumption that each column is 1 element
	                	maxRecordsPerColumn.put(columnName, 1);
	                	
	                	if (myLayer.getColumnClass(myLayer.getColumnNum(columnName)).isArray()) {
                			int curMax = maxRecordsPerColumn.get(columnName);
	                		for (int i=0; i<rows; i++) {
	                			Object dataVal = myStampTable.getStamp(i).getData(myLayer.getColumnNum(columnName));
	                			
	                			if (dataVal==null) continue;
	                			int n = java.lang.reflect.Array.getLength(dataVal);
	                			curMax = Math.max(n, curMax);
	                		}
                			maxRecordsPerColumn.put(columnName, curMax);
	                	}	                	
	                }
	                
	                // Output the header line
	                for (int j=0; j<cols; j++) {
	                	String columnName = myStampTable.getColumnName(j);

	                    if (j>0) {
	                    	fout.print(delimiter);
	                    }

	                	int numValues = maxRecordsPerColumn.get(columnName);
	                    if (numValues==1) {
	                    	fout.print(columnName);
	                    } else {
	                    	for (int i=0; i<numValues; i++) {
	                    		fout.print(columnName+"["+(i+1)+"]");
	                    		fout.print(delimiter);
	                    	}
	                    	// Add an extra header column for the number of values exported for this table column
	                    	fout.print(columnName+"SampleCount");
	                    }
	                }
	                
	                if (myLayer.spectraData()) {
	                	// Add a column with the number of columns exported for each array
//	            		for (int x=0; x<maxXAxis; x++) {
//		                	fout.print(delimiter);
//		                	fout.print("xaxis["+(x+1)+"]");
//	            		}
	                }
	                fout.print("\n");
	                
	                // Output the data
	                for (int i=0; i<rows; i++) {
	                    for (int j=0; j<cols; j++) {
	                    	if (j>0) {
	                    		fout.print(delimiter);
	                    	}
	                    	
	                    	Object dataVal = myStampTable.getValueAt(i, j);
	                    	
	                    	//
		                	String columnName = myStampTable.getColumnName(j);
//		                	System.out.println("columnName = " + columnName);
//		                	
//		                	for (String key : maxRecordsPerColumn.keySet()) {
//		                		System.out.println("Key = " + key);
//		                		System.out.println("val = " + maxRecordsPerColumn.get(key));
//		                	}
		                	
		                	//
		                	int numValues = maxRecordsPerColumn.get(myStampTable.getColumnName(j));
		                    if (numValues==1) {
		                    	fout.print(dataVal);
		                    } else {
		                    	for (int x=0; x<numValues; x++) {
		                    		fout.print(Array.get(dataVal, x));
		                    		fout.print(delimiter);
		                    	}
		                    	// Add an extra header column for the number of values exported for this table column
		                    	fout.print(numValues);
		                    }
	                    }
	                    
	                    
	                    
	                    
	                    
		                if (myLayer.spectraData()) {
		                	
		                }
	                    fout.print("\n");
	                }
	            }
	        } catch(FileNotFoundException ex){
	            JOptionPane.showMessageDialog(OutlineFocusPanel.this,	                                              
	            	"Unable to open file!\n" + selectedFile, "FILE OPEN ERROR", JOptionPane.ERROR_MESSAGE);
	        } finally {
	        	if (fout!=null) {
	        		fout.close();
	        	}
	        }
		}
	}
	
	private void recalculateColors() {
		ArrayList<StampShape> stamps = myLayer.getStamps();
		
		if (stamps==null) {
			return;
		}

		for (StampShape s : stamps) {
			s.setCalculatedColor(null);
		}
		
		Color colors[] = getColorMap();
		
		double min = getMinValue();
		double max = getMaxValue();
		
		if (Double.isNaN(min)||Double.isNaN(max)) {
			return;
		}
		
		for (StampShape s : stamps) {
			double val = s.getCalculatedValue();
			float colorVal = (float)((val-min)/(max-min));
			
			// Make outliers transparent?  Offer an option?
			if (colorVal<0) {
				colorVal=0.0f;
				if (hideOutofRange()) {
					continue;
				}
			}
			if (colorVal>1) {
				colorVal=1.0f;
				if (hideOutofRange()) {
					continue;
				}
			}
			
			if (Double.isNaN(colorVal)) {
				continue;
			}	
			
			int colorInt = (int)(colorVal * 255);
			
			Color color = colors[colorInt];

			s.setCalculatedColor(color);
		}		
	}
	
	private void updateSettings(){
		mySettings.colorMin=getMinValue();
		mySettings.colorMax=getMaxValue();
		if(scaleTF != null && scaleTF.getText().length()>0){
			mySettings.setOriginMagnitude(Double.parseDouble(scaleTF.getText()));
		}
		if(hideOutofRangeCBx != null){
			mySettings.hideValuesOutsideRange=hideOutofRangeCBx.isSelected();
		}
	}
	
	
	private void refreshViews() {
		StampLView viewToUpdate = myLayer.viewToUpdate;
		viewToUpdate.clearLastOutlines();
		viewToUpdate.drawOutlines();
		
		myStampTable.repaint();
		
		//update the 3d view if has lview3d enabled
//		if(viewToUpdate.getLView3D().isEnabled()){
//			ThreeDManager mgr = ThreeDManager.getInstance();
//			//If the 3d is already visible, update it
//			if(mgr.getFrame().isVisible()){
//				mgr.update();
//			}
//		}		
	}
	
	
	
}

