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
		public int grassNum;
		public int dirtNum;
		public int waterNum;
		public int deepNum;

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
		int id = 0, deep = 0, water = 0, grass = 0, dirt = 0;
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
				case "dirt":
					dirt = new Integer(parser.getString());
					break;
					
				case "grass":
					grass = new Integer(parser.getString());
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
					set.dirtNum = dirt;
					set.grassNum = grass;
					tilesets.add(set);
					
					// then reset all fieldsd
					name = "";
					id = 0;
					deep = 0;
					water = 0;
					grass = 0;
					dirt = 0;
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
					System.out.println("      water=" + t.waterNum + ", deep=" + t.deepNum +
							", grass=" + t.grassNum + ", dirt=" + t.dirtNum);
				}
			}
		}
	}
}