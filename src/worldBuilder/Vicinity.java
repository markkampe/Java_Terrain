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
		distances = new double[NUM_NEIGHBORS];
		int found = 0;
		
		// start with the closest MeshPoint to this spot
		MeshPoint center = new MeshPoint(x,y);
		MeshPoint start = mesh.choosePoint(x,  y);
		neighbors[found] = start.index;
		distances[found++] = center.distance(start);
		
		// try to enumerate the enclosing polygon
		MeshPoint prev = null;
		MeshPoint current = start;
		while(found < NUM_NEIGHBORS) {
			MeshPoint next = nextPoint(center, current, prev);
			if (next == null)
				break;	// we can go no farther
			if (next == start)
				break;	// we have come full circle
			neighbors[found] = next.index;
			distances[found++] = center.distance(next);
			prev = current;
			current = next;
		}
		
		// pad out the remainder of the neighbors array
		while(found < NUM_NEIGHBORS) {
			neighbors[found] = -1;
			distances[found++] = 666;
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
		
		double cRadius = center.distance(vertex);
		for(int n = 0; n < vertex.neighbors; n++)
			if (vertex.neighbor[n] != previous) {
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
