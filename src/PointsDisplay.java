import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.rogach.jopenvoronoi.Point;

/**
 * displays a set of MapPoints in a pop-up window
 */
public class PointsDisplay extends JPanel {

	public enum Shape {
		CIRCLE, CROSS, DIAMOND, SQUARE
	};

	private static final int POINTSIZE = 4;
	private Color background;
	private int width;
	private int height;
	private JFrame frame;
	private static final long serialVersionUID = 0;

	private class DisplayPoint {
		public double x;
		public double y;
		public Color color;
		public Shape shape;

		public DisplayPoint(double x, double y, Shape shape, Color color) {
			this.x = x;
			this.y = y;
			this.shape = shape;
			this.color = color;
		}

		public String toString() {
			return ("<" + x + "," + y + ">");
		}
	}

	private List<DisplayPoint> points;

	/**
	 * display a set of points on a 2D map
	 * 
	 * @param title name of the map
	 * @param width field width (in pixels)
	 * @param height field height (in pixels)
	 * @param background color
	 */
	public PointsDisplay(String title, int width, int height, Color background) {
		frame = new JFrame(title);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(this);
		frame.setSize(width, height);

		this.background = background;
		this.width = width;
		this.height = height;
		this.points = new LinkedList<DisplayPoint>();
	}

	/**
	 * add a new set of points to the display
	 * @param pointset  ... points to be displayed
	 * @param shape ... shape to be used for these points
	 * @param color ... color to be used for these points
	 */
	public void addPoints(Point[] pointset, Shape shape, Color color ) {
		frame.setVisible(false);	// prevent display during update
		for( Point p: pointset) {
			points.add(new DisplayPoint((p.x + 0.5) * width, (p.y + 0.5) * height, shape, color));
		}
		frame.setVisible(true);
	}

	/**
	 * paintComponent ... called to update the display
	 */
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		setBackground(background);

		for (Iterator<DisplayPoint> iterator = points.iterator(); iterator.hasNext(); ) {
			DisplayPoint p = iterator.next();
			int x = (int) p.x - POINTSIZE / 2;
			int y = (int) p.y - POINTSIZE / 2;
			g.setColor(p.color);
			switch (p.shape) {
			case CROSS:
				g.drawLine(x, y + POINTSIZE / 2, x + POINTSIZE, y + POINTSIZE / 2);
				g.drawLine(x + POINTSIZE / 2, y, x + POINTSIZE / 2, y + POINTSIZE);
				continue;
			case CIRCLE:
				g.drawOval(x, y, POINTSIZE, POINTSIZE);
				continue;
			case SQUARE:
				g.drawLine(x, y, x + POINTSIZE, y); // top
				g.drawLine(x, y, x, y + POINTSIZE); // left
				g.drawLine(x + POINTSIZE, y, x + POINTSIZE, y + POINTSIZE); // right
				g.drawLine(x + POINTSIZE, y, x + POINTSIZE, y + POINTSIZE); // bottom
				continue;
			case DIAMOND:
				g.drawLine(x + POINTSIZE / 2, y, x, y + POINTSIZE / 2); // top
				g.drawLine(x + POINTSIZE / 2, y, x + POINTSIZE, y + POINTSIZE / 2); // topright
				g.drawLine(x, y + POINTSIZE / 2, x + POINTSIZE / 2, y + POINTSIZE); // bot left
				g.drawLine(x + POINTSIZE, y + POINTSIZE / 2, x + POINTSIZE / 2, y + POINTSIZE); // bot right
				continue;
			}
		}
	}
}
