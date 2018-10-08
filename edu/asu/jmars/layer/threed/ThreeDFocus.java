package edu.asu.jmars.layer.threed;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.font.TextAttribute;
import java.io.File;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;
import javax.vecmath.Color3f;
import javax.vecmath.Vector3f;

import edu.asu.jmars.layer.FocusPanel;
import edu.asu.jmars.layer.map2.MapSource;
import edu.asu.jmars.layer.util.NumericMapSourceDialog;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.Util;

@SuppressWarnings("serial")
public class ThreeDFocus extends FocusPanel {
	private ThreeDLayer myLayer;
	private JFrame viewer;
	private ThreeDCanvas canvasPanel;
	
	private JButton updateBtn;
	private JCheckBox lightChk;
	private ColorButton backgroundCBtn;
	private JCheckBox backplaneChk;
	private ColorButton lightCBtn;
	private DirectionalLightWidget lightWidget;
	private JTextField sourceTF;
	private JTextArea mapDescTA;
	private JLabel ppdLbl;
	private JLabel unitLbl;
	private JLabel ignoreLbl;
	private JLabel minLbl;
	private JLabel meanLbl;
	private JLabel maxLbl;
	private JLabel stdLbl;
	private JComboBox<String> scaleBx;
	private JTextField exagTF;
	private JTextField totalExagTF;
	
	private JFrame controlFrame;
	private JFileChooser stlChooser;
	private JFileChooser pngChooser;
	
	private final String ppdPrompt = "PPD: ";
	private final String unitPrompt = "Units: ";
	private final String ignorePrompt = "Ignore Value: ";
	private final String minPrompt = "Min: ";
	private final String meanPrompt = "Mean: ";
	private final String maxPrompt = "Max: ";
	private final String stdPrompt = "St Dev (σ): ";
	
	private StartupParameters settings = null;	
	
	private int pad = 3;
	private Insets in = new Insets(pad, pad, pad, pad);
	private int row;
	private Font descripFont = new Font("Dialog", Font.PLAIN, 12);
	
	private static DebugLog log = DebugLog.instance();
	
	public ThreeDFocus(ThreeDLView parent, StartupParameters settings) {
		super(parent, false);
		
		this.myLayer = (ThreeDLayer)parent.getLayer();
		
		if (settings != null) {
			this.settings = settings;
		} else {
			this.settings = new StartupParameters();
		}
		
		//add the main tab
		add(createControlPanel(), "Controls");
		
		//set up and display the 3D view window
		setup3D();
		
		//populate map and scale panels with info from intial source
		updateMapAndScaleInfo(myLayer.getElevationSource());
	}
	
	private void setup3D(){
		//create the canvas and set initial settings
		canvasPanel = new ThreeDCanvas(parent, settings);
		canvasPanel.setBackgroundColor(settings.backgroundColor);
		canvasPanel.enableBackplane(settings.backplaneBoolean);
		canvasPanel.setAltitudeSource(myLayer.getElevationSource());
		canvasPanel.enableDirectionalLight(settings.directionalLightBoolean);
		canvasPanel.setDirectionalLightColor(settings.directionalLightColor);
		canvasPanel.setDirectionalLightDirection(
					settings.directionalLightDirection.x, settings.directionalLightDirection.y,
					settings.directionalLightDirection.z);
		
		// set up external viewer.
		viewer = new JFrame("3D View");
		Container viewerContentPane = viewer.getContentPane();
		viewerContentPane.add(canvasPanel, BorderLayout.CENTER);
		viewer.pack();
		viewer.setVisible(true);
	}
	
