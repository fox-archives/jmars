package edu.asu.jmars.layer.shape2;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import org.jdesktop.swingx.combobox.ListComboBoxModel;

import edu.asu.jmars.Main;
import edu.asu.jmars.layer.util.features.CalculatedField;
import edu.asu.jmars.layer.util.features.Feature;
import edu.asu.jmars.layer.util.features.FeatureCollection;
import edu.asu.jmars.layer.util.features.Field;
import edu.asu.jmars.layer.util.features.FieldArea;
import edu.asu.jmars.layer.util.features.FieldBearing;
import edu.asu.jmars.layer.util.features.FieldList;
import edu.asu.jmars.layer.util.features.FieldFactory;
import edu.asu.jmars.layer.util.features.FieldLat;
import edu.asu.jmars.layer.util.features.FieldLength;
import edu.asu.jmars.layer.util.features.FieldLon;
import edu.asu.jmars.layer.util.features.FieldMap;
import edu.asu.jmars.layer.util.features.FieldFormula;
import edu.asu.jmars.layer.util.features.LookupTable;
import edu.asu.jmars.util.FunctionMap;
import edu.asu.jmars.util.LineType;
import edu.asu.jmars.util.Tarjan;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.util.FunctionMap.Function;

public class ColumnEditor {
	public static final Map<String,Class<?>> basicTypes = new LinkedHashMap<String,Class<?>>();
	static {
		basicTypes.put("Integer Number", Integer.class);
		basicTypes.put("Real Number", Double.class);
		basicTypes.put("String", String.class);
		basicTypes.put("Color", Color.class);
		basicTypes.put("Line Type", LineType.class);
		basicTypes.put("Boolean", Boolean.class);
	}
	
	// this set should contain only one field for each 'type', except for calculated fields
	public static final List<FieldFactory<?>> types = new ArrayList<FieldFactory<?>>();
	static {
		for (String name: basicTypes.keySet()) {
			types.add(new BasicField.Factory(name, basicTypes.get(name)));
		}
		types.add(new FieldLon.Factory());
		types.add(new FieldLat.Factory());
		types.add(new FieldArea.Factory());
		types.add(new FieldLength.Factory());
		types.add(new FieldBearing.Factory());
		types.add(new FieldMap.Factory());
		types.add(new LookupTable.Factory());
		types.add(new FieldFormula.Factory());
		types.add(new FieldList.Factory());
	}
	
	// state of this editing session
	private final DefaultListModel model = new DefaultListModel();
	private final DefaultComboBoxModel model1=new DefaultComboBoxModel();//1787
	private final DefaultTableModel aliasTableModel = new DefaultTableModel(new Object[]{"Alias name","Field name"},0);//1787

	private final Map<Field,CalculatedField> calculatedFields = new LinkedHashMap<Field,CalculatedField>();
	private final Set<Field> toUpdate = new LinkedHashSet<Field>();
	
	//1787 Hashmap to store the alias names and corresponding column names
	private Map<String,String> aliasMap=new HashMap<String,String>();
	//Hashmap to store the filename and corresponding alias names mapping
	public static Map<String, Map<String,String>> fileMap=new HashMap<String,Map<String,String>>();
	private String fileName;
	//end 1787
	
	// other instance fields
	private final ShapeLayer shapeLayer;
	private final FeatureCollection fc;
	private final JList fieldList = new JList(model);
	
	//1787 start 
	private final JComboBox columnName = new JComboBox(model1);
	private final JTable aliasNames=new JTable(aliasTableModel){
		private static final long serialVersionUID = 1L;
		public boolean isCellEditable(int rowIndex,int colIndex){
			return false;
		}
	};
	//1787 end
	private final JLabel errorField = new JLabel();
	private final JButton okPB = new JButton("Okay");
	private final Frame parent;
	private final Set<Field> oldFields = new LinkedHashSet<Field>();
	
	public ColumnEditor(ShapeLayer shapeLayer, FeatureCollection fc, Frame parent) {
		this.shapeLayer = shapeLayer;
		this.fc = fc;
		this.parent = parent;
	}
	
