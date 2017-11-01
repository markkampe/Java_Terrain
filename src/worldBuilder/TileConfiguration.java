package worldBuilder;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;

import javax.json.Json;
import javax.json.stream.JsonParser;

public class TileConfiguration {
	private static final String DEFAULT_CONFIG = "/Templates/tilesets.json";
	private static TileConfiguration singleton;

	public class TileSet {
		public String name;
		public int id;
		public int sandNum;
		public int rockNum;
		public int dirtNum;
		public int grassNum;
		public int snowNum;
		public int waterNum;
		
		public int treeNum;
		public int palmNum;
		public int pineNum;
		public int xmasNum;
		
		public int deepNum;
		public int grassHillNum;
		public int dirtHillNum;
		public int snowHillNum;
		public int mountainNum;
		public int peakNum;
		public int snowPeakNum;

		public TileSet(String name, int id) {
			this.name = name;
			this.id = id;
		}
	}

	public static TileConfiguration getInstance() {
		return singleton;
	}

	public LinkedList<TileSet> tilesets;

	public TileConfiguration(String filename, int debug) {
		singleton = this;
		tilesets = new LinkedList<TileSet>();

		BufferedReader r;
		JsonParser parser;
		if (filename == null) {
			filename = DEFAULT_CONFIG;
			InputStream s = getClass().getResourceAsStream(filename);
			r = new BufferedReader(new InputStreamReader(s));
		} else {
			try {
				r = new BufferedReader(new FileReader(filename));
			} catch (FileNotFoundException e) {
				System.out.println("ERROR: unable to open tile configuration file " + filename);
				return;
			}
		}

		String name = "", thisKey = "";
		int id = 0, deep = 0, water = 0, grass = 0, sand = 0, dirt = 0, snow = 0;
		int tree = 0, palm = 0, pine = 0, xmas = 0;
		int grassHill = 0, dirtHill = 0, snowHill = 0, rocks = 0;
		int mountain = 0, peak = 0, snowPeak = 0;
		parser = Json.createParser(r);
		while (parser.hasNext()) {
			JsonParser.Event e = parser.next();
			switch (e) {
			case KEY_NAME:
				thisKey = parser.getString();
				break;

			case VALUE_STRING:
				switch (thisKey) {
				case "name":
					name = parser.getString();
					break;
				}
				break;

			case VALUE_NUMBER:
				switch (thisKey) {
				case "id":
					id = new Integer(parser.getString());
					break;
				case "deep":
					deep = new Integer(parser.getString());
					break;
					
				case "water":
					water = new Integer(parser.getString());
					break;
					
				case "sand":
					sand = new Integer(parser.getString());
					break;
					
				case "rocks":
					rocks = new Integer(parser.getString());
					break;
					
				case "dirt":
					dirt = new Integer(parser.getString());
					break;
					
				case "dirtHill":
					dirtHill = new Integer(parser.getString());
					break;
					
				case "grass":
					grass = new Integer(parser.getString());
					break;
					
				case "grassHill":
					grassHill = new Integer(parser.getString());
					break;
					
				case "snow":
					snow = new Integer(parser.getString());
					break;
					
				case "snowHill":
					snowHill = new Integer(parser.getString());
					break;
					
				case "tree":
					tree = new Integer(parser.getString());
					break;
					
				case "palm":
					palm = new Integer(parser.getString());
					break;
					
				case "pine":
					pine = new Integer(parser.getString());
					break;
					
				case "xmas":
					xmas = new Integer(parser.getString());
					break;
						
				case "mountain":
					mountain = new Integer(parser.getString());
					break;
					
				case "peak":
					peak = new Integer(parser.getString());
					break;
					
				case "snowPeak":
					snowPeak = new Integer(parser.getString());
					break;
					
				default:
					break;
				}
				break;

			case END_OBJECT:
				if (name != "") {
					TileSet set = new TileSet(name, id);
					set.deepNum = deep;
					set.waterNum = water;
					set.sandNum = sand;
					set.rockNum = rocks;
					set.dirtNum = dirt;
					set.dirtHillNum = dirtHill;
					set.grassNum = grass;
					set.grassHillNum = grassHill;
					set.snowNum = snow;
					set.snowHillNum = snowHill;
					set.treeNum = tree;
					set.palmNum = palm;
					set.pineNum = pine;
					set.xmasNum = xmas;
					set.mountainNum = mountain;
					set.peakNum = peak;
					set.snowPeakNum = snowPeak;
					tilesets.add(set);
					
					// then reset all fieldsd
					name = "";
					id = 0;
					deep = 0;
					water = 0;
					sand = 0;
					grass = 0;
					dirt = 0;
					rocks = 0;
					tree = 0;
					palm = 0;
					pine = 0;
					xmas = 0;
					grassHill = 0;
					dirtHill = 0;
					snowHill = 0;
					mountain = 0;
					peak = 0;
					snowPeak = 0;
				}
				break;

			default:
				break;
			}
		}

		if (debug > 0) {
			int len = tilesets.size();
			System.out.println("Tileset Configuration (" + filename + "), " + len + " tile sets");
			if (debug > 1) {
				for( TileSet t: tilesets) {
					System.out.println("   id: " + t.id + ", name=" + t.name);
					System.out.println("      water=" + t.waterNum + ", deep=" + t.deepNum);
					System.out.println("      sand=" + t.sandNum + ", dirt=" + t.dirtNum + 
							                  ", rocks=" + t.rockNum + ", grass=" + t.grassNum + 
							                  ", snow=" + t.snowNum );
					System.out.println("      tree=" + t.treeNum + ", palm=" + t.palmNum +
											  ", pine=" + t.pineNum + ", xmas=" + t.xmasNum);
					if (t.dirtHillNum + t.grassHillNum + t.snowHillNum > 0)
						System.out.println("      dirtHill=" + t.dirtHillNum +
											  ", grassHill=" + t.grassHillNum +
											  ", snowHill=" + t.snowHillNum);
					if (t.mountainNum + t.peakNum + t.snowPeakNum > 0)
						System.out.println("      mountain=" + t.mountainNum +
							  				  ", peak=" + t.peakNum + ", snowPeak=" + t.snowPeakNum);
				}
			}
		}
	}
}