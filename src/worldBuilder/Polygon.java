package worldBuilder;
/**
 * Polygon is the set of connected MeshPoints surrounding an (x,y) coordinate
 *    
 *	This Vicnity sub-class yields satisfactory lake shores, where
 *  there is an abrupt transition from water to land at the altitude
 *  of the escape point.
 *  
 *  Note: the first MeshPoint in a vicinity is the one nearest to the
 *        specified <x,y> coordinates, which makes a POLYGON Vicinity
 *        also usable for nearest value (rather than interpolation)
 */
public class Polygon extends Vicinity {

	/**
	 * create a new Vicinity of the surrounding Polygon
	 * @param mesh to be searched
	 * @param x coordinate of vicinity
	 * @param y coordinate of vicinity
	 */
	public Polygon(Mesh mesh, double x, double y) {
		super(mesh, x, y);
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

		// XXX Polygon.outsideMesh within square but outside polygon?
		//	I do not handle this case correctly, but does it matter?
		return false;
	}
}
