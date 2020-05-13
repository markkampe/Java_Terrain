package worldBuilder;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * Exporter to render a Cartesian map w/JSON descriptions of each point.
 */
public class FoundExporter implements Exporter {	

	private Parameters parms;
	
	private static final int IN_SIZE = 256;
	private static final int OUT_SIZE = 1024;
	
	private int x_points;			// width of map (in points)
	private int y_points;			// height of map (in points)
	
	private double Tmean;			// mean temperature
	private double Tsummer;			// mean summer temperature
	private double Twinter;			// mean winter temperature
	
	private double[][] heights;		// per point height (meters)
	private double[][] erode;		// per point erosion (meters)
	private double[][] hydration;	// per point water depth (meters)
	private double[][] soil;		// per point soil type
	
	private boolean needHeight;		// height or erosion has changed
	private double maxHeight;		// highest discovered altitude
	private double minHeight;		// lowest discovered altitude
	private double waterLevel;		// anything below this is u/w
	private double maxDepth;		// deepest discovered water
	
	// brightness constants for image colors
	private static final int DIM = 16;
	private static final int BRIGHT = 256 - DIM;
	private static final int FULL_WHITE = 65535;
	
	/**
	 * create a new Foundation exporter
	 * 
	 * @param width of the export area (in tiles)
	 * @param height of the export area ((in tiles)
	 */
	public FoundExporter(int width, int height) {
		if (width != IN_SIZE || height != IN_SIZE) {
			System.err.println("ERROR: Foundation requires 256x256 export");
			return;
		}
		this.x_points = width;
		this.y_points = height;
		parms = Parameters.getInstance();
		this.needHeight = true;
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
		// these have no role in Foundation
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
		this.needHeight = true;
	}

	/**
	 * Up-load the net erosion/deposition for every tile
	 * @param erode	per point height (in meters) of soil lost to erosion
	 * 		negative means sedimentqation
	 */
	public void erodeMap(double[][] erode) {
		this.erode = erode;	
		this.needHeight = true;
	}

