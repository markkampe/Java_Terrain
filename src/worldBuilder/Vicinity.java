package worldBuilder;
/**
 * find and interpolate values from the 3 nearest MeshPoints to any map coordinate
 *    ... used to produce a (dense) Cartesian map from a (sparse) Voronoi mesh
 *
 * Note: we use mesh-indices rather than MeshPoints because the former are
 *	     stable over the life of the mesh.
 */
public class Vicinity {

	/** number of MeshPoints that define a Vicinity	*/
	public static final int NUM_NEIGHBORS = 3;
	/** indices of the (closeness sorted) three closest MeshPoints in this vicinity */
	public int[] neighbors;
	/** distances to each of the three closest MeshPoints */
	public double[] distances;
	
	/**
	 * create a new (empty) vicinity
	 */
	public Vicinity() {
		neighbors = new int[NUM_NEIGHBORS];
		distances = new double[NUM_NEIGHBORS];
		for (int i = 0; i < NUM_NEIGHBORS; i++) {
			neighbors[i] = -1;
			distances[i] = 666;
		}
	}

	/**
	 * test a MeshPoint to see if it is one of the three closest
	 * 
	 * @param index of MeshPoint to be considered
	 * @param distance from point of interest to this MeshPoint
	 */
	public void consider(int index, double distance) {
		if (distance >= distances[NUM_NEIGHBORS-1])
			return;

		// XXX should I rewind this top 3 neighbors loop?
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
	
	/**
	 * interpolate a value from those of my three closest MeshPoints
	 * 
	 * @param values	values for my three nearest MeshPoints
	 */
	public double interpolate(double values[]) {
		double norm = 0;	// distance weighted sum of unit values
		double sum = 0;		// distance weighted sum of values
		for(int n = 0; n < NUM_NEIGHBORS; n++) {
			double dist = distances[n];
			double v = values[neighbors[n]];
			sum += v/dist;
			norm += 1/dist;
		}
		return sum / norm;
	}
}
