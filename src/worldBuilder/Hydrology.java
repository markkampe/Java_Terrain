package worldBuilder;

import java.awt.Color;

/**
 * engine to use topology to compute water placement, flow,
 * erosion an dsedimentation.
 */
/*
 * This is probably the most complex and computationally expensive
 * class in the entire program as it embodies a considerable amount 
 * of (badly approximated) physics.
 */
public class Hydrology {
	
	private Map map;
	private Mesh mesh;
	private Parameters parms;
	
	// instance variables used by reCacluate & height Sort
	private double heightMap[];	// Z value of each MeshPoint
	private double erodeMap[];	// Z erosion of each MeshPoint
	private double velocityMap[];	// water speed through MeshPoint
	private int downHill[];		// down-hill neighbor of each MeshPoint
	private int byHeight[];		// MeshPoint indices, sorted by height
	private boolean oceanic[];	// which points are under the sea
	
	/** how much water can different types of soil hold (m^3/m^3) */
	public static double saturation[] = {
		0.03,	// max water content of sedimentary soil
		0.02,	// max water content of metamorphic soil
		0.01,	// max water content of igneous soil
		0.06	// max water content of alluvial soil
	};
	
	/** relative erosion resistances for various bed rocks */
	public static double competence[] = {
		1.0,	// sedimentary erosion resistance
		4.0,	// metamorphic erosion resistance
		2.5,	// igneous erosion resistance
		0.3		// alluvial erosion resistance
	};
	
	/**
	 * The sinkMap indicates, for each non-oceanic MeshPoint, the 
	 * point to which it ultimately drains.  It is used to identify
	 * the set of points within a common depression.
	 */
	private int sinkMap[];
	private static final int UNKNOWN = -666;	// sink point not yet found
	private static final int OCEAN = -1;		// drains to ocean
	private static final int OFF_MAP = -2;		// drains off map

	// a few useful conversion constants
	private static final double year = 365.25 * 24 * 60 *  60;
	private double area;			// square meters per MeshPoint
	private double rain_to_flow;	// maping from rain(cm) to flow(M^3/s)
	
	private static final int HYDRO_DEBUG = 2;
	
	/**
	 * Water flow and level calculation engine
	 * @param Map (and Mesh) on which we will operate
	 */
	public Hydrology(Map map) {
		this.map = map;
		this.mesh = map.getMesh();
		this.parms = Parameters.getInstance();
		
		// create our internal working maps
		oceanic = new boolean[mesh.vertices.length];
		sinkMap = new int[mesh.vertices.length];
		velocityMap = new double[mesh.vertices.length];
		byHeight = new int[mesh.vertices.length];
		for(int i = 0; i < byHeight.length; i++)
			byHeight[i] = i;

		// many square meters per (average) mesh point
		area = 1000000 * (parms.xy_range * parms.xy_range) / mesh.vertices.length;
		// how many M^3/s is 1cm of annual rainfall
		rain_to_flow = .01 * area / year;
	}

	/**
	 * mark a point (and its sub-sea-level neighbors) as oceanic
	 * @param point_index of the known-to-be-oceanic point
	 */
	private void mark_as_oceanic(int point_index) {
		oceanic[point_index] = true;

		// follow the chain of neighbors til we go above sea-level
		for(int i = 0; i < mesh.vertices[point_index].neighbors; i++) {
			int x = mesh.vertices[point_index].neighbor[i].index;
			if (heightMap[x] < parms.sea_level && !oceanic[x])
				mark_as_oceanic(x);
		}
	}
	
