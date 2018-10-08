package edu.asu.jmars.layer.tes6;

import java.io.*;
import java.util.*;
import java.awt.*;
import java.net.*;
import java.awt.geom.*;
import edu.asu.jmars.layer.tes6.*;
import edu.asu.jmars.util.DebugLog;

public class TesInterpolatedFpCalculator implements TesFpCalculator {

	private static DebugLog log = DebugLog.instance();
	
	private static String orbitTblName = "/edu/asu/jmars/layer/tes6/orbit-tbl.txt";
	private static String centerTblName = "/edu/asu/jmars/layer/tes6/perfect-5555-spill-under+over.txt";
	private static String cornerOff0TblName = "/edu/asu/jmars/layer/tes6/det-offsets.txt";
	private static String cornerOff16TblName = "/edu/asu/jmars/layer/tes6/det-offsets-16deg.txt";
	
    // constructor
    protected TesInterpolatedFpCalculator(){
        fillOrbitTbl(orbitTblName);
        fillCenterTbl(centerTblName);
        fillDetCornerOffTbl(cornerOff0TblName, cornerOff16TblName);
    }

    // implement a singleton
    private static TesInterpolatedFpCalculator instance = null;
    public static TesInterpolatedFpCalculator getInstance(){
        if (instance == null){
            instance = new TesInterpolatedFpCalculator();
        }
        return instance;
    }
    
    // interface function implementation
    public Point2D getDetectorCenter(int ock, int ick, int det, float clon, float clat) throws NoInfoException{
    	Point2D center = null;
    	
    	// TODO: Make the following points local to the object, so that we have working space.
    	Point2D[] corners = { new Point2D.Double(), new Point2D.Double(), new Point2D.Double(), new Point2D.Double() };
    	
    	if (getDetectorCorners(ock,ick,det-1,clon,clat, corners) == false){
        	throw new NoInfoException(ock,ick,det,orbitTblName);
        }
    	
    	double px = 0, py = 0;
    	if (corners != null){
    		for(int i = 0; i < 4; i++){
    			px += corners[i].getX();
    			py += corners[i].getY();
    		}
    		px /= 4;
    		py /= 4;
    		
    		center = new Point2D.Float((float)px, (float)py);
    	}
    	
    	return center;
    }

    public void fillDetectorCenter(int ock, int ick, int det, float clon, float clat, Point2D center)
    	throws NoInfoException{

    	// TODO: Make the following points local to the object, so that we have working space.
    	Point2D[] corners = { new Point2D.Double(), new Point2D.Double(), new Point2D.Double(), new Point2D.Double() };
    	
    	if (getDetectorCorners(ock,ick,det-1,clon,clat,corners) == false){
        	throw new NoInfoException(ock,ick,det,orbitTblName);
        }
    	
    	double px = 0, py = 0;
		for(int i = 0; i < 4; i++){
			px += corners[i].getX();
			py += corners[i].getY();
		}
		px /= 4;
		py /= 4;
		
		center = new Point2D.Float((float)px, (float)py);
    }

    // interface function implementation
    public GeneralPath getDetectorOutline(int ock, int ick, int det, float clon, float clat) throws NoInfoException{
        GeneralPath poly = null;

        // TODO: Computes corners of all four detectors, which consumes more space and CPU.
        Point2D[][] corners = getDetectorCornersAllDetectors(ock, ick, clon, clat);
        if (corners == null){
        	throw new NoInfoException(ock,ick,det,orbitTblName);
        }
        if (corners != null){
            poly = new GeneralPath();
            poly.moveTo((float)corners[det-1][0].getX(), (float)corners[det-1][0].getY());
            poly.lineTo((float)corners[det-1][1].getX(), (float)corners[det-1][1].getY());
            poly.lineTo((float)corners[det-1][2].getX(), (float)corners[det-1][2].getY());
            poly.lineTo((float)corners[det-1][3].getX(), (float)corners[det-1][3].getY());
            poly.closePath();
        }
        return poly;
    }
    
    // interface function implementation
    public Point2D[] getDetectorOutlinePoints(int ock, int ick, int det, float clon, float clat) throws NoInfoException{
        // TODO: Computes corners of all four detectors, which consumes more space and CPU.
    	Point2D[] corners = { new Point2D.Double(), new Point2D.Double(), new Point2D.Double(), new Point2D.Double() };
        if (getDetectorCorners(ock, ick, det-1, clon, clat, corners) == false){
        	throw new NoInfoException(ock,ick,det,orbitTblName);
        }
        
        return corners;
    }

    /**
     * Fill the given Point2D[4] corners with detector corners.
     * 
     * @param ock
     * @param ick
     * @param det
     * @param clon
     * @param clat
     * @param corners
     * @throws NoInfoException
     */
    public void fillDetectorOutlinePoints(int ock, int ick, int det, float clon, float clat, Point2D[] corners) throws NoInfoException{
        // TODO: Computes corners of all four detectors, which consumes more space and CPU.
        if (getDetectorCorners(ock, ick, det-1, clon, clat, corners) == false){
        	throw new NoInfoException(ock,ick,det,orbitTblName);
        }
    }

