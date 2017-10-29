package worldBuilder;

/**
 * An exporter is a class that can persist a world map
 * in a form that some other softwrae caqn import.
 *
 */
public interface Exporter {

	/**
	 * @param name of this map (as opposed to file)
	 */
	void name(String text);

	/**
	 * @param x_points map width
	 * @param y_points map height
	 */
	void dimensions(int x_points, int y_points);

	/**
	 * @param meters real-world width of a tile
	 */
	void tileSize(int meters);

	/**
	 * @param lat real world latitude of map center
	 * @param lon real world longitude of map center
	 */
	void position(double lat, double lon);

	/**
	 * @param meanTemp	mean (all year) temperature
	 * @param meanSummer	mean (summer) temperature
	 * @param meanWinter	mean (winter) temperature
	 */
	void temps(double meanTemp, double meanSummer, double meanWinter);

	/**
	 * @param heights	height (in meters) of every point
	 */
	void heightMap(double[][] heights);

	/**
	 * @param erode	per point height (in meters) of soil lost to erosion
	 * 		negative -> sedimentqation
	 */
	void erodeMap(double[][] erode);

	/**
	 * @param rain	per point depth (in meters) of annual rainfall
	 */
	void rainMap(double[][] rain);

	/**
	 * @param per point soil type
	 */
	void soilMap(double[][] soil);
	
	/**
	 * @param per point depth of water
	 */
	void waterMap(double[][] hydration);
	
	/**
	 * force out the file
	 */
	boolean flush();
}
