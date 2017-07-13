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
	
	private Path[] mesh;
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
	public MeshDisplay(String title, Path[] paths, int width, int height, Color background, Color foreground) {
		frame = new JFrame(title);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(this);
		frame.setSize(width, height);
		frame.setVisible(true);

		this.background = background;
		this.color = foreground;
		this.width = width;
		this.height = height;
		this.mesh = paths;
	}

	/**
	 * paintComponent ... called to update the display
	 */
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		setBackground(background);
		g.setColor(color);
		
		for(int i = 0; i < mesh.length; i++) {
			Path p = mesh[i];
			double x1 = (p.source.x + 0.5) * width;
			double y1 = (p.source.y + 0.5) * height;
			double x2 = (p.target.x + 0.5) * width;
			double y2 = (p.target.y + 0.5) * height;
			g.drawLine((int) x1,  (int) y1,  (int) x2,  (int) y2);
		}
	}
}
