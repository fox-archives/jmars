package edu.asu.jmars.layer.tes6;

import java.awt.Color;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import edu.asu.jmars.swing.ColorInterp;
import edu.asu.jmars.swing.ColorMapper;


// TODO: Get rid of user/all fields distinction

class TesContext implements Cloneable, Serializable {
	private static final long serialVersionUID = 1L;
	
	public TesContext(String title) {
        this.title = title;
        if (title == null)
        	throw new IllegalArgumentException("Title may not be null.");
        this.desc = "";
        
        userFields = new Vector<FieldDesc>();
		userSelects = new Vector<RangeDesc>();
        orderParams = new Vector<OrderCriterion>();
        colorBy = new ColorBy(null, null, null);
        drawReal = new Boolean(false);
        drawNull = new Boolean(true);
        colorMapperState = ColorMapper.State.DEFAULT;

        // initialize tracking of changes to the context
        lastChangeTime     = System.currentTimeMillis();
        titleChangeTime    = lastChangeTime;
        descChangeTime     = lastChangeTime;
        fieldsChangeTime   = lastChangeTime;
        selectsChangeTime  = lastChangeTime;
        orderBysChangeTime = lastChangeTime;
        colorChangeTime    = lastChangeTime;
        drawRealChangeTime = lastChangeTime;
        drawNullChangeTime = lastChangeTime;

        whereClauseTime    = lastChangeTime;
        orderByClauseTime  = lastChangeTime;
        ctxImageTime       = lastChangeTime;
	}
	
	public TesContext clone() {
		try {
			TesContext clone = (TesContext)super.clone();

			userFields = copyFields(userFields);
			userSelects = copySelects(userSelects);
			orderParams = copyOrderBys(orderParams);
			colorBy = colorBy.clone();
			colorMapperState = (ColorMapper.State)colorMapperState.clone();

			clone.resetChangeFlags();

			return clone;
		}
		catch(CloneNotSupportedException ex){
			throw new RuntimeException("TesContext not clonable.", ex);
		}
	}
	
	/**
	 * @return <code>true</code> if there is no selection criteria specified.
	 */
	public boolean isUnconstrained(){
		return userSelects.isEmpty();
	}
	
	public boolean isEmpty(){
		return userFields.isEmpty() && userSelects.isEmpty() && orderParams.isEmpty() && colorBy.isEmpty();
	}

    public synchronized void setTitle(String title){
        // Note: Title change is considered non-destructive.
        lastChangeTime  = System.currentTimeMillis();
        titleChangeTime = lastChangeTime;

        this.title = title;
    }

    public synchronized void setDesc(String desc){
        // Note: Title change is considered non-destructive.
        lastChangeTime  = System.currentTimeMillis();
        descChangeTime = lastChangeTime;

        this.desc = desc;
    }

    public synchronized void setFields(Vector<FieldDesc> userFields){
        // Note: changing the list of fields is considered non-destructive.
        lastChangeTime   = System.currentTimeMillis();
        fieldsChangeTime = lastChangeTime;

        this.userFields.clear();
        this.userFields.addAll(userFields);

    }

    public synchronized void setSelects(Vector<RangeDesc> selects){
        lastChangeTime    = System.currentTimeMillis();
        selectsChangeTime = lastChangeTime;

        // TODO: This has repercussions when selects are
        // TODO: modified in place by the UI.
        // TODO: Deep copy instead.
        userSelects.clear();
        userSelects.addAll(selects);
    }

    public synchronized void setOrderCriteria(Vector<OrderCriterion> orderings){
        lastChangeTime     = System.currentTimeMillis();
        orderBysChangeTime = lastChangeTime;

        // TODO: This has repercussions when order criteria are
        // TODO: changed in place by the UI.
        // TODO: Deep copy instead.
        orderParams.clear();
        orderParams.addAll(orderings);
    }

    public synchronized void addOrderCriterion(OrderCriterion c){
        lastChangeTime     = System.currentTimeMillis();
        orderBysChangeTime = lastChangeTime;

        // TODO: clone the order criterion
        orderParams.add(c);
    }

    public synchronized void setColorBy(ColorBy colorBy){
        lastChangeTime  = System.currentTimeMillis();
        colorChangeTime = lastChangeTime;

        this.colorBy = colorBy;
    }
    
    public synchronized void setDrawReal(Boolean drawReal){
    	lastChangeTime = System.currentTimeMillis();
    	drawRealChangeTime = lastChangeTime;
    	
    	this.drawReal = drawReal;
    }
    
    public synchronized void setDrawNull(Boolean drawNull){
    	lastChangeTime = System.currentTimeMillis();
    	drawNullChangeTime = lastChangeTime;
    	
    	this.drawNull = drawNull;
    }
    
