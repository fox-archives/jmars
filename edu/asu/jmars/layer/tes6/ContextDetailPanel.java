package edu.asu.jmars.layer.tes6;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.EventListener;
import java.util.EventObject;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.border.BevelBorder;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.EventListenerList;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import edu.asu.jmars.layer.tes6.FieldsPrompt.FieldPushEvent;
import edu.asu.jmars.swing.FancyColorMapper;


class ContextDetailPanel extends JPanel {
    /**
	 * stop eclipse from complaining
	 */
	private static final long serialVersionUID = 1L;
	
	private JCheckBox drawRealCheckBox; // Always draw real data
	private JCheckBox drawNullCheckBox; // Draw null data as outlines
	public ContextDetailPanel(TesLView tesLView){
        //this.tesLView = tesLView;
        initComponents();

        titleChanged = false;
        descChanged = false;
        fieldsChanged = false;
        selectsChanged = false;
        orderBysChanged = false;
        colorByChanged = false;
        drawRealChanged = false;
        drawNullChanged = false;

        dbUtils = DbUtils.getInstance();
    }

	public void viewCleanup(){
		if (fieldsPrompt != null)
			fieldsPrompt.setVisible(false);
	}
	
	public void fillFrom(TesContext ctx){
		setTitle(ctx.getTitle()); titleChanged = true;
		setDesc(ctx.getDesc()); descChanged = true;
		setFields(TesContext.copyFields(ctx.getFields())); fieldsChanged = true;
		setSelects(TesContext.copySelects(ctx.getSelects())); selectsChanged = true;
		setOrderBys(TesContext.copyOrderBys(ctx.getOrderCriteria())); orderBysChanged = true;
		setColorBy(ctx.getColorBy().clone()); colorByChanged = true;
		cmap.setState(ctx.getColorMapperState());
		setDrawReal(ctx.getDrawReal()); drawRealChanged = true;
		setDrawNull(ctx.getDrawNull()); drawNullChanged = true;
	}
	
    public String getTitle(){ return nameTextField.getText(); }
    public String getDesc(){ return descTextArea.getText(); }
    public Vector<FieldDesc> getFields(){ return fieldsTableModel.getFields(); }
    public Vector<RangeDesc> getSelects(){ return selectsTableModel.getSelects(); }
    public Vector<OrderCriterion> getOrderBys(){ return orderByTableModel.getOrderBys(); }
    public ColorBy getColorBy(){
        FieldDesc field = (FieldDesc)setColorFieldComboBox.getSelectedItem();
        Serializable minVal = null, maxVal = null;
        if (field != null){
            try { minVal = field.valueOf(colorMinTextField.getText()); }
            catch(NumberFormatException ex){
                if (colorMinTextField.getText().trim().length() == 0){
                    minVal = new Double(0);
                }
                else {
                    throw new SourcedRuntimeException(colorMinTextField, ex);
                }
            }
            
            try { maxVal = field.valueOf(colorMaxTextField.getText()); }
            catch(NumberFormatException ex){
                if (colorMaxTextField.getText().trim().length() == 0){
                    maxVal = new Double(0);
                }
                else {
                    throw new SourcedRuntimeException(colorMaxTextField, ex);
                }
            }
        }
        return new ColorBy(field, minVal, maxVal);
    }
    public Boolean getDrawReal(){
    	return new Boolean(drawRealCheckBox.isSelected());
    }
    public Boolean getDrawNull(){
    	return new Boolean(drawNullCheckBox.isSelected());
    }
    
    public boolean getTitleChanged(){ return titleChanged; }
    public boolean getDescChanged(){ return descChanged; }
    public boolean getFieldsChanged(){ return fieldsChanged; }
    public boolean getSelectsChanged(){ return selectsChanged; }
    public boolean getOrderBysChanged(){ return orderBysChanged; }
    public boolean getColorByChanged(){ return colorByChanged; }
    public boolean getDrawRealChanged(){ return drawRealChanged; }
    public boolean getDrawNullChanged(){ return drawNullChanged; }

    // return overall changed
    public boolean getChanged(){
        return (titleChanged
                || descChanged
                || fieldsChanged
                || selectsChanged
                || orderBysChanged
                || colorByChanged
				|| drawRealChanged
				|| drawNullChanged);
    }


    public void   resetChangeFlags(){
        titleChanged = false; descChanged = false;
        fieldsChanged = false; selectsChanged = false;
        orderBysChanged = false; colorByChanged = false;
        drawRealChanged = false; drawNullChanged = false;
    }

    public void   setChangeFlags(boolean value){
        titleChanged = value; descChanged = value;
        fieldsChanged = value; selectsChanged = value;
        orderBysChanged = value; colorByChanged = value;
        drawRealChanged = value; drawNullChanged = value;
    }
    
    public void   setTitle(String title){
    	nameTextField.setText(title);
    	nameTextField.setCaretPosition(0);
    }
    public void   setDesc(String desc){
    	descTextArea.setText(desc);
    	descTextArea.setCaretPosition(0);
    }
    public void   setFields(Vector<FieldDesc> fields){
        fieldsTableModel.setFields(fields);
        
        // Add the fields to the list of order-by and color-by fields
        // list-boxes.
        for(Iterator<FieldDesc> i = fields.iterator(); i.hasNext(); ){
            FieldDesc f = (FieldDesc)i.next();
            //addOrderByFieldComboBox.addItem(f);
            if (!f.isArrayField())
            	setColorFieldComboBox.addItem(f);
        }
        
    }
    public void   setSelects(Vector<RangeDesc> selects){ selectsTableModel.setSelects(selects); }
    public void   setOrderBys(Vector<OrderCriterion> orderBys){ orderByTableModel.setOrderBys(orderBys); }
    public void   setColorField(FieldDesc colorField){ setColorFieldComboBox.setSelectedItem(colorField); }
    public void   setColorBy(ColorBy colorBy){
        if (colorBy.field != null){ setColorFieldComboBox.setSelectedItem(colorBy.field); }
        if (colorBy.minVal != null){ colorMinTextField.setText(colorBy.minVal.toString()); }
        if (colorBy.maxVal != null){ colorMaxTextField.setText(colorBy.maxVal.toString()); }
    }
    public void   setDrawReal(Boolean drawReal){
    	drawRealCheckBox.setSelected(drawReal.booleanValue());
    }
    public void   setDrawNull(Boolean drawNull){
    	drawNullCheckBox.setSelected(drawNull.booleanValue());
    }

    public void   setInitialFocus(){
        nameTextField.requestFocus();
    }

    public void fireMyActionEvent(ActionEvent e){
        Object[] listeners = myListenersList.getListenerList();

        for(int i = 0; i < listeners.length; i+=2){
            if (listeners[i] == ActionListener.class){
                ((ActionListener)listeners[i+1]).actionPerformed(e);
            }
        }
    }

