package worldBuilder;

public class TerritoryEngine {
	private Parameters parms;
	private Map map;
	private double[] heights;
	private double[] riverFlux;
	private boolean[] oceanic;
	
	Journey[] nodes;			// status of every point
	Journey.NextSteps queue;	// queue of points to be explored

	/**
	 * 
	 */
	public TerritoryEngine(Map map) {
		this.map = map;
		this.heights = map.getHeightMap();
		this.riverFlux = map.getFluxMap();
		this.oceanic = map.getDrainage().oceanic;
		
		this.parms = Parameters.getInstance();
		
		String[] names = map.getNameMap();
		
		int n = map.mesh.vertices.length;
		nodes = new Journey[n];
		Journey dummy = new Journey(-1, -1,-1);
		queue = dummy.new NextSteps();
		
		// identify all of the known cities
		for(int i = 0; i < n; i++) {
			if (names[i] == null)
				continue;
			if (names[i].startsWith("capitol") || names[i].startsWith("city")) {
				// a city defines a new territory
				nodes[i] = new Journey(i, i,-1);
				nodes[i].cost = 0;
				// enqueue its neighbors for outward journey
				for(int j = 0; j < map.mesh.vertices[i].neighbors; j++) {
					Journey step = new Journey(j, i,i);
					step.cost = cost(i, j);
					nodes[j] = step;
					queue.add(step);
				}
			}
		}
	}
	
	/**
	 * journey outwards from each city until we have covered the map
	 */
	private void process() {
		for( Journey step = queue.next(); step != null; step = queue.next()) {
			// recursively explore all of our neighbors
			MeshPoint p = map.mesh.vertices[step.index];
			for(int i = 0; i < p.neighbors; i++) {
				MeshPoint neighbor = p.neighbor[i];
				// ignore edge nodes
				if (neighbor.neighbors < 3)
					continue;
				// ignore already claimed nodes
				int neighbor_x = neighbor.index;
				if (nodes[neighbor_x] != null)
					continue;
				// ignore oceanic nodes
				if (oceanic[neighbor_x])
					continue;
				
				// add this node to the list to be explored
				Journey nextStep = new Journey(neighbor_x, step.city, step.index);
				nextStep.cost = step.cost + cost(step.index, neighbor_x);
				queue.add(nextStep);
			}
		}
	}
	
	/**
	 * compute the cost of traveling from one point to a neighbor
	 * @param from origin point
	 * @param to destination point
	 */
	public double cost(int from, int to) {
		double distance = map.mesh.vertices[from].distance(map.mesh.vertices[to]);
		double difficulty = 1.0;
		
		// going up-hill is expensive
		double dZdX = parms.height(heights[to] - heights[from]) / (1000 * parms.km(distance));
		if (dZdX > 0)
			difficulty += Math.sqrt(dZdX);
		
		// crossing rivers is expensive
		double flux = riverFlux[from];
		if (flux > 0)
			difficulty += Math.sqrt(flux);
		
		
		return difficulty * distance;
	}
}
