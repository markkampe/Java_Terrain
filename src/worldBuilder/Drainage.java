package worldBuilder;

import java.awt.Color;

/**
 * engine to use topology to compute directions of water flow
 */
public class Drainage {
	private Parameters parms;
	private Map map;
	private Mesh mesh;
	
	// maps that we import/export from/to Map
	private double heightMap[];		// Z value of each MeshPoint (from Map)
	private double erodeMap[];		// Z erosion of each MeshPoint (from Map)
	
	
	// per Meshpoint maps we create for use by WaterFlow
	public boolean oceanic[];		// which points are under the sea
	protected int downHill[];		// down-hill neighbor of each MeshPoint
	public double outlet[];			// height at which detained water can escape
	protected double slopeMap[];	// slope downhill from each MeshPoint
	protected int landPoints;		// number of non-oceanic points
	protected int byHeight[];		// land MeshPoint indices, sorted by height
	protected int byFlow[];			// land MeshPoint indices, sorted by water flow
	
	// maps we create for our own use
	private int sinkMap[];			// the point to which each MeshPoint drains
	private int references[];		// number of nodes for which we are downHill
	
	protected static final int UNKNOWN = -666;	// sinkMap: sink point not yet found
	protected static final int OCEAN = -1;		// sinkMap: drains to ocean
	protected static final int OFF_MAP = -2;	// sinkMap: drains off map
	
	private static final int TOO_BIG = 666;			// an impossibly large number
	private static final int TOO_SMALL = -666;		// an impossibly negative number
	
	private static final int HYDRO_DEBUG = 2;	// 3 enables painful tracing
	private static final String DEBUG_LOG_FILE = "/tmp/drainage_debug.log";
	private DebugLog debug_log;
	
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
	public Drainage(Map map) {
		// obtain copies of the needed resources
		parms = Parameters.getInstance();
		this.map = map;
		this.mesh = map.getMesh();
		this.heightMap = map.getHeightMap();
		this.erodeMap = map.getErodeMap();
		
		// make sure we have heights to work with
		if (mesh == null || mesh.vertices.length == 0 || heightMap == null)
			return;
		
		// create our own maps
		this.downHill = new int[mesh.vertices.length];
		this.oceanic = new boolean[mesh.vertices.length];
		this.sinkMap = new int[mesh.vertices.length];
		this.slopeMap = new double[mesh.vertices.length];
		this.outlet = new double[mesh.vertices.length];
		this.byHeight = new int[mesh.vertices.length];
		this.byFlow = new int[mesh.vertices.length];
		
		references = new int[mesh.vertices.length];
		
		// see if we are producing a debug log
		if (parms.debug_level > HYDRO_DEBUG)
			debug_log = new DebugLog("Drainage", DEBUG_LOG_FILE);
		else
			debug_log = null;
		
		// compute the down-hill neighbors
		recompute();
	}
	
