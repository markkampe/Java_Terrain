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
			this.x2 = x;
			this.y2 = y;
		}
	};
	
	/**
	 * process this script
	 * @param map current Map
	 * 
	 * @return anything other than DO_NOT_EXIT is an exit code
	 */
	public int process(Map map) {
		MapWindow window = map.window;
		parms = Parameters.getInstance();
		TerrainEngine t = new TerrainEngine(map);
		AttributeEngine a = new AttributeEngine(map);
		
		// auto-placement rules
		double[] quotas = { 1.0, 0.0, 0.0, 0.0 };

		tokens = new String[MAX_TOKENS];
		lineNum = 0;
		int cmdNum = 0;
		String line, should_be, rules_file;
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
			case "sleep":	// seconds
				int seconds = (tokens[1] == null) ? 5 : (int) num_w_unit(tokens[1], "s", "sleep");
				try {
					Thread.sleep(seconds * 1000);
				} catch (InterruptedException e) {
					// no difference
				}
				break;
				
			case "set":		// attribute value
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

			case "load":	// filename
				if (tokens[1] == null)
					System.err.println(String.format("Error: %s[%d] \"%s\" - s.b. load filename", filename, lineNum, line));
				else
					map.read(tokens[1]);
				break;

			case "save":	// filename
				if (tokens[1] == null)
					System.err.println(String.format("Error: %s[%d] \"%s\" - s.b. save filename", filename, lineNum, line));
				else
					map.write(tokens[1]);
				break;
				
			case "export":	// <x,y>-<x,y> filename format tile-size
				should_be = "<x,y>-<x,y> output-file format tile-size [rules-file]";
				if (tokens[1] == null || tokens[2] == null || tokens[3] == null || tokens[4] == null)
					System.err.println(String.format("Error: %s[%d] \"%s\" - s.b. %s %s",
													filename, lineNum, line, tokens[0], should_be));
				else {
					// make sure we have a reasonable export reagion
					XY_pos xy = position(tokens[1], "export region");
					double box_width = xy.x2 - xy.x;
					double box_height = xy.y2 - xy.y;
					if (box_width <= 0 || box_height <= 0) {
						System.err.println(String.format("Error: %s[%d] \"%s\" - empty export region", filename, lineNum, line));
						break;
					}
					
					// check tile size and compute export size
					double tilesz = xy_value(tokens[4], "tile size");
					if (tilesz == -1) {
						System.err.println(String.format("Error: %s[%d] \"%s\" - illegal tilesize", filename, lineNum, line));
						break;
					}
					int width = (int) (box_width / tilesz);
					int height = (int) (box_height / tilesz);
					if (width < 10 || height < 10 || width > 1000 || height > 1000) {
						System.err.println(String.format("Error: %s[%d] \"%s\" - unreasonable tilesize", filename, lineNum, line));
						break;
					}
					
					// figure out which exporter we need
					Exporter exporter = null;
					switch(tokens[3]) {
					case "raw":
						exporter = new JsonExporter(width, height);
						break;
					case "overworld":
						rules_file = (tokens[5] != null) ? tokens[5] : parms.exportRules.get(RPGMexport.OW_TILES);
						RPGMTiler tiler = new RPGMTiler(rules_file, width, height);
						tiler.floraQuotas(1.0, 0.3, 0.3);	// 30% brush, 30% trees, rest grass
						exporter = tiler;
						break;
					case "outside":
						rules_file = (tokens[5] != null) ? tokens[5] : parms.exportRules.get(RPGMexport.OUT_TILES);
						tiler = new RPGMTiler(rules_file, width, height);
						tiler.floraQuotas(1.0, 0.3, 0.3);	// 30% brush, 30% trees, rest grass
						tiler.highlandLevels(5);			// PIT/GROUND/5xHILL
						exporter = tiler;
						break;
					case "foundation":
						// Foundation Export is hard code to 256x256 tiles
						exporter = new FoundExporter(256, 256);
						tilesz = parms.km(box_width)*1000/256;
						break;
					case "object":
						rules_file = (tokens[5] != null) ? tokens[5] : parms.overlay_objects;
						exporter = new ObjectExporter(rules_file, width, height);
						break;
					}
					if (exporter == null)
						System.err.println(String.format("Error: %s[%d] \"%s\" - unrecognized export format: %s",
								filename, lineNum, line, tokens[3]));
					else {
						ExportEngine e = new ExportEngine(map, xy.x, xy.y, box_width, box_height);
						double meters = parms.km(tilesz) * 1000;
						e.tile_size((int) meters);
						e.export(exporter);
						exporter.writeFile(tokens[2]);
					}
				}
				break;

			case "sealevel":	// z-value (or height)
				if (tokens[1] == null)
					System.err.println(String.format("Error: %s[%d] \"%s\" - s.b. sealevel z/height", filename, lineNum, line));
				else
					map.setSeaLevel(z_value(tokens[1], tokens[0]));
				break;
				
			case "display":		// display-options
				if (tokens[1] != null) {
					window.display = 0;
					window.setDisplay(displayOptions(tokens[1]), true);
				} else {
					window.repaint();
				}
				break;
					
			case "slope":	// angle fraction
				if (tokens[1] == null || tokens[2] == null)
					System.err.println(String.format("Error: %s[%d] \"%s\" - s.b. slope angle inclineG", filename, lineNum, line));
				else {
					int axis = (int) num_w_unit(tokens[1], null, "slope axis");
					double slope = num_w_unit(tokens[2], null, "slope (dz/dx)");
					t.slope(axis, slope);
					t.commit();
				}
				break;
				
			case "river":	// <x,y> flow
				if (tokens[1] == null || tokens[2] == null)
					System.err.println(String.format("Error: %s[%d] \"%s\" - s.b. river <x,y> #m3/s", filename, lineNum, line));
				else {
					XY_pos xy = position(tokens[1], "river entry point");
					double flow = num_w_unit(tokens[2], "m3/s", "River flow");
					MeshPoint p = map.mesh.choosePoint(xy.x, xy.y);
					t.setIncoming(p, flow);
					t.commit();
				}
				break;
				
			case "rainfall":	// <x1,y1>-<x2,y2> rainfall
				if (tokens[1] == null || tokens[2] == null)
					System.err.println(String.format("Error: %s[%d] \"%s\" - s.b. rainfall <x,y> #cm/y", filename, lineNum, line));
				else {
					XY_pos xy = position(tokens[1], "Rainfall region");
					boolean[] selected = pointsInBox(map, xy.x, xy.y, xy.x2, xy.y2);
					double rain = num_w_unit(tokens[2], "cm/y", "Annual Rainfall");
					a.placement(selected, AttributeEngine.WhichMap.RAIN, rain);
					a.commit();
				}
				break;
				
			case "raise":	// <x1,y1>-<x2,y2> height
			case "lower":	// <x1,y1>-<x2,y2> height
				if (tokens[1] == null || tokens[2] == null)
					System.err.println(String.format("Error: %s[%d] \"%s\" - s.b. raise/lower <x,y>-<x,y> height", filename, lineNum, line));
				else {
					XY_pos xy = position(tokens[1], "Raise/Lower region");
					boolean[] selected = pointsInBox(map, xy.x, xy.y, xy.x2, xy.y2);
					double deltaZ = z_value(tokens[2], "Raise/Lower height");
					if (tokens[0].equals("lower"))
						deltaZ *= -1;
					t.raise(selected, deltaZ);
					t.commit();
				}
				break;
				
			case "exaggerate":	// <x1,y1>-<x2,y2> multiple
				if (tokens[1] == null || tokens[2] == null)
					System.err.println(String.format("Error: %s[%d] \"%s\" - s.b. exaggerate <x,y>-<x,y> multiple", filename, lineNum, line));
				else {
					XY_pos xy = position(tokens[1], "Exaggeration region");
					boolean[] selected = pointsInBox(map, xy.x, xy.y, xy.x2, xy.y2);
					double zMultiple = num_w_unit(tokens[2], null, "Exaggeration factor");
					t.exaggerate(selected, zMultiple);
					t.commit();
				}
				break;
				
			case "outline":		// {square,elipse}
				if (tokens[1] == null)
					System.err.println(String.format("Error %s[%d] \"%s\" - s.b. outline {square,elipse}", filename, lineNum, line));
				else switch(tokens[1]) {
				case "square":
					parms.dOutline = Parameters.SQUARE;
					break;
				case "elipse":
					parms.dOutline = Parameters.ELIPSE;
					break;
				default: 
					System.err.println("Unrecognized mountain outline: " + tokens[1]);
					break;
				}
				break;
				
			case "mountain":	// <x1,y1> height(m,km) radius(m,km) [shape]
			case "pit":
			case "ridge":		// <x1,y1>-<x2,y2> height(m,km) radius(m,km) [shape]
			case "valley":
				String exp = tokens[0].equals("mountain") ? "mountain <x,y>" : "ridge <x,y>-<x,y>";
				if (tokens[1] == null || tokens[2] == null || tokens[3] == null)
					System.err.println(String.format("Error: %s[%d] \"%s\" - s.b. %s height radius", filename, lineNum, line, exp));
				else {
					XY_pos xy = position(tokens[1], "location");
					double z = z_value(tokens[2], "altitude");
					double r = xy_value(tokens[3], "radius");
					// see if a shape was specified
					int shape = (Parameters.CONICAL + Parameters.SPHERICAL)/2;
					if (tokens[4] != null)
						switch(tokens[4]) {
						case "flat":
							shape = Parameters.CYLINDRICAL;
							break;
						case "round":
							shape = Parameters.SPHERICAL;
							break;
						case "cone":
							shape = Parameters.CONICAL;
							break;
						default:
							System.err.println("Unrecognized mountain shape: " + tokens[4]);
						}
					t.ridge(xy.x, xy.y, xy.x2, xy.y2, z, r, shape);
					t.commit();
				}
				break;
				
			case "minerals":	// <x1,y1>-<x2,y2> [type]
				if (tokens[1] == null)
					System.err.println(String.format("Error: %s[%d] \"%s\" - s.b. minerals <x,y> [type]", filename, lineNum, line));
				else {
					XY_pos xy = position(tokens[1], "mineral region");
					boolean[] selected = pointsInBox(map, xy.x, xy.y, xy.x2, xy.y2);
					if (tokens[2] == null) {	// auto-placement
						quotas[0] = 1.0;
						quotas[1] = (double) parms.dRockMin * parms.dRockPct / 10000.0;
						quotas[3] = (double) (1 - parms.dRockMax) * parms.dRockPct / 10000.0;
						quotas[2] = (double) (parms.dRockMax - parms.dRockMin) * parms.dRockPct / 10000.0;
						a.placementRules(parms.mineral_rules, MineralDialog.rockClasses, AttributeEngine.WhichMap.MINERAL);
						a.autoPlacement(selected, quotas, AttributeEngine.WhichMap.MINERAL);
					} else {	// manual placement
						double type = map.getSoilType(tokens[2]);
						a.placement(selected, AttributeEngine.WhichMap.MINERAL, type);
					}
					a.commit();
				}
				break;
				
			case "flora":	// <x1,y1>-<x2,y2> [type]
				if (tokens[1] == null)
					System.err.println(String.format("Error: %s[%d] \"%s\" - s.b. flora <x,y> [type]", filename, lineNum, line));
				else {
					XY_pos xy = position(tokens[1], "flora region");
					boolean[] selected = pointsInBox(map, xy.x, xy.y, xy.x2, xy.y2);
					if (tokens[2] == null) {	// auto-placement
						quotas[0] = 1.0;
						quotas[1] = (double) parms.dFloraMin * parms.dFloraPct / 10000.0;
						quotas[3] = (double) (1 - parms.dFloraMax) * parms.dFloraPct / 10000.0;
						quotas[2] = (double) (parms.dFloraMax - parms.dFloraMin) * parms.dFloraPct / 10000.0;
						a.placementRules(parms.flora_rules, FloraDialog.floraClasses, AttributeEngine.WhichMap.FLORA);
						a.autoPlacement(selected, quotas, AttributeEngine.WhichMap.FLORA);
					} else {	// manual placement
						double type = map.getFloraType(tokens[2]);
						a.placement(selected, AttributeEngine.WhichMap.FLORA, type);
					}
					a.commit();
				}
				break;
				
			case "fauna":	// <x1,y1>-<x2,y2> [type]
				if (tokens[1] == null)
					System.err.println(String.format("Error: %s[%d] \"%s\" - s.b. fauna <x,y> [type]", filename, lineNum, line));
				else {
					XY_pos xy = position(tokens[1], "fauna region");
					boolean[] selected = pointsInBox(map, xy.x, xy.y, xy.x2, xy.y2);
					if (tokens[2] == null) {	// auto-placement
						quotas[0] = 1.0;
						quotas[1] = (double) parms.dFaunaMin * parms.dFaunaPct / 10000.0;
						quotas[3] = (double) (1 - parms.dFaunaMax) * parms.dFaunaPct / 10000.0;
						quotas[2] = (double) (parms.dFaunaMax - parms.dFaunaMin) * parms.dFaunaPct / 10000.0;
						a.placementRules(parms.fauna_rules, FaunaDialog.faunaClasses, AttributeEngine.WhichMap.FAUNA);
						a.autoPlacement(selected, quotas, AttributeEngine.WhichMap.FAUNA);
					} else {	// manual placement
						double type = map.getFaunaType(tokens[2]);
						a.placement(selected, AttributeEngine.WhichMap.FAUNA, type);
					}
					a.commit();
				}
				break;
				
			case "region":	// region <x,y> name [description]
			case "capital":	// capital <x1,y1> name [description]
			case "city":	// city <x1,y1> name [description]
			case "town":	// town <x1,y1> name [description]
			case "village":	// village <x1,y1> name [description]
			case "entrypoint":	// entrypoint <x1,y1> name [description]
			case "exitpoint":	// exitpoint <x1,y1> name [description]
				if (tokens[1] == null || tokens[2] == null)
					System.err.println(String.format("Error: %s[%d] \"%s\" - s.b. %s <x,y> name [description]", 
										filename, lineNum, line, tokens[0]));
				else {
					XY_pos xy = position(tokens[1], "location");
					String entry = tokens[0].equals("region") ? "" : tokens[0] + ": ";
					entry += tokens[2];
					if (tokens[3] != null)
						entry += " - " +  tokens[3];
					map.addName(entry, xy.x, xy.y);
					
					if (parms.debug_level > 0)
						System.out.println(String.format("%s: %s at <%.5f,%.5f>",
											tokens[0], tokens[2], parms.latitude(xy.y), parms.longitude(xy.x)));
				}
				break;

			case "exit":	// [optional code]
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
			
			// slash-slash ends a line
			if (c == '/' && line.charAt(pos+1) == '/' && !in_token)
				break;

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
	 * lex off a horizontal distance (radius, km, or m)
	 * @param value	string to be lexed
	 * @param attribute name (for error messages)
	 * @return
	 */
	private double xy_value(String value, String attribute) {
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
			return -1;
		}

		// see if this is in km or meters
		if (suffix != null) 
			if (suffix.equals("km"))
				return(parms.x(retval));
			else if (suffix.equals("m"))
				return parms.x(retval)/1000.0;
		// assume it is in map units
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
		else if (suffix.equals("km"))
			return 1000.0 * parms.z(retval);
		
		// unrecognized unit
		System.err.println(attribute + " Unit Error: got: " + suffix + ", expected: m/km");
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
	 * construct an array of booleans for which MeshPoints are in a box
	 * @param x
	 * @param y
	 * @param x2
	 * @param y2
	 * @return
	 */
	boolean[] pointsInBox(Map map, double x, double y, double x2, double y2) {
		double xMin = (x < x2) ? x : x2;
		double xMax = (x2 > x) ? x2 : x;
		double yMin = (y < y2) ? y : y2;
		double yMax = (y2 > y) ? y2 : y;
		
		boolean[] selected = new boolean[map.mesh.vertices.length];
		for(int i = 0; i < selected.length; i++) {
			MeshPoint p = map.mesh.vertices[i];
			selected[i] = (p.x >= xMin && p.x <= xMax && p.y >= yMin && p.y <= yMax);
		}
		return selected;
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
				options |= MapWindow.SHOW_TOPO;
				break;
			case 'r': case 'R':
				options |= MapWindow.SHOW_RAIN;
				break;
			case 'w': case 'W':
				options |= MapWindow.SHOW_WATER;
				break;
			case 'e': case 'E':
				options |= MapWindow.SHOW_ERODE;
				break;
			case 'm': case 'M':
				options |= MapWindow.SHOW_ROCKS;
				break;
			case 'f': case 'F':
				options |= MapWindow.SHOW_FLORA;
				break;
			case 'a': case 'A':
				options |= MapWindow.SHOW_FAUNA;
				break;
			case 'c': case 'C':
				options |= MapWindow.SHOW_CITY;
				break;
			}
		}
		return options;
	}
}
