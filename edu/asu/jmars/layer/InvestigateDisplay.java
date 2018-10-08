package edu.asu.jmars.layer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DefaultXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.DefaultXYDataset;


import edu.asu.jmars.Main;
import edu.asu.jmars.ToolManager;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.ruler.RulerManager;
import edu.asu.jmars.util.CSVFilter;

public class InvestigateDisplay extends JFrame{
	public static InvestigateDisplay invDisplay;
	private ArrayList<InvestigateData> invData;
	private static JTabbedPane tabbedPane;
	private static ChartPanel chartPanel;
	private static JScrollPane listSP;
	private static JTextPane textPane;
	private static JFreeChart chart;
	private JLabel noChartLbl;
	private JLabel noChartLbl2;
	
	static Dimension scrnSize = Toolkit.getDefaultToolkit().getScreenSize();
	
	private Color lightYellow = new Color(255,245,219); 
	private Color darkGreen = new Color(0,138,25);
	private Color lightBlue = UIManager.getColor("TabbedPane.selected");;
	
	//used for the investigate lview mouse listener, if there's no data spike
	//  don't draw a point and save the chart
	public static boolean isDataSpike = false;
	
	
// Constructor...instantiates gui	
	InvestigateDisplay(){
		//keeps track of the data from available layers
		invData = new ArrayList<InvestigateData>();
		//builds the ui
		buildLayout();

		addKeyListener(new InvKeyHandler());
		setUndecorated(true);
		add(tabbedPane);
	} // end constructor

//Instantiate UI components and lay them out	
	private void buildLayout(){
		//Contains the list display text
		textPane = new JTextPane();
		textPane.setEditable(false);
		textPane.setFocusable(false);
		textPane.setBackground(lightYellow);
		//Contains the list text pane
		listSP = new JScrollPane(textPane, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, 
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		
		
		//Chart tab
		chart = null;
		chartPanel = new ChartPanel(chart, true);
		noChartLbl = new JLabel("Chart not available.");
		noChartLbl2 = new JLabel("Insufficient data points.");
				
		
		tabbedPane = new JTabbedPane();
		tabbedPane.addKeyListener(new InvKeyHandler());
		tabbedPane.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				setProperSize();
			}
		});
		tabbedPane.setTabPlacement(JTabbedPane.BOTTOM);
		tabbedPane.addTab("List View", listSP);
		tabbedPane.addTab("Chart View", chartPanel);
	}

// Is safer to call this method to retrieve invDisplay 	
	public static InvestigateDisplay getInstance(){
		if (invDisplay==null)
			invDisplay = new InvestigateDisplay();
		return invDisplay;
	} //end method
	
// Is used for formatting the textPane that is displayed as 
// the invDisplay.	
	protected void addStylesToDocument(StyledDocument doc) {
        //Initialize some styles.
        Style def = StyleContext.getDefaultStyleContext().
                        getStyle(StyleContext.DEFAULT_STYLE);
 
        Style regular = doc.addStyle("Regular", def);
        StyleConstants.setFontFamily(def, "SansSerif");
 
        Style s = doc.addStyle("Italic", regular);
        StyleConstants.setItalic(s, true);
        
        s = doc.addStyle("Bold", regular);
        StyleConstants.setBold(s, true);
        
        s = doc.addStyle("Small", regular);
        StyleConstants.setFontSize(s, 11);
        
        s = doc.addStyle("BoldSmall", regular);
        StyleConstants.setBold(s, true);
        StyleConstants.setFontSize(s, 11);
        
        s = doc.addStyle("ItalicSmall", regular);
        StyleConstants.setItalic(s, true);
        StyleConstants.setFontSize(s, 11);
        
        s = doc.addStyle("ItalicBold", regular);
        StyleConstants.setItalic(s, true);
        StyleConstants.setBold(s, true);
        
        s = doc.addStyle("ItalicSmallBlue", regular);
        StyleConstants.setItalic(s, true);
        StyleConstants.setForeground(s, Color.BLUE);
        StyleConstants.setFontSize(s, 11);
        
        s = doc.addStyle("ItalicSmallRed", regular);
        StyleConstants.setItalic(s, true);
        StyleConstants.setForeground(s, Color.RED);
        StyleConstants.setFontSize(s, 11);
        
        s = doc.addStyle("SmallBlue", regular);
        StyleConstants.setForeground(s, Color.BLUE);
        StyleConstants.setFontSize(s, 11);
        
        s = doc.addStyle("Green", regular);
        StyleConstants.setForeground(s, darkGreen);
         
	} //end method
	
