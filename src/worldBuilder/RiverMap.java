package worldBuilder;

import java.awt.Color;
import java.awt.Graphics;

public class RiverMap {
	private Map map;		// mesh to which we correspond

	private static final int WATER_DIM = 128;	// dimmest water
	
	private Parameters parms;
	
	public RiverMap(Map m) {
		this.map = m;
		this.parms = Parameters.getInstance();
	}
	
	
	
	
	/**
	 * Display the streams and rivers
	 * 
	 * @param graphics context
	 * @param display map width
	 * @param display map height
	 */
	public void paint(Graphics g, int width, int height) {
		
		Mesh mesh = map.getMesh();
		double[] heightMap = map.getHeightMap();
		int downHill[] = map.getDownHill();
		double flux[] = map.getFluxMap();
		
		// calculate the color curve (blue vs flow)
		int blue_range = 255 - WATER_DIM;
		double min_stream = parms.stream_flux;
		double dBdF = blue_range/(map.max_flux - min_stream);
		
		// draw the streams, rivers, lakes and oceans
		for(int i = 0; i < flux.length; i++) {
			if (heightMap[i] < parms.sea_level)
				continue;	// don't display rivers under the ocean
			if (flux[i] < min_stream)
				continue;	// don't display flux below stream cut-off
			if (downHill[i] >= 0) {
				int d = downHill[i];
				double x1 = (mesh.vertices[i].x + Parameters.x_extent/2) * width;
				double y1 = (mesh.vertices[i].y + Parameters.y_extent/2) * height;
				double x2 = (mesh.vertices[d].x + Parameters.x_extent/2) * width;
				double y2 = (mesh.vertices[d].y + Parameters.y_extent/2) * height;
				
				// if a river segment flows into the sea, halve its length
				if (heightMap[d] < parms.sea_level) {
					x2 = (x1 + x2)/2;
					y2 = (y1 + y2)/2;
				}
				// blue gets brighter, green dimmer w/increasing flow
				double delta = (flux[i] - min_stream) * dBdF;
				double blue = WATER_DIM + delta;
				double green = Math.max(0, WATER_DIM - delta);
				g.setColor(new Color(0, (int) green, (int) blue));
				g.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
			} else {
				// TODO: render basins
			}
		}
	}
}