    public void addActionListener(ActionListener l){
        myListenersList.add(ActionListener.class, l);
    }

    public void removeActionListener(ActionListener l){
        myListenersList.remove(ActionListener.class, l);
    }


    private void initComponents() {
        nameLabel = new javax.swing.JLabel("Title:");
        nameTextField = new javax.swing.JTextField(40);
        descLabel = new javax.swing.JLabel("Desc:");
        descTextArea = new javax.swing.JTextArea(3,40);
        descScrollPane = new javax.swing.JScrollPane();
        drawRealCheckBox = new JCheckBox("Real data");
        nameFieldsSeparator = new javax.swing.JSeparator();
        fieldsLabel = new javax.swing.JLabel("Fields:");
        fieldsScrollPane = new javax.swing.JScrollPane();
        fieldsTableModel = new FieldsTableModel();
        fieldsTable = new javax.swing.JTable();
        addFieldTextField = new javax.swing.JTextField();
        delFieldButton = new javax.swing.JButton("Delete");
        addFieldButton = new javax.swing.JButton("Add");
        selectsLabel = new javax.swing.JLabel("Selects:");
        selectsScrollPane = new javax.swing.JScrollPane();
        selectsTableModel = new SelectsTableModel();
        selectsTable = new javax.swing.JTable();
        addSelectFieldTextField = new javax.swing.JTextField();
        addSelectMinTextField = new javax.swing.JTextField();
        addSelectMaxTextField = new javax.swing.JTextField();
        addSelectButton = new javax.swing.JButton("Add");
        delSelectButton = new javax.swing.JButton("Delete");
        orderByLabel = new javax.swing.JLabel("Order:");
        orderByScrollPane = new javax.swing.JScrollPane();
        orderByTable = new javax.swing.JTable();
        orderByTable.setDefaultRenderer(Orders.class, new OrderByTableCellRenderer());
        orderByTable.setDefaultEditor(Orders.class, new DefaultCellEditor(new JComboBox(Orders.values())));
        orderByTableModel = new OrderByTableModel();
        //addOrderByFieldComboBox = new javax.swing.JComboBox();
        addOrderByFieldTextField = new javax.swing.JTextField();
        addOrderByOrderComboBox = new javax.swing.JComboBox();
        addOrderByButton = new javax.swing.JButton("Add");
        delOrderByButton = new javax.swing.JButton("Delete");
        moveUpButton = new javax.swing.JButton("Move Up");
        moveDnButton = new javax.swing.JButton("Move Down");

        colorFieldLabel = new javax.swing.JLabel("Color:");
        setColorFieldComboBox = new javax.swing.JComboBox();
        colorMinTextField = new javax.swing.JTextField();
        colorMaxTextField = new javax.swing.JTextField();
        drawNullCheckBox = new JCheckBox("Draw null");
        //defaultColorBoxButton = new javax.swing.JButton();

        // TODO: get the appropriate buffer from LView here
		// cmap = tesLView.createFancyColorMapper();
        cmap = new FancyColorMapper();

        // Fields prompt is initialized on demand.
        fieldsPrompt = null;
        fieldsPromptButtonFields = new JButton("?");
        fieldsPromptButtonSelects = new JButton("?");
        fieldsPromptButtonOrderBys = new JButton("?");

        
        fieldsPromptButtonFields.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e){
        		fieldsPromptButtonActionPerformed(e);
        	}
        });
        fieldsPromptButtonSelects.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e){
        		fieldsPromptButtonActionPerformed(e);
        	}
        });
        fieldsPromptButtonOrderBys.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e){
        		fieldsPromptButtonActionPerformed(e);
        	}
        });
        

        fieldsTableModel.addTableModelListener(new TableModelListener(){
                public void tableChanged(TableModelEvent evt){
                    fieldsTableModelTableChanged(evt);
                }
            });

        fieldsTableModel.addFieldAddDelEventListener(new FieldAddDelEventListener(){
                public void fieldAdded(FieldAddDelEvent evt){
                    fieldsTableModelFieldAddedEvent(evt);
                }
                public void fieldDeleted(FieldAddDelEvent evt){
                    fieldsTableModelFieldDeletedEvent(evt);
                }
            });

        orderByTableModel.addTableModelListener(new TableModelListener(){
                public void tableChanged(TableModelEvent evt){
                    orderByTableModelTableChanged(evt);
                }
            });

        nameTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                // nameTextFieldActionPerformed(evt);
            }
        });
        nameTextField.addKeyListener(new KeyListener(){
                public void keyPressed(KeyEvent evt){ }
                public void keyReleased(KeyEvent evt){ }
                public void keyTyped(KeyEvent evt){
                    nameTextFieldKeyTypedEvent(evt);
                }
            });
        nameTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                nameTextFieldFocusGainedEvent(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                nameTextFieldFocusLostEvent(evt);
            }
        });
        nameTextField.getDocument().addDocumentListener(new DocumentListener(){
                public void changedUpdate(DocumentEvent e){
                    nameTextFieldDocumentChangeEvent(e);
                }
                public void insertUpdate(DocumentEvent e){
                    nameTextFieldDocumentChangeEvent(e);
                }
                public void removeUpdate(DocumentEvent e){
                    nameTextFieldDocumentChangeEvent(e);
                }
            });
        
        descTextArea.setLineWrap(true);
        descTextArea.setWrapStyleWord(true);
        descTextArea.addKeyListener(new KeyListener(){
                public void keyPressed(KeyEvent evt){ }
                public void keyReleased(KeyEvent evt){ }
                public void keyTyped(KeyEvent evt){
                    descTextAreaKeyTypedEvent(evt);
                }
            });
        descTextArea.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                descTextAreaFocusGainedEvent(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                descTextAreaFocusLostEvent(evt);
            }
        });
        descTextArea.getDocument().addDocumentListener(new DocumentListener(){
                public void changedUpdate(DocumentEvent e){
                    descTextAreaDocumentChangeEvent(e);
                }
                public void insertUpdate(DocumentEvent e){
                    descTextAreaDocumentChangeEvent(e);
                }
                public void removeUpdate(DocumentEvent e){
                    descTextAreaDocumentChangeEvent(e);
                }
            });
        

        drawRealCheckBox.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e){
        		drawRealCheckBoxActionEvent(e);
        	}
        });
        
        addSelectFieldTextField.addFocusListener(new FocusAdapter(){
            public void focusGained(java.awt.event.FocusEvent evt) {
                addSelectFieldTextFieldFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                addSelectFieldTextFieldFocusLost(evt);
            }
        });

        addFieldTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                addFieldTextFieldFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                addFieldTextFieldFocusLost(evt);
            }
        });

        addFieldButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addFieldButtonActionPerformed(evt);
            }
        });

        delFieldButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt){
                delFieldButtonActionPerformed(evt);
            }
        });

        addSelectMinTextField.addFocusListener(new FocusAdapter(){
            public void focusGained(java.awt.event.FocusEvent evt) {
                addSelectFieldTextFieldFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                addSelectFieldTextFieldFocusLost(evt);
            }
        });

        addSelectMaxTextField.addFocusListener(new FocusAdapter(){
            public void focusGained(java.awt.event.FocusEvent evt) {
                addSelectFieldTextFieldFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                addSelectFieldTextFieldFocusLost(evt);
            }
        });

        addSelectButton.addActionListener(new ActionListener(){
            public void actionPerformed(java.awt.event.ActionEvent evt){
                addSelectButtonActionPerformed(evt);
            }
        });

        delSelectButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent evt){
                delSelectButtonActionPerformed(evt);
            }
        });

        addOrderByFieldTextField.addFocusListener(new FocusAdapter(){
            public void focusGained(java.awt.event.FocusEvent evt) {
                addOrderByFieldTextFieldFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                addOrderByFieldTextFieldFocusLost(evt);
            }
        });
        
        addOrderByButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent evt){
                addOrderByButtonActionPerformed(evt);
            }
        });
        
        delOrderByButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent evt){
                delOrderByButtonActionPerformed(evt);
            }
        });

        moveUpButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent evt){
                moveUpButtonActionPerformed(evt);
            }
        });

        moveDnButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent evt){
                moveDnButtonActionPerformed(evt);
            }
        });

        setColorFieldComboBox.addItem(null); // add no-color-field option
        setColorFieldComboBox.setRenderer(new OrderFieldCellRenderer());
        setColorFieldComboBox.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent evt){
                    setColorFieldComboBoxActionPerformed(evt);
                }
            });
        setColorFieldComboBox.addItemListener(new ItemListener(){
                public void itemStateChanged(ItemEvent evt){
                    setColorFieldComboBoxItemEvent(evt);
                }
            });

        colorMinTextField.addKeyListener(new KeyListener(){
            public void keyPressed(KeyEvent evt){ }
            public void keyReleased(KeyEvent evt){ }
            public void keyTyped(KeyEvent evt){
                colorMinTextFieldKeyTypedEvent(evt);
            }
        });
        
        colorMinTextField.getDocument().addDocumentListener(new DocumentListener(){
            public void changedUpdate(DocumentEvent e){
                colorMinTextFieldDocumentChangeEvent(e);
            }
            public void insertUpdate(DocumentEvent e){
                colorMinTextFieldDocumentChangeEvent(e);
            }
            public void removeUpdate(DocumentEvent e){
                colorMinTextFieldDocumentChangeEvent(e);
            }
        });
        
        colorMaxTextField.addKeyListener(new KeyListener(){
            public void keyPressed(KeyEvent evt){ }
            public void keyReleased(KeyEvent evt){ }
            public void keyTyped(KeyEvent evt){
                colorMaxTextFieldKeyTypedEvent(evt);
            }
        });
        colorMaxTextField.getDocument().addDocumentListener(new DocumentListener(){
            public void changedUpdate(DocumentEvent e){
                colorMaxTextFieldDocumentChangeEvent(e);
            }
            public void insertUpdate(DocumentEvent e){
                colorMaxTextFieldDocumentChangeEvent(e);
            }
            public void removeUpdate(DocumentEvent e){
                colorMaxTextFieldDocumentChangeEvent(e);
            }
        });

        drawNullCheckBox.setSelected(true);
        drawNullCheckBox.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e){
        		drawNullCheckBoxActionEvent(e);
        	}
        });

        /*
		cmap.addChangeListener(
				new ChangeListener()
				 {
					public void stateChanged(ChangeEvent e)
					 {
						if(!cmap.isAdjusting())
							tesLView.setColorMapOp(cmap.getColorMapOp());
					 }
				 }
		);
		// Apply the current color map to the layer view window
		tesLView.setColorMapOp(cmap.getColorMapOp());
		*/

		setLayout(new java.awt.GridBagLayout());
		
		/*
		// put horizontal grid spacer in
		for(int i = 0; i < 25; i++){
			//Box.Filler spacer = new Box.Filler(new Dimension(18,1), new Dimension(18,1), new Dimension(50,1));
			JLabel spacer = new JLabel(""); spacer.setMinimumSize(new Dimension(18,1));
			gbc = new GridBagConstraints();
			gbc.gridx = i; gbc.gridy = -1;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			add(spacer, gbc);
		}
		*/

        final int gcw = 15; // grid cell width in pixels
        final int th = 20; // text field height
        final Dimension td = new Dimension(150,th*5); // table dimension pixels
        final Dimension tad = new Dimension(150,th*3);

        GridBagConstraints gbc;
        int row = 0;
        
        // title
        gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = row;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.WEST;
        add(nameLabel, gbc);

        nameTextField.setMinimumSize(new Dimension(15*gcw,th));
        gbc = new GridBagConstraints();
        gbc.gridx = 3; gbc.gridy = row;
        gbc.gridwidth = 15; gbc.gridheight = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(nameTextField, gbc);
        
        gbc = new GridBagConstraints();
        gbc.gridx = 18; gbc.gridy = row;
        gbc.gridwidth = 6;
        add(drawRealCheckBox, gbc);

        row ++;
        // separator
        gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = row;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.insets = new java.awt.Insets(3, 0, 3, 0);
        add(nameFieldsSeparator, gbc);
        
        row ++;
        // description
        gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = row;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.WEST;
        add(descLabel, gbc);

        descScrollPane.setMinimumSize(tad);
        descScrollPane.setPreferredSize(tad);
        descScrollPane.setViewportView(descTextArea);
        
        gbc = new GridBagConstraints();
        gbc.gridx = 3; gbc.gridy = row;
        gbc.gridwidth = 15; gbc.gridheight = 3;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(descScrollPane, gbc);
        
        row += gbc.gridheight;
        // separator
        gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = row;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.insets = new java.awt.Insets(3, 0, 3, 0);
        add(nameFieldsSeparator, gbc);
        
        row++;
        // fields
        gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = row;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.WEST;
        add(fieldsLabel, gbc);

        fieldsScrollPane.setMinimumSize(td);
        fieldsScrollPane.setPreferredSize(td);
        fieldsTable.setModel(fieldsTableModel);
        fieldsScrollPane.setViewportView(fieldsTable);

        row++;
        gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = row;
        gbc.gridwidth = 18; gbc.gridheight = 6;
        gbc.weightx = 1.0; gbc.weighty = 2.0;
        gbc.fill = GridBagConstraints.BOTH;
        add(fieldsScrollPane, gbc);

        row += gbc.gridheight;
        gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = row;
        gbc.gridwidth = 1;
        fieldsPromptButtonFields.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        add(fieldsPromptButtonFields, gbc);
        
        addFieldTextField.setMinimumSize(new Dimension(17*gcw,th));
        gbc = new GridBagConstraints();
        gbc.gridx = 1; gbc.gridy = row;
        gbc.gridwidth = 17; gbc.gridheight = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(addFieldTextField, gbc);

        gbc = new GridBagConstraints();
        gbc.gridx = 18; gbc.gridy = row-1;
        gbc.gridwidth = 6; gbc.gridheight = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.SOUTH;
        add(delFieldButton, gbc);

        gbc = new GridBagConstraints();
        gbc.gridx = 18; gbc.gridy = row;
        gbc.gridwidth = 6; gbc.gridheight = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(addFieldButton, gbc);

        row += 2;
        // Selects
        gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = row;
        gbc.gridwidth = 4;
        gbc.anchor = GridBagConstraints.WEST;
        add(selectsLabel, gbc);

        selectsScrollPane.setMinimumSize(td);
        selectsScrollPane.setPreferredSize(td);
        selectsTable.setModel(selectsTableModel);
        selectsScrollPane.setViewportView(selectsTable);

        row++;
        gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = row;
        gbc.gridwidth = 18; gbc.gridheight = 6;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0; gbc.weighty = 2.0;
        add(selectsScrollPane, gbc);

        row += gbc.gridheight;
        gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = row;
        gbc.gridwidth = 1;
        //gbc.anchor = GridBagConstraints.WEST;
        fieldsPromptButtonSelects.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        add(fieldsPromptButtonSelects, gbc);
        
        addSelectFieldTextField.setMinimumSize(new Dimension(9*gcw,th));
        gbc = new GridBagConstraints();
        gbc.gridx = 1; gbc.gridy = row;
        gbc.gridwidth = 9;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(addSelectFieldTextField, gbc);

        addSelectMinTextField.setMinimumSize(new Dimension(4*gcw,th));
        gbc = new GridBagConstraints();
        gbc.gridx = 10; gbc.gridy = row;
        gbc.gridwidth = 4;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(addSelectMinTextField, gbc);

        addSelectMaxTextField.setMinimumSize(new Dimension(4*gcw,th));
        gbc = new GridBagConstraints();
        gbc.gridx = 14; gbc.gridy = row;
        gbc.gridwidth = 4;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(addSelectMaxTextField, gbc);

        gbc = new GridBagConstraints();
        gbc.gridx = 18; gbc.gridy = row;
        gbc.gridwidth = 6;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(addSelectButton, gbc);

        gbc = new GridBagConstraints();
        gbc.gridx = 18; gbc.gridy = row-1;
        gbc.gridwidth = 6;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.SOUTH;
        add(delSelectButton, gbc);

        row += 2;
        // Order
        gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = row;
        gbc.gridwidth = 4;
        gbc.anchor = GridBagConstraints.WEST;
        add(orderByLabel, gbc);

        orderByScrollPane.setMinimumSize(td);
        orderByScrollPane.setPreferredSize(td);
        orderByTable.setModel(orderByTableModel);
        orderByScrollPane.setViewportView(orderByTable);

        row++;
        gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = row;
        gbc.gridwidth = 18; gbc.gridheight = 6;
        gbc.weightx = 1.0; gbc.weighty = 2.0;
        gbc.fill = GridBagConstraints.BOTH;
        add(orderByScrollPane, gbc);

        row += gbc.gridheight;
        gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        fieldsPromptButtonOrderBys.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        add(fieldsPromptButtonOrderBys, gbc);

        addOrderByFieldTextField.setMinimumSize(new Dimension(9*gcw,th));
        //addOrderByFieldComboBox.setRenderer(new OrderFieldCellRenderer());
        gbc = new GridBagConstraints();
        gbc.gridx = 1; gbc.gridy = row;
        gbc.gridwidth = 9;
        gbc.weightx = 2.0;        
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(addOrderByFieldTextField, gbc);

        addOrderByOrderComboBox.setModel(new javax.swing.DefaultComboBoxModel(Orders.values()));
        gbc = new GridBagConstraints();
        gbc.gridx = 10; gbc.gridy = row;
        gbc.gridwidth = 8;
        gbc.weightx = 1.0;        
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(addOrderByOrderComboBox, gbc);

        gbc = new GridBagConstraints();
        gbc.gridx = 18; gbc.gridy = row;
        gbc.gridwidth = 6;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(addOrderByButton, gbc);

        gbc = new GridBagConstraints();
        gbc.gridx = 18; gbc.gridy = row-1;
        gbc.gridwidth = 6;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.SOUTH;
        add(delOrderByButton, gbc);

        gbc = new GridBagConstraints();
        gbc.gridx = 18; gbc.gridy = row-3;
        gbc.gridwidth = 6;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(moveUpButton, gbc);

        gbc = new GridBagConstraints();
        gbc.gridx = 18; gbc.gridy = row-2;
        gbc.gridwidth = 6;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTH;
        add(moveDnButton, gbc);

        row += 2;
        gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = row;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.WEST;
        add(colorFieldLabel, gbc);

        row++;
        gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = row;
        gbc.gridwidth = 18; gbc.gridheight = 3;
        gbc.weighty = 0.001;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(cmap, gbc);
        
        row += gbc.gridheight;
        gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = row;
        gbc.gridwidth = 10;
        gbc.weightx = 2.0;        
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(setColorFieldComboBox, gbc);

        colorMinTextField.setMinimumSize(new Dimension(4*gcw,th));
        colorMinTextField.setEnabled(false);
        gbc = new GridBagConstraints();
        gbc.gridx = 10; gbc.gridy = row;
        gbc.gridwidth = 4; gbc.gridheight = 1;
        gbc.weightx = 1.0;        
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(colorMinTextField, gbc);

        colorMaxTextField.setMinimumSize(new Dimension(4*gcw,th));
        colorMaxTextField.setEnabled(false);
        gbc = new GridBagConstraints();
        gbc.gridx = 14; gbc.gridy = row;
        gbc.gridwidth = 4; gbc.gridheight = 1;
        gbc.weightx = 1.0;        
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(colorMaxTextField, gbc);
        
        gbc = new GridBagConstraints();
        gbc.gridx = 18; gbc.gridy = row;
        gbc.gridwidth = 6; gbc.gridheight = 1;
        add(drawNullCheckBox, gbc);

        // TODO: Move this functionality to the place where
        // focus panels are populated.
        final JComponent c = this;
        addAncestorListener(new AncestorListener(){
			public void ancestorAdded(AncestorEvent event) { resize(); }
			public void ancestorMoved(AncestorEvent event) {}
			public void ancestorRemoved(AncestorEvent event) {}
        	
			private void resize(){
				edu.asu.jmars.util.Util.resizeTopLevelContainerToPreferredSize(c);
				removeAncestorListener(this);
			}
        });
    }

    private void setColorFieldComboBoxItemEvent(ItemEvent e){
        FieldDesc fieldDesc = (FieldDesc)setColorFieldComboBox.getSelectedItem();

        //colorMinTextField.setText((fieldDesc == null)? "": fieldDesc.getMinVal());
        //colorMaxTextField.setText((fieldDesc == null)? "": fieldDesc.getMaxVal());
        colorMinTextField.setText("");
        colorMaxTextField.setText("");

        colorMinTextField.setEnabled(fieldDesc != null);
        colorMaxTextField.setEnabled(fieldDesc != null);

        colorByChanged = true;

        // notify listeners that something got changed in the panel
        fireChangeEvent();
    }
    private void setColorFieldComboBoxActionPerformed(java.awt.event.ActionEvent evt){
        //System.err.println("setColorFieldComboBoxActionPerformed: "+ evt);
    }


    private void fieldsPromptButtonActionPerformed(ActionEvent e){
		if (fieldsPrompt == null){
			fieldsPrompt = new FieldsPrompt(null);
			fieldsPrompt.addFieldPushListener(new FieldsPrompt.FieldPushListener(){
				public void fieldPushed(FieldPushEvent evt) {
					FieldDesc fieldDesc = evt.getFieldDesc();
					
					if (evt.getContext() == FieldPushEvent.PUSH_TO_FIELDS){
						addFieldTextField.setText(fieldDesc.getFieldName());
					}
					else if (evt.getContext() == FieldPushEvent.PUSH_TO_SELECTS){
						addSelectFieldTextField.setText(fieldDesc.getFieldName());
					}
					else if (evt.getContext() == FieldPushEvent.PUSH_TO_ORDERBYS){
						addOrderByFieldTextField.setText(fieldDesc.getFieldName());
					}
				}
			});
		}
		//JButton b = (JButton)e.getSource();
		fieldsPrompt.setLocationRelativeTo(this);
		fieldsPrompt.setVisible(true);
    }
    
    private void nameTextFieldDocumentChangeEvent(DocumentEvent evt){
        // notify listeners that something got changed in the panel
        fireChangeEvent();
        titleChanged = true;
    }
    private void descTextAreaDocumentChangeEvent(DocumentEvent evt){
        // notify listeners that something got changed in the panel
        fireChangeEvent();
        descChanged = true;
    }
    private void colorMinTextFieldDocumentChangeEvent(DocumentEvent evt){
        // notify listeners that something got changed in the panel
        fireChangeEvent();
    }
    private void colorMaxTextFieldDocumentChangeEvent(DocumentEvent evt){
        // notify listeners that something got changed in the panel
        fireChangeEvent();
    }

    private void nameTextFieldKeyTypedEvent(KeyEvent evt){
        // fireFieldKeyTypeEvent(evt);
        //titleChanged = true;
    }
    
    private void nameTextFieldFocusGainedEvent(FocusEvent evt){
        nameTextField.selectAll();
    }

    private void nameTextFieldFocusLostEvent(FocusEvent evt){
        nameTextField.setSelectionStart(nameTextField.getSelectionEnd());
    }

    private void descTextAreaKeyTypedEvent(KeyEvent evt){
        // fireFieldKeyTypeEvent(evt);
        //titleChanged = true;
    }
    
    private void descTextAreaFocusGainedEvent(FocusEvent evt){
        descTextArea.selectAll();
    }

    private void descTextAreaFocusLostEvent(FocusEvent evt){
        descTextArea.setSelectionStart(descTextArea.getSelectionEnd());
    }

    private void colorMinTextFieldKeyTypedEvent(KeyEvent evt){
        colorByChanged = true;
    }

    private void colorMaxTextFieldKeyTypedEvent(KeyEvent evt){
        colorByChanged = true;
    }

	private void drawRealCheckBoxActionEvent(ActionEvent e) {
		drawRealChanged = true;
		fireChangeEvent();
	}
	
	private void drawNullCheckBoxActionEvent(ActionEvent e){
		drawNullChanged = true;
		fireChangeEvent();
	}
	
    private void addFieldTextFieldFocusLost(java.awt.event.FocusEvent evt) {
    	if (getRootPane() != null)
    		getRootPane().setDefaultButton(null);
    }

    private void addFieldTextFieldFocusGained(java.awt.event.FocusEvent evt) {
    	if (getRootPane() != null)
    		getRootPane().setDefaultButton(addFieldButton);
    }

    private void addSelectFieldTextFieldFocusLost(java.awt.event.FocusEvent evt) {
    	if (getRootPane() != null)
    		getRootPane().setDefaultButton(null);
    }

    private void addSelectFieldTextFieldFocusGained(java.awt.event.FocusEvent evt) {
    	if (getRootPane() != null)
    		getRootPane().setDefaultButton(addSelectButton);
    }

    private void addFieldButtonActionPerformed(java.awt.event.ActionEvent evt) {
        String fieldName = addFieldTextField.getText().trim();

        // trivial case, return on empty string
        if (fieldName.length() == 0){ return; }

        // regular case, verify field and values
        if (dbUtils.validSelectClauseField(fieldName)){
        	try {
        		FieldDesc f = dbUtils.getFieldDescFromDb(fieldName);
        		SortedSet<FieldDesc> sortedFieldNames = new TreeSet<FieldDesc>(new FieldDesc.FieldComparatorByName());
        		sortedFieldNames.addAll(fieldsTableModel.getFields());
        		if (!sortedFieldNames.contains(f)){
        			fieldsTableModel.addField(f);
        			fieldsChanged = true;
        		}
        		addFieldTextField.setText("");
        		addFieldTextField.requestFocus();

        		// notify listeners that something got changed in the panel
        		fireChangeEvent();
        	}
        	catch(SQLException ex){
                addFieldTextField.selectAll();
                addFieldTextField.requestFocus();
        	}
        }
        else {
            addFieldTextField.selectAll();
            addFieldTextField.requestFocus();
        }
    }

    private void delFieldButtonActionPerformed(java.awt.event.ActionEvent evt) {
        int[] selectedRows = fieldsTable.getSelectedRows();
        fieldsTableModel.delRows(selectedRows);
        fieldsChanged = true;
    }

    private void addSelectButtonActionPerformed(ActionEvent evt){
        String fieldName = addSelectFieldTextField.getText().trim();
        String minValStr = addSelectMinTextField.getText().trim();
        String maxValStr = addSelectMaxTextField.getText().trim();
        Serializable minVal = null, maxVal = null;
        
        // trivial case, return on empty string
        if (fieldName.length() == 0){ return; }

        // regular case, verify field and values
        if (dbUtils.validWhereClauseField(fieldName)){
        	try {
        		FieldDesc f = dbUtils.getFieldDescFromDb(fieldName);
        		minVal = null; maxVal = null;
        		try { minVal = f.valueOf(minValStr); }
        		catch(IllegalArgumentException ex){
        			addSelectMinTextField.selectAll();
        			addSelectMinTextField.requestFocus();
        		}
        		try { maxVal = f.valueOf(maxValStr); }
        		catch(IllegalArgumentException ex){
        			addSelectMaxTextField.selectAll();
        			addSelectMaxTextField.requestFocus();
        		}

        		if (minVal != null && maxVal != null){
        			selectsTableModel.addSelect(new RangeDesc(f, minVal, maxVal));
        			addSelectFieldTextField.requestFocus();
        			addSelectFieldTextField.setText("");
        			addSelectMinTextField.setText("");
        			addSelectMaxTextField.setText("");
        			selectsChanged = true;

        			// notify listeners that something got changed in the panel
        			fireChangeEvent();
        		}
        	}
        	catch(SQLException ex){
                addSelectFieldTextField.selectAll();
                addSelectFieldTextField.requestFocus();
        	}
        }
        else {
            addSelectFieldTextField.selectAll();
            addSelectFieldTextField.requestFocus();
        }
    }

    private void delSelectButtonActionPerformed(ActionEvent evt){
        int[] selectedRows = selectsTable.getSelectedRows();
        selectsTableModel.delRows(selectedRows);
        selectsChanged = true;

        // notify listeners that something got changed in the panel
        fireChangeEvent();
    }

    private void addOrderByFieldTextFieldFocusGained(java.awt.event.FocusEvent evt){
    	if (getRootPane() != null)
    		getRootPane().setDefaultButton(addOrderByButton);
    }
    private void addOrderByFieldTextFieldFocusLost(java.awt.event.FocusEvent evt){
    	if (getRootPane() != null)
    		getRootPane().setDefaultButton(null);
    }

    private void addOrderByButtonActionPerformed(ActionEvent evt){
        String fieldName = addOrderByFieldTextField.getText().trim();

        // trivial case, return on empty string
        if (fieldName.length() == 0){ return; }

        // regular case, verify field and values
        if (dbUtils.validOrderByClauseField(fieldName)){
        	try {
        		FieldDesc f = dbUtils.getFieldDescFromDb(fieldName);
        		Orders dirObj = (Orders)addOrderByOrderComboBox.getSelectedItem();
        		boolean dir = dirObj == Orders.ASCENDING;
        		orderByTableModel.addOrderCriterion(new OrderCriterion(f, dir));

        		addOrderByFieldTextField.setText("");
        		addOrderByFieldTextField.requestFocus();
        		addOrderByOrderComboBox.setSelectedIndex(0);
        		orderBysChanged = true;

        		// notify listeners that something got changed in the panel
        		fireChangeEvent();
        	}
        	catch(SQLException ex){
                addOrderByFieldTextField.selectAll();
                addOrderByFieldTextField.requestFocus();
        	}
        }
        else {
            addOrderByFieldTextField.selectAll();
            addOrderByFieldTextField.requestFocus();
        }

        /*
        if (addOrderByFieldComboBox.getSelectedItem() == null){ return; }

        FieldDesc f = (FieldDesc)addOrderByFieldComboBox.getSelectedItem();
        if (f != null){
            String dirStr = (String)addOrderByOrderComboBox.getSelectedItem();
            boolean dir = orders[0].equals(dirStr);
            orderByTableModel.addOrderCriterion(new OrderCriterion(f, dir));
            addOrderByFieldComboBox.requestFocus();
            addOrderByOrderComboBox.setSelectedIndex(0);
            orderBysChanged = true;

            // notify listeners that something got changed in the panel
            fireChangeEvent();
        }
        else {
            addOrderByFieldComboBox.requestFocus();
        }
        */
    }

    private void delOrderByButtonActionPerformed(ActionEvent evt){
        int[] selectedRows = orderByTable.getSelectedRows();
        orderByTableModel.delRows(selectedRows);
        orderBysChanged = true;

        // notify listeners that something got changed in the panel
        fireChangeEvent();
    }

    private void moveUpButtonActionPerformed(ActionEvent evt){
        int[] selectedRows = orderByTable.getSelectedRows();
        boolean moveOccurred = orderByTableModel.moveRowsUp(selectedRows);
        if (moveOccurred){
            orderBysChanged = true;

            orderByTable.clearSelection();
            for(int i = 0; i < selectedRows.length; i++){
                orderByTable.addRowSelectionInterval(selectedRows[i]-1, selectedRows[i]-1);
            }

            // notify listeners that something got changed in the panel
            fireChangeEvent();
        }
    }

    private void moveDnButtonActionPerformed(ActionEvent evt){
        int[] selectedRows = orderByTable.getSelectedRows();
        boolean moveOccurred = orderByTableModel.moveRowsDown(selectedRows);
        if (moveOccurred){
            orderBysChanged = true;

            orderByTable.clearSelection();
            for(int i = 0; i < selectedRows.length; i++){
                orderByTable.addRowSelectionInterval(selectedRows[i]+1, selectedRows[i]+1);
            }

            // notify listeners that something got changed in the panel
            fireChangeEvent();
        }
    }


    private void fieldsTableModelFieldAddedEvent(FieldAddDelEvent evt){
        //addOrderByFieldComboBox.addItem(evt.getField());
    	if (!evt.getField().isArrayField())
    		setColorFieldComboBox.addItem(evt.getField());
    }

    private void fieldsTableModelFieldDeletedEvent(FieldAddDelEvent evt){
        //addOrderByFieldComboBox.removeItem(evt.getField());
        //orderByTableModel.removeOrderField(evt.getField());
        setColorFieldComboBox.removeItem(evt.getField());
        setColorFieldComboBox.setSelectedIndex(0);
    }

    private void fieldsTableModelTableChanged(TableModelEvent evt){}

    private void orderByTableModelTableChanged(TableModelEvent evt){}

    // notify listeners that some data has changed
    private void fireChangeEvent(){
        Object[] listeners = myListenersList.getListenerList();
        ChangeEvent evt = null;

        for(int i = 0; i < listeners.length; i+=2){
            if (listeners[i] == ChangeListener.class){
                if (evt == null){ evt = new ChangeEvent(this); }
                ((ChangeListener)listeners[i+1]).stateChanged(evt);
            }
        }
    }

    public void addChangeListener(ChangeListener l){
        myListenersList.add(ChangeListener.class, l);
    }

    public void removeChangeListener(ChangeListener l){
        myListenersList.remove(ChangeListener.class, l);
    }


    /*
    public FieldDesc findFieldByName(String fieldName){
        for(int i = 0; i < fields.size(); i++){
            FieldDesc f = (FieldDesc)fields.get(i);
            if (f.getFieldName().equals(fieldName)){
                return f;
            }
        }
        return null;
    }
    */

    public FancyColorMapper getColorMapper(){
        return cmap;
    }

    //private TesLView tesLView;
    //private Vector<FieldDesc> fields;
    private EventListenerList myListenersList = new EventListenerList();


    // Title
    private JLabel nameLabel;
    private JTextField nameTextField;
    private JSeparator nameFieldsSeparator;
    
    // Description
    private JLabel descLabel;
    private JTextArea descTextArea;
    private JScrollPane descScrollPane;

    // FieldsTable(fieldName, tableName, fieldDesc)
    private JLabel fieldsLabel;
    private JScrollPane fieldsScrollPane;
    private JTable fieldsTable;
    private FieldsTableModel fieldsTableModel;
    private JTextField addFieldTextField;
    private JButton addFieldButton;
    private JButton delFieldButton;
    
    // SelectsTable(fieldName, minValue,  maxValue)
    private JLabel selectsLabel;
    private JScrollPane selectsScrollPane;
    private JTable selectsTable;
    private SelectsTableModel selectsTableModel;
    private JTextField addSelectFieldTextField;
    private JTextField addSelectMaxTextField;
    private JTextField addSelectMinTextField;
    private JButton addSelectButton;
    private JButton delSelectButton;

    // OrderByTable(fieldName, sortDirection)
    private JLabel orderByLabel;
    private JScrollPane orderByScrollPane;
    private JTable orderByTable;
    private OrderByTableModel orderByTableModel;
    // private JComboBox addOrderByFieldComboBox;
    private JTextField addOrderByFieldTextField;
    private JComboBox addOrderByOrderComboBox;
    private JButton addOrderByButton;
    private JButton delOrderByButton;
    private JButton moveDnButton;
    private JButton moveUpButton;

    // ColorField
    private JLabel colorFieldLabel;
    private JComboBox setColorFieldComboBox;
    private JTextField colorMinTextField;
    private JTextField colorMaxTextField;
    // private JButton defaultColorBoxButton;
    private FancyColorMapper cmap;
    
    // Fields Prompt
    FieldsPrompt fieldsPrompt;
    JButton fieldsPromptButtonFields;
    JButton fieldsPromptButtonSelects;
    JButton fieldsPromptButtonOrderBys;

    // Orderings
    public enum Orders {
    	ASCENDING(){
    		public String toString(){
    			return super.toString().toLowerCase();
    		}
    	},
    	DESCENDING {
    		public String toString(){
    			return super.toString().toLowerCase();
    		}
    	}
    };

    // keep track of what changed
    private boolean titleChanged;
    private boolean descChanged;
    private boolean fieldsChanged;
    private boolean selectsChanged;
    private boolean orderBysChanged;
    private boolean colorByChanged;
    private boolean drawRealChanged;
    private boolean drawNullChanged;

    // handle to various db-utilities
    private DbUtils dbUtils;


    // End of variables declaration


    class FieldsTableModel extends AbstractTableModel {
        public int getRowCount(){ return fields.size(); }
        public int getColumnCount(){ return 1; }
        public Class<?> getColumnClass(int col){ return String.class; }
        public String getColumnName(int col){ return "Field Name"; }
        public boolean isCellEditable(int row, int col){ return false; }

        public Object getValueAt(int row, int col){
            FieldDesc f = (FieldDesc)fields.get(row);
            return f.getFieldName();
        }
        public void addField(FieldDesc f){
            fields.add(f);
            fireTableRowsInserted(fields.size()-1, fields.size()-1);
            fireFieldAddedEvent(f);
        }

        public void delRows(int[] indices){
            for(int i = 0; i < indices.length; i++){
                FieldDesc f = (FieldDesc)fields.get(indices[i]);
                fields.removeElementAt(indices[i]);
                fireTableRowsDeleted(indices[i],indices[i]);
                fireFieldDeletedEvent(f);

                for(int j = i+1; j < indices.length; j++){
                    if (indices[j] > indices[i]){
                        indices[j]--;
                    }
                }
            }
        }
        
        public FieldDesc getFieldAtRow(int row){
            return (FieldDesc)fields.get(row);
        }



        public void fireFieldAddedEvent(FieldDesc f){
            FieldAddDelEvent evt = new FieldAddDelEvent(this, f);
            Object[] listeners = listenersList.getListenerList();

            for(int i = 0; i < listeners.length; i+=2){
                if (listeners[i] == FieldAddDelEventListener.class){
                    FieldAddDelEventListener l = (FieldAddDelEventListener)listeners[i+1];
                    l.fieldAdded(evt);
                }
            }
        }
        
        public void fireFieldDeletedEvent(FieldDesc f){
            FieldAddDelEvent evt = new FieldAddDelEvent(this, f);
            Object[] listeners = listenersList.getListenerList();

            for(int i = 0; i < listeners.length; i+=2){
                if (listeners[i] == FieldAddDelEventListener.class){
                    FieldAddDelEventListener l = (FieldAddDelEventListener)listeners[i+1];
                    l.fieldDeleted(evt);
                }
            }
        }

        public void addFieldAddDelEventListener(FieldAddDelEventListener l){
            listenersList.add(FieldAddDelEventListener.class, l);
        }

        public void removeFieldAddDelEventListener(FieldAddDelEventListener l){
            listenersList.remove(FieldAddDelEventListener.class, l);
        }

        
        public Vector<FieldDesc> getFields(){ return fields; }
        public void setFields(Vector<FieldDesc> newFields){
            int oldSize = fields.size();
            
            fields.clear();
            fireTableRowsDeleted(0, oldSize);

            fields.addAll(newFields);
            fireTableRowsInserted(0, fields.size());
        }

        private Vector<FieldDesc> fields = new Vector<FieldDesc>();
        private EventListenerList listenersList = new EventListenerList();

    }

    class FieldAddDelEvent extends EventObject {
        public FieldAddDelEvent(Object source, FieldDesc f){
            super(source); this.f = f;
        }
        public FieldDesc getField(){ return f; }
        private FieldDesc f;
    }
    
    interface FieldAddDelEventListener extends EventListener {
        public void fieldAdded(FieldAddDelEvent evt);
        public void fieldDeleted(FieldAddDelEvent evt);
    }

    class SelectsTableModel extends AbstractTableModel {
		public int getRowCount(){ return selects.size(); }
        public int getColumnCount(){ return 3; }
        
        public boolean isCellEditable(int row, int col){
        	return (col > 0);
        }

        public Object getValueAt(int row, int col){
            RangeDesc s = (RangeDesc)selects.get(row);
            FieldDesc f = s.getField();
            switch(col){
            case 0: return f.getFieldName();
            case 1: return s.getMinValue();
            case 2: return s.getMaxValue();
            }
            return null;
        }

		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			//super.setValueAt(aValue, rowIndex, columnIndex);
			
			RangeDesc s = (RangeDesc)selects.get(rowIndex);
			if (s == null)
				return;
			
			Serializable val = null;
			try {
				val = s.getField().valueOf((String)aValue);
			}
			catch(Exception ex){
			}

			if (val != null){
				switch(columnIndex){
				case 1: s.setMinValue(val); break;
				case 2: s.setMaxValue(val); break;
				default: throw new RuntimeException("Unhandled case "+columnIndex);
				}

				selectsChanged = true;
				// notify listeners that something got changed in the panel
				fireChangeEvent();
			}

		}

        public Class<?> getColumnClass(int col){
            switch(col){
            case 0: return String.class;
            case 1: return Object.class;
            case 2: return Object.class;
            }
            return null;
        }

        public String getColumnName(int col){
            switch(col){
            case 0: return "Field Name";
            case 1: return "Min Val";
            case 2: return "Max Val";
            }
            return null;
        }

        public void addSelect(RangeDesc s){
            selects.add(s);
            fireTableRowsInserted(selects.size()-1, selects.size()-1);
        }

        public void delRows(int[] indices){
            for(int i = 0; i < indices.length; i++){
                selects.removeElementAt(indices[i]);
                fireTableRowsDeleted(indices[i],indices[i]);

                for(int j = i+1; j < indices.length; j++){
                    if (indices[j] > indices[i]){
                        indices[j]--;
                    }
                }
            }
        }

        public Vector<RangeDesc> getSelects(){ return selects; }
        public void setSelects(Vector<RangeDesc> newSelects){
            int oldSize = selects.size();

            selects.clear();
            fireTableRowsDeleted(0, oldSize);

            selects.addAll(newSelects);
            fireTableRowsInserted(0, newSelects.size());
        /*
            int[] delIndices = new int[newSelects.size()];
            for(int i = 0; i < delIndices.length; i++){ delIndices[i] = i; }
            delRows(delIndices);

            for(int i = 0; i < newSelects.size(); i++){
                addSelect((RangeDesc)newSelects.get(i));
            }
        */
        }

        private Vector<RangeDesc> selects = new Vector<RangeDesc>();
    }

    static class OrderByTableCellRenderer extends DefaultTableCellRenderer {
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {
			
			if (value instanceof Orders)
				value = value.toString();
			
			return super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
					row, column);
		}
    }
    
    class OrderByTableModel extends AbstractTableModel {
        public int getRowCount(){ return orderings.size(); }
        public int getColumnCount(){ return 2; }
        
        public boolean isCellEditable(int row, int col){
        	return (col > 0);
        }

        public Object getValueAt(int row, int col){
            OrderCriterion c = (OrderCriterion)orderings.get(row);
            switch(col){
            case 0: return c.getField().getFieldName();
            case 1: return c.getDirection()? Orders.ASCENDING: Orders.DESCENDING;//(c.getDirection()?orders[0]:orders[1]);
            }
            return null;
        }

		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			super.setValueAt(aValue, rowIndex, columnIndex);
			
			OrderCriterion c = (OrderCriterion)orderings.get(rowIndex);
			if (c == null)
				return;
			
			Boolean val = null;
			try {
				Orders dirObj = (Orders)aValue;
				switch(dirObj){
				case ASCENDING: val = Boolean.TRUE; break;
				case DESCENDING: val = Boolean.FALSE; break;
				default:
					throw new IllegalArgumentException("Unhandled order value "+dirObj);
				}
			}
			catch(Exception ex){
			}
			
			if (val != null) {
				c.setDirection(val.booleanValue());
				orderBysChanged = true;
				fireChangeEvent();
			}
		}
        
		public Class<?> getColumnClass(int col){
            switch(col){
            case 0: return String.class;
            case 1: return Orders.class;
            }
            return null;
        }

        public String getColumnName(int col){
            switch(col){
            case 0: return "Field Name";
            case 1: return "Direction";
            }
            return null;
        }

        public void addOrderCriterion(OrderCriterion c){
            orderings.add(c);
            fireTableRowsInserted(orderings.size()-1, orderings.size()-1);
        }

        public void delRows(int[] indices){
            for(int i = 0; i < indices.length; i++){
                orderings.removeElementAt(indices[i]);
                fireTableRowsDeleted(indices[i],indices[i]);

                for(int j = i+1; j < indices.length; j++){
                    if (indices[j] > indices[i]){ indices[j]--; }
                }
            }
        }

        public boolean moveRowsDown(int[] indices){
            if (indices.length <= 0){
                return false;
            }

            Arrays.sort(indices);

            // If the highest index is at the last row, then ignore this move
            if (indices[indices.length-1] >= (orderings.size()-1)){
                return false;
            }

            for(int i = indices.length-1; i >= 0; i--){
                OrderCriterion elt = orderings.get(indices[i]);
                orderings.removeElementAt(indices[i]);
                orderings.insertElementAt(elt, indices[i]+1);
            }
            
            // let the view know that it should repaint these rows
            if (indices.length > 0){
                fireTableRowsUpdated(indices[0], indices[indices.length-1]+1);
            }

            return true;
        }

        public boolean moveRowsUp(int[] indices){
            if (indices.length <= 0){
                return false;
            }

            Arrays.sort(indices);

            // If the lowest index is at the first row, then ignore this move
            if (indices[0] <= 0){
                return false;
            }

            for(int i = 0; i < indices.length; i++){
                OrderCriterion elt = orderings.get(indices[i]);
                orderings.removeElementAt(indices[i]);
                orderings.insertElementAt(elt, indices[i]-1);
            }

            // let the view know that it should repaint these rows
            if (indices.length > 0){
                fireTableRowsUpdated(indices[0]-1, indices[indices.length-1]);
            }

            return true;
        }

        public void removeOrderField(FieldDesc f){
            for(int i = 0; i < orderings.size(); i++){
                if (((OrderCriterion)orderings.get(i)).getField() == f){
                    delRows(new int[]{ i });
                    orderBysChanged = true;
                    return;
                }
            }
        }

        public Vector<OrderCriterion> getOrderBys(){ return orderings; }
        public void setOrderBys(Vector<OrderCriterion> newOrderings){
            int oldSize = orderings.size();

            orderings.clear();
            fireTableRowsDeleted(0, oldSize);
            
            orderings.addAll(newOrderings);
            fireTableRowsInserted(0, orderings.size());

            /*
            int[] delIndices = new int[newOrderings.size()];
            for(int i = 0; i < delIndices.length; i++){ delIndices[i] = i; }
            delRows(delIndices);

            for(int i = 0; i < selects.size(); i++){
                addOrderingCriterion((OrderCriterion)newOrderings.get(i));
            }
            */
        }
        
        private Vector<OrderCriterion> orderings = new Vector<OrderCriterion>();
    }
    
    class OrderFieldCellRenderer extends JLabel implements ListCellRenderer {
        public Component getListCellRendererComponent(
            JList list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus)
        {
            setOpaque(true);

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
            	String fieldName = ((FieldDesc)value).getFieldName();
            	int maxLength = 30;
            	if (fieldName.length() > maxLength){
            		setText(fieldName.substring(0,maxLength-1)+"...");
            	}
            	else {
            		setText(fieldName);
            	}
            	setToolTipText(fieldName);
            }

            return this;
        }
    }

    

}


