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
	private double surface[];	// Z depression padding
	private int byHeight[];		// MeshPoints sorted from high to low
	
	/** how much water can different types of soil hold (m^3/m^3) */
	public static double saturation[] = {
		0.30,	// max water content of sedimentary soil
		0.15,	// max water content of metamorphic soil
		0.10,	// max water content of igneous soil
		0.40	// max water content of alluvial soil
	};
	
	/** relative erosion resistances for various bed rocks */
	public static double competence[] = {
		1.0,	// sedimentary erosion resistance
		4.0,	// metamorphic erosion resistance
		2.5,	// igneous erosion resistance
		0.3		// alluvial erosion resistance
	};
	
	// drainage notations
	private static final int UNKNOWN = -666;
	private static final int OCEAN = -1;
	private static final int OFF_MAP = -2;
	
	private static final double EPSILON = .0000001;
	private static final double MAX_RATIO = 20;	// max river W/D
	
	private static final int HYDRO_DEBUG = 2;
	
	/**
	 * instantiate a flow/erosion/deposition engine
	 * @param map to be calculated
	 */
	public Hydrology(Map map) {
		this.map = map;
		this.parms = Parameters.getInstance();
	}
	
	/*
	 * return the CURRENT (map) height of a Meshpoint
	 *
	 *  Many algorithms depend on processing points from 
	 *  highest-to-lowest or vice versa.  Because of the
	 *  way we do hydrological processing, height is divided
	 *  into three components:
	 * 		heightMap[p] ... (fixed) height of the underlying rock
	 * 		erodeMap[p] ... (changing) erosion/deposition above that
	 * 		surface[p] ... brings under-water points up to H2O surface
	 * 					   (to make them up-hill from their exit points)
	 */
	private double height(int point) {
		double h = heightMap[point] + surface[point] - erodeMap[point];
		return h;
	}
	
	/**
	 * reCalculate downHill, flux, and erosion
	 * 		needed whenever Height, Rain or Erosion changes
	 * 
	 * @param reset ... new erosion map (vs incremental erosion)
	 */
	/* Calculation:
	 * 1. figure out what is down-hill from what
	 * 2. sort MeshPoints by descending height
	 * 3. identify all local depressions and sink points
	 * 4. identify escape point from each depression
	 * 5. route drainage to escape point, pad heights to escape height
	 *    re-sort MeshPoints by descending (padded) height
	 * 6. identify all points that are under the ocean
	 * 7. compute rain-fall and absorbtion for each point,
	 * 	  any excess water drains down-hill
	 * 8. use down-hill slope, velocity, flow to compute
	 * 	  erosion and sedimentation.
	 * 9. fill depressions w/excess water up to escape heights
	 * 10.spread shallow water and sedimentation over flood planes
	 */
	public void reCalculate(boolean reset) {
		// collect topographic data from the map
		mesh = map.getMesh();
		if (mesh == null || mesh.vertices.length == 0)
			return;
		heightMap = map.getHeightMap();
		if (heightMap == null)
			return;
		int[] downHill = map.getDownHill();
		double[] fluxMap = map.getFluxMap();
		double[] soilMap = map.getSoilMap();
		double[] hydrationMap = map.getHydrationMap();
		double[] sinkMap = new double[mesh.vertices.length];
		double[] incoming = map.getIncoming();
		surface = new double[mesh.vertices.length];
		erodeMap = map.getErodeMap();
		
		// initialize each array and find downhill neighbor
		for( int i = 0; i < downHill.length; i++ ) {
			downHill[i] = -1;
			fluxMap[i] = (incoming == null) ? 0 : incoming[i];
			hydrationMap[i] = 0;
			surface[i] = 0;
			sinkMap[i] = UNKNOWN;
			if (reset)
				erodeMap[i] = 0;
			
			double lowest_height = height(i);
			for( int n = 0; n < mesh.vertices[i].neighbors; n++) {
				int x = mesh.vertices[i].neighbor[n].index;
				double z = height(x);
				if (z < lowest_height) {
					downHill[i] = x;
					lowest_height = z;
				}
			}
		}
		
		// sort the MeshPoints by descending height
		byHeight = new int[heightMap.length];
		for( int i = 0; i < byHeight.length; i++ )
			byHeight[i] = i;
		heightSort(0, byHeight.length - 1);
		
		// identify sinks (scan lowest to highest)
		if (parms.debug_level >= HYDRO_DEBUG)
			map.highlight(-1, null);
		for(int i = byHeight.length - 1; i >= 0; i--) {
			int point = byHeight[i];
			
			// points we already know
			if (sinkMap[point] != UNKNOWN)
				continue;
			
			// edge points drain to OCEAN or OFF_MAP
			double msl = height(point) - parms.sea_level;
			if (mesh.vertices[point].neighbors < 3) {
				sinkMap[point] = (msl < 0) ? OCEAN : OFF_MAP;
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
		
		// find escape points and consolidate sinks into larger ones
		int escapePoint;
		double escapeHeight;
		boolean gotSome;
		do {gotSome = false;	// iterate until no more changes
			for(int s = 0; s < mesh.vertices.length; s++) {
				if (sinkMap[s] != s)	// find a local sink
					continue;
				if (downHill[s] >= 0)	// with no known escape point
					continue;
				
				// find the lowest bordering neighbor
				escapePoint = -1;
				escapeHeight = Parameters.z_extent;
				// enumerate every point in that sink
				for(int i = 0; i < byHeight.length; i++) {
					int point = byHeight[i];
					if (sinkMap[point] != s)
						continue;
					
					// find the lowest neighbor not in the sink
					for(int j = 0; j < mesh.vertices[point].neighbors; j++) {
						int n = mesh.vertices[point].neighbor[j].index;
						if (sinkMap[n] == s)
							continue;
						double z = heightMap[n] - erodeMap[n];
						if (z < escapeHeight) {
							escapePoint = n;
							escapeHeight = heightMap[n] - erodeMap[n];
						}
					}
				}
				
				// re-route all drainage from that depression to escape point
				if (escapePoint >= 0) {
					if (parms.debug_level >= HYDRO_DEBUG) {
						map.highlight(escapePoint, sinkMap[escapePoint] == OCEAN ? Color.BLUE : Color.GREEN);
						map.highlight(s,  Color.ORANGE);
					}
					
					// merge this depression into the escape point's depression
					for(int i = 0; i < mesh.vertices.length; i++) {
						// only consider points in selected depression
						if (sinkMap[i] != s)
							continue;
						
						// redirect flow from local sink to new escape point
						int dh = downHill[i];
						if (dh == -1 || heightMap[i] - erodeMap[i] <= escapeHeight) {
							downHill[i] = escapePoint;
							surface[i] = EPSILON + escapeHeight - (heightMap[i] - erodeMap[i]);
						} else
							surface[i] = 0;
						
						// reassign this point to the surrounding basin
						sinkMap[i] = sinkMap[escapePoint];
						
						gotSome = true;
					}
				} else if (parms.debug_level >= HYDRO_DEBUG)
					map.highlight(s, Color.RED);
			}
		} while (gotSome);
		
		// ocean is points below sea-level that drain below sea-level
		for(int i = 0; i < mesh.vertices.length; i++) {
			double z = heightMap[i] - erodeMap[i];
			if (z >= parms.sea_level)
				continue;
			double h = height(i);	// height at which it drains
			if (h >= parms.sea_level)
				continue;
			
			hydrationMap[i] = parms.altitude(z);
			surface[i] = 0;		// so we don't compute flux
		}
		
		// collect rain-fall and tributary information from the Map
		double[] rainMap = map.getRainMap();
		if (rainMap == null && incoming == null)
			return;

		// figure out the mapping from rainfall (cm/y) to water flow (m3/s)
		double area = 1000000 * (parms.xy_range * parms.xy_range) / byHeight.length;
		double year = 365.25 * 24 * 60 * 60;
		double rain_to_flow = .01 * area / year;
		
		// map units per meter
		double zScale = Parameters.z_extent / parms.z_range;

		map.max_flux = 0;
		map.max_velocity = 0;
		map.max_erosion = 0;
		map.max_deposition = 0;
		double load[] = new double[mesh.vertices.length];
		
		// re-sort (now that we have filled depressions)
		heightSort(0, byHeight.length - 1);
		for( int i = 0; i < byHeight.length; i++ ) {
			int x = byHeight[i];
			
			// flux in the ocean is meaningless
			if (hydrationMap[x] < 0 && surface[x] == 0) {
				fluxMap[x] = 0;
				continue;
			}
			
			// figure how much water comes into this cell
			double rain = (rainMap == null) ? 0 : rainMap[x] * rain_to_flow;
			fluxMap[x] += rain;
			
			// how much water can it hold, how much evaporates in a year
			int soilType = erodeMap[x] < 0 ? Map.ALLUVIAL : (int) soilMap[x];
			double maxH2O = saturation[soilType] * parms.Dp * area;
			double lost = maxH2O *= evaporation();
			
			
			// figure out if any water leaves this cell
			if (fluxMap[x] * year <= lost) {
				hydrationMap[x] = fluxMap[x] * year / (parms.Dp * area); 
				fluxMap[x] = 0;
				continue;
			}
				
			hydrationMap[x] = saturation[soilType];
			fluxMap[x] -= lost / year;
			
			// remember the largest flux I have seen
			if (fluxMap[x] > map.max_flux)
				map.max_flux = fluxMap[x];
			
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
		
		// fill the lakes in to their surface altitudes
		for(int i = 0; i < mesh.vertices.length; i++) {
			if (surface[i] == 0)	// not in a sink
				continue;
			
			// see if I (or my exit point) have excess flow
			double f = fluxMap[i];
			int d = downHill[i];
			if (d >= 0)
				f = fluxMap[d];
			if (f <= 0)
				continue;
				
			// this point should be under water
			hydrationMap[i] = -surface[i];
		}
		
		// TODO rivers don't flow around mountains (fill non-depressions)	
	}
	
	/**
	 * @return slope between two MeshPoints
	 */
	private double slope(int here, int there) {
		double dx = (parms.km(mesh.vertices[there].x) - parms.km(mesh.vertices[here].x)) * 1000;
		double dy = (parms.km(mesh.vertices[there].y) - parms.km(mesh.vertices[here].y)) * 1000;
		double dz = parms.altitude(heightMap[there]) - parms.altitude(heightMap[here]);
		return dz / Math.sqrt(dx*dx + dy*dy);
	}
	
	/**
	 * estimated flow velocity
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
	private double evaporation () {
		double degC = parms.meanTemp();
		double half_time = parms.E35C * Math.pow(2, (35-degC)/parms.Edeg);
		return 1 - Math.pow(0.5, 365.25/half_time);
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
}

