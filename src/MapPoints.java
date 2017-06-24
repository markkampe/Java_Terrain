/**
 * a MapPoints is an array of MapPoint
 */
public class MapPoints {
	
	static private double x_extent = 1;	// X map diameter
	static private double y_extent = 1;	// Y map diameter
	public MapPoint[] set;
	
	/**
	 * generate a set of n map points
	 * 
	 * @param n ... number of points
	 * @return ... an array of that many points
	 */
	public MapPoints(int n) {
		set = new MapPoint[n];
		
		for(int i = 0; i < n; i++) {
			double x = x_extent * (Math.random() - 0.5);
			double y = y_extent * (Math.random() - 0.5);
			set[i] = new MapPoint(x,y);
		}
	}
	
	public int length() {
		return set.length;
	}
	
	public MapPoint point(int n) {
		if (n < set.length)
			return set[n];
		else
			return null;
	}
	
	/**
	 * represent a set of points as a string
	 */
	public String toString() {
		String result = "";
		for(int i = 0; i < set.length; i++) {
			if (result != "")
				result += "    ";
			result += set[i].toString() + "\n";
		}
		return result;
	}
	/**
	 * @return the centroid of a set of points
	 */
	public MapPoint centroid() {
		double x_tot = 0;
		double y_tot = 0;
		for(int i = 0; i < set.length; i++) {
			x_tot += set[i].x;
			y_tot += set[i].y;
		}
		return new MapPoint(x_tot/set.length, y_tot/set.length);
	}
}
