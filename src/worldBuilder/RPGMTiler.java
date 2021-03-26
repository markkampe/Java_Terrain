package worldBuilder;

import java.awt.Color;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ListIterator;
import java.util.Random;

/**
 * exporter to write world map as tile numbers in RPGMaker levels
 */
public class RPGMTiler implements Exporter {

	private static final int MAXRULES = 40;
	private static final int SPRITES_PER_ROW = 8;
	private static final int EXPORT_DEBUG = 3;
	
	private static final boolean BY_CLASS_ONLY = true;	// FIX full RPGM Tile bidding
	
	private Parameters parms;	// general parameters
	RPGMRule rules;				// tile placement rules 
	private Random random;		// random # generator
	
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

	/** per point height (meters)	*/
	public double[][] heights;
	/** per point erosion/depostion (meters)	*/
	public double[][] erode;
	/** per point water depth (meters)	*/
	public double[][] depths;
	/** per point soil type	*/
	public double[][] soil;
	/** per point terrain level	*/
	public int[][] levels;
	/** per point flora type	*/
	public int[][] floraTypes;
	/** mapping from levels to TerrainTypes	*/
	public int[] typeMap;
	/** map from flora types to class names	*/
	public String[] floraNames;
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
	}

	/**
	 * write out an RPGMaker map
	 * @param filename of output file
	 */
	public boolean writeFile(String filename) {		
		random = new Random((int) (lat * lon * 1000));
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
					if (typeMap[l] <= TerrainType.PASSABLE_WATER)
						map[i][j] = Color.BLUE;
					else {
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
		
		// assemble a list of bidders and their ecotopes for this level
		RPGMRule bidders[] = new RPGMRule[MAXRULES];
		int bidder_ecotope[] = new int[MAXRULES];
		int numRules = 0;
		for( ListIterator<ResourceRule> it = ResourceRule.iterator(); it.hasNext();) {
			RPGMRule r = (RPGMRule) it.next();
			if (r.level != level)
				continue;
			bidders[numRules] = r;
			for(int i = 0; i < floraNames.length; i++)
				if (r.className != null && floraNames[i] != null && r.className.equals(floraNames[i])) {
					bidder_ecotope[i] = i;
					break;
				}
			numRules++;
		}
		Bidder bidder = new Bidder(numRules);
		
		for (int i = 0; i < y_points; i++)
			for (int j = 0; j < x_points; j++) {
				grid[i][j] = 0;
				
				// collect all the attributes of this square
				int alt = (int) parms.altitude(heights[i][j] - erode[i][j]);
				double lapse = alt * parms.lapse_rate;
				double depth = depths[i][j];
				double slope = slope(i,j);
				double face = direction(i, j);
				double soilType = soil[i][j];
				double flux = 0.0;	// unused for RPGM Tile rules
				double rain = 0.0;	// unused for RPGM Tile rules
				int terrain = typeMap[levels[i][j]];
				
				// south slopes may be a special terrain type
				if (useSLOPE &&								// SLOPE rules enabled
						!TerrainType.isWater(terrain) && 	// is not water
						i < y_points - 1 && 				// must have a south neighbor
						levels[i][j] > levels[i + 1][j] && 	// on a lower level
						!TerrainType.isWater(typeMap[levels[i + 1][j]]) // that is not water
						)
					terrain = TerrainType.SLOPE;
				
				if (parms.debug_level >= EXPORT_DEBUG)
					System.out.println("l" + level + "[" + i + "," + j + "]: " +
						" terrain=" + TerrainType.terrainType(terrain) +
						", class=" + floraNames[floraTypes[i][j]] + 
						", alt=" + alt +
						String.format(", depth=%.2f", depth) + 
						String.format(", temp=%.1f-%.1f", Twinter - lapse, Tsummer - lapse) +
						String.format(", soil=%.1f",  soilType) + 
						String.format(", slope=%.3f", slope));
				// collect the bids from each rule
				bidder.reset();
				int bids = 0;
				for(int b = 0; b < numRules; b++) {
					RPGMRule r = bidders[b];
					r.justification = "OK";
					double bid = 0;
					if (r.wrongTerrain(terrain))
						r.justification = "Terain mismatch";
					else if (bidder_ecotope[b] != 0 && bidder_ecotope[b] != floraTypes[i][j])
						r.justification = "Class mismatch";
					else
						bid = r.bid(alt, depth, flux, rain, Tmean - lapse, Tmean - lapse, soilType);

					if (r.debug)
						System.out.println(r.ruleName + "[" + i + "," + j + "] (" + r.baseTile + 
								") bids " + bid + " (" + r.justification + ")");
					if (bid > 0) {
						bidder.bid(r.baseTile, bid);
						bids++;
					}
				}
				if (bids != 0) {
					int winner = bidder.winner(random.nextFloat());
					grid[i][j] = winner;
					if (parms.debug_level >= EXPORT_DEBUG)
						System.out.println("    winner = " + winner);
				} else if (level == 5) {	// shadows are in this level
					// TODO ... should these be Outside (useSLOPE) only?
					if (TerrainType.isWater(terrain))
						continue;			// no shadows on water
					if (terrain == TerrainType.SLOPE)
						continue;			// no shadows on walls
					if (j == 0 || levels[i][j-1] <= levels[i][j])
						continue;			// shadows on right of slopes
					grid[i][j] = SHADOW_TL + SHADOW_BL;
				} else if (level == 1) {
					// there seems to be a hole in the rules
					System.err.println("NOBID l" + level + "[" + i + "," + j + "]: " +
							" terrain=" + TerrainType.terrainType(terrain) +
							", class=" + floraNames[floraTypes[i][j]] + 
							", alt=" + alt +
							String.format(", depth=%.2f", depth) + 
							String.format(", temp=%.1f-%.1f", Twinter - lapse, Tsummer - lapse) +
							String.format(", soil=%.1f",  soilType) + 
							String.format(", slope=%.3f", slope));
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
		
		// start with all zeroes	
		for (int i = 0; i < y_points; i++)
			for (int j = 0; j < x_points; j++)
				grid[i][j] = 0;

		// enumerate the applicable rules
		RPGMRule bidders[] = new RPGMRule[MAXRULES];
		int numRules = 0;
		for( ListIterator<ResourceRule> it = ResourceRule.iterator(); it.hasNext();) {
			RPGMRule r = (RPGMRule) it.next();
			if (r.level != level)
				continue;
			if (r.width <= SPRITES_PER_ROW) {
				bidders[numRules++] = r;
			} else
				System.err.println("WARNING: rule " + r.ruleName + ", width=" + r.width + " > " + SPRITES_PER_ROW);
		}
		Bidder bidder = new Bidder(numRules);
		
		// now try to populate it with tiles
		boolean groupCorrections = false;
		for (int i = 0; i < y_points; i++)
			for (int j = 0; j < x_points; j++) {
				// make sure top-left hasn't already been filled
				if (grid[i][j] != 0)
					continue;
				
				// collect the bids for each applicable rule
				bidder.reset();
				int bids = 0;
				for( int b = 0; b < numRules; b++ ) {
					int bid = 0;
					RPGMRule r = bidders[b];
					
					// corrections require all stamps to be height/width aligned
					if ((i % r.height) != 0 || (j % r.width) != 0)
							continue;
					
					// give this stamp a chance to bid on every square in this area
					boolean noBid = false;
					for(int dy = 0; dy < r.height && !noBid; dy++)
						for(int dx = 0; dx < r.width && !noBid; dx++) {
							// make sure this square is empty and legal
							if (i + dy >= y_points || j + dx >= x_points || grid[i + dy][j + dx] != 0) {
								noBid = true;
								continue;
							}
							// FIX reconcile stamp and tile bid/debug
							// make sure this square meets the rule terrain type
							int terrain = typeMap[levels[i+dy][j+dx]];
							if (useSLOPE && i+dy > 0 && !TerrainType.isWater(terrain) && levels[i+dy-1][j+dx] > levels[i+dy][j+dx])
								terrain = TerrainType.SLOPE;
							if (r.wrongTerrain(terrain)) {
								noBid = true;
								continue;
							}
							
							// make sure this square matches the rule flora type
							if (r.wrongFlora(floraNames[floraTypes[i+dy][j+dx]])) {
								noBid = true;
								continue;
							}
							
							// collect the attributes of this square
							int alt = (int) parms.altitude(heights[i+dy][j+dx] - erode[i+dy][j+dx]);
							double lapse = alt * parms.lapse_rate;
							double depth = depths[i+dy][j+dx];
							double slope = slope(i+dy,j+dx);
							double face = direction(i+dy, j+dx);
							double soilType = soil[i+dy][j+dx];
							double flux = 0.0;	// not used for RPGM tile rules
							double rain = 0.0;	// not used for RPGM tile rules
							
							if (parms.debug_level >= EXPORT_DEBUG && b == 0)
								System.out.println("l" + level + "[" + (i+dy) + "," + (j+dx) + "]: " +
									" terrain=" + TerrainType.terrainType(terrain) +
									", alt=" + alt + ", hyd=" + 
									String.format("%.2f", depth) + 
									String.format(", temp=%.1f-%.1f", Twinter - lapse, Tsummer - lapse) +
									String.format(", soil=%.1f",  soilType) + 
									String.format(", slope=%03.0f", face));
							
							double thisBid = r.bid(alt, depth , flux, rain, Tmean - lapse, Tmean - lapse, soilType);
							if (r.debug)
								System.out.println(r.ruleName + "[" + (i+dy) + "," + (j+dx) + "] (" + 
										r.baseTile + ") bids " + thisBid + " (" + r.justification + ")");
							if (thisBid <= 0)
								noBid = true;
							else
								bid += thisBid;
						}
					
					// place the bid for this rule
					if (!noBid) {
						bidder.bid(b, bid);
						bids++;
					}
				}
			
				// find and install the winner
				if (bids > 0) {
						int winner = bidder.winner(random.nextFloat());
						RPGMRule r = bidders[winner];
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
	 * Up-load the flora type for every tile
	 * @param flora - per point flora type
	 * @param names - per type name strings
	 */
	public void floraMap(double[][] flora, String[] names) {
		this.floraTypes = new int[flora.length][flora[0].length];
		for(int y = 0; y < flora.length; y++)
			for(int x = 0; x < flora[0].length; x++)
				floraTypes[y][x] = (int) flora[y][x];
		
		this.floraNames = names;
	}
	
	/**
	 * Up-load the flora assignments for every tile
	 * @param flora assignments per point
	 * @param names of flora classes
	 */
	public void rpgmFloraMap(int[][] flora, String[] names) {
		this.floraTypes = flora;
		this.floraNames = names;
	}
}
