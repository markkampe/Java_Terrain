package worldBuilder;

import java.io.FileWriter;
import java.io.IOException;

/**
 * exporter that creates an RPGMaker map
 */
public class RpgmExporter implements Exporter {

	private Parameters parms;	// world builder parameters

	private String filename;	// output file name
	private String mapname;		// name of this map

	// physical map parameters
	private int x_points; 		// width of map (in points)
	private int y_points; 		// height of map (in points)
	private int tile_size; 		// tile size (in meters)
	private double lat;			// latitude
	private double lon;			// longitude

	// plant influencing parameters
	private double Tmean; 		// mean temperature (degC)
	private double Tsummer; 	// mean summer temperature
	private double Twinter; 	// mean winter temperature
	private double[][] rain;	// per point rainfall (meters)

	// basic ground tile selection
	private double[][] heights;	// per point height (meters)
	private double[][] erode;	// per point erosion (meters)
	private double[][] hydration; // per point water depth (meters)
	private double[][] soil;	// per point soil type

	// RPGMaker tile address space
	private static final int tileSet = 1;
	private static final int A0_BASE = 0;	// auto-tile ground cover
	private static final int DIRT_BASE = 0;
	private static final int GRASS_BASE = 0;
	private static final int ROCK_BASE = 0;
	private static final int WATER_BASE = 0;
	private static final int SNOW_BASE = 0;
	private static final int MARSH_BASE = 0;
	
	private static final int A1_BASE = 0;	// tile sets
	private static final int B_BASE = 0;
	private static final int C_BASE = 0;
	private static final int D_BASE = 0;

	/**
	 * create a new output writer
	 * 
	 * @param filename
	 */
	public RpgmExporter(String filename) {
		this.filename = filename;
		parms = Parameters.getInstance();
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
			
			// level 0 - ground level
			for(int i = 0; i < y_points; i++)
				for(int j = 0; j < x_points; j++) {
					output.write("0,");	// TODO terrain tiles
				}
			
			// level 1 - objects on the ground
			for(int i = 0; i < y_points; i++)
				for(int j = 0; j < x_points; j++)
					output.write("0,");	// TODO rock and tree tiles
			
			// level 2 - ground level structures
			for(int i = 0; i < y_points; i++)
				for(int j = 0; j < x_points; j++)
					output.write("0,");
			
			// level 3 - above ground level structures
			for(int i = 0; i < y_points; i++)
				for(int j = 0; j < x_points; j++)
					output.write("0,");
			
			// level 4 - shadows
			//	UL = 1, UR = 2, BL = 4, BR = 8. 
			for(int i = 0; i < y_points; i++)
				for(int j = 0; j < x_points; j++)
					output.write("0,");
			
			// level 5 - encounters
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

	// attribute/info setting methods (from Exporter.java)
	public void name(String name) {
		this.mapname = name;
	}

	public void dimensions(int x_points, int y_points) {
		this.x_points = x_points;
		this.y_points = y_points;
	}

	public void tileSize(int meters) {
		this.tile_size = meters;

	}

	public void position(double lat, double lon) {
		this.lat = lat;
		this.lon = lon;
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