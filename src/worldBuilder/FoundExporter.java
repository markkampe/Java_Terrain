package worldBuilder;

import java.awt.Color;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Exporter to render a Cartesian map w/JSON descriptions of each point.
 */
public class FoundExporter implements Exporter {	

	private Parameters parms;
	
	private static final int x_points = 1024;	// width of map (in points)
	private static final int y_points = 1024;	// height of map (in points)
	
	private double Tmean;			// mean temperature
	private double Tsummer;			// mean summer temperature
	private double Twinter;			// mean winter temperature
	
	private double[][] heights;		// per point height (meters)
	private double[][] erode;		// per point erosion (meters)
	private double[][] hydration;	// per point water depth (meters)
	private double[][] soil;		// per point soil type
	
	private double maxHeight;		// highest discovered altitude
	private double minHeight;		// lowest discovered altitude
	private double maxDepth;		// deepest discovered water
	
	// brightness constants for preview colors
	private static final int DIM = 32;
	private static final int BRIGHT = 256 - DIM;
	private static final int NORMAL = 128;
	
	private static final int EXPORT_DEBUG = 2;
	
	/**
	 * create a new Foundation exporter
	 * 
	 * @param width of the export area (in tiles)
	 * @param height of the export area ((in tiles)
	 */
	public FoundExporter(int width, int height) {
		if (width != x_points || height != y_points)
			System.err.println("ERROR: all Foundation exports are 1024x1024");
		parms = Parameters.getInstance();
	}

	/**
	 * Set the size of a single tile
	 * @param meters real-world width of a tile
	 */
	public void tileSize(int meters) {
		// Foundation tile size is fixed at 1 meter
	}

	/**
	 * Set the lat/lon of the region being exported
	 * @param lat real world latitude of map center
	 * @param lon real world longitude of map center
	 */
	public void position(double lat, double lon) {
		// these have no place in Foundation
	}

	/**
	 * Set seasonal temperature range for region being exported
	 * @param meanTemp	mean (all year) temperature
	 * @param meanSummer	mean (summer) temperature
	 * @param meanWinter	mean (winter) temperature
	 */
	public void temps(double meanTemp, double meanSummer, double meanWinter) {
		this.Tmean = meanTemp;
		this.Tsummer = meanSummer;
		this.Twinter = meanWinter;
	}

	/**
	 * Up-load the altitude of every tile
	 * @param heights	height (in meters) of every point
	 */
	public void heightMap(double[][] heights) {
		this.heights = heights;
	}

	/**
	 * Up-load the net erosion/deposition for every tile
	 * @param erode	per point height (in meters) of soil lost to erosion
	 * 		negative means sedimentqation
	 */
	public void erodeMap(double[][] erode) {
		this.erode = erode;	
	}

	/**
	 * Up-load the annual rainfall for every tile
	 * @param rain	per point depth (in meters) of annual rainfall
	 */
	public void rainMap(double[][] rain) {
		// this.rain = rain;
	}

	/**
	 * Up-load the soil type for every tile
	 * @param soil - per point soil type
	 */
	public void soilMap(double[][] soil) {
		this.soil = soil;
	}

	/**
	 * Up-load the surface-water-depth for every tile
	 * @param hydration - per point depth of water
	 */
	public void waterMap(double[][] hydration) {
		this.hydration = hydration;
	}
	
	/**
	 * Up-load the flora assignments for every tile
	 * @param flora assignments per point
	 * @param names of flora classes
	 */
	public void floraMap(int[][] flora, String[] names) {
		
	}

