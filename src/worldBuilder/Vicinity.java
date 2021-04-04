package worldBuilder;
/**
 * find and interpolate values from the surrounding MeshPoints to any map
 *    coordinate ... used to produce a (dense) Cartesian map from a (sparse)
 *    Voronoi Mesh.
 */   
/* HISTORY:
 *  I started out with the nearest neighbors, but that resulted in
 *  very poor lake boundaries.  I fixed this by defining a Vicinity
 *  as the points in the surrounding polygon, but that resulted in 
 *  discontinuous altitude interpolations.
 * 
 * 	 1.	Topographically correct lake boundaries:
 * 
 * 		Lake escape points are MeshPoints, which means that land/water
 * 		transitions should occur sharply at those MeshPoints.  Cartesian
 *      water-level interpolation among the nearest points, however,
 *      resulted in water at altitude-inappropriate points in the map.
 *      
 *      I fixed this by defining the neighbors (for interpolation purposes) as
 * 		the MeshPoints of the concavely-surrounding Voronoi polygon.
 * 
 *   2. Topographically continuous altitude maps:
 *   
 *		The above change enabled sharp transitions (between land and water)
 *		at polygon boundaries.  But it also resulted in discontinuities in
 *		interpolated altitudes for points on opposite sides of a polygon
 *		boundary.  These discontinuities might not have been noticed, until
 *		I started Foundation export with its high-resolution altitude maps.
 *
 *  To deal with this problem, I turned Vicninity into a super-class,
 *  and created distinct sub-classes for polygons and nearest neighbors.
 */
public class Vicinity {

	/** max number of MeshPoints that define a Vicinity	(most are smaller) */
	public static final int NUM_NEIGHBORS = 12;

	/** indices of the MeshPoints surrounding this vicinity */
	public int[] neighbors;
	/** distances to each of the three closest MeshPoints */
	public double[] distances;
	
	protected Mesh mesh;
	
	/**
	 * create a new (empty) vicinity
	 * @param mesh to be searched
	 * @param x coordinate of vicinity
	 * @param y coordinate of vicinity
	 */
	public Vicinity(Mesh mesh, double x, double y) {
		this.mesh = mesh;
		
		// the neighboring meshpoints will be found in the subclass
		neighbors = new int[NUM_NEIGHBORS];
		for(int i = 0; i < NUM_NEIGHBORS; i++)
			neighbors[i] = -1;
		distances = new double[NUM_NEIGHBORS];
	}
	
	/**
	 * interpolate a value from those of my surrounding MeshPoints
	 * 
	 * @param values array for all MeshPoints
	 */
	public double interpolate(double values[]) {
		// compute weighted sum of neighboring point values
		// with each value weighted inversely to its distance
		double sumValues = 0.0;
		double sumWeights = 0.0;
		for(int n = 0; n < NUM_NEIGHBORS; n++)
			if (neighbors[n] >= 0) {	// may not be in all quadrants
				double weight = 1/distances[n];
				sumValues += weight * values[neighbors[n]];
				sumWeights += weight;
			}

		return sumValues / sumWeights;			// normalize weights to sum to 1
	}
	
	/**
	 * return the value of the nearest MeshPoint
	 * @param values array for all MeshPoints
	 * 
	 * Note: I have stopped using Proxcimity Vicnities, and the first point
	 * 		 in the first point in a Polygon Vicnity is the nearest, so we
	 * 		 can just use the first value we find.
	 */
	public double nearest(double values[]) {
		for(int n = 0; n < NUM_NEIGHBORS; n++)
			if (neighbors[n] >= 0)
				return values[neighbors[n]];
			return 0;
	}
	
	/**
	 * return the value of the nearest MeshPoint with a VALID value
	 * @param values array of per MeshPoint values
	 * @param invalid the value to be ignored
	 * 
	 * This function is used to find the water level associated with
	 * the nearest neighbor, where most nodes do not have a water level
	 */
	public double nearestValid(double values[], double invalid) {
		int nearest = -1;
		for(int n = 0; n < NUM_NEIGHBORS; n++) {
			if (neighbors[n] >= 0 && values[neighbors[n]] != invalid)
				if (nearest < 0 || distances[n] < distances[nearest])
					nearest = n;
		}
		return (nearest >= 0) ? values[neighbors[nearest]] : invalid;
	}
}
