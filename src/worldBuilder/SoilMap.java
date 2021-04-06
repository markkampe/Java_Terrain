package worldBuilder;

import java.awt.Color;
import java.awt.Graphics;

/**
 * a class to render the MeshPoint soil types in shades of yellow-to-blue
 */
public class SoilMap {

	private static final int DARK = 0;
	
	private static int ALLUVIAL;
	
	private Map map;
	private boolean show_soil;
	
	/**
	 * instantiate a soil map generator
	 * @param map to be displayed
	 * @param show_soil should we display soil type
	 * @param show_hydro should we display water content
	 */
	public SoilMap(Map map, boolean show_soil, boolean show_hydro) {
		this.map = map;
		this.show_soil = show_soil;
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
					Color color = new Color(DARK, DARK, DARK);
					if (show_soil)
						color = previewColors[s];
					else						// nothing to see
						continue;
					
					g.setColor(color);
					g.fillRect(c * cellWidth, r * cellWidth, cellWidth, cellWidth);
				}
	}
}

