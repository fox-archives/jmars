package edu.asu.jmars;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import edu.asu.jmars.swing.UrlLabel;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.HttpRequestType;
import edu.asu.jmars.util.JmarsHttpRequest;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.viz3d.Decal;
import edu.asu.jmars.viz3d.Visual3D;

public class ThreeDViewWindow extends JFrame{
	private static final String DefaultShapeModel = "http://jmars.mars.asu.edu/alpha_dsk/UnitSphere.bin.gz";
    private JButton controlBtn;
	private JButton shapeModelBtn;
	private JButton updateBtn;
	private JButton clearBtn;
	private JPanel buttonPnl;
	private JCheckBox keepViewChk;
	
	private JButton citationBtn;
	private JFrame citationFrame;
	private JPanel citationPnl;
	private JTextArea citationTA;
	
	
    private Visual3D canvas3D;
    private File meshFile;
    private String selectedShapeModel;
    private String selectedBDS = "";
	
    private Color lightBlue = UIManager.getColor("TabbedPane.selected");
    private int pad = 2;
    private Insets in = new Insets(pad,pad,pad,pad);
    
	private static DebugLog log = DebugLog.instance();
    
    public ThreeDViewWindow(){
    	Main.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    	buildLayout();
    	
    	setVisible(true);
    	Main.mainFrame.setCursor(Cursor.getDefaultCursor());
    	
    	addWindowListener(windowClose);
    	canvas3D.requestFocusInWindow();
    }
    
    
    

    private void buildLayout(){
    	//button panel
    	buttonPnl = new JPanel();
    	buttonPnl.setBackground(lightBlue);
    	controlBtn = new JButton(controlAct);
    	shapeModelBtn = new JButton(shapeModelAct);
    	updateBtn = new JButton(updateAct);
    	keepViewChk = new JCheckBox("Preserve View");
    	keepViewChk.setBackground(lightBlue);
    	keepViewChk.setToolTipText("Keep previous images on the 3D body when updating the view");
    	keepViewChk.setSelected(true);
    	clearBtn = new JButton(clearAct);
    	citationBtn = new JButton(citAct);
    	
    	buttonPnl.add(controlBtn);
//    	buttonPnl.add(shapeModelBtn);//removing until issues are resolved
    	buttonPnl.add(updateBtn);
    	buttonPnl.add(keepViewChk);
    	buttonPnl.add(clearBtn);
    	buttonPnl.add(citationBtn);
    	
    	//canvas
    	setShapeModel();
    	update3DCanvas();    	
    	
    	JPanel mainPnl = new JPanel(new BorderLayout());
    	mainPnl.add(buttonPnl, BorderLayout.NORTH);
    	mainPnl.add(canvas3D, BorderLayout.CENTER);
    	
    	String title = "JMARS 3D View";
    	if(selectedBDS.length()>0){
    		title+= " -- "+selectedBDS;
    	}
    	
    	setTitle(title);
    	
    	getContentPane().add(mainPnl);
    	setSize(new Dimension(900, 950));

    }
    
    /**
     * Sets the shape model based on the class variable selectedShapeModel.
     * If selectedShapeModel is null, it will use the default model for 
     * whatever the current body is.
     */
    private void setShapeModel(){
    	Visual3D.clearSingleton();
    	
    	if(selectedShapeModel == null){
    		selectedShapeModel = Config.get(Util.getProductBodyPrefix()+"shape_model", ThreeDViewWindow.DefaultShapeModel);
    	}
    	
    	meshFile = Util.getCachedFile(selectedShapeModel, true);
    	try {
    		boolean isDefault = false;
    		if(selectedShapeModel.contains("UnitSphere")){
    			isDefault = true;
    		}
			canvas3D = Visual3D.getVisual3D(900, 900, null, Main.getCurrentBody(),
					this, meshFile, isDefault);
		} catch (IOException e) {
			log.aprintln("Error: Could not create Visual3D for 3D view window.");
			log.aprintln(e);
		}
    }
    
