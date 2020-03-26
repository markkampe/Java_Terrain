package worldBuilder;

import java.io.FileWriter;
import java.io.IOException;

/**
 * write out a series of levels-of-tiles as an RPGMaker map
 */
public class RPGMwriter {

	private FileWriter out;	
	private TileRules rules;
	private int numRows;
	private int numCols;
	private int[] typeMap;
	
	public RPGMwriter(FileWriter outfile, TileRules rules) {
		out = outfile;
		this.rules = rules;
		typeMap = null;
	}
	
	public void typeMap(int[] map) {
		this.typeMap = map;
	}
	
	public void prologue( int height, int width, int tileset ) throws IOException {
		
		// patch in the array size and tile set
		setparm("height", String.format("%d", height));
		setparm("width", String.format("%d", width));
		setparm("tilesetId", String.format("%d", tileset));

		// output all the standard parameters in the standard order
		out.write("{\n");
		
		writeParmList(parms1);
		out.write(",");
		startList("bgm", "{");
		writeParmList(bgmParms);
		out.write("},");
		startList("bgs", "{");
		writeParmList(bgsParms);
		out.write("},");
		writeParmList(parms2);
		out.write(",");
		out.write("\n");
		
		numRows = height;
		numCols = width;
	}
	
	/**
	 * write out one table (numRows x numCols)
	 * 
	 * @param tiles ... the array to be written out
	 * @param last .... is this the last table to be written
	 */
	public void writeTable(int[][] tiles, boolean last) throws IOException {
		for(int i = 0; i < numRows; i++) {
			for(int j = 0; j < numCols; j++) {
				out.write(String.format("%4d", (tiles == null) ? 0 : tiles[i][j]));
				if (!last || i < numRows - 1 || j < numCols - 1)
					out.write(",");
			}
			out.write("\n");
		}
		if (!last)
			out.write("\n");
	}
	
	/**
	 * write out one table (numRows x numCols) w/autotile adjustments
	 * 
	 * @param baseTiles ... the array to be written out
	 * @param levels   ... array of types for each tile
	 *
	 * Note: there is no auto-tiling for the last level (6)
	 */
	public void writeAdjustedTable(int[][] baseTiles, int[][] levels) throws IOException {
		for(int i = 0; i < numRows; i++) {
			for(int j = 0; j < numCols; j++) {
				int v = (baseTiles == null) ? 0 : baseTiles[i][j];
				if (v != 0)
					v += auto_tile_offset(baseTiles, levels, i, j);
				out.write(String.format("%4d,", v));
			}
			out.write("\n");
		}
		out.write("\n");
	}
	
	
	