    // interface function implementation
    public GeneralPath getFpSixPack(int ock, int ick, float clon, float clat) throws NoInfoException{
        GeneralPath poly = null;
        Point2D[][] corners = getDetectorCornersAllDetectors(ock, ick, clon, clat);
        if (corners == null){
        	throw new NoInfoException(ock,ick,0,orbitTblName);
        }
        if (corners != null){
            poly = new GeneralPath();
            for(int i = 0; i < 6; i++){
                poly.moveTo((float)corners[i][0].getX(), (float)corners[i][0].getY());
                poly.lineTo((float)corners[i][1].getX(), (float)corners[i][1].getY());
                poly.lineTo((float)corners[i][2].getX(), (float)corners[i][2].getY());
                poly.lineTo((float)corners[i][3].getX(), (float)corners[i][3].getY());
                poly.closePath();
            }
        }
        return poly;
    }

    // interface function implementation
    public Point2D[][] getFpSixPackPoints(int ock, int ick, float clon, float clat) throws NoInfoException{
        Point2D[][] corners = getDetectorCornersAllDetectors(ock, ick, clon, clat);
        if (corners == null){
        	throw new NoInfoException(ock,ick,0,orbitTblName);
        }
        
        return corners;
    }
    
    // interface function implementation
    public GeneralPath getFpOutline(int ock, int ick, float clon, float clat) throws NoInfoException{
        return new GeneralPath();
    }

    // interface function implementation
    public Point2D[] getFpOutlinePoints(int ock, int ick, float clon, float clat) throws NoInfoException{
        return new Point2D[0];
    }


    // INTERNAL FUNCTIONS

    /**
     * Compute corners of all detectors for the given TES orbit and ick.
     */
	private boolean getDetectorCorners(int orbit, int ick, int det, float clon, float clat, Point2D[] corners){
		if (orbitTbl == null || orbit >= orbitTbl.length || orbitTbl[orbit] == null){ 
            Integer o = new Integer(orbit);
            if (!postedDontHaveOrbitInfo.contains(o)){
                postedDontHaveOrbitInfo.add(o);
                log.println("Don't have information about ock "+orbit+" in orbit-table.");
            }
            return false;
		}

		/*
		** Our perfect-orbit (i.e. reference orbit stored in centerTbl) may be 
		** longer/shorter than the orbit in question. The following scale value
		** fixes the icks to reflect this scale difference.
		*/
		double ickScalingFactor = centerTblOrbitLength / orbitTbl[orbit].orbitLength;
		double lonOffset = orbitTbl[orbit].lonOffset;
		double ickZeroOffset = orbitTbl[orbit].ickZeroOffset;

        /*
        ** ock 12856 and above have |pnt_angle| ~ 16-degrees because the spacecraft
        ** was flying off-axis.
        */
        int    pnt_angle_16 = (orbit >= 12856)? 1: 0;

		/*
        ** Compute the scaled ick. The ickZeroOffset is the distance in perfect icks
        ** that the perfect-zero-ick is from the dequax-ick.
        */
		double scaledIck = centerTblBaseIck + (ick + ickZeroOffset) * ickScalingFactor;

		/*
		** Get the low ick number which brackets the scaled-ick value. These bracketing
		** ick numbers are brLowIck=floor(scaledIck) and brHighIck=ceil(scaledIck).
		*/
		int brLowIck = (int)scaledIck;

        if (brLowIck < 0){
            log.aprintln("centerTbl underflow occurred at brLowIck="+brLowIck+" #centerTbl="+centerTbl.length);
            brLowIck = 0;
        }
		if ((brLowIck+1) >= centerTbl.length){
			log.aprintln("centerTbl overflow occurred at brLowIck="+brLowIck+" #centerTbl="+centerTbl.length);
			brLowIck = centerTbl.length - 2;
		}

		/*
		** Compute the closeness (t) of scaled ick from the bracketing icks.
		** It is (scaledIck - brLowIck)/(brHighIck - brLowIck).
		** However, since the brHighIck = brLowIck + 1 therefore, the
		** denominator is not shown as part of this computation.
		** This acts as weight when the (lon,lat) is computed as a linear 
		** combination of the (lon,lat)-pairs at the low and high icks.
		*/
		double tIck = (scaledIck - (double)brLowIck);
		int    ascending = centerTbl[brLowIck].ascending; /* ascending/descending leg of orbit track */

		// System.err.println("C: "+ick+" "+(clon+lonOffset)+" "+clat);

		/*
		** The center-lat computed above is used to find the corners
		** of the 6 detectors. This is done by locating the bracketing
		** center-lats in the "corner offsets" (cornerOffTbl) table.
		** The corner-offsets table is populated at a fixed resolution
		** given by cornerOffTblLatRes. Thus one can computationally
		** determine the bracketing latitude indices.
		**
		** NOTE:
		** For all practical purposes the qlat (or query-latitude) and
		** lat (or effective-latitude) are the same. At least upto the
		** TES maximum and minimum latitudes hit. Beyond that they
		** deviate.
		*/
		int cornerOffTblIdx = (int)((clat - cornerOffTbl[pnt_angle_16][ascending][0].qlat) / cornerOffTblLatRes);
		if (cornerOffTblIdx >= cornerOffTbl[pnt_angle_16][ascending].length){
			/* If corner offset table index overflows, bring it back within range */
			log.aprint("cornerOffTblIdx overflow occurred with value "+ cornerOffTblIdx);
			cornerOffTblIdx = cornerOffTbl[pnt_angle_16][ascending].length - 2;
			log.aprintln(" clamped to value "+ cornerOffTblIdx);
		}

		double tLat = (clat - cornerOffTbl[pnt_angle_16][ascending][cornerOffTblIdx].qlat) / cornerOffTblLatRes;
		CornerOffTblEntry brLowOffEntry = cornerOffTbl[pnt_angle_16][ascending][cornerOffTblIdx];
		CornerOffTblEntry brHighOffEntry = cornerOffTbl[pnt_angle_16][ascending][cornerOffTblIdx+1];

		getInterpolatedCorners(brLowOffEntry, brHighOffEntry, tLat, det, clat, clon, corners);
		
		return true;
	}

