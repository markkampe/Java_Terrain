package worldBuilder;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.*;

public class Map extends JPanel {
	/**
	 * a Map is the displayable resizeable form of a Mesh.
	 * 
	 * This class maintains a few basic types of information:
	 * 	 1. a Mesh of random Voronoi MeshPoints (with attributes) 
	 *   2. Cartesian arrays with the attributes of displayable points
	 *   3. a translation ray to determine the values of Cartesian
	 *      cells as a function of the surrounding Voronoi points.
	 *      
	 * This class operates on logical (-0.5 to +0.5) mesh coordinates,
	 * and height/width display coordinates.  It is (largely) agnostic
	 * about real-world (e.g. meters) coordinates.
	 */

	// types of displays
	public static final int SHOW_ALL = 0xff;
	public static final int SHOW_POINTS = 0x01;
	public static final int SHOW_MESH = 0x02;
	public static final int SHOW_TOPO = 0x04;
	public static final int SHOW_RAIN = 0x08;
	public static final int SHOW_WATER = 0x10;
	public static final int SHOW_ERODE = 0x20;
	public static final int SHOW_SOIL = 0x40;
	private int display;		// bitmask for enabled SHOWs
	
	// soil types
	public static final int SEDIMENTARY = 0;
	public static final int METAMORPHIC = 1;
	public static final int IGNEOUS = 2;
	public static final int ALLUVIAL = 3;
	public static final String soil_names[] = {
			"Sedimentary", "Metamorphic", "Igneous", "Alluvial"
	};
	
	// map size (in pixels)
	private static final int MIN_WIDTH = 400;	// min screen width
	private static final int MIN_HEIGHT = 400;	// min screen height
	private static final int SMALL_POINT = 2;	// width of a small point
	private static final int LARGE_POINT = 4;	// width of a large point
	private static final int TOPO_CELL = 5;		// pixels/topographic cell
												// CODE DEPENDS ON THIS CONSTANT
	private Dimension size;

	// display colors
	private static final Color SELECT_COLOR = Color.WHITE;
	private static final Color POINT_COLOR = Color.PINK;
	private static final Color MESH_COLOR = Color.GREEN;
	
	// the interesting data
	private Mesh mesh;			// mesh of Voronoi points
	private Hydrology hydro;	// hydrology calculator
	
	// per MeshPoint information
	private double heightMap[]; // Height of each mesh point
	private double soilMap[];	// Soil type of each mesh point
	private double rainMap[];	// Rainfall of each mesh point
	private double fluxMap[];	// Water flow through each point
	private double erodeMap[];	// erosion/deposition
	private double hydrationMap[];	// soil hydration
	private int downHill[];		// down-hill neighbor
	private Cartesian map;		// Cartesian translation of Voronoi Mesh
	private int erosion;		// number of erosion cycles
	
	// hydrological results
	public double max_slope;		// maximum slope
	public double max_flux;			// maximum river flow
	public double max_velocity;		// maximum water velocity
	public double max_erosion;		// maximum soil loss due to erosion
	public double max_deposition;	// maximum soil gain due to sedimentation

	private Parameters parms;
	
	private static final long serialVersionUID = 1L;

	/**
	 * instantiate a displayable map widget
	 * 
	 * @param width
	 *            ... preferred width (in pixels)
	 * @param height
	 *            ... perferred height 9in pixels)
	 * @param background
	 *            ... background color
	 */
	public Map(Mesh mesh, int width, int height) {
		size = new Dimension(width, height);
		this.mesh = mesh;
		if (mesh != null) {
			this.map = new Cartesian(mesh, 
					-Parameters.x_extent/2, -Parameters.y_extent/2, 
					Parameters.x_extent/2, Parameters.y_extent/2,
					width/TOPO_CELL, height/TOPO_CELL);
			this.heightMap = new double[mesh.vertices.length];
			this.rainMap = new double[mesh.vertices.length];
			this.downHill = new int[mesh.vertices.length];
			this.fluxMap = new double[mesh.vertices.length];
			this.erodeMap = new double[mesh.vertices.length];
			this.soilMap = new double[mesh.vertices.length];
			this.hydrationMap = new double[mesh.vertices.length];
			this.parms = Parameters.getInstance();
			this.hydro = new Hydrology(this);
			
			// ensure that the map is not perfectly flat
			MountainDialog.placeMountain(this, 0, 0, Parameters.x_extent, Parameters.z_extent/10000, Parameters.CONICAL, ALLUVIAL);	
			hydro.reCalculate();
		}
		selectNone();
	}

