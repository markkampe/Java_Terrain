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
	public static final int SHOW_SOIL = 0x20;
	private int display;		// bitmask for enabled SHOWs
	
	// map size (in pixels)
	private static final int MIN_WIDTH = 400;	// min screen width
	private static final int MIN_HEIGHT = 400;	// min screen height
	private static final int SMALL_POINT = 2;	// width of a small point
	private static final int TOPO_CELL = 5;		// pixels/topographic cell
	private Dimension size;

	// display colors
	private static final Color SELECT_COLOR = Color.WHITE;
	private static final Color POINT_COLOR = Color.PINK;
	private static final Color MESH_COLOR = Color.GREEN;
	
	// the interesting data
	private Mesh mesh;			// mesh of Voronoi points
	private double heightMap[]; // Height of each mesh point
	private double rainMap[];	// Rainfall of each mesh point
	private double depression[]; // which cells are in depressions
	private int downHill[];		// each cell's downhill neighbor
	private Cartesian map;		// Cartesian translation of Voronoi Mesh
	
	private Parameters parms;	// world parameters
	
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
			this.map = new Cartesian(mesh, width/TOPO_CELL, height/TOPO_CELL);
			this.heightMap = new double[mesh.vertices.length];
			this.rainMap = new double[mesh.vertices.length];
		}
		this.parms = Parameters.getInstance();
		selectNone();
	}

	/**
	 * get/set routines for Mesh and per MeshPoint attributes
	 */
	public Mesh getMesh() { return mesh; }
	public void setMesh(Mesh mesh) {
		this.mesh = mesh;	
		if (mesh != null) {
			this.map = new Cartesian(mesh, getWidth()/TOPO_CELL, getHeight()/TOPO_CELL);
			this.heightMap = new double[mesh.vertices.length];
			this.rainMap = new double[mesh.vertices.length];
		} else {
			this.map = null;
			this.heightMap = null;
			this.rainMap = null;
		}
		
		repaint();
	}
	
	public double[] getHeightMap() {return heightMap;}
	public double[] setHeightMap(double newHeight[]) {
		double old[] = heightMap; 
		heightMap = newHeight; 
		calc_downhill();
		repaint();
		return old;
	}

	public double[] getRainMap() {return rainMap;}
	public double[] setRainMap(double newRain[]) {
		double old[] = rainMap; 
		rainMap = newRain; 
		repaint();
		return old;
	}
	
	public int[] getDownHill() {calc_downhill(); return downHill;}	
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
		return display & view;
	}
	
	public void export(String filename, double x, double y, double dx, double dy, int meters) {
		System.out.println("TODO: Implement Map.export to file " + filename + ", <" + x + "," + y + ">, " + dx + "x" + dy + ", grain=" + meters + "m");
		// TODO implement Mesh:export ... maybe move it to Map:export
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
			map = new Cartesian(mesh, width/TOPO_CELL, height/TOPO_CELL);
		
		// start by rendering backgrounds (rain, altitude, and water bodies)
		if ((display & SHOW_RAIN) != 0) {
				RainMap r = new RainMap(this);
				r.paint(g, width, height, TOPO_CELL);
		} else if ((display & SHOW_TOPO) != 0) {
				AltitudeMap a = new AltitudeMap(this);
				a.paint(g, width, height, TOPO_CELL);
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
			Path paths[] = mesh.edges;
			for (int i = 0; i < paths.length; i++) {
				Path p = paths[i];
				double x1 = (p.source.x + x_extent / 2) * width;
				double y1 = (p.source.y + y_extent / 2) * height;
				double x2 = (p.target.x + x_extent / 2) * width;
				double y2 = (p.target.y + y_extent / 2) * height;
				g.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
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
	 * recalculate the map of who is downhill from whom
	 * 	must be done whenever the height map changes
	 */
	private void calc_downhill() {
		// find the down-hill neighbor of each point
		downHill = new int[mesh.vertices.length];
		for( int i = 0; i < downHill.length; i++ ) {
			downHill[i] = -1;
			double lowest_height = heightMap[i];
			for( int n = 0; n < mesh.vertices[i].neighbors; n++) {
				int x = mesh.vertices[i].neighbor[n].index;
				if (heightMap[x] < lowest_height) {
					downHill[i] = x;
					lowest_height = heightMap[x];
				}
			}
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
