package worldBuilder;

import java.awt.Color;
import java.util.LinkedList;


/**
 * parameters and bids for a single RPGMaker tile
 */
public class TileRule {
	
	/** list of all ingested rules	*/
	public static LinkedList<TileRule> rules;
	
	/** name of this rule and its class	*/
	public String ruleName, className;

	/** RPGMaker map level	*/
	public int level;
	/** RPGMaker tile set	*/
	public int tileSet;
	/** base tile number for this rule	*/
	public int baseTile;	// base tile for this rule
	/** base tile number for what we might surround*/
	public int altTile;		// base tile for surrounded version
	/** dimensions (if this is a stamp)	*/
	public int height, width;
	/** number of neighbors for auto-tiling	*/
	public int neighbors;
	
	/** TerrainType for this rule	*/
	public int terrain;
	/** Altitude ranges for this rule	*/
	public int minAltitude, maxAltitude;
	/** Hydration and water-depth ranges for this rule	*/
	public double minDepth, maxDepth, minHydro, maxHydro;
	/** Temperature range for this rule	*/
	public double minTemp, maxTemp;
	/** slope/face ranges for this rule	*/
	public double minSlope, maxSlope, minFace, maxFace;
	/** soil type range for this rule	*/
	public double minSoil, maxSoil;
	/** is there flexibility in these ranges	*/
	public boolean flexRange;
	/** taper bids as we move away from mid-range	*/
	public boolean taperedBid;
	
	/** does this tile represent an impassable barrier	*/
	public boolean barrier;
	
	/** how high should this rule bid	*/
	public int vigor;
	
	/** what color should this render as in previews	*/
	public Color previewColor;
	
	/** Debug: trace bids from this rule	*/
	public boolean debug;			// trace bids from this rule

	/** Debug: explain the basis for the last bid	*/
	public String justification;
	
	private static Parameters parms = Parameters.getInstance();
	private static final double IMPOSSIBLE = -666.0;
	
	/**
	 * create a new rule
	 * @param name of this rule
	 * @param tileset RPGMaker tile set ID
	 * @param level RPGMaker level to which this rule applies
	 * @param base tile number for this rule
	 */
	public TileRule(String name, int tileset, int level, int base) {
		this.ruleName = name;
		this.tileSet = tileset;
		this.level = level;
		this.baseTile = base;
		this.height = 1;
		this.width = 1;
		this.altTile = 0;
		this.debug = false;
		this.barrier = false;
		previewColor = null;
		
		// default values will meet any terrestrial conditions
		className = null;
		terrain = TerrainType.NONE;
		minAltitude = -parms.alt_max;
		maxAltitude = parms.alt_max;
		minDepth = 0;
		maxDepth = 0;
		minHydro = 0.0;
		maxHydro = 1.0;
		minTemp = -60;
		maxTemp = 70;
		minSlope = -100;
		maxSlope = 100;
		minSoil = 0;
		maxSoil = 5;
		minFace = 0;
		maxFace = 360;
		flexRange = false;
		taperedBid = false;
		vigor = 16;
	}
	
	/**
	 * dump out a rule (for debugging)
	 * @param prefix ... leading blanks
	 */
	public void dump( String prefix ) {
		System.out.print(prefix + "Rule: " + ruleName + ", level=" + level + ", base=" + baseTile);
		if (altTile > 0)
			System.out.print(", surround=" + altTile);
		if (height > 1 || width > 1)
			System.out.print(", stamp=" + width + "x" + height);
		System.out.println("");
		if (className != null)
			System.out.println(prefix + "      " + "class:   " + className);
		System.out.println(prefix + "      " + "terrain: " + TerrainType.terrainType(terrain));
		System.out.println(prefix + "      " + "alt:     " + minAltitude + "-" + maxAltitude);
		System.out.println(prefix + "      " + "depth:   " + (int) minDepth + "-" + (int) maxDepth);
		System.out.println(prefix + "      " + "hydro:   " + String.format("%.1f", minHydro) + "-" + String.format("%.1f", maxHydro));
		System.out.println(prefix + "      " + "temp:    " + minTemp + "-" + maxTemp);
		System.out.println(prefix + "      " + "soil:    " + String.format("%.1f", minSoil) + "-" + String.format("%.1f", maxSoil));
		System.out.println(prefix + "      " + "slope:   " + String.format("%.1f", minSlope) + "-" + String.format("%.1f", maxSlope));
		System.out.println(prefix + "      " + "face:    " + (int) minFace + "-" + (int) maxFace);
		System.out.println(prefix + "      " + "bid      " + (taperedBid ? "tapered" : "flat"));
		System.out.println(prefix + "      " + "range:   " + (flexRange ? "flexible" : "strict"));
		if (barrier)
			System.out.println(prefix + "      " + "barrier: " + "true");
		if (previewColor != null)
			System.out.println(prefix + "      " + "color:   " +
								previewColor.getRed() + "," +
								previewColor.getGreen() + "," +
								previewColor.getBlue());
		System.out.println(prefix + "      " + "vigor:   " + vigor);
	}
	