	/**
	 * get/set routines for Mesh and per MeshPoint attributes
	 */
	public Mesh getMesh() { return mesh; }
	public void setMesh(Mesh mesh) {
		this.mesh = mesh;	
		if (mesh != null) {
			this.map = new Cartesian(mesh, 
					-Parameters.x_extent/2, -Parameters.y_extent/2, 
					Parameters.x_extent/2, Parameters.y_extent/2,
					getWidth()/TOPO_CELL, getHeight()/TOPO_CELL);
			this.heightMap = new double[mesh.vertices.length];
			this.rainMap = new double[mesh.vertices.length];
			this.downHill = new int[mesh.vertices.length];
			this.fluxMap = new double[mesh.vertices.length];
			this.erodeMap = new double[mesh.vertices.length];
			this.soilMap = new double[mesh.vertices.length];
			this.hydrationMap = new double[mesh.vertices.length];
			this.hydro = new Hydrology(this);
			
			// ensure that the map is not perfectly flat
			MountainDialog.placeMountain(this, 0, 0, Parameters.x_extent, Parameters.z_extent/10000, Parameters.CONICAL, ALLUVIAL);
			hydro.reCalculate();
		} else {
			this.map = null;
			this.heightMap = null;
			this.rainMap = null;
			this.downHill = null;
			this.fluxMap = null;
			this.erodeMap = null;
			this.soilMap = null;
			this.hydrationMap = null;
			this.hydro = null;
		}
		
		repaint();
	}
	
	/* Z value (pre-erosion) for each Mesh point */
	public double[] getHeightMap() {return heightMap;}
	public double[] setHeightMap(double newHeight[]) {
		double old[] = heightMap; 
		heightMap = newHeight; 
		hydro.reCalculate();
		repaint();
		return old;
	}

	/* rainfall (in cm) for each Mesh point */
	public double[] getRainMap() {return rainMap;}
	public double[] setRainMap(double newRain[]) {
		double old[] = rainMap; 
		rainMap = newRain; 
		repaint();
		return old;
	}
	
	/* Number of configured erosion cycles	*/
	public int getErosion() {return erosion;}
	public int setErosion(int cycles) {
		int old = erosion;
		erosion = cycles;
		hydro.reCalculate();
		repaint();
		return old;
	}
	
	/*
	 * these arrays are regularly re-calculated from height/rain
	 * and so do not need to be explicitly SET
	 */
	public int[] getDownHill() { return downHill; }
	public double[] getFluxMap() {return fluxMap;}
	public double[] getErodeMap() {return erodeMap;}
	public double [] getSoilMap() {return soilMap;}
	public double [] getHydrationMap() { return hydrationMap; }
	
	/* Meshpoint to Cartesian translation matrix */
	public Cartesian getCartesian() {return map;}
	
	/**
	 * enable/disable display elements
	 * 
	 * @param view to be enabled/disabled
	 * @param on/off
	 * @return current sense of that view
	 */
	public int setDisplay(int view, boolean on) {
		if (on)
			display |= view;
		else
			display &= ~view;
		if (mesh != null)
			repaint();
		return display;
	}
	
	/**
	 * convert a screen coordinate into a map coordinate
	 */
	double x(int screen_x) {
		double x = (double) screen_x / getWidth();
		x -= .5;
		return x * Parameters.x_extent;	
	}

	double y(int screen_y) {
		double y = (double) screen_y / getHeight();
		y -= .5;
		return y * Parameters.y_extent;	
	}

	// description of the area to be highlighted
	private int sel_x0, sel_y0, sel_x1, sel_y1;
	private int sel_height, sel_width;
	private int sel_radius;
	private enum Selection {NONE, CIRCLE, LINE, RECTANGLE};
	private Selection sel_type;
	
	/**
	 * highlight a rectangular selection
	 * 
	 * @param x0	
	 * @param y0	
	 * @param x1	
	 * @param y1	
	 */
	public void selectLine(int x0, int y0, int x1, int y1) {
		sel_x0 = x0;
		sel_y0 = y0;
		sel_x1 = x1;
		sel_y1 = y1;
		sel_type = Selection.LINE;
		
		repaint();
	}
	/**
	 * highlight a rectangular selection
	 * 
	 * @param x0	
	 * @param y0	
	 * @param x1	
	 * @param y1	
	 */
	public void selectRect(int x0, int y0, int width, int height) {
		// normalize boxes defined upwards or to the left
		if (width > 0) {
			sel_x0 = x0;
			sel_width = width;
		} else {
			sel_x0 = x0 + width;
			sel_width = -width;
		}
		if (height > 0) {
			sel_y0 = y0;
			sel_height = height;
		} else {
			sel_y0 = y0 + height;
			sel_height = -height;
		}
		sel_type = Selection.RECTANGLE;
		repaint();
	}
	
