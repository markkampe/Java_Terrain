package worldBuilder;

import java.awt.Color;
import java.awt.Graphics;

public class AltitudeMap {

	// topographic lines are shades of gray
	private static final int TOPO_DIM = 0;
	private static final int TOPO_BRITE = 255;
	
	private Map map;
	//private Parameters parms;
	
	public AltitudeMap(Map map) {
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
			
			// interpolate Z values from the latest mesh
			Cartesian cart = map.getCartesian();
			double zArray[][] = cart.interpolate(map.getHeightMap());
			double eArray[][] = cart.interpolate(map.getErodeMap());
			double hArray[][] = cart.interpolate(map.getHydrationMap());
			
			// use height to generate background colors
			for(int r = 0; r < h; r++)
				for(int c = 0; c < w; c++) {
					if (hArray[r][c] < 0)	// cell is under water
						continue;
					
					double z = zArray[r][c] - eArray[r][c];
					double shade = Map.linear(TOPO_DIM, TOPO_BRITE, z + Parameters.z_extent/2);
					g.setColor(new Color((int) shade, (int) shade, (int) shade));
					g.fillRect(c * cellWidth, r * cellWidth, cellWidth, cellWidth);
				}
			
	}
}
