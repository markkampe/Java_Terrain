

/**
 * a MapPoint has an X and Y coordinate
 *
 */
public class MapPoint {
	public double x;		// X coordinate
	public double y;		// Y coordinate
	public int index;		// point index #
	public int neighbors[];	// neighbor's index #s
	
	private static final String format="%7.5f";
	
	public MapPoint(double x,double y) {
		this.x = x;
		this.y = y;
		neighbors = new int[3];
	}
	
	public String toString() {
		return "<" + String.format(format,x) + "," + String.format(format,y) + ">";
	}	
}
