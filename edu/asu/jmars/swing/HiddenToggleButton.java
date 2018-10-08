package edu.asu.jmars.swing;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.JToggleButton;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

	/**
	 * Provides a toggle button that uses borders to indicate mouse-over, and
	 * that stores the isSelected property on the LView's isVisible property.
	 */
	public class HiddenToggleButton extends JToggleButton {
		public static final Color DEFAULT_BACK = UIManager
				.getColor("Button.background");
		public static final Border borderNone = BorderFactory.createLineBorder(
				DEFAULT_BACK, 2);
		public static final Border borderRaised = BorderFactory
				.createCompoundBorder(borderNone,
						new LineBorder(Color.black, 1));
		public static final Border borderLowered = BorderFactory
				.createCompoundBorder(borderNone,
						new LineBorder(Color.black, 1));

		private Border realBorder;
		private boolean borderActive;

		public HiddenToggleButton(String text, boolean selected) {
			super(text, selected);
			addActionListener(borderChanger);
			addMouseListener(borderActivator);
			// replace the background color (which is a subclass of Color
			// implementing the UIResource interface) with a Color instance that
			// does not implement it, so the JButton UI painter won't fill
			// gradients
			Color b = getBackground();
			setBackground(new Color(b.getRed(), b.getGreen(), b.getBlue(),
					b.getAlpha()));
			setBorder(borderNone);
			this.realBorder = selected ? borderLowered : borderRaised;
		}

		public void updateBorder() {
			realBorder = isSelected() ? borderLowered : borderRaised;
			setBorder(borderActive ? realBorder : borderNone);
		}

		private static final ActionListener borderChanger = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				((HiddenToggleButton) e.getSource()).updateBorder();
			}
		};

		private static final MouseListener borderActivator = new MouseAdapter() {
			public void mouseEntered(MouseEvent e) {
				HiddenToggleButton btn = (HiddenToggleButton) e.getSource();
				btn.borderActive = true;
				btn.updateBorder();
			}

			public void mouseExited(MouseEvent e) {
				HiddenToggleButton btn = (HiddenToggleButton) e.getSource();
				btn.borderActive = false;
				btn.updateBorder();
			}
		};
	}
