package worldBuilder;
/**
 * find and interpolate values from the surrounding MeshPoints to any map
 *    coordinate ... used to produce a (dense) Cartesian map from a (sparse)
 *    Voronoi Mesh
 */
public class Vicinity {

	/** max number of MeshPoints that define a Vicinity	*/
	public static final int NUM_NEIGHBORS = 8;
	/** indices of the MeshPoints surrounding this vicinity */
	public int[] neighbors;
	/** distances to each of the three closest MeshPoints */
	public double[] distances;
	
	/**
	 * create a new (empty) vicinity
	 * @param mesh to be searched
	 * @param x coordinate of vicinity
	 * @param y coordinate of vicinity
	 */
	public Vicinity(Mesh mesh, double x, double y) {
		neighbors = new int[NUM_NEIGHBORS];
		for(int i = 0; i < NUM_NEIGHBORS; i++)
			neighbors[i] = -1;
		distances = new double[NUM_NEIGHBORS];
		double sum_distances = 0;
		int found = 0;
		
		// start with the closest MeshPoint to this spot
		MeshPoint center = new MeshPoint(x,y);
		MeshPoint start = mesh.choosePoint(x,  y);
		neighbors[found] = start.index;
		distances[found] = center.distance(start);
		sum_distances = distances[found];
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
			sum_distances += distances[found];
			prev = current;
			current = next;
			found++;
		}
		
		if (next == start) {
			// FIX point might be outside the polygon
			return;
		}
		
		/*
		 * We were unable to close the sides of the polygon.
		 * This is likely because we are outside the mesh.
		 * 
		 * Weak heuristic: discard points that are farther from
		 * 		the center than the center is from the edge.
		 */
		double x_edge = Parameters.x_extent/2 + (x >= 0 ? -x : x);
		double y_edge = Parameters.y_extent/2 + (y >= 0 ? -y : y);
		while(found > 2 && 
			  (distances[found-1] > x_edge || distances[found-1] > y_edge)) {
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
		 * one of the Voronoi polygons, because it is outside the mesh.
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
	 * interpolate a value from those of my three closest MeshPoints
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
