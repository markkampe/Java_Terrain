package worldBuilder;

import java.awt.Color;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ListIterator;

/**
 * exporter to write world map as tile numbers in RPGMaker levels
 */
public class RPGMTiler implements Exporter {

	private static final int MAXRULES = 40;
	private static final int SPRITES_PER_ROW = 8;
	private static final int EXPORT_DEBUG = 3;

	private static final int ECOTOPE_ANY = -1;
	private static final int ECOTOPE_GREEN = -2;
	private static final int ECOTOPE_NON_GREEN = -3;	// NONE, Desert, Alpine

	private static final int WATER_SHADE_MIN = 96;	// for previews
	private static final int WATER_SHADE_DELTA = 80;
	
	public static final int FLORA_NONE = 0;
	public static final int FLORA_GRASS = 1;
	public static final int FLORA_BRUSH = 2;
	public static final int FLORA_TREES = 3;

	private Parameters parms;	// general parameters
	RPGMRule rules;				// tile placement rules 

	private boolean useSLOPE;	// enables special level processing

	/** map dimensions (in tiles)	*/
	public int x_points, y_points;
	private int tile_size; 		// tile size (in meters)
	private double lat;			// latitude
	private double lon;			// longitude

	/** seasonal mean temperatures	*/
	public double Tsummer, Twinter;
	private double Tmean; 	// mean temperature (degC)
	//private double[][] rain;	// per point rainfall (meters)

	/** per point height (Z units)	*/
	public double[][] heights;
	/** per point erosion/depostion (Z units)	*/
	public double[][] erode;
	/** per point water depth (Z units)	*/
	public double[][] depths;
	/** per point soil type	*/
	public double[][] soil;
	/** per point terrain level	*/
	public int[][] levels;
	/** per point flora type	*/
	public int[][] floraTypes;
	/** mapping from levels to TerrainTypes	*/
	public int[] typeMap;
	/** map from ecotope types to names	*/
	public String[] floraNames;
	
	/** fraction of tiles to be covered by each flora class */
	private double[] floraQuotas;

	private RPGMRule bidders[];		// list of bidders for the current level
	private	int numRules = 0;		// number of rules eleigible to bid

	private int bidder_ecotope[];	// (integer) ectotope for each bidder
	private double bidder_quota[];	// (double) max coverage for each bidder
	private boolean[] floraGreen;	// which ecotope classes support grass

	//private double minHeight;	// lowest altitude in export
	//private double maxHeight;	// highest altitude in export
	//private double minDepth;	// shallowest water in export
	//private double maxDepth;	// deepest water in export

	/**
	 * create a new output writer
	 * 
	 * @param tileRules ... name of output file
	 * @param width of map (in cells)
	 * @param height of map (in cells)
	 */
	public RPGMTiler(String tileRules, int width, int height) {
		this.parms = Parameters.getInstance();
		this.x_points = width;
		this.y_points = height;

		// read in the rules for this tile-set
		RPGMRule x = new RPGMRule("dummy");
		x.loadRules(tileRules);

		// determine whether or not we are rendering levels
		useSLOPE = false;
		for( ListIterator<ResourceRule> it = ResourceRule.iterator(); it.hasNext();) {
			RPGMRule r = (RPGMRule) it.next();
			if (r.terrain == TerrainType.SLOPE)
				useSLOPE = true;
		}
		
		// default quotas - everyone can bid on every tile
		floraQuotas = new double[4];
		for(int i = 0; i < floraQuotas.length; i++)
			floraQuotas[i] = 1.0;
	}
	
	/**
	 * return list of needed map up-loads
	 */
	public int neededInfo() {
		return(HEIGHT + DEPTH + EROSION + MINERALS + FLORA);
	}
	
	/**
	 * set the fraction of tiles for each flora class
	 * @param grass ... fraction of squares eligible for grass
	 * @param brush ... fraction of squares eligible for brush
	 * @param trees ... fraction of squares eligible for trees
	 */
	public void floraQuotas(double grass, double brush, double trees) {
		floraQuotas[FLORA_GRASS] = grass;
		floraQuotas[FLORA_BRUSH] = brush;
		floraQuotas[FLORA_TREES] = trees;
	}

