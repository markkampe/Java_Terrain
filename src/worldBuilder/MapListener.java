package worldBuilder;

/**
 * interface for call-backs resulting from select clicks on the Map
 */
public interface MapListener {

	/**
	 * called whenever a region selection changes
	 * @param map_x0	left most point (map coordinate)
	 * @param map_y0	upper most point (map coordinate)
	 * @param width		(in map units)
	 * @param height	(in map units)
	 * @param complete	boolean, has selection completed
	 * 
	 * @return	boolean	(should selection continue)
	 */
	public boolean regionSelected(
			double map_x0, double map_y0, 
			double width, double height,
			boolean complete);
	
	/**
	 * called when a point is selected on the map
	 * @param map_x		(map coordinate)
	 * @param map_y		(map coordinate)
	 * @return
	 */
	public boolean pointSelected(double map_x, double map_y);
}
