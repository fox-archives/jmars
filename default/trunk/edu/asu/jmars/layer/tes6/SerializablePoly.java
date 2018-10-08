package edu.asu.jmars.layer.tes6;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Vector;

/**
 * Serializable implementation of a polygon. This implementation
 * is backed by a GeneralPath and implements Shape, thus it can
 * be drawn etc.
 * 
 * @author saadat
 *
 */
public class SerializablePoly implements Serializable, Shape {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public SerializablePoly() {
		super();
		backingPath = allocBackingPath();
	}

	public SerializablePoly(GeneralPath poly){
		super();
		backingPath = allocBackingPath();
		if (poly != null){
			backingPath.reset();
			backingPath.append(poly, false);
		}
	}
	
	public SerializablePoly(Point2D[] poly){
		super();
		backingPath = allocBackingPath();
		
		if (poly != null){
			if (poly.length > 0){
				backingPath.moveTo((float)poly[0].getX(), (float)poly[0].getY());
			}
			for(int i = 1; i < poly.length; i++){
				backingPath.lineTo((float)poly[i].getX(), (float)poly[i].getY());
			}
			if (poly.length > 0){
				backingPath.closePath();
			}
		}
	}
	
	public SerializablePoly(Point2D[][] polys){
		super();
		backingPath = allocBackingPath();
		
		if (polys != null){
			for(int j = 0; j < polys.length; j++){
				if (polys[j].length > 0){
					backingPath.moveTo((float)polys[j][0].getX(), (float)polys[j][0].getY());
				}
				for(int i = 1; i < polys[j].length; i++){
					backingPath.lineTo((float)polys[j][i].getX(), (float)polys[j][i].getY());
				}
				if (polys[j].length > 0){
					backingPath.closePath();
				}
			}
		}
	}
	
	private GeneralPath allocBackingPath(){
		return new GeneralPath(GeneralPath.WIND_EVEN_ODD, 10);
	}
	
	public boolean contains(double x, double y) {
		return backingPath.contains(x, y);
	}

	public boolean contains(double x, double y, double w, double h) {
		return backingPath.contains(x, y, w, h);
	}

	public boolean intersects(double x, double y, double w, double h) {
		return backingPath.intersects(x, y, w, h);
	}

	public Rectangle getBounds() {
		return backingPath.getBounds();
	}

	public boolean contains(Point2D p) {
		return backingPath.contains(p);
	}

	public Rectangle2D getBounds2D() {
		return backingPath.getBounds2D();
	}

	public boolean contains(Rectangle2D r) {
		return backingPath.contains(r);
	}

	public boolean intersects(Rectangle2D r) {
		return backingPath.intersects(r);
	}

	public PathIterator getPathIterator(AffineTransform at) {
		return backingPath.getPathIterator(at);
	}

	public PathIterator getPathIterator(AffineTransform at, double flatness) {
		return backingPath.getPathIterator(at, flatness);
	}

	public void transform(AffineTransform at){
		backingPath.transform(at);
	}
	
	public GeneralPath getBackingPoly(){
		return backingPath;
	}
	
	public Object clone(){
		SerializablePoly copy = null;
		try {
			copy = (SerializablePoly)super.clone();
		}
		catch(CloneNotSupportedException ex){
			copy = new SerializablePoly();
		}
		copy.backingPath = (GeneralPath)backingPath.clone();
		
		return copy;
	}
	
	public Object writeReplace() throws ObjectStreamException {
		return new StoredSerializedPoly(this);
	}
	
	public String toString(){
		StringBuffer sbuf = new StringBuffer();
		float[] pt = new float[6];
		int segType, windRule, nPts;
		String marker;
		
		sbuf.append(getClass().getName()+"[");
		
		PathIterator pi = backingPath.getPathIterator(null);
		windRule = pi.getWindingRule();
		sbuf.append((windRule == PathIterator.WIND_EVEN_ODD)?"WEO":"WNZ");
		sbuf.append(":");
		
		while(!pi.isDone()){
			segType = pi.currentSegment(pt);
			switch(segType){
				case PathIterator.SEG_MOVETO:  marker="M"; nPts = 2; break;
				case PathIterator.SEG_LINETO:  marker="L"; nPts = 2; break;
				case PathIterator.SEG_QUADTO:  marker="Q"; nPts = 4; break;
				case PathIterator.SEG_CUBICTO: marker="C"; nPts = 6; break;
				case PathIterator.SEG_CLOSE:   marker="X"; nPts = 0; break;
				default: marker="?"; nPts = 0; break;
			}
			
			sbuf.append(marker);
			if (nPts > 0){ sbuf.append("("); }
			for(int i = 0; i < nPts; i++){
				sbuf.append(pt[i]);
				if (i < (nPts-1)){ sbuf.append(","); }
			}
			if (nPts > 0){ sbuf.append(")"); }
			
			pi.next();
		}
		sbuf.append("]");
		
		return sbuf.toString();
	}
	
	
	private GeneralPath backingPath;
	
	
	/**
	 * The serialized version of poly {@ref SerializedPoly} does
	 * not serialize by itself. Instead, it is stored by replacing
	 * itself with this class. On de-serialization, the stored
	 * version of the poly is replaced with a normal serialized
	 * poly. The idea is to not do any kind of custom writing,
	 * but to use the built-in Java stuff to do the serialization
	 * and de-serialization.
	 *   
	 * @author saadat
	 *
	 */
	private static final class StoredSerializedPoly implements Serializable {
		/**
		 * Serial number of the stored version of serialized poly. 
		 */
		private static final long serialVersionUID = 1L;
		
