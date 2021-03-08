package worldBuilder;

import java.awt.Color;
import java.awt.Graphics;

/**
 * a class to render world mesh erosion and deposition coloration:
 *   shades of violet for erosion, yellow for deposition.
 */
public class ErodeMap {

	private static final int DIM = 128;
	private static final int BRITE = 255;
	private static final double MIN_DEPOSITION = 0.05;	// meters
	
	private Map map;
	private Parameters parms;
	
	/**
	 * instantiate an erosion/deposition map renderer
	 * @param map	to be rendered
	 */
	public ErodeMap(Map map) {
		this.map = map;
		this.parms = Parameters.getInstance();
	}

	/**
	 * Render current mesh as an erosion/deposition map
	 * 
	 * @param g Graphics context
	 * @param width of the display map
	 * @param height of the display map
	 * @param cellWidth - pixels per cell
	 */
	public void paint(Graphics g, int width, int height, int cellWidth) {
		int h = height/cellWidth;
		int w = width/cellWidth;
		double min_deposition = parms.z(MIN_DEPOSITION);
		
		// note the range on our map
		double max_erosion = map.max_erosion;
		double max_deposition = map.max_deposition;
		
		// interpolate erosion values from the latest mesh
		Cartesian cart = map.getCartesian(Cartesian.vicinity.NEIGHBORS);
		double eArray[][] = cart.interpolate(map.getErodeMap());
		
		// render each cell according to its erosion/deposition
		for(int r = 0; r < h; r++)
			for(int c = 0; c < w; c++) {
				double e = eArray[r][c];
				if (e > 0) {		// erosion
					double shade = Map.logarithmic(DIM, BRITE, e/max_erosion, 0.1);
					g.setColor(new Color((int) shade, 0, (int) shade));
					g.fillRect(c * cellWidth, r * cellWidth, cellWidth, cellWidth);
				} else if (e < -min_deposition) {	// deposition
					double shade = Map.logarithmic(DIM, BRITE, -e/max_deposition, 0.1);
					g.setColor(new Color((int) shade, (int) shade, 0));
					g.fillRect(c * cellWidth, r * cellWidth, cellWidth, cellWidth);
				}
			}
	}
}