	/**
	 * Called to report any problems with duplicates, missing dependencies, or
	 * cyclic dependencies, and to enable/disable the OK button based on whether
	 * the state is ok.
	 */
	public void validate() {
		List<String> errors = new ArrayList<String>();
		List<String> warnings = new ArrayList<String>();
		
		// report duplicate name/type
		Set<Field> fields = new HashSet<Field>();
		for (Field f: getModelFields()) {
			if (fields.contains(f)) {
				errors.add("Duplicate field " + s(f));
			} else {
				fields.add(f);
			}
		}
		
		// Use a FunctionMap to express the Map<Field,CalculatedField> as a
		// graph, and have the Tarjan code give us our components
		Map<Field,Collection<Field>> graph = new FunctionMap<Field,Collection<Field>>(
				calculatedFields.keySet(),
				new Function<Field,Collection<Field>>() {
			public Collection<Field> calculate(Field key) {
				return calculatedFields.get(key).getFields();
			}
		});		
		Tarjan<Field> tarjan = new Tarjan<Field>(graph);
		for (Collection<Field> scc: tarjan.getComponents()) {
			if (scc.size() > 1) {
				List<String> sccText = new ArrayList<String>();
				for (Field f: scc) {
					sccText.add(s(f));
				}
				// any strongly connected component with multiple elements has a cycle
				errors.add("Cyclic dependency with fields " + Util.join(", ", sccText));
			}
		}
		
		
		// report self-reference
		for (Field key: graph.keySet()) {
			if (graph.get(key).contains(key)) {
				errors.add("Field " + s(key) + " depends on itself");
			}
		}
		
		// report missing dependencies, but ignore FIELD_PATH since it is hidden
		// at the start of this editor
		for (Field key: graph.keySet()) {
			for (Field dep: graph.get(key)) {
				if (!dep.equals(Field.FIELD_PATH)&& !fields.contains(dep)) {
					errors.add("Field " + s(key) + " depends on missing field " + s(dep));
				}
			}
		}
		
		// ensure target fields can be assigned values produced by the field calculators
		for (Field key: calculatedFields.keySet()) {
			CalculatedField calc = calculatedFields.get(key);
			if (!key.type.isAssignableFrom(calc.type)) {
				errors.add("Field " + s(key) + " not assignable from " + s(calc));
			}
		}
		
		List<String> issues = new ArrayList<String>();
		for (String error: errors) {
			issues.add("Error: " + error);
		}
		for (String warn: warnings) {
			issues.add("Warning: " + warn);
		}
		
		errorField.setText("<html><body>" + Util.join("<br>", issues) + "</body></html>");
		okPB.setEnabled(errors.isEmpty());
	}
	
	private static String s(Field f) {
		return f.name + " (" + f.type.getSimpleName() + ")";
	}