    public synchronized void setColorMapperState(ColorMapper.State colorMapperState){
    	// don't need to keep track of time since the change is instantenous
    	// we only store this piece of information on a context change
    	this.colorMapperState = colorMapperState;
    }
    

    private static long max(long[] vals){
        long m = vals[0];

        for(int i = 1; i < vals.length; i++){
            m = Math.max(m, vals[i]);
        }

        return m;
    }

    public synchronized long getLastChangeTime(){
        return lastChangeTime;
    }

    public synchronized long getLastDestructiveChangeTime(){
        return max(new long[] { /*lastChangeTime, descChangeTime, */ selectsChangeTime, orderBysChangeTime,
        		colorChangeTime, drawRealChangeTime, drawNullChangeTime });
    }

    public synchronized String getTitle(){ return title; }
    public synchronized String getDesc(){ return desc; }

	public synchronized Vector<FieldDesc> getFields(){
		return new Vector<FieldDesc>(userFields);
	}

    public synchronized int getFieldIndex(FieldDesc field){
        return userFields.indexOf(field);
    }

    public synchronized FieldDesc getFieldByIndex(int idx){
        return userFields.get(idx);
    }

	public synchronized Vector<RangeDesc> getSelects(){
		return userSelects;
	}

    public synchronized Vector<OrderCriterion> getOrderCriteria(){
        return orderParams;
    }

    public synchronized ColorBy getColorBy(){
        // TODO: clone it at some point
        return colorBy;
    }
    
    public synchronized Boolean getDrawReal(){
    	return drawReal;
    }
    
    public synchronized Boolean getDrawNull(){
    	return drawNull;
    }
    
    public ColorMapper.State getColorMapperState(){
    	return colorMapperState;
    }

    /*
    public synchronized boolean[] getChangeFlags(){
        boolean[] flags = new boolean[4];

        flags[0] = (lastChangeTime > titleChangeTime);
        flags[1] = (lastChangeTime > fieldsChangeTime);
        flags[2] = (lastChangeTime > orderBysChangeTime);
        flags[3] = (lastChangeTime > colorChangeTime);

        return flags;
    }
    */

    public synchronized void resetChangeFlags(){
        lastChangeTime     = System.currentTimeMillis();
        titleChangeTime    = lastChangeTime;
        descChangeTime     = lastChangeTime;
        fieldsChangeTime   = lastChangeTime;
        selectsChangeTime  = lastChangeTime;
        orderBysChangeTime = lastChangeTime;
        colorChangeTime    = lastChangeTime;
        drawRealChangeTime = lastChangeTime;
        drawNullChangeTime = lastChangeTime;
    }

    public synchronized String toString(){ return "TesContext["+title+"]"; }

	public synchronized Set<String> getParticipatingTables(){
		Set<String> tables = new HashSet<String>();
		int i;

		// Get a list of all the tables involved
		for(i = 0; i < userFields.size(); i++){
			tables.add(((FieldDesc)userFields.get(i)).getTableName());
		}
		for (i = 0; i < userSelects.size(); i++){
			tables.add(((RangeDesc)userSelects.get(i)).getField().getTableName());
		}
        for (i = 0; i < orderParams.size(); i++){
            tables.add(((OrderCriterion)orderParams.get(i)).getField().getTableName());
        }

		return tables;
	}
	
    // following three calls are caching calls
    public synchronized String getWhereClause(){
        if (whereClause == null || (whereClauseTime < selectsChangeTime)){
            StringBuffer sql = new StringBuffer();
            RangeDesc select;
            FieldDesc field;
            int i;
            
            // Collect disjunctions. These are fields appearing more than once.
            Map<String,Vector<RangeDesc>> fieldRanges = new HashMap<String,Vector<RangeDesc>>();
            for(i = 0; i < userSelects.size(); i++){
            	String fieldName = ((RangeDesc)userSelects.get(i)).getField().getFieldName();
            	Vector<RangeDesc> ranges = fieldRanges.get(fieldName);
            	if (ranges == null){
            		ranges = new Vector<RangeDesc>();
            		fieldRanges.put(fieldName, ranges);
            	}
           		ranges.add(userSelects.get(i));
            }
            
            
            // TODO: quoting of string values ???????????????????????????????
            boolean first = true;
            for(String rangeFieldName: fieldRanges.keySet()){
                if (!first){ sql.append(" AND "); }
                first = false;
                
            	Vector<RangeDesc> ranges = (Vector<RangeDesc>)fieldRanges.get(rangeFieldName);
            	
            	if (ranges.size() > 1){ sql.append("("); }
            	for(i = 0; i < ranges.size(); i++){
            		if (i > 0){ sql.append(" OR "); }
            		sql.append("(");
            		select = (RangeDesc)ranges.get(i);
                    field = select.getField();
                    sql.append(field.getQualifiedFieldName());
                    sql.append(">=");
                    if (field.getFieldType() == String.class){
                        sql.append("'");
                        sql.append(select.getMinValue());
                        sql.append("'");
                    }
                    else {
                        sql.append(select.getMinValue());
                    }
                    sql.append(" AND ");
                    sql.append(field.getQualifiedFieldName());
                    sql.append("<=");
                    if (field.getFieldType() == String.class){
                        sql.append("'");
                        sql.append(select.getMaxValue());
                        sql.append("'");
                    }
                    else {
                        sql.append(select.getMaxValue());
                    }
                    sql.append(")");
            	}
            	if (ranges.size() > 1){ sql.append(")"); }
            }
            
            whereClause = sql.toString();
            whereClauseTime = System.currentTimeMillis();
        }
        return whereClause;
    }

