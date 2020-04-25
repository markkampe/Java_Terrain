package worldBuilder;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.LinkedList;
import java.util.ListIterator;

import javax.json.Json;
import javax.json.stream.JsonParser;

/**
 * a Singleton class containing global program parameters.
 * 
 * values may be default or overridden from command line or dialogs
 */

public class Parameters {
	// version identification info
	private static final String PROGRAM_NAME = "WorldBuilder";
	private static final String PROGRAM_VERSION = "0.1";
	private static final String VERSION_NAME = "(WIP)";
	public String title = PROGRAM_NAME + " " + PROGRAM_VERSION + " " + VERSION_NAME;

	// default configuration information
	private static final String DEFAULT_CONFIG = "/Templates/worldBuilder.json";
	public String map_name = "Map001";
	public String parent_name = null;
	public String project_dir = null;
	public LinkedList<String> exportRules;
	public String config_directory = "";

	/** operating units ... hard-wired into the code (assumes UTF8)	*/
	/** distance unit	*/		public static final String unit_xy = "km";
	/** land area unit	*/		public static final String unit_xy2 = "km\u00B2";
	/** altitude unit	*/		public static final String unit_z = "m";
	/** rainfall unit	*/		public static final String unit_r = "cm/y";
	/** slope unit		*/		public static final String unit_s = "cm/km";
	/** flow rate unit	*/		public static final String unit_f = "m\u00B3/s";
	/** velocity unit	*/		public static final String unit_v = "m/s";
	/** volume unit		*/		public static final String unit_V = "m\u00B3";
	/** temperature unit*/		public static final String unit_t = "\u00B0C";
	/** direction unit	*/		public static final String unit_d = "\u00B0";
	/** screen res unit	*/		public static final String unit_p = "px";

	/** map coordinate ranges ... no reason to change	*/
	/** map: Xmax - Xmin	*/	public static final double x_extent = 1.0;
	/** map: Ymax - yMin	*/	public static final double y_extent = 1.0;
	/** map: Zmax - ZMin	*/	public static final double z_extent = 1.0;
	
	/** meteorological constants ... not configurable	*/
	/** temp lapse rate		*/	public double lapse_rate = 0.01;	// degC/M
	/** avg polar temp		*/	public  double Tmin = -5;			// degC
	/** avg equitorial temp	*/	public double Tmax = 30;			// degC

	/** planetary characteristics (override in config.json)	*/
	/** planetary radius	*/	public int radius = 6371;			// km
	/** planetary tilt		*/	public double tilt = 23.5;			// degrees
	
	/** limits on world parameters (overide in config.json)	*/
	/** max world altitude	*/	public int alt_max = 10000;			// m
	/** max world diameter	*/	public int diameter_max = 5000;		// km
	/** initial slope		*/	public double slope_init = 0.00001; // dz/dx

	/** range limits on sliders	*/
	/** MSL slider range		*/	public int msl_range = 1000;	// +/- m
	/** rain slider	range		*/	public int rain_max = 500;		// cm/y
	/** mountn wid slider range	*/	public int m_width_divisor = 2; // world/mountain width
	/** max tiles warn limit	*/	public int tiles_max = 10000;
	/** max river flow			*/	public int tribute_max = 1000;
	/** min	height/alt levels	*/	public int levels_min = 5;
	/** max height/alt levels	*/	public int levels_max = 20;
	/** exp temp slider range	*/	public int delta_t_max = 15;	// degC
	/** exp hydro slider range	*/	public int delta_h_max = 100;	// %
	/** landform alt slider range*/	public int delta_z_max = 1000;	// m

	/** world size slider units	*/
	/** diam slider unit	*/	public int diam_scale = 100;		// km
	/** diam slider ticks	*/	public int diam_grain = 500;		// km
	/** alt slider unit		*/	public int alt_scale = 1000;		// m

	/** default display parameters (override in config.json)	*/
	private static final int PIXELS = 800; // display height/width
	/** sceern height		*/	public int height = PIXELS;
	/** screen width		*/	public int width = PIXELS;
	/** border width		*/	public int border;
	/** dialog border		*/	public int dialogBorder;
	/** descript width		*/	public int descr_width;
	/** descript height		*/	public int descr_height;
	
