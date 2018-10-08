package edu.asu.jmars.graphics;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import edu.asu.jmars.util.Util;

/**
 * This is a swing component that allows the user to pick a font type, size,
 * style, and color.
 */

public class JFontChooser {
	public static final Font defaultFont = UIManager.getDefaults().getFont("MenuBar.font");
	public static final String[] sizes = new String[] { "8", "10", "12", "14", "16", "18", "20", "22", "24", "30", "36", "48", "72" };
	public static final String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
	public static final String[] fontStyles = new String[] { "Bold", "Italic", "Plain" };
	public static final Integer[] fontStyleIDs = {Font.BOLD, Font.ITALIC, Font.PLAIN};
	
	/**
	 * Adds an action listener for when the dialog is closed, handles the actual
	 * return of this super class.
	 */
	private static class FontTracker implements ActionListener {
		private JFontChooser chooser;
		private Font font;

		public FontTracker(JFontChooser c) {
			chooser = c;
		}

		public void actionPerformed(ActionEvent e) {
			font = chooser.getSelectedFont();
		}

		/**
		 * @return returns the font that is selected, if dialog is canceled,
		 *         then returns the default font.
		 */
		public Font getFont() {
			if (font == null) {
				return defaultFont;
			}
			return font;
		}
	}

	/**
	 * selectedFont is the font that is selected in this instance of
	 * JFontChooser fontColor is the font color that is chosen by the user in
	 * this instance of JFontChooser outlineColor is the outline color that is
	 * chosen by the user in this instance of JFontChooser
	 */
	public Font selectedFont;
	public Color fontColor, outlineColor;
	private Color lastOutline, lastFont;
	private FontRenderer graphicsPane;
	private JButton okButton, cancelButton, fontColorButton, outlineColorButton;
	private int fontSize, fontStyle;
	private String fontName;
	private JDialog dialog;
	private JComboBox fontComboBox, sizeComboBox, fontStyleComboBox;
	private JCheckBox fillOnlyCheckbox, outlineOnlyCheckbox;
	private boolean wasCanceled = false;
	
	/**
	 * @param initialFont
	 * @param outline
	 * @param fill
	 */
	public JFontChooser(Font initialFont, Color outline, Color fill) {
		selectedFont = initialFont;
		fontColor = fill;
		lastFont = fill;
		outlineColor = outline;
		lastOutline = outline;
		fontSize = initialFont.getSize();
		fontName = initialFont.getFamily();
		fontStyle = initialFont.getStyle();
		graphicsPane = new FontRenderer(selectedFont, outlineColor, fontColor);
		graphicsPane.setLabel("AaBbCcDd-1234");
		graphicsPane.setAntiAlias(true);
	}
	
	/**
	 * @param enableOutline Set if user should be able to select a font outline
	 */
	public JFontChooser() {
		this(defaultFont, Color.black, Color.white);
	}

