package worldBuilder;

public class Hydrology {
	
	private Map map;
	private Parameters parms;
	private int downHill[];
	private int sinks[];
	
	public Hydrology(Map map) {
		this.map = map;
		this.parms = Parameters.getInstance();
	}

	// TODO sink computation
	// edges have a downhill of OFFMAP ... based on edge, not neighbors
	// no downhill is NONESUCH (or -(1+ESCAPE POINT)
	// have a count of tributaries as well as ultimate down hill
	// for each sink with tributaries, find lowest neighbor w/different sink
	//	that is the escape point for this sink
	/**
	 * return a map of what is down-hill from what
	 * 		side-effect ... update map.height, flux, erosion
	 * 	
	 * @param	there has been a parameter change
	 * 
	 * 		this must be recalculated if there has been a change
	 * 		in the mesh, heights, rainfall, or erosion.
	 */
	public int[] downhill(boolean recalculate) {
		
		if (recalculate)
			return downHill;
		
		// collect topographic/hyrdological data from the map
		Mesh mesh = map.getMesh();
		double[] heightMap = map.getHeightMap();
		double[] erodeMap = map.getErodeMap();
		
		final int UNKNOWN = -666;
		
		// find the down-hill neighbor of each point
		downHill = new int[mesh.vertices.length];
		sinks = new int[mesh.vertices.length];
		for( int i = 0; i < downHill.length; i++ ) {
			downHill[i] = -1;
			sinks[i] = UNKNOWN;
			double lowest_height = heightMap[i] - erodeMap[i];
			for( int n = 0; n < mesh.vertices[i].neighbors; n++) {
				int x = mesh.vertices[i].neighbor[n].index;
				double z = heightMap[x] = erodeMap[x];
				if (z < lowest_height) {
					downHill[i] = x;
					lowest_height = z;
				}
			}
		}
		
		// follow the down-hill pointers to identify the sinks
		for(int i = 0; i < downHill.length; i++) {
			int point = i;
			while(true) {
				// if we already know how this ends, we can stop
				if (sinks[point] != UNKNOWN) {
					sinks[i] = sinks[point];
					break;
				}
				// if we make it to sea level, there is no sink
				if (heightMap[i] - erodeMap[i] < parms.sea_level) {
					sinks[i] = -1;
					break;
				}
				// if we make it to the edge, there is no sink
				if (mesh.vertices[point].neighbors < 3) {
					sinks[i] = -1;
					break;
				}
				// if we reach a low point, we are done
				int x = downHill[point];
				if (x == -1) {
					sinks[i] = point;
					break;
				}
				// continue following the downhill pointers
				point = x;
			}	
		}
		return downHill;
	}
}
