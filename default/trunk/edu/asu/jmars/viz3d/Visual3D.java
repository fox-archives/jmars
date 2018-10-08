package edu.asu.jmars.viz3d;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUquadric;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.math.VectorUtil;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.newt.event.awt.AWTKeyAdapter;
import com.jogamp.newt.event.awt.AWTMouseAdapter;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;

import edu.asu.jmars.Main;
import edu.asu.jmars.ProjObj;
import edu.asu.jmars.layer.threed.Vec3dMath;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.HVector;
import edu.emory.mathcs.backport.java.util.Arrays;

/*
 * Notes:
 * http://www.sjbaker.org/steve/omniv/opengl_lighting.html
 * 
 * glMaterial and glLight
   The OpenGL light model presumes that the light that reaches your eye from the polygon surface 
   arrives by four different mechanisms:
    AMBIENT - light that comes from all directions equally and is scattered in all directions
     equally by the polygons in your scene. This isn't quite true of the real world - but it's
     a good first approximation for light that comes pretty much uniformly from the sky and 
     arrives onto a surface by bouncing off so many other surfaces that it might as well be uniform.
    DIFFUSE - light that comes from a particular point source (like the Sun) and hits surfaces 
      with an intensity that depends on whether they face towards the light or away from it. 
      However, once the light radiates from the surface, it does so equally in all directions. 
      It is diffuse lighting that best defines the shape of 3D objects.
    SPECULAR - as with diffuse lighting, the light comes from a point souce, but with specular 
      lighting, it is reflected more in the manner of a mirror where most of the light bounces 
      off in a particular direction defined by the surface shape. Specular lighting is what 
      produces the shiney highlights and helps us to distinguish between flat, dull surfaces 
      such as plaster and shiney surfaces like polished plastics and metals.
    EMISSION - in this case, the light is actually emitted by the polygon - equally in all directions. 
 *
 */

public class Visual3D extends GLJPanel implements GLEventListener {
	
	private static Visual3D singleton = null;
	private ProjObj currentProj;
	
    private static final long serialVersionUID = 1L;
    private static DebugLog log = DebugLog.instance();
    private static String version = null;
    private static String gluVersion = null;
    
	GLU glu;
	GL2 gl;
	int shader, program = 0;
	private float alpha = 90f;  // angle about x axis
	private float beta = 0;	// angle about y axis
	private float zoomFactor = 0.88f;//0.00128f; // 1f; //500f;
	private float ZOOM_INC = 0.25f;
	private int prevMouseX;
	private int prevMouseY;
    private Texture decalTexture;
    private int decalTextureHeight;
    private int decalTextureWidth;
    private BufferedImage decalImage = null; // screen capture from the main view
    private boolean glColorMaterialTest = false;
    private float ambient = 0.2f, diffused = 1f, specular = 0f; // lighting default
    private float mSpecular = 0.8f, mDiffused = 0.3f; // material properties
    private FloatBuffer vBuf = null, vcBuf = null, nBuf = null, texBuf = null, tvBuf = null, gridVerts = null, gridColor = null;
    private IntBuffer tBuf = null, polyBuf = null; 
    private boolean applyDecal = true;
    private boolean applyMesh = true;
    private boolean drawGrid = false;
    private Mesh meshData = null;
    
    private float transX = 0.0f;
    private float transY = 0.0f;
    private float transZ = 0.0f;
	float gamma = 0f; 
	static enum Direction {VERT, HORZ, NONE};    
    
	private final int SHAPE_VERTEX_DATA = 1;
//	private final int TEXTURE_DATA = 2;
	private final int SHAPE_INDEX_DATA = 2;
	private final int SHAPE_NORMAL_DATA = 3;
	private final int SHAPE_COLOR_DATA = 4;
	private int[] bufferObjs = new int[5];
	private boolean VBO = true;
    private static boolean isRQ36 = false;
    enum MeshType { MESH_DSK, MESH_GASKELL, MESH_TRI_VERTEX };
    JFrame parent = null;
    ArrayList<Decal> decals = new ArrayList<Decal>();
    ArrayList<Decal> incomingDecals = new ArrayList<Decal>();
    ArrayList<ActionListener> actions = new ArrayList<ActionListener>();
    
    GLUquadric quadric = null;
    
    boolean clear = false;
    boolean light = true;
    
    File meshZipFile;
    
    boolean pole = false;
    boolean isDefaultShapeModel = false;
	float[] lightSpecular1 =	 { 1.0f, 1.0f, 1.0f, 1.0f };
	float[] lightSpecular2 =	 { 0.5f, 0.5f, 0.5f, 1.0f };
	float[] lightDiffuse =	 { 1.0f, 1.0f, 1.0f, 1.0f };
	float[] lightAmbient =	 { 0.5f, 0.5f, 0.5f, 1.0f };
	float[] lightPosition1 =	 { 10.0f, 10.0f, -5.0f, 0.0f };
	float[] lightPosition2 =	 { -10.0f, -10.0f, 0.0f, 0.0f };
	float[]	matSpecular = { 1.0f, 1.0f, 1.0f, 1.0f };
	float[] matShininess = { 10.0f };	
    
	float[] lightEmission =	 { 0.2f, 0.2f, 0.2f, 1.0f };
    
    private Visual3D(int width, int height, String kernelFile, String body, JFrame parent, File meshFile, boolean defaultShapeModelFlag) throws IOException {
    	super(Visual3D.createGLCapabilities());
        setSize(width, height);
        addGLEventListener(this);
        
   		isRQ36 = false;
        meshZipFile = meshFile;
        this.parent = parent;    
        currentProj = Main.PO;
        this.isDefaultShapeModel = defaultShapeModelFlag;//must be above the computeVertexColors() method
        loadMeshFromZippedBinary(meshZipFile);
		this.meshData.calcMinMaxAvgMagnitudes();
		this.meshData.computeVertexColors();
		if (isDefaultShapeModel) {
			light = true;
		} else {
			light = false;
		}
        
    }
    
    private static GLCapabilities createGLCapabilities() {
        GLCapabilities capabilities = new GLCapabilities(GLProfile.getDefault());
        capabilities.setRedBits(8);
        capabilities.setBlueBits(8);
        capabilities.setGreenBits(8);
        capabilities.setAlphaBits(8);
        return capabilities;
    }
    
    public void addDecal(Decal decal) {
    	if (Main.PO != currentProj && meshData != null) {
    		
			currentProj = Main.PO;
    		vertsToWorldHVector(meshData.vertices, meshData.worldCoords);    	
    	}
    	incomingDecals.add(decal);
    	this.repaint();
    }
    
    public static void clearSingleton() {
    	Visual3D.singleton = null;
    }
	public void init(GLAutoDrawable drawable) {
    	glu = new GLU();
    	gl = drawable.getGL().getGL2();
    	drawable.setGL(gl);
    	
        writeStats(drawable);
   	
       // Enable z- (depth) buffer for hidden surface removal. 
        gl.glClearDepth(1.0f);                      // Depth Buffer Setup
        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glDepthFunc(GL.GL_LEQUAL);
        
        // Enable smooth shading.
        gl.glShadeModel(GL2.GL_SMOOTH);
        
        // We want a nice perspective...maybe???
        gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_NICEST);
 
		gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_AMBIENT, lightAmbient, 0);
		gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPECULAR, lightSpecular1, 0);
		gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, lightDiffuse, 0);
		gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, lightPosition1, 0);

		gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_AMBIENT, lightAmbient, 0);
		gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_SPECULAR, lightSpecular1, 0);
		gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_DIFFUSE, lightDiffuse, 0);
		gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_POSITION, lightPosition2, 0);

		gl.glEnable(GL2.GL_COLOR_MATERIAL);
		gl.glColorMaterial(GL2.GL_FRONT_AND_BACK, GL2.GL_AMBIENT_AND_DIFFUSE);
		
		gl.glMaterialfv(GL.GL_FRONT, GL2.GL_SPECULAR, lightSpecular1, 0);
		gl.glMaterialfv(GL.GL_FRONT, GL2.GL_SHININESS, matShininess, 0);
		gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL2.GL_EMISSION, lightEmission, 0);
		gl.glLightModeli(GL2.GL_LIGHT_MODEL_LOCAL_VIEWER, GL.GL_TRUE);
		
		if (VBO) {
			gl.glGenBuffers(5, bufferObjs, 0);
			for (Decal d : decals) {
				gl.glGenBuffers(3, d.getBufferObj(), 0);
			}
		}
        // Define "clear" color.
        gl.glClearColor(0f, 0f, 0f, 1f);   
        


        
		for (Decal d : decals) {
			d.loadTexture(gl);
		}
		
		try {
			createShapeMeshBuffers(gl);
			
			for (Decal d : decals) {
				if (d.getMesh() == null) {
					d.setMesh(meshData);
				}
				checkTriangleTextures(gl, d);
				createDecalBuffers(gl, d);
			}
			
		}
		catch(Exception ex){
		        log.aprint(ex);
		        JOptionPane.showMessageDialog(parent,
		                "Error processing shape model or image data: " + ex.getLocalizedMessage() + ".",
		                "Shape Model/Image Error",
		                JOptionPane.ERROR_MESSAGE);
		        writeStats(drawable);
		        return;
		}
		
		// Create A Pointer To The Quadric Object (Return 0 If No Memory)
        quadric = glu.gluNewQuadric();
        glu.gluQuadricNormals(quadric, GLU.GLU_SMOOTH);  // Create Smooth Normals
        glu.gluQuadricTexture(quadric, true);            // Create Texture Coords


	    MouseListener simpleMouse = new SimpleMouseAdapter();
	    KeyListener simpleKeys = new SimpleKeyAdapter();
	
	    if (drawable instanceof Window) {
	        Window window = (Window) drawable;
	        window.addMouseListener(simpleMouse);
	        window.addKeyListener(simpleKeys);
	    } else if (GLProfile.isAWTAvailable() && drawable instanceof java.awt.Component) {
	        java.awt.Component comp = (java.awt.Component) drawable;
	        new AWTMouseAdapter(simpleMouse, drawable).addTo(comp); 
	        new AWTKeyAdapter(simpleKeys, drawable).addTo(comp);
	    }
			
		int errCode = GL2.GL_NO_ERROR;
	    if ((errCode = gl.glGetError()) != GL2.GL_NO_ERROR) {
	       String errString = glu.gluErrorString(errCode);
	       log.println("OpenGL Error: "+errString);
	       JOptionPane.showMessageDialog(parent,
	                "Error preparing data for rendering: " + errString + ".",
	                "Graphics Processing Error",
	                JOptionPane.ERROR_MESSAGE);
	    }    
        
	}

	public void dispose(GLAutoDrawable drawable) {
		if (vBuf != null) { 
			vBuf.clear();
		}
		if (vcBuf != null) { 
			vcBuf.clear();
		}
		if (nBuf != null) { 
			nBuf.clear();
		}
		if (texBuf != null) { 
			texBuf.clear();
		}
		if (tvBuf != null) {
			tvBuf.clear();
		}
		if (gridVerts != null) { 
			gridVerts.clear();
		}
		if (gridVerts != null) { 
			gridColor.clear();
		}
		if (tBuf != null) { 
			tBuf.clear();
		}
		if (polyBuf != null) { 
			polyBuf.clear();
		}
	    vBuf = null;
	    vcBuf = null;
	    nBuf = null;
	    texBuf = null;
	    tvBuf = null;
	    gridVerts = null;
	    gridColor = null;
	    tBuf = null;
	    polyBuf = null; 
	    for (ActionListener al : actions) {
	    	al.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "close"));
	    }
	}
	
	public void clearDecals() {
		clear = true;
		drawGrid = false;
		this.repaint();
	}

	public void display(GLAutoDrawable drawable) {
		final GL2 gl = drawable.getGL().getGL2();
    	drawable.setGL(gl);
    	
    	if (clear) {
    		for (Decal d : decals) {
				d.getDecalTexture().disable(gl);
				d.getDecalTexture().destroy(gl);
				d.dispose();
    		}
    		decals.clear();
    		clear = false;
    	}
    	
		for (Decal d : incomingDecals) {
			gl.glGenBuffers(3, d.getBufferObj(), 0);
			d.loadTexture(gl);
			checkTriangleTextures(gl, d);
			createDecalBuffers(gl, d);
			decals.add(d);
		}
		incomingDecals.clear();

        for (Decal d : decals) {
        	if (!d.hasBeenDisplayed) {
        		gamma = (float)d.center.getX();
        		alpha = (float)d.center.getY();
        		d.hasBeenDisplayed = true;
        	}
        }
        
    	// clear screen
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        gl.glClearColor(0f, 0f, 0f, 1f);   

       	gl.glPushMatrix();

        // setup the camera
      	setCamera(gl, glu, meshData.maxLen * 3f, transX, transY, transZ);
        
    	// Set light parameters.
		gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, lightPosition1, 0);        
		gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_POSITION, lightPosition2, 0);        
		
		gl.glRotatef(-beta,0.0f,1.0f,0.0f);             	// rotate around y-axis
		gl.glRotatef(-alpha,1.0f,0.0f,0.0f);          	  	// rotate around x-axis
		gl.glRotatef(gamma,0.0f,0.0f,1.0f);          	  	// rotate around z-axis
		
        // Prepare light parameters.
		if (light) {
			gl.glEnable(GL2.GL_LIGHTING);
			gl.glEnable(GL2.GL_LIGHT0);
//			gl.glEnable(GL2.GL_LIGHT1);
		} else {
			gl.glDisable(GL2.GL_LIGHTING);
		}
		        
        gl.glEnable(GL2.GL_CULL_FACE);
		gl.glCullFace(GL2.GL_BACK);
    	gl.glFrontFace(GL2.GL_CCW);
        
			
			gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
			gl.glEnableClientState(GL2.GL_NORMAL_ARRAY);
			gl.glEnableClientState(GL2.GL_COLOR_ARRAY);
			gl.glEnableClientState(GL2.GL_INDEX_ARRAY);
		if (VBO) {
			if (applyMesh) {		
				// Vertices
				gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
				gl.glBindBuffer( GL2.GL_ARRAY_BUFFER, bufferObjs[SHAPE_VERTEX_DATA] );
				gl.glVertexPointer( 3, GL2.GL_FLOAT, 0, 0 );
				
				// Normals.
				gl.glBindBuffer( GL.GL_ARRAY_BUFFER, bufferObjs[SHAPE_NORMAL_DATA] );
				gl.glNormalPointer( GL2.GL_FLOAT, 0, 0 );
				
				// Colors
				gl.glBindBuffer( GL.GL_ARRAY_BUFFER, bufferObjs[SHAPE_COLOR_DATA] );
				gl.glColorPointer(3, GL2.GL_FLOAT, 0, 0 );
				
				// Indices
				gl.glBindBuffer( GL.GL_ELEMENT_ARRAY_BUFFER, bufferObjs[SHAPE_INDEX_DATA] );
				
				gl.glDrawElements(GL2.GL_TRIANGLES, meshData.triangles.length, GL2.GL_UNSIGNED_INT, 0);
				gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
			} 
			
		} else {
			if (applyMesh) {	
				gl.glVertexPointer(3, GL2.GL_FLOAT, 0, vBuf);
				gl.glColorPointer(3, GL2.GL_FLOAT, 0, vcBuf);
				gl.glDrawElements(GL2.GL_TRIANGLES, meshData.triangles.length, GL2.GL_UNSIGNED_INT, tBuf);
			}
		}

		if (drawGrid) {
			
			float[] v = meshData.vertices;
			int [] t = decals.get(0).triangles;
			for (int i=0; i<t.length; i+=3) {
				this.drawLine(gl, v[t[i]*3], v[t[i]*3+1], v[t[i]*3+2],
						v[t[i+1]*3+0], v[t[i+1]*3+1], v[t[i+1]*3+2],
						v[t[i+2]*3+0], v[t[i+2]*3+1], v[t[i+2]*3+2],							
						1, 1, 0, 0, 1);
			}
		    gl.glColor3f( 1, 1, 1);
		}

		gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
		gl.glDisableClientState(GL2.GL_NORMAL_ARRAY);
		gl.glDisableClientState(GL2.GL_COLOR_ARRAY);
		gl.glDisableClientState(GL2.GL_INDEX_ARRAY);			
		
		if (applyDecal == true && decals.size() > 0) {
	        gl.glEnable(GL2.GL_CULL_FACE);
			gl.glCullFace(GL2.GL_BACK);
	    	gl.glFrontFace(GL2.GL_CCW);
			gl.glEnable(GL.GL_BLEND);
			gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
			for (Decal d : decals) {
				d.getDecalTexture().enable(gl);
				d.getDecalTexture().bind(gl);
				gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
				gl.glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
				gl.glEnableClientState(GL2.GL_INDEX_ARRAY);			
				if (VBO) {
					gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
					gl.glBindBuffer( GL2.GL_ARRAY_BUFFER, d.getBufferObj()[Decal.DECAL_VERTEX_DATA] );
					gl.glVertexPointer( 3, GL2.GL_FLOAT, 0, 0 );
					
					gl.glBindBuffer( GL.GL_ARRAY_BUFFER, d.getBufferObj()[Decal.DECAL_TEXTURE_DATA] );
					gl.glTexCoordPointer( 2, GL2.GL_FLOAT, 0, 0 );
									
					// Indices
					gl.glBindBuffer( GL.GL_ELEMENT_ARRAY_BUFFER, d.getBufferObj()[Decal.DECAL_INDEX_DATA] );
					gl.glDrawElements(GL2.GL_TRIANGLES, d.getIndices().length, GL2.GL_UNSIGNED_INT, 0);
				} else {
					gl.glVertexPointer(3, GL2.GL_FLOAT, 0, tvBuf);
					gl.glTexCoordPointer(2, GL2.GL_FLOAT, 0, texBuf);
					gl.glDrawElements(GL2.GL_TRIANGLES, d.getIndices().length, GL2.GL_UNSIGNED_INT, polyBuf);
				}
				d.getDecalTexture().disable(gl);
			}
			gl.glDisableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
			gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
			gl.glDisableClientState(GL2.GL_INDEX_ARRAY);	
			
		}
	    this.drawAxisIndicator(gl);
		
        gl.glPushMatrix();
        gl.glLoadIdentity();
		
		gl.glPopMatrix();
		
		gl.glPopMatrix();
		
    	int errCode = GL2.GL_NO_ERROR;
	    if ((errCode = gl.glGetError()) != GL2.GL_NO_ERROR) {
	       String errString = glu.gluErrorString(errCode);
	       log.println("OpenGL Error: "+errString);
	       JOptionPane.showMessageDialog(parent,
	                "Error while rendering: " + errString + ".",
	                "Graphics Rendering Error",
	                JOptionPane.ERROR_MESSAGE);
	    }    
	}
	
	private void writeStats(GLAutoDrawable drawable) {
    	glu = new GLU();
    	gl = drawable.getGL().getGL2();
    	drawable.setGL(gl);
        // Check for VBO support.

        // Check version.
        version = gl.glGetString( GL.GL_VERSION );
        gluVersion = glu.gluGetString(GLU.GLU_VERSION);
        log.println("GL version: "+version);
        log.println("GLU version: "+gluVersion);
        
        String tempVersion = version.substring( 0, 3);
        float versionNum = new Float(tempVersion).floatValue();
        boolean versionValid = (versionNum >= 1.5f) ? true : false;
        log.println("Valid GL version:"+tempVersion+"  -> "+versionValid);
        
        // Check if extensions are available.
        boolean extValid =  gl.isExtensionAvailable("GL_ARB_vertex_buffer_object");
        log.println("VBO extension: "+extValid);
        boolean texNPOT = gl.isExtensionAvailable("GL_ARB_texture_non_power_of_two");
     
        log.println("Texture NPOT extension: "+texNPOT);
        
        // Check for VBO functions.
        boolean funcsValid = 
        		gl.isFunctionAvailable("glGenBuffers") &&
        		gl.isFunctionAvailable("glBindBuffer") &&
        		gl.isFunctionAvailable("glBufferData") &&
        		gl.isFunctionAvailable("glDeleteBuffers");      
       log.println("Needed JOGL Functions Available: "+ funcsValid);

        if(!extValid || !funcsValid) {
           // VBOs are not supported.
        	log.println( "VBOs are not supported.");
        	VBO = false;
        }
        log.println("Using VBOs: "+VBO);
        log.println("GL Context: \n"+drawable.getContext());
   	

	}

	private void drawAxisIndicator(GL2 gl) {
		if (pole) {
		    gl.glDisable(GL2.GL_COLOR_MATERIAL);
		    gl.glDisable(GL2.GL_LIGHTING);
			float[] prevColor = new float[4];
		    gl.glGetFloatv(GL2.GL_CURRENT_COLOR, prevColor, 0);
			float len = meshData.maxLen * 1.1f;
	        this.drawLine(gl, 0f, 0f, 0f, 0f, 0f, len, 1, 1, 0, 0, 1); // z axis
	        
	        gl.glPushMatrix();
	        gl.glTranslatef(0.0f, 0.0f, len);   // position the cone
	        // draw the cone 
	        // (GLUquadric, base, top, height, #slices, #stacks)
	        glu.gluCylinder(quadric, meshData.maxLen * 0.01f, 0.0f, meshData.maxLen * 0.04f, 32, 32);  
	        gl.glPopMatrix();
	        
		    gl.glEnable(GL2.GL_LIGHTING);
		    gl.glEnable(GL2.GL_COLOR_MATERIAL);
		    
		    gl.glColor4f(prevColor[0], prevColor[1], prevColor[2], prevColor[3]);	    
		}
	}
	
	public void reshape(GLAutoDrawable drawable, int x, int y, int w,
			int h) {
		GL2 gl = drawable.getGL().getGL2();
		gl.glViewport(0, 0, w, h);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluOrtho2D(0.0, (double) w, 0.0, (double) h);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
		
	}
	
	private void checkTriangleTextures(GL2 gl, Decal decal) {
		
		boolean points = false;
		int[] tris = meshData.triangles;
		float[] texs = meshData.texture;
		double[] worlds = meshData.worldCoords;
		ArrayList<Integer> triangles = new ArrayList<>();
		for (int i=0; i<tris.length; i+=3) {
			// check each point for an intercept with the texture
			int[] t = new int[3];
			t[0] = tris[i];
			t[1] = tris[i+1];
			t[2] = tris[i+2];
			points = decal.getCorners().checkTriangleFirst(t, worlds, texs);
						
			if (points) { // the entire triangle is inside the texture
				triangles.add(t[0]);
				triangles.add(t[1]);
				triangles.add(t[2]);

			}
			
			if (decal.getCorners().crossesMeridian() ) {
				points = decal.getCorners().checkTriangleSecond(t, worlds, texs);
				if (points) { // the entire triangle is inside the texture
					triangles.add(t[0]);
					triangles.add(t[1]);
					triangles.add(t[2]);
				}
			}
		}
		int[] textureTriangles = new int[triangles.size()];		 
		int k = 0;
		for (Integer f : triangles) {
			textureTriangles[k++] = f.intValue(); 
		}
		
		decal.setIndices(textureTriangles);
		
	}

	public Mesh getMesh() {
		return meshData;
	}
	private void createShapeMeshBuffers(GL2 gl) {
		gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
		gl.glEnableClientState(GL2.GL_NORMAL_ARRAY);
		gl.glEnableClientState(GL2.GL_COLOR_ARRAY);
		gl.glEnableClientState(GL2.GL_INDEX_ARRAY);
		
		if (VBO) {
			vBuf = FloatBuffer.wrap(meshData.vertices);
			nBuf = FloatBuffer.wrap(meshData.vertexNormals);
			vcBuf = FloatBuffer.wrap(meshData.colors);
			tBuf = IntBuffer.wrap(meshData.triangles);
			gl.glBindBuffer( GL2.GL_ARRAY_BUFFER, bufferObjs[SHAPE_VERTEX_DATA] );
			// Copy data to the server into the VBO.
			gl.glBufferData( GL2.GL_ARRAY_BUFFER, meshData.vertices.length*(Float.SIZE/Byte.SIZE), vBuf, GL2.GL_STATIC_DRAW );
			// Normals.
			gl.glBindBuffer( GL.GL_ARRAY_BUFFER, bufferObjs[SHAPE_NORMAL_DATA] );
			gl.glBufferData( GL.GL_ARRAY_BUFFER, meshData.vertexNormals.length*(Float.SIZE/Byte.SIZE), nBuf, GL.GL_STATIC_DRAW );
			
			gl.glBindBuffer( GL.GL_ARRAY_BUFFER, bufferObjs[SHAPE_COLOR_DATA] );
			gl.glBufferData( GL.GL_ARRAY_BUFFER, meshData.colors.length*(Float.SIZE/Byte.SIZE), vcBuf, GL.GL_STATIC_DRAW );
			gl.glBindBuffer( GL.GL_ELEMENT_ARRAY_BUFFER, bufferObjs[SHAPE_INDEX_DATA] );
			gl.glBufferData( GL.GL_ELEMENT_ARRAY_BUFFER, meshData.triangles.length*(Integer.SIZE/Byte.SIZE), tBuf, GL.GL_STATIC_DRAW );
		} else { // use vertex arrays in the client
			vBuf = Buffers.newDirectFloatBuffer(meshData.vertices);
			nBuf = Buffers.newDirectFloatBuffer(meshData.vertexNormals);
			vcBuf = Buffers.newDirectFloatBuffer(meshData.colors);
			tBuf = Buffers.newDirectIntBuffer(meshData.triangles);
			gl.glVertexPointer(3, GL2.GL_FLOAT, 0, vBuf);
			gl.glColorPointer(3, GL2.GL_FLOAT, 0, vcBuf);
		}		
		
	}	
	
	private void createDecalBuffers(GL2 gl, Decal d) {
		gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);		
		gl.glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
		
		if (VBO) {
			if (texBuf == null) {
				texBuf = FloatBuffer.wrap(meshData.texture);
			}
			gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
			gl.glBindBuffer( GL2.GL_ARRAY_BUFFER, d.getBufferObj()[Decal.DECAL_VERTEX_DATA] );
			gl.glBufferData( GL2.GL_ARRAY_BUFFER, meshData.vertices.length*(Float.SIZE/Byte.SIZE), vBuf, GL2.GL_STATIC_DRAW );
			
			gl.glBindBuffer( GL.GL_ARRAY_BUFFER, d.getBufferObj()[Decal.DECAL_TEXTURE_DATA] );
			gl.glBufferData( GL.GL_ARRAY_BUFFER, meshData.texture.length*(Float.SIZE/Byte.SIZE), texBuf, GL.GL_STATIC_DRAW );

			gl.glBindBuffer( GL.GL_ELEMENT_ARRAY_BUFFER, d.getBufferObj()[Decal.DECAL_INDEX_DATA] );
			gl.glBufferData( GL.GL_ELEMENT_ARRAY_BUFFER, d.getIndices().length*(Integer.SIZE/Byte.SIZE), d.getDecalTris(), GL.GL_STATIC_DRAW );
		} else {
			if (texBuf == null) {
				texBuf = Buffers.newDirectFloatBuffer(meshData.texture);
			}
			if (tvBuf == null) {
				tvBuf = Buffers.newDirectFloatBuffer(meshData.vertices);
			}
			polyBuf = Buffers.newDirectIntBuffer(d.getIndices());
			
			gl.glVertexPointer(3, GL2.GL_FLOAT, 0, tvBuf);
			gl.glTexCoordPointer(3, GL2.GL_FLOAT, 0, texBuf);
		}
		gl.glDisableClientState(GL2.GL_TEXTURE_COORD_ARRAY);	
		
    	int errCode = GL2.GL_NO_ERROR;
	    if ((errCode = gl.glGetError()) != GL2.GL_NO_ERROR) {
	       String errString = glu.gluErrorString(errCode);
	       log.println("OpenGL Error: "+errString);
	       JOptionPane.showMessageDialog(parent,
	                "Error accessing graphics card: " + errString + ".",
	                "Graphics Rendering Error",
	                JOptionPane.ERROR_MESSAGE);
	    }    

	}

	private void loadMeshFile(File mesh) {
		try {
			GZIPInputStream zipReader = new GZIPInputStream(new FileInputStream(mesh));
			InputStreamReader streamReader = new InputStreamReader(zipReader);
			BufferedReader br = new BufferedReader(streamReader);			
			String strLine;
			String delims = "[ ]+";

			// Read File Line By Line
			int vertexCnt = 0;
			int plateCnt = 0;
			
			strLine = br.readLine();
			strLine = strLine.trim();
			String[] toks = strLine.split(delims);
			if (toks.length != 2) {
				log.aprintln("Incorrect number of vertices/plates in the input shape file: "+mesh.getAbsolutePath()+mesh.pathSeparator+mesh.getName());
				br.close();
				return;
			}
			
			vertexCnt = Integer.parseInt(toks[0]);
			plateCnt = Integer.parseInt(toks[1]);
			
			float [] vertices = new float[vertexCnt * 3];
			int vertIdx = 0;
			
			for (int i = 0; i < vertexCnt; i++) {
				strLine = br.readLine();
				strLine = strLine.trim();
				String[] tokens = strLine.split(delims);
				if (tokens.length != 4) {
					log.aprintln("Incorrect number of vertices in the input shape file: "+mesh.getAbsolutePath()+mesh.pathSeparator+mesh.getName()+" on line: "+(i+1));
					br.close();
					return;
				}
				vertices[vertIdx++] = (float)(Double.parseDouble(tokens[1]));
				vertices[vertIdx++] = (float)(Double.parseDouble(tokens[2]));
				vertices[vertIdx++] = (float)(Double.parseDouble(tokens[3]));
			}

			int [] tris = new int[plateCnt * 3];
			int triIdx = 0;
			
			for (int k=0; k<plateCnt; k++) {
				strLine = br.readLine();
				strLine = strLine.trim();
				String[] tokens = strLine.split(delims);
				if (tokens.length != 4) {
					log.aprintln("Incorrect number of vertices in the input shape file: "+mesh.getAbsolutePath()+mesh.pathSeparator+mesh.getName()+" on line: "+(k+1));
					br.close();
					return;
				}
				tris[triIdx++] = (Integer.parseInt(tokens[1]));
				tris[triIdx++] = (Integer.parseInt(tokens[2]));
				tris[triIdx++] = (Integer.parseInt(tokens[3]));
				
			}
			
		    float[] normals = getVertexNormals(tris, vertices);
				

	           this.meshData = new Mesh(vertices, tris, normals);
	           this.meshData.worldCoords = vertsToWorldHVector(vertices);
			// Close the input stream
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
	}
	
	private void loadMeshFromBinary(File file) {
		try {
			FileInputStream fis = new FileInputStream(file);
			FileChannel channel = fis.getChannel();
			// Read File Line By Line
			int vertexCnt = 0;
			int plateCnt = 0;

			ByteBuffer counts = ByteBuffer.allocateDirect(Integer.SIZE/Byte.SIZE * 2);
			
			counts.clear();
			int ret = channel.read(counts);
			if (ret != Integer.SIZE/Byte.SIZE * 2) {
				throw new IOException("Unable to read the input shape file: "+file.getAbsolutePath()+file.pathSeparator+file.getName());
			}
			counts.flip();

			IntBuffer isb = counts.asIntBuffer();
			vertexCnt = isb.get(0);
			plateCnt = isb.get(1);
			log.aprint("number of vertices "+vertexCnt);			
			log.aprint("number of plates "+plateCnt);
			
			float [] vertices = new float[vertexCnt * 3];
			ByteBuffer vb = ByteBuffer.allocateDirect(Double.SIZE/Byte.SIZE * vertexCnt * 3);
			ret = channel.read(vb);	
			if (ret != Double.SIZE/Byte.SIZE * vertexCnt * 3) {
				throw new IOException("Unable to read the input shape file vertices: "+file.getAbsolutePath()+file.pathSeparator+file.getName());
			}
			
			vb.flip();
			
			DoubleBuffer db = vb.asDoubleBuffer();
			
			for (int i=0; i<vertices.length; i+=3) {
				vertices[i] = (float)db.get(i);
				vertices[i+1] = (float)db.get(i+1);
				vertices[i+2] = (float)db.get(i+2);
			}
			vb.clear();
			
			int[] triangles = new int[plateCnt * 3];
			
			ByteBuffer tb = ByteBuffer.allocateDirect(Integer.SIZE/Byte.SIZE * plateCnt * 3);
			ret = channel.read(tb);			
			if (ret != Integer.SIZE/Byte.SIZE * plateCnt * 3) {
				throw new IOException("Unable to read the input shape file plates: "+file.getAbsolutePath()+file.pathSeparator+file.getName());
			}
			
			tb.flip();
			
			IntBuffer ib = tb.asIntBuffer();
			
			for (int j=0; j<triangles.length; j+=3) {
				triangles[j] = ib.get(j);
				triangles[j+1] = ib.get(j+1);
				triangles[j+2] = ib.get(j+2);
			}
			
			tb.clear();
			
		    float[] normals = getVertexNormals(triangles, vertices);
			
	        this.meshData = new Mesh(vertices, triangles, normals);
	        this.meshData.worldCoords = vertsToWorldHVector(vertices);
	        
			// Close the input stream
			channel.close();
			fis.close();
		} catch (IOException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
			
	}
	
	
	private void loadMeshFromZippedBinary(File file) throws IOException {
		try {
			FileInputStream fis = new FileInputStream(file);
			GZIPInputStream zipReader = new GZIPInputStream(fis);
			DataInputStream data = new DataInputStream(zipReader);
			
			// Read File Line By Line
			int vertexCnt = data.readInt();
			int plateCnt = data.readInt();

			System.err.println("number of vertices "+vertexCnt);			
			System.err.println("number of plates "+plateCnt);
			
			float [] vertices = new float[vertexCnt * 3];
			byte[] verts = new byte[Float.SIZE/Byte.SIZE * vertexCnt * 3];
			data.readFully(verts, 0, verts.length);
			ByteBuffer vb = ByteBuffer.wrap(verts);
			for (int i=0; i<vertices.length; i++) {
				vertices[i] = vb.asFloatBuffer().get(i);
			}
			vb.clear();
			vb = null;
			
			int[] triangles = new int[plateCnt * 3];
			
			byte[] tris = new byte[Integer.SIZE/Byte.SIZE * plateCnt * 3];
			data.readFully(tris, 0, tris.length);
			
			ByteBuffer tb = ByteBuffer.wrap(tris);
			for (int j=0; j<triangles.length; j++) {
				triangles[j] = tb.asIntBuffer().get(j);
			}
			
			tb.clear();
			tb = null;
			float[] normals = null;
			if (this.isDefaultShapeModel) {
				normals = new float[vertices.length];
				for (int i=0; i<vertices.length; i+=3) {
					float[] tmp = new float[]{vertices[i], vertices[i+1], vertices[i+2]};
					normalize(tmp, 0);
					normals[i] = tmp[0];
					normals[i+1] = tmp[1];
					normals[i+2] = tmp[2];
				}
				normals = vertices;
			} else {
				normals = getVertexNormals(triangles, vertices);
			}
			
	        this.meshData = new Mesh(vertices, triangles, normals);
	        this.meshData.worldCoords = vertsToWorldHVector(vertices);
			// Close the input streams
			data.close();
			zipReader.close();
			fis.close();
		} catch (IOException e) {
			log.aprint(e);
			throw e;
		}
			
	}

	private void loadGaskellMesh(File file) {
		
		int numOfTriangles = 0;
		try {
		    String filePath = "";
		    FileInputStream fis = new FileInputStream(file.getAbsolutePath());
		    ZipInputStream zipIn = new ZipInputStream(new BufferedInputStream(fis));
		    ZipEntry entry = zipIn.getNextEntry();
		    if (entry != null) {
		        filePath = Main.getJMarsPath()+"localcache"+File.separator+entry.getName();
		        byte data[] = new byte[4096];
		        FileOutputStream fos = new FileOutputStream(filePath);
		        BufferedOutputStream bos = new BufferedOutputStream(fos, 4096);
		        int read = 0;
		        while ((read = zipIn.read(data, 0, 4096)) != -1) {
		            bos.write(data, 0, read);
		        }
		        bos.flush();
		        bos.close();
		        zipIn.closeEntry();
		    }
		    zipIn.close();
        
		BufferedReader br = new BufferedReader(new FileReader(new File(filePath)));
		// Get the object of DataInputStream
		String strLine;
		String delims = "[ ]+";

		// Read File Line By Line
		int vertexCnt = 0;
		
		
		ArrayList<Double> v = new ArrayList<Double>();
		ArrayList<Double> n = new ArrayList<Double>();
		ArrayList<Integer> ind = new ArrayList<Integer>();
		
		int idx = 0;
	
		while ((strLine = br.readLine()) != null) {
			strLine = strLine.trim();
			if (strLine.contains("endsolid")) {
				break;
			}
			String[] tokens = strLine.split(delims);
			if (tokens.length != 3) {
				log.aprintln("Incorrect number of points in the input file!");
//				continue;
				throw new IllegalArgumentException("Incorrect number of points in the input file!");
			}
			n.add((Double.parseDouble(tokens[0])));
			n.add((Double.parseDouble(tokens[1])));
			n.add((Double.parseDouble(tokens[2])));

			strLine = br.readLine();
			if (strLine == null) {
				log.aprintln("Incorrect number of points in the input file!");
//				continue;
				throw new IllegalArgumentException("Incorrect number of points in the input file!");
			}
			strLine = strLine.trim();
			tokens = strLine.split(delims);
			if (tokens.length != 3) {
				log.aprintln("Incorrect number of points in the input file!");
//				continue;
				throw new IllegalArgumentException("Incorrect number of points in the input file!");
			}
			v.add((Double.parseDouble(tokens[0])));
			v.add((Double.parseDouble(tokens[1])));
			v.add((Double.parseDouble(tokens[2])));
			
			strLine = br.readLine();
			if (strLine == null) {
				log.aprintln("Incorrect number of points in the input file!");
//				continue;
				throw new IllegalArgumentException("Incorrect number of points in the input file!");
			}
			strLine = strLine.trim();
			tokens = strLine.split(delims);
			if (tokens.length != 3) {
				log.aprintln("Incorrect number of points in the input file!");
//				continue;
				throw new IllegalArgumentException("Incorrect number of points in the input file!");
			}
			v.add((Double.parseDouble(tokens[0])));
			v.add((Double.parseDouble(tokens[1])));
			v.add((Double.parseDouble(tokens[2])));
			strLine = br.readLine();
			if (strLine == null) {
				log.aprintln("Incorrect number of points in the input file!");
//				continue;
				throw new IllegalArgumentException("Incorrect number of points in the input file!");
			}
			strLine = strLine.trim();
			tokens = strLine.split(delims);
			if (tokens.length != 3) {
				log.aprintln("Incorrect number of points in the input file!");
//				continue;
				throw new IllegalArgumentException("Incorrect number of points in the input file!");
			}
			v.add((Double.parseDouble(tokens[0])));
			v.add((Double.parseDouble(tokens[1])));
			v.add((Double.parseDouble(tokens[2])));
			
			ind.add(idx++);		
			ind.add(idx++);		
			ind.add(idx++);		
		}
		
		
		float [] vertices = new float[v.size()];
		float [] norms = new float[n.size()];
		int [] tris = new int[ind.size()];
		
		for (int i = 0; i < v.size(); i++) {
			vertices[i] = v.get(i).floatValue();
		}

		for (int k=0; k<ind.size(); k++) {
			tris[k] = ind.get(k).intValue();
		}
		
		for (int j=0; j<ind.size(); j++) {
			norms[j] = n.get(j).floatValue();
		}
		
System.err.println("Number of vertices: "+vertices.length/3);			
System.err.println("Number of normals: "+norms.length/3);			
System.err.println("Number of triangles: "+tris.length/3);			
	    this.meshData = new Mesh(vertices, tris, norms);
        this.meshData.worldCoords = vertsToWorldHVector(vertices);
		// Close the input stream
		br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

    private double[] vertsToWorldHVector(float[] verts) {
        double[] lonlats = new double[verts.length * 2 / 3];
        double latitude = 0.0;
        double longitude = 0.0;
    	int idx = 0;
        HVector temp = new HVector();
        ProjObj proj = Main.PO;
        
        for (int i=0; i<verts.length; i+=3) {
        	temp = temp.set(verts[i], verts[i+1], verts[i+2]);
        	latitude = temp.lat();
        	longitude = temp.lonE();
        	
        	Point2D spw = proj.convSpatialToWorld(360.0-(longitude), latitude);
			lonlats[idx++] = spw.getX();
			lonlats[idx++] = spw.getY();      	
        }
        
        return lonlats;
    }
       
    private void vertsToWorldHVector(float[] verts, double[] worlds) {
    	Arrays.fill(worlds, 0f);
        double latitude = 0.0;
        double longitude = 0.0;
    	int idx = 0;
        HVector temp = new HVector();
        ProjObj proj = Main.PO;
        
        for (int i=0; i<verts.length; i+=3) {
        	temp = temp.set(verts[i], verts[i+1], verts[i+2]);
        	latitude = temp.lat();
        	longitude = temp.lonE();
        	
        	Point2D spw = proj.convSpatialToWorld(360.0-(longitude), latitude);
			worlds[idx++] = spw.getX();
			worlds[idx++] = spw.getY();      	
        }
    }
       
    private float[] getVertexNormals(int[] tris, float[] vertices) {
    	float[] vertexNorms = new float[vertices.length];
    	float[] surfaceNorms = new float[tris.length];
    	int stride = 3;    	

    	for (int i=0; i<tris.length; i+=3) {
    		float[] normVec = Vec3dMath.normalFromPoints(vertices, tris[i], vertices, tris[i+1], vertices, tris[i+2]);
    		if (normVec == null) {
    			System.err.println("Null normal vector!");
    		}
    		surfaceNorms[i] = normVec[0];
    		surfaceNorms[i+1] = normVec[1];
    		surfaceNorms[i+2] = normVec[2];
    	}
    	
    	// calculate vertex normals from the surface normals
    	int snLoc = 0;
    	for (int v=0; v<tris.length; v++) {
    		
    		if (v%stride == 0) {
    			snLoc = v;
    		}
    		int ptLoc=0;
    		
    		ptLoc = tris[v]; //* stride; // get the index into the vertex normal vector array
    		
    		vertexNorms[ptLoc] = vertexNorms[ptLoc] + surfaceNorms[snLoc];
    		vertexNorms[ptLoc+1] = vertexNorms[ptLoc+1] + surfaceNorms[snLoc+1];
    		vertexNorms[ptLoc+2] = vertexNorms[ptLoc+2] + surfaceNorms[snLoc+2];
    	}
    	
    	// normalize the array of normal vectors
    	for (int n=0; n<vertexNorms.length; n+=stride) {
    		float[] norm = Vec3dMath.normalized(vertexNorms, n);
    		vertexNorms[n] = norm[0];
    		vertexNorms[n+1] = norm[1];
    		vertexNorms[n+2] = norm[2];   
    	}
    	
    	return vertexNorms;
    }
    
	private void setCamera(GL2 gl, GLU glu, float distance, float lookX, float lookY, float lookZ) {
		float fov = 45f * zoomFactor;
		if (fov > 180f) {
			fov = 180f;
		} else if (fov < Float.MIN_NORMAL) {
			fov = Float.MIN_NORMAL;
		}
         // Change to projection matrix.
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();

        float nearZ = 0.0001f;
        float farZ = distance * 2f;
               
        // Perspective.
        float widthHeightRatio = (float) getWidth() / (float) getHeight();
        glu.gluPerspective(fov, widthHeightRatio, nearZ, farZ);
        glu.gluLookAt(lookX, lookY, distance, lookX, lookY, lookZ, 0, 1, 0);

        // Change back to model view matrix.
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

	private void moveCamera(GL2 gl, GLU glu, float distance, float lookX, float lookY, float lookZ) {
       // Change to projection matrix.
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();

        // Perspective.
        float widthHeightRatio = (float) getWidth() / (float) getHeight();
        glu.gluPerspective(45, widthHeightRatio, 0.05 /*distance * 3f*/, /*1000*/ distance * 3f);
        glu.gluLookAt(lookX, lookY, distance, lookX, lookY, lookZ, 0, 1, 0);

        // Change back to model view matrix.
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

	void drawLine(GL2 gl, float ax, float ay, float az, float bx, float by, float bz, int width, int r, int g, int b, int a )
	{
	    gl.glColor3f( r, g, b);

	    gl.glLineWidth(width);
	    gl.glBegin(GL.GL_LINES);
	    gl.glVertex3f( ax, ay, az);
	    gl.glVertex3f( bx, by, bz);
	    gl.glEnd();
	}
	
	void drawLine(GL2 gl, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, int width, int r, int g, int b, int a )
	{
	    gl.glColor3f( r, g, b);

	    gl.glLineWidth(width);
	    gl.glBegin(GL.GL_LINES);
	    gl.glVertex3f( ax, ay, az);
	    gl.glVertex3f( bx, by, bz);
	    gl.glVertex3f( cx, cy, cz);
	    gl.glVertex3f( ax, ay, az);
	    gl.glEnd();
	}
	
	public void loadDecalImage() {
        if (decalImage != null){

        	
    		decalTextureHeight = decalImage.getHeight();
    		decalTextureWidth = decalImage.getWidth();
    		
        	// get the texture
        	try {
        		
        		final ByteArrayOutputStream output = new ByteArrayOutputStream() {
        		    @Override
        		    public synchronized byte[] toByteArray() {
        		        return this.buf;
        		    }
        		};
        		
        		ImageIO.write(decalImage, "png", output);
        		InputStream dstream = new ByteArrayInputStream(output.toByteArray());

        		TextureData decalData = TextureIO.newTextureData(gl.getGLProfile(), dstream, false, "png");

        		decalTexture = TextureIO.newTexture(decalData);
        	}
        	catch (IOException exc) {
        		exc.printStackTrace();
        		System.exit(1);
        	}
        }		
	}
	
	boolean isPowerOf2(int i) {
		return i > 2 && ((i&-i)==i);
	}
	int vertexLoc(int x, int y, int rowWidth) {
		return (x + (y * rowWidth)) * 3 ;
	}
	
	int vertexLoc2(int x, int y, int rowWidth) {
		return (x + (y * rowWidth)) ;
	}
	
	   /**
	    * Modifies the vector (v[offset],v[offset+1],v[offset+2]), replacing it with a
	    * unit vector that points in the same direction.  If the length of the vector
	    * is not positive, then the original vector is not modified.
	    */
	   public static void normalize(float[] v, int offset) {
	      float length = length(v,offset);
	      if (length > 0) {
	    	  float invLength = 1f/length;
	         v[offset] *= invLength;
	         v[offset+1] *= invLength;
	         v[offset+2] *= invLength;
	      }
	   }
	
	   /**
	    * Returns the length of the vector (v[offset],v[offset+1],v[offset+2]).
	    */
	   public static float length(float[] v, int offset) {
	      return (float) Math.sqrt(v[offset]*v[offset] + v[offset+1]*v[offset+1] + v[offset+2]*v[offset+2]);
	   }
	
	boolean isMagZero(float[] vertex, int idx) {
		boolean zero = false;
		float mag = (float)Math.sqrt(vertex[idx]*vertex[idx]+vertex[idx+1]*vertex[idx+1]+vertex[idx+2]*vertex[idx+2]);
		if (Float.compare(mag, 0.0f) == 0) {
			zero = true;
		} else {
		}
		
		return zero;
	}
	
	public static Visual3D getVisual3D(int width, int height, String kernelFile, String body, JFrame parent, File mesh, boolean defaultShapeModel) throws IOException {
		if (Visual3D.singleton == null) {
			Visual3D.singleton = new Visual3D(width, height, kernelFile, body, parent, mesh, defaultShapeModel);
		}		
		return Visual3D.singleton;
	}
	
	public void addActionListener(ActionListener action) {
		actions.add(action);	
	}
	
	class SimpleKeyAdapter extends KeyAdapter {
		public void keyPressed(KeyEvent e) {
			int kc = e.getKeyCode();
			int kchar = e.getKeyChar();
			switch(kc){
			case KeyEvent.VK_LEFT: transX += 0.02f; break;
			case KeyEvent.VK_RIGHT: transX -= 0.02f; break;
			case KeyEvent.VK_UP: transY -= 0.02f; break;
			case KeyEvent.VK_DOWN: transY += 0.02f; break;
			case KeyEvent.VK_PLUS: gamma += 2f; break;
			case KeyEvent.VK_ADD: gamma += 2f; break;
			case KeyEvent.VK_MINUS: gamma -= 2f; break;
			case KeyEvent.VK_SUBTRACT: gamma -= 2f; break;
			case KeyEvent.VK_Z:
				switch(kchar){
				case 'Z':
					zoomFactor += ZOOM_INC;
					if (zoomFactor > 2.88f) {
						zoomFactor = 2.88f;
					}
					break;
				case 'z':
					zoomFactor -= ZOOM_INC;
					if (zoomFactor < 0.51f) {
						zoomFactor = 0.51f;
					}
					break;
				}
				break;
			case KeyEvent.VK_A:
				switch(kchar){
				case 'a': ambient = Math.max(0, ambient - 0.2f); break;
				case 'A': ambient = Math.min(1, ambient + 0.2f); break;
				}
				break;
			case KeyEvent.VK_B:
				switch(kchar){
				case 'b': applyMesh = false; break;
				case 'B': applyMesh = true; break;
				}
				break;
			case KeyEvent.VK_D:
				switch(kchar){
				case 'd': diffused = Math.max(0, diffused - 0.2f); break;
				case 'D': diffused = Math.min(1, diffused + 0.2f); break;
				}
				break;
			case KeyEvent.VK_G:
				switch(kchar){
				case 'g': drawGrid = false; break;
				case 'G': drawGrid = true; break;
				}
				break;
			case KeyEvent.VK_S:
				switch(kchar){
				case 's': specular = Math.max(0, specular - 0.2f); break;
				case 'S': specular = Math.min(1, specular + 0.2f); break;
				}
				break;
			case KeyEvent.VK_P:
				switch(kchar){
				case 'p': pole = false; break;
				case 'P': pole = true; break;
				}
				break;
			case KeyEvent.VK_T:
				switch(kchar){
				case 't': applyDecal = false; break;
				case 'T': applyDecal = true; break;
				}
				break;
			case KeyEvent.VK_I:
				switch(kchar){
				case 'i': mDiffused = Math.max(0, mDiffused - 0.2f); break;
				case 'I': mDiffused = Math.min(1, mDiffused + 0.2f); break;
				}
			case KeyEvent.VK_L: 
				light = !light; break;
			}
			// ugly hack since VK_PLUS is not consistent across platforms/languages
//			if (e.isShiftDown() && "+".equalsIgnoreCase(Character.toString((char) kchar))) {
//				gamma += 2f;
//			}
		    Visual3D.this.grabFocus();
					
			Visual3D.this.repaint();
		}
	}

  class SimpleMouseAdapter extends MouseAdapter {
	  
	  Direction dir = Direction.NONE;
	  boolean start = true;	  
	  
      public void mousePressed(MouseEvent e) {
        prevMouseX = e.getX();
        prevMouseY = e.getY();
      }

      public void mouseReleased(MouseEvent e) {
    	  dir = Direction.NONE;
    	  start = true;
      }

      public void mouseDragged(MouseEvent e) {
    	  
      	float xThresh = 10.0f;
      	float yThresh = 10.0f;
          int x = e.getX();
          int y = e.getY();
          int width=0, height=0;
          Object source = e.getSource();
          if(source instanceof Window) {
              Window window = (Window) source;
              width=window.getWidth();
              height=window.getHeight();
          } else if (GLProfile.isAWTAvailable() && source instanceof java.awt.Component) {
              java.awt.Component comp = (java.awt.Component) source;
              width=comp.getWidth();
              height=comp.getHeight();
          } else {
              throw new RuntimeException("Event source neither Window nor Component: "+source);
          }
          
          float thetaY = 360.0f * ((float)(prevMouseX-x)/(float)width);
          float thetaX = 360.0f * ((float)(prevMouseY-y)/(float)height);
          float deltaY = y-prevMouseY;
          float deltaX = prevMouseX-x;

          if (start) {
          	if (Math.abs(deltaY/(Math.abs(deltaX) > Float.MIN_NORMAL ? deltaX : 1f)) > 0.5f) {
          		dir = Direction.VERT;
          		start = false;
          	} else {
          		dir = Direction.HORZ;
          		start = false;
          	}       	
          }
          if(e.isControlDown()) {
          	// translate
        	  if (Visual3D.this.isDefaultShapeModel) {
  				transX += ((float)(prevMouseX-x)) / 64; 
  				transY += ((float)(y-prevMouseY)) / 64;
        	  } else {
				transX += ((float)(prevMouseX-x)); 
				transY += ((float)(y-prevMouseY));
        	  }
          } else {
        	  if (alpha + thetaX > 180f) {
        		  alpha = 180f;
        	  } else if (alpha + thetaX < 0f) {
        		  alpha = 0f;
        	  } else {
        		  alpha += thetaX;
        	  }
            gamma -= thetaY;
          }
          
          prevMouseX = x;
          prevMouseY = y;
          
    	  
        Visual3D.this.repaint();
      }
      public void mouseWheelMoved(MouseEvent e) {
	  	  float multiplier = 1.01f;
    	  float[] direction = e.getRotation();
          if(e.isControlDown()) {
        	  multiplier = 1.1f;
          } else {
        	  multiplier = 1.01f;
          }

      	  if (direction[1] >= 0) {
  				zoomFactor /= multiplier;
  				if (zoomFactor < 0.0001f) {
  					zoomFactor = 0.0001f;
  				}
      	  } else {
  				zoomFactor *= multiplier;
  				if (zoomFactor > 4f) {
  					zoomFactor = 4f;
  				}			
      	  }
          Visual3D.this.repaint();
   	  }
  }
  
  private int genTexture(GL gl) {
      final int[] tmp = new int[1];
      gl.glGenTextures(1, tmp, 0);
      return tmp[0];
  }
  
  static public double norm(float[] coords, int start){
      double length = Math.sqrt((coords[start+0]*coords[start+0]) +
                      (coords[start+1]*coords[start+1]) +
                      (coords[start+2]*coords[start+2]));
      return length;
  }
  
  static public double norm(double[] coords, int start){
      double length = Math.sqrt((coords[start+0]*coords[start+0]) +
                      (coords[start+1]*coords[start+1]) +
                      (coords[start+2]*coords[start+2]));
      return length;
  }
  static public double sumOfSquares(double a, double b, double c){
      return a*a+b*b+c*c;
  }

  static public double norm(double a, double b, double c){
      return Math.sqrt(a*a+b*b+c*c);
  }
  
  static public double[] getNormal(double[] v1, int v1start, double[] v2, int v2start) {
      double[] n = {
              v1[v1start+1]*v2[v2start+2]-v1[v1start+2]*v2[v2start+1],
              v1[v1start+2]*v2[v2start+0]-v1[v1start+0]*v2[v2start+2],
              v1[v1start+0]*v2[v2start+1]-v1[v1start+1]*v2[v2start+0] };
      return n;
  }
  
  
  class Mesh {
	    float[] vertices;
	    int[] triangles;
	    float[] colors;
	    float[] monoColor;
	    float[] vertexNormals;
	    float[] texture;
	    double[] worldCoords;
	    float[] textureVertices;
	    int[] textureTriangles;
	    int[] texturePolys;
	    float minLen = Float.MAX_VALUE, maxLen = 0f, avgLen, stdDev; // min/max vertex vector lengths

	    // Copies references - no deep copy
	    public Mesh(float[] vertices, int[] triangles, float[] norms){
	    	
	    	this.vertices = new float[vertices.length];
	    	System.arraycopy(vertices, 0, this.vertices, 0, vertices.length);
	    	
	    	this.triangles = new int[triangles.length];
	    	System.arraycopy(triangles, 0, this.triangles, 0, triangles.length);

	    	this.vertexNormals = new float[norms.length];
	    	System.arraycopy(norms, 0, this.vertexNormals, 0, norms.length);
	    	
	    	this.texture = new float[vertices.length / 2 * 3];
	    	Arrays.fill(this.texture, Float.NaN);
	    	
	    }

	    public void computeVertexColors(){
            if (Visual3D.this.isDefaultShapeModel) {
            	System.err.println("Calculating colors for default shape model");
            }
            
	            colors = new float[(vertices.length / 3) * 3];
	            for(int i=0; i<colors.length; i+=3){
	                    double norm = norm(vertices, i);
	                    double color = norm;
	                    		
	                    		
	                    if (Visual3D.this.isDefaultShapeModel) {
							color = 0.4;
	                    } else {
		                    color = Math.min(1.0, (1.0*((norm - minLen) / (maxLen - minLen))));
	                    }

		                colors[i] = colors[i+1] = colors[i+2] = (float)color;	                    
	            }
	            
	    }

	    public void calcMinMaxAvgMagnitudes(){
	            float normSum = 0;
	            int normCount = 0;
	            float normSumSquared = 0f;
	            minLen = Float.MAX_VALUE;
	            maxLen = 0f;

	            for(int i=0; i<vertices.length; i+=3){
	                    float m = (float)norm(vertices, i);
	                    if (Float.compare(m, 0f) != 0) {
		                    maxLen = (float)Math.max(maxLen, m);
		                    minLen = (float)Math.min(minLen, m);
		                    normSum += m;
		                    normCount++;
		                    normSumSquared += m * m;
	                    }
	            }
	            avgLen = normSum / normCount;
	            float avgSquaredLen = normSumSquared / normCount;
	            stdDev = (float) Math.sqrt((double)(avgSquaredLen - avgLen * avgLen));
 	            
	    }
	    
	    public void setTexture(float[] textures) {
	    	this.texture = new float[textures.length];
	    	System.arraycopy(textures, 0, this.texture, 0, textures.length);
	    }

	    public void setTextureVertices(float[] textureVerts) {
	    	this.textureVertices = new float[textureVerts.length];
	    	System.arraycopy(textureVerts, 0, this.textureVertices, 0, textureVerts.length);
	    }
	    
	    public void setTextureTriangles(int[] triangles) {
	      	this.textureTriangles = new int[triangles.length];
	      	System.arraycopy(triangles, 0, this.textureTriangles, 0, triangles.length);
	    }
	    
	    public void setTexturePolys(int[] polys) {
	    	this.texturePolys = new int[polys.length];
	      	System.arraycopy(polys, 0, this.texturePolys, 0, polys.length);
	    }
	    
	    /** 
	     * Return the image data after histogram-equalization.
	     * This method applies the algorithm of histogram-equalization
	     * on the input image data.
	     * @return the image data histogram-equalized
	     * @param inputGreyImageData input grey image data need to be histogram-equalized
	     * @param equalizedHistogram equalized histogram of inputGreyImageData
	     */
	    public float[] equalizeImage(float[] inputGreyImageData,
					   double[] equalizedHistogram) {
	        float[] data = new float[inputGreyImageData.length];

	        for (int y = 0; y < inputGreyImageData.length; y++) {
                data[y] = (float) (equalizedHistogram[(int)inputGreyImageData[y]]  /** 255 */);

	        }
	        
	        return data;
	    }

	    /* 
	     * Create normalized and equalized histograms of inputGreyImageData
	     * @param inputGreyImageData a grey image data
	     * @param normalizedHistogram used to return normalized histogram
	     * @param equalizedHistogram used to return equalized histogram
	     */
	    public void createHistograms(float[] inputGreyImageData,
	                                 double[] normalizedHistogram, double[] equalizedHistogram) {
	        int[] histogram = new int[256];

	        // count the number of occurences of each color
	        for (int y = 0; y < inputGreyImageData.length; y++) {
                ++histogram[(int)inputGreyImageData[y]];
	        }

	        // normalize and equalize the histogram array
	        double sum = 0;
	        for (int v = 0; v < 256; v++) {
	            if (normalizedHistogram != null) {
	                normalizedHistogram[v] = (double) histogram[v] / (inputGreyImageData.length);
	            }
	            if (equalizedHistogram != null) {
	                sum += histogram[v];
	                equalizedHistogram[v] = sum / (inputGreyImageData.length);
	            }
	        }
	    }
	    
	}

}

class Triangle {
	float[][] vertices = new float[3][3];
	double[][] worldCoords = new double[3][2];
	double[][] texs = new double[3][2];
	boolean divided = false;
	
	Triangle () {
		
	}
	
	Triangle (float[] v1, float[] v2, float[] v3) {
		vertices[0] = v1;
		vertices[1] = v2;
		vertices[2] = v3;
	}
	
	public boolean subDivide(boolean b0, boolean b1, boolean b2, BoundingCorners box) {
		// check for which points are interior points
		ArrayList<Integer> in = new ArrayList<Integer>();
		ArrayList<Integer> out = new ArrayList<Integer>();
		if (b0) {
			in.add(0);
		} else {
			out.add(0);
		}
		if (b1) {
			in.add(1);
		} else {
			out.add(1);
		}
		if (b2) {
			in.add(2);
		} else {
			out.add(2);
		}
		
		for (int i=0; i<out.size(); i++) {
			for (int j=0; j<in.size(); j++) {
				if (bisect(vertices[out.get(i)], vertices[in.get(j)], worldCoords[out.get(i)], worldCoords[in.get(j)], box)) {
					return true;
				}
			}
		}
		divided = true;
		return true;
	}
	
	private boolean bisect(float[] vOut, float[] vIn, double[] wOut, double[] wIn, BoundingCorners box) {
		// calculate new world coords
		double[] wTemp = new double[2];
		wTemp[0] = wIn[0] + ((wOut[0] - wIn[0]) / 2.0);
		wTemp[1] = wIn[1] + ((wOut[1] - wIn[1]) / 2.0);
		// calculate new vertex
		float[] temp = new float[3];
		temp[0] = vIn[0] + ((vOut[0] - vIn[0]) / 2f);
		temp[1] = vIn[1] + ((vOut[1] - vIn[1]) / 2f);
		temp[2] = vIn[2] + ((vOut[2] - vIn[2]) / 2f);
		

		if (box.checkPoint(wTemp[0], wTemp[1])) {
			vOut[0] = temp[0];
			vOut[1] = temp[1];
			vOut[2] = temp[2];
			wOut[0] = wTemp[0];
			wOut[1] = wTemp[1];
			return true;
		} else {
			return bisect(temp, vIn, wTemp, wIn, box);
		}
	}
	
	public void setVertex(int i, float[] v) {
		vertices[i] = v;
	}
	public float[][] getVertices() {
		return vertices;
	}
	
	public double[][] getWorldCoords() {
		return worldCoords;
	}	
	
	public double[][] getTextures() {
		return texs;
	}
	
	public void setX(int i, double worldX) {
		worldCoords[i][0] = worldX;
	}
	
	public void setY(int i, double worldY) {
		worldCoords[i][1] = worldY;
	}

	public void setU(int i, double u) {
		texs[i][0] = u;
	}
	
	public void setV(int i, double v) {
		texs[i][1] = v;
	}
	
	public boolean isSubDivided() {
		return divided;
	}
}

class Vertex {
	float [] coords = new float[3];
	
	Vertex() {
		
	}
	Vertex (float p1, float p2, float p3) {
		coords[0] = new Float(p1);
		coords[1] = new Float(p2);
		coords[2] = new Float(p3);
	}
	public float[] getCoords() {
		return coords;
	}
	public void setCoords(float [] p) {
		coords= p;
	}
	static public Vertex offset(Vertex vIn, Vertex vOffset) {
		float[] in = vIn.getCoords();
		float[] offset = vOffset.getCoords();
		return new Vertex(in[0]-offset[0], in[1]-offset[1], in[2]-offset[2]);
	}
	static public Vertex getNormal(Vertex vA, Vertex vB) {
		float[] a = vA.getCoords();
		float[] b = vB.getCoords();
		// calculate the cross product
		return new Vertex(a[1]*b[2]-a[2]*b[1], a[2]*b[0]-a[0]*b[2], a[0]*b[1]-a[1]*b[0]);
	}
	
	static public double norm(Vertex v){
		float[] coords = v.getCoords();
		double length = Math.sqrt((coords[0]*coords[0]) +
				(coords[1]*coords[1]) +
				(coords[2]*coords[2]));
		return length;
	}
	
	static public Vertex mul(Vertex v, double m){
		float[] coords = v.getCoords();
		return new Vertex((float)(coords[0]*m), (float)(coords[1]*m), (float)(coords[2]*m));
	}
	
	static public Vertex normalize(Vertex v) {
		Vertex rv = null;
		float[] coords = v.getCoords();
		double length = norm(v);
		if (length > 0) {
			rv = new Vertex((float)(coords[0]/length), (float)(coords[1]/length), (float)(coords[2]/length));
		} 
		return rv;
	}
	
	public String toString() {
		
		return "x=" + coords[0] + " y=" + coords[1] + " z=" + coords[2];
	}
    
    
}

