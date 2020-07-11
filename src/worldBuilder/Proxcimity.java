package worldBuilder;
/**
 * Proxcimity is the set of MeshPoints nearest to an (x,y) coordinate
 *    
 *	This Vicnity sub-class yields satisfactory interpolations of
 *  continuous functions (like altitude).
 */
public class Proxcimity extends Vicinity {

	// a proxcimity is defined by the nearest neighbors in each direction
	private static final int SE = 0;
	private static final int SW = 1;
	private static final int NW = 2;
	private static final int NE = 3;

	private double x, y;				// center of our Vicinity

	/**
	 * create a new Vicinity from the closest MeshPoints
	 * @param mesh to be searched
	 * @param x coordinate of vicinity
	 * @param y coordinate of vicinity
	 */
	public Proxcimity(Mesh mesh, double x, double y) {
		super(mesh, x, y);
		this.x = x;
		this.y = y;

		// we have not yet found any near-by points
		for (int i = 0; i < NUM_NEIGHBORS; i++)
			distances[i] = 666;
		
		// find the nearest points in the mesh
		for(int i = 0; i < mesh.vertices.length; i++)
			consider(mesh.vertices[i]);
	}

	/**
	 * note the nearest MeshPoint in each quadrant
	 * @param mesh point to be considered
	 */
	private void consider(MeshPoint p) {
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
}
