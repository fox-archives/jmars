package edu.asu.jmars;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
//import java.net.URLConnection;                             TODO (PW) Remove
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageWriterSpi;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.event.EventListenerList;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.DefaultEditorKit;

import org.json.JSONObject;

import net.sf.ehcache.Ehcache;

//import org.apache.commons.httpclient.HttpClient;                        TODO (PW) Remove
//import org.apache.commons.httpclient.HttpStatus;
//import org.apache.commons.httpclient.NameValuePair;
//import org.apache.commons.httpclient.URI;
//import org.apache.commons.httpclient.cookie.CookiePolicy;
//import org.apache.commons.httpclient.methods.PostMethod;

import edu.asu.jmars.TestDriverLayered.LManagerMode;
import edu.asu.jmars.TestDriverLayered.PannerMode;
import edu.asu.jmars.layer.AddLayer;
import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.LViewFactory;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.layer.LayerParameters;
import edu.asu.jmars.layer.LoadSaveLayerDialogs;
import edu.asu.jmars.layer.ProjectionEvent;
import edu.asu.jmars.layer.ProjectionListener;
import edu.asu.jmars.layer.SavedLayer;
import edu.asu.jmars.layer.investigate.InvestigateFactory;
import edu.asu.jmars.layer.map2.CacheManager;
import edu.asu.jmars.layer.map2.MapServer;
import edu.asu.jmars.layer.map2.MapServerFactory;
import edu.asu.jmars.layer.nomenclature.NomenclatureLView;
import edu.asu.jmars.layer.stamp.NumericStampSource;
import edu.asu.jmars.layer.stamp.StampCache;
import edu.asu.jmars.layer.stamp.StampServer;
import edu.asu.jmars.layer.stamp.StampSource;
import edu.asu.jmars.layer.streets.OpenStreetMapTiles;
import edu.asu.jmars.layer.util.FileLogger;
import edu.asu.jmars.places.PlacesMenu;
import edu.asu.jmars.places.XmlPlaceStore;
import edu.asu.jmars.pref.DefaultBrowser;
import edu.asu.jmars.ruler.RulerManager;
import edu.asu.jmars.swing.PopupEventQueue;
import edu.asu.jmars.swing.TimeField;
import edu.asu.jmars.swing.TimeField.TimeFormatChangeListener;
import edu.asu.jmars.swing.TimeField.TimeFormatEvent;
import edu.asu.jmars.swing.UrlLabel;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.GeoreferenceFileExport;
import edu.asu.jmars.util.HVector;
import edu.asu.jmars.util.HighResExport;
import edu.asu.jmars.util.JmarsHttpRequest;
import edu.asu.jmars.util.JmarsTrustManager;
import edu.asu.jmars.util.Time;
import edu.asu.jmars.util.TimeCacheFactory;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.util.HttpRequestType;
import edu.emory.mathcs.backport.java.util.Collections;
import edu.stanford.ejalbert.BrowserLauncher;

