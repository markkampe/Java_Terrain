package worldBuilder;

import java.awt.Color;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Exporter to render a Cartesian map w/JSON descriptions of each point.
 */
public class LuaWriter {

	private FileWriter luaFile;
	private Parameters parms;

	private static final String LUA_NAME = "mod.lua";

	private static final int EXPORT_DEBUG = 2;
	
	/**
	 * Most of the items on the map are defined by (x,y,z) positions
	 */
	public class Position {
		public int x, y, z;
		
		public Position(int x, int y, int z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
		
		public String toString() {
			return "{ " + x + ", " + y + ", " + z + " }";
		}
	}
	
	/** 
	 * Foundation needs to know the coordinates of the 
	 * entry and exit points for each city/village.
	 */
	public class CityInfo {
		public Position entrance;
		public Position exit;
		
		public CityInfo(Position entrance, Position exit) {
			this.entrance = entrance;
			this.exit = exit;
		}
		
		public String toString(String indent) {
			String ret = indent + "{\n";
			ret += indent + "    Entrance = " + entrance.toString() + ",\n";
			ret += indent + "    Exit = " + exit.toString() + "\n";
			ret += indent + "}";
			return ret;
		}
	}
	
	private static String ALL_ORIENTATIONS = "{ 0.0, math.randomf(-180, 180), 0.0 }";
	/**
	 * for reasons I don't understand, it also needs to now a 
	 * reference position for each resource 
	 * (even tho the map shows the position)
	 */
	public class ResourceInfo {
		public String name;
		public Position position;
		
		public ResourceInfo(String name, Position position) {
			this.name = name;
			this.position = position;
		}
		
		/**
		 * @return SpawnList declaration for this resource
		 * @param indent (before each line)
		 * 
		 * Note: last line will not have a newline, as a comma may be needed
		 */
		public String toString(String indent) {
			String ret = indent + "{\n";
			ret += indent + "    Prefab = \"PREFAB_RESOURCE_" + name + "\",\n";
			ret += indent + "    Position = " + position.toString() + ",\n";
			ret += indent + "    Orientation = " + ALL_ORIENTATIONS + "\n";
			ret += indent + "}";	// caller must terminate the line
			return ret;
		}
	}
	
	public class MapInfo {
		public String mapName;
		public String prefabName;
		public double density;
		public double weight;
		public double minOffset;
		public double maxOffset;
		public double minScale;
		public double maxScale;
		
		public MapInfo(String prefabName, double weight, 
						double minOffset, double maxOffset, 
						double minScale, double maxScale) {
			this.prefabName = prefabName;
			this.weight = weight;
			this.minOffset = minOffset;
			this.maxOffset = maxOffset;
			this.minScale = minScale;
			this.maxScale = maxScale;
		}
		
		/**
		 * return string representation of 
		 * @param indent
		 * @return
		 */
		public String toString(String indent) {
			String plus4 = indent + "    ";
			String plus8 = plus4 + "    ";
			
			String ret = indent + "{\n";
			
			ret += plus4 + "PrefabList = { \"PREFAB_" + prefabName + "\" },\n";
			if (weight == 1.0 || weight == 8.0)
				ret += plus4 + String.format("RandomWeight = %d,\n", (int) weight);
			else
				ret += plus4 + String.format("RandomWeight = %3.1f,\n", weight);
			
			ret += plus4 + "OffsetSizeRange = {\n";
			ret += plus8 + String.format("Min = %4.2f,\n", minOffset);
			if (maxOffset == 1.0)
				ret += plus8 + "Max = 1\n";
			else
				ret += plus8 + String.format("Max = %4.2f\n", maxOffset);
			ret += plus4 + "},\n";
			
			ret += plus4 + "OrientationRange = {\n";
			ret += plus8 + "Min = { 0, -180, 0},\n";
			ret += plus8 + "Max = { 0, 180, 0}\n";
			ret += plus4 + "},\n";
			
			ret += plus4 + "ScaleRange = {\n";
			ret += plus8 + String.format("Min = %4.2f,\n", minScale);
			ret += plus8 + String.format("Max = %4.2f\n", maxScale);
			ret += plus4 + "},\n";
			
			ret += plus4 + "ColorRange = {\n";
			ret += plus8 + "Min = { 0.8, 0.8, 0.8, 1 },\n";
			ret += plus8 + "Max = { 1, 1, 1, 1 }\n";
			ret += plus4 + "}\n";
			
			ret += indent + "}";
			
			return ret;
		}
	}
	
	/**
	 * create a new Foundation exporter
	 * 
	 * @param width of the export area (in tiles)
	 * @param height of the export area ((in tiles)
	 */
	public LuaWriter(String dirname) {
		parms = Parameters.getInstance();
		try {
			luaFile = new FileWriter(dirname + "/" + LUA_NAME);
			if (parms.debug_level >= EXPORT_DEBUG)
				System.out.println("Exportingd Foundation description " + 
									LUA_NAME + " to " + dirname);
		} catch(IOException e) {
			System.err.println("Write error while attempting to create " + LUA_NAME);
			luaFile = null;
		}
	}