	/**
	 * write out an RPGMaker map
	 * @param filename of output file
	 */
	public boolean writeFile(String filename) {		
		try {
			FileWriter output = new FileWriter(filename);
			RPGMwriter w = new RPGMwriter(output);
			w.typeMap(typeMap);
			w.prologue(y_points,  x_points,  RPGMRule.tileSet);

			// produce the actual map of tiles
			w.startList("data", "[");

			// level 1 objects on the ground
			int baseTiles[][] = new int[y_points][x_points];
			tiles(baseTiles, 1);
			w.writeAdjustedTable(baseTiles, useSLOPE ? levels : null);

			// level 2 objects on the ground drawn over level 1
			tiles(baseTiles, 2);
			w.writeAdjustedTable(baseTiles, useSLOPE ? levels : null);

			// level 3 - foreground mountains/trees/structures (B/C object sets)
			stamps(baseTiles, 3);
			w.writeTable(baseTiles, false);

			// level 4 - background mountains/trees/structures (B/C object sets)
			stamps(baseTiles, 4);
			w.writeTable(baseTiles, false);

			// level 5 - shadows (only cast by walls)
			if (useSLOPE) {
				tiles(baseTiles, 5);
				w.writeTable(baseTiles,  false);
			} else
				w.writeTable(null, false);

			// level 6 - encounters ... to be created later
			w.writeTable(null, true);

			w.endList("],");	// end of DATA

			// SOMEDAY create transfer events at sub-map boundaries
			w.startList("events", "[\n");
			output.write("null,\n");
			output.write("null\n");
			w.endList("]");

			// terminate the file
			w.epilogue();
			output.close();
		} catch (IOException e) {
			System.err.println("ERROR - unable to write output file: " + filename);
			return false;
		}

		if (parms.debug_level > 0) {
			System.out.println("Exported(RPGMaker " + RPGMRule.ruleset + ") "  + x_points + "x" + y_points + " " + tile_size
					+ "M tiles from <" + String.format("%9.6f", lat) + "," + String.format("%9.6f", lon)
					+ ">");
			System.out.println("                             to file " + filename);
		}

		// SOMEDAY update MapInfo.json
		//	find it in the same directory as the map
		//	read it in
		//	look for an entry for the current map
		//	update it (w/location), adding it as necessary
		//	rewrite
		return true;
	}

	/**
	 * generate an export preview, mapping levels to colors
	 */
	public void preview(WhichMap chosen, Color colormap[]) {
		// fill in preview from the per-point attributes
		Color map[][] = new Color[y_points][x_points];
		for(int i = 0; i < y_points; i++)
			for(int j = 0; j < x_points; j++) {
				int l = levels[i][j];
				if (chosen == WhichMap.FLORAMAP) {
					if (typeMap[l] <= TerrainType.PASSABLE_WATER) {
						map[i][j] = waterColor(typeMap[l]);
					} else {
						int flora = floraTypes[i][j];
						map[i][j] = (flora <= 0) ? Color.GRAY : colormap[flora];
					}
				} else {
					map[i][j] = colormap[l];
				}
			}
		new PreviewMap("Export Preview (" +
				(chosen == WhichMap.FLORAMAP ? "flora" : "terrain") +
				")", map, 0);
	}

	/**
	 * search the loaded rules for a list of bidders eligible for this level
	 *
	 *	initialize the list of bidders and the ecotope for each
	 */
	void get_bidders(int level) {

		bidders = new RPGMRule[MAXRULES];
		bidder_ecotope = new int[MAXRULES];
		bidder_quota = new double[MAXRULES];
		numRules = 0;

		// enumerate all eligible bidding rules
		for( ListIterator<ResourceRule> it = ResourceRule.iterator(); it.hasNext();) {
			RPGMRule r = (RPGMRule) it.next();
			if (r.level != level)				// rule is not for this level
				continue;
			if (r.width > SPRITES_PER_ROW) {	// rule has impossibly wide tiles
				System.err.println("WARNING: rule " + r.ruleName + ", width=" + r.width + " > " + SPRITES_PER_ROW);
				continue;
			}
			bidders[numRules] = r;

			// get the numeric floral ecotope type for each competing rule
			if (r.floraType == null)
				bidder_ecotope[numRules] = ECOTOPE_ANY;
			else {
				if (r.floraType.equals("any") || r.floraType.equals("ANY"))
					bidder_ecotope[numRules] = ECOTOPE_ANY;
				else if (r.floraType.equals("green") || r.floraType.equals("GREEN"))
					bidder_ecotope[numRules] = ECOTOPE_GREEN;
				else if (r.floraType.equals("non-green") || r.floraType.equals("NON-GREEN"))
					bidder_ecotope[numRules] = ECOTOPE_NON_GREEN;
				else
					for(int i = 0; i < floraNames.length; i++)
						if (r.floraType.equals(floraNames[i])) {
							bidder_ecotope[numRules] = i;
							break;
						}
			}
			
			// get the tile quota for this rule's flora class
			bidder_quota[numRules] = 1.0;
			if (r.className != null) {
				if (r.className.equals("Grass"))
					bidder_quota[numRules] = floraQuotas[FLORA_GRASS];
				else if (r.className.equals("Brush"))
					bidder_quota[numRules] = floraQuotas[FLORA_BRUSH];
				else if (r.className.equals("Tree"))
					bidder_quota[numRules] = floraQuotas[FLORA_TREES];
			}

			numRules++;
		}
	}

