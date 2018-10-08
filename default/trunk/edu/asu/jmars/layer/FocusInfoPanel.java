package edu.asu.jmars.layer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import edu.asu.jmars.layer.stamp.StampLayer;
import edu.asu.jmars.swing.UrlLabel;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.JmarsHttpRequest;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.util.HttpRequestType;

public class FocusInfoPanel extends JPanel {
	//Attached mapLView
	private Layer.LView lView;
	//All GUI Pieces
	private JPanel northPanel, centerPanel, west, east;
	private JLabel title, units;
	private JPanel description, links, citation;
	private JScrollPane descripSP, linkSP, citSP;
	private JTextArea descrip, cit;
	private JPanel link;
	private JLabel linkLbl;
	// Constants
	private final Color lightBlue = UIManager.getColor("TabbedPane.selected");
	private final Color beige = UIManager.getColor("Panel.background");
	private final Dimension defaultSize = new Dimension(300,300);
	
	public boolean isLimited;
	
    private static ExecutorService pool = Executors.newCachedThreadPool(new ThreadFactory() {
    	int id = 0;
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r);
			t.setName("FocusInfoQuery-" + (id++));
			t.setPriority(Thread.MIN_PRIORITY);
			t.setDaemon(true);
			return t;
		}
    });
	
	public FocusInfoPanel(Layer.LView lv, boolean limited){
		this.lView = lv;
		layoutFields(limited);	
		loadFields();	
	}

	
