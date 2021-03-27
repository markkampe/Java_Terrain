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
 * parameters and bids for a single resource (eg flora or mineral) 
 * that bids for MeshPoints or export tiles.
 */
public class ResourceRule {
	protected static final String DEFAULT_CONFIG = "/Templates";
	protected static final int NO_VALUE = 666666;
	protected static final int RULE_DEBUG = 2;
	
	/** list of all ingested rules	*/
	public static String ruleFile;
	public static String ruleset;
	protected static LinkedList<ResourceRule> rules;
	
	/** name of this rule and its class	*/
	public String ruleName, className;

	/** resource type ID # */
	public int id;

	/** Altitude ranges for this rule	*/
	public int minAltitude, maxAltitude;
	/** water-depth ranges for this rule	*/
	public double minDepth, maxDepth;
	/** Temperature range for this rule	*/
	public double minTemp, maxTemp;
	/** slope/face ranges for this rule	*/
	public double minSlope, maxSlope, minFace, maxFace;	// TODO support slope and face rules
	/** soil type range for this rule	*/
	public double minSoil, maxSoil;
	/** rainfall range for this rule	*/
	public double minRain, maxRain;
	/** river flux range for this rule	*/
	public double minFlux, maxFlux;
	/** is there flexibility in these ranges	*/
	public boolean flexRange;
	/** taper bids as we move away from mid-range	*/
	public boolean taperedBid;
	/** how high should this rule bid	*/
	public int vigor;
	
	/** when, in the sequence of bids, does this go	*/
	public int order;
	
	/** what color should this render as in previews	*/
	public Color previewColor;
	
	/** Debug: trace bids from this rule	*/
	public boolean debug;			// trace bids from this rule

	/** Debug: explain the basis for the last bid	*/
	public String justification;
	
	private static Parameters parms = Parameters.getInstance();
	private static final double IMPOSSIBLE = -666.0;
	
	/**
	 * create a new subtype
	 * @param name of this subtype
	 */
	public ResourceRule(String name) {
		
		this.ruleName = name;
		this.debug = false;
		previewColor = null;// no previews
		className = null;	// no default class
		id = 0;				// no default ID
		order = 9;			// end of the line
		
		// default values will meet any terrestrial conditions
		minAltitude = -parms.alt_max;
		maxAltitude = parms.alt_max;
		minDepth = 0;
		maxDepth = 0;
		minTemp = -60;
		maxTemp = 70;
		minSoil = 0;
		maxSoil = 9;	// FIX, how many legal soil types are there
		minSlope = 0.0;
		maxSlope = 666;
		minFace = 0;
		maxFace = 360;
		minRain = 0;
		maxRain = 666;
		minFlux = 0;
		maxFlux = 666;
		flexRange = false;
		taperedBid = false;
		vigor = 16;
	}
	
	/**
	 * Factory method (to be overridden by subclasses)
	 * 
	 * @param name ... name of new rule to be instantiated
	 * @return new ResourceRule
	 */
	protected ResourceRule factory(String name) {
		return new ResourceRule(name);
	}
	
	// load and iterate over placement rules
	public static int size() { return rules.size(); }
	public static ListIterator<ResourceRule> iterator() { return rules.listIterator(); }
	
	/*
	 * These methods are intended to be overridden by sub-classes, so that
	 * they can process sub-class-specific attributes.
	 */
	public void set_attribute(String name, String value) {
		System.err.println(ruleFile + ": Unrecognized attribute (" + name + "=\"" + value + "\")");
	}
	public void set_attribute(String name, int value) {
		System.err.println(ruleFile + ": Unrecognized attribute (" + name + "=" + value + ")");
	}
	public void set_range(String name, String limit, double value) {
		System.err.println(ruleFile + ": Unrecognized attribute (" + name + "." + limit + "=" + value);
	}
	
