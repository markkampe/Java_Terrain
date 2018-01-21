package worldBuilder;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.ListIterator;

import javax.json.Json;
import javax.json.stream.JsonParser;

public class RpgmIndex {

	private static final String INFO_FILE_NAME = "MapInfos.json";
	private static final int NO_VALUE = 666666;
	
	public LinkedList<RpgmMap> maps;
	private int numMaps;
	private String path;
	private Parameters parms;
	
	public RpgmIndex(String directory) {
		path = directory;
		maps = new LinkedList<RpgmMap>();
		parms = Parameters.getInstance();
		
		BufferedReader r;
		JsonParser parser;
		String filename = path + "/" + INFO_FILE_NAME;
		
		try {
			r = new BufferedReader(new FileReader(filename));
		} catch (FileNotFoundException e) {
			System.err.println("ERROR: unable to open map index " + filename);
			return;
		}
		
		// parsing state variables
		int read = 0;
		String thisKey = "";
		int r_id = NO_VALUE;
		boolean r_expanded = false;;
		String r_name = null;
		int r_order = 0;
		int r_parent = 0;
		double r_x = 0;
		double r_y = 0;
		
		parser = Json.createParser(r);
		while (parser.hasNext()) {
			JsonParser.Event e = parser.next();
			switch (e) {
			case KEY_NAME:
				thisKey = parser.getString();
				break;
				
			case START_OBJECT:
				break;
				
			case END_OBJECT:
				RpgmMap m = new RpgmMap(r_name, r_expanded);
				m.id = r_id;
				m.parent = r_parent;
				m.order = r_order;
				m.x = r_x;
				m.y = r_y;
				maps.add(m);
				read++;
				
				// note the highest ID we have seen
				if (m.id > numMaps)
					numMaps = m.id;
				
				r_id = 0;
				r_parent = 0;
				r_order = 0;
				r_x = 0;
				r_y = 0;
				r_name = null;
				r_expanded = false;
				break;
				
			case START_ARRAY:
				break;
				
			case END_ARRAY:
				break;

			case VALUE_STRING:
				switch (thisKey) {
				case "name":
					r_name = parser.getString();
					break;
				}
				break;

			case VALUE_NUMBER:
				switch (thisKey) {
				case "id":
					r_id = parser.getInt();
					break;
				case "order":
					r_order = parser.getInt();
					break;
				case "parentId":
					r_parent = parser.getInt();
					break;
				case "scrollX":
					r_x = Double.parseDouble(parser.getString());
					break;
				case "scrollY":
					r_y = Double.parseDouble(parser.getString());
					break;
				}
				
			case VALUE_TRUE:
				if (thisKey.equals("expanded"))
					r_expanded = true;
				break;
				
			case VALUE_FALSE:
				if (thisKey.equals("expanded"))
					r_expanded = false;
				break;
				
			default:
				break;
			}
		}
		
		if (parms.debug_level > 0) {
			System.out.println("discovered " + read + " maps in map index " + filename);
		}
	}
	
	/**
	 * rewrite the index
	 */
	public void flush() {
		String filename = path + "/" + INFO_FILE_NAME;
		try {
			FileWriter output = new FileWriter(filename);
			output.write("[\nnull,");
			boolean firstline = true;
			int written = 0;
			for( ListIterator<RpgmMap> it = maps.listIterator(); it.hasNext(); ) {
				RpgmMap m = it.next();
				if (written > 0)
					output.write(",");
				output.write("\n");
				output.write(String.format("{\"id\":%d", m.id));
				output.write(String.format(",\"expanded\":%s", m.expanded ? "true" : "false"));
				output.write(String.format(",\"name\":\"%s\"", m.name));
				output.write(String.format(",\"order\":%d", m.order));
				output.write(String.format(",\"parentId\":%d", m.parent));
				output.write(String.format(",\"scrollX\":%.1f", m.x));
				output.write(String.format(",\"scrollY\":%.1f", m.y));
				output.write("}");
				written++;
			}
			output.write("\n]");
			output.close();
			
			if (parms.debug_level > 0) {
				System.out.println("described " + written + " maps in map index " + filename);
			}
		} catch (IOException e) {
			System.err.println("Unable to (re)create index: " + filename);
		}
	}
	
	/**
	 * create a new map and add it to the index
	 * 
	 * @param parent	name of parent map
	 * @param expanded	is this tactical scale
	 */
	public RpgmMap addMap(String parent, boolean expanded) {

		String name = String.format("MAP%03d",++numMaps);
		RpgmMap newMap = new RpgmMap(name, expanded);
		
		newMap.id = numMaps;
		RpgmMap p = lookup(parent);
		if (p != null) {
			newMap.parent = p.id;
			newMap.x = p.x;		// I don't understand this field
			newMap.y = p.y;		// I don't understand this field
		} 
		newMap.order = numMaps;
		maps.add(newMap);
		return newMap;
	}
	
	/**
	 * look up the Map ID associated with a name
	 * @param name to look up
	 */
	public RpgmMap lookup(String name) {
		for( ListIterator<RpgmMap> it = maps.listIterator(); it.hasNext(); ) {
			RpgmMap m = it.next();
			if (name.equals(m.name))
				return(m);
		}
		return null;
	}
}
