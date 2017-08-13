package worldBuilder;

/**
 * This is a singleton class containing global program parameters.
 * 
 * values may be default or overridden from command line or dialogs
 */
 
 public class Parameters {
	// limits on world parameters
	public static final int ALT_SCALE = 1000;	// slider labeling unit
	public static final int ALT_MIN = 0;		// min altitude (m x 1000)
	public static final int ALT_MAX = 10;		// max altitude (m x 1000)
	public static final int RADIUS = 6371;		// planetary radius (m x 1000)

	// display parameters
	private static final int PIXELS = 800;		// display height/width
	public int height = PIXELS;			// screen height
	public int width = PIXELS;			// screen width
	public int dialogDX = PIXELS/3;		// X offset for dialog boxes
	public int dialogDY = 5*PIXELS/4;	// Y offset for dialog boxes
	
	// map scale/location parameters
	public int xy_range = 500;	// X/Y range (km)
	public int z_range = 10000;	// Z range (m)
	public int r_range = 500;	// rain (cm/y)
	public double latitude = 30.0;	// central point latitude
	public double longitude = -90.0;// central point longitude
	
	// map rendering thresholds
	public static final int RIVER_FLOW = 5;	// river/stream
	public double min_flux = 0.25;	// minimum stream flow (m3/s)
	public double topo_major = 0.1;	// max 10 major lines
	public int topo_minor = 5;		// minor lines per major

	// persistent defaults
	public double sea_level = 0;// sea level (map space)
	public int dDiameter;		// mountain diameter
	public int dAltitude;		// mountain altitude
	public int dShape;			// mountain shape
		public static final int CONICAL = 0;
		public static final int SPHERICAL = 4;
		public static final int CYLINDRICAL = 8;
	public int dDirection;		// incoming weather
	public int dAmount;			// annual rainfall
	public int dRainHeight;		// mean height of incoming rain
	
	
	// map generation parameters
	public int improvements = 1;	// number of smoothing iterations
	public int points = 4096;		// desired number of grid points
	
	// coordinate ranges (probably don't want to change)
	public static final double x_extent = 1.0;	// Xmax - Xmin
	public static final double y_extent = 1.0;	// Ymax - Ymin
	public static final double z_extent = 1.0;	// Zmax - Zmin

	// diagnostic options
	public int debug_level = 1;			// level of verbosity
	
	private static Parameters singleton = null;
	private Parameters() {
		dDiameter = maxDiameter()/2;
		dAltitude = z_range/20;
		dShape = (CONICAL + SPHERICAL)/2;
		dDirection = 0;				// weather from the north
		dAmount = r_range/10;		// moderate rainfall
		dRainHeight = ALT_MAX/5;	// not particularly high	
	}
	
	public static Parameters getInstance() {
		if (singleton == null)
			singleton = new Parameters();
		return singleton;
	}
	
	/**
	 * the largest legal mountain diameter
	 */
	public int maxDiameter() {
		return(xy_range/2);
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
	 * parse a switch specification and set options accordingly
	 * 
	 * @param arg	String to be parsed (less the switch character)
	 */
	public void parseSwitch( String arg ) {
		// figure out what the option is
		char c = arg.charAt(0);
		switch (c) {
		}
	}
	
	/**
	 * turn a map coordinate into a latitude
	 * 
	 * @param y (map coordinate)
	 */
	public double latitude(double y) {
		double degrees = 180.0 * xy_range / (Math.PI * RADIUS);
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
		double radius = Math.cos(lat) * RADIUS;
		
		double degrees = 360.0 * xy_range / (2 * Math.PI * radius);
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
