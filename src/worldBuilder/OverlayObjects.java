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

/**
 * read in and digest a collection of Tile Rules
 */
public class OverlayObjects {
	private static final String DEFAULT_CONFIG = "/Templates";
	private static final int NO_VALUE = 666666;
	
	/** name of this set of objects	*/	public String setName;
	/** (preview) width of a tile	*/	public int tileSize;
	
	/** list of accumulated objects	*/
	private LinkedList<OverlayObject> objects;
	
	/**
	 * open and process an list of overlay objects
	 * @param objFile file to be processed
	 */
	public OverlayObjects(String objFile) {
		objects = new LinkedList<OverlayObject>();
	
		BufferedReader r;
		JsonParser parser;
		String filename;
		if (objFile.charAt(0) != '/') {
			filename = DEFAULT_CONFIG + "/" + objFile;
			InputStream s = getClass().getResourceAsStream(filename);
			r = new BufferedReader(new InputStreamReader(s));
		} else {
			filename = objFile;
			try {
				r = new BufferedReader(new FileReader(filename));
			} catch (FileNotFoundException e) {
				System.out.println("ERROR: unable to open tile rules file " + filename);
				return;
			}
		}
		
		// parsing state variables
		String thisKey = "";
		boolean inList = false;
		
		// accumulated object attributes
		String thisName = "", thisIcon = "";
		int thisHeight = NO_VALUE, thisWidth = NO_VALUE, 
			thisZmin = NO_VALUE, thisZmax = NO_VALUE;
		
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
				if (!inList)
					break;
				
				if (!thisName.equals("") && thisHeight != NO_VALUE && thisWidth != NO_VALUE) {
					OverlayObject newObj = new OverlayObject(thisName, thisHeight, thisWidth);
					if (thisZmin != NO_VALUE)
						newObj.z_min = thisZmin;
					if (thisZmax != NO_VALUE)
						newObj.z_max = thisZmax;
					if (thisIcon != "") {
						newObj.icon = null;	// FIX open icon file and ImageIO.read the file
					}
					objects.add(newObj);
				} else {
					System.err.println("Error: missing object attributes");
					// FIX - better diagnostics
				}
				
				thisHeight = NO_VALUE;
				thisWidth = NO_VALUE;
				thisZmin = NO_VALUE;
				thisZmax = NO_VALUE;
				thisName = "";
				thisIcon = "";
				break;
				
			case START_ARRAY:
				setName = thisKey;
				thisKey = "";
				inList = true;
				break;
				
			case END_ARRAY:
				thisKey = "";
				inList = false;
				break;

			case VALUE_STRING:
				switch (thisKey) {
				case "//":	// comment
					parser.getString();
					break;
				case "name":
					thisName = parser.getString();
					break;
				case "icon":
					thisIcon = parser.getString();
					break;
				}
				thisKey = "";
				break;

			case VALUE_NUMBER:
				switch (thisKey) {
				case "height":
					thisHeight = parser.getInt();
					break;
				case "width":
					thisWidth = parser.getInt();
					thisKey = "";
					break;
				case "z_min":
					thisZmin = parser.getInt();
					thisKey = "";
					break;
				case "z_max":
					thisZmax = parser.getInt();
					thisKey = "";
					break;
				case "tile_size":
					tileSize = parser.getInt();
					thisKey = "";
					break;
				}
				thisKey = "";
				break;

			default:
				break;
			}
		}
		
		int level = Parameters.getInstance().debug_level;
		if (level > 0)
			System.out.println("OverlayObjects (" + setName + 
					"): read " + objects.size() + 
					" (tile_size=" + tileSize + ")" +
					" descriptions from " + filename);
		if (level > 2) {
			for (ListIterator<OverlayObject> it = objects.listIterator(); it.hasNext(); )
				 it.next().dump("    ");
		}
	}
	
	/*
	 * size and iterator functions for the accumulated objects
	 */
	public ListIterator<OverlayObject> listIterator() {
		return objects.listIterator();
	}
	
	public int size() {
		return objects.size();
	}
}