	/**
	 * highlight a circular selection
	 * 
	 * @param x
	 * @param y
	 * @param radius
	 */
	public void selectCircle(int x, int y, int radius) {
		sel_x0 = x;
		sel_y0 = y;
		sel_radius = radius;
		sel_type = Selection.CIRCLE;
		
		repaint();
	}
	
	/**
	 * clear selection highlight
	 */
	public void selectNone() {
		sel_type = Selection.NONE;
		repaint();
	}

	/**
	 * repaint the map pane
	 * 
	 *  Note: order of painting is to enable layering of some things
	 *  	  atop others
	 */
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		int height = getHeight();
		int width = getWidth();
		
		// make sure we have something to display
		if (mesh == null || mesh.vertices.length == 0) {
			setBackground(Color.WHITE);
			g.setColor(Color.BLACK);
			g.drawString("use menu File:New or File:Open to create a mesh", width / 4, height / 2);
			return;
		} else
			setBackground(Color.GRAY);
		
		// get scaling factors
		double x_extent = Parameters.x_extent;
		double y_extent = Parameters.y_extent;
		
		// make sure the Cartesian translation is up-to-date
		if (map.height != height/TOPO_CELL || map.width != width/TOPO_CELL)
			map = new Cartesian(mesh, 
					-Parameters.x_extent/2, -Parameters.y_extent/2, 
					Parameters.x_extent/2, Parameters.y_extent/2,
					width/TOPO_CELL, height/TOPO_CELL);
		
		// make sure all the rainfall/river/erosion data is up-to-date
		hydro.reCalculate();
		
		// start by rendering backgrounds (rain, altitude, erosion, and water bodies)
		if ((display & SHOW_RAIN) != 0) {
				RainMap r = new RainMap(this);
				r.paint(g, width, height, TOPO_CELL);
		} else if ((display & SHOW_TOPO) != 0) {
				AltitudeMap a = new AltitudeMap(this);
				a.paint(g, width, height, TOPO_CELL);
		}
		
		if ((display & SHOW_ERODE) != 0 ) {
			ErodeMap e = new ErodeMap(this);
			e.paint(g, width, height, TOPO_CELL);
		}
		
		if ((display & SHOW_WATER) != 0) {
			WaterMap w = new WaterMap(this);
			w.paint(g, width, height, TOPO_CELL);
		}
		
		// see if we are rendering points
		if ((display & SHOW_POINTS) != 0) {
			g.setColor(POINT_COLOR);
			MeshPoint[] points = mesh.vertices;
			for (int i = 0; i < points.length; i++) {
				MeshPoint p = points[i];
				double x = ((p.x + x_extent / 2) * width) - SMALL_POINT / 2;
				double y = ((p.y + y_extent / 2) * height) - SMALL_POINT / 2;
				g.drawOval((int) x, (int) y, SMALL_POINT, SMALL_POINT);
			}
		}

		// see if we are rendering the mesh
		if ((display & SHOW_MESH) != 0) {
			g.setColor(MESH_COLOR);
			// for each mesh point
			for(int i = 0; i < mesh.vertices.length; i++) {
				MeshPoint m = mesh.vertices[i];
				// for each neighbor
				for(int j = 0; j < m.neighbors; j++) {
					MeshPoint n = m.neighbor[j];
					if (n.index < i)
						continue;	// we already got this one
					double x1 = (m.x + x_extent / 2) * width;
					double y1 = (m.y + y_extent / 2) * height;
					double x2 = (n.x + x_extent / 2) * width;
					double y2 = (n.y + y_extent / 2) * height;
					g.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
				}
			}
		}
		
		// see if we are rendering topographic lines
		if ((display & SHOW_TOPO) != 0) {
			TopoMap t = new TopoMap(this);
			t.paint(g, width, height, TOPO_CELL);
		}
		
		// see if we are rendering rivers
		if ((display & SHOW_WATER) != 0) {
			RiverMap r = new RiverMap(this);
			r.paint(g, width, height);
		}
	
		// see if we have a selection area to highlight
		switch(sel_type) {
		case LINE:
			g.setColor(SELECT_COLOR);
			g.drawLine(sel_x0,  sel_y0,  sel_x1,  sel_y1);
			break;
		case CIRCLE:
			g.setColor(SELECT_COLOR);
			g.drawOval(sel_x0, sel_y0, sel_radius, sel_radius);
			break;
		case RECTANGLE:
			g.setColor(SELECT_COLOR);
			g.drawRect(sel_x0, sel_y0, sel_width, sel_height);
			break;
		case NONE:
			break;
		}
	}
	
	/**
	 * @return minimum acceptable canvas size
	 */
	public Dimension getMinimumSize() {
		return new Dimension(MIN_WIDTH, MIN_HEIGHT);
	}

	/**
	 * @return preferred canvas size
	 */
	public Dimension getPreferredSize() {
		return size;
	}
}
