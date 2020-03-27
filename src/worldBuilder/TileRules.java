package worldBuilder;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.ListIterator;

import javax.json.Json;
import javax.json.stream.JsonParser;

/**
 * read in and digest a collection of Tile Rules
 */
public class TileRules {
	private static final String DEFAULT_CONFIG = "/Templates";
	private static final int NO_VALUE = 666666;
	
	/** name of this set of rules	*/
	public String ruleset;
	/** RPGMaker tile-set ID		*/
	public int tileset;
	
	/** list of the rules			*/
	public LinkedList<TileRule> rules;
	
	/**
	 * open and process an export rules file
	 * @param exportRules name of file to be processed
	 */
	public TileRules(String exportRules) {
		rules = new LinkedList<TileRule>();
	
		Parameters parms = Parameters.getInstance();
		BufferedReader r;
		JsonParser parser;
		String filename;
		if (exportRules.charAt(0) != '/') {
			filename = DEFAULT_CONFIG + "/" + exportRules;
			InputStream s = getClass().getResourceAsStream(filename);
			r = new BufferedReader(new InputStreamReader(s));
		} else {
			filename = exportRules;
			try {
				r = new BufferedReader(new FileReader(filename));
			} catch (FileNotFoundException e) {
				System.out.println("ERROR: unable to open tile rules file " + filename);
				return;
			}
		}
		
		// parsing state variables
		TileRule thisRule = null;
		String name = "", thisKey = "", thisObject = "", thisValue = "";
		boolean inRules = false;
		
		int level = NO_VALUE, base = NO_VALUE;
		
		int    aMin = NO_VALUE, aMax = NO_VALUE;	// altitude
		double dMin = NO_VALUE, dMax = NO_VALUE;	// depth
		double hMin = NO_VALUE, hMax = NO_VALUE;	// hydration
		double tMin = NO_VALUE, tMax = NO_VALUE;	// temperature
		double mMin = NO_VALUE, mMax = NO_VALUE;	// slope
		double sMin = NO_VALUE, sMax = NO_VALUE;	// soil
		double fMin = NO_VALUE, fMax = NO_VALUE;	// facing direction
		int red = NO_VALUE, blue = NO_VALUE, green = NO_VALUE;
		
		int terrain = TerrainType.NONE;
		int surround = NO_VALUE;					// surrounding tile
		int vigor = NO_VALUE;
		int height = 1, width = 1;					// stamp size
		int neighbors = 8;							// auto-tile neighbors
		boolean flexRange = false;
		boolean taperedBid = false;
		boolean barrier = false;
		
		String className = null;					// floral class
		
		int inColor = 0;
		
		parser = Json.createParser(r);
		while (parser.hasNext()) {
			JsonParser.Event e = parser.next();
			switch (e) {
			case KEY_NAME:
				thisKey = parser.getString();
				break;
				
			case START_OBJECT:
				if (thisKey != "") {
					thisObject = thisKey;
				}
				break;
				
			case END_OBJECT:
				if (!inRules)
					break;
				
				// are these min/max attributes
				if (thisObject == "") {	// end of a new rule?
					// no name or illegal level is not a real rule
					if (name.equals("") || (level != NO_VALUE && level > 6))
						break;
					
					// create a new rule
					thisRule = new TileRule(name, tileset, level, base);
					
					// copy in all of the values we got
					if (className != null)
						thisRule.className = className;
					if (aMin != NO_VALUE)
						thisRule.minAltitude = aMin;
					if (hMin != NO_VALUE)
						thisRule.minHydro = hMin;
					if (dMin != NO_VALUE)
						thisRule.minDepth = dMin;
					if (tMin != NO_VALUE)
						thisRule.minTemp = tMin;
					if (sMin != NO_VALUE)
						thisRule.minSoil = sMin;
					if (mMin != NO_VALUE)
						thisRule.minSlope = mMin;
					if (fMin != NO_VALUE)
						thisRule.minFace = fMin;
					if (aMax != NO_VALUE)
						thisRule.maxAltitude = aMax;
					if (hMax != NO_VALUE)
						thisRule.maxHydro = hMax;
					if (dMax != NO_VALUE)
						thisRule.maxDepth = dMax;
					if (tMax != NO_VALUE)
						thisRule.maxTemp = tMax;
					if (sMax != NO_VALUE)
						thisRule.maxSoil = sMax;
					if (mMax != NO_VALUE)
						thisRule.maxSlope = mMax;
					if (fMax != NO_VALUE)
						thisRule.maxFace = fMax;
					if (vigor != NO_VALUE)
						thisRule.vigor = vigor;
					if (surround != NO_VALUE)
						thisRule.altTile = surround;
					if (red != NO_VALUE)
						thisRule.previewColor = new Color(red, blue, green);
					thisRule.terrain = terrain;
					thisRule.height = height;
					thisRule.width = width;
					thisRule.neighbors = neighbors;
					thisRule.flexRange = flexRange;
					thisRule.taperedBid = taperedBid;
					thisRule.barrier = barrier;
					
					// are we tracing this rule
					thisRule.debug = parms.rule_debug != null && 
							(parms.rule_debug.equals(name) || parms.rule_debug.equals("*"));
					if (thisRule.debug)
						thisRule.dump("DEBUG: ");
					rules.add(thisRule);
					
					// now reset all the parameters for the next rule
					level = NO_VALUE;
					base = NO_VALUE;
					surround = NO_VALUE;
					className = null;
					height = 1; width = 1;
					aMin = NO_VALUE; aMax = NO_VALUE;
					dMin = NO_VALUE; dMax = NO_VALUE;
					hMin = NO_VALUE; hMax = NO_VALUE;
					tMin = NO_VALUE; tMax = NO_VALUE;	
					mMin = NO_VALUE; mMax = NO_VALUE;
					fMin = NO_VALUE; fMax = NO_VALUE;
					sMin = NO_VALUE; sMax = NO_VALUE;
					flexRange = false;
					taperedBid = false;
					barrier = false;
					vigor = NO_VALUE;
					red = NO_VALUE; green = NO_VALUE; blue = NO_VALUE;
					terrain = TerrainType.NONE;
					name = "";
				} else
					thisObject = "";
				thisKey = "";
				break;
				
			case START_ARRAY:
				if (thisKey.equals("color")) {
						inColor = 1;
				} else {
					if (!thisKey.equals("")) {
						ruleset = thisKey;
						thisKey = "";
					}
					inRules = true;
				}
				break;
				
			case END_ARRAY:
				thisKey = "";
				if (inColor > 0)
					inColor = 0;
				else
					inRules = false;
				break;

			case VALUE_STRING:
				switch (thisKey) {
				case "//":
					parser.getString();
					break;
				case "name":
					name = parser.getString();
					break;
				case "class":
					className = parser.getString();
					break;
				case "stamp":
					thisValue = parser.getString();
					int x = thisValue.indexOf('x');
					width = Integer.parseInt(thisValue.substring(0,x));
					height = Integer.parseInt(thisValue.substring(x+1));
					break;
				case "terrain":
					terrain = TerrainType.terrainType(parser.getString());
					break;
				case "range":
					flexRange = parser.getString().equals("flexible");
					break;
				case "bid":
					taperedBid = parser.getString().equals("tapered");
					break;
				case "barrier":
					barrier = parser.getString().equals("true");
					break;
				}
				thisKey = "";
				break;

			case VALUE_NUMBER:
				if (inColor > 0) {
					switch(inColor) {
					case 1:
						red = parser.getInt();
						inColor++;
						break;
					case 2:
						blue = parser.getInt();
						inColor++;
						break;
					case 3:
						green = parser.getInt();
						inColor++;
						break;
					default:
						System.err.println("ERROR: too many fields in color");
						break;
					}
					break;
				}
				switch (thisKey) {
				case "level":
					level = parser.getInt();
					thisKey = "";
					break;
				case "base":
					base = parser.getInt();
					thisKey = "";
					break;
				case "surround":
					surround = parser.getInt();
					break;
				case "tileset":
					tileset = parser.getInt();
					thisKey = "";
					break;
				case "vigor":
					vigor = parser.getInt();
					thisKey = "";
					break;
				case "neighbors":
					neighbors = parser.getInt();
					break;
				case "min":
					thisValue = parser.getString();
					switch(thisObject) {
					case "alt":
						aMin = Integer.parseInt(thisValue);
						break;
					case "temp":
						tMin = Double.parseDouble(thisValue);
						break;
					case "hydro":
						hMin = Double.parseDouble(thisValue);
						break;
					case "depth":
						dMin = Double.parseDouble(thisValue);
						break;
					case "slope":
						mMin = Double.parseDouble(thisValue);
						break;
					case "soil":
						sMin = Double.parseDouble(thisValue);
						break;
					case "face":
						fMin = Double.parseDouble(thisValue);
						break;
					}
					thisKey = "";
					break;
				case "max":
					thisValue = parser.getString();
					switch(thisObject) {
					case "alt":
						aMax = Integer.parseInt(thisValue);
						break;
					case "temp":
						tMax = Double.parseDouble(thisValue);
						break;
					case "hydro":
						hMax = Double.parseDouble(thisValue);
						break;
					case "depth":
						dMax = Double.parseDouble(thisValue);
						break;
					case "slope":
						mMax = Double.parseDouble(thisValue);
						break;
					case "soil":
						sMax = Double.parseDouble(thisValue);
						break;
					case "face":
						fMax = Double.parseDouble(thisValue);
						break;
					}
					thisKey = "";
					break;
				}
				break;

			default:
				break;
			}
		}
		
		level = Parameters.getInstance().debug_level;
		if (level > 0)
			System.out.println("Tile rules " + ruleset + "(tilesetId=" + tileset +
					"): read " + rules.size() + " rules from " + filename);
		if (level > 2) {
			for (ListIterator<TileRule> it = rules.listIterator(); it.hasNext(); ) {
				thisRule = it.next();
				thisRule.dump("    ");
			}
		}
	}
	
	/**
	 * tile information: number of (auto-tile) neghbors for a base tile
	 * @param   base tile
	 * @return  number of (auto-tile) neighors
	 */
	public int neighbors(int base) {
		for( ListIterator<TileRule> it = rules.listIterator(); it.hasNext();) {
			TileRule r = it.next();
			if (r.baseTile != base)
				continue;
			return r.neighbors;
		}
		return -1;
	}
	
	/**
	 * tile information: does this tile represent an impassible barrier
	 * @param	base tile
	 * @return	whether or not tile creates barriers
	 */
	public boolean landBarrier(int base) {
		for( ListIterator<TileRule> it = rules.listIterator(); it.hasNext();) {
			TileRule r = it.next();
			if (r.baseTile != base)
				continue;
			return r.barrier;
		}
		return false;
	}
}
