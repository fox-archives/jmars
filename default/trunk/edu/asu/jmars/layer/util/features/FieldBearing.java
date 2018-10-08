package edu.asu.jmars.layer.util.features;

import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.util.Collections;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.asu.jmars.layer.shape2.ColumnEditor;
import edu.asu.jmars.layer.shape2.ShapeLayer;

public class FieldBearing extends CalculatedField {
	private static final long serialVersionUID = 1L;
	private static final Set<Field> fields = Collections.singleton(Field.FIELD_PATH);
	public FieldBearing(String name) {
		super(name, Double.class);
	}
	public Set<Field> getFields() {
		return fields;
	}
	public Object getValue(ShapeLayer layer, Feature f) {
		Shape gp = f.getPath().getSpatialEast().getShape();
		float[] coords = new float[6];
		float[] a = new float[2];
		float[] b = new float[2];
		int i = 0;
		for (PathIterator pi = gp.getPathIterator(null); !pi.isDone(); pi.next()) {
			switch (pi.currentSegment(coords)) {
			case PathIterator.SEG_MOVETO:
			case PathIterator.SEG_LINETO:
				if (i > 0) {
					System.arraycopy(b, 0, a, 0, 2);
				}
				System.arraycopy(coords, 0, b, 0, 2);
				i++;
				if (i > 2) {
					return null;
				}
				break;
			case PathIterator.SEG_CLOSE:
				break;
			default:
				return null;
			}
		}
		
		if (i == 2) {
			// the formula for initial compass bearing is:
			// theta = atan2(sin(deltalong)*cos(lat2),cos(lat1)*sin(lat2)-sin(lat1)*cos(lat2)*cos(deltalong))
			// from initial(a to b), final bearing is (initial(b to a)+180)%360
			double dlon = Math.toRadians(a[0] - b[0]);
			double sin1 = Math.sin(Math.toRadians(b[1]));
			double cos1 = Math.cos(Math.toRadians(b[1]));
			double sin2 = Math.sin(Math.toRadians(a[1]));
			double cos2 = Math.cos(Math.toRadians(a[1]));
			double theta = Math.atan2(Math.sin(dlon)*cos2, cos1*sin2-sin1*cos2*Math.cos(dlon));
			return (Math.toDegrees(theta) + 180)%360;
		} else {
			return null;
		}
	}
	public static class Factory extends FieldFactory<FieldBearing> {
		public Factory() {
			super("Line Direction", FieldBearing.class, Double.class);
		}
		public JPanel createEditor(ColumnEditor editor, Field f) {
			JPanel out = new JPanel();
			out.add(new JLabel("<html>Computes azimuth of a line<br>in degrees east of north.</html>"));
			return out;
		}
		public FieldBearing createField(Set<Field> fields) {
			return new FieldBearing(getName());
		}
	}
}
