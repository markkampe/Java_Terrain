package worldBuilder;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class Script {
	private Parameters parms;	// configuration info
	
	private BufferedReader r;	// open file being processed
	private String filename;	// name of script being processed
	private int lineNum;		// line number being processed
	private String[] tokens;	// lexed tokens from current line
	
	private static final int MAX_TOKENS = 10;
	private static final int SCRIPT_DEBUG = 2;
	
	public static final int DO_NOT_EXIT = 1024;

	public Script(String filename) {
		this.filename = filename;
		this.parms = Parameters.getInstance();
		
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
		tokens = new String[MAX_TOKENS];
		lineNum = 0;
		String line;
		while( true ) {
			try {
				line = r.readLine();
				if (line == null) {
					r.close();
					if (parms.debug_level > 0)
						System.out.println("Executed " + lineNum + " lines from " + filename);
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
			
			if (parms.debug_level >= SCRIPT_DEBUG)
				System.out.println(" ... " + line);
			
			// process the command
			switch(tokens[0]) {
			case "set":
				System.out.println(">>> set " + tokens[1] + " = " + tokens[2]);
				break;
				
			case "load":	// load the specified map
				map.read(tokens[1]);
				break;
				
			case "save":	// save map to specified file
				map.write(tokens[1]);
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
				System.out.println(lineNum + ": " + tokens[0]);
				for(int i = 1; i < MAX_TOKENS; i++)
					if (tokens[i] != null)
						System.out.println("    " + tokens[i]);
				break;
				
			case "exit":	// optional exit code argument
				if (parms.debug_level > 0)
					System.out.println("Executed " + lineNum + " lines from " + filename);
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
}
