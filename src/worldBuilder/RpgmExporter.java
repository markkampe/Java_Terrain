package worldBuilder;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ListIterator;
import java.util.Random;

/**
 * exporter that creates an RPGMaker map
 */
public class RpgmExporter implements Exporter {

	private static final int MAXRULES = 20;
	
	private String filename;	// output file name
	private Parameters parms;	// general parameters
	private TileRules rules;	// tile selection rules
	private Random random;		// random # generator
	
	// physical map parameters
	private int x_points; 		// width of map (in points)
	private int y_points; 		// height of map (in points)
	private int tile_size; 		// tile size (in meters)
	private double lat;			// latitude
	private double lon;			// longitude

	// tile selection parameters
	//private double Tmean; 		// mean temperature (degC)
	private double Tsummer; 	// mean summer temperature (degC)
	private double Twinter; 	// mean winter temperature (degC)
	//private double[][] rain;	// per point rainfall (meters)
	private double[][] heights;	// per point height (meters)
	private double[][] erode;	// per point erosion (meters)
	private double[][] hydration; // per point water depth (meters)
	private double[][] soil;	// per point soil type

	
	/**
	 * create a new output writer
	 * 
	 * @param filename
	 */
	public RpgmExporter(String filename, String tileset, int width, int height) {
		this.parms = Parameters.getInstance();
		
		this.filename = filename;
		this.x_points = width;
		this.y_points = height;
		
		this.rules = new TileRules(tileset);
	}

	/**
	 * write out an RPGMaker map
	 */
	public boolean flush() {
		random = new Random((int) (lat * lon * 1000));
		try {
			FileWriter output = new FileWriter(filename);
			output.write("{\n");
			boilerPlate(output);
			
			// produce the actual map of tiles
			startList(output, "data", "[");
			
			// level 1 objects on the ground
			int l[][] = new int[y_points][x_points];
			tiles(l, 1);
			for(int i = 0; i < y_points; i++)
				for(int j = 0; j < x_points; j++) {
					int adjustment = auto_tile_offset(l, i, j);
					output.write(String.format("%d,", l[i][j] + adjustment));
				}
			
			// level 2 objects on the ground drawn over level 1
			tiles(l, 2);
			for(int i = 0; i < y_points; i++)
				for(int j = 0; j < x_points; j++) {
					int adjustment = l[i][j] > 0 ? auto_tile_offset(l, i, j) : 0;
					output.write(String.format("%d,", l[i][j] + adjustment));	
				}
			
			// level 3 - foreground mountains/trees/structures (B/C object sets)
			stamps(l, 3);
			for(int i = 0; i < y_points; i++)
				for(int j = 0; j < x_points; j++)
					output.write(String.format("%d,", l[i][j]));
			
			// level 4 - background mountains/trees/structures (B/C object sets)
			stamps(l, 4);
			for(int i = 0; i < y_points; i++)
				for(int j = 0; j < x_points; j++)
					output.write(String.format("%d,", l[i][j]));
			
			// level 5 - shadows ... come later (w/walls)
			//	UL = 1, UR = 2, BL = 4, BR = 8. 
			for(int i = 0; i < y_points; i++)
				for(int j = 0; j < x_points; j++)
					output.write("0,");
			
			// level 6 - encounters ... to be created later
			for(int i = 0; i < y_points; i++)
				for(int j = 0; j < x_points; j++) {
					output.write("0");
					if (i < y_points - 1 || j < x_points - 1)
						output.write(",");
				}
			output.write("],\n");
			
			// Events list ... to be created later
			startList(output, "events", "[\n");
			output.write("null,\n");
			output.write("null\n");
			output.write("]\n");
			
			// terminate the file
			output.write("}\n");
			output.close();
			return true;
		} catch (IOException e) {
			System.err.println("ERROR - unable to write output file: " + filename);
			return false;
		}
		
		// TODO update MapInfo.json
		//	find it in the same directory as the map
		//	read it in
		//	look for an entry for the current map
		//	update it (w/location), adding it as necessary
		//	rewrite
	}
	
