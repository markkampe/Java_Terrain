package worldBuilder;

public class TerritoryEngine {
	// imported tables
	private Parameters parms;
	private Map map;
	private String[] names;
	private double[] heights;
	private double[] erosion;
	private double[] riverFlux;
	private double[] waterLevel;
	private boolean[] oceanic;
	private TradeRoutes routes;
	
	// local data
	Journey[] nodes;			// status of every MeshPoint
	Journey.NextSteps queue;	// queue of points to be explored
	
	private static final int TERRITORY_DEBUG = 2;

	/**
	 * MeshPoints are grouped into territories, based on what city
	 * they are closest too (not in km, but in travel days).  This
	 * Engine starts from all of the cities and travels out until it
	 * encounters the border (equal travel to either city).
	 * 
	 * This approach is taken from O'Leary, though w/different computations
	 */
	public TerritoryEngine(Map map) {
		this.map = map;
		this.heights = map.getHeightMap();
		this.erosion = map.getErodeMap();
		this.waterLevel = map.getWaterLevel();
		this.riverFlux = map.getFluxMap();
		this.names = map.getNameMap();
		this.oceanic = map.getDrainage().oceanic;
		this.parms = Parameters.getInstance();
		
		int n = map.mesh.vertices.length;
		nodes = new Journey[n];
		Journey dummy = new Journey(-1, -1, null);
		queue = dummy.new NextSteps();
		this.routes = new TradeRoutes(map);
		
		// identify all of the known cities
		for(int i = 0; i < n; i++) {
			if (names[i] == null)
				continue;
			if (names[i].startsWith("capitol:") || names[i].startsWith("city:")) {
				// a city defines a new territory
				nodes[i] = new Journey(i, i, null);
				nodes[i].cost = 0;
				if (parms.debug_level >= TERRITORY_DEBUG)
					System.out.println(String.format("%d: %s", i, names[i]));
				// enqueue its neighbors for outward journey
				for(int j = 0; j < map.mesh.vertices[i].neighbors; j++) {
					MeshPoint neighbor = map.mesh.vertices[i].neighbor[j];
					int x = neighbor.index;
					Journey step = new Journey(x, i, nodes[i]);
					step.cost = cost(i, x);
					nodes[x] = step;
					queue.add(step);
				}
			}
		}
		
		// enumerate all reachable nodes (within dTravelMax/2)
		outwards();
		map.repaint();
	}
	
	
	/**
	 * Journey outwards from each city until we have covered the map.
	 * When we reach a MeshPoint already covered from another city,
	 * record it as a possible trade route.
	 */
	private void outwards() {
		
		for( Journey step = queue.next(); step != null; step = queue.next()) {
			// recursively explore all of our neighbors
			MeshPoint p = map.mesh.vertices[step.index];
			if (parms.debug_level >= TERRITORY_DEBUG)
				System.out.println(String.format("%d->%d: territory=%s, cost=%f", step.route, p.index, names[step.city], step.cost));
			
			for(int i = 0; i < p.neighbors; i++) {
				MeshPoint neighbor = p.neighbor[i];
				// ignore edge nodes
				if (neighbor.neighbors < 3)
					continue;

				// already claimed nodes might be cross-overs
				int neighbor_x = neighbor.index;
				if (nodes[neighbor_x] != null) {
					if (nodes[neighbor_x].city != step.city)
						routes.addCrossing(step, nodes[neighbor_x]);
					continue;
				}
				
				// stop at the ocean
				if (oceanic[neighbor_x]) {
					Journey ocean = new Journey(neighbor_x, -1, null);
					routes.addCrossing(step, ocean);
					continue;
				}
				
				// no crossing lakes
				if (waterLevel[neighbor_x] > heights[neighbor_x] - erosion[neighbor_x])
					continue;
				
				// no farther than (half) the maximum allowable inter-city journey
				double thisCost = step.cost + cost(step.index, neighbor_x);
				if (thisCost > parms.dTravelMax/2)
					continue;
				
				// add this Journey node to the list to be explored
				Journey nextStep = new Journey(neighbor_x, step.city, step);
				nextStep.cost = thisCost;
				nodes[neighbor_x] = nextStep;
				queue.add(nextStep);
			}
		}
	}
	
	/**
	 * compute the cost of traveling from one point to a neighbor
	 * @param from origin point
	 * @param to destination point
	 * @return travel time (in days)
	 */
	public double cost(int from, int to) {
		// start with our horizontal travel time
		double dX = parms.km(map.mesh.vertices[from].distance(map.mesh.vertices[to]));
		double minutes = dX * parms.dTimeTravel;
		
		// going up-hill adds extra time
		double dZ = parms.height(heights[to] - heights[from]);
		if (dZ > 0)
			minutes += dZ * parms.dTimeClimb / 1000.0;
		
		// crossing rivers adds extra time
		double flux = riverFlux[from];
		if (flux > 0)
			minutes += Math.sqrt(flux) * parms.dTimeCross;
		
		return minutes / parms.dTravelDay;
	}
}