	/**
	 * collect bids from eligible bidders, choose a winner
	 * @param	pre-allocated bidder (it can be reused)
	 * @param	level
	 * @param	row
	 * @param	col
	 */
	RPGMRule winner(int level, int row, int col) {
		// per point attributes for tile bidding
		int alt = 0, terrain = 0;
		double lapse = 0.0, depth = 0.0;
		double flux = 0.0, rain = 0.0;	// unused for RPGM Tile rules

		// collect the bids from each rule
		double best_bid = 0;
		RPGMRule winning_rule = null;
		for(int b = 0; b < numRules; b++) {
			RPGMRule r = bidders[b];
			r.justification = "OK";
			double bid = 0;

			// multi-tile rules only bid empty UL group corners
			if ((row % r.height) != 0 || (col % r.width) != 0)
				continue;
			
			// see if this bidder has a limited quota
			if (bidder_quota[b] < 1.0 && Math.random() > bidder_quota[b])
				continue;

			// give this bid a shot at every tile in the group
			int refused = 0;
			String tile_info = null;
			for(int dy = 0; dy < r.height && refused == 0; dy++)
				for(int dx = 0; dx < r.width && refused == 0; dx++) {
					// make sure we are entirely within the grid
					if (row + dy >= y_points || col + dx >= x_points) {
						refused++;
						continue;
					}
					
					double thisBid = 0;
					// collect all the attributes of this square
					alt = (int) parms.altitude(heights[row+dy][col+dx] - erode[row+dy][col+dx]);
					lapse = alt * parms.lapse_rate;
					depth = parms.height(depths[row+dy][col+dx]);
					terrain = typeMap[levels[row+dy][col+dx]];
					if (useSLOPE &&												// SLOPE rules enabled
							!TerrainType.isWater(terrain) &&					// is not water
							row + dy + 1 < y_points &&							// must have a south neighbor
							levels[row+dy][col+dx] > levels[row+dy+1][col+dx] &&// on a lower level
							!TerrainType.isWater(typeMap[levels[row+dy+1][col+dx]]) // that is not water
							)
						terrain = TerrainType.SLOPE;	// for Outside slope is a different TerrainType

					// rule/bid debugging will want to know all attributes of this tile
					if (parms.debug_level >= EXPORT_DEBUG)
						tile_info = "l" + level + "[" + (row+dy) + "," + (col+dx) + "]: " +
								" terrain=" + TerrainType.terrainType(terrain) +
								", flora=" + floraNames[floraTypes[row][col]] + 
								", alt=" + alt +
								String.format(", depth=%.2f", depth) + 
								String.format(", temp=%.1f-%.1f", Twinter - lapse, Tsummer - lapse); 

					// does our terrain type match the rule
					if (r.wrongTerrain(terrain)) {
						r.justification = "terain mismatch";
						refused++;
						// debug: multi-tile mismatches part way through
						if (dx + dy > 0 && parms.debug_level >= EXPORT_DEBUG)
							System.out.println(tile_info + " ... " + r.ruleName + " - TERAIN MISMATCH");
						continue;
					}
					// does our ecotope match the rule
					if (wrongEcotope(bidder_ecotope[b], floraTypes[row][col])) {
						r.justification = "ecotope mismatch" + 
								" (" + r.floraType + "!=" + floraNames[floraTypes[row+dy][col+dx]] + ")";
						refused++;
						// debug: multi-tile mismatches part way through
						if (dx + dy > 0 && parms.debug_level >= EXPORT_DEBUG)
							System.out.println(tile_info + " ... " + r.ruleName + " - ECOTOPE MISMATCH");
						continue;
					}
					else	// if bid fails, it will add its own justification
						thisBid = r.bid(alt, depth, flux, rain, Tmean - lapse, Tmean - lapse);

					// if full debug is enabled, log every bid for every tile
					if (r.debug || parms.debug_level >= EXPORT_DEBUG) {
						System.out.println(tile_info + " ... " + r.ruleName + 
									" bids " + thisBid + " (" + r.justification + ")");
					}

					// tell the tile bidder about each successful bid
					if (thisBid <= 0)
						refused++;
					else
						bid += thisBid;
				}

			// if every square made a positive bid, record the sum
			if (bid > best_bid && refused == 0) {
				best_bid = bid;
				winning_rule = r;
			}
		}

		if (winning_rule != null) {
			if (parms.debug_level >= EXPORT_DEBUG)
				System.out.println("    winner = " + winning_rule.ruleName + 
								   " (" + winning_rule.baseTile + ")");
			return winning_rule;
		}

		// every level 1 point should be claimed by some rule
		if (level == 1) {
			// there seems to be a hole in the rules
			System.err.println("NOBID l" + level + "[" + row + "," + col + "]: " +
					" terrain=" + TerrainType.terrainType(terrain) +
					", flora=" + floraNames[floraTypes[row][col]] + 
					", alt=" + alt +
					String.format(", depth=%.2f", depth) + 
					String.format(", temp=%.1f-%.1f", Twinter - lapse, Tsummer - lapse));
		}

		return null;
	}