	/**
	 * Export the up-loaded information in selected format
	 * 
	 * @param dirname - name of output directory
	 */
	public boolean writeFile( String dirname ) {
		
		// make sure the output directories exist
		File proj_dir = new File(dirname);
		if (!proj_dir.exists())
			proj_dir.mkdir();
		File map_dir = new File(dirname + "/maps");
		if (!map_dir.exists())
			map_dir.mkdir();
		
		boolean ok = createJsonFile(dirname);
		
		LuaWriter lua = new LuaWriter(dirname);
		
		// FIX get the altitude range from the map
		lua.fileHeader(-40, 95);
		
		// FIX let user select one or more entry points on the map
		LuaWriter.CityInfo[] villages = new LuaWriter.CityInfo[4];
		villages[0] = lua.new CityInfo(lua.new Position(15, 350, 0), lua.new Position(15, 530, 0));
		villages[1] = lua.new CityInfo(lua.new Position(15, 880, 0), lua.new Position(15, 700, 0));
		villages[2] = lua.new CityInfo(lua.new Position(800, 1009, 0), lua.new Position(250, 1009, 0));
		villages[3] = lua.new CityInfo(lua.new Position(1009, 750, 0), lua.new Position(1009, 300, 0));
		lua.villages(villages);
		
		/*
		 * these are discrete resource placements, unnecessary w/density maps
		LuaWriter.ResourceInfo[] resources = new LuaWriter.ResourceInfo[4];
		resources[0] = lua.new ResourceInfo("BERRIES", lua.new Position(940, 0, 424));
		resources[1] = lua.new ResourceInfo("ROCK", lua.new Position(950, 0, 250));
		resources[2] = lua.new ResourceInfo("IRON", lua.new Position(610, 0, 940));
		resources[3] = lua.new ResourceInfo("FISH", lua.new Position(563, 2, 93));
		lua.resources(resources);
		*/
		
		lua.startDensities();
		
		// the first group of trees has four different types
		LuaWriter.MapInfo maps[] = new LuaWriter.MapInfo[4];
		maps[0] = lua.new MapInfo("TREE_POPLAR", 8, 0.75, 1.0, 0.85, 1.15);
		maps[1] = lua.new MapInfo("TREE_OAK", 0.1, 0.9, 1.9, 0.9, 1.9);
		maps[2] = lua.new MapInfo("TREE_SYCAMORE", 8, 0.75, 1.0, 0.85, 1.15);
		maps[3] = lua.new MapInfo("TREE_PINE", 1, 0.15, 0.45, 0.7, 1.0);
		// FIX add a deciduous density slider
		lua.map("DECIDUOUS_DENSITY_MAP", 0.9, maps, false);
		
		// the resource maps are individual maps
		maps = new LuaWriter.MapInfo[1];
		maps[0] = lua.new MapInfo("TREE_PINE", 0.1, 0.75, 1.0, 0.85, 1.15);
		// FIX add a coniferous density slider
		lua.map("CONIFEROUS_DENSITY_MAP", 0.9, maps, false);
		
		maps[0] = lua.new MapInfo("RESOURCE_BERRIES", 0.1, 0.75, 1.0, 0.85, 1.15);
		lua.map("BERRIES_DENSITY_MAP", 1, maps, false);
		
		maps[0] = lua.new MapInfo("RESOURCE_ROCK", 0.1, 0.75, 1.0, 0.85, 1.15);
		lua.map("ROCK_DENSITY_MAP", 1, maps, false);
		
		maps[0] = lua.new MapInfo("RESOURCE_IRON", 0.1, 0.75, 1.0, 0.85, 1.15);
		lua.map("IRON_DENSITY_MAP", 1, maps, false);
		
		maps[0] = lua.new MapInfo("RESOURCE_FISH", 0.1, 0.75, 1.0, 0.85, 1.15);
		lua.map("FISH_DENSITY_MAP", 1, maps, true);
		
		lua.endDensities();
		lua.close();
		
		ok &= createHeightMap(dirname);
		
		ok &= createMaterialMask(dirname);
		ok &= createRockMap(dirname);
		ok &= createIronMap(dirname);
		
		ok &= createConiferMap(dirname);
		ok &= createDeciduousMap(dirname);
		ok &= createBerryMap(dirname);
		
		ok &= createFishMap(dirname);
		
		if (parms.debug_level > 0)
			System.out.println("Exported Foundation map " + parms.map_name + " to " + dirname);
		
		return ok;
	}
	
	private boolean createJsonFile(String project_dir) {
		String filename = "mod.json";
		
		try {
			FileWriter output = new FileWriter(project_dir + "/" + filename);
			String indent = "    ";
			String indentx2 = indent + indent;
			output.write("{\n");
			output.write(indent + "\"Name\": \"" + parms.region_name + "\",\n");
			output.write(indent + "\"Author\": \"" + parms.author_name + "\",\n");
			output.write(indent + "\"Description\": \"" + parms.description + "\",\n");
			output.write(indent + "\"Version\": \"1.0.0\",\n");
			output.write(indent + "\"MapList\": [\n");
			output.write(indentx2 + "{\n");
			output.write(indentx2 + "\"Name\": \"" + "Custom Map - " + parms.region_name + "\",\n");
			output.write(indentx2 + "\"Id\": \"" + parms.map_name + "\"\n");
			output.write(indentx2 + "}\n");
			output.write(indent + "]\n");
			output.write("}\n");
			output.close();
		} catch (IOException e) {
			System.err.println("Write error while attempting to create " + filename);
			return false;
		}
		
		return true;
	}
	
