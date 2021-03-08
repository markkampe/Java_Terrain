package worldBuilder;

import java.awt.Color;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 * Exporter to render a simple Cartesian map with mountains, forests, etc
 * overlayed (as discrete objects) on top of it.
 */
public class ObjectExporter implements Exporter {	

	private Parameters parms;
	// private static final String soilTypes[] = {
	// 		"sedimentary", "metamorphic", "igneous", "alluvial"
	// };

	private int x_points;			// width of map (in points)
	private int y_points;			// height of map (in points)
	private int tile_size;			// tile size (in meters)

	private double lat;				// latitude
	private double lon;				// longitude

	// private double Tmean;		// mean temperature
	// private double Tsummer;		// mean summer temperature
	// private double Twinter;		// mean winter temperature

	private double[][] heights;		// per point height (meters)
	// private double[][] rain;		// per point rainfall (meters)
	private double[][] erode;		// per point erosion (meters)
	private double[][] waterDepth;	// per point water depth (meters)
	// private double[][] soil;		// per point soil type
	private OverlayObjects objSet;	// defined overlay objects

	private double maxHeight;		// highest discovered altitude
	private double minHeight;		// lowest discovered altitude
	private double maxDepth;		// deepest discovered water

	// brightness constants for preview colors
	private static final int DIM = 32;
	private static final int BRIGHT = 256 - DIM;
	private static final int NORMAL = 128;

	private static final int EXPORT_DEBUG = 2;

	/**
	 * create a new Raw JSON exporter
	 * 
	 * @param obj_palette name of OverlayObjects definition file
	 * @param width of the export area (in tiles)
	 * @param height of the export area ((in tiles)
	 */
	public ObjectExporter(String obj_palette, int width, int height) {
		this.x_points = width;
		this.y_points = height;
		parms = Parameters.getInstance();

		if (obj_palette != null && !obj_palette.equals(""))
			objSet = new OverlayObjects(obj_palette);
			
		if (parms.debug_level >= EXPORT_DEBUG)
			System.out.println("new Object exporter (" + height + "x" + width + ")");
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
		// this.Tmean = meanTemp;
		// this.Tsummer = meanSummer;
		// this.Twinter = meanWinter;
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
		// this.rain = rain;
	}

	/**
	 * Up-load the soil type for every tile
	 * @param soil - per point soil type
	 */
	public void soilMap(double[][] soil) {
		// this.soil = soil;
	}

	/**
	 * Up-load the flora type for every tile
	 * @param flora - per point flora type
	 */
	public void floraMap(double[][] flora) {
	}

	/**
	 * Up-load the surface-water-depth for every tile
	 * @param depths - per point depth of water
	 */
	public void waterMap(double[][] depths) {
		this.waterDepth = depths;

		// note the max and min heights
		maxDepth = 0;
		for (int i = 0; i < depths.length; i++)
			for (int j = 0; j < depths[0].length; j++) {
				if (depths[i][j] < maxDepth)
					maxDepth = depths[i][j];
			}
	}
	
	/**
	 * one object to be overlayed on our export grid
	 */
	private class Overlay {
		public int row;		// Y coordinate (tile offset)
		public int col;		// X coordinate (tile offset)
		OverlayObject obj;	// associated Overlay Object
		
		public Overlay(OverlayObject obj, int row, int col) {
			this.obj = obj;
			this.row = row;
			this.col = col;
		}
	}

	/**
	 * try to find places for OverlayObjects on our map
	 */
	public LinkedList<Overlay> overlays;	// all overlaid objects
	void chooseOverlays() {
		boolean[][] taken = new boolean[y_points][x_points];
		overlays = new LinkedList<Overlay>();
		
		// give each Overlay Object a shot at every tile
		for( ListIterator<OverlayObject> it = objSet.listIterator(); it.hasNext();) {
			OverlayObject o = it.next();
			for(int y = 0; y < y_points - o.height; y++)
				for(int x = 0; x < x_points - o.width; x++) {
					// see if every square within meets object requirements
					boolean ok = true;
					for(int i = 0; ok && i < o.height; i++)
						for(int j = 0; ok && j < o.width; j++) {
							// convert the Z altitude into a Z percentage
							double z = 100 * (heights[y+i][x+j] - erode[y+i][x+j]);
							if (taken[y+i][x+j])
								ok = false;
							else if (waterDepth[y+i][x+j] < 0)
								ok = false;
							else if (z < o.z_min || z >= o.z_max)
								ok = false;
						}
				
					if (ok) {
						overlays.add(new Overlay(o, y, x));
						for(int i = 0; ok && i < o.height; i++)
							for(int j = 0; ok && j < o.width; j++)
								taken[y+i][x+j] = true;
					}
				}
			
		}
	}

