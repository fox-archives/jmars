package edu.asu.jmars.layer.investigate;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import edu.asu.jmars.Main;
import edu.asu.jmars.util.CSVFilter;
import edu.asu.jmars.swing.ColorCombo;
import edu.asu.jmars.layer.InvestigateData;


public class DataSpikeFocus extends JPanel{
	//Attributes
	private InvestigateLView myLView;
	private InvestigateLayer myLayer;
	private InvestigateFocus myFP;
	private DataSpike myDS;
	private JFreeChart myChart;
	private double lat;
	private double lon;
	//GUI components
	private JPanel chartPnl;
	private JPanel eastPnl;
	private ChartPanel cPnl;
	private ChartReadOutTable chartTbl;
	private ChartReadOutTableModel chartModel;
	private JButton exChartBtn;
	private JLabel shapeLbl;
	private JLabel outlineLbl;
	private JLabel fillLbl;
	private JLabel colorLbl;
	private JLabel sizeLbl;
	private JComboBox shapeBx;
	private ColorCombo oColorBx;
	private ColorCombo fColorBx;
	private ColorCombo lColorBx;
	private JComboBox sizeBx;
	private JCheckBox labelChk;
	private JCheckBox markerChk;
	private JButton renameBtn;
	private JButton delBtn;
	private JButton exShapeBtn;
	private JButton exCSVBtn;
	private String[] shapeArr = {DataSpike.CIRCLE_STYLE, DataSpike.SQUARE_STYLE}; //TODO: add more shapes
	private Integer[] sizeArr = {10,11,12,13,14,15,16,17,18,19,20};
	
	private final Color lightBlue = UIManager.getColor("TabbedPane.selected");
	
	public DataSpikeFocus(DataSpike ds, InvestigateLView lview){
		myLView = lview;
		myLayer = (InvestigateLayer)myLView.getLayer();
		myFP = (InvestigateFocus)lview.getFocusPanel();
		myDS = ds;
		myChart = ds.getChart();
		lat = myDS.getPoint().getY();
		lon = 360 - myDS.getPoint().getX();
		//TODO: limit decimals to 3 or 4 digits?
		layoutContents();
		
		formatChart();
	}
	
	
	private void formatChart(){
		myChart.getXYPlot().setDomainCrosshairVisible(true);
		myChart.getXYPlot().setDomainCrosshairPaint(Color.blue);
		myChart.getXYPlot().setDomainCrosshairStroke(new BasicStroke(1.0f));
		myChart.getXYPlot().setDomainCrosshairLockedOnData(false);
		//mouse listener for readout table
		cPnl.addChartMouseListener(new ChartMouseListener() {
			public void chartMouseMoved(ChartMouseEvent event) {
				chartMouseMovedEventOccurred(event);
			}
			public void chartMouseClicked(ChartMouseEvent event) {
			}
		});		
	}

