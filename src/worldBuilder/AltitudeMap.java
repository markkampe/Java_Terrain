package worldBuilder;

import java.awt.Color;
import java.awt.Graphics;

public class AltitudeMap {

	// topographic lines are shades of gray
	private static final int TOPO_DIM = 0;
	private static final int TOPO_BRITE = 255;
	
	private Map map;
	private Parameters parms;
	
	public AltitudeMap(Map map) {
		this.map = map;
		this.parms = Parameters.getInstance();
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
			
			// interpolate Z values from the latest mesh
			double zArray[][] = map.getCartesian().interpolate(map.getHeightMap());
			
			// use height to generate background colors
			for(int r = 0; r < h; r++)
				for(int c = 0; c < w; c++) {
					double z = zArray[r][c];
					if (z < parms.sea_level)
						continue;
					double shade = linear(TOPO_DIM, TOPO_BRITE, z + Parameters.z_extent/2);
					g.setColor(new Color((int) shade, (int) shade, (int) shade));
					g.fillRect(c * cellWidth, r * cellWidth, cellWidth, cellWidth);
				}
			
	}
	
	/**
	 * linear interpolation of a (color) value within a range
	 * 
	 * @param min return value
	 * @param max return value
	 * @param value (0-1) to be scaled
	 */
	private static double linear(int min, int max, double value) {
		double ret = value * (max - min);
		return min + ret;
	}
}
