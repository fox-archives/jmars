package edu.asu.jmars.layer.util.features;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import edu.asu.jmars.Main;
import edu.asu.jmars.util.Util;

public class FeatureProviderReadOnly implements FeatureProvider {
	private static final Object lock = new Object();
	private static final String base = Main.getJMarsPath() + "shapes" + File.separator;
	
	private String directory;
	private String file; //.shp
	private String urlString;
	
	public FeatureProviderReadOnly(String dir, String fileName, String URL){
		directory=dir;
		file=fileName;
		urlString=URL;
	}
	
	public String getDirectory() {
		return this.directory;
	}
	public String getFile() {
		return this.file;
	}
	public String getUrlString() {
		return this.urlString;
	}
	public String getDescription() {
		return null;
	}

	public File[] getExistingSaveToFiles(FeatureCollection fc, String baseName) {
		return null;
	}

	public String getExtension() {
		return null;
	}

	public boolean isFileBased() {
		return false;
	}

	public boolean isRepresentable(FeatureCollection fc) {
		return false;
	}
	
	public FeatureCollection load(String fileName) {
		synchronized(lock) {
			try {
				URL url = new URL(urlString);
				ZipInputStream zis = new ZipInputStream(url.openStream());
				for (ZipEntry entry = zis.getNextEntry(); entry != null; entry = zis.getNextEntry()) {
					long time = entry.getTime();
					File out = new File(base + entry.getName());
					if (!out.exists() || out.lastModified() != time) {
						out.getParentFile().mkdirs();
						FileOutputStream fos = new FileOutputStream(out);
						Util.copy(zis, fos);
						fos.flush();
						fos.close();
						out.setLastModified(time);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		if(file.contains(".csv")){
			return new FeatureProviderCSV().load(base + directory + File.separator + file);
		}
		
		return new FeatureProviderSHP(false).load(base + directory + File.separator + file);
	}
	
	public int save(FeatureCollection fc, String fileName) {
		throw new UnsupportedOperationException("This is not supported");
	}

    @Override
    public boolean setAsDefaultFeatureCollection() {
        return false;
    }
}

