package worldBuilder;

public class TerritoryEngine {
	private Parameters parms;
	private Map map;
	private double[] heights;
	private double[] riverFlux;

	/**
	 * 
	 */
	public TerritoryEngine(Map map) {
		this.map = map;
		this.heights = map.getHeightMap();
		this.riverFlux = map.getFluxMap();
		this.parms = Parameters.getInstance();
		
		String[] names = map.getNameMap();
		
		int n = map.mesh.vertices.length;
		Journey[] nodes = new Journey[n];
		Journey dummy = new Journey(-1,-1);
		Journey.NextSteps queue = dummy.new NextSteps();
		
		// identify all of the known cities
		for(int i = 0; i < n; i++) {
			if (names[i] == null)
				continue;
			if (names[i].startsWith("capitol") || names[i].startsWith("city")) {
				// a city defines a new territory
				nodes[i] = new Journey(i,-1);
				nodes[i].cost = 0;
				// enqueue its neighbors for outward journey
				for(int j = 0; j < map.mesh.vertices[i].neighbors; j++) {
					Journey step = new Journey(i,i);
					step.cost = cost(i, j);
					nodes[j] = step;
					queue.add(step);
				}
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
