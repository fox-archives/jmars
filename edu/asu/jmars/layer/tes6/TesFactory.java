package edu.asu.jmars.layer.tes6;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import edu.asu.jmars.layer.*;

public class TesFactory extends LViewFactory {
	public TesFactory(){
		type = "tes";
	}
	
	public Layer.LView createLView() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void createLView(boolean async, LayerParameters lp){
		Layer l = new TesLayer();
		Layer.LView v = new TesLView(l);
		
		if (v != null){
			v.setLayerParameters(lp);
			v.originatingFactory = this;
			LManager.receiveNewLView(v);
		}
	}
	
	public Layer.LView recreateLView(SerializedParameters parmBlock) {
		if (parmBlock instanceof TesLView.InitialParams) {
			TesLView.InitialParams initalParams = (TesLView.InitialParams)parmBlock;
			TesLayer l = new TesLayer();
			TesLView v = new TesLView(l);
			v.originatingFactory = this;

			for(TesContext ctx: initalParams.getContexts())
				l.addContext(ctx);

			l.setActiveContext(initalParams.getSelectedContext());

			return v;
		} else {
			return null;
		}
	}

	public String getName(){
		return "TES";
	}
	
	public String getDesc(){
		return "TES";
	}
	
	protected JMenuItem[] createMenuItems() {
		JMenu spectra = new JMenu("Spectra");
		for (JMenuItem item: super.createMenuItems()) {
			spectra.add(item);
		}
		return new JMenuItem[]{spectra};
	}
}
