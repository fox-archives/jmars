package edu.asu.jmars.swing;

import javax.swing.JLabel;

/*
 * This class extends the JLabel component in order to convert \t characters into what
 * seems to be a reasonable amount of spaces.
 * 
 */

public class TabLabel extends JLabel {
	public TabLabel(String text) {
		super(text);
	}
	
	public void setText(String newText) {
		while(newText.indexOf('\t')>0) {
			newText=newText.substring(0, newText.indexOf('\t'))+
			"              "
			+newText.substring(newText.indexOf('\t')+1);
		}
		super.setText(newText);
	}
}