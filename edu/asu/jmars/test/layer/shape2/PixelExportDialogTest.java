package edu.asu.jmars.test.layer.shape2;

import java.awt.geom.Point2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import edu.asu.jmars.Main;
import edu.asu.jmars.layer.map2.MapServer;
import edu.asu.jmars.layer.map2.MapServerFactory;
import edu.asu.jmars.layer.map2.MapSource;
import edu.asu.jmars.layer.shape2.PixelExportDialog;
import edu.asu.jmars.layer.util.features.FPath;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.Util;


public class PixelExportDialogTest {
	private boolean idBool;
	private boolean latBool;
	private boolean lonBool;
	private boolean nullBool;
	private ArrayList<MapSource> sources;
	private int ppd;
	private FPath path;
	private BufferedWriter writer;
	private volatile static boolean done = false;
	
	private static String file1 = System.getProperty("user.home")+"/pixelExportUnitTest1.csv";
	private static String file2 = System.getProperty("user.home")+"/pixelExportUnitTest2.csv";
	private static String file3 = System.getProperty("user.home")+"/pixelExportUnitTest3.csv";
	
	private static String startingBody;
	
	@BeforeClass
	public static void startUp(){
		//get body before unit tests change body
		startingBody = Util.getProductBodyPrefix().replace(".", "");
		
		//start with a projection centered at 0,0
		Main.startupInCyl("0", "0");
		//start on Earth
		setBody("earth");
    	
    	waitForMaps();
	}
	
	private static void setBody(String body){
		Main.setCurrentBody(body);
    	Config.set(Config.CONFIG_SELECTED_BODY, body);
    	Util.updateRadii();
	}
	
	private static void waitForMaps(){
		done = false;
		
		MapServerFactory.whenMapServersReady(new Runnable() {
			public void run() {
				//Null out the mapservers and loaderThread in order for kickloader to execute 
				MapServerFactory.disposeOfMapServerReferences();
				//call the getMapServers method which we can be sure will only get called once the maps servers have loaded
				MapServerFactory.getMapServers();
				
				MapServerFactory.whenMapServersReady(new Runnable() {
					public void run() {
						done = true;
					}
				});
			}
		});
		
		while(!done){
			try{
				Thread.sleep(1000);
			}catch(InterruptedException e){
				e.printStackTrace();
			}
		}
	}
	

