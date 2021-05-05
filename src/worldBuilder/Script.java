package worldBuilder;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class Script {
	private Parameters parms;	// configuration singleton
	private BufferedReader r;	// open file being processed
	private String filename;	// name of script being processed
	private int lineNum;		// line number being processed
	private String[] tokens;	// lexed tokens from current line

	private static final int MAX_TOKENS = 10;
	private static final int SCRIPT_DEBUG = 2;

	public static final int DO_NOT_EXIT = 1024;

	public Script(String filename) {
		this.filename = filename;
		try {
			r = new BufferedReader(new FileReader(filename));
		} catch (FileNotFoundException e) {
			System.err.println("Unable to open script: " + filename);
		}
	}

	/**
	 * XY_positions are the (-0.5-0.5) coordiantes of a point
	 */
	private class XY_pos {
		public double x;	// starting x coordinate (map units)
		public double y; 	// starting y coordinate (map units)
		public double x2;	// Ending x coordinate (map units)
		public double y2;	// Ending y coordinate (map units)
		
		public XY_pos(double x, double y) {
			this.x = x;
			this.y = y;
		}
	};
	
	/**
	 * process this script
	 * @param map current Map
	 * 
	 * @return anything other than DO_NOT_EXIT is an exit code
	 */
	public int process(Map map) {
		parms = Parameters.getInstance();
		TerrainEngine t = new TerrainEngine(map);

		tokens = new String[MAX_TOKENS];
		lineNum = 0;
		int cmdNum = 0;
		String line;
		while( true ) {
			try {
				line = r.readLine();
				if (line == null) {
					r.close();
					if (parms.debug_level > 0)
						System.out.println("Processed " + cmdNum + " commands from " + filename);
					return(DO_NOT_EXIT);
				}
				lineNum++;
			} catch (IOException e) {
				System.err.println("Read error in script: " + filename);
				return(DO_NOT_EXIT);
			}

			lex(line);

			// skip comments and blank lines
			if (tokens[0] == null)
				continue;

			cmdNum++;
			if (parms.debug_level >= SCRIPT_DEBUG)
				System.out.println(" ... " + line);

			// process the command
			switch(tokens[0]) {
			case "sleep":
				int seconds = (tokens[1] == null) ? 5 : (int) num_w_unit(tokens[1], "s", "sleep");
				try {
					Thread.sleep(seconds * 1000);
				} catch (InterruptedException e) {
					// no difference
				}
				break;
				
			case "set":
				if (tokens[1] == null || tokens[2] == null)
					System.err.println(String.format("Error: %s[%d] \"%s\" - s.b. set parameter value", filename, lineNum, line));
				else
					switch(tokens[1]) {
					case "region":
						parms.region_name = tokens[2];
						break;
					case "description":
						parms.description = tokens[2];
						break;
					case "author":
						parms.author_name = tokens[2];
						break;
					case "xy_scale":
						double km = num_w_unit(tokens[2], "km", tokens[1]);
						parms.xy_range = (int) km;
						break;
					case "z_scale":
						double m = num_w_unit(tokens[2], "m", tokens[1]);
						parms.z_range = (int) m;
						break;
					case "lat":
						double lat = num_w_unit(tokens[2], null, tokens[1]);
						parms.latitude = lat;
						break;
					case "lon":
						double lon = num_w_unit(tokens[2], null, tokens[1]);
						parms.longitude = lon;
						break;

					default:
						System.err.println("set of unrecognized parameter: " + tokens[1]);
						break;
					}
				break;

			case "load":	// load the specified map
				if (tokens[1] == null)
					System.err.println(String.format("Error: %s[%d] \"%s\" - s.b. load filename", filename, lineNum, line));
				else
					map.read(tokens[1]);
				break;

			case "save":	// save map to specified file
				if (tokens[1] == null)
					System.err.println(String.format("Error: %s[%d] \"%s\" - s.b. save filename", filename, lineNum, line));
				else
					map.write(tokens[1]);
				break;

			case "sealevel":	// set the sea level
				if (tokens[1] == null)
					System.err.println(String.format("Error: %s[%d] \"%s\" - s.b. sealevel z/height", filename, lineNum, line));
				else
					parms.sea_level = z_value(tokens[1], tokens[0]);
				break;
				
			case "display":		// update the display
				if (tokens[1] != null) {
					map.display = 0;
					map.setDisplay(displayOptions(tokens[1]), true);
				} else {
					map.repaint();
				}
				break;
					
			case "slope":
				if (tokens[1] == null || tokens[2] == null)
					System.err.println(String.format("Error: %s[%d] \"%s\" - s.b. slope angle fraction", filename, lineNum, line));
				else {
					int axis = (int) num_w_unit(tokens[1], null, "slope axis");
					double slope = num_w_unit(tokens[2], null, "slope (dz/dx)");
					t.slope(axis, slope);
					t.commit();
				}
				break;
				
			case "river":
				if (tokens[1] == null || tokens[2] == null)
					System.err.println(String.format("Error: %s[%d] \"%s\" - s.b. river <x,y> #m3/s", filename, lineNum, line));
				else {
					XY_pos xy = position(tokens[1], "river entry point");
					double flow = num_w_unit(tokens[2], "m3/s", "River flow");
					System.out.println("River: loc=<" + xy.x +"," + xy.y + ">, flux=" + flow);	// FIX place river
				}
				break;
				
			case "rainfall":
				if (tokens[1] == null || tokens[2] == null)
					System.err.println(String.format("Error: %s[%d] \"%s\" - s.b. rainfall <x,y> #cm/y", filename, lineNum, line));
				else {
					XY_pos xy = position(tokens[1], "Rainfall region");
					double rain = num_w_unit(tokens[2], "cm/y", "Annual Rainfall");
					System.out.println("RainFall: loc=<" + xy.x + "," + xy.y + ">-<" +
									   xy.x2 + "," + xy.y2 + ">, flux=" + rain);	// FIX place rain
				}
				break;
			case "mountain":
			case "canyon":
				System.err.println(tokens[0] + " command not yet implemented");
				break;
				
			case "minerals":
				if (tokens[1] == null || tokens[2] == null)
					System.err.println(String.format("Error: %s[%d] \"%s\" - s.b. minerals <x,y> type", filename, lineNum, line));
				else {
					XY_pos xy = position(tokens[1], "mineral region");
					String type = tokens[2];
					System.out.println("Minerals: loc=<" + xy.x + "," + xy.y + ">-<" +
									   xy.x2 + "," + xy.y2 + ">, type=" + type);	// FIX place minerals
				}
				break;
				
			case "flora":
				if (tokens[1] == null || tokens[2] == null)
					System.err.println(String.format("Error: %s[%d] \"%s\" - s.b. flora <x,y> type", filename, lineNum, line));
				else {
					XY_pos xy = position(tokens[1], "flora region");
					String type = tokens[2];
					System.out.println("Flora: loc=<" + xy.x + "," + xy.y + ">-<" +
									   xy.x2 + "," + xy.y2 + ">, type=" + type);	// FIX place flora
				}
				break;
				
			case "fauna":
				if (tokens[1] == null || tokens[2] == null)
					System.err.println(String.format("Error: %s[%d] \"%s\" - s.b. fauna <x,y> type", filename, lineNum, line));
				else {
					XY_pos xy = position(tokens[1], "fauna region");
					String type = tokens[2];
					System.out.println("Fauna: loc=<" + xy.x + "," + xy.y + ">-<" +
									   xy.x2 + "," + xy.y2 + ">, type=" + type);	// FIX place fauna
				}
				break;
				
			case "PoI":
				if (tokens[1] == null || tokens[2] == null || tokens[3] == null)
					System.err.println(String.format("Error: %s[%d] \"%s\" - s.b. PoI <x,y> type name", filename, lineNum, line));
				else {
					XY_pos xy = position(tokens[1], "POI location");
					String type = tokens[2];
					String name = tokens[3];
					System.out.println("POI: loc=<" + xy.x + "," + xy.y + ">, type=" + type + ", name=" + name);	// FIX place PoI
				}
				break;

			case "exit":	// optional exit code argument
				if (parms.debug_level > 0)
					System.out.println("Processed " + cmdNum + " commands from " + filename + " (and exiting)");
				if (tokens[1] != null)
					return Integer.parseInt(tokens[1]);
				else
					return(0);	// need a non-zero value

			default:
				System.err.println(filename + "[" + lineNum + "] Unrecognized command: " + tokens[0]);
			}
		}
	}

	/**
	 * lex an input line into white-space separated tokens
	 * (but properly handle quoted strings)
	 * @param line
	 */
	private void lex(String line) {
		int token = 0;
		boolean in_token = false;
		char quote = 0;
		int token_start = 0;
		for(int pos = 0; pos < line.length(); pos++) {
			char c = line.charAt(pos);
			// white space delimits tokens
			if (c == ' ' || c == '\t') {
				if (quote != 0)
					continue;
				if (!in_token)
					continue;
				// the current token has just ended
				tokens[token++] = line.substring(token_start, pos);
				in_token = false;
				continue;
			}

			// closing quotes end a token
			if (quote == c) {
				tokens[token++] = line.substring(token_start, pos);
				quote = 0;
				in_token = false;
				continue;
			}

			// first non-white-space starts new token
			if (!in_token) {
				if (c == '"' || c == '\'') {
					quote = c;
					token_start = pos+1;
				} else
					token_start = pos;
				in_token = true;
			}
		}

		// line probably ends in mid-token
		if (in_token) {
			tokens[token++] = line.substring(token_start);
		}

		// if the first token is a # or / the line is a comment
		if (token > 0 && (tokens[0].startsWith("#") || tokens[0].startsWith("//")))
			token = 0;

		// null out all remaining tokens
		while(token < MAX_TOKENS)
			tokens[token++] = null;
	}

	/**
	 * lex off a numeric value with an optional unit
	 * @param value	string to be lexed
	 * @param unit	expected (optional) unit string
	 * @param attribute name (for error messages)
	 * @return
	 */
	private double num_w_unit(String value, String unit, String attribute) {
		// see if there is a suffix
		String number = value;
		String suffix = null;
		for(int pos = 0; pos < value.length(); pos++) {
			char c = value.charAt(pos);
			if (Character.isDigit(c) || c == '.' || c == '-')
				continue;
			number = value.substring(0,pos);
			suffix = value.substring(pos);
			break;
		}
		double retval = 0;
		try {
			retval = Double.parseDouble(number);
		} catch (NumberFormatException e) {
			System.err.println(attribute + " Non-numeric value: " + number);
		}

		// check the suffix
		if (suffix != null && !suffix.equals(unit))
			System.err.println(attribute + " Unit Error: got " + suffix + ", expected " + unit);

		return retval;
	}

	/**
	 * lex off a numeric value that might be in Z units or meters
	 * @param value string to be lexed
	 * @param attribute name for error messages
	 * @return
	 */
	private double z_value(String value, String attribute) {
		// see if there is a suffix
		String number = value;
		String suffix = null;
		for(int pos = 0; pos < value.length(); pos++) {
			char c = value.charAt(pos);
			if (Character.isDigit(c) || c == '.' || c == '-')
				continue;
			number = value.substring(0,pos);
			suffix = value.substring(pos);
			break;
		}
		double retval = 0;
		try {
			retval = Double.parseDouble(number);
		} catch (NumberFormatException e) {
			System.err.println(attribute + " Non-numeric value: " + number);
		}
		
		// no suffix ... it is already a Z value
		if (suffix == null)
			return retval;

		// in meters ... convert to Z value
		if (suffix.equals("m"))
			return parms.z(retval);
		
		// unrecognized unit
		System.err.println(attribute + " Unit Error: got: " + suffix + ", expected: m");
		return(0);
	}
	
	/**
	 * lex x/y coordinates
	 * @param string of the form <x,y> or <x,y>-<x,y>
	 * @param attribute being read (for error messages)
	 * @return XY_pos
	 */
	private XY_pos position(String string, String attribute) {
		// figure out where the <> delimiters are
		if (string.charAt(0) != '<') {
			System.err.println(attribute + " position: does not begin with '<'");
			return new XY_pos(0, 0);
		}
		int comma = string.indexOf(',', 1);
		int end = string.indexOf('>', 1);
		if (end < 4 || comma < 2 || comma > end) {
			System.err.println(attribute + " position: not of the form '<x,y>'");
			return new XY_pos(0, 0);
		}
		String x1 = string.substring(1,comma);
		String y1 = string.substring(comma+1,end);
		XY_pos pos = null;
		try {
			double x = Double.parseDouble(x1);
			double y = Double.parseDouble(y1);
			pos = new XY_pos(x, y);
		} catch (NumberFormatException e) {
			System.err.println(attribute + " position: \"<" + x1 + "," + y1 + ">\": non-numeric x/y");
			return new XY_pos(0, 0);
		}
		
		// see if there is a second position
		pos.x2 = -1;
		pos.y2 = -1;
		if (string.length() == end+1)
			return pos;
		
		// second < should be two bytes past the first >
		int start2 = end + 2;
		comma = string.indexOf(',', start2);
		end = string.indexOf('>', start2);
		if (string.length() < start2 + 5 || comma < 0 || end < 0 ||
				string.charAt(start2 - 1) != '-' || string.length() < start2 + 1 ||
				string.charAt(start2) != '<' || end < start2+4 || 
				comma < start2 + 2 || comma > end) {
			System.err.println(attribute + " positions: \"" + string + "\": not of the form <x1,y1>-<x2,y2>");
			return pos;
		}
		try {
			pos.x2 = Double.parseDouble(string.substring(start2+1, comma));
			pos.y2 = Double.parseDouble(string.substring(comma+1, end));
		} catch (NumberFormatException e) {
			System.err.println(attribute + " position2: \"" + string.substring(start2) + "\": non-numeric x/y");
		}
		
		return pos;
	}
	
	/**
	 * turn letters into Map display options
	 * 
	 * @param list string of display options
	 */
	private int displayOptions(String list) {
		int options = 0;
		for(int i = 0; i < list.length(); i++) {
			switch(list.charAt(i)) {
			case 't': case 'T':
				options |= Map.SHOW_TOPO;
				break;
			case 'r': case 'R':
				options |= Map.SHOW_RAIN;
				break;
			case 'w': case 'W':
				options |= Map.SHOW_WATER;
				break;
			case 'e': case 'E':
				options |= Map.SHOW_ERODE;
				break;
			case 'm': case 'M':
				options |= Map.SHOW_ROCKS;
				break;
			case 'f': case 'F':
				options |= Map.SHOW_FLORA;
				break;
			case 'a': case 'A':
				options |= Map.SHOW_FAUNA;
				break;
			}
		}
		return options;
	}
}