	/**
	 * use topography to compute the directions water will flow,
	 *	   regenerate the downHill map
	 */
	public void drainage() {
		// obtain copies of the current height/erosion maps
		heightMap = map.getHeightMap();
		if (mesh == null || mesh.vertices.length == 0 || heightMap == null)
			return;
		erodeMap = map.getErodeMap();
		downHill = map.getDownHill();
		
		// reinitialize the maps we are to create
		for(int i = 0; i < mesh.vertices.length; i++) {
			downHill[i] = -1;		// no known down-hill neighbors
			sinkMap[i] = UNKNOWN;	// no known sink points
			oceanic[i] = false;		// no known ocean points
		}

		// turn off any previous sink-point debugs
		if (parms.debug_level >= HYDRO_DEBUG)
			map.highlight(-1, null);

		/*
		 * 1. determine which points are under the ocean
		 *	  a) any sub-sea-level point on the edge is oceanic
		 *    b) any sub-sea-level neighbor of an oceanic point
		 */
		for(int i = 0; i < mesh.vertices.length; i++) {
			if (oceanic[i])		// already known to be oceanic
				continue;

			if (heightMap[i] < parms.sea_level && mesh.vertices[i].neighbors < 3)
				mark_as_oceanic(i);		// recurses for all sub-sea-level neighbors
		}

		/*
		 * 2. determine the down-hill neighbor of all non-oceanic points
		 */
		for(int i = 0; i < mesh.vertices.length; i++) {
			if (oceanic[i])			// sub-oceanic points have none
				continue;

			// find the lowest neighbor who is lower than me
			double lowest_height = heightMap[i] - erodeMap[i];
			for( int n = 0; n < mesh.vertices[i].neighbors; n++) {
				int x = mesh.vertices[i].neighbor[n].index;
				double z = heightMap[x] - erodeMap[x];
				if (z < lowest_height) {
					downHill[i] = x;
					lowest_height = z;
				}
			}
		}

		/*
		 * 3. find sink-point for each non-oceanic point
		 *	  (scan lowest-to-highest because sink points are transitive)
		 */
		heightSort(0, byHeight.length - 1);

		for(int i = byHeight.length - 1; i >= 0; i--) {
			int point = byHeight[i];
			
			if (oceanic[point])		// oceanic points have no sinks
				continue;

			if (sinkMap[point] != UNKNOWN)	// already know this point
				continue;
			
			// low points on edge of map drain off-map
			if (mesh.vertices[point].neighbors < 3) {
				sinkMap[point] = OFF_MAP;
				continue;
			}
			
			// others inherit sink from their downHill neighbor
			int d = downHill[point];
			if (d >= 0) {
					sinkMap[point] = sinkMap[d];
					continue;
			}
			
			// any point with no down-hill neighbors is a sink
			sinkMap[point] = point;
		}

		/*
		 * 4. find the escape point from each local sink, and then 
		 *	  re-route all flow in that sink to the escape point
		 */
		int escapePoint;
		double escapeHeight;
		boolean combined;
		do { combined = false;	// iterate until no more changes

			// find a local sink with no known escape point
			for(int s = 0; s < mesh.vertices.length; s++) {
				if (oceanic[s] || sinkMap[s] != s || downHill[s] >= 0)
					continue;
				
				// search all points in this sink for lowest outside neighbor
				escapePoint = -1;
				escapeHeight = Parameters.z_extent;
				for(int i = 0; i < byHeight.length; i++) {
					int point = byHeight[i];
					if (sinkMap[point] != s)	// point not in this sink
						continue;
					
					for(int j = 0; j < mesh.vertices[point].neighbors; j++) {
						int n = mesh.vertices[point].neighbor[j].index;
						if (sinkMap[n] == s)	// neighbor still in this sink
							continue;
						double z = heightMap[n] - erodeMap[n];
						if (z < escapeHeight) {
							escapePoint = n;
							escapeHeight = heightMap[n] - erodeMap[n];
						}
					}
				}
				
				// re-route all drainage from this sink to that escape point
				if (escapePoint >= 0) {
					// merge this depression into the escape point's depression
					for(int i = 0; i < mesh.vertices.length; i++) {
						if (sinkMap[i] != s)	// not a point in this sink
							continue;
						
						// redirect flow from local sink to new escape point
						int dh = downHill[i];
						if (dh == -1 || heightMap[i] - erodeMap[i] <= escapeHeight) {
							downHill[i] = escapePoint;
							// surface[i] = EPSILON + escapeHeight - (heightMap[i] - erodeMap[i]);
						}
						
						// reassign this point to the surrounding basin
						sinkMap[i] = sinkMap[escapePoint];
						
						combined = true;
					}
				}
				
				/*
				 * highlights for escape point debugging
				 *  ORANGE	sink w/escape point
				 *	RED		sink w/no escape point
				 *  BLUE	escape point to the ocean
				 *  GREEN	escape point to another sink
				 */
				if (parms.debug_level >= HYDRO_DEBUG)
					if (escapePoint >= 0) {
						map.highlight(escapePoint, oceanic[escapePoint] ? Color.BLUE : Color.GREEN);
						// XXX I have seen blue (likely intermediate) escape points inside of a depression
						map.highlight(s,  Color.ORANGE);
					} else
						map.highlight(s, Color.RED);
			}
		} while (combined);
		
		// we have updated the in-Map downHill array
	}