public class Main extends JFrame {
	// @since change bodies
	//body names
	public static final String MARS_STR = "Mars";
	private static String currentBody = null;
	private static String selectedBody = null;
	//directories
	private static String sessionKey = generateSessionKey();
	private static String bodyBaseDir = "bodies/"+sessionKey+"/";
	public static final String DIR_RESOURCES = "resources/";
	public static final String SESSION_KEY_STR = "sessionKey";//constant for setting and getting value in session files
	public static final String SESSION_FILE_STR = "sessionFile";
	public static final String SESSION_FILE_NAME_STR = "sessionFilename";
	public static final String BODY_FILE_MAP_STR = "bodySavedLayerMap";
	private static TreeMap<String,String[]> mapOfBodies = null;
	//file names
	private static final String LAYERS_FILE_EXT = ".jlf";
	// end change bodies
	// authentication domains
	public static final String MSFF = "msff";
	static {
		// Make sure J2SE 5 drag and drop does the right thing.
		// Note that this must be done before the look and feel is set, since
		// the class instantiation loads in SwingUtilities2, which inspects this
		// property value!
		System.setProperty("sun.swing.enableImprovedDragGesture", "true");
		
		// Disable medialib and codeclib extensions, since we have a particular
		// release of all-Java JAI that we need to use without interference from an
		// unknown release of the native JAI extension.
		System.setProperty("com.sun.media.jai.disableMediaLib", "true");
		System.setProperty("com.sun.media.imageio.disableCodecLib", "true");
		
		/*
		 * NOTE: We don't need to do this anymore - set the swing.defaultlaf system
		 * property on the command-line instead.
		 * configure the look and feel
		 * Metal isn't the most novel look and feel, but it's everywhere now
		 */
		try {
			UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
			//UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
			//UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
			//UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			//UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
			//UIManager.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel");
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		// Enable cut/copy/paste/select-all on Mac OS
		if (System.getProperty("os.name").toUpperCase().startsWith("MAC OS X")) {
			try {
				InputMap im;
				String[] fields = {"PasswordField.focusInputMap", "TextField.focusInputMap", "TextPane.focusInputMap"};
					for (String field : fields) {
						im = (InputMap) UIManager.get(field);
						im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.META_DOWN_MASK), DefaultEditorKit.copyAction);
						im.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.META_DOWN_MASK), DefaultEditorKit.cutAction);
						im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.META_DOWN_MASK), DefaultEditorKit.pasteAction);
						im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.META_DOWN_MASK), DefaultEditorKit.selectAllAction);
						// TODO: implement undo/redo support (Undo/Redo isn't defined in DefaultEditorKit)
					}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// Force US Locale.  This has the unpleasant side effect of removing a lot of nice localization things Java provides by default
		// for foreign users.  However, it also makes things like CSV import/export actually work, which seems more important.
		Locale.setDefault(Locale.US);
		
	}
	
	private static DebugLog log = DebugLog.instance();
	private static FileLogger fileLogger;
	static {
		try {
			initJMarsDir();
			fileLogger = new FileLogger();
		} catch (FileNotFoundException e) {
			log.aprintln("Unable to open file logger");
		}
	}
	
	private static String TITLE = Config.get(Util.getProductBodyPrefix()+"edition");//KJR - changed this from being final so that it can change when they select a body 11/29/2011
	private static String TITLE_FILE = "";
	private static String TITLE_LOC = "";
	private static final String EXT = Config.get(Config.CONFIG_SESSION_EXT,"."+Config.get(Config.CONFIG_PRODUCT)+"-jmars");//get body.session_ext...if not set
	
	public static UserProperties userProps;
	
	/**
	 * Temporarily stores saved layers so they can be loaded when reading the
	 * saved jlf file, and added to the LViewManager later during construction
	 * of the Main frame.
	 */
	static List<SavedLayer> savedLayers;
	
	public static long NOW = Time.getNowEt();
	
	public static final String ABOUT_URL = Config.get("homepage");
	public static final String ABOUT_EMAIL = Config.get("default_email");
	
	private static final URL VERSION_URL = getResource("resources/about.txt");
	public static boolean IN_JAR = VERSION_URL == null ? false : VERSION_URL.getProtocol().equals("jar");
	private static About CACHED_ABOUT;
	
	// parameters from the ServerAbout response
	public static String KEY;
	public static String DB_USER;
	public static String DB_PASS;
	
	
	private JDialog progressDialog = null;// @since change bodies
	
	private static void generateBodyBaseDir() {
		bodyBaseDir = "bodies/"+sessionKey+"/";
	}
	public static String getBodyBaseDir() {
		return bodyBaseDir;
	}
	private static String generateSessionKey() {
		String key = new Timestamp(new Date().getTime()).toString().replace(' ', '_').replace(':','_').replace('.','_');
		return key;
	}
	private static String getFullSessionKeyPath() {
		//only generate a new sessionKey if it is null
		if (Main.sessionKey == null) {
			Main.sessionKey = Main.generateSessionKey();
		}
		//make sure that the bodyBasDir is using the latest sessionKey
		Main.generateBodyBaseDir();
		//return the entire path to the session key directory
		return Main.getJMarsPath()+bodyBaseDir;
	}
	public static String getSessionKey() {
		return sessionKey;
	}
	/**
	* @since change bodies
	*/
	public static String getCurrentBody() {
		return currentBody;
	}
	/**
	* @since change bodies
	*/
	public static void setCurrentBody(String body) {
		currentBody = body.toLowerCase();
	}

	//@since J-all. This was moved out so that we are able to select the correct body 
	//when loading from a session file
	final ArrayList<JRadioButtonMenuItem> bodyItems = new ArrayList<JRadioButtonMenuItem>();
	
	/** Returns the local about object */
	public static About ABOUT() {
		if (CACHED_ABOUT == null) {
			try {
				CACHED_ABOUT = new About(Util.readLines(VERSION_URL.openStream()));
			} catch (Exception e) {
				// create an empty about object (so we don't just keep retrying)
				IN_JAR = false;
				CACHED_ABOUT = new About();
			}
		}
		return CACHED_ABOUT;
	}
	
	/**
	 * Parses the basic About file format, which is generated by the build
	 * process and includes information on the build time and distribution
	 * contents.
	 * 
	 * The build time is used to ensure the application is up to date, and
	 * the other info is shown to the user in the 'about' Help menu item.
	 */
	public static class About {
		protected int line = 0;
		public final String DATE;
		public final long SECS;
		public final int LINES;
		public final int PAGES;
		public final int FILES;
		public final int CLASSES;
		public About() {
			DATE = "";
			SECS = 0;
			LINES = PAGES = FILES = CLASSES = 0;
		}
		public About(String[] lines) throws IOException {
			DATE = lines[line++];
			SECS = Long.parseLong(lines[line++].trim());
			LINES = Integer.parseInt(lines[line++].trim());
			PAGES = LINES / 60;
			FILES = Integer.parseInt(lines[line++].trim());
			CLASSES = Integer.parseInt(lines[line++].trim());
			
			// validate the date, but since this is guaranteed to fail on JVM's
			// that do not support the 'en-US' Locale, we only log a warning when
			// the date is not valid
			try {
				// If date parsed fine then do a sanity check to make sure 
				// the date is within a reasonable range.
				long aboutTime = new SimpleDateFormat("E MMM dd HH:mm:ss z yyyy", new Locale("en", "US")).parse(DATE).getTime();
				
				String earliestDate = "1998-01-01";
				long earliestTime = new SimpleDateFormat("yyyy-MM-dd").parse(earliestDate).getTime();
				if (aboutTime < earliestTime){
					throw new IllegalStateException("Date is invalid: " + DATE + ".  The date can not be before " + earliestDate);
				}
				
				// Check for the lastest acceptable date.
				int daysIntoFuture = 3; // number of days into the future that are acceptable (for time zone errors).
				long currentTime = (new Date()).getTime();
				long latestTime = currentTime + (daysIntoFuture * (24*60*60*1000));
				if (aboutTime > latestTime){
					log.aprintln("Date is invalid: " + DATE + ".  The date can not be after " + (new Date(latestTime)));
				}
				
				// verify that the SECS field matches the seconds value of the DATE field.
				if (Math.abs(SECS - aboutTime/1000) > 5) {
					throw new IllegalStateException("Timestamp and date differ by more than 5 seconds at T=" + SECS);
				}
			} catch(Exception e) {
				log.aprintln("Invalid date: " + DATE + ", caused by:");
				log.aprintln(e);
			}
		}
	}
	
	/**
	 * As the normal About class, but the server produces a response with the
	 * following additional lines (in this order):
	 * <ul>
	 * <li>accesskey
	 * <li>dbuser
	 * <li>dbpass
	 * </ul>
	 */
	public static final class ServerAbout extends About {
		public final String KEY;
		public final String DBUSER;
		public final String DBPASS;
		public ServerAbout(String[] lines) throws IOException {
			super(lines);
			KEY = lines[line++];
			DBUSER = lines[line++];
			DBPASS = lines[line++];
		}
	}
	
	/**
	 ** Sets the filename and/or location string used for the titlebar
	 ** of the application.
	 **
	 ** @param fname if null, no change is made
	 ** @param loc if null, no change is made
	 **/
	public static void setTitle(String fname, String loc)
	 {
		if(fname != null)
			TITLE_FILE = fname;
		if(loc != null)
			TITLE_LOC = loc;

		mainFrame.setTitle(checkDemoOrBeta() + createTitle());
	 }
	private static String checkDemoOrBeta() {
		String returnVal = "";
		boolean isBeta = Config.get("is_beta", false);
		boolean isDemo = Config.get("is_demo", false);
		if (isBeta) {
			returnVal = "BETA ";
		} else if (isDemo) {
		    returnVal = "DEMO ";
		}
		return returnVal;
	}

	/**
	 ** Returns what the TITLE, TITLE_LOC, and TITLE_FILE variables
	 ** indicate the current title should be.
	 **/
	private static String createTitle()
	 {
		String t = TITLE;
		if(!TITLE_FILE.equals(""))
			t += " (" + TITLE_FILE + ")";
		t += " " + TITLE_LOC;
		return  t;
	 }

	/**
	 ** Replaces the whole getClass().getResource() chain of calls,
	 ** which is difficult to do statically without obfuscating our
	 ** obfuscation settings.
	 **/
	public static URL getResource(String name)
	 {
		return  Main.class.getResource("/" + name);
	 }

	/**
	 ** Replaces the whole getClass().getResource() chain of calls,
	 ** which is difficult to do statically without obfuscating our
	 ** obfuscation settings.
	 **/
	public static InputStream getResourceAsStream(String name)
	 {
		return  Main.class.getResourceAsStream("/" + name);
	 }

	/**
	 ** Utility method for retrieving the contents of a resource file
	 ** as a complete String. Returns null if the resource can't be
	 ** found.
	 **
	 ** @throws RuntimeException If some type of IO error occurs,
	 ** which should be rare. The underlying exception that caused the
	 ** problem will be chained correctly.
	 **/
	public static String getResourceAsString(String name)
	 {
		URL url = Main.class.getResource("/" + name);
		InputStream is = Main.class.getResourceAsStream("/" + name);
		if(is == null)
			return  null;

		try
		 {
			StringBuffer sb = new StringBuffer(1024);
			BufferedReader br = new BufferedReader(new InputStreamReader(is));

			char[] chars = new char[1024];
			while(br.read(chars) > -1)
				sb.append(String.valueOf(chars));

			return sb.toString();
		 }
		catch(IOException e)
		 {
			throw  new RuntimeException(
				"I/O error while reading resource file " + url, e);
		 }
		finally
		 {
			try { is.close(); } catch(Throwable e) { }
		 }
	 }

	/**
	 ** Utility method for printing the contents of a resource file
	 ** to an OutputStream.
	 **/
	public static void printResource(String name, OutputStream os)
	 throws IOException
	 {
		URL url = Main.class.getResource("/" + name);
		InputStream is = Main.class.getResourceAsStream("/" + name);
		if(is == null)
			throw new FileNotFoundException("Unable to open resource " + name);

		try
		 {
			BufferedInputStream bis = new BufferedInputStream(is);
			byte[] buff = new byte[1024];
			int numRead;
			while( (numRead = bis.read(buff)) > -1 )
				os.write(buff, 0, numRead);
		 }
		catch(Throwable e)
		 {
			throw  (IOException)
				new IOException("Error while reading " + url).initCause(e);
		 }
		finally
		 {
			try { is.close(); } catch(Throwable e) { }
		 }
	 }

	/**
	 ** If there's a newer version of JMARS available (according to
	 ** the server), a dialog box pops up warning the user. If the
	 ** server can't be contacted or its response is suspicious, a
	 ** different dialog pops up and informs the user.
	 **
	 ** <p>Otherwise, nothing visible occurs.
	 **/
	public static void checkAppVersion() {
		if (IN_JAR && !Config.get("skipabout", false) && USER != null && USER.length() > 0) {
			log.printStack(0);
			try {
				ServerAbout update = getServerAbout();
				if(update.SECS > ABOUT().SECS) {
					String msg =
						"Your version of JMARS is out of date!\n" +
						"\n" +
						"Yours was built:\n" +
						"    " + ABOUT().DATE + "\n" +
						"The server says the most recent build is:\n" +
						"    " + update.DATE + "\n" +
						"\n" +
						"It is strongly recommended that you upgrade.\n" +
						"\n";
					JOptionPane.showMessageDialog(null, msg, "JMARS", JOptionPane.WARNING_MESSAGE);
				}
			} catch(Exception e) {
				log.aprintln(e);
				String msg =
					"JMARS is unable to determine whether or not it's\n" +
					"out of date!\n" +
					"\n" +
					"This may be because the server is down, or you've\n" +
					"lost network connectivity, or even because your\n" +
					"JMARS is so old that it doesn't know how to check\n" +
					"correctly (or there is a problem with your account.)\n" +
					"\n" +
					"Your JMARS was built:\n" +
					"    " + ABOUT().DATE + "\n" +
					"\n" +
					"If you receive this message repeatedly, it might be\n" +
					"time for you to upgrade.\n" +
					"\n";
				JOptionPane.showMessageDialog(null, msg, "JMARS", JOptionPane.ERROR_MESSAGE);
			}
		}
	}


	public static ProjObj PO;
	public static Main mainFrame;
	
	/**
	 ** Returns an indication of whether or not the ruler package is
	 ** around. Should be used to firewall any ruler code, so that the
	 ** public version (which doesn't include rulers) won't throw any
	 ** errors. Based on config file info.
	 **/
	public static boolean haveRulers()
	 {
		return  "yes".equals(Config.get("rulers.enabled"));
	 }

	public static boolean isInternal()
	 {
		return  Config.get("int") != null;
	 }


	/**
	 ** If non-null, indicates an initial location (in world
	 ** coordinates) for cylindrical mode. Used only for the public
	 ** "rounded" cylindrical mode, where the initial location is NOT
	 ** the center of the projeciton.
	 **/
	public static Point2D initialWorldLocation = null;

	public static TestDriverLayered testDriver;

	/**
	 ** The username used for server authentication.
	 **/
	public static String USER = Config.get("username");

	/**
	 ** The password used for server authentication.
	 **/
	public static String PASS = "";
	
	/** JMARS authentication domain */
	public static String AUTH_DOMAIN = Config.get("auth_domain", MSFF);
	
	/** JMARS product name */
	public static String PRODUCT = Config.get("product", "jmars");
	
	/**
	 * Retrieves the {@link ServerAbout} object from the server. Tries the
	 * stored username/password (in .jmarsrc) if it's present. Re-prompts the
	 * user as necessary, when/if the login attempt fails.
	 */
	private static void authenticateUser() {
		log.printStack(0);

		// Note: these values are SAVED down in the Main.saveState() routine.
		if (USER == null) {
			USER = userProps.getProperty("jmars.user");
		}

		try {
			if (USER == null || PASS == null || PASS == "")
				promptUserPass();

			do {
				try {
					ServerAbout auth = getServerAbout();
					KEY = auth.KEY;
					DB_USER = auth.DBUSER;
					DB_PASS = auth.DBPASS;
				} catch (Exception e) {
					log.aprintln(e);
					JOptionPane.showMessageDialog(
						null,
						"There was a problem validating your username and password:\n" + e.getMessage(),
						"JMARS AUTHENTICATION",
						JOptionPane.ERROR_MESSAGE);
				}
				if (KEY == null)
					promptUserPass();
			} while (KEY == null);
		} catch (LocalAuthentication e) {
			log.aprintln("Logging in as guest user - Custom Maps and specialized user settings will be disabled.");
		}
	}

	/**
	 * Used to signal our username="x"+password="" hack to skip the
	 * authentication key.
	 **/
	private static class LocalAuthentication extends Exception
	 {
	 }

	/**
	 ** Prompts the user for their username/password, storing the
	 ** acquired values into {@link #USER} and {@link #PASS}.
	 **/
	private static void promptUserPass() throws LocalAuthentication
	 {
		class MyLabel extends JLabel
		 {
			MyLabel(String s)
			 {
				super(s);
				setAlignmentX(1);
				setAlignmentY(0.5f);
			 }
		 }

		class MyBox extends Box
		 {
			MyBox(JComponent a, JComponent b)
			 {
				super(BoxLayout.Y_AXIS);
				add(a);
				add(b);
			 }
		 }

		final JTextField txtUser = new JTextField(USER);
		if (USER == null || "".equals(USER)) {
		    String uid = Config.get("userid","");
		    txtUser.setText(uid);
		}
		final JPasswordField txtPass = new JPasswordField();

		txtUser.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
				// Do not let the Dialog OK button steal the default focus from us
	            if (e.getOppositeComponent() instanceof JButton) {
	            	txtUser.grabFocus();
	            	txtUser.removeFocusListener(this);
	            }
			}
		
			public void focusGained(FocusEvent e) {
				// nop
			}		
		});

		txtPass.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
				// Do not let the Dialog OK button steal the default focus from us
	            if (e.getOppositeComponent() instanceof JButton) {
	            	txtPass.grabFocus();
	            	txtPass.removeFocusListener(this);
	            }
			}
		
			public void focusGained(FocusEvent e) {
				// nop
			}		
		});
		
		if (Main.autosaveFile.lastModified() > 0) {
			cbRestore.setToolTipText("Last saved " + new SimpleDateFormat("EEE h:mm aaa, d MMM yyyy").format(new Date(Main.autosaveFile.lastModified())));
		} else {
			//the following code is to support old default.jlf files that may exist from previous versions
			File oldAutosaveFile = new File(Main.getJMarsPath() + "default.jlf");
			if (oldAutosaveFile.exists() && oldAutosaveFile.canRead()) {
				oldAutosaveFile.renameTo(autosaveFile);
				cbRestore.setToolTipText("Last saved " + new SimpleDateFormat("EEE h:mm aaa, d MMM yyyy").format(new Date(Main.autosaveFile.lastModified())));
				//end support for previous versions
			} else {
				cbRestore.setToolTipText("No autosave file found");
				cbRestore.setEnabled(false);
			}
		}
		

		boolean allowGuests=Config.get("guestlogin", "yes").endsWith("yes");
		JPanel gPanel = new JPanel();
		JButton guestLogin = null;

		if (allowGuests) {
			guestLogin = new JButton();
			gPanel.add(guestLogin);
		}
		Box fields = new Box(BoxLayout.X_AXIS);
		fields.add(new MyBox(new MyLabel(Config.get(Util.getProductBodyPrefix()+"login.lbl") + " "), new MyLabel("Password: ")), BorderLayout.WEST);// @since change bodies – added prefix
		fields.add(new MyBox(txtUser, txtPass), BorderLayout.CENTER);

		URL url=null;
		ImageIcon jmarsLogo=null;
		JLabel iconLbl = null;
		try {
			url = new URL(Config.get("loginLogoUrl"));
			jmarsLogo = new ImageIcon(ImageIO.read(url));
			iconLbl = new JLabel(jmarsLogo);
		} catch (Exception e1) {
		}
		
		if (url == null || iconLbl == null){
			iconLbl = new JLabel("Welcome to "+checkDemoOrBeta() + TITLE);
			iconLbl.setFont(new Font("Dialog",Font.BOLD+Font.ITALIC,18));
		}

		String hideRegLink = Config.get("hideRegPageLink");
		Object[] loginDialogContents;
		if(hideRegLink != null && !hideRegLink.equalsIgnoreCase("false")) {
			loginDialogContents = new Object[] {
				iconLbl, "\n", fields, cbRestore,
				"\nJMARS homepage: ",new UrlLabel(Config.get("homepage"), null),"Version: "+Util.getVersionNumber() + " " +checkDemoOrBeta(), "\n"};
		} else {
			loginDialogContents = new Object[] {
				iconLbl, "\n", fields, cbRestore, gPanel,
				"\nTo register, please visit:\n",
				new UrlLabel(Config.get("registrationpage"), null),
				"JMARS homepage: ",new UrlLabel(Config.get("homepage"), null),"Version: "+Util.getVersionNumber() + " " +checkDemoOrBeta()};		
		}
		
		final JOptionPane op =
			new JOptionPane(
				loginDialogContents,
				JOptionPane.PLAIN_MESSAGE,
				JOptionPane.OK_CANCEL_OPTION
				);
		

		//this is the java 1.5 way of doing this.
		JFrame tempFrame = new JFrame();
		tempFrame.setIconImage(Util.getJMarsIcon());
		//end java 1.5 way of doing this.
		final JDialog dialog = op.createDialog(
			tempFrame, checkDemoOrBeta() + TITLE);// @since change bodies
		//java 1.6 way of making the icon show up
		//dialog.setIconImage(Util.getJMarsIcon());
		//end java 1.6 way
        dialog.addWindowListener(
			new WindowAdapter()
			 {
				public void windowActivated(WindowEvent we)
				 {
					if(txtUser.getText().equals(""))
						txtUser.grabFocus();
					else
						txtPass.grabFocus();
                }
			 }
			);
        
		dialog.setResizable(false);
		Action guestAct = new AbstractAction("Login as Guest") {	
			public void actionPerformed(ActionEvent e) {
				txtUser.setText("");
				txtPass.setText("");
				op.setValue(JOptionPane.OK_OPTION);
				dialog.setVisible(false);
			}
		};
		
		if (allowGuests) {
			guestLogin.setAction(guestAct);
		}
		
		dialog.setVisible(true);
		
		if(!new Integer(JOptionPane.OK_OPTION).equals(op.getValue()))
		 {
			log.println("User exited.");
			System.exit(-1);
		 }
		USER = txtUser.getText();
		PASS = new String(txtPass.getPassword());
		if (cbRestore.isSelected()) {
			try {
				FileInputStream bis = new FileInputStream(autosaveFile);
				savedLayers = SavedLayer.load(bis);
				bis.close();
			} catch (Exception e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(
					Main.mainFrame,
					"Unable to restore autosaved sessions:\n\n" + e.getMessage() + "\n\nSee log for more details",
					"Error restoring autosave",
					JOptionPane.ERROR_MESSAGE);
			}
		}
		
		Config.set("userid", USER);
		if (!Config.get("login_required", true) && (USER.equals("") || USER.equalsIgnoreCase("x"))  &&  PASS.equals(""))
		 {
			USER = "";
			throw  new LocalAuthentication();
		 }
	 }
	
	/**
	 * Using the current USER and PASS, retrieves and returns the
	 * server key.
	 * @return The about object if the USER/PASS combination is valid, null if not.
	 * @throws IOException If there is an error connecting or parsing the document.
	 * @throws URISyntaxException if the URI is malformed
	 */
	private static ServerAbout getServerAbout() throws IOException, URISyntaxException {                  // TODO (PW) Remove commented-out code
//		PostMethod post = null;
        JmarsHttpRequest request = null;
	    try {
			String url = Config.get("auth");
			log.println("Retrieving key from " + url);
			
			URI uri = Util.getSuffixedURI(new URI(url),
					"product="+PRODUCT,
					"domain="+AUTH_DOMAIN);
			request = new JmarsHttpRequest(uri.toString(), HttpRequestType.POST);
//			post = new PostMethod(uri.toString());
//			post.setRequestBody(new NameValuePair[] {
//				new NameValuePair("user",USER),
//				new NameValuePair("pass",PASS),
			request.addRequestParameter("user", USER);
			request.addRequestParameter("pass", PASS);

//          post.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
            request.setBrowserCookies();
            request.setLaxRedirect();

//			int code = Util.postWithRedirect(new HttpClient(), post, 3);
            boolean successful = request.send();
            if (!successful) {
				throw new IllegalStateException("Server gave unexpected response code " + request.getStatus());
			}
			String[] lines = Util.readLines(request.getResponseAsStream());
			if (lines.length > 0 && lines[0].startsWith("ERROR!")) {
				throw new IllegalStateException(Util.join("\n", lines));
			}
			try {
				return new ServerAbout(lines);
			} catch (Exception e) {
				if (lines.length > 10) {
					String[] temp = new String[10];
					System.arraycopy(lines, 0, temp, 0, 10);
					lines = temp;
				}
				throw new IllegalStateException("Unable to parse server response:\n" + Util.join("\n", lines), e);
			}
		} finally {
			if (request != null) {
				request.close();
			}
		}
	}
	
	// Daemon timer thread for server authentication and versioning.
	private static Timer timer = new Timer(true);

     public Main()
     {
         this(true);
     }
     
     /**
      * @param createUI Controls whether or not a UI is created and
      * displayed; <code>null</code> means do not create UI, but
      * do initialize various state variables.
      */
	 private Main(boolean createUI)
	 {
		//do this by default for the window list, taskbar, etc. that is platform dependent. Once we display the frame, we will change the title to be correct
		 super(Config.get("window_label", "JMARS"));
		 
		 Date d = new Date();
         log.aprintln("=======================New Login "+d.toString()+"===============================");
		 
         setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		 
		 // Set the frame icon
		 setIconImage(Util.getJMarsIcon());

		 // Test for and setup as necessary the JMARS and TILES directories
		 initJMarsDir();

		if (createUI)
		{
			// build the main view and load in any properties from the config file, if
			// there is one.
			testDriver = new TestDriverLayered();

			// Instantiate LManager
			new LManager(testDriver.mainWindow);

			Main.testDriver.setLManagerMode(LManagerMode.Verti);
			
			// The menu bar adds 23 pixels to the height of the panel.
			initMenuBar();

			// Set the tool-tip delay if that property exists in the config file.
			ToolTipManager ttm = ToolTipManager.sharedInstance();
			int tooltipInitialDelay = Config.get("tooltip.initial", ttm.getInitialDelay());
			ttm.setInitialDelay(Math.abs(tooltipInitialDelay));
			int tooltipDismissDelay = Config.get("tooltip.dismiss", ttm.getDismissDelay());
			ttm.setDismissDelay(Math.abs(tooltipDismissDelay));

			// Set up the RulerManager ( only one allowed ) before
			// building the layers, so that the layers can create their
			// own rulers as necessary. The original layered pane is now a
			// child of the RulerManager instead of the Main window panel.
			RulerManager.Instance.setContent( (JComponent)testDriver, this);
			setContentPane( RulerManager.Instance);
			RulerManager.Instance.hideAllDragbars( true);

			// If there are any views to be built, build them now.
			// Previously, this happened when the testDriver was created.
			// But now the RulerManager content must be set before any
			// views are created, otherwise the rulers will never be created.
			// The code to do this is therefore sandwiched in betweeen 
			// creating the testDriver and building the views.
			testDriver.buildViews();

	    	
			// Get the properties of the rulers, if such there be.
			// This must be done AFTER views are created because rulers
			// are created when views are and only the properties of 
			// created rulers are loaded.  
			testDriver.loadRulersState();

			// Once everything is loaded, we
			// need to update the tabbed pane so that any loaded properties
			// show up in the panel.
			// RulerManager.Instance.updatePropertiesTabbedPane();

			// Load the LayerParameters list either from the backend or a 
			// cached file if available.
			LayerParameters.resetLayerParameters();
			
			// reset the parameters of the Layer Manager.
			LManager.getLManager().loadLManagerState();

			// Set up a location listener so that the rulers will be updated even
			// if the layer is turned off.
			RulerManager.Instance.setLViewManager( testDriver.mainWindow);

			// Resize everything and display.
			RulerManager.Instance.packFrame();
			Util.centerFrame(this);
			
			//takes the focus off the location manager...so cursor shortcuts work
			testDriver.mainWindow.getGlassPanel().requestFocusInWindow();

			userProps.setWindowPosition(this);
			setVisible(true);
			setTitle(checkDemoOrBeta() + createTitle());
		}
	 }
	
	private static final boolean confirmExit = Config.get("main.close_confirm", false);
	
	protected void processWindowEvent(WindowEvent e) {
		if (e.getID() == WindowEvent.WINDOW_CLOSING) {
			if (!confirmExit || JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
					this, "Exit " + TITLE + " ?", "Confirm Exit",// @since change bodies
					JOptionPane.YES_NO_OPTION)) {
				// user wasn't asked, or confirmed exit, so autosave and pass the event on
				try {
					Main.cleanSessionKeyDirectories();
					autosave();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				super.processWindowEvent(e);
			}
		} else {
			// not a window closing event, so just pass it on
			super.processWindowEvent(e);
		}
	}
	
	private static void cleanSessionKeyDirectories() {
		try {
			File bodyDir = new File(getJMarsPath()+"bodies");
			if (bodyDir.exists()) {
				Main.deleteSessionKeyDirectory();
				//remove empty dirs that should not exist
				String[] subFiles = bodyDir.list();
				for(String oneFileName: subFiles) {
					File oneFile = new File(oneFileName.trim());
					if (oneFile.isDirectory()) {
						//only delete empty directories because directories with files in them might be for another session of JMARS
						if (oneFile.list().length == 0) {
							Util.recursiveRemoveDir(oneFile);
						}
					}
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
			log.println(e);
		}
	}
    static public void initJMarsDir()
    {
        File myDir = new File(getJMarsPath());
        boolean failure = false;
        try {
        	if (!myDir.exists() || !myDir.isDirectory()) {
        		log.print("Attempting to create JMARS directory in Main....");
        		//JMARS directory doesn't exist or is not a directory, try to create it
        		if (!(myDir.mkdirs())) {
        			//we could not create the JMARS directory...now we are sure we have a real problem. Alert the user and exit.
        			failure = true;
        		}
        	}
        } catch (SecurityException se) {
        	//security exception means we don't have access to read this dir
        	log.println(se.getMessage());
        	se.printStackTrace();
        	failure = true;
        } catch (Exception e) {
        	e.printStackTrace();
        	log.println(e.getMessage());
        	failure = true;
        }
        
        if (failure) {
        	//maybe we should add a file dialog here and let them select a directory?
        	Main.showFailureDialogAndExit();
        }
    }
    private static void showFailureDialogAndExit() {
    	final JDialog failureDialog = new JDialog();
		failureDialog.setModal(true);
		failureDialog.setLayout(new FlowLayout());
		failureDialog.setSize(500, 200);
		JTextPane textPane = new JTextPane();
		textPane.setText("The JMARS home directory could not be created or found. JMARS is exiting.");
		textPane.setSize(60, 60);
		failureDialog.add(textPane);
		JButton okButton = new JButton("Exit");
		okButton.setSize(100, 20);
		okButton.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		failureDialog.add(okButton);
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension d = failureDialog.getSize();
		int x = (screen.width - d.width) / 2;
		int y = (screen.height - d.height) / 2;
		failureDialog.setLocation(x, y);
		failureDialog.setVisible(true);
		System.exit(0);
    }
	public static void setStatus(String status)
	 {
	    if ( status == null )
	        return;

		JLabel text = Main.testDriver.statusBar;
		text.setText(status);
	 }
	
	/**
	 * Sets the status bar text based on the given sequence of world points,
	 * indicating position of the last point and, if multiple points are
	 * provided, the total distance traversed by great circles connecting
	 * those points.
	 * 
	 * This method approximates ellipsoidal curvature by segmenting each line
	 * into sublines which are never larger than 5 degrees, and projecting up to
	 * the ellipse. This method works well for all mostly-spherical target
	 * bodies. The radii are those defined in {@link Util#MARS_EQUAT} and
	 * {@link Util#MARS_POLAR}.
	 * 
	 * This method formats the position and distance values to have just enough
	 * digits of precision based on the PPD of the main view.  The values may
	 * be thought of as precise enough to indicate the position of the mouse and
	 * the distance the mouse has been moved, in pixels at the current scale.
	 */
	public static void setStatusFromWorld(Point2D ... points) {
		Point2D spatial = Main.PO.convWorldToSpatial(points[points.length-1]);
		StringBuffer buff = new StringBuffer(20);
		buff.append(statusFormat(spatial));
		double[] angLin = Util.angularAndLinearDistanceWorld(points);
		double degs = angLin[0];
		double km = angLin[1];
		DecimalFormat fmtDeg = getFormatter(1);
		DecimalFormat fmtKm = getFormatterKm(1);
		if (km > 0) {
			buff.append("\tDistance = " + fmtDeg.format(degs) + "\u00B0 = " + fmtKm.format(km) + " km");
		}
		Main.setStatus(buff.toString());
	}
	
	/** Gets a formatter that will provide just better than pixel level precision at the current ppd for the given units per kilometer. */
	public static DecimalFormat getFormatterKm(double unitsPerKm) {
		return getFormatter(unitsPerKm * 2*Math.PI*Util.EQUAT_RADIUS/360);
	}
	
	/** Gets a formatter that will provide just better than pixel level precision at the current ppd for the given units per degree. */
	public static DecimalFormat getFormatter(double unitsPerDeg) {
		// get pixels/degree; we want # of base 10 digits
		int pixPerDeg = Main.testDriver.mainWindow.getZoomManager().getZoomPPD();
		// get pixels/unit; we want # of base 10 digits
		double pixPerUnit = pixPerDeg / unitsPerDeg;
		// convert to digits by getting base 10 log of each value, shifted up a
		// bit so we can see the digits where meaningful rounding occurs
		int unitDigits = Math.max(0, (int)Math.round(Math.log(pixPerUnit)/Math.log(10) + 0.5));
		DecimalFormat fmt = new DecimalFormat("0.00");
		fmt.setMinimumFractionDigits(unitDigits);
		fmt.setMaximumFractionDigits(unitDigits);
		return fmt;
	}
	
	/**
	 * Format the given spatial west point into a String that has the same
	 * spacing as used by the LViewManager's default mouseMoved event does.
	 * Useful for other mouse listeners that want to format a position in
	 * the same was as normal, but for another position.
	 */
	public static String statusFormat(Point2D spatialWest) {
		StringBuffer buff = new StringBuffer(20);
		DecimalFormat fmtDeg = getFormatter(1);
		
		// Format the longitude
		double x = 360-spatialWest.getX();
		double xa = Math.abs(x);
		if (xa < 100)
			buff.append("  ");
		if (xa < 10)
			buff.append("  ");
		buff.append(fmtDeg.format(x));
		buff.append("\u00B0E ");
		
		// Format the latitude
		double y = spatialWest.getY();
		double ya = Math.abs(y);
		if (y > 0)
			buff.append("  ");
		if (ya < 10)
			buff.append("  ");
		buff.append(fmtDeg.format(y));
		buff.append("\u00B0N");
		
		return buff.toString();
	}
	private static String userHomeDirectory = null;
	/** Return the user home directory */
	public static String getUserHome() {
		if (Main.userHomeDirectory == null) {
			//get userHomeDir passed in first
			log.println("Attempting to get user home directory from java parameter. ");
			String testVal = System.getProperty("userHomeDir");
			if (testVal == null || testVal.equals("null")) {
				log.println("Did not find java user home parameter. Getting user.home");
				//didn't find it. Use user.home
				Main.userHomeDirectory = System.getProperty("user.home");
				log.println("Using User.home : "+ Main.userHomeDirectory);
			} else {
				log.println("Found Java user home parameter: "+testVal);
				Main.userHomeDirectory = testVal;
			}
		}
		if (Main.userHomeDirectory == null) {
			Main.showFailureDialogAndExit();
		}
		
		return Main.userHomeDirectory;
	}
	
    /**
     * Returns path to base JMARS directory, under which other files and directories
     * (such as the map tiles cache or stamp list data cache files) may be stored. 
     */
    public static String getJMarsPath() {
        return getUserHome() + File.separator + "jmars" + File.separator;
    }

	private static void initiateAuthentication()
	 {
		authenticateUser();

		final int delayMS = 1000 * 60 * 30; // 30 minutes
		final int delayForVersionMS = 1000 * 60 * 60 * 24; // one day

		TimerTask checkServerTask =
			new TimerTask()
			 {
				long lastTime = System.currentTimeMillis();
				long lastTimeVer = System.currentTimeMillis();
				public void run()
				 {
					long currTime = System.currentTimeMillis();
					// Prevent the normal timer behavior of
					// queuing a single extra overlapped timer
					// task.
					if(currTime - lastTime < delayMS/2)
						return;

					try
					 {
						authenticateUser();
					 }
					catch(Throwable e)
					 {
						log.aprintln("FAILED PERIODIC USER KEY CHECK:");
						log.aprintln(e);
					 }
					try
					 {
						if(currTime - lastTimeVer > delayForVersionMS)
						 {
							lastTimeVer = currTime;
							checkAppVersion();
						 }
					 }
					catch(Throwable e)
					 {
						log.aprintln("FAILED PERIODIC APP VERSION CHECK:");
						log.aprintln(e);
					 }

					lastTime = System.currentTimeMillis();
				 }
			 }
			;

		// Only schedule this check if the user actually logged in.
		if (USER!=null && PASS!=null && USER.length()>0 && PASS.length()>0) {
			timer.schedule(checkServerTask, delayMS, delayMS);
		}

	 }
	
	private static final File autosaveFile = new File(Main.getJMarsPath() + "jmars_autosave.xml");
	
	/** Defaults to one minute, but can be overridden in jmars.config */
	private static final long autosaveDelay = Config.get("autosave.delay", 60000);
	
	/**
	 * The max duration in milliseconds that autosave can take before all
	 * subsequent autosaves will be disabled. This does not stop autosave after
	 * this amount of time, but if autosave takes a really long time for any
	 * reason, this will prevent it from repeatedly doing so.
	 */
	private static final long autosaveDuration = Config.get("autosave.duration", 5000);
	
	/** Saves a jlf file of all the layers, with gaps of at least one minute between the end of the last save and the start of the next. */
	private static void initiateSaving() {
		timer.schedule(new TimerTask() {
			long lastTime = System.currentTimeMillis();
			public void run() {
				long thisTime = System.currentTimeMillis();
				// save if 'delay' time has passed since the last save, with one second of slop
				if (thisTime - lastTime > autosaveDelay - 1000) {
					try {
						autosave();
						log.println("Auto-save finished in " + (thisTime - lastTime) + " ms");
					} catch (Exception e) {
						cancel();
						log.aprintln("Auto-save disabled; an error occurred:");
						log.aprintln(e);
					}
				}
				lastTime = System.currentTimeMillis();
				if (lastTime - thisTime > autosaveDuration) {
					cancel();
					log.aprintln("Auto-save disabled; the operation took too long (" + (lastTime - thisTime) + " ms)");
				}
			}
		}, autosaveDelay, autosaveDelay);
	}
	//@since 3.0.2 - autosave() saves places out. To avoid saving the places xml twice for no reason, separated out the methods.
	private static void autosave() throws IOException {
		saveLayers (autosaveFile);
		savePlaces(places);
	}
	
	private static void savePlaces(PlacesMenu placesInstance) {
		// @since change bodies
		if (placesInstance != null) {
			placesInstance.save();
		}
	}
	// @since change bodies
	private static void saveLayers(File saveFile) throws IOException {
		final List<SavedLayer> layers = new ArrayList<SavedLayer>();
		// Ensure we are on the AWT event thread when copying values.
		Runnable copyData = new Runnable() {
			public void run() {
				for (LView view: testDriver.mainWindow.viewList) {
					layers.add(new SavedLayer(view));
				}
			}
		};
		try {
			if (SwingUtilities.isEventDispatchThread()) {
				copyData.run();
			} else {
				SwingUtilities.invokeAndWait(copyData);
			}
		} catch (Exception e) {
			// if anything goes wrong, do not replace a potentially good
			// session with nothingness, just report and get out.
			e.printStackTrace();
			return;
		}
		// The remainder of the save operation is done on the calling thread
		// which ensures we spend a minimum of time in the AWT thread if we
		// are calling from some other thread (like the timer thread.)
		Collections.reverse(layers);
		FileOutputStream fos = new FileOutputStream(saveFile);
		BufferedOutputStream bos = new BufferedOutputStream(fos);// @since change bodies
		SavedLayer.save(layers, bos);
		bos.flush();
		fos.close();
		bos.close();
	}
	
	private static void processFlagMultiHead(String arg, ListIterator iter)
	 {
		if(arg.equals(":"))
		 {
			Util.printAltDisplays();
			System.exit(0);
		 }

		Util.setAltDisplay(arg);
		iter.remove();
	 }
	
	public static void cleanCache()
	 {
		log.aprintln("------ CLEANING! -------");

		// Delete the whole cache directory for edu.asu.jmars.layer.map2
		File[] files = new File(CacheManager.getCacheDir()).listFiles();
		if (files != null) {
			for (File f: files) {
				Util.recursiveRemoveDir(f);
			}
		}
		
		FilenameFilter capFilter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.toLowerCase().startsWith("wms_") && name.toLowerCase().endsWith(".xml");
			}
		};
		
		for (File capXml: new File(Main.getJMarsPath()).listFiles(capFilter)) {
			capXml.delete();
		}
		
		// Delete the stamp cache directory for edu.asu.jmars.layer.stamp
		StampCache.cleanCache();
		
		
		//Delete the layerParams cache
		LayerParameters.cleanCache();
	 }

	private static void processFlagSlideshow(String arg, ListIterator iter)
	 {
		log.aprintln("----- SLIDESHOW MODE! -----");

		try
		 {
			new Thread(new Slideshow(System.in)).start();
		 }
		catch(Throwable e)
		 {
			log.aprintln(e);
			log.aprintln("UNABLE TO START SLIDE SHOW DUE TO ABOVE!");
			System.exit(-1);
		 }

		iter.remove();
	 }
    
    /** Either starts up centered on the given lon/lat, or if isInternal() is false, starts up in a rounded projection where longitude is rounded to the nearest 90 degrees and latitude is rounded to the nearest 45 degrees. */
	public static void startupInCyl(String dispLon, String dispLat) {
		if (isInternal()) {
			log.aprintln("You're using the CYLINDRICAL projection.");
			
			TITLE_LOC = dispLon + "E " + dispLat + "N";
			// USER east lon => JMARS west lon
			double latnum = Double.parseDouble(dispLat);
			double lonnum = (360 - Double.parseDouble (dispLon) % 360.0) % 360.0;
			if (Math.abs (latnum) <= 90.0) {
				PO = new ProjObj.Projection_OC(lonnum, latnum);
			//If on earth, and no command line args are specified, start up on
			// user's current location	
//				if(numOfArgs == 0 && getBody().equalsIgnoreCase("earth")){
//					String[] lonlat = getHomeLocation();
//					Double lonD = 360-Double.parseDouble(lonlat[0]);
//					Double latD = Double.parseDouble(lonlat[1]);
//					initialWorldLocation = PO.convSpatialToWorld(lonD,latD);
//				}
//				else{
					initialWorldLocation = PO.convSpatialToWorld(lonnum, latnum);
//				}	
			} else {
				log.aprintln ("Unable to process latitudes greater than 90 degrees!");
				System.exit (-1);
			}
		} else {
			// View center
			double lon = Double.parseDouble(dispLon);
			double lat = Double.parseDouble(dispLat);

			if(lon < 0  ||  lon > 360)
			 {
				System.out.println("Bad longitude: must be between 0 and 360!");
				System.out.println("Start with -help for usage.");
				System.exit(-1);
			 }
			if(lat < -90  ||  lat > 90)
			 {
				System.out.println("Bad latitude: must be between -90 and +90!");
				System.out.println("Start with -help for usage.");
				System.exit(-1);
			 }

			// USER east lon => JMARS west lon
			lon = (360 - lon) % 360;

			// Determine projection center (?: and abs() force northern hemisphere)
			double plon = lat >= 0 ? lon : (lon+180)%360;
			double plat = Math.abs(lat);
			// Polar
			if(plat > (90+45)/2.)
			 {
				plat = 90;
				plon = 0;
			 }
			// Equatorial
			else if(plat < (0+45)/2.)
			 {
				plat = 0;
				plon = 0;
			 }
			// One of the 45-degree ones
			else
			 {
				plat = 45;
				plon = Math.round(plon / 90) * 90 % 360;
			 }

			// JMARS west lon => USER east lon
			TITLE_LOC = (int) (360-plon)%360 + "E " + (int) plat + "N";
			PO = new ProjObj.Projection_OC(plon, plat);
		//If on earth, and no command line args are specified, start up on
		// user's current location	
//			if(numOfArgs == 0 && getBody().equalsIgnoreCase("earth")){
//				String[] lonlat = getHomeLocation();
//				System.out.println(lonlat[0]+" "+lonlat[1]);
//				Double lonD = 360-Double.parseDouble(lonlat[0]);
//				Double latD = Double.parseDouble(lonlat[1]);
//				initialWorldLocation = PO.convSpatialToWorld(lonD,latD);
//			}
//			else{
				initialWorldLocation = PO.convSpatialToWorld(lon, lat);
//			}
			log.aprintln("You're using the CYLINDRICAL projection.");
			log.println("Your display is centered at " + lon + "W " + lat);
			log.println("Your projection is centered at " + plon + "W " + plat);
			log.println("Your initial world pt is near " +
						(int) initialWorldLocation.getX() + "x " +
						(int) initialWorldLocation.getY() + "y");
		}
	}
	
	private static void startupFromFile(String fname)
	 {
		try {

			boolean fromSessionFile = false;
			boolean fromLayersFile = false;
			if (fname!=null) {
				try {
					URL url; 
					
					url = new URL(fname);
					String protocol = url.getProtocol();
					if (protocol!=null && protocol.length()>1) {
						log.aprintln("LOADING FROM REMOTE FILE " + fname + " using " + protocol);
						
                        String urlStr = url.toString();
                        JmarsHttpRequest req = new JmarsHttpRequest(urlStr, HttpRequestType.GET);
                        boolean goodConnection = req.send();
                        if (!goodConnection) {
                             int httpStatus = req.getStatus();
                             System.out.println(httpStatus);
                        }

						if (fname.indexOf(EXT) > -1) {
							fromSessionFile = true;
							userProps = new UserProperties(req.getResponseAsStream());
						} else {
							try {
								List<SavedLayer> layers = SavedLayer.load(req.getResponseAsStream());
								if (layers.size() > 0) {
									fromLayersFile = true;
									savedLayers = layers;
									userProps = new UserProperties();
								}
							} catch (Exception e) {
								// fall back to old session files
								userProps = new UserProperties(req.getResponseAsStream());
							}
						}
						req.close();
					}
				} catch (java.net.MalformedURLException | URISyntaxException me) {
					// Not a valid URL
				}
			}
			
			if (userProps == null && savedLayers == null) { // fname isn't a URL
				// Attempt to open local properties file to restore state
				File f = new File(fname);
				log.aprintln("LOADING FROM SAVED FILE " + fname);
				//3/8/12 - the logic was such that we would try jlf first, if an Exception was thrown, we would load the session file. This was probably in an attempt
				//to phase out the session files. However, many use them, so I am putting a check in for .jmars file types
				
				if (fname.indexOf(EXT) > -1) {
					fromSessionFile = true;
					userProps = new UserProperties(f);
					getRcFileChooser().setSelectedFile(f);
				} else {
					try {
						FileInputStream fis = new FileInputStream(f);
						List<SavedLayer> layers = SavedLayer.load(fis);
						fis.close();
						if (layers.size() > 0) {
							fromLayersFile = true;
							savedLayers = layers;
							userProps = new UserProperties();
						}
					} catch (Exception e) {
						// fall back to old session files
						userProps = new UserProperties(f);
						getRcFileChooser().setSelectedFile(f);
						if(userProps.getPropertyBool("TimeProjection")) {
							JOptionPane.showMessageDialog(Main.mainFrame,
								"Time Mode Session Files cannot be used since Time Mode has been removed from JMARS!",
								"Session File Unusable",
								JOptionPane.ERROR_MESSAGE);
							throw new IllegalArgumentException("Invalid session file");
						}
					}
				}
			}
			//@since J-all. This section is executed when we are starting up from a session file. This process is 
			//a bit different than loading a session from the File menu. The main difference is that we have not yet
			//loaded the map servers, etc. This process will first determine whether there is a selected body specified
			//in the session file. If there is not, it will prompt the user to select a body for this session. Once the
			//user selects the body, it will make the proper changes and then continue with startup. If
			//there is a body in the session file, it will get it out and compare it to the body that would have been 
			//used for startup. Again, if it is different, it will make the changes and then continue with startup.
			//This is a synchronous process, so we will wait for a response from the user if they are prompted to select a body.
			
		
			String tempSelectedBody = Config.get(Config.CONFIG_SELECTED_BODY, "Mars");//get the selected body from config
			String tempSessionSelectedBody = null;
			if (fromSessionFile) {
				tempSessionSelectedBody = userProps.getProperty("selectedBody");//get the selected body from the session
			} else if (fromLayersFile) {
				tempSessionSelectedBody = savedLayers.get(0).bodyName;
			}
			if ((fromSessionFile || fromLayersFile) && tempSessionSelectedBody == null) {
				//attribute is not in session file, we need to prompt the user for the body
				final JDialog bodyDialog = new JDialog();
				bodyDialog.setModal(true);
				bodyDialog.setLayout(new FlowLayout());
				bodyDialog.setSize(300, 200);
				
				//build a list of bodies
				TreeMap<String, String[]> mapOfBodies = Util.getBodyList();
				ArrayList<String> bList = new ArrayList<String>();
				Iterator<Entry<String, String[]>> iter = mapOfBodies.entrySet().iterator();
				while (iter.hasNext()) {
					String[] bodyArr = (String[]) ((Entry<String, String[]>) iter.next()).getValue();
					for(String bdy : bodyArr) {
						bList.add(bdy.toUpperCase());
					}
				}
				JTextPane textPane = new JTextPane();
				String sessionOrLayer = "session";//just changing the message
				if (fromLayersFile) {
					sessionOrLayer = "layers file";
				}
				textPane.setText("Please select the planetary body associated\n with this "+sessionOrLayer+" . To avoid this in the future,\n please re-save the "+sessionOrLayer+".");
				textPane.setSize(60, 60);
				bodyDialog.add(textPane);
				
				final JComboBox bodyCombo = new JComboBox(bList.toArray(new String[]{}));
				bodyCombo.setSize(100, 20);
				bodyDialog.add(bodyCombo);
				JButton selectButton = new JButton("Select");
				selectButton.setSize(100,100);
				selectButton.addActionListener(new ActionListener() {
					
					public void actionPerformed(ActionEvent e) {
						//the user has selected a body, use that body
						String tempBody = (String) bodyCombo.getSelectedItem();
						Main.currentBody = tempBody.toLowerCase();
						Main.selectedBody = Main.currentBody;
						Config.set(Config.CONFIG_SELECTED_BODY, selectedBody);
						Util.updateRadii();
						TITLE = Config.get(Util.getProductBodyPrefix()+Config.CONFIG_EDITION);
						bodyDialog.dispose();
					}
				});
				
				bodyDialog.add(selectButton);
				Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
				Dimension d = bodyDialog.getSize();
				int x = (screen.width - d.width) / 2;
				int y = (screen.height - d.height) / 2;
				bodyDialog.setLocation(x, y);
				bodyDialog.setVisible(true);
			} else if ((fromSessionFile || fromLayersFile) && !tempSessionSelectedBody.equalsIgnoreCase(tempSelectedBody)) {
				//Since we are in here, we found a body in the session file. Also, it is different than the last body used.
				Main.currentBody = tempSessionSelectedBody.toLowerCase();
				Main.selectedBody = Main.currentBody;
				Config.set(Config.CONFIG_SELECTED_BODY, selectedBody);
				Util.updateRadii();
				TITLE = Config.get(Util.getProductBodyPrefix()+Config.CONFIG_EDITION);
			}
			if (fromSessionFile) {
				//We are starting up, so we should not have a session key directory to delete
				//get the session key from the session file. If it does not exist, generate a new one.
				//no matter where we got the sessionKey from, we need to write out the body layer files under that directory.
				//whenever we load a session, we create a new sessionKey and create a new session key directory for the bodies to make sure that we 
				//do not corrupt any other session
				Main.deleteSessionKeyDirectory();
				Main.sessionKey = Main.generateSessionKey();
				String sessionKeyPath = Main.getFullSessionKeyPath();	
				File bbDir = new File(sessionKeyPath);
				bbDir.mkdir();
				//The following section is for body switching. When the user saved the session, the SavedLayers files for each body
				//were written out to the session. This section will read in those objects from the session and re-create the files
				
				//no way to not have type safety warning here since we are deserializing the HashMap. It is not worth any possible solution that
				//would check every value in the map and make sure that it of the right type. 
				HashMap<String,ArrayList<SavedLayer>> bodyFileMap = (HashMap<String,ArrayList<SavedLayer>>) userProps.loadUserObject(Main.BODY_FILE_MAP_STR);
				if (bodyFileMap != null && !bodyFileMap.isEmpty()) {
					Iterator<String> iter = bodyFileMap.keySet().iterator();
					while (iter.hasNext()) {
						String fileName = iter.next();
						String newFileName = fileName;
						//version 3.0.0 used .xml as the extention. This will check for that file extension, replace with .jlf and eventually phase out the need for it.
						if (fileName.endsWith(".xml")) {
							newFileName = fileName.substring(0,fileName.lastIndexOf(".xml"));
							newFileName += Main.LAYERS_FILE_EXT;
						}
						try {
							FileOutputStream fos = new FileOutputStream(new File(sessionKeyPath+newFileName));
							SavedLayer.save((ArrayList<SavedLayer>)bodyFileMap.get(fileName), fos);
							fos.close();
						} catch (Exception e) {
							//if we fail on one file, move on to the next
							log.println("Failed to load saved layer file: "+fileName);
						}
					}
				}
				String versionNum = userProps.getProperty("versionNumber");
				if (versionNum != null && versionNum.trim().equalsIgnoreCase("3.0.0")) {
					//for version 3.0.0, we put saved layer files in the session as String properties. We will
					//handle that temporarily even though it was changed in 3.0.1 to be serialized in a HashMap
					//Remove this entire if statement when it is believed safe to do so
					for (int i=0; i<100; i++) {//arbitrary number of 100 bodies that we might have in a session file
						String key = Main.SESSION_FILE_STR+i;
						String filename = Main.SESSION_FILE_NAME_STR+i;
						String savedLayerFileContents = userProps.getProperty(key);
						String savedLayerFilename = userProps.getProperty(filename);
						if (savedLayerFileContents == null) {
							break;
						}
						File f = new File(sessionKeyPath+savedLayerFilename);
						FileOutputStream fos = new FileOutputStream(f);
						fos.write(savedLayerFileContents.getBytes());
						fos.close();
					}
				}
			}
			TITLE_FILE = fname;
		}
		catch(IOException e)
		 {
			log.aprintln(e);
			log.aprintln("UNABLE TO LOAD JMARS FILE!");
			log.aprintln(fname);
			System.exit(-1);
		 }		
	 }
	
	
	private static int numOfArgs;
	private static boolean waitForBodySelect = false;
	private static void processArgs(String[] av) {
		numOfArgs = av.length;
		
		try {
			
			// Special case for Landing Site Build
			if (av.length==0) {
				String startupSession = Config.get("startupSession");
				if (startupSession != null) {
					av = new String[1];
					av[0] = startupSession;
				}
			}
			
			
			switch (av.length) {
			case 0:
				String lonDefault = Config.get(Util.getProductBodyPrefix()+"startup_lon","0");
				String latDefault = Config.get(Util.getProductBodyPrefix()+"startup_lat","0");
				startupInCyl(lonDefault, latDefault);
				break;
			case 1:
				startupFromFile(av[0]);
				if (!waitForBodySelect) {
					String lon, lat;
					if (savedLayers != null) {
						lon = lat = "0";
					} else {
						lon = userProps.getProperty("Projection_lon");
						lat = userProps.getProperty("Projection_lat");
					}
					startupInCyl(lon, lat);
				}
				break;
			case 2:
				startupInCyl(av[0], av[1]);
				break;
			case 3:
				startupFromFile(av[2]);
				startupInCyl(av[0], av[1]);
				break;
			default:
				showUsage();
			}
		} catch (Exception e) {
			log.aprintln("ERROR PROCESSING STARTUP ARGUMENTS:");
			log.aprintln(e);
			showUsage();
		}
	}
	
	private static void showVersion(boolean verbose)
	 {
		if(verbose)
		 {
			Map props = new TreeMap(System.getProperties());
			for(Iterator i=props.keySet().iterator(); i.hasNext(); )
			 {
				String key = (String) i.next();
				String pad = "                              ";
				pad = pad.substring(Math.min(key.length(), pad.length()));
				System.out.println(key + pad + " " +
								   log.DARK + props.get(key) + log.RESET);
			 }
			System.out.println("YOUR CACHE PATH IS: " + CacheManager.getCacheDir());
		 }
		else
			System.out.println(
				"\n * Invoke with -VERSION in caps for even more\n");

		System.out.println("================================================");
		System.out.println("Your JMARS was built:");
		System.out.println("\t" + ABOUT().DATE);
		System.out.println("");
		System.out.println("Your Java version is:");
		System.out.println("\t" + System.getProperty("java.runtime.name"));
		System.out.println("\t" + System.getProperty("java.vm.vendor") +
						   " / "+ System.getProperty("java.runtime.version"));
		System.out.println("");
		System.out.println("Your operating system version is:");
		System.out.println("\t" + System.getProperty("os.name") +
						   " / "+ System.getProperty("os.arch"));
		System.out.println("\t" + System.getProperty("os.version"));
		System.out.println("================================================");
		System.exit(-1);
	 }

	private static void showUsage()
	 {
		try
		 {
			BufferedReader fin = new BufferedReader(new InputStreamReader(
				getResourceAsStream("resources/usage")));
			String line;
			while((line = fin.readLine()) != null)
				System.out.println(line);
		 }
		catch(Exception e)
		 {
			log.aprintln("ERROR! UNABLE TO PRINT USAGE DUE TO:");
			log.aprintln(e);
		 }
		System.exit(-1);
	 }

	private static void processAllArgs(String[] av)
	 throws Throwable
	 {
		// Construct a modifiable list of the command-line arguments
		List<String> args = new LinkedList<String>(Arrays.asList(av));
		ListIterator<String> iter = args.listIterator();

		// Consume the flags from the command-line argument list
		while(iter.hasNext())
		 {
			String arg = (String) iter.next();

			// processFlagXyz calls modify the list and/or call System.exit()
			if     (arg.equals("-help")   ) showUsage();
			else if(arg.equals("-version")) showVersion(false);
			else if(arg.equals("-VERSION")) showVersion(true);
			else if(arg.startsWith(":")   ) processFlagMultiHead (arg, iter);
			else if(arg.equals("clean")   ) {
				cleanCache();
				iter.remove();
			} else if(arg.equals("slideshow"))processFlagSlideshow (arg, iter);
			else if(arg.equals("-open")) iter.remove();
		 }

		// Process the "real" startup parameters
		processArgs((String[]) args.toArray(new String[0]));
	 }

	public static void main(String av[])
	 throws Throwable
	 {
	    Toolkit.getDefaultToolkit().getSystemEventQueue().push( new PopupEventQueue());
		// create a MAC OS X event handler reference in case
		// we are on a MAC
		OSXresponder osxResponder = null;
		try
		 {
			// If we are running on a MAC then instantiate the OS X
			// event handler and register it with the OS
	        if (MAC_OS_X) {
	        	osxResponder = new OSXresponder(); 
	 		 	osxResponder.registerForMacOSXEvents();
	 		 	// take a brief nap just in case webstart is slow starting up
	 		 	Thread.sleep(1000L);
	 		 	if (osxResponder.getStartupFile() != null) {
		 		 	String[] updatedArgs = new String[av.length+1];
		 		 	System.arraycopy(av, 0, updatedArgs, 0, av.length);
		 		 	updatedArgs[updatedArgs.length-1] = osxResponder.getStartupFile();
		 		 	av = updatedArgs;
	 		 	}
	 		}
	        
			log.println("===> mac Arguments <===");
			for (String a: av) {
				log.println("mac arg " + a);				
			}
			Map<String,String> env = System.getenv();
			log.println("===> ENVIRONMENT <===");
			for (String key: env.keySet()) {
				log.println(key + "=" + env.get(key));
			}
			if (DebugLog.getOutputStream() != null) {
				log.println("===> PROPERTIES  <===");
				Properties props = System.getProperties();
				for (Object key: props.keySet()) {
					log.println(key + "=" + props.get(key));
				}
			}
			log.println("===> Arguments <===");
			for (String a: av) {
				log.println(a);
			}
			
			// install JMARS-specific certificate trust policy
			JmarsTrustManager.install();

			// Churn through the command-line arguments
			processAllArgs(av);

			// Load database drivers
			Util.loadSqlDrivers();
			
			if(userProps == null) // Might've been populated in the processArgs
				userProps = new UserProperties();

			// Configure the Mac Dock Icon (if we can find the necessary classes)
			try {
				Class appClass = Class.forName("com.apple.eawt.Application");
				Constructor appConstructor = appClass.getConstructor();
				Method setDockImageMethod = appClass.getMethod("setDockIconImage", Image.class);
				setDockImageMethod.invoke(appConstructor.newInstance(), Util.getJMarsIcon());
			} catch (Exception e) {
				//e.printStackTrace();
			}
			
			// Pop-up authentication dialog, set a timer to do so periodically
			initiateAuthentication();

			// Load any remote or chained config files
			Config.loadRemoteProps();

			// Upgrade nag screen
			if(ABOUT().DATE != null)
				checkAppVersion();

			final int numOfArgs = av.length;
			// FINALLY: Create the application frame!
			SwingUtilities.invokeLater(new Runnable(){
				public void run(){
					mainFrame = new Main();

					if (numOfArgs != 1){
						mainFrame.setExtendedState(Frame.MAXIMIZED_BOTH);
					}
					initiateSaving();
				}
			});
		 }
		catch(Exception e)
		 {
			log.aprintln(e);
			System.exit(-1);
		 }
	 }


	//Override to manage initial size and location
	public void setVisible( boolean b )
	 {
		if ( b )
			userProps.setWindowPosition(this);

		super.setVisible(b);
	 }
	
	class TimeCraftItem extends JRadioButtonMenuItem {
		public TimeCraftItem(boolean selected, final String craft, String desc, boolean isEnabled, ButtonGroup timeCraftsG) {
			super(new AbstractAction(craft + " - " + desc) {
				public void actionPerformed(ActionEvent e) {
					TimeField.setDefaultCraft(craft);
				}
			});
			setSelected(selected);
			timeCraftsG.add(this);
		}
	}

	/** These menu items are referenced elsewhere in the class */
	JMenu menuAction;
	JMenu menuFile;
	JMenu menuBody;// @since change bodies
	
	/** If selected, then the user requested an autosave restore. */
	private static final JCheckBox cbRestore = new JCheckBox("Restore autosave");
	
	/** If non-null, provides the contents of the user's recent and bookmarked places. */
	public static PlacesMenu places;
	private JMenuItem placesMenu;
	private XmlPlaceStore placeStore;
	
	/* Time-format tag to menu item map: e.g. "E" -> "ET" menu item */
	Map<String,TimeFormatItem> fmtTagToMenuItem = new HashMap<String,TimeFormatItem>();

	/* Spacecraft tag to menu item map: e.g. "ODY" -> "Odyssey menu item */
	Map<String,TimeCraftItem> craftTagToMenuItem = new HashMap<String,TimeCraftItem>();

    private static class TimeFormatItem extends JRadioButtonMenuItem {
		TimeFormatItem(boolean selected, final char format, String label, ButtonGroup timeFormatsG) {
			super(new AbstractAction(label) {
				public void actionPerformed(ActionEvent e) {
					TimeField.setDefaultFormat(format);
				}
			});
			setSelected(selected);
			timeFormatsG.add(this);
		}
	}
    
    /**
     * Updates the time format choice selection from the 
     * {@link TimeField#getDefaultFormat()}.
     */
    public void updateTimeFormatSelection(){
    	fmtTagToMenuItem.get(new Character(TimeField.getDefaultFormat()).toString()).setSelected(true);
    }
    
    //@since J-all. This method was created to be able to change the selected body on the menu when the body is changed
    //while loading a session. 
    private void updateSelectedBody() {
    	for(JRadioButtonMenuItem radio : bodyItems) {
    		if (currentBody.equalsIgnoreCase(radio.getText().trim())) {
    			radio.setSelected(true);
    		} else {
    			radio.setSelected(false);
    		}
		}
    	
    }
    
    private boolean showSelectBody() {
    	//get the Map of bodies
    	mapOfBodies = Util.getBodyList();
    	//only add this menu if the current product has multiple bodies
    	//if (mapOfBodies.size() > 1) {
    	
    	//take a look at the arrays that are stored in the Map and determine if we should show the select body menu entry
    	Collection<String[]> values = mapOfBodies.values();//get the String arrays out
    	boolean showSelectBody = false;
    	if (values.size() > 1) {//if we have more than one array, we should show select body 
    		showSelectBody = true;
    	} else {//if we only have one in the list, it might have more than one entry
        	for (String[] bList : values) { //should only be one in the Collection
				if (bList.length > 1) {//if it is larger than 1 show select body
					showSelectBody = true;
					break;
				}
			}
    	}
    	return showSelectBody;
    }
    
    // @since change bodies
    protected void initBodyMenu(JMenuBar mainMenu) {
		//add the Body entry to the menu
    	menuBody = new JMenu("Body");
    	menuBody.setMnemonic(KeyEvent.VK_B);
    	menuBody.setEnabled(false);//disable until map has loaded
    	MapServerFactory.whenMapServersReady(new Runnable() {
			public void run() {
				//enable the body menu only after the map has finished loading. This prevents a body change while the layer is not in a valid 
            	//state, allowing an invalid layer to be written out
				Main.this.menuBody.setEnabled(true);
			}
		});
    	mainMenu.add(menuBody);
		boolean showSelectBody = this.showSelectBody();
    	if (showSelectBody) {
    		//create a "Select Body" sub menu with the body options under it
	    	JMenu mainSelect = new JMenu("Select Body");
	    	menuBody.add(mainSelect);
    		//note for the end of the day, figure out how to iterate through the map and determine if we should 
	    	// show the select body or not. it should only show if any of the String[] in the map are > 1
    		
	    	//@since J-all. bodyItems was moved to an instance variable on Main
	    	ActionListener bodyListener = new ActionListener() {
	    		public void actionPerformed(ActionEvent e) {
	    			JMenuItem source = (JMenuItem)e.getSource();
	    			String name = source.getName();
	    			if (!name.equalsIgnoreCase(currentBody)) {
	    				for(JRadioButtonMenuItem radio : bodyItems) {
	    					radio.setSelected(false);
	    				}
		    	    	//disable the body menu to prevent any further body selections until the load is complete
		    	    	menuBody.setEnabled(false);
		    	    	startSelectBody();
						setCurrentBody(name);
						setSelectedBody(name);
						doSelectBody();
	    			}
	    			source.setSelected(true);//do this all the time
				}
	    	};
	    	
	    	Iterator<String> iter = mapOfBodies.keySet().iterator();
    		String level = iter.next();    		
    		if ("".equals(level)) {
    			//we are not going to have a 2nd level
    			String[] bList = mapOfBodies.get(level);
    			//loop through the body values from config
				for (String bodyName : bList) {
					String displayName = Config.get(bodyName.toLowerCase()+".body_menu_display","");
					if (displayName.equals("")) {
						displayName = bodyName;
					}
					JRadioButtonMenuItem bodyRadioButton = new JRadioButtonMenuItem(bodyName);
					bodyRadioButton.setName(bodyName);
					bodyRadioButton.setText(displayName);
					bodyRadioButton.addActionListener(bodyListener);
					mainSelect.add(bodyRadioButton);
					bodyItems.add(bodyRadioButton);
					
					//disable the menu entry for the initially selected body
					if (currentBody.equalsIgnoreCase(bodyName)) {
						bodyRadioButton.setSelected(true);
					}
				}
    		} else {
    			//we will have a 2nd level
    			//store the "Systems" in a temporary place and add them at the end so that it looks better in the menu
    			ArrayList<JMenu> systemEntries = new ArrayList<JMenu>();
    			iter = mapOfBodies.keySet().iterator();
    			while (iter.hasNext()) {
    				level = iter.next();
    				String levelDisplay = Util.properCase(level);
    				
    				//here we are going to do some logic to figure out if we should put the word "System" after the entry.
    				//System should show up only for entries that have sub menus. For example...jupiter ["Io","Europa","Ganymede","Callisto"].
    				//The above example would say "Jupiter System" with a sub select for its moons. 
    				//However, mars["Mars"] should not. This is just to keep the data structure consistent and should just say "Mars". 
    				//The question becomes, when Jupiter ends up being a body that can be selected, do we change it to say Jupiter with sub menus
    				//that have the moons under it, or do we allow them to click "Jupiter System"? This is a question for later. Right now
    				//all bodies that are selectable have no other bodies under them. 
    				
    				String[] tempBodyArr = mapOfBodies.get(level);
    				boolean singleLevelFlag = false;
    				if (tempBodyArr.length == 1) {
    					//the only condition we care about is when there is one entry. zero is an error, and more than one is a sub menu
    					if (tempBodyArr[0].equalsIgnoreCase(level)) {
    						//this is the case where the structure looks like this mars=[Mars]. We will set a flag for later
    						singleLevelFlag = true;
    					}
    				}
    				String tempLevelName = levelDisplay;
    				if (!singleLevelFlag) {
    				    tempLevelName = tempLevelName.replaceAll("_system", " System");
    				    tempLevelName = tempLevelName.replaceAll("_System", " System");
    					JMenu subSelect = new JMenu(tempLevelName);
    					if (tempLevelName.trim().equalsIgnoreCase("Vesta")) {  	
    						JMenuItem pdfButton = new JMenuItem("Vesta Coordinate Systems");
    						pdfButton.setForeground(Color.BLUE);   						
    						subSelect.add(pdfButton);
							pdfButton.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent e) {
									try {								
										String urlString = "http://jmars.mars.asu.edu/vesta/4_Vesta.html#Coordinate_controversy";
										BrowserLauncher browserLauncher = new BrowserLauncher();
										browserLauncher.setNewWindowPolicy(false);
										browserLauncher.openURLinBrowser(urlString);
										
									} catch (Exception e1) {
										log.aprintln(e1);
									}											
								}
							});    					
    					}
            			String[] bList = mapOfBodies.get(level);
            			for (String bodyName : bList) {
            				String displayBodyName = translateBodyNameFromConfig(bodyName);
            				JRadioButtonMenuItem bodyRadioButton = new JRadioButtonMenuItem(displayBodyName);
            				bodyRadioButton.setName(bodyName);
        					bodyRadioButton.addActionListener(bodyListener);
        					subSelect.add(bodyRadioButton);
        					bodyItems.add(bodyRadioButton);
        					systemEntries.add(subSelect);//store to be added at the bottom);
        					if (tempLevelName.trim().equalsIgnoreCase("Vesta")) {
        						if (bodyName.trim().equalsIgnoreCase("Vesta_Dawn_Claudia")){	
        							ToolTipManager.sharedInstance().setInitialDelay(0);       							
        							bodyRadioButton.setToolTipText("<html>This coordinate system is used by the<br>Dawn Science Team in publications.</html>");  
        						}      					
        						if (bodyName.trim().equalsIgnoreCase("Vesta_IAU")) {
        							ToolTipManager.sharedInstance().setInitialDelay(0);
        							bodyRadioButton.setToolTipText("<html>Dawn/Vesta data products in the Planetary<br>Data System are in this coordinate system.</html>");
        						}
        					}	
        					
        					//disable the menu entry for the initially selected body
        					if (currentBody.equalsIgnoreCase(bodyName)) {
        						bodyRadioButton.setSelected(true);
        					}
        				}
    				} else {
    					if (Config.get(level + "." + Config.CONFIG_BODY_NAME, "") != "") {
    						//main level is selectable
    						JRadioButtonMenuItem bodyRadioButton = new JRadioButtonMenuItem(tempLevelName);
        					bodyRadioButton.setName(level);
        					bodyRadioButton.addActionListener(bodyListener);
        					mainSelect.add(bodyRadioButton);
        					bodyItems.add(bodyRadioButton);
        					
        					//disable the menu entry for the initially selected body
        					if (currentBody.equalsIgnoreCase(level)) {
        						bodyRadioButton.setSelected(true);
        					}
    					} 
    				}
    			}
    			JMenu[] subMenuArr = systemEntries.toArray(new JMenu[]{});
    			for(int i=0; i<subMenuArr.length; i++) {
    				mainSelect.add(subMenuArr[i]);
    			}
    		}
    	}
    	JMenuItem resetLayers = new JMenuItem("Reset Layers");
    	menuBody.add(resetLayers);
    	resetLayers.addActionListener(new ActionListener() {
    		public void actionPerformed(ActionEvent e) {
    			if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
    					Main.this, "Are you sure you want to reset the layers?", "Confirm Reset Layers",
    					JOptionPane.YES_NO_OPTION)) {
    				Main.this.resetLayersForCurrentBody();
    			}
    		}
    	});
    	JMenuItem reloadLayers = new JMenuItem("Reload Saved Layers");
    	menuBody.add(reloadLayers);
    	reloadLayers.addActionListener(new ActionListener() {
    		public void actionPerformed(ActionEvent e) {
    			Main.this.reloadLayersForCurrentBody();
    		}
    	});
    }
    private String translateBodyNameFromConfig(String bName) {
    	//When we display the a body name in the menu, we will replace the _ with a space.
    	//Only Vesta with its multi-word names has this issue right now.
    	bName = bName.replace('_', ' ');
    	return bName;
    }
    private synchronized void startSelectBody() {
    	JOptionPane pane = new JOptionPane("Please wait while the planetary body loads...", JOptionPane.NO_OPTION);
    	pane.setOptions(new Object[]{});
    	progressDialog = pane.createDialog(this,"Loading");
    	progressDialog.setLocationRelativeTo(Main.mainFrame);
    	progressDialog.setAlwaysOnTop(false);
    	progressDialog.setModal(false);
    	progressDialog.setVisible(true);
    	saveCurrentLayers();
    }
    // @since change bodies
    //this method gets executed when a body is selected
    private synchronized void doSelectBody() {
    	boolean cancel = this.removeAllCurrentViews(true, Main.MODE_CHANGE_BODY);
    	if (!cancel) {
			StampSource.clearSources();
			NumericStampSource.clearSources();
    		//Set tool mode back to default when switching bodies
			ToolManager.setToolMode(ToolManager.SEL_HAND);
			//Set flag to false, if layer exists it will be set to true in the recreateLview method
			InvestigateFactory.setLviewExists(false);
			
			changeConfigValues();
			userProps.reset();
			LViewFactory.refreshAllLViews();
			refreshMainView();
			//TODO: this is not right...should properly refresh/rebuild
			// the threeD view (if it's open) after everything has loaded
			// from a body switch (right?)
			if(threeDWindow != null){
				threeDWindow.setVisible(false);
				threeDWindow = null;
			}
			threeDMenuItem.setSelected(false);
			
			try {
				autosave();
			} catch (IOException e) {
				JOptionPane.showMessageDialog(
						LManager.getDisplayFrame(),
						"Error autosaving: " + e,
						"Error autosaving",
						JOptionPane.ERROR_MESSAGE);
				log.println("Main.saveCurrentLayers - an IOException occurred while trying to autosave after selecting body.");
				log.println(e);
			}
			
//			if (getBody().equalsIgnoreCase("earth")) {	
//				// Only add the current location option to the Earth body
//				if (placesMenu!=null){
//					placesMenu.add(myIP, 0);
//				}
//			}
    	}
    }

	// a button which returns your view to where the user is currently located on Earth
