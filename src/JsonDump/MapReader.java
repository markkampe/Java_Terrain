package JsonDump;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;

import javax.json.Json;
import javax.json.stream.JsonParser;

/**
 * This class reads the raw JSON output of a worldBuilder map, into
 * a collection of basic (location, scale and temperature) attributes
 * and a set of (per Cartesion point) altitude, rainfall, depth
 * and soil type arrays.
 *
 * This class uses a streaming JSON parser to avoid
 * the huge memory footprint associated with complete
 * object parsing for large numbers of points.
 */
public class MapReader {

	// returned directions
	public enum CompassDirection {
		NONE, NORTH, NORTH_EAST, EAST, SOUTH_EAST, SOUTH, SOUTH_WEST, WEST, NORTH_WEST
	};
	private static final double MIN_SLOPE = .1;	//
	
	// seasons
	public enum Seasons {
		SPRING, SUMMER, FALL, WINTER
	}
	
	// adiabatic temperature lapse rate
	private static final double DEGC_PER_KM = -6.4;
	
	// per map state
	private String region;
	private double lat;
	private double lon;
	private int height;
	private int width;
	private int tileSize;
	private double mean_temp;
	private double summer_temp;
	private double winter_temp;
	
	// per cell state
	private double altitude[][];
	private int rainfall[][];
	private double depth[][];
	private String soil[][];
	
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
				depth = new double[height][width];
				soil = new String[height][width];
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
					height = Integer.valueOf(parser.getString());
					break;
					
				case "width":
					width = Integer.valueOf(parser.getString());
					break;
					
				case "tilesize":
					String s = parser.getString();
					int x = s.indexOf('m');
					if (x != -1)
						s = s.substring(0, x-1);
					tileSize = Integer.valueOf(s);
					break;
					
				case "latitude":
					lat = Double.valueOf(parser.getString());
					break;
					
				case "longitude":
					lon = Double.valueOf(parser.getString());
					break;
					
				case "altitude":
					s = parser.getString();
					x = s.indexOf('m');
					if (x != -1)
						s = s.substring(0, x);
					altitude[row][col] = Double.valueOf(s);
					break;
					
				case "rainfall":
					s = parser.getString();
					x = s.indexOf("cm");
					if (x != -1)
						s = s.substring(0, x);
					rainfall[row][col] = Integer.valueOf(s);
					break;
					
				case "depth":
					s = parser.getString();
					x = s.indexOf('m');
					if (x != -1)
						s = s.substring(0, x);
					depth[row][col] = Double.valueOf(s);
					break;
					
				case "soil":
					soil[row][col] = parser.getString();
					break;
					
				// temperatures
				case "mean":
					s = parser.getString();
					x = s.indexOf('C');
					if (x != -1)
						s = s.substring(0, x-1);
					mean_temp = Double.valueOf(s);
					break;
				case "summer":
					s = parser.getString();
					x = s.indexOf('C');
					if (x != -1)
						s = s.substring(0, x-1);
					summer_temp = Double.valueOf(s);
					break;
				case "winter":
					s = parser.getString();
					x = s.indexOf('C');
					if (x != -1)
						s = s.substring(0, x-1);
					winter_temp = Double.valueOf(s);
					break;
				}
				break;
				
			case END_ARRAY:
				inPoints = false;
				break;
				
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
			default:
				break;
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
	 * altitude (meters) above mean sea level
	 */
	public double altitude(int row, int col) {
		return altitude[row][col];
	}
	
	/**
	 * slope upwards to the east
	 */
	public double dZdX(int row, int col) {
		if (col == width - 1)
			col--;
		double dz = altitude[row][col+1] - altitude[row][col];
		double dx = tileSize;
		return dz/dx;
	}

	/**
	 * slope upwards to the south
	 */
	public double dZdY(int row, int col) {
		if (row == height - 1)
			row--;
		double dz = altitude[row+1][col] - altitude[row][col];
		double dy = tileSize;
		return dz/dy;
	}
	
	/**
	 * max steepness
	 */
	public double slope(int row, int col ) {
		double dzdy = dZdY(row, col);
		double dzdx = dZdX(row, col);
		return Math.sqrt(dzdx*dzdx + dzdy*dzdy);
	}
	
	/**
	 * compass orientation of face
	 */
	public double direction(int row, int col) {
		double dzdy = dZdY(row, col);
		double dzdx = dZdX(row, col);
		double theta = Math.atan(-dzdx/dzdy) * 180 /Math.PI;
		if (dzdy < 0)
			return theta + 180;
		if (dzdx > 0)
			return theta + 360;
		return theta;
	}
	
	/**
	 * direction slope faces
	 */
	public CompassDirection face(int row, int col ) {
		double dzdy = dZdY(row, col);
		double dzdx = dZdX(row, col);

		final CompassDirection dirmap[] = {
				CompassDirection.NONE,
				CompassDirection.NORTH, CompassDirection.EAST, CompassDirection.NORTH_EAST,
				CompassDirection.SOUTH, 
				CompassDirection.NONE, 
				CompassDirection.SOUTH_EAST,
				CompassDirection.NONE,
				CompassDirection.WEST, CompassDirection.NORTH_WEST, 
				CompassDirection.NONE, CompassDirection.NONE,
				CompassDirection.SOUTH_WEST,
				CompassDirection.NONE, CompassDirection.NONE, CompassDirection.NONE
		};
		int face = 0;
		face += (dzdy > MIN_SLOPE) ? 1 : 0;
		face += (dzdx < -MIN_SLOPE) ? 2 : 0;
		face += (dzdy < -MIN_SLOPE) ? 4 : 0;
		face += (dzdx > MIN_SLOPE) ? 8 : 0;
	
		return dirmap[face];
	}
	
	/**
	 * cm of annual rainfall
	 */
	public int rainfall(int row, int col) {
		return rainfall[row][col];
	}
	
	/**
	 * water depth
	 */
	public double depth(int row, int col) {
		return depth[row][col];
	}
	
	/**
	 * dominant soil composition
	 */
	public String soilType(int row, int col) {
		return soil[row][col];
	}
	
	/**
	 * temperature
	 */
	public double meanTemp(int row, int col, Seasons season) {
		double temp = 0;
		// if it is not ocean, correct it for altitude
		if (depth[row][col] != altitude[row][col])
			temp = DEGC_PER_KM * altitude[row][col] / 1000;
		
		switch(season) {
		case SPRING:
		case FALL:
			temp += mean_temp;
			break;
		case SUMMER:
			temp += summer_temp;
			break;
		case WINTER:
			temp += winter_temp;
			break;
		}
		return temp;
	}
}
