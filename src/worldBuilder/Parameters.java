package worldBuilder;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;

import javax.json.Json;
import javax.json.stream.JsonParser;

/**
 * This is a singleton class containing global program parameters.
 * 
 * values may be default or overridden from command line or dialogs
 */
 
 public class Parameters {
	 
	// operating units ... hard-wired into the code
	public static final String unit_xy = "km";
	public static final String unit_z = "m";
	public static final String unit_r = "cm/y";
	public static final String unit_s = "cm/km";
	public static final String unit_f = "m3/s";
	
	// map coordinate ranges (probably don't want to change)
	public static final double x_extent = 1.0;	// Xmax - Xmin
	public static final double y_extent = 1.0;	// Ymax - Ymin
	public static final double z_extent = 1.0;	// Zmax - Zmin
	
	// limits on world parameters (overide in config.json)
	public int radius = 6371;			// planetary radius (km)
	public int alt_max = 10000;			// max altitude (m)
	public int alt_maxrain = 1000;		// max rain altitude (m)
	public int diameter_max = 5000;		// max world diameter (km)
	public int msl_range = 1000;		// +/- (m)
	public int rain_max = 1000;			// rain (cm/y)
	public int slope_max = 5;			// slope m/km
	public int mountain_divisor = 2;	// world/mountain width
	
	// world size slider units
	public int diameter_scale = 100;	// slider labeling unit (km)
	public int diameter_grain = 500;	// diameter rounding unit (km)
	public int alt_scale = 1000;		// slider labeling unit (km)

	// default display parameters (override in config.json)
	private static final int PIXELS = 800;	// display height/width
	public int height = PIXELS;			// screen height
	public int width = PIXELS;			// screen width
	public int border;					// screen border width
	public int dialogDX;				// X offset for dialog boxes
	public int dialogDY;				// Y offset for dialog boxes
	public int dialogDelta;				// per dialog offsets
	public int dialogBorder;			// dialog box border
	
	public String title = "WorldBuilder";
	
	// map rendering thresholds
	public double stream_flux = 0.1;	// stream threshold (m3/s)
	public int topo_major = 10;			// max 10 major lines
	public int topo_minor = 5;			// minor lines per major

	// user selected world size/location parameters
	public int xy_range;		// X/Y range (km)
	public int z_range;			// Z range (m)
	public int mountain_max;	// widest mountain (km)
	public double latitude;		// central point latitude
	public double longitude;	// central point longitude
	
	// persistent defaults
	public double sea_level = 0;// sea level (map space)
	public int dSlope;			// continental slope
	public int dDiameter;		// mountain diameter
	public int dAltitude;		// mountain altitude
	public int dShape;			// mountain shape
		public static final int CONICAL = 0;
		public static final int SPHERICAL = 4;
		public static final int CYLINDRICAL = 8;
	public int dDirection;		// incoming weather
	public int dAmount;			// annual rainfall
	public int dRainHeight;		// mean height of incoming rain
	
	// physical processes
	public double Ve = 1.0;		// minimum flow for erosion
	public double Ce = 1.0;		// coefficient of erosion
	public double Vd = 0.1;		// maximum flow for deposition
	public double Cd = .001;	// coefficient of deposition
	
	// map generation parameters
	public int improvements = 1;	// number of smoothing iterations
	public int points = 4096;		// desired number of grid points
	
	// diagnostic options
	public int debug_level;			// level of verbosity
	
	// default display
	public int display_options;
	
	private static Parameters singleton = null;
	
	// private constructor for singleton use
	private Parameters() {
			setDefaults();
	}
	
	// default values are a function of configured limits
	private void setDefaults() {
		// default world size
		xy_range = diameter_max/5;
		z_range = alt_max;
		
		// default inclination
		dSlope = 1;
		
		// default mountain size/shape
		dAltitude = z_range/4;
		dDiameter = xy_range/(mountain_divisor * 4);
		dShape = (CONICAL + SPHERICAL)/2;
		
		// default weather
		dDirection = 0;				// weather from the north
		dAmount = rain_max/10;		// moderate rainfall
		dRainHeight = alt_maxrain/4;// not particularly high	
	}
	
	// public constructor to read from configuration file
	public Parameters(String filename, int debug) {
		debug_level = debug;
		singleton = this;
		JsonParser parser;
		try {
			parser = Json.createParser(new BufferedReader(new FileReader(filename)));
		} catch (FileNotFoundException e) {
			System.err.println("FATAL: unable to open configuration file file " + filename);
			return;
		}

		String thisKey = "";
		while (parser.hasNext()) {
			JsonParser.Event e = parser.next();
			switch (e) {
			case KEY_NAME:
				thisKey = parser.getString();
				break;

			case VALUE_TRUE:
				switch (thisKey) {
				case "points":
					display_options |= Map.SHOW_POINTS;
					break;
				case "mesh":
					display_options |= Map.SHOW_MESH;
					break;
				case "topo":
					display_options |= Map.SHOW_TOPO;
					break;
				case "rain":
					display_options |= Map.SHOW_RAIN;
					break;
				case "water":
					display_options |= Map.SHOW_WATER;
					break;
				case "soil":
					display_options |= Map.SHOW_SOIL;
					break;
				}
				break;
				
			case VALUE_STRING:
				switch (thisKey) {
				case "title":
					title = parser.getString();
					break;
				}
				break;

			case VALUE_NUMBER:
				switch (thisKey) {
				// default window parameters
				case "height":
					height = new Integer(parser.getString());
					break;
				case "width":
					width = new Integer(parser.getString());
					break;
				case "border":
					border = new Integer(parser.getString());
					break;
				case "dialog_x":
					dialogDX = new Integer(parser.getString());
					break;
				case "dialog_y":
					dialogDY = new Integer(parser.getString());
					break;
				case "dialog_delta":
					dialogDelta = new Integer(parser.getString());
					break;
				case "dialog_border":
					dialogBorder = new Integer(parser.getString());
					break;
					
				// default planetary parameters
				case "latitude":
					latitude = new Double(parser.getString());
					break;
				case "longitude":
					longitude = new Double(parser.getString());
					break;
				case "radius":
					radius = new Integer(parser.getString());
					break;

				// limits on configuration sliders
				case "diameter_unit":
					diameter_scale = new Integer(parser.getString());
					break;
				case "diameter_max":
					diameter_max = new Integer(parser.getString());
					break;
				case "diameter_grain":
					diameter_grain = new Integer(parser.getString());
					break;
				case "mountain_fraction":
					mountain_divisor = new Integer(parser.getString());
					break;
				case "altitude_unit":
					alt_scale = new Integer(parser.getString());
					break;
				case "altitude_max":
					alt_max = new Integer(parser.getString());
					break;
				case "msl_range":
					msl_range = new Integer(parser.getString());
					break;
				case "rain_max":
					rain_max = new Integer(parser.getString());
					break;
				case "altitude_rain":
					alt_maxrain = new Integer(parser.getString());
					break;
					
				// physical process parameters
				case "Ve":
					Ve = new Double(parser.getString());
					break;
				case "Ce":
					Ce = new Double(parser.getString());
					break;
				case "Vd":
					Vd = new Double(parser.getString());
					break;
				case "Cd":
					Cd = new Double(parser.getString());
					break;
					
				// map rendering parameters
				case "topo_major":
					topo_major = new Integer(parser.getString());
					break;
				case "toppo_minor":
					topo_minor = new Integer(parser.getString());
					break;
				case "stream":
					stream_flux = new Double(parser.getString());
					break;
					
				// mesh creation parameters
				case "points":
					points = new Integer(parser.getString());
					break;
				case "improvements":
					improvements = new Integer(parser.getString());
					break;
					
				default:
					break;
				}
			default:
				break;
			}
		}
		parser.close();
		setDefaults();
		
		if (debug_level > 0) {
			System.out.println("Configuration Parameters");
			System.out.println("   window:     " + width + "x" + height + ", border=" + border);
			System.out.println("   dialogs:    x+" + dialogDX + ", y+" + dialogDY + " + " + dialogBorder + "/, border=" + dialogBorder);
			System.out.println("   topo maps:  " + topo_major + " major lines, " + topo_minor*topo_major + " minor");
			System.out.println("   stream:     >= river " + stream_flux + " " + unit_f);
			System.out.println("   max ranges: " + diameter_max + unit_xy + 
					", altitude +/-" + alt_max + unit_z + 
					", msl +/-" + msl_range + unit_z);
			System.out.println("               mountain diameter=world/" + mountain_divisor +
					", max rain=" + rain_max + unit_r + " (bottoms at " + alt_maxrain + unit_z + ")");
			System.out.println("   debug=" + debug_level);
			worldParms();
		}
	}
	
	public static Parameters getInstance() {
		if (singleton == null)
			singleton = new Parameters();
		return singleton;
	}
	
	public void worldParms() {
		System.out.println("World Configuration");
		System.out.println("   maped area: " + xy_range + "x" + xy_range + " " + unit_xy + "2, altitude " + z_range/2 + unit_z);
		System.out.println("   planetary:  lat=" + latitude + ", lon=" + longitude + ", radius=" + radius + unit_xy);
	}
	
	/**
	 * attractive slider calibration
	 * 
	 * @param min value
	 * @param max_value
	 * @param major ...	major tics (vs minor)
	 */
	public static int niceTics(int min, int max, boolean major) {
		int full_scale = max - min;
		if (major) {
			if (min == -max)
				return full_scale/4;
			else
				return full_scale/5;
		} else {
			if ((min == -max) && (full_scale % 8) == 0)
				return full_scale/8;
			else
				return full_scale/10;
		}
	}
	
	/**
	 * turn a map coordinate into a latitude
	 * 
	 * @param y (map coordinate)
	 */
	public double latitude(double y) {
		double degrees = 180.0 * xy_range / (Math.PI * radius);
		double offset = y * degrees / y_extent;
		return latitude + offset;
	}
	
	/**
	 * turn a map coordinate into a longitude
	 * 
	 * @param x (map coordinate)
	 */
	public double longitude(double x) {
		// radius must be corrected for latitude
		double lat = latitude * Math.PI / 180;
		double r = Math.cos(lat) * radius;
		
		double degrees = 360.0 * xy_range / (2 * Math.PI * r);
		double offset = x * degrees / x_extent;
		return longitude + offset;
	}
	/**
	 * turn a map distance into world km
	 * 
	 * @param d	(distance in map coordiantes)
	 * @return km
	 */
	public double km(double d) {
		return d * xy_range / x_extent;
	}
	
	/**
	 * turn a map coordinate into a world altitude
	 * 
	 * @param z (map coordinate)
	 * @return	meters (above sea level)
	 */
	public double altitude(double z) {
		return (z - sea_level) * z_extent * z_range;
	}
}
