package worldBuilder;

import java.util.Iterator;
import java.util.LinkedList;

public class TerritoryEngine {
	// imported tables and data
	private Parameters parms;
	private Map map;
	private String[] names;
	private double[] heights;
	private double[] erosion;
	private double[] riverFlux;
	private double[] waterLevel;
	private boolean[] oceanic;
	
	LinkedList<TradeRoute> curRoutes;	// currently displayed
	LinkedList<TradeRoute> prevRoutes;	// last committed
	
	// local data
	private TradeRoutes routes;	// route management
	Journey[] nodes;			// territorial status of every MeshPoint
	Journey.NextSteps queue;	// queue of points to be explored
	
	// travel parameters
	private double time_travel;	// minutes to travel one flat km
	private double time_climb;	// minutes to gain 1000 m
	private double time_cross;	// minutes to cross 1m^3/s stream
	private double travel_day;	// minutes of travel per day
	private double max_days;	// max days between cities
	
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
		
		time_travel = parms.dTimeTravel;
		time_climb = parms.dTimeClimb;
		time_cross = parms.dTimeCross;
		travel_day = parms.dTravelDay;
		max_days = parms.dTravelMax;
		
		int n = map.mesh.vertices.length;
		this.nodes = new Journey[n];
		Journey dummy = new Journey(-1, -1, null);
		this.queue = dummy.new NextSteps();
		this.routes = new TradeRoutes(map);
		
		// start with the current list of routes
		curRoutes = map.tradeRoutes();
		if (curRoutes == null) {
			curRoutes = new LinkedList<TradeRoute>();
			map.tradeRoutes(curRoutes);
		}
		commit();	// make a copy of it in case we need to abort
	}
	
	/**
	 * find all known cities and add all of their neighbors to the
	 * queue to be explored.
	 */
	public void allCities() {
		int n = map.mesh.vertices.length;
		for(int i = 0; i < n; i++) {
			if (names[i] == null)
				continue;
			if (names[i].startsWith("capital:") || names[i].startsWith("city:"))
				startFrom(i);
		}
		
		// move outwards from those to define our territories and find trade routes
		outwards(999, true);
	}
	
	/**
	 * discard all records of previous expansions
	 *   to enable new point-to-point routes
	 */
	public void reset() {
		for(int i = 0; i < nodes.length; i++)
			nodes[i] = null;
	}

	/**
	 * add a new point to the list from which we expand outwards
	 * @param point index of point
	 */
	public boolean startFrom(int point) {
		// if one end is oceanic, the other will find it
		if (oceanic[point])
			return false;
		
		nodes[point] = new Journey(point, point, null);
		nodes[point].cost = 0;
		if (parms.debug_level >= TERRITORY_DEBUG)
			System.out.println(String.format("start: %d %s", point, map.pointName(point)));
		for(int j = 0; j < map.mesh.vertices[point].neighbors; j++) {
			MeshPoint neighbor = map.mesh.vertices[point].neighbor[j];
			int x = neighbor.index;
			Journey step = new Journey(x, point, nodes[point]);
			step.cost = cost(point, x);
			nodes[x] = step;
			queue.add(step);
		}
		return true;
	}
	
	/**
	 * Journey outwards from each city until we have covered the map.
	 * When we reach a MeshPoint already covered from another city,
	 * record it as a possible trade route.
	 * 
	 * @param needed ... maximum number of routes to generate
	 * @param ocean_too ... do we want paths to the ocean
	 */
	public TradeRoute outwards(int needed, boolean ocean_too) {
		// if he only wants one route, don't worry about length
		boolean long_ok = needed == 1;
		TradeRoute lastAdded = null;
		
		for( Journey step = queue.next(); step != null; step = queue.next()) {
			// once quota is satisfied, just drain the queue
			if (needed <= 0)
				continue;
			
			// recursively explore all of our neighbors
			MeshPoint p = map.mesh.vertices[step.index];
			if (parms.debug_level >= TERRITORY_DEBUG)
				System.out.println(String.format("%d->%d: territory=%s, cost=%f", step.route.index, p.index, 
							map.pointName(step.city), step.cost));
			
			for(int i = 0; i < p.neighbors; i++) {
				MeshPoint neighbor = p.neighbor[i];
				// ignore edge nodes
				if (neighbor.neighbors < 3)
					continue;

				// already claimed nodes might be cross-overs
				int neighbor_x = neighbor.index;
				if (nodes[neighbor_x] != null) {
					if (nodes[neighbor_x].city != step.city) {
						TradeRoute r = routes.addCrossing(step, nodes[neighbor_x]);
						if (r != null) {
							lastAdded = r;
							needed--;
						}
					}
					continue;
				}
				
				// stop at the ocean
				if (oceanic[neighbor_x]) {
					if (ocean_too) {
						Journey ocean = new Journey(neighbor_x, -1, null);
						TradeRoute r = routes.addCrossing(step, ocean);
						if (r != null) {
							lastAdded = r;
							needed--;
						}
					}
					continue;
				}
				
				// no crossing lakes
				if (waterLevel[neighbor_x] > heights[neighbor_x] - erosion[neighbor_x])
					continue;
				
				// no farther than (half) the maximum allowable inter-city journey
				double thisCost = step.cost + cost(step.index, neighbor_x);
				if (thisCost > max_days/2 && !long_ok)
					continue;
				
				// add this Journey node to the list to be explored
				Journey nextStep = new Journey(neighbor_x, step.city, step);
				nextStep.cost = thisCost;
				nodes[neighbor_x] = nextStep;
				queue.add(nextStep);
			}
		}
		return lastAdded;
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
		double minutes = dX * time_travel;
		
		// going up-hill adds extra time
		double dZ = parms.height(heights[to] - heights[from]);
		if (dZ > 0)
			minutes += dZ * time_climb / 1000.0;
		
		// crossing rivers adds extra time
		double flux = riverFlux[from];
		if (flux > 0)
			minutes += Math.sqrt(flux) * time_cross;
		
		return minutes / travel_day;
	}
	
	/**
	 * update the parameters that guide route selection
	 * @param t_flat	minutes to travel one flat km
	 * @param t_climb	minutes to gain 1000 meters
	 * @param t_cross	minutes to cross a 1M^3/s stream
	 * @param min_per_day	minutes of travel per day
	 * @param max_days	maximum days between cities
	 */
	public void set_parms(double t_flat, double t_climb, double t_cross, double min_per_day, double max_days) {
		this.time_travel = t_flat;
		this.time_climb = t_climb;
		this.time_cross = t_cross;
		this.travel_day = min_per_day;
		this.max_days = max_days;
	}
	
	/**
	 * commit the current set of routes
	 */
	public void commit() {
		// make a new copy of the current list
		prevRoutes = new LinkedList<TradeRoute>();
		for(Iterator<TradeRoute> it = curRoutes.iterator(); it.hasNext(); ) {
			TradeRoute r = it.next();
			prevRoutes.add(r);
		}
	}
	
	/**
	 * fall back to the previously saved set of routes
	 */
	public void abort() {
		// throw away everything on the current list
		while(curRoutes.size() > 0)
			curRoutes.remove();
		
		// recreate the list with its previous contents
		for(Iterator<TradeRoute> it = prevRoutes.iterator(); it.hasNext(); ) {
			TradeRoute r = it.next();
			curRoutes.add(r);
		}
		map.window.repaint();
	}
}
