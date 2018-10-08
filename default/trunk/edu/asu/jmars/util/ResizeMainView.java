package edu.asu.jmars.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import edu.asu.jmars.Main;
import edu.asu.jmars.layer.MultiProjection;

public class ResizeMainView {
//    public static boolean exporting = false;
    public static int zoomFactor = 1;
    public static Rectangle2D fullExportExtent = null;
        
    static Point2D.Double oldLoc=new Point2D.Double();;
    static Dimension oldWinSize =new Dimension();
    static Dimension maxScreenSize = new Dimension();
    static int oldZoom = -1;
    static int extendedState = -1;
    
    static double currentLRlon = -1;
    static double currentLRlat = -1;
    
    // This is used to save the user's screen settings before starting a main view resize so they can be restored after we're done
    public static void recordOldSizes() {
        oldLoc=(Point2D.Double)Main.testDriver.mainWindow.getLocationManager().getLoc();
        oldWinSize = Main.mainFrame.getSize();
        oldZoom = Main.testDriver.mainWindow.getZoomManager().getZoomPPD();
        extendedState = Main.mainFrame.getExtendedState();
    }
    
    // This is used to restore a user's screen settings after completion (or cancellation) of a main view resize
    public static void restoreOldSizes() {
//        exporting=false;
        Main.testDriver.mainWindow.getZoomManager().setZoomPPD(oldZoom, true);
        Main.testDriver.mainWindow.getLocationManager().setLocation(oldLoc, true);
        Main.mainFrame.setSize(oldWinSize);
        Main.mainFrame.setExtendedState(extendedState);
    }
        
    public static void resize(final Rectangle2D worldBounds) {
        recordOldSizes();
                                        
        final Rectangle2D extentMain = Main.testDriver.mainWindow.getProj().getWorldWindow();
        
        double diffw = worldBounds.getWidth() / extentMain.getWidth();
        double diffh = worldBounds.getHeight() / extentMain.getHeight();
        
        final Point2D center = new Point2D.Double(worldBounds.getCenterX(), worldBounds.getCenterY());
        
        Dimension currentSize = ((MultiProjection)Main.testDriver.mainWindow.getProj()).getScreenSize();

        Dimension totalWinSize = Main.mainFrame.getSize();
        
        // Get screen sizes so they can't be accidentally exceeded
        maxScreenSize = Toolkit.getDefaultToolkit().getScreenSize();
        
        
        Main.mainFrame.setExtendedState(JFrame.NORMAL);
        
        Main.mainFrame.setSize((int)(totalWinSize.getWidth() - (currentSize.getWidth()-(currentSize.getWidth()*diffw))), (int)(totalWinSize.getHeight()-(currentSize.getHeight()-(currentSize.getHeight()*diffh))));

        Main.mainFrame.validate();

        Main.testDriver.mainWindow.getLocationManager().setLocation(center, true);

        adjustView();                         
    }
        
    

    static JTextField upperLeftCorner = new JTextField();
    static JTextField lowerRightCorner = new JTextField();

    static JTextField viewWidth = new JTextField();
    static JTextField viewHeight = new JTextField();
    
    static JLabel message = new JLabel();
    
    static int deltaWidthInPixels = -1;
    static int deltaHeightInPixels = -1;
    
    static Rectangle2D startingWorldWindow = null;
    static Dimension startingWinSize = null;
    static Point2D currentCenter = null;

    
    private abstract static class FieldWork implements ActionListener, FocusListener {
        public void actionPerformed(ActionEvent e) {
            doStuff();
        }
        public void focusGained(FocusEvent e) {
            // no op
        }
        public void focusLost(FocusEvent e) {
            doStuff();
        }       
        
