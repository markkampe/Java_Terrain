package worldBuilder;

import java.util.LinkedList;
import java.util.Iterator;

public class TradeRoutes {

	public Map map;
	public String[] names;
	public Parameters parms;
	public LinkedList<TradeRoute> routes;
	public LinkedList<TradeRoute> indirects;
	
	private static final int TRADEROUTE_DEBUG = 2;
	
	public class TradeRoute {
		int	city1;	// index of first end point
		int city2;	// index of second end point
		int[] path;	// sequence of connecting points
		double cost;// cost of travel
		
		/**
		 * create a new TradeRoute between two outgoing paths
		 * @param one the path that tried to expand
		 * @param other the (already claimed) neighbor it encountered
		 */
		public TradeRoute(Journey one, Journey other) {
			city1 = one.city;
			city2 = other.city;
			cost = one.cost + other.cost;
			
			// figure out how long the path is
			int leftHops = 0, rightHops = 0;
			for(Journey j = one; j != null; j = j.route)
				leftHops++;
			for(Journey j = other; j != null; j = j.route)
				rightHops++;
			path = new int[leftHops + rightHops];
			
			// itemize the path from here to city1 and city2
			int i = leftHops - 1;
			for(Journey j = one; j != null; j = j.route)
				path[i--] = j.index;
			i = leftHops;
			for(Journey j = other; j != null; j = j.route)
				path[i++] = j.index;
			
			if (parms.debug_level >= TRADEROUTE_DEBUG) {
				System.out.print(city1 + "->" + city2 + ": ");
				for(i = 0; i < path.length; i++) {
					if (i > 0)
						System.out.print("->");
					System.out.print(path[i]);
				}
				System.out.print("\n");
			}
		}
	}

	
	public TradeRoutes(Map map) {
		this.map = map;
		this.names = map.getNameMap();
		this.routes = map.tradeRoutes();
		if (this.routes == null) {
			this.routes = new LinkedList<TradeRoute>();
			map.tradeRoutes(this.routes);
		}
		this.indirects = new LinkedList<TradeRoute>();
		this.parms = Parameters.getInstance();
	}
	
	/**
	 * record a cross-over between two territories
	 * @param one	Journey point on one side
	 * @param other	Journey point on other side
	 * @return boolean (is this a new trade route)
	 */
	public TradeRoute addCrossing(Journey one, Journey other) {
		// see if we already have a route between these two cities
		TradeRoute r = findRoute(one.city, other.city);
		if (r != null)
			return null;
		
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
				if (r1.cost + r2.cost < one.cost + other.cost) {
					r = new TradeRoute(one, other);
					r.cost = r1.cost + r2.cost;
					indirects.add(r);
					if (parms.debug_level > 0)
						System.out.println(String.format("Indirect: %s->%s->%s: %.1f days", 
														map.pointName(one.city), 
														map.pointName(intermediate), 
														map.pointName(other.city), r.cost));
					return null;
				}
			}
		}
		
		// add this new route to the list
		r = new TradeRoute(one, other);
		routes.add(r);
		if (parms.debug_level > 0)
			System.out.println(String.format("Trade route: %s to %s, %.1f days", 
					map.pointName(one.city), map.pointName(other.city), r.cost));
			
		return r;
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
