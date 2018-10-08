package edu.asu.jmars.layer.tes6;

import java.lang.reflect.Array;
import java.util.Collection;

public class Utils {

    /**
     * Reorders the given array of objects in the order specified.
     * Both the input parameters are of same length.
     * 
     * @param objs  Objects to be reordered.
     * @param order Desired order of the objects.
     */
	public static Object[] reorderObjects(Object objs[], int[] order){
		if (objs == null){
			return null;
		}
		
		if (objs.length != order.length){
			throw new IllegalArgumentException("objs and order differ in the number of elements");
		}
		
		Object[] ordered = (Object[])objs.clone();
		
		for(int i = 0; i < order.length; i++){
			Array.set(ordered, i, Array.get(objs, order[i]));
		}
		
		return ordered;
	}
	
    /**
     * Reorders the given array of objects in the order specified.
     * Both the input parameters are of same length.
     * 
     * @param objs  Objects to be reordered.
     * @param order Desired order of the objects.
     */
    public static int[] reorderObjects(int[] objs, int[] order){
    	if (objs == null){
    		return null;
    	}
    	
    	if (objs.length != order.length){
    		throw new IllegalArgumentException("objs and order differ in the number of elements");
    	}
    	
        int ordered[] = new int[objs.length];

        for(int i = 0; i < order.length; i++){
            ordered[i] = objs[order[i]];
        }
        return ordered;
    }
    
    /**
     * Reorders the given array of objects in the order specified.
     * Both the input parameters are of same length.
     * 
     * @param objs  Objects to be reordered.
     * @param order Desired order of the objects.
     */
    public static short[] reorderObjects(short[] objs, int[] order){
    	if (objs == null){
    		return null;
    	}
    	
    	if (objs.length != order.length){
    		throw new IllegalArgumentException("objs and order differ in the number of elements");
    	}
    	
        short ordered[] = new short[objs.length];

        for(int i = 0; i < order.length; i++){
            ordered[i] = objs[order[i]];
        }
        return ordered;
    }

    public static boolean containsSpectralFields(Collection<FieldDesc> fields){
    	for(FieldDesc f: fields)
    		if (f.isSpectralField())
    			return true;
    	return false;
    }


}