        public abstract void doStuff();
    }
    
    
    private static class AdjustUpperCorner extends FieldWork {
        public void doStuff() {
            String newVal = upperLeftCorner.getText();
            String[] vals = newVal.split(",");
            if(vals.length!=2){
                message.setText("Invalid Entry");
                return;
            }
            
            String newLon = vals[0].trim();
            String newLat = vals[1].trim();
            
            try {
                double lon = Double.parseDouble(newLon);
                double lat = Double.parseDouble(newLat);
                
                Rectangle2D worldWindow = Main.testDriver.mainWindow.getProj().getWorldWindow();

                Point2D newLeft = Main.PO.convSpatialToWorld(360-lon, lat);
                
                double xshift = newLeft.getX() - worldWindow.getMinX();
                double yshift = newLeft.getY() - worldWindow.getMaxY();
                                    
                currentCenter.setLocation(worldWindow.getCenterX()+xshift, worldWindow.getCenterY()+yshift);                 

                message.setText("");
                updateDisplay();
            } catch (NumberFormatException nfe) {
                message.setText("Invalid entry");
            }
        }
    } 
    
    
    private static class AdjustWidth extends FieldWork {
        public void doStuff() {
            String newVal = viewWidth.getText();
            try {
                int width = Integer.parseInt(newVal);
                if (width>0 && width<maxScreenSize.width) {
                    deltaWidthInPixels = width - (int)(startingWorldWindow.getWidth() * Main.testDriver.mainWindow.getZoomManager().getZoomPPD());
                    message.setText("");
                } else {
                    message.setText("Invalid width");
                }
                updateDisplay();
            } catch (NumberFormatException nfe) {
                message.setText("Invalid width");
            }
        }
    }