	/**
     * Compute corners of all detectors for the given TES orbit and ick.
     */
	private Point2D[][] getDetectorCornersAllDetectors(int orbit, int ick, float clon, float clat){
		if (orbitTbl == null || orbit >= orbitTbl.length || orbitTbl[orbit] == null){ 
            Integer o = new Integer(orbit);
            if (!postedDontHaveOrbitInfo.contains(o)){
                postedDontHaveOrbitInfo.add(o);
                log.println("Don't have information about ock "+orbit+" in orbit-table.");
            }
            return null;
		}

		/*
		** Our perfect-orbit (i.e. reference orbit stored in centerTbl) may be 
		** longer/shorter than the orbit in question. The following scale value
		** fixes the icks to reflect this scale difference.
		*/
		double ickScalingFactor = centerTblOrbitLength / orbitTbl[orbit].orbitLength;
		double lonOffset = orbitTbl[orbit].lonOffset;
		double ickZeroOffset = orbitTbl[orbit].ickZeroOffset;

        /*
        ** ock 12856 and above have |pnt_angle| ~ 16-degrees because the spacecraft
        ** was flying off-axis.
        */
        int    pnt_angle_16 = (orbit >= 12856)? 1: 0;

		/*
        ** Compute the scaled ick. The ickZeroOffset is the distance in perfect icks
        ** that the perfect-zero-ick is from the dequax-ick.
        */
		double scaledIck = centerTblBaseIck + (ick + ickZeroOffset) * ickScalingFactor;

		/*
		** Get the low ick number which brackets the scaled-ick value. These bracketing
		** ick numbers are brLowIck=floor(scaledIck) and brHighIck=ceil(scaledIck).
		*/
		int brLowIck = (int)scaledIck;

        if (brLowIck < 0){
            log.aprintln("centerTbl underflow occurred at brLowIck="+brLowIck+" #centerTbl="+centerTbl.length);
            brLowIck = 0;
        }
		if ((brLowIck+1) >= centerTbl.length){
			log.aprintln("centerTbl overflow occurred at brLowIck="+brLowIck+" #centerTbl="+centerTbl.length);
			brLowIck = centerTbl.length - 2;
		}

		/*
		** Compute the closeness (t) of scaled ick from the bracketing icks.
		** It is (scaledIck - brLowIck)/(brHighIck - brLowIck).
		** However, since the brHighIck = brLowIck + 1 therefore, the
		** denominator is not shown as part of this computation.
		** This acts as weight when the (lon,lat) is computed as a linear 
		** combination of the (lon,lat)-pairs at the low and high icks.
		*/
		double tIck = (scaledIck - (double)brLowIck);
		int    ascending = centerTbl[brLowIck].ascending; /* ascending/descending leg of orbit track */

		// System.err.println("C: "+ick+" "+(clon+lonOffset)+" "+clat);

		/*
		** The center-lat computed above is used to find the corners
		** of the 6 detectors. This is done by locating the bracketing
		** center-lats in the "corner offsets" (cornerOffTbl) table.
		** The corner-offsets table is populated at a fixed resolution
		** given by cornerOffTblLatRes. Thus one can computationally
		** determine the bracketing latitude indices.
		**
		** NOTE:
		** For all practical purposes the qlat (or query-latitude) and
		** lat (or effective-latitude) are the same. At least upto the
		** TES maximum and minimum latitudes hit. Beyond that they
		** deviate.
		*/
		int cornerOffTblIdx = (int)((clat - cornerOffTbl[pnt_angle_16][ascending][0].qlat) / cornerOffTblLatRes);
		if (cornerOffTblIdx >= cornerOffTbl[pnt_angle_16][ascending].length){
			/* If corner offset table index overflows, bring it back within range */
			log.aprint("cornerOffTblIdx overflow occurred with value "+ cornerOffTblIdx);
			cornerOffTblIdx = cornerOffTbl[pnt_angle_16][ascending].length - 2;
			log.aprintln(" clamped to value "+ cornerOffTblIdx);
		}

		double tLat = (clat - cornerOffTbl[pnt_angle_16][ascending][cornerOffTblIdx].qlat) / cornerOffTblLatRes;
		CornerOffTblEntry brLowOffEntry = cornerOffTbl[pnt_angle_16][ascending][cornerOffTblIdx];
		CornerOffTblEntry brHighOffEntry = cornerOffTbl[pnt_angle_16][ascending][cornerOffTblIdx+1];

		Point2D[][] corners = new Point2D[6][4];
		for(int det = 0; det < 6; det++){
			Point2D[] detCorners = getInterpolatedCorners(brLowOffEntry, brHighOffEntry, tLat, det, clat, clon);
			corners[det] = detCorners;
		}

		return corners;
	}

