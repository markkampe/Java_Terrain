package worldBuilder;

import java.awt.Color;

/**
 * An exporter is a class that can persist a world map
 * in a form that some other software can import.
 *
 */
public interface Exporter {
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
	 * write the output
	 * 
	 * @param name out of output file
	 */
	boolean writeFile(String outputFile);
	
	/**
	 * create an output preview
	 */
	public enum WhichMap { HEIGHTMAP, FLORAMAP };
	void preview(WhichMap chosen, Color colorMap[]);
}