	/** map rendering thresholds	*/
	/** min stream flux		*/	public double stream_flux = 0.1;	// m^3/s
	/** min river flux		*/	public double river_flux = 1.0;		// m^3/s
	/** min artery flux		*/	public double artery_flux = 10.0; 	// m^3/s
	/** min deep water		*/	public double deep_threshold = 10;	// m
	/** topo: minor/major	*/	public int topo_major = 5;
	/** topo: meters/minor	*/	public int topo_minor = 100;		// m

	/** user selected world size/location parameters	*/
	/** max x/y width		*/	public int xy_range;				// km
	/** max map altitude	*/	public int z_range;					// m
	/** max mountain width	*/	public int mountain_max;			// km
	/** center latitude		*/	public double latitude;				// degrees
	/** center longitude	*/	public double longitude;			// degrees
	/** world description	*/	public String description;

	/** persistent defaults for Edit dialogs	*/
	/** dflt sea level		*/	public double sea_level = 0;
	/** dflt x slope		*/	public int dSlope;
	/** dflt slope axis		*/	public int dDirection;
	/** dflt mountain diam	*/	public int dDiameter;
	/** dflt mountain alt	*/	public int dAltitude;
	/** dflt mountain shape	*/	public int dShape;
	/** triangular ridge	*/	public static final int CONICAL = 0;
	/** rounded hill		*/	public static final int SPHERICAL = 4;
	/** plauteau shape		*/	public static final int CYLINDRICAL = 8;

	/** dflt rainfall		*/	public int dAmount;				// cm/y
	/** dflt atrerial flux	*/	public int dTribute;			// m^3/s
	/** dflt expt tile size	*/	public int dTileSize;			// m
	
	/** RPGMaker export parameters	*/
	/** marsh threshold		*/	public int dWaterMin;		// m
	/** deep water threshold*/	public int dWaterMax;		// m
	/** over hill threshold	*/	public int dHillMin;		// m
	/** over mntn threshold	*/	public int dHillMax;		// m
	/** over hill threshold	*/	public int dSlopeMin;		// dz/dx
	/** over mntn threshold	*/	public int dSlopeMax;		// dz/dx
	/** outs pit threshold	*/	public int dGroundMin;		// m
	/** outs hill threshold	*/	public int dGroundMax;		// m
	/** outs alt levels		*/	public int dAltLevels;
	/** dflt plant cover %	*/	public int dFloraPct;
	/** dflt tall grass	%	*/	public int dFloraMin;
	/** dflt brush %		*/	public int dFloraMax;
	/** dflt temp offset	*/	public int dDeltaT;	
	/** dflt hydro offset	*/	public int dDeltaH;
	/** dflt hydro multiply	*/	public int dTimesH;
	/** over palette file	*/	public String OW_palette;
	/** outs palette file	*/	public String Out_palette;

	/** tunable physical process parameters	*/
	/** min water velocity	*/	public double Vmin = 0.002; // m/s
	/** deposition velocity	*/	public double Vd = 0.01; 	// (Hjulstrom) maximum velocity (M/s) for silt deposition
	/** erosion velocity	*/	public double Ve = 0.40; 	// (Hjulstrom) erosion/deposition threshold
	/** max water velocity	*/	public double Vmax = 3.0;	// m/s
	/** max suspended soil	*/	public double Smax = 0.10;	// liters of soil peer liter of water
	/** erosion rate		*/	public double Ce = 0.100;	// how much of possible erosion allowed per point
	/** deposition rate		*/	public double Cd = 0.100;	// how much of possible deposition allowed per point
	/** silting rate		*/	public double Cs = 0.003;	// possible silt deposition (m per m3/s of sub-Vd flow)
	/** spring vs mean flux	*/	public double flood_mult = 2.0;	// how Spring vs average flow
	
	/** rain penetration	*/	public double Dp = 0.5; 	// m
	/** evaporation const	*/	public double Edeg = 10; 	// degC to halve evaporation rate
	/** transpiration const	*/	public double E35C = 100; 	// transpiration half-time at 35C (days)
	/** layer thickness		*/	public double sediment = 100; // m

	/** map generation parameters	*/
	/** smoothing iterations*/	public int improvements = 1;
	/** points per mesh		*/	public int points = 4096;

	/** diagnostic options	*/
	/** verbose/debug level	*/	public int debug_level; 	// level of verbosity
	/** which rule to trace	*/	public String rule_debug;	// export rule to trace

