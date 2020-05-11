package worldBuilder;

import java.awt.Color;
import java.io.File;
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

	public boolean fileHeader(int min_altitude, int max_altitude) {
		try {
			luaFile.write("local mapMod = foundation.createMod();\n");
			luaFile.write("\n");
			for(int i = 0; i < maps.length; i += 2) {
				String line = "mapMod:registerAssetID(\"";
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
	
	public void close() {
		try {
			luaFile.write("})\n");
			luaFile.close();
		} catch(IOException e) {
			System.err.println("Write error while attempting to finish " + LUA_NAME);
		}
		luaFile = null;
	}
	
	public boolean villages() {
		try {
			luaFile.write("        VillagePathList = {\n");
			luaFile.write("        },\n");
		} catch (IOException e) {
			System.err.println("Write error while attempting to create " + LUA_NAME);
			return false;
		}
		return true;
	}
	
	private static String resource[] = {"BERRIES", "ROCK", "IRON", "FISH"};
	private static String ALL_ORIENTATIONS = "Orientation = { 0.0, math.randomf(-180, 180), 0.0 }";

	public boolean resources() {
		try {
			luaFile.write("        SpawnList = {\n");
			for(int i = 0; i < resource.length; i++) {
				luaFile.write("            {\n");
				luaFile.write("                Prefab = \"PREFAB_RESOURCE_" +
							  resource[i] + "\",\n");
				luaFile.write("                Position = { ???, ???, ??? },\n");
				luaFile.write("                " + ALL_ORIENTATIONS + "\n");
				String termination = (i < resource.length-1) ? ",\n" : "\n";
				luaFile.write("             }" + termination);
			}
			luaFile.write("            },\n");
		} catch (IOException e) {
			System.err.println("Write error while attempting to create " + LUA_NAME);
			return false;
		}
		return true;
	}
	
	private static String deciduous[] = {"POPLAR", "OAK", "SYCAMORE", "PINE"};
	public boolean trees() {
		try {
			luaFile.write("        DensitySpawnList = {\n");
			luaFile.write("            DensityMap = \"DECIDUOUS_DENSITY_MAP\",\n");
			luaFile.write("            Density = 0.9\n");
			luaFile.write("            PrefabConfigList = {\n");
			for(int i = 0; i < deciduous.length; i++) {
				luaFile.write("            {\n");
				luaFile.write("                Prefab = \"PREFAB_RESOURCE_" +
							  resource[i] + "\",\n");
				luaFile.write("                Position = { ???, ???, ??? },\n");
				luaFile.write("                " + ALL_ORIENTATIONS + "\n");
				String termination = (i < resource.length-1) ? ",\n" : "\n";
				luaFile.write("             }" + termination);
			}
			luaFile.write("        },\n");
		} catch (IOException e) {
			System.err.println("Write error while attempting to create " + LUA_NAME);
			return false;
		}
		return true;
	}
}
