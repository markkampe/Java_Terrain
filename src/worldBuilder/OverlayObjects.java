package worldBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.ListIterator;

import javax.imageio.ImageIO;
import javax.json.Json;
import javax.json.stream.JsonParser;

/**
 * read in and digest a collection of Tile Rules
 */
public class OverlayObjects {
	private static final String DEFAULT_CONFIG = "/Templates";
	private static final String DEFAULT_ICONS = "/icons";
	private static final int NO_VALUE = 666666;
	
	/** name of this set of objects	  */	public String setName;
	/** (preview) unit width (pixels) */	public int tileSize;
	
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
				System.out.println("ERROR: unable to open Overlay Objects file " + filename);
				return;
			}
		}
		
		// parsing state information
		String thisKey = "";
		boolean inList = false;
		int tile_number = 0;
		
		// accumulated object attributes
		String thisName = "", thisIcon = "";
		int thisHeight = NO_VALUE, thisWidth = NO_VALUE, 
			thisZmin = NO_VALUE, thisZmax = NO_VALUE;
		double thisMmin = NO_VALUE, thisMmax = NO_VALUE;
		
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
				
				tile_number++;	// so we can talk about them
				
				if (!thisName.equals("") && thisHeight != NO_VALUE && thisWidth != NO_VALUE) {
					OverlayObject newObj = new OverlayObject(thisName, thisHeight, thisWidth);
					if (thisZmin != NO_VALUE)
						newObj.z_min = thisZmin;
					if (thisZmax != NO_VALUE)
						newObj.z_max = thisZmax;
					if (thisMmin != NO_VALUE)
						newObj.slope_min = thisMmin;
					if (thisMmax != NO_VALUE)
						newObj.slope_max = thisMmax;
					if (thisIcon != "") {
						String icon_file = thisIcon;
						try {
							if (thisIcon.charAt(0) != '/') {
								icon_file = DEFAULT_ICONS + "/" + thisIcon;
								InputStream s = getClass().getResourceAsStream(icon_file);
								if (s != null)
									newObj.icon = ImageIO.read(s);
								else
									throw new IOException("nonesuch");
							} else
								newObj.icon = ImageIO.read(new File(icon_file));
							objects.add(newObj);
						} catch (IOException x) {
							System.err.println("ERROR: unable to read icon file " + icon_file);
						}
					}
				} else {
					// complain about the missing information
					String prefix = filename + " tile #" + tile_number + ": ";
					if (thisName.equals(""))
						System.err.println(prefix + "tile w/no name");
					if (thisHeight == NO_VALUE)
						System.err.println(prefix + "tile w/no height");
					if (thisWidth == NO_VALUE)
						System.err.println(prefix + "tile w/no width");
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
				case "s_min":
					thisMmin = new Double(parser.getString());
					thisKey = "";
					break;
				case "s_max":
					thisMmax = new Double(parser.getString());
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
