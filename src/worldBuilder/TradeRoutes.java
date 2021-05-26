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
	
	public TradeRoutes(Map map) {
		this.map = map;
		this.names = map.getNameMap();
		this.parms = Parameters.getInstance();
		this.routes = new LinkedList<TradeRoute>();
	}
	
	/**
	 * record a cross-over between two territories
	 * @param one	Journey point on one side
	 * @param other	Journey point on other side
	 * @return boolean (is this a new trade route)
	 */
	public boolean addCrossing(Journey one, Journey other) {
		// is there already a crossing between these two territories
		for(Iterator<TradeRoute> it = routes.iterator(); it.hasNext();) {
			TradeRoute r = it.next();
			if (one.city == r.city1 && other.city == r.city2)
				return false;
			if (one.city == r.city2 && other.city == r.city1)
				return false;
		}
		
		// FIX is there already a cheaper route through another city
		
		// add this new route to the list
		TradeRoute r = new TradeRoute(one, other);
		routes.add(r);
		if (parms.debug_level > 0)
			System.out.println(String.format("Trade route: %s to %s, %.1f days",
								CityDialog.lexName(names[one.city]), 
								CityDialog.lexName(names[other.city]),
								r.cost));
		return true;
	}
}
