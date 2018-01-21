package worldBuilder;

public class RpgmMap {
	
	public String name;		// file name within data directory
	public int id;			// map ID index (starts w/1)
	public int parent;		// map ID of parent
	public int order;		// listing order in index
	public boolean expanded;	// is this a tactical scale map
	public double x;		// x coordinate of top left
	public double y;		// y coordinate of top left
	
	public RpgmMap(String name, boolean expanded) {
		this.name = name;
		this.expanded = expanded;
	}
}