	@Test
	public void testSaveCSV() throws Exception{
		
		// --------- Case 1 ----------------------------------//
		//shape = line
		//source = asterGDEM
		//ppd = 512
		//id = true
		//lat = true
		//lon = true
		//null values = true
		
		//define the shape (a line in this case)
		Point2D[] vertices = new Point2D[2];
		vertices[0] = (new Point2D.Double(115.08984375000003,38.05078124999999));
		vertices[1] = (new Point2D.Double(114.291015625,38.06054687499999));
		path = new FPath(vertices, FPath.SPATIAL_WEST, false);
		//get a source (one from earth for right now)
		sources = new ArrayList<MapSource>();
		MapSource asterSource = null;
		
		for(MapServer server : MapServerFactory.getMapServers()){
			asterSource = server.getSourceByName("asterGDEM");
			if(asterSource != null){
				sources.add(asterSource);
				break;
			}
		}
		idBool = true;
		latBool = true;
		lonBool = true;	//set the values based on the selected body
    	Config.set(Config.CONFIG_SELECTED_BODY, "earth");
		nullBool = true;
		ppd = 512;
		
		File actual = new File(file1);
			
		writer = new BufferedWriter(new FileWriter(actual));
			
		PixelExportDialog ped = new PixelExportDialog(null, null, path);
		
		ped.saveCSV(idBool, latBool, lonBool, nullBool, sources, ppd, path, writer);
			
		//since the saveCSV method kicks off other threads, we need to wait until
		// that is completely finished.  Do that by checking when the file stops 
		// changing size, and is greater than 0.
		long prevSize = actual.length();
		boolean done = false;
		while(!done){
			System.out.println("waiting...");
			//check new size to see if it's the same
			long size = actual.length();
			if(size>0 && size == prevSize){
				done = true;
			}else{
				Thread.sleep(1000);
				prevSize = size;
			}
		}
		
		File expected = new File("edu/asu/jmars/test/layer/shape2/PixelExportResult1.csv");
		
		assertEquals(FileUtils.readLines(expected), FileUtils.readLines(actual));
		
		System.out.println("Done Case 1");
		
		
		//-------Case 2------------------------------//
		//shape = abstract polygon
		//source = asterGDEM, world_maximum_temperature_april_2000, world_precipitation_april_2000
		//ppd = 64
		//id = true
		//lat = true
		//lon = true
		//null values = true
		
		//define the shape (a line in this case)
		vertices = new Point2D[9];
		vertices[0] = (new Point2D.Double(114.31250000000003,38.31249999999999));
		vertices[1] = (new Point2D.Double(115.65625000000004,36.84375));
		vertices[2] = (new Point2D.Double(113.875,35.65624999999999));
		vertices[3] = (new Point2D.Double(110.625,35.65624999999999));
		vertices[4] = (new Point2D.Double(109.87500000000001,37.6875));
		vertices[5] = (new Point2D.Double(112.0,37.437499999999986));
		vertices[6] = (new Point2D.Double(111.78125000000003,38.78125));
		vertices[7] = (new Point2D.Double(113.53125,38.59374999999999));
		vertices[8] = (new Point2D.Double(113.71875,37.59374999999997));
		path = new FPath(vertices, FPath.SPATIAL_WEST, true);
		
		//get sources (three from earth)
		sources = new ArrayList<MapSource>();
		MapSource aprilTemp;
		MapSource aprilPrecip;
		
		for(MapServer server : MapServerFactory.getMapServers()){
			asterSource = server.getSourceByName("asterGDEM");
			aprilTemp = server.getSourceByName("world_maximum_temperature_april_2000");
			aprilPrecip = server.getSourceByName("world_precipitation_april_2000");
			if(asterSource != null){
				sources.add(asterSource);
			}
			if(aprilTemp != null){
				sources.add(aprilTemp);
			}
			if(aprilPrecip != null){
				sources.add(aprilPrecip);
			}
			
			if(sources.size() == 3){
				break;
			}
		}
		
		//sampling and file constraints
		idBool = true;
		latBool = true;
		lonBool = true;
		nullBool = true;
		ppd = 64;
		
		actual = new File(file2);
			
		writer = new BufferedWriter(new FileWriter(actual));
			
		ped = new PixelExportDialog(null, null, path);
			
		ped.saveCSV(idBool, latBool, lonBool, nullBool, sources, ppd, path, writer);
			
		//since the saveCSV method kicks off other threads, we need to wait until
		// that is completely finished.  Do that by checking when the file stops 
		// changing size, and is greater than 0.
		prevSize = actual.length();
		done = false;
		while(!done){
			System.out.println("waiting...");
			//check new size to see if it's the same
			long size = actual.length();
			if(size>0 && size == prevSize){
				done = true;
			}else{
				Thread.sleep(1000);
				prevSize = size;
			}
		}
		
		expected = new File("edu/asu/jmars/test/layer/shape2/PixelExportResult2.csv");
		
		assertEquals(FileUtils.readLines(expected), FileUtils.readLines(actual));
		
		System.out.println("Done Case 2");
		
		
		//-------Case 3------------------------------//
		//shape = triangle
		//source = omega_albedo_r1080, mola_512ppd_npole_topo
		//ppd = 32
		//id = true
		//lat = false
		//lon = falseSystem.out.println("Done Case 1");
		//null values = true
		
		// set body to mars
		setBody("mars");
    	// reload maps
    	waitForMaps();
		
		//define the shape (a line in this case)
		vertices = new Point2D[3];
		vertices[0] = (new Point2D.Double(37.50000000000001,63.500000000000014));
		vertices[1] = (new Point2D.Double(41.468750000000036,49.25000000000001));
		vertices[2] = (new Point2D.Double(27.843749999999996,48.84375000000001));
		path = new FPath(vertices, FPath.SPATIAL_WEST, true);
		
		//get sources (two from mars)
		sources = new ArrayList<MapSource>();
		MapSource albedo;
		MapSource mola;
		
		for(MapServer server : MapServerFactory.getMapServers()){
			albedo = server.getSourceByName("omega_albedo_r1080");
			mola = server.getSourceByName("mola_512ppd_npole_topo");
			if(albedo != null){
				sources.add(albedo);
			}
			if(mola != null){
				sources.add(mola);
			}
			
			if(sources.size() == 2){
				break;
			}
		}
		
		//sampling and file constraints
		idBool = true;
		latBool = false;
		lonBool = false;
		nullBool = true;
		ppd = 32;
		
		
		//output files
		actual = new File(file3);
			
		writer = new BufferedWriter(new FileWriter(actual));
			
		ped = new PixelExportDialog(null, null, path);
		
		ped.saveCSV(idBool, latBool, lonBool, nullBool, sources, ppd, path, writer);
			
		//since the saveCSV method kicks off other threads, we need to wait until
		// that is completely finished.  Do that by checking when the file stops 
		// changing size, and is greater than 0.
		prevSize = actual.length();
		done = false;
		while(!done){
			System.out.println("waiting...");
			//check new size to see if it's the same
			long size = actual.length();
			if(size>0 && size == prevSize){
				done = true;
			}else{
				Thread.sleep(1000);
				prevSize = size;
			}
		}
		
		expected = new File("edu/asu/jmars/test/layer/shape2/PixelExportResult3.csv");
		
		assertEquals(FileUtils.readLines(expected), FileUtils.readLines(actual));
		
		System.out.println("Done Case 3");
	}
	
	
	@AfterClass
	public static void tearDown(){
		//return the body to the original one before unit test was ran
		setBody(startingBody);
		
		//Delete the test files created during the unit tests
		File file = new File(file1);
		file.delete();
		
		file = new File(file2);
		file.delete();
		
		file = new File(file3);
		file.delete();
		
	}
}
