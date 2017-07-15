import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;

import javax.swing.*;

public class Map extends JPanel {
	public static final int POINTS = 0x01;
	public static final int MESH = 0x02;
	public static final int TOPO = 0x04;
	public static final int RAIN = 0x08;
	public static final int WATER = 0x10;
	public static final int SOIL = 0x20;

	private Mesh mesh;
	private Dimension size;
	
	private Color background;
	private int display = MESH;
	private Parameters parms;
	
	private int selectX;
	private int selectY;
	private int selectDx;
	private int selectDy;
	
	private static final int MIN_WIDTH = 400;
	private static final int MIN_HEIGHT = 400;
	private static final int POINT_WIDTH = 2;

	private static Color SELECT_COLOR = Color.WHITE;
	private static Color POINT_COLOR = Color.PINK;
	private static Color MESH_COLOR = Color.GREEN;
	private static Color WATER_COLOR = Color.BLUE;
	private static Color RAIN_COLOR = Color.CYAN;
	private static Color SOIL_COLOR = Color.YELLOW;

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
	public Map(int width, int height, Color background) {
		size = new Dimension(width, height);
		this.background = background;
		parms = Parameters.getInstance();
	}

	/**
	 * enable/disable display elements
	 * 
	 * @param view
	 *            to be enabled/disabled
	 * @param on/off
	 * @return current sense of that view
	 */
	public boolean setDisplay(int view, boolean on) {
		if (on)
			display |= view;
		else
			display &= ~view;
		if (mesh != null)
			repaint();
		return (display & view) == 0 ? false : true;
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
	 * create or update a selection rectangle
	 */
	public void select(int xStart, int yStart, int dx, int dy) {
		selectX = xStart;
		selectY = yStart;
		selectDx = dx;
		selectDy = dy;
		repaint();
	}
	
	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		// get scaling factors
		double x_extent = parms.x_extent;
		double y_extent = parms.y_extent;
		int height = getHeight();
		int width = getWidth();

		// make sure we have something to display
		if (mesh == null) {
			setBackground(Color.WHITE);
			g.setColor(Color.BLACK);
			g.drawString("Generating Grid", width / 2, height / 2);
			return;
		}
		
		setBackground(background);
		
		// see if we are rendering points
		if ((display & POINTS) != 0) {
			g.setColor(POINT_COLOR);
			MapPoint[] points = mesh.vertices;
			System.out.println("drawing " + points.length + "points");
			for (int i = 0; i < points.length; i++) {
				MapPoint p = points[i];
				double x = ((p.x + x_extent / 2) * width) - POINT_WIDTH / 2;
				double y = ((p.y + y_extent / 2) * height) - POINT_WIDTH / 2;
				g.drawOval((int) x, (int) y, POINT_WIDTH, POINT_WIDTH);
			}
		}

		// see if we are rendering the mesh
		if ((display & MESH) != 0) {
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
		
		// see if we have a selection box to draw
		if (selectDx != 0 || selectDy != 0) {
			g.setColor(SELECT_COLOR);
			int x = selectX;
			int dx = selectDx;
			if (selectDx < 0) {
				x += selectDx;
				dx = -dx;
			}
			int y = selectY;
			int dy = selectDy;
			if (dy < 0) {
				y += selectDy;
				dy = -dy;
			}
			g.drawRect(x, y, dx, dy);
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
