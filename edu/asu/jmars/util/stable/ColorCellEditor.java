package edu.asu.jmars.util.stable;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.EventObject;

import javax.swing.Box;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableCellEditor;

import edu.asu.jmars.swing.AbstractCellEditor;

/**
 * TableCellEditor for Color objects.
 */
public class ColorCellEditor extends AbstractCellEditor implements TableCellEditor {
	private Color color = Color.white;
	private JSlider alphaSlider = new JSlider(0,255,255);
	private JColorChooser colorChooser = new JColorChooser();
	private JDialog colorChooserDialog;
	private ColorCellRenderer renderer;
	private boolean shown;
	private boolean acceptedInput = false;
	
	public ColorCellEditor() {
		this(Color.white);
	}
	
	public ColorCellEditor(Color defaultColor) {
		super();
		setColor(defaultColor);
		
		ActionListener okHandler = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// We reach here when user presses OK in the color chooser.
				// If the user did select a color, make it the current color.
				if (colorChooser.getColor() != null) {
					color = colorChooser.getColor();
					acceptedInput = true;
				} else {
					acceptedInput = false;
				}
				
				// Mark the end of editing operation.
				fireEditingStopped();
			}
		};
		ActionListener cancelHandler = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				acceptedInput = false;
				fireEditingCanceled();
			}
		};
		
		colorChooserDialog = JColorChooser.createDialog(renderer, "Color Chooser",
			true, colorChooser, okHandler, cancelHandler);
		
		final JLabel alphaValue = new JLabel();
		alphaSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				alphaValue.setText(alphaSlider.getValue()*100/255 + "% opaque");
			}
		});
		alphaValue.setText(alphaSlider.getValue()*100/255 + "% opaque");
		
		Box alphaBox = Box.createHorizontalBox();
		alphaBox.setBorder(new EmptyBorder(8,8,8,8));
		alphaBox.add(alphaSlider);
		alphaBox.add(Box.createHorizontalStrut(4));
		alphaBox.add(alphaValue);
		alphaBox.add(Box.createHorizontalGlue());
		colorChooserDialog.getContentPane().add(alphaBox, BorderLayout.NORTH);
		
		// Intercepts paint() to make sure we show the editor when we paint the
		// color editor's renderer -- we won't do this for tooltips, but we
		// always do it for a color cell we are editing.
		renderer = new ColorCellRenderer() {;
			public void paint(Graphics g) {
				super.paint(g);
				showEditor(this, false);
			}
		};
	}
	
	/** Sets the color field and alpha slider */
	public void setColor(Color color) {
		alphaSlider.setValue(color.getAlpha());
		color = new Color(color.getRed(), color.getGreen(), color.getBlue(), 255);
		colorChooser.setColor(color);
	}
	
	/** @return true if the dialog was closed by the user pressing okay, false otherwise. */
	public boolean isInputAccepted() {
		return acceptedInput;
	}
	
	public static void main(String[] args) {
		new ColorCellEditor().showEditor(null, true);
	}
	
	/**
	 * Shows the editor if we have not already shown it in this editing session.
	 * The editor is created on the AWT thread, at some later time, so that the
	 * various threads that lead here can finish their work without waiting on
	 * the popup dialog.
	 **/
	public void showEditor(final Component parent, boolean block) {
		if (!shown) {
			shown = true;
			Runnable todo = new Runnable() {
				public void run() {
					colorChooserDialog.setLocationRelativeTo(parent);
					acceptedInput = false;
					colorChooserDialog.setVisible(true);
				}
			};
			if (block) {
				todo.run();
			} else {
				SwingUtilities.invokeLater(todo);
			}
		}
	}
	
	public boolean isCellEditable(EventObject e) {
		if (e instanceof MouseEvent) {
			MouseEvent m = (MouseEvent)e;
			return SwingUtilities.isLeftMouseButton(m) && m.getClickCount() == 2;
		} else {
			return false;
		}
	}
	
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		if (!(value instanceof Color)) {
			value = Color.white;
		}
		setColor((Color)value);
		shown = false;
		return renderer.getTableCellRendererComponent(table, value, isSelected, renderer.isFocusOwner(), row, column);
	}
	
	public Object getCellEditorValue() {
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), alphaSlider.getValue());
	}
	
	public boolean shouldSelectCell(EventObject evt){
		return true;
	}
}
