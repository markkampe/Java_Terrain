package worldBuilder;

import java.util.LinkedList;


public class TileRule {
	
	public static LinkedList<TileRule> rules;
	
	public String ruleName;
	public int level;
	public int tileSet;
	public int baseTile;
	
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
	
	public int vigor;
	
	private static Parameters parms = Parameters.getInstance();
	
	public TileRule(String name, int tileset, int level, int base) {
		this.ruleName = name;
		this.tileSet = tileset;
		this.level = level;
		this.baseTile = base;
		
		
		// default values will meet any terrestrial conditions
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
		vigor = 16;
	}
	
	/**
	 * dump out a rule (for debugging)
	 * @param prefix ... leading blankis
	 */
	void dump( String prefix ) {
		System.out.println(prefix + "Rule: " + ruleName + ", level=" + level + ", base=" + baseTile);
		System.out.println(prefix + "      " + "alt:   " + minAltitude + "-" + maxAltitude);
		System.out.println(prefix + "      " + "depth: " + (int) minDepth + "-" + (int) maxDepth);
		System.out.println(prefix + "      " + "hydro: " + String.format("%.1f", minHydro) + "-" + String.format("%.1f", maxHydro));
		System.out.println(prefix + "      " + "temp:  " + minTemp + "-" + maxTemp);
		System.out.println(prefix + "      " + "soil:  " + String.format("%.1f", minSoil) + "-" + String.format("%.1f", maxSoil));
		System.out.println(prefix + "      " + "slope: " + String.format("%.1f", minSlope) + "-" + String.format("%.1f", maxSlope));
		System.out.println(prefix + "      " + "vigor: " + vigor);
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
	 * @param alt		altitude(M)
	 * @param hydro		hydration(%)
	 * @param winter	low temp(degC)
	 * @param summer	high temp(degC)
	 * @param soil		soil type
	 * @param slope		%
	 * 
	 * @return			integer bid
	 */
	int bid(double alt, double hydro, double winter, double summer, double soil, double slope) {
		// see if any parameters preclude this tile-bid
		if (alt < minAltitude || alt > maxAltitude)
			return 0;
		if (winter < minTemp || summer > maxTemp)
			return 0;
		if (hydro > 0 && (hydro < minHydro || hydro > maxHydro))
			return 0;
		if (hydro > 0 && minDepth > 0)
			return 0;
		if (hydro < 0 && ((-hydro < minDepth || -hydro > maxDepth)))
			return(0);
		if (soil < minSoil || soil > maxSoil)
			return 0;
		if (slope < minSlope || slope > maxSlope)
			return 0;

		// see if any parameters take us outside of the sweet-zone
		double score = vigor;
		if (alt < (maxAltitude + 3*minAltitude)/4)
			score /= 2;
		else if (alt > (minAltitude + 3*maxAltitude)/4)
			score /= 2;
		if (hydro >= 0) {
			if (hydro < (maxHydro + 3*minHydro)/4)
				score /= 2;
			else if (hydro > (minHydro+ 3*maxHydro)/4)
				score /= 2;
		} else {
			if (-hydro < (maxDepth + 3*minDepth)/4)
				score /= 2;
			else if (-hydro > (minDepth+ 3*maxDepth)/4)
				score /= 2;
		}
		if (winter < (maxTemp + 3*minTemp)/4)
			score /= 2;
		else if (summer > (minTemp+ 3*maxTemp)/4)
			score /= 2;
		
		// round it up and return it
		return (int) (score + 0.5);
	}
}
