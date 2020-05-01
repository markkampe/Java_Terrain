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
	
	
}
