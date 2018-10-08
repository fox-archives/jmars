package edu.asu.jmars.layer.shape2;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import edu.asu.jmars.util.*;

import edu.asu.jmars.layer.util.features.*;

public class CommandDialog extends JDialog {
	static public String EXTENTION    = ".ssf";
	static public String DESCRIPTION  = "Shape Script File (*" + EXTENTION + ")";
	
	private final ShapeLayer shapeLayer;
	private final FeatureCollection fc;
	private final History history;
	
	// panel stuff.
	JTextArea     commandArea           = new JTextArea(15,60);
	JMenuItem     runCommandMenuItem    = new JMenuItem("Run");
	JMenuItem     loadMenuItem          = new JMenuItem("Load");
	JMenuItem     saveMenuItem          = new JMenuItem("Save");
	JPanel        statusPanel           = new JPanel();
	JLabel        statusBar             = new JLabel(" ");

	FileChooser fileChooser;

	public CommandDialog(FeatureCollection fc, History history, ShapeLayer shapeLayer, Frame owner){
		super(owner, "Script", false);
		
		this.shapeLayer = shapeLayer;
		this.fc = fc;
		this.history = history;
		
		fileChooser = new FileChooser();

		initPropertyBehavior();

		Container propPane = getContentPane();
		propPane.setLayout( new BorderLayout());
		
		JPanel featurePanel = new JPanel();
		featurePanel.setLayout( new BorderLayout());
		featurePanel.setBorder( BorderFactory.createTitledBorder("Feature Table"));
		
		// build the file menu
		JMenu fileMenu = new JMenu("File");
		fileMenu.add( loadMenuItem);
		fileMenu.add( saveMenuItem);
		
		// build the run menu
		JMenu runMenu = new JMenu("Run");
		runMenu.add( runCommandMenuItem);
		
		
		// build the menu bar
		JMenuBar menuBar = new JMenuBar();
		menuBar.add( fileMenu);
		menuBar.add( runMenu);
		propPane.add( menuBar, BorderLayout.NORTH);
		
		// build the command area.
		commandArea.setEditable(true);
		JScrollPane scrollPane = new JScrollPane( commandArea, 
							  JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
							  JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setBorder(BorderFactory.createEtchedBorder());
		propPane.add( scrollPane, BorderLayout.CENTER);
		
		
		// build the status bar.
		statusBar.setHorizontalAlignment(JLabel.LEFT);
		statusPanel.add( statusBar);
		propPane.add( statusPanel, BorderLayout.SOUTH);

		pack();

		fileChooser.addFilter(new ScriptFileChooser.FeatureProviderScript());
		
		setLocationRelativeTo(owner);
	}

	// Actions for components.
	private void initPropertyBehavior(){

		// Run the command file.
		runCommandMenuItem.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent e){
					// Make a new history frame.
					if (history != null)
						history.mark();

					final ShapeLayer.LEDState ledState = new ShapeLayer.LEDStateFileIO();
					SwingUtilities.invokeLater(new Runnable(){
						public void run(){
							shapeLayer.begin(ledState);
							try {
								String text = commandArea.getText();
								String [] lines = text.split("\n");
								for (int i=0; i< lines.length; i++){
									String result = new FeatureSQL(lines[i], fc, shapeLayer.getIndex(), shapeLayer.getSelections()).getStatusString();
									statusBar.setText(result == null ? "" : result);
									// String perror = FeatureSQL.getResultString();
									//if (perror != null && statusBar != null){
									//	statusBar.setText( perror);
									//}
								}
							}
							finally {
								shapeLayer.end(ledState);
							}
						}
					});
				}
			});


		// Load in a command file.
		loadMenuItem.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent e){
					File  [] scriptFile = fileChooser.chooseFile(CommandDialog.this, "Load");
					if (scriptFile==null){
						return;
					}
					
					ShapeLayer.LEDState ledState = new ShapeLayer.LEDStateFileIO();
					StringBuffer inputLine  = new StringBuffer();
					shapeLayer.begin(ledState);
					try {
						String [] lines = Util.readLines( new FileInputStream( scriptFile[0]));
						for (int i=0; i< lines.length; i++){
							inputLine.append( lines[i] + "\n");
						}
						commandArea.setText( inputLine.toString());
					} catch (Exception exception) {
					} finally {
						shapeLayer.end(ledState);
					}
				}
			});
		
		// Save a command file out.
		saveMenuItem.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent e){
					File [] scriptFile = fileChooser.chooseFile(CommandDialog.this, "Save");
					if (scriptFile==null){
						return;
					}
					
					
					ShapeLayer.LEDState ledState = new ShapeLayer.LEDStateFileIO();
					shapeLayer.begin(ledState);
					try {
						File outFile = filterFile( scriptFile[0]);
						BufferedWriter  outStream = new BufferedWriter( new FileWriter( outFile));
						outStream.write( commandArea.getText());
						outStream.flush();
						outStream.close();
					} catch (IOException ioe) {
						System.out.println("Error writing file " + 
								   scriptFile + ": " + ioe.getMessage());
						ioe.printStackTrace();
					}  catch (Exception ex) {
						ex.printStackTrace();
					}
					finally {
						shapeLayer.end(ledState);
					}
				}
			});
		
		
	} // end: initPropertyBehavior
    

	// The file must end with the file type extention.
	private File filterFile( File f){
		String fileName = f.toString();
		if (!fileName.endsWith( EXTENTION)){
			fileName += EXTENTION;
		}
		return new File( fileName);
	}
	
} // end: CommandDialog.java