    private void update3DCanvas(){
    	//if the keep checkbox is not selected
    	//clear the decals first, then grab new decal
    	if(!keepViewChk.isSelected()){
    		canvas3D.clearDecals();
    	}
    	
    	final BufferedImage image = Main.testDriver.mainWindow.getSnapshot(true);
    	final Point2D[] bounds = Main.testDriver.mainWindow.getWorldBoundingPoints();
    	final Point2D centerPt = Main.testDriver.mainWindow.getCenterPoint();
    	final AtomicReference<String> gzString = new AtomicReference<String>();
    	gzString.set(selectedShapeModel);
    	
    	SwingWorker<Decal, BufferedImage> worker = new SwingWorker<Decal, BufferedImage>() {
			protected Decal doInBackground() throws Exception {
				return new Decal(image, bounds[0], bounds[1], centerPt);
			}
			
			protected void done() {
				Decal decal = null;
				DebugLog log = DebugLog.instance();									
				try {
					decal = get();
					if (decal != null) {
						canvas3D.addDecal(decal);
					}
				} catch (Exception e) {
					log.aprint("Error genertaing decal for 3D window: "+e.getLocalizedMessage());
				}
			}                			                		
		};
		worker.run();
		
		canvas3D.requestFocusInWindow();
    }

    
    private AbstractAction controlAct = new AbstractAction("Controls") {
		public void actionPerformed(ActionEvent e) {
			String msg =
					"<html><pre>" +
					"Mouse Drag Right/Left - Rotate Body\n" +
					"Mouse Drag Up/Down - Tilt Body\n" +
					"Mouse Scroll Wheel - Zoom\n" +
					"Control->Mouse Scroll Wheel - Rapid Zoom\n" +
					"Control->Mouse Drag - Translate\n" +
					"\n" +
					"Arrow Keys - Incremental Translate\n" +
					"Shift->T - Display Decals\n" +
					"Lowercase t - Hide Decals\n" +
					"Shift->B - Display Body\n" +
					"Lowercase b - Hide Body\n" +
					"Shift->P - Display North Pole Indicator\n" +
					"Lowercase p - Hide North Pole Indicator\n" ;
			
			JOptionPane.showMessageDialog(canvas3D,
										new Object[] { msg, "\n" },
										"3D Demo Controls",
										JOptionPane.PLAIN_MESSAGE);
			canvas3D.requestFocusInWindow();
		}
	};
	
	private AbstractAction shapeModelAct = new AbstractAction("Change Shape Model") {
		public void actionPerformed(ActionEvent e) {
            ArrayList<String> models = new ArrayList<String>();
            String alphaDSKConfig = Config.get("alpha_dsk_config");
            File alphaDSKFile = Util.getCachedFile(alphaDSKConfig, true);
            try {
                BufferedReader buff = new BufferedReader(new FileReader(alphaDSKFile));
                while (buff.ready()) {
                    String line = buff.readLine();
                    if (line.indexOf("--") != 0) {
                        //not a comment line
                        String[] tokens = line.split("=");
                        String bodyName = tokens[0].trim();
                        if (Main.getCurrentBody().equalsIgnoreCase(bodyName)) {
                            models.add(tokens[1]);
                        }
                    }
                }
                buff.close();
                String[] modelList = models.toArray(new String[models.size()]);
                String defaultVal = selectedBDS;
                if(!models.contains(defaultVal) && models.size()>0){
                  defaultVal = models.get(0);
                }

                String selection = (String) JOptionPane.showInputDialog(
                        canvas3D,
                        "Select the shape model",
                        "3D Shape Model",
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        modelList,
                        defaultVal);
                
                if (selection != null && selection.length() > 0) {
                	selectedBDS = selection;
                    String temp = selection;
                    temp = temp.substring(0,temp.lastIndexOf(".bds"));
                    String alphaDskLocation = Config.get("alpha_dsk","http://jmars.mars.asu.edu/alpha_dsk/");
                    temp = alphaDskLocation+temp+".bin.gz";
                    selectedShapeModel = temp;  
                    
                    ThreeDViewWindow.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    //rebuild the frame with new shape model
                    buildLayout();
                    ThreeDViewWindow.this.revalidate();
                    
                    ThreeDViewWindow.this.setCursor(Cursor.getDefaultCursor());
                }
                canvas3D.requestFocusInWindow();
            } catch (Exception ex) {
                log.aprintln(ex);
            }
			
		}
	};
	
	private AbstractAction updateAct = new AbstractAction("Update View") {
		public void actionPerformed(ActionEvent e) {
			update3DCanvas();
		}
	};
	
	private AbstractAction clearAct = new AbstractAction("Clear View") {
		public void actionPerformed(ActionEvent e) {
			canvas3D.clearDecals();
			canvas3D.requestFocusInWindow();
		}
	};
	