    /**
     * Compute corners of all detectors for the given TES orbit and ick.
     * This call obtains the center of the foot-print from the centerTable
     * by way of interplation.
     * <p>
     * This call is no longer is in use and will be removed.
     */
	private Point2D[][] getDetectorCornersAllDetectors(int orbit, int ick){
		if (orbitTbl == null || orbit >= orbitTbl.length || orbitTbl[orbit] == null){ 
			log.println("Don't have information about ock "+orbit+" in orbit-table.");
			return null;
		}

		/*
		** Our perfect-orbit (i.e. reference orbit stored in centerTbl) may be 
		** longer/shorter than the orbit in question. The following scale value
		** fixes the icks to reflect this scale difference.
		*/
		double ickScalingFactor = centerTblOrbitLength / orbitTbl[orbit].orbitLength;
		double lonOffset = orbitTbl[orbit].lonOffset;
		double ickZeroOffset = orbitTbl[orbit].ickZeroOffset;
        int    pnt_angle_16 = (orbit >= 12856)? 1: 0;

		// Compute the scaled ick. The ickZeroOffset is the distance in perfect icks
		// that the perfect-zero-ick is from the dequax-ick.
		double scaledIck = (centerTblBaseIck + ick + ickZeroOffset) * ickScalingFactor;

		/*
		** Get the low ick number which brackets the scaled-ick value. These bracketing
		** ick numbers are brLowIck=floor(scaledIck) and brHighIck=ceil(scaledIck).
		*/
		int brLowIck = (int)scaledIck;

        if (brLowIck < 0){
            log.aprintln("centerTbl underflow occurred at brLowIck="+brLowIck+" #centerTbl="+centerTbl.length);
            brLowIck = 0;
        }
		if ((brLowIck+1) >= centerTbl.length){
			log.aprintln("centerTbl overflow occurred at brLowIck="+brLowIck+" #centerTbl="+centerTbl.length);
			brLowIck = centerTbl.length - 2;
		}

		/*
		** Compute the closeness (t) of scaled ick from the bracketing icks.
		** It is (scaledIck - brLowIck)/(brHighIck - brLowIck).
		** However, since the brHighIck = brLowIck + 1 therefore, the
		** denominator is not shown as part of this computation.
		** This acts as weight when the (lon,lat) is computed as a linear 
		** combination of the (lon,lat)-pairs at the low and high icks.
		*/
		double tIck = (scaledIck - (double)brLowIck);

		double clat = centerTbl[brLowIck].lat * (1.0 - tIck) + centerTbl[brLowIck+1].lat * tIck;
		double clon = centerTbl[brLowIck].lon * (1.0 - tIck) + centerTbl[brLowIck+1].lon * tIck;
		int    ascending = centerTbl[brLowIck].ascending; /* ascending/descending leg of orbit track */

		// System.err.println("C: "+ick+" "+(clon+lonOffset)+" "+clat);

		/*
		** The center-lat computed above is used to find the corners
		** of the 6 detectors. This is done by locating the bracketing
		** center-lats in the "corner offsets" (cornerOffTbl) table.
		** The corner-offsets table is populated at a fixed resolution
		** given by cornerOffTblLatRes. Thus one can computationally
		** determine the bracketing latitude indices.
		**
		** NOTE:
		** For all practical purposes the qlat (or query-latitude) and
		** lat (or effective-latitude) are the same. At least upto the
		** TES maximum and minimum latitudes hit. Beyond that they
		** deviate.
		*/
		int cornerOffTblIdx = (int)((clat - cornerOffTbl[pnt_angle_16][ascending][0].qlat) / cornerOffTblLatRes);
		if (cornerOffTblIdx >= cornerOffTbl[pnt_angle_16][ascending].length){
			/* If corner offset table index overflows, bring it back within range */
			log.aprint("cornerOffTblIdx overflow occurred with value "+ cornerOffTblIdx);
			cornerOffTblIdx = cornerOffTbl[pnt_angle_16][ascending].length - 2;
			log.aprintln(" clamped to value "+ cornerOffTblIdx);
		}

		double tLat = (clat - cornerOffTbl[pnt_angle_16][ascending][cornerOffTblIdx].qlat) / cornerOffTblLatRes;
		CornerOffTblEntry brLowOffEntry = cornerOffTbl[pnt_angle_16][ascending][cornerOffTblIdx];
		CornerOffTblEntry brHighOffEntry = cornerOffTbl[pnt_angle_16][ascending][cornerOffTblIdx+1];

		Point2D[][] corners = new Point2D[6][4];
		for(int det = 0; det < 6; det++){
			Point2D[] detCorners = getInterpolatedCorners(brLowOffEntry, brHighOffEntry, tLat, det, clat, clon+lonOffset);
			corners[det] = detCorners;
		}

		return corners;
	}