	private boolean createHeightMap(String project_dir) {
		String filename = "maps/heightmap.png";
		try {
			FileWriter output = new FileWriter(project_dir + "/" + filename);
			output.write("TODO - write " + filename + "\n");
			output.close();
		} catch (IOException e) {
			System.err.println("Write error while attempting to create " + filename);
			return false;
		}
		
		return true;
	}
	
	private boolean createMaterialMask(String project_dir) {
		String filename = "maps/material_mask.png";
		try {
			FileWriter output = new FileWriter(project_dir + "/" + filename);
			output.write("TODO - write " + filename + "\n");
			output.close();
		} catch (IOException e) {
			System.err.println("Write error while attempting to create " + filename);
			return false;
		}
		
		return true;
	}
	
	private boolean createRockMap(String project_dir) {
		String filename = "maps/rock_density.png";
		try {
			FileWriter output = new FileWriter(project_dir + "/" + filename);
			output.write("TODO - write " + filename + "\n");
			output.close();
		} catch (IOException e) {
			System.err.println("Write error while attempting to create " + filename);
			return false;
		}
		
		return true;
	}
	
	private boolean createIronMap(String project_dir) {
		String filename = "maps/iron_density.png";
		try {
			FileWriter output = new FileWriter(project_dir + "/" + filename);
			output.write("TODO - write " + filename + "\n");
			output.close();
		} catch (IOException e) {
			System.err.println("Write error while attempting to create " + filename);
			return false;
		}
		
		return true;
	}
	
	private boolean createConiferMap(String project_dir) {
		String filename = "maps/coniferous_density.png";
		try {
			FileWriter output = new FileWriter(project_dir + "/" + filename);
			output.write("TODO - write " + filename + "\n");
			output.close();
		} catch (IOException e) {
			System.err.println("Write error while attempting to create " + filename);
			return false;
		}
		
		return true;
	}
	
	private boolean createDeciduousMap(String project_dir) {
		String filename = "maps/deciduous_density.png";
		try {
			FileWriter output = new FileWriter(project_dir + "/" + filename);
			output.write("TODO - write " + filename + "\n");
			output.close();
		} catch (IOException e) {
			System.err.println("Write error while attempting to create " + filename);
			return false;
		}
		
		return true;
	}
	
	private boolean createBerryMap(String project_dir) {
		String filename = "maps/berries_density.png";
		try {
			FileWriter output = new FileWriter(project_dir + "/" + filename);
			output.write("TODO - write " + filename + "\n");
			output.close();
		} catch (IOException e) {
			System.err.println("Write error while attempting to create " + filename);
			return false;
		}
		
		return true;
	}
	
	private boolean createFishMap(String project_dir) {
		String filename = "maps/fish_density.png";
		try {
			FileWriter output = new FileWriter(project_dir + "/" + filename);
			output.write("TODO - write " + filename + "\n");
			output.close();
		} catch (IOException e) {
			System.err.println("Write error while attempting to create " + filename);
			return false;
		}
		
		return true;
	}
	
	/**
	 * generate a preview of the currently up-loaded export
	 * @param chosen map type (e.g. height, flora)
	 * @param colorMap - palette to be used in preview
	 */
	public void preview(WhichMap chosen, Color colorMap[]) {
	
		if (chosen == WhichMap.HEIGHTMAP) {
			// figure out the altitude to color mapping
			double aMean = (maxHeight + minHeight)/2;
			double aScale = BRIGHT - DIM;
			if (maxHeight > minHeight)
				aScale /= maxHeight - minHeight;
			
			// fill in the preview map
			Color map[][] = new Color[y_points][x_points];
			for(int i = 0; i < y_points; i++)
				for(int j = 0; j < x_points; j++)
					if (hydration[i][j] >= 0) {	// land
						double h = NORMAL + ((heights[i][j] - aMean) * aScale);
						map[i][j] = new Color((int)h, (int)h, (int)h);
					} else	{							// water
						double depth = hydration[i][j]/maxDepth;
						double h = (1 - depth) * (BRIGHT - DIM);
						map[i][j] = new Color(0, (int) h, BRIGHT);
					}
			new PreviewMap("Export Preview (terrain)", map);
		} else if (chosen == WhichMap.FLORAMAP) {
			Color pMap[][] = new Color[y_points][x_points];
			for(int i = 0; i < y_points; i++)
				for(int j = 0; j < x_points; j++) {
					// TODO water color or the appropriate flora color
					pMap[i][j] = Color.BLUE;
				}
			// TODO IMPLEMENT ME
			System.out.println("Flora previews not supported for JSON export");
		}
	}
}
