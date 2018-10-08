package edu.asu.jmars.layer.tes6;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//import org.apache.commons.httpclient.HttpClient;                         TODO (PW) Remove commented-out code
//import org.apache.commons.httpclient.methods.PostMethod;

import edu.asu.jmars.Main;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.JmarsHttpRequest;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.util.HttpRequestType;


public class DbUtils {
    private static DebugLog log = DebugLog.instance();

    protected DbUtils(){
        LOG_SQL = Config.get("tes.db.logSql", false);
        log.println("Logging is "+(LOG_SQL? "on": "off"));

        try {
        	String tesAuthUrl = Config.get("tes.auth.url");
        	log.println("Retrieving TES authentication credentials from: "+tesAuthUrl);
        	String[] userPass = getUserPass(tesAuthUrl, Main.USER, Main.PASS);
        	dbUser = userPass[0];
        	dbPassword = userPass[1];
        }
        catch(Exception ex){
        	log.println(ex);
        	throw new RuntimeException("Unable to get TES database authentication credentails.", ex);
        }
        
        dbUrl = Config.get("tes.db.url");

        relTable = Config.get("tes.db.releaseTable");
        dataTable = Config.get("tes.db.dataTable");
        polyTable = Config.get("tes.db.polyTable");
        fieldDescTable = Config.get("tes.db.fieldDescTable");
        templateTable = Config.get("tes.db.templateTable");

        // we have this and one in the TesLayer - merge them
        detKeyField = new FieldDesc(
            "detid", "", templateTable, false, false, false,
            Integer.class);
    }
    
