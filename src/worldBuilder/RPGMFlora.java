package worldBuilder;

import java.awt.Color;
import java.util.ListIterator;

/**
 * this class populates a grid with appropriate plants
 * 	based on a set of plant rules, and characteristics of each square
 *
 */
public class RPGMFlora {
	private String names[];		// names of each type of plant
	private Color colors[];		// preview color of each type of plant
	private TileRules rules;	// plant generation rules
	private RPGMTiler tiler;	// tiler (for per square info)
	private Parameters parms;
	
	private static final int RULE_DEBUG = 2;
	
	/**
	 * Instantiate a flora populator
	 * 
	 * @param tiler ... associated tiler
	 * @param pallettFile ... floral palette file
	 */
	public RPGMFlora(RPGMTiler tiler, String paletteFile) {
		this.parms = Parameters.getInstance();
		this.tiler = tiler;
		
		// read in the rules and enumerate their names
		rules = new TileRules(paletteFile);
		names = new String[rules.rules.size()+1];
		colors = new Color[rules.rules.size()+1];
		names[0] = "NONE";
		colors[0] = Color.BLACK;
		int numRules = 1;
		for( ListIterator<TileRule> it = rules.rules.listIterator(); it.hasNext(); numRules++) {
			TileRule r = it.next();
			names[numRules] = r.ruleName;
			colors[numRules] = r.previewColor;
		}
	}

	/**
	 * @return array to map Flora indices into names
	 */
	public String[] getFloraNames() {
		return names;
	}
	
	/**
	 * @return array to map Flora indices into colors
	 */
	public Color[] getFloraColors() {
		return colors;
	}
	
	/**
	 * getFlora ... populate a map with appropriately chosen plant classes
	 * 
	 * @param tiler ... RPGMTiler with fully loaded topography
	 * @param classes ... array of plant class names
	 * @param quotas ... array of plant class quotas (in tiles)
	 * @param palette ... file of plant class definitions
	 * 
	 * @return	2D array of plant class name (per tile)
	 * 
	 * for each class of plant
	 * 		every rule bids on every empty square
	 * 		while we haven't met class quota
	 * 			next highest bid gets its square
	 */
	public int[][] getFlora(String classes[], int quotas[]) {
		// figure out the map size
		int y_points = tiler.heights.length;
		int x_points = tiler.heights[0].length;
		int flora[][] = new int[y_points][x_points];
		
		if (parms.debug_level >= RULE_DEBUG) {
			System.out.println("Populate " + y_points + "x" + x_points + " grid with flora");
			for(int i = 0; i < quotas.length; i++)
				System.out.println("    " + classes[i] + ": " + quotas[i] + " tiles");
		}

		// populate each of the plant classes
		for(int c = 0; c < classes.length; c++) {
			for(int i = 0; i < y_points; i++)
				for(int j = 0; j < x_points; j++) {
					// don't bother with already claimed tiles
					if (flora[i][j] != 0)
						continue;

					// compute the bidding attributes of this square
					int alt = (int) parms.altitude(tiler.heights[i][j] - tiler.erode[i][j]);
					double lapse = alt * parms.lapse_rate;
					int terrain = tiler.typeMap[tiler.levels[i][j]];
					double hydro = tiler.hydration[i][j];
					double soil = tiler.soil[i][j];
					double slope = tiler.slope(i, j);
					double face = tiler.direction(i, j);
					
					// collect a bid from every rule of THIS class
					int rulenum = 0;
					for( ListIterator<TileRule> it = rules.rules.listIterator(); it.hasNext();) {
						TileRule r = it.next();
						rulenum++;		// name/claim based on rulenum + 1
						// make sure this is the right class of plant
						if (!classes[c].equals(r.className))
							continue;
						
						// collect the bid
						double b = r.bid(terrain, alt, hydro, 
										tiler.Twinter - lapse, tiler.Tsummer - lapse, 
										soil, slope, face);
						if (b>0)
							record_bid(rulenum, b, i, j);
					}
				}

			// fill the class quota from the highest bidders
			int needed = quotas[c];
			for( FloraBid b = first_bid(); needed > 0 && b != null; b = next_bid()) {
				// skip bids for already claimed tiles
				if (flora[b.row][b.col] != 0)
					continue;

				// this one is a winner
				flora[b.row][b.col] = b.floraID;
				needed -= 1;
			}
			reset_bids();
			
			// see if we were unable to place the requested number of plants
			if (needed > 0 && parms.debug_level > 0)
				System.out.println("RPGMFlora: only filled " + 
						(quotas[c] - needed) + "/" + 
						quotas[c] + " " + classes[c] + "-tiles");
		}

		return flora;
	}
	
	private class FloraBid {
		
		public int floraID;	// index bidding plant
		public double bid;	// amount of bid
		public int row;		// row,col for which bid was made
		public int col;
		public FloraBid next;
		
		public FloraBid(int index, double bid, int row, int col) {
			this.floraID = index;
			this.bid = bid;
			this.row = row;
			this.col = col;
			this.next = null;
		}
	}
	
	private void record_bid(int id, double bid, int row, int col) {
		FloraBid b = new FloraBid(id, bid, row, col);
		// System.out.println("RULE " + names[id] + " bids " + bid + " for (" + row + "," + col + ")");
		// FIX add this bid to the appropriate list
	}
	
	private FloraBid first_bid() {
		// FIX return highest bid
		return null;
	}
	
	private FloraBid next_bid() {
		// FIX return next bid
		return null;
	}
	
	private void reset_bids() {
		// FIX discard all the bids
		
	}
}
