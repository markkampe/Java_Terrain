package worldBuilder;

import java.awt.Color;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ListIterator;
import java.util.Random;

/**
 * exporter that creates an RPGMaker map
 */
public class OverworldTiler implements Exporter {

	private static final int MAXRULES = 20;
	private static final int SPRITES_PER_ROW = 8;
	private static final int EXPORT_DEBUG = 3;
	
	private Parameters parms;	// general parameters
	private TileRules rules;	// tile selection rules
	private Random random;		// random # generator
	
	// physical map parameters
	private int x_points; 		// width of map (in points)
	private int y_points; 		// height of map (in points)flushflush
	private int tile_size; 		// tile size (in meters)
	private double lat;			// latitude
	private double lon;			// longitude

	// tile selection parameters
	//private double Tmean; 		// mean temperature (degC)
	private double Tsummer; 	// mean summer temperature (degC)
	private double Twinter; 	// mean winter temperature (degC)
	//private double[][] rain;	// per point rainfall (meters)
	private double[][] heights;	// per point height (meters)
	private double[][] erode;	// per point erosion (meters)
	private double[][] hydration; // per point water depth (meters)
	private double[][] soil;	// per point soil type
	private int[][] levels;		// per point terrain level
	private double minHeight;	// lowest altitude in export
	private double maxHeight;	// highest altitude in export
	private double minDepth;	// shallowest water in export
	private double maxDepth;	// deepest water in export
	
	/**
	 * create a new output writer
	 * 
	 * @param filename ... name of output file
	 * @param tileRules ... name of rules file
	 * @param map width (cells)
	 * @param map height (cells)
	 */
	public OverworldTiler(String tileRules, int width, int height) {
		this.parms = Parameters.getInstance();
		this.x_points = width;
		this.y_points = height;
		
		// read in the rules for this tileset
		this.rules = new TileRules(tileRules);
	}