	private double pLon(double lon){
		if (lon > 360){ lon = lon - 360.0; }
		if (lon < 0){ lon = 360.0 + lon; }

		return lon;
	}

	private void getInterpolatedCorners(
			CornerOffTblEntry e0,
			CornerOffTblEntry e1,
			double         t,
			int            det,
			double         clat,
			double         clon,
			Point2D[]      corners) // assumed Point2D[4]
		{
			double    offLon, offLat;

			clon = pLon(clon);
			for(int j = 0; j < 4; j++){
				offLon = e0.offsets[det][j][0]*(1.0-t)+e1.offsets[det][j][0]*t;
				offLat = e0.offsets[det][j][1]*(1.0-t)+e1.offsets[det][j][1]*t;
				corners[j] = new Point2D.Double(clon+offLon, clat+offLat);
			}
		}

	private Point2D[] getInterpolatedCorners(
		CornerOffTblEntry e0,
		CornerOffTblEntry e1,
		double         t,
		int            det,
		double         clat,
		double         clon)
	{
		double    offLon, offLat;
		Point2D[] corners = new Point2D[4];

		clon = pLon(clon);
		for(int j = 0; j < 4; j++){
			offLon = e0.offsets[det][j][0]*(1.0-t)+e1.offsets[det][j][0]*t;
			offLat = e0.offsets[det][j][1]*(1.0-t)+e1.offsets[det][j][1]*t;
			corners[j] = new Point2D.Double(clon+offLon, clat+offLat);
		}

		return corners;
	}

    /**
     * Fills orbit parameters table. This table contains one entry 
     * per orbit. Information contained is:
     * &lt
     * ock, orbit-length, lon0, ick-zero-offset
     * &gt
     * <p>
     * <em>ock</em> is the TES orbit number.
     * <em>orbit-length</em> is the length of the orbit in ET.
     * <em>lon0</em> is the 
     * <em>ick-zero-offset</em> is the slop in the start of
     * this orbit. Its units are icks and is calculated as 
     * half of the difference between the orbit-start-et and
     * equator-crossing-et.
     * <p>
     * Note: This table is generated as follows:
     * <p>
     * <verb>
     * ~/tes/gt/gen_perfect_orbit_locs
     *       -k tes_kernels.txt
     *       -ockrange 5555 10000
     *       -donthone
     *       -boundsonly &gt orbit-tbl-5555-10000.txt
     * </verb>
     * <p>
     * The table contains additonal fields which are discarded 
     * on reading.
     */
	private boolean fillOrbitTbl(String fileName){
		try {
			
			LineNumberReader reader = new LineNumberReader(
					new InputStreamReader(
							getClass().getResourceAsStream(fileName)));
			String line;
			Vector v = new Vector(4000);
			int maxOck = 0;

			while((line = reader.readLine()) != null){
				if (reader.getLineNumber() == 1){ continue; }
				line = line.trim(); if (line.equals("")){ continue; }

				String[] pcs = line.split("\\s+");
				if (pcs.length != 9){
					throw new RuntimeException("All records in input file must have 9 fields in them.");
				}

				int ock = Integer.parseInt(pcs[1]);
				double orbitLength = Double.parseDouble(pcs[3])-Double.parseDouble(pcs[2]); /* ed_et - st_et */
				float  lon0 = Float.parseFloat(pcs[5]); /* lon0 */
				float  ickZeroOffset = (float)(Double.parseDouble(pcs[4])-Double.parseDouble(pcs[2]))/2.0f; /* (dequax_et - st_et)/2.0 */

				if (orbitLength <= 0){
					log.println("Skipping ock "+ock+" with invalid length "+orbitLength+".");
				}
				else {
					OrbitTblRec r = new OrbitTblRec(ock, orbitLength, lon0, ickZeroOffset);
					v.add(r);
				}

				if (ock > maxOck){ maxOck = ock; }
			}

			orbitTbl = new OrbitTblRec[maxOck+1];

			for(int i = 0; i < v.size(); i++){
				OrbitTblRec r = (OrbitTblRec)v.get(i);
				orbitTbl[r.orbit] = r;
			}
		}
		catch(IOException ex){
			System.err.println(ex);
			ex.printStackTrace();
			return false;
		}

		return true;
	}

