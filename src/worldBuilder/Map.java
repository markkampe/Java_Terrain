package worldBuilder;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.*;

public class Map extends JPanel {
	/**
	 * a Map is the displayable form of a Mesh.
	 * 
	 *   it may include more continuous maps of height and 
	 *   other attributes, but all of those are deterministic
	 *   functions, computed from the Mesh.
	 * 
	 *   
	 * The map window is resizable, and the mesh is unaware of the
	 * display window size.
	 */

	// types of displays
	public static final int SHOW_ALL = 0xff;
	public static final int SHOW_POINTS = 0x01;
	public static final int SHOW_MESH = 0x02;
	public static final int SHOW_TOPO = 0x04;
	public static final int SHOW_RAIN = 0x08;
	public static final int SHOW_WATER = 0x10;
	public static final int SHOW_SOIL = 0x20;
	private int display = SHOW_MESH;
	
	// types of select
	public static final int SEL_NONE = 0;
	public static final int SEL_POINT = 1;
	public static final int SEL_LINEAR = 2;
	public static final int SEL_RECTANGULAR = 3;
	private int selectX;
	private int selectY;
	private int selectDx;
	private int selectDy;
	private int selectType;
	
	// map size (in pixels)
	private static final int MIN_WIDTH = 400;	// min screen width
	private static final int MIN_HEIGHT = 400;	// min screen height
	private static final int SMALL_POINT = 2;	// width of a small point
	private static final int LARGE_POINT = 4;	// width of a large point
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
	
	// the underlying collection of points and edges
	private Mesh mesh;
	
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
	 * set the mesh to be displayed
	 */
	public void setMesh(Mesh m) {
		setVisible(false);
		this.mesh = m;
		setVisible(true);
		repaint();
	}
	
	/**
	 * @return reference to current mesh
	 */
	public Mesh getMesh() {
		return this.mesh;
	}

	/**
	 * create or update a selection 
	 * @param	initial X
	 * @param	initial Y
	 * @param 	x distance
	 * @param 	y distance
	 * @param 	linear/rectangular
	 */
	public void select(int xStart, int yStart, int dx, int dy, int selection) {
		selectX = xStart;
		selectY = yStart;
		selectDx = dx;
		selectDy = dy;
		selectType = selection;
		
		repaint();
	}
	
	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		// get scaling factors
		double x_extent = parms.x_extent;
		double y_extent = parms.y_extent;
		double z_extent = parms.z_extent;
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
			MapPoint[] points = mesh.vertices;
			for (int i = 0; i < points.length; i++) {
				MapPoint p = points[i];
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
		if ((display & SHOW_TOPO) != 0) {
			MapPoint[] points = mesh.vertices;
			for (int i = 0; i < points.length; i++) {
				MapPoint p = points[i];
				double x = ((p.x + x_extent / 2) * width) - LARGE_POINT / 2;
				double y = ((p.y + y_extent / 2) * height) - LARGE_POINT / 2;
				double z = ((p.z + z_extent / 2)) * (TOPO_BRITE - TOPO_DIM);
				int c = TOPO_DIM + (int) z;
				g.setColor(new Color(c,c,c));
				g.drawOval((int) x, (int) y, LARGE_POINT, LARGE_POINT);
			}
		}
		
		// see if we have a selection area to draw
		if (selectDx != 0 || selectDy != 0) {
			int x = selectX;
			int dx = selectDx;
			int y = selectY;
			int dy = selectDy;
			
			g.setColor(SELECT_COLOR);
			if (selectType == SEL_RECTANGULAR) {
				if (selectDx < 0) {
					x += selectDx;
					dx = -dx;
				}
				if (dy < 0) {
					y += selectDy;
					dy = -dy;
				}
				g.drawRect(x, y, dx, dy);
			} else if (selectType == SEL_LINEAR) {
				g.drawLine(x,  y,  x+dx, y+dy);
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
