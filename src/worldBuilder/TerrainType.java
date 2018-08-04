package worldBuilder;

public class TerrainType {
	
	/**
	 * types of terrain known to the tile exporting engines
	 */
	public enum TerrainClass {
		DEEP_WATER, SHALLOW_WATER, PASSABLE_WATER,
		GROUND, PIT, HILL, MOUNTAIN
	};
	
	public TerrainClass type;
	
	public TerrainType(TerrainClass tClass) {
		type = tClass;
	}
	
	public boolean isWater() {
		switch(type) {
		case DEEP_WATER:
		case SHALLOW_WATER:
		case PASSABLE_WATER:
			return true;
			
		default:
			return false;
		}
	}
	
	public boolean isLowLand() {
		switch(type) {
		case GROUND:
		case PIT:
			return true;
			
		default:
			return false;
		}
	}
	
	public boolean isHighLand() {
		switch(type) {
		case HILL:
		case MOUNTAIN:
			return true;
			
		default:
			return false;
		}
	}
}