	/*
	 * examine the neighbors, identify boundaries, and figure out
	 * the appropriate tile offsets.
	 * 
	 * @param baseTiles ... base tile # for each grid square
	 * @param levels ...... level # for each grid square (may be null)
	 * @param row
	 * @param col
	 * 
	 * RPGMaker defines up to 48 different ways to slice up a reference
	 * 	tile in order to create borders between dissimilar tiles.
	 *
	 * For level changes, 
	 * 	we put walls on south high ground
	 *  we put auto-tile borders on the north and west high ground
	 *  we put shadows on the east low ground
	 *  
	 * But, for the highlands (where tile changes create barriers)
	 * 	we do not auto-tile between L1 tiles
	 */
	private int auto_tile_offset(int baseTiles[][], int levels[][], int row, int col) {
		/*
		 * this matrix decides which part of a reference tile
		 * to use for different configurations of unlike neighbors
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
		
		int bits = 0;
		int lastrow = baseTiles.length - 1;
		int lastcol = baseTiles[row].length - 1;
		int sameTile = baseTiles[row][col];
		
		/*
		 * most auto-tiling is based on the eight surrounding
		 * neighbors, but a few tiles distinguish only four
		 * interesting neighbors.
		 */
		if (rules.neighbors(sameTile) == 4) {
			bits |= (col > 0 && baseTiles[row][col-1] != sameTile) ? 1 : 0;		// left
			bits |= (row > 0 && baseTiles[row-1][col] != sameTile) ? 2 : 0;		// up
			bits |= (col < lastcol && baseTiles[row][col+1] != sameTile) ? 4 : 0;	// right
			bits |= (row < lastrow && baseTiles[row+1][col] != sameTile) ? 8 : 0;		// down
			return bits;
		}
		
		/*
		 * the primary purpose of auto-tile offsets is to create attractive 
		 * borders between dissimilar tiles.  Where we want the borders depends 
		 * on which neighbors are dissimilar.
		 */
		if (levels == null || !rules.landBarrier(sameTile)) {
			if (row > 0) {
				if (col > 0)
					bits |= (baseTiles[row-1][col-1] != sameTile) ? 1 : 0;
				bits |= (baseTiles[row-1][col] != sameTile) ? 2 : 0;
				if (col < lastcol)
					bits |= (baseTiles[row-1][col+1] != sameTile) ? 4 : 0;
			}
			if (col > 0)
				bits |= (baseTiles[row][col-1] != sameTile) ? 8 : 0;
			if (col < baseTiles[row].length - 1)
				bits |= (baseTiles[row][col+1] != sameTile) ? 16 : 0;
			if (row < lastrow) {
				if (col > 0)
					bits |= (baseTiles[row+1][col-1] != sameTile) ? 32 : 0;
				bits |= (baseTiles[row+1][col] != sameTile) ? 64 : 0;
				if (col < lastcol)
					bits |= (baseTiles[row+1][col+1] != sameTile) ? 128 : 0;
			}
		} else {
			/*
			 *  Some ground-cover tiles automatically create impassable barriers
			 *  to other tiles.  If we have multiple levels and this
			 *  is such a tile, do not create neighbor-based borders.  
			 *  In the few cases where we do want such barriers 
			 *  (e.g. land-to-water) we simply omit the barrier attribute 
			 *  on those tiles, enabling normal auto-tiling.
			 */
		}
		
		/*
		 * if neighboring tiles are on a lower level, we still want to 
		 * create a boundary between us and the downwards slope ...
		 * even if the same tile is on the lower level.
		 */
		if (levels != null) {
			int sameLevel = levels[row][col];
			if (!TerrainType.isWater(typeMap[sameLevel])) {
				if (row > 0) {
					if (col > 0)
						bits |= lowerLevel(levels, row-1, col-1, sameLevel) ? 1 : 0;
					bits |= lowerLevel(levels,  row-1, col, sameLevel) ? 2 : 0;
					if (col < lastcol)
						bits |= lowerLevel(levels, row-1, col+1, sameLevel) ? 4 : 0;
				}
				if (col > 0)
					bits |= lowerLevel(levels, row, col-1, sameLevel) ? 8 : 0;
				if (col < lastcol)
					bits |= lowerLevel(levels, row, col+1, sameLevel) ? 16 : 0;
				if (row < lastrow) {
					if (col > 0)
						bits |= lowerLevel(levels,  row+1, col-1, sameLevel) ? 32 : 0;
					bits |= lowerLevel(levels, row+1, col, sameLevel) ? 64 : 0;
					if (col < lastcol)
						bits |= lowerLevel(levels,  row+1, col+1, sameLevel) ? 128 : 0;
				}
			}
		}
		
		// index into offset array to choose which image to use
		return offset[bits];
	}
	
	// is this a downwards level change
	private boolean lowerLevel(int[][] levels, int row, int col, int ref) {
		int level = levels[row][col];
		
		// dropping down to water doesn't count as a level change
		if (TerrainType.isWater(typeMap[level]))
			return false;
		else
			return level < ref;
	}

	public void epilogue() throws IOException {
		out.write("}\n");
	}

	/*
	 * This is a kluge to produce a bunch of boiler-plate
	 * Someday we will want to choose some of these values.
	 * rumor has it that the order matters
	 */
	private static String parms1[][] = {	// background sounds/music
			{ "autoplayBgm", "boolean", "false" }, 
			{ "autoplayBgs", "boolean", "false" },
			{ "battleback1Name", "string", "" }, 
			{ "battleback2Name", "string", "" }, };
	private static String bgmParms[][] = {	// music pitch/volume
			{ "name", "string", "" }, 
			{ "pan", "int", "0" }, 
			{ "pitch", "int", "100" },
			{ "volume", "int", "90" } };
	private static String bgsParms[][] = {	// sound pitch/volume
			{ "name", "string", "" }, 
			{ "pan", "int", "0" }, 
			{ "pitch", "int", "100" },
			{ "volume", "int", "90" } };
	private static String parms2[][] = {	// processing options
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
	private void writeParmList(String parmlist[][]) throws IOException {
		for (int i = 0; i < parmlist.length; i++)
			writeParm(parmlist[i], i < parmlist.length - 1);
	}

	/**
	 * write out the start of an object or array
	 * @param name	name of List/Aray
	 * @param start	starting character
	 * @throws IOException
	 */
	public void startList(String name, String start) throws IOException {
		String[] list = new String[3];
		list[0] = name;
		list[1] = "literal";
		list[2] = start + "\n";
		writeParm(list, false);
	}
	
	/**
	 * terminate an object or array
	 * @param end   ending character
	 * @throws IOException
	 */
	public void endList(String end) throws IOException {
		out.write(end + "\n");
	}

	/**
	 * write out the value of a single parameter
	 * 
	 * @param writer
	 * @param String[3] ... name, type, value
	 * @throws IOException
	 */
	private void writeParm(String parminfo[], boolean comma) throws IOException {
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
