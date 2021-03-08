package worldBuilder;
/**
 * Nearest is the closest MeshPoint to an (x,y) coordinate
 *    
 *	This Vicinity sub-class is appropriate for things (like plant
 *  or mineral distribution) that do not interpolate.
 */
public class Nearest extends Vicinity {
	/**
	 * create a new Vicinity from the closest MeshPoint
	 * @param mesh to be searched
	 * @param x coordinate of vicinity
	 * @param y coordinate of vicinity
	 */
	public Nearest(Mesh mesh, double x, double y) {
		super(mesh, x, y);

		// we have not yet found any near-by points
		for (int i = 0; i < NUM_NEIGHBORS; i++) {
			distances[i] = 666;
			neighbors[i] = -1;
		}

		// find our ONE nearest neighbor
		for(int i = 0; i < mesh.vertices.length; i++) {
			double dx = mesh.vertices[i].x - x;
			double dy = mesh.vertices[i].y - y;
			double distance = Math.sqrt((dx*dx) + (dy*dy));
			if (distance < distances[0]) {
				neighbors[0] = i;
				distances[0] = distance;
			}
		}
	}
}
