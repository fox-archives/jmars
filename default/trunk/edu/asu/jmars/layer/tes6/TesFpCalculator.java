package edu.asu.jmars.layer.tes6;

import java.awt.*;
import java.awt.geom.*;

public interface TesFpCalculator {
    // TODO: add is16DegOff as a parameter and put the 16-deg off as a bit in the fp-id
    public GeneralPath getDetectorOutline(int ock, int ick, int det, float clon, float clat) throws NoInfoException;
    public GeneralPath getFpSixPack(int ock, int ick, float clon, float clat) throws NoInfoException;
    public GeneralPath getFpOutline(int ock, int ick, float clon, float clat) throws NoInfoException;
    public Point2D[]   getDetectorOutlinePoints(int ock, int ick, int det, float clon, float clat) throws NoInfoException;
    public Point2D[][] getFpSixPackPoints(int ock, int ick, float clon, float clat) throws NoInfoException;
    public Point2D[]   getFpOutlinePoints(int ock, int ick, float clon, float clat) throws NoInfoException;
    public Point2D     getDetectorCenter(int ock, int ick, int det, float clon, float clat) throws NoInfoException;
    public void        fillDetectorOutlinePoints(int ock, int ick, int det, float clon, float clat, Point2D[] points) throws NoInfoException;
    public void        fillDetectorCenter(int ock, int ick, int det, float clon, float clat, Point2D center) throws NoInfoException;
    
    public static class NoInfoException extends Exception {
    	public NoInfoException(int ock, int ick, int det, String orbitTable){
    		super("Don't have information about ock "+ock+" in orbit table.");
    		this.ock = ock;
    		this.ick = ick;
    		this.det = det;
    		this.orbitTable = orbitTable;
    	}
    	
    	public int getOck(){ return ock; }
    	public int getIck(){ return ick; }
    	public int getDet(){ return det; }
    	public String getOrbitTable(){ return orbitTable; }
    	
    	public String toString(){
    		String s = getClass().getName()+"[";
    		s += "ock="+ock+",ick="+ick+",det="+det;
    		s += ",orbitTable="+orbitTable;
    		s += "]";
    		return s;
    	}
    	
    	private int ock, ick, det;
    	private String orbitTable;
    }
}