// Gets the InvestigateData objects from each layer	and adds
// them the the arraylist contained in InvestigateDisplay	
	public void setInvData(MouseEvent event){
	// Clear the list of stored invData objects
		invData.clear();
	// Populates the invData arraylist with investigateData objects from 
	// each showing lview.	
		List<LView> lviews = new ArrayList<LView>(LManager.getLManager().viewList);
		Collections.reverse(lviews);
		for (LView l : lviews){
			if (l != null && l.isVisible() ) {
				InvestigateData iData = l.getInvestigateData(event);
				if (iData == null || iData.getKeys().size()<1){
					continue;
				}
				invData.add(iData);
			}
		}
	//Finally, calls the set display to actually do something with 
	// all the investigateData objects
		setDisplay();
		
	} //end method

// Returns the total number of numeric data points held in the 
//  entire investigate display object.  Used for creating a plot
//	of all the points.	
	public int getTotalDataPoints(){
		int size = 0;
		for (InvestigateData id : invData){
			for(int i=0; i<id.getValSize(); i++){
				//If the value is numeric, add one to size counter
				if(id.getNumerics().get(i)){
					size++;
				}
			}
		}
		return size;
	}
	
// Takes the array of investigateData objects and formats their 
// attributes into a sensible display and adds that to the textPane	
	public void setDisplay(){
//Set List View--------------------------------------------------------
		// Clear textPane before we add new text to display	
		textPane.removeAll();
		StyledDocument doc = new DefaultStyledDocument();
		addStylesToDocument(doc);
		
	// Two arrays of text and styles used to populate the TextPane
	//with proper formatting	
		ArrayList<String> displayText = new ArrayList<String>();
		ArrayList<String> styles = new ArrayList<String>();
	// If there is no data, display a message saying so.	
		if (invData.size()<1){
			displayText.add("No Data Available");
			styles.add("Bold");
		}
	// If there is data, loop through it and add to the string arrays	
		else{
			for (InvestigateData id : invData){
				if(id == null)	continue; 		
				if(id.getKeys().size() != id.getValues().size()) continue;
				if(id.getKeys().size()>0 && id.getStyles().size()>1){
					displayText.add(id.name+"\n");
					for (int i=0; i<id.getKeys().size(); i++){
						String v = id.getValues().get(i);
						if(v==null)	continue;
						if(v.equals("")){
							displayText.add("   "+id.getKeys().get(i));
							displayText.add(" \n");
						}else{
							displayText.add("   "+id.getKeys().get(i)+": ");
							displayText.add(v+" "+id.getUnits().get(i)+"\n");
						}
					}
					styles.addAll(id.getStyles());
				}
			}	
			displayText.add("**Hold ctrl+e to export csv**");
			styles.add("Green");
		}

		try{
			for (int i=0; i<displayText.size(); i++){
					doc.insertString(doc.getLength(), displayText.get(i), doc.getStyle(styles.get(i)));
			}
		} catch (BadLocationException e){
			e.printStackTrace();
		}
		textPane.setStyledDocument(doc);
		
		
// Set chart View---------------------------------------------------------------
		

		Vector<DefaultXYDataset> dataVec = new Vector<DefaultXYDataset>();
		
		for(InvestigateData id : invData){
			if(id.hasNumerics()){
				Comparable s = id.name;
				DefaultXYDataset dcd = new DefaultXYDataset();
				double[][] data = new double[2][id.getNumValSize()];
		
				int dataCount = 0;
				for (int i=0; i<id.getNumValSize(); i++){
				
					
					//make sure value is valid
					Double val = id.getNumValue(i);
					if(Double.isNaN(val) || val == null){
						continue;
					}
					
					data[0][dataCount] = (double)dataCount;
					data[1][dataCount] = val;
		
					dataCount++;
				}
				
				if(dataCount>1){
					dcd.addSeries(s, data);
					dataVec.add(dcd);
				}
			}
		}

		
		//if no data make chart null
		chart = null;
		if(dataVec.size()>0){
			chart = ChartFactory.createXYLineChart(
				"Data Spike",				//title
				"Layers",		 			//x axis label
				"Value",		 			//y axis label
				null,			 			//data
				PlotOrientation.VERTICAL,	//orientation
				true,		 				//legend
				false, 						//tooltips
				false);						//url
			
			
			XYPlot plot = chart.getXYPlot();
			
			for(int i=0; i<dataVec.size(); i++){
				plot.setDataset(i, dataVec.get(i));
				NumberAxis newAxis = new NumberAxis(dataVec.get(i).getSeriesKey(0).toString());
				newAxis.setAutoRangeIncludesZero(false);
				plot.setRangeAxis(i, newAxis);
				plot.mapDatasetToRangeAxis(i, i);
				XYItemRenderer r = new DefaultXYItemRenderer();
				r.setSeriesShape(0, new GeneralPath());
				plot.setRenderer(i, r);
			}
				
		}
			
		chartPanel.removeAll();
		chartPanel.setChart(chart);
		isDataSpike = true;
		if(chart == null){
			isDataSpike = false;
			chartPanel.add(noChartLbl);
			chartPanel.add(noChartLbl2);
		}
			
		
		//set size of invDisplay
		setProperSize();

	} // end method
	
	
	public static void showInvDisplay(int x, int y){
	// Set size
		setProperSize();
	// Location of the component under the mouse (the mainwindow)		
		int compX = (int)Main.testDriver.mainWindow.getLocationOnScreen().getX();
		int compY = (int)Main.testDriver.mainWindow.getLocationOnScreen().getY();
	// Should be the location 10 pixels to the right and 20 pixels below the cursor				
		int newX = compX+x+10;
		int newY = compY+y+20;
	// Once the cursor gets close to the left side of the screen, draw the invDisplay
	//	to the lower right of the cursor.	
		if (newX+15>=(scrnSize.width-invDisplay.getWidth())){
			newX = compX+x - invDisplay.getWidth() - 10;			
		}
	//For the screen height, don't use the screen size because user may have a task bar
	// which we want to take into consideration.
		int winHeight = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().height;
	// Switch the display to upper left of the cursor once it gets close to the bottom...		
		if (newY+2>=(winHeight-invDisplay.getHeight())){
			newY = compY+y - 20 - invDisplay.getHeight();
		}

		invDisplay.setLocation(newX, newY);
		
		if(!invDisplay.isVisible()){
			invDisplay.setVisible(true);
		}
	// This is to make sure the frame doesn't get hidden behind jmars		
		invDisplay.toFront(); 
	} // end method	

	
	static int currWidth = -1;
