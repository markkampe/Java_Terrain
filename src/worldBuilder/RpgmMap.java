package worldBuilder;

/**
 * represents identifying information in an RPGMaker map
 */
public class RpgmMap {
	
	/** file name within the project/map directory	*/
	public String name;
	/** map ID index (starts with 1)				*/
	public int id;
	/** map ID index of parent (containing) map		*/
	public int parent;
	//public int order;		// listing order in index
	//public boolean expanded;	// is this a tactical scale map
	/** RPGMaker big-map coordinates of the top left corner	*/
	public double x, y;
	
	/** 
	 * instantiate a new map (within an index)	
	 * @param name of the map file
	 */
	public RpgmMap(String name) {
		this.name = name;
	}
}