	/**
	 * load in resource placement rules
	 * @param file name of file to be read
	 */
	public void loadRules(String file) {
		rules = new LinkedList<ResourceRule>();
		
		Parameters parms = Parameters.getInstance();
		BufferedReader r;
		JsonParser parser;
		String filename;
		if (file.charAt(0) != '/') {
			filename = DEFAULT_CONFIG + "/" + file;
			InputStream s = ResourceRule.class.getResourceAsStream(filename);
			r = new BufferedReader(new InputStreamReader(s));
			ruleFile = filename;
		} else {
			filename = file;
			try {
				r = new BufferedReader(new FileReader(filename));
			} catch (FileNotFoundException e) {
				System.out.println("ERROR: unable to open flora types file " + filename);
				return;
			}
		}
		
		// parsing state variables
		ResourceRule thisRule = null;
		String name = "", thisKey = "", thisObject = "", thisValue = "";
		boolean inRules = false;
		
		int	   id = NO_VALUE;	// resource ID
		int    aMin = NO_VALUE, aMax = NO_VALUE;	// altitude
		double dMin = NO_VALUE, dMax = NO_VALUE;	// depth
		double tMin = NO_VALUE, tMax = NO_VALUE;	// temperature
		double sMin = NO_VALUE, sMax = NO_VALUE;	// soil
		double mMin = NO_VALUE, mMax = NO_VALUE;	// slope
		double cMin = NO_VALUE, cMax = NO_VALUE;	// face (compass direction)
		double rMin = NO_VALUE, rMax = NO_VALUE;	// rainfall
		double fMin = NO_VALUE, fMax = NO_VALUE;	// river flux
		int red = NO_VALUE, blue = NO_VALUE, green = NO_VALUE;	
		int vigor = NO_VALUE;
		int order = NO_VALUE;

		boolean flexRange = false;
		boolean taperedBid = false;
		
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
					if (name.equals(""))
						break;
					
					// create a new rule (of the appropriate sub-type)
					thisRule = this.factory(name);
					
					// copy in all of the values we got
					if (className != null)
						thisRule.className = className;
					if (id != NO_VALUE)
						thisRule.id = id;
					if (aMin != NO_VALUE)
						thisRule.minAltitude = aMin;
					if (dMin != NO_VALUE)
						thisRule.minDepth = dMin;
					if (tMin != NO_VALUE)
						thisRule.minTemp = tMin;
					if (sMin != NO_VALUE)
						thisRule.minSoil = sMin;
					if (cMin != NO_VALUE)
						thisRule.minFace = cMin;
					if (rMin != NO_VALUE)
						thisRule.minRain = rMin;
					if (fMin != NO_VALUE)
						thisRule.minFlux = fMin;
					if (mMin != NO_VALUE)
						thisRule.minSlope = mMin;
					if (aMax != NO_VALUE)
						thisRule.maxAltitude = aMax;
					if (dMax != NO_VALUE)
						thisRule.maxDepth = dMax;
					if (tMax != NO_VALUE)
						thisRule.maxTemp = tMax;
					if (sMax != NO_VALUE)
						thisRule.maxSoil = sMax;
					if (cMax != NO_VALUE)
						thisRule.maxFace = cMax;
					if (rMax != NO_VALUE)
						thisRule.maxRain = rMax;
					if (fMax != NO_VALUE)
						thisRule.maxFlux = fMax;
					if (mMax != NO_VALUE)
						thisRule.maxSlope = mMax;
					if (vigor != NO_VALUE)
						thisRule.vigor = vigor;
					if (order != NO_VALUE)
						thisRule.order = order;
					if (red != NO_VALUE)
						thisRule.previewColor = new Color(red, blue, green);
					thisRule.flexRange = flexRange;
					thisRule.taperedBid = taperedBid;
					rules.add(thisRule);
					
					// now reset all System.out.println("read string for key " + thisKey);the parameters for the next rule
					aMin = NO_VALUE; aMax = NO_VALUE;
					dMin = NO_VALUE; dMax = NO_VALUE;
					cMin = NO_VALUE; cMax = NO_VALUE;
					tMin = NO_VALUE; tMax = NO_VALUE;
					mMin = NO_VALUE; mMax = NO_VALUE;
					rMin = NO_VALUE; rMax = NO_VALUE;
					fMin = NO_VALUE; fMax = NO_VALUE;
					flexRange = false;
					taperedBid = false;
					vigor = NO_VALUE;
					order = NO_VALUE;
					red = NO_VALUE; green = NO_VALUE; blue = NO_VALUE;
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
				case "range":
					flexRange = parser.getString().equals("flexible");
					break;
				case "bid":
					taperedBid = parser.getString().equals("tapered");
					break;
				default: // unrecognized attribute: see if the sub-class recognizes it
					set_attribute(thisKey, parser.getString());
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
				case "id":
					id = parser.getInt();
					thisKey = "";
					break;
				case "vigor":
					vigor = parser.getInt();
					thisKey = "";
					break;
				case "order":
					order = parser.getInt();
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
					case "depth":
						dMin = Double.parseDouble(thisValue);
						break;
					case "soil":
						sMin = Double.parseDouble(thisValue);
						break;
					case "slope":
						mMin = Double.parseDouble(thisValue);
						break;
					case "face":
						cMin = Double.parseDouble(thisValue);
						break;
					case "rain":
						rMin = Double.parseDouble(thisValue);
						break;
					case "flux":
						fMin = Double.parseDouble(thisValue);
						break;
					default:	// unrecognized attribute - see if subclass wants it 
						set_range(thisObject,  "min",  Double.parseDouble(thisValue));
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
					case "depth":
						dMax = Double.parseDouble(thisValue);
						break;
					case "soil":
						sMax = Double.parseDouble(thisValue);
						break;
					case "slope":
						mMax = Double.parseDouble(thisValue);
						break;
					case "face":
						cMax = Double.parseDouble(thisValue);
						break;
					case "rain":
						rMax = Double.parseDouble(thisValue);
						break;
					case "flux":
						fMax = Double.parseDouble(thisValue);
						break;
					default:	// unrecognized attribute - see if subclass wants it 
						set_range(thisObject,  "max",  Double.parseDouble(thisValue));
					}
					thisKey = "";
					break;
					
