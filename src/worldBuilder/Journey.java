package worldBuilder;

/**
 * These nodes are used to define territory and trade routes.
 *  - A border is the line, between two cities, where the cost of
 *    travel (from each) is equal.
 *  - A trade route is the lowest cost path between them.
 * 
 * We identify these by expanding journeys out from each city
 * until they meet journeys from another city.
 */
public class Journey {
	public int index;	// index of this Journey node
	public int city;	// index of owning city
	public double cost;	// cost of traveling from city
	public Journey route;// path back to owning city
	
	protected Journey next;	// link in NextSteps queue
	
	/**
	 * create a node for the next step in an outward journey
	 * @param index ... corresponding MeshPoint index
	 * @param fromCity ... large city from which we are traveling
	 * @param fromPoint ... previous point in our journey
	 */
	public Journey(int index, int fromCity, Journey fromPoint) {
		this.index = index;
		this.city = fromCity;
		this.route = fromPoint;
		this.next = null;
	}
	
	/**
	 * this is a (lowest) cost-ordered queue of possible next steps
	 * in our outward journeys.  Processing steps in this order
	 * enables us to find the lowest cost routes and crossings.  
	 */
	public class NextSteps {
		Journey lowest;		// first (lowest cost) step in queue
		
		public NextSteps() {
			lowest = null;
		}
		
		/**
		 * @return the next (lowest cost) Journey step from the queue
		 */
		public Journey next() {
			if (this.lowest == null)
				return null;
			
			Journey ret = lowest;
			lowest = ret.next;
			ret.next = null;
			return ret;
		}
		
		/**
		 * add another possible step to the queue of options
		 * @param step	Journey to be added to the queue
		 */
		public void add(Journey step) {
			// should this be the first thing on the queue?
			if (this.lowest == null || step.cost < lowest.cost) {
				step.next = lowest;
				lowest = step;
			} else {	// find the point in front of which we should insert
				Journey prev = lowest;
				while(prev.next != null && prev.next.cost <= step.cost)
					prev = prev.next;
				step.next = prev.next;
				prev.next = step;
			}
		}
	}
}
