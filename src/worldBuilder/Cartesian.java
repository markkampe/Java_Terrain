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
	/** the nearest MeshPoints to every tile in our map	*/
	private Vicinity cells[][];
	
	/** types of vicinities				*/
	public static final int NEAREST = 0;	// the one nearest MeshPoint
	public static final int NEIGHBORS = 0;	// the the 3-4 nearest MeshPoints
	public static final int POLYGON = 0;	// the surrounding polygon
	
	// private static final int ENCODE_DEBUG = 3;

	/**
	 * create a new Cartesian map
	 * @param mesh ... Mesh of Voronoi points
	 * @param left ... left edge of mapped area
	 * @param top ... top of mapped area
	 * @param right ... right edge of mapped area
	 * @param bottom ... bottom edge of mapped area
	 * @param width ... width of desired array
	 * @param height ... height of desired array
	 * @param type ... type of vicinity (NEAREST, NEIGHBORS, POLYGON)
	 */
	public Cartesian(Mesh mesh, 
					double left, double top, double right, double bottom, 
					int width, int height, int type) {	
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
				if (type == NEIGHBORS)
					cells[r][c] = new Proxcimity(mesh, x, y);
				else if (type == POLYGON)
					cells[r][c] = new Proxcimity(mesh, x, y);
				else
					cells[r][c] = new Nearest(mesh, x, y);
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
	
	/** 
	 * Gaussian Blur is a very popular technique for removing noise
	 * from images.  It recomputes the value of each point as a 
	 * weighted average of the values of all of the points within
	 * a specified radius, where the weights fall off as a Gaussian
	 * distribution.
	 * 
	 * This a (very well known) one-dimensional Gaussian kernel,
	 * for a radius of 3, normalized to sum to 1.0.
	 */
	private static final double[] kernel = 
		//  x-3     x-2      x-1    center     x+1      x+2       x+3
		{0.03663, 0.11128, 0.21675, 0.27068, 0.21675, 0.011128, 0.03663};
	
	/**
	 * perform Gaussian Blurr on a (potentially noisy) array
	 * @param array - 2D array of doubles
	 * 
	 * The Cartesian translation process can yield discontinuities 
	 * when neighboring tiles have different sets of closest MeshPoints.
	 * In most cases, these are inconsequential, but they need to be
	 * cleaned up for high-resolution altitude maps.
	 * 
	 * A 2D Gaussian Kernel is merely the product of a pair of 1D kernels,
	 * but (because multiplication is associative) we can get the same
	 * result in (O(n) time) by applying the 1D kernel twice:
	 *    1. recompute each point as the average of its horizontal neighbors
	 *    2. recompute each point as the average of its vertical neighbors
	 */
	public static void smooth(double[][] array) {
		final int diameter = kernel.length;
		final int offset = diameter/2;
		
		// get a copy array to use for the summing
		int height = array.length;
		int width = array[0].length;
		double[][] copy = new double[height][width];
		
		// blur the rows from the original into a copy
		for(int y = 0; y < height; y++)
			for(int x = 0; x < width; x++) {
				double sum = 0.0;
				for(int i = 0; i < diameter; i++) {
					int xx = x + i - offset;
					sum += kernel[i] * ((xx < 0 || xx >= width) ?
										array[y][x] : array[y][xx]);
				}
				copy[y][x] = sum;
			}
		
		// blur the columns from the copy back to the original
		for(int y = 0; y < height; y++)
			for(int x = 0; x < width; x++) {
				double sum = 0.0;
				for(int i = 0; i < diameter; i++) {
					int yy = y + i - offset;
					sum += kernel[i] * ((yy < 0 || yy >= height) ?
										copy[y][x] : copy[yy][x]);
				}
				array[y][x] = sum;
			}
	}
}
