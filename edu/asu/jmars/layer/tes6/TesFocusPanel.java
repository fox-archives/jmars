package edu.asu.jmars.layer.tes6;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.metal.MetalComboBoxIcon;

import com.thoughtworks.xstream.XStream;

import edu.asu.jmars.layer.FocusPanel;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.swing.ColorInterp;
import edu.asu.jmars.swing.ColorMapper;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.Util;

public class TesFocusPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	public TesFocusPanel(Layer.LView associatedLView){
        setLayout(new BorderLayout());

        ctxsPane = new ContextsPane((TesLView)associatedLView);
        add(ctxsPane, BorderLayout.CENTER);

    }
    
    public void viewCleanup(){
    	ctxsPane.viewCleanup();
    }

    private ContextsPane ctxsPane;
    
    public ColorMapper getColorMap() {
    	return ctxsPane.colorMapperListener.cmap;
    }
}

class ContextsPane extends JPanel implements ContextEventListener {
	private static final long serialVersionUID = 1L;
	
	private final String BUILTIN_CTX_PREFIX = "tes.ctx.builtin";
	private final String CONTRIB_CTX_PREFIX = "tes.ctx.contrib";
	private final String USER_CTX_PREFIX = "tes.ctx.user";
	
	private Map<String,TesContext> presetTemplates = new LinkedHashMap<String,TesContext>();
	private Map<String,TesContext> contribTemplates = new LinkedHashMap<String,TesContext>();
	private Map<String,TesContext> userTemplates = new LinkedHashMap<String,TesContext>();
	
	private JFileChooser chooser;
	
    public ContextsPane(TesLView tesLView){
        this.tesLView = tesLView;
        tesLayer = (TesLayer)tesLView.getLayer();
        tesLayer.addContextEventListener(this);
        
        // Every TesContext[i] has a ContextDetailPanel[i]
        ctxs = new Vector<TesContext>();
        ctxPanels = new Vector<ContextDetailPanel>();
        
        // Newly added unsubmitted contexts are registered with the following set.
        // For such contexts, a submit causes a submit and activate.
        newContexts = new HashSet<TesContext>();

        // All ContextDetailPanels have a FancyColorMappers in them.
        // Which one of them gets to manipulate the screen is decided by colorMapperListener.
        colorMapperListener = new MyColorChangeListener(tesLView);
        
        initComponents();
        
        presetTemplates = loadPresetTemplates();
        contribTemplates = loadContribTemplates();
        userTemplates = loadUserTemplates();
        
        chooser = new JFileChooser();
        chooser.addChoosableFileFilter(new CtxFileFilter());
        
        // Install the current configuration from the Layer
        restorePreconfiguredContexts();
    }
    
    private class CtxFileFilter extends FileFilter {
		public boolean accept(File f) {
			return f.isDirectory() || f.getName().endsWith(".ctx");
		}

		public String getDescription() {
			return "TES Context files";
		}
    }
    
    private void restorePreconfiguredContexts(){
        for(TesContext ctx: tesLayer.getContexts()){
        	contextAddedImpl(ctx);
        	ctxPanels.get(ctxPanels.size()-1).resetChangeFlags();
        }
    	
        if (tesLView.getContext() != null)
        	contextActivatedImpl(tesLView.getContext());
    }
    
    public void viewCleanup(){
    	for(ContextDetailPanel cdp: ctxPanels){
    		cdp.viewCleanup();
    	}
    }
    
