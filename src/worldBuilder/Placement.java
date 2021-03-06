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
	private static final int MAX_RULES = 40;
	private static final int MAX_ID = 100;
	private static final int NONE = 0;
	
	private Map map;
	private Parameters parms;

	private MeshPoint points[];	// MeshPoints
	private double waterLevel[];// per mesh-point depth below water
	private double heightMap[];	// per mesh-point altitude
	private double erodeMap[];	// per mesh-point erosion
	private double rainMap[];	// per mesh-point rainfall
	private double fluxMap[];	// per mesh-point water flow
	private double floraMap[];	// per mesh point floral ecotope
	private double resources[];	// per MeshPoint assignments

	private ResourceRule bidders[];	// resource bidding rules
	private int numRules;		// number of bidding rules
	private int firstPass;		// lowest numbered bidding pass
	private int lastPass;		// highest numbered bidding pass

	private Color colorMap[];	// per-rule preview colors
	private String nameMap[];	// per-rule resource names
	// private String classes;		// names of resource classes
	
	private static final int ECOTOPE_ANY = -1;
	private static final int ECOTOPE_GREEN = -2;
	private static final int ECOTOPE_NON_GREEN = -3;	// NONE, Desert, Alpine
	
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
		this.map = map;
		
		// build up a list of ResourceRules
		ResourceRule x = new ResourceRule("dummy");
		x.loadRules(rulesFile);
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
			this.waterLevel = map.getWaterLevel();
			this.heightMap = map.getHeightMap();
			this.erodeMap = map.getErodeMap();
			this.rainMap = map.getRainMap();
			this.fluxMap = map.getFluxMap();
			this.floraMap = map.getFloraMap();
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

	public int[] update(double x0, double y0, double height, double width, int quotas[], String classNames[]) {
		boolean[] selected = new boolean[map.mesh.vertices.length];
		for(int i = 0; i < selected.length; i++) {
			MeshPoint p = map.mesh.vertices[i];
			selected[i] = (p.x >= x0 && p.x <= x0+width && p.y >= y0 && p.y <= y0+height);
		}
		
		return update(selected, quotas, classNames);
	}
	
	/**
	 * populate the selected region w/resources based on our rules
	 * 
	 * @param x0 ... upper left corner of selected region
	 * @param y0 ... upper left corner of selected regions
	 * @param height ... height of selected region
	 * @param width ... width of selected region
	 * @param quotas ... per class quotas (in MeshPoints)
	 * @param classNames ... names of the quota-ed classes
	 * 
	 * @return array of (per-class) point placements
	 */
	public int[] update(boolean[] selected, int quotas[], String classNames[]) {

		int counts[] = new int[MAX_RULES];	// allocated points (vs quotas)
		
		// get the class number for each bidding rule
		int bidderClass[] = new int[numRules];
		for(int i = 0; i < classNames.length; i++)
			for(int r = 0; r < numRules; r++)
				if (bidders[r].className != null && bidders[r].className.equals(classNames[i]))
					bidderClass[r] = i;
		
		// get the ecotope number for each bidding rule
		String[] floraNames = map.floraNames;
		int bidderFlora[] = new int[numRules];
		for(int r = 0; r < numRules; r++) {
			String f = bidders[r].floraType;
			if (f == null || f.equals("ANY") || f.equals("any"))
				bidderFlora[r] = ECOTOPE_ANY;
			else if (f.equals("GREEN") || f.equals("green"))
				bidderFlora[r] = ECOTOPE_GREEN;
			else if (f.equals("NON-GREEN") || f.equals("non-green"))
				bidderFlora[r] = ECOTOPE_NON_GREEN;
			else	// look it up
				for(int i = 0; i < floraNames.length; i++) {
					if (floraNames[i] != null && floraNames[i].equals(f)) {
						bidderFlora[r] = i;
						break;
					}
				}
		}
		
		// figure out which ecotopes are green
		boolean floraGreen[] = new boolean[floraNames.length];
		for(int i = 0; i < floraNames.length; i++) {
			if (floraNames[i] == null || floraNames[i].equals("NONE"))
				continue;
			if (floraNames[i].equals("Desert") || floraNames[i].equals("Alpine"))
				continue;
			floraGreen[i] = true;
		}
		
		// initialize the points to be unpopulated
		for(int i = 0; i < points.length; i++)
			if (selected[i])
				resources[i] = NONE;

		// sub-types bid for mesh points in specified order
		for(int pass = firstPass; pass <= lastPass; pass++) {
			// see if there are any under-quota rules in this class
			String eligible = null;
			for(int r = 0; r < numRules; r++) {
				// see if this bidder is assigned to this pass
				if (bidders[r].order != pass)
					continue;
				
				// see if this bidder has already made its quota
				int thisClass = bidderClass[r];
				if (counts[thisClass] >= quotas[thisClass])
					continue;
				
				// construct a list of eligible bidders (for diagnostic output)
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
				if (!selected[i])
					continue;

				// make sure it is not yet occupied
				if (resources[i] != NONE)
					continue;

				// gather bidding attributes for this point
				int alt = (int) parms.altitude(heightMap[i] - erodeMap[i]);
				double lapse = alt * parms.lapse_rate;
				double depth = 0;
				if (waterLevel[i] > (heightMap[i] - erodeMap[i]))
					depth = parms.height(waterLevel[i] - (heightMap[i] - erodeMap[i]));
				double rain = rainMap[i];
				double flux = fluxMap[i];
				int flora = (int) floraMap[i];

				// figure out the (potentially goosed) temperature
				double Twinter = parms.meanWinter();
				double Tsummer = parms.meanSummer();
				
				// collect all bids (within this order) for this point
				double high_bid = -666.0;
				int winner = -1;
				for(int r = 0; r < numRules; r++) {
					if (bidders[r].order != pass)
						continue;	// not eligible to bid this round
					if (counts[bidderClass[r]] >= quotas[bidderClass[r]])
						continue;	// already at quota
					
					// floral ecotope matching is a little to complex for bid()
					int wants = bidderFlora[r];
					if (wants == ECOTOPE_GREEN && !floraGreen[flora])
						continue;
					if (wants == ECOTOPE_NON_GREEN && floraGreen[flora])
						continue;
					if (wants != ECOTOPE_ANY && wants != flora)
						continue;
										
					double bid = bidders[r].bid(alt, depth, flux, rain, Twinter - lapse, Tsummer - lapse);
					if (parms.debug_level >= PLACEMENT_DEBUG || parms.rule_debug != null && parms.rule_debug.equals(bidders[r].ruleName)) {
						String msg = "   RULE " + bidders[r].ruleName + " bids " +
									String.format("%6.2f for point %5d", bid, i) +
									String.format(", alt=%d%s", alt, Parameters.unit_z);
						msg += String.format(", depth=%.0f%s", depth, Parameters.unit_z);
						msg += String.format(", flux=%f%s", flux, Parameters.unit_f);
						msg += String.format(", rain=%f%s", rain, Parameters.unit_r);
						msg += String.format(", temp=%.1f-%.1f%s",
											Twinter - lapse, Tsummer - lapse, Parameters.unit_t);
						if (bid <= 0)
							msg += " (" + bidders[r].justification + ")";
						System.out.println(msg);
					}
					if (bid <= 0)
						continue;	// doesn't want this point
					if (bid > high_bid) {
						high_bid = bid;
						winner = r;
					}
				}

				// add the winner to our list of winning bids
				if (winner >= 0) {
					int type = bidderClass[winner];
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
