package worldBuilder;

/**
 * This is a singleton class containing global program parameters.
 * 
 * values may be default or overridden from command line or dialogs
 */
 
 public class Parameters {
	// display parameters
	public int height = 800;		// screen height
	public int width = 800;			// screen width
	
	// map scaling parameters
	public int xy_range = 500;	// X/Y range (km)
	public int z_range = 10000;	// Z range (m)
	public int r_range = 500;	// rain (cm/y)
	
	// persistent defaults
	public int dDiameter;		// mountain diameter
	public int dAltitude;		// mountain altitude
	public int dShape;			// mountain shape
	public static final int CONICAL = 0;
	public static final int SPHERICAL = 6;
	
	// map generation parameters
	public int improvements = 1;	// number of smoothing iterations
	public int points = 4096;		// desired number of grid points
	
	// coordinate ranges (probably don't want to change)
	public double x_extent = 1.0;	// Xmax - Xmin
	public double y_extent = 1.0;	// Ymax - Ymin
	public double z_extent = 1.0;	// Zmax - Zmin

	// diagnostic options
	public int debug_level = 1;			// level of verbosity
	
	private static Parameters singleton = null;
	private Parameters() {
		dDiameter = maxDiameter()/2;
		dAltitude = z_range/4;
		dShape = (CONICAL + SPHERICAL)/2;
		
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
		return(xy_range/5);
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
}