				default:	// unrecoginzed -> sub-class
					set_attribute(thisKey, parser.getInt());
					thisKey = "";
					break;
				}
				break;

			default:
				break;
			}
		}
		
		// see if we have been asked for debug output
		if (parms.debug_level >= RULE_DEBUG) {
			System.out.println("Resource rules (" + ruleset + ") from " + filename + ":");
			for(ListIterator<ResourceRule> it = iterator(); it.hasNext(); ) {
				it.next().dump("    ");
			}
		}
	}
	
	/**
	 * dump out a Rule (for debugging)
	 * @param prefix ... leading blanks
	 * 
	 * Note: subclass should call super.dump() and then add its own output
	 */
	public void dump( String prefix ) {
		System.out.print(prefix + "subtype: " + ruleName);
		if (id != 0)
			System.out.print(" (" + id + ")");
		System.out.println("");
		if (className != null)
			System.out.println(prefix + "      " + "class:   " + className);
		System.out.println(prefix + "      " + "alt:     " + minAltitude + "-" + maxAltitude);
		System.out.println(prefix + "      " + "depth:   " + String.format("%.2f", minDepth) + "-" + String.format("%.2f", maxDepth));
		System.out.println(prefix + "      " + "flux:    " + String.format("%.2f", minFlux) + "-" + String.format("%.2f", maxFlux));
		System.out.println(prefix + "      " + "rain:    " + String.format("%.1f", minRain) + "-" + String.format("%.1f", maxRain));
		System.out.println(prefix + "      " + "temp:    " + minTemp + "-" + maxTemp);
		System.out.println(prefix + "      " + "soil:    " + String.format("%.1f", minSoil) + "-" + String.format("%.1f", maxSoil));
		System.out.println(prefix + "      " + "slope:   " + String.format("%.1f", minSlope) + "-" + String.format("%.1f", maxSlope));
		System.out.println(prefix + "      " + "face:    " + minFace + "-" + maxFace);
		System.out.println(prefix + "      " + "range:   " + (flexRange ? "flexible" : "strict"));
		System.out.println(prefix + "      " + "bid:     " + (taperedBid ? "tapered" : "flat"));
		if (previewColor != null)
			System.out.println(prefix + "      " + "color:   " +
								previewColor.getRed() + "," +
								previewColor.getGreen() + "," +
								previewColor.getBlue());
		System.out.println(prefix + "      " + "vigor:   " + vigor);
		System.out.println(prefix + "      " + "order:   " + order);
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
	 * @param depth		positive above water, negative below water
	 * @param flux		river flux (M3/s)
	 * @param rain		rainfall (cm/y)
	 * @param winter	low temp(degC)
	 * @param summer	high temp(degC)
	 * @param soil		soil type
	 * 
	 * @return			bid
	 */
	double bid(double alt, double depth, double flux, double rain, double winter, double summer, double soil) {

		// range check vs altitude, hydration and temperature
		double score = 0;
		double v;
		justification = "";
		v = range_bid(alt, minAltitude, maxAltitude);
		if (v <= 0)
			justification += "alt";
		score += v;
		
		if (minDepth == 0 && maxDepth == 0)
			v = 0;			// no depth requirement
		else
			v = (depth > 0) ? IMPOSSIBLE : range_bid(-depth, minDepth, maxDepth);
		if (v <= 0)
			justification += "+depth";
		score += v;
		
		v = range_bid((winter+summer)/2, minTemp, maxTemp);
		if (v <= 0)
			justification += "+temp";
		score += v;
		
		v = range_bid(soil, minSoil, maxSoil);
		if (v <= 0)
			justification += "+soil";
		score += v;
		
		v = range_bid(rain, minRain, maxRain);
		if (v <= 0)
			justification += "+rain";
		score += v;
		
		v = range_bid(flux, minFlux, maxFlux);
		if (v <= 0)
			justification += "+flux";
		score += v;
		
		return vigor * score;
	}
	
	/**
	 * is this rule inapplicable to a particular floral class
	 * @param  floraClass to be checked
	 */
	public boolean wrongFlora(String floraClass) {
		return (className != null && !className.equals(floraClass));
	}
}