	/**
	 * fill in the array of base tiles for the entire grid
	 * 
	 * @param grid ... array to be filled in
	 * @param level .. what level are we filling
	 */
	void tiles(int[][] grid, int level) {
		// level 5 shadow masks
		final int SHADOW_TL = 1;
		// final int SHADOW_TR = 2;
		final int SHADOW_BL = 4;
		// final int SHADOW_BR = 8;

		// assemble a list of bidders (and their ecotopes) for this level
		get_bidders(level);

		for (int i = 0; i < y_points; i++)
			for (int j = 0; j < x_points; j++) {
				grid[i][j] = 0;		// start out empty
				RPGMRule r = winner(level, i, j);
				if (r != null)
					grid[i][j] = r.baseTile;
				else if (useSLOPE && level == 5) {
					int terrain = typeMap[levels[i][j]];
					if (TerrainType.isWater(terrain))
						continue;			// no shadows on water
					if (terrain == TerrainType.SLOPE)
						continue;			// no shadows on walls
					if (j == 0 || levels[i][j-1] <= levels[i][j])
						continue;			// shadows on right of slopes
					grid[i][j] = SHADOW_TL + SHADOW_BL;
				}
			}
	}

	/**
	 * fill in the array of base tiles with multi-tile stamps
	 * 
	 * @param grid ... the array to be filled in
	 * @param level .. the level being filled in
	 */
	void stamps(int[][] grid, int level) {	
		// assemble a list of bidders (and their ecotopes) for this level
		get_bidders(level);

		// the grid starts out completely empty
		for (int i = 0; i < y_points; i++)
			for (int j = 0; j < x_points; j++)
				grid[i][j] = 0;

		// now try to populate it with stamps
		boolean groupCorrections = false;
		for (int i = 0; i < y_points; i++)
			for (int j = 0; j < x_points; j++) {
				// groups can only claim empty squares
				if (grid[i][j] != 0)
					continue;

				// if a rule wants the whole group ...
				RPGMRule r = winner(level, i, j);
				if (r != null) {
					for(int dy = 0; dy < r.height; dy++)
						for(int dx = 0; dx < r.width; dx++)
							grid[i+dy][j+dx] = r.baseTile + (dy * SPRITES_PER_ROW) + dx;
					if (r.altTile > 0)
						groupCorrections = true;
				}
			}

		/*
		 * Analogous to the auto-tiling adjustments in L1/L2 tiles, some
		 * L3/L4 stamps have alternate groups that can be used to fill in
		 * between identical tiles ... but we have to do that for ourselves.
		 */
		if (!groupCorrections)
			return;
		int[][] corrections = new int[y_points][x_points];
		for (int i = 0; i < y_points; i++)
			for (int j = 0; j < x_points; j++) {
				if (grid[i][j] == 0)
					continue;

				// find the rule that generated this tile
				RPGMRule r = null;
				for(int b = 0; b < numRules; b++) 
					if (bidders[b].baseTile == grid[i][j]) {
						r = bidders[b];
						break;
					}

				// we only care about 1x2 and 2x2 stamps w/alternate tiles
				if (r == null || r.altTile == 0 || r.height != 2 || r.width > 2)
					continue;

				// note the Cartesian neighbors
				boolean top = (i >= r.height) && (grid[i-r.height][j] == r.baseTile);
				boolean bot = (i < y_points - r.height) && (grid[i+r.height][j] == r.baseTile);
				boolean left = (j >= r.width) && (grid[i][j-r.width] == r.baseTile);
				boolean right = (j < x_points - r.width) && (grid[i][j+r.width] == r.baseTile);

				if (top) {
					if (r.width == 2 && left && grid[i-r.height][j-r.width] == r.baseTile)
						corrections[i][j] = r.altTile + SPRITES_PER_ROW;
					if (r.width == 2 && right && grid[i-r.height][j+r.width] == r.baseTile)
						corrections[i][j+1] = r.altTile + SPRITES_PER_ROW + 1;
					if (r.width == 1)
						corrections[i][j] = r.altTile;
				}
				if (bot) {
					if (r.width == 2 && left && grid[i+r.height][j-r.width] == r.baseTile)
						corrections[i+1][j] = r.altTile;
					if (r.width == 2 && right && grid[i+r.height][j+r.width] == r.baseTile)
						corrections[i+1][j+1] = r.altTile + 1;
					if (r.width == 1)
						corrections[i+1][j] = r.altTile + SPRITES_PER_ROW;
				}
			}

		// copy the corrections into the grid
		for (int i = 0; i < y_points; i++)
			for (int j = 0; j < x_points; j++)
				if (corrections[i][j] != 0)
					grid[i][j] = corrections[i][j];
	}

