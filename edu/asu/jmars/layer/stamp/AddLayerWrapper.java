package edu.asu.jmars.layer.stamp;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.GeneralPath;
import java.io.ObjectInputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;

import org.jdesktop.swingx.JXTaskPane;
import org.jdesktop.swingx.JXTaskPaneContainer;

import cookxml.cookswing.util.SpringLayoutUtilities;
import edu.asu.jmars.util.Util;
import edu.asu.msff.DataField;

public class AddLayerWrapper {
	protected EventListenerList listenerList = new EventListenerList();
	private String instrument;
	private String layergroup;
	FilterParams filter = null;
	final JPanel queryPanel = new JPanel();
	
	public List<StampFilter> filters = new ArrayList<StampFilter>();
	
	ArrayList<GeneralPath> paths = null;
	ArrayList<String> srcItems = null;
	String srcName = null;

	// This is optional and only provided if a layer is being restored from a
	// saved JMARS session.
	StampLayerSettings settings = null;
	
	public AddLayerWrapper(String instrument, String srcName, ArrayList<String> srcItems, ArrayList<GeneralPath> paths) {
		this.instrument = instrument;
		
		this.srcName=srcName;
		this.srcItems=srcItems;
		this.paths=paths;

		init();
	}
	
	public AddLayerWrapper(String instrument, String layergroup) {
		this.instrument = instrument;
		this.layergroup = layergroup;

		init();
	}
	
	// Used for restoring from a saved JMARS session
	public AddLayerWrapper(StampLayerSettings settings) {
		this.settings = settings;
		this.instrument = settings.instrument;
		
		if (settings!=null) {
			paths = settings.paths;
			srcItems = settings.srcItems;
			srcName = settings.srcName;
		}
		
		init();	
	}
	
