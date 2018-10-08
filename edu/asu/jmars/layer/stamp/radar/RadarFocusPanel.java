package edu.asu.jmars.layer.stamp.radar;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.font.TextAttribute;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.MouseInputListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYSeriesCollection;

import edu.asu.jmars.layer.stamp.StampImage;
import edu.asu.jmars.layer.stamp.StampImageFactory;
import edu.asu.jmars.layer.stamp.StampLView;
import edu.asu.jmars.layer.stamp.StampLayer;
import edu.asu.jmars.layer.stamp.StampShape;
import edu.asu.jmars.layer.stamp.StampLayer.StampSelectionListener;
import edu.asu.jmars.layer.stamp.networking.StampLayerNetworking;
import edu.asu.jmars.layer.stamp.radar.RadarHorizon;
import edu.asu.jmars.swing.OverlapLayout;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.Util;

//TODO Some restructuring in the RadarFocusPanel still needs to be done, 
// so that the panel is pulling it's information directly from the 
// FilledStampRadarType object instead of copying the values to local variables.
public class RadarFocusPanel extends JPanel implements StampSelectionListener{
	
	final private StampLView parent;
	final private StampLayer stampLayer;
	private JLayeredPane browseLp;
	private JLayeredPane fullResLp;
	private BufferedImage browseImage;
	private BrowseDrawPanel browseDrawPnl;
	private FullResDrawPanel fullResDrawPnl;
	private FullResImagePanel fullResPnl;
	private JFrame fullResControlFrame;
	private JButton chartBtn;
	private JButton fullResBtn;
	private int curSample;
	private double curLon, curLat;
	private double[] plotData;
	private BufferedImage fullResNumeric;
	private BufferedImage fullResImage;
	private StampImage fullResStampImage;
	private JSplitPane split;
	private JFrame fullResFrame;
	private JPanel holdPnl;
	private String xAxisStr = "Value";
	private String yAxis1Str = "Depth (Pixels)";
	private String yAxis2Str = "Delay Time (μs)";
	private JFrame chartFrame;
	private ChartPanel chartPnl;
	private JFreeChart chart;
	private JFileChooser fileChooser;
	private JMenuItem csvSaveItem;
	private JPanel samplePnl;
	private JLabel sampleLbl;
	private String samplePrompt = "Current sample: ";
	private JPanel idPnl;
	private JLabel idLbl;
	private String idPrompt = "ID: ";
	private JScrollPane browseSp;
	private Font hintFont;
	private HorizonPanel horizonPnl;
	
	private StampShape ss = null;
	
	/** This is what percent the cue line is at in regards to entire sharad image */
	private double cuePercent = .5;
	/** This is the x pixel of the full resolution image, which the full resolution image panel begins at. */
	private int fullResXStart = 0;
	/** This is the y pixel of the full resolution image, which the full resolution image panel begins at. */
	private int fullResYStart = 0;
	/** This is the width in pixels of the full resolution image panel. */
	private int fullResViewWidth = 0;
	/** This is the width in pixels of the raster of the full resolution image. */
	private int fullResWidth = 0;
	/** This is the height in pixels of the raster of the full resolution image (should be 3600). */
	private int fullResHeight = 0;
	
	//Variables used for the drawing of horizons
	private ArrayList<Integer> xPts = new ArrayList<Integer>();
	private ArrayList<Integer> yPts = new ArrayList<Integer>();
	private int xEnd = -1;
	private int yEnd = -1;
	private boolean isDrawingHorizon = false;
	private ArrayList<RadarHorizon> horizons = new ArrayList<RadarHorizon>();
	private int horizonInt = 0;
	private Color horizonTmpColor = Color.ORANGE;
	
	private int pad = 0;
	private Insets in = new Insets(pad,pad,pad,pad);
	
    private static DebugLog log = DebugLog.instance();

	
	public RadarFocusPanel(final StampLView lview){
		parent = lview;
		stampLayer = parent.stampLayer;
		
		stampLayer.addSelectionListener(this);
		
		buildLayout();
	}
	
