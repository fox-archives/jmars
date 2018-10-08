package edu.asu.jmars;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPInputStream;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.UIManager;

import edu.asu.jmars.layer.InvestigateDisplay;
import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.layer.investigate.InvestigateFactory;
import edu.asu.jmars.swing.UrlLabel;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.viz3d.Decal;
import edu.asu.jmars.viz3d.Visual3D;


public class ToolManager extends JPanel {
	
	private static List<ToolListener> listeners = new ArrayList<ToolListener>();	
	private static int Mode;
	private static int Prev;
	  
    final public static int ZOOM_IN 	= 0;
    final public static int ZOOM_OUT 	= 1;
    final public static int MEASURE 	= 2;
    final public static int PAN_HAND 	= 3;
    final public static int SEL_HAND 	= 4;
    final public static int SUPER_SHAPE = 5;
    final public static int INVESTIGATE = 6;
    final public static int EXPORT = 7;
    final public static int RESIZE = 8;
    
    ToolButton zoomIn;
    ToolButton zoomOut;
    ToolButton measure;
    ToolButton panHand;
    ToolButton selHand;
    ToolButton superShape;
    static ToolButton investigate;
    ToolButton exportButton;
    ToolButton resizeButton;
 
    
    public ToolManager ()
    {
    	// pulls icons from disk to be used for toolbar/cursor        
        Image zoom_in = Util.loadImage("resources/zoomin.png");
        Image zoom_in_c = Util.loadImage("resources/zoomin_c.png");
        ImageIcon magIn = new ImageIcon(zoom_in);
        
        Image zoom_out = Util.loadImage("resources/zoomout.png"); 
        Image zoom_out_c = Util.loadImage("resources/zoomout_c.png");
        ImageIcon magOut = new ImageIcon(zoom_out);
        
        Image yard_stick = Util.loadImage("resources/ruler.png");
        Image yard_stick_c = Util.loadImage("resources/ruler_c.png");
        ImageIcon ruler = new ImageIcon(yard_stick);
        
        Image pan_hand = Util.loadImage("resources/panhand.png"); 
        Image pan_hand_c = Util.loadImage("resources/panhand_c.png"); 
        ImageIcon hand = new ImageIcon(pan_hand);
        
        Image sel_hand = Util.loadImage("resources/arrow.png");  
        ImageIcon select = new ImageIcon(sel_hand);
        
        Image super_shape = Util.loadImage("resources/lasso.png");
        Image super_shape_c = Util.loadImage("resources/lasso_c.png"); 
        ImageIcon superS = new ImageIcon(super_shape);
        
        Image look_at = Util.loadImage("resources/investigate.png");
        Image look_at_c = Util.loadImage("resources/investigate_c.png"); 
        ImageIcon lookAt = new ImageIcon(look_at);
        
        Image export = Util.loadImage("resources/export.png");
        Image export_c = Util.loadImage("resources/export_c.png"); 
        ImageIcon exportIcon = new ImageIcon(export);
        
        Image resize = Util.loadImage("resources/resize.png");
        ImageIcon resizeIcon = new ImageIcon(resize);
                
        // creates cursors to be used in each mode
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Point hotSpot = new Point(0,0);
        
        Cursor ZoomIn = toolkit.createCustomCursor(zoom_in_c, hotSpot, "zoom in");
        Cursor ZoomOut = toolkit.createCustomCursor(zoom_out_c, hotSpot, "zoom out");
        Cursor Measure = toolkit.createCustomCursor(yard_stick_c, hotSpot, "measure");
        Cursor PanHand = toolkit.createCustomCursor(pan_hand_c, hotSpot, "pan");
        Cursor Default = Cursor.getDefaultCursor();
        Cursor SuperShape = toolkit.createCustomCursor(super_shape_c, hotSpot, "super shape");
        Cursor Investigate = toolkit.createCustomCursor(look_at_c, hotSpot, "investigate");    
        Cursor Export = toolkit.createCustomCursor(export_c, new Point(10,10), "export");    
        Cursor Resize = toolkit.createCustomCursor(export_c, new Point(10,10), "resize");    // same as export
        
        // creates buttons (and button dimensions) for the toolbar
        zoomIn = new ToolButton(ZOOM_IN, magIn, ZoomIn);
        zoomOut = new ToolButton(ZOOM_OUT, magOut, ZoomOut);
        measure = new ToolButton(MEASURE, ruler, Measure);
        panHand = new ToolButton(PAN_HAND, hand, PanHand);
        selHand = new ToolButton(SEL_HAND, select, Default);
        superShape = new ToolButton(SUPER_SHAPE, superS, SuperShape);
        //superShape.setEnabled(false); //TODO: set enabled when functionality is implemented
        investigate = new ToolButton(INVESTIGATE, lookAt, Investigate);
        exportButton = new ToolButton(EXPORT, exportIcon, Export);
        resizeButton = new ToolButton(RESIZE, resizeIcon, Resize);
        
           
        Dimension buttonDim = new Dimension(35,25);
        Dimension gap = new Dimension(5,0);
       
        // sets button sizes so they don't change when window changes
        zoomIn.setPreferredSize(buttonDim);
        zoomIn.setMaximumSize(buttonDim);
        zoomIn.setMinimumSize(buttonDim);
        zoomOut.setPreferredSize(buttonDim);
        zoomOut.setMaximumSize(buttonDim);
        zoomOut.setMinimumSize(buttonDim);
        measure.setPreferredSize(buttonDim);
        measure.setMaximumSize(buttonDim);
        measure.setMinimumSize(buttonDim);
        panHand.setPreferredSize(buttonDim);
        panHand.setMaximumSize(buttonDim);
        panHand.setMinimumSize(buttonDim);
        selHand.setPreferredSize(buttonDim);
        selHand.setMaximumSize(buttonDim);
        selHand.setMinimumSize(buttonDim);
        superShape.setPreferredSize(buttonDim);
        superShape.setMaximumSize(buttonDim);
        superShape.setMinimumSize(buttonDim);
        investigate.setPreferredSize(buttonDim);
        investigate.setMaximumSize(buttonDim);
        investigate.setMinimumSize(buttonDim);
        exportButton.setPreferredSize(buttonDim);
        exportButton.setMaximumSize(buttonDim);
        exportButton.setMinimumSize(buttonDim);
        resizeButton.setPreferredSize(buttonDim);
        resizeButton.setMaximumSize(buttonDim);
        resizeButton.setMinimumSize(buttonDim);
        
        // adds buttons to ToolManager JPanel    
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(Box.createRigidArea(new Dimension(74,0)));
        add(selHand);
        add(Box.createRigidArea(gap));
        add(panHand);
        add(Box.createRigidArea(gap));
        add(zoomIn);   
        add(Box.createRigidArea(gap));
        add(zoomOut);
        add(Box.createRigidArea(gap));
        add(measure);
//        add(Box.createRigidArea(gap));
//        add(superShape);
        add(Box.createRigidArea(gap));
        add(investigate);
        add(Box.createRigidArea(gap));
        add(exportButton);
        add(Box.createRigidArea(gap));
        add(resizeButton);
       
        add(Box.createRigidArea(gap));
        add(Box.createRigidArea(gap));

	        
        // disables a few tool buttons
        superShape.setEnabled(false);

        // sets tooltip texts
        zoomIn.setToolTipText("Zoom In   Shift-I");
        zoomOut.setToolTipText("Zoom Out   Shift-O");
        measure.setToolTipText("Measure   Shift-M");
        panHand.setToolTipText("Pan   Shift-P");
        selHand.setToolTipText("Selection   Shift-D");
        superShape.setToolTipText("Create a Super Shape   Shift-S");
        investigate.setToolTipText("Investigate    Shift-C");
        exportButton.setToolTipText("Export    Shift-E");
        resizeButton.setToolTipText("Resize Main View    Shift-R");
        
    }

    
// Allows the buttons to change the tool mode and respond to that.    
    private class ToolButton extends JToggleButton implements ToolListener
    {
    	int myMode;
    	Cursor myCursor;
    	
    	
    	ToolButton (int tmode, ImageIcon pic, Cursor csr)
    	{
    		super(pic);
    		myMode = tmode;
    		myCursor = csr;
    		
    		addActionListener(new ActionListener() {
				
				public void actionPerformed(ActionEvent e) {
					ToolManager.setToolMode(myMode);
				}
			});
    		
    		ToolManager.addToolListener(this);
    	}
    	
