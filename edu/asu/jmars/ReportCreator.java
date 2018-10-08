package edu.asu.jmars;

import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
                                                         // TODO (PW) Remove commented-out code
//import org.apache.commons.httpclient.HttpClient;
//import org.apache.commons.httpclient.HttpException;
//import org.apache.commons.httpclient.HttpStatus;
//import org.apache.commons.httpclient.methods.PostMethod;




import org.apache.http.HttpStatus;

import edu.asu.jmars.layer.threed.ThreeDPanel;
import edu.asu.jmars.layer.util.FileLogger;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.JmarsHttpRequest;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.util.HttpRequestType;

/**
 * Gathers information on the state of JMARS when a problem occurred and emails
 * it to <code>jmars.config:reportpage</code>.
 */
public class ReportCreator {
	private JDialog dlg;
	private Thread sendingThread;
	private String error;
	
	public JDialog getDialog() {
		return dlg;
	}
	
	public ReportCreator(JFrame parent, FileLogger logger) {
		dlg = new JDialog(parent, "Report a Problem", true);
		dlg.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		
		JTextPane intro = new JTextPane();
		intro.setBackground(dlg.getContentPane().getBackground());
		intro.setEditable(false);
		intro.setText("Got a problem?  We'd like to help.\n\n" +
			"Submit the form below and we'll get back to you as soon as possible.");
		
		JLabel emailLabel = new JLabel("Email Address");
		final JTextField email = new JTextField();
		if (!Main.isInternal()) {
			email.setText(Main.USER);
		}
		
		JLabel noteLabel = new JLabel("How to Recreate the Problem");
		final JTextArea noteText = new JTextArea();
		noteText.setLineWrap(true);
		noteText.setRows(8);
		JScrollPane noteSP = new JScrollPane(noteText);
		
		JLabel dataLabel = new JLabel("Debugging Information");
		final JTextArea data = new JTextArea();
		data.setRows(8);
		data.setEditable(false);
		StringBuffer text = new StringBuffer();
		
		//values we will get out of the System. //we are changing to use the 2 String method so that we can avoid NPEs
		String javaRuntime = Util.getSafeSystemProperty("java.runtime.name","n/a");
		String javaVendor = Util.getSafeSystemProperty("java.vendor","n/a");
		String javaVmVendor = Util.getSafeSystemProperty("java.vm.vendor", "n/a");
		String javaVersion = Util.getSafeSystemProperty("java.version","n/a");
		String javaLibPath = Util.getSafeSystemProperty("java.library.path","n/a");
		String javaVMInfo = Util.getSafeSystemProperty("java.vm.info", "n/a");
		String archModel = Util.getSafeSystemProperty("sun.arch.data.model","n/a");
		
		String osName = Util.getSafeSystemProperty("os.name","n/a");
		String osArch = Util.getSafeSystemProperty("os.arch","n/a");
		String osVersion = Util.getSafeSystemProperty("os.version","n/a");
		
		String product = Config.get("product", "n/a");
		String body = Main.getCurrentBody();
		String aboutDate = Main.ABOUT().DATE;
		if (aboutDate == null) {
			aboutDate = "n/a";
		}
		
		long totalMemory = Runtime.getRuntime().totalMemory();
		long maxMemory = Runtime.getRuntime().maxMemory();
		long freeMemory = Runtime.getRuntime().freeMemory();
		Dimension screenSize = null;
		int resolution = 0;
		int colorDepth = 0;
		int numOfMonitors = 0;
		StringBuffer colorSpace = new StringBuffer();
		StringBuffer transparency = new StringBuffer();
		try {
			screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		} catch (HeadlessException he) {
			screenSize = new Dimension();
		}
		try {
			resolution = Toolkit.getDefaultToolkit().getScreenResolution();
		} catch (HeadlessException he) {
			resolution = -1;
		}
		try {
			colorDepth = Toolkit.getDefaultToolkit().getColorModel().getPixelSize();
		} catch (HeadlessException he) {
			colorDepth = -1;
		}
		try {
			numOfMonitors = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices().length;
		} catch (HeadlessException he) {
			numOfMonitors = -1;
		}
		try {
			int totalComps = Toolkit.getDefaultToolkit().getColorModel().getNumComponents();
			int numComps = Toolkit.getDefaultToolkit().getColorModel().getColorSpace().getNumComponents();
			for(int i=0; i<numComps; i++ ) {
				colorSpace.append(Toolkit.getDefaultToolkit().getColorModel().getColorSpace().getName(i));
				if (i < numComps-1) {
					colorSpace.append(", ");
				}
			}
			if (totalComps-numComps == 1) {
				colorSpace.append(", Alpha");
			}
		} catch (HeadlessException he) {
			colorSpace.append("unknown");
		}
		try {
			int trans = Toolkit.getDefaultToolkit().getColorModel().getTransparency();
			switch (trans) {
			case Transparency.BITMASK:
				transparency.append("BITMASK");
				break;
			case Transparency.OPAQUE:
				transparency.append("OPAQUE");
				break;
			case Transparency.TRANSLUCENT:
				transparency.append("TRANSLUCENT");
				break;
			default:
				transparency.append("unknown");
			}
		} catch (HeadlessException he) {
			transparency.append("unknown");
		}
		String graphicsEnv = Util.getSafeSystemProperty("java.awt.graphicsenv","n/a");
		String proxyHost = Util.getSafeSystemProperty("http.proxyHost", "n/a");
		String proxyPort = Util.getSafeSystemProperty("http.proxyPort", "n/a");
		String ipAddr = "";
		try {
			ipAddr = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		StringBuffer extList = new StringBuffer();
		String dirs = Util.getSafeSystemProperty("java.ext.dirs", "n/a");
		try {
			String[] paths = dirs.split(File.pathSeparator);			
			for (String path : paths) {			
				File exts = new File(path);
				if (exts.exists() && exts.isDirectory()) {
					String[] list = exts.list();
					boolean first = true;
					for(String oneFile : list) {
						if (!first) {
							extList.append(File.pathSeparator);
						}
						extList.append(path);
						extList.append(File.separator);
						extList.append(oneFile);
						first = false;
					}					
				}
			}
		} catch (Exception e) {
			extList.append("Exception accessing java.ext.dirs.\njava.ext.dirs = "+dirs+"\n"+e.getMessage());
		}
		
		//@since 3.0.2 - vm options added to install process, this will put those options in our help report
		String xmxOption = "";
		String xssOption = "";
		
		try {
			Class varClass = Class.forName("com.install4j.api.launcher.Variables");
			Method getVariable = varClass.getDeclaredMethod("getInstallerVariable", String.class);
			xmxOption = (String) getVariable.invoke(null, "xmx");
			xssOption = (String) getVariable.invoke(null, "xss");
		} catch (Exception e1) {
		}
		
		text.append("========Java Properties========\n");
		text.append("Runtime name(java.runtime.name): "+ javaRuntime+"\n");
		text.append("Vendor(java.vendor): "+ javaVendor+"\n");
		text.append("VM Vendor(java.vm.vendor): "+ javaVmVendor+"\n");
		text.append("Version(java.version): "+ javaVersion+"\n");
		text.append("VM Info(java.vm.info): "+ javaVMInfo+"\n");
		text.append("VM 32/64 bit(sun.arch.data.model): "+ archModel+"\n");
		text.append("GL Version: "+ThreeDPanel.getVersion()+"\n");
		text.append("GLU Version: "+ThreeDPanel.getGLUVersion()+"\n");
		text.append("Library path(java.library.path): "+ javaLibPath+"\n");
		text.append("Max memory(runtime.maxMemory):   "+ maxMemory+"\n");
		text.append("Total memory(runtime.totalMemory): "+ totalMemory+"\n");
		text.append("Free memory(runtime.freeMemory):  "+ freeMemory+"\n");
		text.append("Extensions: "+ extList+"\n");
		text.append("Name(os.name): "+ osName+"\n");
		text.append("Arch(os.arch): "+ osArch+"\n");
		text.append("Version(os.version): "+ osVersion+"\n");
		text.append("Screen size(toolkit.screenSize): "+ screenSize+"\n");
		text.append("Resolution(toolkit.screenResolution): "+ resolution+"\n");
		text.append("Color Depth(toolkit.colorModel.pixelSize): "+ colorDepth+"\n");
		text.append("Color Space(toolkit.ColorModel.ColorSpace.Name): "+ colorSpace.toString()+"\n");
		text.append("Color Transparency(toolkit.ColorModel.Transparency): "+ transparency.toString()+"\n");
		text.append("# of monitors(graphicsEnvironment.screenDevices.length): "+ numOfMonitors+"\n");
		text.append("Graphics Env(java.awt.graphicsenv): " + graphicsEnv+"\n");
		text.append("Proxy host(http.proxyHost): " + proxyHost+"\n");
		text.append("Proxy Port(http.proxyPort): " + proxyPort+"\n");
		text.append("IP Address: " + ipAddr+"\n");
		text.append("User: " + Main.USER+"\n");
		if ((xmxOption != null && !xmxOption.equals("")) || (xssOption != null && !xssOption.equals(""))) {
			text.append("vmOptions settings: \n");
			if (xmxOption != null) {
				text.append("	"+xmxOption+"\n");
			}
			if (xssOption != null) {
				text.append("	"+xssOption+"\n");
			}
		} else {
			text.append("vmOptions settings not reported\n");
		}
		text.append("=================================\n\n");
		
		text.append("===========JMARS Info=============\n");
		text.append(product+" "+Util.getVersionNumber()+" "+body+"\n");
		text.append("Build time: "+aboutDate+"\n");
		text.append("Build Type: "+(Config.get("is_beta",false) ? "BETA": "Production (Non-Beta)"));
		text.append("\n==================================\n\n");
		
		text.append("========= Log =========\n");
		text.append(logger == null ? "Log empty!" : logger.getContent());
		text.append("\n\n\n");
		data.setText(text.toString());
		data.setLineWrap(true);
		data.setBackground(dlg.getContentPane().getBackground());
		JScrollPane dataSP = new JScrollPane(data);
		
		final JButton send = new JButton("Send");
		send.setMnemonic(KeyEvent.VK_S);
		send.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				send.setEnabled(false);
				send(email.getText(), noteText.getText(), data.getText());
			}
		});
		
		int pad = 8;
		Container c = dlg.getContentPane();
		c.setLayout(new GridBagLayout());
		Insets base = new Insets(pad,pad,pad,pad);
		Insets indent = new Insets(pad,pad*3,pad,pad);
		c.add(intro, new GridBagConstraints(0,0,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.BOTH, base, 0,0));
		c.add(emailLabel, new GridBagConstraints(0,1,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.BOTH, base, 0,0));
		c.add(email, new GridBagConstraints(0,2,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.BOTH, indent, 0,0));
		c.add(noteLabel, new GridBagConstraints(0,3,1,1,0,0,GridBagConstraints.WEST,GridBagConstraints.BOTH, base, 0,0));
		c.add(noteSP, new GridBagConstraints(0,4,1,1,1,1,GridBagConstraints.WEST,GridBagConstraints.BOTH, indent, 0,0));
		c.add(dataLabel, new GridBagConstraints(0,5,1,1,0,0,GridBagConstraints.WEST,GridBagConstraints.BOTH, base, 0,0));
		c.add(dataSP, new GridBagConstraints(0,6,1,1,1,1,GridBagConstraints.WEST,GridBagConstraints.BOTH, indent, 0,0));
		c.add(send, new GridBagConstraints(0,7,1,1,0,0,GridBagConstraints.EAST,GridBagConstraints.NONE, base, 0,0));
		c.setMaximumSize(new Dimension(800,600));
		
		dlg.pack();
	}
	
	private void send (final String email, final String notes, final String info) {
		final int timeout = 30*1000;
		final String base = dlg.getTitle();
		dlg.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
		dlg.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		error = null;
		
		final Timer timer = new Timer(1000, new ActionListener() {
			int time = timeout/1000;
			public void actionPerformed(ActionEvent e) {
				time--;
				dlg.setTitle(base + " - sending (" + time + " sec...)");
			}
		});
		timer.start();
		
		// get off the AWT thread so updates can be shown in the GUI
		sendingThread = new Thread(new Runnable() {                                    // TODO (PW) Remove commented-out code
			public void run() {
				try {
//					PostMethod post = new PostMethod(Config.get("reportpage"));
//					post.addParameter("user", Main.USER);
//					post.addParameter("email", email);
//					post.addParameter("notes", notes);
//					post.addParameter("info", info);
				    String product = Config.get("product", "JMARS");
				    product = product.toUpperCase();
				    
					JmarsHttpRequest request = new JmarsHttpRequest(Config.get("reportpage"), HttpRequestType.POST);
					request.addRequestParameter("user", Main.USER);
					request.addRequestParameter("email", email);
					request.addRequestParameter("notes", notes);
					request.addRequestParameter("info", info);
					request.addRequestParameter("product",product);
					
					
//					HttpClient http = new HttpClient();
//					http.getHttpConnectionManager().getParams().setConnectionTimeout(timeout);
	                request.setConnectionTimeout(timeout);
	                request.setLaxRedirect();
	                boolean successful = request.send();
//					int code = Util.postWithRedirect(http, post, 3);
                    int code = request.getStatus();
	                if (code != HttpStatus.SC_OK) {
						error = "Server returned unexpected code " + code;
					} else {
						String response = new BufferedReader(
							new InputStreamReader(
								request.getResponseAsStream())).readLine();
						if (response.equals("OKAY")) {
							// mail sent properly
						} else if (response.equals("FAILURE")) {
							error = "Server was unable to deliver the message: " + response;
						}
					}
//				} catch (HttpException e) {
//					error = "Error communicating with server";
				} catch (IOException e) {
					error = "Unable to establish a connection, are you connected?";
				} catch (Exception e) {
					error = "Unexpected exception occurred: " + e.getMessage();
				}
				
				// get back on the AWT thread to handle the result
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						timer.stop();
						dlg.dispose();
						if (error != null) {
							// failure sending report, suggest e-mail
							JOptionPane.showMessageDialog(
								Main.mainFrame,
								"Unable to deliver message: " + error + "\n\nEmail " + Config.get("email"),
								"Unable to deliver message", JOptionPane.ERROR_MESSAGE);
						}
					}
				});
				
				sendingThread = null;
			}
		});
		sendingThread.setPriority(Thread.MIN_PRIORITY);
		sendingThread.start();
	}
}
