package worldBuilder;

import java.awt.Color;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Exporter to render a Cartesian map w/JSON descriptions of each point.
 */
public class JsonExporter implements Exporter {	

	private Parameters parms;
	
	private int x_points;			// width of map (in points)
	private int y_points;			// height of map (in points)
	private int tile_size;			// tile size (in meters)
	
	private double lat;				// latitude
	private double lon;				// longitude
	
	private double Tmean;			// mean temperature
	private double Tsummer;			// mean summer temperature
	private double Twinter;			// mean winter temperature
	
	private double[][] heights;		// per point height (meters)
	private double[][] rain;		// per point rainfall (meters)
	private double[][] erode;		// per point erosion (meters)
	private double[][] hydration;	// per point water depth (meters)
	private double[][] soil;		// per point soil type
	private double[][] flora;		// per point flora type
	
	private double maxHeight;		// highest discovered altitude
	private double minHeight;		// lowest discovered altitude
	private double maxDepth;		// deepest discovered water
	
	static final int ALLUVIAL = 4;	// FIX
	
	// brightness constants for preview colors
	private static final int DIM = 32;
	private static final int BRIGHT = 256 - DIM;
	private static final int NORMAL = 128;
	
	private static final int EXPORT_DEBUG = 2;
	
	/**
	 * create a new Raw JSON exporter
	 * 
	 * @param width of the export area (in tiles)
	 * @param height of the export area ((in tiles)
	 */
	public JsonExporter(int width, int height) {
		this.x_points = width;
		this.y_points = height;
		parms = Parameters.getInstance();
		
		if (parms.debug_level >= EXPORT_DEBUG)
			System.out.println("new JSON exporter (" + height + "x" + width + ")");
	}

	/**
	 * Set the size of a single tile
	 * @param meters real-world width of a tile
	 */
	public void tileSize(int meters) {
		this.tile_size = meters;
	}

	/**
	 * Set the lat/lon of the region being exported
	 * @param lat real world latitude of map center
	 * @param lon real world longitude of map center
	 */
	public void position(double lat, double lon) {
		this.lat = lat;
		this.lon = lon;
	}

	/**
	 * Set seasonal temperature range for region being exported
	 * @param meanTemp	mean (all year) temperature
	 * @param meanSummer	mean (summer) temperature
	 * @param meanWinter	mean (winter) temperature
	 */
	public void temps(double meanTemp, double meanSummer, double meanWinter) {
		this.Tmean = meanTemp;
		this.Tsummer = meanSummer;
		this.Twinter = meanWinter;
	}

	/**
	 * Up-load the altitude of every tile
	 * @param heights	height (in meters) of every point
	 */
	public void heightMap(double[][] heights) {
		this.heights = heights;
		
		// note the max and min heights
		maxHeight = 0;
		minHeight = 666;
		for(int i = 0; i < heights.length; i++)
			for(int j = 0; j < heights[0].length; j++) {
				if (heights[i][j] > maxHeight)
					maxHeight = heights[i][j];
				if (heights[i][j] < minHeight)
					minHeight = heights[i][j];
			}
	}

	/**
	 * Up-load the net erosion/deposition for every tile
	 * @param erode	per point height (in meters) of soil lost to erosion
	 * 		negative means sedimentqation
	 */
	public void erodeMap(double[][] erode) {
		this.erode = erode;	
	}

	/**
	 * Up-load the annual rainfall for every tile
	 * @param rain	per point depth (in meters) of annual rainfall
	 */
	public void rainMap(double[][] rain) {
		this.rain = rain;
	}

	/**
	 * Up-load the soil type for every tile
	 * @param soil - per point soil type
	 */
	public void soilMap(double[][] soil) {
		this.soil = soil;
	}

	/**
	 * Up-load the flora type for every tile
	 * @param flora - per point flora type
	 */
	public void floraMap(double[][] flora) {
		this.flora = flora;
	}

	/**
	 * Up-load the surface-water-depth for every tile
	 * @param hydration - per point depth of water
	 */
	public void waterMap(double[][] hydration) {
		this.hydration = hydration;
		
		// note the max and min heights
		maxDepth = 0;
		for (int i = 0; i < hydration.length; i++)
			for (int j = 0; j < hydration[0].length; j++) {
				if (hydration[i][j] < maxDepth)
					maxDepth = hydration[i][j];
			}
	}

