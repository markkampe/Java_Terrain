package worldBuilder;

import java.io.FileWriter;
import java.io.IOException;

/**
 * exporter that creates an RPGMaker map
 */
public class RpgmExporter implements Exporter {

	private String filename;	// output file name
	private Parameters parms;	// general parameters
	//private String mapname;		// name of this map

	// physical map parameters
	private int x_points; 		// width of map (in points)
	private int y_points; 		// height of map (in points)
	private int tile_size; 		// tile size (in meters)
	//private double lat;			// latitude
	//private double lon;			// longitude

	// plant/terrain influencing parameters
	private double Tmean; 		// mean temperature (degC)
	private double Tsummer; 	// mean summer temperature (degC)
	private double Twinter; 	// mean winter temperature (degC)
	private double[][] rain;	// per point rainfall (meters)

	// basic ground tile selection
	private double[][] heights;	// per point height (meters)
	private double[][] erode;	// per point erosion (meters)
	private double[][] hydration; // per point water depth (meters)
	private double[][] soil;	// per point soil type

	/*
	 * RPGMaker tile address space
	 * The RPGMaker database configures a few tile sets.
	 * A tile-set is a .png containing tiles (of known size)
	 * The maximum number of tiles within a tile set is 48x16=768
	 * A tile number is the base number of its tile set
	 *   plus its offset within the tile set.
	 * In auto-tile tile-sets the images seem to come in
	 *   a well defined order (which enables corners,
	 *   borders, and other transitions)
	 *   
	 * The intention seems to be that one can replace the
	 * individual images while preserving the spatial sense,
	 * switch tile sets, and the same game will have a different
	 * look.
	private static final int A1_BASE = 2048;
	private static final int A2_BASE = A1_BASE + 768;
	private static final int A3_BASE = A2_BASE + (2*768);
	private static final int A4_BASE = A3_BASE + (2*768);
	private static final int A5_BASE = A1_BASE - 512;
	private static final int B_BASE = 0;
	private static final int C_BASE = 256;
	private static final int D_BASE = 512;
	private static final int E_BASE = 768;
	 */
	
	private int tileSet = 2;	// Outdoors 
	// L1 ground level auto-tile
	private int waterTile = 2048;
	private int sandTile = 3584;
	private int dirtTile = 2864;
	private int grassTile = 2816;
	private int snowTile = 3968;
	// L2 above ground auto-tile
	private int rocksTile = 3384;	// L2
	private int deepTile = 2096;	// L2
	private int treeTile = 3006;	// L2
	private int pineTile = 3056;	// L2
	private int palmTile = 3776;	// L2
	private int xmasTile = 4160;	// L2
	private int grassHill = 0;	// OverLand L2 only
	private int dirtHill = 0;	// OverLand L2 only
	private int snowHill = 0;	// OverLand L2 only
	private int mountainTile = 0; // OverLand L2 only
	private int peakTile = 0;	// OverLand L2 only
	private int snowPeakTile = 0; // OverLand L2 only
	
	/**
	 * create a new output writer
	 * 
	 * @param filename
	 */
	public RpgmExporter(String filename, String tileset) {
		this.filename = filename;
		this.parms = Parameters.getInstance();
		
		// load configuration for the selected tile set
		TileConfiguration c = TileConfiguration.getInstance();
		for(TileConfiguration.TileSet t:c.tilesets) {
			if (tileset.equals(t.name)) {
				tileSet = t.id;
				deepTile = t.deepNum;
				waterTile = t.waterNum;
				dirtTile = t.dirtNum;
				sandTile = t.sandNum;
				rocksTile = t.rockNum;
				snowTile = t.snowNum;
				grassTile = t.grassNum;
				grassHill = t.grassHillNum;
				dirtHill = t.dirtHillNum;
				snowHill = t.snowHillNum;
				mountainTile = t.mountainNum;
				peakTile = t.peakNum;
				snowPeakTile = t.snowPeakNum;
				return;
			}
		}
		System.err.println("ERROR - Unknown tileset: " + tileset);
	}

