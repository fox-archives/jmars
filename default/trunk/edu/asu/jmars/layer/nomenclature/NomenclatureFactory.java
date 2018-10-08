package edu.asu.jmars.layer.nomenclature;

import edu.asu.jmars.layer.*;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.util.*;
import java.util.*;

import javax.swing.*;

public class NomenclatureFactory extends LViewFactory {
	
	public NomenclatureFactory(){
		type = "nomenclature";
	}
	
	private static DebugLog log = DebugLog.instance();

        public static class NomenclatureParameterBlock extends DialogParameterBlock {


        }

    	/**
    	 * Overriding the superclass method
    	 * 
	 * @since change bodies
    	 */
    	public Layer.LView showByDefault() {
		if (Config.get(Util.getProductBodyPrefix()
				+ Config.CONFIG_SHOW_NOMENCLATURE_DEFAULT, false)) {
    			// Create the LView instance
    			Layer.LView view = new NomenclatureLView(null);
    			view.originatingFactory = this;
    			return view;
    		} else {// end change bodies
    			// do this for now.
    			return null;
    		}
    	}

	// Implement the main factory entry point.
	public void createLView(boolean async, LayerParameters l) {
        // Create a default set of parameters
        NomenclatureParameterBlock pars = new NomenclatureParameterBlock();
        LView view = realCreateLView();
        view.setLayerParameters(l);
		LManager.receiveNewLView(view);
	 }

	// Internal utility method
	private Layer.LView realCreateLView() {
		// Create a BackLView
		Layer.LView view = new NomenclatureLView(null);
                view.originatingFactory = this;
		view.setVisible(true);

		return  view;
	 }

        //used to restore a view from a save state
        public Layer.LView recreateLView(SerializedParameters parmBlock) {
            return realCreateLView();
        }

	// Supply the proper name and description.
	public String getName() {
		return ("Nomenclature");
	 }
	public String getDesc() {
		return("A layer which provides geographic nomenclature");
	 }
 }
