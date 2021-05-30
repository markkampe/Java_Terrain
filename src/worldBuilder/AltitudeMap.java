package worldBuilder;

import java.awt.Color;
import java.awt.Graphics;

/**
 * a class to render the world mesh as a 2D brightness-for-altitude image
 */
public class AltitudeMap {

	// topographic lines are shades of gray
	private static final int TOPO_DIM = 0;
	private static final int TOPO_BRITE = 255;
	
	private Map map;
	private MapWindow window;
	//private Parameters parms;
	
	/**
	 * instantiate a (brightness) height map renderer
	 * @param map	to be rendered
	 */
	public AltitudeMap(Map map) {
		this.map = map;
		this.window = map.window;
		//this.parms = Parameters.getInstance();
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
			boolean show_water = ((window.display & MapWindow.SHOW_WATER) != 0);
			double[][] heights = window.getTileHeights();
			double[][] depths = window.getTileDepths();
			// use height to generate background colors
			for(int r = 0; r < h; r++)
				for(int c = 0; c < w; c++) {
					if (show_water && depths[r][c] > 0)	// cell is under water
						continue;
					
					double z = heights[r][c];
					double shade = MapWindow.linear(TOPO_DIM, TOPO_BRITE, z + Parameters.z_extent/2);
					g.setColor(new Color((int) shade, (int) shade, (int) shade));
					g.fillRect(c * cellWidth, r * cellWidth, cellWidth, cellWidth);
				}
	}
}