	/*
	 * L1 is the color/texture of the ground, L2 is on the ground
	 */
	void tiles(int[][] grid, int level) {	
		Bidder bidder = new Bidder(MAXRULES);
		
		for (int i = 0; i < y_points; i++)
			for (int j = 0; j < x_points; j++) {
				grid[i][j] = 0;
				int alt = (int) parms.altitude(heights[i][j] - erode[i][j]);
				double lapse = alt * parms.lapse_rate;
				bidder.reset();
				int bids = 0;
				int possibles = 0;
				if (parms.debug_level >2)
					System.out.println("l" + level + "[" + i + "," + j + "]: " +
						" alt=" + alt + ", hyd=" + 
						String.format("%.2f", hydration[i][j]) + 
						String.format(", temp=%.1f-%.1f", Twinter - lapse, Tsummer - lapse) +
						", soil=" + soil[i][j] + ", slope=" + slope(i,j));
				for( ListIterator<TileRule> it = rules.rules.listIterator(); it.hasNext();) {
					// find rules applicable to this level
					TileRule r = it.next();
					if (r.level != level)
						continue;
					// ask each applicable rule for its bid
					int bid = r.bid(alt, hydration[i][j], Twinter - (int) lapse, Tsummer - (int) lapse, 
							soil[i][j], slope(i,j), direction(i,j));
					if (parms.debug_level > 2)
						System.out.println(r.ruleName + "[" + i + "," + j + "] (" + r.baseTile + ") bids " + bid);
					if (bid > 0) {
						bidder.bid(r.baseTile, bid);
						bids++;
					}
					possibles++;
				}
				if (bids != 0) {
					int winner = bidder.winner(random.nextFloat());
					grid[i][j] = winner;
					if (parms.debug_level > 2)
						System.out.println("    winner = " + winner);
				} else if (level == 1) {	// this shouldn't happen
					System.err.println("ERROR: l1[" + i + "," + j + "] bids = 0/" + possibles);
					if (parms.debug_level <= 2)
						System.err.println("    alt=" + alt + ", hyd=" + 
								String.format("%.2f", hydration[i][j]) + 
								String.format(", temp=%.1f-%.1f", Twinter - lapse, Tsummer - lapse) +
								", soil=" + soil[i][j]);
				}
			}
	}
	
	/*
	 * L4 is stamps and L3 fills in between them
	 */
	void stamps(int[][] grid, int level) {	
		Bidder bidder = new Bidder(MAXRULES);
		
		for (int i = 0; i < y_points; i++)
			for (int j = 0; j < x_points; j++) {
				grid[i][j] = 0;
				int alt = (int) parms.altitude(heights[i][j] - erode[i][j]);
				double lapse = alt * parms.lapse_rate;
				bidder.reset();
				int bids = 0;
				int possibles = 0;
				for( ListIterator<TileRule> it = rules.rules.listIterator(); it.hasNext();) {
					// find rules applicable to this level
					TileRule r = it.next();
					if (r.level != level)
						continue;
					// ask each applicable rule for its bid
					int bid = r.bid(alt, hydration[i][j], Twinter - (int) lapse, Tsummer - (int) lapse, 
							soil[i][j], slope(i,j), direction(i,j));
					if (parms.debug_level > 2)
						System.out.println(r.ruleName + "[" + i + "," + j + "] (" + r.baseTile + ") bids " + bid);
					if (bid > 0) {
						bidder.bid(r.baseTile, bid);
						bids++;
					}
					possibles++;
				}
			}
	}
	
	
	/*
	 * return the slope at a point
	 */
	private double slope(int row, int col) {
		double z0 = heights[row][col] - erode[row][col];
		double zx1 = (col > 0) ? heights[row][col-1] - erode[row][col-1] :
								 heights[row][col+1] - erode[row][col+1];
		double zy1 = (row > 0) ? heights[row-1][col] - erode[row-1][col] :
								 heights[row+1][col] - erode[row+1][col];
		double dz = Math.sqrt((z0-zx1)*(z0-zx1) + (z0-zy1)*(z0-zy1));
		return Math.abs(parms.altitude(dz) / tile_size);
	}
	
	/**
	 * slope upwards to the east
	 */
	public double dZdX(int row, int col) {
		if (col == x_points - 1)
			col--;
		double dz = (heights[row][col+1] - erode[row][col+1]) - (heights[row][col] - erode[row][col]) ;
		double dx = tile_size;
		return dz/dx;
	}

	/**
	 * slope upwards to the south
	 */
	public double dZdY(int row, int col) {
		if (row == y_points - 1)
			row--;
		double dz = (heights[row+1][col] - erode[row+1][col]) - (heights[row][col] - erode[row][col]) ;
		double dy = tile_size;
		return dz/dy;
	}
	
	/**
	 * compass orientation of face
	 * @return
	 */
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
	