	/**
	 * compute how much to bid based on value and my range
	 * 
	 * @param value		actual value
	 * @param min		bottom of acceptable range
	 * @param max		top of acceptable range
	 * 
	 * @return			0 to 1 for a favorable bid
	 * 					0 to -1 for an unfavorable bid
	 * 					IMPOSSIBLE if we are outside the range
	 * note:
	 *		taperedBid: bid is proportional to place in range
	 *		flexRange: negative bids for barely outside range
	 */
	double range_bid(double value, double min, double max) {
		
		// figure out the acceptable range and where we are in it
		double mid = (min + max)/2;
		double range = mid - min;
		double delta = Math.abs(value - mid);
		
		// if within range, bid based on distance to center
		if (delta <= range)
			return taperedBid ? 1.0 - delta/range : 1.0;
		
		// if outside worst acceptable we fail
		if (!flexRange || delta > 2*range)
			return IMPOSSIBLE;
		
		// return negative based on how far off we are
		return taperedBid ? -(delta - range)/range : -1.0;
	}
	
	/**
	 * compute the bid that this tile will make for a grid square
	 * 		total bid is the sum of characteristic bids
	 * 		characteristic bid is based on where it is in range
	 * 
	 * @param alt		altitude(M)
	 * @param hydro		hydration(%)
	 * @param winter	low temp(degC)
	 * @param summer	high temp(degC)
	 * @param soil		soil type
	 * @param slope		dz/dxy
	 * @param direction	0-360
	 * 
	 * @return			bid
	 */
	double bid(double alt, double hydro, double winter, double summer, double soil, double slope,
			double direction) {

		// range check vs altitude, hydration and temperature
		double score = 0;
		double v;
		justification = "";
		v = range_bid(alt, minAltitude, maxAltitude);
		if (v <= 0)
			justification += "alt";
		score += v;
		
		if (hydro >= 0) {
			v = range_bid(hydro, minHydro, maxHydro);
			if (v <= 0)
				justification += "+hydro";
		}
		
		v = range_bid((winter+summer)/2, minTemp, maxTemp);
		if (v <= 0)
			justification += "+temp";
		score += v;
		
		v = range_bid(direction, minFace, maxFace);
		if (v <= 0)
			justification += "+dir";
		score += v;
		
		v = range_bid(slope, minSlope, maxSlope);
		if (v <= 0)
			justification += "+slope";
		score += v;
		
		v = range_bid(soil, minSoil, maxSoil);
		if (v <= 0)
			justification += "+soil";
		score += v;
		return vigor * score;
	}
	
	/**
	 * is this rule inapplicable to a particular terrain
	 * @param terrain type to be checked
	 */
	public boolean wrongTerrain(int terrain) {
		// see if the terrain type precludes this tile-bid
		switch (this.terrain) {
		case TerrainType.LAND:
			return !TerrainType.isLand(terrain);

		case TerrainType.LOW:
			return !TerrainType.isLowLand(terrain);

		case TerrainType.HIGH:
			return !TerrainType.isHighLand(terrain);

		// specific terrain types must match
		case TerrainType.DEEP_WATER:
		case TerrainType.SHALLOW_WATER:
		case TerrainType.PASSABLE_WATER:
		case TerrainType.PIT:
		case TerrainType.GROUND:
		case TerrainType.HILL:
		case TerrainType.MOUNTAIN:
		case TerrainType.SLOPE:
		default:
			return this.terrain != terrain;
		}
	}

	/**
	 * is this rule inapplicable to a particular floral class
	 * @param  floraClass to be checked
	 */
	public boolean wrongFlora(String floraClass) {
		return className != null && !className.equals("NONE") && !className.equals(floraClass);
	}
}