    private void initComponents(){
        setLayout(new BorderLayout());

        contextsTabbedPane = new JTabbedPane();
        contextsTabbedPane.setTabPlacement(JTabbedPane.BOTTOM);
        contextsTabbedPane.addMouseListener(new MouseAdapter(){
                public void mouseClicked(MouseEvent e){
                    contextsTabbedPaneMouseClickedEvent(e);
                }
            });

        
        Box buttonPanel1 = Box.createHorizontalBox();

        addButton = new JButton("Add");
        addButton.setToolTipText("Initialize a new context from either a blank or a perdefined template.");
        addButton.setIcon(new MetalComboBoxIcon());
        addButton.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent evt){
        		JPopupMenu addMenu = buildAddMenu();
            	addMenu.setInvoker(addButton);
        		addMenu.show(addButton, 0, addButton.getHeight());
        	}
        });
        buttonPanel1.add(addButton);
        
        
        delButton = new JButton("Delete");
        delButton.setToolTipText("Delete the current context (from current session).");
        delButton.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent evt){
                    delButtonActionPerformed(evt);
                }
            });
        buttonPanel1.add(delButton);

        dupButton = new JButton("Duplicate");
        dupButton.setToolTipText("Duplicate the current context as a new context.");
        dupButton.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent evt){
                    dupButtonActionPerformed(evt);
                }
            });
        buttonPanel1.add(dupButton);

        submitButton = new JButton("Submit");
        submitButton.setToolTipText("Submit changes to the current context. Changed context have a \"*\" next to their name in the tab.");
        submitButton.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent evt){
                    submitButtonActionPerformed(evt);
                }
            });
        buttonPanel1.add(submitButton);
        
        Box buttonPanel2 = Box.createHorizontalBox();
        
        saveAsTmplButton = new JButton("Save As Template");
        saveAsTmplButton.setToolTipText("Save the currently active context as a template into the \"From User Defined Templates\" list.");
        saveAsTmplButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent evt){
                saveUserTemplateActionPerformed(evt);
            }
        });
        buttonPanel1.add(saveAsTmplButton);
        
        //TODO: Remove user defined template
        wipeTmplButton = new JButton("Wipe User Template");
        wipeTmplButton.setToolTipText("Remove a template from the \"From User Defined Templates\" list.");
        wipeTmplButton.setIcon(new MetalComboBoxIcon());
        wipeTmplButton.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent evt){
        		wipeUserTemplateActionPerformed(evt);
        	}
        });
        buttonPanel2.add(wipeTmplButton);
        
        exportTmplButton = new JButton("Export User Template");
        exportTmplButton.setToolTipText("Export a user defined template for off-line storage or to share with someone else.");
        exportTmplButton.setIcon(new MetalComboBoxIcon());
        exportTmplButton.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent evt){
        		JPopupMenu exportMenu = buildExportMenu();
            	exportMenu.setInvoker(exportTmplButton);
        		exportMenu.show(exportTmplButton, 0, exportTmplButton.getHeight());
        	}
        });
        buttonPanel2.add(exportTmplButton);
        
        importTmplButton = new JButton("Import Template");
        importTmplButton.setToolTipText("Import (into the \"From User Defined Templates\" list) a template which was previously exported.");
        importTmplButton.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent evt){
        		importTmplButtonActionPerformed(evt);
        	}
        });
        buttonPanel2.add(importTmplButton);

        Box buttonPanel = Box.createVerticalBox();
        buttonPanel.add(buttonPanel1);
        buttonPanel.add(buttonPanel2);
        
        add(buttonPanel, BorderLayout.SOUTH);
        add(contextsTabbedPane, BorderLayout.CENTER);
    }
    
    private JPopupMenu buildAddMenu(){
        
        JMenuItem blankMenuItem = new JMenuItem("Blank Template");
        blankMenuItem.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent evt){
                addBlankActionPerformed(evt);
            }
        });
        
        JMenu fromTemplSubMenu = buildTemplateSubMenu("From Predefined Templates", presetTemplates);
        JMenu fromContribSubMenu = buildTemplateSubMenu("From Contributed Templates", contribTemplates);
        JMenu fromUserTemplSubMenu = buildTemplateSubMenu("From User Defined Templates", userTemplates);
        
        JPopupMenu addMenu = new JPopupMenu("Add Menu");
        addMenu.add(fromTemplSubMenu);
        addMenu.add(fromContribSubMenu);
        addMenu.add(fromUserTemplSubMenu);
        addMenu.add(blankMenuItem);

    	return addMenu;
    }
    
    private JMenu buildTemplateSubMenu(String menuTitle, Map<String, TesContext> nameTemplateMap){
        JMenu subMenu = new JMenu(menuTitle);
        for(String tmplName: nameTemplateMap.keySet()){
        	TesContext srcCtx = nameTemplateMap.get(tmplName);
        	JMenuItem menuItem = new JMenuItem(new AddUsingTemplateAction(srcCtx));
        	menuItem.setText(srcCtx.getTitle().length() > 40? srcCtx.getTitle().substring(0, 40)+"...": srcCtx.getTitle());
        	if (srcCtx.getDesc() != null)
        		menuItem.setToolTipText("<html>"+Util.foldText(srcCtx.getDesc(), 80, "<br>")+"</html>");
        	subMenu.add(menuItem);
        }
        
        return subMenu;
    }
    
    private class AddUsingTemplateAction extends AbstractAction {
		private static final long serialVersionUID = 1L;
		private final TesContext tmplCtx;
    	
    	public AddUsingTemplateAction(TesContext tmplCtx){
    		super(tmplCtx.getTitle());
    		this.tmplCtx = tmplCtx;
    	}
		public void actionPerformed(ActionEvent e) {
			TesContext ctx = tmplCtx.clone();
    		tesLayer.addContext(ctx);
    		newContexts.add(ctx);
    		
    		ContextDetailPanel p = ctxPanels.get(ctxPanels.size()-1);
    		p.setChangeFlags(true);
    		refreshTabTitle(p);
		}
    }
    
    private JPopupMenu buildExportMenu(){
    	JPopupMenu exportMenu = new JPopupMenu("Export Menu");
    	for(String tmplName: userTemplates.keySet()){
    		TesContext srcCtx = userTemplates.get(tmplName);
    		JMenuItem menuItem = new JMenuItem(new ExportTemplateAction(srcCtx));
    		exportMenu.add(menuItem);
    	}
    	return exportMenu;
    }
    
    private class ExportTemplateAction extends AbstractAction {
		private static final long serialVersionUID = 1L;
		private final TesContext tmplCtx;
    	
    	public ExportTemplateAction(TesContext tmplCtx){
    		super(tmplCtx.getTitle());
    		this.tmplCtx = tmplCtx;
    	}
		public void actionPerformed(ActionEvent e) {
	    	JComponent parentComp = e.getSource() instanceof JComponent? (JComponent)e.getSource(): null;
	    	int result = chooser.showSaveDialog(parentComp);
	    	if (result != JFileChooser.APPROVE_OPTION)
	    		return;
	    	
	    	File file = chooser.getSelectedFile();
			try {
		    	XStream stream = getConfiguredXStream();
				PrintStream ps = new PrintStream(new BufferedOutputStream(new FileOutputStream(file)));
				ps.println(stream.toXML(tmplCtx));
				ps.close();
			} catch (IOException ex) {
				JOptionPane.showMessageDialog(parentComp, "Unable to export context to "+file.getName()+"!", 
						"Reason: "+ex.getMessage(), JOptionPane.ERROR_MESSAGE);
			}
		}
    }
    
    private XStream getConfiguredXStream(){
    	DbUtils dbu = DbUtils.getInstance();
    	
    	XStream stream = new XStream();
		stream.processAnnotations(TesContext.class);
		
		stream.omitField(TesContext.class, "whereClause");
		stream.omitField(TesContext.class, "orderByClause");
		stream.omitField(TesContext.class, "ctxImage");
		stream.omitField(TesContext.class, "whereClauseTime");
		stream.omitField(TesContext.class, "orderByClauseTime");
		stream.omitField(TesContext.class, "ctxImageTime");
	    stream.omitField(TesContext.class, "lastChangeTime");   
	    stream.omitField(TesContext.class, "titleChangeTime");
	    stream.omitField(TesContext.class, "descChangeTime");
	    stream.omitField(TesContext.class, "fieldsChangeTime");
	    stream.omitField(TesContext.class, "selectsChangeTime");
	    stream.omitField(TesContext.class, "orderBysChangeTime");
	    stream.omitField(TesContext.class, "colorChangeTime");
	    stream.omitField(TesContext.class, "drawRealChangeTime");
	    stream.omitField(TesContext.class, "drawNullChangeTime");
	    
		stream.alias("tes-context", TesContext.class);
		stream.alias("orderCriterion", OrderCriterion.class);
		stream.alias("rangeDesc", RangeDesc.class);
		stream.aliasType("field", FieldDesc.class);
		stream.aliasField("selectCriteria", TesContext.class, "userSelects");
		stream.aliasField("orderCriteria", TesContext.class, "orderParams");
		stream.aliasField("colorCriterion", TesContext.class, "colorBy");

		stream.registerConverter(new TesContext.FieldDescConverter(dbu));
		stream.registerLocalConverter(OrderCriterion.class, "direction", new TesContext.OrderingConverter());
		stream.registerConverter(new TesContext.ColorMapperStateConverter());
		
		return stream;
    }
    
    private Map<String,TesContext> loadPresetTemplates(){
    	return loadTemplatesFromConfig(BUILTIN_CTX_PREFIX);
    }
    
    private Map<String,TesContext> loadContribTemplates(){
    	return loadTemplatesFromConfig(CONTRIB_CTX_PREFIX);
    }
    	
    private Map<String,TesContext> loadUserTemplates() {
    	return loadTemplatesFromConfig(USER_CTX_PREFIX);
    }
    
    private Map<String,TesContext> loadTemplatesFromConfig(final String prefix){
    	DbUtils dbu = DbUtils.getInstance();
    	
    	Map<String,TesContext> userTemplates = new LinkedHashMap<String,TesContext>();
    	List<String> loadErrors = new ArrayList<String>();
    	
    	String[] ctxKeys = Config.getChildKeys(prefix);
    	for(int j=0; j<ctxKeys.length; j++){
    		String ctxTitle = Config.get(prefix+"."+ctxKeys[j]+".title");
    		String ctxDesc = Config.get(prefix+"."+ctxKeys[j]+".desc");
    		try {
    			String[] fieldNames = Config.getArray(prefix+"."+ctxKeys[j]+".fields");
    			Vector<FieldDesc> fields = new Vector<FieldDesc>(fieldNames.length);
    			for(int i=0; i<fieldNames.length; i++)
    				fields.add(dbu.getFieldDescFromDb(fieldNames[i]));

    			String[] selectNames = Config.getChildKeys(prefix+"."+ctxKeys[j]+".selects");
    			Vector<RangeDesc> selects = new Vector<RangeDesc>(selectNames.length);
    			for(int i=0; i<selectNames.length; i++){
    				String fieldName = Config.get(prefix+"."+ctxKeys[j]+".selects."+selectNames[i]+".field");
    				String minValString = Config.get(prefix+"."+ctxKeys[j]+".selects."+selectNames[i]+".min");
    				String maxValString = Config.get(prefix+"."+ctxKeys[j]+".selects."+selectNames[i]+".max");
    				FieldDesc field = dbu.getFieldDescFromDb(fieldName);
    				selects.add(new RangeDesc(field, minValString, maxValString));
    			}
    			
    			String[] orderNames = Config.getChildKeys(prefix+"."+ctxKeys[j]+".orders");
    			Vector<OrderCriterion> orders = new Vector<OrderCriterion>(orderNames.length);
    			for(int i=0; i<orderNames.length; i++){
    				String fieldName = Config.get(prefix+"."+ctxKeys[j]+".orders."+orderNames[i]+".field");
    				boolean dir = Config.get(prefix+"."+ctxKeys[j]+".orders."+orderNames[i]+".dir", true);
    				FieldDesc field = dbu.getFieldDescFromDb(fieldName);
    				orders.add(new OrderCriterion(field, dir));
    			}
    			
    			ColorBy colorBy = null;
    			String fieldName = Config.get(prefix+"."+ctxKeys[j]+".color.field");
    			String minValString = Config.get(prefix+"."+ctxKeys[j]+".color.min");
    			String maxValString = Config.get(prefix+"."+ctxKeys[j]+".color.max");

    			FieldDesc field = null;
    			for(FieldDesc f: fields){
    				if (f.getFieldName().equals(fieldName)){
    					field = f;
    					break;
    				}
    			}

    			if (field != null && minValString != null && maxValString != null){
    				colorBy = new ColorBy(field, minValString, maxValString);
    			}

    			String valuesStr = Config.get(prefix+"."+ctxKeys[j]+".color.values");
    			String colorsStr = Config.get(prefix+"."+ctxKeys[j]+".color.colors");
    			String interpKw = Config.get(prefix+"."+ctxKeys[j]+".color.interp_kw");
    			
    			ColorMapper.State colorMapperState = null;
    			ColorInterp colorInterp = ColorInterp.forKeyword(interpKw);
    			if (valuesStr != null && colorsStr != null && colorInterp != null){
        			int[] values = commaDelimStrToIntArray(valuesStr);
        			Color[] colors = commaDelimStrToColorArray(colorsStr);
        			colorMapperState = new ColorMapper.State(values, colors, colorInterp);
    			}
    			
    			boolean drawReal = Config.get(prefix+"."+ctxKeys[j]+".draw_real", false);
    			boolean drawNull = Config.get(prefix+"."+ctxKeys[j]+".draw_null", true);

    			TesContext ctx = new TesContext(ctxTitle);
    			ctx.setDesc(ctxDesc);
    			ctx.setFields(fields);
    			ctx.setSelects(selects);
    			ctx.setOrderCriteria(orders);
    			if (colorBy != null)
    				ctx.setColorBy(colorBy);
    			if (colorMapperState != null)
    				ctx.setColorMapperState(colorMapperState);
    			ctx.setDrawReal(drawReal);
    			ctx.setDrawNull(drawNull);
    			
    			userTemplates.put(ctxTitle, ctx);
    		}
    		catch(SQLException ex){
    			loadErrors.add("\""+ctxTitle+"\" due to "+ex);
    		}
    	}
    	
    	if (!loadErrors.isEmpty())
    		JOptionPane.showMessageDialog(this, loadErrors.toArray(new String[0]),
    				"Unable to load "+loadErrors.size()+"/"+
    				(loadErrors.size()+userTemplates.size())+
    				"user defined context templates.",
    				JOptionPane.WARNING_MESSAGE);
    	
    	return userTemplates;
    }

    private void contextsTabbedPaneMouseClickedEvent(MouseEvent evt){
        // On a left-mouse-button double-click make the context relative
        // to the active tab the active context.
        if (SwingUtilities.isLeftMouseButton(evt) && evt.getClickCount() == 2){

            int selectedTabIndex = contextsTabbedPane.getSelectedIndex();
			if (selectedTabIndex < 0)
				return;

            TesContext ctx = (TesContext)ctxs.get(selectedTabIndex);
            if (ctx == null)
            	return;
            
            if (ctx.isUnconstrained()){
            	JOptionPane.showMessageDialog(this,
            			"Cannot make "+ctx.getTitle()+" active since it is not constrained by selection criteria.",
            			"Unable to activate context!", JOptionPane.WARNING_MESSAGE);
            	return;
            }
            
            tesLayer.setActiveContext(ctx);
        }
    }
    
    private class TitleDescDialog extends JDialog {
    	private JTextField nameTextField;
    	private JTextArea descTextArea;
    	private JButton okButton, cancelButton;
    	private boolean cancelled;
    	
    	public TitleDescDialog(final JFrame owner, final String dialogTitle){
    		super(owner, dialogTitle, true);
    		this.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
    		nameTextField = new JTextField(60);
    		descTextArea = new JTextArea(3, 60);
    		descTextArea.setLineWrap(true);
    		descTextArea.setWrapStyleWord(true);
    		okButton = new JButton("Ok");
    		okButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
    				setVisible(false);
					cancelled = false;
				}
			});
    		cancelButton = new JButton("Cancel");
    		cancelButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
    				setVisible(false);
					cancelled = true;
				}
			});
    		cancelled = true;
    		
    		JPanel panel = new JPanel(new GridBagLayout());
    		
    		GridBagConstraints gbc = new GridBagConstraints();
    		gbc.anchor = GridBagConstraints.LINE_START; gbc.fill = GridBagConstraints.NONE;
    		gbc.weightx = 0; gbc.weighty = 0;
    		gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1; gbc.gridheight = 1;
    		panel.add(new JLabel("Title:"), gbc);
    		
    		gbc = new GridBagConstraints();
    		gbc.fill = GridBagConstraints.HORIZONTAL;
    		gbc.weightx = 1; gbc.weighty = 0;
    		gbc.gridx = 1; gbc.gridy = 0; gbc.gridwidth = 3; gbc.gridheight = 1;
    		panel.add(nameTextField, gbc);
    		
    		gbc = new GridBagConstraints();
    		gbc.anchor = GridBagConstraints.LINE_START; gbc.fill = GridBagConstraints.NONE;
    		gbc.weightx = 0; gbc.weighty = 0;
    		gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1; gbc.gridheight = 1;
    		panel.add(new JLabel("Desc:"), gbc);
    		
    		JScrollPane descScrollPane = new JScrollPane(descTextArea);
    		descScrollPane.setPreferredSize(new Dimension(200,100));
    		gbc = new GridBagConstraints();
    		gbc.fill = GridBagConstraints.BOTH;
    		gbc.weightx = 1; gbc.weighty = 1;
    		gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 3; gbc.gridheight = 3;
    		panel.add(descScrollPane, gbc);
    		
    		gbc = new GridBagConstraints();
    		gbc.anchor = GridBagConstraints.CENTER; gbc.fill = GridBagConstraints.NONE;
    		gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 1; gbc.gridheight = 1;
    		panel.add(okButton, gbc);
    		
    		gbc.fill = GridBagConstraints.NONE;
    		gbc.gridx = 1; gbc.gridy = 4; gbc.gridwidth = 1; gbc.gridheight = 1;
    		panel.add(cancelButton, gbc);
    		
    		setContentPane(panel);
    		pack();
    	}
    	
    	public String getCtxTitle(){
    		return nameTextField.getText();
    	}
    	
    	public String getCtxDesc(){
    		return descTextArea.getText();
    	}
    	
    	public void setCtxTitle(String name){
    		nameTextField.setText(name);
    		nameTextField.setCaretPosition(0);
    	}
    	
    	public void setCtxDesc(String desc){
    		descTextArea.setText(desc);
    		descTextArea.setCaretPosition(0);
    	}
    	
    	private boolean emptyCtxTitle(String title){
    		return title == null || title.trim().length() == 0;
    	}
    	
    	public boolean showAndGet(){
    		do{
    			setVisible(true);
    			if (emptyCtxTitle(getCtxTitle())){
    				JOptionPane.showMessageDialog(this, "Context title may not be null.");
    				nameTextField.requestFocusInWindow();
    				nameTextField.selectAll();
    			}
    		} while(!cancelled && emptyCtxTitle(getCtxTitle()));
    		
    		return !cancelled;
    	}
    }

    private void saveUserTemplateActionPerformed(ActionEvent evt){
        int selectedTabIndex = contextsTabbedPane.getSelectedIndex();
		if (selectedTabIndex < 0)
			return;
		
        ContextDetailPanel p = (ContextDetailPanel)ctxPanels.get(selectedTabIndex);
        if (p.getChanged()){
        	// Unsubmitted contexts don't have all their contents sent to the layer.
        	// Since we use the layer's copy of the context to do the save operation
        	// we end up not saving any changes made on the UI that are as yet
        	// uncommitted/unsubmitted.
        	p.getTitle();
        	JOptionPane.showMessageDialog(this, 
        			"Context \""+p.getTitle()+"\" must be submitted before it can be saved!",
        			"Cannot save context titled \""+p.getTitle()+"\".", JOptionPane.INFORMATION_MESSAGE);
        	return;
        }

        TesContext ctx = (TesContext)ctxs.get(selectedTabIndex);
        if (ctx == null)
        	return;

        TitleDescDialog titleDescDialog = new TitleDescDialog(
        		(JFrame)SwingUtilities.getAncestorOfClass(JFrame.class, this),
        		"Specify template name and description");
        titleDescDialog.setCtxTitle(p.getTitle());
        titleDescDialog.setCtxDesc(p.getDesc());
        if (!titleDescDialog.showAndGet())
        	return;
        String tmplName = titleDescDialog.getCtxTitle();
        String tmplDesc = titleDescDialog.getCtxDesc();
        
        addUserTmplMakePerm(ctx, tmplName, tmplDesc);
    }
    
    private String getConfigSafeTmplNameKey(String tmplName){
    	return tmplName.replaceAll("\\W", "_");
    }
    
    private void addUserTmplMakePerm(TesContext ctx, String tmplName, String tmplDesc){
    	if (tmplName == null)
    		tmplName = ctx.getTitle();
    	if (tmplDesc == null)
    		tmplDesc = ctx.getDesc();
    	
        Vector<FieldDesc> fields = ctx.getFields();
        Vector<RangeDesc> selects = ctx.getSelects();
        Vector<OrderCriterion> orderings = ctx.getOrderCriteria();
        ColorBy colorBy = ctx.getColorBy();
        ColorMapper.State cms = ctx.getColorMapperState();
        
        String keyPrefix = USER_CTX_PREFIX+"."+getConfigSafeTmplNameKey(tmplName);
        Config.setArray(keyPrefix, null); // clear old values
        
        Config.set(keyPrefix+".title", tmplName);
        Config.set(keyPrefix+".desc", tmplDesc);
        
        for(int i=0; i<fields.size(); i++)
        	Config.set(keyPrefix+".fields."+(i+1), fields.get(i).getFieldName());
        
        for(int i=0; i<selects.size(); i++){
        	RangeDesc rd = selects.get(i);
        	Config.set(keyPrefix+".selects."+(i+1)+".field", rd.getField().getFieldName());
        	Config.set(keyPrefix+".selects."+(i+1)+".min", rd.getMinValue().toString());
        	Config.set(keyPrefix+".selects."+(i+1)+".max", rd.getMaxValue().toString());
        }
        
        for(int i=0; i<orderings.size(); i++){
        	OrderCriterion oc = orderings.get(i);
        	Config.set(keyPrefix+".orders."+(i+1)+".field", oc.getField().getFieldName());
        	Config.set(keyPrefix+".orders."+(i+1)+".dir", oc.getDirection());
        }

        if (colorBy.field != null && colorBy.minVal != null && colorBy.maxVal != null){
        	Config.set(keyPrefix+".color.field", colorBy.field.getFieldName());
        	Config.set(keyPrefix+".color.min", colorBy.minVal.toString());
        	Config.set(keyPrefix+".color.max", colorBy.maxVal.toString());

        	Config.set(keyPrefix+".color.values", intArrayToCommaDelimStr(cms.getValues()));
        	Config.set(keyPrefix+".color.colors", colorArrayToCommaDelimStr(cms.getColors()));
        	Config.set(keyPrefix+".color.interp_kw", cms.getInterpolation().getKeyword());
        }
        
        Config.set(keyPrefix+".draw_real", ctx.getDrawReal().booleanValue());
        Config.set(keyPrefix+".draw_null", ctx.getDrawNull().booleanValue());
        
        TesContext copy = ctx.clone();
        copy.setTitle(tmplName);
        userTemplates.put(copy.getTitle(), copy);
    }
    
    private void removeUserTmplWipeConfig(TesContext tmplCtx){
        String keyPrefix = USER_CTX_PREFIX+"."+getConfigSafeTmplNameKey(tmplCtx.getTitle());
        
        // clear old config values
        String[] keys = Config.getAll(keyPrefix);
        for(int i=0; i<keys.length; i+=2)
        	Config.set(keyPrefix+"."+keys[i], null);
        
        userTemplates.remove(tmplCtx.getTitle());
    }

    private void importTmplButtonActionPerformed(ActionEvent evt){
    	JComponent parentComp = evt.getSource() instanceof JComponent? (JComponent)evt.getSource(): null;
    	int result = chooser.showOpenDialog(parentComp);
    	if (result != JFileChooser.APPROVE_OPTION)
    		return;
    	
    	File file = chooser.getSelectedFile();
    	try {
        	XStream stream = getConfiguredXStream();
    		InputStream is = new BufferedInputStream(new FileInputStream(file));
    		TesContext ctx = (TesContext)stream.fromXML(is);
    		is.close();
    		
    		String ctxTitle = ctx.getTitle();
    		while ("".equals(ctxTitle) || userTemplates.containsKey(ctxTitle)){
    			ctxTitle = JOptionPane.showInputDialog(null, "Duplicate title found, please choose a new title.", ctxTitle);
    			if (ctxTitle == null)
    				return;
    		}
    		addUserTmplMakePerm(ctx, ctxTitle, null);
    	}
    	catch(IOException ex){
			JOptionPane.showMessageDialog(this, "Unable to import context from "+file.getName()+"!", 
					"Reason: "+ex.getMessage(), JOptionPane.ERROR_MESSAGE);
    	}
    }
    
    private void wipeUserTemplateActionPerformed(ActionEvent evt){
    	JPopupMenu wipeMenu = buildWipeMenu();
    	wipeMenu.setInvoker(wipeTmplButton);
    	wipeMenu.show(wipeTmplButton, 0, wipeTmplButton.getHeight());
    }

    private JPopupMenu buildWipeMenu(){
    	JPopupMenu exportMenu = new JPopupMenu("Wipe Menu");
    	for(String tmplName: userTemplates.keySet()){
    		TesContext srcCtx = userTemplates.get(tmplName);
    		JMenuItem menuItem = new JMenuItem(new WipeTemplateAction(srcCtx));
    		exportMenu.add(menuItem);
    	}
    	return exportMenu;
    }
    
    private class WipeTemplateAction extends AbstractAction {
		private static final long serialVersionUID = 1L;
		private final TesContext tmplCtx;
    	
    	public WipeTemplateAction(TesContext tmplCtx){
    		super(tmplCtx.getTitle());
    		this.tmplCtx = tmplCtx;
    	}
		public void actionPerformed(ActionEvent e) {
	    	JComponent parentComp = wipeTmplButton; //e.getSource() instanceof JComponent? (JComponent)e.getSource(): null;
	    	
	    	int result = JOptionPane.showConfirmDialog(parentComp, "Wipe user defined template "+tmplCtx.getTitle()+".",
	    			"Wipe Template!", JOptionPane.YES_NO_OPTION);
	    	if (result != JFileChooser.APPROVE_OPTION)
	    		return;

	    	removeUserTmplWipeConfig(tmplCtx);
		}
    }
    
    private String intArrayToCommaDelimStr(int[] vals){
    	StringBuffer sbuf = new StringBuffer();
    	for(int i=0; i<vals.length; i++){
    		if (i>0)
    			sbuf.append(",");
    		sbuf.append(""+vals[i]);
    	}
    	return sbuf.toString();
    }
    
    private int[] commaDelimStrToIntArray(String str){
    	String[] pcs = str.split(",");
    	int[] out = new int[pcs.length];
    	for(int i=0; i<pcs.length; i++)
    		out[i] = Integer.parseInt(pcs[i]);
    	return out;
    }
    
    private String colorArrayToCommaDelimStr(Color[] vals){
    	StringBuffer sbuf = new StringBuffer();
    	for(int i=0; i<vals.length; i++){
    		if (i>0)
    			sbuf.append(",");
    		sbuf.append(""+vals[i].getRGB());
    	}
    	return sbuf.toString();
    }
    
    private Color[] commaDelimStrToColorArray(String str){
    	String[] pcs = str.split(",");
    	Color[] out = new Color[pcs.length];
    	for(int i=0; i<pcs.length; i++)
    		out[i] = new Color(Integer.parseInt(pcs[i]));
    	return out;
    }
    
    private void addBlankActionPerformed(ActionEvent evt){
    	TesContext ctx = new TesContext(getDefaultContextTitle());
    	ctx.setColorMapperState(new ColorMapper.State(new int[]{0,255}, new Color[]{Color.black, getNextDefaultColor()}));
    	newContexts.add(ctx);
    	tesLayer.addContext(ctx);
    	//contextsTabbedPane.setSelectedIndex(ctxs.size()-1);
    }
    

    private void delButtonActionPerformed(ActionEvent evt){
        int selectedTabIndex = contextsTabbedPane.getSelectedIndex();
        if (selectedTabIndex < 0){ return ; }

        tesLayer.removeContext(ctxs.get(selectedTabIndex));

        if (contextsTabbedPane.getSelectedIndex() > -1){
        	TesContext ctx = ctxs.get(contextsTabbedPane.getSelectedIndex());
            if (!ctx.isUnconstrained())
            	tesLayer.setActiveContext(ctx);
        }
    }
    
    private void dupButtonActionPerformed(ActionEvent evt){
        int selectedTabIndex = contextsTabbedPane.getSelectedIndex();
        if (selectedTabIndex < 0){
            // swear at user
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        // TODO: This can certainly be done in a better way.
        ContextDetailPanel p = (ContextDetailPanel)ctxPanels.get(selectedTabIndex);

        String t = "Copy of "+p.getTitle();
        Vector<FieldDesc> f = TesContext.copyFields(p.getFields());
        Vector<RangeDesc> s = TesContext.copySelects(p.getSelects());
        Vector<OrderCriterion> o = TesContext.copyOrderBys(p.getOrderBys());
        ColorBy c;
        try {
        	c = p.getColorBy().clone();
        }
        catch(SourcedRuntimeException ex){
        	c = new ColorBy(null, null, null);
        }
        Boolean r = p.getDrawReal();
        Boolean n = p.getDrawNull();

        TesContext ctx = tesLayer.addContext(t, f, s, o, c, r, n);
        
        newContexts.add(ctx);
    }
    
    private void submitButtonActionPerformed(ActionEvent evt){
        int selectedTabIndex = contextsTabbedPane.getSelectedIndex();
        if (selectedTabIndex < 0){
        	Toolkit.getDefaultToolkit().beep();
        	return;
        }

        ContextDetailPanel p = (ContextDetailPanel)ctxPanels.get(selectedTabIndex);
        String t = null;
        Vector<FieldDesc> f = null;
        Vector<RangeDesc> s = null;
        Vector<OrderCriterion> o = null;
        ColorBy c = null;
        Boolean r = null;
        Boolean n = null;

        // If the submit was on an existing context then submit the
        // changes to the tesLayer so that it can update the context.

        TesContext ctx = (TesContext)ctxs.get(selectedTabIndex);

        // Get values of fields which have been changed, all other
        // fields will remain null.
        if (p.getTitleChanged()){ t = p.getTitle(); }
        if (p.getFieldsChanged()){ f = TesContext.copyFields(p.getFields()); }
        if (p.getSelectsChanged()){ s = TesContext.copySelects(p.getSelects()); }
        if (p.getOrderBysChanged()){ o = TesContext.copyOrderBys(p.getOrderBys()); }
        if (p.getColorByChanged()){
        	try {
        		c = p.getColorBy().clone();
        	}
        	catch(SourcedRuntimeException ex){
        		JTextField tf = (JTextField)ex.getSource();
        		tf.selectAll(); tf.requestFocus();
        		return;
        	}
        }
        if (p.getDrawRealChanged()){ r = p.getDrawReal(); }
        if (p.getDrawNullChanged()){ n = p.getDrawNull(); }

        if (t != null){
        	contextsTabbedPane.setTitleAt(selectedTabIndex, t);
        }

        // Submit changes to the context to the layer.
        tesLayer.updateContext(ctx, t, f, s, o, c, r, n);
        
        // Reset context change indicators.
        p.resetChangeFlags();
        refreshTabTitle(p);
        
        if (newContexts.contains(ctx)){
            if (ctx.isUnconstrained()){
            	JOptionPane.showMessageDialog(this,
            			"Cannot make "+ctx.getTitle()+" active since it is not constrained by selection criteria.",
            			"Unable to activate context!", JOptionPane.WARNING_MESSAGE);
            }
            else {
            	newContexts.remove(ctx);
            	tesLayer.setActiveContext(ctx);
            }
        }
    }

    // Receive notification of when some data in a contextDetailPanel
    // is changed.
    private void ctxDetailPanelStateChangedEvent(ChangeEvent e){
        ContextDetailPanel p = (ContextDetailPanel)e.getSource();
        refreshTabTitle(p);
    }

    private void refreshTabTitle(ContextDetailPanel p){

        int idx = contextsTabbedPane.indexOfComponent(p);

        if (idx > -1){
            String title = "";

            title += (p == activeTab? "[": "");
            title += p.getTitle();
            title += (p.getChanged()? "*": "");
            title += (p == activeTab? "]": "");

            contextsTabbedPane.setTitleAt(idx, title);
        }
    }
    
    
    //
    // Implementation of ContextEventListener
    //
    
    private void contextDeactivatedImpl(TesContext deactivatedContext){
		ContextDetailPanel p = ctxPanels.get(ctxs.indexOf(deactivatedContext));
		p.getColorMapper().removeChangeListener(colorMapperListener);
		colorMapperListener.setColorMapper(null);
		activeTab = null;
		refreshTabTitle(p);
    }
    
    private void contextActivatedImpl(TesContext activatedContext){
    	ContextDetailPanel p = ctxPanels.get(ctxs.indexOf(activatedContext));
    	p.getColorMapper().addChangeListener(colorMapperListener);
    	colorMapperListener.setColorMapper(p.getColorMapper());
		activeTab = p;
    	refreshTabTitle(p);
    }
    
	private void contextAddedImpl(final TesContext addedContext){
        // add a new context panel tab in the context tabbed pane
        final ContextDetailPanel p = new ContextDetailPanel(tesLView);
        p.fillFrom(addedContext);
        p.getColorMapper().addChangeListener(new ChangeListener(){
        	public void stateChanged(ChangeEvent e){
        		addedContext.setColorMapperState(p.getColorMapper().getState());
        	}
        });
        p.setInitialFocus();
        p.addChangeListener(new ChangeListener(){
        	public void stateChanged(ChangeEvent e){
        		ctxDetailPanelStateChangedEvent(e);
        	}
        });
        contextsTabbedPane.addTab(p.getTitle(), p);
        ctxPanels.add(p);
        ctxs.add(addedContext);
        
        // bring this newly added tab in front of the user
        contextsTabbedPane.setSelectedIndex(ctxPanels.indexOf(p));
	}

	public void contextActivated(ContextEvent evt) {
        // Make sure this color mapper does not affect the display any more.
		TesContext deactivatedContext = evt.getOldContext();
		TesContext activatedContext = evt.getContext();
		
		if (deactivatedContext != null){
			contextDeactivatedImpl(deactivatedContext);
		}

        // Make the color mapper for the active context color the view
        if (activatedContext != null){
        	contextActivatedImpl(activatedContext);
        }
	}
	
	public void contextAdded(ContextEvent evt) {
		contextAddedImpl(evt.getContext());
	}

	public void contextDeleted(ContextEvent evt) {
		TesContext ctx = evt.getContext();
		
		int selectedTabIndex = ctxs.indexOf(ctx);
		
        // remove context panel from tabbed panes
        ContextDetailPanel p = (ContextDetailPanel)ctxPanels.get(selectedTabIndex);
        contextsTabbedPane.remove(p);
        ctxPanels.remove(p);
        ctxs.remove(ctx);
        
        p.viewCleanup();
	}

	public void contextUpdated(ContextEvent evt) {
		// Unused here since the FocusPanel is the initiator of all changes to the contexts.
	}
	
	//
	// End - Implementation of ContextEventListener
	//
    
    private String getDefaultContextTitle(){
        return ("Context-"+(newCtxSerialNumber++));
    }
    
    private static synchronized Color getNextDefaultColor(){
        nextColorIndex = (nextColorIndex+1) % defaultColors.length;
        return defaultColors[nextColorIndex];
    }

	/**
	 * Pushes ColorMapper color changes to the LView.
	 */
    class MyColorChangeListener implements ChangeListener {
    	final private TesLView lview;
    	ColorMapper cmap;
    	
    	public MyColorChangeListener(TesLView lview){
    		this.lview = lview;
    		cmap = null;
    	}
    	
    	public void setColorMapper(ColorMapper cm){
    		cmap = cm;
    		
			// Apply the current color map to the view window
//    		if (cmap != null)
//    			lview.setColorMapOp(cmap.getColorMapOp());
    	}
    	
		public void stateChanged(ChangeEvent e) {
			if(cmap != null && !cmap.isAdjusting()) {
				lview.repaint();
			}
//				lview.setColorMapOp(cmap.getColorMapOp());
		}
    }

    
    private static Color[] defaultColors = new Color[]{
        Color.red,
        Color.blue,
        Color.green,
        Color.yellow,
        Color.magenta
    };
    private static int nextColorIndex = -1;
    
    // This is the number used in the auto generated title of a context.
    private int newCtxSerialNumber = 1;

    MyColorChangeListener colorMapperListener; 
    
    private ContextDetailPanel activeTab = null;

    private JTabbedPane contextsTabbedPane;
    private JButton     addButton, delButton, dupButton, submitButton, saveAsTmplButton, wipeTmplButton;
    private JButton     exportTmplButton, importTmplButton;
    
    private Set<TesContext> newContexts;

    // The following two are parallel arrays
    private Vector<ContextDetailPanel> ctxPanels;
    private Vector<TesContext> ctxs;

    private TesLView    tesLView;
    private TesLayer    tesLayer;

}


