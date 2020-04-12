package worldBuilder;

import java.awt.Color;
import java.io.FileWriter;	// debug log
import java.io.IOException;	// writes to debug log

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
	private double fluxMap[];	// water flow through MeshPoint
	private double hydrationMap[];	// hydration or water depth of MeshPoint
	private double removal[];	// M^3 of removed soil
	private double velocityMap[]; // water velocity at MeshPoint
	private double slopeMap[];	// slope downhill from MeshPoint
	public double suspended[];	// M^3 of suspended sediment per second
	private double outlet[];	// height at which detained water escapes
	private int downHill[];		// down-hill neighbor of each MeshPoint
	private int references[];	// number of nodes for which we are downHill
	private int byHeight[];		// MeshPoint indices, sorted by height
	private int byFlow[];		// MeshPoint indices, sorted by water flow
	private boolean oceanic[];	// which points are under the sea
	
	private int landPoints;		// number of non-oceanic points
	
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
	private static final double YEAR = 365.25 * 24 * 60 *  60;
	private double area;			// square meters per MeshPoint
	private double rain_to_flow;	// mapping from rain(cm) to flow(M^3/s)
	
	private static final String DEBUG_FILE = null; // "/tmp/WB_erosion.log";
	private FileWriter debug_log;
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
		removal = new double[mesh.vertices.length];
		suspended = new double[mesh.vertices.length];
		slopeMap = new double[mesh.vertices.length];
		velocityMap = new double[mesh.vertices.length];
		byHeight = new int[mesh.vertices.length];
		byFlow = new int[mesh.vertices.length];
		references = new int[mesh.vertices.length];
		outlet = new double[mesh.vertices.length];
		
		// many square meters per (average) mesh point
		area = 1000000 * (parms.xy_range * parms.xy_range) / mesh.vertices.length;
		// how many M^3/s is 1cm of annual rainfall
		rain_to_flow = .01 * area / YEAR;
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
			references[i] = 0;		// no up-hill neighbors
			sinkMap[i] = UNKNOWN;	// no known sink points
			oceanic[i] = false;		// no known ocean points
			outlet[i] = UNKNOWN;	// no points subject to flooding
			slopeMap[i] = 0.0;		// don't yet know topology
		}

		// turn off any previous sink-point debugs
		if (parms.debug_level >= HYDRO_DEBUG)
			map.highlight(-1, null);

		/*
		 * 1. determine which points are under the ocean
		 *	  a) any sub-sea-level point on the edge is oceanic
		 *    b) any sub-sea-level neighbor of an oceanic point
		 */
		landPoints = 0;
		for(int i = 0; i < mesh.vertices.length; i++) {
			if (oceanic[i])		// already known to be oceanic
				continue;

			if (heightMap[i] < parms.sea_level && mesh.vertices[i].neighbors < 3)
				mark_as_oceanic(i);		// recurses for all sub-sea-level neighbors
			else
				byHeight[landPoints++] = i;	// accumulate list of non-oceanic points
		}

		/*
		 * 2. determine the down-hill neighbor of all non-oceanic points
		 */
		map.min_slope = 666.0;
		map.max_slope = -666.0;
		map.max_height = -666.0;
		map.min_height = 666.0;
		for(int i = 0; i < mesh.vertices.length; i++) {
			if (oceanic[i])			// sub-oceanic points don't count
				continue;

			// note the highest and lowest points on the map
			double best = heightMap[i] - erodeMap[i];
			if (best > map.max_height)
				map.max_height = best;
			else if (best < map.min_height)
				map.min_height = best;
			
			// downhill from an edge point is always off map
			if (mesh.vertices[i].neighbors < 3) {
				downHill[i] = OFF_MAP;
				continue;
			}
			
			// find the lowest neighbor who is lower than me
			for( int n = 0; n < mesh.vertices[i].neighbors; n++) {
				int x = mesh.vertices[i].neighbor[n].index;
				double z = heightMap[x] - erodeMap[x];
				if (z < best) {
					downHill[i] = x;
					best = z;
				}
			}
			
			// record downHill reference and slope
			int d = downHill[i];
			if (d >= 0) {
				references[d] += 1;
				double s = slope(d, i);
				if (s < 0)
					s = -s;
				slopeMap[i] = s;
				if (s > 0 && s < map.min_slope)
					map.min_slope = s;
				if (s > map.max_slope)
					map.max_slope = s;
			}
		}
		
		/*
		 * 3. find sink-point for each non-oceanic point
		 *	  (scan lowest-to-highest because sink points are transitive)
		 */
		heightSort(0, landPoints - 1);
		for(int i = landPoints - 1; i >= 0; i--) {
			int point = byHeight[i];
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
					sinkMap[point] = oceanic[d] ? OCEAN : sinkMap[d];
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
		boolean combined = false;
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
				
				// coalesce this sink into the escape point's sink
				if (escapePoint >= 0) {
					// 1. route downhill flow from sink bottom to escape point
					downHill[s] = escapePoint;
					references[escapePoint] += 1;
					
					// 2. move all points in this sink to escape point's sink
					for(int i = 0; i < mesh.vertices.length; i++) {
						if (sinkMap[i] == s) {
							sinkMap[i] = sinkMap[escapePoint];
							if (heightMap[i] - erodeMap[i] <= escapeHeight)
								outlet[i] = escapeHeight;
						}
					}
					
					// 3. consider escape point part of the lake
					outlet[escapePoint] = escapeHeight;
					
					// this pass was successful, so try another
					combined = true;
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
						map.highlight(escapePoint, sinkMap[escapePoint] == OCEAN ? Color.BLUE : Color.GREEN);
						// XXX I have seen blue (likely intermediate) escape points inside of a depression
						map.highlight(s,  Color.ORANGE);
					} else
						map.highlight(s, Color.RED);
			}
		} while (combined);
		
		/*
		 * 5. create the byFlow map, which is sorted in water flow order
		 */
		check_refcounts();		// debug audit
		sortByFlow();
		
		// we have updated in-Map downHill so it need not be reset
	}

	/**
	 * create a list of points ordered by water flow
	 */
	/* 	  waterFlow computations must visit points in order of when
	 *    water passes through them.  Because of lakes (which back-up
	 *    to an escape point) we may have to process lower (tributary)
	 *    points before we can process higher escape points.  Thus
	 *    we cannot do that processing from an altitude-sorted list.
	 *    Rather, we must sort based on the partial-ordering of the
	 *    downHill pointers (which constitute a Directed Acyclic Graph),
	 *    which we do with Kahn's Topological sort.
	 */
	private void sortByFlow() {
		int	has_parents = 0;// first node with un-processed parents
		int next_slot = 0;	// next free slot in byFlow list
		boolean inList[] = new boolean[map.mesh.vertices.length];
		
		// while there are still nodes that have un-processed parents
		while(has_parents < landPoints) {
			// go back to the first point about which we were unsure
			int point = byHeight[has_parents];
			if (inList[point]) {	// we already know about this one
				has_parents += 1;
				continue;
			}
			
			// if this node now has no parents, it can be added to list
			if (references[point] == 0) {
				byFlow[next_slot++] = point;
				inList[point] = true;
				if (downHill[point] >= 0)
					references[downHill[point]] -= 1;
				has_parents += 1;
				continue;
			}
			
			// continue processing, and then come back
			boolean found_some = false;
			for(int current = has_parents + 1; current < landPoints; current++) {
				point = byHeight[current];
				if (!inList[point] && references[point] == 0) {
					byFlow[next_slot++] = point;
					inList[point] = true;
					if (downHill[point] >= 0)
						references[downHill[point]] -= 1;
					found_some = true;
				}
			}
			
			// if nothing was found, there is most likely a cycle in downHill
			if (!found_some) {
				System.err.println("BUG: sortByFlow stopped after " +
									next_slot + "/" + landPoints + " placements!");
				for(int i = 0; i < landPoints; i++) {
					point = byHeight[i];
					if (inList[point] && references[point] == 0)
						continue;
					System.err.println(String.format("    x=%d, height=%.6f, downhill=%d, sink=%d, refs=%d %s",
										point, heightMap[point] - erodeMap[point],
										downHill[point], sinkMap[point], references[point],
										(i == has_parents ? "*" : "")));
					byFlow[next_slot++] = point;	// KLUGE to get past problem
				}
				break;
			}
		}
	}
	

	/**
	 * compute water flow, water depth, and soil hydration
	 *
	 *	Assertion: drainage has been called: oceanic & downHill are up-to-date
	 */
	public void waterFlow() {

		// see if we are producing an erosion log
		try {
			debug_log = (DEBUG_FILE == null) ? null : new FileWriter(DEBUG_FILE);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// import the rainfall and arterial river influx
		double[] rainMap = map.getRainMap();
		double[] incoming = map.getIncoming();
		double[] soilMap = map.getSoilMap();
		if (incoming == null && rainMap == null)
			return;
		
		// initialize our output maps
		fluxMap = map.getFluxMap();
		hydrationMap = map.getHydrationMap();
		for(int i = 0; i < mesh.vertices.length; i++) {
			fluxMap[i] = 0.0;
			removal[i] = 0.0;
			suspended[i] = 0.0;
			velocityMap[i] = 0.0;
			hydrationMap[i] = oceanic[i] ? heightMap[i] - parms.sea_level : 0.0;
		}
		
		// pick up the erosion parameters
		double Ve = parms.Ve;	// minimum velocity for erosion
		double Vd = parms.Vd;	// maximum velocity for sedimentation
		double Vmin = parms.Vmin;	// minimum velocity to carry water
		double Smax = parms.Smax	;	// maximum sediment per M^3 of water
		
		// calculate the incoming water flux for each non-oceanic point
		map.min_flux = 666.0;
		map.max_flux = -666.0;
		map.min_velocity = 666.0;
		map.max_velocity = -666.0;
		for(int i = 0; i < landPoints; i++) {
			int x = byFlow[i];

			// add incoming off-map rivers and rainfall to this point's flux
			fluxMap[x] += incoming[x] + (rain_to_flow * rainMap[x]);

			// compute the soil absorbtion and evaporative loss
			int soilType = erodeMap[x] < 0 ? Map.ALLUVIAL : (int) soilMap[x];
			double absorbed = saturation[soilType] * parms.Dp * area;
			double lost = absorbed * evaporation();
			
			// if loss exceeds incoming, net flux is zero
			if (fluxMap[x] * YEAR <= lost) {
				hydrationMap[x] = fluxMap[x] * YEAR / (parms.Dp * area); 
				fluxMap[x] = 0;
				continue;
			}
				
			// net incoming is reduced by evaporative loss
			hydrationMap[x] = saturation[soilType];
			fluxMap[x] -= lost / YEAR;
			
			
			// figure out what happens to the excess water
			int d = downHill[x];
			if (d >= 0) {
				String msg = null;	// debug message string
				
				// it flows to my downHill neighbor
				fluxMap[d] += fluxMap[x];
				
				// if we are below our escape point, we are under water
				if (outlet[x] != UNKNOWN) {
					// all of our suspended soil flows to our downhill
					suspended[d] += suspended[x];
					
					// record this point as being under water
					if (heightMap[x] - erodeMap[x] < outlet[x])
						hydrationMap[x] = (heightMap[x] - erodeMap[x]) - outlet[x];
					else
						hydrationMap[x] = -parms.z(0.01);
					
					if (debug_log != null) {
						msg = String.format("x=%4d,%4.1fM u/w, f=%6.3f", x,
											parms.height(-hydrationMap[x]), fluxMap[x]);
						msg += String.format(", e/d=NONE, susp[%4d]=%6.2f", d, suspended[d]);
					}
				} else {
					// calculate the down-hill water velocity
					double v = velocity(slopeMap[x]);
					if (velocityMap[d] < v)
						velocityMap[d] = v;
					
					// take average of my incoming and outgoing
					v = (velocityMap[x] + velocityMap[d]) / 2;
					if (v > map.max_velocity)
						map.max_velocity = v;
					if (v >= Vmin && v < map.min_velocity)
						map.min_velocity = v;
					
					// we might be constructing a debug log entry
					msg = (debug_log == null) ? null :
						String.format("x=%4d, v=%6.3f, f=%6.3f", x, v, fluxMap[x]);
					
					if (v >= Ve) {	
						// figure out how much soil this water can take/hold
						double taken = 0.0;
						double can_hold = Smax * fluxMap[x];
						if (suspended[x] < can_hold) {
							double can_take = erosion((int) soilMap[x], v) * fluxMap[x];
							taken = Math.min(can_take, can_hold - suspended[x]);
						}
						// take it from here and add it to my down-hill
						removal[x] += taken;
						suspended[d] += suspended[x] + taken;
						
						// see if this is the worst erosion on the map
						double delta_z = parms.z(erosion(x));
						if (erodeMap[x] + delta_z > map.max_erosion)
							map.max_erosion = erodeMap[x] + delta_z;
						
						if (debug_log != null) {
							msg += String.format(", e=%6.4f, susp[%4d]=%6.4f", taken, d, suspended[d]);
							if (!oceanic[d])
								msg += String.format(", vin=%6.3f, vout=%6.3f", velocityMap[x], velocityMap[d]);
							else	
								msg += " (flows into the ocean)";
						}
					} else if (suspended[x] > 0 && v < Vd) {
						double dropped = precipitation(v) * suspended[x];
						removal[x] -= dropped;
						suspended[d] += suspended[x] - dropped;
						
						// see if this is the deepest sedimentation on the map
						double delta_z = parms.z(sedimentation(x));
						if (erodeMap[x] - delta_z < -map.max_deposition)
							map.max_deposition = -(erodeMap[x] - delta_z);
						
						if (debug_log != null)
							msg += String.format(", d=%6.4f, susp[%4d]=%6.4f", dropped, d, suspended[d]);
					} else if (suspended[x] > 0) {
						// forward all suspended material down hill (can result in sediment over-saturation
						suspended[d] += suspended[x];
						if (debug_log != null) {
							msg += String.format(", e/d=NONE, susp[%4d]=%6.2f", d, suspended[d]);
							msg += String.format(", vin=%6.3f, vout=%6.3f", velocityMap[x], velocityMap[d]);
						}
					} else
						msg = null;	// nothing happens at this point
				}
				// debug logging
				if (debug_log != null && msg != null)
					try {
						debug_log.write(msg + "\n");
					} catch (IOException e) {
						e.printStackTrace();
					}
			}
			
			if (fluxMap[x] > map.max_flux)
				map.max_flux = fluxMap[x];
			if (fluxMap[x] >= parms.stream_flux/10 && fluxMap[x] < map.min_flux)
				map.min_flux = fluxMap[x];
		}
		
		if (debug_log != null) {
			try {
				debug_log.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			debug_log = null;
		}
		// we have already updated the in-place flux and hydration maps
		
		// if there was no water flow, fix the Map flow parameters
		if (map.max_flux == -666.0) {
			map.min_flux = 0;
			map.max_flux = 0;
			map.min_velocity = 0;
			map.max_velocity = 0;
		}
			
	}
	
	/**
	 * estimated erosion
	 * @param index of point being checked
	 * @return meters of erosion
	 */
	public double erosion(int index) {
		if (removal[index] <= 0)
			return 0.0;
		return removal[index] * YEAR / area;
	}
	
	/**
	 * estimated sediment deposition
	 * @param index of the point being checked
	 * @return meters of deposited sediment
	 */
	public double sedimentation(int index) {
		if (removal[index] >= 0)
			return 0.0;
		return -removal[index] * YEAR / area;
	}
	
	/**
	 * flood all points in a sink up to a specified depth
	 * @param index of the sink node
	 * @param water_level (z units)
	 */
	private void flood_sink(int index, double water_level) {
		for(int i = 0; i < landPoints; i++) {
			int point = byHeight[i];
			if (sinkMap[point] == index) {
				double cur_z = heightMap[point] - erodeMap[point];
				if (cur_z < water_level)
					hydrationMap[point] = cur_z - water_level;
			}
		}
	}

	/**
	 * (recursive) QuickSort a list of points by height
	 * @param left ... left most index of sort region
	 * @param right ... right most index of sort region
	 */
	private void heightSort(int left, int right) {
		// find the X coordinate of my middle element
	    int pivotIndex = left + (right - left) / 2;
	    int pivotPoint = byHeight[pivotIndex];
	    double pivotValue = heightMap[pivotPoint] - erodeMap[pivotPoint];

	    // for every point in my range
	    int i = left, j = right;
	    while(i <= j) {
	    	// find the first thing on left that belongs on right
	        while(heightMap[byHeight[i]] - erodeMap[byHeight[i]] >  pivotValue)
	            i++;
	        // find first thing on right that belongs on left
	        while(heightMap[byHeight[j]] - erodeMap[byHeight[j]] <  pivotValue)
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
	
	/**
	 * compute the slope between two MeshPoints
	 * @param here	point from which flow originates
	 * @param there point towards which water flows
	 * @return slope between two MeshPoints (likely negative)
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
	 */
	public double velocity(double slope) {
		/*
		 * I tried using the Manning formula for open channel flow
		 * 		V = Rh^.65 * S^.5 / n
		 * I complemented this with real river data:
		 * 		Lower Mississippi: speed 0.5M/s, slope  1cm/km
		 * 		Western Columbia:  speed 3.1M/s, slope 10cm/km
		 * But when I applied these to a couple of test maps, I found
		 * that Fantasy maps seem to be much steeper than real landscapes,
		 * so the water never slowed down enough for sedimentation.
		 * 
		 * Fantasy maps seem to have gradients from .001 to 1.0
		 * (100-1000 steeper than real world), so I decided to
		 * curve fit a reasonable range of river speeds (0.005 to 0.5)
		 * to that range of steeper gradients:
		 * 
		 * 		slope	 M/s
		 * 		-----	-----
		 * 		0.001	0.005	dead slow
		 * 		0.010	0.050	mountain stream
		 * 		0.100	0.500	river
		 * 		0.500	3.000	white water
		 */
		double v = slope * 5.0;
		if (v < parms.Vmin)
			return parms.Vmin;
		if (v > parms.Vmax)
			return parms.Vmax;
		return v;
	}
	
	/*
	 * Erosion/Deposition Modeling
	 * 
	 * The Hjulstrom curve says that different sized particles are 
	 * eroded and deposited differently, but ...
	 * 	- most erosion starts somewhere between 0.2 and 1.0 M/s
	 *  - most deposition happens between .005 and 1.0 M/s
	 *  
	 * Suspended concentration studies suggest that a liter of
	 * moving water can carry between 4 and 400 mg (2-200cc)
	 * of suspended soil.  As expected the total transported 
	 * soil is nearly linear with the flow rate, but the 
	 * suspended solids per liter did not seem to be any simple
	 * function of the total flow rate.  Hence, my decision
	 * to base concentration entirely on flow velocity.
	 * 
	 * My over-simplified erosion model:
	 * 	- erosion happens at velocities between Ve and Vmax
	 * 	      w/solids concentration rising linearly w/speed
	 * 
	 * My over-simplfied deposition model:
	 * 	- deposition happens at velocities between Vd and Vmin,
	 * 	      w/between 0 and 1/2 (a linear function of speed)
	 * 		  of the suspended solids settling out per MeshPoint.
	 */
	/**
	 * Compute the erosive power of fast moving water
	 * 
	 * @param mineral composition of river bed
	 * @param velocity of the water
	 * @return M^3 of eroded material per M^3 of water
	 */
	private double erosion(int mineral, double velocity) {
		
		if (velocity <= parms.Ve)
			return 0.0;			// slow water contains no suspended soil
		
		// simplest model is absorbtion proportional to speed
		double suspended = parms.Smax;
		if (velocity < parms.Vmax)
			suspended *= velocity/parms.Vmax;
		return parms.Ce * suspended;
	}
	
	/**
	 * compute the amount of sedimentation from slow water
	 * @param velocity of the water
	 * @return fraction of suspended load that will fall out
	 */
	private double precipitation(double velocity) {
		if (velocity >= parms.Vd)
			return 0.0;			// fast water drops nothing
		double precip = parms.Cd;
		if (velocity > parms.Vmin)
			precip *= (parms.Vd - velocity)/parms.Vd;
		return precip;
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
	public double width(double flow, double velocity) {
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
	public double depth(double flow, double velocity) {
		double area = flow / velocity;
		double ratio = widthToDepth(velocity);
		return Math.sqrt(area / ratio);
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
	
	/*
	 * debug routine to confirm correctness of reference counters
	 */
	private void check_refcounts() {
		for(int i = 0; i < landPoints; i++) {
			int point = byHeight[i];
			int expect = references[point];
			int found = 0;
			for(int j = 0; j < landPoints; j++)
				if (downHill[byHeight[j]] == point)
					found += 1;
			if (found != expect)
				System.err.println("5: x=" + point + ", expected " + expect + ", found " + found);
		}
	}
}