// Sets the proper size of the display (only be large when in chart view)
	private static void setProperSize(){
		//don't do anything if the display hasn't been created yet
		if(invDisplay == null){
			return;
		}
		//if no chart -- no numeric data to display
		if(chart == null){
			chartPanel.setPreferredSize(new Dimension(170,0));
		}
		invDisplay.setPreferredSize(null);
		
		//if new width is larger than buffer, make it the currWidth
		if(invDisplay.getPreferredSize().width>currWidth+10){
			currWidth = invDisplay.getPreferredSize().width+10;
		}
		//if new width is much smaller than buffer, make it the currWidth
		if(invDisplay.getPreferredSize().width<currWidth-20){
			currWidth = invDisplay.getPreferredSize().width+10;
		}
		if(chart == null || tabbedPane.getSelectedComponent() != chartPanel){
			if(invDisplay.getPreferredSize().height<250){
				invDisplay.setPreferredSize(new Dimension(currWidth, invDisplay.getPreferredSize().height));
			}
			if(invDisplay.getPreferredSize().height>=250){
				invDisplay.setPreferredSize(new Dimension(currWidth, 250));
			}
		}else if(tabbedPane.getSelectedComponent() == chartPanel){
			invDisplay.setPreferredSize(new Dimension(600,500));
		}

		invDisplay.pack();
	}
	
	
	
