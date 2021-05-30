package worldBuilder;

import java.awt.Color;
import java.awt.Graphics;

/**
 * a class to render the flora as colored regions
 */
public class FloraMap {
	private Map map;		// mesh to which we correspond

	/**
	 * instantiate a river and water-body map renderer
	 * @param map	to be rendered
	 */
	public FloraMap(Map map) {
		this.map = map;
	}
	
	/**
	 * Display the streams and rivers
	 * 
	 * @param g Graphics context
	 * @param width of the display map
	 * @param height of the display map
	 * @param, width (in pixels) of a single cell
	 */
	public void paint(Graphics g, int width, int height, int cellWidth) {
		int h = height/cellWidth;
		int w = width/cellWidth;
		
		// rather than interpolate, use the nearest mesh point
		Cartesian cart = map.window.getCartesian(Cartesian.vicinity.POLYGON);
		double flora[][] = cart.nearest(map.getFloraMap());
		
		// look up the type to color mapping
		Color[] colors = map.floraColors;
		if (colors == null)
			return;
		
		// use flora types to generate background colors
		for(int r = 0; r < h; r++)
			for(int c = 0; c < w; c++) {
				int f = (int) flora[r][c];	// round down flora type
				if (f > 0) {
					g.setColor(colors[f]);
					g.fillRect(c * cellWidth,  r * cellWidth, cellWidth, cellWidth);
				}
			}
	}
}
