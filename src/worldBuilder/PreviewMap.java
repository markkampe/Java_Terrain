package worldBuilder;

import javax.swing.JPanel;
import javax.swing.JFrame;

import java.awt.Color;
import java.awt.Graphics;

/**
 * generic (small) preview map generator
 */
public class PreviewMap extends JPanel {
	
	private Color colormap[][];	// map to be rendered
	private int height;		// height (Tiles)
	private int width;		// width (tiles)
	private int size;		// pixels per tile
	
	private static final int MIN_PIXELS = 400;
	
	private static final long serialVersionUID = 1L;

	/**
	 * instantiate a preview map JFrame
	 * @param name to be displayed at the top of map
	 * @param array (2D) of Colors to be displayed
	 */
	public PreviewMap(String name, Color array[][]) {
		
		// get the dimensions of the provided map
		height=array.length;
		width = array[0].length;
		
		// figure out how many pixels we want per tile
		size = 1;
		while( size * width < MIN_PIXELS || size * height < MIN_PIXELS)
			size++;
		
		// create the window
		JFrame frame = new JFrame(name);
		frame.setSize(width * size, height * size);
		frame.setResizable(false);
		frame.add(this);
		frame.setVisible(true);
		
		// remember the map to paint
		colormap = array;
	}
	
	/**
	 * repaint the built-up map
	 */
	public void paint (Graphics g) {
		for(int x = 0; x < width * size; x += size)
			for(int y = 0; y < height * size; y += size) {
				g.setColor(colormap[y/size][x/size]);
				g.fillRect(x, y, size, size);
			}
	}
}
