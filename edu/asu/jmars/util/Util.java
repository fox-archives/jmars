// Copyright 2008, Arizona Board of Regents
// on behalf of Arizona State University
// 
// Prepared by the Mars Space Flight Facility, Arizona State University,
// Tempe, AZ.
// 
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
// 
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.


package edu.asu.jmars.util;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.color.ICC_ProfileGray;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.PostMethod;

import edu.asu.jmars.Main;
import edu.asu.jmars.ProjObj.Projection_OC;
import edu.asu.jmars.layer.MultiProjection;
import edu.stanford.ejalbert.BrowserLauncher;

/* Collection of useful things for jlayers... all methods are static,
   "Util" is just a handy namespace kludge, not a real object type */
public final class Util
 {
    private static DebugLog log = DebugLog.instance();

    public static final Color darkGreen = Color.green.darker();
    public static final Color darkRed = Color.red.darker();
    public static final Color purple = new Color(128, 0, 128);
    public static final Color green3 = new Color(0,205,0);
    public static final Color darkViolet = new Color(148,0,211);
    public static final Color cyan3 = new Color(0,205,205);
    public static final Color chocolate4 = new Color(139,69,19);
    public static final Color maroon = new Color(176,48,96);
    public static final Color yellow3 = new Color(205,205,0);
    public static final Color gray50 = new Color(128,128,128);
    public static final Color darkOrange = new Color(255,140,0);

     // This is to prevent some ne'er-do-well from coming in and trying 
     // to instanciate what is supposed to be class of nothing but static methods.
     private Util(){}

    /**
     ** If a String is non-null, returns it, otherwise returns the
     ** empty string.
     **/
    public static String blankNull(String s)
     {
	return  s == null ? "" : s;
     }

    /**
     ** Adds a MouseListener to a container and all its children.
     **/
    public static void addMouseListenerToAll(Container cont, MouseListener ml)
     {
	cont.addMouseListener(ml);
	Component[] comps = cont.getComponents();
	for(int i=0; i<comps.length; i++)
	    if(comps[i] instanceof Container)
		addMouseListenerToAll((Container) comps[i], ml);
	    else
		comps[i].addMouseListener(ml);
     }

    /**
     ** Given a component, creates an image the same size as it and
     ** containing a painted version of it with a given alpha.
     **/
    public static BufferedImage createImage(Component comp, float alpha)
     {
	BufferedImage img = Util.newBufferedImage(comp.getWidth(),
						  comp.getHeight());
	Graphics2D g2 = img.createGraphics();
	if(alpha != 1)
	    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC,
						       alpha));
	comp.paint(g2);
	g2.dispose();

	return  img;
     }

    /**
     ** Dumps out a Shape's path.
     **/
    public static void printShape(String j, Shape s)
     {
	String str = (s + " ------------------------ " + j);
	PathIterator i = s.getPathIterator(null);
	boolean hasNaN = false;
	while(!i.isDone())
	 {
	    double[] coords = new double[2];
	    switch(i.currentSegment(coords))
	     {
	     case PathIterator.SEG_MOVETO:
		str += "\n" + ("MOVE\t" + coords[0] + "\t" + coords[1]);
		if(Double.isNaN(coords[0])  ||
		   Double.isNaN(coords[1]))
		    hasNaN = true;
		break;

	     case PathIterator.SEG_LINETO:
		str += "\n" + ("LINE\t" + coords[0] + "\t" + coords[1]);
		if(Double.isNaN(coords[0])  ||
		   Double.isNaN(coords[1]))
		    hasNaN = true;
		break;

	     case PathIterator.SEG_CLOSE:
		str += "\n" + ("CLOSE");
		break;

	     default:
		str += "\n" + ("UNKNOWN SEGMENT TYPE!");
		break;
	     }
	    i.next();
	 }
	if(hasNaN)
	    log.aprintln(str);
     }

    /**
     ** Reverses the elements in an array of doubles, in place.
     **/
    public static void reverse(double[] values)
     {
	final int middle = values.length >> 1;
	for(int i=0, j=values.length-1; i<middle; i++, j--)
	 {
	    double tmp = values[i];
	    values[i] = values[j];
	    values[j] = tmp;
	 }
     }

    /**
     ** Clones the portion of an array between fromIndex (inclusive)
     ** and toIndex (exclusive).
     **/
    public static Object cloneArray(Object orig, int fromIndex, int toIndex)
     {
	Class cl = orig.getClass().getComponentType();
	if(cl == null)
	    throw new IllegalArgumentException(
		"First argument to cloneArray must be an array!");

	Object copy = Array.newInstance(cl, toIndex-fromIndex);
	System.arraycopy(orig, fromIndex, copy, 0, toIndex-fromIndex);
	return  copy;
     }

    /**
     ** Splits a single-line string on commas, taking into account
     ** backslash-escaped commas and backslashes.
     **
     ** @throws IllegalArgumentException The input string contains
     ** carriage returns or linefeeds, or any backslashes that aren't
     ** used to escape a backslash or a comma.
     **/
    public static final String[] splitOnCommas(String s)
     {
	if(s.indexOf('\n') != -1  ||  s.indexOf('\r') != -1)
	    throw  new IllegalArgumentException(
		"Carriage returns and linefeeds aren't allowed in " +
		"splitOnCommas() input string.");

	// Replace all double backslashes with a linefeed. There are
	// two levels of escape on both backslashes in the string
	// literal below... Java string escape and regex escape.
	// Annoying, eh? :)
	s = s.replaceAll("\\\\\\\\", "\n");

	// Replace all backslash-comma sequences with a carriage
	// return.
	s = s.replaceAll("\\\\,", "\r");

	if(s.indexOf('\\') != -1)
	    throw  new IllegalArgumentException(
		"Illegal backslash escape sequence.");

	// Do our comma-splitting
	String[] f = s.split(",");

	// Restore the escaped commas and backslashes. Again we have
	// double-escaping due to Java and regex.
	for(int i=0; i<f.length; i++)
	    f[i] = f[i].replaceAll("\n", "\\\\").replaceAll("\r", ",");

	return  f;
     }

    /**
     ** Given an exception, returns a string describing the exceptiona
     ** and all its chained causes. Specifically, it returns
     **
     ** "e1 filler e2 filler e3 ..."
     **
     ** where e1 is e.toString(), e2 is e.getCause().toString(), e3 is
     ** e.getCause().getCause().toString(), and so on until there are
     ** no remaining causes.
     **/
    public static String chainToString(Throwable e, String filler)
     {
	String msg = e.toString();
	while((e = e.getCause()) != null)
	    msg += filler + e;
	return  msg;
     }

    /**
     ** Utility method that creates an exception object of the same
     ** type as the one passed in, with the same message, but with its
     ** cause set as the original. Useful for throwing exceptions
     ** across different threads, while preserving context.
     **
     ** In the event of an error while trying to clone the argument,
     ** the original is returned.
     **/
    public static Throwable chainClone(Throwable orig)
     {
	try
	 {
	    Throwable copy = (Throwable)
		orig.getClass()
		.getDeclaredConstructor(new Class[] { String.class })
		.newInstance(new Object[] { orig.getMessage() });
	    copy.initCause(orig);
	    return  copy;
	 }
	catch(Throwable e)
	 {
	    log.aprintln("-- UNABLE TO CLONE THE FOLLOWING EXCEPTION:");
	    log.aprintln(e);
	    log.aprintln("-- DUE TO THE FOLLOWING PROBLEM:");
	    log.aprintln(orig);
	    return  orig;
	 }
     }

    /**
     ** Utility method that executes a Runnable synchronously on the
     ** AWT thread, no matter who calls it. This form uses a supplied
     ** object as the synchronization object for coordinating with
     ** AWT, instead of AWT's internal objects. THIS IS USEFUL
     ** STRICTLY FOR VERY SPECIFIC MULTI-THREADING ISSUES (SEE
     ** MICHAEL), OTHERWISE USE THE SIMPLER VERSION BELOW THAT DOESN'T
     ** TAKE AN EXTRA OBJECT.
     **
     ** <p>Basically this is a safe version of {@link
     ** SwingUtilities#invokeAndWait}, implemented using {@link
     ** SwingUtilities#invokeLater}.
     **
     ** <p>The implementation of this method invokes lock.notifyAll()
     ** and lock.wait(), thereby relinquishing the object if it's
     ** already locked by the caller (we lock it ourselves here, just
     ** in case it isn't).
     **
     ** @throws RuntimeException If the underlying doRun.run() throws
     ** one.
     **
     ** @throws Error If the underlying doRun.run() throws one.
     **
     ** @throws UndeclaredThrowableException If the underlying
     ** doRun.run() throws an unchecked exception, or if anything else
     ** goes wrong.
     **/
    public static void invokeAndWaitSafely(final Object lock,
					   final Runnable doRun)
     {
	// If we're called from AWT directly, just do the deed and we're done.
	if(SwingUtilities.isEventDispatchThread())
	 {
	    doRun.run();
	    return;
	 }

	// Used to prevent a missed notify, just cause I'm paranoid.
	final boolean[] awtFinished = { false };

	// Used to hold any exceptions thrown from doRun within the
	// AWT thread.
	final Throwable[] exc = { null };

	// Queue doRun to the AWT event-handling thread. This call
	// returns immediately.
	SwingUtilities.invokeLater(
	    new Runnable()
	     {
		// Eventually AWT will execute this for us, when it
		// feels like it.
		public void run()
		 {
		    try
		     {
			doRun.run();
		     }
		    catch(Throwable e)
		     {
			exc[0] = e;
		     }

		    // Okay, AWT has executed it for us. Time to allow
		    // the wait() in code further below to unblock.
		    synchronized(lock)
		     {
			awtFinished[0] = true;
			lock.notifyAll();
		     }
		 }
	     }
	    );

	try
	 {
	    synchronized(lock)
	     {
		// Block until AWT has found time to execute our code
		// up above. We unblock once notifyAll() is called
		// above.
		while(!awtFinished[0])
		    lock.wait(500); // half-sec timeout, just in case
	     }
	 }
	catch(InterruptedException e)
	 {
	    log.aprintln(e);
	 }

	// Finally, behave just as if we'd executed doRun.run()
	// without any threading stuff... if it threw an exception,
	// then so do we (of the same type if it's an unchecked
	// exception)!
	if(exc[0] != null)
	 {
	    if(exc[0]
	       instanceof RuntimeException)
		throw (RuntimeException) chainClone(exc[0]);

	    if(exc[0]
	       instanceof Error)
		throw (Error) chainClone(exc[0]);

	    throw  new UndeclaredThrowableException(exc[0]);
	 }
     }

    /**
     ** Utility method that executes a Runnable synchronously on the
     ** AWT thread, no matter who calls it. If called within the AWT
     ** even dispatch thread, then the Runnable is invoked in the
     ** current thread. If called on any other thread, the Runnable is
     ** passed to {@link SwingUtilities#invokeAndWait}.
     **
     ** <p>Basically this is a safe version of {@link
     ** SwingUtilities#invokeAndWait}.
     **
     ** @throws UndeclaredThrowableException if invokeAndWait throws
     ** an exception.
     **/
    public static void invokeAndWaitSafely(Runnable doRun)
     {
	if(SwingUtilities.isEventDispatchThread())
	    doRun.run();
	else
	    try
	     {
		SwingUtilities.invokeAndWait(doRun);
	     }
	    catch(InterruptedException e)
	     {
		throw new UndeclaredThrowableException(e);
	     }
	    catch(InvocationTargetException e)
	     {
		throw new UndeclaredThrowableException(e);
	     }
     }

    /**
     ** Given an array of longitude/latitude points representing the
     ** vertices of a spherical polygon, returns the surface area of
     ** the polygon. Assumes a unit sphere. The polygon is assumed to
     ** be closed, so there is no need to pass a copy of the first
     ** vertex as the last vertex.
     **
     ** <p>To convert the result to square kilometers, assuming a
     ** spherical Mars with radius 3386 km (the average of the polar
     ** and equatorial ellipsoid radii), use the following:
     **
     ** <pre>unitArea = Util.sphericalArea(...);
     **      kmArea = unitArea * 3386 * 3386;   </pre>
     **/
    public static double sphericalArea(Point2D[] polygonLL)
     {
	HVector[] polygonV = new HVector[polygonLL.length];
	for(int i=0; i<polygonLL.length; i++)
	    polygonV[i] = new HVector(polygonLL[i]);
	return  sphericalArea(polygonV);
     }

    /**
     ** Given an array of vectors representing the vertices of a
     ** spherical polygon, returns the surface area of the
     ** polygon. Assumes a unit sphere. The polygon is assumed to be
     ** closed, so there is no need to pass a copy of the first vertex
     ** as the last vertex.
     **
     ** <p>To convert the result to square kilometers, assuming a
     ** spherical Mars with radius 3386 km (the average of the polar
     ** and equatorial ellipsoid radii), use the following:
     **
     ** <pre>unitArea = Util.sphericalArea(...);
     **      kmArea = unitArea * 3386 * 3386;   </pre>
     **/
    public static double sphericalArea(HVector[] polygonV)
     {
	// Calculating the area of a spherical polygon is actually
	// easier than doing so on the plane, using what's called the
	// spherical excess formula. If the polygon has N vertices,
	// then its area is equal to the sum of the interior angles,
	// minus (N-2)*PI.
	double area = 0;
	for(int i=0; i<polygonV.length; i++)
	    area += sphericalAngle(polygonV[ i                     ],
				   polygonV[(i+1) % polygonV.length],
				   polygonV[(i+2) % polygonV.length]);
	area -= (polygonV.length-2) * Math.PI;

	// The one catch: depending on whether the polygon winds
	// clockwise or counter-clockwise, we may have just summed the
	// internal angles OR we might have summed the EXTERNAL
	// angles! If so, we wound up with an area greater than half
	// the sphere... so just invert.
	if(area > 2 * Math.PI)
	    area = 4*Math.PI - area;

	return  area;
     }

    /**
     ** Returns the counter-clockwise angle (in radians) created by
     ** vertex abc on the unit sphere's surface. The returned value is
     ** always between 0 and 2*PI (i.e. it's never negative).
     **/
    public static double sphericalAngle(HVector a, HVector b, HVector c)
     {
	HVector bb = b.unit();

	// Get the sides of the vertex
	HVector ba = a.sub(b);
	HVector bc = c.sub(b);

	// Project the sides into the plane tangent at b
	ba.subEq(bb.mul(bb.dot(ba)));
	bc.subEq(bb.mul(bb.dot(bc)));

	// Finally, take the signed angle between them, around b
	double angle = ba.separation(bc, b);

	// "Un-sign" the angle
	return  angle>0 ? angle : angle+2*Math.PI;
     }
    
    /**
     * Compute the angular and linear distances between two world points p1 and p2.
     * This code was lifted from GridLView which puts a distance value
     * in the ruler when mouse is dragged in the panner.
     * @return angular-distance (degrees), linear-distance (km)
     */
    public static double[] angularAndLinearDistanceW(Point2D p1, Point2D p2, MultiProjection proj){
    	p1 = proj.world.toSpatial(p1);
    	p2 = proj.world.toSpatial(p2);
    	return angularAndLinearDistanceS(p1, p2, proj);
    }

    /**
     * Compute the angular and linear distances between two spatial points p1 and p2.
     * This code was lifted from GridLView which puts a distance value
     * in the ruler when mouse is dragged in the panner.
     * @return angular-distance (degrees), linear-distance (km)
     */
    public static double[] angularAndLinearDistanceS(Point2D p1, Point2D p2, MultiProjection proj){
    	double angDistance = proj.spatial.distance(p1, p2);
    	double linDistance = angDistance * 3390.0 * 2*Math.PI / 360.0;
    	
    	return new double[]{ angDistance, linDistance};
    }

    /**
     ** Given an ET, returns an epoch-based approximation to Ls, also
     ** known as "solar longitude" or "heliocentric longitude". The
     ** returned value is in degrees.
     **
     ** <p>This routine uses a hand-fitted curve that was shown to
     ** approximate kernel-based calculations for the years 2000 to
     ** 2050. Over that time period, the error is always within 0.4
     ** degrees. The error is even less during the years 2002 to 2018,
     ** under 0.26 degrees.
     **/
    public static double lsubs(double et)
     {
	final double A = 59340000.0;

	return  (et+46090000)%A * 360.0 / A
	    + Math.sin(et*2*Math.PI/A + .4) * 10.75
	    + 6*Math.sin( -2*LSUBS.scale(et) - .3 ) / 9
	    - Math.pow( Math.sin(LSUBS.scale(et))+1, 4) / 9
	    - 3 * et/2000000000.0
	    - 8.75;
     }

    /**
     ** Internal class, used to test and implement the {@link
     ** Util#lsubs} function.
     **/
    private static class LSUBS
     {
	/**
	 ** Shorthand scaling function for {@link Util#lsubs}.
	 **/
	private static double scale(double et)
	 {
	    final double A = 59340000.0;

	    return  et / A * 2 * Math.PI + 2;
	 }

	/**
	 ** Test driver.
	 **/
	public static void main(String[] av)
	 {
	    double et0 = Double.parseDouble(av[0]);
	    int delta = Integer.parseInt(av[1]);
	    long len = Long.parseLong(av[2]);

	    for(long i=0; i<len; i+=delta)
	     {
		double et = et0 + i;
		System.out.println(et + "\t" + lsubs(et));
	     }
	 }
     }

    /**
     ** Convenience method for URL-encoding a string, a la {@link
     ** URLEncoder}, in compliance with W3C recommendations..
     **/
    public static String urlEncode(String s)
     {
	try
	 {
	    return  URLEncoder.encode(s, "UTF-8");
	 }
	catch(UnsupportedEncodingException e)
	 {
	    // Should never occur, UTF-8 is required to be supported
	    log.aprintln("THIS SHOULDN'T BE HAPPENING!");
	    log.aprintln(e);
	    throw  new Error("The UTF-8 encoding is failing", e);
	 }
     }

    /**
     ** Returns the same color as the given one, but with the alpha
     ** value set to the given value.
     **/
    public static Color alpha(Color col, int alpha)
     {
	return  new Color(col.getRGB()&0xFFFFFF | (alpha<<24), true);
     }

    /**
     ** Convenience method: given a long, returns a hexadecimal string
     ** of its bits. All hex characters are uppercase. Treats the
     ** argument as an unsigned number.
     **/
    public static String toHex(long n)
     {
	return  Long.toHexString(n).toUpperCase();
     }

    /**
     ** Convenience method: given an integer, returns a hexadecimal
     ** string of its bits. All hex characters are uppercase. Treats
     ** the argument as an unsigned number.
     **/
    public static String toHex(int n)
     {
	return  Integer.toHexString(n).toUpperCase();
     }

    /** Returns an array of all lines from the given stream */
    public static String[] readLines(InputStream is) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		List<String> lines = new ArrayList<String>();
		String buff;
		while ((buff = br.readLine()) != null)
			lines.add(buff);
		return lines.toArray(new String[lines.size()]);
	}

    /**
     ** For every non-empty line in the given stream, returns an array
     ** of whitespace-separated tokens of length at most maxCount.
     **/
    public static String[][] readLineTokens(InputStream is, int maxCount)
     throws IOException
     {
	String[] lines = readLines(is);
	List tokens = new ArrayList(lines.length);
	for(int i=0; i<lines.length; i++)
	    if(lines[i].trim().length() != 0)
		tokens.add(lines[i].split("\\s+", maxCount));
	return	(String[][]) tokens.toArray(new String[0][]);
     }

    /**
     ** For every non-empty line in the given stream, returns an array
     ** of whitespace-separated tokens.
     **/
    public static String[][] readLineTokens(InputStream is)
     throws IOException
     {
	return  readLineTokens(is, 0);
     }

    /**
     ** Determines whether or not a string can be parsed as a
     ** base-10 integer.
     **/
    public static boolean isInteger(String s)
     {
	try
	 {
	    Integer.parseInt(s);
	    return  true;
	 }
	catch(NumberFormatException e)
	 {
	    return  false;
	 }
     }

    /**
     ** Determines whether or not a string can be parsed as a
     ** base-10 real.
     **/
    public static boolean isDouble(String s)
     {
	try
	 {
	    Double.parseDouble(s);
	    return  true;
	 }
	catch(NumberFormatException e)
	 {
	    return  false;
	 }
     }

    /**
     ** Java imitation of Perl's <code>split</code> operator. Given a
     ** string containing tokens separated by sequences of
     ** single-character delimiters, returns an array of tokens.
     **
     ** @param delims The characters of this string are taken to be
     ** the delimeters.
     ** @param str The string to split into tokens.
     **/
    public static String[] split(String delims, String str)
     {
	StringTokenizer tok = new StringTokenizer(str, delims);
	String[] tokens = new String[tok.countTokens()];
	for(int i=0; i<tokens.length; i++)
	    tokens[i] = tok.nextToken();
	return	tokens;
     }

    /**
	 * Java version of Perl's <code>join</code> operator. Returns a single
	 * string composed of <code>items</code> separated by
	 * <code>between</code>.
	 */
	public static <E extends Object> String join(String between, E ... items) {
		String joined = "";
		for (int i = 0; i < items.length; i++)
			joined += i == 0 ? items[i] : between + items[i];
		return joined;
	}
    
    public static String join(String between, Collection list) {
    	String joined = "";
    	for(Iterator it=list.iterator(); it.hasNext(); )
    	    joined += (joined.length()==0 ? "" : between) + it.next().toString();
    	return joined;
    }
    
    /**
     * Escape an SQL string. Every instance of single-quote which appears
     * in the passed string is converted into a pair of single-quotes.
     */
    public static String sqlEscape(String s){
    	if (s == null)
    		return null;
    	
    	StringBuffer sbuf = new StringBuffer();
    	for(int i=0; i < s.length(); i++){
    		if (s.charAt(i) == '\'')
    			sbuf.append("''");
    		else
    			sbuf.append(s.charAt(i));
    	}
    	return sbuf.toString();
    }
    
    /**
     * Escapes every SQL string in the given array using {@link #sqlEscape(String)}.
     */
    public static String[] sqlEscape(String s[]){
    	if (s == null)
    		return null;
    	
    	String[] out = new String[s.length];
    	for(int i=0; i < s.length; i++)
    		out[i] = sqlEscape(s[i]);
    	
    	return out;
    }
    
    /**
     * Encloses each individual item of the items array with the specified character.
     * For example: <pre>{abc, def, ghi}</pre> will become <pre>{'abc','def','ghi'}</pre>
     * when enclose is called with {abc,def,ghi} items and the single-quote character.
     */
    public static String[] enclose(String[] items, char c){
    	if (items == null)
    		return null;
    	
    	String[] modified = new String[items.length];
    	
    	for(int i=0; i < modified.length; i++)
    		modified[i] = c+items[i]+c;
    	
    	return modified;
    }

    /**
     ** Performs a {@link JFileChooser#showSaveDialog}, but confirms
     ** with the user if the file exists. If the user cancels at any
     ** stage, the "current selected file" of the file chooser is
     ** restored to its starting value, as if this function never took
     ** place.
     **
     ** @param extension If the file the user chooses doesn't end in
     ** extension, then this string is appended to the filename. Can
     ** be null (in which case there is no effect).
     **
     ** @return Whether or not the user actually settled on a file
     ** (false indicates a cancellation at some step).
     **/
    public static boolean showSaveWithConfirm(JFileChooser fc,
					      Component parent,
					      String extension)
     {
	File old = fc.getSelectedFile();
	File f;
	do
	 {
	    if(fc.showSaveDialog(parent)
	       != JFileChooser.APPROVE_OPTION)
	     {
		fc.setSelectedFile(old);
		return	false;
	     }

	    f = fc.getSelectedFile();
	    if(extension != null)
	     {
		String fname = fc.getSelectedFile().toString();
		if(!fname.endsWith(extension))
		 {
		    fname += extension;
		    fc.setSelectedFile(f = new File(fname));
		 }
	     }
	    if(f.exists())
	     {
		switch(
		    JOptionPane.showConfirmDialog(
			parent,
			"File already exists, overwrite?\n\n" + f + "\n\n",
			"FILE EXISTS",
			JOptionPane.YES_NO_CANCEL_OPTION,
			JOptionPane.WARNING_MESSAGE)
		    )
		 {
		 case JOptionPane.YES_OPTION:
		    break; // Do nothing, we'll exit the while() loop

		 case JOptionPane.NO_OPTION:
		    f = null; // Go for another round in the while() loop
		    break;

		 case JOptionPane.CLOSED_OPTION:
		 case JOptionPane.CANCEL_OPTION:
		 default:
		    fc.setSelectedFile(old);
		    return  false; // Stop the save operation altogether
		 }
	     }
	 }
	while(f == null);

	return	true;
     }

    public static void recursiveRemoveDir(File directory)
     {
	String[] filelist = directory.list();
	if(filelist == null)
	    return;
	for(int i=0; i<filelist.length; i++)
	 {
	    File tmpFile = new File(directory.getAbsolutePath(),filelist[i]);
	    if(tmpFile.isDirectory())
	       recursiveRemoveDir(tmpFile);
	    else
		tmpFile.delete();
	 }
	directory.delete();
     }

    public static String getHomeFilePath(String fname)
     {
	String fpath = "";

	String home = System.getProperty("user.home");
	if ( home != null )
	 {
	    fpath += home;
	    fpath += System.getProperty("file.separator");
	 }

	fpath += fname;

	return fpath;
     }

    /**
     ** The Mars polar radius, in km.
     **/
    public static double MARS_POLAR = 3376.20;
    
    /** The Mars mean radius */
    public static double MARS_MEAN = 3386.0;
    
    /**
     ** The Mars equatorial radius, in km.
     **/
    public static double MARS_EQUAT = 3396.19;

    /**
     ** The flattening coefficient of the Mars ellipsoid.
     **/
    private static double MARS_FLATTENING = 1 - (MARS_POLAR / MARS_EQUAT);

    /**
     ** Convenience constant for conversions.
     **/
    private static double G2C_SCALAR = (1-MARS_FLATTENING)*(1-MARS_FLATTENING);

    /** Converts a latitude value given as an "ographic" coordinate reference
     ** to an "ocentric" one.
     ** 
     ** @param ographic latitude in degrees
     **/
    public static double ographic2ocentric(double ographic)
     {
	return atanD( G2C_SCALAR * tanD(ographic) );
     }

    /** Converts a latitude value given as an "ocentric" coordinate reference
     ** to an "ographic" one.
     ** 
     ** @param ocentric latitude in degrees
     **/
    public static double ocentric2ographic(double ocentric)
     {
	return atanD( tanD(ocentric) / G2C_SCALAR );
     }

    /**
     ** Like {@link Math#tan}, but takes degrees.
     **/
    public static double tanD(double degs)
     {
	return  Math.tan(Math.toRadians(degs));
     }

    /**
     ** Like {@link Math#atan}, but returns degrees.
     **/
    public static double atanD(double x)
     {
	return  Math.toDegrees(Math.atan(x));
     }

    /**
     ** Returns the distance between the sun and mars at a particular
     ** time, in units of AU. Returns zero on error.
     **
     ** <p>This function is a cut+paste translation of stuff from <a
     ** href="http://hem.passagen.se/pausch/comp/ppcomp.html">this
     ** website</a>.
     **/
    public static double getMarsSunDistance(double et)
     {
	try
	 {
	    TimeCache tc = TimeCache.getInstance("ODY");

	    StringTokenizer tok = new StringTokenizer(
		UTC_DF.format(tc.et2date(et)));

	    // Web site, section 3: The time scale

	    int y = Integer.parseInt(tok.nextToken());
	    int m = Integer.parseInt(tok.nextToken());
	    int D = Integer.parseInt(tok.nextToken());
	    double UT = Integer.parseInt(tok.nextToken());	   // hours
	    UT += Integer.parseInt(tok.nextToken()) / 60.0;	   // mins
	    UT += Integer.parseInt(tok.nextToken()) / 60.0 / 60.0; // secs

	    double d = 367*y - 7 * ( y + (m+9)/12 ) / 4 + 275*m/9 + D - 730530;
	    d = d + UT / 24.0;

	    // Web site, section 4: The orbital elements

	    double a = 1.523688; // (AU)
	    double e = 0.093405 + 2.516E-9 * d;
	    double M = Math.toRadians(18.6021 + 0.5240207766 * d);

	    // Web site, section 6: The position of the Moon and of the planets

	    double E0;
	    double E1 = M + e * Math.sin(M) * ( 1.0 + e * Math.cos(M) );
	    do
	     {
		E0 = E1;
		E1 = E0 -
		    ( E0 - e * Math.sin(E0) - M ) / ( 1 - e * Math.cos(E0) );
		log.println("E = " + E0);
		log.println("	 " + E1);
	     }
	    while(Math.abs(E0 - E1) >= Math.toDegrees(0.001));
	    double E = E1;

	    double xv = a * ( Math.cos(E) - e );
	    double yv = a * ( Math.sqrt(1.0 - e*e) * Math.sin(E) );
	    double r = Math.sqrt( xv*xv + yv*yv );

	    // All done!

	    return  r;
	 }
	catch(Throwable e)
	 {
	    log.aprintln(e);
	    log.aprintln("UNABLE TO COMPUTE MARS-SUN DISTANCE AT ET=" + et);
	    return  0;
	 }
     }
    // This differs from TimeCache.UTC_DF
    private static final SimpleDateFormat UTC_DF = new SimpleDateFormat(
	"yyyy MM dd HH mm ss");

    /**
     ** Interface for modifying a single coordinate of a {@link
     ** PathIterator}.
     **/
    private static interface CoordModifier
     {
	/**
	 ** @param coords The coordinate array returned by a shape's
	 ** {@link PathIterator}.
	 ** @param count The number of coordinates in the array (as
	 ** determined by the point type of the {@link PathIterator}.
	 **/
	public void modify(float[] coords, int count);
     }

    /**
     ** Given a shape, iterates over it and performs the given
     ** coordinate modification to every point in the shape.
     **/
    private static Shape modify(Shape s, CoordModifier cm)
     {
	GeneralPath gp = new GeneralPath();
	PathIterator iter = s.getPathIterator(null);
	float[] coords = new float[6];

	// NOTE: No loss of precision in coords. All of the
	// GeneralPath.foobarTo() methods take FLOATS and not doubles.

	while(!iter.isDone())
	 {
	    switch(iter.currentSegment(coords))
	     {

	     case PathIterator.SEG_CLOSE:
		gp.closePath();
		break;

	     case PathIterator.SEG_LINETO:
		cm.modify(coords, 2);
		gp.lineTo(coords[0], coords[1]);
		break;

	     case PathIterator.SEG_MOVETO:
		cm.modify(coords, 2);
		gp.moveTo(coords[0], coords[1]);
		break;

	     case PathIterator.SEG_QUADTO:
		cm.modify(coords, 4);
		gp.quadTo(coords[0], coords[1],
			  coords[2], coords[3]);
		break;

	     case PathIterator.SEG_CUBICTO:
		cm.modify(coords, 6);
		gp.curveTo(coords[0], coords[1],
			   coords[2], coords[3],
			   coords[4], coords[5]);
		break;

	     default:
		log.aprintln("INVALID GENERALPATH SEGMENT TYPE!");

	     }
	    iter.next();
	 }
	return	gp;
     }

    // Quick hack to allow verbatim code-reuse from GraphicsWrapped.java
    private static final double mod = 360;

    /**
     ** Performs the modulo operation on a shape's coordinates.
     **/
    private static final CoordModifier cmModulo =
	new CoordModifier()
	 {
	    public void modify(float[] coords, int count)
	     {
		for(int i=0; i<count; i+=2)
		    coords[i] -= Math.floor(coords[i]/mod)*mod;
	     }
	 };

    /**
     ** Takes care of wrap-around on a shape's coordinates.
     **/
    private static final CoordModifier cmWrapping =
	new CoordModifier()
	 {
	    public void modify(float[] coords, int count)
	     {
		for(int i=0; i<count; i+=2)
		    if(coords[i] < mod/2)
			coords[i] += mod;
	     }
	 };
	 
	 /** Return the input world x value normalized into the 0-360 range */
	 public static final double mod360(double x) {
		 x -= Math.floor(x / 360.0) * 360.0;
		 return x;
	 }
	 
	 /**
	  * Given a lon/lat rectangle, returns a new rectangle with the same lat
	  * values, and the longitude value switched from east to west or west to
	  * east.
	  */
	 public static Rectangle2D swapRect(Rectangle2D rect) {
		 return new Rectangle2D.Double(
			 Util.mod360(-rect.getMaxX()),
			 rect.getMinY(),
			 rect.getWidth(),
			 rect.getHeight());
	 }
	
    /**
     ** ONLY FOR CYLINDRICAL: Given a shape in world coordinates,
     ** "normalizes" it. This ensures that its left-most x coordinate
     ** is within the x-range [0:360], and that there is no
     ** wrap-around (that is, the shape simply pushes past 360).
     **/
    public static Shape normalize360(Shape s)
     {
	double x = s.getBounds2D().getMinX();
	if(x < 0  ||  x >= mod)
	    s = modify(s, cmModulo);

	if(s.getBounds2D().getWidth() >= mod/2)
	    s = modify(s, cmWrapping);

	return	s;
     }

	/**
	 * Normalize the given vertices w.r.t. to the first vertex.
	 * On return all the points will be within 180 degrees of the
	 * first vertex.
	 * <u>This method behaves differently than {@link Util#normalize360(Shape)}</u>
	 * in the following ways:
	 * <b>
	 * <ul>
	 * <li> The starting point of the transformation is the first input point,
	 * not the point with minimum x-value.
	 * <li> Shapes that may be bigger than 180 degrees are not refolded/wrapped-around.
	 * </ul> 
	 * 
	 * @param v Non null array of vertices.
	 * @return Vertices normalized to stay within 180 degress of the first vertex.
	 */
	public static Point2D[] normalize360(Point2D[] v){
		Point2D.Double[] n = new Point2D.Double[v.length];
		
		if (v.length < 1)
			return n;
		
		n[0] = new Point2D.Double(v[0].getX(), v[0].getY());
		double anchor = n[0].x;
		for(int i=1; i<v.length; i++){
			n[i] = new Point2D.Double(v[i].getX(), v[i].getY());
			if (Math.abs(anchor - n[i].x) > 180.0){
				n[i].x += Math.round((anchor - n[i].x) / 360.0) * 360.0;
			}
		}
		return n;
	}

    /**
     ** Given a list of shapes that have been {@link
     ** #normalize360}-ified, and a rectangle, returns a list of the
     ** shapes that intersect that rectangle in modulo-360
     ** coordinates. May return an empty array, but will never return
     ** null.
     **/
    public static int[] intersects360(Rectangle2D rect, Shape[] shapes)
     {
	// Normalize the original rectangle
	Rectangle2D.Double r1 = new Rectangle2D.Double();
	r1.setFrame(rect);
	if(r1.width > 180)
	 {
	    r1.width -= Math.floor(r1.width/360) * 360;
	    r1.width = 360 - r1.width;
	    r1.x -= r1.width;
	 }
	r1.x -= Math.floor(r1.x / 360) * 360;

	// Create the second rectangle, to catch shapes that cross 0/360
	Rectangle2D.Double r2 = (Rectangle2D.Double) r1.clone();
	r2.x += r2.x<180 ? 360 : -360;

	// Find all the intersections
	int count = 0;
	int[] found = new int[shapes.length];
	for(int i=0; i<shapes.length; i++)
	    if(shapes[i].intersects(r1) ||
	       shapes[i].intersects(r2) )
		found[count++] = i;

	// Return the intersections
	int[] found2 = new int[count];
	System.arraycopy(found, 0, found2, 0, count);
	return  found2;
     }

    /**
     ** Given a list of shapes that have been {@link
     ** #normalize360}-ified, and a rectangle, returns a list of the
     ** shapes that intersect that rectangle in modulo-360
     ** coordinates. May return an empty array, but will never return
     ** null.
     **
     ** @param shapes should contain only {@link Shape} objects.
     **/
    public static int[] intersects360(Rectangle2D rect, Collection shapes)
     {
	// Normalize the original rectangle
	Rectangle2D.Double r1 = new Rectangle2D.Double();
	r1.setFrame(rect);
	if(r1.width > 180)
	 {
	    r1.width -= Math.floor(r1.width/360) * 360;
	    r1.width = 360 - r1.width;
	    r1.x -= r1.width;
	 }
	r1.x -= Math.floor(r1.x / 360) * 360;

	// Create the second rectangle, to catch shapes that cross 0/360
	Rectangle2D.Double r2 = (Rectangle2D.Double) r1.clone();
	r2.x += r2.x<180 ? 360 : -360;

	// Find all the intersections
	int count = 0;
	int[] found = new int[shapes.size()];
	int i = 0;
	for(Iterator iter=shapes.iterator(); iter.hasNext(); i++)
	 {
	    Shape sh = (Shape) iter.next();
	    if(sh.intersects(r1) ||
	       sh.intersects(r2) )
		found[count++] = i;
	 }

	// Return the intersections
	int[] found2 = new int[count];
	System.arraycopy(found, 0, found2, 0, count);
	return  found2;
     }

    /**
     ** Returns the floor() of the base-2 logarithm of its argument,
     ** FOR EXACT POWERS OF TWO.
     **/
    public static final int log2(int x)
     {
	return	(int) Math.round(Math.log(x) / Math.log(2));
     }

    /**
     ** Returns the sign of its argument.
     **/
    public static final int sign(double x)
     {
	if(x < 0)
	    return  -1;
	if(x > 0)
	    return  +1;
	return	0;
     }

    /**
     ** Returns the sign of its argument.
     **/
    public static final int sign(int x)
     {
	if(x < 0)
	    return  -1;
	if(x > 0)
	    return  +1;
	return	0;
     }

    /**
     ** Returns whether or not val lies (inclusively) between min and
     ** max.
     **/
    public static final boolean between(int min, int val, int max)
     {
	return	val >= min  &&	val <= max;
     }

    /**
     ** Returns whether or not val lies (inclusively) between min and
     ** max.
     **/
    public static final boolean between(double min, double val, double max)
     {
	return	val >= min  &&	val <= max;
     }

    /**
     ** Handy util method for combination min/max bounding
     ** calls. Equivalent to <code>Math.min(Math.max(value, min),
     ** max)</code>.
     **/
    public static final int bound(int min, int value, int max)
     {
	if(value <= min)
	    return  min;
	if(value >= max)
	    return  max;
	return	value;
     }

    /**
     ** Handy util method for combination min/max bounding
     ** calls. Equivalent to <code>Math.min(Math.max(value, min),
     ** max)</code>. Hasn't been checked for infinity and nan
     ** handling.
     **/
    public static final double bound(double min, double value, double max)
     {
	if(value <= min)
	    return  min;
	if(value >= max)
	    return  max;
	return	value;
     }

    /**
     ** Given an array, returns a duplicate of that array with one
     ** change: an element is inserted at index <code>idx</code>.
     ** Thus the returned array has one more element than the supplied
     ** array.
     **
     ** @param src The array to which an element is added. The
     ** argument is compile-time-typed to "Object" to allow for arrays
     ** of primitives to be passed.
     **/
    public static Object insElement(Object src, int idx)
     {
	int srclen = Array.getLength(src);
	Object dst = Array.newInstance(src.getClass().getComponentType(),
				       srclen + 1);
	System.arraycopy(src, 0,
			 dst, 0,
			 idx);
	System.arraycopy(src, idx,
			 dst, idx+1,
			 srclen-idx);
	return	dst;
     }

    /**
     ** Given an array, returns a duplicate of that array with one
     ** change: the element at index <code>idx</code> is deleted. Thus
     ** the returned array has one less element than the supplied
     ** array.
     **
     ** @param src The array from which to delete an element. The
     ** argument is compile-time-typed to "Object" to allow for arrays
     ** of primitives to be passed.
     **/
    public static Object delElement(Object src, int idx)
     {
	int srclen = Array.getLength(src);
	Object dst = Array.newInstance(src.getClass().getComponentType(),
				       srclen - 1);
	System.arraycopy(src, 0,
			 dst, 0,
			 idx);
	System.arraycopy(src, idx+1,
			 dst, idx,
			 srclen-idx-1);
	return	dst;
     }

    /**
     ** Given an array of primitive-wrapper objects, returns an
     ** equivalent array of primitive elements with the same
     ** values. For instance, supplying an array of {@link Integer}
     ** objects returns an array of <code>int</code> values. If null
     ** is passed in, null is returned.
     **/
    public static Object toPrimitive(Object[] src)
     {
	if(src == null)
	    return  null;

	int len = src.length;
	Class wrapType = src.getClass().getComponentType();

	if(wrapType == Boolean.class)
	 {
	    Boolean[] srcW = (Boolean[]) src;
	    boolean[] dst = new boolean[len];
	    for(int i=0; i<len; i++)
		dst[i] = srcW[i].booleanValue();
	    return  dst;
	 }

	if(wrapType == Character.class)
	 {
	    Character[] srcW = (Character[]) src;
	    char[] dst = new char[len];
	    for(int i=0; i<len; i++)
		dst[i] = srcW[i].charValue();
	    return  dst;
	 }

	if(wrapType == Byte.class)
	 {
	    Byte[] srcW = (Byte[]) src;
	    byte[] dst = new byte[len];
	    for(int i=0; i<len; i++)
		dst[i] = srcW[i].byteValue();
	    return  dst;
	 }

	if(wrapType == Short.class)
	 {
	    Short[] srcW = (Short[]) src;
	    short[] dst = new short[len];
	    for(int i=0; i<len; i++)
		dst[i] = srcW[i].shortValue();
	    return  dst;
	 }

	if(wrapType == Integer.class)
	 {
	    Integer[] srcW = (Integer[]) src;
	    int[] dst = new int[len];
	    for(int i=0; i<len; i++)
		dst[i] = srcW[i].intValue();
	    return  dst;
	 }

	if(wrapType == Long.class)
	 {
	    Long[] srcW = (Long[]) src;
	    long[] dst = new long[len];
	    for(int i=0; i<len; i++)
		dst[i] = srcW[i].longValue();
	    return  dst;
	 }

	if(wrapType == Float.class)
	 {
	    Float[] srcW = (Float[]) src;
	    float[] dst = new float[len];
	    for(int i=0; i<len; i++)
		dst[i] = srcW[i].floatValue();
	    return  dst;
	 }

	if(wrapType == Double.class)
	 {
	    Double[] srcW = (Double[]) src;
	    double[] dst = new double[len];
	    for(int i=0; i<len; i++)
		dst[i] = srcW[i].doubleValue();
	    return  dst;
	 }

	throw  new ClassCastException("Not a primitive-wrapper class: " +
				      wrapType.getName());
     }

    /**
     ** Given an array of primitive elements, returns an equivalent
     ** array of primitive-wrapper objects with the same values. For
     ** instance, supplying an array of <code>int</code> values
     ** returns an array of {@link Integer} objects. If null is passed
     ** in, null is returned.
     **/
    public static Object[] fromPrimitive(Object src)
     {
	if(src == null)
	    return  null;

	Class primType = src.getClass().getComponentType();
	if(primType == null)
	    throw  new ClassCastException("Expected an array of primitives: " +
					  src.getClass());

	if(primType == Boolean.TYPE)
	 {
	    boolean[] srcP = (boolean[]) src;
	    int len = srcP.length;
	    Boolean[] dst = new Boolean[len];
	    for(int i=0; i<len; i++)
		dst[i] = new Boolean(srcP[i]);
	    return  dst;
	 }

	if(primType == Character.TYPE)
	 {
	    char[] srcP = (char[]) src;
	    int len = srcP.length;
	    Character[] dst = new Character[len];
	    for(int i=0; i<len; i++)
		dst[i] = new Character(srcP[i]);
	    return  dst;
	 }

	if(primType == Byte.TYPE)
	 {
	    byte[] srcP = (byte[]) src;
	    int len = srcP.length;
	    Byte[] dst = new Byte[len];
	    for(int i=0; i<len; i++)
		dst[i] = new Byte(srcP[i]);
	    return  dst;
	 }

	if(primType == Short.TYPE)
	 {
	    short[] srcP = (short[]) src;
	    int len = srcP.length;
	    Short[] dst = new Short[len];
	    for(int i=0; i<len; i++)
		dst[i] = new Short(srcP[i]);
	    return  dst;
	 }

	if(primType == Integer.TYPE)
	 {
	    int[] srcP = (int[]) src;
	    int len = srcP.length;
	    Integer[] dst = new Integer[len];
	    for(int i=0; i<len; i++)
		dst[i] = new Integer(srcP[i]);
	    return  dst;
	 }

	if(primType == Long.TYPE)
	 {
	    long[] srcP = (long[]) src;
	    int len = srcP.length;
	    Long[] dst = new Long[len];
	    for(int i=0; i<len; i++)
		dst[i] = new Long(srcP[i]);
	    return  dst;
	 }

	if(primType == Float.TYPE)
	 {
	    float[] srcP = (float[]) src;
	    int len = srcP.length;
	    Float[] dst = new Float[len];
	    for(int i=0; i<len; i++)
		dst[i] = new Float(srcP[i]);
	    return  dst;
	 }

	if(primType == Double.TYPE)
	 {
	    double[] srcP = (double[]) src;
	    int len = srcP.length;
	    Double[] dst = new Double[len];
	    for(int i=0; i<len; i++)
		dst[i] = new Double(srcP[i]);
	    return  dst;
	 }

	throw  new ClassCastException("Not a primitive class: " +
				      primType.getName());
     }

    public static void launchBrowser(String url)
     {
	if(url == null	||  url.equals(""))
	    return;
	try
	 {
	    BrowserLauncher.openURL(url);
	    log.aprintln(url);
	 }
	catch(Throwable e)
	 {
	    log.aprintln("Failed to open url due to " + e);
	    JOptionPane.showMessageDialog(null,
					  "Unable to open your browser!\n"
					  + "Details are on the command-line.",
					  "Browse URL",
					  JOptionPane.ERROR_MESSAGE);
	 }
     }

    /**
     ** Prints on stdout a list of the available GraphicsDevice
     ** "displays", marking the default one.
     **/
    public static void printAltDisplays()
     {
	System.out.println("---- AVAILABLE DISPLAYS ----");
	GraphicsEnvironment ge =
	    GraphicsEnvironment.getLocalGraphicsEnvironment();
	GraphicsDevice def = ge.getDefaultScreenDevice();
	GraphicsDevice[] devices = ge.getScreenDevices();
	for(int i=0; i<devices.length; i++)
	 {
	    System.out.print("\t" + devices[i].getIDstring());
	    if(devices[i] == def)
		System.out.print(" <- default");
	    System.out.println("");
	 }
     }

    /**
     ** The display used for some popups and dialogs (notably
     ** LManager).
     **/
    private static GraphicsDevice altDisplay;

    /**
     ** Sets the display used for some popups and dialogs. If the
     ** <code>id</code> can't be found, the system exits with an
     ** error.
     **/
    public static void setAltDisplay(String id)
     {
	altDisplay = null;
	GraphicsEnvironment ge =
	    GraphicsEnvironment.getLocalGraphicsEnvironment();
	GraphicsDevice[] devices = ge.getScreenDevices();
	for(int i=0; i<devices.length; i++)
	    if(devices[i].getIDstring().equals(id))
		altDisplay = devices[i];
	if(altDisplay == null)
	 {
	    log.aprintln("UNABLE TO SET DISPLAY TO '" + id + "', EXITING!");
	    System.exit(-1);
	 }
	log.aprintln("SET ALTERNATE DISPLAY TO '" + id + "'");
     }

    public static GraphicsConfiguration getAltDisplay()
     {
	GraphicsDevice display = altDisplay;
	if(display == null)
	    display = GraphicsEnvironment
		.getLocalGraphicsEnvironment()
		.getDefaultScreenDevice();
	return	display.getDefaultConfiguration();
     }

    /**
     ** The standard jdbc mapping from SQL data types to java
     ** classes. The keys of the map are all-uppercase sql data type
     ** names, and the values are the corresponding {@link Class}
     ** objects describing the object returned from a {@link ResultSet
     ** ResultSet.getObject()} call on a column of that sql data type.
     **/
    public static final Class jdbc2java(String sqlType)
     {
	Class javaType = (Class) typeMap.get(sqlType);
	if(javaType == null)
	    return  Object.class;
	else
	    return  javaType;
     }
    
    public static final String java2jdbc(Class javaType){
    	return (String)revTypeMap.get(javaType);
    }

    private static HashMap typeMap = new HashMap();
    private static HashMap revTypeMap = new HashMap();
    static
     {
	typeMap.put("CHAR", String.class);
	typeMap.put("VARCHAR", String.class);
	typeMap.put("LONGVARCHAR", String.class);
	typeMap.put("NUMERIC", Double.class);
	typeMap.put("DECIMAL", Double.class);
	typeMap.put("BIT", Boolean.class);
	typeMap.put("TINYINT", Integer.class);
	typeMap.put("SMALLINT", Integer.class);
	typeMap.put("INTEGER", Integer.class);
	typeMap.put("BIGINT", Long.class);
	typeMap.put("REAL", Float.class);
	typeMap.put("FLOAT", Double.class);
	typeMap.put("DOUBLE", Double.class);
	typeMap.put("DOUBLE PRECISION", Double.class);
	typeMap.put("BINARY", byte[].class);
	typeMap.put("VARBINARY", byte[].class);
	typeMap.put("LONGVARBINARY", byte[].class);
	typeMap.put("DATE", java.sql.Date.class);
	typeMap.put("TIME", java.sql.Time.class);
	typeMap.put("TIMESTAMP", java.sql.Timestamp.class);
	typeMap.put("DISTINCT", Object.class); // not terribly precise
	typeMap.put("CLOB", Clob.class);
	typeMap.put("BLOB", Blob.class);
	typeMap.put("ARRAY", java.sql.Array.class);
	typeMap.put("STRUCT", java.sql.Struct.class); // not terribly precise
	typeMap.put("REF", Ref.class);
	typeMap.put("JAVA_OBJECT", Object.class);
	
	revTypeMap.put(String.class,  "VARCHAR");
	revTypeMap.put(Double.class,  "DOUBLE PRECISION");
	revTypeMap.put(Float.class,   "REAL");
	revTypeMap.put(Boolean.class, "BOOLEAN");
	revTypeMap.put(Integer.class, "INTEGER");
	revTypeMap.put(Short.class,   "SHORT");
	revTypeMap.put(Byte.class,    "BYTE");
	revTypeMap.put(byte[].class, "VARBINARY");
	revTypeMap.put(java.sql.Date.class, "DATE");
	revTypeMap.put(java.sql.Time.class, "TIME");
	revTypeMap.put(java.sql.Timestamp.class, "TIMESTAMP");
	revTypeMap.put(Clob.class, "CLOB");
	revTypeMap.put(Blob.class, "BLOB");
	revTypeMap.put(java.sql.Array.class, "ARRAY");
	revTypeMap.put(java.sql.Struct.class, "STRUCT");
	revTypeMap.put(java.sql.Ref.class, "REF");
	revTypeMap.put(Object.class, "JAVA_OBJECT");
	
     }

    /**
     * Loads the JDBC drivers specified in the jmars.config file.
     */
    public static final void loadSqlDrivers() {
    	String[] driverNames = Config.getArray("dbDriver");
    	log.println("Found a total of "+driverNames.length+" JDBC drivers to load.");
    	
    	for(int i=0; i<driverNames.length; i++){
    		log.println("Loading JDBC driver: "+driverNames[i]);
    		try {
    			Class.forName(driverNames[i]);
    			log.println("Loaded JDBC driver: "+driverNames[i]);
    		}
    		catch(Exception ex){
    			log.aprintln(ex.toString());
				JOptionPane.showMessageDialog(null,
						"The SQL driver \""+driverNames[i]+"\" failed to load.",
						"Error loading JDBC driver",
						JOptionPane.ERROR_MESSAGE);
    			throw new Error("Unable to load driver \""+driverNames[i]+"\".", ex);
    		}
    	}
    	log.println("Done loading drivers.");
    }

    private static final Component sComponent = new Component() {};
    private static final MediaTracker sTracker = new MediaTracker(sComponent);
    private static int sID = 0;

    /** You most likely won't care about this function */
    public static final boolean waitForImage(Image image)
     {
	int id;
	synchronized(sComponent) { id = sID++; }
	sTracker.addImage(image, id);
	try
	 {
	    sTracker.waitForID(id);
	 }
	catch(InterruptedException ie)
	 {
	    log.println("Unable to waitForImage:");
//	    log.aprintln(ie);
	    sTracker.removeImage(image, id);
	    return  false;
	 }
	if(sTracker.isErrorID(id))
	 {
	    log.println("Failed waitForImage, id " + id);
	    sTracker.removeImage(image, id);
	    return  false;
	 }
	sTracker.removeImage(image, id);

	return	true;
     }

    /** Loads a given filename into a BufferedImage */
    public static final BufferedImage loadBufferedImage(String path)
     {
	return	makeBufferedImage(blockingLoad(path));
     }

    /** Loads a given url into a BufferedImage */
    public static final BufferedImage loadBufferedImage(URL url)
     {
	return	makeBufferedImage(blockingLoad(url));
     }

    /** Given a filename, returns a fully-loaded image */
    public static final Image blockingLoad(String path)
     {
	Image image = Toolkit.getDefaultToolkit().createImage(path);
	if(waitForImage(image) == false)
	 {
	    log.println("Failed to load image: " + path);
	    return  null;
	 }
	return	image;
     }

    /** Given a url, returns a fully-loaded image */
    public static final Image blockingLoad(URL url)
     {
	Image image = Toolkit.getDefaultToolkit().createImage(url);
	if(waitForImage(image) == false)
	 {
	    log.println("Failed to load image: " + url);
	    return  null;
	 }
	return	image;
     }

    /** Given an image reference, returns a BufferedImage "of it", with
       a default pixel format */
    public static final BufferedImage makeBufferedImage(Image image)
     {
	if(waitForImage(image) == false)
	    return  null;
	BufferedImage bufferedImage = null;

	try 
	{
	    bufferedImage = newBufferedImage(image.getWidth(null), image.getHeight(null));
	}
	catch (NullPointerException e)
	{
	    log.printStack(-1);
	    log.println("We've got a NULL image on our hands!");
	    return(null);
	}

	Graphics2D g2 = bufferedImage.createGraphics();
	g2.drawImage(image, null, null);
	return	bufferedImage;
     }

    /** Given an image reference, returns a BufferedImage "of it", with
       a given pixel format */
    public static final BufferedImage makeBufferedImage(Image image, int imageType)
     {
	if(waitForImage(image) == false)
	    return  null;

	BufferedImage bufferedImage = newBufferedImage(image.getWidth(null),
						       image.getHeight(null));
	Graphics2D g2 = bufferedImage.createGraphics();
	g2.drawImage(image, null, null);
	return	bufferedImage;
     }

    /** Centers a frame on the screen */
    public static final void centerFrame(Frame f)
     {
	Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
	Dimension d = f.getSize();
	int x = (screen.width - d.width) / 2;
	int y = (screen.height - d.height) / 2;
	f.setLocation(x, y);
     }

    /** Returns an affine transform that maps pixel coordinates into world coordinates */
    public static final AffineTransform image2world(int imageWidth, int imageHeight, Rectangle2D worldRect) {
    	AffineTransform at = new AffineTransform();
    	at.translate(worldRect.getX(), worldRect.getY());
    	at.scale(worldRect.getWidth() / imageWidth, worldRect.getHeight() / imageHeight);
    	at.translate(0, imageHeight);
    	at.scale(1, -1);
    	return	at;
    }

    /** Returns an affine transform that maps world coordinates to pixel coordinates */
    public static final AffineTransform world2image(Rectangle2D rect, int imageWidth, int imageHeight) {
    	AffineTransform at = new AffineTransform();
    	at.scale(1, -1);
    	at.translate(0, -imageHeight);
    	at.scale(imageWidth / rect.getWidth(), imageHeight / rect.getHeight());
    	at.translate(-rect.getX(), -rect.getY());
    	return	at;
    }

    /** Returns a new BufferedImage of the given size, with the
     ** default pixel format (as determined by what's most-compatible
     ** with the native screen's preferred pixel format).
     **/
    public static final BufferedImage newBufferedImage(int w, int h)
     {
	if(w == 0  ||  h == 0)
	    log.aprintln("BAD IMAGE SIZE REQUESTED: " + w + "x" + h);
	return
	    GraphicsEnvironment
	    .getLocalGraphicsEnvironment()
	    .getDefaultScreenDevice()
	    .getDefaultConfiguration()
	    .createCompatibleImage(w, h, Transparency.TRANSLUCENT);
     }

    /** Returns a new BufferedImage of the given size, with the
     ** default pixel format (as determined by what's most-compatible
     ** with the native screen's preferred pixel format).
     **/
    public static final BufferedImage newBufferedImageOpaque(int w, int h)
     {
	if(w == 0  ||  h == 0)
	    log.aprintln("BAD IMAGE SIZE REQUESTED: " + w + "x" + h);
	return
	    GraphicsEnvironment
	    .getLocalGraphicsEnvironment()
	    .getDefaultScreenDevice()
	    .getDefaultConfiguration()
	    .createCompatibleImage(w, h, Transparency.OPAQUE);
     }

    /**
     ** Convenience method to perform {@link #urlToDisk1} repeatedly
     ** until it succeeds without error. Attempts the download up to
     ** <code>retryCount</code> times, catching any exceptions. On the
     ** last run, throws whatever exception was generated.
     **/
    public static void urlToDisk(int retryCount,
				 String urlAddress,
				 String filename)
     throws IOException
     {
	while(true)
	    try
	     {
		urlToDisk1(urlAddress, filename);
		return;
	     }
	    catch(IOException	   e) { if(retryCount-- <= 0) throw e; }
	    catch(RuntimeException e) { if(retryCount-- <= 0) throw e; }
	// The RuntimeException handler is to allow for retry on
	// unchecked exceptions (such as NullPointerException).
     }

    /**
     ** Attempts (once) to retrieve all data from the given url and
     ** store it in the given file. Throws an exception if any errors
     ** are encountered. Use {@link #urlToDisk} to automatically retry
     ** a given number of times before giving up.
     **/
    public static void urlToDisk1(String urlAddress, String filename)
     throws IOException
     {
	URL url = null;
	try
	 {
	    url = new URL(urlAddress);
	 }
	catch(MalformedURLException e)
	 {
	    throw  new FileNotFoundException(urlAddress + " (" + e + ")");
	 }
	
	urlToDisk1( url, filename);
     }


    /**
     * 
     * Attempts (once) to retrieve all data from the given url and
     * store it in the given file. Throws an exception if any
     * errors are encountered. Use {@link #urlToDisk} to
     * automatically retry a given number of times before giving
     * up.
     */
     public static void urlToDisk1(URL url, String filename) {
		
	 InputStream in = null;
	 OutputStream out = null;
	 try {
	     in = url.openStream();
	     out = new BufferedOutputStream(new FileOutputStream(filename));
	     
	     byte[] buffer = new byte[40960];
	     int bytes_read;
	     
	     while(( bytes_read = in.read(buffer) ) != -1) {
		 out.write(buffer, 0, bytes_read);
	     }
	     
	     out.close();
	     
	 } catch (IOException e){
	     e.printStackTrace( System.err);
	 } finally {

	    try {  in.close(); } catch(Throwable e) { }
	    try { out.close(); } catch(Throwable e) { }

	     // We've been getting some zero length files...let's end it here
	     File f = new File(filename);
	     if(f.exists()  &&  f.length() == 0) {
		 log.println("Deleting downloaded zero-length file");
		 log.println("(" + filename + ")");
		 f.delete();
	     }
	 }
     }
  


   public static final String getBaseFileName( String fullPath) {

      String ret = fullPath;

      int pos = fullPath.lastIndexOf(System.getProperty("file.separator"));
      if ( pos > -1 )
	ret = fullPath.substring(pos+1);

      return ret;
   }

	/**
	 * Read to the end of the given input stream, closing the stream and
	 * returning the string found. If anything goes wrong, it tries to
	 * close the stream at that point, and then returns as much of the
	 * response as was read.
	 */
	public static String readResponse(InputStream istream) {
		BufferedReader br = new BufferedReader(new InputStreamReader(istream));
		char[] buffer = new char[2048];
		CharArrayWriter ow = new CharArrayWriter();
		try {
			while (true) {
				int count = br.read(buffer, 0, buffer.length);
				if (count < 0) {
					// end of stream reached
					break;
				} else if (count > 0) {
					ow.write(buffer, 0, count);
				} else {
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						// woken up for some reason, just continue on
					}
				}
			}
		} catch (IOException e) {
			log.println(e);
		}
		try {
			br.close();
		} catch (IOException e) {
			log.println(e);
		}
		return ow.toString();
	}
	
    public static class TestSaveAsJpeg
     {
	public static void main(String[] av)
	 throws Throwable
	 {
	    BufferedImage tran = newBufferedImage(5, 5);
	    BufferedImage opaq = newBufferedImageOpaque(5, 5);
	    saveAsJpeg(tran, av[0]);
	    saveAsJpeg(opaq, av[1]);
	 }
     }

    public static final void saveAsJpeg(BufferedImage img, String fname)
     {
	try
	 {
	    javax.imageio.ImageIO.write(img, "jpg", new File(fname));
	 }
	catch(IOException e)
	 {
	    log.println("From " + fname + " " + img);
	    log.println(e);
	    throw new RuntimeException(e.toString(), e);
	 }
     }

    public static String zeroPadInt(long inV, int totalLength)
    {
	StringBuffer result = new StringBuffer(Integer.toString((int)inV));
	if(totalLength > 0)
	    for(; result.length() < totalLength; result.insert(0, "0"));
	return result.toString();
    }

    public static String formatDouble(double inNumber, int totalLength, int inPrecision)
    {
	return formatDouble(inNumber, totalLength, inPrecision, ' ');
    }

    public static String formatDouble(double inNumber, int totalLength, int inPrecision, char padChar)
    {
	NumberFormat formatter = NumberFormat.getInstance();
	formatter.setMinimumFractionDigits(inPrecision);
	formatter.setMaximumFractionDigits(inPrecision);
	formatter.setGroupingUsed(false);
	StringBuffer result = new StringBuffer(formatter.format(inNumber));
	if(totalLength > 0)
	    for(; result.length() < totalLength; result.insert(0, padChar));
	return result.toString();
    }

    public static String formatDouble(double inNumber, int inPrecision)
    {
	NumberFormat formatter = NumberFormat.getInstance();
	formatter.setMinimumFractionDigits(inPrecision);
	formatter.setMaximumFractionDigits(inPrecision);
	formatter.setGroupingUsed(false);
	return formatter.format(inNumber);
    }
    
    /**
     * Formats a spatial Point2D to two decimal places and appends
     * E to the lon value and N to the lat value for display purposes.
     * An example return value would be:  222.12E 77.32N
     * @param spatial
     * @return
     */
	public static String formatSpatial(Point2D spatial)
	 {
		DecimalFormat f = new DecimalFormat("0.00");
		StringBuffer buff = new StringBuffer(20);

		// Format the longitude
		double x  = 360 - spatial.getX(); // JMARS west lon => USER east lon
		double xa = Math.abs(x);
		if(xa < 100)
			buff.append("  ");
		if(xa < 10)
			buff.append("  ");
		buff.append(f.format(x));
		buff.append("E ");

		// Format the latitude
		double y  = spatial.getY();
		double ya = Math.abs(y);
		if(y > 0)
			buff.append("  ");
		if(ya < 10)
			buff.append("  ");
		buff.append(f.format(y));
		buff.append("N");

		return  buff.toString();
	 }

    public static final BufferedImage createGrayscaleImageRot(int dataW,
							      int dataH,
							      byte[] data,
							      int offset,
							      boolean rotate)
     {
	int imgW = rotate ? dataH : dataW;
	int imgH = rotate ? dataW : dataH;

	try
	 {
	    BufferedImage img = newBufferedImage(imgW, imgH);

	    log.println("Turning byte buffer into int buffer");
	    int[] dataAsInt = new int[imgW * imgH];
	    int x=0, y=0; // Declared here to be available during exception
	    int nData=0, nImage=0; // ditto
	    try
	     {
		if(rotate)
		    // Note: x is zero at left, increasing rightward in IMAGE
		    //	     y is zero at top, increasing downward in IMAGE
		    for(x=0; x<imgW; x++)
			for(y=0; y<imgH; y++)
			 {
			    nData = x*imgH + imgH - y - 1 + offset;
			    nImage = y * imgW + x;
			    final int b = data[nData] & 0xFF;
			    dataAsInt[nImage] = new Color(b, b, b).getRGB();
			 }
		else
		    for(x=0; x<imgW*imgH; x++)
		     {
			int b = data[x + offset] & 0xFF;
			dataAsInt[x] = new Color(b, b, b).getRGB();
		     }
	     }
	    catch(ArrayIndexOutOfBoundsException e)
	     {
		log.aprintln("BAD ARRAY INDEX ENCOUNTERED");
		log.aprintln("rotate = " + rotate);
		log.aprintln("x = " + x);
		log.aprintln("y = " + y);
		log.aprintln("imgW = " + imgW);
		log.aprintln("imgH = " + imgH);
		log.aprintln("offset = " + offset);
		log.aprintln("data.length = " + data.length);
		log.aprintln("dataAsInt.length = " + dataAsInt.length);
		log.aprintln("nData = " + nData);
		log.aprintln("nImage = " + nImage);
		throw  e;
	     }

	    log.println("Writing buffer into image");
	    img.setRGB(0, 0,
		       imgW, imgH,
		       dataAsInt,
		       0,
		       imgW);
	    log.println("Done");
	    return  img;
	 }
	catch(Throwable e)
	 {
	    log.aprintln("UNABLE TO CREATE GRAYSCALE IMAGE DUE TO:");
	    log.aprintln(e.toString());
	    log.aprintStack(10);
	    return  null;
	 }
     }

    public static final BufferedImage createGrayscaleImage(int w, int h,
						     byte[] data, int offset)
     {
	if(true)
	    throw  new Error("THIS FUNCTION MIGHT NOT WORK");
	BufferedImage img = newBufferedImage(w, h);

	log.println("Turning byte buffer into int buffer");
	int[] dataAsInt = new int[data.length];
	for(int i=0; i<data.length; i++)
	 {
	    final int b = data[i] & 0xFF;
	    dataAsInt[i] = new Color(b, b, b).getRGB();
	 }

	log.println("Writing buffer into image");
	img.setRGB(0, 0,
		   w, h,
		   dataAsInt,
		   offset,
		   w);
	log.println("Done");
	return	img;
     }


    public static final BufferedImage createGrayscaleImage(int w, int h, boolean linearColorSpace, int transparency, Hashtable properties){
    	ColorSpace destCS = linearColorSpace? getLinearGrayColorSpace(): ColorSpace.getInstance(ColorSpace.CS_GRAY);
    	ColorModel destCM;
    	if (transparency == Transparency.OPAQUE) 
    		destCM = new ComponentColorModel(destCS, false, false, transparency, DataBuffer.TYPE_BYTE);
    	else
    		destCM = new ComponentColorModel(destCS, true, false, transparency, DataBuffer.TYPE_SHORT);
    	
		return new BufferedImage(destCM, destCM.createCompatibleWritableRaster(w, h), destCM.isAlphaPremultiplied(), properties);
    }
    
    /**
     ** @deprecated Don't use, the resulting image is strangely slow.
     **/
    public static final BufferedImage BOOK_createGrayscale(int w, int h,
						     byte[] data, int offset)
     {
	ComponentColorModel ccm =
	    new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY),
				    new int[] { 8 },
				    false,
				    false,
				    Transparency.OPAQUE,
				    DataBuffer.TYPE_BYTE);
	ComponentSampleModel csm =
	    new ComponentSampleModel(DataBuffer.TYPE_BYTE,
				     w, h, 1, w, new int[] { 0 });
	DataBuffer dataBuff = new DataBufferByte(data, w, offset);

	WritableRaster wr = Raster.createWritableRaster(csm,
							dataBuff,
							new Point(0,0));

	BufferedImage rawImage = new BufferedImage(ccm, wr, true, null);

	BufferedImage niceImage = newBufferedImage(w, h);
	log.aprintln("Re-rendering image");
	niceImage.createGraphics().drawImage(rawImage, 0, 0, null);
	log.aprintln("Done.");

	return	niceImage;
     }


    public static final Object loadUserObject(String filename)
    {
	Object oData = null;

	try {

	    if ( filename.length() > 0 )
	    {
		log.println("Loading user preferences from: " + filename);
		ObjectInputStream fin = new ObjectInputStream(new FileInputStream(filename));
		oData =	 fin.readObject();
		fin.close();
	     }

	}
	catch (FileNotFoundException f) {
	    //ignore this - no big deal if not found
	    log.println("File not found - ignored");
	}
	catch (Exception e) {
	     log.println("Error: " + e.getMessage());
	}

	return oData;
    }

    public static final void saveUserObject( String filename, Serializable dataObject)
    {

	try {

	  if ( filename.length() > 0 )
	  {
	      log.println("Saving user preferences to: " + filename);

	      ObjectOutputStream fout = new ObjectOutputStream(new FileOutputStream(filename));
	      fout.writeObject(dataObject);
	      fout.flush();
	      fout.close();
	  }
	}
	catch (Exception e) {
	     System.out.println("Error: " + e.getMessage());
	}
    }

    /**
     ** Given a set of values in the range 0-225, and a set of
     ** corresponding colors, produces a complete 256-color
     ** interpolated color map.
     **
     ** @param scheme The interpolation scheme: -1 linear HSB
     ** decreasing hue, 0 linear HSB shortest hue path, +1 linear HSB
     ** increasing hue, +2 linear HSB direct hue path, +3 linear RGB.
     **/
    public static final Color[] createColorMap(int[] values,
					       Color[] colors,
					       int scheme)
     {
	Color[] colorMap = new Color[256];

	int idx1 = 0;
	Color col0 = colors[0];
	Color col1 = colors[0];
	int val0 = values[0];
	int val1 = values[0];
	for(int i=0; i<256; i++)
	 {
	    if(idx1 < values.length)
		if(values[idx1] == i)
		 {
		    col0 = col1;
		    val0 = val1;
		    ++idx1;
		    if(idx1 < values.length)
		     {
			col1 = colors[idx1];
			val1 = values[idx1];
		     }
		 }
	    if(val0 == val1)
		colorMap[i] = col0;
	    else
	     {
		double mix1 = (i-val0) / (double) (val1-val0);
		if(scheme != +3)
		    colorMap[i] = mixColorHSB(col0, col1, mix1, scheme);
		else
		    colorMap[i] = mixColorRGB(col0, col1, mix1);
	     }
	 }
	return	colorMap;
     }

    /**
     ** Returns a color that is <code>mix1</code> "between" c0 and
     ** c1. If mix1=0 you get c0, mix1=1 you get c1, in between you
     ** get something in between (calculated by linear HSB
     ** interpolation by shortest hue path). Values of mix1 outside
     ** the range [0,1] will return unspecified results.
     **/
    public static final Color mixColor(Color c0, Color c1, double mix1)
     {
	return	mixColorHSB(c0, c1, mix1, 0);
     }

    /**
     ** Returns a color that is <code>mix1</code> "between" c0 and
     ** c1. If mix1=0 you get c0, mix1=1 you get c1, in between you
     ** get something in between (calculated by linear HSB
     ** interpolation). Values of mix1 outside the range [0,1] will
     ** return unspecified results.
     **
     ** @param hueDir Should be one of -1, +1, 0, or +2 to indicate
     ** hue interpolation by descending, ascending, shortest path, or
     ** non-wrapping path method.
     **/
    public static final Color mixColorHSB(Color c0,
					  Color c1,
					  double mix1,
					  int hueDir)
     {
	// Trivial case.
	if(c0.equals(c1)  &&  (hueDir == 0 || hueDir == +2))
	    return  c0;

	// Convenience: mix1 is the amount of c1 and mix0 is the amount of c0
	double mix0 = 1 - mix1;

	// Get the HSB values of each color
	float hsb0[] = Color.RGBtoHSB(c0.getRed(),
				      c0.getGreen(),
				      c0.getBlue(),
				      null);
	float hsb1[] = Color.RGBtoHSB(c1.getRed(),
				      c1.getGreen(),
				      c1.getBlue(),
				      null);
	double h0 = hsb0[0];
	double h1 = hsb1[0];

	// If we have a grayscale, its hue is actually
	// meaningless... so to prevent its "random" value from
	// causing trouble, we fix the grayscale's hue to the color's
	// hue for smooth fading. We also later use partGray to
	// prevent grayscales from doing the "red to red" thing and
	// spanning the whole spectrum.
	boolean partGray = false;
	if(hsb0[1] == 0.0)
	 {
	    partGray = true;
	    h0 = h1;
	 }
	else if(hsb1[1] == 0.0)
	 {
	    partGray = true;
	    h1 = h0;
	 }

	// Our derived color is a simple linear combination of the base colors
	double h; // determined below by hueDir
	double s = mix0*hsb0[1] + mix1*hsb1[1];
	double b = mix0*hsb0[2] + mix1*hsb1[2];

	switch(hueDir)
	 {

	 case 2: // Non-wrapping path method
	    h = mix0*h0 + mix1*h1;
	    break;
	    
	 case 0: // Shortest path method
	    h = mix0*h0 + mix1*h1;
	    if(Math.abs(h0 - h1) > 0.5)
	     {
		// Fix for hues separated by > 180 degrees
		h = 1 - (Math.max(h0,h1)-Math.min(h0,h1));
		h *= (h1 > h0 ? mix0 : mix1);
		h += Math.max(h0,h1);
		if(h > 1.0)
		    h -= 1.0;
	     }
	    break;

	 case +1: // Enforce increasing hue
	    double distance = (1 + h1 - h0) % 1;
	    if(distance == 0  &&  !partGray)
		distance = 1;
	    h = 1 + h0 + mix1 * distance;
	    h %= 1;
	    break;

	 case -1: // Enforce decreasing hue
	    distance = (1 + h0 - h1) % 1;
	    if(distance == 0  &&  !partGray)
		distance = 1;
	    h = 1 + h0 - mix1 * distance;
	    h %= 1;
	    break;

	 default:
	    h = 0;
	    log.aprintln("INVALID HUE DIRECTION: " + hueDir);
	 }

	return	Color.getHSBColor((float) h,
				  (float) s,
				  (float) b);
     }

    /**
     ** Returns a color that is <code>mix1</code> "between" c0 and
     ** c1. If mix1=0 you get c0, mix1=1 you get c1, in between you
     ** get something in between (calculated by linear RGB
     ** interpolation). Values of mix1 outside the range [0,1] will
     ** return unspecified results.
     **/
    public static final Color mixColorRGB(Color c0,
					  Color c1,
					  double mix1)
     {
	// Convenience: mix1 is the amount of c1 and mix0 is the amount of c0
	double mix0 = 1 - mix1;

	int r = (int) Math.round( mix0*c0.getRed()   + mix1*c1.getRed()	  );
	int g = (int) Math.round( mix0*c0.getGreen() + mix1*c1.getGreen() );
	int b = (int) Math.round( mix0*c0.getBlue()  + mix1*c1.getBlue()  );

	return	new Color(r, g, b);
     }

    /**
     ** Returns the "B" component of the HSB representation of a Color.
     **/
    public static final float getB(Color c)
     {
	return	Color.RGBtoHSB(
	    c.getRed(),
	    c.getGreen(),
	    c.getBlue(),
	    null
	    )[2];
     }

    /**
     ** Returns <code>num</code> rounded to the nearest multiple of
     ** <code>mul</code>.
     **/
    public static final double roundToMultiple(double num, double mul)
     {
	return	Math.round(num / mul) * mul;
     }

    /**
     ** Returns <code>num</code> rounded to the nearest multiple of
     ** <code>mul</code>.
     **/
    public static final long roundToMultiple(double num, long mul)
     {
	return	Math.round(num / mul) * mul;
     }

    /**
     ** Returns <code>num</code> rounded to the nearest multiple of
     ** <code>mul</code>.
     **/
    public static final int roundToMultiple(int num, int mul)
     {
	return	Math.round(num / mul) * mul;
     }

    /**
     ** Returns string with newlines inserted at the specified
     ** character intervals or earlier, i.e., wraps at word
     ** boundaries.
     **/
    public static final String lineWrap(String text, int interval)
    {
        String wrapped = null;
        
        if (text != null)
        {
            StringBuffer buf = new StringBuffer(text);
            
            for (int i=interval; i < buf.length(); i+=interval)
            {
                // Scan this interval from back to front for
                // a newline; if found start next interval scan
                // here.
                boolean nextInterval = false;
                for (int j=i; j > i - interval; j--)
                    if (buf.charAt(j) == '\n')
                    {
                        i = j;
                        nextInterval = true;
                        break;
                    }
                    
                if (!nextInterval)
                    // Scan interval from back to front for
                    // first whitespace character and replace it
                    // with newline.  If front of interval is reached
                    // without finding whitespace, put insert newline at
                    // end of interval.
                    for (int j=i; j >= i - interval; j--) {
                        if (j == i - interval)
                            buf.insert(i, '\n');
                        else if (Character.isWhitespace(buf.charAt(j))) {
                            buf.setCharAt(j, '\n');
                            i = j;
                            break;
                        }
                    }
            }
            
            wrapped = buf.toString();
        }
        
        return wrapped;
    }

     /**
      ** Sorts one list relative to the order that objects appear in
      ** another list.
      **
      ** @param sortList list of objects to be sorted; all objects
      ** in this list must "appear" in the <code>orderList</code>. An
      ** object appears if it matches an object with the equals()
      ** method test.  If an object does not appear in the second
      ** list, an {@link IllegalArgumentException} is thrown.
      **
      ** @param orderList list of objects that represent the relative
      ** object sorting order for the <code>sortList</code>.
      **
      ** @see List#indexOf
      **/
    public static final void relativeSort(final List sortList, final List orderList)
     throws IllegalArgumentException
     {
	Collections.sort(sortList,
			 new Comparator()
			  {
			     public int compare(Object o1, Object o2)
			      {
				 int index1 = orderList.indexOf(o1);
				 int index2 = orderList.indexOf(o2);
		      
				 if (index1 < 0 ||
				     index2 < 0)
				     throw new IllegalArgumentException(
					 "object not found in list");

				 return index1 - index2;
			      }
		  
			     public boolean equals(Object obj) 
			      {
				 return super.equals(obj);
			      }
			  }
	    );
     }
    
    /**
     * Resizes the top-level ancestor of the given component "fp" such
     * that it is not smaller than the preferred size of "fp".
     * If the component "c" is null or it does not have a top level
     * ancestor that can be resized, nothing happens.
     */
    public static final void resizeTopLevelContainerToPreferredSize(JComponent fp){
    	if (fp != null && fp.getTopLevelAncestor() != null){
    		Dimension p = fp.getPreferredSize();
    		Dimension c = fp.getTopLevelAncestor().getSize();
    		Dimension r = new Dimension(
    				(int)Math.max(p.getWidth(),c.getWidth()),
    				(int)Math.max(p.getHeight(),c.getHeight()));
    		fp.getTopLevelAncestor().setSize(r);
    		fp.getTopLevelAncestor().doLayout();
    		fp.getTopLevelAncestor().repaint();
    	}
    }

	/**
	 * Calls binRanges(int[]) with the portion of 'indices' from index 0 to
	 * length-1 inclusive.
	 */
	public static int[][] binRanges (int[] indices, int length) {
		int[] array = new int[length];
		System.arraycopy(indices, 0, array, 0, length);
		return binRanges(array);
	}

	/**
	 * Returns an array of disjoint ranges in the given array of indices.
	 * @param indices Left in sorted order, whether it was before or not.
	 * @return Array of int[2] arrays, where the contained array elements
	 * are the start/end indices.
	 */
	public static int[][] binRanges (int[] indices) {
		Arrays.sort (indices);
		java.util.List ranges = new LinkedList ();
		if (indices != null && indices.length > 0) {
			int[] range = new int[2];
			range[0] = indices[0];
			for (int i = 1; i < indices.length; i++) {
				if (indices[i] - indices[i-1] != 1) {
					range[1] = indices[i-1];
					ranges.add (range.clone());
					range[0] = indices[i];
				}
			}
			range[1] = indices[indices.length-1];
			ranges.add (range);
		}
		return (int[][]) ranges.toArray (new int[0][]);
	}

    /**
     * Fold text so that the text (excluding delimiter) fits in the
     * specified width. This implementation is very simplistic and
     * is quite extravagent in its memory usage. Thus it is inadvisable
     * to use it for folding large chunks of texts. The text is folded
     * at white-spaces and multiple white-spaces are coalesced.
     * <b>CAUTION:</b>The code does not check for invalid values of
     * width or null inputs.
     * 
     * @param text The text to be folded.
     * @param width The width (exclusive of delimiter) to fold to.
     * @param foldDelim String to use as a fold marker.
     * @return Input text folded to the specified length.
     */
    // 
    public static final String foldText(String text, int width, String foldDelim){
    	if (text == null){ return null; }

    	String[] words = text.split("\\s+");
    	String   line = "";
    	StringBuffer folded = new StringBuffer();
    	int      i = 0;

    	while(i < words.length){
    		// If the new word will make the line longer than user requested width
    		if ((line + " " + words[i]).length() > width){
    			// then wait for it to come around in the next pass, if it itself
    			// is less than the user requested width
    			if (words[i].length() < width){
    				folded.append(foldDelim);
    				line = "";
    			}
    			// otherwise 
    			else {
    				if (!line.equals("")){ folded.append(" "); }
    				String s = words[i];
    				while(s.length() >= width){
    					// hyphenate
    					folded.append(s.substring(0,width-1));
    					folded.append("-");
    					folded.append(foldDelim);
    					s = s.substring(width);
    				}
    				folded.append(s);
    				line = s;
    				i++;
    			}
    		}
    		else {
    			if (!line.equals("")){
    				folded.append(" ");
    				line += " ";
    			}
    			folded.append(words[i]);
    			line += words[i];
    			i++;
    		}
    	}

    	return folded.toString();
    }
    
    /** Sets the size of the frame to the larger of its current and preferred size */
    public static void expandSize(Frame top) {
    	if (top == null)
    		return;
		Dimension oldSize = top.getSize();
		Dimension newSize = top.getPreferredSize();
		top.setSize(new Dimension(Math.max(oldSize.width, newSize.width),
			Math.max(oldSize.height, newSize.height)));
		top.validate();
		top.repaint();
	}
	
	public static Component getNearest (Component c, Class type) {
		while (c != null && ! type.isInstance(c))
			c = c.getParent();
		return c;
	}
	
	/**
	 * Read and return all bytes from the specified input stream until an
	 * end of stream is encountered.
	 * @throws IOException
	 */
	public static byte[] getAllBytes(InputStream is) throws IOException {
		InputStream st = is;
		if (!(is instanceof BufferedInputStream))
			st = new BufferedInputStream(is);
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] data = new byte[10000];
		int status = 0;
		do {
			out.write(data, 0, status);
			status = st.read(data, 0, data.length);
		} while (status >= 0);
		return out.toByteArray();
	}
	
	public static Point2D interpolate(Line2D line, double t){
		return new Point2D.Double(
				line.getX1()+t*(line.getX2()-line.getX1()),
				line.getY1()+t*(line.getY2()-line.getY1()));
	}
	
	public static Point2D interpolate(Point2D p1, Point2D p2, double t){
		return new Point2D.Double(
				p1.getX()+t*(p2.getX()-p1.getX()),
				p1.getY()+t*(p2.getY()-p1.getY()));
	}

	public static double uninterploate(Line2D line, Point2D pt){
		HVector p = new HVector(pt.getX(), pt.getY(), 0);
		HVector p1 = new HVector(line.getX1(), line.getY1(), 0);
		HVector p2 = new HVector(line.getX2(), line.getY2(), 0);
		double t = HVector.uninterpolate(p1, p2, p);
		return t;
	}
	
	/**
	 * DDA line drawing algorithm lifted from Computer Graphics by Hearn & Baker.
	 * @return An array of Points on the line.
	 */
	public static Point[] dda(int x0, int y0, int x1, int y1){
		int dx = x1-x0;
		int dy = y1-y0;
		int steps = 1 + (Math.abs(dx) > Math.abs(dy) ? Math.abs(dx): Math.abs(dy));
		double xinc = ((double)dx)/steps;
		double yinc = ((double)dy)/steps;

		Point[] points = new Point[steps];
		double x = x0, y = y0;
		for(int k = 0; k < steps; k ++) {
			points[k] = new Point((int)Math.round(x), (int)Math.round(y));
			x += xinc;
			y += yinc;
		}

		return points;
	}
	
	/**
	 * Return an empty image of the given width and height that will be
	 * compatible with the given image
	 */
	public static BufferedImage createCompatibleImage(BufferedImage image, int w, int h) {
		ColorModel cm = image.getColorModel();
		WritableRaster r = image.getRaster().createCompatibleWritableRaster(w, h);
		return new BufferedImage(cm, r, image.isAlphaPremultiplied(), null);
	}
	
	/**
	 * Returns the wrapped world rectangles covered by this unwrapped world
	 * rectangle. There can not be more than two rectangles in the output.
	 */
	public static Rectangle2D[] toWrappedWorld(Rectangle2D unwrappedWorld) {
		double y = unwrappedWorld.getY();
		double h = unwrappedWorld.getHeight();
		if (unwrappedWorld.getWidth() > 360.0)
			return new Rectangle2D[] {
				new Rectangle2D.Double(0, y, 360, h)
			};
		
		double minx = unwrappedWorld.getMinX();
		double maxx = unwrappedWorld.getMaxX();
		double xoffset = 0;
		if (minx < 0) {
			xoffset = Math.ceil(-minx/360.0)*360.0;
		} else if (minx > 360) {
			xoffset = Math.floor(minx/360.0)*-360.0;
		}
		minx += xoffset;
		maxx += xoffset;
		
		if (maxx <= 360.0)
			return new Rectangle2D[] {
				new Rectangle2D.Double(minx, y, maxx-minx, h)
			};
		else
			return new Rectangle2D[] {
				new Rectangle2D.Double(0, y, maxx-360, h),
				new Rectangle2D.Double(minx, y, 360-minx, h)
			};
	}
	
	/**
	 * Returns all occurrences of wrappedImage in the specified unwrapped
	 * domain. The results will intersect unwrappedDomain but may not be
	 * completely contained.
	 */
	public static Rectangle2D[] toUnwrappedWorld(Rectangle2D wrappedImage, Rectangle2D unwrappedDomain) {
		List matches = new LinkedList();
		Rectangle2D query = new Rectangle2D.Double();
		double start = Math.floor(unwrappedDomain.getMinX() / 360.0) * 360.0 + wrappedImage.getMinX();
		double y = wrappedImage.getY();
		double w = wrappedImage.getWidth();
		double h = wrappedImage.getHeight();
		for (double x = start; x < unwrappedDomain.getMaxX(); x += 360.0) {
			query.setFrame(x, y, w, h);
			if (unwrappedDomain.intersects(query)) {
				matches.add((Rectangle2D)query.clone());
			}
		}
		final Rectangle2D[] type = new Rectangle2D[0];
		return (Rectangle2D[])matches.toArray(type);
	}
	
	/**
	 * Get the lon/lat of the up vector to send to a MapServer using the WMS
	 * JMARS:1 projection, based on the given oblique cylindrical projection.
	 * @param poc The projection from which to derive the up vector.
	 * @param out The point to put the result into. If null, a new point is created.
	 * @return The result point; will be equal to <code>out</code> if it was not null.
	 */
	public static final Point2D getJmars1Up(Projection_OC poc, Point2D out) {
		if (out == null)
			out = new Point2D.Double();
		double upLat = poc.getUpLat();
		double upLon = poc.getUpLon();
		double centerLat = poc.getCenterLat();
		if (centerLat<=0) {
			upLon+=180;
		} 
		out.setLocation(upLon, upLat);
		return out;
	}
	
	/**
	 * Converts the X coordinate of the JMARS world coordinate system to an equivalent X
	 * coordinate in the WMS JMARS:1 coordinate system.
	 * The two systems have the same X values for points in the northern hemisphere, but
	 * they are 180 degrees away from each other in the southern hemisphere.
	 * @param poc The projection from which to determine the side of the planet we're on.
	 * @param in The starting longitude
	 * @return The input longitude + 180 degrees if the projection's center latitude is
	 * above 0, the input longitude otherwise.
	 */
	public static final double worldXToJmars1X(Projection_OC poc, double in) {
		return poc.getCenterLat() <= 0 ? in - 180.0 : in;
	}
	
	/**
	 * Returns the angular distance from p1 to p2 using pure spherical trig.
	 * This method takes about 50% longer to run than
	 * {@link HVector#separation(HVector)}, but that uses the arcsin formula
	 * and this uses a formula that is stable in all cases.
	 * 
	 * @param p1 [east-longitude in degrees,geocentric-lat in degrees] point
	 * @param p2 [east-longitude in degrees,geocentric-lat in degrees] point
	 * @return The angular distance along the unit sphere from p1 to p2, in radians.
	 */
	public static double separation(Point2D p1, Point2D p2) {
		double latRad2 = Math.toRadians(p2.getY());
		double cosLat2 = Math.cos(latRad2);
		double sinLat2 = Math.sin(latRad2);
		double latRad1 = Math.toRadians(p1.getY());
		double cosLat1 = Math.cos(latRad1);
		double sinLat1 = Math.sin(latRad1);
		double deltaLon = Math.toRadians(p2.getX() - p1.getX());
		double cosDeLon = Math.cos(deltaLon);
		double sinDeLon = Math.sin(deltaLon);
		double a = cosLat2 * sinDeLon;
		double b = cosLat1 * sinLat2 - sinLat1 * cosLat2 * cosDeLon;
		double c = sinLat1 * sinLat2 + cosLat1 * cosLat2 * cosDeLon;
		return Math.atan2(Math.hypot(a, b), c);
	}
	
	public static ImageIcon loadIcon(String name) {
		return new ImageIcon(loadImage(name));
	}
	
	public static BufferedImage loadImage(String name) {
		try {
			return ImageIO.read(Main.getResource(name));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/** Returns a writable raster for band 'band' in the given image */
	public static WritableRaster getBands(BufferedImage img, int ... bands) {
		return img.getRaster().createWritableChild(0, 0, img.getWidth(), img.getHeight(), 0, 0, bands);
	}
	
	/** Returns the portion of the raster without the alpha band (could be all of it) */
	public static WritableRaster getColorRaster(BufferedImage bi) {
		ColorModel cm = bi.getColorModel();
		WritableRaster raster = bi.getRaster();
		if (cm.hasAlpha() == false) {
			return raster;
		}
		int[] bands = new int[cm.getNumColorComponents()];
		for (int i = 0; i < bands.length; i++) {
			bands[i] = i;
		}
		int x = raster.getMinX();
		int y = raster.getMinY();
		return raster.createWritableChild(x, y, raster.getWidth(), raster.getHeight(), x, y, bands);
	}
	
	/**
	 * Our GrayCS is a linear Gray ColorSpace. Java's GrayCS is non-linear.
	 */
	private static ColorSpace linearGrayColorSpace = null;
	public static ColorSpace getLinearGrayColorSpace(){
		if (linearGrayColorSpace != null)
			return linearGrayColorSpace;
		
		try {
			ICC_Profile myGrayCP = ICC_ProfileGray.getInstance(Main.getResourceAsStream("resources/sGray.icc"));
			linearGrayColorSpace = new ICC_ColorSpace(myGrayCP);
		}
		catch(IOException ex){
			ex.printStackTrace();
		}
		
		return linearGrayColorSpace;
	}
	
	public static BufferedImage replaceWithLinearGrayCS(BufferedImage in){
		// Pass nulls back as is.
		if (in == null)
			return null;
		
		// Return non-GrayCS images back as is.
		if (in.getColorModel().getColorSpace().getType() != ColorSpace.TYPE_GRAY)
			return in;

		
		// Replace the BufferedImage's ColorSpace with a linear Grayscale ColorSpace.
		ColorModel cm = new ComponentColorModel(getLinearGrayColorSpace(), in.getColorModel().hasAlpha(), in.isAlphaPremultiplied(), in.getTransparency(), in.getSampleModel().getTransferType());
		BufferedImage out = new BufferedImage(cm, in.getRaster(), in.isAlphaPremultiplied(), null);
		return out;
	}
	
	/** Returns the latin1 encoded string made from an apache-style hash of the given username and password */
	public static String apachePassHash(String user, String password) {
		String token = user + password;
		byte[] coded = Base64.encodeBase64(DigestUtils.sha(token));
		return Charset.forName("ISO-8859-1").decode(ByteBuffer.wrap(coded)).toString().replaceAll("=+$", "");
	}
	
	/** Returns a MySQL 3.5-4.00 era password hash of the given plaintext password */
	public static String mysqlPassHash(String password) {
		int nr = 0x50305735;
		int nr2 = 0x12345671;
		int add = 7;
		for (char ch : password.toCharArray()) {
			if (ch == ' ' || ch == '\t')
				continue;
			int charVal = ch;
			nr ^= (((nr & 63) + add) * charVal) + (nr << 8);
			nr &= 0x7fffffff;
			nr2 += (nr2 << 8) ^ nr;
			nr2 &= 0x7fffffff;
			add += charVal;
		}
		return String.format("%08x%08x", nr, nr2);
	}
	
	/**
	 * Wrapper around basic HttpClient execution of PostMethod to handle
	 * redirects.
	 * 
	 * @return The HTTP response code from the last URI contacted.
	 * @throws IOException Thrown if an IO error occurs
	 * @throws NullPointerException Thrown if an empty location header is found
	 * @throws HttpException Thrown if another kind of HTTP error occurs
	 * @throws URIException Thrown if an invalid URI is used
	 */
	public static int postWithRedirect(HttpClient client, PostMethod post, int maxRedirects)
			throws URIException, HttpException, NullPointerException, IOException {
		int code = -1;
		for (int tries = 0; tries < maxRedirects; tries++) {
			code = client.executeMethod(post);
			switch (code) {
			case 301: // moved permanently
			case 302: // moved temporarily
			case 307: // temporary redirect
				Header loc = post.getResponseHeader("location");
				if (loc != null) {
					post.setURI(new URI(loc.getValue(), false));
				} else {
					return code;
				}
				break;
			case 200:
			default:
				return code;
			}
		}
		return code;
	}
	
	/**
	 * Retrieves the given remote file, caches it in cachePath. Subsequent
	 * calls return the cached copy. The cached copy is brought up-to-date
	 * with respect to the remoteUrl before being returned if updateCheck
	 * is <code>true</code>.
	 * @param remoteUrl URL of the source file
	 * @param updateCheck Whether to check for updates or not. This is only
	 *        applicable if the file exists already. If not, an update is automatically
	 *        performed.
	 * @return <code>null</code> in case of an error, or the {@link File} in case
	 *         of success.
	 */
	public static File getCachedFile(String remoteUrl, boolean updateCheck) {
		String cachePath = Main.getJMarsPath()+"localcache"+File.separator;
		try {
			URL url = new URL(remoteUrl);
			
			File localFile = new File(cachePath + url.getFile().replaceAll("[^a-zA-Z0-9]", "_"));
			if (!updateCheck){
				if (localFile.exists()){
					log.println("No update check requested, returning existing file.");
					return localFile;
				}
				else {
					log.println("No update check requested, but the file does not exist. Forcing update.");
				}
			}
			
			URLConnection conn = url.openConnection();
			conn.setRequestProperty("User-Agent", "Java");
			if (!(localFile.exists() && localFile.lastModified() == conn.getLastModified())){
				if (localFile.exists())
					log.println("File from "+remoteUrl+" is out of date ("+(new Date(localFile.lastModified()))+" vs "+(new Date(conn.getLastModified()))+").");
				else
					log.println("File from "+remoteUrl+" is not cached locally.");
				
				new File(cachePath).mkdirs();
				InputStream is = conn.getInputStream();
				OutputStream os = new BufferedOutputStream(new FileOutputStream(localFile));
				byte[] buff = new byte[1024];
				int nread;
				while((nread = is.read(buff)) > -1)
					os.write(buff, 0, nread);
				os.close();
				if (conn.getLastModified() != 0)
					localFile.setLastModified(conn.getLastModified());
				
				log.println("Downloaded file from " + remoteUrl+ " modification date: "+(new Date(conn.getLastModified())));
			}
			else {
				log.println("Using cached copy for "+remoteUrl+".");
			}
			return localFile;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static void addEscapeAction(final JDialog dlg) {
		KeyStroke esc = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
		dlg.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(esc, "ESCAPE");
		dlg.getRootPane().getActionMap().put("ESCAPE", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				dlg.setVisible(false);
			}
		});
	}
	
	public static void copy(InputStream is, OutputStream os) throws IOException {
		byte[] buffer = new byte[2048];
		int count = 0;
		while (0 <= (count = is.read(buffer))) {
			if (count == 0) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else {
				os.write(buffer, 0, count);
			}
		}
	}
}