	private static String maps[] = {
		"HEIGHT_MAP",				"maps/heightmap.png",
		"MATERIAL_MASK",			"maps/material_mask.png",
		"CONIFEROUS_DENSITY_MAP",	"maps/coniferous_density.png",
		"DECIDUOUS_DENSITY_MAP",	"maps/deciduous_density.png",
		"BERRIES_DENSITY_MAP",		"maps/berries_density.png",
		"ROCK_DENSITY_MAP",			"maps/rock_density.png",
		"IRON_DENSITY_MAP",			"maps/iron_density.png",
		"FISH_DENSITY_MAP",			"maps/fish_density.png" };
	
	/**
	 * write out a mod.lua file header
	 *
	 * @param min_altitude
	 * @param max_altitude
	 * @return
	 */
	public boolean fileHeader(int min_altitude, int max_altitude) {
		try {
			luaFile.write("local mapMod = foundation.createMod();\n");
			luaFile.write("\n");
			for(int i = 0; i < maps.length; i += 2) {
				String line = "mapMod:registerAssetId(\"";
				line += maps[i+1];
				line += "\", \"";
				line += maps[i];
				line += "\")\n";
				luaFile.write(line);
			}
			luaFile.write("\n");
			
			luaFile.write("-- Register WorldBuilder Generated Map\n");
			luaFile.write("mapMod:register({\n");
			luaFile.write("        DataType = \"CUSTOM_MAP\",\n");
			luaFile.write("        Id = \"" + parms.map_name + "\",\n" );
			luaFile.write("        HeightMap = \"HEIGHT_MAP\",\n" );
			luaFile.write("        MaterialMask = \"MATERIAL_MASK\",\n" );
			luaFile.write("        MinHeight = " + min_altitude + ",\n" );
			luaFile.write("        MaxHeight = " + max_altitude + ",\n" );
		} catch (IOException e) {
			System.err.println("Write error while attempting to create " + LUA_NAME);
			return false;
		}
		return true;
	}
	
	/**
	 * write out the closing braces and close the file
	 */
	public void close() {
		try {
			luaFile.write("})\n");
			luaFile.close();
		} catch(IOException e) {
			System.err.println("Write error while attempting to finish " + LUA_NAME);
		}
		luaFile = null;
	}
	
	public boolean villages(CityInfo[] cities) {
		try {
			String indent = "        ";
			String indentx2 = "            ";
			luaFile.write(indent + "VillagePathList = {\n");
			for(int i = 0; i < cities.length; i++) {
				luaFile.write(cities[i].toString(indentx2));
				luaFile.write((i < cities.length-1) ? ",\n" : "\n");
			}
			luaFile.write(indent + "},\n");
		} catch (IOException e) {
			System.err.println("Write error while attempting to create " + LUA_NAME);
			return false;
		}
		return true;
	}
	
	

	public boolean resources(ResourceInfo[] resource) {
		try {
			String indent = "        ";
			String indentx2 = "            ";
			luaFile.write(indent + "SpawnList = {\n");
			for(int i = 0; i < resource.length; i++) {
				luaFile.write(resource[i].toString(indentx2));
				luaFile.write((i < resource.length-1) ? ",\n" : "\n");
			}
			luaFile.write(indent + "},\n");
		} catch (IOException e) {
			System.err.println("Write error while attempting to create " + LUA_NAME);
			return false;
		}
		return true;
	}
	
	public boolean startDensities() {
		try {
			String indent = "        ";
			luaFile.write(indent + "DensitySpawnList = {\n");
		}  catch (IOException e) {
			System.err.println("Write error while attempting to create " + LUA_NAME);
			return false;
		}
		return true;
	}
	
	public boolean map(String name, double density, MapInfo[] maps, boolean last) {
		try {
			String indent = "            ";
			String plus4 = indent + "    ";
			String plus8 = plus4 + "    ";
			luaFile.write(indent + "{\n");
			luaFile.write(plus4 + "DensityMap = \"" + name + "\",\n");
			if (density == 1.0)		// FIX - just to make output identical to sample
				luaFile.write(plus4 + "Density = 1,\n");
			else
				luaFile.write(String.format("%sDensity = %3.1f,\n", plus4, density));
			luaFile.write(plus4 + "PrefabConfigList = {\n");
			for(int i = 0; i < maps.length; i++) {
				luaFile.write(maps[i].toString(plus8));
				luaFile.write((i < maps.length-1) ? ",\n" : "\n");
			}
			luaFile.write(plus4 + "}\n");
			luaFile.write(indent + (last ? "}\n" : "},\n"));
		} catch (IOException e) {
			System.err.println("Write error while attempting to create " + LUA_NAME);
			return false;
		}
		return true;
	}
	
	public boolean endDensities() {
		try {
			String indent = "        ";
			luaFile.write(indent + "}\n");
		}  catch (IOException e) {
			System.err.println("Write error while attempting to create " + LUA_NAME);
			return false;
		}
		return true;
	}
}
