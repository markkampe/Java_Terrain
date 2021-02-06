package RPGMdump;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import javax.json.Json;
import javax.json.stream.JsonParser;

public class RPGMreader {
	/**
	 *	read an RPGM map into a set of per-level Cartesian arrays of tile #s
	 */
	private int height;
	private int width;
	private int tilesetID;
	private int[][][] levels;

	public RPGMreader(String filename) {
		
		BufferedReader r;
		JsonParser parser;
		
		try {
			r = new BufferedReader(new FileReader(filename));
		} catch (FileNotFoundException e) {
			System.out.println("ERROR: unable to open RPGMaker map " + filename);
			return;
		}
		
		String thisKey = "";
		boolean inData = false;
		int dataItems = 0;
		parser = Json.createParser(r);
		while (parser.hasNext()) {
			JsonParser.Event e = parser.next();
			switch (e) {
			
			case KEY_NAME:
				thisKey = parser.getString();
				break;
				
			case VALUE_NUMBER:
				switch(thisKey) {
				case "height":
					height = parser.getInt();
					break;
				case "width":
					width = parser.getInt();
					break;
				case "tilesetId":
					tilesetID = parser.getInt();
					break;
				case "data":
					int l = dataItems/(height * width);
					int row = (dataItems%(height * width))/width;
					int col = dataItems % width;
					levels[l][row][col] = parser.getInt();
					dataItems++;
					break;
				}
				break;
				
			case START_ARRAY:
				if (thisKey.equals("data")) {
					inData = true;
					levels = new int[6][height][width];
				}
				break;
				
			case END_ARRAY:
				if (inData) {
					if (dataItems != 6 * height * width)
						System.err.println("DATA read error, expected " + 6 * height * width + ", got " + dataItems);
					inData = false;
				}
				break;
				
			case START_OBJECT:
				break;
				
			case END_OBJECT:
				break;

			case VALUE_STRING:
				break;
				
			default:
				break;
			}
		}
	}

	public int height() {
		return height;
	}
	
	public int width() {
		return width;
	}
	
	public int tileSet() {
		return tilesetID;
	}
	
	public int[][] getLevel(int whichlevel) {
		return levels[whichlevel-1];
	}
}
