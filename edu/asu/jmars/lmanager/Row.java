package edu.asu.jmars.lmanager;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.SoftBevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MouseInputListener;

import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.swing.HiddenToggleButton;
import edu.asu.jmars.util.Config;

public class Row extends JPanel {
	
	private static Border selectedBorder = new SoftBevelBorder(Config.get("lmanager.raised", true) ? BevelBorder.RAISED : BevelBorder.LOWERED);
	private static Border selectedBorder2 = new LineBorder(Color.blue, // FOCUS_COLOR
			selectedBorder.getBorderInsets(null).top);
	
	static final int PAD = 2;

	final Layer.LView view;
	JComponent light;
	HiddenToggleButton btnM;
	HiddenToggleButton btnP;
	JLabel label;
	JSlider slider;
	JPanel bottom;
	Border outsideBorder = BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY);
	CompoundBorder compoundBorderSelected = new CompoundBorder(this.outsideBorder, selectedBorder2);
	CompoundBorder compoundBorderUnSelected = null;//initialized in the constructor
	Border insideBorder;//initialized in constructor

	public Row(final Layer.LView view, MouseInputListener rowMouseHandler) {
		this.view = view;
		this.light = view.getLight();
		this.btnM = new HiddenToggleButton("M", view.isVisible());
		this.btnP = new HiddenToggleButton("P", view.getChild().isVisible());
		this.label = new JLabel(" " + LManager.getLManager().getUniqueName(view));
		this.slider = new JSlider(0, 1000);

		slider.setFocusable(false);
		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				view.setAlpha((float) slider.getValue()
						/ slider.getMaximum());
				slider.setToolTipText("Opacity: "
						+ (int) Math.round(slider.getValue() * 100d
								/ slider.getMaximum()) + "%");
			}
		});
		slider.setValue((int) (view.getAlpha() * 1000));

		label.addMouseListener(rowMouseHandler);
		label.addMouseMotionListener(rowMouseHandler);

		addMouseListener(rowMouseHandler);
		addMouseMotionListener(rowMouseHandler);

		prepare(btnM);
		prepare(btnP);

		int btnHeight = Math.max(btnM.getPreferredSize().height,
				btnM.getPreferredSize().width);
		btnHeight = Math.max(btnHeight, slider.getPreferredSize().height);
		Dimension d = new Dimension(btnHeight, btnHeight);

		setAll(d, light);

		d.height += PAD * 2;
		d.width += PAD * 2;
		setAll(d, btnM);
		setAll(d, btnP);
		d.height -= PAD * 2;

		
		d.width = 130;
		setAll(d, slider);


		this.setLayout(new GridLayout(2,1));
		bottom = new JPanel();
		bottom.setLayout(new BoxLayout(bottom, BoxLayout.LINE_AXIS));
		bottom.setBorder(new EmptyBorder(0, 0, 3, 0));
		bottom.add(Box.createHorizontalStrut(5));
		bottom.add(light);
		bottom.add(Box.createHorizontalStrut(5));
		bottom.add(btnM);
		bottom.add(Box.createHorizontalStrut(5));
		bottom.add(btnP);
		bottom.add(Box.createHorizontalGlue());
		bottom.add(slider);
		bottom.add(Box.createHorizontalStrut(5));
		
		this.add(label);
		this.add(bottom);
		
		this.insideBorder = new LineBorder(this.getBackground(), selectedBorder.getBorderInsets(null).top);
		this.compoundBorderUnSelected = new CompoundBorder(this.outsideBorder, this.insideBorder);
		updateRow();
	}

	public void setText(String newText) {
		label.setText(newText);
		label.setToolTipText(newText);
	}
	
	public void updateVis() {
		btnM.setSelected(view.isVisible());
		btnP.setSelected(view.getChild().isVisible());	
		
		//@since 3.0.3 - added to update the slider on updateVis since after loading a session, the view opacity was property adjusted,
		//but the slider was not being updated 
		slider.setValue((int) (view.getAlpha() * 1000));
	}
	
	public Layer.LView getView() {
		return view;
	}
	
	public void setBackground(Color newColor) {
		super.setBackground(newColor);
		if (bottom!=null){
			bottom.setBackground(newColor);
		}
		if (slider!=null) {
			slider.setBackground(newColor);
		}
	}
	
	public void updateRow() {
		setBorder(view == LManager.getLManager().getActiveLView() ? this.compoundBorderSelected : this.compoundBorderUnSelected);
	}

	void prepare(JToggleButton btn) {
		btn.setMargin(new Insets(0, 0, 0, 0));
		btn.setFocusable(false);
		btn.addActionListener(visibilityHandler);
	}
	
	private void setAll(Dimension d, JComponent c) {
		d = new Dimension(d);
		c.setMinimumSize(d);
		c.setMaximumSize(d);
		c.setPreferredSize(d);
	}

	private ActionListener visibilityHandler = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			JToggleButton btn = (JToggleButton) e.getSource();
			Row row = (Row) btn.getParent().getParent(); //have to do this twice because the new layout has the button added to a panel, then the row
			Layer.LView view = row.view;
			if (btn.getText().equals("P"))
				view = view.getChild();
			
			//KJR - 6/6/12
			if (btn.isSelected()) {
				view.setDirty(true);//we need it to believe it is dirty in order to trigger a redraw
			}
			
			view.setVisible(btn.isSelected());
		}
	};
//Not used currently, but could be called in MainPanel to be used to alternate row colors
	public void setColor(Color c){
		this.setBackground(c);
		bottom.setBackground(c);
	}
}

