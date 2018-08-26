package worldBuilder;

public class TerrainType {
	
	/**
	 * types of terrain known to the tile exporting engines
	 */
	public static final int NONE = 0;
	public static final int DEEP_WATER = 1;
	public static final int SHALLOW_WATER = 2;
	public static final int PASSABLE_WATER = 3;
	public static final int PIT = 4;
	public static final int GROUND = 5;
	public static final int HILL = 6;
	public static final int MOUNTAIN = 7;
	public static final int LAND = 8;
	public static final int LOW = 9;
	public static final int HIGH = 10;
	
	private static final String typeNames[] = {
			"NONE", 
			"deep", "water", "fordable", 
			"pit", "ground", "hill", "mountain", 
			"land", "lowlands", "highlands"
	};
	
	/**
	 * @return string describing this Terrain
	 */
	public static String terrainType(int type) {
		if (type > 0 && type < typeNames.length)
			return typeNames[type];
		return "NONE";
	}
	
	/**
	 * @return terrainType associated with namejk
	 */
	public static int terrainType(String name) {
		for(int i = 0; i < typeNames.length; i++)
			if (name.equals(typeNames[i]))
					return(i);
		return NONE;
	}
	
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
	
	public static boolean isLowLand(int type) {
		switch(type) {
		case GROUND:
		case PIT:
			return true;
			
		default:
			return false;
		}
	}
	
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
