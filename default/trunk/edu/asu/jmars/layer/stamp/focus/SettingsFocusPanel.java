package edu.asu.jmars.layer.stamp.focus;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import edu.asu.jmars.layer.stamp.StampLView;
import edu.asu.jmars.layer.stamp.StampLayer;
import edu.asu.jmars.layer.stamp.StampLayerSettings;
import edu.asu.jmars.layer.stamp.radar.HorizonColorDisplayPanel;
import edu.asu.jmars.swing.ColorCombo;

public class SettingsFocusPanel extends JPanel {
    
	public SettingsFocusPanel(final StampLView stampLView)
	{
		JLabel borderColorLbl = new JLabel("Outline color:");
		final ColorCombo borderColor = new ColorCombo();
		//we will get the settings from the LView for the first time
		StampLayerSettings settings = stampLView.getSettings();
		borderColor.setColor(settings.getUnselectedStampColor());
		final ColorCombo fillColor = new ColorCombo();
		JLabel fillColorLbl = new JLabel("Fill color:");
		fillColor.setColor(new Color(settings.getFilledStampColor().getRGB() & 0xFFFFFF, false));
		
		ActionListener fillColorListener = new ActionListener() {
          	public void actionPerformed(ActionEvent e) {
          		//in order to make sure that the LView and the focus panel are using the same settings object, we will get the 
          		//settings object out of the LView again. Making the above settings final and using them here does not work
          		StampLayerSettings layerSettings = stampLView.getSettings();
          		//border color
          		layerSettings.setUnselectedStampColor(borderColor.getColor());
           		//end border color
           		//fill color
           		int alpha = layerSettings.getFilledStampColor().getAlpha();
				layerSettings.setFilledStampColor(
					new Color( (alpha<<24) | (fillColor.getColor().getRGB() & 0xFFFFFF), true));
				
				if (!layerSettings.hideOutlines() || alpha != 0) {
					//if nothing is being shown, don't redraw, otherwise, redraw everything
					stampLView.redrawEverything(true);
				}

          	}
        };
		borderColor.addActionListener(fillColorListener);
		fillColor.addActionListener(fillColorListener);
		
		JLabel alphaLbl = new JLabel("Fill alpha:");
		final JSlider alpha = new JSlider(0, 255, 0);
		alpha.setValue(settings.getFilledStampColor().getAlpha());
	    alpha.addChangeListener(new ChangeListener() {
	    	public void stateChanged(ChangeEvent e) {
	    		if (alpha.getValueIsAdjusting()) {
	    			return;
	    		}
	    		//in order to make sure that the LView and the focus panel are using the same settings object, we will get the 
          		//settings object out of the LView instead of using the final instance that is passed in
          		StampLayerSettings layerSettings = stampLView.getSettings();
	    		int alphaVal = alpha.getValue();
	    		int color = layerSettings.getFilledStampColor().getRGB() & 0xFFFFFF;
	    		layerSettings.setFilledStampColor(new Color((alphaVal<<24) | color, true));
	    		stampLView.redrawEverything(true);
	    	}
	    });
		
//		JLabel nameLbl = new JLabel("Layer name:");
//		final PasteField name = new PasteField(12);
//		name.setText(settings.getName());
//		name.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//            	//in order to make sure that the LView and the focus panel are using the same settings object, we will get the 
//          		//settings object out of the LView instead of using the final instance that is passed in
//          		StampLayerSettings layerSettings = stampLView.getSettings();
//            	layerSettings.setName(name.getText());
//                LManager.getLManager().updateLabels();
//            }
//        });
		
		
			    
	    // Wind Vector related
		JLabel magnitudeLabel = new JLabel("Magnitude Scale Factor:");
		final JTextField magnitudeField = new JTextField(""+settings.getMagnitude());
		magnitudeField.addActionListener(new ActionListener() {
		
			public void actionPerformed(ActionEvent e) {
				//in order to make sure that the LView and the focus panel are using the same settings object, we will get the 
          		//settings object out of the LView instead of using the final instance that is passed in
          		StampLayerSettings layerSettings = stampLView.getSettings();
				layerSettings.setMagnitude(Double.parseDouble(magnitudeField.getText()));
           		StampLView child = (StampLView) stampLView.getChild();

       			stampLView.drawOutlines();
      			child.drawOutlines();
			}
		});

		JLabel originColorLbl = new JLabel("Origin color:");
		final ColorCombo originColor = new ColorCombo();
		originColor.setColor(settings.getOriginColor());
		originColor.addActionListener(new ActionListener() {
          	public void actionPerformed(ActionEvent e) {
          	//in order to make sure that the LView and the focus panel are using the same settings object, we will get the 
          		//settings object out of the LView instead of using the final instance that is passed in
          		StampLayerSettings layerSettings = stampLView.getSettings();		
           		layerSettings.setOriginColor(originColor.getColor());
           		StampLView child = (StampLView) stampLView.getChild();

       			stampLView.drawOutlines();
      			child.drawOutlines();
          	}
        });

		JLabel originMagnitudeLabel = new JLabel("Origin Scale Factor:");
		final JTextField originMagnitudeField = new JTextField(""+settings.getOriginMagnitude());
		originMagnitudeField.addActionListener(new ActionListener() {
		
			public void actionPerformed(ActionEvent e) {
				//in order to make sure that the LView and the focus panel are using the same settings object, we will get the 
          		//settings object out of the LView instead of using the final instance that is passed in
          		StampLayerSettings layerSettings = stampLView.getSettings();
				layerSettings.setOriginMagnitude(Double.parseDouble(originMagnitudeField.getText()));
           		StampLView child = (StampLView) stampLView.getChild();
       			stampLView.drawOutlines();
      			child.drawOutlines();
			}
		});	    
	    
	    JPanel settingsP = new JPanel();
	    settingsP.setLayout(new GridBagLayout());
	    settingsP.setBorder(new EmptyBorder(4,4,4,4));
	    int row = 0;
	    int pad = 4;
	    Insets in = new Insets(pad,pad,pad,pad);
	    
	    StampLayer stampLayer = stampLView.stampLayer;
	    
	    if (stampLayer.vectorShapes()) {  // Wind vectors
		    settingsP.add(borderColorLbl, new GridBagConstraints(0,row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
		    settingsP.add(borderColor, new GridBagConstraints(1,row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
		    row++;
			settingsP.add(magnitudeLabel, new GridBagConstraints(0,row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
			settingsP.add(magnitudeField, new GridBagConstraints(1,row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
		    row++;
		    settingsP.add(originColorLbl, new GridBagConstraints(0,row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
		    settingsP.add(originColor, new GridBagConstraints(1,row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
		    row++;
		    settingsP.add(originMagnitudeLabel, new GridBagConstraints(0,row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
		    settingsP.add(originMagnitudeField, new GridBagConstraints(1,row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));				    	
	    } else if (stampLayer.pointShapes()) {  // MOLA Shots
//		    settingsP.add(originColorLbl, new GridBagConstraints(0,row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
//		    settingsP.add(originColor, new GridBagConstraints(1,row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
//		    row++;
//		    settingsP.add(originMagnitudeLabel, new GridBagConstraints(0,row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
//		    settingsP.add(originMagnitudeField, new GridBagConstraints(1,row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));				    		    	
	    } else if (stampLayer.lineShapes()){ //Radar (SHARAD)
	    	JLabel colorLbl = new JLabel("Default Radar Horizon Color:");
	    	JLabel fullResLbl = new JLabel("Default Full Resolution Horizon Width:");
	    	JLabel browseLbl = new JLabel("Default Browse Image Horizon Width:");
	    	JLabel lviewLbl = new JLabel("Default LView Horizon Width:");
	    	final ColorCombo horizonColor = new ColorCombo(settings.getHorizonColor());
	    	horizonColor.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					StampLayerSettings settings = stampLView.getSettings();
					settings.setHorizonColor(horizonColor.getColor());
				}
			});
	    	Integer[] widthOptions = {1,2,3,4,5,6,7,8};
	    	final JComboBox<Integer> fullResBx = new JComboBox<Integer>(widthOptions);
	    	fullResBx.setSelectedItem(settings.getFullResWidth());
	    	fullResBx.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					StampLayerSettings settings = stampLView.getSettings();
					settings.setFullResWidth((int)fullResBx.getSelectedItem());
				}
			});
	    	final JComboBox<Integer> browseBx = new JComboBox<Integer>(widthOptions);
	    	browseBx.setSelectedItem(settings.getBrowseWidth());
	    	browseBx.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					StampLayerSettings settings = stampLView.getSettings();
					settings.setBrowseWidth((int)browseBx.getSelectedItem());
				}
			});
	    	final JComboBox<Integer> lviewBx = new JComboBox<Integer>(widthOptions);
	    	lviewBx.setSelectedItem(settings.getLViewWidth());
	    	lviewBx.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					StampLayerSettings settings = stampLView.getSettings();
					settings.setLViewWidth((int)lviewBx.getSelectedItem());
				}
			});
	    	
	    	
	    	settingsP.add(colorLbl, new GridBagConstraints(0, row, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, in, pad, pad));
	    	settingsP.add(horizonColor, new GridBagConstraints(1, row, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, in, pad, pad));
	    	settingsP.add(fullResLbl, new GridBagConstraints(0, ++row, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, in, pad, pad));
	    	settingsP.add(fullResBx, new GridBagConstraints(1, row, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, in, pad, pad));
	    	settingsP.add(browseLbl, new GridBagConstraints(0, ++row, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, in, pad, pad));
	    	settingsP.add(browseBx, new GridBagConstraints(1, row, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, in, pad, pad));
	    	settingsP.add(lviewLbl, new GridBagConstraints(0, ++row, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, in, pad, pad));
	    	settingsP.add(lviewBx, new GridBagConstraints(1, row, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, in, pad, pad));
		    settingsP.add(new JLabel("Footprint Color:"), new GridBagConstraints(0,++row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
		    settingsP.add(borderColor, new GridBagConstraints(1,row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
	    	settingsP.add(new HorizonColorDisplayPanel(stampLView), new GridBagConstraints(0, ++row, 2, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		    
	    } else {  // All other stamp layers
		    settingsP.add(borderColorLbl, new GridBagConstraints(0,row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
		    settingsP.add(borderColor, new GridBagConstraints(1,row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
		    row++;
		    
		    if (!stampLayer.lineShapes()) {
			    settingsP.add(fillColorLbl, new GridBagConstraints(0,row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
			    settingsP.add(fillColor, new GridBagConstraints(1,row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
			    row++;
			    settingsP.add(alphaLbl, new GridBagConstraints(0,row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
			    settingsP.add(alpha, new GridBagConstraints(1,row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
			    row++;
		    }		                
//		    settingsP.add(nameLbl, new GridBagConstraints(0,row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
//		    settingsP.add(name, new GridBagConstraints(1,row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
		    row++;

		    settingsP.add(new JLabel(),new GridBagConstraints(0,row,2,1,1,1,GridBagConstraints.WEST,GridBagConstraints.BOTH,in,pad,pad));
		} 
	    
	    JScrollPane displayPane = new JScrollPane(settingsP);
	    	    
	    setLayout(new BorderLayout());
	    add(displayPane, BorderLayout.CENTER);
	}
}