	private void init() {
		queryPanel.setLayout(new BorderLayout());
		
		JPanel messagePanel = new JPanel();
		messagePanel.setLayout(new FlowLayout());
		
		messagePanel.add(new JLabel("Loading query parameters from server..."));
		queryPanel.add(messagePanel, BorderLayout.CENTER);
		
		Thread thread = new Thread(new Runnable() {
			public void run() {
				
				boolean latAdded=false;
				
				// Get default field list, add a few of our own and grab an iterator.
				Object[] fieldList = getFieldList();

				ArrayList<Object> allFields = new ArrayList<Object>();
				allFields.addAll(Arrays.asList(fieldList));

				List <String>       allCategories  = new ArrayList <String>();
				List <SpringLayout> allLayouts     = new ArrayList <SpringLayout>();
				List <JXTaskPane>   allTaskPanes   = new ArrayList <JXTaskPane>();
				List <JPanel>       allFieldPanels = new ArrayList <JPanel>();            
				List <int[]>        allCounts      = new ArrayList <int[]>();

				JXTaskPaneContainer taskContainer = new JXTaskPaneContainer();

				JLabel lastLabel = null;

				int index=0;

				for(Object next : allFields)
				{            	
					if (next instanceof DataField) {
						DataField df = (DataField)next;
						String category = df.getCategory();

						if (!allCategories.contains(category)) {
							allCategories.add(category);
							SpringLayout newLayout = new SpringLayout();
							allLayouts.add(newLayout);
							allTaskPanes.add(new JXTaskPane());
							allFieldPanels.add(new JPanel(newLayout));
							int cnt[] = new int[1];
							cnt[0]=0;
							allCounts.add(cnt);
						}

						lastLabel = new JLabel(df.getDisplayName(), JLabel.TRAILING);
						//                	lastLabel.setToolTipText("Test tooltiptext");
						index = allCategories.indexOf(category);

						allCounts.get(index)[0]++;
						allFieldPanels.get(index).add(lastLabel);     
										
						ImageIcon icon = Util.loadIcon("resources/unknown.gif");
						
						JLabel li = new JLabel(icon);
						
						if (df.getMinAllowedValue()!=null && df.getMaxAllowedValue()!=null) {
							li.setToolTipText("<html>"+df.getFieldTip() + "<p>" + "Values range from: " + df.getMinAllowedValue() + " to " + df.getMaxAllowedValue()+"</html>");
							StampFilter newFilter = new StampFilter(df);
							
							// Latitude and Longitude are not normal fields.  Exclude them from the list of filters
							if (!df.getFieldName().equalsIgnoreCase("longitude") && !df.getFieldName().equalsIgnoreCase("latitude")) {
								filters.add(newFilter);	
							}					
						} else {
							String tip = df.getFieldTip();
							if (tip!=null && tip.trim().length()>0) {
								li.setToolTipText(df.getFieldDesc() + "<p>"+ df.getFieldTip());
							} else {
								li.setToolTipText("For help with this field, click the Help button at the bottom of this tab");
							}
						}
						
						
						JPanel p = new JPanel(new FlowLayout());
						p.add(li);
						allFieldPanels.get(index).add(li);
						
						if (df.getFieldName().equalsIgnoreCase("latitude")) {
							latAdded=true;
						}
					} else {
						//                	if (next instanceof JTextField) {
							//	                	// hitting enter same as clicking "OK" or "Cancel" button.
						//	                    ( (JTextField) next ).addActionListener(this);
						//	               	}

						JPanel newPanel = new JPanel();
						lastLabel.setLabelFor(newPanel);

						if (next instanceof JList[]) {
							JList list[] = (JList[])next;
							if (list.length==1) {
								JScrollPane areaScrollPane = new JScrollPane(list[0]);
								areaScrollPane.setVerticalScrollBarPolicy(
										JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
								areaScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);                		        
								areaScrollPane.setPreferredSize(new Dimension(250, 50));
								newPanel.setLayout(new FlowLayout());
								newPanel.add(areaScrollPane);
							}					
						} else if (next instanceof JTextArea[]) {
							JTextArea textArea[] = (JTextArea[])next;
							if (textArea.length==1) {
								textArea[0].setWrapStyleWord(true);
								textArea[0].setLineWrap(true);
								JScrollPane areaScrollPane = new JScrollPane(textArea[0]);
								areaScrollPane.setVerticalScrollBarPolicy(
										JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
								areaScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);                		        
								areaScrollPane.setPreferredSize(new Dimension(250, 50));
								newPanel.setLayout(new FlowLayout());
								newPanel.add(areaScrollPane);
							}
						} else if (next instanceof JTextField[]) {
							JTextField textFields[] = (JTextField[])next;
							if (textFields.length==1) {
								newPanel.setLayout(new SpringLayout());
								newPanel.add(textFields[0]);
								SpringLayoutUtilities.makeCompactGrid(newPanel, 1, 1, 6, 6, 6, 6);                			
							} else if (textFields.length==2) {
								newPanel.setLayout(new SpringLayout());
								newPanel.add(textFields[0]);
								newPanel.add(new JLabel(" to ", JLabel.CENTER));
								newPanel.add(textFields[1]);
								SpringLayoutUtilities.makeCompactGrid(newPanel, 1, 3, 6, 6, 6, 6);                			
							} else {
								// error?
							}
						} else if (next instanceof JComboBox[]) {
							JComboBox textFields[] = (JComboBox[])next;
							if (textFields.length==1) {
								newPanel.setLayout(new FlowLayout());
								newPanel.add(textFields[0]);
							} else if (textFields.length==2) {
								newPanel.setLayout(new FlowLayout());
								newPanel.add(textFields[0]);
								newPanel.add(new JLabel(" to "));
								newPanel.add(textFields[1]);
							} else {
								// error?
							}
						} else if (next instanceof JCheckBox[]) {
							JCheckBox textFields[] = (JCheckBox[])next;
							newPanel.setLayout(new SpringLayout());

							for (JCheckBox cb : textFields) {
								newPanel.add(cb);
							}            			
							SpringLayoutUtilities.makeCompactGrid(newPanel, 2, textFields.length/2, 6, 6, 6, 6);
							//        			layout.putConstraint(SpringLayout.WEST, newPanel, 5, SpringLayout.WEST, fieldPanel);

							allLayouts.get(index).putConstraint(SpringLayout.EAST, newPanel, 5, SpringLayout.EAST, allFieldPanels.get(index));
						}

						allFieldPanels.get(index).add(newPanel);

						if (latAdded) {
							allCounts.get(index)[0]++;
							allFieldPanels.get(index).add(new JLabel(""));
							allFieldPanels.get(index).add(new JLabel(""));
							JPanel mvbPanel = new JPanel();
							mvbPanel.add(latLonButton);
							allFieldPanels.get(index).add(mvbPanel);
							latAdded=false;
						}
					}

				}

				int numCategories = allCategories.size();

				for (int i=0; i<numCategories; i++) {            	
					SpringLayoutUtilities.makeCompactGrid(allFieldPanels.get(i), allCounts.get(i)[0], 3, 6, 6, 6, 6);
					allTaskPanes.get(i).add(allFieldPanels.get(i));
					allTaskPanes.get(i).setTitle(allCategories.get(i) + " Parameters");
					allTaskPanes.get(i).setCollapsed(i>0?true:false);
					taskContainer.add(allTaskPanes.get(i));
				}

				JPanel panel = new JPanel(new GridLayout(1,1));
				panel.add(taskContainer);
				final JScrollPane pane = new JScrollPane(panel);            
				pane.getVerticalScrollBar().setUnitIncrement(16);
				// Put it all together.
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						queryPanel.removeAll();
						queryPanel.setLayout(new BorderLayout());
						queryPanel.add(pane, BorderLayout.CENTER);
						queryPanel.validate();
						if (queryPanel.getParent()!=null) {
							Container c = queryPanel.getParent();
							while (c!=null && !(c instanceof JDialog)) {
								c = c.getParent();
							}
							if (c!=null) {
								((JDialog)c).pack();
								c.setSize(c.getWidth(), Math.max(c.getHeight(), 600));
							}
						}
					}
				});
			}
		});
		
		thread.setName("FieldFetcher");
		thread.setPriority(Thread.MIN_PRIORITY);
		thread.setDaemon(true);
		thread.start();		
	}

	/** Returns an ImageIcon, or null if the path was invalid. */
	protected ImageIcon createImageIcon(String path,
	                                           String description) {
		try {
	    java.net.URL imgURL = new URL(path);
	    if (imgURL != null) {
	        return new ImageIcon(imgURL, description);
	    } else {
	        System.err.println("Couldn't find file: " + path);
	        return null;
	    }
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public String getInstrument() {
		return instrument;
	}

	/**
	 * Returns list of field description pairs for adding into
	 * a {@link Container} when called via {@link #buildContainer} 
	 * method.  Each pair must consist of either a Component
	 * instance or a String instance in any combination; the
	 * pair elements will be displayed in left-to-right order.
	 * 
	 * @return List containing pairs of label strings (usually)
	 * and components to add as description/field
	 * pairs to the container.  However, any combination of strings
	 * and components are allowed.
	 * 
	 * @see #buildContainer
	 */
	final JButton latLonButton = new JButton("Set Lon/Lat to bounds of View");

	protected Object[] getFieldList() {
		DataField datafields[] = null;
		HashMap<DataField, JComponent[]> dataMap = new HashMap<DataField, JComponent[]>();

		final JPopupMenu latLonFiller = new JPopupMenu();
		JMenuItem fillVals = new JMenuItem("Set lon/lat to bounds of MainView");
		latLonFiller.add(fillVals);

		final JTextField minLonBox=new JTextField(8);
		final JTextField maxLonBox=new JTextField(8);
		final JTextField minLatBox=new JTextField(8);
		final JTextField maxLatBox=new JTextField(8);

		ActionListener al = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				minLonBox.setText("MainView");
				maxLonBox.setText("");
				minLatBox.setText("");
				maxLatBox.setText("");
			}
		};
		
		latLonButton.addActionListener(al);
		fillVals.addActionListener(al);

		MouseListener lonLatListener = new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getButton()==MouseEvent.BUTTON3) {
					latLonFiller.show(e.getComponent(), e.getX(), e.getY());
				}
			}
		};

		minLonBox.addMouseListener(lonLatListener);
		maxLonBox.addMouseListener(lonLatListener);
		minLatBox.addMouseListener(lonLatListener);
		maxLatBox.addMouseListener(lonLatListener);

		Object[] fields = null;

		try {
			String urlStr = "FieldFetcher?instrument="+getInstrument();
			if (layergroup!=null&&layergroup.length()>0) urlStr+="&group="+layergroup;

			ObjectInputStream ois = new ObjectInputStream(StampLayer.queryServer(urlStr));

			datafields = (DataField[])ois.readObject();

			ois.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (datafields==null) {
			fields = new Object[0];
		} else {
			
			if (paths!=null) {
				// Need to add an extra datafield for the intersection paths
				DataField biggerFields[] = new DataField[datafields.length+1];
				System.arraycopy(datafields, 0, biggerFields, 1, datafields.length);

				DataField df1 = new DataField("Intersection", srcName, "intersect", "intersect", "intersect",  false, false);

				biggerFields[0]=df1;

				datafields=biggerFields;
			}
			
			fields = new Object[datafields.length*2];				
						
			for (int i=0; i<datafields.length; i++) {
				fields[2*i]=datafields[i];

				if (datafields[i].getFieldName().equalsIgnoreCase("intersect")) {
					JList area[] = new JList[1];
					Vector v = new Vector<String>();
					for (String val : srcItems) {
						v.add(val);
					}
					area[0] = new JList(v);
					dataMap.put(datafields[i], area);
					fields[2*i+1]=area;					
				} else if (datafields[i].isMultiSelect()) {
					String values[] = datafields[i].getValidValues();

					JCheckBox options[] = new JCheckBox[values.length];
					
					ArrayList<String> selectedVals = new ArrayList<String>();
					if (settings!=null) {
						String vals[]=getSettingsValueFor("min"+datafields[i].getFieldName()).split("%2C");
						for (int i2=0; i2<vals.length; i2++) {
							selectedVals.add(vals[i2]);
						}
					} else {
						String val = datafields[i].getDefaultValue();
						if (val!=null) {
							String vals[]=val.split("%2C");
							for (int i2=0; i2<vals.length; i2++) {
								selectedVals.add(vals[i2]);
							}				
						}
					}
					
					for (int j=0 ; j<options.length; j++) {
						options[j]=new JCheckBox(values[j]);
						if (selectedVals.contains(values[j])) {
							options[j].setSelected(true);
						}
					}
					dataMap.put(datafields[i], options);
					fields[2*i+1]=options;                			
				} else
					if (datafields[i].isRange()) {                		
						if (datafields[i].getValidValues()!=null) {
							JComboBox combo[] = new JComboBox[2];
							combo[0]=new JComboBox(datafields[i].getValidValues());
							combo[1]=new JComboBox(datafields[i].getValidValues());
							dataMap.put(datafields[i], combo);
							fields[2*i+1]=combo;
							
							if (settings!=null) {
								combo[0].setSelectedItem(getSettingsValueFor("min"+datafields[i].getFieldName()));
								combo[1].setSelectedItem(getSettingsValueFor("max"+datafields[i].getFieldName()));	
							} else {
								String val = datafields[i].getDefaultValue();
								String val2 = datafields[i].getDefaultValue2();
								if (val!=null) combo[0].setSelectedItem(val);
								if (val2!=null) combo[1].setSelectedItem(val2);									
							}
						} else {
							JTextField textField[] = new JTextField[2];

							if (datafields[i].getFieldName().equalsIgnoreCase("longitude")) {
								textField[0] = minLonBox;
								textField[1] = maxLonBox;
							} else if (datafields[i].getFieldName().equalsIgnoreCase("latitude")) {
								textField[0] = minLatBox;
								textField[1] = maxLatBox;
							} else {
								textField[0]=new JTextField(4);
								textField[1]=new JTextField(4);   								
							}

							if (settings!=null) {
								textField[0].setText(getSettingsValueFor("min"+datafields[i].getFieldName()));
								textField[1].setText(getSettingsValueFor("max"+datafields[i].getFieldName()));	
							} else {
								String val = datafields[i].getDefaultValue();
								String val2 = datafields[i].getDefaultValue2();
								if (val!=null) textField[0].setText(val);
								if (val2!=null) textField[1].setText(val2);	
							}

							dataMap.put(datafields[i], textField);
							fields[2*i+1]=textField;                    		
						}                    		
					} else {
						if (datafields[i].getFieldName().equalsIgnoreCase("idList")) {
							JTextArea textArea[] = new JTextArea[1];
							textArea[0]=new JTextArea(3, 12);
							dataMap.put(datafields[i], textArea);
							fields[2*i+1]=textArea;
							
							if (settings!=null) {
								textArea[0].setText(getSettingsValueFor(datafields[i].getFieldName()));
							} else {
								String val = datafields[i].getDefaultValue();
								if (val!=null) textArea[0].setText(val);
							}
						} else if (datafields[i].getValidValues()!=null) {
							JComboBox combo[] = new JComboBox[1];
							combo[0]=new JComboBox(datafields[i].getValidValues());
							dataMap.put(datafields[i], combo);
							fields[2*i+1]=combo;
							
							if (settings!=null) {
								combo[0].setSelectedItem(getSettingsValueFor(datafields[i].getFieldName()));
							} else {
								String val = datafields[i].getDefaultValue();
								if (val!=null) combo[0].setSelectedItem(val);
							}
						} else {
							JTextField textField[] = new JTextField[1];
							textField[0]=new JTextField(4);
							dataMap.put(datafields[i], textField);
							fields[2*i+1]=textField;
							
							if (settings!=null) {
								textField[0].setText(getSettingsValueFor(datafields[i].getFieldName()));
							} else {
								String val = datafields[i].getDefaultValue();
								if (val!=null) textField[0].setText(val);
							}
						}
					}

			}
		}                	

		filter = new FilterParams(getInstrument(), datafields, dataMap);
		filter.paths=paths;
		
		return fields;
	}

	private String getSettingsValueFor(String fieldName) {
		String query = settings.queryStr;
		
		int loc = query.indexOf("&"+fieldName+"=");
		if (loc<0) return "";
		
		String sub = query.substring(loc);
		loc = sub.indexOf("=");
		
		sub = sub.substring(loc+1);
		
		loc = sub.indexOf("&");
		
		if (loc<0) return URLDecoder.decode(sub);
		
		sub = sub.substring(0,loc);
		
		return URLDecoder.decode(sub);
	}
	
	public JPanel getContainer() {
		return queryPanel;
	}

	public List<StampFilter> getFilters() {
		return filters;
	}
	
	public String getQuery() { 
		return filter.getSql();
	}
}