		public StoredSerializedPoly(){
			control = null;
			data = null;
		}
		
		public StoredSerializedPoly(SerializablePoly poly){
			float[] pt = new float[6];
			int segType, windRule, nPts;
			Vector controlVec = new Vector();
			Vector dataVec = new Vector();
			
			PathIterator pi = poly.getPathIterator(null);
			windRule = pi.getWindingRule();
			controlVec.add(new Integer(windRule));
			
			while(!pi.isDone()){
				segType = pi.currentSegment(pt);
				controlVec.add(new Integer(segType));
				switch(segType){
					case PathIterator.SEG_MOVETO:  nPts = 2; break;
					case PathIterator.SEG_LINETO:  nPts = 2; break;
					case PathIterator.SEG_QUADTO:  nPts = 4; break;
					case PathIterator.SEG_CUBICTO: nPts = 6; break;
					case PathIterator.SEG_CLOSE:   nPts = 0; break;
					default: nPts = 0; break;
				}
				
				for(int i = 0; i < nPts; i++){
					dataVec.add(new Float(pt[i]));
				}
				
				pi.next();
			}
			
			control = intVectorToArray(controlVec);
			data = floatVectorToArray(dataVec);
		}
		
		public Object readResolve() throws ObjectStreamException {
			GeneralPath poly = new GeneralPath();
			int ci = 0, di = 0;
			
			poly.setWindingRule(control[ci++]);
			while(ci < control.length){
				switch(control[ci]){
					case PathIterator.SEG_MOVETO:
						poly.moveTo(data[di++], data[di++]);
						break;
					case PathIterator.SEG_LINETO:
						poly.lineTo(data[di++], data[di++]);
						break;
					case PathIterator.SEG_CUBICTO:
						poly.curveTo(
								data[di++], data[di++], data[di++],
								data[di++], data[di++], data[di++]);
						break;
					case PathIterator.SEG_QUADTO:
						poly.quadTo(data[di++], data[di++], data[di++], data[di++]);
						break;
					case PathIterator.SEG_CLOSE:
						poly.closePath();
						break;
					default:
						throw new RuntimeException("Unhandled segment type "+control[ci]);
				}
				ci++;
			}
			
			return new SerializablePoly(poly);
		}
		
		private int[] intVectorToArray(Vector v){
			int[] asArray = new int[v.size()];
			
			for(int i = 0; i < v.size(); i++){
				asArray[i] = ((Integer)v.get(i)).intValue();
			}
			
			return asArray;
		}
		
		private float[] floatVectorToArray(Vector v){
			float[] asArray = new float[v.size()];
			
			for(int i = 0; i < v.size(); i++){
				asArray[i] = ((Float)v.get(i)).floatValue();
			}
			
			return asArray;
		}
		
		private int[] control;
		private float[] data;
	}
	
	public static void main(String[] args){
		try {
			GeneralPath p = new GeneralPath();
			p.moveTo(10.0f, 10.0f);
			p.lineTo(10.0f, 20.0f);
			p.lineTo(20.0f, 20.0f);
			p.closePath();
		
			SerializablePoly q = new SerializablePoly(p);
			System.out.println(q);
		
			FileOutputStream fos = new FileOutputStream("test.bin");
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			ObjectOutputStream oos = new ObjectOutputStream(bos);
		
			oos.writeObject(q);
			oos.close();
		
			FileInputStream fis = new FileInputStream("test.bin");
			BufferedInputStream bis = new BufferedInputStream(fis);
			ObjectInputStream ois = new ObjectInputStream(bis);
		
			SerializablePoly qq = (SerializablePoly)ois.readObject();
			System.out.println(qq);
			
			ois.close();
		}
		catch(Exception ex){
			ex.printStackTrace();
		}
	}
}
