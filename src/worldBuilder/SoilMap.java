package worldBuilder;

import java.awt.Color;
import java.awt.Graphics;

/**
 * a class to render the MeshPoint soil types in shades of yellow-to-blue
 */
public class SoilMap {

	// topographic lines are shades of gray
	private static final int DARK = 0;
	private static final int DIM = 128;
	private static final int BRITE = 255;
	
	private static int ALLUVIAL;
	
	private Map map;
	private boolean show_soil;
	private boolean show_hydro;
	
	/**
	 * instantiate a soil map generator
	 * @param map to be displayed
	 * @param show_soil should we display soil type
	 * @param show_hydro should we display water content
	 */
	public SoilMap(Map map, boolean show_soil, boolean show_hydro) {
		this.map = map;
		this.show_soil = show_soil;
		this.show_hydro = show_hydro;
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
			
			// interpolate values from the latest mesh
			Cartesian cart = map.getCartesian(Cartesian.NEAREST);
			double sArray[][] = cart.interpolate(map.getSoilMap());
			cart = map.getCartesian(Cartesian.NEIGHBORS);
			double hArray[][] = cart.interpolate(map.getHydrationMap());
			double eArray[][] = cart.interpolate(map.getErodeMap());
			
			// use soil type/hydration to generate background colors
			Color[] previewColors = map.rockColors;
			for(int r = 0; r < h; r++)
				for(int c = 0; c < w; c++) {
					if (hArray[r][c] < 0)	// ignore under water
						continue;
					int s = (int) sArray[r][c];	// soil type
					if (eArray[r][c] < 0)		// negative erosion is alluvial
						s = ALLUVIAL;
					double hydration = 0.5;		// how saturated is the soil
					if (show_hydro)
						hydration = hArray[r][c] / Hydrology.saturation[s];
					int shade = (int) Map.linear(DIM, BRITE, hydration);
					Color color = new Color(0, 0, 0);
					if (show_soil)
						color = previewColors[s];
					else if (show_hydro)		// shades of light blue
						color = new Color(0, shade, shade);
					else						// nothing to see
						continue;
					
					g.setColor(color);
					g.fillRect(c * cellWidth, r * cellWidth, cellWidth, cellWidth);
				}
			
	}
}