	/**
	 * determine whether or not a tile ecotope matches a rule
	 * @param rule_ecotope
	 * @param tile_ecotope
	 * @return
	 */
	boolean wrongEcotope(int rule_ecotope, int tile_ecotope) {
		switch(rule_ecotope) {
		case ECOTOPE_ANY:
			return false;
		case ECOTOPE_GREEN:
			return !floraGreen[tile_ecotope];
		case ECOTOPE_NON_GREEN:
			return floraGreen[tile_ecotope];
		default:
			return rule_ecotope != tile_ecotope;
		}
	}

	/**
	 * aggregate slope
	 * @param row (tile) within the export region
	 * @param col (tile) within the export region
	 * @return aggregate slope (dZdTILE) of that tile
	 */
	public double slope(int row, int col) {
		double z0 = heights[row][col] - erode[row][col];
		double zx1 = (col > 0) ? heights[row][col-1] - erode[row][col-1] :
			heights[row][col+1] - erode[row][col+1];
		double zy1 = (row > 0) ? heights[row-1][col] - erode[row-1][col] :
			heights[row+1][col] - erode[row+1][col];
		double dz = Math.sqrt((z0-zx1)*(z0-zx1) + (z0-zy1)*(z0-zy1));
		return Math.abs(parms.altitude(dz) / tile_size);
	}

	/**
	 * slope (upwards to the east)
	 * @param row (tile) within the export region
	 * @param col (tile) within the export region
	 * @return slope upwards to the east
	 */
	public double dZdX(int row, int col) {
		if (col == x_points - 1)
			col--;
		double dz = (heights[row][col+1] - erode[row][col+1]) - (heights[row][col] - erode[row][col]) ;
		double dx = tile_size;
		return dz/dx;
	}

	/**
	 * slope (upwards to the south)
	 * @param row (tile) within the export region
	 * @param col (tile) within the export region
	 * @return slope upwards to the south
	 */
	public double dZdY(int row, int col) {
		if (row == y_points - 1)
			row--;
		double dz = (heights[row+1][col] - erode[row+1][col]) - (heights[row][col] - erode[row][col]) ;
		double dy = tile_size;
		return dz/dy;
	}

