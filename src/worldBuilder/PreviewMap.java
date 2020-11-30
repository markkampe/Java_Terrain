package worldBuilder;

import javax.swing.JPanel;

import javax.swing.JFrame;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 * generic (small) preview map generator
 */
public class PreviewMap extends JPanel {
	
	private Color colormap[][];	// map to be rendered
	private int height;		// height (Tiles)
	private int width;		// width (tiles)
	private int size;		// pixels per tile
	
	/** icons to be overlayed on top of Preview map	*/
	private class Icon {
		int row;			// y coordinate of upper-left corner
		int col;			// x coordinate of upper-left corner
		BufferedImage image; // icon image
		
		public Icon(int row, int col, BufferedImage image) {
			this.row = row;
			this.col = col;
			this.image = image;
		}
	}
	LinkedList<Icon> icons;
	
	private static final int MIN_PIXELS = 400;
	
	private static final long serialVersionUID = 1L;

	/**
	 * instantiate a preview map JFrame
	 * @param name to be displayed at the top of map
	 * @param array (2D) of Colors to be displayed
	 * @param size (in pixels) of a tile (1->default)
	 */
	public PreviewMap(String name, Color array[][], int size) {
		
		// get the dimensions of the provided map
		height=array.length;
		width = array[0].length;
		
		// figure out how many pixels we want per tile
		if (size <= 1) {
			size = 1;
			while( size * width < MIN_PIXELS || size * height < MIN_PIXELS)
				size++;
		}
		this.size = size;
		
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
	 * overlay an icon on the Preview Map
	 * @param row	(tile) row number
	 * @param col	(tile) column number
	 * @param icon	icon image
	 */
	public void addIcon(int row, int col, BufferedImage icon) {
		if (icons == null)
			icons = new LinkedList<Icon>();
		
		icons.add(new Icon(row * size, col * size, icon));
	}
	
	/**
	 * repaint the built-up map
	 */
	public void paint (Graphics g) {
		// draw rectangles for each tile
		for(int x = 0; x < width * size; x += size)
			for(int y = 0; y < height * size; y += size) {
				g.setColor(colormap[y/size][x/size]);
				g.fillRect(x, y, size, size);
			}
		
		// see if we have been given any icons to overlay
		if (icons == null)
			return;
		
		g.setColor(Color.BLACK);
		for( ListIterator<Icon> it = icons.listIterator(); it.hasNext();) {
			Icon i = it.next();
			
			// get the array of pixels
			int height = i.image.getHeight();
			int width = i.image.getWidth();
			int[] pixels = new int[width*height];
			i.image.getRaster().getPixels(0, 0, width, height, pixels);
			
			// paint only the black pixels
			for(int y = 0; y < height; y++) {
				for(int x = 0; x < width; x++) {
					if (pixels[(y * width) + x] == 0)
						g.drawLine(i.col + x, i.row + y, i.col + x, i.row + y);
				}
			}
		}
	}
}