	/** default map options	*/	public int display_options;

	private static Parameters singleton = null;

	// private constructor for singleton use
	private Parameters() {
		setDefaults();
	}

	// default values are a function of configured limits
	private void setDefaults() {	
		// default world size
		xy_range = diameter_max / 5;
		z_range = alt_max;

		// default inclination
		dSlope = 1;
		dDirection = 0; 	// north/south

		// default mountain size/shape
		dAltitude = z_range / 4;
		dDiameter = xy_range / (m_width_divisor * 4);
		dShape = (CONICAL + SPHERICAL) / 2;

		// default weather
		
		dAmount = rain_max / 10; // moderate rainfall

		// default watershed
		dTribute = tribute_max / 10; // pretty studley
		
		// default export tile size
		dTileSize = 1000;			// 1km
		
		// default altitude/slope/depth thresholds
		dWaterMin = 1;
		dWaterMax = 10;
		dHillMin = 10;
		dHillMax = 30;
		dSlopeMin = 10;
		dSlopeMax = 30;
		dGroundMin = 10;
		dGroundMax = 20;
		dAltLevels = 6;
		// default plant distribution thresholds
		dFloraPct = 50;
		dFloraMin = 40;
		dFloraMax = 70;
		// default temp and hydration offsets
		dDeltaT = 0;
		dDeltaH = 0;
		dTimesH = 100;
		
		description = "";
		descr_height = 4;
		descr_width = 80;
	}
	
	/**
	 * ensure defaults are consistent w/(updated) world size
	 */
	public void checkDefaults() {
		// default mountain width less than half the world size
		if (dDiameter > (xy_range / m_width_divisor))
			dDiameter = xy_range / m_width_divisor;
		
		// default mountain height within the world height
		if (dAltitude > z_range/2)
			dAltitude = z_range/4;
		else if (dAltitude < -z_range/2)
			dAltitude = -z_range/4;
		
		// world at least ten tiles across
		if (dTileSize > (xy_range * 1000 / 10))
			dTileSize = xy_range * 1000 / 10;
	}

	/**
	 * read parameter values from a configuration file
	 * @param filename of configuration file to be read
	 * @param debug level of output to be produced
	 */
	public Parameters(String filename, int debug) {
		debug_level = debug;
		singleton = this;
		exportRules = new LinkedList<String>();
		
		BufferedReader r;
		JsonParser parser;
		if (filename == null) {
			filename = DEFAULT_CONFIG;
			InputStream s = getClass().getResourceAsStream(filename);
			r = new BufferedReader(new InputStreamReader(s));
		} else {
			try {
				r = new BufferedReader(new FileReader(filename));
			} catch (FileNotFoundException e) {
				System.out.println("ERROR: unable to open configuration file " + filename);
				return;
			}
		}
		parser = Json.createParser(r);

		String thisKey = "";
		boolean inRules = false;
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
				case "hydro":
					display_options |= Map.SHOW_HYDRO;
					break;
				}
				break;

