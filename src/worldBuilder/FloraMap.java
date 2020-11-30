package worldBuilder;

import java.awt.Color;
import java.awt.Graphics;

/**
 * a class to render the flora as colored regions
 */
public class FloraMap {
	private Map map;		// mesh to which we correspond

	private Parameters parms;
	
	/**
	 * instantiate a river and water-body map renderer
	 * @param map	to be rendered
	 */
	public FloraMap(Map map) {
		this.map = map;
		this.parms = Parameters.getInstance();
	}
	
	/**
	 * Display the streams and rivers
	 * 
	 * @param g Graphics context
	 * @param width of the display map
	 * @param height of the display map
	 */
	public void paint(Graphics g, int width, int height) {
		Mesh mesh = map.getMesh();
	}
}
