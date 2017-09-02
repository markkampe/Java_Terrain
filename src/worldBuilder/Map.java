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
	
	// displayed window offset and size
	double x_min, y_min, x_max, y_max;

	// display colors
	private static final Color SELECT_COLOR = Color.WHITE;
	private static final Color POINT_COLOR = Color.PINK;
	private static final Color MESH_COLOR = Color.GREEN;
	
	private Color highLights[];		// points to highlight
	private boolean highlighting;	// are there points to highlight
	
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
	private MeshPoint artery;	// artery entry point
	private double artery_flow;	// incoming arterial flow
	
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
	public Map(int width, int height) {
		this.size = new Dimension(width, height);
		this.parms = Parameters.getInstance();
		setWindow(-Parameters.x_extent/2, -Parameters.y_extent/2, Parameters.x_extent/2, Parameters.y_extent/2);
		selectNone();
	}

	/**
	 * get/set routines for Mesh and per MeshPoint attributes
	 */
	public Mesh getMesh() { return mesh; }
	public void setMesh(Mesh mesh) {
		this.mesh = mesh;	
		if (mesh != null) {
			this.map = new Cartesian(mesh, x_min, y_min, x_max, y_max, getWidth()/TOPO_CELL, getHeight()/TOPO_CELL);
			this.heightMap = new double[mesh.vertices.length];
			this.rainMap = new double[mesh.vertices.length];
			this.downHill = new int[mesh.vertices.length];
			this.fluxMap = new double[mesh.vertices.length];
			this.erodeMap = new double[mesh.vertices.length];
			this.soilMap = new double[mesh.vertices.length];
			this.hydrationMap = new double[mesh.vertices.length];
			this.highLights = new Color[mesh.vertices.length];
			this.hydro = new Hydrology(this);
			this.artery = null;
			this.erosion = 1; // FIX s.b parms.dErosion;
			hydro.reCalculate(true);
		} else {
			this.map = null;
			this.heightMap = null;
			this.rainMap = null;
			this.downHill = null;
			this.fluxMap = null;
			this.erodeMap = null;
			this.soilMap = null;
			this.hydrationMap = null;
			this.highLights = null;
			this.hydro = null;
			this.artery = null;
		}
		
		repaint();
	}
	
	/* Z value (pre-erosion) for each Mesh point */
	public double[] getHeightMap() {return heightMap;}
	public double[] setHeightMap(double newHeight[]) {
		double old[] = heightMap; 
		heightMap = newHeight; 
		for(int i = 0; i < erosion; i++)
			hydro.reCalculate(i == 0);
		repaint();
		return old;
	}

	/* rainfall (in cm) for each Mesh point */
	public double[] getRainMap() {return rainMap;}
	public double[] setRainMap(double newRain[]) {
		double old[] = rainMap; 
		rainMap = newRain; 
		for(int i = 0; i < erosion; i++)
			hydro.reCalculate(i == 0);
		repaint();
		return old;
	}
	
	/* Number of configured erosion cycles	*/
	public int getErosion() {return erosion;}
	public int setErosion(int cycles) {
		int old = erosion;
		erosion = cycles;
		for(int i = 0; i < erosion; i++)
			hydro.reCalculate(i == 0);
		repaint();
		return old;
	}
	
	/* incoming arterial river	*/
	public void setArtery(MeshPoint point, double flow) {
		artery = point;
		artery_flow = flow;
		repaint();
	}
	public MeshPoint getArtery() { return artery; }
	public double getArterial() { return artery_flow; }
	
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
	public double map_x(int screen_x) {
		double x = (double) screen_x / getWidth();
		double range = x_max - x_min;
		return x_min + (x * range);
	}

	public double map_y(int screen_y) {
		double y = (double) screen_y / getHeight();
		double range = y_max - y_min;
		return y_min + (y * range);
	}
	
	/**
	 * convert a map coordinate into a screen coordinate
	 */
	public int screen_x(double x) {
		if (x < x_min || x > x_max)
			return -1;
		double X = getWidth() * (x - x_min)/(x_max - x_min);
		return (int) X;
	}
	
	public int screen_y(double y) {
		if (y < y_min || y > y_max)
			return -1;
		double Y = getHeight() * (y - y_min)/(y_max - y_min);
		return (int) Y;
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
	 * @param x0	screen x
	 * @param y0	screen y
	 * @param x1	screen x
	 * @param y1	screen y
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
	 * @param x0	screen x
	 * @param y0	screen y
	 * @param x1	screen x
	 * @param y1	screen y
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
	 * @param x		screen x
	 * @param y		screen y
	 * @param radius	screen radius
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
	 * highlight points (typically for diagnostic purposes
	 * 
	 * @param point number (-1 = reset)
	 * @param color
	 */
	public void highlight(int point, Color c) {
		if (point >= 0) {
			highLights[point] = c;
			highlighting = true;
		} else {
			for(int i = 0; i < highLights.length; i++)
				highLights[i] = null;
			highlighting = false;
		}
	}
	
	/**
	 * find the mesh point closest to a screen location
	 * @param screen_x
	 * @param screen_y
	 * @return nearest MeshPoint
	 */
	public MeshPoint choosePoint(int screen_x, int screen_y) {
		
		double x = map_x(screen_x);
		double y = map_y(screen_y);
		MeshPoint spot = new MeshPoint(x, y);
		MeshPoint closest = null;
		double distance = 2 * Parameters.x_extent;
		
		for(int i = 0; i < mesh.vertices.length; i++) {
			MeshPoint point = mesh.vertices[i];
			double d = spot.distance(point);
			if (d < distance) {
				closest = point;
				distance = d;
			}
		}
		
		return closest;
	}
	
	/*
	 * change the display window to the specified range
	 * @param map x/y upper left, lower right
	 */
	public void setWindow(double x0, double y0, double x1, double y1) {
		x_min = (x1 >= x0) ? x0 : x1;
		y_min = (y1 >= y0) ? y0 : y1;
		x_max = (x1 >= x0) ? x1 : x0;
		y_max = (y1 >= y0) ? y1: y0;
		map = new Cartesian(mesh, x_min, y_min, x_max, y_max, getWidth()/TOPO_CELL, getHeight()/TOPO_CELL);
		repaint();
		
		if (parms.debug_level > 0)
			System.out.println("Display window <" + x_min + ", " + y_min + "> to <" + x_max + ", " + y_max + ">");
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
		
		// make sure the Cartesian translation is up-to-date
		if (map.height != height/TOPO_CELL || map.width != width/TOPO_CELL)
			map = new Cartesian(mesh, x_min, y_min, x_max, y_max, width/TOPO_CELL, height/TOPO_CELL);
		
		// make sure all the rainfall/river/erosion data is up-to-date
		for(int i = 0; i < erosion; i++)
			hydro.reCalculate(i == 0);
		
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
		
		// see if we are rendering points
		if ((display & SHOW_POINTS) != 0) {
			g.setColor(POINT_COLOR);
			MeshPoint[] points = mesh.vertices;
			for (int i = 0; i < points.length; i++) {
				MeshPoint p = points[i];
				double x = screen_x(p.x) - SMALL_POINT / 2;
				double y = screen_y(p.y) - SMALL_POINT / 2;
				if (x >= 0 && y >= 0)
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
					double x1 = screen_x(m.x);
					double y1 = screen_y(m.y);
					double x2 = screen_x(n.x);
					double y2 = screen_y(n.y);
					
					// make sure it is on the screen
					if (x1 < 0 || x2 < 0 || y1 < 0 || y2 < 0)
						continue;
					
					g.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
				}
			}
		}
		
		// see if we are rendering topographic lines
		if ((display & SHOW_TOPO) != 0) {
			TopoMap t = new TopoMap(this);
			t.paint(g, width, height, TOPO_CELL);
		}
		
		// see if we are rendering soil/hydration
		if ((display & SHOW_SOIL) != 0) {
			SoilMap s = new SoilMap(this);
			s.paint(g, width, height, TOPO_CELL);
		}
		
		// see if we are rendering rivers
		if ((display & SHOW_WATER) != 0) {
			RiverMap r = new RiverMap(this);
			r.paint(g, width, height);
			WaterMap w = new WaterMap(this);
			w.paint(g, width, height, TOPO_CELL);		
		}
		
		// see if we have points to highlight
		if (highlighting)
			for(int i = 0; i < highLights.length; i++)
				if (highLights[i] != null) {
					g.setColor(highLights[i]);
					MeshPoint p = mesh.vertices[i];
					double x = screen_x(p.x) - LARGE_POINT / 2;
					double y = screen_y(p.y) - LARGE_POINT / 2;
					g.drawOval((int) x, (int) y, LARGE_POINT, LARGE_POINT);
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
	 * linear interpolation of a (color) value within a range
	 * 
	 * @param min return value
	 * @param max return value
	 * @param value (0-1) to be scaled
	 */
	public static double linear(int min, int max, double value) {
		if (value <= 0)
			return min;
		else if (value >= 1)
			return max;
		
		double ret = value * (max - min);
		return min + ret;
	}
	
	/**
	 * logarithmic interpolation of a (color) value within a range
	 * 
	 * @param min
	 *            return value
	 * @param max
	 *            return value
	 * @param value
	 *            (0-1) to be scaled
	 * @param base
	 *            (result/2 for each increment)
	 */
	public static double logarithmic(int min, int max, double value, double base) {
		if (value <= 0)
			return min;
		else if (value >= 1)
			return max;
		
		double resid = 0.5;
		double ret = 0;
		while (value > 0) {
			if (value > base)
				ret += resid;
			else
				ret += resid * value / base;
			resid /= 2;
			value -= base;
		}
		return min + (ret * (max - min));
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