//    private AbstractAction goToMyIP = new AbstractAction("My Current Location"){
//		public void actionPerformed(ActionEvent e){
//			String[] lonlat = getHomeLocation();
//			String location = lonlat[0]+","+lonlat[1];
//			testDriver.locMgr.setLocation(testDriver.locMgr.textToWorld(location), true);
//		}
//	};	
//	private JMenuItem myIP = new JMenuItem(goToMyIP);
	
    // @since change bodies
    //this method will remove the layers for the current view, 
    private void resetLayersForCurrentBody() {
    	boolean cancel = this.removeAllCurrentViews(false, Main.MODE_RESET_LAYERS);
    	if (!cancel) {
    		userProps.reset();
	    	//userProps.setPropertyInt("ViewCount", 0);
	    	LViewFactory.refreshAllLViews();
	    	this.deleteLayersFile();//delete the layers.xml file that is stored for this body
	    	this.loadLayers(null);
    	}
    }
    private static String getLayerFilename() {
    	return currentBody+LAYERS_FILE_EXT;
    }
    private void deleteLayersFile() {
    	//When the user resets the layers for a body, we should delete the layers.xml file in order to reset
    	//the layers to the default behavior.
		String path = Main.getJMarsPath()+bodyBaseDir;
		//delete the file
		File saveFile = new File(path+Main.getLayerFilename());
		if (saveFile.exists()) {
			saveFile.delete();
		}
    }
    //@since change bodies
    private void reloadLayersForCurrentBody() {
    	JOptionPane pane = new JOptionPane("Please wait while the planetary body loads...", JOptionPane.NO_OPTION);
    	pane.setOptions(new Object[]{});
    	progressDialog = pane.createDialog(this,"Loading");
    	progressDialog.setLocationRelativeTo(Main.mainFrame);
    	progressDialog.setAlwaysOnTop(true);
    	progressDialog.setModal(false);
    	progressDialog.setVisible(true);
    	boolean cancel = this.removeAllCurrentViews(false, Main.MODE_RESET_LAYERS);
    	if (!cancel) {
    		userProps.reset();
	    	
    		//we do not need to reset factories or connect to other map servers, there is no body change here.
	    	InputStream layersStream = this.getLayersStream();
			loadLayers(layersStream);
			try {
				if (layersStream != null) {
					layersStream.close();
				}
			} catch (IOException e) {
				log.println(e);
			}
			LManager.getLManager().repaint();
				
			this.disposeProgressDialog();
    	}
    }
    //@since change bodies
    //updated for new process
    private InputStream loadExistingLayers() {
    	InputStream layersStream = this.getLayersStream();
    	return layersStream;//this can be null
    }
    // @since change bodies
    protected void saveCurrentLayers() {
    	//save the current layers
    	try {
     		String path = Main.getJMarsPath()+Main.getBodyBaseDir();
    		File directory = new File(path);
    		//create the bodies and product directory if necessary
    		if (!directory.exists()) {
    			directory.mkdirs();
    		}
    		directory = null;
    		//save the file
    		File saveFile = new File(path+Main.getLayerFilename());
    		if (!saveFile.exists() || saveFile.canWrite()) {
    			saveLayers(saveFile);
    		} else {
    			JOptionPane.showMessageDialog(
    					LManager.getDisplayFrame(),
    					"Error saving layers: Can not write to path: "+path+
    					JOptionPane.ERROR_MESSAGE);
    			log.println("Main.saveCurrentLayers - "+path+Main.getLayerFilename()+ "existed and was not writable. The current layers could not be saved. ");
    		}
		} catch (IOException e) {
			JOptionPane.showMessageDialog(
					LManager.getDisplayFrame(),
					"Error saving layers: " + e,
					"Error saving layers",
					JOptionPane.ERROR_MESSAGE);
			log.println("Main.saveCurrentLayers - an IOException occurred while trying to create the body directories or save the current layers.");
			log.println(e);
		}
    }
    private static final int MODE_CHANGE_BODY = 1;
    private static final int MODE_RESET_LAYERS = 2;
    private static final int MODE_LOAD_SESSION = 3;
    // @since change bodies
    private boolean removeAllCurrentViews(boolean checkUnsavedInformation, int mode) {
    	//get the list of views from the LManager
    	List<LView> viewList = LManager.getLManager().getViewList();
    	//loop through the list of views and remove each one
    	LView[] lViewArr = viewList.toArray(new LView[]{});
    	boolean cancel = false;
    	//if we are resetting layers, we will not check for unsaved information
    	if (checkUnsavedInformation) {
	    	//loop through the list of layers twice. First, loop through and verify that if there is unsaved data, that the user
	    	//wants to delete the layer.
	    	for (int x=0;x<lViewArr.length;x++) {
	    		LView tempLView = lViewArr[x];
	    		switch (mode) {
		    		case (MODE_CHANGE_BODY) : {
		    			tempLView.changeBody(); //display warning for changing body
		    			break;
		    		}
		    		case (MODE_RESET_LAYERS) : {
		    			//do nothing
		    			break;
		    		}
		    		case (MODE_LOAD_SESSION) : {
		    			cancel = tempLView.loadSession();//display warning for loading session. If true, they said "No" and want to cancel
		    			break;
		    		}
		    		default : {
		    			break;
		    		}
	    		}
	    		
	    	}
    	}
    	if (!checkUnsavedInformation || mode != MODE_LOAD_SESSION || !cancel) {//this is the only condition we currently want to not continue
	    	//do the delete without checking for unsaved data. That check was done above
	    	for (int x=0;x<lViewArr.length;x++) {
	    		LView tempLView = lViewArr[x];
	    		if (tempLView instanceof NomenclatureLView) {
	    			((NomenclatureLView) tempLView).clearSettings();
	    		}
	 			LManager.getLManager().deleteLayerNoCheckForUnsavedData(tempLView);
	    	}
    	}
    	return cancel;//false unless we canceled
    }
    private void setSelectedBody(String body) {
    	selectedBody = body;
    }
    // @since change bodies
    private InputStream getLayersStream() {
    	//load the layers for the selected body
    	//check to see if a layers.xml exists, otherwise, load from config
    	
    	if (selectedBody == null) {
    		setSelectedBody(currentBody);
    	}
    	String path = Main.getJMarsPath()+bodyBaseDir;
    	File file = new File(path);
    	String layersFilePath = null;
    	InputStream layersStream = null;
    	if (file.exists()) {
    		//we have a directory, look for the body xml file
    		layersFilePath = path+Main.getLayerFilename();
    		try {
				layersStream = new FileInputStream(new File(layersFilePath));
			} catch (FileNotFoundException e) {
				//do nothing, return null
			}
    	}
    	return layersStream;
    }
    // @since change bodies
    protected void refreshMainView() {
    	this.refreshMainView(true);
    }
    //@since J-all. This was separated out into two methods in order to be able to call it without loading the layers. We 
    //need to be able to do this when loading sessions, otherwise, when loading a session, we get each layer loaded twice.
    protected void refreshMainView(final boolean loadLayersFlag) {
    	InputStream layersStream = this.getLayersStream();
    	
    	//create a new thread group in order to keep track of other spawned threads and make sure they are all finished before allowing the layers to be materialized.
    	final ThreadGroup serverRefreshThreadGroup = new ThreadGroup("changeBody");
    	//make it a daemon thread so that it destroys itself when all threads are complete
    	serverRefreshThreadGroup.setDaemon(true);
    	Thread refreshMapServersThread = new Thread(serverRefreshThreadGroup, new Runnable() {
    		public void run() {
    			//kick off the map refresh logic
    			refreshMapServers();
    		}
    	});
    	//start the thread 
    	refreshMapServersThread.start();
    	//create a final copy of the layers file in order to have it available for use in a separate thread
    	final InputStream copyOfLayers = layersStream;//this can be null
    	//create a new thread to wait for the server refresh thread group to finish, materialize the layers, and re-enable the body menu
    	Thread layerThread = new Thread(new Runnable() {
    		public void run() {
    			//check to see if the server refresh has finished
    			while(!serverRefreshThreadGroup.isDestroyed()) {
    				try {
    					//sleep for 200 ms off the AWT thread
						Thread.sleep(200);
					} catch (InterruptedException e) {
						//do nothing here
					}
    			}
				//the server refresh is done, we can now continue. Kick this off now on the AWT thread 
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						if (loadLayersFlag) {
							loadLayers(copyOfLayers);
						} else {
							loadLayers(null);
							try {
								//the stream does not get closed in this else so close it
								if (copyOfLayers != null) {
									copyOfLayers.close();
								}
							} catch (IOException io) {
								io.printStackTrace();
								log.println(io);
							}
						}
						LManager.getLManager().repaint();
					}
				});
				Main.this.disposeProgressDialog();
    		}
    	});
    	//start the layer that will materialize the layers when the server refresh is done
    	layerThread.start();
    }
    
    private void disposeProgressDialog() {
    	//enable the body menu.
    	if (Main.this.menuBody != null) {
    		Main.this.menuBody.setEnabled(true);
    	}
		//remove the progress dialog
		if (this.progressDialog != null) {
			this.progressDialog.setVisible(false);
			this.progressDialog.dispose();
		}
    }
    
    // @since change bodies
    //this method was extracted to be able to be called when we were in the thread to load the layers, and also when we want to reset the layers. 
    //the parameter is final in order to be used while in the thread that is created while changing bodies
	protected void loadLayers(final InputStream copyOfLayers) {
		//load the layers file which will either be saved from a previous session or the default 
		try {
			if (copyOfLayers != null) {
				List<SavedLayer> layers = SavedLayer.load(copyOfLayers);
				copyOfLayers.close();
				Main.savedLayers = layers;
			} else {
				Main.savedLayers = null;
			}
			testDriver.buildViews();
			SwingUtilities.invokeLater(new Runnable() {
				
				public void run() {
					LManager.getLManager().setInitialState();
				}
			});
			
		} catch (Exception e) {
			JOptionPane.showMessageDialog(
					LManager.getDisplayFrame(),
					"Error loading layers: " + e,
					"Error loading layers",
					JOptionPane.ERROR_MESSAGE);
			log.println(e);
		}
	}
	// @since change bodies
    protected void refreshMapServers() {
    	//get the list of map servers
    	List<MapServer> mapsList = MapServerFactory.getMapServers();
    	if (mapsList != null) {
	    	//convert to an array so there are no conflicts with manipulating the List while iterating
	    	MapServer[] mapServerArr = (MapServer[]) mapsList.toArray(new MapServer[]{});
			places.resetHistory();
	    	//remove each of the map servers
	    	for (int x=0; x<mapServerArr.length; x++) {
	    		MapServerFactory.removeMapServer(mapServerArr[x]);
	    	}
	    	
	    	//start the load map server process again now that the configuration values have been updated
	    	//we need to make sure that the thread finishes before getting the maps so we use the method that accepts a Runnable
	    	MapServerFactory.whenMapServersReady(new Runnable() {
	    		public void run() {
	    			//Null out the mapservers and loaderThread in order for kickloader to execute 
	    			MapServerFactory.disposeOfMapServerReferences();
	    			//call the getMapServers method which we can be sure will only get called once the maps servers have loaded
	    			MapServerFactory.getMapServers();
	    			StampServer.initializeStampSources();
	    		}
	    	});
	    	HVector.refreshEllipsoidRadii();//must reset the x, y and z values in the HVector
	    	LViewFactory.refreshAllLViews();
	    	
    		//execute this on the AWT thread 
	    	try {
				SwingUtilities.invokeAndWait(new Runnable () {
					public void run() {
						LayerParameters.resetLayerParameters();
						AddLayer.setInitialCategory();
						LManager.getLManager().refreshAddMenu();
					}
				});
			} catch (InterruptedException e) {
				//do nothing
			} catch (InvocationTargetException e) {
				JOptionPane.showMessageDialog(
						LManager.getLManager(),
						"Error loading layers: " + e.getTargetException().getMessage(),
						"Error loading layers",
						JOptionPane.ERROR_MESSAGE);
				log.println(e.getTargetException().getMessage());
			}
	    	
    	}
    }
    // @since change bodies
    protected void changeConfigValues() {
    	//set the values based on the selected body
    	Config.set(Config.CONFIG_SELECTED_BODY, selectedBody);
    	//update the values for polar radius, etc. in order to make sure that the Util constants have the correct and latest values
    	Util.updateRadii();//need to make this call before using Util again as it resets the prefix. Maybe I should break this out into two calls? But both need to be done together
    	TITLE = Config.get(Util.getProductBodyPrefix()+Config.CONFIG_EDITION);
    	mainFrame.setTitle(checkDemoOrBeta() + TITLE);
    	mainFrame.setName(TITLE);
    	placeStore.reloadPlaces();
    }
    
    public static JCheckBoxMenuItem threeDMenuItem;
    protected void initMenuBar() {
		JMenuBar mainMenuBar = new JMenuBar();
		menuFile = new JMenu("File");
		menuFile.setMnemonic(KeyEvent.VK_F);
		mainMenuBar.add(menuFile);

		initFileMenu();

		JMenu menuView = new JMenu("View");
		menuView.setMnemonic(KeyEvent.VK_V);
		mainMenuBar.add(menuView);
		
		AbstractAction dockLManager = new AbstractAction("Dock Layer Manager"){
			public void actionPerformed(ActionEvent e){
				if(!LManager.getLManager().isDocked()){
					LManager.getLManager().dock();
				}
			}
		};
		JMenuItem dockMgr = new JMenuItem(dockLManager);
		menuView.add(dockMgr);
		
		//TODO: should probably not be enabled when the lmanager is docked and
		// it's tab is selected...needs to be triggered (enabled/disabled) from 
		// lmanager when tabs are selected.  This is good enough for now I think.
		JMenuItem shwMgr = new JMenuItem(new AbstractAction("Show Layer Manager"){
			public void actionPerformed(ActionEvent e) {
				if(LManager.getDisplayFrame()!=null){
					LManager.getDisplayFrame().setVisible(true);
					LManager.getDisplayFrame().toFront();
					LManager.getDisplayFrame().repaint();
				}
				LManager.getLManager().activateTab(0);
			}
		});
		shwMgr.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H,KeyEvent.CTRL_DOWN_MASK));
		menuView.add(shwMgr);
		
		menuView.addSeparator();
		

		
		//Show 3D view
		threeDMenuItem = create3DMenu();
		menuView.add(threeDMenuItem);
		
		
		menuView.add(createPannerControlMenu());
		
		final JCheckBoxMenuItem viewMeters = new JCheckBoxMenuItem("Memory Meter");
		viewMeters.setMnemonic('M');
		viewMeters.setSelected(Config.get("main.meters.enable", false));
		viewMeters.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Main.testDriver.setMetersVisible(viewMeters.isSelected());
			}
		});
		menuView.add(viewMeters);
		
		menuView.addSeparator();
		
		// Keyboard navigation shortcuts
		menuView.add(testDriver.mainWindow.getNavMenu());
		
		JMenuItem recenterItem = new JMenuItem(new AbstractAction("Recenter projection") {
			{
				setEnabled("true".equals(Config.get("reproj")));
			}

			public void actionPerformed(ActionEvent e) {
				testDriver.locMgr.reprojectFromText();
				RulerManager.Instance.notifyRulerOfViewChange();
				OpenStreetMapTiles.reprojectMessage();
			
			}
		});
		recenterItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK));
		recenterItem.setMnemonic('R');
		menuView.add(recenterItem);
		
		JMenu menuTimes = new JMenu("Default time format");

		// Single time format menu selection
		ButtonGroup timeFormatsG = new ButtonGroup();

		Map<String,String> fmtKeyMap = TimeCacheFactory.instance().getTimeFmtTagToNameMap();
		String format = Config.get("default.time.format", TimeCacheFactory.FMT_TAG_ET);
		if (format.length() != 1 || !fmtKeyMap.keySet().contains(format))
			format = TimeCacheFactory.FMT_TAG_ET;
		for(String fmtKey: fmtKeyMap.keySet()){
			TimeFormatItem item = new TimeFormatItem(format.equals(fmtKey), fmtKey.charAt(0), fmtKeyMap.get(fmtKey), timeFormatsG); 
			fmtTagToMenuItem.put(fmtKey, item);
			menuTimes.add(item);
		}
		TimeField.setDefaultFormat(format.charAt(0));
		updateTimeFormatSelection(); // select default choice
		
		TimeField.addTimeFormatListener(new TimeFormatChangeListener(){
			public void timeFormatChanged(TimeFormatEvent e) {
				updateTimeFormatSelection();
			}
		});
		
		menuTimes.add(new JSeparator());
		
		try {
			// Single time craft menu selection
			ButtonGroup timeCraftsG = new ButtonGroup();
			
			// set up a separated list of crafts to choose from; there are many
			// things that can go wrong here, so we just catch any kind of
			// exception and ommit adding the menu unless it all goes well
			Map<String,String> craftNameMap = TimeCacheFactory.instance().getCraftNameMap();
			String defaultCraftTag = Config.get("time.db.default_craft", new ArrayList<String>(craftNameMap.keySet()).get(0));
			for(String craftTag: craftNameMap.keySet()){
				boolean isMRO = TimeCacheFactory.SC_TAG_MRO.equals(craftTag);
				TimeCraftItem item = new TimeCraftItem(defaultCraftTag.equals(craftTag), craftTag, craftNameMap.get(craftTag), !isMRO, timeCraftsG);
				craftTagToMenuItem.put(craftTag, item);
				menuTimes.add(item);
			}
			
			// TODO: Enable/disable time format menu options based on TimeCache.getSupportedFormats()
			TimeField.setDefaultCraft(defaultCraftTag);
		} catch (Exception e) {
			 log.println(e);
		}

		menuView.add(menuTimes);

    
		if (Config.get(Config.CONFIG_SHOW_BOUNDING_BOX, false)) {
			/*
			 * The following is for a request made by Jon Hill to make mosaic changes. He is always zoomed in to at least 512ppd. 
			 * The code below lets Jon grab the lat/long points of the view window so he can put them into his script and fix any
			 * errors in his mosaic. This doesn't work well when the view window is zoomed out to 4 ppd or less because it doesn't
			 * take white space into consideration. 				
			 */
			JMenuItem boundingBoxCoordinates = new JMenuItem(new AbstractAction("Bounding Box") {
							
				public void actionPerformed(ActionEvent e){
					Clipboard system = Toolkit.getDefaultToolkit().getSystemClipboard();
					
					Rectangle2D spatial = testDriver.mainWindow.getProj().getWorldWindow();
					Point2D wSSpatial = testDriver.mainWindow.getProj().world.toSpatial(spatial.getMinX(), spatial.getMinY()); 
					Point2D eNSpatial = testDriver.mainWindow.getProj().world.toSpatial(spatial.getMaxX(), spatial.getMaxY());
					Double minX = wSSpatial.getX();
					Double minY = wSSpatial.getY();
					Double maxX = eNSpatial.getX();
					Double maxY = eNSpatial.getY();
					String minXString = minX.toString();
					String minYString = minY.toString();
					String maxXString = maxX.toString();
					String maxYString = maxY.toString();
				
					StringSelection selection = new StringSelection(maxYString+" "+minYString+" "+maxXString+" "+minXString);
					
					system.setContents(selection, selection); 
				
	
				};
	
								
			});
			boundingBoxCoordinates.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK));	
			boundingBoxCoordinates.setMnemonic(KeyEvent.VK_W);
			menuView.add(boundingBoxCoordinates);
		}

		try {
			placeStore = new XmlPlaceStore();
			places = new PlacesMenu(placeStore);
			if (cbRestore.isSelected()) {
				places.restoreLocation();
			}
			placesMenu = places.getMenu();
			placesMenu.setMnemonic('P');
			mainMenuBar.add(placesMenu);
		} catch (Exception e) {
			// in case xml store is corrupt or something else goes wrong
			log.aprintln(e);
		}
		if (Util.showBodyMenu()) {
			initBodyMenu(mainMenuBar);// @since change bodies
		}
		
