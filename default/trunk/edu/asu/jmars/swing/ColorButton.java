/**
 * A class for a button that defines the color of another object.  
 * The button is displayed in an initial color.  Clicking on it 
 * brings up a color chooser dialog. The color of the
 * button changes to the color selected in this dialog.
 * The color of the button may be accessed by other objects.
 *
 *  @author  James Winburn MSSF-ASU  
 */
package edu.asu.jmars.swing;

// generic java imports.
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JColorChooser;

import edu.asu.jmars.util.stable.ColorCellEditor;

public class ColorButton extends JButton {
	private Color  color;
	private final boolean enableAlpha;
	
	public ColorButton(String l, Color c) {
		this(l, c, false);
	}
	
	public ColorButton(String l, Color c, boolean enableAlpha) {
		super(l);
		this.enableAlpha = enableAlpha;
		setColor(c);
		setFocusPainted( false);
		addActionListener(buttonPressed);
		setOpaque(true);
	}
	
	private final ActionListener buttonPressed = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			if (enableAlpha) {
				ColorCellEditor ce = new ColorCellEditor(color);
				ce.showEditor(ColorButton.this, true);
				if (ce.isInputAccepted()) {
					setColor((Color)ce.getCellEditorValue());
				}
			} else {
				Color newColor = JColorChooser.showDialog (ColorButton.this, getText(), color);
				if (newColor != null){
					setColor( newColor);
				}
			}
		}
	};
	
	// sets the background as the color of the button.  If the color is lighter
	// than gray, then black is used for the color of the button's text instead
	// of white.
	public void setColor(Color c) {
		color = enableAlpha ? c : dupColor(c, 255);
		setBackground( c);
		if (c.getAlpha() < 100 || (c.getRed() + c.getGreen() + c.getBlue()) > (128 + 128 + 128) ) {
			setForeground(Color.black);
		} else {
			setForeground(Color.white);
		}
	}
	
	/**
	 * Special painter that ensures any garbage behind the component is cleared
	 * before the partially transparent component is painted over it.
	 */
	public void paintComponent(Graphics g) {
		g.setColor(Color.white);
		g.clearRect(0, 0, getWidth(), getHeight());
		super.paintComponent(g);
	}
	
	public Color getColor() {
		return color;
	}
	
	private static Color dupColor(Color c, int alpha) {
		return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
	}
} // end: ColorButton

