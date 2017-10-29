package worldBuilder;

import java.io.FileWriter;
import java.io.IOException;

/**
 * exporter that creates a generic json summary of a map
 */
public class JsonExporter implements Exporter {	

	private Parameters parms;
	
	private static final String soilTypes[] = {
			"sedimentary", "metamorphic", "igneous", "alluvial"
	};
	
	private String filename;		// output file name
	private String mapname;			// name of this map
	
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
	
	/**
	 * create a new output file
	 * @param filename
	 */
	public JsonExporter(String filename) {
		this.filename = filename;
		parms = Parameters.getInstance();
	}
	
	
	public void name(String name) {
		this.mapname = name;
	}

	public void dimensions(int x_points, int y_points) {
		this.x_points = x_points;
		this.y_points = y_points;	
	}

	public void tileSize(int meters) {
		this.tile_size = meters;
		
	}

	public void position(double lat, double lon) {
		this.lat = lat;
		this.lon = lon;
	}

	public void temps(double meanTemp, double meanSummer, double meanWinter) {
		this.Tmean = meanTemp;
		this.Tsummer = meanSummer;
		this.Twinter = meanWinter;
	}

	public void heightMap(double[][] heights) {
		this.heights = heights;
	}

	public void erodeMap(double[][] erode) {
		this.erode = erode;	
	}

	public void rainMap(double[][] rain) {
		this.rain = rain;
	}

	public void soilMap(double[][] soil) {
		this.soil = soil;
		
	}

	public void waterMap(double[][] hydration) {
		this.hydration = hydration;
	}

	
	/**
	 * export a map as high resolution tiles
	 */
	public boolean flush() {
	
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
					output.write(String.format(FORMAT_S, "soil", 
							soilTypes[erode[r][c] < 0 ? Map.ALLUVIAL : st]));
					output.write(" }");
				}
			}
			output.write(NEWLINE);
			output.write("]\n");	// end of points
			output.write( "}\n");	// end of grid
			output.close();
			return true;
		} catch (IOException e) {
			return false;
		}
	}
}