	/** shows a modal dialog for editing the columns in a selected shape layer */
	public void showColumnEditor() {
		
		oldFields.clear();
		oldFields.addAll(fc.getSchema());
		oldFields.addAll(shapeLayer.getStyles().getFields());
		oldFields.remove(Field.FIELD_PATH);
		
		calculatedFields.clear();
		calculatedFields.putAll(shapeLayer.calcFieldMap.get(fc).getCalculatedFields());
		
		//1787 It appears that calc fields were never added to the fieldset. They never really worked anyway,
		//so it probably did not matter. Now that they do work, we need them to be in the list of fields
		//in order for them to be found and be able to be compiled when needed.
		oldFields.addAll(calculatedFields.keySet());
		
		model.removeAllElements();
		model1.removeAllElements();//1787
		for (Field f: oldFields) {
			model.addElement(new FieldWrapper(f));
			model1.addElement(f);//1787
		}
		//1787 start		
		aliasNames.setPreferredScrollableViewportSize(new Dimension(50,220));
		
		//get the name of the file for which alias names are to be displayed
		int r=shapeLayer.fileTable.getSelectedRow();
		fileName=(String)shapeLayer.fileTable.getValueAt(r, 1);
		
		if(!(fileMap.containsKey(fileName))){
			fileMap.put(fileName, aliasMap);
		}
		
		/*To describe how this has been setup - There is a Map called fileMap. It stores a filename
		 * and another Map. When building the Edit Columns page, we loop through the fileMap
		 * and look for the correct file name as the key, to get the associated Map containing
		 * alias names. Once the correct one is found, the Map is used to set the aliasMap
		 * in FieldFormula and the aliasTableModel. I am not sure why there are so many Maps being used, 
		 * but if this comment is still here, I did not find a better way of doing it. 
		 */
		
		if (fileMap.containsKey(fileName)) {
			FieldFormula.aliasMap.clear();
			aliasTableModel.setRowCount(0);
			for(Map.Entry<String,String> temporaryMapOfAliases: fileMap.get(fileName).entrySet()){
				String tempKey = temporaryMapOfAliases.getKey();
				String tempValue = temporaryMapOfAliases.getValue();
				FieldFormula.aliasMap.put(tempKey, tempValue);
				aliasTableModel.addRow(new String[] {tempKey, tempValue});
			}
		}
		
		aliasNames.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		//1787 end
		validate();
		
		fieldList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		errorField.setForeground(Color.red);
		
		final JComboBox valueCB = new JComboBox();
		valueCB.setEnabled(false);
		
		final JCheckBox replaceCB = new JCheckBox("Update All Rows");
		replaceCB.setToolTipText("Updates all rows with the value set above; this can take some time for calculated fields");
		replaceCB.setEnabled(false);
		replaceCB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Field field = ((FieldWrapper)fieldList.getSelectedValue()).f;
				if (replaceCB.isSelected()) {
					toUpdate.add(field);
				} else {
					toUpdate.remove(field);
				}
			}
		});
		
		final JButton delPB = new JButton("Delete Column");
		delPB.setEnabled(false);
		
		delPB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int[] rows = fieldList.getSelectedIndices();
					for (int i = rows.length-1; i >= 0; i --) {
					Field field = ((FieldWrapper)model.remove(rows[i])).f;
					model1.removeElement(field);//1787
					//Need to remove from aliasMap
					for(String aliasKey: FieldFormula.aliasMap.keySet()) {
						String aliasValue = FieldFormula.aliasMap.get(aliasKey);
						if (aliasValue.equals(field.name)) {
							FieldFormula.aliasMap.remove(aliasKey);//1787
						}
					}
					
					calculatedFields.remove(field);
					toUpdate.remove(field);
				}
				validate();
				//1787
				aliasTableModel.setRowCount(0);
				for(Map.Entry<?,?> entry:FieldFormula.aliasMap.entrySet()){
					aliasTableModel.addRow(new Object[] {entry.getKey(),entry.getValue()});
				}
				//1787 end
			}
		});
		
		//1787 start
		final JButton delAliasPB=new JButton("Delete Alias name");
		delAliasPB.setEnabled(false);
		aliasNames.getSelectionModel().addListSelectionListener(new ListSelectionListener(){

			public void valueChanged(ListSelectionEvent e) {
				// TODO Auto-generated method stub
				if (e.getValueIsAdjusting()) {
					return;
				}
				int selectCount=aliasNames.getSelectedRowCount();
				delAliasPB.setEnabled(selectCount>0);
			}});
		
		delAliasPB.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				int[] rows = aliasNames.getSelectedRows();
				for(int i=rows.length-1; i>=0; i--){
					int selectedIndex = rows[i];
					String str = (String) aliasTableModel.getValueAt(selectedIndex, 0);
					aliasTableModel.removeRow(selectedIndex);
					FieldFormula.aliasMap.remove(str);
					aliasMap.remove(str);
					Map<String, String> tempMap = fileMap.get(fileName);//get the aliasMap in fileMap
					tempMap.remove(str);
				}
				validate();
			}
		});
		//1787 end
		
		final JScrollPane calcEditorPane = new JScrollPane();
		//set the scroll increment to something more useful
		calcEditorPane.getVerticalScrollBar().setUnitIncrement(15);
		valueCB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Object item = valueCB.getSelectedItem();
				calcEditorPane.setViewportView(null);
				if (item instanceof FieldFactory<?>) {
					FieldFactory<?> fac = (FieldFactory<?>)item;
					Field target = ((FieldWrapper)fieldList.getSelectedValue()).f;
					Field source = calculatedFields.get(target);
					if (source == null || fac.getFieldType() != source.getClass()) {
						source = fac.createField(new LinkedHashSet<Field>(getModelFields()), (Component)e.getSource());
						if (source instanceof CalculatedField) {
							CalculatedField calc = (CalculatedField)source;
							calculatedFields.put(target, calc);
						} else if (calculatedFields.containsKey(target)) {
							calculatedFields.remove(target);
						}
					}
					JPanel editor = fac.createEditor(ColumnEditor.this, source);
					if (editor != null) {
						Box v = Box.createVerticalBox();
						v.add(editor);
						v.add(Box.createGlue());
						Box h = Box.createHorizontalBox();
						h.add(v);
						h.add(Box.createGlue());
						calcEditorPane.setViewportView(h);
					} else {
						calcEditorPane.setViewportView(new JLabel(""));
					}
					calcEditorPane.invalidate();
					calcEditorPane.validate();
				}
				validate();
			}
		});
		
		fieldList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting()) {
					return;
				}
				int selCount = fieldList.getSelectedIndices().length;
				delPB.setEnabled(selCount > 0);
				valueCB.setEnabled(false);
				calcEditorPane.setEnabled(false);
				replaceCB.setEnabled(false);
				if (selCount == 1) {
					Field field = ((FieldWrapper)fieldList.getSelectedValue()).f;
					valueCB.setEnabled(field.editable);
					calcEditorPane.setEnabled(field.editable);
					replaceCB.setEnabled(field.editable);
					replaceCB.setSelected(toUpdate.contains(field));
					List<FieldFactory<?>> calcFields = new ArrayList<FieldFactory<?>>();
					FieldFactory<?> selected = null;
					for (FieldFactory<?> fac: types) {
						if (fac.getDataType() == null || field.type.isAssignableFrom(fac.getDataType())) {
							calcFields.add(fac);
							if (!(fac instanceof BasicField.Factory)) {
								if (fac.getFieldType().isInstance(calculatedFields.get(field))) {
									selected = fac;
								}
							} else if (selected == null) {
								selected = fac;
							}
						}
					}
					valueCB.setModel(new ListComboBoxModel<FieldFactory<?>>(calcFields));
					valueCB.setSelectedItem(selected);
				} else {
					valueCB.setModel(new ListComboBoxModel<Object>(Arrays.asList()));
					valueCB.setSelectedItem(-1);
					calcEditorPane.setViewportView(null);
				}
			}
		});
				
		final JTextField nameText = new JTextField();
		
		final JTextField aliasText=new JTextField();//1787
		columnName.setSelectedItem(null);//1787
					
		final JComboBox typeCombo = new JComboBox(new ListComboBoxModel<FieldFactory<?>>(types));
		typeCombo.setSelectedItem(null);
		
		final JButton addPB = new JButton("Add Column");
		addPB.setEnabled(false);
		
		class EnableAdd {
			public void check() {
				addPB.setEnabled(nameText.getText().length() > 0 &&
					typeCombo.getSelectedItem() != null);
			}
		}
		
		final EnableAdd addEnabler = new EnableAdd();
		
		nameText.getDocument().addDocumentListener(new DocumentListener() {
			public void changedUpdate(DocumentEvent e) {
				addEnabler.check();
			}
			public void insertUpdate(DocumentEvent e) {
				addEnabler.check();
			}
			public void removeUpdate(DocumentEvent e) {
				addEnabler.check();
			}
		});
		typeCombo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				addEnabler.check();
			}
		});
				
		addPB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// warn if column is already present, otherwise schedule for addition
				try {
					String name = nameText.getText();
					FieldFactory<?> fac = (FieldFactory<?>)typeCombo.getSelectedItem();
					// create a field from the factory first, to get the exact target type
					Field field = fac.createField(new LinkedHashSet<Field>(getModelFields()), (Component)e.getSource());
					if (field != null) {//1787
						Field target = new Field(name, field.type, true);
						FieldWrapper wrapper = new FieldWrapper(target);
						model.addElement(wrapper);
						model1.addElement(target);//1787
						if (field instanceof CalculatedField) {
							CalculatedField calcField = (CalculatedField)field;
							toUpdate.add(target);
							calculatedFields.put(target, calcField);
						}
						fieldList.setSelectedValue(wrapper, true);
						nameText.setText("");
						typeCombo.setSelectedIndex(-1);
					}//1787
				} catch (Exception ex) {
					ex.printStackTrace();
					JOptionPane.showMessageDialog(
						parent,
						ex.getMessage(), "Error adding column",
						JOptionPane.ERROR_MESSAGE);
				} finally {
					validate();
				}
			}
		});
		
		//1787 start
		final JButton addAliasPB=new JButton("Set Alias name");
		addAliasPB.setEnabled(false);
		
		class EnableAddAlias{
			public void check(){
				addAliasPB.setEnabled(aliasText.getText().length()>0 && columnName.getSelectedItem()!=null);
			}
		}
		
		final EnableAddAlias enableAlias=new EnableAddAlias();
		
		aliasText.getDocument().addDocumentListener(new DocumentListener(){
			public void changedUpdate(DocumentEvent e){
				enableAlias.check();
			}
			public void removeUpdate(DocumentEvent e){
				enableAlias.check();
			}
			public void insertUpdate(DocumentEvent e){
				enableAlias.check();
			}
		});
		
		columnName.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				enableAlias.check();
			}
		});
		
		addAliasPB.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				try{
					String alName=aliasText.getText();
					Pattern p=Pattern.compile("[a-zA-Z0-9]");
					Matcher m=p.matcher(alName);
					Pattern p1=Pattern.compile("[^a-zA-Z0-9]");
					Matcher m1=p1.matcher(alName);
					
					if (alName != null && alName.length() > 0) {
						if((!alName.contains(" "))&&(m.find())&&(!m1.find())||(alName.contains("_"))) {
							Field colNameField=(Field)columnName.getSelectedItem();
							String colName=colNameField.name;
							if (fileMap.containsKey(fileName)) {
								aliasMap = fileMap.get(fileName);
								aliasMap.put(alName, colName);
								FieldFormula.aliasMap.clear();
								aliasTableModel.setRowCount(0);
								for(Map.Entry<String, String> entry1:	aliasMap.entrySet()){
									FieldFormula.aliasMap.put(entry1.getKey(), entry1.getValue());
									aliasTableModel.addRow(new Object[] {entry1.getKey(),entry1.getValue()});
								}
							}
							
	                    } else {
							JOptionPane.showMessageDialog(parent,"Alias name does not conform to specifications. Cannot add alias name","Error adding name",JOptionPane.WARNING_MESSAGE);
							aliasText.setText("");
						}
					}
					aliasText.setText("");
					columnName.setSelectedItem(null);
				}
				catch(Exception exc){
					exc.printStackTrace();
					JOptionPane.showMessageDialog(parent,exc.getMessage(),"Error adding alias name",JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		//1787 end
		
		final JDialog dlg = new JDialog(parent, "Edit Columns...", true);
		dlg.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		Util.addEscapeAction(dlg);
		
		final boolean[] ok = {false};
		okPB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				validate();
				if (okPB.isEnabled()) {
					ok[0] = true;
					dlg.setVisible(false);
				}
			}
		});
		
		JButton cancelPB = new JButton("Cancel");
		cancelPB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dlg.setVisible(false);
			}
		});
		
		Container content = dlg.getContentPane();
		content.setLayout(new GridBagLayout());
		int p = 4;
		Insets in = new Insets(p,p,p,p);
		
		// top section, for adding new fields
		content.add(new JLabel("Create Field"), new GridBagConstraints(0,0,1,1,0,0,GridBagConstraints.NORTHWEST,GridBagConstraints.NONE,in,p,p));
		content.add(new JLabel("Name"), new GridBagConstraints(1,0,1,1,0,0,GridBagConstraints.NORTHWEST,GridBagConstraints.NONE,in,p,p));
		content.add(nameText, new GridBagConstraints(2,0,1,1,1,0,GridBagConstraints.NORTHWEST,GridBagConstraints.HORIZONTAL,in,p,p));
		content.add(new JLabel("Type"), new GridBagConstraints(1,1,1,1,0,0,GridBagConstraints.NORTHWEST,GridBagConstraints.NONE,in,p,p));
		content.add(typeCombo, new GridBagConstraints(2,1,1,1,1,0,GridBagConstraints.NORTHWEST,GridBagConstraints.HORIZONTAL,in,p,p));
		
		//for bug 1787
		content.add(new JLabel("Set alias name"),new GridBagConstraints(0,3,1,1,1,0,GridBagConstraints.NORTHWEST,GridBagConstraints.HORIZONTAL,in,p,p));
		content.add(delAliasPB,new GridBagConstraints(3,10,1,1,0,0,GridBagConstraints.SOUTH,GridBagConstraints.HORIZONTAL,in,p,p));
		content.add(new JLabel("Enter alias name"),new GridBagConstraints(1,3,1,1,1,0,GridBagConstraints.NORTHWEST,GridBagConstraints.HORIZONTAL,in,p,p));
		content.add(aliasText,new GridBagConstraints(2,3,1,1,1,0,GridBagConstraints.NORTHWEST,GridBagConstraints.HORIZONTAL,in,p,p));
		content.add(new JLabel("Choose column name"),new GridBagConstraints(1,4,1,1,1,0,GridBagConstraints.NORTHWEST,GridBagConstraints.HORIZONTAL,in,p,p));
		content.add(columnName,new GridBagConstraints(2,4,1,1,1,0,GridBagConstraints.NORTHWEST,GridBagConstraints.HORIZONTAL,in,p,p));
		content.add(addAliasPB,new GridBagConstraints(3,3,1,1,1,0,GridBagConstraints.NORTHWEST,GridBagConstraints.HORIZONTAL,in,p,p));
		
		content.add(new JSeparator(JSeparator.HORIZONTAL),new GridBagConstraints(0,5,4,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,in,p,p));
		//1787 end
		
		content.add(addPB, new GridBagConstraints(3,0,1,2,0,0,GridBagConstraints.NORTHEAST,GridBagConstraints.HORIZONTAL,in,p,p));
		content.add(new JSeparator(JSeparator.HORIZONTAL), new GridBagConstraints(0,2,4,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,in,p,p));
		
		// left and right section, for showing and removing fields
		content.add(new JScrollPane(fieldList), new GridBagConstraints(0,6,1,8,1,1,GridBagConstraints.NORTHWEST,GridBagConstraints.BOTH,in,p,p));
		content.add(delPB, new GridBagConstraints(3,6,1,3,0,0,GridBagConstraints.NORTHEAST,GridBagConstraints.HORIZONTAL,in,p,p));
		content.add(new JLabel("Alias names mapping"),new GridBagConstraints(3,7,1,1,1,0,GridBagConstraints.NORTHWEST,GridBagConstraints.HORIZONTAL,in,p,p));//1787
		content.add(new JScrollPane(aliasNames),new GridBagConstraints(3,8,1,2,1,1,GridBagConstraints.NORTHEAST,GridBagConstraints.BOTH,in,p,p));//1787
	
		// middle section, for showing the selected field
		content.add(new JLabel("Value"), new GridBagConstraints(1,6,1,1,0,0,GridBagConstraints.NORTHWEST,GridBagConstraints.NONE,in,p,p));
		content.add(valueCB, new GridBagConstraints(2,6,1,1,1,0,GridBagConstraints.NORTHWEST,GridBagConstraints.HORIZONTAL,in,p,p));
		content.add(calcEditorPane, new GridBagConstraints(1,7,2,6,1,1,GridBagConstraints.NORTHWEST,GridBagConstraints.BOTH,in,p,p));
		content.add(replaceCB, new GridBagConstraints(1,13,2,1,1,0,GridBagConstraints.NORTHWEST,GridBagConstraints.HORIZONTAL,in,p,p));
		// exit buttons
		content.add(new JSeparator(JSeparator.HORIZONTAL),new GridBagConstraints(3,11,4,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,in,p,p));
		content.add(okPB, new GridBagConstraints(3,12,1,1,0,0,GridBagConstraints.SOUTH,GridBagConstraints.HORIZONTAL,in,p,p));
		content.add(cancelPB, new GridBagConstraints(3,13,1,1,0,0,GridBagConstraints.SOUTH,GridBagConstraints.HORIZONTAL,in,p,p));
		// errors
		content.add(errorField, new GridBagConstraints(0, 14, 4, 1, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, in,p,p));
		
		dlg.pack();
		dlg.setSize(700,680);
		dlg.setLocationRelativeTo(Main.mainFrame);
		dlg.setVisible(true);
		
		if (ok[0]) {
			Set<Field> f2 = new LinkedHashSet<Field>(getModelFields());
			
			// add and remove fields to match new model
			Set<Field> toRemove = new LinkedHashSet<Field>(oldFields);
			toRemove.removeAll(f2);
			
			Set<Field> toAdd = new LinkedHashSet<Field>(f2);
			toAdd.removeAll(oldFields);
			
			// TODO: move merging of results back into ShapeLayer
			
			if (toAdd.size() > 0 || toRemove.size() > 0 || toUpdate.size() > 0 ||
					!shapeLayer.calcFieldMap.get(fc).getCalculatedFields().equals(calculatedFields)) {
				shapeLayer.getHistory().mark();
				
				// remove deleted fields from the schema
				for (Field f: toRemove) {
					fc.removeField(f);
				}
				
				// add newly-added fields
				for (Field f: toAdd) {
					fc.addField(f);
				}
				
				// update calculated fields
				if (!calculatedFields.equals(shapeLayer.calcFieldMap.get(fc).getCalculatedFields())) {
					shapeLayer.calcFieldMap.get(fc).setCalculatedFields(calculatedFields);
				}
				
				// apply user defaults for all fields
				Map<Field,CalculatedField> columns = new LinkedHashMap<Field,CalculatedField>();
				for (Field field: toUpdate) {
					columns.put(field, calculatedFields.get(field));
				}
				if (columns.size() > 0) {
					shapeLayer.calcFieldMap.get(fc).updateValues((List<Feature>)fc.getFeatures(), columns);
				}
			}
		}
	}
	
	/** @return a new Set<Field> based on the contents of the model. */
	public List<Field> getModelFields() {
		List<Field> list = new ArrayList<Field>();
		for (int i = 0; i < model.getSize(); i++) {
			list.add(((FieldWrapper)model.getElementAt(i)).f);
		}
		return list;
	}
		
	private static class FieldWrapper {
		public final Field f;
		public FieldWrapper(Field f) {
			this.f = f;
		}
		public String toString() {
			return s(f);
		}
	}
	
	private static class BasicField extends CalculatedField {
		public BasicField(String name, Class<?> type) {
			super(name, type);
		}
		public Set<Field> getFields() {
			return Collections.emptySet();
		}
		public Object getValue(ShapeLayer layer, Feature f) {
			return f.getAttribute(this);
		}
		public static class Factory extends FieldFactory<Field> {
			public Factory(String name, Class<?> dataType) {
				super(name, Field.class, dataType);
			}
			public JPanel createEditor(ColumnEditor editor, Field f) {
				return null;
			}
			public Field createField(Set<Field> fields) {
				return new Field(getName(), getDataType(), true);
			}
		}
	}
}
