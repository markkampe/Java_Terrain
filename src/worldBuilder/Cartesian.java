package worldBuilder;

/**
 * a dense 2D array w/Cartesian coordinates, whose values have
 * been interpolated from the Voronoi mesh points.
 */
public class Cartesian {

	/** number of tile columns in this map	*/
	public int width;
	/** number of tile rows in this map */
	public int height;
	/** the three nearest MeshPoints to every tile in our map	*/
	private Vicinity cells[][];
	
	private static final int ENCODE_DEBUG = 3;

	/**
	 * create a new Cartesian map
	 * @param mesh ... Mesh of Voronoi points
	 * @param left ... left edge of mapped area
	 * @param top ... top of mapped area
	 * @param right ... right edge of mapped area
	 * @param bottom ... bottom edge of mapped area
	 * @param width ... width of desired array
	 * @param height ... height of desired array
	 */
	public Cartesian(Mesh mesh, double left, double top, double right, double bottom, int width, int height) {	
		// note the key parameters
		this.height = height;
		this.width = width;
		
		// allocate the arrays
		cells = new Vicinity[height][width];
		
		// figure out the spacing
		double dx = (double)(right - left) / width;
		double dy = (double)(bottom - top) / height;
		
		// create the Cartesian->Voronoi map for the specified region
		for(int r = 0; r < height; r++) {
			double y = top + (r * dy);
			for(int c = 0; c < width; c++) {
				double x = left + (c * dx);
				cells[r][c] = new Vicinity(mesh, x, y);
			}
		}
	}
	
	/**
	 * interpolate values for every Cartesian cell
	 * 
	 * @param meshValues - array of per-MeshPoint values
	 * @return Cartesian array of interpolated values
	 */
	public double[][] interpolate(double[] meshValues) {
		double[][] result = new double[height][width];
		
		for(int r = 0; r < height; r++) {
			for(int c = 0; c < width; c++) {
				result[r][c] = cells[r][c].interpolate(meshValues);
			}
		}
		return result;
	}
	
	/**
	 * interpret a 2D array of doubles into ranges in an integer value
	 * 			 (typically used to translate altitudes to colors
	 * @param array - 2D array of doubles
	 * @param minValue - minimum value of output range
	 * @param maxValue - maximum value of output range
	 * @return 2D array of integers representing re-encoded values
	 */
	public static int[][] encode(double[][] array, int minValue, int maxValue) {
		int height = array.length;
		int width = array[0].length;
		
		// 1. find the minimum and maximum values in the input array
		double lowest = 666666;
		double highest = -666666;
		for(int y = 0; y < height; y++)
			for(int x = 0; x < width; x++) {
				if (array[y][x] > highest)
					highest = array[y][x];
				if (array[y][x] < lowest)
					lowest = array[y][x];
			}
		
		// 2. calculate the range translation factors
		double scale = maxValue - minValue;
		if (highest > lowest)
			scale /= (highest - lowest);
		
		// 3. create a new array with translated values
		int[][] encoded = new int[height][width];
		for(int y = 0; y < height; y++)
			for(int x = 0; x < width; x++) {
				double v = (array[y][x] - lowest) * scale;
				encoded[y][x] = (int) v;
			}
				
		return encoded;
	}
	
	/** 1D, radius=3, normalized Gaussian kernel		*/
	private static final double[] kernel = 
		{0.03663, 0.11128, 0.21675, 0.27068, 0.21675, 0.011128, 0.03663};
	
	/**
	 * perform Gaussian Blurr on a (potentially noisy) array
	 * @param array - 2D array of doubles
	 * @param radius - radius for smoothing
	 * 
	 * The Cartesian translation process can yield discontinuities 
	 * when neighboring tiles have different sets of closest MeshPoints.
	 * In most cases, these are inconsequential, but they need to be
	 * cleaned up for high-resolution altitude maps.
	 * 
	 * Because of associatitivy of the Gaussian function,
	 * we can do this in O(n) time:
	 *    1. do a 1D blur of each row
	 *    2. do a 1D blur of each column
	 */
	public static void smooth(double[][] array) {
		final int diameter = 7;
		final int offset = -diameter/2;
		
		// get a copy array to use for the summing
		int height = array.length;
		int width = array[0].length;
		double[][] copy = new double[height][width];
		
		// blur the rows from the original into a copy
		for(int y = 0; y < height; y++)
			for(int x = 0; x < width; x++) {
				double sum = 0.0;
				for(int i = 0; i < diameter; i++) {
					// FIX - cheat the edge cases
					sum += kernel[i] * array[y][x + i - offset];
				}
				copy[y][x] = sum;
			}
		
		// blur the columns from the copy back to the original
		for(int y = 0; y < height; y++)
			for(int x = 0; x < width; x++) {
				double sum = 0.0;
				for(int i = 0; i < diameter; i++) {
					// FIX - cheat the edge cases
					sum += kernel[i] * copy[y + i - offset][x];
				}
				array[y][x] = sum;
			}
	}
}
