package JsonDump;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import javax.swing.JPanel;
import javax.swing.JFrame;

/**
 * this class puts up a graphical rendition of a specified map
 */
public class PreviewMap extends JPanel implements WindowListener {
	
	private static final int MAX_SIZE = 1024;
	private static final int MAX_PIXELS = 4;
	
	private static final long serialVersionUID = -1;
	
	public static int openWindows = 0;
	protected MapReader reader;
	protected String windowName;
	protected int rows;
	protected int cols;
	protected int box_size;
	protected Color[][] colorMap;

	public PreviewMap(String name, MapReader r) {
		windowName = name;
		reader = r;
		
		// figure out the map size
		rows = reader.height();
		cols = reader.width();
		int max = (rows > cols) ? rows : cols;
		box_size = (max <= MAX_SIZE/MAX_PIXELS) ? MAX_SIZE/max : MAX_PIXELS;
		
		colorMap = new Color[rows][cols];
	}
	
	public void display() {
		
		// create the map frame
		JFrame frame = new JFrame(windowName);
		frame.setSize(cols * box_size, rows * box_size);
		frame.setResizable(false);
		frame.add(this);
		frame.setVisible(true);
		frame.addWindowListener(this);
		openWindows++;
	}
	
	/**
	 * update the display based on the 
	 */
	public void paint( Graphics g) {
		for(int r = 0; r < rows; r++)
			for(int c = 0; c < cols; c++) {
				g.setColor(colorMap[r][c]);
				g.fillRect(c * box_size, r * box_size, box_size, box_size);
			}
	}

	public void windowClosing(WindowEvent arg0) {openWindows--;}
	public void windowActivated(WindowEvent arg0) {}
	public void windowClosed(WindowEvent arg0) {}
	public void windowDeactivated(WindowEvent arg0) {}
	public void windowDeiconified(WindowEvent arg0) {}
	public void windowIconified(WindowEvent arg0) {}
	public void windowOpened(WindowEvent arg0) {}
}
