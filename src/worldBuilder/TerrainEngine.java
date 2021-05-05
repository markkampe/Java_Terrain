/**
 * This engine does the real work for terrain editing operations:
 *  - continental slope
 *  - mountain/valley creation
 *  - vertical exaggeration/compression
 */
package worldBuilder;

public class TerrainEngine {
	private Parameters parms;		// configuration singleton
	private Map map;				// map on which we are operating
	
	private double thisHeight[];	// currently active per MeshPoint Z values
	private double prevHeight[];	// last committed per MesshPoint Z values
	private int axis;				// last used slope axis
	private double inclination;		// last used slope inclination
	private int above, below;		// above/below sea-level relocations
	
	private static final int TERRAIN_DEBUG = 2;

	public TerrainEngine(Map map) {
		this.map = map;
		this.parms = Parameters.getInstance();
		
		// save the incoming heightMap
		prevHeight = map.getHeightMap();
		thisHeight = new double[prevHeight.length];
		for(int i = 0; i < prevHeight.length; i++)
			thisHeight[i] = prevHeight[i];
		
	}
	
	/**
	 * impose a slope on the entire map
	 * @param axis (degrees: -180 - 180)
	 * @param inclination (dy/dx)
	 */
	public boolean slope(int axis, double inclination) {
		this.axis = axis;
		this.inclination = inclination;
		
		// identify the end-points of the line
		double sin = Math.sin(Math.toRadians(axis));
		double cos = Math.cos(Math.toRadians(axis));
		double X0 = -cos;
		double Y0 = sin;
		double X1 = cos;
		double Y1 = -sin;
		
		// calculate maximum vertical (Z) deflection
		double Zscale = inclination;
		Zscale *= parms.xy_range * 1000;	// slope times distance (m)
		Zscale /= parms.z_range;			// scaled to Z range (m)
		
		// get map and display parameters
		Mesh m = map.getMesh();
		
		// note sea level
		double Zsealevel = parms.sea_level;
		above = 0;
		below = 0;

		// height of every point is its distance (+/-) from the axis
		for(int i = 0; i < thisHeight.length; i++) {
			double d = m.vertices[i].distanceLine(X0, Y0, X1, Y1);
			
			// make sure the new height is legal
			double newZ = Zscale * d + prevHeight[i];
			if (newZ > Parameters.z_extent/2)
				thisHeight[i] = Parameters.z_extent/2;
			else if (newZ < -Parameters.z_extent/2)
				thisHeight[i] = -Parameters.z_extent/2;
			else
				thisHeight[i] = newZ;
			
			// tally points above and below sea-level
			if (newZ > Zsealevel)
				above++;
			else
				below++;
		}
		
		// tell the map about the update
		map.setHeightMap(thisHeight);
		
		if (parms.debug_level >= TERRAIN_DEBUG)
			System.out.println(String.format("Slope axis=%d\u00B0, incline=%.1fcm/km: %d points above sea level, %d below",
								axis, inclination*100000, above, below));
		return true;
	}
	
	/**
	 * place a mountain, ridge, pit or valley on the map
	 * @param x1	starting x
	 * @param y1	starting y
	 * @param x2	ending x
	 * @param y2	ending y
	 * @param height maximum z
	 * @param width	width at base (in x/y units)
	 * @param shape  (CONICAL-SPHERICAL-CYLINDRICAL)
	 */
	public boolean mountain(double x1, double y1, double x2, double y2, double height, double width, int shape) {
		System.out.println(String.format("Mountain: <%.6f,%.6f>-<%.6f,%.6f>, height=%dM, width=%.6f, shape=%d",
										x1, y1, x2, y2, width, parms.height(height), shape));
		return false;
	}
	
	/**
	 * make this heightMap official
	 */
	public boolean commit() {
		// make the this our fall-back
		for(int i = 0; i < prevHeight.length; i++)
			prevHeight[i] = thisHeight[i];
		
		if (parms.debug_level > 0)
			if (inclination != 0)
				System.out.println(String.format("Slope axis=%d\u00B0, incline=%.1fcm/km: %d points above sea level, %d below",
								axis, inclination*100000, above, below));
		return true;
	}

	/**
	 * revert heightMap to last committed values
	 */
	public boolean abort() {
		// fall back to the last committed map
		map.setHeightMap(prevHeight);
		return true;
	}
}
