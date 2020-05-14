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
	private double[][] hydration;	// per point water depth (meters)
	private double[][] soil;		// per point soil type
	
	private double dDecid = 0.9;	// FIX add deciduous desity slider
	private double dConif = 0.9;	// FIX add coniferous density slider
	
	// calculated information for our output
	private static final int XY_POINTS = 1024;	// Foundation map size
	
	private double[][] altitudes;	// height+erosion, in meters
	private double highest;			// max altitude (meters MSL)
	private double lowest;			// min altitude (meters MSL)
	
	private int[][] ports;			// entry and exit points
	private int num_ports;			// number reported
	private static final int MAX_PORTS = 4;
	
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
		heightRange();
		lua.fileHeader((int) lowest, (int) highest);
		
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
		String indent = "            ";
		
		lua.comment(indent + "-- Create forest of Deciduous trees and a few pine\n");
		LuaWriter.MapInfo maps[] = new LuaWriter.MapInfo[4];
								// tree			weight	offset		scale
		maps[0] = lua.new MapInfo("TREE_POPLAR", 8, 	0.75, 1.0,	0.85, 1.15);
		maps[1] = lua.new MapInfo("TREE_OAK",	0.1,	0.9, 1.9,	0.9, 1.9);
		maps[2] = lua.new MapInfo("TREE_SYCAMORE", 8,	0.75, 1.0,	0.85, 1.15);
		maps[3] = lua.new MapInfo("TREE_PINE",	1,		0.15, 0.45,	0.7, 1.0);
		lua.map("DECIDUOUS_DENSITY_MAP", dDecid, maps, false);
		
		lua.comment(indent + "-- Create forest of Pine Trees\n");
		maps = new LuaWriter.MapInfo[1];
								// tree			weight	offset		scale
		maps[0] = lua.new MapInfo("TREE_PINE",	0.1,	0.75, 1.0,	0.85, 1.15);
		lua.map("CONIFEROUS_DENSITY_MAP", dConif, maps, false);
		
		lua.comment(indent + "-- Create forest of berry bushes\n");
								// resource			weight	offset		scale
		maps[0] = lua.new MapInfo("RESOURCE_BERRIES", 0.1,	0.75, 1.0,	0.85, 1.15);
		lua.map("BERRIES_DENSITY_MAP", 1, maps, false);
		
		lua.comment(indent + "-- Create rock outcrops - stone resource\n");
								// resource			weight	offset		scale
		maps[0] = lua.new MapInfo("RESOURCE_ROCK",	0.1,	0.75, 1.0,	0.85, 1.15);
		lua.map("ROCK_DENSITY_MAP", 1, maps, false);
		
		lua.comment(indent + "-- Create iron deposits\n");
								// resource			weight	offset		scale
		maps[0] = lua.new MapInfo("RESOURCE_IRON",	0.1,	0.75, 1.0,	0.85, 1.15);
		lua.map("IRON_DENSITY_MAP", 1, maps, false);
		
		lua.comment(indent + "-- Create fish shoals\n");
								// resource			weight	offset		scale
		maps[0] = lua.new MapInfo("RESOURCE_FISH",	0.1,	0.75, 1.0,	0.85, 1.15);
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
	 * to create an array of (per x/y_point) altitudes (in meters) 
	 * 
	 * TODO heightRange cannot deal with above-sea-level lakes/rivers
	 */
	private void heightRange() {
		if (altitudes == null) {	// if it does not already exist
			altitudes = new double[y_points][x_points];
			
			// so we can find the highest and lowest points
			highest = -666666;
			lowest = 666666;
			for(int y = 0; y < y_points; y++)
				for(int x = 0; x < x_points; x++) {
					double alt = parms.altitude(heights[y][x] - erode[y][x]);
					altitudes[y][x] = alt;
					if (alt > highest)
						highest = alt;
					if (alt < lowest)
						lowest = alt;
				}
			
			if (parms.debug_level >= EXPORT_DEBUG)
				System.out.println(String.format(
									"    Export height range: %.1f-%.1f (m MSL)",
									lowest, highest));
		}
	}
	
	/**
	 * create the height map for the exported region
	 * @param project_dir
	 * @return boolean - success/failure
	 */
	private boolean createHeightMap(String project_dir) {
		// figure out altitude-to-intensity mapping
		heightRange();
		double aScale = FULL_WHITE;
		if (highest > lowest)
			aScale /= highest - lowest;
		
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
		
		// step 1: sparsely fill the inflated image
		for(int y = 0; y < XY_POINTS; y += scale) {
			int y_in = y/scale;
			for(int x = 0; x < XY_POINTS; x += scale) {
				// fill in image point that corresponds to altitude point
				int x_in = x/scale;
				double bright = (altitudes[y_in][x_in] - lowest) * aScale;
				img.setRGB(x, y, (int) bright);
			}
		}
		
		// fill in the missing points
		interpolate(img, scale);
		
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
		
		if (parms.debug_level >= EXPORT_DEBUG)
			System.out.println(String.format(
								"Exported (%dx%d)x%d gray-scale height map %s",
								x_points, y_points, scale, filename));
		return true;
	}
	
	/**
	 * interpolate the missing points in a sparsely populated image
	 * @param img ... image to be completed
	 * @param sparseness ... current granularity (every n'th)
	 */
	private void interpolate(BufferedImage img, int sparseness) {
		int height = img.getHeight();
		int width = img.getWidth();
		
		for(int y = 0; y < height; y += sparseness)
			for( int x = 0; x < width; x += sparseness) {
				// TODO real 2D interpolation
				int value = img.getRGB(x, y);
				for(int i = 1; i < sparseness; i++)
					for(int j = 1; j < sparseness; j++)
						img.setRGB(x+j, y+i, value);
			}
	}
	
	private boolean createMaterialMask(String project_dir) {
		// create an appropriately sized RGB image
		BufferedImage img = new BufferedImage(XY_POINTS, XY_POINTS, 
											 BufferedImage.TYPE_INT_RGB);
		
		// write it out as a .png
		String filename = project_dir + "/maps/material_mask.png";
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
	
	private boolean createRockMap(String project_dir) {
		// create an appropriately sized gray-scale image
		BufferedImage img = new BufferedImage(XY_POINTS, XY_POINTS, 
											 BufferedImage.TYPE_USHORT_GRAY);
		
		// write it out as a .png
		String filename = project_dir + "/maps/rock_density.png";
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
	
	private boolean createIronMap(String project_dir) {
		// create an appropriately sized gray-scale image
		BufferedImage img = new BufferedImage(XY_POINTS, XY_POINTS, 
											 BufferedImage.TYPE_USHORT_GRAY);
		
		// write it out as a .png
		String filename = project_dir + "/maps/iron_density.png";
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
	
	private boolean createConiferMap(String project_dir) {
		// create an appropriately sized gray-scale image
		BufferedImage img = new BufferedImage(XY_POINTS, XY_POINTS, 
											 BufferedImage.TYPE_USHORT_GRAY);
		
		// write it out as a .png
		String filename = project_dir + "/maps/coniferous_density.png";
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
	
	private boolean createDeciduousMap(String project_dir) {
		// create an appropriately sized gray-scale image
		BufferedImage img = new BufferedImage(XY_POINTS, XY_POINTS, 
											 BufferedImage.TYPE_USHORT_GRAY);
		
		// write it out as a .png
		String filename = project_dir + "/maps/deciduous_density.png";
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
	
	private boolean createBerryMap(String project_dir) {
		// create an appropriately sized gray-scale image
		BufferedImage img = new BufferedImage(XY_POINTS, XY_POINTS, 
											 BufferedImage.TYPE_USHORT_GRAY);
		
		// write it out as a .png
		String filename = project_dir + "/maps/berries_density.png";
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
	
	private boolean createFishMap(String project_dir) {
		// create an appropriately sized gray-scale image
		BufferedImage img = new BufferedImage(XY_POINTS, XY_POINTS, 
											 BufferedImage.TYPE_USHORT_GRAY);
		
		// write it out as a .png
		String filename = project_dir + "/maps/fish_density.png";
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
	 * generate a preview of the currently up-loaded export
	 * @param chosen map type (e.g. height, flora)
	 * @param colorMap - palette to be used in preview
	 */
	public void preview(WhichMap chosen, Color colorMap[]) {
	
		if (chosen == WhichMap.HEIGHTMAP) {
			// figure out mapping from altitude to color
			heightRange();
			double aScale = BRIGHT - DIM;
			if (highest > lowest)
				aScale /= highest - lowest;
			
			// fill in the preview map
			Color map[][] = new Color[y_points][x_points];
			for(int y = 0; y < y_points; y++)
				for(int x = 0; x < x_points; x++) {
					if (hydration[y][x] < 0) {	// water depth
						map[y][x] = Color.BLUE;
					} else {	// show altitude
						double h = altitudes[y][x] - lowest;
						double b = DIM + (h * aScale);
						map[y][x] = new Color((int)b, (int)b, (int)b);
					}
				}
			new PreviewMap("Export Preview (height map)", map);
		} else if (chosen == WhichMap.FLORAMAP) {
			Color pMap[][] = new Color[y_points][x_points];
			for(int y = 0; y < y_points; y++)
				for(int x = 0; x < x_points; x++) {
					pMap[y][x] = Color.GREEN;	// FIX flora
				}
			new PreviewMap("Export Preview (trees/bushes/grass)", pMap);
		}
	}

	// perfunctory set methods for information we don't use
	public void tileSize(int meters) {}
	public void position(double lat, double lon) {}
	public void temps(double meanTemp, double meanSummer, double meanWinter) {}
	public void rainMap(double[][] rain) {}
}
