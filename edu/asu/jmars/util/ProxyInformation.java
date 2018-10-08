package edu.asu.jmars.util;

import static java.nio.file.attribute.PosixFilePermission.GROUP_WRITE;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_WRITE;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Scanner;
import java.util.Set;

import org.json.JSONObject;

import edu.asu.jmars.Main;

public class ProxyInformation {
    
    String  proxyHost = null;
    int     proxyPort = 0;
    String  proxyUser = null;
    String  proxyPass = null;
    String  proxyNtlmDomain = null;
    boolean proxyUsed = false;
    boolean authenticationUsed = false;
    boolean NtlmUsed = false;
    private DebugLog log = DebugLog.instance(); // for logging errors

    // Singleton
    public static final ProxyInformation INSTANCE = new ProxyInformation();
    
    //Hide the constructor
    private ProxyInformation() {
    	// Attempt to grab proxy settings from file.
    	/*
    	 * ~/.jmars_proxy_config is a json file with the following schema:
    	 * {
    	 *     host: string containing a hostname or IP address,
    	 *     port: integer port number,
    	 *     username: proxy username (leave blank if no auth required),
    	 *     password: proxy password (leave blank if no auth required)
    	 * }
    	 */
        File configfile = null;
    	try {
    		configfile = new File(Main.getUserHome(), ".jmars_proxy_config");
    		
    		// Ensure that the file is protected such that only the owner can modify it
/*    		Path filePath = Paths.get(configfile.getAbsolutePath());
            PosixFileAttributes attr = Files.readAttributes(filePath, PosixFileAttributes.class);
            Set<PosixFilePermission> perms = attr.permissions();
            if (perms.contains(GROUP_WRITE) || perms.contains(OTHERS_WRITE)) {
                System.out.println("Cannot use provided proxy config file-- it is not secure.");
            } else {
*/                Scanner jsonstream = new Scanner(configfile);
                jsonstream.useDelimiter("\\Z");
                String jsonTxt = jsonstream.next();
                jsonstream.close();
                
                JSONObject proxysettings = new JSONObject(jsonTxt);
                
                if (proxysettings.has("host"))
                    setHost(proxysettings.getString("host"));
                if (proxysettings.has("port"))
                    setPort(proxysettings.getInt("port"));
                if (proxysettings.has("username"))
                    setUsername(proxysettings.getString("username"));
                if (proxysettings.has("password"))
                    setPassword(proxysettings.getString("password"));               
/*            }
*/    		
    	} catch (Exception e) {
    		this.log.println("ProxyInformation: Proxy config file "+configfile.getAbsolutePath()+" not found. Assuming no proxy server. "+e.toString());
    	}
    }
    
    
    // PUBLIC METHODS

    public static ProxyInformation getInstance() {
        return INSTANCE;
    }
    
    public String getHost() {
        return proxyHost;
    }
    
    public int getPort() {
        return proxyPort;
    }
    
    public String getUsername() {
        return proxyUser;
    }

    public String getPassword() {
        return proxyPass;
    }
    
    public String getNtlmDomain() {
        return proxyNtlmDomain;
    }

    public boolean isProxyUsed() {
        return proxyUsed;
    }

    public boolean isAuthenticationUsed() {
        return authenticationUsed;
    }

    public boolean isNtlmUsed() {
        return NtlmUsed;
    }

    public void setHost(String host) {
        proxyHost = host;
        if (host != null) {
            proxyUsed = true;          
        }
     }
 
    public void setPort(int port) {
        proxyPort = port;       
    }

    public void setUsername(String user) {
        proxyUser = user;       
        if (user != null) {
            authenticationUsed = true;           
        }
    }

    public void setPassword(String pass) {
        proxyPass = pass;       
    }
    
    public void setNtlmDomain(String domain) {
        proxyNtlmDomain = domain;
        if (domain != null) {
            NtlmUsed = true;           
        }
    }

} // class ProxyInformation
