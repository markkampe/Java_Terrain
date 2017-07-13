
/**
 * This is a singleton class containing global program parameters.
 * 
 * values may be default or overridden from command line
 */
 
 public class Parameters {
	
	// map generation parameters
	public double x_extent = 1.0;	// Xmax - Xmin
	public double y_extent = 1.0;	// Ymax - Ymin
	public int improvements = 1;	// number of smoothing iterations
	public int points = 200;		// desired number of grid points
	
	// diagnostic options
	public boolean show_points = false;	// display original and improved points
	public boolean show_grid = true;	// display final grid
	public int debug_level = 1;			// level of verbosity
	
	private static Parameters singleton = null;
	private Parameters() {}
	
	public static Parameters getInstance() {
		if (singleton == null)
			singleton = new Parameters();
		return singleton;
	}
}