			case VALUE_STRING:
				switch (thisKey) {
				case "title":
					title = parser.getString();
					break;
				case "map_name":
					map_name = parser.getString();
					break;
				}
				if (inRules) {
					exportRules.add(parser.getString());
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
				case "tilt":
					tilt = new Double(parser.getString());
					break;

				// limits on configuration sliders
				case "diameter_unit":
					diam_scale = new Integer(parser.getString());
					break;
				case "diameter_max":
					diameter_max = new Integer(parser.getString());
					break;
				case "diameter_grain":
					diam_grain = new Integer(parser.getString());
					break;
				case "mountain_fraction":
					m_width_divisor = new Integer(parser.getString());
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
				case "tribute_max":
					tribute_max = new Integer(parser.getString());
					break;
				case "tiles_max":
					tiles_max = new Integer(parser.getString());
					break;
				case "slope_min":
					slope_init = new Double(parser.getString());
					break;

				// physical process parameters
				case "Ve": // critical velocity for erosion
					Ve = new Double(parser.getString());
					break;
				case "Vd": // critical velocity for deposition
					Vd = new Double(parser.getString());
					break;
				case "Vmin": // lowest water velocity
					Vmin = new Double(parser.getString());
				case "Vmax": // highest water velocity
					Vmax = new Double(parser.getString());
				case "Dp": // rain penetration depth (m)
					Dp = new Double(parser.getString());
					break;
				case "Ce": // per-point erosion coefficient
					Ce = new Double(parser.getString());
					break;
				case "Cd": // per-point deposition coefficient
					Cd = new Double(parser.getString());
					break;
				case "Cs": // per-point silting coefficient
					Cs = new Double(parser.getString());
					break;
				case "Edeg": // evaporation half-time doubling temp
					Edeg = new Double(parser.getString());
					break;
				case "E35C": // evaporation half-time for 35C
					E35C = new Double(parser.getString());
					break;
				case "sediment": // sedimentary layer thickness
					sediment = new Double(parser.getString());
					break;

				// map rendering parameters
				case "topo_major":
					topo_major = new Integer(parser.getString());
					break;
				case "topo_minor":
					topo_minor = new Integer(parser.getString());
					break;
				case "stream":
					stream_flux = new Double(parser.getString());
					break;
				case "river":
					river_flux = new Double(parser.getString());
					break;
				case "artery":
					artery_flux = new Double(parser.getString());
					break;
				case "deep":
					deep_threshold = new Integer(parser.getString());
					break;
					
				// export parameters
					
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
				
			case START_ARRAY:
				if (thisKey.equals("rules"))
					inRules = true;
				break;
				
			case END_ARRAY:
				inRules = false;
				break;
				
			default:
				break;
			}
		}
		parser.close();

		setDefaults();

		if (debug_level > 0) {
			System.out.println("Configuration Parameters (" + filename + ")");
			System.out.println("   window:     " + width + "x" + height + unit_p + ", border=" + border + unit_p);
			System.out.println("   waterways:  stream=" + stream_flux + unit_f + ", river=" + river_flux + unit_f
					+ ", artery=" + artery_flux + unit_f + ", deep=" + deep_threshold + unit_z);
			System.out.println("   erosion:  "
					+ "  Ve=" + String.format("%.2f", Ve) + unit_v
					+ ", Vmax=" + String.format("%.1f", Vmax) + unit_v
					+ ", Ce=" + String.format("%.2f",  Ce)
					+ ", max susp=" + String.format("%4.1f%%", 100 * Smax));
			System.out.println("   sediment: "
					+ "  Vmin=" + String.format("%.3f", Vmin) + unit_v
					+ ", Vd=" + String.format("%.2f", Vd) + unit_v
					+ ", Cd=" + String.format("%.2f",  Cd));
			System.out.println("   rainfall:   " + "default=" + dAmount + unit_r
					+ ", Dp=" + String.format("%.1f",  Dp) + unit_z);
			System.out.println("   mean temps: polar=" + Tmin + unit_t + ", equator=" + Tmax + unit_t);
			System.out.println("   max ranges: " + diameter_max + unit_xy + ", altitude +/-" + alt_max + unit_z
					+ ", msl +/-" + msl_range + unit_z);
			System.out.println("               mountain diameter=world/" + m_width_divisor);
			System.out.println("               initial slope=" + String.format("%.6f", slope_init) + unit_s);
			System.out.println("               sedimentary layer=" + String.format("%.0f%s", sediment, unit_z));
			System.out.println("               watershed=" + tribute_max + " " + unit_f);
			System.out.println("   export:     name=" + map_name);
			for (ListIterator<String> it = exportRules.listIterator(); it.hasNext();) {
				System.out.println("               rule file: " + it.next());
			}
			System.out.println("   warnings:   tiles=" + tiles_max);
			System.out.println("   verbosity:  " + debug_level);
		}
	}

	/**
	 * @return Singleton Parameter instance
	 */
	public static Parameters getInstance() {
		if (singleton == null)
			singleton = new Parameters();
		return singleton;
	}

	/**
	 * print out the world configuration (after changes and reloads)
	 */
	public void worldParms() {
		System.out.println("World Configuration");
		System.out.println("   mapped area: " + xy_range + "x" + xy_range + unit_xy2 + ", max altitude "
				+ z_range / 2 + unit_z);
		System.out.println("   planetary:   lat=" + latitude + unit_d + ", lon=" + longitude + unit_d 
				+ ", radius=" + radius + unit_xy + ", tilt=" + tilt + unit_d);
		System.out.println("                Tmean=" + String.format("%.1f", meanTemp()) + unit_t + ", Tsummer="
				+ String.format("%.1f", meanSummer()) + unit_t + ", Twinter=" + String.format("%.1f", meanWinter())
				+ unit_t);
		System.out.println("   topo maps:   " + topo_minor + unit_z + "/line, " + topo_major + " minors/major");
		if (description != "")
			System.out.println("   description: " + getDescription() + "\n");
	}

	/**
	 * attractive slider calibration
	 * 
	 * @param min slider value
	 * @param max slider value
	 * @param major tic interval
	 * @return recommended minor tic interval
	 */
	public static int niceTics(int min, int max, boolean major) {
		int full_scale = max - min;
		if (major) {
			if (full_scale < 10)
				return full_scale;
			else if (min == -max)
				return full_scale / 4;
			else
				return full_scale / 5;
		} else {
			if ((min == -max) && (full_scale % 8) == 0)
				return full_scale / 8;
			else
				return full_scale / 10;
		}
	}

	/**
	 * mean temperature (for this latitude)
	 * 
	 * @return mean temperature (degC)
	 */
	public double meanTemp() {
		double radians = latitude * Math.PI / 180;
		return Tmin + Tmax * Math.cos(radians);
	}

	/**
	 * mean Winter temperature (for this latitude)
	 * 
	 * @return mean temperature (degC)
	 */
	public double meanWinter() {
		double lat = latitude > 0 ? latitude + tilt : latitude - tilt;
		double radians = lat * Math.PI / 180;
		return Tmin + Tmax * Math.cos(radians);

	}

	/**
	 * mean Summer temperature (for this latitude)
	 * 
	 * @return mean temperature (degC)
	 */
	public double meanSummer() {
		double lat = latitude > 0 ? latitude - tilt : latitude + tilt;
		double radians = lat * Math.PI / 180;
		return Tmin + Tmax * Math.cos(radians);
	}

	/**
	 * translate map coordinate into a latitude
	 * 
	 * @param y	map coordinate (-0.5 to 0.5)
	 * return latitude (in degrees)
	 */
	public double latitude(double y) {
		double degrees = 180.0 * xy_range / (Math.PI * radius);
		double offset = y * degrees / y_extent;
		return latitude - offset;
	}

	/**
	 * translate map coordinate into a longitude
	 * 
	 * @param x	map coordinate (-0.5 to 0.5)
	 * return longitude (in degrees)
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
	 * @param d (distance in map coordiantes)
	 * @return distance in kilometers
	 */
	public double km(double d) {
		return d * xy_range / x_extent;
	}

	/**
	 * turn a world distance into a map x/y value
	 * 
	 * @param km - distance
	 *
	 * @return map delta-x (0.0-1.0)
	 */
	public double x(double km) {
		return x_extent * km / xy_range;
	}

	/**
	 * turn a map z value into a world height
	 * 
	 * @param z (height in map coordinates)
	 * @return meters
	 */
	public double height(double z) {
		return z * z_extent * z_range;
	}

	/**
	 * turn a map z coordinate into a world altitude
	 * 
	 * @param z (height in map coordinates)
	 * @return meters (above sea level)
	 */
	public double altitude(double z) {
		return (z - sea_level) * z_extent * z_range;
	}

	/**
	 * turn a world height into a map z value
	 * 
	 * @param height in megers
	 * @return map delta-z (0.0 - 1.0)
	 */
	public double z(double height) {
		return z_extent * height / z_range;
	}
	
	/**
	 * @return world description string (\n escapes replaced by newlines)
	 */
	public String getDescription() {
		String result = "";
		int start = 0;
		for(int next = description.indexOf("\\n",start);
				next >= 0;
				next = description.indexOf("\\n", start)) {
			result += description.substring(start, next);
			result += "\n";
			start = next + 2;
		}
		return result + description.substring(start);
	}
	/**
	 * set world description string
	 * @param descr string (possibly containing newlines, which will be escaped)
	 */
	public void setDescription(String descr) {
		description = "";
		int start = 0;
		for(int next = descr.indexOf("\n",start);
				next >= 0;
				next = descr.indexOf("\n", start)) {
			description += descr.substring(start, next);
			description += "\\n";
			start = next + 1;
		}
		description += descr.substring(start);
	}
}