	public void chartMouseMovedEventOccurred(ChartMouseEvent e){
		Point2D pt = cPnl.translateScreenToJava2D(e.getTrigger().getPoint());
		XYPlot xyPlot = myChart.getXYPlot();
		Double indexDouble = xyPlot.getDomainAxis().java2DToValue(
					pt.getX(), 
					cPnl.getChartRenderingInfo().getPlotInfo().getDataArea(), 
					xyPlot.getDomainAxisEdge());
		//average between values (value 2 starts halfway through value 1)
		indexDouble += 0.5;
		int index = indexDouble.intValue();
		ArrayList<Double> sampleData = new ArrayList<Double>();
		ArrayList<String> ids = new ArrayList<String>();
		ArrayList<String> units = new ArrayList<String>();
		for(InvestigateData id : myDS.getInvData()){
			//Continue if the investigate data is not numeric
			if(id.getNumValSize()<=2){
				continue;
			}
			//In case one data set is longer than another,
			// don't try and set the new dataset to an index that is too
			// large, display value unavailable.
			if(index<id.getNumValSize() && index>-1){
				sampleData.add(id.getNumValue(index));
				ids.add(id.getNumKey(index));
				String unit = id.getNumUnit(index);
				if(unit == null){
					units.add("Not Avail.");
				}else{
					units.add(unit);
				}
			}else{
				sampleData.add(Double.NaN);
				ids.add("Not Avail.");
				units.add("Not Avail.");
			}
		}
		//set the chart crosshair
		xyPlot.setDomainCrosshairValue(index);
		//set the table value
		chartModel.setSampleData(sampleData, ids, units);
	}
	
	
	private void layoutContents(){
		setLayout(new BorderLayout());
		setBackground(lightBlue);
		
	//Chart JPanel (Center)
		//center panel
		chartPnl = new JPanel();
		chartPnl.setBackground(lightBlue);
		chartPnl.setBorder(new TitledBorder("Chart"));
		chartPnl.setLayout(new BorderLayout());
		String title = myDS.getName()+" - Location:("+lat+"N, "+lon+"E)";
		myChart.setTitle(title);
		cPnl = new ChartPanel(myDS.getChart(), true);
		
		//Middle
		JLabel chartOpts = new JLabel("~~To see more options, please right click on chart.~~");
		chartOpts.setBackground(lightBlue);
		chartOpts.setHorizontalTextPosition(JLabel.CENTER);
		Font optsFont = new Font("Dialog",Font.BOLD,14);
		chartOpts.setFont(optsFont);
		
		//south panel
		ArrayList<String> titles = new ArrayList<String>();
		ArrayList<Double> data = new ArrayList<Double>();
		ArrayList<String> ids = new ArrayList<String>();
		ArrayList<String> units = new ArrayList<String>();
		for(InvestigateData id : myDS.getInvData()){
			//dont add non-numeric layers to the table
			if(id.getNumValSize()<3){
				continue;
			}
			titles.add(id.name);
			data.add(Double.NaN);
			ids.add("");
			units.add("");
		}
		chartModel = new ChartReadOutTableModel(titles, myChart);
		chartModel.setSampleData(data, ids, units);
		chartTbl = new ChartReadOutTable(chartModel);
		chartTbl.setRowSelectionAllowed(false);
		chartTbl.setPreferredScrollableViewportSize(chartTbl.getPreferredSize());
		chartTbl.setFillsViewportHeight(true);
		chartTbl.packAll();
		JScrollPane tableSP = new JScrollPane(chartTbl, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, 
									ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);

		JPanel cBotPnl = new JPanel();
		cBotPnl.setBackground(lightBlue);
		cBotPnl.setLayout(new BoxLayout(cBotPnl, BoxLayout.PAGE_AXIS));
		cBotPnl.setBorder(new EmptyBorder(10, 10, 0, 10));
		cBotPnl.add(chartOpts);
		cBotPnl.add(Box.createVerticalStrut(10));
		cBotPnl.add(tableSP);
		cBotPnl.add(Box.createVerticalStrut(10));

		chartPnl.add(cPnl, BorderLayout.CENTER);
		chartPnl.add(cBotPnl, BorderLayout.SOUTH); 
		
	//Settings And Export (East)
		eastPnl = new JPanel();
		eastPnl.setBackground(lightBlue);
		eastPnl.setLayout(new BorderLayout());
		eastPnl.setBorder(new EmptyBorder(15,0,0,0));		
		
		//display panel (top)
		JPanel displayPnl = new JPanel();
		displayPnl.setBackground(lightBlue);
		displayPnl.setBorder(new TitledBorder("Display Settings"));
		FormLayout dispLayout = new FormLayout("2dlu, right:pref, pref, 5dlu right:pref, 40dlu, 2dlu",  //columns
												"5dlu, pref, 5dlu, pref, 4dlu, pref, 12dlu, pref, 5dlu, pref, 4dlu, pref, 5dlu");//rows
		displayPnl.setLayout(dispLayout);
		
		JLabel markerLbl = new JLabel("<HTML><U><B>Marker</B></U></HTML>");
		
		shapeLbl = new JLabel("Style: ");
		shapeBx = new JComboBox<String>(shapeArr);
		shapeBx.setSelectedItem(myDS.getShapeStyle());
		shapeBx.addActionListener(boxListener);
		
		markerChk = new JCheckBox("Show Marker");
		markerChk.setSelected(myDS.isMarkerOn());
		markerChk.setBackground(lightBlue);
		markerChk.addActionListener(hideListener);
		
		outlineLbl = new JLabel("Outline: ");
		oColorBx = new ColorCombo(1);
		oColorBx.setColor(myDS.getOutlineColor());
		oColorBx.addActionListener(boxListener);
		
		fillLbl = new JLabel("Fill: ");
		fColorBx = new ColorCombo(1);
		fColorBx.setColor(myDS.getFillColor());
		fColorBx.addActionListener(boxListener);
		
		JLabel labelLbl = new JLabel("<HTML><U><B>Label</B></U></HTML>");
		
		sizeLbl = new JLabel("Size: ");
		sizeBx = new JComboBox(sizeArr);
		sizeBx.setSelectedItem(myDS.getLabelSize());
		sizeBx.addActionListener(boxListener);
		//--
		labelChk = new JCheckBox("Show Label");
		labelChk.setBackground(lightBlue);
		labelChk.setSelected(myDS.isLabelOn());
		labelChk.addActionListener(hideListener);
		//--
		colorLbl = new JLabel("Color: ");
		lColorBx = new ColorCombo(1);
		lColorBx.setColor(myDS.getLabelColor());
		lColorBx.addActionListener(boxListener);
		//--

		//add to display panel
		CellConstraints cc = new CellConstraints();
		displayPnl.add(markerLbl, cc.xyw(2, 2, 5, CellConstraints.CENTER, CellConstraints.CENTER));
		displayPnl.add(shapeLbl, cc.xy(2, 4));
		displayPnl.add(shapeBx, cc.xy(3, 4));
		displayPnl.add(markerChk, cc.xyw(5,4,2));
		displayPnl.add(outlineLbl, cc.xy(2,6));
		displayPnl.add(oColorBx, cc.xy(3,6));
		displayPnl.add(fillLbl, cc.xy(5,6));
		displayPnl.add(fColorBx, cc.xy(6,6));
		displayPnl.add(labelLbl, cc.xyw(2, 8, 5, CellConstraints.CENTER, CellConstraints.CENTER));
		displayPnl.add(sizeLbl, cc.xy(2, 10));
		displayPnl.add(sizeBx, cc.xy(3, 10));
		displayPnl.add(labelChk, cc.xyw(5, 10, 2));
		displayPnl.add(colorLbl, cc.xy(2, 12));
		displayPnl.add(lColorBx, cc.xy(3, 12));
		
		
		//export panel (middle)
		JPanel exportPnl = new JPanel();
		exportPnl.setBackground(lightBlue);
		exportPnl.setBorder(new TitledBorder("Export Options"));
		exportPnl.setLayout(new GridLayout(3,1));
		exCSVBtn = new JButton(exCSVAct);		
		exChartBtn = new JButton("Export Chart"); //TODO: write an exChartAct!
		exChartBtn.setEnabled(false);
		exShapeBtn = new JButton("Export Shape"); //TODO: exShapeAct
		exShapeBtn.setEnabled(false);
		JPanel ex1 = new JPanel();
		ex1.setBackground(lightBlue);
		ex1.add(exCSVBtn);
		JPanel ex2 = new JPanel();
		ex2.setBackground(lightBlue);
		ex2.add(exChartBtn);
		JPanel ex3 = new JPanel();
		ex3.setBackground(lightBlue);
		ex3.add(exShapeBtn);
		exportPnl.add(ex1);
		exportPnl.add(ex2);
		exportPnl.add(ex3);
		//bottom pnl
		JPanel dataspikePnl = new JPanel();
		dataspikePnl.setBackground(lightBlue);
		dataspikePnl.setBorder(new TitledBorder("DataSpike Options"));
		dataspikePnl.setLayout(new GridLayout(2,1));
		delBtn = new JButton(delAct);
		renameBtn = new JButton(renameAct);
		JPanel ds1 = new JPanel();
		ds1.setBackground(lightBlue);
		ds1.add(renameBtn);
		JPanel ds2 = new JPanel();
		ds2.setBackground(lightBlue);
		ds2.add(delBtn);
		dataspikePnl.add(ds1);
		dataspikePnl.add(ds2);		
		
		//center panel (mid and bot)
		JPanel eastCenPnl = new JPanel();
		eastCenPnl.setBackground(lightBlue);
		eastCenPnl.setLayout(new BoxLayout(eastCenPnl, BoxLayout.PAGE_AXIS));
		eastCenPnl.add(Box.createVerticalStrut(20));
		eastCenPnl.add(exportPnl);
		eastCenPnl.add(Box.createVerticalStrut(20));
		eastCenPnl.add(dataspikePnl);
		eastCenPnl.add(Box.createVerticalStrut(20));
		
		//Add to east
		eastPnl.add(displayPnl, BorderLayout.NORTH);
		eastPnl.add(eastCenPnl, BorderLayout.CENTER);
		
		
		add(chartPnl, BorderLayout.CENTER);
		add(eastPnl, BorderLayout.EAST);
	}
	
	
	Action renameAct = new AbstractAction("Rename DataSpike"){
		public void actionPerformed(ActionEvent e) {
			String name = (String) JOptionPane.showInputDialog(renameBtn, "DataSpike Name:","Rename DataSpike", JOptionPane.INFORMATION_MESSAGE, null, null, myDS.getName());
			//if canceled name is null...just return 
			if(name == null){
				return;
			}
			for(DataSpike ds : myLayer.getDataSpikes()){
				if (name.equals(ds.getName())){
					JOptionPane.showMessageDialog(renameBtn, "That name is already in use.\nPlease choose another.");
					return;
				}
			}
			
			//rename data spike object
			myDS.setName(name);
			//reset chart name
			String title = myDS.getName()+" - Location:("+lat+"N, "+lon+"E)";
			myChart.setTitle(title);
			//reset tab name
			myFP.renameTab(DataSpikeFocus.this, name);
			//redraw lview (reset label)
			myLView.repaint();
		}
	};
	
	
	Action delAct = new AbstractAction("Delete DataSpike"){
		public void actionPerformed(ActionEvent e) {
			int response = JOptionPane.showConfirmDialog(delBtn, 
														"Are you sure you would like to delete DataSpike '"+myDS.getName()+"'?",
														"Delete DataSpike",JOptionPane.YES_NO_OPTION);
			if(response == JOptionPane.YES_OPTION){
				myLayer.getDataSpikes().remove(myDS);
				myLView.repaint();
				myFP.setSelectedIndex(0);
				myFP.removeTabByComponent(DataSpikeFocus.this);
			}
		}
	};
	
	
	Action exCSVAct = new AbstractAction("Export CSV"){
		public void actionPerformed(ActionEvent e) {
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
		
	};
	
	// Cycles through investigate data and prints out a csv	
	private void writeCSV(String saveFile){
		try{
			FileWriter wr = new FileWriter(saveFile);
			wr.append("Name, Value\n");
			for(int i=0; i<myDS.getInvData().size(); i++){
				wr.append(myDS.getInvData().get(i).getCSVDump());
				wr.append("\n");
			}
			wr.flush();
			wr.close();
		}catch(IOException e){
			e.printStackTrace();
		}
	}


	private ActionListener hideListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			if(e.getSource() == labelChk){
				myDS.setLabel(labelChk.isSelected());
			}else if(e.getSource() == markerChk){
				myDS.setMarkerShow(markerChk.isSelected());
			}
			//refresh lview
			myLView.repaint();
		}
	};
	
	
	private ActionListener boxListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			//outline color
			if(e.getSource() == oColorBx){
				myDS.setOutlineColor((Color)oColorBx.getSelectedItem());
			}
			//fill color
			if(e.getSource() == fColorBx){
				myDS.setFillColor((Color)fColorBx.getSelectedItem());
			}
			//label color
			if(e.getSource() == lColorBx){
				myDS.setLabelColor((Color)lColorBx.getSelectedItem());
			}
			if(e.getSource() == shapeBx){
				//Circle
				if(shapeBx.getSelectedIndex() == 0){
					myDS.setShapeStyle("Circle");
				}//Square
				else if(shapeBx.getSelectedIndex() == 1){
					myDS.setShapeStyle("Square");
				}
			}
			if(e.getSource() == sizeBx){
				myDS.setLabelSize((Integer)sizeBx.getSelectedItem());
			}
			
			
			myLView.repaint();
		}
	};
}
