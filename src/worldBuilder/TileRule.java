package worldBuilder;

import java.util.LinkedList;


public class TileRule {
	
	public static LinkedList<TileRule> rules;
	
	public String ruleName;
	public int level;		// map level
	public int tileSet;		// tile set ID
	public int baseTile;	// base tile for this rule
	public int altTile;		// base tile for surrounded version
	public int height;		// stamp height
	public int width;		// stamp width
	
	public int terrain;
	public int minAltitude;
	public int maxAltitude;
	public double minDepth;
	public double maxDepth;
	public double minHydro;
	public double maxHydro;
	public double minTemp;
	public double maxTemp;
	public double minSlope;
	public double maxSlope;
	public double minSoil;
	public double maxSoil;
	public double maxFace;
	public double minFace;
	
	public int vigor;
	
	public String justification;	// reason for the last bid
	
	private static Parameters parms = Parameters.getInstance();
	
	public TileRule(String name, int tileset, int level, int base) {
		this.ruleName = name;
		this.tileSet = tileset;
		this.level = level;
		this.baseTile = base;
		this.height = 1;
		this.width = 1;
		this.altTile = 0;
		
		// default values will meet any terrestrial conditions
		terrain = TerrainType.NONE;
		minAltitude = -parms.alt_max;
		maxAltitude = parms.alt_max;
		minDepth = 0;
		maxDepth = 0;
		minHydro = 0.0;
		maxHydro = 1.0;
		minTemp = -30;
		maxTemp = 35;
		minSlope = 0.0;
		maxSlope = 100.0;
		minSoil = 0;
		maxSoil = Map.soil_names.length - 1;
		minFace = 0;
		maxFace = 360;
		vigor = 16;
	}
	
	/**
	 * dump out a rule (for debugging)
	 * @param prefix ... leading blankis
	 */
	void dump( String prefix ) {
		System.out.print(prefix + "Rule: " + ruleName + ", level=" + level + ", base=" + baseTile);
		if (altTile > 0)
			System.out.print(", surround=" + altTile);
		if (height > 1 || width > 1)
			System.out.print(", stamp=" + width + "x" + height);
		System.out.println("");
		System.out.println(prefix + "      " + "terrain: " + TerrainType.terrainType(terrain));
		System.out.println(prefix + "      " + "alt:     " + minAltitude + "-" + maxAltitude);
		System.out.println(prefix + "      " + "depth:   " + (int) minDepth + "-" + (int) maxDepth);
		System.out.println(prefix + "      " + "hydro:   " + String.format("%.1f", minHydro) + "-" + String.format("%.1f", maxHydro));
		System.out.println(prefix + "      " + "temp:    " + minTemp + "-" + maxTemp);
		System.out.println(prefix + "      " + "soil:    " + String.format("%.1f", minSoil) + "-" + String.format("%.1f", maxSoil));
		System.out.println(prefix + "      " + "slope:   " + String.format("%.1f", minSlope) + "-" + String.format("%.1f", maxSlope));
		System.out.println(prefix + "      " + "face:    " + (int) minFace + "-" + (int) maxFace);
		System.out.println(prefix + "      " + "vigor:   " + vigor);
	}
	
	/**
	 * compute the bid that this tile will make for a grid square
	 * 
	 * 	NOTE: a characteristic in the center half of the range
	 * 		  will get a full bid.  a characteristic in the high
	 * 		  or low quarter will get a half bid.  The total bid
	 * 		  is the product of the vigor and the characteristic
	 * 		  bids.
	 * 
	 * comments on vigor:
	 * 		1: any characteristic out of sweet zone -> 0
	 * 		2: two characteristics out of sweet zone -> 0
	 * 		4: three characteristics out of sweet zone -> 0
	 * 		8: four characteristics out of sweet zone -> 0
	 * 		16 is probably a good neutral vigor
	 * 
	 * @param terrain	TerrainClass
	 * @param alt		altitude(M)
	 * @param hydro		hydration(%)
	 * @param winter	low temp(degC)
	 * @param summer	high temp(degC)
	 * @param soil		soil type
	 * @param slope		dz/dxy
	 * @param direction	0-360
	 * 
	 * @return			integer bid
	 */
	int bid(int terrain, double alt, double hydro, double winter, double summer, 
			double soil, double slope, double direction) {
	
		// see if the terrain type precludes this tile-bit
		switch(this.terrain) {
			case TerrainType.LAND:
				if (!TerrainType.isLand(terrain)) {
					justification = "land terrain mismatch";
					return 0;
				}
				break;
				
			case TerrainType.LOW:
				if (!TerrainType.isLowLand(terrain)) {
					justification = "low terrain mismatch";
					return 0;
				}
				break;
					
			case TerrainType.HIGH:
				if (!TerrainType.isHighLand(terrain)) {
					justification = "high terrain mismatch";
					return 0;
				}
				break;
				
			// specific terrain types must match
			case TerrainType.DEEP_WATER:
			case TerrainType.SHALLOW_WATER:
			case TerrainType.PASSABLE_WATER:				
			case TerrainType.PIT:
			case TerrainType.GROUND:
			case TerrainType.HILL:
			case TerrainType.MOUNTAIN:
			case TerrainType.SLOPE:
				if (this.terrain != terrain) {
					justification = "terrain mismatch";
					return 0;
				}
				break;
				
			default:
				break;
		}
		
		// see if any parameters preclude this tile-bid
		if (alt < minAltitude || alt > maxAltitude) {	
			justification = "alt out of range";
			return 0;
		}
		if (winter < minTemp || summer > maxTemp) {	
			justification = "temp out of range";
			return 0;
		}
		if (hydro > 0 && (hydro < minHydro || hydro > maxHydro)) {	
			justification = "hydro out of range";
			return 0;
		}
	/*	
		if (hydro > 0 && minDepth > 0) {	
			justification = "not u/w";
			return 0;
		}
		if (hydro < 0 && ((-hydro < minDepth || -hydro > maxDepth))) {	
			justification = "depth out of range";
			return(0);
		}
		*/
		if (soil < minSoil || soil > maxSoil) {	
				justification = "soil out of range";
			return 0;
		}
		if (slope < minSlope || slope > maxSlope) {	
			justification = "slope out of range";
			return 0;
		}
		if (direction < minFace || direction > maxFace) {	
			justification = "face out of range";
			return 0;
		}
		
		// see if any parameters take us outside of the sweet-zone
		double score = vigor;
		justification = "";
		if (alt < (maxAltitude + 3 * minAltitude) / 4) {
			justification += "low altitude reduction ";
			score /= 2;
		} else if (alt > (minAltitude + 3 * maxAltitude) / 4) {
			justification += "high altitude reduction ";
			score /= 2;
		}
		if (hydro >= 0) {
			if (hydro < (maxHydro + 3 * minHydro) / 4) {
				justification += "low hydration reduction ";
				score /= 2;
			} else if (hydro > (minHydro + 3 * maxHydro) / 4) {
				justification += "high hydration reduction ";
				score /= 2;
			}
		}
		if (winter < (maxTemp + 3 * minTemp) / 4) {
			justification += "low temp reduction ";
			score /= 2;
		} else if (summer > (minTemp + 3 * maxTemp) / 4) {
			justification += "high temp reduction ";
			score /= 2;
		}

		// round it up and return it
		return (int) (score + 0.5);
	}
}