    	public void toolChanged (int newMode)
    	{
    		if (newMode == myMode){
    			this.setSelected(true);
    			if (Main.testDriver != null){
    				Main.testDriver.mainWindow.setCursor(myCursor);
    			}
    		}
    		else{
    			this.setSelected(false);
    			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    		}
    	}
    	
    	protected void paintComponent(Graphics g){
            Graphics2D g2 = (Graphics2D)g.create();
            g2.setPaint(new GradientPaint(
                    new Point(0, 0), 
                    Color.WHITE, 
                    new Point(0, getHeight()), 
                    Color.PINK.darker()));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();

            super.paintComponent(g);
        }
    }

    public static void addToolListener(ToolListener tl){
    	listeners.add(tl);
    }

    public static boolean removeToolListener(ToolListener tl){
    	return listeners.remove(tl);
    }
    
    public static void notifyToolListeners(){
    	for (ToolListener tl: listeners)
    		tl.toolChanged(Mode);
    }
    
    public static void setToolMode(int newMode){
    	Prev = Mode;
    	Mode = newMode;
    	
    	if(newMode == ToolManager.INVESTIGATE){
    		//Display instruction dialog unless indicated in config file
    		if(Config.get("showInvestigateInstructions").equalsIgnoreCase("true")){
    			JCheckBox chkBox = new JCheckBox("Do not show this message again.");
    			int n = displayInvestigatePopup(chkBox);

    			if(n == JOptionPane.OK_OPTION){	//if Okay, check the checkbox to set config variable
    				boolean dontShow = chkBox.isSelected();
    				if(dontShow){
    					Config.set("showInvestigateInstructions", "false");
    				}
    			}else{      			//if cancel, return to previous toolmode
    				Mode = Prev;
    				setToolMode(Mode);
    				return;
    			}
    		}
    		if(InvestigateFactory.getLviewExists() == false){
        		//Create investigate layer if doesn't exist
    			new InvestigateFactory().createLView(true, null);
    		}else{
    			//if it does exist, make sure it's selected when in this tool mode
    			for(LView lv :LManager.getLManager().viewList){
    				if(lv.getName().equalsIgnoreCase("investigate layer")){
    					LManager.getLManager().setActiveLView(lv);
    				}
    			}
    			
    		}
    		LManager.getLManager().repaint();
    	}
    	notifyToolListeners();
    }
    
    
    private static int displayInvestigatePopup(JCheckBox cb){

    	JLabel welcomeMessage = new JLabel("Welcome to the new Investigate Tool");
		welcomeMessage.setFont(new Font("Dialog",Font.ITALIC+Font.BOLD,16));

		JTextArea message = new JTextArea("\n" +
						 "• While in this mode, a display box will follow the cursor around.\n" +
						 "   This display box shows all the information under the cursor at that\n" +
						 "   point on the screen.\n\n" +
						 "• By default the display will show the list view.  When numeric data\n" +
						 "   is available (with at least two data points at one spot) switching\n" +
						 "   to the chart view will display a chart.  To change views, use the\n" +
						 "   left and right arrow keys.\n\n" +
						 "• Left clicking when a chart is available will save that chart\n" +
						 "   temporarily to the Investigate Layer (which is created and selected\n" +
						 "   whenever the Investigate Tool is selected).  These charts can be\n" +
						 "   viewed and exported by opening up the focus panel for the\n" +
						 "   Investigate Layer.\n\n"+
						 "• For more information see the tutorial page:");
		message.setEditable(false);
		message.setBackground(UIManager.getColor("Panel.background"));
		message.setFont(new Font("Dialog",Font.BOLD,12));
		
		UrlLabel tutorial = new UrlLabel("     https://jmars.mars.asu.edu/investigate-layer");
		
    	JCheckBox chkBox = cb;
		chkBox.setFont(new Font("Dialog", Font.PLAIN, 12));
		
		Object[] params = {welcomeMessage, message, tutorial, chkBox};
		return JOptionPane.showConfirmDialog(investigate, params, "Investigate Tool Instructions", JOptionPane.OK_CANCEL_OPTION);
    }
   
    public static int getToolMode(){
    	return Mode;
    }
    
    public static int getPrevMode(){
    	return Prev;
    }
    
    public static void setGrabHand(){
    	Image grab_hand = Util.loadImage("resources/pandrag_c.png");
		Cursor gh = Toolkit.getDefaultToolkit().createCustomCursor(grab_hand,new Point(0,0),"pandrag");	
		Main.testDriver.mainWindow.setCursor(gh);
	
    }
   
}
