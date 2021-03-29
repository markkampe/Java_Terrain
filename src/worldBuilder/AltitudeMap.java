package worldBuilder;

import java.awt.Color;
import java.awt.Graphics;

import worldBuilder.Cartesian.vicinity;

/**
 * a class to render the world mesh as a 2D brightness-for-altitude image
 */
public class AltitudeMap {

	// topographic lines are shades of gray
	private static final int TOPO_DIM = 0;
	private static final int TOPO_BRITE = 255;
	
	private Map map;
	//private Parameters parms;
	
	/**
	 * instantiate a (brightness) height map renderer
	 * @param map	to be rendered
	 */
	public AltitudeMap(Map map) {
		this.map = map;
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
			
			// interpolate Z values from the latest mesh
			Cartesian cart = map.getCartesian(vicinity.POLYGON);
			double zArray[][] = cart.interpolate(map.getHeightMap());
			double eArray[][] = cart.interpolate(map.getErodeMap());
			double dArray[][] = cart.interpolate(map.getDepthMap());
			
			// use height to generate background colors
			for(int r = 0; r < h; r++)
				for(int c = 0; c < w; c++) {
					if (dArray[r][c] < 0)	// cell is under water
						continue;
					
					double z = zArray[r][c] - eArray[r][c];
					double shade = Map.linear(TOPO_DIM, TOPO_BRITE, z + Parameters.z_extent/2);
					g.setColor(new Color((int) shade, (int) shade, (int) shade));
					g.fillRect(c * cellWidth, r * cellWidth, cellWidth, cellWidth);
				}
	}
}
