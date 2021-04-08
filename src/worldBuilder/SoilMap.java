package worldBuilder;

import java.awt.Color;
import java.awt.Graphics;

/**
 * a class to render the MeshPoint soil types in shades of yellow-to-blue
 */
public class SoilMap {
	private static int ALLUVIAL;
	
	private Map map;
	
	/**
	 * instantiate a soil map generator
	 * @param map to be displayed
	 */
	public SoilMap(Map map) {
		this.map = map;
		ALLUVIAL = map.getSoilType("Alluvial");
	}

	/**
	 * Render current mesh as an altitude map
	 * 
	 * @param g Graphics context
	 * @param width of the display map
	 * @param height of the display map
	 * @param cellWidth - pixels per cell
	 */
	public void paint(Graphics g, int width, int height, int cellWidth) {
			int h = height/cellWidth;
			int w = width/cellWidth;
			double dArray[][] = map.getTileDepths();
			
			// interpolate values from the latest mesh
			Cartesian cart = map.getCartesian(Cartesian.vicinity.POLYGON);
			double sArray[][] = cart.nearest(map.getSoilMap());
			double eArray[][] = cart.interpolate(map.getErodeMap());
			
			// use soil type to generate background colors
			Color[] previewColors = map.rockColors;
			for(int r = 0; r < h; r++)
				for(int c = 0; c < w; c++) {
					if (dArray[r][c] > 0)	// ignore under water
						continue;
					int s = (int) sArray[r][c];	// soil type
					if (eArray[r][c] < 0)		// negative erosion is alluvial
						s = ALLUVIAL;
					if (s > 0) {
						Color color = previewColors[s];
						g.setColor(color);
						g.fillRect(c * cellWidth, r * cellWidth, cellWidth, cellWidth);
					}
				}
	}
}

