package worldBuilder;

import java.awt.Color;
import java.util.ListIterator;

/**
 * class to populates a grid with appropriate plants
 * (based on a set of plant rules, and characteristics of each square)
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
	 * @param paletteFile ... name of floral palette file
	 */
	public RPGMFlora(RPGMTiler tiler, String paletteFile) {
		this.parms = Parameters.getInstance();
		this.tiler = tiler;
		
		// read in the rules and enumerate their names
		rules = new TileRules(paletteFile);
		names = new String[rules.rules.size()+1];
		colors = new Color[rules.rules.size()+1];
		names[0] = "NONE";
		colors[0] = Color.GRAY;
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
	 * @param classes ... array of plant class names
	 * @param quotas ... array of plant class quotas (in tiles)
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

		double Tmean = (tiler.Tsummer + tiler.Twinter)/2;
		// populate each of the plant classes
		for(int c = 0; c < classes.length; c++) {
			// collect bids for every square
			for(int i = 0; i < y_points; i++)
				for(int j = 0; j < x_points; j++) {
					// don't bother with already claimed tiles
					if (flora[i][j] != 0)
						continue;

					// compute the bidding attributes of this square
					int alt = (int) parms.altitude(tiler.heights[i][j] - tiler.erode[i][j]);
					double lapse = alt * parms.lapse_rate;
					int level = tiler.levels[i][j];
					int terrain = tiler.typeMap[level];
					double hydro = tiler.hydration[i][j];
					double soil = tiler.soil[i][j];
					double slope = tiler.slope(i, j);
					double face = tiler.direction(i, j);
					
					// collect a bid from every rule of THIS class
					int rulenum = 0;
					for( ListIterator<TileRule> it = rules.rules.listIterator(); it.hasNext();) {
						TileRule r = it.next();
						rulenum++;		// name/claim based on rulenum + 1
						
						// does this rule apply to this terrain and floral class
						if (r.wrongFlora(classes[c]))
							continue;
						if (r.wrongTerrain(terrain))
							continue;
						
						// collect the bid
						double b = r.bid(alt, hydro, 
										Tmean - lapse, Tmean - lapse, 
										soil, slope, face);
						if (b <= 0)
							continue;
						
						// avoid putting major flora adjacent to level changes
						if (i > 0 && tiler.levels[i-1][j] != level)
							continue;
						if (i < y_points - r.height && tiler.levels[i+r.height][j] != level)
							continue;
						if (j > 0 && tiler.levels[i][j-1] != level)
							continue;
						if (j < x_points - r.width && tiler.levels[i][j+r.width] != level)
							continue;
						
						// if it is stamp, we have to check the whole region
						if (r.width > 1 || r.height > 1) {
							// region must be height/width aligned, within grid
							if ((i%r.height != 0) || (j%r.width != 0))
								b = 0;
							if (i + r.height > y_points || j + r.width > x_points)
								b = 0;
							
							// see if all squares in region are acceptable
							for(int dr = 0; dr < r.height; dr++)
								for(int dc = 0; b > 0 && dc < r.width; dc++) {
									if (dr == 0 && dc == 0)
										continue;	// already got this one
									
									if (tiler.levels[i+dr][j+dc] != level) {
										b = 0;
										continue;	// stamps cannot cross levels
									}
									
									// check the bid for this square too
									alt = (int) parms.altitude(tiler.heights[i+dr][j+dc] - tiler.erode[i+dr][j+dc]);
									lapse = alt * parms.lapse_rate;
									terrain = tiler.typeMap[tiler.levels[i+dr][j+dc]];
									hydro = tiler.hydration[i+dr][j+dc];
									soil = tiler.soil[i+dr][j+dc];
									slope = tiler.slope(i+dr, j+dc);
									face = tiler.direction(i+dr, j+dc);
									if (r.bid(alt, hydro, Tmean - lapse, Tmean - lapse, soil, slope, face) <= 0)
										b = 0;
									
									// TODO: compute bid for entire stamp area?
								}
						}
						if (b > 0)
							record_bid(rulenum, b, i, j);
					}
				}

			// fill the class quota from the highest bids
			int needed = quotas[c];
			for( FloraBid b = first_bid(); needed > 0 && b != null; b = next_bid()) {
				// skip bids for already claimed tiles
				if (flora[b.row][b.col] != 0)
					continue;
		
				// go back to the winning rule
				TileRule r = null;
				int rulenum = 1;
				for( ListIterator<TileRule> it = rules.rules.listIterator(); it.hasNext();) {
					r = it.next();
					if (rulenum == b.floraID)
						break;
					rulenum++;
				}
				// assign tile(s) to that rule
				for(int dr = 0; dr < r.height; dr++)
					for(int dc = 0; dc < r.width; dc++) {
						flora[b.row+dr][b.col+dc] = b.floraID;
						needed--;
					}
				
			}
			reset_bids();
			
			// see if we were unable to place the requested number of plants
			if (parms.debug_level > 0)
				System.out.println("     placed " + (quotas[c] - needed) + "/" + quotas[c] + " " + classes[c] + "-tiles"); 
		}

		return flora;
	}
	
	/**
	 * This private class keeps track of every flora type's bids
	 * on every available tile.
	 */
	private class FloraBid {
		// TODO - rewrite FloraBid w/Iterator?
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
	
	private FloraBid bids;
	private FloraBid thisBid;
	
	/**
	 * record a bid on a tile
	 * 
	 * @param id ... floral class index
	 * @param bid ... bid
	 * @param row ... tile row
	 * @param col ... tile column
	 */
	private void record_bid(int id, double bid, int row, int col) {
		FloraBid b = new FloraBid(id, bid, row, col);
		if (bids == null || bid > bids.bid) {
			// insert at front of list
			b.next = bids;
			bids = b;
		} else {	// insertion sort into the list
			FloraBid f = bids;
			while(f.next != null && f.next.bid > bid)
				f = f.next;
			b.next = f.next;
			f.next = b;
		}
	}

	private FloraBid first_bid() {
		thisBid = bids;
		return thisBid;
	}
	
	private FloraBid next_bid() {
		if (thisBid != null)
			thisBid = thisBid.next;
		return thisBid;
	}
	
	private void reset_bids() {
		bids = null;
		thisBid = null;
	}
}