	/*
	 * examine the neighbors, identify boundaries, and figure out
	 * the appropriate tile offsets.
	 * 
	 * RPGMaker defines 48 different ways to slice up a reference
	 * 	tile in order to create borders between them
	 */
	private int auto_tile_offset(int map[][], int row, int col) {
		/*
		 * this matrix decides which part of a reference tile
		 * to use for different configurations of unlike tiles
		 */
		int offset[] = {
			// .=same, x=different, *=tile in question
			// TOP ROW							// MID/BOTTOM
			// ... x.. .x. xx. ..x x.x .xx xxx 
				0,	1,	20,	20,	2,	3,	20,	20,	// .*./...	
				16,	16,	34,	34,	17,	17,	34,	34,	// x*./...	
				24,	26,	36,	36,	24,	26,	36,	36,	// .*x/...	
				32,	32,	42,	42,	32,	32,	42,	42,	// x*x/...	
				8,	9,	22,	22,	10,	11,	22,	22,	// .*./x..	
				16,	16,	34,	34,	17,	17,	34,	34,	// x*./x..	
				25,	27,	37,	37,	25,	27,	37,	37,	// .*x/x..	
				32,	32,	42,	42,	32,	32,	42,	42,	// x*x/x..
				28,	29,	33,	33,	30,	31,	33,	33,	// .*./.x.	
				40,	40,	43,	43,	41,	41,	43,	43,	// x*./.x.	
				38,	39,	45,	45,	38,	39,	45,	45,	// .*x/.x.	
				44,	44,	46,	46,	44,	44,	46,	46,	// x*x/.x.
				28,	29,	33,	33,	30,	31,	33,	33,	// .*./xx.	
				40,	40,	43,	43,	41,	41,	43,	43,	// x*./xx.	
				38,	39,	45,	45,	38,	39,	45,	45,	// .*x/xx.	
				44,	44,	46,	46,	44,	44,	46,	46,	// x*x/xx.
			// ... x.. .x. xx. ..x x.x .xx xxx 
				4,	5,	21,	21,	6,	7,	21,	21,	// .*./..x	
				18,	18,	35,	35,	19,	19,	35,	35,	// x*./..x	
				24,	26,	36,	36,	24,	26,	36,	36,	// .*x/..x	
				32,	32,	42,	42,	32,	32,	42,	42,	// x*x/..x	
				12,	13,	23,	23,	14,	15,	23,	23,	// .*./x.x	
				18,	18,	35,	35,	19,	19,	35,	35,	// x*./x.x	
				25,	27,	37,	37,	25,	27,	37,	37,	// .*x/x.x	
				32,	32,	42,	42,	32,	32,	42,	42,	// x*x/x.x
				28,	29,	33,	33,	30,	31,	33,	33,	// .../.xx	
				40,	40,	43,	43,	41,	41,	43,	43,	// x*./.xx	
				38,	39,	45,	45,	38,	39,	45,	45,	// .*x/.xx	
				44,	44,	46,	46,	44,	44,	46,	46,	// x*x/.xx
				28,	29,	33,	33,	30,	31,	33,	33,	// .../xxx	
				40,	40,	43,	43,	41,	41,	43,	43,	// x*./xxx	
				38,	39,	45,	45,	38,	39,	45,	45,	// .*x/xxx	
				44,	44,	46,	46,	44,	44,	46,	46,	// x*x/xxx
		};
		
		// look at neighbors to identify boundaries
		int bits = 0;
		int lastrow = map.length - 1;
		int lastcol = map[row].length - 1;
		int same = map[row][col];
		// top row ... left to right
		if (row > 0) {
			if (col > 0)
				bits |= (map[row-1][col-1] != same) ? 1 : 0;
			bits |= (map[row-1][col] != same) ? 2 : 0;
			if (col < lastcol)
				bits |= (map[row-1][col+1] != same) ? 4 : 0;
		}
		// middle row ... left to right
		if (col > 0)
			bits |= (map[row][col-1] != same) ? 8 : 0;
		if (col < lastcol)
			bits |= (map[row][col+1] != same) ? 16 : 0;
		// bottom row ... left to right
		if (row < lastrow) {
			if (col > 0)
				bits |= (map[row+1][col-1] != same) ? 32 : 0;
			bits |= (map[row+1][col] != same) ? 64 : 0;
			if (col < lastcol)
				bits |= (map[row+1][col+1] != same) ? 128 : 0;
		}
		
		// index into offset array to choose which image to use
		return offset[bits];
	}

	public void tileSize(int meters) {
		this.tile_size = meters;
	}

	public void position(double lat, double lon) {
		this.lat = lat;
		this.lon = lon;
	}

	public void temps(double meanTemp, double meanSummer, double meanWinter) {
		//this.Tmean = meanTemp;
		this.Tsummer = meanSummer;
		this.Twinter = meanWinter;
	}

