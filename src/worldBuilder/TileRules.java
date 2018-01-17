package worldBuilder;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.ListIterator;

import javax.json.Json;
import javax.json.stream.JsonParser;

public class TileRules {
	private static final String DEFAULT_CONFIG = "/Templates";
	private static final int NO_VALUE = 666666;
	
	public String ruleset;				// name of these rules
	public int tileset;					// associated tile set ID
	
	public LinkedList<TileRule> rules;	// the rules
	private Parameters parms;

	
	public TileRules(String exportType) {
		parms = Parameters.getInstance();
		rules = new LinkedList<TileRule>();
	
		BufferedReader r;
		JsonParser parser;
		String filename;
		if (parms.config_directory == "") {
			filename = DEFAULT_CONFIG + "/" + exportType + ".json";
			InputStream s = getClass().getResourceAsStream(filename);
			r = new BufferedReader(new InputStreamReader(s));
		} else {
			filename = parms.config_directory + "/" + exportType + ".json";
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
		int surround = NO_VALUE;					// surrounding tile
		int vigor = NO_VALUE;
		int height = 1, width = 1;					// stamp size
		
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
					thisRule = new TileRule(name, tileset, level, base);
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
					thisRule.height = height;
					thisRule.width = width;
					
					rules.add(thisRule);
					level = NO_VALUE;
					base = NO_VALUE;
					surround = NO_VALUE;
					height = 1; width = 1;
					aMin = NO_VALUE; aMax = NO_VALUE;
					dMin = NO_VALUE; dMax = NO_VALUE;
					hMin = NO_VALUE; hMax = NO_VALUE;
					tMin = NO_VALUE; tMax = NO_VALUE;	
					mMin = NO_VALUE; mMax = NO_VALUE;
					fMin = NO_VALUE; fMax = NO_VALUE;
					sMin = NO_VALUE; sMax = NO_VALUE;
					vigor = NO_VALUE;
					name = "";
					
				} else
					thisObject = "";
				
				thisKey = "";
				break;
				
			case START_ARRAY:
				if (thisKey != "") {
					ruleset = thisKey;
					thisKey = "";
				}
				inRules = true;
				break;
				
			case END_ARRAY:
				thisKey = "";
				inRules = false;
				break;

			case VALUE_STRING:
				switch (thisKey) {
				case "name":
					name = parser.getString();
					break;
				case "stamp":
					thisValue = parser.getString();
					if (thisValue.equals("2x2")) {	// FIX generalize stamp size
						height = 2;
						width = 2;
					}
					break;
				}
				thisKey = "";
				break;

			case VALUE_NUMBER:
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
			System.out.println("Tiles \"" + ruleset + "\": id=" + tileset +
					", " + rules.size() + " rules read from " + filename);
		if (level > 2) {
			for (ListIterator<TileRule> it = rules.listIterator(); it.hasNext(); ) {
				thisRule = it.next();
				thisRule.dump("    ");
			}
		}
	}
}