	/**
	 * Export the up-loaded information in selected format
	 * 
	 * @param filename - name of output file
	 */
	public boolean writeFile( String filename ) {
		
		// make sure we have an overlay list
		if (overlays == null)
			chooseOverlays();
		
		// strip off suffix and leading directories to get base name
		int dot = filename.lastIndexOf('.');
		String mapname = (dot == -1) ? filename : filename.substring(0, dot);
		int slash = mapname.lastIndexOf('/');
		if (slash == -1)
			slash = mapname.lastIndexOf('\\');
		if (slash != -1)
			mapname = mapname.substring(slash + 1);

		// generate the output
		try {
			FileWriter output = new FileWriter(filename);
			
			// start with the per-tile info
			final String FORMAT_S = " \"%s\": \"%s\"";
			final String FORMAT_D = " \"%s\": %d";
			final String FORMAT_DM = " \"%s\": \"%dm\"";
			final String FORMAT_L = " \"%s\": %.6f";
			final String FORMAT_O = " \"%s\": {";
			final String FORMAT_A = " \"%s\": [";

			final String NEW_POINT = "\n        { ";
			final String NEWLINE = "\n    ";
			final String COMMA = ", ";
			
			// write out the grid wrapper
			output.write("{");
			output.write(NEWLINE);
			output.write(String.format(FORMAT_S,  "name", mapname));
			output.write(",");
			output.write(NEWLINE);
			output.write(String.format(FORMAT_O, "dimensions"));
				output.write(String.format(FORMAT_D, "height", y_points));
				output.write(COMMA);
				output.write(String.format(FORMAT_D, "width", x_points));
				output.write(" },");
				output.write(NEWLINE);
			output.write(String.format(FORMAT_DM, "tilesize", tile_size));
			output.write(",");
			output.write(NEWLINE);
			output.write(String.format(FORMAT_O, "center"));
				output.write(String.format(FORMAT_L, "latitude", lat));
				output.write(COMMA);
				output.write(String.format(FORMAT_L, "longitude", lon));
				output.write(" },");
			output.write(NEWLINE);
			
			// write out the per-point altitudes and water depth
			final String FORMAT_Z = " \"%s\": \"%.8f\"";
			output.write(String.format(FORMAT_A, "points"));
			boolean first = true;
			for(int r = 0; r < y_points; r++) {
				for(int c = 0; c < x_points; c++) {
					if (first)
						first = false;
					else
						output.write(",");
					output.write(NEW_POINT);
					double z = heights[r][c]-erode[r][c];
					output.write(String.format(FORMAT_Z, "z", z));
					double depth = waterDepth[r][c];
					if (depth < 0) {
						output.write(COMMA);
						output.write(String.format(FORMAT_Z, "u/w", parms.z(-depth)));
					}
					output.write(" }");
				}
			}
			output.write(NEWLINE);
			output.write("]");	// end of points
			
			// write out the overlaid objects
			int overlay_count = 0;
			if (overlays.size() > 0) {
				output.write(",");
				output.write(NEWLINE);
				output.write(String.format(FORMAT_A, "overlays"));
				first = true;
				for( ListIterator<Overlay> it = overlays.listIterator(); it.hasNext();) {
					Overlay o = it.next();
					if (first)
						first = false;
					else
						output.write(",");
					output.write(NEW_POINT);
					output.write(String.format(FORMAT_D, "x", o.col));
					output.write(COMMA);
					output.write(String.format(FORMAT_D, "y", o.row));
					output.write(COMMA);
					output.write(String.format(FORMAT_S, "tile", o.obj.name));
					output.write(COMMA);
					output.write(String.format(FORMAT_D, "dx", o.obj.width));
					output.write(COMMA);
					output.write(String.format(FORMAT_D, "dy", o.obj.height));
					output.write(" }");
					
					overlay_count++;
				}
				output.write(NEWLINE);
				output.write("]");	// end of overlays
			}
			
			// and close out the grid
			output.write("\n");
			output.write( "}\n");
			output.close();

			if (parms.debug_level > 0) {
				System.out.println("Exported(OBJECT(WIP)) "  + 
						x_points + "x" + y_points + " " + 
						tile_size + "M tiles" +
						" plus " + overlay_count + " overlay objects" +
						" from <" + String.format("%9.6f", lat) + "," + String.format("%9.6f", lon) +
						"> to file " + filename);
			}
			return true;
		} catch (IOException e) {
			System.err.println("Unable to export map to file " + filename);
			return false;
		}
	}

	/**
	 * generate a preview of the currently up-loaded export
	 * @param chosen map type (e.g. height, flora)
	 * @param colorMap - palette to be used in preview
	 */
	public void preview(WhichMap chosen, Color colorMap[]) {

		if (chosen == WhichMap.HEIGHTMAP) {
			// make sure we have an overlay list
			if (overlays == null)
				chooseOverlays();
			
			// figure out the (range scaled) altitude to color mapping
			double aMean = (maxHeight + minHeight)/2;
			double aScale = BRIGHT - DIM;
			if (maxHeight > minHeight)
				aScale /= maxHeight - minHeight;

			// fill in the preview map with altitudes or water
			Color map[][] = new Color[y_points][x_points];
			for(int i = 0; i < y_points; i++)
				for(int j = 0; j < x_points; j++)
					if (waterDepth[i][j] >= 0) {	// land
						double h = NORMAL + ((heights[i][j] - aMean) * aScale);
						map[i][j] = new Color((int)h, (int)h, (int)h);
					} else	{							// water
						double depth = waterDepth[i][j]/maxDepth;
						double h = (1 - depth) * (BRIGHT - DIM);
						map[i][j] = new Color(0, (int) h, BRIGHT);
					}
			
			// add any overlayed icons
			PreviewMap preview = new PreviewMap("Export Preview (terrain)", map, objSet.tileSize);
			if (overlays != null && overlays.size() > 0)
				for( ListIterator<Overlay> it = overlays.listIterator(); it.hasNext();) {
					Overlay o = it.next();
					preview.addIcon(o.row, o.col, o.obj.icon);
				}
		} else if (chosen == WhichMap.FLORAMAP) {
			// fill in the preview map
			Color map[][] = new Color[y_points][x_points];
			for(int i = 0; i < y_points; i++)
				for(int j = 0; j < x_points; j++) {
					// FIX: implement flora preview
					map[i][j] = new Color(NORMAL, NORMAL, NORMAL);
				}
			new PreviewMap("Export Preview (flora)", map, objSet.tileSize);
		}
	}
}
