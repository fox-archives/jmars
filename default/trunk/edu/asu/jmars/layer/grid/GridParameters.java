/**
 * 
 */
package edu.asu.jmars.layer.grid;

import java.awt.Color;

import edu.asu.jmars.layer.SerializedParameters;
import edu.asu.jmars.util.Config;

/**
 * User selections from the LL Grid panel to be saved/loaded with sessions/layers.
 *
 * This is a serializable object so that it can be saved during a SAVE SESSION or
 * SAVE LAYER operation by the user.  It can then be loaded in a similar fashion
 * by LOAD operations.
 *
 */

public class GridParameters implements SerializedParameters {

    private static final long serialVersionUID = 1687855554601081823L;

    Integer majorColor         = 0;   // RGB value
    Double  majorSpacing       = 0.0;
    Boolean majorMainVisible   = true;
    Boolean majorPannerVisible = true;
    Integer majorWidth         = 1;

    Integer minorColor         = 0;   // RGB value
    Double  minorSpacing       = 0.0;
    Boolean minorMainVisible   = true;
    Boolean minorPannerVisible = true;
    Integer minorWidth         = 1;

    public GridParameters() {
       majorColor         = new Integer(new Color(Config.get("gridcolor",  0)).getRGB());
       majorSpacing       = new Double(10.0);
       majorMainVisible   = new Boolean(true);
       majorPannerVisible = new Boolean(true);
       majorWidth         = new Integer(1);

       minorColor         = new Integer(Color.gray.getRGB());
       minorSpacing       = new Double(2.0);
       minorMainVisible   = new Boolean(false);
       minorPannerVisible = new Boolean(false);
       minorWidth         = new Integer(1);

    }

}