//		if (placesMenu!=null && getBody().equalsIgnoreCase("earth")){
//			placesMenu.add(myIP, 0);
//		}
		

/****  Tools dropdown menu           *********************************************/		
		
		JMenu menuTools = new JMenu("Tools");
		menuTools.setMnemonic(KeyEvent.VK_T);
		mainMenuBar.add(menuTools);
		final class ToolMenuItem extends JCheckBoxMenuItem implements ToolListener
		{
			int myMode;
			
			ToolMenuItem (int tmode, String name)
			{
				super(name);
				myMode = tmode;
				
				addActionListener(new ActionListener(){
					public void actionPerformed(ActionEvent e){
						ToolManager.setToolMode(myMode);
					}
				});
				
				ToolManager.addToolListener(this);
			}
			
			public void toolChanged (int newMode)
			{
				if (newMode == myMode)
					this.setSelected(true);
				else
					this.setSelected(false);
			}
		}
		
		// creates all the tool menu choices and assigns their shortcuts
		ToolMenuItem zoomIn, zoomOut, measure, panHand, selHand, superShape, investigate, export, resize; 
		zoomIn = new ToolMenuItem(ToolManager.ZOOM_IN, "Zoom In");
		zoomIn.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, KeyEvent.SHIFT_DOWN_MASK));
		zoomOut = new ToolMenuItem(ToolManager.ZOOM_OUT, "Zoom Out");
		zoomOut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.SHIFT_DOWN_MASK));
		measure = new ToolMenuItem(ToolManager.MEASURE, "Measure");
		measure.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, KeyEvent.SHIFT_DOWN_MASK));
		panHand = new ToolMenuItem(ToolManager.PAN_HAND, "Pan");
		panHand.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.SHIFT_DOWN_MASK));
		selHand = new ToolMenuItem(ToolManager.SEL_HAND, "Selection");
		selHand.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.SHIFT_DOWN_MASK));
		superShape = new ToolMenuItem(ToolManager.SUPER_SHAPE, "Super Shape");
		superShape.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.SHIFT_DOWN_MASK));
		investigate = new ToolMenuItem(ToolManager.INVESTIGATE, "Investigate");
		investigate.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.SHIFT_DOWN_MASK));
		export = new ToolMenuItem(ToolManager.EXPORT, "Export");
		export.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.SHIFT_DOWN_MASK));
        resize = new ToolMenuItem(ToolManager.RESIZE, "Resize View");
        resize.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.SHIFT_DOWN_MASK));
		
