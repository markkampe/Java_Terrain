package worldBuilder;

import java.awt.Color;
import java.awt.Graphics;

/**
 * a class to render the fauna as colored regions
 */
public class FaunaMap {
	private Map map;		// mesh to which we correspond
	private int only;		// show only one type

	/**
	 * instantiate a river and water-body map renderer
	 * @param map	to be rendered
	 * @param name only type to be displayed
	 */
	public FaunaMap(Map map, String name) {
		this.map = map;
		if (name == null)
			only = -1;
		else
			only = map.getFaunaType(name);
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
		double fauna[][] = cart.nearest(map.getFaunaMap());
		
		// look up the type to color mapping
		Color[] colors = map.faunaColors;
		if (colors == null)
			return;
		
		// use flora types to generate background colors
		for(int r = 0; r < h; r++)
			for(int c = 0; c < w; c++) {
				int f = (int) fauna[r][c];	// round down fauna type
				if (f > 0 && (only < 0 || only == f)) {
					g.setColor(colors[f]);
					g.fillRect(c * cellWidth,  r * cellWidth, cellWidth, cellWidth);
				}
			}
	}
}