	private AbstractAction citAct = new AbstractAction("See Citation...") {
		public void actionPerformed(ActionEvent e) {
			String citURL = Config.get("alpha_dsk_citation");
			//body
			citURL += "body="+Main.getBody();
			//shape model
			String model = selectedBDS;
			if(model == null || model.length()<1){
				model = selectedShapeModel.substring(36);
			}
			citURL += "&shapeModel="+model;
			
			StringBuilder sb = new StringBuilder();
			
			try {
		        JmarsHttpRequest request = new JmarsHttpRequest(citURL, HttpRequestType.GET);
		        request.setConnectionTimeout(60*1000);
		        request.setReadTimeout(60*1000);
		        request.send();
                
			    BufferedReader br = new BufferedReader(new InputStreamReader(request.getResponseAsStream()));
				
				String line = null;
				while((line=br.readLine())!=null){
					sb.append(line);
				}
				br.close();

				showCitation(sb.toString());
				
			} catch (IOException | URISyntaxException e1) {
				e1.printStackTrace();
			}
			
		}
	};
	
	private void showCitation(String cit){
		JButton closeBtn = new JButton("Close");
		JPanel centerPnl = new JPanel(new GridBagLayout());
		centerPnl.setBackground(lightBlue);
		if(citationFrame == null){
			//instantiate the frame
			citationFrame = new JFrame("Citation Information");
			//instantiate the panel
			citationPnl = new JPanel();
			citationPnl.setBackground(lightBlue);
			citationPnl.setLayout(new BorderLayout());
			citationPnl.setBorder(new EmptyBorder(10, 10, 10, 10));
			//label
			JLabel citationLbl = new JLabel("Citation information:");
			//instantiate the text area
			citationTA = new JTextArea();
			citationTA.setWrapStyleWord(true);
			citationTA.setLineWrap(true);
			citationTA.setEditable(false);
			citationTA.setPreferredSize(new Dimension(400, 100));
			citationTA.setBorder(new EmptyBorder(5, 5, 5, 5));
			//scrollpane just in case
			JScrollPane citationSP = new JScrollPane(citationTA);
			centerPnl.add(citationSP, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2,2,2,2), 2, 2));
			//close button
			closeBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					citationFrame.setVisible(false);
				}
			});
			JPanel closePnl = new JPanel();
			closePnl.setBackground(lightBlue);
			closePnl.add(closeBtn);
			
			//build the layout
			citationPnl.add(citationLbl, BorderLayout.NORTH);
			citationPnl.add(centerPnl, BorderLayout.CENTER);
			citationPnl.add(closePnl, BorderLayout.SOUTH);
			
			citationFrame.getContentPane().add(citationPnl);
		}
		
		//reset the text
		//first look for any urls and keep track of where they are
		String[] tokens = cit.split(" ");
		int row = 1;
		boolean hasLink = false;
		ArrayList<Integer> removeIndices = new ArrayList<Integer>();
		for(int i=0; i<tokens.length; i++){
			String s = tokens[i];
			//If there is a url, add it to the centerpnl
			if(s.contains("ftp")){
				hasLink = true;
				removeIndices.add(i);
				UrlLabel u = new UrlLabel(s);
				centerPnl.add(u, new GridBagConstraints(0, row++, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
			}
		}
		
		//If at least one link is found, remove all instances of it from
		// the tokens string array and rebuild the string without it.  Then
		// add an extra sentence at the end.
		if(hasLink){
			ArrayList<String> tokenList = new ArrayList(Arrays.asList(tokens));
			for(int i : removeIndices){
				tokenList.remove(i);
			}
			
			StringBuilder sb = new StringBuilder();
			for(String s : tokenList){
				sb.append(s);
			}
			sb.append("See link below for more information.");
			cit = sb.toString();
		}

		citationTA.setText(cit);
		
		citationFrame.revalidate();
		//show the frame
		citationFrame.setLocationRelativeTo(citationBtn);
		citationFrame.pack();
		citationFrame.setVisible(true);
		
	}
	
	
	/**
	 * Make sure to reset the check box menu item in the view menu
	 * on the main frame
	 */
	private WindowAdapter windowClose = new WindowAdapter() {
		public void windowClosing(WindowEvent event){
			Main.threeDMenuItem.setSelected(false);
		}
	};
}