	/**
	 * Export the up-loaded information in selected format
	 * 
	 * @param filename - name of output file
	 */
	public boolean writeFile( String filename ) {
		// strip off suffix and leading directories to get base name
		int dot = filename.lastIndexOf('.');
		String mapname = (dot == -1) ? filename : filename.substring(0, dot);
		int slash = mapname.lastIndexOf('/');
		if (slash == -1)
			slash = mapname.lastIndexOf('\\');
		if (slash != -1)
			mapname = mapname.substring(slash + 1);
			
		// generate the output
		try {
			FileWriter output = new FileWriter(filename);
			final String FORMAT_S = " \"%s\": \"%s\"";
			final String FORMAT_D = " \"%s\": %d";
			final String FORMAT_DM = " \"%s\": \"%dm\"";
			final String FORMAT_DP = " \"%s\": %.2f";
			final String FORMAT_FM = " \"%s\": \"%.2fm\"";
			final String FORMAT_CM = " \"%s\": \"%.0fcm\"";
			final String FORMAT_L = " \"%s\": %.6f";
			final String FORMAT_O = " \"%s\": {";
			final String FORMAT_A = " \"%s\": [";
			final String FORMAT_T = " \"%s\": \"%.1fC\"";
			final String NEW_POINT = "\n        { ";
			final String NEWLINE = "\n    ";
			final String COMMA = ", ";
			
			// write out the grid wrapper
			output.write("{");
			output.write(NEWLINE);
			output.write(String.format(FORMAT_S,  "name", mapname));
			output.write(",");
			output.write(NEWLINE);
			output.write(String.format(FORMAT_O, "dimensions"));
				output.write(String.format(FORMAT_D, "height", y_points));
				output.write(COMMA);
				output.write(String.format(FORMAT_D, "width", x_points));
				output.write(" },");
				output.write(NEWLINE);
			output.write(String.format(FORMAT_DM, "tilesize", tile_size));
			output.write(",");
			output.write(NEWLINE);
			output.write(String.format(FORMAT_O, "center"));
				output.write(String.format(FORMAT_L, "latitude", lat));
				output.write(COMMA);
				output.write(String.format(FORMAT_L, "longitude", lon));
				output.write(" },");
			output.write(NEWLINE);
			output.write(String.format(FORMAT_O, "temperatures"));
				output.write(String.format(FORMAT_T, "mean", Tmean));
				output.write(COMMA);
				output.write(String.format(FORMAT_T, "summer", Tsummer));
				output.write(COMMA);
				output.write(String.format(FORMAT_T, "winter", Twinter));
				output.write(" },");
			output.write(NEWLINE);
			
			output.write(String.format(FORMAT_A, "points"));
			
			boolean first = true;
			for(int r = 0; r < y_points; r++) {
				for(int c = 0; c < x_points; c++) {
					if (first)
						first = false;
					else
						output.write(",");
					output.write(NEW_POINT);
					double z = heights[r][c]-erode[r][c];
					double hydro = hydration[r][c];
					output.write(String.format(FORMAT_FM, "altitude", parms.altitude(z)));
					output.write(COMMA);
					output.write(String.format(FORMAT_CM, "rainfall", rain[r][c]));
					output.write(COMMA);
					output.write(String.format(FORMAT_DP, "hydration", hydro));
					output.write(COMMA);
					
					int st = (int) Math.round(soil[r][c]);
					// FIX - should be string, but don't have map
					output.write(String.format(FORMAT_S, "soil", 
							soil[erode[r][c] < 0 ? ALLUVIAL : st]));
					output.write(" }");
				}
			}
			output.write(NEWLINE);
			output.write("]\n");	// end of points
			output.write( "}\n");	// end of grid
			output.close();
			
			if (parms.debug_level > 0) {
				System.out.println("Exported(Raw Json) "  + x_points + "x" + y_points + " " + tile_size
						+ "M tiles from <" + String.format("%9.6f", lat) + "," + String.format("%9.6f", lon)
						+ "> to file " + filename);
			}
			return true;
		} catch (IOException e) {
			System.err.println("Unable to export map to file " + filename);
			return false;
		}
	}
	
	/**
	 * generate a preview of the currently up-loaded export
	 * @param chosen map type (e.g. height, flora)
	 * @param colorMap - palette to be used in preview
	 */
	public void preview(WhichMap chosen, Color colorMap[]) {
	
		// figure out the altitude to color mapping
		double aMean = (maxHeight + minHeight)/2;
		double aScale = BRIGHT - DIM;
		if (maxHeight > minHeight)
			aScale /= maxHeight - minHeight;
		
		// fill in preview from the per-point attributes
		Color map[][] = new Color[y_points][x_points];
		for(int i = 0; i < y_points; i++)
			for(int j = 0; j < x_points; j++)
				if (hydration[i][j] >= 0) {	// land
					double h = NORMAL + ((heights[i][j] - aMean) * aScale);
					if (chosen == WhichMap.FLORAMAP && flora[i][j] > 0)
						map[i][j] = colorMap[(int) flora[i][j]];
					else	// altitudes
						map[i][j] = new Color((int)h, (int)h, (int)h);
				} else	{							// water
					double depth = hydration[i][j]/maxDepth;
					double h = (1 - depth) * (BRIGHT - DIM);
					map[i][j] = new Color(0, (int) h, BRIGHT);
				}
		
		new PreviewMap("Export Preview (" +
						(chosen == WhichMap.FLORAMAP ? "flora" : "terrain") +
						")", map, 0);
	}
}
