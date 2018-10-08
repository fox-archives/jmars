package edu.asu.jmars.layer;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import edu.asu.jmars.util.Util;

public class FocusPanel extends JTabbedPane
{
	public FocusInfoPanel infoPanel;
	public JPanel dockMe;
	public JFrame parentFrame = new JFrame();
	
	public boolean isLimited;
	public Layer.LView parent;

	public int selectedIndex;
	
	public static boolean dockByDefault=true;
	
	public FocusPanel(Layer.LView parent) {
		this(parent, false);
	}
	
	public FocusPanel(Layer.LView parent, boolean isLimited){
		this.parent = parent;
		this.isLimited = isLimited;
		infoPanel = new FocusInfoPanel(parent,isLimited);
		dockMe = new JPanel();
		
		if(parent==null){
			
			parentFrame.setTitle("Blank Layer");
		}
		else{
			parentFrame.setTitle(parent.getName()+" Options");
		}
		parentFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		this.setTabPlacement(JTabbedPane.BOTTOM);
		
		this.add("Info", infoPanel);
//		this.addTab("Dock Me", dockMe);
		
		MouseListener dockTabListener = new MouseAdapter() {
        	public void mousePressed(MouseEvent e) {
        		int tabnumber = indexAtLocation(e.getX(), e.getY());  		
        		if(tabnumber == -1)
        			return;
        		if (getComponentAt(tabnumber) == dockMe){
        			dock(true);
        		}
        		else{
        			selectedIndex = tabnumber;
        		}
        	}
		};

		if (selectedIndex<getComponentCount()) {
			setSelectedIndex(selectedIndex);
		}
	    addMouseListener(dockTabListener);
	
//	    if (dockByDefault) dock(false);
	}
	
	boolean docked=false;
	public void dock(boolean selectTab) {
		String name = parentFrame.getTitle();
		// focus panel
		remove(dockMe);
		if (selectedIndex>0 && selectedIndex<=getComponentCount()) {
			setSelectedIndex(selectedIndex);
		}
		parentFrame.setVisible(false);
		
		// lmanager
		LManager.getLManager().dockTab(name, parent.light2, FocusPanel.this, selectTab);
		docked=true;
	}
	
	public boolean isDocked() {
		return docked;
	}
	
	// These are here to override JTabbedPane's method to make sure
	// that the dockMe tab is the last one.

	public Component add(String title, Component component) {
		
		int dockLocation = indexOfComponent(dockMe);
		if(dockLocation==-1)
			super.add(title, component);
		else{
			super.add(component, dockLocation);
			super.setTitleAt(dockLocation, title);
		}
	        return component;
	}
	
    public void addTab(String title, Component component) {
    	
    	int dockLocation = indexOfComponent(dockMe);
    	if (dockLocation == -1)
    		super.addTab(title, component);
    	else{
    		 insertTab(title, null, component, null, dockLocation); 
    	}
	
    }
    
    public JFrame getFrame(){
    	return parentFrame;
    }

    public void restoreInFrame(int locX, int locY, int width, int height) {
    	this.addTab("Dock Me", dockMe);
		parentFrame.setContentPane(this);
		
		parentFrame.setLocation(locX, locY);
		parentFrame.setSize(width, height);		
		parentFrame.setIconImage(Util.getJMarsIcon());
		parentFrame.setVisible(true);
		docked=false;
    }

    private boolean hasBeenInitialized = false;
    public void showInFrame() {
    	this.addTab("Dock Me", dockMe);
		parentFrame.setContentPane(this);
		if(!hasBeenInitialized){
			parentFrame.pack();
			hasBeenInitialized=true;
		}
		parentFrame.setIconImage(Util.getJMarsIcon());
		// If it's minimized, this will bring the window back up.
		if(parentFrame.getState()!=JFrame.NORMAL) { parentFrame.setState(JFrame.NORMAL); }
		parentFrame.toFront();	
		parentFrame.setVisible(false);
		parentFrame.setVisible(true);
		docked=false;
    }
	
	
}