	/**
	 * compute water flow, water depth, and soil hydration
	 *
	 *	Assertion: drainage has been called: oceanic & downHill are up-to-date
	 */
	public void waterFlow() {

		// import the rainfall and arterial river influx
		double[] rainMap = map.getRainMap();
		double[] incoming = map.getIncoming();
		double[] soilMap = map.getSoilMap();
		if (incoming == null && rainMap == null)
			return;
		
		// initialize our output maps
		double[] fluxMap = map.getFluxMap();
		double[] hydrationMap = map.getHydrationMap();
		for(int i = 0; i < mesh.vertices.length; i++) {
			fluxMap[i] = 0.0;
			hydrationMap[i] = 0.0;
		}
		
		// calculate the incoming water flux for each point
		map.max_slope = 0.0;
		map.max_flux = 0.0;
		map.max_velocity = 0.0;
		heightSort(0, byHeight.length - 1);
		for(int i = 0; i < byHeight.length; i++) {
			int x = byHeight[i];

			// oceanic points are under water w/no flow
			if (oceanic[x])	{
				hydrationMap[x] = heightMap[x] - parms.sea_level;
				continue;
			}

			// add incoming off-map rivers and rainfall to this point's flux
			fluxMap[x] += incoming[x] + (rain_to_flow * rainMap[x]);

			// compute the soil absorbtion and evaporative loss
			int soilType = erodeMap[x] < 0 ? Map.ALLUVIAL : (int) soilMap[x];
			double absorbed = saturation[soilType] * parms.Dp * area;
			double lost = absorbed * evaporation();
			
			// if loss exceeds incoming, net flux is zero
			if (fluxMap[x] * year <= lost) {
				hydrationMap[x] = fluxMap[x] * year / (parms.Dp * area); 
				fluxMap[x] = 0;
				continue;
			}
				
			// net incoming is reduced by evaporative loss
			hydrationMap[x] = saturation[soilType];
			fluxMap[x] -= lost / year;
			
			// figure out what happens to the excess water
			int d = downHill[x];
			if (d >= 0) {
				// it flows to my downHill neighbor
				fluxMap[d] += fluxMap[x];
			
				// if we are in a depression, it fills with water
				double myHeight = heightMap[x] - erodeMap[x];
				double hisHeight = heightMap[d] - erodeMap[d];
				if (myHeight <= hisHeight) {
					hydrationMap[x] = myHeight - hisHeight;
					velocityMap[x] = 0.0;
				} else {
					double s = slope(d,x);
					if (s > map.max_slope)
						map.max_slope = s;
					velocityMap[x] = velocity(s);
					if (velocityMap[x] > map.max_velocity)
						map.max_velocity = velocityMap[x];
				}
			} else
				velocityMap[x] = 0.0;
			
			if (fluxMap[x] > map.max_flux)
				map.max_flux = fluxMap[x];
		}
		// we already updated the in-place flux and hydration maps
	}

	/**
	 * (recursive) QuickSort a list of points by height
	 * @param left ... left most index of sort region
	 * @param right ... right most index of sort region
	 */
	private void heightSort(int left, int right) {
		// find the X coordinate of my middle element
	    int pivotIndex = left + (right - left) / 2;
	    double pivotValue = height(byHeight[pivotIndex]);

	    // for every point in my range
	    int i = left, j = right;
	    while(i <= j) {
	    	// find the first thing on left that belongs on right
	        while(height(byHeight[i]) >  pivotValue)
	            i++;
	        // find first thing on right that belongs on left
	        while(height(byHeight[j]) < pivotValue)
	            j--;

	        if(i <= j) { 
	        	if (i < j) {	// swap them
	        		int tmp = byHeight[i];
	        		byHeight[i] = byHeight[j];
	        		byHeight[j] = tmp;
	        	}
	            i++;
	            j--;
	        }
	    }

	    if(left < j)	// recursively sort to the left
	        heightSort(left, j);
	    if(right > i)	// recursively sort to the right
	        heightSort(i, right);
	}

	/*
	 * return the effective (map) height of a Meshpoint
	 *
	 *	Sink discovery and flux accumulation must be done
	 *  lowest-to-highest and highest-to-lowest.  There are
	 *  two tricks to determining the height of a point:
	 *	  1. (for display purposes) we keep erosion/deposition
	 *		 separate from base terrain height, so these two
	 *		 values msut be added.
	 *	  2. for flux purposes all points within a depression
	 *		 are considered to be up-hill from their escape point.
	 */
	private double height(int point) {
		final double EPSILON = .0000001;
		double myHeight = heightMap[point] - erodeMap[point];

		// has my flow been redirected to an escape point
		int d = downHill[point];
		if (d >= 0) {
			double hisHeight = heightMap[d] - erodeMap[d];
			if (hisHeight >= myHeight)
				return hisHeight + EPSILON;
		}

		return myHeight;
	}