    /**
     * Fills foot-print center table, calculates the foot-print
     * table length in time, and the ick-base (which is the
     * offset in the table to reach the ick-zero entry).
     * <p>
     * Each entry in the table has the following format:
     * <p>
     * &lt
     * orbit, ock, ick, et, pos0, pos1, pos2, lat, lon, lon0,
     * ascending
     * &gt
     * <p>
     * Where (pos0,pos1,pos2) is the position vector in
     * IAU_MARS, (lat,lon,lon0) are the spacecraft position 
     * latitude, longitude and longitude minus longitude-at-ick-zero,
     * and ascending is is a boolean =1 for ascending leg
     * and =0 for descending leg.
     * <p>
     * The table has many icks prior to ick-zero and many icks
     * after the ock has ended so that ick-slippage is taken
     * care of. Ick-slippage is the relative difference in 
     * location of an ick when compared to its location in 
     * the reference/standard orbit. Note that different
     * orbits can different ick lengths.
     * <p>
     * Note: This table is generated as follows:
     * <p>
     * <verb>
     * ~/tes/gt/gen_perfect_orbit_locs
     *    -ockrange 5555 5555
     *    - k tes_kernels.txt
     *    -donthone
     *    -spillover 50
     *    -spillunder 50
     *    -useboresightlat
     *    /tes/mapping/data/documentation/equator_crossings
     *    starting-ick2.txt
     *
     * </verb>
     */
	private boolean fillCenterTbl(String fileName){
		try {
			LineNumberReader reader = new LineNumberReader(
					new InputStreamReader(
							getClass().getResourceAsStream(fileName)));
			String line;
			Vector v = new Vector(5000);
			double st_et = 0.0, ed_et = 0.0;
            double lon0, lat;
            int    ascending;
            int    first_ick = 0;

			while((line = reader.readLine()) != null){
				line = line.trim(); if (line.equals("")){ continue; }

				String[] pcs = line.split("\\s+");
				if (pcs.length != 11){
					throw new RuntimeException("All records in input file must have 11 fields in them.");
				}

				if (reader.getLineNumber() > 1){ // skip first line which is the header
                    if (reader.getLineNumber() == 2){ first_ick = Integer.parseInt(pcs[2]); }

					if (pcs[2].equals("0")){ st_et = Double.parseDouble(pcs[3]); }

					// if "ick" has the special value "END" - get the ending ET from it.
					if (pcs[2].equals("END")){ ed_et = Double.parseDouble(pcs[3]); continue; }

                    lon0 = Double.parseDouble(pcs[9]);
                    lat = Double.parseDouble(pcs[7]);
                    ascending = Integer.parseInt(pcs[10]);
					CenterTblEntry e = new CenterTblEntry(lon0, lat, ascending);
					v.add(e);
				}
			}

			centerTbl = new CenterTblEntry[v.size()];
			v.toArray(centerTbl);

			centerTblOrbitLength = ed_et - st_et;
            centerTblBaseIck = -first_ick;
		}
		catch(IOException ex){
			System.err.println(ex);
			ex.printStackTrace();
			return false;
		}

		return true;
	}