    public synchronized String getOrderByClause(){
        if (orderByClause == null || (orderByClauseTime < orderBysChangeTime)){
            StringBuffer sql = new StringBuffer();
            OrderCriterion oc;
            int i;

            for(i = 0; i < orderParams.size(); i++){
                if (i > 0){ sql.append(","); }
                oc = (OrderCriterion)orderParams.get(i);
                sql.append(oc.getField().getQualifiedFieldName());
                sql.append(" "+(oc.getDirection()? "asc": "desc"));
            }

            orderByClause = sql.toString();
            orderByClauseTime = System.currentTimeMillis();
        }
        return orderByClause;
    }

    public synchronized CtxImage getCtxImage(){
        //if (ctxImage == null || (ctxImageTime < getLastDestructiveChangeTime())){
        if (ctxImage == null || (ctxImageTime < getLastChangeTime())){
            ctxImage = new CtxImage(this);
            ctxImageTime = System.currentTimeMillis();
        }
        return ctxImage;
    }
    
    public static Vector<FieldDesc> copyFields(Vector<FieldDesc> fdv){
    	return new Vector<FieldDesc>(fdv);
    }
    
    public static Vector<RangeDesc> copySelects(Vector<RangeDesc> rdv){
    	Vector<RangeDesc> out = new Vector<RangeDesc>(rdv.size());
    	for(RangeDesc rd: rdv)
    		out.add(rd.clone());
    	
    	return out;
    }
    