    public Connection createConnection() throws SQLException {
    	return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    public static DbUtils getInstance(){
        if (dbUtils == null){
            dbUtils = new DbUtils();
        }
        return dbUtils;
    }
    
    private String[] getUserPass(String fromUrl, String user, String pass) throws URISyntaxException, IOException { // TODO (PW) Remove commented-out code
//    	HttpClient client = new HttpClient();
//   	PostMethod post = new PostMethod(fromUrl);
//    	post.addParameter("user", user);
//    	post.addParameter("pass", pass);
        JmarsHttpRequest request = new JmarsHttpRequest(fromUrl, HttpRequestType.POST);
        request.addRequestParameter("user", user);
        request.addRequestParameter("pass", pass);
        
        String[] lines = {"ERROR: no response received from server}"};
    	try {
//    		Util.postWithRedirect(client, post, 3);
    	    request.send();
        	lines = Util.readLines(request.getResponseAsStream());
    	} finally {
    		request.close();
    	}
    	
        for(int i=0; i<lines.length; i++) {
        	if (lines[i].trim().toLowerCase().startsWith("error")) {
        		throw new RuntimeException(Util.join("\n", lines));
        	}
        }
        
        if (lines.length != 2) {
        	throw new RuntimeException((lines.length>2?"More":"Less")+" than expected lines ("+lines.length+") returned.");
        }
    	
        return lines;
    }
    
    public boolean validSelectClauseField(String fieldName){
        Connection c = null;
        boolean valid = false;

        try {
            // Have to use key field here otherwise avg() and count()
            // aggregates get passed through.
            String sql = "select "+
                detKeyField.getQualifiedFieldName()+","+fieldName+
                " from "+templateTable+" limit 0";
            if (LOG_SQL){ log.println(sql); }

            c = createConnection();
            Statement stmt = c.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            // if the execute is successful then consider this field valid
            
            valid = true;
        }
        catch(SQLException ex){
        	log.println(ex);
            valid = false;
        }
        finally {
            if (c != null){ try { c.close(); } catch(SQLException ex){ log.println(ex); } }
        }

        return valid;
    }

    /* NOTE: According to the documentation, if the
       ResultSetMetaData.isSearchable(i) is true for 
       field[i], then the field can be used in the
       where-clause. But our mysql driver seems to
       return isSearchable() true even on aggregated
       fields, but is unable to actually select on
       them. Hence I am using an alternate method here.
    */
    public boolean validWhereClauseField(String fieldName){
        String sql = "select * from "+templateTable+
            " where "+fieldName+" is not null "+
            " limit 0";
        if (LOG_SQL){ log.println(sql); }
        Connection c = null;
        boolean valid = false;

        try {
            c = createConnection();
            ResultSet rs = c.createStatement().executeQuery(sql);
            // if the execute is successful then consider this field valid

            valid = true;
        }
        catch(SQLException ex){
            valid = false;
        }
        finally {
            if (c != null){ try { c.close(); } catch(SQLException ex){ log.println(ex); } }
        }

        return valid;
    }

    public boolean validOrderByClauseField(String fieldName){
        String sql = "select * from "+templateTable+
            " order by "+fieldName+" asc "+
            " limit 0";
        if (LOG_SQL){ log.println(sql); }
        
        Connection c = null;
        boolean valid = false;

        try {
            c = createConnection();
            ResultSet rs = c.createStatement().executeQuery(sql);
            // if the execute is successful then consider this field valid

            valid = true;
        }
        catch(SQLException ex){
            valid = false;
        }
        finally {
            if (c != null){ try { c.close(); } catch(SQLException ex){ log.println(ex); } }
        }

        return valid;
    }

    public FieldDesc getFieldDescFromDb(String fieldName) throws SQLException {
        String sql;

        FieldDesc f = null;
        Connection c = null;
        Statement stmt;
        ResultSet rs;

        try {
            c = createConnection();
            stmt = c.createStatement();

            // Get field attributes from the database. Note that these
            // may be derived attributes, e.g. log(target_temp).
            sql = "select "+fieldName+" from "+templateTable+" limit 0";
            if (LOG_SQL){ log.println(sql); }
            rs = stmt.executeQuery(sql);
            ResultSetMetaData md = rs.getMetaData();
            String columnDbType = md.getColumnTypeName(1);
            Class columnClass = null;
            try {
                columnClass = Class.forName(md.getColumnClassName(1));
            }
            catch(ClassNotFoundException ex){
                ex.printStackTrace();
            }
            // md.isSearchable(1); -- not properly implemented in our driver

            // If the field can be decomposed into base plus index range then do so.
            // This decomposition will be used for spectral-fields.
            String baseFieldName = fieldName;
            int indexRange[] = null;
            Pattern singleDimArrayName = Pattern.compile("^([a-zA-Z_]\\w*)\\[(\\d+):(\\d+)\\]$");
            Matcher m = singleDimArrayName.matcher(fieldName);
            if (m.matches()){
            	try {
            		baseFieldName = m.group(1);
            		indexRange = new int[2];
            		indexRange[0] = Integer.parseInt(m.group(2))-1;
            		indexRange[1] = Integer.parseInt(m.group(3))-1;
            	}
            	catch(NumberFormatException ex){
            		log.println("Unable to split possible array field into its constituents. "+ex.toString());
            		baseFieldName = fieldName;
            		indexRange = null;
            	}
            }
            
            // Determine if this field is a foot-print field or a per 
            // detector-field. We can do this poorly by looking for this
            // field in the field_desc table. If the field is marked as
            // foot-print field, we assume it is a detector-print field.
            sql = "select is_fp_field, is_spectral_field from "+fieldDescTable+
                " where field_name=\'"+baseFieldName+"\' limit 1";
            if (LOG_SQL){ log.println(sql); }
            rs = stmt.executeQuery(sql);

            boolean fpField = false, spectralField = false;
            while(rs.next()){ fpField = rs.getBoolean(1); spectralField = rs.getBoolean(2); }

            boolean arrayField = md.getColumnClassName(1).equals("java.sql.Array");
            f = new FieldDesc(fieldName, fieldName, fieldName, dataTable, fpField, arrayField, spectralField, columnClass, indexRange);
        }
        finally{
            if (c != null){ c.close(); }
        }

        return f;
    }

	public static String buildSqlSimple(String fields[], String table){
		String sql = "select ";
		
		for(int i = 0; i < fields.length; i++){
			sql += fields[i];
			if (i < (fields.length-1)){ sql += ","; }
		}

		sql += " from "+ table;

		return sql;
	}

    public int getPolyFieldEnc(){
        String polyEncString = Config.get("tes.db.polyEncoding");
        
        if (polyEncString != null){
        	if (polyEncString.equals("array")){
        		return POLY_ENC_ARRAY;
        	}
        	else if (polyEncString.equals("string")){ // PostgreSQL string
        		return POLY_ENC_STRING;
        	}
        	else if (polyEncString.equals("wkb")){
        		return POLY_ENC_WKB;
        	}
        }
        return POLY_ENC_WKB;
    }
    
    public String getDbUrl(){ return dbUrl; }
    
    /**
     * @return Revision-id of the TES-data or <tt>null</tt> if none was found.
     * @throws SQLException In case of an SQLException.
     */
    public String getDataRevision() throws SQLException {
    	String sql = "select rev_id from "+relTable+" order by rev_date desc limit 1";
        Connection c = null;
        Statement stmt;
        ResultSet rs;
        String rev = null;

        try {
            c = createConnection();
            stmt = c.createStatement();

            if (LOG_SQL){ log.println(sql); }
            
            rs = stmt.executeQuery(sql);
            if (rs.next()){
            	rev = rs.getString(1);
            }
        }
        finally{
            if (c != null){ c.close(); }
        }
        
        return rev;
    }


    public String      relTable;
    public String      dataTable;
    public String      polyTable;
    public String      fieldDescTable;
    public String      templateTable;
    public FieldDesc   detKeyField;
    
    // singleton instance of this object
    private static DbUtils dbUtils = null;

    private boolean LOG_SQL = false;
	private String dbUrl;
    private String dbUser;
    private String dbPassword;

    public static final int POLY_ENC_WKB = 0;
    public static final int POLY_ENC_STRING = 1;
    public static final int POLY_ENC_ARRAY = 2;

}
