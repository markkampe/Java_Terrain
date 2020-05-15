package worldBuilder;
/**
 * find and interpolate values from the surrounding MeshPoints to any map
 *    coordinate ... used to produce a (dense) Cartesian map from a (sparse)
 *    Voronoi Mesh.
 *    
 * TODO: polygon lake boundaries result in altitude discontinuities
 *
 * This problem has arisen from two conflicting goals
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
 *		boundary.  These discontinuities are generally only a problem for
 *		high-resolution altitude maps ... such as those used for Foundation.
 *
 * I am thinking that the solution may be to go back to proximity-based
 * Vicinities, and find a different solution for surface water.	
 */
public class Vicinity {

	/** max number of MeshPoints that define a Vicinity	*/
	public static final int NUM_NEIGHBORS = 12;
	/** indices of the MeshPoints surrounding this vicinity */
	public int[] neighbors;
	/** distances to each of the three closest MeshPoints */
	public double[] distances;
	
	private Mesh mesh;
	
	/**
	 * create a new (empty) vicinity
	 * @param mesh to be searched
	 * @param x coordinate of vicinity
	 * @param y coordinate of vicinity
	 */
	public Vicinity(Mesh mesh, double x, double y) {
		this.mesh = mesh;
		
		neighbors = new int[NUM_NEIGHBORS];
		for(int i = 0; i < NUM_NEIGHBORS; i++)
			neighbors[i] = -1;
		distances = new double[NUM_NEIGHBORS];
		int found = 0;
		
		// start with the closest MeshPoint to this spot
		MeshPoint center = new MeshPoint(x,y);
		MeshPoint start = mesh.choosePoint(x,  y);
		neighbors[found] = start.index;
		distances[found] = center.distance(start);
		found++;
		
		// try to follow a circle of neighboring MeshPoints
		MeshPoint prev = null;
		MeshPoint current = start;
		MeshPoint next = null;
		while(found < NUM_NEIGHBORS) {
			next = nextPoint(center, current, prev);
			if (next == start || next == null)
				break;	// closed polygon or reached dead end
			
			// add the next MeshPoint and continue
			neighbors[found] = next.index;
			distances[found] = center.distance(next);
			prev = current;
			current = next;
			found++;
		}
		
		// point successfully enclosed in a polygon
		if (next == start && !outsideMesh(center))
			return;
		
		/*
		 * We were unable to close the sides of the polygon.
		 * This is likely because we are outside the mesh.
		 * 
		 * Weak heuristic: discard all but the closest
		 * point and its most concave neighbor ... which
		 * should be adequate for points on the edge.
		 */
		while(found > 2) {
			neighbors[found-1] = -1;	// lose this point
			found -= 1;
		}
	}
	
	/**
	 * Choose the next point in the enclosing polygon
	 * @param center MeshPoint we are trying to enclose
	 * @param vertex MeshPoint of last chosen vertex	
	 * @param previous MeshPoint of vertex before that
	 * @return next MeshPoint in polygon
	 */
	private MeshPoint nextPoint(MeshPoint center, MeshPoint vertex, MeshPoint previous) {

		MeshPoint best = null;
		double dRdC = 666;
		
		/*
		 * we can enumerate the sides of a polygon by, at each MeshPoint,
		 * choosing the outgoing path that is most concave to the center.
		 * I define the most concave path as the one with the lowest
		 * derivative of radius with respect to circumference.
		 * 
		 * This heuristic fails, however, if the center is not within
		 * one of the Voronoi polygons (because it is outside the mesh)
		 * in which case minimum dR/dC no longer guarantees a minimal
		 * polygon.
		 */
		double cRadius = center.distance(vertex);
		for(int n = 0; n < vertex.neighbors; n++)
			if (vertex.neighbor[n] != previous) {	// ignore incoming path
				MeshPoint candidate = vertex.neighbor[n];
				double dRadius = center.distance(candidate) - cRadius;
				double dCircumference = vertex.distance(candidate);
				if (dRadius/dCircumference < dRdC) {
					best = candidate;
					dRdC = dRadius/dCircumference;
				}
			}
			
		return best;
	}
	
	/**
	 * determine whether or not vicinity is outside the Mesh
	 * @param center MeshPoint for vicinity
	 * 
	 * Point-Inside-Polygon is an expensive computation, so 
	 * I am using a collection of cheap heuristics.
	 */
	private boolean outsideMesh(MeshPoint center) {
		// 1. gather some info about the polygon
		double mean_radius = 0;
		int count = 0;
		double x_min = 666, x_max = -666, y_min = 666, y_max = -666;
		for (int i = 0; i < NUM_NEIGHBORS; i++)
			if (neighbors[i] >= 0) {
				MeshPoint point = mesh.vertices[neighbors[i]];
				if (point.x < x_min) x_min = point.x;
				if (point.x > x_max) x_max = point.x;
				if (point.y < y_min) y_min = point.y;
				if (point.y > y_max) y_max = point.y;	
				mean_radius += distances[i];
				count++;
			}
		mean_radius /= count;
		
		// 2. is the point completely outside the enclosing square
		if (center.x < x_min || center.x > x_max || center.y < y_min || center.y > y_max)
			return true;
		
		// 3. mean radius exceeds size of enclosing square
		if (mean_radius > (x_max - x_min))
			return true;
		if (mean_radius > (y_max - y_min))
			return true;

		// FIX center within the square but outside the polygon
		return false;
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
}
