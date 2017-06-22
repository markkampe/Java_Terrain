

/**
 * a MapPoint has an X and Y coordinate
 *
 */
public class MapPoint {
	public double x;
	public double y;
	
	private static final String format="%7.5f";
	
	public MapPoint(double x,double y) {
		this.x = x;
		this.y = y;
	}
	
	public String toString() {
		return "<" + String.format(format,x) + "," + String.format(format,y) + ">";
	}	
}
