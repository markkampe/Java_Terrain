package worldBuilder;

public class TradeRoute {
	int	city1;	// index of first end point
	int city2;	// index of second end point
	int[] path;	// sequence of connecting points
	double cost;// cost of travel
	
	private static final int TRADEROUTE_DEBUG = 2;

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

		if (Parameters.getInstance().debug_level >= TRADEROUTE_DEBUG) {
			System.out.print(city1 + "->" + city2 + ": ");
			for(i = 0; i < path.length; i++) {
				if (i > 0)
					System.out.print("->");
				System.out.print(path[i]);
			}
			System.out.print("\n");
		}
	}

	/**
	 * create a Route where the path and cost are known
	 * @param steps		array of steps
	 * @param numsteps	number of steps in array
	 * @param cost		cost (in days) of this route
	 */
	public TradeRoute(int[] steps, int numsteps, double cost) {
		this.city1 = steps[0];
		this.city2 = steps[numsteps - 1];
		this.cost = cost;
		this.path = new int[numsteps];
		for(int i = 0; i < numsteps; i++)
			this.path[i] = steps[i];
		
		if (Parameters.getInstance().debug_level >= TRADEROUTE_DEBUG) {
			System.out.print(city1 + "->" + city2 + ": ");
			for(int i = 0; i < path.length; i++) {
				if (i > 0)
					System.out.print("->");
				System.out.print(path[i]);
			}
			System.out.print("\n");
		}
	}
}
