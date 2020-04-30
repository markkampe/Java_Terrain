package worldBuilder;
/**
 * find and interpolate values from the surrounding MeshPoints to any map
 *    coordinate ... used to produce a (dense) Cartesian map from a (sparse)
 *    Voronoi Mesh
 */
public class Vicinity {

	/** number of MeshPoints that define a Vicinity	*/
	public static final int NUM_NEIGHBORS = 4;
	/** indices of the (closeness sorted) three closest MeshPoints in this vicinity */
	public int[] neighbors;
	/** distances to each of the three closest MeshPoints */
	public double[] distances;
	
	private double x, y;	// center of vicinity
	
	// indices into the neighbors array
	private static final int SE = 0;
	private static final int SW = 1;
	private static final int NW = 2;
	private static final int NE = 3;
	
	/**
	 * create a new (empty) vicinity
	 * @param x coordinate of vicinity
	 * @param y coordinate of vicinity
	 */
	public Vicinity(double x, double y) {
		this.x = x;
		this.y = y;
		neighbors = new int[NUM_NEIGHBORS];
		distances = new double[NUM_NEIGHBORS];
		for (int i = 0; i < NUM_NEIGHBORS; i++) {
			neighbors[i] = -1;
			distances[i] = 666;
		}
	}

	/**
	 * test a MeshPoint to see if it is immediately surrounding
	 * 
	 * @param p - MeshPoint to be considered
	 */
	public void consider(MeshPoint p) {
		// how far is it to this point
		double dx = p.x - x;
		double dy = p.y - y;
		double distance = Math.sqrt((dx*dx) + (dy*dy));
		
		// note the closest point in each quadrant
		if (dx >= 0) {	// point is to the east
			if (dy >= 0) {
				if (distance < distances[SE]) {
					neighbors[SE] = p.index;
					distances[SE] = distance;
				}
			} else {
				if (distance < distances[NE]) {
					neighbors[NE] = p.index;
					distances[NE] = distance;
				}
			}
		} else {	// point is to the west
			if (dy >= 0) {
				if (distance < distances[SW]) {
					neighbors[SW] = p.index;
					distances[SW] = distance;
				}
			} else {
				if (distance < distances[NW]) {
					neighbors[NW] = p.index;
					distances[NW] = distance;
				}
			}
		}
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
