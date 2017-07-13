import java.awt.Color;
import java.awt.Graphics;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * displays a set of MapPoints in a pop-up window
 */
public class MeshDisplay extends JPanel {

	private Color background;
	private int width;
	private int height;
	private JFrame frame;
	private static final long serialVersionUID = 0;
	
	private MapPoint[] mesh;
	private Color color;

	/**
	 * display a mesh of points on a 2D map
	 * 
	 * @param title name of the map
	 * @param set of MapPoints to display
	 * @param width field width (in pixels)
	 * @param height field height (in pixels)
	 * @param background color
	 * @param foreground color
	 */
	public MeshDisplay(String title, MapPoint[] pointset, int width, int height, Color background, Color foreground) {
		frame = new JFrame(title);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(this);
		frame.setSize(width, height);
		frame.setVisible(true);

		this.background = background;
		this.color = foreground;
		this.width = width;
		this.height = height;
		this.mesh = pointset;
	}

	/**
	 * paintComponent ... called to update the display
	 */
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		setBackground(background);
		g.setColor(color);
		
		// for every point in the map
		for(int i = 0; i < mesh.length; i++) {
			MapPoint m = mesh[i];
			double myX = (m.x+0.5) * width;
			double myY = (m.y+0.5) * height;
			// for each neighbor
			// FIX this is redundant
			for(int j = 0; j < m.neighbors; j++) {
				double hisX = (m.neighbor[j].x + 0.5) * width;
				double hisY = (m.neighbor[j].y + 0.5) * height;
				g.drawLine((int) myX,  (int) myY,  (int) hisX,  (int) hisY);
			}
		}
	}
}
