package worldBuilder;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;

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
		int downHill[] = map.getDownHill();
		double flux[] = map.getFluxMap();
		
		// calculate the color curve (blue vs flow)
		int blue_range = 255 - WATER_DIM;
		double min_stream = parms.stream_flux;
		double min_river = parms.river_flux;
		double min_artery = parms.artery_flux;
		double dBdF = blue_range/(min_river - min_stream);
		
		// draw the streams, rivers, lakes and oceans
		for(int i = 0; i < flux.length; i++) {
			if (flux[i] < min_stream)
				continue;	// don't display flux below stream cut-off
			if (downHill[i] >= 0) {
				int d = downHill[i];
				// ignore lines that are completely off map
				if (!map.on_screen(mesh.vertices[i].x, mesh.vertices[i].y) &&
				    !map.on_screen(mesh.vertices[d].x, mesh.vertices[d].y))
				   		continue;
				
				// figure out where the end-points are on screen
				int x1 = map.screen_x(mesh.vertices[i].x);
				int y1 = map.screen_y(mesh.vertices[i].y);
				int x2 = map.screen_x(mesh.vertices[d].x);
				int y2 = map.screen_y(mesh.vertices[d].y);
				
				// blue gets brighter, green dimmer w/increasing flow
				double delta = (flux[i] - min_stream) * dBdF;
				if (delta >= (255-WATER_DIM)) delta = (255-WATER_DIM);
				double blue = WATER_DIM + delta;
				double green = Math.max(0, WATER_DIM - delta);
				g.setColor(new Color(0, (int) green, (int) blue));
				// XXX use stroke-width for rivers
				g.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
				if (flux[i] < min_river)
					continue;
				g.drawLine((int) (x1+1), (int) (y1+1), (int) (x2+1), (int) (y2+1));
				if (flux[i] < min_artery)
					continue;
				g.drawLine((int) (x1-1), (int) (y1-1), (int) (x2-1), (int) (y2-1));
			}
		}
	}
}
