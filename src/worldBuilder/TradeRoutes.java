package worldBuilder;

import java.util.LinkedList;
import java.util.Iterator;

/**
 * Before we create a new TradeRoute (path from point A to point B)
 * we want to make sure we do not already have that path.  This class
 * maintains lists of direct (and indirect) routes and does these
 * checks before creating a new route.
 * 
 * There are two constructors:
 *   
 *   the first is used by TerritoryEngine when the expanding
 *   set of Journeys encounters a Journey from another Territory.
 *   
 *	 the second is used by Map.read() when we are simply re-loading
 *	 routes that have already been defined (we know all the steps)
 */
public class TradeRoutes {

	public Map map;
	public String[] names;
	public Parameters parms;
	public LinkedList<TradeRoute> routes;
	public LinkedList<TradeRoute> indirects;
	
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
