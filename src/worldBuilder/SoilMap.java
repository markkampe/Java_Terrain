package worldBuilder;

import java.awt.Color;
import java.awt.Graphics;

public class SoilMap {

	// topographic lines are shades of gray
	private static final int DARK = 0;
	private static final int DIM = 128;
	private static final int BRITE = 255;
	
	private Map map;
	//private Parameters parms;
	
	public SoilMap(Map map) {
		this.map = map;
		//this.parms = Parameters.getInstance();
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
					if (eArray[r][c] < 0)
						s = Map.ALLUVIAL;
					double sat = Hydrology.saturation[s];
					double hydration = hArray[r][c] / sat;
					int shade;
					Color color;
					switch(s) {
					case Map.IGNEOUS:
						shade = (int) Map.linear(DARK, DIM, hydration);
						color = new Color(shade, shade, shade);
						break;
					case Map.METAMORPHIC:
						shade = (int) Map.linear(DIM, BRITE, hydration);
						color = new Color(shade, shade, shade);
						break;
					case Map.SEDIMENTARY:
						shade = (int) Map.linear(DIM, BRITE, hydration);
						color = new Color(shade, shade, 0);
						break;
					case Map.ALLUVIAL:
						shade = (int) Map.linear(DIM, BRITE, hydration);
						color = new Color(0, shade, 0);
						break;
					default:
						continue;
					}
					g.setColor(color);
					g.fillRect(c * cellWidth, r * cellWidth, cellWidth, cellWidth);
				}
			
	}
}