	public void recompute() {
		// reinitialize the maps we are to create
		for(int i = 0; i < mesh.vertices.length; i++) {
			oceanic[i] = false;		// no known ocean points
			downHill[i] = UNKNOWN;	// no known down-hill neighbors
			slopeMap[i] = 0.0;		// no calculated down-hill slopes
			sinkMap[i] = UNKNOWN;	// no known sink points
			outlet[i] = UNKNOWN;	// no points subject to flooding
			references[i] = 0;		// no up-hill neighbors
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
		}
		for(int i = 0; i < mesh.vertices.length; i++)
			if (!oceanic[i])	// anything left is a land point
				byHeight[landPoints++] = i;

		/*
		 * 2. determine the down-hill neighbor of all non-oceanic points
		 */
		map.min_slope = TOO_BIG;
		map.max_slope = TOO_SMALL;
		map.max_height = TOO_SMALL;
		map.min_height = TOO_BIG;
		for(int i = 0; i < mesh.vertices.length; i++) {
			if (oceanic[i])			// sub-oceanic points don't count
				continue;

			// note the highest and lowest points on the map
			double best = heightMap[i] - erodeMap[i];
			if (best > map.max_height)
				map.max_height = best;
			else if (best < map.min_height)
				map.min_height = best;
			
			// find the lowest neighbor who is lower than me
			for( int n = 0; n < mesh.vertices[i].neighbors; n++) {
				int x = mesh.vertices[i].neighbor[n].index;
				double z = heightMap[x] - erodeMap[x];
				if (z < best) {
					downHill[i] = x;
					best = z;
				}
			}
			
			if (downHill[i] < 0) {	// no down-hill neighbors found
				if (mesh.vertices[i].neighbors < 3)
					downHill[i] = OFF_MAP;
				continue;
			}
			
			// record downHill reference and slope
			int d = downHill[i];
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
		int escapeTo, escapeThru;
		double escapeHeight;
		boolean combined = false;
		do { combined = false;	// iterate until no more changes
			// find a local sink with no known escape point
			for(int s = 0; s < mesh.vertices.length; s++) {
				if (oceanic[s] || sinkMap[s] != s || downHill[s] >= 0)
					continue;
				
				// search all points in this sink for lowest outside neighbor
				escapeTo = -1; escapeThru = -1;
				escapeHeight = Parameters.z_extent;
				for(int i = 0; i < byHeight.length; i++) {
					int point = byHeight[i];
					if (sinkMap[point] != s)	// point not in this sink
						continue;
					
					double z1 = heightMap[point] - erodeMap[point];
					for(int j = 0; j < mesh.vertices[point].neighbors; j++) {
						int n = mesh.vertices[point].neighbor[j].index;
						if (sinkMap[n] == s)	// neighbor still in this sink
							continue;
						double z2 = heightMap[n] - erodeMap[n];
						if (z2 >= z1 && z2 < escapeHeight) {
							// this point, up-hill from us, is in another sink
							escapeThru = n;
							escapeTo = n;
							escapeHeight = z2;
						} else if (z1 >= z2 && z1 < escapeHeight) {
							// this point, in our sink, can drain to another sink
							escapeThru = point;
							escapeTo = n;
							escapeHeight = z1;
						}
					}
				}
				
				// coalesce this sink into the escape point's sink
				if (escapeTo >= 0) {
					// users will never care, but I've spent much debug time here
					if (debug_log != null) {
						double sinkHeight = heightMap[s] - erodeMap[s];
						String msg = String.format("sink bottom %d at %.1fMSL escapes to %d", 
													s, parms.altitude(sinkHeight), escapeTo);
						msg += (sinkMap[escapeTo] == OCEAN) ? " (to the ocean)" : 
								String.format(" (in sink %d)", sinkMap[escapeTo]);
						if (escapeTo != escapeThru)
							msg += String.format(" thru saddle %d", escapeThru);
						msg += String.format(" at %.1f MSL", parms.altitude(escapeHeight));
						debug_log.write(msg + "\n");
					}
					
					// 1. route downhill flow from sink bottom to escape point
					if (escapeTo == escapeThru) {	// escape point in another sink
						downHill[s] = escapeTo;
						references[escapeTo] += 1;
					} else {	// escape point at edge of our sink
						downHill[s] = escapeThru;
						references[escapeThru] += 1;
						int prev = downHill[escapeThru];
						if (prev >= 0)
							references[prev] -= 1;
						downHill[escapeThru] = escapeTo;
						references[escapeTo] += 1;
					}
					
					// 2. move all points in this sink to escape point's sink
					for(int i = 0; i < mesh.vertices.length; i++) {
						if (sinkMap[i] == s) {
							sinkMap[i] = sinkMap[escapeTo];
							if (heightMap[i] - erodeMap[i] <= escapeHeight)
								outlet[i] = escapeHeight;
						}
					}
					
					// 3. consider escape point on edge part of the lake
					if (escapeTo == escapeThru) {
						outlet[escapeTo] = escapeHeight;
						// if it is an edge point, it drains off map
						if (mesh.vertices[escapeTo].neighbors < 3 && downHill[escapeTo] >= 0) {
							int prev = downHill[escapeTo];
							references[prev] -= 1;
							downHill[escapeTo] = OFF_MAP;
						}
					}
					
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
					if (escapeTo >= 0) {
						map.highlight(escapeTo, sinkMap[escapeTo] == OCEAN ? Color.BLUE : Color.GREEN);
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
		
		// if we have a debug log, close it
		if (debug_log != null)
			debug_log.close();
	}

	/**
	 * Create a list of land points sorted from source to sink
	 * 	  
	 * waterFlow computations must visit points in order of when
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
	
	/*
	 * debug routine to confirm correctness of reference counters
	 */
	private void check_refcounts() {
		int tot_expected = 0;
		int tot_found = 0;
		for(int i = 0; i < landPoints; i++) {
			int point = byHeight[i];
			int expect = references[point];
			int found = 0;
			for(int j = 0; j < landPoints; j++)
				if (downHill[byHeight[j]] == point)
					found += 1;
			if (found != expect)
				System.err.println("x=" + point + ", expected " + expect + ", found " + found);
			tot_expected += expect;
			tot_found += found;
		}
		
		if (parms.debug_level > 1)
			System.out.println("DownHill audit: " + landPoints + " land points" +
							", refcount=" + tot_expected + 
							", pointers=" + tot_found);
	}
}

