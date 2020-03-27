package worldBuilder;

/**
 * types of terrain known to the tile exporting engines
 */
public class TerrainType {
	
	/** constants for defined TerrainTypes */
	public static final int NONE = 0,
							DEEP_WATER = 1, SHALLOW_WATER = 2, PASSABLE_WATER = 3,
							PIT = 4, GROUND = 5,
							HILL = 6, MOUNTAIN = 7,
							LAND = 8, LOW = 9, HIGH = 10, SLOPE = 11;
	
	private static final String typeNames[] = {
			"NONE", 
			"deep", "water", "fordable", 
			"pit", "ground", "hill", "mountain", 
			"land", "lowlands", "highlands", "slope"
	};
	
	/**
	 * return terrain description string
	 * @param type constant to be displayed
	 * @return string describing this Terrain
	 */
	public static String terrainType(int type) {
		if (type > 0 && type < typeNames.length)
			return typeNames[type];
		return "NONE(" + type + ")";
	}
	
	/**
	 * map a name into a TerrainType
	 * @param name of terrain class to be encoded
	 * @return terrainType associated with name
	 */
	public static int terrainType(String name) {
		for(int i = 0; i < typeNames.length; i++)
			if (name.equals(typeNames[i]))
					return(i);
		return NONE;
	}
	
	/**
	 * determine if a TerrainType is water
	 * @param type terrain class to be checked
	 * @return (boolean) is this surface water
	 */
	public static boolean isWater(int type) {
		switch(type) {
		case DEEP_WATER:
		case SHALLOW_WATER:
		case PASSABLE_WATER:
			return true;
			
		default:
			return false;
		}
	}
	
	/**
	 * determine if a TerrainType is land
	 * @param type terrain class to be checked
	 * @return (boolean) is this a (nonwater) land terrain
	 */
	public static boolean isLand(int type) {
		switch(type) {
		case HILL:
		case MOUNTAIN:
		case GROUND:
		case PIT:
			return true;
			
		default:	// slopes are not land
			return false;
		}
	}
	
	/**
	 * determine if a TerrainType is low-land
	 * @param type terrain class to be checked
	 * @return (boolean) is this a non-highland terrain
	 */
	public static boolean isLowLand(int type) {
		switch(type) {
		case GROUND:
		case PIT:
			return true;
			
		default:
			return false;
		}
	}
	
	/**
	 * determine if a TerrainType is high-land
	 * @param type terrain class to be checked
	 * @return (boolean) is this a highland terrain
	 */
	public static boolean isHighLand(int type) {
		switch(type) {
		case HILL:
		case MOUNTAIN:
			return true;
			
		default:
			return false;
		}
	}
}
