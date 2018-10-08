package edu.asu.jmars.layer.util.features;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.shape2.ColumnEditor;
import edu.asu.jmars.layer.shape2.ShapeLayer;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.ListType;
import edu.asu.jmars.util.Util;
import gnu.jel.CompiledExpression;
import gnu.jel.DVMap;
import gnu.jel.Evaluator;
import gnu.jel.Library;

/**
 * Provides a formula parser and evaluator for Feature instances based on any
 * combination of byte, short, int, long, float, or double field values.
 * 
 * TODO: boo, autosave badly messes things up, not sure why.
 */
public class FieldFormula extends CalculatedField {
	private static final long serialVersionUID = 1L;
	private static DebugLog log = DebugLog.instance();
	
	/** The text expression the user entered. */
	private String textExpression;
	/** The schema used in the last expression compilation, used to restore a serialized FieldFormula. */
	private  Collection<Field> schema;
	//Hashmap to store the alias names and field names mapping
	public final static Map<String,String> aliasMap=new HashMap<String,String>();//1787
	 /**
	 * The context array, always a single instance of FieldAccessor. This is a
	 * temp variable really, just passed to the evaluate method again and again,
	 * so we don't save it.
	 */
	private transient FieldAccessor[] callContext;
	/**
	 * The compiled expression, or null if none has been successfully compiled
	 * yet. This is not saved, but is instead rebuilt each time; this is to both
	 * ensure that we are always evaluating code that was compiled with the
	 * latest JEL library, but also because JEL expressions do not serialize
	 * well.
	 */
	private transient CompiledExpression compiledExpression;
	
	private FieldFormula(Class<?> type) {
		super("Formula Field", type);
	}
	
	/**
	 * Called by the deserialization code as the last step to determining if
	 * this is the actual object instance we want to inject into the jvm. We use
	 * this point to set up and compile the expression.
	 */

	private Object readResolve() {
		try {
			setExpression(textExpression,schema);
		} catch (Throwable t) {
			// let well enough alone, but whine about it
			log.aprintln(t);
		}
		return this;
	}
	
	private void setExpression(String text, Collection<Field> schema) throws Throwable {

		FieldAccessor map=new FieldAccessor(schema);
		Library lib = new Library(
			new Class[]{Math.class, ColorMethods.class, FieldFormulaMethods.class},
			new Class[]{FieldAccessor.class},
			new Class[]{},
			map,
			null);
		CompiledExpression exp = null;
		exp = Evaluator.compile("convertReturnType("+text+")", lib, type);
		this.callContext = new FieldAccessor[]{map};
		this.schema=schema;
		this.textExpression = text;
		this.compiledExpression = exp;
	}
	
	@Override
	public Set<Field> getFields() {
		if (callContext == null) {
			return Collections.emptySet();
		} else {
			return new LinkedHashSet<Field>(callContext[0].getUsedFields());
		}
	}
	
	@Override
	public Object getValue(ShapeLayer layer, Feature f) {
		if (compiledExpression == null) {
			return null;
		}
		try {
			callContext[0].setFeature(f);
			return compiledExpression.evaluate(callContext);
		} catch (Throwable e) {
			// TODO: disable this, or gather the spew in some way
			log.println(e);
			return null;
		}
	}
	
