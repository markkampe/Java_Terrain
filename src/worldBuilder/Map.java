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
	private int display;
	
	// map size (in pixels)
	private static final int MIN_WIDTH = 400;	// min screen width
	private static final int MIN_HEIGHT = 400;	// min screen height
	private static final int SMALL_POINT = 2;	// width of a small point
	private static final int LARGE_POINT = 4;	// width of a large point
	private static final int TOPO_CELL = 4;		// width of a topographic cell
	private Dimension size;

	// display colors
	private static final Color SELECT_COLOR = Color.WHITE;
	private static final Color POINT_COLOR = Color.PINK;
	private static final Color MESH_COLOR = Color.GREEN;
	private static final Color WATER_COLOR = Color.BLUE;
	private static final Color RAIN_COLOR = Color.CYAN;
	private static final Color SOIL_COLOR = Color.YELLOW;
	// topographic lines are shades of gray
	private static final int TOPO_DIM = 0;
	private static final int TOPO_BRITE = 255;
	private Color background;
	
	// the interesting data
	private Mesh mesh;				// mesh of Voronoi points
	private MeshRef cartesian[][];	// Voronoi to Cartesian translation
	
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
		size = new Dimension(width, height);
		this.background = new Color(128, 128, 128);
		parms = Parameters.getInstance();
		selectNone();
	}

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

	/**
	 * reset the mesh to be displayed
	 * 		typically used to switch between a base Mesh
	 * 		and a tentative mesh (e.g. for new slopes,
	 * 		mountains, erosion, etc).
	 * 
	 * NOTE: this is a change of MeshPoints only.
	 * 		 the voronoi vertex coordinates are assumed
	 * 		 to be unchanged.
	 */
	public void setMesh(Mesh m) {
		setVisible(false);	// avoid races w/paint
		this.mesh = m;
		setVisible(true);
		repaint();
	}
	
	/**
	 * load an entirely new mesh
	 * 		based on a new/different set of Voronoi vertices
	 */
	public void newMesh(Mesh m) {
		setVisible(false);	// avoid races w/paint
		this.mesh = m;
		newMap();			// recreate Voronoi-to-Cartesian map
		setVisible(true);
		repaint();
		
		// FIX: need to call newMap() when window resizes
	}
	
	/**
	 * @return reference to current mesh
	 */
	public Mesh getMesh() {
		return this.mesh;
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

	
	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		// get scaling factors
		double x_extent = Parameters.x_extent;
		double y_extent = Parameters.y_extent;
		double z_extent = Parameters.z_extent;
		int height = getHeight();
		int width = getWidth();

		// make sure we have something to display
		if (mesh == null) {
			setBackground(Color.WHITE);
			g.setColor(Color.BLACK);
			g.drawString("use menu File:New or File:Open to create a mesh", width / 4, height / 2);
			return;
		}
		
		setBackground(background);
		
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
		
		// see if we are rendering topology
		if ((display & SHOW_TOPO) != 0)
				paint_topo(g);
		
		// see if we have a selection area to draw
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
	 * render a topographical map
	 * 
	 *   1. create a dense height map
	 */
	private void paint_topo(Graphics g) {
		
		// for each cell in the displayable grid
		for(int r = 0; r < cartesian.length; r++)
			for(int c = 0; c < cartesian[0].length; c++) {
				// interpolate height (from surrounding MeshPoints)
				double z = cartesian[r][c].height();
				
				// shade a rectangle for that altitude
				double shade = TOPO_DIM + ((z + parms.z_extent/2) * (TOPO_BRITE - TOPO_DIM));
				g.setColor(new Color((int) shade, (int) shade, (int) shade));
				g.drawRect(c * TOPO_CELL, r * TOPO_CELL, TOPO_CELL, TOPO_CELL);
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
	
	/**
	 * create mapping from Voronoi mesh to map grid
	 * 
	 * 		The Voronoi mesh is deliberately sparse and
	 * 		non-uniform, but we must eventually produce a
	 * 		dense and uniform map.  This function creates
	 * 		a map which lists, for each 2D map point, the
	 * 		three nearest Voronoi points (and their distances).
	 * 		This can be used to interpolate the value of 
	 * 		any property of any point as a function of
	 * 		the immediately surrounding Voronoi points.
	 * 
	 * 		This map only changes when the Mesh changes,
	 * 		or when the display/export size changes.
	 */
	public void newMap() {
		int h = getHeight()/TOPO_CELL;
		int w = getWidth()/TOPO_CELL;
		
		cartesian = new MeshRef[h][w];
		for(int r = 0; r < h; r++) {
			double y = (double) r/h - parms.y_extent/2;
			for(int c = 0; c < w; c++) {
				double x =  (double)c/w - parms.x_extent/2;
				MeshPoint m = new MeshPoint(x,y);
				MeshRef ref = new MeshRef();
				cartesian[r][c] = ref;
				for(int v = 0; v < mesh.vertices.length; v++) {
					MeshPoint p = mesh.vertices[v];
					ref.consider(p.index, p.distance(m));
				}
			}
		}
	}
	
	/**
	 * Each cell of the Voronoi to grid map is represented
	 * by a MeshRef, which describes the 3 nearest Voronoi
	 * points.
	 * 
	 * We refer to the Voronoi points by index rather than
	 * by MeshPoint because the latter change as a result
	 * of editing operations, but the former change only
	 * when a new Mesh is created.
	 * 
	 * This is a private class within Map because it needs
	 * to make reference to the selected Mesh.
	 */
	private class MeshRef {
		public int neighbors[];		// MeshPoint index
		private double distances[];	// distance to that MeshPoint
		private static final int NUM_NEIGHBORS = 3;
		
		public MeshRef() {
			neighbors = new int[NUM_NEIGHBORS];
			distances = new double[NUM_NEIGHBORS];
			
			for(int i = 0; i < NUM_NEIGHBORS; i++) {
				neighbors[i] = -1;
				distances[i] = 666;
			}
		}
		
		/**
		 * consider a MeshPoint and note the three closest
		 * 
		 * @param index 	index of this mesh point
		 * @param distance	distance to this mesh points
		 */
		public void consider(int index, double distance) {
			if (distance >= distances[2])
				return;
			
			// XXX there is a more elegant/general way to code this
			if (distance >= distances[1]) {
				// replace last in list
				neighbors[2] = index;
				distances[2] = distance;
			} else if (distance >= distances[0]) {
				// replace second in list
				neighbors[2] = neighbors[1];
				distances[2] = distances[1];
				neighbors[1] = index;
				distances[1] = distance;
			} else {
				// replace first in list
				neighbors[2] = neighbors[1];
				distances[2] = distances[1];
				neighbors[1] = neighbors[0];
				distances[1] = distances[0];
				neighbors[0] = index;
				distances[0] = distance;	
			}
		}
		
		/**
		 * @return proximity weighted average height
		 */
		public double height() {
			double z[] = new double[NUM_NEIGHBORS];
			double d[] = new double[NUM_NEIGHBORS];
			for(int n = 0; n < NUM_NEIGHBORS; n++) {
				z[n] = mesh.vertices[neighbors[n]].z;
				d[n] = distances[n];
			}
			
			double zAvg = 0;
			for(int n = 0; n < NUM_NEIGHBORS; n++) {
				zAvg += z[n]/3;		// FIX: compute proximity weighted height
			}
			
			return zAvg;
		}
	}
}
