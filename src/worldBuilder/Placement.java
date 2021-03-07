package worldBuilder;

import java.awt.Color;
import java.util.ListIterator;

/**
 * the Placement engine associates resources with MeshPoints based on
 * bids resulting from ResourceRules.  It was originally designed for
 * flora placement, but can also work for minerals ... and can probably
 * be extended for fauna and other natural resources.
 */
public class Placement {
	private static final int MAX_RULES = 20;
	private static final int MAX_ID = 100;
	private static final int NONE = 0;
	
	private Parameters parms;

	private MeshPoint points[];	// MeshPoints
	private double hydroMap[];	// per mesh-point hydration
	private double heightMap[];	// per mesh-point altitude
	private double erodeMap[];	// per mesh-point erosion
	private double soilMap[];	// per mesh-point soil map
	private double resources[];	// per MeshPoint assignments

	private ResourceRule bidders[];	// resource bidding rules
	private int numRules;		// number of bidding rules
	private int firstPass;		// lowest numbered bidding pass
	private int lastPass;		// highest numbered bidding pass

	private Color colorMap[];	// per-rule preview colors
	private String nameMap[];	// per-rule resource names
	
	private static final int PLACEMENT_DEBUG = 2;


	/**
	 * instantiate a new Placement engine
	 * 
	 * 	note that a Placement engine can be instantiated without a Map
	 * 		 simply to load name<->resource ID mapping
	 * 
	 * @param rulesFile ... file of resource bidding rules
	 * @param Map ... Map (can be null)
	 * @param resources ... array of per MeshPoint values (can be null)
	 */
	public Placement(String rulesFile, Map map, double resources[]) {
	
		// build up a list of ResourceRules
		ResourceRule.loadRules(rulesFile);
		bidders = new ResourceRule[MAX_RULES];
		colorMap = new Color[MAX_ID+1];
		nameMap = new String[MAX_ID+1];
		numRules = 0;
		firstPass = 666;
		lastPass = -666;
		for( ListIterator<ResourceRule> it = ResourceRule.iterator(); it.hasNext();) {
			ResourceRule r = it.next();
			bidders[numRules++] = r;
			colorMap[r.id] = r.previewColor;
			nameMap[r.id] = r.ruleName;
			if (r.order < firstPass)
				firstPass = r.order;
			if (r.order > lastPass)
				lastPass = r.order;
		}
		
		// if we were passed a map, save its tables
		if (map != null) {
			this.points = map.mesh.vertices;
			this.hydroMap = map.getHydrationMap();
			this.heightMap = map.getHeightMap();
			this.erodeMap = map.getErodeMap();
			this.soilMap = map.getSoilMap();
		}
		this.resources = resources;
		parms = Parameters.getInstance();
	}
	
	/**
	 * @return id->preview Color map
	 */
	public Color[] previewColors() {
		return colorMap;
	}
	
	/**
	 * @return id->name map
	 */
	public String[] resourceNames() {
		return nameMap;
	}
	
	/**
	 * @return ID associated with a resource name
	 */
	public int resourceID(String name) {
		for(int i = 0; i < nameMap.length; i++)
			if (nameMap[i] != null && name.equals(nameMap[i]))
				return i;
		return NONE;
	}

	/**
	 * record of a winning bid (from a id(type) for a MeshPoint
	 */
	private class PointBid {
		int	type;		// resource type (meaningful to its class)
		int id;			// resource id
		int index;		// index of the point for which this bid was made
		double bid;		// amount of this bid
		PointBid next;	// next bid in the list

		PointBid(int index, int type, int id, double bid) {
			this.index = index;
			this.type = type;
			this.id = id;
			this.bid = bid;
			this.next = null;
		}
	}

	/**
	 * populate the selected region w/resources based on our rules
	 * 
	 * @param x0 ... upper left corner of selected region
	 * @param y0 ... upper left corner of selected regions
	 * @param height ... height of selected region
	 * @param width ... width of selected region
	 * @param quotas ... per class quotas (in MeshPoints)
	 * 
	 * @return array of (per-class) point placements
	 */
	public int[] update(double x0, double y0, double height, double width, int quotas[]) {

		int counts[] = new int[MAX_RULES];	// allocated points (vs quotas)
		
		// initialize the points to be populated
		for(int i = 0; i < points.length; i++)
			if (points[i].x >= x0 && points[i].x < x0+width &&
				points[i].y >= y0 && points[i].y < y0+height) {
				resources[i] = NONE;
			}

		// sub-types bid for mesh points in specified order
		for(int pass = firstPass; pass <= lastPass; pass++) {
			// see if there are any under-quota rules in this class
			String eligible = null;
			for(int r = 0; r < numRules; r++) {
				if (bidders[r].order != pass)
					continue;
				if (counts[bidders[r].type] >= quotas[bidders[r].type])
					continue;
				if (eligible == null)
					eligible = bidders[r].ruleName;
				else
					eligible = eligible + "," + bidders[r].ruleName;
			}
			if (eligible == null)
				continue;
				
			// collect bids for every point in the range
			PointBid thisBid;
			PointBid winners = null;
			for(int i = 0; i < points.length; i++) {
				// make sure it is in selected area
				if (points[i].x < x0 || points[i].x >= x0+width ||
						points[i].y < y0 || points[i].y >= y0+height)
					continue;

				// make sure it is not yet occupied
				if (resources[i] != NONE)
					continue;

				// gather bidding attributes for this point
				int alt = (int) parms.altitude(heightMap[i] - erodeMap[i]);
				double lapse = alt * parms.lapse_rate;
				double soil = soilMap[i];
				double hydro = hydroMap[i];

				// figure out the (potentially goosed) temperature
				double Twinter = parms.meanWinter();
				double Tsummer = parms.meanSummer();
				
				// collect all bids (within this order) for this point
				double high_bid = -666.0;
				int winner = -1;
				for(int r = 0; r < numRules; r++) {
					if (bidders[r].order != pass)
						continue;	// not eligible to bid this round
					if (counts[bidders[r].type] >= quotas[bidders[r].type])
						continue;	// already at quota
					
					double bid = bidders[r].bid(alt, hydro, Twinter - lapse, Tsummer - lapse, soil);
					if (bid <= 0)
						continue;	// doesn't want this point
					if (bid > high_bid) {
						high_bid = bid;
						winner = r;
					}
				}

				// add the winner to our list of winning bids
				if (winner >= 0) {
					int type = bidders[winner].type;
					int id = bidders[winner].id;
					thisBid = new PointBid(i, type, id, high_bid);

					if (winners == null)
						winners = thisBid;		// first bid
					else if (winners.bid < thisBid.bid) {
						thisBid.next = winners;	// highest bid
						winners = thisBid;
					} else {					// insert it after the last bigger-than-this
						PointBid prev = winners;
						while(prev.next != null && prev.next.bid > thisBid.bid)
							prev = prev.next;
						thisBid.next = prev.next;
						prev.next = thisBid;
					}
				}
			}

			// award tiles in bid-order
			int placed = 0;
			thisBid = winners;
			while(thisBid != null) {
				// point must not yet be awarded, bidder must be under quota
				if (resources[thisBid.index] == NONE && counts[thisBid.type] < quotas[thisBid.type]) {
					resources[thisBid.index] = thisBid.id;
					counts[thisBid.type] += 1;
					placed += 1;
				}
				thisBid = thisBid.next;
			}
			
			if (parms.debug_level >= PLACEMENT_DEBUG)
				System.out.println("... pass " + pass + " placed " + placed + " from " + eligible);
		}
		return counts;
	}
}