	/*
	
			// calculate the down-hill flow and erosion
			double v = 0;
			if (downHill[x] >= 0) {
				// figure out slope and velocity
				double s = slope(downHill[x], x);
				if (s > map.max_slope)
					map.max_slope = s;
				v = velocity(s);
				if (v > map.max_velocity)
					map.max_velocity = v;
				
				// are we eroding or depositing
				double e = erosion(v);
				if (e > 0) {
					double taken = e * fluxMap[x] * zScale / competence[soilType];
					erodeMap[x] += taken;
					if (heightMap[x] - erodeMap[x] < -Parameters.z_extent/2)
						erodeMap[x] = heightMap[x] + Parameters.z_extent/2;
					if (erodeMap[x] > map.max_erosion)
						map.max_erosion = erodeMap[x];
					fluxMap[downHill[x]] += fluxMap[x];
					load[downHill[x]] = load[x] + taken;
				} else {
					double d = sedimentation(v);
					if (d > 0) {
						// TODO flood plains spread to all neighbors
						double given = d * load[x] * zScale;
						erodeMap[x] -= given;
						if (erodeMap[x] < - map.max_deposition)
							map.max_deposition = - erodeMap[x];
						fluxMap[downHill[x]] += fluxMap[x];
						load[downHill[x]] = load[x] - given;
					}
				}
			}
		}
	*/
	
	/**
	 * compute the slope between two MeshPoints
	 * @param here	point from which flow originates
	 * @param there point towards which water flows
	 * @return slope between two MeshPoints
	 */
	private double slope(int here, int there) {
		double dx = (parms.km(mesh.vertices[there].x) - parms.km(mesh.vertices[here].x)) * 1000;
		double dy = (parms.km(mesh.vertices[there].y) - parms.km(mesh.vertices[here].y)) * 1000;
		double dz = parms.altitude(heightMap[there]) - parms.altitude(heightMap[here]);
		return dz / Math.sqrt(dx*dx + dy*dy);
	}
	
	/**
	 * estimated water flow velocity
	 * @param slope ... dZ/dX
	 * @return flow speed (meters/second)
	 * 
	 * NOTE: velocity ranges from .1m/s to 3m/s
	 */
	public static double velocity(double slope) {
		double v = 3 * slope;
		double vMin = Parameters.getInstance().vMin;
		return (v < vMin) ? vMin : v;
	}
	
	/**
	 * estimated river width and depth
	 * 
	 * @param flow ... rate (cubic meters/second)
	 * @param velocity ... flow speed (meters/second)
	 * 
	 * NOTE: W/D ranges from 2-20 ... call it 6/V
	 * 	1. Area = Flow/Velocity
	 * 	2. Area = W x D
	 * 	3. R = W / D (~6/V)
	 * 
	 *  from (3):
	 *  	(3a) W=RD or (3b) D=W/R
	 *  combining (3a) with (2)
	 *  	W = RD = RA/W = sqrt(AR)
	 *  combining (3b) with (2)
	 *  	D = W/R = A/DR = sqrt(A/R) 
	 */
	private static double widthToDepth(double velocity) {
		final double MAX_RATIO = 20;	// max river W/D
		double ratio = 6/velocity;
		return (ratio > MAX_RATIO) ? MAX_RATIO : ratio;
	}
	
	/**
	 * estimated river width
	 * @param flow speed (cubic meters/second)
	 * @param velocity ... flow speed (meters/second)
	 * @return estimated width (meters)
	 */
	public static double width(double flow, double velocity) {
		double area = flow / velocity;
		double ratio = widthToDepth(velocity);
		return Math.sqrt(area * ratio);
	}

	/**
	 * estimated river depth
	 * @param flow speed (cubic meters/second)
	 * @param velocity ... flow speed (meters/second)
	 * @return estimated depth (meters)
	 */
	public static double depth(double flow, double velocity) {
		double area = flow / velocity;
		double ratio = widthToDepth(velocity);
		return Math.sqrt(area / ratio);
	}
	
	/**
	 * estimated erosion and deposition
	 * 
	 * NOTE:
	 * 	The Hjulstrom curve says that 
	 * 		erosion starts at 1m/s and is major by 2m/s
	 * 			slopes between .6 and 1.2
	 * 		sedimentation starts at .1m/s and is done by .005m/s
	 * 			slopes between .03 and .0015
	 */
	// returns M3 soil that will be eroded per M3 of flow
	private double erosion( double v ) {
		double Ve = parms.Ve;
		return (v < Ve) ? 0 : parms.Ce * (v * v)/(Ve * Ve);
	}
	
	// returns fraction of carried load that will settle out
	private double sedimentation( double v) {
		if (v > parms.Vd)
			return 0;
		return parms.Cd/v;
	}
	
	/*
	 * returns fraction of soil-water lost over a year
	 * 
	 * 	Note: I read a few articles and, finding them quite complex
	 * 		  made up a formula that I fit to a few data
	 * 		  points based on two plausible assumptions:
	 * 			1. there is a half-time, during which 1/2 of the water evaporates
	 * 			2. the half time decreases exponentially as the temperature rises
	 */
	private double evaporation() {
		double degC = parms.meanTemp();
		double half_time = parms.E35C * Math.pow(2, (35-degC)/parms.Edeg);
		return 1 - Math.pow(0.5, 365.25/half_time);
	}
	
	
}

