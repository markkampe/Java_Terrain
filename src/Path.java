
/**
 * A Path is a connection between two adjacent vertices
 * 
 * this class also maintains a list of known paths
 */
public class Path {
	public MapPoint source;
	public MapPoint target;
	public int index;
	
	public Path(MapPoint p1, MapPoint p2, int index) {	
		source = p1;
		target = p2;
		this.index = index;
	}
}
