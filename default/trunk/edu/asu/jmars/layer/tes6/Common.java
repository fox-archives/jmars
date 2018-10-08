package edu.asu.jmars.layer.tes6;

public class Common {

	/**
	 * Convenience class for holding det-id components.
	 * 
	 * @author saadat
	 *
	 */
    public static class DetId {
    	int ock;
    	int ick;
    	int det;
    	
    	public void populateFromDetId(int detId){
            ock = getOck(detId);
            ick = getIck(detId);
            det = getDet(detId);
    	}
    }
    
    /**
     * Convenience class for holding fp-id components.
     * 
     * @author saadat
     *
     */
    public static class FpId {
    	int ock;
    	int ick;
    	boolean offAxis;
    	boolean off16Deg;
    	
    	public void populateFromFpId(int fpId){
            ock      = getOck(fpId);
            ick      = getIck(fpId);
            offAxis  = getOffAxis(fpId);
            off16Deg = getOff16Deg(fpId);
    	}
    }
    
    /**
     * Extracts and returns the ock value from det-id or fp-id.
     * @param anyId
     * @return
     */
    public static int getOck(int anyId){
    	return (int)((anyId & OCK_MASK ) >>> OCK_MASK_SHR);
    }
    
    /**
     * Extracts and returns the ick value from det-id or fp-id.
     * @param anyId
     * @return
     */
    public static short getIck(int anyId){
		return (short)((anyId & ICK_MASK ) >>>  ICK_MASK_SHR);
    }
    
    /**
     * Extracts and returns the detector value from det-id.
     * @param detId
     * @return
     */
	public static byte getDet(int detId){
		return (byte)((detId & DET_MASK ) >>  DET_MASK_SHR);
	}

	/**
	 * Extracts and returns the off-axis flag from fp-id.
	 * @param fpId
	 * @return
	 */
	public static boolean getOffAxis(int fpId){
		return (((fpId & OFF_AXIS_MASK) != 0));
	}

	/**
	 * Extracs and returns the off-16-deg flag from fp-id
	 * @param fpId
	 * @return
	 */
	public static boolean getOff16Deg(int fpId){
		return (((fpId & OFF_16_DEG_FLAG_MASK) != 0));
	}

	/**
	 * Extracts and returns the special-handling flag from
	 * fp-id. Observations that have this flag set must
	 * get their polygons directly from the database.
	 * @param fpId
	 * @return
	 */
	public static boolean getSpHandling(int fpId) {
		return (((fpId & OFF_SP_HANDLE_FLAG_MASK) != 0));
	}

	/**
	 * Extracts and returns the common part between det-id
	 * and fp-id which is basically the ock and the ick values.
	 * @param anyId
	 * @return
	 */
	public static int getDetIdFpIdCommonPart(int anyId){
		return ((anyId & COMMON_ID_MASK) >> COMMON_ID_MASK_SHR);
	}

	/**
	 * Masks and shift-right values to get the value start at
	 * the first bit.
	 */
	public static final int   COMMON_ID_MASK              = 0xFFFFFFF0;
    public static final int   COMMON_ID_MASK_SHR          = 4;
    public static final int   OCK_MASK                    = 0xFFFF0000;
    public static final int   OCK_MASK_SHR                = 16;
    public static final int   ICK_MASK                    = 0x0000FFF0;
    public static final int   ICK_MASK_SHR                = 4;
    public static final int   DET_MASK                    = 0x0000000E;
    public static final int   DET_MASK_SHR                = 1;
    public static final int   OFF_SP_HANDLE_FLAG_MASK     = 0x00000002;
    public static final int   OFF_SP_HANDLE_FLAG_MASK_SHR = 1;
    public static final int   OFF_16_DEG_FLAG_MASK        = 0x00000004;
    public static final int   OFF_16_DEG_FLAG_MASK_SHR    = 2;
    public static final int   OFF_AXIS_MASK               = 0x00000008;
    public static final int   OFF_AXIS_MASK_SHR           = 3;
    
    public static final float BLON_BLAT_SCALE       = 1/1000.0f;

}
