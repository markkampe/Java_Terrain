package worldBuilder;

import java.awt.Color;
import java.awt.Graphics;

/**
 * a class to render a set of mesh points as a traditional topographic map image.
 */
public class TopoMap {
	// topographic lines are shades of gray
	private static final int TOPO_DIM = 0;
	private static final int TOPO_BRITE = 255;
	
	private Map map;			// map we are drawing from
	
	private Parameters parms;

	/**
	 * instantiate a (topographic lines) height map renderer
	 * @param map	to be rendered
	 */
	public TopoMap(Map map) {
		this.map = map;
		this.parms = Parameters.getInstance();
	}
	
	/**
	 * Render current mesh as a topographic map
	 * 
	 * @param g Graphics context
	 * @param width of the display map
	 * @param height of the display map
	 * @param cellWidth - pixels per cell
	 * 
	 * This method uses the Marching Squares algorithm.
	 *   for each topo line
	 *   	construct an over/under 2D bitmap
	 *   	march through the bitmap, summing neighbors
	 *   	use sum of neighbors to select an image
	 */
	public void paint(Graphics g, int width, int height, int cellWidth) {

		// see if a screen resize has invalidated Cartesian translation
		int h = height / cellWidth;
		int w = width / cellWidth;

		// interpolate Z values from the latest mesh
		Cartesian cart = map.getCartesian(Cartesian.vicinity.POLYGON);
		double zArray[][] = cart.interpolate(map.getHeightMap());
		double eArray[][] = cart.interpolate(map.getErodeMap());
		
		// allocate an over-under bitmap
		boolean over_under[][] = new boolean[zArray.length][zArray[0].length];

		// figure out how many topographic lines we have to render
		double deltaH = parms.z(parms.topo_minor);
		int maxLines = (int) (1 + (Parameters.z_extent / deltaH));
		
		for (int line = 0; line < maxLines; line++) {
			double z = line * deltaH - Parameters.z_extent / 2;
			boolean major = (line % parms.topo_major) == 0;

			// create an over/under bitmap for this isoline
			for (int r = 0; r < h; r++)
				for (int c = 0; c < w; c++)
					over_under[r][c] = zArray[r][c] - eArray[r][c] > z;

			// choose a line color for this isoline
			// 	major lines are full dark or full bright
			// 	minor lines contrast with their background
			double shade;
			if (major)
				if (z <= 0)
					shade = TOPO_BRITE;
				else
					shade = TOPO_DIM;
			else {
				double range = (TOPO_BRITE + TOPO_DIM) / 2;
				if (z <= 0) { // from bright to neutral gray
					double base = 3 * range / 2;
					shade = base + range * z;
				} else { // from dark to neutral gray
					double base = 3 * range / 4;
					shade = base + range * z;
				}
			}
			g.setColor(new Color((int) shade, (int) shade, (int) shade));

			/*
			 * Marching Squares topology generation algorithm
			 * 
			 * https://en.wikipedia.org/wiki/Marching_squares
			 * 
			 * march a 2x2 square through the over/under array for each point,
			 * note which neighbors are over use that four-bit-number to choose
			 * one of 16 images
			 */
			for (int r = 0; r < h - 1; r++)
				for (int c = 0; c < w - 1; c++) {
					int sum = 0;
					if (over_under[r][c])
						sum += 8;
					if (over_under[r][c + 1])
						sum += 4;
					if (over_under[r + 1][c])
						sum += 1;
					if (over_under[r + 1][c + 1])
						sum += 2;
					topoCell(g, r, c, sum);
				}
		}
	}

	/**
	 * render a 5x5 block of a topo map
	 * 
	 * @param Graphics context
	 * @param topographic row
	 * @param topographic column
	 * @param sum of corners (from Marching Square)
	 * 
	 * NOTE: The "Marching Square" topology line generation scheme
	 *       works best with 3x3 or 5x5 cells.
	 */
	private void topoCell(Graphics g, int r, int c, int sum) {
		final int cellWidth = 5;	// hard coded below
		int x = c * cellWidth;
		int y = r * cellWidth;

		switch (sum) {
		case 0: // all below
		case 15: // all above
			break;
		case 1: // lower left above
		case 14: // lower left below
			g.drawLine(x, y + 3, x + 1, y + 4);
			break;
		case 2: // lower right above
		case 13: // lower right below
			g.drawLine(x + 3, y + 4, x + 4, y + 3);
			break;
		case 3: // bottom above
		case 12: // bottom below
			g.drawLine(x, y + 2, x + 4, y + 2);
			break;
		case 4: // upper right above
		case 11: // upper right below
			g.drawLine(x + 3, y, x + 4, y + 1);
			break;
		case 5: // sw/ne saddle
			g.drawLine(x + 3, y + 4, x + 4, y + 3);
			g.drawLine(x, y + 1, x + 1, y);
			break;
		case 6: // right above
		case 9: // left above
			g.drawLine(x + 2, y, x + 2, y + 4);
			break;
		case 7: // upper left below
		case 8: // upper left above
			g.drawLine(x, y + 1, x + 1, y);
			break;
		case 10: // nw/se saddle
			g.drawLine(x, y + 3, x + 1, y + 4);
			g.drawLine(x + 3, y, x + 4, y + 1);
			break;
		}
	}
}
