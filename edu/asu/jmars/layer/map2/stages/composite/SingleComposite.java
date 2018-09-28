// Copyright 2008, Arizona Board of Regents
// on behalf of Arizona State University
// 
// Prepared by the Mars Space Flight Facility, Arizona State University,
// Tempe, AZ.
// 
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
// 
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.


package edu.asu.jmars.layer.map2.stages.composite;

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import edu.asu.jmars.layer.map2.MapAttr;
import edu.asu.jmars.layer.map2.MapData;
import edu.asu.jmars.util.PolyArea;
import edu.asu.jmars.util.Util;

/**
 * Simple composite stage that doesn't really compose - just takes the place of
 * a composite operator when the user just wants to see a single map
 */
public class SingleComposite extends CompositeStage implements Cloneable, Serializable {
	private static final long serialVersionUID = 2L;
	
	public static final String[] inputNames = new String[]{ "Input" };
	
	// This determines what our output image is going to look like.
	// We need this since we don't know what kind of data are we going to get.
	transient private BufferedImage imagePreset = null;
	
	public SingleComposite(SingleCompositeSettings settings){
		super(settings);
	}
	
	public BufferedImage makeBufferedImage(int width, int height) {
		if (imagePreset == null)
			return null;
		
		ColorModel cm = imagePreset.getColorModel();
		if (cm.getNumColorComponents() == 1)
			return createGrayImageWithAlpha(width, height);
		else
			return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
	}
	
	private BufferedImage createGrayImageWithAlpha(int width, int height){
		ColorModel outCM = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY),
				true, false, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);
		WritableRaster raster = outCM.createCompatibleWritableRaster(width, height);
		BufferedImage image = new BufferedImage(outCM, raster, outCM.isAlphaPremultiplied(), null);
		return image;
	}
	
	/** Simply returns the input image */
	public MapData process(int input, MapData data, Area changedArea) {
		if (data.getImage().getColorModel().hasAlpha())
			return data;
		
		// Set an image preset for makeBufferedImage(). We have to do this because we
		// don't know at the outset of SingleComposite image about what kind of input
		// data to expect.
		imagePreset = data.getImage();
		
		// The super method will call makeBufferedImage, which will use imagePreset
		// to determine what image to create; it then sets the alpha
		// samples where we haven't received data to transparent
		// prior to returning.
		MapData outData = super.process(input, data, changedArea);
		
		BufferedImage inputImage = data.getImage();
		inputImage.coerceData(false); // Remove alpha component from RGB values
		ColorModel cm = inputImage.getColorModel();
		
		BufferedImage outImage = outData.getImage();
		int[] bands = getBands(Math.min(cm.getNumComponents(),
			outImage.getColorModel().getNumComponents()));
		
		// Copy color channels to output image
		WritableRaster outRaster = outImage.getRaster();
		outRaster = outRaster.createWritableChild(
				0, 0, outImage.getWidth(), outImage.getHeight(),
				0, 0, bands);
		
		Raster inRaster = inputImage.getRaster();
		inRaster = inRaster.createChild(
				inRaster.getMinX(), inRaster.getMinY(), inRaster.getWidth(), inRaster.getHeight(), 
				inRaster.getMinX(), inRaster.getMinY(), bands);
		
		outRaster.setRect(inRaster);
		
		// set opaque alpha for each piece of the changed area
		WritableRaster alpha = Util.getBands(outImage, outImage.getColorModel().getNumComponents()-1);
		for (Rectangle2D r: new PolyArea(changedArea).getRectangles()) {
			WritableRaster rectAlpha = MapData.getRasterForWorld(alpha, outData.getRequest().getExtent(), r);
			for (int i = rectAlpha.getWidth()-1; i >= 0; i--) {
				for (int j = rectAlpha.getHeight()-1; j >= 0; j--) {
					rectAlpha.setSample(i, j, 0, 255);
				}
			}
		}
		
		return outData;
	}
	
	private int[] getBands(int n){
		int[] bands = new int[n];
		for(int i=0; i<bands.length; i++)
			bands[i] = i;
		
		return bands;
	}
	
	public MapAttr[] consumes(int inputNumber){
		return new MapAttr[]{ MapAttr.COLOR, MapAttr.GRAY };
	}
	
	/** TODO: SingleComposite will be removed, this output type is wrong and will not apply any more. */
	public MapAttr produces() {
		return new MapAttr(MapAttr.NUM_CC_ANY, DataBuffer.TYPE_BYTE);
	}
	
	public String getStageName(){
		return getSettings().getStageName();
	}
	
	public int getInputCount(){
		return inputNames.length;
	}
	
	public String getInputName(int inputNumber){
		return inputNames[inputNumber];
	}
	
	public String[] getInputNames(){
		return (String[])inputNames.clone();
	}
	
	public Object clone() throws CloneNotSupportedException {
		SingleComposite stage = (SingleComposite)super.clone();
		return stage;
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
	}
}
