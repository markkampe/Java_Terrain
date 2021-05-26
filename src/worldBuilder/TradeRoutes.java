package worldBuilder;

import java.util.LinkedList;
import java.util.Iterator;

public class TradeRoutes {
	
	public class TradeRoute {
		int node1;	// index of one border node
		int	city1;	// index of node 1 source city
		int node2;	// index of second border node
		int city2;	// index of node 2 source city
		double cost;// cost of travel
		
		public TradeRoute(Journey one, Journey other) {
			node1 = one.index;
			city1 = one.city;
			node2 = other.index;
			city2 = other.city;
			cost = one.cost + other.cost;
		}
	}
	
	public Map map;
	public String[] names;
	public Parameters parms;
	public LinkedList<TradeRoute> routes;
	public LinkedList<TradeRoute> indirects;
	
	private static final int ROUTE_DEBUG = 2;
	
	public TradeRoutes(Map map) {
		this.map = map;
		this.names = map.getNameMap();
		this.parms = Parameters.getInstance();
		this.routes = new LinkedList<TradeRoute>();
		this.indirects = new LinkedList<TradeRoute>();
	}
	
	/**
	 * record a cross-over between two territories
	 * @param one	Journey point on one side
	 * @param other	Journey point on other side
	 * @return boolean (is this a new trade route)
	 */
	public boolean addCrossing(Journey one, Journey other) {
		// see if we already have a route between these two cities
		TradeRoute r = findRoute(one.city, other.city);
		if (r != null)
			return false;
		
		// get names of source and destination (for logging)
		String s1 = (one.city >= 0) ? CityDialog.lexName(names[one.city]) : "Ocean";
		String s2 = (other.city >= 0) ? CityDialog.lexName(names[other.city]) : "Ocean";
		
		// see if there is a cheaper route through another city
		for(Iterator<TradeRoute> it = routes.iterator(); it.hasNext();) {
			TradeRoute r1 = it.next();
			if (r1.city1 != one.city && r1.city2 != one.city)
				continue;	// this route doesn't involve us
			int intermediate = (r1.city1 == one.city) ? r1.city2 : r1.city1;
			if (intermediate == -1)
				continue;	// no going through the ocean
			TradeRoute r2 = findRoute(intermediate, other.city);
			if (r2 != null) {	// there is an indirect alternative
				String s3 = CityDialog.lexName(names[intermediate]);
				if (r1.cost + r2.cost < one.cost + other.cost) {
					r = new TradeRoute(one, other);
					r.cost = r1.cost + r2.cost;
					indirects.add(r);
					if (parms.debug_level > 0)
						System.out.println(String.format("Indirect: %s->%s->%s: %.1f days", 
														s1, s3, s2, r.cost));
					return false;
				}
			}
		}
		
		// add this new route to the list
		r = new TradeRoute(one, other);
		routes.add(r);
		if (parms.debug_level > 0)
			System.out.println(String.format("Trade route: %s to %s, %.1f days", s1, s2, r.cost));
			
		return true;
	}
	
	/**
	 * look for an existing crossing between two Journey nodes
	 */
	TradeRoute findRoute(int city1, int city2) {
		// see if we already have a direct route
		for(Iterator<TradeRoute> it = routes.iterator(); it.hasNext();) {
			TradeRoute r = it.next();
			if (city1 == r.city1 && city2 == r.city2)
				return r;
			if (city1 == r.city2 && city2 == r.city1)
				return r;
		}
		
		// see if we already have a better indirect route
		for(Iterator<TradeRoute> it = indirects.iterator(); it.hasNext();) {
			TradeRoute r = it.next();
			if (city1 == r.city1 && city2 == r.city2)
				return r;
			if (city1 == r.city2 && city2 == r.city1)
				return r;
		}
		
		return null;
	}
}