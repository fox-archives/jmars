package edu.asu.jmars.layer.tes6;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.*;
import java.util.*;
import javax.swing.*;

import edu.asu.jmars.util.Config;

public class AnimatedPoly {
	public AnimatedPoly(GeneralPath poly, CanvasProvider canvasProvider){
		this.poly = (GeneralPath)poly.clone();
		this.canvasProvider = canvasProvider;
		
		colorIndex = 0; // set initial draw color index
		visible = false; // flag whether the poly is visible or not
		
		myClockListener = new ClockListener(){
			public void clockEventOccurred(ClockEvent evt) {
				serviceClockPulse();
			}
		};
	}
	
	public synchronized void startAnimation(){
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				draw(true);
			}
		});
	
		animate = true;
		
		getClockSource().addClockListener(myClockListener);
	}
	
	public synchronized void stopAnimation(){
		getClockSource().removeClockListener(myClockListener);
		
		animate = false;
		
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				draw(true);
			}
		});
	}
	
	public synchronized void serviceClockPulse(){
		if (animate){
			SwingUtilities.invokeLater(new Runnable(){
				public void run(){
					draw(false);
				}
			});
		}
	}
	
	/**
	 * Draws the current polygon as if it were drawn as a
	 * boundary draw (initial draw to make visible, or last
	 * draw to make invisible) or a non-boundary draw (a 
	 * middle draw where poly with previous color is erased
	 * and a poly with the new color is drawn).
	 * 
	 * CAUTION: This method should be called on the Swing Thread.
	 */
	private void draw(boolean boundary){
		// Get the on-screen graphics context
		Graphics2D g2 = canvasProvider.getDrawingCanvas();
		
		if (g2 == null){ return; }
	
		// Set single-pixel stroke
		g2.setStroke(singlePixelStroke);

		// Draw in XOR-mode
		g2.setXORMode(Color.white);

		// This is either a draw or an erase depending upon the
		// when the call is made.
		Color c1 = colors[colorIndex];
		g2.setColor(c1);
		g2.draw(poly);
		visible = !visible;
		
		if (!boundary){
			// This is always a draw, and only called when the
			// previous call had erased, which happens either
			// at the start or at the end, i.e. boundary.
			colorIndex = (colorIndex+1)%colors.length;
			Color c2 = colors[colorIndex];
			g2.setColor(c2);
			g2.draw(poly);
			visible = !visible;
		}
		
		// Revert back to paint mode
		g2.setPaintMode();
	}
	
	private GeneralPath poly;
	private CanvasProvider canvasProvider;
	private int colorIndex;
	private boolean visible = false; // dynamic state during draw
	private boolean animate = false;
	private ClockListener myClockListener;
	
	public  Clock getClockSource(){
		if (clockSource == null){
			clockSource = new Clock("AnimatedPolyClock",animationDelay);
		}
		
		return clockSource;
	}
	
	private static final long animationDelay =
		(long)Config.get("tes.animatedPoly.animationDelay",250L);
	private static Clock clockSource = null;
	
	private static final Stroke singlePixelStroke = new BasicStroke(0);
	private static final Color[] colors = {
		Color.cyan,
		Color.magenta,
		Color.white,
		Color.yellow,
		Color.lightGray,
		Color.red,
		Color.blue,
		Color.green
	};
	
	public static interface CanvasProvider {
		public Graphics2D getDrawingCanvas();
	}
	
	// Test code.
	public static void main(String[] args){
		final Vector aPolys = new Vector();
		
		final JFrame f = new JFrame("animated poly test");
		f.setSize(500, 500);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JPanel contentPane = new JPanel(new BorderLayout());
		final JPanel drawCanvas = new JPanel();
		drawCanvas.setBackground(Color.white);
		
		JButton add = new JButton("Add");
		
		JButton del = new JButton("Delete");
		
		JPanel buttonBox = new JPanel(new FlowLayout());
		buttonBox.add(add); buttonBox.add(del);
		
		contentPane.add(buttonBox, BorderLayout.NORTH);
		contentPane.add(drawCanvas, BorderLayout.CENTER);
		
		final CanvasProvider cp = new CanvasProvider(){
			public Graphics2D getDrawingCanvas(){
				return (Graphics2D)drawCanvas.getGraphics();
			}
		};
		
		add.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				float corner = (float)Math.random()*400.0f;
				float side = (float)Math.random()*200.0f;
				
				GeneralPath poly = new GeneralPath();
				poly.moveTo(corner, corner);
				poly.lineTo(corner, corner+side);
				poly.lineTo(corner+side, corner+side);
				poly.lineTo(corner+side, corner);
				poly.closePath();
				
				AnimatedPoly aPoly = new AnimatedPoly(poly, cp);
				aPoly.startAnimation();
				
				aPolys.add(aPoly);
			}
		});
		
		del.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				int idx = (int)(Math.random()*aPolys.size());
				if (idx < aPolys.size()){
					AnimatedPoly p = (AnimatedPoly)aPolys.get(idx);
					aPolys.removeElementAt(idx);
					p.stopAnimation();
				}
			}
		});
		
		f.setContentPane(contentPane);
		f.setVisible(true);
	}
}

