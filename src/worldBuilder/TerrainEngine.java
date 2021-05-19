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
	private double prevHeight[];	// last committed per MeshPoint Z values
	private double thisRivers[];	// currently active per MeshPoint incoming flux
	private double prevRivers[];	// last committed per MeshPoint incoming flux
	private int axis;				// last used slope axis
	private double inclination;		// last used slope inclination
	private int above, below;		// above/below sea-level relocations
	private double lastFlux;		// last added river flux
	private MeshPoint lastRiver;	// MeshPoint of last added river
	private double deltaZ;			// last used raise/lower distance
	private double zMultiple;		// last used exaggerate/compress factor
	private double ridgeHeight;		// height of last ridge
	private double ridgeLength;		// length of last ridge
	private double ridgeRadius;		// radius of last ridge
	private int adjusted;			// number of points raised/lowered
	
	/*
	 * SQUARE borders parallel the ridge line (at distance r)
	 * ELIPTICAL radius r at borders, but bulge mid-ridge.
	 * 
	 * Note B
	 * 		I have code that can implement SQUARE mountain outlines, but 
	 * 		the (even distorted by the Voronoi mesh) these lines look 
	 * 		unnaturally straight.  The ELIPTICAL outlines, while they 
	 * 		take great liberties with the assigned radius, look far
	 * 		more natural.  But it is trivial to change between the two
	 * 		models ... or you could even make it a parameter (to ridge?)
	 */
	private enum outline {SQUARE, ELIPTICAL};
	private outline ridge_outline;
	
	private static final int TERRAIN_DEBUG = 2;

	public TerrainEngine(Map map) {
		this.map = map;
		this.parms = Parameters.getInstance();
		this.ridge_outline = outline.ELIPTICAL;	// see Note B
		
		// save the incoming heightMap
		prevHeight = map.getHeightMap();
		thisHeight = new double[prevHeight.length];
		for(int i = 0; i < prevHeight.length; i++)
			thisHeight[i] = prevHeight[i];
		
		// save the incoming river flux
		prevRivers = map.getIncoming();
		thisRivers = new double[prevRivers.length];
		for(int i = 0; i < prevRivers.length; i++)
			thisRivers[i] = prevRivers[i];
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
		double Zsealevel = map.getSeaLevel();
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
	 * raise or lower all points in a box
	 * @param selected ... per point selected/not booleans
	 * @param deltaZ amount to add to each MeshPoint
	 */
	public boolean raise(boolean[] selected, double deltaZ) {
		this.deltaZ = deltaZ;
		
		// adjust the height of every point in the box
		int points = 0;
		for(int i = 0; i < map.mesh.vertices.length; i++) {
			if (selected[i]) {
				thisHeight[i] += deltaZ;
				points++;
			}
		}

		// tell the map about the update
		map.setHeightMap(thisHeight);
		
		this.adjusted = points;
		if (parms.debug_level >= TERRAIN_DEBUG)
			System.out.println(String.format("Adjusted heights of %d points by %d%s", 
								this.adjusted, (int) parms.height(deltaZ), Parameters.unit_z));
		return true;
	}
	
	/**
	 * exaggerate/compress delta Z for all points in a box
	 * @param selected ... per point selected/not booleans
	 * @param deltaZ amount to add to each MeshPoint
	 */
	public boolean exaggerate(boolean[] selected, double zMultiple) {
		this.zMultiple = zMultiple;
		
		// find the mean altitude for the box
		int points = 0;
		double zMin = 666, zMax = -666;
		for(int i = 0; i < map.mesh.vertices.length; i++) {
			if (selected[i]) {
				if (prevHeight[i] > zMax)
					zMax = prevHeight[i];
				if (prevHeight[i] < zMin)
					zMin = prevHeight[i];
				points++;
			}
		}
		
		if (points == 0)
			return false;
		else
			adjusted = points;
		
		// exaggerate all those points by the specified amount
		double zMean = (zMax + zMin)/2;
		for(int i = 0; i < map.mesh.vertices.length; i++) {
			if (selected[i]) {
				double delta = prevHeight[i] - zMean;
				thisHeight[i] = zMean + (delta * zMultiple);
				points++;
			}
		}

		// tell the map about the update
		map.setHeightMap(thisHeight);
		
		this.adjusted = points;
		if (parms.debug_level >= TERRAIN_DEBUG)
			System.out.println(String.format("Exaggerated heights of %d points by %f", 
								this.adjusted, this.zMultiple));
		return true;
	}
	
	/**
	 * lay out a symmetric mountain/pit or ridge/valley
	 * @param x0	one end x (map coordinate)
	 * @param y0	one end y (map coordinate)
	 * @param x1	other end x (map coordinate)
	 * @param y1	other end y (map coordinate)
	 * @param height	height/depth (z units)
	 * @param radius	width/2 (x/y units)
	 * @param shape	from Parameters.CONICAL-CYLINDRICAL
	 * @return	boolean (were any points relocated)
	 */
	public boolean ridge(double x0, double y0, double x1, double y1, double height, double radius, int shape) {
		// pass symmetric parameters to asymmetric ridge builder
		return ridge(x0, y0, x1, y1, height, radius, radius, shape, shape);
	}
	
	// shape coefficients, indexed by shape parameter
	//								  cone                   sphere                  cylinder
	private static double[] f_cone = {1.00, 0.75, 0.50, 0.25, 0.00, 0.00, 0.00, 0.00, 0.00};
	private static double[] f_circ = {0.00, 0.25, 0.50, 0.75, 1.00, 0.75, 0.50, 0.25, 0.00};
	private static double[] f_cyl =  {0.00, 0.00, 0.00, 0.00, 0.00, 0.25, 0.50, 0.75, 1.00};
	/**
	 * lay out a potentially asymmetric mountain/pit or ridge/valley
	 * @param x0	one end x (map coordinate)
	 * @param y0	one end y (map coordinate)
	 * @param x1	other end x (map coordinate)
	 * @param y1	other end y (map coordinate)
	 * @param height	height/depth (z units)
	 * @param r1	top/right width/2 (x/y units)
	 * @param r2	bot/left width/2 (x/y units)
	 * @param shape1 top/right profile (from Parameters.CONICAL-CYLINDRICAL)
	 * @param shape2 bot/left profile
	 * @return	boolean (were any points relocated)
	 * 
	 * NOTE A:
	 * 		MeshPoint.distanceLine() behaves badly when the line is of zero
	 * 		length or the point is beyond one of its end-points.  Radius out
	 * 		from an end-point works out-side the end-points, but yields incorrectly
	 * 		small values for points between the two end-points.  Thus it is important
	 * 		that we be able to distinguish these two cases so we can use the most
	 * 		appropriate distance formula.  
	 * 
	 * 		A point is in the end region if it is within R of an end-point, and
	 * 		beyond the separation distance from the other end-point.  That separation
	 * 		distance ranges between sep and sqrt(sep^2 + radius^2) ... depending on
	 * 		how far away from the center line the point is.  I am currently using
	 * 		the larger number for the test, but this means that distanceLine is being
	 * 		used in regions where it yields incorrect values ... resulting in radius
	 * 		inaccuracies outside the end-points.
	 */
	public boolean ridge(double x0, double y0, double x1, double y1, double height, double r1, double r2, int shape1, int shape2) {
		// restore all heights to last committed values
		for(int i = 0; i < prevHeight.length; i++)
			thisHeight[i] = prevHeight[i];
		
		// note the two end-points and distance between them
		MeshPoint p0 = new MeshPoint(x0, y0);
		MeshPoint p1 = new MeshPoint(x1, y1);
		double sep = p0.distance(p1);
		double rMax = (r1 > r2) ? r1 : r2;
		
		// update all points within the range of this ridge
		int points = 0;
		for(int i = 0; i < thisHeight.length; i++) {
			// how far is this point from each of the foci
			MeshPoint p = map.mesh.vertices[i];
			double d0 = p.distance(p0);
			double d1 = p.distance(p1);
			
			// crude/cheap test for being farther than radius from ridgeline
			double d = (sep > rMax) ? 
					(d0 + d1 - sep) : 	// within elipse defined by foci
					(d0 + d1)/2;		// within a circle around center
			if (d > rMax)
				continue;
			
			double radius = r1;			// symmetric or top/right 
			int shape = shape1;			// symmetric or top/right
			
			// calculate distance from the ridge-line, or the end-points?
			//  (distanceLine doesn't work for points off the end)
			double dLine = p.distanceLine(x0, y0, x1, y1);
			double nearest = (d0 < d1) ? d0 : d1;
			double farthest = (d0 > d1) ? d0 : d1;
			double hypoteneuse = Math.sqrt((rMax*rMax) + (sep*sep));	// TODO see note A
			if (sep <= rMax || (nearest <= rMax && farthest >= hypoteneuse)) {
				d = nearest;			// point is off one end
				if ((p.x <= p0.x && p.x <= p1.x) || (p.y >= p0.y && p.y >= p1.y)) {
					// below or to the left of the lower left end-point
					radius = r2;
					shape = shape2;
				}
			} else {					// point is somewhere off the center line
				if (dLine < 0) {	// below or to the left of the center line
					radius = r2;
					shape = shape2;
				}
				if (ridge_outline == outline.SQUARE)
					d = (dLine > 0) ? dLine : -dLine;
			}
				
			// now we have more accurate radius and distance
			if (d >= radius)
				continue;
			
			// calculate the delta-z for this point (based on chosen shape)
			double dz_cone = f_cone[shape] * (radius - d) * height / radius;
			double dz_circ = f_circ[shape] * Math.cos(Math.PI*d/(4*radius)) * height;
			double dz_cyl = f_cyl[shape] * height;
			double z_new = thisHeight[i] + dz_cone + dz_circ + dz_cyl;
			
			// make sure it is legal
			if (z_new > Parameters.z_extent/2)
				thisHeight[i] = Parameters.z_extent/2;
			else if (z_new < -Parameters.z_extent/2)
				thisHeight[i] = -Parameters.z_extent/2;
			else
				thisHeight[i] = z_new;
			
			points++;
		}
		
		map.setHeightMap(thisHeight);
		this.adjusted = points;
		this.ridgeHeight = height;
		this.ridgeRadius = rMax;
		this.ridgeLength = sep;
		return points > 0;
	}
	
	/**
	 * set the incoming river flux for the specified MeshPoint
	 * @param MeshPointoint to be updated
	 * @param flux for this MeshPoint
	 */
	public boolean setIncoming(MeshPoint p, double flux) {
		thisRivers[p.index] = flux;
		map.setIncoming(thisRivers);
		lastRiver = p;
		lastFlux = flux;
		
		if (parms.debug_level >= TERRAIN_DEBUG) {
			System.out.println(String.format("Arterial river enters at <%.6f,%.6f>, flow=%.1f%s",
					parms.latitude(p.y), parms.longitude(p.x), flux, Parameters.unit_f));
		}
		return true;
	}
	
	/**
	 * make this heightMap official
	 */
	public boolean commit() {
		// make the this our fall-back
		for(int i = 0; i < prevHeight.length; i++)
			prevHeight[i] = thisHeight[i];
		for(int i = 0; i < prevRivers.length; i++)
			prevRivers[i] = thisRivers[i];
		
		// log the most recent changes being committed
		if (parms.debug_level > 0) {
			if (ridgeHeight > 0) {
				System.out.println(String.format("Mountain/Ridge (length %d%s, width %d%s, height %d%s) raised %d points",
						(int) parms.km(ridgeLength), Parameters.unit_xy, (int) parms.km(ridgeRadius), Parameters.unit_xy,
						(int) parms.height(ridgeHeight), Parameters.unit_z, adjusted));
				ridgeHeight = 0;
				ridgeLength = 0;
				ridgeRadius = 0;
			} else if (ridgeHeight < 0) {
				System.out.println(String.format("Pit/Valley (length %d%s, width %d%s, depth %d%s) lowered %d points",
						(int) parms.km(ridgeLength), Parameters.unit_xy, (int) parms.km(ridgeRadius), Parameters.unit_xy,
						(int) parms.height(-ridgeHeight), Parameters.unit_z, adjusted));
				ridgeHeight = 0;
				ridgeLength = 0;
				ridgeRadius = 0;
			}
			if (deltaZ != 0) {
				System.out.println(String.format("Adjusted heights of %d points by %d%s", 
						this.adjusted, (int) parms.height(deltaZ), Parameters.unit_z));
				deltaZ = 0;
			}
			if (zMultiple != 0) {
				System.out.println(String.format("Exaggerated heights of %d points by %f", 
						this.adjusted, this.zMultiple));
				zMultiple = 0;
			}
			if (inclination != 0) {
				System.out.println(String.format("Slope axis=%d\u00B0, incline=%.1fcm/km: %d points above sea level, %d below",
								axis, inclination*100000, above, below));
				inclination = 0;
			}
			if (lastRiver != null) {
				System.out.println(String.format("Arterial river enters at <%.6f,%.6f>, flow=%.1f%s",
						parms.latitude(lastRiver.y), parms.longitude(lastRiver.x), lastFlux, Parameters.unit_f));
				lastRiver = null;
			}
		}
		return true;
	}

	/**
	 * revert maps to last committed values
	 */
	public boolean abort() {
		for(int i = 0; i < prevHeight.length; i++)
			thisHeight[i] = prevHeight[i];
		map.setHeightMap(prevHeight);
		
		for(int i = 0; i < prevRivers.length; i++)
			thisRivers[i] = prevRivers[i];
		map.setIncoming(prevRivers);
		return true;
	}
}
