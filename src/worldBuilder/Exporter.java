package worldBuilder;

import java.awt.Color;

/**
 * a class to write out a map for importation into other software
 */
public interface Exporter {
	/**
	 * Set the size of a single tile
	 * @param meters real-world width of a tile
	 */
	void tileSize(int meters);

	/**
	 * Set the lat/lon of the region being exported
	 * @param lat real world latitude of map center
	 * @param lon real world longitude of map center
	 */
	void position(double lat, double lon);

	/**
	 * Set seasonal temperature range for region being exported
	 * @param meanTemp	mean (all year) temperature
	 * @param meanSummer	mean (summer) temperature
	 * @param meanWinter	mean (winter) temperature
	 */
	void temps(double meanTemp, double meanSummer, double meanWinter);

	/**
	 * Up-load the altitude of every tile
	 * @param heights	height (Z value) of every point
	 */
	void heightMap(double[][] heights);

	/**
	 * Up-load the net erosion/deposition for every tile
	 * @param erode	per point height (Z value) of soil lost to erosion
	 * 		negative means sedimentation
	 */
	void erodeMap(double[][] erode);

	/**
	 * Up-load the annual rainfall for every tile
	 * @param rain	per point depth (in meters) of annual rainfall
	 */
	void rainMap(double[][] rain);

	/**
	 * Up-load the soil type for every tile
	 * @param soil - per point soil type
	 * @param names - per-type name strings
	 */
	void soilMap(double[][] soil, String[] names);
	
	/**
	 * Up-load the surface-water-depth (delta-Z) for every tile
	 * @param depth - per point depth of water
	 */
	void waterMap(double[][] depths);
	
	/**
	 * Up-load the floral ecotope for every tile
	 * @param flora - per point flora types
	 * @param names - per-type name strings
	 */
	void floraMap(double[][] flora, String[] names);
	
	/**
	 * Export the up-loaded information in selected format
	 * 
	 * @param outputFile - name of output file
	 */
	boolean writeFile(String outputFile);
	
	/**
	 * what type of preview are we to generate?
	 */
	public enum WhichMap { HEIGHTMAP, FLORAMAP };

	/**
	 * generate a preview of the currently up-loaded export
	 * @param chosen map type (e.g. height, flora)
	 * @param colorMap - palette to be used in preview
	 */
	public void preview(WhichMap chosen, Color colorMap[]);
}
