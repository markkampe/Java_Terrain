package worldBuilder;

import java.io.FileWriter;
import java.io.IOException;

/**
 * Exporter to render a Cartesian map w/JSON descriptions of each point.
 */
public class LuaWriter {

	private FileWriter luaFile;
	private String lua_filename;
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
		
		/**
		 * return String representation of Position
		 */
		public String toString() {
			return "{ " + x + ", " + z + ", " + y + " }";
		}
	}
	
	/** 
	 * Foundation needs to know the coordinates of the 
	 * entry and exit points for each city/village.
	 */
	public class EntryPoint {
		public Position entrance;
		public Position exit;
		
		public EntryPoint(Position entrance, Position exit) {
			this.entrance = entrance;
			this.exit = exit;
		}
		
		/**
		 * return String representation of a City location
		 * @param indent String to put in front of each line
		 * @return String
		 */
		public String toString(String indent) {
			String ret = indent + "{\n";
			ret += indent + "    Entrance = " + entrance.toString() + ",\n";
			ret += indent + "    Exit = " + exit.toString() + "\n";
			ret += indent + "}";
			return ret;
		}
		
		public String toString() {return toString("");}
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
		
		public String toString() {return toString("");}
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
		
		public String toString() {return toString("");}
	}
	
	/**
	 * create a new Foundation exporter
	 * 
	 * @param dirname of directory into which maps should be written
	 */
	public LuaWriter(String dirname) {
		parms = Parameters.getInstance();
		lua_filename = dirname + "/" + LUA_NAME;
		try {
			luaFile = new FileWriter(lua_filename);
		} catch(IOException e) {
			System.err.println("Write error while attempting to create " + lua_filename);
			luaFile = null;
		}
	}

	private static String maps[] = {
		"HEIGHT_MAP",				"maps/heightmap",
		"MATERIAL_MASK",			"maps/material_mask",
	 };
	
	/**
	 * write out a mod.lua file header
	 *
	 * @param min_altitude
	 * @param max_altitude
	 * @param resource_maps
	 * @return
	 */
	public boolean fileHeader(int min_altitude, int max_altitude, String[] resource_maps) {
		try {
			luaFile.write("local mapMod = foundation.createMod();\n");
			luaFile.write("\n");
			
			// standard bit-maps
			for(int i = 0; i < maps.length; i += 2) {
				String line = "mapMod:registerAssetId(\"";
				line += maps[i+1] + "_" + parms.map_name + ".png";
				line += "\", \"";
				line += maps[i];
				line += "\")\n";
				luaFile.write(line);
			}
			
			// resource bit-maps
			for(int i = 0; i < resource_maps.length; i += 1) {
				String line = "mapMod:registerAssetId(\"";
				line += "maps/" + resource_maps[i] + "_density.png";
				line += "\", \"";
				line += resource_maps[i].toUpperCase() + "_DENSITY_MAP";
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
			System.err.println("Write error while writing header to " + lua_filename);
			return false;
		}
	
		return true;
	}
	
	public void comment(String text) {
		try {
			luaFile.write(text);
		} catch (IOException e) {
			System.err.println("Write error adding comment to " + lua_filename);
		}
	}
	
	/**
	 * write out the closing braces and close the file
	 */
	public void close() {
		try {
			luaFile.write("})\n");
			luaFile.close();
		} catch(IOException e) {
			System.err.println("Write error while attempting to finish " + lua_filename);
		}
		
		if (parms.debug_level >= EXPORT_DEBUG)
			System.out.println("Created Foundation configuration file: " + lua_filename);
		luaFile = null;
	}
	
	public boolean entrypoints(EntryPoint[] places) {
		try {
			String indent = "        ";
			String indentx2 = "            ";
			luaFile.write(indent + "VillagePathList = {\n");
			for(int i = 0; i < places.length; i++) {
				luaFile.write(places[i].toString(indentx2));
				luaFile.write((i < places.length-1) ? ",\n" : "\n");
			}
			luaFile.write(indent + "},\n");
		} catch (IOException e) {
			System.err.println("Write error while adding entrances/exits to " + lua_filename);
			return false;
		}
		return true;
	}
	
	
	/*
	 * The SpawnList is for explicit resource placement, but
	 * we are achieving this with resource density maps ...
	 * making the SpawnList is unnecessary
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
	*/
	
	public boolean startDensities() {
		try {
			String indent = "        ";
			luaFile.write(indent + "DensitySpawnList = {\n");
		}  catch (IOException e) {
			System.err.println("Write error while adding DensitySpawNlist to " + lua_filename);
			return false;
		}
		return true;
	}
	
	public boolean map(String name, String comment, double density, MapInfo[] maps, boolean last) {
		try {
			String indent = "            ";
			String plus4 = indent + "    ";
			String plus8 = plus4 + "    ";
			luaFile.write(indent + "{\n");
			luaFile.write(plus4 + "-- Create " + comment + "\n");
			luaFile.write(plus4 + "DensityMap = \"" + name + "\",\n");
			luaFile.write(String.format("%sDensity = %3.1f,\n", plus4, density));
			luaFile.write(plus4 + "PrefabConfigList = {\n");
			for(int i = 0; i < maps.length; i++) {
				luaFile.write(maps[i].toString(plus8));
				luaFile.write((i < maps.length-1) ? ",\n" : "\n");
			}
			luaFile.write(plus4 + "}\n");
			luaFile.write(indent + (last ? "}\n" : "},\n"));
		} catch (IOException e) {
			System.err.println("Write error while adding spawn info to " + lua_filename);
			return false;
		}
		return true;
	}
	
	public boolean endDensities() {
		try {
			String indent = "        ";
			luaFile.write(indent + "}\n");
		}  catch (IOException e) {
			System.err.println("Write error while finishing DensitySpawnList in " + lua_filename);
			return false;
		}
		return true;
	}
}
