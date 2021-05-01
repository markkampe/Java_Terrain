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
	
	// input from ExportBase
	private int x_points;			// width of map (in points)
	private int y_points;			// height of map (in points)
	
	private double[][] heights;		// per point height (meters)
	private double[][] erode;		// per point erosion (meters)
	private double[][] depths;		// per point water depth (meters)
	private double[][] flora;		// per point plant IDs
	private String[] floraNames;	// per type name strings
	private double[][] soil;		// per point soil type
	private String[] rockNames;		// per type name strings
	private double[][] fauna;		// per point game IDs
	private String[] faunaNames;	// per type name strings
	
	private double dDecid = 0.9;	// XXX where to get deciduous density
	private double dConif = 0.9;	// XXX where to get coniferous density
	
	// calculated information for our output
	private static final int XY_POINTS = 1024;	// Foundation map size
	
	private double[][] altitudes;	// height+erosion, in meters
	private double highest;			// max altitude (meters MSL)
	private double lowest;			// min altitude (meters MSL)
	private double maxDepth;		// max depth (meters MSL)
	
	private int[][] ports;			// entry and exit points
	private int num_ports;			// number reported
	public static final int MAX_PORTS = 4;
	
	// list of resource bitmaps to be created
	private static String resource_maps[] = {
			"coniferous",
			"deciduous",
			"berries",
			"rock",
			"iron",
			"fish"
	};
	
	// mapping from eco/geo type names into Foundation resources
	private static String coniferous_resources[] = { "Conifers" };
	private static String deciduous_resources[] = { "Broadleaf" };
	private static String berry_resources[] = { "Riperian" };
	private static String grass_resources[] = { "Grassland" };
	private static String stone_resources[] = { "Sand Stone", "Granite" };
	private static String metal_resources[] = { "Iron Ore", "Copper Ore" };
	private static String fish_resources[] = { "Fish" };
	
	// color ranges for use in output maps
	private static final int DIM = 16;
	private static final int BRIGHT = 256 - DIM;
	private static final int FULL_WHITE = 65535;
	
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
			System.out.println(String.format("Foundation Exporter (%dx%d)->(%dx%d)",
											width, height, XY_POINTS, XY_POINTS));
	}
	
	/**
	 * Up-load the altitude of every tile
	 * @param heights	height (in meters) of every point
	 */
	public void heightMap(double[][] heights) {
		this.heights = heights;
		this.altitudes = null;
	}

	/**
	 * Up-load the net erosion/deposition for every tile
	 * @param erode	per point height (in meters) of soil lost to erosion
	 * 		negative means sedimentqation
	 */
	public void erodeMap(double[][] erode) {
		this.erode = erode;	
		this.altitudes = null;
	}

	/**
	 * Up-load the soil type for every tile
	 * @param soil - per point soil type
	 * @param names - per type name strings
	 */
	public void soilMap(double[][] soil, String[] names) {
		this.soil = soil;
		this.rockNames = names;
	}

	/**
	 * Up-load the surface-water-depth for every tile
	 * @param depths - per point depth of water
	 */
	public void waterMap(double[][] depths) {
		this.depths = depths;
	}

	/**
	 * Up-load the flora assignments for every tile
	 * @param flora assignments per point
	 * @param names - per type name strings
	 */
	public void floraMap(double[][] flora, String[] names) {
		this.flora = flora;
		this.floraNames = names;
	}
	
	/**
	 * Up-load the fauna type for every tile
	 * @param fauna - per point fauna type
	 */
	public void faunaMap(double[][] fauna, String[] names) {
		this.fauna = fauna;
		this.faunaNames = names;
	}

	/**
	 * entry/exit points (for explorers)
	 * 
	 * @param x_in	(x_points, relative to top-left)
	 * @param y_in	(y_points, relative to top-left)
	 * @param x_out	(x_points, relative to top-left)
	 * @param y_out	(y_points, relative to top-left)
	 * 
	 * Note: Foundation coordinates relative to bottom-left
	 */
	public void entryPoint(int x_in, int y_in, int x_out, int y_out) {
		if (num_ports == 0)
			ports = new int[MAX_PORTS][4];
		
		if (num_ports < MAX_PORTS) {
			ports[num_ports][0] = x_in * XY_POINTS / x_points;
			ports[num_ports][1] = (y_points - y_in) * XY_POINTS / y_points;
			ports[num_ports][2] = x_out * XY_POINTS / x_points;
			ports[num_ports][3] = (y_points - y_out) * XY_POINTS / y_points;
			num_ports += 1;
		}
	}
	
	/**
	 * Export the up-loaded information in selected forma
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
		heightRange();
		lua.fileHeader((int) lowest, (int) highest, resource_maps);
		
		// report our entry and exit points
		if (num_ports > 0) {
			LuaWriter.EntryPoint[] entries = new LuaWriter.EntryPoint[num_ports];
			for (int i = 0; i < num_ports; i++)
				entries[i] = lua.new EntryPoint(lua.new Position(ports[i][0], ports[i][1], 0), 
				    							lua.new Position(ports[i][2], ports[i][3], 0));
			lua.entrypoints(entries);
		}
		
		/*
		 * discrete resource placements are unnecessary w/density maps
		 * LuaWriter.ResourceInfo[] resources = new LuaWriter.ResourceInfo[4];
		 * resources[0] = lua.new ResourceInfo("BERRIES", lua.new Position(940, 0, 424));
		 * resources[1] = lua.new ResourceInfo("ROCK", lua.new Position(950, 0, 250));
		 * resources[2] = lua.new ResourceInfo("IRON", lua.new Position(610, 0, 940));
		 * resources[3] = lua.new ResourceInfo("FISH", lua.new Position(563, 2, 93));
		 * lua.resources(resources);
		 */
		
		/*
		 * the DensitySpawnList describes, for each density map, 
		 * the type of resource covered by that map, and its
		 * placement parameters.  The program will then automatically
		 * spawn resource instances, in the density-map-indicated
		 * locations, with the below-specified parameters.
		 */
		lua.startDensities();
		//String indent = "            ";
		LuaWriter.MapInfo[] maps =  new LuaWriter.MapInfo[4];
		
								// tree			weight	offset		scale
		maps[0] = lua.new MapInfo("TREE_POPLAR", 8, 	0.75, 1.0,	0.85, 1.15);
		maps[1] = lua.new MapInfo("TREE_OAK",	0.1,	0.9, 1.9,	0.9, 1.9);
		maps[2] = lua.new MapInfo("TREE_SYCAMORE", 8,	0.75, 1.0,	0.85, 1.15);
		maps[3] = lua.new MapInfo("TREE_PINE",	1,		0.15, 0.45,	0.7, 1.0);
		lua.map("DECIDUOUS_DENSITY_MAP", "forest of Deciduous trees and a few pine", dDecid, maps, false);

		maps = new LuaWriter.MapInfo[1];
								// tree			weight	offset		scale
		maps[0] = lua.new MapInfo("TREE_PINE",	0.1,	0.75, 1.0,	0.85, 1.15);
		lua.map("CONIFEROUS_DENSITY_MAP", "forest of Pine Trees", dConif, maps, false);
								// resource			weight	offset		scale
		maps[0] = lua.new MapInfo("RESOURCE_BERRIES", 0.1,	0.75, 1.0,	0.85, 1.15);
		lua.map("BERRIES_DENSITY_MAP", "forest of berry bushes", 1, maps, false);
								// resource			weight	offset		scale
		maps[0] = lua.new MapInfo("RESOURCE_ROCK",	0.1,	0.75, 1.0,	0.85, 1.15);
		lua.map("ROCK_DENSITY_MAP", "rock outcrops - stone resource", 1, maps, false);
								// resource			weight	offset		scale
		maps[0] = lua.new MapInfo("RESOURCE_IRON",	0.1,	0.75, 1.0,	0.85, 1.15);
		lua.map("IRON_DENSITY_MAP", "iron deposits", 1, maps, false);
								// resource			weight	offset		scale
		maps[0] = lua.new MapInfo("RESOURCE_FISH", 0.1,	0.75, 1.0,	0.85, 1.15);
		lua.map("FISH_DENSITY_MAP", "fish shoals", 1, maps, true);
		
		lua.endDensities();
		lua.close();
		
		ok &= createHeightMap(dirname);
		
		// create the rock maps
		BufferedImage img = new BufferedImage(XY_POINTS, XY_POINTS, 
				 BufferedImage.TYPE_USHORT_GRAY);
		for(int i = 0; i < stone_resources.length; i++)
			add_to_map(img, soil, stone_resources[i], rockNames, FULL_WHITE);
		ok &= createPng(img, dirname + "/maps/rock_density.png");
		
		img = new BufferedImage(XY_POINTS, XY_POINTS, BufferedImage.TYPE_USHORT_GRAY);
		for(int i = 0; i < metal_resources.length; i++)
			add_to_map(img, soil, metal_resources[i], rockNames, FULL_WHITE);
		ok &= createPng(img, dirname + "/maps/iron_density.png");
		
		// create the tree maps
		img = new BufferedImage(XY_POINTS, XY_POINTS, BufferedImage.TYPE_USHORT_GRAY);
		for(int i = 0; i < coniferous_resources.length; i++)
			add_to_map(img, flora, coniferous_resources[i], floraNames, FULL_WHITE);
		ok &= createPng(img, dirname + "/maps/coniferous_density.png");
		
		img = new BufferedImage(XY_POINTS, XY_POINTS, BufferedImage.TYPE_USHORT_GRAY);
		for(int i = 0; i < deciduous_resources.length; i++)
			add_to_map(img, flora, deciduous_resources[i], floraNames, FULL_WHITE);
		ok &= createPng(img, dirname + "/maps/deciduous_density.png");
		
		// create the berry map
		img = new BufferedImage(XY_POINTS, XY_POINTS, BufferedImage.TYPE_USHORT_GRAY);
		for(int i = 0; i < berry_resources.length; i++)
			add_to_map(img, flora, berry_resources[i], floraNames, FULL_WHITE);
		ok &= createPng(img, dirname + "/maps/berries_density.png");
		
		// create the grass/sand map
		final int SAND_COLOR = Color.GREEN.getRGB();
		final int GRASS_COLOR = Color.RED.getRGB();
		img = new BufferedImage(XY_POINTS, XY_POINTS, BufferedImage.TYPE_INT_RGB);
		for(int y = 0; y < XY_POINTS; y++)
			for(int x = 0; x < XY_POINTS; x++)
				img.setRGB(x, y, SAND_COLOR);
		for(int i = 0; i < grass_resources.length; i++)
			add_to_map(img, flora, grass_resources[i], floraNames, GRASS_COLOR);
		// TODO - they want a fairly wide brown blurred transition
		ok &= createPng(img, dirname + "/maps/material_mask_" + parms.map_name + ".png");
		
		// create the fish map
		img = new BufferedImage(XY_POINTS, XY_POINTS, BufferedImage.TYPE_USHORT_GRAY);
		for(int i = 0; i < fish_resources.length; i++)
			add_to_map(img, fauna, fish_resources[i], faunaNames, FULL_WHITE);
		ok &= createPng(img, dirname + "/maps/fish_density.png");
		
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
		String filename = project_dir + "/" + "mod.json";
		
		try {
			FileWriter output = new FileWriter(filename);
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
		
		if (parms.debug_level >= EXPORT_DEBUG)
			System.out.println("Created Foundation export description: " + filename);
			
		return true;
	}
	
	/**
	 * process the (map coordinates) height and erosion maps
	 * to create an array of (per x/y_point) altitudes (in meters MSL) 
	 */
	private void heightRange() {
		if (altitudes == null) {	// if it does not already exist
			altitudes = new double[y_points][x_points];
			
			// 1. convert all Z heights to Meters MSL
			highest = -666666;
			lowest = 666666;
			maxDepth = 0;
			for(int y = 0; y < y_points; y++)
				for(int x = 0; x < x_points; x++) {
					double alt = parms.altitude(heights[y][x] - erode[y][x]);
					altitudes[y][x] = alt;
					if (alt > highest)
						highest = alt;
					if (alt < lowest)
						lowest = alt;
					if (depths[y][x] > 0) {
						double depth = parms.height(depths[y][x]);
						if (depth > maxDepth)
							maxDepth = depth;
					}
				}

			// 2. ensure that all u/w points have negative altitude
			for(int y = 0; y < y_points; y++)
				for(int x = 0; x < x_points; x++) {
					// sub-oceanic and dry points are already OK
					if (altitudes[y][x] < 0 || depths[y][x] == 0)
						continue;
					// move this point to -depth MSL
					double depth = parms.height(depths[y][x]);
					altitudes[y][x] = -depth >= lowest ? -depth : lowest;
				}
		}
	}
	
	/**
	 * create the height map for the exported region
	 * @param project_dir
	 * @return boolean - success/failure
	 */
	private boolean createHeightMap(String project_dir) {

		// smooth the altitudes and translate to gray-scale
		heightRange();
		Cartesian.smooth(altitudes);
		int[][] grayscale = Cartesian.encode(altitudes, 0, FULL_WHITE);
		
		// create an appropriately sized gray-scale image
		BufferedImage img = new BufferedImage(XY_POINTS, XY_POINTS, 
											 BufferedImage.TYPE_USHORT_GRAY);
		
		/*
		 * we chose a relatively low resolution map from ExportBase because
		 *  (a) Cartesian exports are very per-tile expensive
		 *  (b) This is high-resolution color and Cartesian exports,
		 *      while interpolated, are not smooth.
		 * So we attempt to do a smoothing interpolation as we up-scale
		 * the altitude map to Foundations 1024x1024 format.
		 */
		int scale = XY_POINTS/x_points;
		
		for(int y = 0; y < XY_POINTS; y += scale) {
			int y_in = y/scale;
			for(int x = 0; x < XY_POINTS; x += scale) {
				int x_in = x/scale;
	
				// get values for the four bordering corners
				int ul = grayscale[y_in][x_in];
				boolean x_ok = x_in < x_points - 1;
				boolean y_ok = y_in < y_points - 1;
				int ur = x_ok ? grayscale[y_in][x_in+1] : ul;
				int ll = y_ok ? grayscale[y_in+1][x_in] : ul;
				int lr = (x_ok && y_ok) ? grayscale[y_in+1][x_in+1] : ul;
				
				// two-dimensional interpolation for all points between them
				for(int i = 0; i < scale; i++)
					for(int j = 0; j < scale; j++) {
						int sum = ul * (scale - i) * (scale - j);
						sum += ll * i * (scale - j);
						sum += ur * j * (scale - i);
						sum += lr * i * j;
						img.setRGB(x+j, y+i, sum/(scale * scale));
					}
			}
		}
		
		// write it out as a .png
		String filename = project_dir + "/maps/heightmap_" + parms.map_name + ".png";
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
		
		if (parms.debug_level < EXPORT_DEBUG)
			return true;
		
		System.out.println(String.format(
							"Exported (%dx%d)x%d gray-scale height map %s",
							x_points, y_points, scale, filename));
		return true;
	}
	
	/**
	 * write a grey map image out to a file
	 * @param image to be written
	 * @param name of desired output file
	 * @return success
	 */
	private boolean createPng(BufferedImage img, String filename) {		
		// write it out as a .png
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
	
	/**
	 * add resources to a grey-scale map
	 * @param BufferedImage for the map
	 * @param tileValues 2D array of resources
	 * @param className desired resource
	 * @param nameMap array of resource class names
	 * @param rgb value color value to set for matching points
	 */
	private boolean add_to_map(BufferedImage img, double[][] tileValues, String classname, String[] nameMap, int rgb) {
		// figure out what class we are looking for
		int desired = -1;
		for(int i = 0; i < nameMap.length; i++)
			if (nameMap[i] != null && classname.equals(nameMap[i])) {
				desired = i;
				break;
			}
		if (desired < 0)
			return false;
		
		int scale = XY_POINTS/x_points;
		int points = 0;
		for(int y = 0; y < XY_POINTS; y += scale) {
			int y_in = y/scale;
			for(int x = 0; x < XY_POINTS; x += scale) {
				int x_in = x/scale;
				if (tileValues[y_in][x_in] != desired)
					continue;
				
				// fill in the entire box
				for(int i = 0; i < scale; i++)
					for(int j = 0; j < scale; j++) {
						img.setRGB(x+j, y+i, rgb);
						points++;
					}
				// FIX round corners of scaleXscale boxes
			}
		}
		
		if (parms.debug_level >= EXPORT_DEBUG)
			System.out.println("    " + "placed " + points + " points of " + classname);
		return true;
	}
	
	/**
	 * generate a preview of the currently up-loaded export
	 * @param chosen map type (e.g. height, flora)
	 * @param colorMap - palette to be used in preview
	 */
	public void preview(WhichMap chosen, Color colorMap[]) {
	
		// figure out mapping from altitude to color
		heightRange();
		int[][] grayscale = Cartesian.encode(altitudes, DIM, BRIGHT);
		
		// fill in the preview map
		Color map[][] = new Color[y_points][x_points];
		for(int y = 0; y < y_points; y++)
			for(int x = 0; x < x_points; x++) {
				if (depths[y][x] > 0) {	// water depth
					double depth = parms.height(depths[y][x]);
					double shade = 1.0 - (depth / maxDepth);
					map[y][x] = new Color(0, 0, DIM + (int)(BRIGHT * shade));
				} else {	// show altitude
					int bright = grayscale[y][x];
					if (chosen == WhichMap.FLORAMAP && flora[y][x] > 0)
						map[y][x] = colorMap[(int) flora[y][x]];
					else
						map[y][x] = new Color(bright, bright, bright);
				}
			}
		new PreviewMap("Export Preview (" +
					   (chosen == WhichMap.FLORAMAP ? "flora" : "terrain") +
					   ")", map, 0);
	}

	// perfunctory set methods for information we don't use
	public void tileSize(int meters) {}
	public void position(double lat, double lon) {}
	public void temps(double meanTemp, double meanSummer, double meanWinter) {}
	public void rainMap(double[][] rain) {}
}
