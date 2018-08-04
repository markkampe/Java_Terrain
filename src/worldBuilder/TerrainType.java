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
}
