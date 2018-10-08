package edu.asu.jmars.swing;

import edu.asu.jmars.util.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.net.*;
import javax.swing.*;

public class UrlLabel extends JLabel
 {
	private static DebugLog log = DebugLog.instance();

	private String url;
	private String plain;
	private String under;

	public UrlLabel(URL url)
	 {
		this(url.toString(), null);
	 }
	public UrlLabel(String s){
		this(s, null);
	}
	public UrlLabel(String s, String color)
	 {
		this.url = s;
		if(color==null){
			plain = "<html><pre><font color=#0000CC><font face=\"arial\">"+s+"</font></color></pre>";
			under = "<html><pre><font color=#0000CC><u><font face=\"arial\">"+s+"</font></u></color></pre>";
		}else{
			plain = "<html><pre><font color="+color+"><font face=\"arial\">"+s+"</font></color></pre>";
			under = "<html><pre><font color="+color+"><u><font face=\"arial\">"+s+"</font></u></color></pre>";
		}
		
		setText(plain);
		url = url.trim(); //make sure there's no white space in the url (most likely preceeding or following)
		addMouseListener(
			new MouseAdapter()
			 {
				Font oldFont = getFont();
				public void mouseClicked(MouseEvent e)
				 {
					if(SwingUtilities.isLeftMouseButton(e)){
						try{
							Util.launchBrowser(url);
						}catch(Exception ex){
							log.aprintln(ex);
							log.aprintln(url);
							JOptionPane.showMessageDialog(
								UrlLabel.this,
								"Unable to open browser due to:\n" + ex,
								"JMARS",
								JOptionPane.ERROR_MESSAGE);
						}
					}
					if(SwingUtilities.isRightMouseButton(e)){
						//Show right-click popup menu (has copy item)
						showMenu(e.getX(), e.getY());
					}
				 }
				public void mouseEntered(MouseEvent ev)
				 {
					setText(under);
				 }
				public void mouseExited(MouseEvent e)
				 {
					setText(plain);
				 }
			 }
			);
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	 }
	

	
	//Builds and displays the copy popup menu
	private void showMenu(int x, int y){
		JPopupMenu rcMenu = new JPopupMenu();
		JMenuItem copyItem = new JMenuItem(copyAct);
		rcMenu.add(copyItem);
		
		rcMenu.show(this, x, y);
	}
	//Copy url string to clipboard
	private Action copyAct = new AbstractAction("Copy url text"){
		public void actionPerformed(ActionEvent e) {
			StringSelection copyString = new StringSelection(url);
			Clipboard cboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			cboard.setContents(copyString, null);
		}
	};
 }
