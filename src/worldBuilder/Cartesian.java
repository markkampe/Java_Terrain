package worldBuilder;

/**
 * this is a dense 2D array w/Cartesian coordinates
 * whose values cn be interpolated from the random points
 * in a Voronoi mesh.
 */
public class Cartesian {

	public int width;			// columns in this map
	public int height;			// rows in this map
	private MeshRef cells[][];	// mapping from Cartesian points to MeshPoints

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
		cells = new MeshRef[height][width];
		
		// figure out the spacing
		double dx = (double)(right - left) / width;
		double dy = (double)(bottom - top) / height;
		
		// create the Cartesian->Voronoi map for the specified region
		for(int r = 0; r < height; r++) {
			double y = top + (r * dy);
			for(int c = 0; c < width; c++) {
				double x = left + (c * dx);
				MeshPoint m = new MeshPoint(x,y);
				MeshRef ref = new MeshRef();
				cells[r][c] = ref;
				for(int v = 0; v < mesh.vertices.length; v++) {
					MeshPoint p = mesh.vertices[v];
					ref.consider(p.index, p.distance(m));
				}
			}
		}
	}
	
	/**
	 * interpolate values for every Cartesian cell
	 * 
	 * @param array of per-MeshPoint values
	 * @return Cartesian array of interpolated values
	 */
	public double[][] interpolate(double[] meshValues) {
		double[][] result = new double[height][width];
		
		for(int r = 0; r < height; r++) {
			for(int c = 0; c < width; c++) {
				// compute the proximity-weighted average value
				MeshRef ref = cells[r][c];
				double norm = 0;
				double sum = 0;
				for(int n = 0; n < MeshRef.NUM_NEIGHBORS; n++) {
					double dist = ref.distances[n];
					double v = meshValues[ref.neighbors[n]];
					sum += v/dist;
					norm += 1/dist;
				}
				result[r][c] = sum / norm;
			}
		}
		return result;
	}
	
	/**
	 * Each cell of the Voronoi to grid map is represented
	 * by a MeshRef, which describes the 3 nearest Voronoi
	 * points.
	 * 
	 * We refer to the Voronoi points by index rather than
	 * by MeshPoint because the latter change as a result
	 * of editing operations, but the former change only
	 * when a new Mesh is created.
	 * 
	 * This is a private class within Map because it needs
	 * to make reference to the selected Mesh.
	 */
	private class MeshRef {
		public int neighbors[];		// MeshPoint index
		public double distances[];	// distance to that MeshPoint
		public static final int NUM_NEIGHBORS = 3;
		
		public MeshRef() {
			neighbors = new int[NUM_NEIGHBORS];
			distances = new double[NUM_NEIGHBORS];
			
			for(int i = 0; i < NUM_NEIGHBORS; i++) {
				neighbors[i] = -1;
				distances[i] = 666;
			}
		}
		
		/**
		 * Consider a mesh point index and distance,
		 * and remember the nearest three.
		 * 
		 * @param index 	index of this mesh point
		 * @param distance	distance to this mesh points
		 */
		public void consider(int index, double distance) {
			if (distance >= distances[2])
				return;
			
			// XXX rewind top 3 neighbors loop?
			if (distance >= distances[1]) {
				// replace last in list
				neighbors[2] = index;
				distances[2] = distance;
			} else if (distance >= distances[0]) {
				// replace second in list
				neighbors[2] = neighbors[1];
				distances[2] = distances[1];
				neighbors[1] = index;
				distances[1] = distance;
			} else {
				// replace first in list
				neighbors[2] = neighbors[1];
				distances[2] = distances[1];
				neighbors[1] = neighbors[0];
				distances[1] = distances[0];
				neighbors[0] = index;
				distances[0] = distance;	
			}
		}
	}
}