    public static Vector<OrderCriterion> copyOrderBys(Vector<OrderCriterion> ocv){
    	Vector<OrderCriterion> out = new Vector<OrderCriterion>(ocv.size());
    	for(OrderCriterion oc: ocv)
    		out.add(oc.clone());
    	
    	return out;
    }

    
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
	}

    // cached state
    private transient String   whereClause;
    private transient String   orderByClause;
    private transient CtxImage ctxImage;
    private transient long     whereClauseTime;
    private transient long     orderByClauseTime;
    private transient long     ctxImageTime;
    
    // title for reference purpose
    private String title;
    private String desc;

	// query parameters
    private Vector<FieldDesc> userFields;
	private Vector<RangeDesc> userSelects;

	// ordering parameters
    private Vector<OrderCriterion> orderParams;

	// coloring parameters
    private ColorBy colorBy;

    // real/interpolated draw mode selector
    private Boolean drawReal;
    
    // draw outlines for null color value
    private Boolean drawNull;
    
    // state of the color mapper
    ColorMapper.State colorMapperState;
    
    // reference information
    // private TesLayer layer;

    // Keep track of changes to the context.
    private long  lastChangeTime;   
    private long  titleChangeTime;
    private long  descChangeTime;
    private long  fieldsChangeTime;
    private long  selectsChangeTime;
    private long  orderBysChangeTime;
    private long  colorChangeTime;
    private long  drawRealChangeTime;
    private long  drawNullChangeTime;


    // Immutable image of TesContext
    final class CtxImage {
        
        private  CtxImage(TesContext ctx){
            this.srcCtx = ctx;
            lastDestructiveChangeTime = ctx.getLastDestructiveChangeTime();
            title = ctx.getTitle();
            desc = ctx.getDesc();

            Vector<FieldDesc> fieldsVector = ctx.getFields();
            fields = new FieldDesc[fieldsVector.size()];
            fieldsVector.toArray(fields);

            whereClause = ctx.getWhereClause();
            orderByClause = ctx.getOrderByClause();
            colorBy = ctx.getColorBy();
            drawReal = ctx.getDrawReal();
            drawNull = ctx.getDrawNull();
        }
        
        public  String      getTitle(){ return title; }
        public  String      getDesc(){ return desc; }
        public  String      getWhereClause(){ return  whereClause; }
        public  String      getOrderByClause(){ return orderByClause; }
        public  FieldDesc[] getFields(){ return (FieldDesc[])fields.clone(); }
        public  FieldDesc   getColorField(){ return colorBy.field; }
        public  ColorBy     getColorBy(){  return colorBy; }
        public  Boolean     getDrawReal(){ return drawReal; }
        public  Boolean     getDrawNull(){ return drawNull; }
        public  long        getLastDestructiveChangeTime(){ return lastDestructiveChangeTime; }
        public  TesContext  getSrcCtx(){ return srcCtx; }
        
        private String      title;
        private String      desc;
        private FieldDesc[] fields;
        private String      whereClause;
        private String      orderByClause;
        private ColorBy     colorBy;
        private Boolean     drawReal;
        private Boolean     drawNull;
        
        private long        lastDestructiveChangeTime;
        private TesContext  srcCtx;
    }
    
    
    //
    // XStream converters
    //
    public static class FieldDescConverter implements Converter {
    	DbUtils dbu;
    	
    	public FieldDescConverter(DbUtils dbu){
    		this.dbu = dbu;
    	}
    	
        public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
        	FieldDesc fieldDesc = (FieldDesc)source;
            writer.setValue(fieldDesc.getFieldName());
        }

        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        	String fieldName = reader.getValue();
        	try {
        		return dbu.getFieldDescFromDb(fieldName);
        	}
        	catch(SQLException ex){
        		throw new ConversionException(ex);
        	}
        }

        public boolean canConvert(Class type) {
            return type.equals(FieldDesc.class);
        }
    }

    public static class OrderingConverter implements Converter {
    	public static final String ORDER_ASC = "ascending";
    	public static final String ORDER_DSC = "descending";
    	
        public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
        	Boolean order = (Boolean)source;
        	writer.setValue(order? ORDER_ASC: ORDER_DSC);
        }

        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        	return new Boolean(ORDER_ASC.equals(reader.getValue())? true: false);
        }

        public boolean canConvert(Class type) {
            return type.equals(Boolean.class);
        }
    }

    public static class ColorMapperStateConverter implements Converter {
    	private String toString(int[] intArray){
    		StringBuffer sbuf = new StringBuffer();
    		for(int i=0; i<intArray.length; i++){
    			if (i > 0)
    				sbuf.append(",");
    			sbuf.append(intArray[i]);
    		}
    		return sbuf.toString();
    	}
    	
    	private int[] toIntArray(String s){
    		s = s.trim();
    		
    		if (s.length() == 0)
    			return new int[0];
    		
    		String[] pcs = s.split("\\s*,\\s*");
    		int[] intArray = new int[pcs.length];
    		for(int i=0; i<pcs.length; i++)
    			intArray[i] = Integer.parseInt(pcs[i]);
    		
    		return intArray;
    	}
    	
    	private String toString(Color[] colorArray){
    		StringBuffer sbuf = new StringBuffer();
    		for(int i=0; i<colorArray.length; i++){
    			if (i > 0)
    				sbuf.append(",");
    			sbuf.append(Integer.toHexString(colorArray[i].getRGB()));
    		}
    		return sbuf.toString();
    	}
    	
    	private Color[] toColorArray(String s){
    		s = s.trim();
    		
    		if (s.length() == 0)
    			return new Color[0];
    		
    		String[] pcs = s.split("\\s*,\\s*");
    		Color[] colorArray = new Color[pcs.length];
    		for(int i=0; i<pcs.length; i++)
    			colorArray[i] = new Color((int)Long.parseLong(pcs[i].toUpperCase(), 16));
    		
    		return colorArray;
    	}
    	
        public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
        	ColorMapper.State s = (ColorMapper.State)source;
        	writer.addAttribute("interp", s.getInterpolation().getKeyword());
        	writer.startNode("values");
        	writer.setValue(toString(s.getValues()));
        	writer.endNode();
        	writer.startNode("colors");
        	writer.setValue(toString(s.getColors()));
        	writer.endNode();
        }

        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        	ColorInterp interpolation = ColorInterp.forKeyword(reader.getAttribute("interp"));
        	reader.moveDown();
        	int[] values = toIntArray(reader.getValue());
        	reader.moveUp();
        	reader.moveDown();
        	Color[] colors = toColorArray(reader.getValue());
        	reader.moveUp();
        	return new ColorMapper.State(values, colors, interpolation);
        }

        public boolean canConvert(Class type) {
            return type.equals(ColorMapper.State.class);
        }
    }

}



