package worldBuilder;

public class TerritoryEngine {
	// imported tables
	private Parameters parms;
	private Map map;
	private String[] names;
	private double[] heights;
	private double[] riverFlux;
	private boolean[] oceanic;
	private TradeRoutes routes;
	
	// local data
	Journey[] nodes;			// status of every point
	Journey.NextSteps queue;	// queue of points to be explored
	
	
	// caravan travel speed estimates
	private static final double WALK_TIME = 20.0;	// minutes/km
	private static final double	CLIMB_TIME = 150.0;	// minutes/km
	private static final double FORD_TIME = 60.0;	// minutes/M^3/s
	private static final double PER_DAY = 330;		// minutes/day of travel
	
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
		this.riverFlux = map.getFluxMap();
		this.names = map.getNameMap();
		this.oceanic = map.getDrainage().oceanic;
		this.routes = new TradeRoutes(map);
		
		this.parms = Parameters.getInstance();
		
		int n = map.mesh.vertices.length;
		nodes = new Journey[n];
		Journey dummy = new Journey(-1, -1,-1);
		queue = dummy.new NextSteps();
		
		// identify all of the known cities
		for(int i = 0; i < n; i++) {
			if (names[i] == null)
				continue;
			if (names[i].startsWith("capitol:") || names[i].startsWith("city:")) {
				// a city defines a new territory
				nodes[i] = new Journey(i, i, -1);
				nodes[i].cost = 0;
				if (parms.debug_level >= TERRITORY_DEBUG)
					System.out.println(String.format("%d: %s", i, names[i]));
				// enqueue its neighbors for outward journey
				for(int j = 0; j < map.mesh.vertices[i].neighbors; j++) {
					MeshPoint neighbor = map.mesh.vertices[i].neighbor[j];
					int x = neighbor.index;
					Journey step = new Journey(x, i, i);
					step.cost = cost(i, x);
					nodes[x] = step;
					queue.add(step);
				}
			}
		}
		
		// enumerate all reachable nodes
		process();
		
		// record the routes
		map.tradeRoutes(routes.routes);
		map.journeys(nodes);
	}
	
	/**
	 * journey outwards from each city until we have covered the map
	 */
	private void process() {
		for( Journey step = queue.next(); step != null; step = queue.next()) {
			// recursively explore all of our neighbors
			MeshPoint p = map.mesh.vertices[step.index];
			if (parms.debug_level >= TERRITORY_DEBUG)
				System.out.println(String.format("%d->%d: territory=%s, cost=%f", step.route, p.index, names[step.city], step.cost));
			for(int i = 0; i < p.neighbors; i++) {
				MeshPoint neighbor = p.neighbor[i];
				// ignore edge nodes
				if (neighbor.neighbors < 3) {
					if (parms.debug_level > TERRITORY_DEBUG)
						System.out.println ("Territory: ignoring edge node " + neighbor.index);
					continue;
				}
				// already claimed nodes might be cross-overs
				int neighbor_x = neighbor.index;
				if (nodes[neighbor_x] != null) {
					if (nodes[neighbor_x].city != step.city)
						routes.addCrossing(step, nodes[neighbor_x]);
					continue;
				} 
				// ignore oceanic nodes
				if (oceanic[neighbor_x]) {
					if (parms.debug_level > TERRITORY_DEBUG)
						System.out.println ("Territory: ignoring oceanic node " + neighbor.index);
					continue;
				}
				
				// FIX no crossing lakes
				
				// add this node to the list to be explored
				Journey nextStep = new Journey(neighbor_x, step.city, step.index);
				nextStep.cost = step.cost + cost(step.index, neighbor_x);
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
		double minutes = dX * WALK_TIME;
		
		// going up-hill adds extra time
		double dZ = parms.height(heights[to] - heights[from]);
		if (dZ > 0)
			minutes += dZ * CLIMB_TIME / 1000.0;
		
		// crossing rivers adds extra time
		double flux = riverFlux[from];
		if (flux > 0)
			minutes += Math.sqrt(flux) * FORD_TIME;
		
		return minutes / PER_DAY;
	}
}
