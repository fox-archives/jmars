package edu.asu.jmars.layer.tes6;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import edu.asu.jmars.Main;
import edu.asu.jmars.util.DebugLog;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.constructs.blocking.BlockingCache;

public class CacheKeeper {
	private static DebugLog log = DebugLog.instance();
	
	// Time between consecutive auto-flushes.
	public static final long AUTOFLUSH_PERIOD = 2000L;
	public static final String CACHE_CONFIG = "/edu/asu/jmars/layer/tes6/ehcache.xml";
	public static final String DISK_STORE_ELEMENT = "diskStore";
	public static final String PATH_ATTRIBUTE = "path";
	public static final String TES_CACHE_NAME = "tes";
	
	
	static private CacheKeeper instance = null;
	
	private CacheManager cacheManager;
	private BlockingCache tesCache;
	private Set<Object> cacheUsers;
	private Timer autoflushTimer;
	private Thread shutdownThread;
	
	
	public static CacheKeeper instance(){
		if (instance == null)
			instance = new CacheKeeper();
		return instance;
	}

	// Obtain an instance of the cache
	public synchronized Ehcache checkout(Object checkedOutBy){
		if (cacheUsers.isEmpty()){
			// Create a CacheManager and a Cache
	    	cacheManager = CacheManager.create(getConfigInputStream());
	    	tesCache = new BlockingCache(cacheManager.getCache(TES_CACHE_NAME));
	    	
	    	// Start auto-flush timer task
			autoflushTimer = new Timer("CacheKeeper Autoflush", true);
			autoflushTimer.scheduleAtFixedRate(new AutoFlushTask(tesCache, this), 0, AUTOFLUSH_PERIOD);
		}
		cacheUsers.add(checkedOutBy);
		return tesCache;
	}
	
	// Return the instance of the cache after use for proper shutdown of CacheManager
	public synchronized void checkin(Object checkedOutBy){
		cacheUsers.remove(checkedOutBy);
		if (cacheUsers.isEmpty()){
			// Cacnel auto-flush timer task
			autoflushTimer.cancel();
			// Flush the Cache and shutdown the CacheManager.
			tesCache.flush();
			cacheManager.shutdown();
			
		}
	}
	
	protected CacheKeeper(){
		cacheUsers = new HashSet<Object>();
		
		shutdownThread = new Thread(){
			public void run(){
				// Cancel autoflush timer task
				if (autoflushTimer != null)
					autoflushTimer.cancel();
				
				// Shutdown cache-manager
				log.println("Shutting down cacheManager.");
				if (cacheManager != null){
					cacheManager.shutdown();
				}
			}
		};
		
		Runtime.getRuntime().addShutdownHook(shutdownThread);
	}
	
	private InputStream getConfigInputStream(){
		try {
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			docBuilderFactory.setNamespaceAware(true);
			//docBuilderFactory.setValidating(true);
			//docBuilderFactory.setSchema(schema);

			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			//docBuilder.setEntityResolver()
			Document doc = docBuilder.parse(getClass().getResourceAsStream(CACHE_CONFIG));
			Node diskStoreNode = doc.getDocumentElement().getElementsByTagName(DISK_STORE_ELEMENT).item(0);
			Node pathAttribute = diskStoreNode.getAttributes().getNamedItem(PATH_ATTRIBUTE);
			pathAttribute.setNodeValue(Main.getJMarsPath());

			
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");

			//initialize StreamResult with File object to save to file
			StreamResult result = new StreamResult(new StringWriter());
			DOMSource source = new DOMSource(doc);
			transformer.transform(source, result);

			String xmlString = result.getWriter().toString();
			return new ByteArrayInputStream(xmlString.getBytes());
		}
		catch(Exception ex){
			throw new RuntimeException(ex);
		}
	}

	static class AutoFlushTask extends TimerTask {
		final private Ehcache cache;
		final private Object lock;
		
		public AutoFlushTask(Ehcache cache, Object lock){
			this.cache = cache;
			this.lock = lock;
		}
		
		public void run() {
			synchronized (lock) {
				if (cache.getStatus() == Status.STATUS_ALIVE){
					log.println("Autoflushing.");
					cache.flush();
				}
			}
		}
	};

	public static void main(String[] args){
		CacheKeeper ck = CacheKeeper.instance();
		Object obj = new Object();
		Ehcache c = ck.checkout(obj);
		for(Object key: c.getKeys()){
			System.out.println(key+":"+c.get(key));
		}
		c.put(new Element(new Integer(1), "a"));
		c.put(new Element(new Integer(2), "b"));
		c.put(new Element(new Integer(3), "c"));
		c.put(new Element(new Integer(4), "d"));
		try {
			System.err.println("Sleeping");
			Thread.sleep(5000);
			System.err.println("Coming out of sleep");
		}
		catch(InterruptedException ex){
			ex.printStackTrace();
		}
		ck.checkin(obj);
		
		Object obj2 = new Object();
		Ehcache c2 = ck.checkout(obj2);
		c2.put(new Element(new Integer(1), "a"));
		c2.put(new Element(new Integer(2), "b"));
		try {
			System.err.println("Sleeping");
			Thread.sleep(5000);
			System.err.println("Coming out of sleep");
		}
		catch(InterruptedException ex){
			ex.printStackTrace();
		}
		ck.checkin(obj2);
		
		System.exit(0);
	}
}
