package worldBuilder;

import java.awt.Color;

/**
 * a class to write out a map for importation into other software
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
	 * 		negative means sedimentqation
	 */
	void erodeMap(double[][] erode);

	/**
	 * @param rain	per point depth (in meters) of annual rainfall
	 */
	void rainMap(double[][] rain);

	/**
	 * @param soil - per point soil type
	 */
	void soilMap(double[][] soil);
	
	/**
	 * @param hydration - per point depth of water
	 */
	void waterMap(double[][] hydration);
	
	/**
	 * write the output
	 * 
	 * @param outputFile - name of output file
	 */
	boolean writeFile(String outputFile);
	
	/**
	 * what type of preview are we to generate?
	 */
	public enum WhichMap { HEIGHTMAP, FLORAMAP };
	void preview(WhichMap chosen, Color colorMap[]);
}