    /**
     * Fills detector corner offset table for on-axis nadir pointing and
     * 16-deg off-axis nadir pointing standard/reference orbits. Each table
     * has two legs: an ascending leg and a descending leg
     * corresponding to whether the spacecraft was going from low latitude
     * to high latitude or vice-versa. The legs are sorted in ascending
     * order on the query-center-latitude. The entries are regularly spaced
     * in query-center-latitude.
     * <p>
     * Each entry in this table has the following format::
     * <p>
     * &lt query-center-latitude, center-longitude, response-center-latitude,
     * d1c1lon, d1c1lat, d1c2lon, d1c2lat, ... d1c4lon, d1c4lat, ...
     * d6c4lon, d6c4lat &gt .
     * <p>
     * The corners are listed in the clock-wise direction starting from
     * the top-left detector corner (as per the detector six-pack).
     * <p>
     * Note: The detectors are numbered as follows:
     * <p>
     * <verb>
     * +---+---+---+
     * | 1 | 2 | 3 |
     * +---+---+---+
     * | 4 | 5 | 6 |
     * +---+---+---+
     * </verb>
     * <p>
     * This table is generated by:
     * <p>
     * <verb>
     * ~/tes/gt/gen_det_offsets
     *     -k tes_kernels.txt
     *     -lat0et 1543923.591354
     *     -etstep 0.2
     *     -latstep 0.1
     *     -p 0 &gt det-offset.txt
     *
     * ~/tes/gt/gen_det_offsets
     *     -k tes_kernels.txt
     *     -lat0et 1543923.591354
     *     -etstep 0.2
     *     -latstep 0.1
     *     -p -15.984375 &gt det-offset-16.txt
     * </verb>
     * <p>
     */
	private boolean fillDetCornerOffTbl(String fileName, String fileName16){
		/**
		 ** ASSUMPTIONS:
		 ** 1. Input records are presorted in ascending order.
		 ** 2. Same number of input records are present for ascending and descending
		 **    legs of the orbit.
		 ** 3. Records are of the following format:
		 **    (qlat,lon,lat,ascending,d1c1lon,d1c1lat,...d6c4lon,d6c4lat)
		 **    with white-space as field separator.
		 **/

        String[] fileNames = new String[] { fileName, fileName16 };
        Vector[][] v = new Vector[2][2]; // 1st index is for i, 2nd index 0=dsc, 1=asc

        for(int pta16 = 0; pta16 < 2; pta16++){ // i=0: pnt_angle=0; i=1: pnt_angle=16
            v[pta16][0] = new Vector(3600);
            v[pta16][1] = new Vector(3600);
            
            try {
                LineNumberReader reader = new LineNumberReader(
                		new InputStreamReader(
                				getClass().getResourceAsStream(fileNames[pta16])));
                String line;
                String[] pcs = null;
                double qlat, lat, lon;
                int    asc;
                float[][][] corner_offsets;
                int i, j, k, m;
                
                while((line = reader.readLine()) != null){
                    line = line.trim(); if (line.equals("")){ continue; }
                    
                    if (reader.getLineNumber() > 1){ // skip first line which is the header
                        pcs = line.split("\\s+");
                        
                        qlat = Double.parseDouble(pcs[0]);
                        lon = Double.parseDouble(pcs[1]);
                        lat = Double.parseDouble(pcs[2]);
                        asc  = Integer.parseInt(pcs[3]);
                        corner_offsets = new float[6][4][2];
                        
                        m = 4; // location of the first det corner
                        for(i = 0; i < 6; i++){
                            for(j = 0; j < 4; j++){
                                for(k = 0; k < 2; k++){
                                    corner_offsets[i][j][k] = Float.parseFloat(pcs[m++]);
                                }
                            }
                        }
                        
                        CornerOffTblEntry e = new CornerOffTblEntry(qlat, lat, lon, corner_offsets);
                        v[pta16][asc].add(e);
                    }
                }

            }
            catch(IOException ex){
                System.err.println(ex);
                ex.printStackTrace();
                return false;
            }

        }

        if (v[0][0].size() != v[0][1].size() || v[0][0].size() != v[1][0].size() || v[0][0].size() != v[1][1].size()){
            System.err.println("Error! Corner offset tables are of different sizes.");
            return false;
        }

        // alloc cornerOffTbl and store collected data in it
        cornerOffTbl = new CornerOffTblEntry[2][2][v[0][0].size()];

        v[0][0].toArray(cornerOffTbl[0][0]);
        v[0][1].toArray(cornerOffTbl[0][1]);
        v[1][0].toArray(cornerOffTbl[1][0]);
        v[1][1].toArray(cornerOffTbl[1][1]);

		// store the lat resolution of the table
		cornerOffTblLatRes = cornerOffTbl[0][0][1].qlat - cornerOffTbl[0][0][0].qlat;

		return true;
	}

	/**
	 * Orbit-table-record: Holds various pieces of information about one TES-ock.
	 */
	final class OrbitTblRec {
		public OrbitTblRec(int orbit, double orbitLength, float lonOffset, float ickZeroOffset){
			this.orbit = orbit;
			this.orbitLength = orbitLength;
			this.lonOffset = lonOffset;
			this.ickZeroOffset = ickZeroOffset;
		}

		/** The "ock" value stored in the tes-vanilla tables. */
		public int    orbit;

		/** Length of perfect-orbit in ET (seconds). */
		public double orbitLength;    // length of perfect-orbit in ET (seconds)

		/**
		 * lon@perfect-ick0 (i.e. perfect-eqx)
		 * our reference orbit as stored in centerTbl is normalized to
		 * start at lon=0 at lat=0. The actual orbit's lon at lat=0
		 * is stored in lonOffset. Thus:
		 * <code> actual_lon = centerTbl[ick][1]+lonOffset </code>
		 */
		public float  lonOffset;

		/**
		 * Difference between perfect-ick0 (or prefect-eqx) and actual ick0 (or DEQUAX).
		 * <code> ickOffset = (first_ick*2.0 - et@perfect_eqx)/2.0 </code>
		 */
		public float  ickZeroOffset;

		/**
		 * Base-value to be added to the ick number if ick0 of the orbit
		 * has been numbered starting at a higher value.
		 * For example ock 12563's first ick starts at a higher number than
		 * zero. Even though it is equivalent to ick0. Sigh!
		 */
		// public int    ickBase;
	}

	/**
	 * Orbit table.
	 * There is one record per ock, directly indexable.
	 * The entry is set to "null" if we don't have data
	 * about this orbit.
	 */
	private OrbitTblRec[] orbitTbl = null;
	

