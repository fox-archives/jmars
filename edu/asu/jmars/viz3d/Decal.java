/**
 * 
 */
package edu.asu.jmars.viz3d;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.imageio.ImageIO;
import com.jogamp.opengl.GL;

import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.opengl.util.texture.Texture;

import edu.asu.jmars.util.Util;
import edu.asu.jmars.viz3d.BoundingCorners;
import edu.asu.jmars.viz3d.Visual3D.Mesh;

public class Decal {
    public static final int DECAL_VERTEX_DATA = 0;
	public static final int DECAL_TEXTURE_DATA = 1;
	public static final int DECAL_INDEX_DATA = 2;
	
	int[] bufferObj = new int[3];
	float[] vertices = null;
	int[] triangles = null;
	float[] texCoords = null;
	BufferedImage image = null;
	BoundingCorners corners = null;
	Texture decalTexture = null;
	FloatBuffer decalVerts = null; 
	FloatBuffer decalTex = null;
	IntBuffer decalTris = null;
	Mesh mesh = null;
	double minX, maxX = 0.0;
	
	boolean hasBeenDisplayed = false;
	Point2D center;
	
	public Decal(BufferedImage img, Point2D min, Point2D max, Point2D center) {
		
		this.center = center;
		
    	corners = new BoundingCorners(min, max);	
    	
    	minX = corners.minX;
    	maxX = corners.maxX;
    	
    	corners.pad(0.05); //0.10);
		
		image = createTransparentBorderImage(img);
		
		Point2D pt = new Point2D.Double(center.getX() - 90., Math.abs(center.getY() - 90.));
		
		this.center = pt;
		
	}

	public BufferedImage createTransparentBorderImage(BufferedImage img) {
		BufferedImage startSlice = null;
		BufferedImage endSlice = null;
		if (corners.getPixelsToTrim() > 0) {
			int newWidth = img.getWidth() - corners.getPixelsToTrim();
			BufferedImage trimmed = img.getSubimage(0, 0, newWidth, img.getHeight());			
			img = trimmed;			
		}
		
		double org_width = img.getWidth();
		
		double widthAdder = img.getWidth() * 0.05; //0.10; 
		double heightAdder = img.getHeight() * 0.05; //0.10; 
		
		// the far right end of the pad will start to overlap the far left edge of the original image
		double adderW = (maxX - minX) * 0.05;
		double tempWidthW = maxX - minX + adderW;
		if (tempWidthW > 360.0) {
			double slice = tempWidthW - 360.0;
			double percentToCopy = slice / (maxX - minX); 
			
			double sliceInPixels = img.getWidth() * percentToCopy;
			
			startSlice = img.getSubimage(0, 0, (int)sliceInPixels, img.getHeight());
			
			endSlice = img.getSubimage(img.getWidth() - (int)sliceInPixels, 0, (int)sliceInPixels, img.getHeight());
			corners.setWrap(true);
		}
		
		BufferedImage tmp = new BufferedImage(img.getWidth() + (2 * (int)widthAdder), img.getHeight() + (2 * (int)heightAdder),
				BufferedImage.TYPE_INT_ARGB);

		Graphics2D g = (Graphics2D) tmp.getGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);

		g.setColor(new Color(0, 0, 0, 0));
		g.fillRect(0, 0, tmp.getWidth(), tmp.getHeight());
		
		g.drawImage(img, null, (int)widthAdder, (int)heightAdder);
		
		if (startSlice != null) {
			Graphics2D gr = (Graphics2D) tmp.getGraphics();
			gr.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);
			
			gr.drawImage(startSlice, null, tmp.getWidth() - startSlice.getWidth(), 0 + (int)heightAdder);
			gr.drawImage(endSlice, null, 0, 0 + (int)heightAdder);
		}

		return tmp;
		
	}
	
	public void loadTexture(GL gl) {
        if (image != null){
        	// get the texture
        	try {
        		
        		final ByteArrayOutputStream output = new ByteArrayOutputStream() {
        		    @Override
        		    public synchronized byte[] toByteArray() {
        		        return this.buf;
        		    }
        		};
        		
        		ImageIO.write(image, "png", output);
        		InputStream dstream = new ByteArrayInputStream(output.toByteArray());
        		
        		TextureData decalData = TextureIO.newTextureData(gl.getGLProfile(), dstream, false, "png");

        		decalTexture = TextureIO.newTexture(decalData);
        	}
        	catch (IOException exc) {
        		exc.printStackTrace();
        	}
        }		
	}
	
	public int[] getBufferObj() {
		return bufferObj;
	}
	public void setBufferObj(int[] bufferObj) {
		this.bufferObj = bufferObj;
	}
	public float[] getVertices() {
		return vertices;
	}
	public void setVertices(float[] verts) {
		vertices = verts;
	}
	public int[] getIndices() {
		return triangles;
	}

	public void setIndices(int[] tris) {
		triangles = tris;
	}
	
	public float[] getTexCoords() {
		return texCoords;
	}

	public void setTexCoords(float[] texCoords) {
		this.texCoords = texCoords;
	}

	public Texture getDecalTexture() {
		return decalTexture;
	}

	public void setDecalTexture(Texture decalTexture) {
		this.decalTexture = decalTexture;
	}

	public FloatBuffer getDecalVerts() {
		if (decalVerts == null) {
			decalVerts = FloatBuffer.wrap(this.vertices);
		}
		return decalVerts;
	}

	public FloatBuffer getDecalTex() {
		if (decalTex == null) {
			decalTex = FloatBuffer.wrap(this.texCoords);
		}
		return decalTex;
	}

	public IntBuffer getDecalTris() {
		if (decalTris == null) {
			decalTris = IntBuffer.wrap(this.triangles);
		}
		return decalTris;
	}

	public BufferedImage getImage() {
		return image;
	}
	public BoundingCorners getCorners() {
		return corners;
	}
	public Mesh getMesh() {
		return mesh;
	}

	public void setMesh(Mesh mesh) {
		this.mesh = mesh;
	}

	public int getWidth() {
		if (image != null) {
			return image.getWidth();
		} else {
			return 0;
		}
	}
	public int getHeight() {
		if (image != null) {
			return image.getHeight();
		} else {
			return 0;
		}
	}
	
	public void dispose() {
		bufferObj = null;
		vertices = null;
		triangles = null;
		image = null;
		corners = null;
		decalTexture = null;
		decalVerts = null; 
		decalTex = null;
		decalTris = null;
		mesh = null;
		texCoords = null;
	}
}
