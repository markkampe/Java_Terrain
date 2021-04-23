package worldBuilder;

/**
 * a Point of Interest is a designated location on the map,
 *   presumably something that might be of interest to exporters
 */
public class POI {
	public String type;
	public String name;
	public double x;
	public double y;
	
	/**
	 * @param type	(String) type, recognized by exporters
	 * @param name  (String) name of this Point of Interest
	 * @param x		map x coordinate
	 * @param y		map y coordinate
	 */
	public POI(String type, String name, double x, double y) {
		this.type = type;
		this.name = name;
		this.x = x;
		this.y = y;
	}
}
