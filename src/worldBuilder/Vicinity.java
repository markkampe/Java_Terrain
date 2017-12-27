package worldBuilder;
/**
 * Each cell of the Voronoi to grid map is represented by a MeshRef, which
 * describes the 3=N nearest Voronoi points.
 * 
 * We refer to the Voronoi points by index rather than by MeshPoint because the
 * latter change as a result of editing operations, but the former change only
 * when a new Mesh is created.
 */

public class Vicinity {

	public static final int NUM_NEIGHBORS = 3;
	public int[] neighbors;
	public double[] distances;
	
	public Vicinity() {
		neighbors = new int[NUM_NEIGHBORS];
		distances = new double[NUM_NEIGHBORS];
		for (int i = 0; i < NUM_NEIGHBORS; i++) {
			neighbors[i] = -1;
			distances[i] = 666;
		}
	}

	/**
	 * Consider a mesh point index and distance, and remember the nearest three.
	 * 
	 * @param index
	 *            index of this mesh point
	 * @param distance
	 *            distance to this mesh points
	 */
	public void consider(int index, double distance) {
		if (distance >= distances[NUM_NEIGHBORS-1])
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
	
	/**
	 * interpolate a value from those of the three nearest neighbors
	 * 
	 * @param values	array of per-point values
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
