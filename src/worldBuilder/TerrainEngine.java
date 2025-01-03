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
	private double thisErosion[];	// currently active per MeshPoint erosion scale
	private double prevErosion[];	// last committed per MeshPoint erosion scale
	private double thisSediment[];	// currently active per MeshPoint sedimentation scale
	private double prevSediment[];	// last committed per MeshPoint sedimentation scale
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
	private double e_factor;		// last used erosion factor
	private double s_factor;		// last used sedimentation factor
	private int adjusted;			// number of points raised/lowered
	
	private static final int TERRAIN_DEBUG = 2;

	public TerrainEngine(Map map) {
		this.map = map;
		this.parms = Parameters.getInstance();
		
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
		
		// save erosion and sedimentation factors
		prevErosion = map.getE_factors();
		prevSediment = map.getS_factors();
		thisErosion = new double[prevErosion.length];
		thisSediment = new double[prevSediment.length];
		for(int i = 0; i < prevErosion.length; i++) {
			thisErosion[i] = prevErosion[i];
			thisSediment[i] = prevSediment[i];
		}
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
		map.setHeightMap(thisHeight, true);
		
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
		map.setHeightMap(thisHeight, true);
		
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
		map.setHeightMap(thisHeight, true);
		
		this.adjusted = points;
		if (parms.debug_level >= TERRAIN_DEBUG)
			System.out.println(String.format("Exaggerated heights of %d points by %f", 
								this.adjusted, this.zMultiple));
		return true;
	}
	
	/**
	 * update erosion factor for all points in a box
	 * @param selected ... per point selected/not booleans
	 * @param e_factor erosion scaling factor
	 */
	public boolean erosion(boolean[] selected, double e_factor) {
		this.e_factor = e_factor;
		
		// set erosion factor for each of those points
		int points = 0;
		for(int i = 0; i < map.mesh.vertices.length; i++) {
			if (selected[i]) {
				thisErosion[i] = e_factor;
				points++;
			}
		}

		// tell the map about the update
		map.setE_factors(thisErosion, true);
		
		this.adjusted = points;
		if (parms.debug_level >= TERRAIN_DEBUG)
			System.out.println(String.format("Set Erosion factor of %d points to %f", 
								this.adjusted, this.e_factor));
		return true;
	}
	
	/**
	 * update sedimentation factor for all points in a box
	 * @param selected ... per point selected/not booleans
	 * @param s_factor sedimentation scaling factor
	 */
	public boolean sedimentation(boolean[] selected, double s_factor) {
		this.s_factor = s_factor;
		
		// set sedimentation factor for each of those points
		int points = 0;
		for(int i = 0; i < map.mesh.vertices.length; i++) {
			if (selected[i]) {
				thisSediment[i] = s_factor;
				points++;
			}
		}

		// tell the map about the update
		map.setS_factors(thisSediment, true);
		
		this.adjusted = points;
		if (parms.debug_level >= TERRAIN_DEBUG)
			System.out.println(String.format("Set Sedimentation factor of %d points to %f", 
								this.adjusted, this.s_factor));
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
			double dLine = p.distanceLine(x0, y0, x1, y1);	// only valid between the foci
			double nearest = (d0 < d1) ? d0 : d1;
			double farthest = (d0 > d1) ? d0 : d1;
			double hypotenuse = Math.sqrt((rMax*rMax) + (sep*sep));	// TODO see note A
			if (parms.dOutline == Parameters.SQUARE) {
				if (sep <= rMax || (nearest <= rMax && farthest >= hypotenuse)) {
					d = nearest;		// this point is outside of the foci
					if ((p.x <= p0.x && p.x <= p1.x) || (p.y >= p0.y && p.y >= p1.y)) {
						// below or to the left of the lower left end-point
						radius = r2;
						shape = shape2;
					}
				} else {				// point is somewhere off the center line
					if (dLine < 0) {	// negative means left or below center line
						radius = r2;
						shape = shape2;
						d = -dLine;
					} else
						d = dLine;
				}
			} else {	// ELIPTICAL
				if (dLine < 0) {	// dLine tells us top/right vs bottom/left
					radius = r2;
					shape = shape2;
				}
			}
				
			// compare (more accurate) distance w/correct radius
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
		
		map.setHeightMap(thisHeight, true);
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
		map.setIncoming(thisRivers, true);
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
		for(int i = 0; i < prevErosion.length; i++) {
			prevErosion[i] = thisErosion[i];
			prevSediment[i] = thisSediment[i];
		}
		
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
			if (e_factor != 0) {
				System.out.println(String.format("Scaled erosion at %d points by %f",
						this.adjusted, this.e_factor));
				e_factor = 0;
			}
			if (s_factor != 0) {
				System.out.println(String.format("Scaled sedimentation at %d points by %f",
						this.adjusted, this.s_factor));
				s_factor = 0;
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
		// back out any changes to erosion/sedimentation
		map.setE_factors(prevErosion, false);
		map.setS_factors(prevSediment, false);
		for(int i = 0; i < prevErosion.length; i++) {
			thisErosion[i] = prevErosion[i];
			thisSediment[i] = prevSediment[i];
		}
		
		// back out any changes to incoming rivers
		for(int i = 0; i < prevRivers.length; i++)
			thisRivers[i] = prevRivers[i];
		map.setIncoming(prevRivers, false);
		
		// revert to the last committed height map
		for(int i = 0; i < prevHeight.length; i++)
			thisHeight[i] = prevHeight[i];
		map.setHeightMap(prevHeight, true);	// recompute drainage/waterflow
		
		return true;
	}
}
