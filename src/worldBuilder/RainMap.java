package worldBuilder;

import java.awt.Color;
import java.awt.Graphics;

public class RainMap {
	// rain fall is indicated by shades of cyan
	private static final int RAIN_DIM = 0;
	private static final int RAIN_BRITE = 255;

	private Map map;
	private Parameters parms;

	public RainMap(Map map) {
		this.map = map;
		this.parms = Parameters.getInstance();
	}

	/**
	 * Render current mesh as a rainfall map
	 * 
	 * @param g Graphics context
	 * @param width of the display map
	 * @param height of the display map
	 * @param cellWidth - pixels per cell
	 * 
	 * for each (row,col) compute interpolated rain generate shaded background
	 */
	public void paint(Graphics g, int width, int height, int cellWidth) {

		int h = height / cellWidth;
		int w = width / cellWidth;

		// interpolate Z values from the latest mesh
		double rArray[][] = map.getCartesian().interpolate(map.getRainMap());

		// use height to generate background colors
		for (int r = 0; r < h; r++)
			for (int c = 0; c < w; c++) {
				// interpolate height (from surrounding MeshPoints)
				double rain = rArray[r][c];

				// shade a rectangle w/cyan for that rainfall
				double shade = Map.logarithmic(RAIN_DIM, RAIN_BRITE, rain / parms.rain_max, 0.2);
				g.setColor(new Color(0, (int) shade, (int) shade));
				g.fillRect(c * cellWidth, r * cellWidth, cellWidth, cellWidth);
			}
	}

}