	/**
	 * Center table entry: Stores an entry of the center table for a given ick.
	 * A center table entry is composed of the center longitude,
	 * center latitude and an ascending/descending orbit leg
	 * identifier. All these values are w.r.t. to a particular
	 * ick.
	 */
	final class CenterTblEntry {
		public CenterTblEntry(double lon, double lat, int ascending){
			this.lon = lon;
			this.lat = lat;
			this.ascending = ascending;
		}
		public double   lon;
		public double   lat;

		/**
		 * Ascending/descending leg of the orbit.
		 * TES orbits traverse lats as follows:
		 * <code> 0..-45..-90..-45..0..45..90..45..0 </code>
		 * Though they never hit +/- 90 degrees.<p>
		 * When ascending=1 the (lon,lat) belongs to an
		 * ick on the <code> -90..-45..0..45..90 </code>
		 * leg of the orbit.<p>
		 * When ascending=0 the (lon,lat) belongs to an
		 * ick on the <code> 0..-45..-90 </code> or
		 * <code> 90..45..0 </code> leg of the orbit.
		 */
		public int      ascending;
	}

	/**
	 * Footprint centers table. Contains center 
	 * (lon,lat) coordinates of foot-prints for a 
	 * perfect nadir-looking orbit at every ick.
	 * The table contains one entry per ick. The table may
	 * contain entries past the end of the perfect-orbit to
	 * cater for the actual orbits which tend to spill over
	 * the lat=0 due to the predicted nature of equator crossings.
	 * This orbit serves as a reference orbit that we'll use to
	 * compute foot-print data.
	 */
	private CenterTblEntry[] centerTbl= null;

	/**
	 * Length of the reference orbit stored in centerTbl.
	 * Please note that it is not the difference of the first
	 * and the last entry in the centerTbl, since the centerTbl
	 * may contain entries past the last perfect-ick of the
	 * perfect table.
	 */
	private double centerTblOrbitLength = 0; // center-table ref-orbit's length in ET

    /**
     * Base ick number to be added to the index while accessing an
     * ick in the center-table.
     *
     * The center-table may not start at ick zero, it may instead start
     * at a negative ick number. The negative icks handle the orbits
     * that start in the vanilla database before the equator crossing.
     * The equator crossing in this case is the perfect-ick@lat0.
     * For example ock 5614 which has two icks before the equator
     * crossing.<p>
     * <code>centerTbl[qick+centerTblBaseIck]</code> to get center (lon,lat)
     * at ick given by "qick".
     */
    private int    centerTblBaseIck   = 0;


	/**
	 * Stores an entry of the detector corners offsets table at a given latitude.
	 */
	final class CornerOffTblEntry {
		public CornerOffTblEntry(double qlat, double lat, double lon, float[][][] offsets){
			this.qlat = qlat;
			this.lat = lat;
			this.lon = lon;

			this.offsets = new float[6][4][2];
			System.arraycopy(offsets, 0, this.offsets, 0, offsets.length);
		}

		/**
		 * Latitude value that was queried upon when fetching detector corner 
		 * offsets by way of interpolation. This value goes all the way to 
		 * +/- 90 degrees. Compare this value to the lat value below which
		 * does only to the maximum latitutde achieved by a TES orbit.<p>
		 * <note>Use this value while querying.</note>
		 */
		public double qlat;

		/**
		 * Interpolated latitude value at the given query latitude.
		 * This is also referred to as the response latitude.
		 */
		public double lat;

		/**
		 * Interpolated longitude value at the given query latitude.
		 * This is also referred to as the response longitude.
		 */
		public double lon;

		/**
		 * Interpolated detector corner offsets at the given query
		 * latitude. There are detector offsets for all six detectors,
		 * four corners each detector, two values each corner.
		 * The array is organized as a 6x4x2 array.
		 */
		public float[][][] offsets;
	}

	/**
	 * Detector corners offsets table.
	 * Stores detector corner offsets
	 * from the foot-print center. The table is organized in 
	 * two parts. One for descending leg of the orbit and the
	 * other for the ascending leg of the orbit. Each leg is
	 * organized from latitudes of -90..0..90 at a fixed
	 * resolution, say, 0.5 degrees apart. This resolution is
	 * stored in the corner offset table latitude resolution
	 * value below. <p>
     * First index:  0: pnt_angle=0;       1: pnt_angle=16<p>
     * Second index: 0: descending track;  1: ascending track<p>
     * Third index:  values separated by latitude of cornerOffTblLatRes<p>
	 * @see CenterTblEntry#ascending
	 */
	private CornerOffTblEntry[][][] cornerOffTbl = null;

	/**
	 * Latitudenal resolution of the detector corners offsets table.
	 * <code> cornerOffTbl[0][0][1].lat-cornerOffTbl[0][0][0].lat = cornerOffTblLatRes </code>
	 */
	private double cornerOffTblLatRes = 0.0f;

    /**
     * A list of posted events about orbits that we don't have info about.
     * So that they are notified to the user once and once only. It is
     * annoying to see them multiple times.
     */
    private Set postedDontHaveOrbitInfo = new HashSet();

}