	/**
	 * write out an RPGMaker map
	 */
	public boolean writeFile(String filename) {
		random = new Random((int) (lat * lon * 1000));
		try {
			FileWriter output = new FileWriter(filename);
			RPGMwriter w = new RPGMwriter(output);
			w.prologue(y_points,  x_points,  rules.tileset);
		
			// produce the actual map of tiles
			w.startList("data", "[");
			
			// level 1 objects on the ground
			int l[][] = new int[y_points][x_points];
			tiles(l, 1);
			w.writeAdjustedTable(l);
			
			// level 2 objects on the ground drawn over level 1
			tiles(l, 2);
			w.writeAdjustedTable(l);
			
			// level 3 - foreground mountains/trees/structures (B/C object sets)
			stamps(l, 3);
			w.writeTable(l, false);
			
			// level 4 - background mountains/trees/structures (B/C object sets)
			stamps(l, 4);
			w.writeTable(l, false);
			
			// level 5 - shadows ... come later (w/walls)
			int zeroes[][] = new int[y_points][x_points];
			w.writeTable(zeroes, false);
			
			// level 6 - encounters ... to be created later
			w.writeTable(zeroes, true);
			
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
			System.out.println("Exported(RPGMaker Overworld) "  + x_points + "x" + y_points + " " + tile_size
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
		if (chosen == WhichMap.HEIGHTMAP) {
			Color pMap[][] = new Color[y_points][x_points];
			for(int i = 0; i < y_points; i++)
				for(int j = 0; j < x_points; j++) {
					pMap[i][j] = colormap[levels[i][j]];
				}
			new PreviewMap("Export Preview (terrain)", pMap);
		}
	}
	
	/*
	 * L1 is the color/texture of the ground, L2 is on the ground
	 */
	void tiles(int[][] grid, int level) {	
		
		TileRule bidders[] = new TileRule[MAXRULES];
		int numRules = 0;
		for( ListIterator<TileRule> it = rules.rules.listIterator(); it.hasNext();) {
			TileRule r = it.next();
			if (r.level == level)
				bidders[numRules++] = r;
		}
		Bidder bidder = new Bidder(numRules);
		
		for (int i = 0; i < y_points; i++)
			for (int j = 0; j < x_points; j++) {
				grid[i][j] = 0;
				
				// collect all the attributes of this square
				int alt = (int) parms.altitude(heights[i][j] - erode[i][j]);
				double lapse = alt * parms.lapse_rate;
				double hydro = hydration[i][j];
				double slope = slope(i,j);
				double face = direction(i, j);
				double soilType = soil[i][j];
				int terrain = levels[i][j];
				if (parms.debug_level >= EXPORT_DEBUG)
					System.out.println("l" + level + "[" + i + "," + j + "]: " +
						" alt=" + alt +
						String.format(", hydro=%.2f", hydro) + 
						String.format(", temp=%.1f-%.1f", Twinter - lapse, Tsummer - lapse) +
						String.format(", soil=%.1f",  soilType) + 
						String.format(", slope=%.3f", slope));
				// collect the bids from each rule
				bidder.reset();
				int bids = 0;
				for(int b = 0; b < numRules; b++) {
					TileRule r = bidders[b];
					int bid = r.bid(terrain, alt, hydro, Twinter - (int) lapse, Tsummer - (int) lapse, 
							soilType, slope, face);
					if (parms.rule_debug != null && parms.rule_debug.equals(r.ruleName))
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
				} else if (level == 1) {
					// there seems to be a hole in the rules
					System.err.println("NOBID l" + level + "[" + i + "," + j + "]: " +
							" ter=" + TerrainType.terrainType(terrain) +
							", alt=" + alt +
							String.format(", hydro=%.2f", hydro) + 
							String.format(", temp=%.1f-%.1f", Twinter - lapse, Tsummer - lapse) +
							String.format(", soil=%.1f",  soilType) + 
							String.format(", slope=%.3f", slope));
				}
			}
	}
	
	/*
	 * L3 and L4 can use (multi-cell) stamps
	 */
	void stamps(int[][] grid, int level) {	
		
		// start with all zeroes	
		for (int i = 0; i < y_points; i++)
			for (int j = 0; j < x_points; j++)
				grid[i][j] = 0;

		// enumerate the applicable rules
		TileRule bidders[] = new TileRule[MAXRULES];
		int numRules = 0;
		for( ListIterator<TileRule> it = rules.rules.listIterator(); it.hasNext();) {
			TileRule r = it.next();
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
					TileRule r = bidders[b];
					
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
							// collect all the attributes of this square
							int alt = (int) parms.altitude(heights[i+dy][j+dx] - erode[i+dy][j+dx]);
							double lapse = alt * parms.lapse_rate;
							double hydro = hydration[i+dy][j+dx];
							double slope = slope(i+dy,j+dx);
							double face = direction(i+dy, j+dx);
							double soilType = soil[i+dy][j+dx];
							int terrain = levels[i][j];
							if (parms.debug_level >= EXPORT_DEBUG && b == 0)
								System.out.println("l" + level + "[" + (i+dy) + "," + (j+dx) + "]: " +
									" alt=" + alt + ", hyd=" + 
									String.format("%.2f", hydro) + 
									String.format(", temp=%.1f-%.1f", Twinter - lapse, Tsummer - lapse) +
									String.format(", soil=%.1f",  soilType) + 
									String.format(", slope=%03.0f", face));
							
							int thisBid = r.bid(terrain, alt, hydro , Twinter - lapse, Tsummer - lapse, soilType, slope, face);
							if (parms.rule_debug != null && parms.rule_debug.equals(r.ruleName))
								System.out.println(r.ruleName + "[" + (i+dy) + "," + (j+dx) + "] (" + 
										r.baseTile + ") bids " + thisBid + " (" + r.justification + ")");
							if (thisBid == 0)
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
						TileRule r = bidders[winner];
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
					TileRule r = null;
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
	
	
	/*
	 * return the slope at a point
	 */
	private double slope(int row, int col) {
		double z0 = heights[row][col] - erode[row][col];
		double zx1 = (col > 0) ? heights[row][col-1] - erode[row][col-1] :
								 heights[row][col+1] - erode[row][col+1];
		double zy1 = (row > 0) ? heights[row-1][col] - erode[row-1][col] :
								 heights[row+1][col] - erode[row+1][col];
		double dz = Math.sqrt((z0-zx1)*(z0-zx1) + (z0-zy1)*(z0-zy1));
		return Math.abs(parms.altitude(dz) / tile_size);
	}
	
	/**
	 * slope upwards to the east
	 */
	public double dZdX(int row, int col) {
		if (col == x_points - 1)
			col--;
		double dz = (heights[row][col+1] - erode[row][col+1]) - (heights[row][col] - erode[row][col]) ;
		double dx = tile_size;
		return dz/dx;
	}

	/**
	 * slope upwards to the south
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
	 * @return
	 */
	/**Notes
	 * compass orientation of face
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
	
	

	public void tileSize(int meters) {
		this.tile_size = meters;
	}

	public void position(double lat, double lon) {
		this.lat = lat;
		this.lon = lon;
	}

	public void temps(double meanTemp, double meanSummer, double meanWinter) {
		//this.Tmean = meanTemp;
		this.Tsummer = meanSummer;
		this.Twinter = meanWinter;
	}

	public void heightMap(double[][] heights) {
		this.heights = heights;
	}
	
	/**
	 * initialize the bucketized level map
	 * @param land percentile to level map
	 * @param water percentile to level map
	 * @param slope percentile to level map
	 * @param level to TerrainType map
	 */
	public void levelMap(int [] landMap, int[] waterMap, int[] slopeMap, int[] terrainMap) {

		// ascertain the slope at every point
		double slopes[][] = new double[y_points][x_points];
		double minSlope = 666;
		double maxSlope = 0;
		for(int i = 0; i < y_points; i++)
			for(int j = 0; j < x_points; j++) {
				slopes[i][j] = slope(i,j);
				double m = (slopes[i][j] >= 0) ? slopes[i][j] : -slopes[i][j];
				if (m < minSlope)
					minSlope = m;
				if (m > maxSlope)
					maxSlope = m;
			}
		
		// ascertain the range of altitudes, depths, and slopes
		double aRange = (maxHeight > minHeight) ? maxHeight - minHeight : 0.000001;
		double dRange = (maxDepth > minDepth) ? maxDepth - minDepth : 1;
		double mRange = (maxSlope > minSlope) ? maxSlope - minSlope : 1;
		
		// use the supplied maps to characterize each tile in the grid
		levels = new int[y_points][x_points];
		for(int i = 0; i < y_points; i++)
			for(int j = 0; j < x_points; j++) {
				if (hydration[i][j] < 0) {	// under water
					double h = -hydration[i][j];
					double pctile = 99 * (h - minDepth) / dRange;
					if ((int) pctile > 99)	// FIX debug code
						System.out.println("h=" + h + " - mindepth=" + minDepth + " / dRange=" + dRange);
					levels[i][j] = terrainMap[waterMap[(int) pctile]];
					continue;
				} else {	// land form (based on height and slope)
					double a = heights[i][j];
					double pctile = 99 * (a - minHeight) / aRange;
					int aType = terrainMap[landMap[(int) pctile]];
					
					double m = slopes[i][j];
					pctile = 99 * (m - minSlope) / mRange;
					int mType = terrainMap[slopeMap[(int) pctile]];
					
					// choose the least mountainous of the two land forms
					levels[i][j] = (mType < aType) ? mType : aType;
				}
			}
	}
	
	public void erodeMap(double[][] erode) {
		this.erode = erode;
	}

	public void rainMap(double[][] rain) {
		//this.rain = rain;
	}

	public void soilMap(double[][] soil) {
		this.soil = soil;
	}

	public void waterMap(double[][] hydration) {
		this.hydration = hydration;
		
		// figure out minimum and maximum heights/depths in the region
		minDepth = 666666;
		minHeight = 666;
		maxDepth = 0;
		maxHeight = 0;
		for (int i = 0; i < hydration.length; i++)
			for (int j = 0; j < hydration[0].length; j++) {
				double h = hydration[i][j];
				if (h < 0) {
					if (-h < minDepth)
						minDepth = -h;
					else if (-h > maxDepth)
						maxDepth = -h;
				} else {
					if (heights[i][j] < minHeight)
						minHeight = heights[i][j];
					if (heights[i][j] > maxHeight)
						maxHeight = heights[i][j];
				}
			}
	}
}