	/**
	 * Handles the actual construction of the dialog
	 * 
	 * @param ok
	 *            The listener that should be used on the user clicking the "OK"
	 *            button (FontListener).
	 * @return returns the dialog to the showdialog() method
	 */
	private void createDialog(Component c, ActionListener ok) {
		if (dialog != null) {
			return;
		}
		
		fontComboBox = new JComboBox(fonts);
		int fontFamilyIdx = Arrays.asList(fonts).indexOf(selectedFont.getFamily());
		if (fontFamilyIdx >= 0) {
			fontComboBox.setSelectedIndex(fontFamilyIdx);
		}
		
		sizeComboBox = new JComboBox(sizes);
		int fontSizeIdx = Arrays.asList(sizes).indexOf(""+fontSize);
		if (fontSizeIdx >= 0) {
			sizeComboBox.setSelectedIndex(fontSizeIdx);
		}
		
		fontStyleComboBox = new JComboBox(fontStyles);
		int fontStyleIdx = Arrays.asList(fontStyleIDs).indexOf(fontStyle);
		if (fontStyleIdx >= 0) {
			fontStyleComboBox.setSelectedIndex(fontStyleIdx);
		}
		
		fontColorButton = new JButton("          ");
		outlineColorButton = new JButton("          ");
		outlineColorButton.setBackground(outlineColor);
		fontColorButton.setBackground(fontColor);
		
		ActionListener fontColorListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Color newColor = JColorChooser.showDialog((Component)e.getSource(), "Pick A Color", fontColor);
				if (newColor != null) {
					fontColor = newColor;
					fontColorButton.setBackground(fontColor);
					graphicsPane.setForeground(fontColor);
					lastFont = fontColor;
				}
			}
		};
		
		ActionListener outlineColorListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Color newColor = JColorChooser.showDialog((Component)e.getSource(), "Pick A Color", outlineColor);
				if (newColor != null) {
					outlineColor = newColor;
					outlineColorButton.setBackground(outlineColor);
					graphicsPane.setOutlineColor(outlineColor);
					lastOutline = outlineColor;
				}
			}
		};
		
		outlineColorButton.addActionListener(outlineColorListener);
		fontColorButton.addActionListener(fontColorListener);
		fillOnlyCheckbox = new JCheckBox("Fill");
		outlineOnlyCheckbox = new JCheckBox("Outline");
		fillOnlyCheckbox.setSelected(true);
		outlineOnlyCheckbox.setSelected(true);

		if (fontColor == null) {
			fillOnlyCheckbox.setSelected(false);
			fontColorButton.setEnabled(false);

		}
		if (outlineColor == null) {
			outlineOnlyCheckbox.setSelected(false);
			outlineColorButton.setEnabled(false);
		}
		
		ActionListener cbListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (fillOnlyCheckbox.isSelected() && outlineOnlyCheckbox.isSelected()) {
					// Fill and outline
					if (fontColor == null) {
						if (lastFont == null)
							lastFont = Color.white;
						fontColor = lastFont;
					}
					if (outlineColor == null) {
						if (lastOutline == null)
							lastOutline = Color.black;
						outlineColor = lastOutline;
					}

					fontColorButton.setEnabled(true);
					fontColorButton.setBackground(fontColor);
					outlineColorButton.setEnabled(true);
					outlineColorButton.setBackground(outlineColor);
				} else if (fillOnlyCheckbox.isSelected() && !outlineOnlyCheckbox.isSelected()) {
					// Fill and NO outline
					if (fontColor == null) {
						if (lastFont == null)
							lastFont = Color.white;
						fontColor = lastFont;
					}
					fontColorButton.setEnabled(true);
					outlineColorButton.setEnabled(false);
					outlineColor = null;
				} else if (!fillOnlyCheckbox.isSelected() && outlineOnlyCheckbox.isSelected()) {
					// No Fill, Only Outline
					if (outlineColor == null) {
						if (lastOutline == null)
							lastOutline = Color.black;
						outlineColor = lastOutline;
					}
					fontColorButton.setEnabled(false);
					outlineColorButton.setEnabled(true);
					fontColor = null;
				} else if (!fillOnlyCheckbox.isSelected() && !outlineOnlyCheckbox.isSelected()) {
					//Both Deselected
					fillOnlyCheckbox.setSelected(true);
					outlineOnlyCheckbox.setSelected(true);
					if (lastOutline == null)
						lastOutline = Color.black;
					if (lastFont == null)
						lastFont = Color.white;
					fontColor = lastFont;
					outlineColor = lastOutline;

					fontColorButton.setEnabled(true);
					fontColorButton.setBackground(fontColor);
					outlineColorButton.setBackground(outlineColor);
					outlineColorButton.setEnabled(true);
				}
				graphicsPane.setOutlineColor(outlineColor);
				graphicsPane.setForeground(fontColor);
			}
		};
		
		fillOnlyCheckbox.addActionListener(cbListener);
		outlineOnlyCheckbox.addActionListener(cbListener);
		
		okButton = new JButton("Ok");
		cancelButton = new JButton("Cancel");
		okButton.setPreferredSize(cancelButton.getPreferredSize());

		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (e.getSource() == cancelButton) {
					wasCanceled = true;
					dialog.dispose();
				}
			}
		});

		okButton.setActionCommand("OK");
		if (ok != null) {
			okButton.addActionListener(ok);
		}
		
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.dispose();
			}
		});

		ActionListener fontListener = new ActionListener() {// listen for a
			// change in the font list/size list
			public void actionPerformed(ActionEvent e) {
				if (e.getSource() == sizeComboBox) {
					fontSize = Integer.valueOf(sizeComboBox.getSelectedItem().toString());
				} else if (e.getSource() == fontComboBox) {
					String fontSelected = fontComboBox.getSelectedItem().toString();
					fontName = fontSelected;
				} else if (e.getSource() == fontStyleComboBox) {
					if (fontStyleComboBox.getSelectedItem().toString() == "Plain") {
						fontStyle = Font.PLAIN;
					} else if (fontStyleComboBox.getSelectedItem().toString() == "Italic") {
						fontStyle = Font.ITALIC;
					} else if (fontStyleComboBox.getSelectedItem().toString() == "Bold") {
						fontStyle = Font.BOLD;

					}
				}
				selectedFont = new Font(fontName, fontStyle, fontSize);
				graphicsPane.setFont(selectedFont);
			}
		};
		
		fontStyleComboBox.addActionListener(fontListener);
		sizeComboBox.addActionListener(fontListener);
		fontComboBox.addActionListener(fontListener);
		
		JPanel buttonPanel = new JPanel();// panel that contains the ok and cancel buttons
		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);
		
		int pad = 4;
		
		Box fontControls = Box.createHorizontalBox();
		fontControls.add(sizeComboBox);
		fontControls.add(Box.createHorizontalStrut(pad));
		fontControls.add(fontStyleComboBox);
		fontControls.add(Box.createHorizontalStrut(pad));
		fontControls.add(fontComboBox);
		
		JPanel fontPanel = new JPanel(new GridBagLayout());
		Insets in = new Insets(pad,pad,pad,pad);
		fontPanel.add(fontControls, new GridBagConstraints(0, 0, 4, 1, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, in, pad, pad));
		fontPanel.add(graphicsPane, new GridBagConstraints(0, 1, 4, 1, 1, 1, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, in, pad, pad));
		fontPanel.add(fillOnlyCheckbox, new GridBagConstraints(0, 2, 1, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, in, pad, pad));
		fontPanel.add(fontColorButton, new GridBagConstraints(1, 2, 1, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, in, pad, pad));
		fontPanel.add(outlineOnlyCheckbox, new GridBagConstraints(0, 3, 1, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, in, pad, pad));
		fontPanel.add(outlineColorButton, new GridBagConstraints(1, 3, 1, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, in, pad, pad));
		fontPanel.add(okButton, new GridBagConstraints(3, 2, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, in, pad, pad));
		fontPanel.add(cancelButton, new GridBagConstraints(3, 3, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, in, pad, pad));
		
		// If we are given a frame to set this relative to, pass it to the JDialog.  Otherwise pass null
		JFrame frame = null;
		if (c instanceof JFrame) {
			frame = (JFrame) c;
		}	
		
		dialog = new JDialog(frame, "Font Picker", true);
		dialog.add(fontPanel);
		dialog.setLocationRelativeTo(c);
	    Font oldFont = graphicsPane.getFont();
	    graphicsPane.setFont(new Font(fontName, fontStyle, 30));
		dialog.pack();
		graphicsPane.setFont(oldFont);
		Util.addEscapeDisposesAction(dialog);
		dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	}
	
	public static void main(String[] args) {
		new JFontChooser().showDialog(null);
	}
	
	/**
	 * @return Color that is chosen
	 */
	public Color getFontColor() {
		return fontColor; // returns the font color
	}

	/**
	 * @return Outline Color that is chosen (null if outline is disabled)
	 */
	public Color getOutlineColor() {
		return outlineColor; // returns the outline color
	}

	/**
	 * @return gets the selected font, same as the return from "showDialog()"
	 */
	public Font getSelectedFont() {
		return selectedFont;// returns the selected font
	}

	/**
	 * Shows a FontChooser Dialog
	 * 
	 * @return Font that is selected in the dialog, null if dialog was canceled
	 */
	public Font showDialog(Component c) {
		final JFontChooser pane = this;
		FontTracker ok = new FontTracker(pane);
		createDialog(c, ok);
		dialog.setVisible(true);
		if (!wasCanceled) {
			return ok.getFont();
		} else {
			return null;
		}
	}
}
