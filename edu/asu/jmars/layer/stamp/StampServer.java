package edu.asu.jmars.layer.stamp;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import edu.asu.jmars.layer.map2.MapServer;
import edu.asu.jmars.layer.map2.MapServerDefault;
import edu.asu.jmars.layer.map2.MapServerFactory;
import edu.asu.jmars.layer.map2.MapServerListener;
import edu.asu.jmars.layer.map2.MapSource;

/**
 * Provides a list of stamp sources from each stamp layers, so the user may get
 * at the list of stamp layers in the advanced dialog and other places.
 */
public final class StampServer extends MapServerDefault implements MapServer {
	private static final StampServer instance = new StampServer();
	private static final URI stampServerURI = URI.create("jmars://stamplayers");
	static {
		MapServerFactory.addNewServer(instance);
	}
	public static StampServer getInstance() {
		return instance;
	}
	
	public static void initializeStampSources() {
		MapServerFactory.addNewServer(instance);
	}
	private final List<MapSource> sources = new ArrayList<MapSource>();
	private transient List<MapServerListener> listeners = new ArrayList<MapServerListener>();
	
	public void add(MapSource source) {
		if (!sources.contains(source)) {
			sources.add(source);
			dispatch(source, MapServerListener.Type.ADDED);
		}
	}
	
	public void remove(String name) {
		MapSource source = getSourceByName(name);
		if (sources.remove(source)) {
			dispatch(source, MapServerListener.Type.REMOVED);
		}
	}
	
	public void remove(MapSource source) {
		if (sources.remove(source)) {
			dispatch(source, MapServerListener.Type.REMOVED);
		}
		remove(source.getName());
	}
	
	private List<MapServerListener> getListeners() {
		if (listeners == null) {
			listeners = new ArrayList<MapServerListener>();
		}
		return listeners;
	}
	
	public void addListener(MapServerListener l) {
		getListeners().add(l);
	}
	
	public void removeListener(MapServerListener l) {
		getListeners().remove(l);
	}
	
	private void dispatch(MapSource source, MapServerListener.Type type) {
		for (MapServerListener l: getListeners()) {
			l.mapChanged(source, type);
		}
	}
	
	public void delete() {
		throw new UnsupportedOperationException("May not remove this map server");
	}

	public List<MapSource> getMapSources() {
		return sources;
	}

	public URI getMapURI() {
		return stampServerURI;
	}

	public int getMaxRequests() {
		return 2;
	}

	public String getName() {
		return "Stamp Layers";
	}

	public MapSource getSourceByName(String name) {
		for (MapSource s: sources) {
			if (s.getName().equals(name)) {
				return s;
			}
		}
		return null;
	}

	public int getTimeout() {
		return 0;
	}

	public String getTitle() {
		return getName();
	}

	public URI getURI() {
		return stampServerURI;
	}

	public boolean isUserDefined() {
		return false;
	}

	public void load(String serverName) {
		throw new UnsupportedOperationException("May not load this map server");
	}

	public void loadCapabilities(boolean cached) {
	}

	public void save() {
		throw new UnsupportedOperationException("May not save this map server");
	}
	
	public String toString(){
		return getTitle();
	}
}

