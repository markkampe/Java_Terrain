package worldBuilder;

public class Hydrology {
	
	private Map map;
	private Mesh mesh;
	private Parameters parms;
	
	// instance varialbles used by reCacluate & heightSort
	private double heightMap[];	// Z value of each MeshPoint
	private double erodeMap[];	// Z erosion of each MeshPoint
	private int byHeight[];		// MeshPoints sorted from high to low
		
	private static final int UNKNOWN = -666;	// no known downhill target
	
	public Hydrology(Map map) {
		this.map = map;
		this.parms = Parameters.getInstance();
	}


	
	/**
	 * re-calculate downHill, flux, and erosion
	 * 		needed whenever Height, Rain or Erosion changes
	 */
	public void reCalculate() {
		
		// collect topographic/hyrdological data from the map
		mesh = map.getMesh();
		if (mesh == null)
			return;
		heightMap = map.getHeightMap();
		erodeMap = map.getErodeMap();
		if (heightMap == null)
			return;
		
		double[] rainMap = map.getRainMap();
		double[] fluxMap = map.getFluxMap();
		if (rainMap == null)
			return;
		int[] downHill = map.getDownHill();
		
		// TODO interate over number of cycles
		//		perhaps moving this into a subroutine
		
		// initialize each array and find downhill neighbor
		for( int i = 0; i < downHill.length; i++ ) {
			downHill[i] = -1;
			fluxMap[i] = 0;
			erodeMap[i] = 0;
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
		
		// sort the MeshPoints by descending height
		byHeight = new int[heightMap.length];
		for( int i = 0; i < byHeight.length; i++ )
			byHeight[i] = i;
		heightSort(0, byHeight.length - 1);
		
		// figure out the mapping from rainfall to water flow
		double area = (parms.xy_range * parms.xy_range) / byHeight.length;
		double year = 365.25 * 24 * 60 * 60;
		double rain_to_flow = .01 * area * 1000000 / year;
		
		// map units per meter
		double zScale = Parameters.z_extent / parms.z_range;
		
		// processing points form highest to lowest,
		//	send rainfall down the downhill neighbor chain
		map.max_flux = 0;
		map.max_velocity = 0;
		map.max_erosion = 0;
		map.max_deposition = 0;
		double load[] = new double[mesh.vertices.length];
		for( int i = 0; i < byHeight.length; i++ ) {
			int x = byHeight[i];
			
			// FIX: need better criterion for being ocean
			//		?below sea level AND drains to edge?
			if (heightMap[x] - erodeMap[x] < parms.sea_level) {
				fluxMap[x] = 0;
				continue;
			}
			
			double flux = rainMap[x] * rain_to_flow;
			// TODO calculate soil water absorbtion/hydration
			//	penetration depth = min(depth[soiltype], rain/penetration)
			//	double saturation = saturation[soiltype] * depth
			fluxMap[x] += flux;
			//	double absorbed = min(saturation, fluxMap[x])
			//	saturation[x] = absorbed / depth[soiltype]
			//  fluxMap[x] -= absorbed;
			if (fluxMap[x] > map.max_flux)
				map.max_flux = fluxMap[x];
			
			// calculate the down-hill flow and erosion
			double v = 0;
			if (downHill[x] >= 0) {
				fluxMap[downHill[x]] += fluxMap[x];
				double s = slope(downHill[x], x);
				if (s > map.max_slope)
					map.max_slope = s;
				v = velocity(s);
				if (v > map.max_velocity)
					map.max_velocity = v;
				double e = erosion(v);
				if (e > 0) {
					double taken = e * fluxMap[x] * zScale;
					// FIX: make sure taken isn't too large
					erodeMap[x] += taken;
					if (erodeMap[x] > map.max_erosion)
						map.max_erosion = erodeMap[x];
					load[downHill[x]] = load[x] + taken;
				} else {
					double d = sedimentation(v);
					if (d > 0) {
						double given = d * load[x] * zScale;
						// FIX: make sure given isn't too large
						erodeMap[x] -= given;
						if (erodeMap[x] < - map.max_deposition)
							map.max_deposition = - erodeMap[x];
						load[downHill[x]] = load[x] - given;
					}
				}
			}
		}
		
		// TODO flood planes
		// do
		//		from highest to lowest
		//		if erode >= 0
		//			continue
		//		for each neighbor
		//			if higher than me - epsilon
		//				continue
		//			give them half of delta-epsilon
		// while did something
		
		// TODO sink computation
		// edges have a downhill of OFFMAP ... based on edge, not neighbors
		// no downhill is NONESUCH (or -(1+ESCAPE POINT)
		// have a count of tributaries as well as ultimate down hill
		// for each sink with tributaries, find lowest neighbor w/different sink
		//	that is the escape point for this sink
		// follow the down-hill pointers to identify the sinks
//		for(int i = 0; i < downHill.length; i++) {
//			int point = i;
//			while(true) {
//				// if we already know how this ends, we can stop
//				if (sinks[point] != UNKNOWN) {
//					sinks[i] = sinks[point];
//					break;
//				}
//				// if we make it to sea level, there is no sink
//				if (heightMap[i] - erodeMap[i] < parms.sea_level) {
//					sinks[i] = -1;
//					break;
//				}
//				// if we make it to the edge, there is no sink
//				if (mesh.vertices[point].neighbors < 3) {
//					sinks[i] = -1;
//					break;
//				}
//				// if we reach a low point, we are done
//				int x = downHill[point];
//				if (x == -1) {
//					sinks[i] = point;
//					break;
//				}
//				// continue following the down-hill pointers
//				point = x;
//			}	
//		}
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
	 * 		 max sustainable slope is 1/1
	 */
	private static double velocity(double slope) {
		return 3 * slope;
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
	 * 	3. W / D = 6/V
	 * 
	 *  from (3):
	 *  	(3a) W=6D/V or (3b) D=WV/6
	 *  substituting (3a) into (2)
	 *  	A = 6D/V * D = 6D^2/V; D = sqrt(AV/6)
	 *  substituting (3b) into (2)
	 *  	A = W * WV/6 = W^2V/6; W = sqrt(6A/V)
	 */
	private static double width(double flow, double velocity) {
		double area = flow / velocity;
		return Math.sqrt(6 * area / velocity);
	}

	private static double depth(double flow, double velocity) {
		double area = flow / velocity;
		return Math.sqrt(area * velocity / 6);
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
	// returns M3 soil per M3/s of flow
	public double erosion( double v ) {
		double Ve = parms.Ve;
		return (v < Ve) ? 0 : parms.Ce * (v * v)/(Ve * Ve);
	}
	
	// returns fraction of carried load
	public double sedimentation( double v) {
		return (v > parms.Vd) ? 0 : parms.Cd/v;
	}
	
	/**
	 * (recursive) QuickSort a list of points by height
	 * @param left ... left most index of sort region
	 * @param right ... right most index of sort region
	 */
	private void heightSort(int left, int right) {
		// find the X coordinate of my middle element
	    int pivotIndex = left + (right - left) / 2;
	    double pivotValue = heightMap[byHeight[pivotIndex]];

	    // for every point in my range
	    int i = left, j = right;
	    while(i <= j) {
	    	// find the first thing on left that belongs on right
	        while(heightMap[byHeight[i]] - erodeMap[byHeight[i]] >  pivotValue)
	            i++;
	        // find first thing on right that belongs on left
	        while(heightMap[byHeight[j]] - erodeMap[byHeight[i]]< pivotValue)
	            j--;

	        // swap them
	        if(i <= j) {
	        	if (i < j) {
	        		int tmp = byHeight[i];
	        		byHeight[i] = byHeight[j];
	        		byHeight[j] = tmp;
	        	}
	            i++;
	            j--;
	        }
	    }

	    // recursively sort everything to my left
	    if(left < j)
	        heightSort(left, j);
	    // recursively sort everything to my right
	    if(right > i)
	        heightSort(i, right);
	}

}

