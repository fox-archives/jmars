package edu.asu.jmars.layer.stamp.focus;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;

import edu.asu.jmars.layer.stamp.AddLayerWrapper;
import edu.asu.jmars.layer.stamp.StampLayer;
import edu.asu.jmars.util.Util;

public class QueryFocusPanel extends JPanel {

	public QueryFocusPanel(final AddLayerWrapper wrapper, final StampLayer stampLayer)
	{
		JPanel queryPanel = new JPanel();
		queryPanel.setLayout(new BorderLayout());
		queryPanel.add(wrapper.getContainer(), BorderLayout.CENTER);
	    
	    // Construct the "buttons" section of the container.
	    JPanel buttons = new JPanel();
	    buttons.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	
	    JButton ok = new JButton("Update Search");
	    ok.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (stampLayer.queryThread==null) {
	                // Update layer/view with stamp data from new version of
	                // query using parameters from query panel.
	                String queryStr = wrapper.getQuery();
	                stampLayer.setQuery(queryStr);
				}
			}
		});
	    buttons.add(ok);
	    
	    JButton help = new JButton("Help");
	    help.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
	    		Util.launchBrowser(stampLayer.getParam(stampLayer.HELP_URL));
			}
	    });
	    
	    buttons.add(help);
	             
	    queryPanel.add(buttons, BorderLayout.SOUTH);
	    
	    setLayout(new BorderLayout());
	    add(queryPanel, BorderLayout.CENTER);
	}
}
