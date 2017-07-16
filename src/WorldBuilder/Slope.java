package WorldBuilder;

/**
 * Slope operations introduce a constant slope to the whole map
 */
public class Slope {
	
	/**
	 * incline the entire map plane
	 * @param m		cordinate mesh
	 * @param slope (dy/dx)
	 * @param inclination (0-1.0)
	 */
	public static void incline(Mesh m, double slope, double inclination) {
		Parameters parms = Parameters.getInstance();
		
		// FIX ... for now, the axis is a horizontal line
		double a, b, c = 0;
		if (slope == 0) {
			a = 0;
			b = 1;
		} else if (slope > 1) {
			a = slope;
			b = -1;
		} else {
			a = -1;
			b = slope;
		}
		
		// height of every point is its distance (+/-) from the axis
		for(int i = 0; i < m.vertices.length; i++) {
			double z = m.vertices[i].distanceLine(a, b, c);
			z *= inclination * (parms.z_extent/2);
			m.vertices[i].z = z;
		}
	}
}
