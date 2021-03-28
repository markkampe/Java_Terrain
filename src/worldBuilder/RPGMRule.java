package worldBuilder;

import java.util.ListIterator;

/**
 * An extension of ResourceRules that includes information about RPGM tiles 
 */
public class RPGMRule extends ResourceRule {
	
	/** RPGMaker tile set	*/
	public static int tileSet;
	/** RPGMaker map level	*/
	public int level;
	/** base tile number for this rule	*/
	public int baseTile;
	/** base tile number for what we might surround*/
	public int altTile;
	/** TerrainType for this rule	*/
	public int terrain;
	/** does this tile represent an impassable barrier	*/
	public boolean barrier;
	/** dimensions (if this is a stamp)	*/
	public int height, width;
	/** number of neighbors for auto-tiling	*/
	public int neighbors;
	/** ecotope for this tile	*/
	public String ecotope;
	
	// save extended parameters to store in next factory-instantiated object
	private static int n_level, n_terrain, n_baseTile, n_altTile, n_neighbors;
	private static int n_height = 1, n_width = 1;
	private static String n_eco = "ANY";
	private static boolean n_barrier = false;

	/**
	 * create a new rule
	 * @param name of this rule
	 */
	public RPGMRule(String name) {
		super(name);
		
		// initialize our extended attributes
		this.level = 0;
		this.baseTile = 0;
		this.altTile = 0;
		this.neighbors = 8;
		this.terrain = TerrainType.NONE;
		this.height = 1;
		this.width = 1;
		this.barrier = false;
		this.ecotope = "ANY";
	}
	
	/**
	 * Factory method (to permit subclass instantiation by ResourceRule.loadFile)
	 * @param name of this rule
	 * 
	 * Note: this method is called by ResourceRule.read() when the rule 
	 * 		 reading is complete, which means that any extended attributes
	 * 		 have already been set (in the static save variables).  We 
	 * 		 copy those into the new rule, and then reinitialize them.
	 */
	public RPGMRule factory(String name) {
		RPGMRule newRule = new RPGMRule(name);
		
		// copy extended attributes into the new rule
		newRule.level = n_level;
		newRule.terrain = n_terrain;
		newRule.baseTile = n_baseTile;
		newRule.altTile = n_altTile;
		newRule.neighbors = n_neighbors;
		newRule.height = n_height;
		newRule.width = n_width;
		newRule.barrier = n_barrier;
		newRule.ecotope = n_eco;
		
		// reset their values for the next rule
		n_level = 0;
		n_terrain = TerrainType.NONE;
		n_baseTile = 0;
		n_altTile = 0;
		n_neighbors = 8;
		n_height = 1;
		n_width = 1;
		n_barrier = false;
		n_eco = "ANY";
		
		// and return the newly fabricated object (to ResourceRule.read)
		return newRule;
	}
	
	/**
	 * called from ResourceRule.loadFile ... set an extended attribute (string value)
	 * @param name of the attribute being set
	 * @param value to be set 
	 */
	public void set_attribute(String name, String value) {
		switch (name) {
		case "ecotope":		// ecotope
			n_eco = value;
			return;
		case "stamp":	// width x height
			int x = value.indexOf('x');
			n_width = Integer.parseInt(value.substring(0,x));
			n_height = Integer.parseInt(value.substring(x+1));
			return;
		case "terrain":
			n_terrain = TerrainType.terrainType(value);
			return;
		case "barrier":
			if (value.equals("true"))
				n_barrier = true;
			else if (value.equals("false"))
				n_barrier = false;
			return;
		}
		System.err.println(ruleFile + ": Unrecognized attribute (" + name + "=\"" + value + "\")");
	}
	
	/**
	 * called from ResourceRule.loadFile set an extended attribute (integer value)
	 * @param name of the attribute being set
	 * @param value to be set
	 */
	public void set_attribute(String name, int value) {
		switch (name) {
		case "level":
			n_level = value;
			return;
		case "base":
			n_baseTile = value;
			return;
		case "surround":
			n_altTile = value;
			return;
		case "tileset":
			tileSet = value;	// this is for the entire rule set
			return;
		case "neighbors":
			n_neighbors = value;
			return;
		}
		System.err.println(ruleFile + ": Unrecognized attribute: (" + name + "=" + value + ")");
	}
	
	/**
	 * set an extended attribute (min/max double value)
	 * @param name of the attribute being set
	 * @param limit (min or max)
	 * @param value to be set
	 */
	public void set_range(String name, String limit, double value) {
		// we do not yet have extended attributes with min/max values
		System.err.println(ruleFile + ": Unrecognized RPGMRule attribute (" + name + "." + limit + "=" + value + ")");
	}
	
	/**
	 * return a reference to the first rule for a specified base tile
	 * @param base tile number
	 */
	public static RPGMRule tileRule(int base) {
		for( ListIterator<ResourceRule> it = ResourceRule.iterator(); it.hasNext();) {
			RPGMRule r = (RPGMRule) it.next();
			if (r.baseTile != base)
				return r;
		}
		return null;
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
	 * dump out the extended field attributes (for debugging)
	 */
	public void dump(String prefix) {
		// start with the standard info
		super.dump(prefix);
		
		System.out.println(prefix + "      terrain: " + TerrainType.terrainType(terrain));
		if (ecotope != null)
			System.out.println(prefix + "      ecotope: " + ecotope);
		System.out.print(prefix   + "      tile:    L" + level);
		System.out.print(", base=" + tileSet + "." + baseTile);
		if (altTile > 0)
			System.out.print(", surround=" + tileSet + "." + altTile);
		if (neighbors != 8)
			System.out.print(", neighbors=" + neighbors);
		if (height > 1 || width > 1)
			System.out.print(", stamp=" + width + "x" + height);
		System.out.println("");
		
		if (barrier)
			System.out.println(prefix + "      barrier=true");

	}
}