//This method loads all the gui for the infoPanel...
//	it has two possible layouts depending on whether
//	it is a "limited" infoPanel (such as the lat/lon
//	grid's or map scalebar's)	
	public void layoutFields(boolean limited){
		this.isLimited=limited;
		
	//sets properties of the InfoPanel (which is a tabbed pane on the focusPanel)	
		setBackground(lightBlue);
		setLayout(new BorderLayout());
		setPreferredSize(defaultSize);
		
	/* builds the northPanel which
	 * is added to ---> FocusInfoPanel 
	 * contains: title, units
	 */ 
		northPanel = new JPanel();
		northPanel.setBackground(lightBlue);
		northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.PAGE_AXIS));
	// builds title and units which go to northPanel	
		title = new JLabel("No title available");
		title.setFont(new Font("Dialog", Font.BOLD, 20));
		title.setAlignmentX(CENTER_ALIGNMENT);
		units = new JLabel("Units: ---");
		units.setFont(new Font("Dialog", Font.BOLD, 15));
		units.setAlignmentX(CENTER_ALIGNMENT);
		northPanel.add(Box.createRigidArea(new Dimension(0,4)));
		northPanel.add(title);
		northPanel.add(units);
		
	/* builds centerPanel which
	 * is added to ---> FocusInfoPanel
	 * contains: description, detAndLink, citation panels 
	 */
		centerPanel = new JPanel();
		centerPanel.setBackground(lightBlue);
		centerPanel.setLayout(new GridBagLayout());
	// builds description which contains a scrollPane and textArea
		description = new JPanel();
		description.setLayout(new GridLayout(1,1));
		description.setBorder(BorderFactory.createTitledBorder("Description"));
		description.setMinimumSize(new Dimension(100,50));
		descrip = new JTextArea("There is currently no description for this layer");
		descrip.setBackground(beige);
		descrip.setBorder(BorderFactory.createEmptyBorder(0,5,0,5));
		descrip.setEditable(false);
		descrip.setLineWrap(true);
		descrip.setWrapStyleWord(true);
		descripSP = new JScrollPane(descrip, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		descripSP.setBorder(BorderFactory.createEmptyBorder());
		description.add(descripSP);
	
		
		if(!isLimited){
		
/**		The details panel has been removed. If it will be added back in the future,
 		uncomment the following code (and code in the beginning of the class which 
 		defines the gui components) and it will be added back in.
 
 		builds detAndLink panel which
		 is added to ---> centerPanel
		 contains: details, links panels	
		 
 			detAndLink = new JPanel();
			detAndLink.setBackground(lightBlue);
			detAndLink.setLayout(new GridLayout(1, 2));
			detAndLink.setMinimumSize(new Dimension(100,50));
		 builds details and links panels which each contain a scrollPane and textArea
			details = new JPanel();
			details.setBorder(BorderFactory.createTitledBorder("Details"));
			details.setLayout(new GridLayout(1,1));
			dets = new JTextArea("There are currently no details for this layer");
			dets.setBackground(beige);
			dets.setBorder(BorderFactory.createEmptyBorder());
			dets.setEditable(false);
			dets.setLineWrap(true);
			dets.setWrapStyleWord(true);
			detSP = new JScrollPane(dets, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
					ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			detSP.setBorder(BorderFactory.createEmptyBorder());
			detSP.setMinimumSize(new Dimension(0,50));
			details.add(detSP);
		 add a little white (blue) space in each of the dets and links panels 	
			JPanel detsWSpace = new JPanel();
			detsWSpace.setLayout(new BoxLayout(detsWSpace, BoxLayout.LINE_AXIS));
			detsWSpace.setBackground(lightBlue);
			detsWSpace.add(details);
			detsWSpace.add(Box.createHorizontalStrut(2));
			JPanel linksWSpace = new JPanel();
			linksWSpace.setLayout(new BoxLayout(linksWSpace, BoxLayout.LINE_AXIS));
			linksWSpace.setBackground(lightBlue);
			linksWSpace.add(Box.createHorizontalStrut(2));
			linksWSpace.add(links);
		 add details and links (with spaces) to detAndLink	
			detAndLink.add(detsWSpace);
			detAndLink.add(linksWSpace);
**/			
		// 	builds links panel
			links = new JPanel();
			links.setBorder(BorderFactory.createTitledBorder("Links"));
			links.setLayout(new GridLayout(1,1));
			link = new JPanel();
			link.setLayout(new BoxLayout(link, BoxLayout.PAGE_AXIS));
			link.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
			linkLbl = new JLabel("There are currently no links for this layer.");
			Font f = new Font("Arial", Font.PLAIN, 12);
			linkLbl.setFont(f);
			link.add(linkLbl);
			linkSP = new JScrollPane(link, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
					ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			linkSP.setBorder(BorderFactory.createEmptyBorder());
			links.add(linkSP);	
		// builds citation panel which contains a scrollPane and textArea
			citation = new JPanel();
			citation.setBorder(BorderFactory.createTitledBorder("Citation"));
			citation.setMinimumSize(new Dimension(100,50));
			citation.setLayout(new GridLayout(1,1));
			cit = new JTextArea("There is currently no citation for this layer");
			cit.setBackground(beige);
			cit.setBorder(BorderFactory.createEmptyBorder(0,5,0,5));
			cit.setEditable(false);
			cit.setLineWrap(true);
			cit.setWrapStyleWord(true);
			citSP = new JScrollPane(cit, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
					ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			citSP.setBorder(BorderFactory.createEmptyBorder());
			citation.add(citSP);
		// adds panels to centerPanel
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			c.gridx = 0;
			c.gridy = 0;
			c.ipady = 50;
			c.weighty = 1;
			c.weightx = 1;
			c.insets = new Insets(10, 0, 0, 0);
			centerPanel.add(description, c);
			c.gridx = 0;
			c.gridy = 1;
			c.ipady = 10;
			c.insets = new Insets(4,0,0,0);
			centerPanel.add(links, c);
			c.gridx = 0;
			c.gridy = 2;
			c.insets = new Insets(4,0,5,0);
			centerPanel.add(citation, c);
			
			
		}
		
		if (isLimited){
			centerPanel.add(Box.createRigidArea(new Dimension(0,10)));
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			c.weighty = 1;
			c.weightx = 1;
			c.insets = new Insets(5,0,10,0);
			centerPanel.add(description,c);
		}
		
	//adds pieces to focusInfoPanel
		west = new JPanel();
		west.setBackground(lightBlue);
		east = new JPanel();
		east.setBackground(lightBlue);
		add(northPanel, BorderLayout.NORTH);
		add(centerPanel, BorderLayout.CENTER);
		add(west, BorderLayout.WEST);
		add(east, BorderLayout.EAST);
		
	}
		
	
	public void loadFields(){
		Runnable fetchData = new Runnable() {
			
			@Override
			public void run() {
				String body = Config.get(Util.getProductBodyPrefix() + "bodyname");//@since change bodies
				body = body.toLowerCase();
				String layerType = "";
				String layerKey = "";
				String layerId = "";
				LayerParameters layerParams = lView.getLayerParameters();
				String urlStr = "";
				
				if (layerParams == null){
					layerKey = lView.getLayerKey();
		//			System.out.println(layerKey);
					if(layerKey == null || layerKey.equals("")){
						layerKey = LManager.getLManager().getUniqueName(lView);
					}
					layerType = lView.getLayerType();
					urlStr = "LayerInfoFetcher?body="+body+"&layerType="+layerType+"&layerKey="+layerKey;
				}else{
					layerId = layerParams.id;
					layerType = layerParams.type;
					urlStr = "LayerInfoFetcher?body="+body+"&layerId="+layerId;
				}

				ArrayList<String> fields;
				
				try {
					int idx = urlStr.indexOf("?");
		
					String connStr = LayerParameters.paramsURL + urlStr.substring(0,idx);
		
					String data = urlStr.substring(idx+1)+StampLayer.getAuthString()+StampLayer.versionStr;
		
					// Connect timeout and SO_TIMEOUT of 10 seconds
					//			URL url = new URL(connStr);               // TODO Remove old code
					//			URLConnection conn = url.openConnection();
					//			conn.setConnectTimeout(10*1000);
					//			conn.setReadTimeout(10*1000);
					//			
					//			conn.setDoOutput(true);
					//			OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
					//			wr.write(data);
					//			wr.flush();
					//			wr.close();
					//			
					//			ObjectInputStream ois = new ObjectInputStream(conn.getInputStream());
					//			fields = (ArrayList<String>) ois.readObject();
					//			ois.close();
					
					JmarsHttpRequest req = new JmarsHttpRequest(connStr, HttpRequestType.POST);
					req.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
					req.addOutputData(data);
					req.setConnectionTimeout(10*1000);
					req.setReadTimeout(10*1000);
					req.send();
					ObjectInputStream ois = new ObjectInputStream(req.getResponseAsStream());
					fields = (ArrayList<String>) ois.readObject();
					ois.close();
					req.close();
				} catch (Exception e) {
					e.printStackTrace();
					System.err.println("Error retrieving layer parameters");
					fields = new ArrayList<String>();
				}
				
				final ArrayList<String> updateFields = fields;
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						updateName();
						
						//		System.out.println(fields);
				
				// If the lview does not have an lparams associated with it, populate the 
				//  infopanel off a database query.
						if(layerParams==null){
							if(updateFields!=null&&updateFields.size()>0){
								//Layerkey is passed back at index 0 in fields in old code, ignore
								// that value now since the name is set using the updateName method
								if(updateFields.get(1)!=null && updateFields.get(1).length()>0 && !isLimited)
									cit.setText(updateFields.get(1));
								if(updateFields.get(2)!=null && updateFields.get(2).length()>0)
									descrip.setText(updateFields.get(2));
								if(updateFields.get(3)!=null && updateFields.get(3).length()>0)
									units.setText("Units: "+updateFields.get(3));
								if(updateFields.size()>=5 && !isLimited){
									if(updateFields.get(4)!=null && updateFields.get(4).length()>2){
										link.removeAll();
										String ltext = updateFields.get(4);
										ltext = ltext.substring(1, ltext.length()-1);
										ltext = ltext.trim();
										String[] links = ltext.split(",");
										for(String s : links){
											UrlLabel u = new UrlLabel(s, "#7498c8");
											link.add(u);
										}
									}	
								}
							}
						}else{
				// populate the infopanel from lparams attributes
							if(!isLimited){
								if(layerParams.citation.length()>2){
									cit.setText(layerParams.citation);
								}
								if(layerParams.links.length()>2){
									link.removeAll();
									String ltext = layerParams.links;
									ltext = ltext.substring(1, ltext.length()-1);
									ltext = ltext.trim();
									String[] links = ltext.split(",");
									for(String s : links){
										//remove quotes if there are any
										s = s.replace("\"", "");
										s = s.replace("\'", "");
										//create url label based off string
										UrlLabel u = new UrlLabel(s, "#7498c8");
										//add label to focus panel
										link.add(u);
									}
								}
							}
							if(layerParams.description.length()>0){
								descrip.setText(layerParams.description);
							}
							if(layerParams.units.length()>0){
								units.setText("Units: "+layerParams.units);
							}
							
						}	

						repaint();
					}
				});
			
			}
		};
		
		pool.execute(fetchData);
	}
	
// Set the name of the focuspanel to match the row (since it is the same layer)
	public void updateName(){
		title.setText(LManager.getLManager().getUniqueName(lView));
	}

}
