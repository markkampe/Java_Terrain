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
	private Color background;	// default background color
	private static final Color SELECT_COLOR = Color.WHITE;
	private static final Color POINT_COLOR = Color.PINK;
	private static final Color MESH_COLOR = Color.GREEN;
	private static final Color RIVER_COLOR = Color.BLUE;
	private static final Color STREAM_COLOR = Color.CYAN;
	//private static final Color SOIL_COLOR = Color.YELLOW;
	
	// topographic lines are shades of gray
	private static final int TOPO_DIM = 0;
	private static final int TOPO_BRITE = 255;
	// rain fall is indicated by shades of cyan
	private static final int RAIN_DIM = 0;
	private static final int RAIN_BRITE= 255;
	
	// the interesting data
	private Mesh mesh;			// mesh of Voronoi points
	private double heightMap[]; // Height of each mesh point
	private double rainMap[];	// Rainfall of each mesh point
	private double flowMap[];	// Water flow through each mesh point
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
			this.flowMap = new double[mesh.vertices.length];
		}
		this.background = new Color(128, 128, 128);
		this.parms = Parameters.getInstance();
		selectNone();
	}

	/**
	 * change/return the associated Mesh
	 */
	public Mesh getMesh() { return mesh; }
	public void setMesh(Mesh mesh) {
		this.mesh = mesh;	
		if (mesh != null) {
			this.map = new Cartesian(mesh, getWidth()/TOPO_CELL, getHeight()/TOPO_CELL);
			this.heightMap = new double[mesh.vertices.length];
			this.rainMap = new double[mesh.vertices.length];
			this.flowMap = new double[mesh.vertices.length];
		} else {
			this.map = null;
			this.heightMap = null;
			this.rainMap = null;
			this.flowMap = null;
		}
		
		repaint();
	}
	
	// get/set methods for MeshPoints and per MeshPoint attributes
	public double[] getHeightMap() {return heightMap;}
	public double[] getFlowMap() {return flowMap;}
	public double[] getRainMap() {return rainMap;}
	public int[] getDownHill() {return downHill;}
	
	public double[] setHeightMap(double newHeight[]) {
		double old[] = heightMap; 
		heightMap = newHeight; 
		calc_downhill();
		repaint();
		return old;
	}

	public double[] setRainMap(double newRain[]) {
		double old[] = rainMap; 
		rainMap = newRain; 
		repaint();
		return old;
	}	

	public double[] setFlowMap(double newFlow[]) {
		double old[] = flowMap; 
		flowMap = newFlow; 
		repaint();
		return old;
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
	 *  Note: order of painting is to enable layring of some things
	 *  	  atop others
	 */
	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		// get scaling factors
		double x_extent = Parameters.x_extent;
		double y_extent = Parameters.y_extent;
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
		
		// see if we are rendering rainfall
		if ((display & SHOW_RAIN) != 0) {
				paint_rain(g);
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
		
		// see if we are rendering topology
		if ((display & (SHOW_TOPO+SHOW_WATER)) != 0)
			paint_topo(g);
		
		// see if we are rendering rivers
		if ((display & SHOW_WATER) != 0)
			paint_water(g);

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
	
	private double topoMajor = 0.1;		// Z range for major line
	private int topoMinors = 5;			// minor lines per major line
	
	public void setTopoLines(double major, int minorPerMajor) {
		topoMajor = major;
		topoMinors = minorPerMajor;
	}
	
	/**
	 * linear interpolation of a (color) value within a range
	 * 
	 * @param min return value
	 * @param max return value
	 * @param value (0-1) to be scaled
	 */
	private static double linear(int min, int max, double value) {
		double ret = value * (max - min);
		return min + ret;
		
	}
	
	/**
	 * logarithmic interpolation of a (color) value within a range
	 * 
	 * @param min return value
	 * @param max return value
	 * @param value (0-1) to be scaled
	 * @param base (result/2 for each increment)
	 */
	private static double logarithmic(int min, int max, double value, double base) {
		double resid = 0.5;
		double ret = 0;
		while(value > 0) {
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
	 * Render current mesh as a rainfall map
	 * 
	 *   for each <row,col>
	 *   	compute interpolated rain
	 *   	generate shaded background
	 */
	private void paint_rain(Graphics g) {
		// see if we have any rain to display
		if (mesh == null || mesh.vertices.length == 0)
			return;
		
		// see if a screen resize has invalidated Cartesian translation
		int h = getHeight()/TOPO_CELL;
		int w = getWidth()/TOPO_CELL;
		if (map.height != h || map.width != w)
			map = new Cartesian(mesh, w, h);
		
		// interpolate Z values from the latest mesh
		double rArray[][] = map.interpolate(rainMap);
		
		// use height to generate background colors
		for(int r = 0; r < h; r++)
			for(int c = 0; c < w; c++) {
				// interpolate height (from surrounding MeshPoints)
				double rain = rArray[r][c];
	
				// shade a rectangle w/cyan for that rainfall	
				double shade = logarithmic(RAIN_DIM, RAIN_BRITE, rain / parms.r_range, 0.2);
				g.setColor(new Color(0, (int) shade, (int) shade));
				g.fillRect(c * TOPO_CELL, r * TOPO_CELL, TOPO_CELL, TOPO_CELL);
			}
	}
	
	/**
	 * Render current mesh as a topology map
	 * 
	 *   for each <row,col>
	 *   	compute interpolated height
	 *   	generate shaded background
	 *   for each topo line
	 *   	for each <row,col>
	 *   		compute an over/under bit-map
	 *   	for each <row,col>
	 *   		examine neighbors in over/under map
	 *   		choose and render the appropraite image
	 */
	private void paint_topo(Graphics g) {
		// see if we have any topology to render
		if (mesh == null || mesh.vertices.length == 0)
			return;
		
		// see if a screen resize has invalidated Cartesian translation
		int h = getHeight()/TOPO_CELL;
		int w = getWidth()/TOPO_CELL;
		if (map.height != h || map.width != w)
			map = new Cartesian(mesh, w, h);
		
		// interpolate Z values from the latest mesh
		double zArray[][] = map.interpolate(heightMap);
		
		// use height to generate background colors
		for(int r = 0; r < h; r++)
			for(int c = 0; c < w; c++) {
				// interpolate height (from surrounding MeshPoints)
				double z = zArray[r][c];
	
				// background color for altitude if nobody else needs it
				if ((display & (SHOW_MESH+SHOW_POINTS+SHOW_RAIN)) == 0) {
					if ((display & SHOW_WATER) != 0 && z < parms.sea_level) {
						g.setColor(RIVER_COLOR);
					} else {
						double shade = linear(TOPO_DIM, TOPO_BRITE, z + Parameters.z_extent/2);
						g.setColor(new Color((int) shade, (int) shade, (int) shade));
					}
					g.fillRect(c * TOPO_CELL, r * TOPO_CELL, TOPO_CELL, TOPO_CELL);
				}
			}
		
		// if we aren't doing topo lines, we're done
		if ((display & SHOW_TOPO) == 0)
			return;
		
		// allocate an over-under bitmap
		boolean over_under[][] = new boolean[map.height][map.width];
		
		// figure out how many topographic lines we have to render
		double deltaH = topoMajor / topoMinors;
		int maxLines = (int) (1 + Parameters.z_extent/deltaH);
		for (int line = 0; line < maxLines; line++) {
			double z = line * deltaH - Parameters.z_extent/2;
			if ((display & SHOW_WATER) != 0 && z < parms.sea_level)
				continue;
			
			boolean major = (line % topoMinors) == 0;
			
			// create an over/under bitmap for this isoline
			for(int r = 0; r < h; r++)
				for(int c = 0; c < w; c++)
					over_under[r][c] = zArray[r][c] > z;
					
			// choose a line color for this isoline
			//		major lines are full dark or full bright
			//		minor lines contrast with their background
			double shade;
			if (major)
				if (z <= 0)
					shade = TOPO_BRITE;
				else
					shade = TOPO_DIM;
			else {
				double range = (TOPO_BRITE + TOPO_DIM)/2;
				if (z <= 0) {	// from bright to neutral gray
					double base = 3 * range / 2;
					shade = base + range * z;
				} else {		// from dark to neutral gray
					double base = 3 * range / 4;
					shade = base + range * z;
				}
			}
			g.setColor(new Color((int) shade, (int) shade, (int) shade));
			
			/*
			 * Marching Squares topology generation algorithm
			 * 
			 *  https://en.wikipedia.org/wiki/Marching_squares
			 * 
			 * 	march a 2x2 square through the over/under array
			 * 	for each point, note which neighbors are over
			 *  use that four-bit-number to choose one of 16 images
			 */
			for(int r = 0; r < h-1; r++)
				for(int c = 0; c < w-1; c++) {
					int sum = 0;
					if (over_under[r][c])
						sum += 8;
					if (over_under[r][c+1])
						sum += 4;
					if (over_under[r+1][c])
						sum += 1;
					if (over_under[r+1][c+1])
						sum += 2;
					topoCell(g, r, c, sum);
				}	
		}
	}
	
	private void paint_water(Graphics g) {
		// make sure we have something to display
		if (mesh == null || mesh.vertices.length == 0)
			return;
		
		// collect display parameters
		int height = getHeight();
		int width = getWidth();
		double x_extent = parms.x_extent;
		double y_extent = parms.y_extent;
		
		// calculate the per-cell flow and peak flow
		calc_downhill();
		FlowMap f = new FlowMap(this);
		double[] flux = f.calculate();
		double peakFlux = 0;
		for(int i = 0; i < flux.length; i++)
			if (flux[i] > peakFlux)
				peakFlux = flux[i];
		
		// draw the rivers
		double threshold = peakFlux * 0.1;	// FIX what is display threshold
		for(int i = 0; i < flux.length; i++) {
			if (heightMap[i] < parms.sea_level)
				continue;	// don't display rivers under the ocean
			if (flux[i] < threshold)
				continue;	// don't display flux below critical level
			// System.out.println("flux["+i+"]=" + flux[i] + "->" + f.downHill[i]);
			if (downHill[i] >= 0) {
				int d = downHill[i];
				double x1 = (mesh.vertices[i].x + x_extent/2) * width;
				double y1 = (mesh.vertices[i].y + y_extent/2) * height;
				double x2 = (mesh.vertices[d].x + x_extent/2) * width;
				double y2 = (mesh.vertices[d].y + y_extent/2) * height;
				//double hue = 255 * flux[i]/peakFlux;
				//g.setColor(new Color(0, 0, (int) hue));
				g.setColor(RIVER_COLOR);
				g.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
			} else {
				// TODO: render basins
			}
		}
	}
	/**
	 * render a 5x5 block of a topo map
	 * @param Graphics context
	 * @param topographic row
	 * @param topographic column
	 * @param sum of corners (from Marching Square)
	 * 
	 * NOTE: The "Marching Square" topology line generation
	 *       scheme works best with 3x3 or 5x5 cells.
	 */
	private void topoCell(Graphics g, int r, int c, int sum) {
		int x = c * TOPO_CELL;
		int y = r * TOPO_CELL;
		
		switch(sum) {
		case 0:		// all below
		case 15:	// all above
			break;		
		case 1:		// lower left above
		case 14:	// lower left below
			g.drawLine(x,y+3, x+1, y+4);
			break;
		case 2:		// lower right above
		case 13:	// lower right below
			g.drawLine(x+3, y+4, x+4, y+3);
			break;
		case 3:		// bottom above
		case 12:	// bottom below
			g.drawLine(x,y+2, x+4, y+2);
			break;
		case 4:		// upper right above
		case 11:	// upper right below
			g.drawLine(x+3, y, x+4, y+1);
			break;
		case 5:		// sw/ne saddle
			g.drawLine(x+3, y+4, x+4, y+3);
			g.drawLine(x, y+1, x+1, y);
			break;
		case 6:		// right above
		case 9:		// left above
			g.drawLine(x+2, y, x+2, y+4);
			break;
		case 7:		// upper left below
		case 8:		// upper left above
			g.drawLine(x, y+1, x+1, y);
			break;
		case 10:	// nw/se saddle
			g.drawLine(x,y+3, x+1, y+4);
			g.drawLine(x+3, y, x+4, y+1);
			break;
		}
	}
	
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