    private static class AdjustHeight extends FieldWork {
        public void doStuff() {
            String newVal = viewHeight.getText();
            try {
                int height = Integer.parseInt(newVal);
                if (height>0 && height<maxScreenSize.height) {
                    deltaHeightInPixels = height - (int)(startingWorldWindow.getHeight() * Main.testDriver.mainWindow.getZoomManager().getZoomPPD());
                    message.setText("");
                } else {
                    message.setText("Invalid height");
                }
                updateDisplay();
            } catch (NumberFormatException nfe) {
                message.setText("Invalid height");
            }
        }
    }

    
    static {
        
        AdjustUpperCorner ulcorn = new AdjustUpperCorner();
        upperLeftCorner.addActionListener(ulcorn);
        upperLeftCorner.addFocusListener(ulcorn);
        
        lowerRightCorner.setEditable(false);

        AdjustWidth aw = new AdjustWidth();
        viewWidth.addActionListener(aw);
        viewWidth.addFocusListener(aw);
        
        AdjustHeight ah = new AdjustHeight();
        viewHeight.addActionListener(ah);
        viewHeight.addFocusListener(ah);

        Dimension messageSize = new Dimension(200,10);
                
        message.setPreferredSize(messageSize);
        message.setMinimumSize(messageSize);
        message.setSize(messageSize);
        message.setForeground(Color.RED);
    }
    

    
    public static void adjustView() {
        String zoomOptions[] = Main.testDriver.mainWindow.getZoomManager().getExportZoomFactors();
        
        if (zoomOptions==null || zoomOptions.length==0) {
            JOptionPane.showMessageDialog(null, "Sorry, there aren't any higher zoom levels available", "Unable to zoom further", JOptionPane.ERROR_MESSAGE);
            restoreOldSizes();
            return;
        }
        
        SwingUtilities.invokeLater(new Runnable() {     
            public void run() {             
                deltaWidthInPixels=0;
                deltaHeightInPixels=0;
                
                startingWorldWindow=Main.testDriver.mainWindow.getProj().getWorldWindow();
                startingWinSize = Main.mainFrame.getSize();
                
                currentCenter = Main.testDriver.mainWindow.getLocationManager().getLoc();
        
                final JDialog coordConfirmation = new JDialog(Main.mainFrame, true);
                coordConfirmation.setTitle("Adjust location and size of Main View");
                coordConfirmation.setLocationRelativeTo(Main.mainFrame);
                coordConfirmation.setResizable(false);
                
                JPanel p = new JPanel();
                p.setLayout(new BorderLayout());
                
                JPanel gridPanel = new JPanel();
                gridPanel.setLayout(new BoxLayout(gridPanel, BoxLayout.PAGE_AXIS));
                gridPanel.setBorder(BorderFactory.createEmptyBorder(10,10,0,10));
                
                JPanel upperLeftPanel = new JPanel();
                upperLeftPanel.setLayout(new BoxLayout(upperLeftPanel, BoxLayout.LINE_AXIS));
                upperLeftPanel.setBorder(new TitledBorder("Corners (lon, lat)"));
                
                JPanel cornersTxtPanel = new JPanel();
                cornersTxtPanel.setLayout(new GridLayout(2, 2));
                cornersTxtPanel.setBorder(BorderFactory.createEmptyBorder(7, 10, 15, 10));
                JLabel ulLbl = new JLabel("  Upper Left");
                JLabel lrLbl = new JLabel("  Lower Right");
                
                JPanel one = new JPanel();
                one.setLayout(new BoxLayout(one, BoxLayout.LINE_AXIS));
                upperLeftCorner.setColumns(8);
                one.add(upperLeftCorner);
                one.add(Box.createHorizontalStrut(4));
                JPanel two = new JPanel();
                two.setLayout(new BoxLayout(two, BoxLayout.LINE_AXIS));
                two.add(Box.createHorizontalStrut(4));
                lowerRightCorner.setColumns(8);
                two.add(lowerRightCorner);
                
                cornersTxtPanel.add(ulLbl);
                cornersTxtPanel.add(lrLbl);
                cornersTxtPanel.add(one);
                cornersTxtPanel.add(two);
                
                upperLeftPanel.add(cornersTxtPanel);
                upperLeftPanel.add(ResizeMainView.createNudgePanel());               
                upperLeftPanel.add(Box.createHorizontalStrut(10));
                
                JPanel widthPanel = new JPanel();
                widthPanel.setBorder(BorderFactory.createTitledBorder("View width (pixels)"));
                viewWidth.setColumns(12);
                widthPanel.add(viewWidth);
                widthPanel.add(ResizeMainView.createWidthPanel());

                        
                JPanel heightPanel = new JPanel();
                heightPanel.setBorder(BorderFactory.createTitledBorder("View height (pixels)"));
                viewHeight.setColumns(12);
                heightPanel.add(viewHeight);
                heightPanel.add(ResizeMainView.createHeightPanel());

                JPanel sizePanel = new JPanel();
                sizePanel.setLayout(new BoxLayout(sizePanel, BoxLayout.LINE_AXIS));
                sizePanel.add(widthPanel);
                gridPanel.add(Box.createRigidArea(new Dimension(10,0)));
                sizePanel.add(heightPanel);
                
                gridPanel.add(upperLeftPanel);
                gridPanel.add(Box.createVerticalStrut(10));
                gridPanel.add(sizePanel);
                
                p.add(gridPanel, BorderLayout.CENTER);
                
                JPanel dialogButtons = new JPanel();
                
                JButton ok = new JButton("Okay");
                
                final boolean keepGoing[]=new boolean[1];
                
                ok.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        keepGoing[0]=true;
                        coordConfirmation.setVisible(false);
                    }
                });
                
                JButton cancel = new JButton("Cancel");
                
                cancel.addActionListener(new ActionListener(){
                    public void actionPerformed(ActionEvent e) {
                        keepGoing[0]=false;
                        coordConfirmation.setVisible(false);
                    }
                });

                BoxLayout buttonLayout = new BoxLayout(dialogButtons, BoxLayout.LINE_AXIS);
                dialogButtons.setLayout(buttonLayout);
                dialogButtons.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                dialogButtons.add(message);
                dialogButtons.add(Box.createHorizontalGlue());
                dialogButtons.add(Box.createRigidArea(new Dimension(10,0)));
                dialogButtons.add(ok);
                dialogButtons.add(Box.createRigidArea(new Dimension(10,0)));
                dialogButtons.add(cancel);
                
                p.add(dialogButtons, BorderLayout.SOUTH);
            
                coordConfirmation.add(p);
                coordConfirmation.pack();
        
                updateDisplay();
                coordConfirmation.setVisible(true);
                
                if (!keepGoing[0]) {
                    restoreOldSizes();
                    return;
                } else {
                    return;
                }
                
//                pickExportZoom();
            }
        });
    }
    
    private static void updateDisplay() {
        SwingUtilities.invokeLater(new Runnable(){
            public void run() {
                
                int newW = (int)startingWinSize.width + deltaWidthInPixels;
                int newH = (int)startingWinSize.height + deltaHeightInPixels;

                //Check to see if the new boundaries are within screen limits firstl
                if (newW>maxScreenSize.width){
                    message.setText("Invalid width");
                    return;
                }
                if (newH>maxScreenSize.height){
                    message.setText("Invalid height");
                    return;
                }
                //--------------------------------------------------------------
                
                
                int widthInPixels = (int)(startingWorldWindow.getWidth() * Main.testDriver.mainWindow.getZoomManager().getZoomPPD()) + deltaWidthInPixels;
                int heightInPixels = (int)(startingWorldWindow.getHeight() * Main.testDriver.mainWindow.getZoomManager().getZoomPPD()) + deltaHeightInPixels;
                        
                viewWidth.setText(""+widthInPixels);
                viewHeight.setText(""+heightInPixels);      
                    
                Point2D offset = new Point2D.Double(currentCenter.getX()+((0.5 * deltaWidthInPixels)/ Main.testDriver.mainWindow.getZoomManager().getZoomPPD()),
                                                    currentCenter.getY()-((0.5 * deltaHeightInPixels)/ Main.testDriver.mainWindow.getZoomManager().getZoomPPD()));

                Main.testDriver.mainWindow.getLocationManager().setLocation(offset, true);
                
                Main.mainFrame.setSize(newW, newH);

                Main.mainFrame.validate();
                
                // Reset our values, since we've now changed the screen parameters (added 3/8/2017 to fix glaring bug)
                deltaWidthInPixels=0;
                deltaHeightInPixels=0;              
                startingWorldWindow=Main.testDriver.mainWindow.getProj().getWorldWindow();
                startingWinSize = Main.mainFrame.getSize();
                currentCenter = Main.testDriver.mainWindow.getLocationManager().getLoc();
                
                SwingUtilities.invokeLater(new Runnable(){
                    public void run() {
                        Rectangle2D worldWindow=Main.testDriver.mainWindow.getProj().getWorldWindow();
        
                        Point2D upperLeft = Main.PO.convWorldToSpatial(worldWindow.getMinX(), worldWindow.getMaxY());
                        Point2D lowerRight =  Main.PO.convWorldToSpatial(worldWindow.getMaxX(), worldWindow.getMinY());
                        
                        DecimalFormat df = Main.getFormatter(1);
                        
                        try {
                            String ullon = df.format(360 - upperLeft.getX());
                            String ullat = df.format(upperLeft.getY());
                            upperLeftCorner.setText(ullon+", "+ullat);
                            
                            String lrlon = df.format(360 - lowerRight.getX());
                            String lrlat = df.format(lowerRight.getY());
                            lowerRightCorner.setText(lrlon+", "+lrlat);
                                                        
                            currentLRlon = Double.parseDouble(df.format(360-lowerRight.getX()));
                            currentLRlat = Double.parseDouble(df.format(lowerRight.getY()));
                            
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
        
    
    }


    public static  JPanel createNudgePanel(){

        JPanel nudgeCursorPanel = new JPanel();
        nudgeCursorPanel.setLayout(new GridLayout(3,3));
        nudgeCursorPanel.setMaximumSize(new Dimension(60,60));
        nudgeCursorPanel.setBorder(new EmptyBorder(0, 0, 2, 0));
        
        // -------------------------------------------
        // Step up the nudge buttons
        // -------------------------------------------
        
        // define the button dimensions
        Dimension buttonDim = new Dimension(20,20);
        
        JButton left = new JButton(new ImageIcon(Main.getResource("resources/pan_w.gif")));
        left.setPreferredSize(buttonDim);
        JButton right = new JButton(new ImageIcon(Main.getResource("resources/pan_e.gif")));
        right.setPreferredSize(buttonDim);
        JButton up = new JButton(new ImageIcon(Main.getResource("resources/pan_n.gif")));
        up.setPreferredSize(buttonDim);
        JButton down = new JButton(new ImageIcon(Main.getResource("resources/pan_s.gif")));
        down.setPreferredSize(buttonDim);
        final JButton stepToggle = new JButton(new ImageIcon(Main.getResource("resources/dot.gif")));
        stepToggle.setPreferredSize(buttonDim);
        
        left.setFocusable(false);
        right.setFocusable(false);
        up.setFocusable(false);
        down.setFocusable(false);
        stepToggle.setFocusable(false);
                
        left.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                currentCenter = new Point2D.Double(currentCenter.getX()+(1.0/ Main.testDriver.mainWindow.getZoomManager().getZoomPPD()),currentCenter.getY());               
                updateDisplay();
            }
        });
        
        right.addActionListener(new ActionListener() {              
            public void actionPerformed(ActionEvent e) {
                currentCenter = new Point2D.Double(currentCenter.getX()-(1.0/ Main.testDriver.mainWindow.getZoomManager().getZoomPPD()),currentCenter.getY());               
                updateDisplay();
            }
        });
        
        up.addActionListener(new ActionListener() {             
            public void actionPerformed(ActionEvent e) {
                currentCenter = new Point2D.Double(currentCenter.getX(),currentCenter.getY()-(1.0/ Main.testDriver.mainWindow.getZoomManager().getZoomPPD()));
                updateDisplay();
            }
        });
        
        down.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                currentCenter = new Point2D.Double(currentCenter.getX(),currentCenter.getY()+(1.0/ Main.testDriver.mainWindow.getZoomManager().getZoomPPD()));               
                updateDisplay();
            }
        });
        
        nudgeCursorPanel.add(new JLabel());
        nudgeCursorPanel.add(up);
        nudgeCursorPanel.add(new JLabel());
        nudgeCursorPanel.add(left);
        nudgeCursorPanel.add(stepToggle);
        nudgeCursorPanel.add(right);
        nudgeCursorPanel.add(new JLabel());
        nudgeCursorPanel.add(down);
        nudgeCursorPanel.add(new JLabel());
                
        return nudgeCursorPanel;
    }

    public static  JPanel createWidthPanel(){

        JPanel nudgeCursorPanel = new JPanel();
        nudgeCursorPanel.setLayout(new GridLayout(2,1));
        nudgeCursorPanel.setMaximumSize(new Dimension(60,60));
        
        // -------------------------------------------
        // Step up the nudge buttons
        // -------------------------------------------
        
        // define the button dimensions
        Dimension buttonDim = new Dimension(20,20);
        
        JButton left = new JButton(new ImageIcon(Main.getResource("resources/pan_n.gif")));
        left.setPreferredSize(buttonDim);
        JButton right = new JButton(new ImageIcon(Main.getResource("resources/pan_s.gif")));
        right.setPreferredSize(buttonDim);
        
        left.setFocusable(false);
        right.setFocusable(false);
                
        left.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                deltaWidthInPixels++;
                updateDisplay();
            }
        });
        
        right.addActionListener(new ActionListener() {              
            public void actionPerformed(ActionEvent e) {
                deltaWidthInPixels--;
                updateDisplay();
            }
        });
                
        nudgeCursorPanel.add(left);
        nudgeCursorPanel.add(right);
                
        return nudgeCursorPanel;
    }

    public static  JPanel createHeightPanel(){

        JPanel nudgeCursorPanel = new JPanel();
        nudgeCursorPanel.setLayout(new GridLayout(2,1));
        nudgeCursorPanel.setMaximumSize(new Dimension(60,60));
        
        // -------------------------------------------
        // Step up the nudge buttons
        // -------------------------------------------
        
        // define the button dimensions
        Dimension buttonDim = new Dimension(20,20);
        
        JButton up = new JButton(new ImageIcon(Main.getResource("resources/pan_n.gif")));
        up.setPreferredSize(buttonDim);
        JButton down = new JButton(new ImageIcon(Main.getResource("resources/pan_s.gif")));
        down.setPreferredSize(buttonDim);
        
        up.setFocusable(false);
        down.setFocusable(false);
                        
        up.addActionListener(new ActionListener() {             
            public void actionPerformed(ActionEvent e) {
                deltaHeightInPixels++;
                updateDisplay();
            }
        });
        
        down.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                deltaHeightInPixels--;
                updateDisplay();
            }
        });
        
        nudgeCursorPanel.add(up);
        nudgeCursorPanel.add(down);
                
        return nudgeCursorPanel;
    }

}