//	TODO re-enable when we get the code done	
		superShape.setEnabled(false);
		
		menuTools.add(selHand);
		menuTools.add(panHand);
		menuTools.add(zoomIn);
		menuTools.add(zoomOut);
		menuTools.add(measure);
//		menuTools.add(superShape);
		menuTools.add(investigate);
		menuTools.add(export);
        menuTools.add(resize);

		ToolManager.setToolMode(ToolManager.SEL_HAND);
/***************************************************************************/		
		
/***************** Options Menu (moved some items from Help menu) **********/
		JMenu menuOpts = new JMenu("Options");
		menuOpts.setMnemonic(KeyEvent.VK_O);
		mainMenuBar.add(menuOpts);
		
		// Check for updates menu item
		JMenuItem updateHelp = new JMenuItem(new AbstractAction("Check for Updates") {
			public void actionPerformed(ActionEvent e) {
				// This will return immediately if you call it from the EDT,
				// otherwise it will block until the installer application exits
				SwingUtilities.invokeLater(new Runnable() {
				    public void run() {
				    	try { 	
				    		// use reflection to make the callback from install4j runtime to JMARS to save any sessions prior to jmars app shutdown during an update 
							Object proxy = Proxy.newProxyInstance(Class.forName("com.install4j.api.launcher.ApplicationLauncher")
													.getClassLoader(),new Class[] { Class.forName("com.install4j.api.launcher.ApplicationLauncher$Callback") }, new InvocationHandler() {
								public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
									String methodName = method.getName();
									methodName = methodName.trim();
									if (methodName.equalsIgnoreCase("prepareShutdown")) {
										// prompt the user to save an session info if desired
										int n = JOptionPane.showConfirmDialog(null,"Would you like for your session to be saved before JMARS shuts down and installs the updates?", "JMARS", JOptionPane.YES_NO_OPTION);
										if (n == 0) {
											JFileChooser fc = getRcFileChooser();
											if (Util.showSaveWithConfirm(fc, Main.this, EXT)) {
												File f = fc.getSelectedFile();
												saveState(f.toString());
												setTitle(f.toString(), null);
											}
										}
									} else if (methodName.equalsIgnoreCase("exited")) {
									}
									return null;
								}
							});
							Class appLauncherClass = Class.forName("com.install4j.api.launcher.ApplicationLauncher");
							Method appLauncherMethod = appLauncherClass.getDeclaredMethod("launchApplicationInProcess", new Class[] {
								String.class, 
								String[].class,
								Class.forName("com.install4j.api.launcher.ApplicationLauncher$Callback"),
								Class.forName("com.install4j.api.launcher.ApplicationLauncher$WindowMode"),
								Window.class }
							);
							Class windowModeClass = Class.forName("com.install4j.api.launcher.ApplicationLauncher$WindowMode");
							Object windowModeObj = windowModeClass.getDeclaredField("FRAME").get(null);
							appLauncherMethod.invoke(null, new Object[] {"784", null, proxy, windowModeObj, Main.mainFrame });
						} catch (Exception e) {
							// if the user is running a stand alone jar file, let them know updating is not an option
							JOptionPane.showMessageDialog(null, "The Update feature is not currently available in this version.", "JMARS", JOptionPane.ERROR_MESSAGE);
						}
					}
				});
			}
		});
		

		updateHelp.setMnemonic(KeyEvent.VK_U);
		menuOpts.add(updateHelp);
		
		// Cache manager menu item
		JMenuItem cacheHelp = new JMenuItem(new AbstractAction("Manage Cache") {
			public void actionPerformed(ActionEvent e) {
				new CacheDialog(Main.this).getDialog().setVisible(true);
			}
		});
		cacheHelp.setMnemonic(KeyEvent.VK_C);
		menuOpts.add(cacheHelp);
		
		
		
		AbstractAction browseAct = new AbstractAction("Set Default Web Browser") {
			public void actionPerformed(ActionEvent e) {
				new DefaultBrowser().show();
			}
		};
		JMenuItem setBrowser = new JMenuItem(browseAct);
		menuOpts.add(setBrowser);
			
		
		
