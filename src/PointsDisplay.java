import java.awt.Color;
import java.awt.Graphics;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * displays a set of MapPoints in a pop-up window
 */
public class PointsDisplay extends JPanel {
	
	private static final int HEIGHT = 400;
	private static final int WIDTH = 400;
	private static final int POINTSIZE = 3;
	private static final Color FOREGROUND = Color.WHITE;
	private static final Color BACKGROUND = Color.BLACK;
	
	private MapPoints points;
	private Color background;
	private Color foreground;
	
	private static final long serialVersionUID = 0;

	/**
	 * display a set of points on a 2D map
	 * 
	 * @param title	name of the map
	 * @param set	set of points to be plotted
	 * @param width	field width (in pixels)
	 * @param height field height (in pixels)
	 */
	public PointsDisplay(String title, MapPoints set, 
			int width, int height, Color foreround, Color background) {
		JFrame frame = new JFrame(title);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(this);
		frame.setSize(width, height);
		frame.setVisible(true);
		
		this.points = set;
		this.foreground = foreground;
		this.background = background;
	}
	
	/**
	 * a more convenient constructor with default parameters
	 * 
	 * @param title	window title
	 * @param set	set MapPoints
	 */
	public PointsDisplay(String title, MapPoints set) {
		JFrame frame = new JFrame(title);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(this);
		frame.setSize(WIDTH, HEIGHT);
		frame.setVisible(true);
		
		this.points = set;
		this.foreground = FOREGROUND;
		this.background = BACKGROUND;
	}
	
	/**
	 * paintComponent ... called to update the display
	 */
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		setBackground(background);
		setForeground(foreground);
		for( int i = 0; i < points.length(); i++) {
			MapPoint point = points.point(i);
			double myX = getWidth() * (point.x + .5);
			double myY = getHeight() * (point.y + .5);
			g.drawOval((int) myX, (int) myY, POINTSIZE, POINTSIZE);
		}
	}
	

}
