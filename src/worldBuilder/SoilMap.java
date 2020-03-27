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
			Cartesian cart = map.getCartesian();
			double sArray[][] = cart.interpolate(map.getSoilMap());
			double hArray[][] = cart.interpolate(map.getHydrationMap());
			double eArray[][] = cart.interpolate(map.getErodeMap());
			
			// use soil type/hydration to generate background colors
			for(int r = 0; r < h; r++)
				for(int c = 0; c < w; c++) {
					if (hArray[r][c] < 0)	// ignore under water
						continue;
					int s = (int) sArray[r][c];	// soil type
					if (eArray[r][c] < 0)		// negative erosion is alluvial
						s = Map.ALLUVIAL;
					
					double hydration = 0.5;		// how saturated is the soil
					if (show_hydro)
						hydration = hArray[r][c] / Hydrology.saturation[s];
					int shade = (int) Map.linear(DIM, BRITE, hydration);
					Color color = new Color(0, 0, 0);
					if (show_soil) {
						switch(s) {
						case Map.IGNEOUS:		// shades of dark grey
							shade = (int) Map.linear(DARK, DIM, hydration);
							color = new Color(shade, shade, shade);
							break;
						case Map.METAMORPHIC:	// shades of light grey
							color = new Color(shade, shade, shade);
							break;
						case Map.SEDIMENTARY:	// shades of bright yellow
							color = new Color(shade, shade, 0);
							break;
						case Map.ALLUVIAL:		// shades of bright green
							color = new Color(0, shade, 0);
							break;
						default:				// no soil?
							continue;
						}
					} else if (show_hydro)		// shades of light blue
						color = new Color(0, shade, shade);
					else						// nothing to see
						continue;
					
					g.setColor(color);
					g.fillRect(c * cellWidth, r * cellWidth, cellWidth, cellWidth);
				}
			
	}
}