/**********************End opts menu*************************************/		
		
		JMenu menuHelp = new JMenu("Help");
		menuHelp.setMnemonic(KeyEvent.VK_H);
		mainMenuBar.add(menuHelp);

		// Help file menu item stuff
		JMenuItem miHelp = new JMenuItem(new AbstractAction("JMARS HomePage") {
			public void actionPerformed(ActionEvent e) {
				try {
					Util.launchBrowser(Config.get("wikipage"));
				} catch (Exception e1) {
					log.aprintln(e1);
				}
			}
		});
		miHelp.setMnemonic(KeyEvent.VK_H);
		menuHelp.add(miHelp);
		
		
		// Link to screencast tutorials
		JMenu tutHelp = new JMenu("Video Tutorials");
		
		JMenuItem allTutorials = new JMenuItem(new AbstractAction("All Video Tutorials") {
			public void actionPerformed(ActionEvent e) {
				try{
					Util.launchBrowser(Config.get("tutorialpage"));
				} catch (Exception e1){
					log.aprintln(e1);
				}
			}
		});
		tutHelp.add(allTutorials);
		tutHelp.addSeparator();
		
		JMenuItem video1 = new JMenuItem(new AbstractAction("Adv Shape Layer") {
			public void actionPerformed(ActionEvent e) {
				try{
					Util.launchBrowser(Config.get("advShapeLayer"));
				} catch (Exception e1){
					log.aprintln(e1);
				}
			}
		});
		tutHelp.add(video1);
		
		JMenuItem video2 = new JMenuItem(new AbstractAction("Computing Crater Areas") {
			public void actionPerformed(ActionEvent e) {
				try{
					Util.launchBrowser(Config.get("craterArea"));
				} catch (Exception e1){
					log.aprintln(e1);
				}
			}
		});
		tutHelp.add(video2);
		
		JMenuItem video3 = new JMenuItem(new AbstractAction("Crater Counting") {
			public void actionPerformed(ActionEvent e) {
				try{
					Util.launchBrowser(Config.get("craterCounting"));
				} catch (Exception e1){
					log.aprintln(e1);
				}
			}
		});
		tutHelp.add(video3);
		
		JMenuItem video4 = new JMenuItem(new AbstractAction("Custom Maps") {
			public void actionPerformed(ActionEvent e) {
				try{
					Util.launchBrowser(Config.get("customMaps"));
				} catch (Exception e1){
					log.aprintln(e1);
				}
			}
		});
		tutHelp.add(video4);
		
		JMenuItem video5 = new JMenuItem(new AbstractAction("High Resolution Export") {
			public void actionPerformed(ActionEvent e) {
				try{
					Util.launchBrowser(Config.get("highResExport"));
				} catch (Exception e1){
					log.aprintln(e1);
				}
			}
		});
		tutHelp.add(video5);
		
		JMenuItem video6 = new JMenuItem(new AbstractAction("Numeric Profiles") {
			public void actionPerformed(ActionEvent e) {
				try{
					Util.launchBrowser(Config.get("numericProfiles"));
				} catch (Exception e1){
					log.aprintln(e1);
				}
			}
		});
		tutHelp.add(video6);
		
		JMenuItem video7 = new JMenuItem(new AbstractAction("Polygon Functions") {
			public void actionPerformed(ActionEvent e) {
				try{
					Util.launchBrowser(Config.get("polyFunctions"));
				} catch (Exception e1){
					log.aprintln(e1);
				}
			}
		});
		tutHelp.add(video7);
		
		JMenuItem video8 = new JMenuItem(new AbstractAction("Saving Sessions/Layers") {
			public void actionPerformed(ActionEvent e) {
				try{
					Util.launchBrowser(Config.get("savingSessions"));
				} catch (Exception e1){
					log.aprintln(e1);
				}
			}
		});
		tutHelp.add(video8);
		
		JMenuItem video9 = new JMenuItem(new AbstractAction("Shape Layer") {
			public void actionPerformed(ActionEvent e) {
				try{
					Util.launchBrowser(Config.get("shapeLayer"));
				} catch (Exception e1){
					log.aprintln(e1);
				}
			}
		});
		tutHelp.add(video9);	
		menuHelp.add(tutHelp);
		
		// Link to forums
		JMenuItem frmHelp = new JMenuItem(new AbstractAction("Forums") {
			public void actionPerformed(ActionEvent e) {
				try {
					String url = Config.get("forumspage");				    
				    Util.launchBrowser(url);
				} catch(Exception _e) {
					log.aprintln(_e);
				}
			}
		});
		menuHelp.add(frmHelp);
		
		// Link to release notes
		JMenuItem relnHelp = new JMenuItem(new AbstractAction("Release Notes") {
			public void actionPerformed(ActionEvent e) {
				try {
					Util.launchBrowser(Config.get("releasenotespage"));
				} catch(Exception _e) {
					log.aprintln(_e);
				}
			}
		});
		menuHelp.add(relnHelp);
		
		menuHelp.addSeparator();
		
		// View logs menu item
		JMenuItem logHelp = new JMenuItem(new AbstractAction("View Log") {
			public void actionPerformed(ActionEvent e) {
				new LogViewer(fileLogger).getDialog().setVisible(true);
			}
		});
		logHelp.setMnemonic(KeyEvent.VK_G);
		menuHelp.add(logHelp);
		
		// disable the logger if the log object couldn't be created
		logHelp.setEnabled(fileLogger != null);
		
		// Report problem menu item
		JMenuItem reportHelp = new JMenuItem(new AbstractAction("Report a Problem") {
			public void actionPerformed(ActionEvent e) {
				new ReportCreator(Main.mainFrame, fileLogger).getDialog().setVisible(true);
			}
		});
		reportHelp.setMnemonic(KeyEvent.VK_R);
		menuHelp.add(reportHelp);
		
		AbstractAction aboutAction = new AbstractAction("About") {
			public void actionPerformed(ActionEvent e) {
				String msg =
					"<html><p>" +
					"<b>J</b>ava<br />" +
					"<b>M</b>ission-planning and<br />" +
					"<b>A</b>nalysis for<br />" +
					"<b>R</b>emote<br />" +
					"<b>S</b>ensing<br />" +
					"<br />" +
					"JMARS is a product of The ASU Mars<br />" +
					"Scientific Software Team.<br />" +
					"<br />" +
					"Your version number is: "+Util.getVersionNumber()+" <br /><br />" +
					"The source code is currently " + ABOUT().LINES + " lines<br />" +
					"(or " + ABOUT().PAGES + " printed pages), contained in "
					+ ABOUT().FILES + "<br />" +
					"source files that define " + ABOUT().CLASSES
					+ " java classes.<br />" +
					"<br />" +
					ABOUT_EMAIL + "</p>";

				try {
					JOptionPane.showMessageDialog(
							Main.this,
							new Object[] { msg, new UrlLabel(ABOUT_URL, null), "\n" },
							"About " + checkDemoOrBeta() + TITLE,// @since change bodies
							JOptionPane.PLAIN_MESSAGE);

				} catch (Exception ex) {
					JOptionPane.showMessageDialog(Main.this, msg, "About JMARS", JOptionPane.PLAIN_MESSAGE);
				}
			}
		};

		menuHelp.addSeparator();
		
		JMenuItem miAbout = new JMenuItem(aboutAction);
		miAbout.setMnemonic(KeyEvent.VK_A);
		menuHelp.add(miAbout);

		this.setJMenuBar(mainMenuBar);
	}

	private JMenuItem createPannerControlMenu() {
		boolean pannerOn = Config.get("panner.mode", PannerMode.Horiz.ordinal()) != PannerMode.Off.ordinal();
		final JCheckBoxMenuItem pannerSelect = new JCheckBoxMenuItem("Panner View", pannerOn);
		pannerSelect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				testDriver.setPannerMode(pannerSelect.isSelected() ? PannerMode.Horiz : PannerMode.Off);
			}
		});
		pannerSelect.setMnemonic('P');
		return pannerSelect;
	}
	
	//TODO: not sure this is the proper place for things to live, but it's
	// better here than in the ToolManager
	ThreeDViewWindow threeDWindow;
	private JCheckBoxMenuItem create3DMenu(){
		final JCheckBoxMenuItem shw3D = new JCheckBoxMenuItem("3D View");
		shw3D.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if(shw3D.isSelected()){
					if(threeDWindow==null){
						threeDWindow = new ThreeDViewWindow();
					}
					threeDWindow.setVisible(true);
				}else{
					if(threeDWindow!=null){
						threeDWindow.setVisible(false);
					}
				}
				
			}
		});
		
		return shw3D;
	}
    
	private static JFileChooser rcFileChooser;
	private static JFileChooser getRcFileChooser()
	 {
		if(rcFileChooser == null)
		 {
			rcFileChooser = new JFileChooser();
			rcFileChooser.setFileFilter(new SessFilter());
		 }
		return  rcFileChooser;
	 }
	
	protected void initFileMenu()
	 {
		final AbstractAction saveAction = new AbstractAction("Save") {
			{
				putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_S));
				setEnabled(rcFileChooser != null && rcFileChooser.getSelectedFile() != null);
			}

			public void actionPerformed(ActionEvent e) {
				saveState(getRcFileChooser().getSelectedFile().toString());
			}
		};
		
		JMenuItem saveMe = new JMenuItem(saveAction);
		saveMe.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
						
		menuFile.add(saveMe);
		
		AbstractAction saveAsAction = new AbstractAction("Save Session As...") {
			{
				putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_A));
			}

			public void actionPerformed(ActionEvent e) {
				JFileChooser fc = getRcFileChooser();
				if (Util.showSaveWithConfirm(fc, Main.this, EXT)) {
					File f = fc.getSelectedFile();
					saveState(f.toString());
					saveAction.setEnabled(true);
					setTitle(f.toString(), null);
				}
			}
		};
		menuFile.add(saveAsAction);
		
		final LoadSaveLayerDialogs layerDialogs = new LoadSaveLayerDialogs();
		
		AbstractAction saveLayersAction = new AbstractAction("Save Layers...") {
			{
				putValue(MNEMONIC_KEY, KeyEvent.VK_V);
			}
			public void actionPerformed(ActionEvent e) {
				JDialog dlg = layerDialogs.new SaveLayersDialog().dlg;
				dlg.setLocationRelativeTo(menuFile);
				dlg.setVisible(true);
			}
		};
		menuFile.add(saveLayersAction);
		
		AbstractAction loadLayersAction = new AbstractAction("Load Layers...") {
			{
				putValue(MNEMONIC_KEY, KeyEvent.VK_L);
			}
			public void actionPerformed(ActionEvent e) {
				layerDialogs.new LoadLayersDialog();
			}
		};
		menuFile.add(loadLayersAction);
		
		AbstractAction loadSessionAction = new AbstractAction("Load Session...") {
			{
				putValue(MNEMONIC_KEY, KeyEvent.VK_U);
			}
			public void actionPerformed(ActionEvent e) {
				boolean success = loadSession();
				saveAction.setEnabled(success);
			}
		};
		menuFile.add(loadSessionAction);
		
		menuFile.add(new JSeparator());

		AbstractAction action = new AbstractAction("Export at higher resolution...") {
            public void actionPerformed(ActionEvent e) {
            	HighResExport.exportHighResolution(Main.testDriver.mainWindow.getProj().getWorldWindow());
            }
          };

          JMenuItem miExportFullRes = new JMenuItem(action);
          miExportFullRes.setMnemonic(KeyEvent.VK_H);
          menuFile.add(miExportFullRes);
          
          AbstractAction geoFile = new AbstractAction("Export Georeference Data and TIFF") {
        	  public void actionPerformed(ActionEvent e) {
        		  GeoreferenceFileExport fge = new GeoreferenceFileExport();
        		  fge.calculateBbox();
                }
              };

              JMenuItem geoFileExport = new JMenuItem(geoFile);
              geoFileExport.setMnemonic(KeyEvent.VK_G);
              menuFile.add(geoFileExport);
		
		action = new AbstractAction("Capture To JPEG") {
            public void actionPerformed(ActionEvent e) {
                String filename = null;
                JFileChooser fc = getFileChooser(".jpg", "JPEG Files (*.jpg)");
                if (fc == null)
                    return;
                fc.setDialogTitle("Capture to JPEG File");
                
                if (fc.showSaveDialog(Main.mainFrame) != JFileChooser.APPROVE_OPTION)
                    return;
                if ( fc.getSelectedFile() != null )
                    filename = fc.getSelectedFile().getPath();
                
                if(filename == null)
                        return;
                testDriver.dumpMainLViewManagerJpg(filename);

            }
          };

          JMenuItem miCapture = new JMenuItem(action);
          miCapture.setMnemonic(KeyEvent.VK_C);
          menuFile.add(miCapture);

          action = new AbstractAction("Capture To PNG") {
              public void actionPerformed(ActionEvent e) {
                  String filename = null;
                  JFileChooser fc = getFileChooser(".png", "PNG Files (*.png)");
                  if (fc == null)
                      return;
                  fc.setDialogTitle("Capture to PNG File");
                  
                  if (fc.showSaveDialog(Main.mainFrame) != JFileChooser.APPROVE_OPTION)
                      return;
                  if ( fc.getSelectedFile() != null )
                      filename = fc.getSelectedFile().getPath();
                  
                  if (filename == null)
                      return;
                  testDriver.dumpMainLViewManagerPNG(filename);
              }
          };
          
          JMenuItem miPngCapture = new JMenuItem(action);
          miPngCapture.setMnemonic(KeyEvent.VK_P);
          menuFile.add(miPngCapture);

          action = new AbstractAction("Export Image...") {
        	  public void actionPerformed(ActionEvent e) {
        		  JFileChooser fc = new JFileChooser();
        		  fc.setAcceptAllFileFilterUsed(false);
        		  fc.setMultiSelectionEnabled(false);
        		  Iterator<ImageWriterSpi> spiIt = IIORegistry.getDefaultInstance().getServiceProviders(ImageWriterSpi.class, true);
        		  Map<FileFilter,ImageWriterSpi> filterMap = new HashMap<FileFilter,ImageWriterSpi>();
        		  while (spiIt.hasNext()) {
            		  ImageWriterSpi spi = spiIt.next();
            		  final String name = spi.getDescription(Locale.getDefault());
            		  final Collection<String> suffixes = new LinkedHashSet<String>();
            		  for (String suffix: spi.getFileSuffixes()) {
            			  suffixes.add(suffix.toLowerCase());
            		  }
            		  FileFilter filter = new FileFilter() {
            			  public boolean accept(File f) {
            				  if (f.isDirectory()) {
            					  return true;
            				  }
            				  String fname = f.getName();
            				  int idx = fname.lastIndexOf(".");
            				  if (idx < 0 || idx + 1 >= fname.length()) {
            					  return false;
            				  }
            				  String suffix = fname.substring(idx+1).toLowerCase();
            				  return suffixes.contains(suffix);
            			  }

            			  public String getDescription() {
            				  return name;
            			  }
            		  };
            		  fc.addChoosableFileFilter(filter);
            		  filterMap.put(filter, spi);
        		  }
        		  if (JFileChooser.APPROVE_OPTION == fc.showDialog(Main.this, "Export")) {
        			  File f = fc.getSelectedFile();
        			  FileFilter filter = fc.getFileFilter();
        			  ImageWriterSpi spi = filterMap.get(filter);
        			  try {
        				  ImageWriter writer = spi.createWriterInstance();
        				  writer.setOutput(ImageIO.createImageOutputStream(f));
        				  boolean canAlpha = Arrays.asList("tiff","tif","png").contains(spi.getFileSuffixes()[0].toLowerCase());
        				  writer.write(Main.testDriver.mainWindow.getSnapshot(!canAlpha));
        			  } catch (IOException e1) {
        				  e1.printStackTrace();
        				  String msg = "Error writing file:\n\n" + e1.getMessage() + "\n\nSee log for more details";
        				  JOptionPane.showMessageDialog(Main.this, msg, "Unable to export image", JOptionPane.ERROR_MESSAGE);
        			  }
        		  }
        	  }
          };
          menuFile.add(action);

         menuFile.add(new JSeparator());

         AbstractAction exitAction =
         	new AbstractAction("Exit")
			    {
				     public void actionPerformed(ActionEvent e)
				     {
				    	 try {
				    		 Main.cleanSessionKeyDirectories();
				    		 autosave();
				    	 } catch (Exception ex) {
				    		 ex.printStackTrace();
				    	 }
				    	 System.exit(0);
				     }
			    }
                ;

          JMenuItem miExit = new JMenuItem(exitAction);
          miExit.setMnemonic(KeyEvent.VK_X);
          menuFile.add(miExit);
        }
    
        private static HashMap fileChoosersMap = new HashMap();
    	public static JFileChooser getFileChooser(final String extension, final String description)
    	{
            JFileChooser fileChooser = (JFileChooser) fileChoosersMap.get(extension);
    	    if (fileChooser == null)
    	    {
    	        // Create the file chooser
    	        fileChooser = new JFileChooser(System.getProperty("user.dir"));
    	        fileChooser.addChoosableFileFilter(
    	                                            new javax.swing.filechooser.FileFilter()                                                {
    	                                                public boolean accept(File f)
    	                                                {
    	                                                    String fname = f.getName().toLowerCase();
    	                                                    return  f.isDirectory()  ||  fname.endsWith(extension);
    	                                                }
    	                                                public String getDescription()
    	                                                {
    	                                                    return description;
    	                                                }
    	                                            }
    	        );
                
                fileChoosersMap.put(extension, fileChooser);
    	    }
            
    	    return  fileChooser;
    	}
     
	 // saves the application properties to an external file (fname). 
	 protected void saveState(String fname)
	 {
		 // update the User Properties 
		 userProps.reset();
		 testDriver.saveState();

		 // Write the user properties to the config file.
		 try
		 {
			 userProps.savePropertiesFile(fname);
		 }
		 catch(IOException e)
		 {
			 log.aprintln(e);
			 JOptionPane.showMessageDialog(
				 this,
				 "Unable to save JMARS file:\n\n" + fname + "\n\n" + e + "\n\n",
				 "FILE SAVE ERROR",
				 JOptionPane.ERROR_MESSAGE);
		 }
	 }

	public static void setProjection(ProjObj po)
	 {
		if(po.getClass() != PO.getClass())
		 {
			log.aprintln("CANNOT ALTER PROJECTION TYPE AT RUNTIME!");
			log.aprintStack(-1);
			log.aprintln("CANNOT ALTER PROJECTION TYPE AT RUNTIME!");
			return;
		 }

		synchronized(listenerList)
		 {
			ProjObj old = PO;
			PO = po;
			fireProjectionEvent(old);
		 }
	 }

	/**
	 ** Used for ProjectionListener registrations.
	 **/
	private static EventListenerList listenerList = new EventListenerList();

	public static void addProjectionListener(ProjectionListener l)
	 {
		listenerList.add(ProjectionListener.class, l);
	 }

	public static void removeProjectionListener(ProjectionListener l)
	 {
		listenerList.remove(ProjectionListener.class, l);
	 }

	protected static void fireProjectionEvent(ProjObj old)
	 {
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();

		// Process the listeners last to first, notifying those that
		// are interested in this event
		ProjectionEvent e = null;
		for(int i=listeners.length-2; i>=0; i-=2)
			if(listeners[i] == ProjectionListener.class)
			 {
				// Lazily create the event
				if(e == null)
					e = new ProjectionEvent(old);
				ProjectionListener l = (ProjectionListener) listeners[i+1];
				l.projectionChanged(e);
			 }
	 }
	
    // Check that we are on Mac OS X.  This is crucial to loading and using the OSXAdapter class.
    public static boolean MAC_OS_X = (System.getProperty("os.name").toLowerCase().startsWith("mac os x"));

	/**
	 * @return the body
	 * @since change bodies
	 */
	public static String getBody() {
		return Config.get(Util.getProductBodyPrefix() + Config.CONFIG_BODY_NAME, "Mars");
	}

	private static JFileChooser savedSessionChooser = null;
	
	/**
	 * @since load sessions
	 */
	public boolean loadSession() {
		boolean success = true;//boolean that if is false, will not enable the save menu
		if (savedSessionChooser == null) {
			savedSessionChooser = getRcFileChooser();
		}
		if (JFileChooser.APPROVE_OPTION == savedSessionChooser.showOpenDialog(Main.mainFrame)) {
			File file = getSelectedFile(savedSessionChooser);
			if (file.exists() && file.canRead()) {
				try {
					//The following logic is for loading sessions and making sure that we are on the right body with the right configuration.
					boolean switchToNewBodyFlag = false;
					final AtomicReference<String> bodyFromComboSelection = new AtomicReference<String>();
					//this is a separate process from the actual loading, but we will check the body situation and do a confirmation if it is a different body
					//we will prompt the user when we recognize a different body.
					UserProperties tmpUP = new UserProperties(file);//get the user properties, but just tmp because we will get them again later if needed
					String tmpSelBody = Config.get(Config.CONFIG_SELECTED_BODY, "Mars");//config selected body
					String tmpSessionBody = tmpUP.getProperty("selectedBody");//session selected body
					final AtomicBoolean confirmed = new AtomicBoolean(true);//this is a flag to be changed in an actionPerformed to confirm the switch
					if (tmpSessionBody != null && !tmpSessionBody.equalsIgnoreCase(tmpSelBody)) {//not null and different then current selected body
						confirmed.set(false);//intialize confirmed to false
						String firstChar = tmpSessionBody.substring(0,1);
						firstChar = firstChar.toUpperCase();
						tmpSessionBody = firstChar + tmpSessionBody.substring(1);
						String msg = "Are you sure you want to switch to body: "+tmpSessionBody+" ?";
						JOptionPane op = new JOptionPane(msg,JOptionPane.PLAIN_MESSAGE,JOptionPane.OK_CANCEL_OPTION);
						JDialog dialog = op.createDialog(null, "Confirm switch body");
						dialog.setVisible(true);
						Object selectedValue = op.getValue();
						if (selectedValue == null) {
							confirmed.set(false);
						} else if (selectedValue instanceof Integer) {
							Integer selValue = (Integer) selectedValue;
							if (selValue.intValue() == JOptionPane.OK_OPTION) {
								confirmed.set(true);
								switchToNewBodyFlag = true;
							}
						}		
					} else if (tmpSessionBody == null) {
						//attribute is not in session file, we need to prompt the user for the body
						final JDialog bodyDialog = new JDialog();
						bodyDialog.setModal(true);
						bodyDialog.setLayout(new FlowLayout());
						bodyDialog.setSize(300, 200);
						
						//build a list of bodies
						TreeMap<String, String[]> bodyList = Util.getBodyList();
						ArrayList<String> bList = new ArrayList<String>();
						Iterator<Entry<String, String[]>> iter = bodyList.entrySet().iterator();
						while (iter.hasNext()) {
							String[] bodyArr = (String[]) ((Entry<String, String[]>) iter.next()).getValue();
							for(String bdy : bodyArr) {
								bList.add(bdy.toUpperCase());
							}
						}
						JTextPane textPane = new JTextPane();
						textPane.setText("Please select the planetary body associated\n with this session. To avoid this in the future,\n please re-save the session.");
						textPane.setSize(60, 60);
						bodyDialog.add(textPane);
						
						final JComboBox bodyCombo = new JComboBox(bList.toArray(new String[]{}));
						bodyCombo.setSize(100, 20);
						bodyDialog.add(bodyCombo);
						JButton selectButton = new JButton("Select");
						selectButton.setSize(100,100);
						selectButton.addActionListener(new ActionListener() {
							
							public void actionPerformed(ActionEvent e) {
								// This section is executed when a user is loading a session with no selected body, selects a body from 
								//a dialog, and clicks select. We check to see if we are switching bodies. If we are, we need
								//to refresh the map servers, etc. so that the new body can be loaded.
								String tempBody = (String) bodyCombo.getSelectedItem();
								bodyFromComboSelection.set(tempBody);
								bodyDialog.dispose();
							}
						});
						bodyDialog.add(selectButton);
						
						JButton cancelButton = new JButton("Cancel");
						cancelButton.setSize(100,100);
						cancelButton.addActionListener(new ActionListener() {
							
							public void actionPerformed(ActionEvent e) {
								confirmed.set(false);//the user has canceled and we need to not load the session
								bodyDialog.dispose();
							}
						});
						bodyDialog.add(cancelButton);
						
						
						Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
						Dimension d = bodyDialog.getSize();
						int x = (screen.width - d.width) / 2;
						int y = (screen.height - d.height) / 2;
						bodyDialog.setLocation(x, y);
						bodyDialog.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
						bodyDialog.setVisible(true);
					}
					
					if (confirmed.get()) {//the user did not cancel
						//we are switching session key at this point. We either switch it to the one from the file or we generate a new one. 
						//Either way, the existing one is gone. Delete it.
						Main.deleteSessionKeyDirectory();
						boolean switchedBodies = false;
						boolean cancel = removeAllCurrentViews(true, Main.MODE_LOAD_SESSION);
						if (!cancel) {
							userProps.reset();
							userProps = new UserProperties(file);
							getRcFileChooser().setSelectedFile(file);// to enable "Save"
							String comboBodySelectionValue = bodyFromComboSelection.get();
							if (switchToNewBodyFlag || comboBodySelectionValue != null) {
								//this means that the session file was different than the selected body, so we need to reset values
								//we need to select the body from the session
								String newBody = null;
								if (comboBodySelectionValue != null) {
									newBody = bodyFromComboSelection.get();
								} else {
									newBody = userProps.getProperty("selectedBody");
								}
								if (menuBody != null) {
									menuBody.setEnabled(false);
								}
								setCurrentBody(newBody);
								setSelectedBody(newBody);
								changeConfigValues();
								LViewFactory.refreshAllLViews();
								refreshMainView(false);//this has to be called because it changes the map server settings over to the correct body
								TITLE = Config.get(Util.getProductBodyPrefix()+Config.CONFIG_EDITION);
								mainFrame.setTitle(checkDemoOrBeta() + TITLE);
								updateSelectedBody();
								switchedBodies = true;
							}
							
							Main.sessionKey = Main.generateSessionKey();
							String sessionKeyPath = Main.getFullSessionKeyPath();	
							File bbDir = new File(sessionKeyPath);
							bbDir.mkdir();
							//The following section is for body switching. When the user saved the session, the SavedLayers files for each body
							//were written out to the session. This section will read in those objects from the session and re-create the files
							
							//no way to not have type safety warning here since we are deserializing the HashMap. It is not worth any possible solution that
							//would check every value in the map and make sure that it of the right type. 
							HashMap<String,ArrayList<SavedLayer>> bodyFileMap = (HashMap<String,ArrayList<SavedLayer>>) userProps.loadUserObject(Main.BODY_FILE_MAP_STR);
							if (bodyFileMap != null && !bodyFileMap.isEmpty()) {
								Iterator<String> iter = bodyFileMap.keySet().iterator();
								while (iter.hasNext()) {
									String fileName = iter.next();
									String newFileName = fileName;
									//version 3.0.0 used .xml as the extention. This will check for that file extension, replace with .jlf and eventually phase out the need for it.
									if (fileName.endsWith(".xml")) {
										newFileName = fileName.substring(0,fileName.lastIndexOf(".xml"));
										newFileName += Main.LAYERS_FILE_EXT;
									}
									try {
										FileOutputStream fos = new FileOutputStream(new File(sessionKeyPath+newFileName));
										SavedLayer.save((ArrayList<SavedLayer>)bodyFileMap.get(fileName), fos);
										fos.close();
									} catch (Exception e) {
										//if we fail on one file, move on to the next
										log.println("Failed to load saved layer file: "+fileName);
									}
								}
							}
							String versionNum = userProps.getProperty("versionNumber");
							if (versionNum != null && versionNum.trim().equalsIgnoreCase("3.0.0")) {
								//for version 3.0.0, we put saved layer files in the session as String properties. We will
								//handle that temporarily even though it was changed in 3.0.1 to be serialized in a HashMap
								//Remove this entire if statement when it is believed safe to do so
								for (int i=0; i<100; i++) {//arbitrary number of 100 bodies that we might have in a session file
									String key = Main.SESSION_FILE_STR+i;
									String filename = Main.SESSION_FILE_NAME_STR+i;
									String savedLayerFileContents = userProps.getProperty(key);
									String savedLayerFilename = userProps.getProperty(filename);
									if (savedLayerFileContents == null) {
										break;
									}
									File f = new File(sessionKeyPath+savedLayerFilename);
									FileOutputStream fos = new FileOutputStream(f);
									fos.write(savedLayerFileContents.getBytes());
									fos.close();
								}
							}
							
							String lon, lat;
							lon = userProps.getProperty("Projection_lon");
							lat = userProps.getProperty("Projection_lat");
							startupInCyl(lon, lat);
							restoreMapLocation();
							if (!switchedBodies) {
								//if we switched bodies for this session file, we already call buildViews in a new thread in refreshMainView();
								testDriver.buildViews();
							}
							setTitle(file.toString(), null);
							
						} else {
							success = false;
						}
					} else {
						success = false;
					}
				} catch (Exception e) {
					e.printStackTrace();
					String msg = "There was an error loading the sessions file. ";
					JOptionPane.showMessageDialog(Main.mainFrame, msg, "Unable load file", JOptionPane.ERROR_MESSAGE);
					success = false;
				}
			} else {
				String msg = "The file was not found or can not be read. ";
				JOptionPane.showMessageDialog(Main.mainFrame, msg, "Unable load file", JOptionPane.ERROR_MESSAGE);
				success = false;
			}
		} else {
			success = false;//add this condition to not activate the "Save" option under file if they select "Cancel" 
		}
		return success;
	}
	private static void deleteSessionKeyDirectory() {
		try {
			File bodyDir = new File(getJMarsPath()+"bodies");
			if (bodyDir.exists()) {
				File directory = new File(Main.getJMarsPath()+getBodyBaseDir());
				Util.recursiveRemoveDir(directory);
			}
		} catch(Exception e) {
			e.printStackTrace();
			log.println(e);
		}
	}
	private void restoreMapLocation() {
		
		String projLon = Main.userProps.getProperty("Projection_lon", "");
		String projLat = Main.userProps.getProperty("Projection_lat", "");
		
		Integer ppd = Main.userProps.getPropertyInt("MainZoom", 32);
		
		double projLonDb = Double.parseDouble(projLon);
		double projLatDb = Double.parseDouble(projLat);
		
		ProjObj po = new ProjObj.Projection_OC(-projLonDb, projLatDb);
		Main.setProjection(po);
		
		String xStr = Main.userProps.getProperty("Initialx", "");
		String yStr = Main.userProps.getProperty("Initialy", "");
		Double lonD = new Double(xStr);
		Double latD = new Double(yStr);
		
		//set location
		Point2D tempPoint2d = new Point2D.Double(lonD,latD);
		Main.testDriver.locMgr.setLocation(tempPoint2d, true);
		//set PPD
		Main.testDriver.mainWindow.getZoomManager().setZoomPPD(ppd, true);
		
	}

	/** Filters file chooser selections down to readable files that have the .jmars extension */
	private static final class SessFilter extends FileFilter {
		public boolean accept(File f) {
			return f.isDirectory() || (f.isFile() && f.canRead() && f.getName().toLowerCase().endsWith(Main.EXT));
		}
		public String getDescription() {
			return "JMARS Session File (*" + Main.EXT + ")";
		}
	}
	/** Returns the selected file on 'fc', with the file format suffix added if necessary */
	private static final File getSelectedFile(JFileChooser fc) {
		File f = fc.getSelectedFile();
		String name = f.getAbsolutePath();
		if (fc.getFileFilter() instanceof SessFilter && !name.toLowerCase().trim().endsWith(Main.EXT)) {
			name = name + Main.EXT;
		}
		return new File(name);
	}
	
	
	// Used to set the location on earth in default start up (when no location arguments
	//	are passed through the command line.  Recenters on user's ip address location
	
	//TODO: NO LONGER USED because the service which converted ip address to lat 
	//		lon no longer works/exits.  Could be reimplemented in the futur if another service
	//		is provided/if the desire is expressed.
	public static String[] getHomeLocation(){
		String[] location = {"0","0"};
		if (!getBody().equalsIgnoreCase("earth")){
			return location;
		}
		String ipAddr=null;
		String ipQuery = Config.get("ipLookUpURL");
		String latLonQuery= Config.get("latLonLookUpURL");
		String lat = "";
		String lon = "";
		JmarsHttpRequest request = null;
		try{
			//Gets the user's ip address
//			URL ipUrl = new URL(ipQuery);
//			URLConnection ipConn = ipUrl.openConnection();
            request = new JmarsHttpRequest(ipQuery, HttpRequestType.GET);
//		    ipConn.setConnectTimeout(10000);
//			ipConn.setReadTimeout(10000);
            request.setConnectionTimeout(10000);
            request.setReadTimeout(10000);
            boolean successful = request.send();
			ObjectInputStream ois = new ObjectInputStream(request.getResponseAsStream());
			ipAddr = (String) ois.readObject();
			request.close();
			latLonQuery+=ipAddr;
			//Converts ip to lat lon
//			URL url = new URL(latLonQuery);
//			URLConnection conn = url.openConnection();
//			conn.setConnectTimeout(10000);
//			conn.setReadTimeout(10000);
            request = new JmarsHttpRequest(latLonQuery, HttpRequestType.GET);
            request.setConnectionTimeout(10000);
            request.setReadTimeout(10000);
            request.send();
			InputStreamReader is = new InputStreamReader(request.getResponseAsStream());
			BufferedReader br = new BufferedReader(is);
			
			StringBuilder jsonread = new StringBuilder();
			String aux = "";
			
			while ((aux = br.readLine()) != null)
				jsonread.append(aux);
			
			JSONObject geoip = new JSONObject(jsonread.toString());
			
			location[0] = geoip.getString("geobyteslongitude");
			location[1] = geoip.getString("geobyteslatitude");
			
//			ois.close();
//			is.close();
			request.close();
			
			return location;
		} catch (UnknownHostException err){
			err.printStackTrace();
			log.println(err);
			return new String[]{"0","0"};
		} catch (Exception err2){
			err2.printStackTrace();
			log.println(err2);
			return new String[]{"0","0"};
		}	
	}
	
	public static boolean quit() {
		try {
   		 Main.cleanSessionKeyDirectories();
   		 autosave();
   	 } catch (Exception ex) {
   		 ex.printStackTrace();
   		 return false;
   	 }
   	 System.exit(0);
   	 return true;
	}
}