	private void buildLayout(){
		this.setLayout(new BorderLayout());
		split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		//give the left panel the resize weight (expand the image panel not the chart)
		split.setResizeWeight(1.0);
		
		idLbl = new JLabel(idPrompt);
		idPnl = new JPanel();
		idPnl.add(idLbl);
		
		browseLp = new JLayeredPane();
		browseLp.setLayout(new DrawLayout());
		
		browseSp = new JScrollPane(browseLp, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		browseSp.setBorder(new EmptyBorder(0, 0, 0, 0));

		JLabel hintLbl = new JLabel("**To move the cue line click and drag on the radargram**");
		hintLbl.setForeground(Color.BLUE);
		hintFont = new Font("Dialog", Font.PLAIN, 12);
		hintLbl.setFont(hintFont);
		
		JLabel hintLbl2 = new JLabel("**Once full resolution is loaded, hold the shift key to move the viewbox**");
		hintLbl2.setForeground(Color.BLUE);
		hintLbl2.setFont(hintFont);
		
		samplePnl = new JPanel();
		sampleLbl = new JLabel(samplePrompt);
		samplePnl.add(sampleLbl);
		chartBtn = new JButton(chartAct);
		chartBtn.setEnabled(false);
		fullResBtn = new JButton(fullResAct);
		fullResBtn.setEnabled(false);
		JPanel btnPnl = new JPanel();
		btnPnl.add(chartBtn);
		btnPnl.add(fullResBtn);
		
		
		int row = 0;
		JPanel leftPnl = new JPanel(new GridBagLayout());
		leftPnl.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
		leftPnl.add(idPnl, new GridBagConstraints(0, row++, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		leftPnl.add(browseSp, new GridBagConstraints(0, row++, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 7, 0, 0), pad, pad));
		leftPnl.add(sampleLbl, new GridBagConstraints(0, row++, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		leftPnl.add(hintLbl, new GridBagConstraints(0, row++, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		leftPnl.add(hintLbl2, new GridBagConstraints(0, row++, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		leftPnl.add(btnPnl, new GridBagConstraints(0, row++, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));

		chart = ChartFactory.createXYLineChart("ID:", xAxisStr, yAxis1Str, new XYSeriesCollection());
		chartPnl = new ChartPanel(chart);
		chartPnl.setChart(chart);
		chart.getXYPlot().getRangeAxis().setRange(0,3600);
		chart.getXYPlot().getRangeAxis().setInverted(true);
		//create the second y-axis
		ValueAxis yAxis2 = new NumberAxis(yAxis2Str);
		yAxis2.setRange(0, 135);
		yAxis2.setInverted(true);
		chart.getXYPlot().setRangeAxis(1, yAxis2);
		chartPnl.setPreferredSize(new Dimension(300, 0));
		
		horizonPnl = new HorizonPanel(parent);
		
		//Put the chart panel in another JPanel to add a border so
		// the height lines up with the browse image by default
		JPanel chartTab = new JPanel();
		chartTab.setBackground(Color.WHITE);
		chartTab.setLayout(new BorderLayout());
		chartTab.setBorder(new EmptyBorder(0, 0, 25, 0));
		chartTab.add(chartPnl, BorderLayout.CENTER);
		
		JTabbedPane rightPane = new JTabbedPane();
		rightPane.setTabPlacement(JTabbedPane.BOTTOM);
		rightPane.addTab("Depth Plot", chartTab);
		rightPane.addTab("Manage Horizons", horizonPnl);

		split.setLeftComponent(leftPnl);
		split.setRightComponent(rightPane);
		
		this.add(split, BorderLayout.CENTER);

		//try and use a decently wide panel size to begin with
		// leave space for the chart on the right of the divider.
		split.setDividerLocation(450);
		//TODO: this should really be smarter...
		this.setPreferredSize(new Dimension(820, 670));
		
		//if a selection has already been made, be sure to show 
		// it's radar data...do this by calling selectionChanged.
		if(stampLayer.getSelectedStamps().size()>0){
			selectionsChanged();
		}
	}

	
	private AbstractAction chartAct = new AbstractAction("Load Graph") {
		public void actionPerformed(ActionEvent e) {
			//change the cursor to give some feedback to the user 
			//TODO: later we should use a progress bar
			RadarFocusPanel.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			
			//Pull the raw data file from stamp server
			String id = getSelectedStampID();

			//make url for the stamp server
			String urlStr = "ImageServer?instrument=sharad&id="+id+"&imageType=SHARAD_NUM&zoom=3600";
			//Get image from the server or cache if already exists
			BufferedImage bi = StampImageFactory.getImage(urlStr, true);
			
			if(bi != null){
				fullResNumeric = bi;
				//populate the new chart
				updateChart();
			}
			
			chartBtn.setEnabled(false);
			
			//change the cursor back when finished
			RadarFocusPanel.this.setCursor(Cursor.getDefaultCursor());
		}
	};
	
	private AbstractAction fullResAct = new AbstractAction("View Full Res") {
		public void actionPerformed(ActionEvent e) {
			
			//only do something special if we have changed the selection and need to pull data
			// always show the frame.
			if(fullResImage==null){
			
				//change the cursor to give some feedback to the user 
				//TODO: later we should use a progress bar
				RadarFocusPanel.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				
				//Pull the full res image
				String id = getSelectedStampID();
				
				//construct URL for stamp server
				String urlStr = "ImageServer?instrument=sharad&id="+id+"&imageType=SHARAD&zoom=3600";
				//get image from server or cache if it exists already
				BufferedImage bi = StampImageFactory.getImage(urlStr, false);
				
				setFullResImage(bi);
				
				//change the cursor back when finished
				RadarFocusPanel.this.setCursor(Cursor.getDefaultCursor());
			
				//create a new filledStampRadarType and add it to the rendered tab
				((FilledStampRadarTypeFocus)parent.getFocusPanel().getRenderedView()).addStamp(ss, "SHARAD", new FilledStampRadarType.State(urlStr, "SHARAD"));
		
			}
			
			fullResFrame.setVisible(true);
			
		}
	};
	
	
	public void setFullResImage(BufferedImage bi){
		if(bi != null){
			fullResImage = bi;
			
			//If the frame hasn't been created, create it
			if(fullResFrame == null){
				fullResFrame = new JFrame();
				fullResFrame.setSize(new Dimension (500,500));
				fullResFrame.setLocationRelativeTo(chartPnl);
				fullResFrame.setLayout(new GridLayout(1,1));
				//add window listener to the frame so if it closes, enable the button
				fullResFrame.addWindowListener(new WindowAdapter() {
					public void windowClosing(WindowEvent e){
						fullResBtn.setEnabled(true);
						browseDrawPnl.repaint();
					}
				});
				//add key listener to the frame to allow for arrow panning
				fullResFrame.addKeyListener(fullResKeyListener);
				
				//create the holding panel, that has the arrow key label
				holdPnl = new JPanel(new BorderLayout());
				holdPnl.setBackground(Util.lightBlue);
				fullResFrame.getContentPane().add(holdPnl);
				
				//create layered pane to hold draw and image panels
				fullResLp = new JLayeredPane();
				fullResLp.setLayout(new DrawLayout());
				
				//add the file bar
				JMenuBar fullResBar = new JMenuBar();
				JMenu fileMenu = new JMenu("File");
				JMenuItem pngItm = new JMenuItem(saveFullresImageAct);
				JMenu helpMenu = new JMenu("Help");
				JMenuItem ctrlItm = new JMenuItem(showControlsAct);
				
				fileMenu.add(pngItm);
				fullResBar.add(fileMenu);
				fullResFrame.setJMenuBar(fullResBar);
				helpMenu.add(ctrlItm);
				fullResBar.add(helpMenu);
			}
			
			fullResPnl = new FullResImagePanel();
			fullResDrawPnl = new FullResDrawPanel();
			fullResDrawPnl.addMouseMotionListener(fullResImageListener);
			fullResDrawPnl.addMouseListener(fullResImageListener);
			
			//set the full res dimensions, because they are used several places
			fullResWidth = fullResImage.getRaster().getWidth();
			fullResHeight = fullResImage.getRaster().getHeight();
			
			fullResLp.removeAll();
			fullResLp.add(fullResPnl, new Integer(0));
			fullResLp.add(fullResDrawPnl, new Integer(1));
			fullResLp.revalidate();
			
			holdPnl.add(fullResLp, BorderLayout.CENTER);
			fullResFrame.revalidate();
			fullResFrame.repaint();
			fullResFrame.setTitle("Full Resolution: "+getSelectedStampID());
			
			if(!fullResFrame.isVisible()){
				fullResFrame.setVisible(true);
			}
			
			fullResBtn.setEnabled(false);
			
			//reset the stampImage object also
			fullResStampImage = new StampImage(ss, ss.getId(), "SHARAD", "SHARAD_NUM", null, StampLayerNetworking.getProjectionParams(ss.getId(), "SHARAD", "SHARAD_NUM"));
		}
	}
	
	private AbstractAction saveFullresImageAct = new AbstractAction("Capture as PNG...") {
		public void actionPerformed(ActionEvent e) {
			
			JFileChooser chooser = new JFileChooser();
			chooser.setDialogTitle("Capture screen to PNG");
		    FileNameExtensionFilter filter = new FileNameExtensionFilter(
			        "PNG - Portable Network Graphics", "png", "PNG");
			chooser.setFileFilter(filter);
			
			boolean succeed = true;
			if(chooser.showSaveDialog(fullResFrame) == JFileChooser.APPROVE_OPTION){
				//change the cursor to give some feedback to the user 
				fullResLp.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

				
				String fileName = chooser.getSelectedFile().toString();
				//add the extension to the file name if the user didn't specify
				if(!fileName.endsWith(".png")){
					fileName = fileName+".png";
				}
				File file = new File(fileName);

				try {
					//grab the contents of the layered pane displayed in the full res frame
					BufferedImage bi = new BufferedImage(fullResDrawPnl.getWidth(), fullResDrawPnl.getHeight(), BufferedImage.TYPE_INT_RGB);
					fullResLp.paint(bi.getGraphics());
					ImageIO.write(bi, "PNG", file);
				} catch (IOException e1) {
					e1.printStackTrace();
					succeed = false;
				}
				
				if(succeed){
					JOptionPane.showMessageDialog(fullResFrame, "PNG Capture Successful!", "Capture Success", JOptionPane.INFORMATION_MESSAGE);
				}else{
					JOptionPane.showMessageDialog(fullResFrame, "PNG Capture Not Successful.\nSee log for more info.", "Capture Failure", JOptionPane.INFORMATION_MESSAGE);
				}	
			}		
			//change the cursor back when finished
			fullResLp.setCursor(Cursor.getDefaultCursor());
		}
	};
	
	private AbstractAction showControlsAct = new AbstractAction("Show Controls") {
		public void actionPerformed(ActionEvent e) {
			//build frame and panel to display
			if(fullResControlFrame == null){
				fullResControlFrame = new JFrame();
				fullResControlFrame.setTitle("Controls");
				//hide window when it's closed instead of disposing
				fullResControlFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
				fullResControlFrame.addWindowListener(new WindowAdapter() {
					public void windowClosing(WindowEvent windowEvent){
						fullResControlFrame.setVisible(false);
					}
				});
				
				//build display
				JPanel mainPnl = new JPanel();
				mainPnl.setBackground(Util.lightBlue);
				
				JPanel controlPnl = new JPanel();
				controlPnl.setBorder(new CompoundBorder(new TitledBorder("Full Resolution Controls"), new EmptyBorder(5, 5, 5, 5)));
				controlPnl.setLayout(new GridBagLayout());
				
				JLabel panLbl = new JLabel("Panning");
				JLabel cueLbl = new JLabel("Profile Cue");
				JLabel horizonLbl = new JLabel("Horizon Drawing");
				Font headerFont = new Font("Dialog", Font.BOLD, 14);
				Map att = headerFont.getAttributes();
				att.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
				panLbl.setFont(headerFont.deriveFont(att));
				cueLbl.setFont(headerFont.deriveFont(att));
				horizonLbl.setFont(headerFont.deriveFont(att));
				
				Font infoFont = new Font("Dialog", Font.PLAIN, 12);
				JLabel panInfoLbl = new JLabel("<html>To pan within the full res view use <b>arrow keys</b>.</html>");
				panInfoLbl.setFont(infoFont);
				
				JLabel cueInfoLbl = new JLabel("<html>To move yellow cue line either <b>click</b> anywhere<br>"
											+ "in the image, or <b>click and drag</b> in the image.</html>");
				cueInfoLbl.setFont(infoFont);
				
				JLabel horizonInfoLbl = new JLabel("<html><center>Hold <b>shift and click</b> on the image to start <br>horizon."
												+ "<b><br>Click</b> (while shift is down) to add a vertex to <br>the horizon."
												+ "<b><br>Double click</b> (while shift is down) to <br>complete horizon."
												+ "<br>Press <b>Esc</b> while drawing to remove last added <br>vertex (before "
												+ "horizon has been completed).</center></html>");
				horizonInfoLbl.setFont(infoFont);
				
				int row = 0;
				controlPnl.add(panLbl, new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(Box.createVerticalStrut(3), new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(panInfoLbl, new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(Box.createVerticalStrut(5), new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(cueLbl, new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(Box.createVerticalStrut(3), new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(cueInfoLbl, new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(Box.createVerticalStrut(5), new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(horizonLbl, new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(Box.createVerticalStrut(3), new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(horizonInfoLbl, new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(Box.createVerticalStrut(5), new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
				
				
				mainPnl.add(controlPnl);
				fullResControlFrame.setContentPane(mainPnl);
				fullResControlFrame.pack();
				fullResControlFrame.setLocationRelativeTo(fullResFrame);
			}
			//display frame
			fullResControlFrame.setVisible(true);
		}
	};
	
	
	public void setHorizons(ArrayList<RadarHorizon> newHorizons){
		//TODO I think this is the proper way to set it?
		horizons = new ArrayList<RadarHorizon>(newHorizons);
		repaintHorizon();
		parent.repaint();
		horizonPnl.refreshHorizonTable(horizons);
	}
	
	//Mostly copied from ...map2/ChartView
	private AbstractAction CSVAct = new AbstractAction("Save as CSV") {
		public void actionPerformed(ActionEvent e) {
			if(fileChooser == null){
				fileChooser = new JFileChooser();
				fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				fileChooser.setFileFilter(new FileFilter(){
					public boolean accept(File f) {
						String fileName = f.getName();
						int indexOfDot = fileName.lastIndexOf('.');
						if (indexOfDot > -1 && (fileName.substring(indexOfDot).equalsIgnoreCase(".txt") || fileName.substring(indexOfDot).equals(".csv"))){
							return true;
						}
						return false;
					}

					public String getDescription() {
						return "Text Files";
					}
				});
			}
			
			while (true) {
				int rc = fileChooser.showSaveDialog(chartFrame);
				if (rc != JFileChooser.APPROVE_OPTION || fileChooser.getSelectedFile() == null) {
					break;
				}
				File selected = fileChooser.getSelectedFile();
				if (!selected.exists() ||
						JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
							chartFrame, "File exists, overwrite?", "File already exists",
							JOptionPane.YES_NO_OPTION)) {
					try {
						saveAsText(fileChooser.getSelectedFile());
					} catch(Exception ex) {
						JOptionPane.showMessageDialog(chartFrame, "Unable to save file: "+ex.getMessage(), "Error!", JOptionPane.ERROR_MESSAGE);
					}
					break;
				}
			}
		}
	};
	
	
	private void saveAsText(File outputFile) throws FileNotFoundException {
		String delim = ",";
		
		// Output header
		PrintStream ps = new PrintStream(new FileOutputStream(outputFile));
		ps.println("SHARAD Reading at Lat,Lon: "+curLat+", "+curLon);
		ps.println("Line (Pixels)"+delim+"Value (DN)");
		
		
		// Output data
		for (int i=0; i<plotData.length; i++){
			ps.println(i+delim+plotData[i]);
		}
		
		ps.close();
		log.aprintln("File named '"+outputFile.getName()+"' saved.");
		
	}
	
	
	private String getSelectedStampID(){
		if(ss == null){
			return "";
		}
		return ss.getStamp().getId().toLowerCase();
	}
	
	
	@Override
	public void selectionsChanged() {
		List<StampShape> list = stampLayer.getSelectedStamps();
		//If a selection has been made, and it's different than the previous selection
		if(list.size()>0 && ss!=list.get(0)){
			
			//get the stamp id
			ss = list.get(0);
			String id = getSelectedStampID();
			//make url for the stamp server, zoom=540 is 0.15*3600 (15% the original image)
			String urlStr = "ImageServer?instrument=sharad&id="+id+"&imageType=SHARAD&zoom=540";
			//Get image from the server or cache if exists
			BufferedImage bi = StampImageFactory.getImage(urlStr, false);
			
			if(bi!=null){				
				browseImage = bi;
				Dimension size = new Dimension(browseImage.getRaster().getWidth(), browseImage.getRaster().getHeight());
				//create the panels that display in the layered pane
				BrowseImagePanel panel = new BrowseImagePanel();
				panel.setPreferredSize(size);
				
				browseDrawPnl = new BrowseDrawPanel(size);
				//mouse listener for changing the profile line on the image and updating the lview
				browseDrawPnl.addMouseMotionListener(browseImageListener);
				browseDrawPnl.addMouseListener(browseImageListener);
				
				browseLp.removeAll();
				browseLp.add(panel, new Integer(0));
				browseLp.add(browseDrawPnl, new Integer(1));
				
				updateRadarPanel();
				
				//Clear chart and full res image
				chart = ChartFactory.createXYLineChart("ID:", xAxisStr, yAxis1Str, new XYSeriesCollection());
				chart.getXYPlot().getRangeAxis().setRange(0,3600);
				chart.getXYPlot().getRangeAxis().setInverted(true);
				//create the second y-axis
				ValueAxis yAxis2 = new NumberAxis(yAxis2Str);
				yAxis2.setInverted(true);
				yAxis2.setRange(0,135);
				chart.getXYPlot().setRangeAxis(1, yAxis2);
				plotData = null;
				chartPnl.setChart(chart);
				//reset all full res data
				fullResNumeric = null;
				chartBtn.setEnabled(fullResNumeric==null);
				fullResImage = null;
				//These are commented out so that when switching between rendered
				// images, the location within the image is presevered....
//				fullResXStart = 0;
//				fullResYStart = 0;
				fullResWidth = 0;
				fullResHeight = 0;
				fullResBtn.setEnabled(true);
				if(fullResFrame!=null){
					fullResFrame.setVisible(false);
				}
				parent.highlightChanged(null);
				
				//remove all horizons
				horizons.clear();
				horizonInt = 0;
			}
		}
		
		horizonPnl.refreshHorizonTable(horizons);
	}

	@Override
	public void selectionsAdded(List<StampShape> newStamps) {
		selectionsChanged();	
	}
	
	/**
	 * This is called from the ProfileLineCueingListener on the LView.
	 * 
	 * Set the spatial point for the cue line from the 
	 * profileLineCueingListener.  This point is used to 
	 * find the nearest point in the sharad points array
	 * and then draw the profile line on the sharad tiff
	 * in the focus panel accordingly.
	 * @param pt Cue line mid point (in spatial coords) from the lview
	 */
	public void setCuePoint(Point2D pt){
		
		double percent = 0;
		double lowIndex = 0;
		
		if(ss!=null && pt!=null){
			//the points the sharad data consists of
			double[] points = ss.getStamp().getPoints();
			
			double[] midPt = new double[2];
			midPt[0] = pt.getX();
			if(midPt[0]<0){
				midPt[0] = midPt[0]+360;
			}
			midPt[1] = pt.getY();
			
			
			double difference = 99999;

			//find the point in the sharad data that is 
			// closest to the midpoint of the cue line from
			// the lview
			for(int i =0; i<points.length; i=i+2){
				double x_diff = points[i] - midPt[0];
				double y_diff = points[i+1] - midPt[1];
				double diff = Math.sqrt((x_diff*x_diff)+(y_diff*y_diff));
				
				if(diff<difference){
					difference = diff;
					lowIndex = i;
				}
				
			}
			
			//To get the percent:  The points length is twice as long as the
			// actual image, plus the last two entries in it are NaN, NaN.
			// So remove those last two (-2).  Also, the length is actually one 
			// longer than the number of elements in it, so subtract another.
			// This is why denominator is points.length-3.
			// Add 0.5 to the lowIndex because that is the lon index, and the 
			// point is actually defined by lon and lat (lat being 1 more than 
			// lon).  So lowIndex+0.5 is the average between the lat and lon indices.
			// Now divide that index location by the total number of valid locations
			// to get the percent.
			percent = (lowIndex+0.5)/((points.length-3));

			//set the lat lon of the sample, used in the csv and chart
			curLon = points[(int)lowIndex];
			curLat = points[(int)lowIndex+1];
		}
		
		//this cuePercent is used when drawing the cue line
		// in the sharad image on the focus panel
		cuePercent = percent;
		//set the sample number used for plotting the chart
		setCurrentSample((int)(lowIndex/2) *10); //the *10 is because the points array is a 10th of the full data
		
	//redraw the focus panel
		updateRadarPanel();
		
	}
	
	
	public void updateRadarPanel(){
		//update id label
		idLbl.setText(idPrompt+ss.getStamp().getId());
		idPnl.repaint();
		//update profile line
		browseDrawPnl.repaint();
		//if full res is used, update profile line as well
		if(fullResFrame!=null && fullResFrame.isVisible()){
			fullResDrawPnl.repaint();
		}
		//update sample readout
		sampleLbl.setText(samplePrompt+curSample);
		samplePnl.repaint();
		//update chart
		updateChart();
	}
	
	private void updateChart(){
		if(fullResNumeric!=null){
			//get the column of pixel data
			int lines = fullResNumeric.getRaster().getHeight();
			plotData = new double[lines];
			
			//populates the plotData array
			fullResNumeric.getRaster().getPixels(curSample, 0, 1, lines, plotData);
			
			//populate the new chart
			DefaultXYDataset data = new DefaultXYDataset();
			Comparable s = "SHARAD";
			double[][] da = new double[2][plotData.length];
			
			//plot the value (plotData) on the x axis
			// and the location (pixel) on the y axis.
			for(int i=0; i<plotData.length; i++){
				da[0][i] = plotData[i];
				da[1][i] = (double) i;
			}
			
			data.addSeries(s, da);
			
			chart = ChartFactory.createXYLineChart(
					"ID: "+ss.getStamp().getId(), //Title
					xAxisStr,  //x axis label
					yAxis1Str,//y axis label
					data, //dataset
					PlotOrientation.VERTICAL, //orientation
					false, //legend
					false, //tooltips
					false); //url
			
			
			//flip the y axis (pixel) so it gets deeper as we go down
			// just like it does in the radargram picture and make the 
			// range go to 3600. Because that is always the number of 
			// lines in the image.
			ValueAxis yAxis = chart.getXYPlot().getRangeAxis();
			yAxis.setInverted(true);
			yAxis.setRange(0, 3600);
			
			//create the second y-axis
			ValueAxis yAxis2 = new NumberAxis(yAxis2Str);
			yAxis2.setInverted(true);
			yAxis2.setRange(0,135);
			chart.getXYPlot().setRangeAxis(1, yAxis2);
			
			
			chartPnl.setChart(chart);
			
			//add the csv save option to popup menu
			JPopupMenu menu = chartPnl.getPopupMenu();
			if(csvSaveItem == null){
				csvSaveItem = new JMenuItem(CSVAct);
				menu.add(new JPopupMenu.Separator());
			}
			menu.add(csvSaveItem);
			chartPnl.setPopupMenu(menu);
		}
	}
	
	private void setCurrentSample(int newSample){
		curSample = newSample;
		if(curSample<0)	curSample=0;
		//if full res has been loaded.
		if(fullResWidth>0){
			if(curSample>fullResWidth) curSample = fullResWidth-1;
		}
		//else use an approximation from the browse
		else{
			int approxWidth = (int)(browseImage.getRaster().getWidth()*20.0/3);
			if(curSample>approxWidth){
				curSample = approxWidth-1;
			}
		}
	}
	
	

	
	private void calculateFullResBounds(MouseEvent e){
		double x = e.getX();
		double y = e.getY();
		//scale to x location to full res pixel value (15%->100%)
		double full_x = x*20/3;
		double full_y = y*20/3;
		
		//start with the x at 0
		fullResXStart = 0;
		//if the full resolution x of the mouse point minus half the width
		// of the display frame is greater than 0, then use that point.
		if(full_x-fullResPnl.getWidth()/2 > 0){
			fullResXStart = (int)full_x-fullResPnl.getWidth()/2;
		}
		//now if that position is greater than the image size minus the width 
		// of the display window, then set the start to the image width minus
		// display with.
		if(fullResXStart > fullResWidth-fullResPnl.getWidth()){
			fullResXStart = fullResWidth-fullResPnl.getWidth();
		}
		//finally make sure that is greater than zero, if not, set to zero
		if(fullResXStart<0){
			fullResXStart=0;
		}
		
		//Set the y at the mouse y minus half the height.
		fullResYStart = (int)full_y - fullResPnl.getHeight()/2;
		//if this makes the start less than 0, set it to 0
		if(fullResYStart<0){
			fullResYStart = 0;	
		}
		if(fullResYStart+fullResPnl.getHeight()>fullResHeight){
			fullResYStart = fullResHeight-fullResPnl.getHeight();
		}
		if(fullResYStart<0){
			fullResYStart = 0;
		}
	}
	

	private void updateProfileLine(){
		//Notify and update lview accordingly
		double[] points = ss.getStamp().getPoints();
		
		int lonIndex = (int) Math.round(cuePercent*(points.length-2));
		//lonIndex needs to be an even number
		if(lonIndex%2 == 1){
			lonIndex++;
		}
		int latIndex = lonIndex + 1;
		
		
		//The last two points in the list are NaN,NaN to signify the 
		// end of the shape. So the last real data point is at index
		// length-4 and length-3 which is why we do the following:
		if(lonIndex>=points.length-4){
			lonIndex = points.length-4;
			latIndex = lonIndex + 1;
		}
		if(lonIndex<0){
			lonIndex = 0;
			latIndex = 1;
		}
		
		Point2D spatialPt = new Point2D.Double(points[lonIndex], points[latIndex]);
					
		Point2D worldPt = parent.getProj().spatial.toWorld(spatialPt);
		
		parent.cueChanged(worldPt);
		
		//update focus panel
		updateRadarPanel();
	}
	
	
	/**
	 * Dispose any open frames.
	 */
	public void cleanUp(){
		//Full resolution view
		if(fullResFrame!=null){
			fullResFrame.dispose();
		}
		//Controls for full res
		if(fullResControlFrame!=null){
			fullResControlFrame.dispose();
		}
	}
	
	
	

	private RadarHorizon createHorizonFromPts(){
		//create the list of spatial points from the x points
		Point2D[] points = fullResStampImage.getPoints();
		
		//make a copy of the xPts arraylist to sort and find the max and min x values
		ArrayList<Integer> xCopy = new ArrayList<Integer>(xPts);
		Collections.sort(xCopy);
		
		int startIndex = xCopy.get(0) + fullResXStart;
		int endIndex = xCopy.get(xCopy.size()-1) + fullResXStart;

		//parse out only the part of the full resolution points that we need for the new horizon
		//also, the points returned from StampImage are in degrees W
		ArrayList<Point2D> spatialPts = new ArrayList<Point2D>();
		for(int i=startIndex; i<=endIndex; i++){
			spatialPts.add(points[i]);
		}
		
		//convert the x and y points to be the absolute x and y coordinats
		ArrayList<Integer> xList = new ArrayList<Integer>();
		ArrayList<Integer> yList = new ArrayList<Integer>();
		
		for(int i=0; i<xPts.size(); i++){
			xList.add(xPts.get(i)+fullResXStart);
			yList.add(yPts.get(i)+fullResYStart);
		}
		
		return new RadarHorizon(getSelectedStampID(), xList, yList, spatialPts, horizonInt++, parent.getSettings());
	}
	
	
	private void updateFullResHighlight(){
		//calculate the path shape that needs to be highlighted
		double[] points = ss.getStamp().getPoints();
		
		double startPercent = (double)fullResXStart/(double)fullResWidth;
		double endPercent = (double)(fullResXStart+fullResViewWidth)/(double)fullResWidth;
	
		int startLonIndex = (int) Math.round(startPercent*(points.length-2));
		//lon index needs to be an even number
		if(startLonIndex%2 == 1){
			startLonIndex++;
		}
		
		int endLonIndex = (int) Math.round(endPercent*(points.length-2));
		//lon index needs to be an even number
		if(endLonIndex%2 == 1){
			endLonIndex++;
		}
		
		//The last two points in the list are NaN,NaN to signify the 
		// end of the shape. So the last real data point is at index
		// length-4 and length-3 which is why we do the following:
		if(endLonIndex>=points.length-4){
			endLonIndex = points.length-4;
		}
		int endLatIndex = endLonIndex + 1;
		
		//build an array with all the world points
		//TODO: may have to catch edge cases with world points wrapping!
		ArrayList<Point2D> worldPts = new ArrayList<Point2D>();
		for(int i=startLonIndex; i<=endLatIndex; i=i+2){
			Point2D spatialPt = new Point2D.Double(points[i], points[i+1]);
			Point2D worldPt = parent.getProj().spatial.toWorld(spatialPt);
			worldPts.add(worldPt);
		}
		
		//use those points to create a path shape
		GeneralPath worldPath = new GeneralPath();
		for(Point2D pt : worldPts){
			//Start the path
			if(worldPath.getCurrentPoint() == null){
				worldPath.moveTo(pt.getX(), pt.getY());
			}
			//continue the path
			else{
				worldPath.lineTo(pt.getX(), pt.getY());
			}
		}
		
		//update the lview
		parent.highlightChanged(worldPath);
	}
	
	
	public ArrayList<RadarHorizon> getHorizons(){
		return horizons;
	}
	
	
	/**
	 * @return The selected horizon in the horizon table from 
	 * the horizon panel (manage horizon tab)
	 */
	public RadarHorizon getSelectedHorizon(){
		return horizonPnl.getSelectedHorizon();
	}
	
	
	
	//Override one method of a Layout class that was created to be used
	// with layered panes.  This allows their container to know their real
	// size.  So that they actually work inside scrollpanes (used with the 
	// BrowseImagePanel and BrowseDrawPanel.  The layout that is extended 
	// also allows the contents of the layered pane to expand when their 
	// container expands (as used with the FullResImagePanel and FullResDrawPanel.
	private class DrawLayout extends OverlapLayout{
		public Dimension preferredLayoutSize(Container parent)
		 {			
			if(parent.getComponentCount()>0){
				//return the size of the browseDrawPanel which should
				// be the correct size of the radar gram.  This will 
				// allow the scroll pane to work properly.
				Component c = parent.getComponent(1);				
				return c.getPreferredSize();
			}
			else{
				return new Dimension(200,200);
			}
		 }
	}
	
	public void repaintHorizon(){
		browseDrawPnl.repaint();
		fullResDrawPnl.repaint();
	}
	
// ------------------------------------------------------------------------- //	
// Below is all the code for the extended JPanel classes and the mouse and 
// key listeners for both the browse image and the full resolution image.
// ------------------------------------------------------------------------- //
	
	private class BrowseDrawPanel extends JPanel{
		
		int height;
		int width;
		
		BrowseDrawPanel(Dimension size){
			setOpaque(false);
			height = (int)size.getHeight();
			width = (int)size.getWidth();
		}
		
		public void paintComponent(Graphics g){
			super.paintComponent(g);
			
			Graphics2D g2 = (Graphics2D) g;
			
			//draw the profile line	
			g2.setColor(Color.YELLOW);
			
			int x = (int)(width*cuePercent);

			if(cuePercent>1){
				x=width-1;
			}
			if(cuePercent <=0){
				x=0;
			}
			
			g2.drawLine(x, 0, x, height);
			
			
			//draw the full res box if it's showing
			if(fullResFrame!=null && fullResFrame.isVisible()){
				//convert the full res coordinates back to the browse size
				// which is 15% (3/20).
				int x1 = fullResXStart*3/20;
				int y1 = fullResYStart*3/20;
				int w = fullResPnl.getWidth()*3/20;
				int h =fullResPnl.getHeight()*3/20;
				
				g2.setColor(Color.WHITE);
				g2.drawRect(x1, y1, w, h);
			}
			
			//draw horizons
			for(RadarHorizon h : horizons){
				if(h.isVisible()){
					int[] xtmp = h.getXPoints();
					int[] ytmp = h.getYPoints();
					int[] xList = new int[xtmp.length];
					int[] yList = new int[ytmp.length];
					
					for(int i=0; i<xtmp.length; i++){
						xList[i] = xtmp[i]*3/20;
						yList[i] = ytmp[i]*3/20;
					}
					
					//change the color if this is the selected horizon
					if(h.equals(getSelectedHorizon())){
						g2.setColor(new Color(~h.getColor().getRGB()));
					}else{
						g2.setColor(h.getColor());
					}
					g2.setStroke(new BasicStroke(h.getBrowseWidth()));
					g2.drawPolyline(xList, yList, xList.length);
				}
			}
		}
	}
	
	private class FullResDrawPanel extends JPanel{
		
		private FullResDrawPanel(){
			setOpaque(false);
		}
		
		public void paintComponent(Graphics g){
			super.paintComponent(g);
			
			Graphics2D g2 = (Graphics2D) g;
			
			int viewHeight = fullResPnl.getHeight();
			
			//draw the profile line
			//Calculate where in the entire image the cue line is supposed to be
			int x = (int)(fullResWidth*cuePercent);

			if(cuePercent>1){
				x=fullResWidth-1;
			}
			if(cuePercent <=0){
				x=0;
			}
			
			//find out if that part of the image is currently visible
			if(x>=fullResXStart && x<=(fullResXStart+fullResViewWidth)){
				g2.setColor(Color.YELLOW);
				
				int relX = x-fullResXStart;
				
				g2.drawLine(relX, 0, relX, viewHeight);
			}
			
			
			//draw the current horizon
			if(!xPts.isEmpty()){
				g2.setColor(horizonTmpColor);
				
				int arraySize = xPts.size();
				int[] intX = new int[arraySize+1];
				int[] intY = new int[arraySize+1];
				for(int i=0; i<arraySize; i++){
					intX[i] = xPts.get(i);
					intY[i] = yPts.get(i);
				}
				
				if(xEnd>-1 && yEnd>-1){
					intX[xPts.size()] = xEnd;
					intY[xPts.size()] = yEnd;
					arraySize++;
				}
				
				g2.drawPolyline(intX, intY, arraySize);
			}
			
			
			//draw existing horizons
			for(RadarHorizon rh : horizons){
				if(rh.isVisible()){
					//change the color if this is the selected horizon
					if(rh.equals(getSelectedHorizon())){
						g2.setColor(new Color(~rh.getColor().getRGB()));
					}else{
						g2.setColor(rh.getColor());
					}
					g2.setStroke(new BasicStroke(rh.getFullResWidth()));
					g2.drawPolyline(rh.getXPointsForStartingX(fullResXStart), rh.getYPointsForStartingY(fullResYStart), rh.getNumberOfPoints());
				}
			}
		}
	}
	
	
	private class BrowseImagePanel extends JPanel{
		
		public void paintComponent(Graphics g){
			super.paintComponent(g);
			
			g.drawImage(browseImage, 0, 0, null);
		}
	}
	
	
	
	private class FullResImagePanel extends JPanel{

		public void paintComponent(Graphics g){
			super.paintComponent(g);
			
			//use the width and height of the full res
			// display frame to get the size of the subimage.
			// Make sure the subimage dimensions are no larger
			// than the buffered image height and width.
			fullResViewWidth = fullResPnl.getWidth();
			if(fullResViewWidth>fullResWidth){
				fullResViewWidth = fullResWidth;
			}
			int height = fullResPnl.getHeight();
			if(height>fullResHeight){
				height = fullResHeight;
			}
			
			//if a redraw gets triggered by a resize, we won't have
			// the logic from the mouse moved event, so make sure that
			// the cropping bounds still fit inside the fullresimage
			if(fullResXStart+fullResViewWidth>fullResWidth){
				fullResXStart = fullResWidth-fullResViewWidth;
			}
			if(fullResYStart+height>fullResHeight){
				fullResYStart = fullResHeight-height;
			}

			//crop the image
			BufferedImage myImg = fullResImage.getSubimage(fullResXStart, fullResYStart, fullResViewWidth, height);
			
			//draw the image
			g.drawImage(myImg, 0, 0, null);
			
			//update the outline on the profile panel
			browseDrawPnl.repaint();
			
			//update the highlight on the lview
			updateFullResHighlight();
		}
	}
	
	

	private MouseInputListener browseImageListener = new MouseInputListener() {
		
		public void mouseDragged(MouseEvent e) {
			//Allow user to drag profile line on image
			double x = e.getX();
			cuePercent = x/browseImage.getRaster().getWidth();
			
			//current sample is 15% of the real image.
			setCurrentSample((int)(x*20.0/3));

			updateProfileLine();
		}
		
		public void mouseReleased(MouseEvent e) {
			//if the full res window is open
			if(e.isShiftDown() &&  fullResFrame != null && fullResFrame.isVisible()){
				
				//calculate the bounds
				calculateFullResBounds(e);
				
				//update the full res view
				fullResPnl.repaint();
			}
		}
		public void mousePressed(MouseEvent e) {
		}
		public void mouseMoved(MouseEvent e) {
			//if the full res window is open
			if(e.isShiftDown() && fullResFrame != null && fullResFrame.isVisible()){
				
				//calculate the bounds
				calculateFullResBounds(e);
				
				//update the full res view
				fullResPnl.repaint();
			}
		}
		public void mouseExited(MouseEvent e) {
		}
		public void mouseEntered(MouseEvent e) {
		}
		public void mouseClicked(MouseEvent e) {
			//Allow user to drag profile line on image
			double x = e.getX();
			cuePercent = x/browseImage.getWidth();
			
			//current sample is 15% of the real image.
			setCurrentSample((int)(x*(20.0/3)));
			
			updateProfileLine();
		}
	};
	
	
	private MouseInputListener fullResImageListener = new MouseInputListener() {
		
		public void mouseDragged(MouseEvent e) {
			//allow the profile line to be adjusted in full res
			//relative pixel location as long as the user is not
			//drawing a horizon
			if(!isDrawingHorizon){
				double relX = e.getX();
				//adjust to aboslute pixel location in image
				double x = relX + fullResXStart;
				cuePercent = x/fullResWidth;
				
				setCurrentSample((int)x);
	
				updateProfileLine();
			}
		}
		
		public void mouseReleased(MouseEvent e) {
		}
		public void mousePressed(MouseEvent e) {
		}
		public void mouseMoved(MouseEvent e) {
			if(isDrawingHorizon){
				xEnd = e.getX();
				yEnd = e.getY();
				fullResDrawPnl.repaint();
			}
		}
		public void mouseExited(MouseEvent e) {
		}
		public void mouseEntered(MouseEvent e) {
			fullResFrame.requestFocus();
		}
		public void mouseClicked(MouseEvent e) {

			
			//Draw new horizon
			//Start new horizon or add a vertice
			if(e.getClickCount()==1 && e.isShiftDown()){
				
				isDrawingHorizon = true;
				int x = e.getX();
				int y = e.getY();
				xPts.add((int)x);
				yPts.add(y);
				
				xEnd = x;
				yEnd = y;
				
				
				fullResDrawPnl.repaint();
			}
			//close the horizon
			else if(e.getClickCount() == 2 && isDrawingHorizon && e.isShiftDown()){
				
				xEnd = -1;
				yEnd = -1;
				
				//create some sort of horizon object.
				RadarHorizon rh = createHorizonFromPts();
				horizons.add(rh);
				//call an update of the horizon panel when a new horizon is created
				horizonPnl.refreshHorizonTable(horizons);
				
				//add the horizon to the filledstamp object
				((FilledStampRadarTypeFocus)parent.getFocusPanel().getRenderedView()).getFilledStamp().addHorizon(rh);
				
				
				//clear temp horizon data
				xPts.clear();
				yPts.clear();
				isDrawingHorizon = false;
				fullResDrawPnl.repaint();
			}
			
			//move the cue line
			else if (!isDrawingHorizon){
				//allow the profile line to be adjusted in full res
				//relative pixel location
				double relX = e.getX();
				//adjust to aboslute pixel location in image
				double x = relX + fullResXStart;
				cuePercent = x/fullResWidth;
				
				setCurrentSample((int)x);
				
				updateProfileLine();
			}
		}
		
	};
	
	/**
	 * Not actually used yet... may not be needed.
	 */
	private KeyListener browseKeyListener = new KeyListener() {
		public void keyTyped(KeyEvent e) {
		}
		
		public void keyReleased(KeyEvent e) {
		}
		
		public void keyPressed(KeyEvent e) {
		}
	};
	
	
	private KeyListener fullResKeyListener = new KeyListener() {
		public void keyTyped(KeyEvent e) {
			//use escape to remove last point placed for horizon
			if(e.getKeyChar() == KeyEvent.VK_ESCAPE){
				int size = xPts.size();
				if(size>1){
					xPts.remove(size-1);
					yPts.remove(size-1);
				}else{
					xPts.clear();
					yPts.clear();
					xEnd = -1;
					yEnd = -1;
					isDrawingHorizon = false;
				}
				fullResDrawPnl.repaint();
			}
		}
		public void keyReleased(KeyEvent e) {
		}
		
		public void keyPressed(KeyEvent e) {
			//up
			if(e.getKeyCode() == KeyEvent.VK_UP){
				fullResYStart = fullResYStart - 10;
				if(fullResYStart<0){
					fullResYStart = 0;
				}
				fullResPnl.repaint();
			}
			//down
			if(e.getKeyCode() == KeyEvent.VK_DOWN){
				fullResYStart = fullResYStart + 10;
				if(fullResYStart>fullResHeight-fullResPnl.getHeight()){
					fullResYStart = fullResHeight - fullResPnl.getHeight();
				}
				fullResPnl.repaint();
			}
			//left
			if(e.getKeyCode() == KeyEvent.VK_LEFT){
				fullResXStart = fullResXStart - 10;
				if(fullResXStart<0){
					fullResXStart = 0;
				}
				fullResPnl.repaint();
			}
			//right
			if(e.getKeyCode() == KeyEvent.VK_RIGHT){
				fullResXStart = fullResXStart + 10;
				if(fullResXStart>fullResWidth-fullResPnl.getWidth()){
					fullResXStart = fullResWidth-fullResPnl.getWidth();
				}
				fullResPnl.repaint();
			}
		}
	};
}

