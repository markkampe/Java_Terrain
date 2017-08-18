package worldBuilder;

import java.awt.Color;
import java.awt.Graphics;

public class ErodeMap {

	private static final int DIM = 0;
	private static final int BRITE = 255;
	private Map map;
	private Parameters parms;
	
	public ErodeMap(Map map) {
		this.map = map;
		this.parms = Parameters.getInstance();
	}

	/**
	 * Render current mesh as an altitude map
	 * 
	 * @param graphics context
	 * @param display map width
	 * @param display map height
	 * @param pixels per displayed cell
	 */
	public void paint(Graphics g, int width, int height, int cellWidth) {
		int h = height/cellWidth;
		int w = width/cellWidth;
		
		// interpolate erosion values from the latest mesh
		Cartesian cart = map.getCartesian();
		double eArray[][] = cart.interpolate(map.getErodeMap());
		
		// find the maximum and minimum erosions/depositions
		double max_erode = 0;
		double max_deposit = 0;
		for(int r = 0; r < h; r++)
			for(int c = 0; c < w; c++) {
				double e = eArray[r][c];
				if (e > max_erode)
					max_erode = e;
				else if (e < max_deposit)
					max_deposit = e;
			}
		
		// render each cell according to its erosion/deposition
		for(int r = 0; r < h; r++)
			for(int c = 0; c < w; c++) {
				double e = eArray[r][c];
				if (e > 0) {		// erosion
					e /= max_erode;
					double shade = linear(DIM, BRITE, e);
					g.setColor(new Color((int) shade, (int) shade, 0));
				} else if (e < 0) {	// deposition
					e /= max_deposit;
					double shade = linear(DIM, BRITE, e);
					g.setColor(new Color((int) shade, 0, (int) shade));
				}
				g.fillRect(c * cellWidth, r * cellWidth, cellWidth, cellWidth);
			}
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
}