class OSXresponder { 
	protected static DebugLog log = DebugLog.instance();
	private String startupFile = null;
	
    // Generic registration with the Mac OS X application menu
    // Checks the platform, then attempts to register with the Apple EAWT
    // See OSXAdapter.java to see how this is done without directly referencing any Apple APIs
    public void registerForMacOSXEvents() {
            try {
                // Generate and register the OSXAdapter, passing it a hash of all the methods we wish to
                // use as delegates for various com.apple.eawt.ApplicationListener methods
                /*  OSXAdapter.setAboutHandler(this, getClass().getDeclaredMethod("about", (Class[])null));
                  OSXAdapter.setPreferencesHandler(this, getClass().getDeclaredMethod("preferences", (Class[])null));
                */
            	
            	OSXAdapter.setQuitHandler(this, Main.class.getDeclaredMethod("quit"));
            	
               // register our method for handling file association events	
               OSXAdapter.setFileHandler(this, getClass().getDeclaredMethod("loadJmarsFile", new Class[] { String.class }));
            } catch (Exception e) {
				log.aprintln(e);
                System.err.println("Error while loading the OSXAdapter:");
                e.printStackTrace();
            }
    }

    // This method is the used for handling file association events on MAC OS X
    public void loadJmarsFile(String path) {
    	startupFile = path;
    }
 
    public String getStartupFile(){
    	return startupFile;
    }
}

