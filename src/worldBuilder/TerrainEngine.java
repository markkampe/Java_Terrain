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
	 * @param axis
	 * @param inclination
	 */
	public boolean slope(int axis, double inclination) {
		System.out.println("slope: axis=" + axis + ", dz/dx=" + inclination);
		return false;
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