// Takes either a mouseWheelEvent from mainGlass, or a keyEvent from invKeyHandler	
	public static void scroll(InputEvent e){
		listSP.dispatchEvent(e);		
	} // end method

//	Listens for up and down arrows to control the scrollbar on invDisplay
	private class InvKeyHandler implements KeyListener{
		public void keyTyped(KeyEvent e) {	
		}
		public void keyPressed(KeyEvent e) {
		// Used to scroll through the invDisplay list with keyboard	
			if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN) //down key or up key is pressed
				scroll(e);			
		// Used to export csv of data	
			if(e.isControlDown() && e.getKeyCode()==KeyEvent.VK_E){
				JFileChooser fc = new JFileChooser();
				CSVFilter filter = new CSVFilter();
				fc.setFileFilter(filter);
				int returnVal = fc.showSaveDialog(Main.mainFrame);
				if (returnVal == JFileChooser.APPROVE_OPTION){
					String fileName = fc.getSelectedFile().getPath();
					if (!fileName.contains(".csv")){
						fileName+=".csv";
					}	
					writeCSV(fileName);
				}
			}
			
		//Copy recenter code from main
			if(e.isControlDown() && e.getKeyCode() == KeyEvent.VK_R){
				Main.testDriver.locMgr.reprojectFromText();
				RulerManager.Instance.notifyRulerOfViewChange();
			}
		//Change tool modes with hot keys
			if(e.isShiftDown() && e.getKeyCode() == KeyEvent.VK_D){
				ToolManager.setToolMode(ToolManager.SEL_HAND);
				getInstance().setVisible(false);
			}
			if(e.isShiftDown() && e.getKeyCode() == KeyEvent.VK_P){
				ToolManager.setToolMode(ToolManager.PAN_HAND);
				getInstance().setVisible(false);
			}
			if(e.isShiftDown() && e.getKeyCode() == KeyEvent.VK_I){
				ToolManager.setToolMode(ToolManager.ZOOM_IN);
				getInstance().setVisible(false);
			}
			if(e.isShiftDown() && e.getKeyCode() == KeyEvent.VK_O){
				ToolManager.setToolMode(ToolManager.ZOOM_OUT);
				getInstance().setVisible(false);
			}
			if(e.isShiftDown() && e.getKeyCode() == KeyEvent.VK_M){
				ToolManager.setToolMode(ToolManager.MEASURE);
				getInstance().setVisible(false);
			}
			if(e.isShiftDown() && e.getKeyCode() == KeyEvent.VK_E){
				ToolManager.setToolMode(ToolManager.EXPORT);
				getInstance().setVisible(false);
			}
		
		}
		public void keyReleased(KeyEvent e) {

		}
	} // end keylistener class
	
// Cycles through investigate data and prints out a csv	
	private void writeCSV(String saveFile){
		try{
			FileWriter wr = new FileWriter(saveFile);
			wr.append("Name, Value\n");
			for(int i=0; i<invData.size(); i++){		
				wr.append(invData.get(i).getCSVDump());
				wr.append("\n");
			}
			wr.flush();
			wr.close();
		}catch(IOException e){
			e.printStackTrace();
		}
		
	}
	
	
//Returns the chart (used when temporarily storing charts)
	public static JFreeChart getChart(){
		return chart;
	}
	
//Returns the arraylist of invData objects
	public ArrayList<InvestigateData> getInvData(){
		return invData;
	}
	
} // end InvestigateDisplay class

