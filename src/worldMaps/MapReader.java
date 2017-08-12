package worldMaps;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;

import javax.json.Json;
import javax.json.stream.JsonParser;

/**
 * this class uses a streaming JSON parser to avoid
 * the huge memory footprint associated with complete
 * object parsing.
 */
public class MapReader {

	// returned soil types
	public enum SoilType {
			UNKNOWN, IGNIOUS, METAMORPHIC, SEDIMENTARY, ALLUVIAL
	};
	
	// returned directions
	public enum CompassDirection {
		NORTH, NORTH_EAST, EAST, SOUTH_EAST, SOUTH, SOUTH_WEST, WEST, NORTH_WEST
	};
	
	// per map state
	private String region;
	private double lat;
	private double lon;
	private int height;
	private int width;
	private int tileSize;
	
	// per cell state
	private double altitude[][];
	private int rainfall[][];
	private int hydration[][];
	private SoilType soil[][];
	
	public MapReader(String filename) {
		JsonParser parser;
		try {
			parser = Json.createParser(new BufferedReader(new FileReader(filename)));
		} catch (FileNotFoundException e) {
			System.err.println("FATAL: unable to open input file " + filename);
			return;
		}

		String thisKey = "";
		boolean inPoints = false;
		int row = 0;
		int col = 0;
		while(parser.hasNext()) {
			JsonParser.Event e = parser.next();
			switch(e) {
			case START_ARRAY:
				inPoints = true;
				altitude = new double[height][width];
				rainfall = new int[height][width];
				hydration = new int[height][width];
				soil = new SoilType[height][width];
				break;
				
			case KEY_NAME:
				thisKey = parser.getString();
				break;
				
			case VALUE_FALSE:
			case VALUE_TRUE:
			case VALUE_STRING:
			case VALUE_NUMBER:
				switch(thisKey) {
				case "name":
					region = parser.getString();
					break;
					
				case "height":
					height = new Integer(parser.getString());
					break;
					
				case "width":
					width = new Integer(parser.getString());
					break;
					
				case "tilesize":
					String s = parser.getString();
					int x = s.indexOf('m');
					if (x != -1)
						s = s.substring(0, x-1);
					tileSize = new Integer(s);
					break;
					
				case "latitude":
					lat = new Double(parser.getString());
					break;
					
				case "longitude":
					lon = new Double(parser.getString());
					break;
					
				case "altitude":
					s = parser.getString();
					x = s.indexOf('m');
					if (x != -1)
						s = s.substring(0, x);
					altitude[row][col] = new Double(s);
					break;
					
				case "rainfall":
					s = parser.getString();
					x = s.indexOf("cm");
					if (x != -1)
						s = s.substring(0, x);
					rainfall[row][col] = new Integer(s);
					break;
					
				case "hydration":
					s = parser.getString();
					x = s.indexOf('%');
					if (x != -1)
						s = s.substring(0, x);
					hydration[row][col] = new Integer(s);
					break;
					
				case "soil":
					switch(parser.getString()) {
					case "igneous":
						soil[row][col] = SoilType.IGNIOUS;
						break;
					case "metamorphic":
						soil[row][col] = SoilType.METAMORPHIC;
						break;
					case "sedimentary":
						soil[row][col] = SoilType.SEDIMENTARY;
						break;
					case "alluvial":
						soil[row][col] = SoilType.ALLUVIAL;
						break;
					default:
						soil[row][col] = SoilType.UNKNOWN;
						break;	
					}
				}
				break;
				
			case END_ARRAY:
				inPoints = false;
				
			case END_OBJECT:
				if (inPoints) {
					col++;
					if (col >= width) {
						col = 0;
						row++;
					}
				}
				break;
				
			case START_OBJECT:
			}
		}
		parser.close();
	}
	
	/**
	 * methods to return scalar map attributes
	 */
	public String name() { return region; }
	public double latitude() { return lat; }
	public double longitude() { return lon; }
	public int height() { return height; }
	public int width() { return width; }
	public int tileSize() {return tileSize; };
	
	/**
	 * methods to return per-cell information
	 */
	public double altitude(int row, int col) {
		return altitude[row][col];
	}
	
	public int rainfall(int row, int col) {
		return rainfall[row][col];
	}
	
	public double slope(int row, int col ) {
		// FIX: implement slope
		return(0);
	}
	
	public CompassDirection face(int row, int col ) {
		// FIX: implement face
		return(CompassDirection.SOUTH);
	}
	
	public int hydration(int row, int col) {
		return hydration[row][col];
	}
	
	public SoilType soilType(int row, int col) {
		return SoilType.UNKNOWN;	// FIX: implement soilTYpe
	}
}
