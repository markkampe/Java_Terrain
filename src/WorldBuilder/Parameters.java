package WorldBuilder;

/**
 * This is a singleton class containing global program parameters.
 * 
 * values may be default or overridden from command line
 */
 
 public class Parameters {
	// display parameters
	public int height = 800;		// screen height
	public int width = 800;			// screen width
	
	// map generation parameters
	public double x_extent = 1.0;	// Xmax - Xmin
	public double y_extent = 1.0;	// Ymax - Ymin
	public double z_extent = 1.0;	// Zmax - Zmin
	public int improvements = 1;	// number of smoothing iterations
	public int points = 2048;		// desired number of grid points
	
	// diagnostic options
	public int debug_level = 1;			// level of verbosity
	
	private static Parameters singleton = null;
	private Parameters() {}
	
	public static Parameters getInstance() {
		if (singleton == null)
			singleton = new Parameters();
		return singleton;
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