	/**
	 * compass orientation of face
	 * @param row (tile) within the export region
	 * @param col (tile) within the export region
	 * @return compass orientation (0-359) of face
	 */
	public double direction(int row, int col) {
		double dzdy = dZdY(row, col);
		double dzdx = dZdX(row, col);
		double theta = Math.atan(-dzdx/dzdy) * 180 /Math.PI;
		if (dzdy < 0)
			return theta + 180;
		if (dzdx > 0)
			return theta + 360;
		return theta;
	}



	/**
	 * Set the size of a single tile
	 * @param meters real-world width of a tile
	 */
	public void tileSize(int meters) {
		this.tile_size = meters;
	}

	/**
	 * Set the lat/lon of the region being exported
	 * @param lat real world latitude of map center
	 * @param lon real world longitude of map center
	 */
	public void position(double lat, double lon) {
		this.lat = lat;
		this.lon = lon;
	}

	/**
	 * Set seasonal temperature range for region being exported
	 * @param meanTemp	mean (all year) temperature
	 * @param meanSummer	mean (summer) temperature
	 * @param meanWinter	mean (winter) temperature
	 */
	public void temps(double meanTemp, double meanSummer, double meanWinter) {
		this.Tmean = meanTemp;
		this.Tsummer = meanSummer;
		this.Twinter = meanWinter;
	}

	/**
	 * Up-load the altitude of every tile
	 * @param heights	height (in meters) of every point
	 */
	public void heightMap(double[][] heights) {
		this.heights = heights;
	}

	/**
	 * initialize the bucketized level map
	 * @param levels ... level for every map point
	 * @param typeMap ... terrain type of each level
	 */
	public void levelMap(int[][] levels, int[] typeMap ) {
		this.levels = levels;
		this.typeMap = typeMap;
	}

	/**
	 * Up-load the net erosion/deposition for every tile
	 * @param erode	per point height (in meters) of soil lost to erosion
	 * 		negative means sedimentqation
	 */
	public void erodeMap(double[][] erode) {
		this.erode = erode;
	}

	/**
	 * Up-load the annual rainfall for every tile
	 * @param rain	per point depth (in meters) of annual rainfall
	 */
	public void rainMap(double[][] rain) {
		//this.rain = rain;
	}

	/**
	 * Up-load the soil type for every tile
	 * @param soil - per point soil type
	 * @param names - per type name strings
	 */
	public void soilMap(double[][] soil, String[] names) {
		this.soil = soil;
	}

	/**
	 * Up-load the surface-water-depth for every tile
	 * @param depths - per point depth of water
	 */
	public void waterMap(double[][] depths) {
		this.depths = depths;
	}

	/**
	 * Up-load the (integer) flora type for every tile
	 * @param flora - per point flora type
	 * @param names - per type name strings
	 */
	public void floraMap(double[][] flora, String[] names) {
		// ecotope valued do not interpolate, they are discrete integers
		this.floraTypes = new int[flora.length][flora[0].length];
		for(int y = 0; y < flora.length; y++)
			for(int x = 0; x < flora[0].length; x++)
				floraTypes[y][x] = (int) flora[y][x];

		// figure out which ecotope types are green
		this.floraNames = names;
		floraGreen = new boolean[names.length];
		for(int i = 0; i < names.length; i++) {
			String ecotope = names[i];
			if (ecotope == null || ecotope.equals("NONE"))
				continue;
			if (ecotope.equals("Desert") || ecotope.equals("Alpine"))
				continue;
			floraGreen[i] = true;
		}
	}
	
	/**
	 * Up-load the fauna type for every tile
	 * @param fauna - per point fauna type
	 */
	public void faunaMap(double[][] soil, String[] names) {
		// this.fauna = fauna;
		// this.faunaNames = names;
	}
	
	/**
	 * water colors for flora previews
	 * @param terrainType
	 */
	public Color waterColor(int terrainType) {

		switch( terrainType ) {
		case TerrainType.DEEP_WATER:
			return new Color(0, 0, WATER_SHADE_MIN);
		case TerrainType.SHALLOW_WATER:
			return new Color(0, 0, WATER_SHADE_DELTA);
		case TerrainType.PASSABLE_WATER:
			return new Color(0, 0, 255);
		}
		return Color.BLACK;
	}
}
