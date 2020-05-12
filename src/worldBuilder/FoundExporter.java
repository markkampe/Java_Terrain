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
	private static final String soilTypes[] = {
			"sedimentary", "metamorphic", "igneous", "alluvial"
	};
	
	private int x_points;			// width of map (in points)
	private int y_points;			// height of map (in points)
	private int tile_size;			// tile size (in meters)
	
	private double lat;				// latitude
	private double lon;				// longitude
	
	private double Tmean;			// mean temperature
	private double Tsummer;			// mean summer temperature
	private double Twinter;			// mean winter temperature
	
	private double[][] heights;		// per point height (meters)
	private double[][] rain;		// per point rainfall (meters)
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
		this.x_points = width;
		this.y_points = height;
		parms = Parameters.getInstance();
		
		if (parms.debug_level >= EXPORT_DEBUG)
			System.out.println("new Foundation exporter (" + height + "x" + width + ")");
	}

	/**
	 * Set the size of a single tile
	 * @param meters real-world width of a tile
	 */
	public void tileSize(int meters) {
		this.tile_size = meters;
	}

	/**
	 * Set the lat/lon of the region being exported
	 * @param lat real world latitude of map center
	 * @param lon real world longitude of map center
	 */
	public void position(double lat, double lon) {
		this.lat = lat;
		this.lon = lon;
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
		
		// note the max and min heights
		maxHeight = 0;
		minHeight = 666;
		for(int i = 0; i < heights.length; i++)
			for(int j = 0; j < heights[0].length; j++) {
				if (heights[i][j] > maxHeight)
					maxHeight = heights[i][j];
				if (heights[i][j] < minHeight)
					minHeight = heights[i][j];
			}
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
		this.rain = rain;
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
		
		// note the max and min heights
		maxDepth = 0;
		for (int i = 0; i < hydration.length; i++)
			for (int j = 0; j < hydration[0].length; j++) {
				if (hydration[i][j] < maxDepth)
					maxDepth = hydration[i][j];
			}
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
		
		// FIX get real altitude data, this just copies sample
		lua.fileHeader(-40, 95);
		
		// FIX get real city data, this just copies sample
		LuaWriter.CityInfo[] villages = new LuaWriter.CityInfo[4];
		villages[0] = lua.new CityInfo(lua.new Position(15, 0, 350), lua.new Position(15, 0, 530));
		villages[1] = lua.new CityInfo(lua.new Position(15, 0, 880), lua.new Position(15, 0, 700));
		villages[2] = lua.new CityInfo(lua.new Position(800, 0, 1009), lua.new Position(250, 0, 1009));
		villages[3] = lua.new CityInfo(lua.new Position(1009, 0, 750), lua.new Position(1009, 0, 300));
		lua.villages(villages);
		
		// FIX get real resource data, this just copies sample
		LuaWriter.ResourceInfo[] resources = new LuaWriter.ResourceInfo[4];
		resources[0] = lua.new ResourceInfo("BERRIES", lua.new Position(940, 0, 424));
		resources[1] = lua.new ResourceInfo("ROCK", lua.new Position(950, 0, 250));
		resources[2] = lua.new ResourceInfo("IRON", lua.new Position(610, 0, 940));
		resources[3] = lua.new ResourceInfo("FISH", lua.new Position(563, 2, 93));
		lua.resources(resources);
		
		lua.startDensities();
		LuaWriter.MapInfo map;
		map = lua.new MapInfo("CONIFEROUS_DENSITY_MAP", "TREE_PINE",
							  0.9, 0.1, 0.75, 1.0, 0.85, 1.15);
		lua.map(map, false);
		
		map = lua.new MapInfo("BERRIES_DENSITY_MAP", "RESOURCE_BERRIES",
				  1, 0.1, 0.75, 1.0, 0.85, 1.15);
		lua.map(map, false);
		
		map = lua.new MapInfo("ROCK_DENSITY_MAP", "RESOURCE_ROCK",
				  1, 0.1, 0.75, 1.0, 0.85, 1.15);
		lua.map(map, false);
		
		map = lua.new MapInfo("IRON_DENSITY_MAP", "RESOURCE_IRON",
				  1, 0.1, 0.75, 1.0, 0.85, 1.15);
		lua.map(map, false);
		
		map = lua.new MapInfo("FISH_DENSITY_MAP", "RESOURCE_FISH",
				  1, 0.1, 0.75, 1.0, 0.85, 1.15);
		lua.map(map, true);
		
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
