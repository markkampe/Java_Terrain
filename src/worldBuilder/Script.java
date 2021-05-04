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
	 * process this script
	 * @param map current Map
	 * 
	 * @return anything other than DO_NOT_EXIT is an exit code
	 */
	public int process(Map map) {
		parms = Parameters.getInstance();

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
			case "set":
				if (tokens[1] == null || tokens[2] == null)
					System.err.println(String.format("Error: %s[%d] %s - s.b. set parameter value", filename, lineNum, line));
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
					System.err.println(String.format("Error: %s[%d] %s - s.b. load filename", filename, lineNum, line));
				else
					map.read(tokens[1]);
				break;

			case "save":	// save map to specified file
				if (tokens[1] == null)
					System.err.println(String.format("Error: %s[%d] %s - s.b. save filename", filename, lineNum, line));
				else
					map.write(tokens[1]);
				break;

			case "sealevel":	// set the sea level
				if (tokens[1] == null)
					System.err.println(String.format("Error: %s[%d] %s - s.b. sealevel z/height", filename, lineNum, line));
				else
					parms.sea_level = z_value(tokens[1], tokens[0]);
				break;
					
			case "slope":
			case "mountain":
			case "canyon":
			case "river":
			case "rainfall":
			case "minerals":
			case "flora":
			case "fauna":
			case "PoI":
				System.err.println(tokens[0] + " command not yet implemented");
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
}