	public static final class ColorMethods {
		public static float[] hsb(Color c) {
			float[] values = {0,0,0};
			Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), values);
			return values;
		}
		public static float[] rgba(Color c) {
			return c.getRGBComponents(null);
		}
		public static Color fromhsb(float h, float s, float i) {
			return Color.getHSBColor(h, s, i);
		}
		public static Color fromrgba(float r, float g, float b, float a) {
			return new Color(r,g,b,a);
		}
		public static Color color(String name) {
			return Color.getColor(name);
		}
		public static Color color(int r, int g, int b) {
			return color(r,g,b,255);
		}
		public static Color color(int r, int g, int b, int a) {
			return new Color(r,g,b,a);
		}
		public static int red(Color c) {
			return c.getRed();
		}
		public static int green(Color c) {
			return c.getGreen();
		}
		public static int blue(Color c) {
			return c.getBlue();
		}
		public static int alpha(Color c) {
			return c.getAlpha();
		}
		private static float[] temp = new float[3];
		private static Color color;
		public static double hue(Color c) {
			set(c);
			return temp[0];
		}
		public static double saturation(Color c) {
			set(c);
			return temp[1];
		}
		public static double intensity(Color c) {
			set(c);
			return temp[2];
		}
		private static void set(Color c) {
			if (c != color) {
				color = c;
				Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), temp);
			}
		}
	}
	public static class Factory extends FieldFactory<FieldFormula> {
		public Factory() {
			super("Formula Field", FieldFormula.class, null);
		}

		@Override
		public JPanel createEditor(final ColumnEditor editor, Field source) {
			final Collection<Field> schema = editor.getModelFields();
			final FieldFormula f = (FieldFormula)source;
			JLabel help = new JLabel("Enter function. See help for the available functions.");
			
			JButton helpPB = new JButton("Help");
			helpPB.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try {
						Util.launchBrowser(Config.get("shape.formula.help"));
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				}
			});
			
			Box buttons = Box.createHorizontalBox();
			buttons.add(helpPB);
			buttons.add(Box.createHorizontalStrut(4));
			buttons.add(Box.createHorizontalGlue());
			
			final JLabel errors = new JLabel();
			errors.setVerticalAlignment(JLabel.TOP);
			errors.setForeground(Color.red);
			
			final JTextArea text = new JTextArea(4, 60);
			text.getDocument().addDocumentListener(new DocumentListener() {
				public void changedUpdate(DocumentEvent e) {
					change();
				}
				public void insertUpdate(DocumentEvent e) {
					change();
				}
				public void removeUpdate(DocumentEvent e) {
					change();
				}
				private void change() {
					try {
						if((text.getText()!=null)&&(text.getText().length()>0)){
							f.setExpression(text.getText(), schema);
							errors.setText("");
							editor.validate();
						} else {
							errors.setText("");
							f.textExpression="";
						}
					} catch (Throwable e) {
						// make no change on error, just whine about it
						String msg = e.getMessage();
						while (e.getCause() != null) {
							e = e.getCause();
							msg += "\n  Caused by: " + e.getMessage();
						}
						errors.setText(msg);
					}
				}
			});
			text.setText(f.textExpression);
			
			JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
			split.setDividerLocation(.8);
			split.add(text);
			split.add(errors);
						
			JPanel panel = new JPanel();
			panel.setLayout(new BorderLayout());
			panel.setBorder(new EmptyBorder(4,4,4,4));
			panel.add(help, BorderLayout.NORTH);
			panel.add(split, BorderLayout.CENTER);
			panel.add(buttons, BorderLayout.SOUTH);
			return panel;
		}

		@Override
		public FieldFormula createField(Set<Field> fields) {
			JComboBox box = new JComboBox(new Object[]{
				String.class, Boolean.class, Integer.class, Double.class
			});
			String[] names = ColumnEditor.basicTypes.keySet().toArray(new String[]{});
			Object result = JOptionPane.showInputDialog(
				LManager.getDisplayFrame(),   // TODO: This isn't the right place to show the dialog, but we don't have an obvious reference to the right one
				"Choose expression type", "Choose expression type",
				JOptionPane.QUESTION_MESSAGE, null,
				names, names[0]);
			Class<?> type = ColumnEditor.basicTypes.get(result);
			if (type == null) {
				return null;
			} else {
				return new FieldFormula(type);
			}
		}
	}
	
	public static final class FieldAccessor extends DVMap implements Serializable{
		private static final long serialVersionUID = 1L;
		/** The fields for this schema. */
		private final Field[] fields;
		/** Fields actually looked up by the compiler. */
		private Set<Field> usedFields = new LinkedHashSet<Field>();
		/** The current feature to retrieve values from */
		private transient Feature f;
		/** Creates the name->field mapping for all possible fields. */
		public FieldAccessor(Collection<Field> fields) {
			this.fields = fields.toArray(new Field[fields.size()]);
		}
		
		/**
		 * After this context is used to compile an expression, this method will
		 * return the fields the compiler looked up.
		 */
		public Set<Field> getUsedFields() {
			return usedFields;
		}
		
		/**
		 * Called by the compiler to get the variable type, which the JEL
		 * assembler will use to determine which get<Type>Property() method to
		 * call. We fail any type for which this class does not have such a
		 * get<Type>Property() method.
		 */
		public String getTypeName(String name) {
			Class<?> failedType = null;
				for (int i = 0; i < fields.length; i++) {
					if (name.equalsIgnoreCase(fields[i].name)) {
						if (String.class.isAssignableFrom(fields[i].type)) {
							return "String";
						} else if (Boolean.class.isAssignableFrom(fields[i].type)) {
							return "Boolean";
						} else if (Color.class.isAssignableFrom(fields[i].type)) {
							return "Color";
						} else if (Byte.class.isAssignableFrom(fields[i].type)) {
							return "Byte";
						} else if (Short.class.isAssignableFrom(fields[i].type)) {
							return "Short";
						} else if (Integer.class.isAssignableFrom(fields[i].type)) {
							return "Integer";
						} else if (Long.class.isAssignableFrom(fields[i].type)) {
							return "Long";
						} else if (Float.class.isAssignableFrom(fields[i].type)) {
							return "Float";
						} else if (Double.class.isAssignableFrom(fields[i].type)) {
							return "Double";
						} else if (ListType.class.isAssignableFrom(fields[i].type)){
							return "ListType";
						}
						
						else {
								// mark the unsupported type, but keep looking in case
								// we have another field with the right name *and* the
								// right type
							if (failedType == null) {
							// track the first unsupported type
							failedType = fields[i].type;
							}
							}
					} else {//1787
						for(Map.Entry<String, String> entry:aliasMap.entrySet()){
							if(name.equals(entry.getKey())){
								String val=entry.getValue();
								for (i = 0; i < fields.length; i++){
									if (val.equalsIgnoreCase(fields[i].name)) {
										if (String.class.isAssignableFrom(fields[i].type)) {
											return "String";
										} else if (Boolean.class.isAssignableFrom(fields[i].type)) {
											return "Boolean";
										} else if (Color.class.isAssignableFrom(fields[i].type)) {
											return "Color";
										} else if (Byte.class.isAssignableFrom(fields[i].type)) {
											return "Byte";
										} else if (Short.class.isAssignableFrom(fields[i].type)) {
											return "Short";
										} else if (Integer.class.isAssignableFrom(fields[i].type)) {
											return "Integer";
										} else if (Long.class.isAssignableFrom(fields[i].type)) {
											return "Long";
										} else if (Float.class.isAssignableFrom(fields[i].type)) {
											return "Float";
										} else if (Double.class.isAssignableFrom(fields[i].type)) {
											return "Double";
										} else if (ListType.class.isAssignableFrom(fields[i].type)){
											return "ListType";
										} 
										
										else {
											// mark the unsupported type, but keep looking in case
											// we have another field with the right name *and* the
											// right type
											if (failedType == null) {
												// track the first unsupported type
												failedType = fields[i].type;
											}
										}
									}
								}
							}
						}
					}
				}
				if (failedType == null) {
					// no name matched
					// throw new IllegalArgumentException("Name " + name + " not found");
					return null;
				} else {
					// at least one name matched but the type was wrong, so report
					// the unsupported type of the first field where that occurred.
					throw new IllegalArgumentException("Variable " + name +
						" has unsupported type " + failedType.getSimpleName());
				}
			}
		
		/**
		 * Called by the compiler to convert variable names into field indices,
		 * matching in a case-insensitive way.
		 */
		public Object translate(String name) {
			for (int i = 0; i < fields.length; i++) {
				if (name.equalsIgnoreCase(fields[i].name)) {
					usedFields.add(fields[i]);
					return i;
				} else {//1787
					for(Map.Entry<String, String> entry:aliasMap.entrySet()){
						if(name.equals(entry.getKey())){
							String val=entry.getValue();
							for (i = 0; i < fields.length; i++) {
								if (val.equalsIgnoreCase(fields[i].name)) {
									usedFields.add(fields[i]);
									return i;
								}
							}
						}
					}
				}//1787 end
			}
			throw new IllegalArgumentException("Name " + name + " not found");
		}
		
		/**
		 * Sets the feature to use as the source of variable values. A series of
		 * calculations that iterate over a FeatureCollection should call this
		 * method once per feature just prior to evaluating the CompiledExpression.
		 * 
		 * Note that this allows us to use one object that provides access to
		 * each Feature in its turn, rather than creating N wrapper objects that
		 * add nothing but the getXXXValue() methods.
		 */
		public void setFeature(Feature f) {
			this.f = f;
		}

		/**
		 * Called by the evaluator to get the value at the given column
		 * position. We don't optimize access to attributes, beyond the
		 * name->field lookup, because a Feature can contain hundreds of
		 * columns, and the time to optimize will greatly exceed the cost of a
		 * single lookup for a single column, which is probably the common case.
		 */
		public Object getProperty(int column) {
			return f.getAttribute(fields[column]);
		}
		public String getStringProperty(int column) {
			return (String)getProperty(column);
		}
		public Boolean getBooleanProperty(int column) {
			return (Boolean)getProperty(column);
		}
		public Color getColorProperty(int column) {
			return (Color)getProperty(column);
		}
		public Byte getByteProperty(int column) {
			return (Byte)getProperty(column);
		}
		public Short getShortProperty(int column) {
			return (Short)getProperty(column);
		}
		public Integer getIntegerProperty(int column) {
			return (Integer)getProperty(column);
		}
		public Long getLongProperty(int column) {
			return (Long)getProperty(column);
		}
		public Float getFloatProperty(int column) {
			return (Float)getProperty(column);
		}
		public Double getDoubleProperty(int column) {
			return (Double)getProperty(column);
		}
		public String getListTypeProperty(int column) {
			if(getProperty(column)==null){
				return "";
			}
			return (String)getProperty(column);
		}
		
		
		public static void main(String[] args) throws Throwable {
			// int, double
			// 123, 1.05
			// 5, 0.03
			FeatureCollection fc = new SingleFeatureCollection();
				
			Feature f = new Feature();
			Field c1 = new Field("c1", Integer.class);
			Field c2 = new Field("c2", Double.class);
			f.setAttribute(c1, 123);
			f.setAttribute(c2, 1.05);
			fc.addFeature(f);
			
			f = new Feature();
			f.setAttribute(c1, 5);
			f.setAttribute(c2, .03);
			fc.addFeature(f);
			
			FieldAccessor map = new FieldAccessor(fc.getSchema());
			Class type = Integer.class;
			Library lib = new Library(
					new Class[]{Math.class, ColorMethods.class, FieldFormulaMethods.class},
					new Class[]{FieldAccessor.class},
					new Class[]{},
					map,
					null);
			CompiledExpression exp = Evaluator.compile("convertReturnType(123)", lib, type);
			for (Feature feat: fc.getFeatures()) {
				map.setFeature(feat);
				System.out.println(exp.evaluate(new Object[]{map}));
			}
		}
	}
}