	/**
	 * Up-load the annual rainfall for every tile
	 * @param rain	per point depth (in meters) of annual rainfall
	 */
	public void rainMap(double[][] rain) {
		// this is unnecessary for Foundation tile bidding
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
		
		// mod.json is merely identification information
		boolean ok = createJsonFile(dirname);
		
		LuaWriter lua = new LuaWriter(dirname);
		
		// establish height range and create the header
		if (this.needHeight)
			heightRange();
	
		int lowest = (int) parms.height(minHeight - waterLevel);
		int highest = (int) parms.height(maxHeight - waterLevel);
		lua.fileHeader(lowest, highest);
		
		// FIX let user select one or more entry points on the map
		LuaWriter.EntryPoint[] villages = new LuaWriter.EntryPoint[4];
		villages[0] = lua.new EntryPoint(lua.new Position(15, 350, 0), lua.new Position(15, 530, 0));
		villages[1] = lua.new EntryPoint(lua.new Position(15, 880, 0), lua.new Position(15, 700, 0));
		villages[2] = lua.new EntryPoint(lua.new Position(800, 1009, 0), lua.new Position(250, 1009, 0));
		villages[3] = lua.new EntryPoint(lua.new Position(1009, 750, 0), lua.new Position(1009, 300, 0));
		lua.entrypoints(villages);
		
		/*
		 * discrete resource placements are unnecessary w/density maps
		 * LuaWriter.ResourceInfo[] resources = new LuaWriter.ResourceInfo[4];
		 * resources[0] = lua.new ResourceInfo("BERRIES", lua.new Position(940, 0, 424));
		 * resources[1] = lua.new ResourceInfo("ROCK", lua.new Position(950, 0, 250));
		 * resources[2] = lua.new ResourceInfo("IRON", lua.new Position(610, 0, 940));
		 * resources[3] = lua.new ResourceInfo("FISH", lua.new Position(563, 2, 93));
		 * lua.resources(resources);
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
		
		ok &= createRockMap(dirname);
		ok &= createIronMap(dirname);
		
		ok &= createMaterialMask(dirname);
		ok &= createConiferMap(dirname);
		ok &= createDeciduousMap(dirname);
		ok &= createBerryMap(dirname);
		
		ok &= createFishMap(dirname);
		
		if (parms.debug_level > 0)
			System.out.println("Exported Foundation map " + parms.map_name + " to " + dirname);
		
		return ok;
	}
	
	/**
	 * create the mod.json file of map identification information
	 * @param project_dir
	 * @return boolean - success/failure
	 */
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
	
	/**
	 * examine the height and topology maps to get
	 * the altitude range
	 */
	private void heightRange() {
		waterLevel = parms.sea_level;
		maxHeight = -666;
		minHeight = 666;
		for(int i = 0; i < y_points; i++)
			for(int j = 0; j < x_points; j++) {
				double z = heights[i][j] - erode[i][j];
				if (z > maxHeight)
					maxHeight = z;
				if (z < minHeight)
					minHeight = z;
				// FIX deal with above-sea-level lakes
			}
		
		this.needHeight = false;
	}
	
	/**
	 * create the height map for the exported region
	 * @param project_dir
	 * @return boolean - success/failure
	 */
	private boolean createHeightMap(String project_dir) {		
		// figure out altitude-to-intensity mapping
		double aScale = FULL_WHITE;
		if (maxHeight > minHeight)
			aScale /= maxHeight - minHeight;
		
		// create a height grey-scale image, converting 256x256 to 1024x1024
		BufferedImage img = new BufferedImage(IN_SIZE, IN_SIZE, 
											 BufferedImage.TYPE_USHORT_GRAY);
		for(int y = 0; y < y_points; y++)
			for(int x = 0; x < x_points; x++) {
				double h = (heights[y][x] - erode[y][x]) - minHeight;
				double b = h * aScale;
				img.setRGB(x, y, (int)b);
			}
		
		// interpolate the intervening rows and columns
		//dither(img);
		
		// write it out as a .png
		String filename = project_dir + "/maps/heightmap.png";
		File f = new File(filename);
		try {
			if (!ImageIO.write(img, "PNG", f)) {
				System.err.println("ImageIO error while attempting to create " + filename);
				return false;
			}
		} catch (IOException e) {
			System.err.println("Write error while attempting to create " + filename);
			return false;
		}
		return true;
	}
	
	/*
	 * interpolate the missing values in a sparse (256x256) filling
	 * of a 1024x1024 image.
	 * @param img - 1/16 filled BufferedImage
	 */
	private void dither(BufferedImage img) {
		// interpolate the intervening rows
		for(int y = 0; y < OUT_SIZE; y += 4)
			for(int x = 0; x < OUT_SIZE; x += 4) {
				int first = img.getRGB(x, y);
				int last = (y >= OUT_SIZE-4) ? first : img.getRGB(x, y+4);
				img.setRGB(x, y+1, (last + (3*first))/4);
				img.setRGB(x, y+2, (first + last)/2);
				img.setRGB(x, y+3, (first + (3*last))/4);
			}
	
		// interpolate the intervening columns
		for(int y = 0; y < OUT_SIZE; y++)
			for(int x = 0; x < OUT_SIZE; x += 4) {
				int first = img.getRGB(x, y);
				int last = (x >= OUT_SIZE-4) ? first : img.getRGB(x+4, y);
				img.setRGB(x+1, y, first);
				img.setRGB(x+2, y, first);
				img.setRGB(x+3, y, first);
			}	
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
			// figure out mapping from altitude to color
			if (this.needHeight)
				heightRange();
			double aScale = BRIGHT - DIM;
			if (maxHeight > minHeight)
				aScale /= maxHeight - minHeight;
			
			// fill in the preview map
			Color map[][] = new Color[y_points][x_points];
			for(int i = 0; i < y_points; i++)
				for(int j = 0; j < x_points; j++) {
					double h = (heights[i][j] - erode[i][j]) - minHeight;
					double b = DIM + (h * aScale);
					map[i][j] = new Color((int)b, (int)b, (int)b);
				}
			
			// put up the preview
			new PreviewMap("Export Preview (terrain)", map);
		} else if (chosen == WhichMap.FLORAMAP) {
			Color pMap[][] = new Color[y_points][x_points];
			for(int i = 0; i < y_points; i++)
				for(int j = 0; j < x_points; j++) {
					pMap[i][j] = Color.BLUE;	// FIX flora
				}
			// TODO IMPLEMENT ME
			System.out.println("Flora previews not supported for JSON export");
		}
	}
}