	private JPanel createControlPanel(){
		//menubar
		JMenuBar menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu("File");
		JMenuItem savePngItm = new JMenuItem(saveScreenAct);
		JMenuItem saveStlItm = new JMenuItem(saveStlAct);
		fileMenu.add(savePngItm);
		fileMenu.add(saveStlItm);
		JMenu helpMenu = new JMenu("Help");
		JMenuItem controlsItm = new JMenuItem(viewControlAct);
		JMenuItem wTutorialItm = new JMenuItem(wTutorialAct);
		JMenuItem vTutorialItm = new JMenuItem(vTutorialAct);
		helpMenu.add(controlsItm);
		helpMenu.add(wTutorialItm);
		helpMenu.add(vTutorialItm);
		menuBar.add(fileMenu);
		menuBar.add(helpMenu);
		
		//status panel
		JPanel statusPnl = new JPanel(new GridBagLayout());
		statusPnl.setBorder(new TitledBorder("Status"));
		updateBtn = new JButton(updateAct);
		JButton resetBtn = new JButton(resetCameraAct);
		row = 0;
		statusPnl.add(updateBtn, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		statusPnl.add(resetBtn, new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		//display panel
		JPanel displayPnl = new JPanel(new GridBagLayout());
		displayPnl.setBorder(new TitledBorder("Display"));
		backplaneChk = new JCheckBox("Opaque Bottom", settings.backplaneBoolean);
		backplaneChk.addActionListener(backplaneListener);
		backgroundCBtn = new ColorButton("Background Color", settings.backgroundColor);
		backgroundCBtn.addActionListener(backgroundColorListener);
		row = 0;
		displayPnl.add(backplaneChk, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		displayPnl.add(backgroundCBtn, new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		//lighting panel
		JPanel lightPnl = new JPanel(new GridBagLayout());
		lightPnl.setBorder(new TitledBorder("Lighting"));
		lightChk = new JCheckBox("Light On", settings.directionalLightBoolean);
		lightChk.addActionListener(lightListener);
		lightCBtn = new ColorButton("Light Color", settings.directionalLightColor);
		lightCBtn.addActionListener(lightColorListener);
		JPanel widgetPnl = new JPanel(new GridLayout(1, 1));
		widgetPnl.setPreferredSize(new Dimension(65,65));
		lightWidget = new DirectionalLightWidget(this);
		lightWidget.setEnabled(settings.directionalLightBoolean);
		lightWidget.setColor(new Color3f(settings.directionalLightColor));
		//TODO: talk to Warren, see if we can add some kind of 
		// mouselistener functionality for the lightWidget, so that
		// we can get mouse released calls, and then trigger and update
		// off that instead of requiring the user to hit the update btn
		widgetPnl.add(lightWidget);
		row = 0;
		lightPnl.add(lightChk, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		lightPnl.add(widgetPnl, new GridBagConstraints(1, row, 1, 2, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0,3,0,3), 1, 1));
		lightPnl.add(lightCBtn, new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		//Top Panel
		JPanel topPnl = new JPanel(new GridBagLayout());
		topPnl.setBackground(Util.lightBlue);
		row = 0;
		topPnl.add(statusPnl, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		topPnl.add(displayPnl, new GridBagConstraints(1, row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		topPnl.add(lightPnl, new GridBagConstraints(2, row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		
		//map panel
		JPanel mapPnl = new JPanel(new GridBagLayout());
		mapPnl.setBorder(new TitledBorder("Map Source"));
		JButton sourceBtn = new JButton(sourceAct);
		sourceTF = new JTextField(20);
		sourceTF.setEditable(false);
		unitLbl = new JLabel(unitPrompt);
		ppdLbl = new JLabel(ppdPrompt);
		ignoreLbl = new JLabel(ignorePrompt);
		JPanel lblPnl = new JPanel();
		lblPnl.add(unitLbl);
		lblPnl.add(Box.createHorizontalStrut(10));
		lblPnl.add(ppdLbl);
		lblPnl.add(Box.createHorizontalStrut(10));
		lblPnl.add(ignoreLbl);
		JPanel mapDescPnl = new JPanel(new GridLayout(1, 1));
		mapDescPnl.setBorder(new TitledBorder("Description"));
		mapDescPnl.setPreferredSize(new Dimension(0, 150));
		mapDescTA = new JTextArea();
		mapDescTA.setBackground(Util.panelGrey);
		mapDescTA.setLineWrap(true);
		mapDescTA.setWrapStyleWord(true);
		mapDescTA.setEditable(false);
		JScrollPane mapDescSP = new JScrollPane(mapDescTA);
		mapDescSP.setBorder(BorderFactory.createEmptyBorder());
		mapDescPnl.add(mapDescSP);
		row = 0;
		mapPnl.add(sourceBtn, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
		mapPnl.add(sourceTF, new GridBagConstraints(1, row, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, in, pad, pad));
		mapPnl.add(lblPnl, new GridBagConstraints(0, ++row, 2, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		mapPnl.add(mapDescPnl, new GridBagConstraints(0, ++row, 2, 1, .5, .5, GridBagConstraints.CENTER, GridBagConstraints.BOTH, in, pad, pad));
		
		//scaling panel
		JPanel scalePnl = new JPanel(new GridBagLayout());
		scalePnl.setBorder(new TitledBorder("Scaling"));
		JPanel sourcePnl = new JPanel(new GridBagLayout());
		sourcePnl.setBorder(new TitledBorder("Source Values"));
		minLbl = new JLabel(minPrompt);
		meanLbl = new JLabel(meanPrompt);
		maxLbl = new JLabel(maxPrompt);
		stdLbl = new JLabel(stdPrompt);
		sourcePnl.add(Box.createHorizontalStrut(5));
		sourcePnl.add(minLbl);
		sourcePnl.add(Box.createHorizontalStrut(30));
		sourcePnl.add(meanLbl);
		sourcePnl.add(Box.createHorizontalStrut(30));
		sourcePnl.add(maxLbl);
		sourcePnl.add(Box.createHorizontalStrut(30));
		sourcePnl.add(stdLbl);
		sourcePnl.add(Box.createHorizontalStrut(5));
		JLabel scaleLbl = new JLabel("Mode:");
		Vector<String> scaleVec = new Vector<String>();
		scaleVec.add(ThreeDCanvas.SCALE_MODE_AUTO_SCALE);
		scaleVec.add(ThreeDCanvas.SCALE_MODE_RANGE);
		scaleVec.add(ThreeDCanvas.SCALE_MODE_ST_DEV);
		scaleVec.add(ThreeDCanvas.SCALE_MODE_ABSOLUTE);
		scaleBx = new JComboBox<String>(scaleVec);
		//set the scale mode from the settings object
		scaleBx.setSelectedItem(settings.scaleMode);
		scaleBx.addActionListener(scaleListener);
		JLabel exagLbl = new JLabel("Vertical Exaggeration:");
		exagTF = new JTextField(5);
		exagTF.setMinimumSize(new Dimension(60,19));
		exagTF.setText(settings.zScaleString);
		exagTF.addFocusListener(exagFocusListener);
		exagTF.addActionListener(exagListener);
		JLabel totalExagLbl = new JLabel("Total Exaggeration:");
		totalExagTF = new JTextField(5);
		totalExagTF.setEditable(false);
		totalExagTF.setMinimumSize(new Dimension(70,19));
		float displayScaleFactor = (settings.scaleUnitsInKm) ? 1000 : 1;  // If we are representing scale on body in km, then need to covert to meters for display (otherwise already in meters)
		totalExagTF.setText(String.format("%7.3f", Math.abs(Float.parseFloat(settings.zScaleString) *settings.scaleOffset * displayScaleFactor)));
		
		row = 0;
		scalePnl.add(sourcePnl, new GridBagConstraints(0, row, 6, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, in, pad, pad));
		scalePnl.add(scaleLbl, new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
		scalePnl.add(scaleBx, new GridBagConstraints(1, row, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
		scalePnl.add(exagLbl, new GridBagConstraints(2, row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
		scalePnl.add(exagTF, new GridBagConstraints(3, row, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
		scalePnl.add(totalExagLbl, new GridBagConstraints(4, row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
		scalePnl.add(totalExagTF, new GridBagConstraints(5, row, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
		JPanel botPnl = new JPanel(new GridLayout(1,1));
		botPnl.setBorder(new EmptyBorder(3, 0, 0, 0));
		botPnl.setBackground(Util.lightBlue);
		botPnl.add(scalePnl);
		
		JPanel subPnl = new JPanel();
		subPnl.setLayout(new BorderLayout());
		subPnl.setBorder(new EmptyBorder(0, 5, 5, 5));
		subPnl.setBackground(Util.lightBlue);
		subPnl.add(topPnl, BorderLayout.NORTH);
		subPnl.add(mapPnl, BorderLayout.CENTER);
		subPnl.add(botPnl, BorderLayout.SOUTH);
		
		JPanel mainPnl = new JPanel();
		mainPnl.setLayout(new BorderLayout());
		mainPnl.add(menuBar, BorderLayout.NORTH);
		mainPnl.add(subPnl, BorderLayout.CENTER);
		
		return mainPnl;
	}
	
	private AbstractAction saveScreenAct = new AbstractAction("Save scene as PNG...") {
		public void actionPerformed(ActionEvent e) {
			//set up the chooser
			if(pngChooser == null){
				pngChooser = new JFileChooser();
				pngChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
				pngChooser.setDialogTitle("Choose PNG Location");
				//add filter
				FileFilter pngFilter = new FileFilter() {
				    public String getDescription() {
				        return "Image File (.png)";
				    }
				    public boolean accept(File f) {
				        if (f.isDirectory()) {
				            return true;
				        } else {
				            return f.getName().toLowerCase().endsWith(".png");
				        }
				    }
				};
				pngChooser.addChoosableFileFilter(pngFilter);
				pngChooser.setFileFilter(pngFilter);
			}
			
			if(pngChooser.showSaveDialog(getFrame()) == JFileChooser.APPROVE_OPTION){
				String fileStr = pngChooser.getSelectedFile().getPath();
				//check to see if user added extension, add it if they didn't
				if (!fileStr.contains(".png")){
					fileStr += ".png";
				}
				//call save code
				canvasPanel.savePNG(fileStr);
				JOptionPane.showMessageDialog(getFrame(),
						"Screen capture saved successfully!",
						"Save Success", JOptionPane.INFORMATION_MESSAGE);
			}
		}
	};
	
	private AbstractAction saveStlAct = new AbstractAction("Save 3D printer file (stl)...") {
		public void actionPerformed(ActionEvent e) {
			//set up the directory chooser
			if(stlChooser == null){
				stlChooser = new JFileChooser();
				stlChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
				stlChooser.setDialogTitle("Choose STL Destination File");
				//add filter
				FileFilter stlFilter = new FileFilter() {
				    public String getDescription() {
				        return "3D Printer Files (*.stl)";
				    }
				    public boolean accept(File f) {
				        if (f.isDirectory()) {
				            return true;
				        } else {
				            return f.getName().toLowerCase().endsWith(".stl");
				        }
				    }
				};
				stlChooser.addChoosableFileFilter(stlFilter);
				stlChooser.setFileFilter(stlFilter);
			}
			
			int val = stlChooser.showSaveDialog(getFrame());
			String fileStr = "";
			String nameStr = "";
			if(val == JFileChooser.APPROVE_OPTION){
				fileStr = stlChooser.getSelectedFile().getPath();
				nameStr = stlChooser.getSelectedFile().getName();
				//check to see if user added extension, add it if they didn't
				if (!fileStr.contains(".stl")){
					fileStr += ".stl";
				}
			}
			
			//call the save code
			boolean success = true;
			try {
				ThreeDFocus.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				
				canvasPanel.saveBinarySTL(fileStr, nameStr);
			} catch (Exception e1) {
				success = false;
				log.aprintln("Could not save stl file.");
				e1.printStackTrace();
			}
			
			ThreeDFocus.this.setCursor(Cursor.getDefaultCursor());
			
			//if it saved, tell the user with a little dialog
			if(success){
				JOptionPane.showMessageDialog(getFrame(),
						"3D printer file saved successfully!",
						"Save Success", JOptionPane.INFORMATION_MESSAGE);
			}
		}
	};
	
	private AbstractAction viewControlAct = new AbstractAction("View Controls...") {
		public void actionPerformed(ActionEvent e) {
			//build the frame and panel to display
			// we only need to do this if the frame is null,
			// otherwise we just show it because it doesn't change.
			if(controlFrame == null){
				controlFrame = new JFrame("3D Controls");
				//hide the frame when it's closed, instead of disposing of it
				controlFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
				controlFrame.addWindowListener(new WindowAdapter() {
					public void windowClosing(WindowEvent windowEvent){
						controlFrame.setVisible(false);
					}
				});
				
				//build display
				JPanel backPnl = new JPanel();
				backPnl.setLayout(new BorderLayout());
				backPnl.setBackground(Util.lightBlue);
				backPnl.setBorder(new EmptyBorder(8,8,8,8));
				JPanel controlPnl = new JPanel();
				controlPnl.setLayout(new GridBagLayout());
				controlPnl.setBorder(BorderFactory.createCompoundBorder(new TitledBorder("3D Controls"), new EmptyBorder(0, 5, 5, 5)));
				JLabel keyLbl = new JLabel("Key Operations");
				JLabel mouseLbl = new JLabel("Mouse Operations");
				Font headerFont = new Font("Dialog", Font.BOLD, 14);
				Map attributes = headerFont.getAttributes();
				attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
				keyLbl.setFont(headerFont.deriveFont(attributes));
				mouseLbl.setFont(headerFont.deriveFont(attributes));
				
				//Control Labels
				//key labels
				JLabel leftLbl = createControlLbl("Translate scene to the left");
				JLabel rightLbl = createControlLbl("Translate scene to the right");
				JLabel upLbl = createControlLbl("Translate scene up");
				JLabel downLbl = createControlLbl("Translate scene down");
				JLabel plusLbl = createControlLbl("Rotate scene counter-clockwise");
				JLabel minusLbl = createControlLbl("Rotate scene clockwise");
				JLabel wLbl = createControlLbl("Translate camera view up");
				JLabel aLbl = createControlLbl("Translate camera view left");
				JLabel sLbl = createControlLbl("Translate camera view down");
				JLabel dLbl = createControlLbl("Translate camera view right");
				JLabel zLbl = createControlLbl("Zoom out");
				JLabel ZLbl = createControlLbl("Zoom in");
				JLabel f5Lbl = createControlLbl("Update scene");
				//mouse labels
				JLabel scrollLbl = createControlLbl("Zoom in/out");
				JLabel ctrlScrollLbl = createControlLbl("Zoom in/out faster");
				JLabel dragLbl = createControlLbl("Rotate scene about x or y axis");
				JLabel ctrlDragLbl = createControlLbl("Translate scene");
				JLabel shiftVertLbl = createControlLbl("Zoom in/out");
				JLabel shiftHorLbl = createControlLbl("Rotate scene about z axis");
				
				row = 0;
				int pad = 1;
				Insets in = new Insets(pad,5*pad,pad,5*pad);
				controlPnl.add(keyLbl, new GridBagConstraints(0, row++, 2, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(new JLabel("Arrow Left"), new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(leftLbl, new GridBagConstraints(1, row++, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(new JLabel("Arrow Right"), new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(rightLbl, new GridBagConstraints(1, row++, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(new JLabel("Arrow Up"), new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(upLbl, new GridBagConstraints(1, row++, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(new JLabel("Arrow Down"), new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(downLbl, new GridBagConstraints(1, row++, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(new JLabel("+"), new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(plusLbl, new GridBagConstraints(1, row++, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(new JLabel("-"), new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(minusLbl, new GridBagConstraints(1, row++, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(new JLabel("w"), new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(wLbl, new GridBagConstraints(1, row++, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(new JLabel("a"), new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(aLbl, new GridBagConstraints(1, row++, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(new JLabel("s"), new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(sLbl, new GridBagConstraints(1, row++, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(new JLabel("d"), new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(dLbl, new GridBagConstraints(1, row++, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(new JLabel("Z"), new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(ZLbl, new GridBagConstraints(1, row++, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(new JLabel("z"), new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(zLbl, new GridBagConstraints(1, row++, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(new JLabel("F5"), new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(f5Lbl, new GridBagConstraints(1, row++, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(Box.createVerticalStrut(10), new GridBagConstraints(0, row++, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(mouseLbl, new GridBagConstraints(0, row++, 2, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(new JLabel("Scroll Wheel"), new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(scrollLbl, new GridBagConstraints(1, row++, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(new JLabel("Ctrl + Scroll Wheel"), new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(ctrlScrollLbl, new GridBagConstraints(1, row++, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(new JLabel("Click & Drag"), new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(dragLbl, new GridBagConstraints(1, row++, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(new JLabel("Ctrl + Click & Drag"), new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(ctrlDragLbl, new GridBagConstraints(1, row++, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(new JLabel("Shift + Click & Drag Vert."), new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(shiftVertLbl, new GridBagConstraints(1, row++, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(new JLabel("Shift + Click & Drag Horz."), new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(shiftHorLbl, new GridBagConstraints(1, row++, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));

				backPnl.add(controlPnl);
				//add to frame
				controlFrame.setContentPane(backPnl);
				controlFrame.pack();
				controlFrame.setLocationRelativeTo(getFrame());
			}
			//display frame
			controlFrame.setVisible(true);
		}
	};
	
	private JLabel createControlLbl(String text){
		JLabel label = new JLabel(text);
		label.setFont(descripFont);
		return label;
	}
	
	private AbstractAction wTutorialAct = new AbstractAction("View Written Tutorial...") {
		public void actionPerformed(ActionEvent e) {
			try {
				Util.launchBrowser("https://jmars.mars.asu.edu/3d-layer");
			} catch (Exception e1) {
				log.aprintln(e1);
			}
		}
	};
	
	private AbstractAction vTutorialAct = new AbstractAction("View Video Tutorial...") {
		public void actionPerformed(ActionEvent e) {
			try {
				Util.launchBrowser("https://jmars.mars.asu.edu/play-video?videoId=7");
			} catch (Exception e1) {
				log.aprintln(e1);
			}
			
		}
	};
	
	private AbstractAction updateAct = new AbstractAction("Update Scene") {
		public void actionPerformed(ActionEvent e) {
			//if lighting is on, make sure to update the light settings first
			if(lightChk.isSelected()){
				Vector3f lightDir = lightWidget.getLightDirection();
				settings.directionalLightDirection = lightDir;
				canvasPanel.setDirectionalLightDirection(lightDir.x, lightDir.y, lightDir.z);
			}
			update();
		}
	};
	
	private AbstractAction resetCameraAct = new AbstractAction("Reset Camera") {
		public void actionPerformed(ActionEvent e) {
			settings.directionalLightBoolean = false;
			settings.directionalLightDirection = new Vector3f(0.0f, 0.0f, 20.0f);
			settings.directionalLightColor = new Color(128, 128, 128);
			settings.backgroundColor = new Color(0, 0, 0);
			backgroundCBtn.setColor(settings.backgroundColor);
			backgroundCBtn.setBackground(settings.backgroundColor);
			settings.backplaneBoolean = false;
			backplaneChk.setSelected(settings.backplaneBoolean);
			lightCBtn.setColor(settings.directionalLightColor);
			lightCBtn.setEnabled(false);
			lightWidget.setColor(new Color3f(settings.directionalLightColor));
			lightWidget.setLightDirection(settings.directionalLightDirection.x,
					settings.directionalLightDirection.y, settings.directionalLightDirection.z);
			lightWidget.repaint();
			lightChk.setSelected(false);
			settings.alpha = 0f; 
			settings.beta = 0f;	
			settings.gamma = 0f; 
			settings.zoomFactor = 0.88f;
		    settings.transX = 0f;
		    settings.transY = 0f;
		    settings.transZ = 0f;
		    settings.xOffset = 0f; // JNN: added, should actually be at center of map, see ThreeDPanel's display()
		    settings.yOffset = 0f; // JNN: added, should actually be at center of map, see ThreeDPanel's display()
		    settings.zScaleString = "1.0"; //originalExaggeration; // JNN: modified
		    settings.scaleOffset = (float) Config.get(Util.getProductBodyPrefix()+Config.CONFIG_THREED_SCALE_OFFSET, -0.002f);
		    settings.scaleUnitsInKm = (settings.scaleOffset < 0.1) ? true : false;
			exagTF.setText(settings.zScaleString);
		    canvasPanel.goHome(settings);
			parent.setVisible(true);
			parent.setDirty(true);
		}
	};
	
	//Display the NumericMapSourceDialog to allow the user to select a source.
	//Update text fields and the 3d panel when the new source is set.
	private AbstractAction sourceAct = new AbstractAction("Set Vertical Source...") {
		public void actionPerformed(ActionEvent e) {
			MapSource altitudeSource = NumericMapSourceDialog.getUserSelectedSources(ThreeDFocus.this, false).get(0);
			//source can be null if the user cancels out of the dialog
			if(altitudeSource!=null){
				//update elevation source
				canvasPanel.setAltitudeSource(altitudeSource);
				myLayer.setElevationSource(altitudeSource);
				settings.setMapSource(altitudeSource);
				//must call this so the lview requests new tiles
				// with the new elevation source for the canvas to use
				parent.setDirty(true);
				
				//update 3d canvas and focus panel
				update();
			}
		}
	};
	
	private ActionListener backplaneListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			settings.backplaneBoolean = backplaneChk.isSelected();
			canvasPanel.enableBackplane(settings.backplaneBoolean);
			viewer.pack();
			canvasPanel.refresh();
		}
	};
	
	private ActionListener backgroundColorListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			final Color newColor = JColorChooser.showDialog(
						Util.getDisplayFrame(ThreeDFocus.this), 
						backgroundCBtn.getText(),
						backgroundCBtn.getColor());
			if (newColor != null) {
				settings.backgroundColor = newColor;
				backgroundCBtn.setColor(newColor);
				backgroundCBtn.setBackground(newColor);
				canvasPanel.setBackgroundColor(newColor);
				viewer.pack();
				canvasPanel.refresh();
			}
		}
	};
	
	private ActionListener lightListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			settings.directionalLightBoolean = !settings.directionalLightBoolean;
			if (settings.directionalLightBoolean == true) {
				lightCBtn.setEnabled(true);
				lightWidget.setEnabled(true);
			} else {
				lightCBtn.setEnabled(false);
				lightWidget.setEnabled(false);
			}
			lightWidget.repaint();
			canvasPanel.enableDirectionalLight(settings.directionalLightBoolean);
			canvasPanel.refresh();
			viewer.pack();
		}
	};
	
	private ActionListener lightColorListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			final Color newColor = JColorChooser.showDialog(
			Util.getDisplayFrame(ThreeDFocus.this),
			lightCBtn.getText(),
			lightCBtn.getColor());
			if (newColor != null) {
				settings.directionalLightColor = newColor;
				lightCBtn.setColor(newColor);
				canvasPanel.setDirectionalLightColor(newColor);
				lightWidget.setColor(new Color3f(newColor));
				lightWidget.repaint();
				canvasPanel.refresh();
				viewer.pack();
			}
		}
	};

	private ActionListener scaleListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			switch((String)scaleBx.getSelectedItem()){
			case ThreeDCanvas.SCALE_MODE_ST_DEV: // from standard deviation
				settings.scaleMode = ThreeDCanvas.SCALE_MODE_ST_DEV;
				scaleBx.setToolTipText("Divides the data by the standard deviation");
				break;
			case ThreeDCanvas.SCALE_MODE_RANGE: // from range of values
				settings.scaleMode = ThreeDCanvas.SCALE_MODE_RANGE;
				scaleBx.setToolTipText("Multiplies the data by 100% and divides by the range");
				break;
			case ThreeDCanvas.SCALE_MODE_AUTO_SCALE: // from auto scale
				settings.scaleMode = ThreeDCanvas.SCALE_MODE_AUTO_SCALE;
				scaleBx.setToolTipText("JMARS best guess for visual effect");
				break;
			case ThreeDCanvas.SCALE_MODE_ABSOLUTE: // from absolute values
				settings.scaleMode = ThreeDCanvas.SCALE_MODE_ABSOLUTE;
				scaleBx.setToolTipText("Uses source data unmodified");
				break;
			default: // unknown selection
				settings.scaleMode = ThreeDCanvas.SCALE_MODE_AUTO_SCALE;
				scaleBx.setToolTipText("JMARS best guess for visual effect");
				log.aprintln("Unknown scale mode. Setting to default");
				break;
			}
			canvasPanel.setScaleMode(settings.scaleMode, exagTF.getText());
			//update the scene by triggering the lview to get new mapdata
			parent.setDirty(true);
			updateExaggeration();
			update();
		}
	};
	
	private ActionListener exagListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			updateExaggeration();
			update();
		}
	};
	
	private FocusListener exagFocusListener = new FocusListener() {
		public void focusLost(FocusEvent e) {
			updateExaggeration();
			update();
		}
		
		public void focusGained(FocusEvent e) {
		}
	};

	private void updateExaggeration(){
		String prevExag = settings.zScaleString;
		
		//make sure it's a valid number
		try{
			String newExag = exagTF.getText();
			Float.parseFloat(newExag);
			
			settings.zScaleString = newExag;
			
		}catch(NumberFormatException e){
			log.aprintln("Invalid exaggeration entry");
			exagTF.setText(prevExag);
		}
		
		// Set Total Exaggeration field
		float total = Math.abs(settings.scaleOffset);
		if (settings.scaleUnitsInKm) {
			//Convert back to meters to make sense to user
			total = total * 1000;
		}
		totalExagTF.setText(String.format("%7.3f",total));

	}
	
	private void updateMapAndScaleInfo(MapSource source){
		//update the source title
		String sourceTxt = source.getTitle();
		sourceTF.setText(sourceTxt);
		
		//update the source description
		if (source.getAbstract() != null) {
			mapDescTA.setText(source.getAbstract());
			mapDescTA.setCaretPosition(0);
		} else {
			mapDescTA.setText("");
		}
		
		//update the source ppd
		ppdLbl.setText(ppdPrompt+source.getMaxPPD());
		
		//update the source units
		if(source.getUnits() != null){
			unitLbl.setText(unitPrompt+source.getUnits());
		}else{
			unitLbl.setText(unitPrompt+"Unavailable");
		}
		
		//update ignore value
		double[] vals = source.getIgnoreValue();
		if(vals != null){
			ignoreLbl.setText(ignorePrompt+vals[0]);
		}else{
			ignoreLbl.setText(ignorePrompt+"None");
		}
		
		//update the mean/min/max/std values
		Elevation e = canvasPanel.getElevation();
		if(e != null){
			DecimalFormat df = new DecimalFormat("#0.###");
			minLbl.setText(minPrompt+df.format(e.getMinAltitude()));
			meanLbl.setText(meanPrompt+df.format(e.getMean()));
			maxLbl.setText(maxPrompt+df.format(e.getMaxAltitude()));
			stdLbl.setText(stdPrompt+df.format(e.getStandardDeviation()));
		}
	}
	
	/** This updates the scene with new elevation data. */
	public void update() {
		myLayer.setStatus(Color.yellow);

		// Do whatever needs to be done to re-render the scene
		canvasPanel.updateElevationSource();
		canvasPanel.setScale(new Float(settings.zScaleString));
		canvasPanel.refresh();

		/**
		 ** Prevents the 3d window from re-appearing when we delete
		 ** the layer. (added by Michael as a quick fix)
		 **/
		if (((ThreeDLView)parent).isDead)
			return;//

		viewer.pack();
		viewer.setVisible(true);
				
		// clean up
		updateBtn.setEnabled(true);
		exagTF.setEnabled(true);
		parent.setVisible(true); // JNN: added
		parent.setDirty(false);

		//populate map and scale panels with info from intial source
		updateMapAndScaleInfo(myLayer.getElevationSource());
		
		myLayer.setStatus(Util.darkGreen);
	}
	
	public StartupParameters getSettings() {
		
		if (canvasPanel != null) {			
			settings = canvasPanel.getSettings();
		} 	
		return settings;
	}

	/**
	 * called by the focus panel whenever the lview is "cleaned up".
	 */
	public void destroyViewer() {
		if (lightWidget != null) {
			lightWidget.destroy();
			lightWidget = null;
		}
		if (canvasPanel != null) {
			canvasPanel.setVisible(false);
			viewer.getContentPane().remove(canvasPanel);
			canvasPanel.cleanup();
			canvasPanel = null;
			viewer.dispose();
			viewer = null;
		}
		if(controlFrame != null){
			controlFrame.dispose();
		}
	}

	
	// An inner class that displays a button and allows the user to change a color of some
	// component or other.  It is used in this application to change the color of the directional
	// light and the color of the background.
	private class ColorButton extends JButton {
		private Color color;

		public ColorButton(String l, Color c) {
			super(l);
			setContentAreaFilled(true);
			setColor(c);
			setFocusPainted(false);
		}

		// sets the background as the color of the button.  If the color is lighter
		// than gray, then black is used for the color of the button's text instead
		// of white.
		public void setColor(Color c) {
			color = c;
			setBackground(c);
			if ((c.getRed() + c.getGreen() + c.getBlue()) > (128 + 128 + 128)) {
				setForeground(Color.black);
			} else {
				setForeground(Color.white);
			}
		}

		// returns the color that was previously defined.
		public Color getColor() {
			return color;
		}
	}
}