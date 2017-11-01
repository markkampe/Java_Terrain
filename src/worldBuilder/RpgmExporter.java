package worldBuilder;

import java.io.FileWriter;
import java.io.IOException;

/**
 * exporter that creates an RPGMaker map
 */
public class RpgmExporter implements Exporter {

	private String filename;	// output file name
	private Parameters parms;	// general parameters
	private TileConfiguration.TileSet tiles;// tile set information
	
	// physical map parameters
	private int x_points; 		// width of map (in points)
	private int y_points; 		// height of map (in points)
	private int tile_size; 		// tile size (in meters)
	//private double lat;			// latitude
	//private double lon;			// longitude

	// plant/terrain influencing parameters
	private double Tmean; 		// mean temperature (degC)
	//private double Tsummer; 	// mean summer temperature (degC)
	private double Twinter; 	// mean winter temperature (degC)
	private double[][] rain;	// per point rainfall (meters)

	// basic ground tile selection
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
		this.filename = filename;
		this.parms = Parameters.getInstance();
		this.x_points = width;
		this.y_points = height;
		
		// load configuration for the selected tile set
		TileConfiguration c = TileConfiguration.getInstance();
		for(TileConfiguration.TileSet t:c.tilesets) {
			if (tileset.equals(t.name)) {
				tiles = t;
				return;
			}
		}
		System.err.println("ERROR - Unknown tileset: " + tileset);
	}

	/**
	 * write out an RPGMaker map
	 */
	public boolean flush() {
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
		double dirtThreshold = parms.dirt_hydro;
		double grassThreshold = parms.grass_hydro;

		for (int i = 0; i < y_points; i++)
			for (int j = 0; j < x_points; j++) {
				double h = hydration[i][j];
				if (h < 0)
					grid[i][j] = tiles.waterNum;
				else if (snowy(i, j))
					grid[i][j] = tiles.snowNum;
				else if (h >= grassThreshold)
					grid[i][j] = tiles.grassNum;
				else if (rocky(i,j))
					grid[i][j] = tiles.rockNum;
				else if (h >= dirtThreshold)
					grid[i][j] = tiles.dirtNum;
				else
					grid[i][j] = tiles.sandNum;
			}
	}
	
	/*
	 * L2 is ground cover on top of the base texture
	 */
	void populate_l2(int[][] grid) {
		double min_hill = parms.min_hill;
		double min_mountain = parms.min_mountain;
		double min_peak = parms.min_peak;
		double min_slope = parms.min_slope;
		double deepThreshold = parms.deep_water;
		double tree_line = parms.tree_line;
		double tree_hydro = parms.tree_hydro;
		double connifer_alt = parms.pine_line;
		double grassThreshold = parms.grass_hydro;

		// does our tileset support mountains and hills?
		boolean mountains = (tiles.dirtHillNum + tiles.mountainNum + tiles.snowPeakNum) > 0;

		for (int i = 0; i < y_points; i++)
			for (int j = 0; j < x_points; j++) {
				double h = hydration[i][j];
				double alt = parms.altitude(heights[i][j] - erode[i][j]);
				double m = slope(i,j);
				
				if (h < 0)
					grid[i][j] = (h <= deepThreshold) ? tiles.deepNum : 0;
				else if (mountains && alt >= min_hill && m >= min_slope) {
					if (alt >= min_peak)
						grid[i][j] = snowy(i,j) ? tiles.snowPeakNum : tiles.peakNum;
					else if (alt >= min_mountain)
						grid[i][j] = tiles.mountainNum;
					else if (snowy(i,j))
						grid[i][j] = tiles.snowHillNum;
					else if (h >= grassThreshold)
						grid[i][j] = tiles.grassHillNum;
					else
						grid[i][j] = tiles.dirtHillNum;
				} else if (h >= tree_hydro && alt <= tree_line) {
					if (snowy(i,j))
						grid[i][j] = tiles.xmasNum;
					else if (alt >= connifer_alt)
						grid[i][j] = tiles.pineNum;
					else
						grid[i][j] = tiles.treeNum;
				} else
					grid[i][j] = 0;
			}
	}
	
	/*
	 * L3 is trees and other impassable above-ground objects
	 */
	void populate_l3(int[][] grid) {
		// TODO populate L3 of area maps w/trees
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
		double z0 = heights[row][col] - erode[row][col];
		double zx1 = (col > 0) ? heights[row][col-1] - erode[row][col-1] :
								 heights[row][col+1] - erode[row][col+1];
		double zy1 = (row > 0) ? heights[row-1][col] - erode[row-1][col] :
								 heights[row+1][col] - erode[row+1][col];
		double dz = Math.sqrt((z0-zx1)*(z0-zx1) + (z0-zy1)*(z0-zy1));
		return parms.altitude(dz) / tile_size;
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

	public void tileSize(int meters) {
		this.tile_size = meters;
	}

	public void position(double lat, double lon) {
		//this.lat = lat;
		//this.lon = lon;
	}

	public void temps(double meanTemp, double meanSummer, double meanWinter) {
		this.Tmean = meanTemp;
		//this.Tsummer = meanSummer;
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
		setparm("tilesetId", String.format("%d", tiles.id));

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