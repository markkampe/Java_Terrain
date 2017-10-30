package worldBuilder;

import java.io.FileWriter;
import java.io.IOException;

/**
 * exporter that creates an RPGMaker map
 */
public class RpgmExporter implements Exporter {

	private String filename;	// output file name
	//private String mapname;		// name of this map

	// physical map parameters
	private int x_points; 		// width of map (in points)
	private int y_points; 		// height of map (in points)
	//private int tile_size; 		// tile size (in meters)
	//private double lat;			// latitude
	//private double lon;			// longitude

	// plant influencing parameters
	//private double Tmean; 		// mean temperature (degC)
	//private double Tsummer; 	// mean summer temperature
	//private double Twinter; 	// mean winter temperature
	//private double[][] rain;	// per point rainfall (meters)

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
	
	// TODO configurable tileset information
	//	NOTE: choosing a single base tile chooses what can border it
	private static final int tileSet = 2;
	private int deepTile = 2096;
	private int waterTile = 2048;
	private int dirtTile = 2864;
	private int grassTile = 2816;
	
	// TODO configurable terrain thresholds
	private static final double deepThreshold = -3.0;
	private static final double grassThreshold = 0.25;
	
	/**
	 * create a new output writer
	 * 
	 * @param filename
	 */
	public RpgmExporter(String filename) {
		this.filename = filename;
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
			int l1[][] = new int[y_points][x_points];
			int l2[][] = new int[y_points][x_points];
			
			// figure out the base ground cover
			for(int i = 0; i < y_points; i++)
				for(int j = 0; j < x_points; j++) {
					double h = hydration[i][j];
					if (h <= deepThreshold)
						l2[i][j] = deepTile;
					if (h < 0)
						l1[i][j] = waterTile;
					else if (h >= grassThreshold)
						l1[i][j] = grassTile;
					else
						l1[i][j] = dirtTile;
				}
			
			// level 1 objects on the ground
			for(int i = 0; i < y_points; i++)
				for(int j = 0; j < x_points; j++) {
					int adjustment = auto_tile_offset(l1, i, j);
					output.write(String.format("%d,", l1[i][j] + adjustment));
				}
			
			// level 2 objects on the ground drawn over level 1
			for(int i = 0; i < y_points; i++)
				for(int j = 0; j < x_points; j++) {
					int adjustment = l2[i][j] > 0 ? auto_tile_offset(l2, i, j) : 0;
					output.write(String.format("%d,", l2[i][j] + adjustment));	
				}
			
			// TODO add rocks and mountains
			// level 3 - foreground trees/structures (B/C object sets)
			for(int i = 0; i < y_points; i++)
				for(int j = 0; j < x_points; j++)
					output.write("0,");
			
			// level 4 - background trees/structures (B/C object sets)
			for(int i = 0; i < y_points; i++)
				for(int j = 0; j < x_points; j++)
					output.write("0,");
			
			// level 5 - shadows
			//	UL = 1, UR = 2, BL = 4, BR = 8. 
			for(int i = 0; i < y_points; i++)
				for(int j = 0; j < x_points; j++)
					output.write("0,");
			
			// level 6 - encounters
			for(int i = 0; i < y_points; i++)
				for(int j = 0; j < x_points; j++) {
					output.write("0");
					if (i < y_points - 1 || j < x_points - 1)
						output.write(",");
				}
			output.write("],\n");
			
			// write out the events list
			startList(output, "events", "[\n");
			output.write("null,\nnull\n");
			output.write("]\n");
			
			// terminate the file
			output.write("}\n");
			output.close();
			return true;
		} catch (IOException e) {
			return false;
		}
	}
	
	/*
	 * examine the neighbors, identify boundaries, and figure out
	 * the appropriate tile offsets.
	 * 
	 * RPGMaker defines 48 different ways to slice up a reference
	 * 	tile in order to create borders between them
	 */
	int auto_tile_offset(int map[][], int row, int col) {
		int offset[] = {
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
		if (row > 0) {
			if (col > 0)
				bits |= (map[row-1][col-1] != same) ? 1 : 0;
			bits |= (map[row-1][col] != same) ? 2 : 0;
			if (col < lastcol)
				bits |= (map[row-1][col+1] != same) ? 4 : 0;
		}
		if (col > 0)
			bits |= (map[row][col-1] != same) ? 8 : 0;
		if (col < lastcol)
			bits |= (map[row][col+1] != same) ? 16 : 0;
		
		if (row < lastrow) {
			if (col > 0)
				bits |= (map[row+1][col-1] != same) ? 32 : 0;
			bits |= (map[row+1][col] != same) ? 64 : 0;
			if (col < lastcol)
				bits |= (map[row+1][col+1] != same) ? 128 : 0;
		}
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
		//this.tile_size = meters;
	}

	public void position(double lat, double lon) {
		//this.lat = lat;
		//this.lon = lon;
	}

	public void temps(double meanTemp, double meanSummer, double meanWinter) {
		//this.Tmean = meanTemp;
		//this.Tsummer = meanSummer;
		//this.Twinter = meanWinter;
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