	public void heightMap(double[][] heights) {
		this.heights = heights;
	}

	public void erodeMap(double[][] erode) {
		this.erode = erode;
	}

	public void rainMap(double[][] rain) {
		//this.rain = rain;
	}

	public void soilMap(double[][] soil) {
		this.soil = soil;
	}

	public void waterMap(double[][] hydration) {
		this.hydration = hydration;
	}

	/**
	 * standard map prolog/epilog
	 * 
	 * @param out
	 * @throws IOException
	 */
	private void boilerPlate(FileWriter out) throws IOException {
		// patch in the array height and width
		setparm("height", String.format("%d", y_points));
		setparm("width", String.format("%d", x_points));
		setparm("tilesetId", String.format("%d", rules.tileset));

		// output all the standard parameters in the standard order
		writeParmList(out, parms1);
		out.write(",");
		startList(out, "bgm", "{");
		writeParmList(out, bgmParms);
		out.write("},");
		startList(out, "bgs", "{");
		writeParmList(out, bgsParms);
		out.write("},");
		writeParmList(out, parms2);
		out.write(",");
	}

	// This is a kluge to produce a bunch of boiler-plate
	// Someday we will want to produce this stuff intelligently
	// rumor has it that the order matters
	private static String parms1[][] = { 
			{ "autoplayBgm", "boolean", "false" }, 
			{ "autoplayBgs", "boolean", "false" },
			{ "battleback1Name", "string", "" }, 
			{ "battleback2Name", "string", "" }, };
	private static String bgmParms[][] = { 
			{ "name", "string", "" }, 
			{ "pan", "int", "0" }, 
			{ "pitch", "int", "100" },
			{ "volume", "int", "90" } };
	private static String bgsParms[][] = { 
			{ "name", "string", "" }, 
			{ "pan", "int", "0" }, 
			{ "pitch", "int", "100" },
			{ "volume", "int", "90" } };
	private static String parms2[][] = { 
			{ "disableDashing", "boolean", "false" }, 
			{ "displayName", "string", "" },
			{ "encounterList", "array", "" }, 
			{ "encounterStep", "int", "30" }, 
			{ "height", "int", "0" },
			{ "note", "string", "" }, 
			{ "parallaxLoopX", "boolean", "false" }, 
			{ "parallaxLoopY", "boolean", "false" },
			{ "parallaxName", "string", "" }, 
			{ "parallaxShow", "boolean", "true" }, 
			{ "parallaxSx", "int", "0" },
			{ "parallaxSy", "int", "0" }, 
			{ "scrollType", "int", "0" }, 
			{ "specifyBattleback", "boolean", "false" },
			{ "tilesetId", "int", "1" }, 
			{ "width", "int", "0" } };

	/**
	 * write out a list of parameters
	 * 
	 * @param writer
	 * @param String[][3]
	 *            parameters to be written out
	 * @throws IOException
	 */
	private void writeParmList(FileWriter out, String parmlist[][]) throws IOException {
		for (int i = 0; i < parmlist.length; i++)
			writeParm(out, parmlist[i], i < parmlist.length - 1);
	}

	/**
	 * write out the start of an object or array
	 * @param out	FileWriter
	 * @param name	name of List/Aray
	 * @param start	starting character
	 * @throws IOException
	 */
	private void startList(FileWriter out, String name, String start) throws IOException {
		String[] list = new String[3];
		list[0] = name;
		list[1] = "literal";
		list[2] = start;
		writeParm(out, list, false);
	}

	/**
	 * write out the value of a single parameter
	 * 
	 * @param writer
	 * @param String[3] ... name, type, value
	 * @throws IOException
	 */
	private void writeParm(FileWriter out, String parminfo[], boolean comma) throws IOException {
		out.write("\"");
		out.write(parminfo[0]);
		out.write("\":");
		switch (parminfo[1]) {
		case "string":
			out.write("\"");
			out.write(parminfo[2]);
			out.write("\"");
			break;
		case "int":
		case "boolean":
		case "literal":
			out.write(parminfo[2]);
			break;
		case "array":
			out.write("[]");
			break;
		}
		if (comma)
			out.write(",");
	}

	/**
	 * patch a new value into one of the parameter lists
	 * 
	 * @param parameter
	 * @param value
	 */
	private void setparm(String parameter, String value) {
		for (int i = 0; i < parms2.length; i++)
			if (parameter.equals(parms2[i][0])) {
				parms2[i][2] = value;
				return;
			}
	}
}