	/**
	 * write out an RPGMaker map
	 */
	public boolean flush() {
		double deepThreshold = -parms.deep_threshold;

		double slopeThreshold = 0.25;	// TODO move into parms
		double altThreshold = 50.0;		// TODO move into parms
		
		try {
			FileWriter output = new FileWriter(filename);
			output.write("{\n");
			boilerPlate(output);
			
			// produce the actual map of tiles
			startList(output, "data", "[");
			
			// level 1 objects on the ground
			int l[][] = new int[y_points][x_points];
			populate_l1(l);
			for(int i = 0; i < y_points; i++)
				for(int j = 0; j < x_points; j++) {
					int adjustment = auto_tile_offset(l, i, j);
					output.write(String.format("%d,", l[i][j] + adjustment));
				}
			
			// level 2 objects on the ground drawn over level 1
			populate_l2(l);
			for(int i = 0; i < y_points; i++)
				for(int j = 0; j < x_points; j++) {
					int adjustment = l[i][j] > 0 ? auto_tile_offset(l, i, j) : 0;
					output.write(String.format("%d,", l[i][j] + adjustment));	
				}
			
			// level 3 - foreground trees/structures (B/C object sets)
			populate_l3(l);
			for(int i = 0; i < y_points; i++)
				for(int j = 0; j < x_points; j++)
					output.write(String.format("%d,", l[i][j]));
			
			// level 4 - background trees/structures (B/C object sets)
			populate_l4(l);
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
	}
	
	/*
	 * L1 is the color/texture of the ground
	 */
	void populate_l1(int[][] grid) {
		double dirtThreshold = 0.10; 	// TODO parms.dirtTHreshold
		double grassThreshold = 0.25; 	// TODO parms.grassThreshold

		for (int i = 0; i < y_points; i++)
			for (int j = 0; j < x_points; j++) {
				double h = hydration[i][j];
				if (h < 0)
					grid[i][j] = waterTile;
				else if (snowy(i, j))
					grid[i][j] = snowTile;
				else if (h >= grassThreshold)
					grid[i][j] = grassTile;
				else if (h >= dirtThreshold)
					grid[i][j] = dirtTile;
				else
					grid[i][j] = sandTile;
			}
	}
	
	/*
	 * L2 is ground cover on top of the base texture
	 */
	void populate_l2(int[][] grid) {
		double min_hill = 50;		// TODO parms.min_hill
		double min_mountain = 500;	// TODO parms.min_mountain
		double min_peak = 1500;		// TODO parms.min_peak
		double deepThreshold = 15;	// TODO parms.deepThreshold
		double tree_line = 2000;	// TODO parms.tree_line
		double tree_hydro = 0.3;	// TODO parms.tree_hydro
		double connifer_alt = 1500;	// TODO parms.connifer_alt
		double dirtThreshold = 0.10; 	// TODO parms.dirtTHreshold
		double grassThreshold = 0.25; 	// TODO parms.grassThreshold

		boolean mountains = (dirtHill + mountainTile + snowPeakTile) > 0;

		for (int i = 0; i < y_points; i++)
			for (int j = 0; j < x_points; j++) {
				double h = hydration[i][j];
				double alt = heights[i][j];
				
				if (h < -deepThreshold)
					grid[i][j] = deepTile;
				else if (mountains && alt >= min_hill) {
					if (alt >= min_peak)
						grid[i][j] = snowy(i,j) ? snowPeakTile : peakTile;
					else if (alt >= min_mountain)
						grid[i][j] = mountainTile;
					else if (snowy(i,j))
						grid[i][j] = snowHill;
					else if (h >= grassThreshold)
						grid[i][j] = grassHill;
					else
						grid[i][j] = dirtHill;
				} else if (h >= tree_hydro && alt <= tree_line) {
					if (snowy(i,j))
						grid[i][j] = xmasTile;
					else if (alt >= connifer_alt)
						grid[i][j] = pineTile;
					else
						grid[i][j] = treeTile;
				} else if (rocky(i,j))
					grid[i][j] = rocksTile;
				else
					grid[i][j] = 0;
			}
	}
	
	/*
	 * L3 is trees and other impassable above-ground objects
	 */
	void populate_l3(int[][] grid) {
		// TODO populate L3 w/trees
		for(int i = 0; i < y_points; i++)
			for(int j = 0; j < x_points; j++)
				grid[i][j] = 0;
	}
	
	/*
	 * L4 is foreground to L3's background
	 */
	void populate_l4(int[][] grid) {
		for(int i = 0; i < y_points; i++)
			for(int j = 0; j < x_points; j++)
				grid[i][j] = 0;
	}

	
	/*
	 * return the slope at a point
	 */
	private double slope(int row, int col) {
		double dzdx, dzdy;
		if (col > 0)
			dzdx = (heights[row][col] - heights[row][col-1])/tile_size;
		else
			dzdx = (heights[row][col+1] - heights[row][col])/tile_size;
		if (row > 0)
			dzdy = (heights[row][col] - heights[row-1][col])/tile_size;
		else
			dzdy = (heights[row+1][col-1] - heights[row][col])/tile_size;
		return Math.sqrt((dzdx*dzdx)+(dzdy*dzdy));
	}
	
	/*
	 * likelihood of snow at a point
	 */
	private boolean snowy(int row, int col) {
		double snowThreshold = 0.3;	// FIX tunable snow threshold
		if (rain[row][col] < snowThreshold)
			return false;
		double temp = (Twinter + Tmean)/2 - (heights[row][col] * parms.lapse_rate);
		return (temp < 0);
	}
	
	/*
	 * likelihood a point is rocky
	 */
	private boolean rocky(int row, int col) {
		double v = soil[row][col];
		return( v > Map.SEDIMENTARY || v < Map.ALLUVIAL);
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
				8,	9,	34,	34,	10,	11,	37,	22,	// .*./x..	
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
	
	// attribute/info setting methods (from Exporter.java)
	public void name(String name) {
		//this.mapname = name;
	}

	public void dimensions(int x_points, int y_points) {
		this.x_points = x_points;
		this.y_points = y_points;
	}

	public void tileSize(int meters) {
		this.tile_size = meters;
	}

	public void position(double lat, double lon) {
		//this.lat = lat;
		//this.lon = lon;
	}

	public void temps(double meanTemp, double meanSummer, double meanWinter) {
		this.Tmean = meanTemp;
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
		this.rain = rain;
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
		setparm("tilesetId", String.format("%d", tileSet));

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