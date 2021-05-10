package worldBuilder;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.LinkedList;

import javax.json.Json;
import javax.json.stream.JsonParser;
import javax.swing.*;

/**
 * the displayable, resizeable, 2D representation of a mesh and
 * the attributes of its points.
 */
public class Map extends JPanel implements MouseListener, MouseMotionListener {
	/*
	 * This class maintains a few basic types of information:
	 * 	 1. a Mesh of random Voronoi MeshPoints (with attributes) 
	 *   2. Cartesian arrays with the attributes of displayable points
	 *   3. a translation ray to determine the values of Cartesian
	 *      cells as a function of the surrounding Voronoi points.
	 *      
	 * This class operates on logical (-0.5 to +0.5) mesh coordinates,
	 * and height/width display coordinates.  It is (largely) agnostic
	 * about real-world (e.g. meters) coordinates.
	 */

	/** options for enabled display types	*/
	public static final int SHOW_ALL = 0xfff,
							SHOW_POINTS = 0x01,
							SHOW_MESH  = 0x002,
							SHOW_TOPO  = 0x004,
							SHOW_RAIN  = 0x008,
							SHOW_WATER = 0x010,
							SHOW_ERODE = 0x020,
							SHOW_ROCKS = 0x040,
							SHOW_FLORA = 0x080,
							SHOW_FAUNA = 0x100,
							SHOW_CITY  = 0x200;
	protected int display;	// bitmask for enabled SHOWs
	
	// map size (in pixels)
	private static final int MIN_WIDTH = 400;	// min screen width
	private static final int MIN_HEIGHT = 400;	// min screen height
	private static final int SMALL_POINT = 2;	// width of a small point
	private static final int LARGE_POINT = 4;	// width of a large point
	private static final int SELECT_RADIUS = 6;	// width of a selected point indicator
	private static final int TOPO_CELL = 5;		// pixels/topographic cell
												// CODE DEPENDS ON THIS CONSTANT
	private Dimension size;
	
	// displayed window offset and size
	private double x_min, y_min, x_max, y_max;
	
	/** current map is a subRegion of a larger map */
	public boolean isSubRegion;
	/** underlying Mesh of Voronoi points */
	public Mesh mesh;

	// display colors
	private static final Color SELECT_COLOR = Color.WHITE;
	private static final Color POINT_COLOR = Color.PINK;
	private static final Color MESH_COLOR = Color.GREEN;
	
	private Color highLights[];		// points to highlight
	private boolean highlighting;	// are there points to highlight
	private double highlight_x, highlight_y;	// non-mesh-point highlighting
	
	public Drainage drainage;		// drainage calculator
	public WaterFlow waterflow;		// water-flow calculator
	
	public Color floraColors[];	// display color for each flora type
	public Color rockColors[];	// display color for each mineral type
	public Color faunaColors[];	// display color for each fauna type
	public String floraNames[];	// import/export name for each flora type
	public String rockNames[];	// import/export name for each mineral type
	public String faunaNames[];	// import/export name for each fauna type

	// per MeshPoint information
	private String nameMap[];	// name/description of each mesh POint
	private double heightMap[]; // Height of each mesh point
	private double soilMap[];	// Soil type of each mesh point
	private double rainMap[];	// Rainfall of each mesh point
	private double fluxMap[];	// Water flow through each point
	private double erodeMap[];	// erosion/deposition
	private double incoming[];	// incoming water from off-map
	private double floraMap[];	// assigned flora type
	private double faunaMap[];	// assigned fauna type
	private double waterLevel[];// level of nearest water body

	private Cartesian poly_map;		// interpolation based on surrounding polygon
	private double tileHeight[][];	// altitude of each screen tile (Z units)
	private double tileDepth[][];	// depth u/w of each screen tile (meters)
	
	private LinkedList<POI> poi_list;	// list of points of interest
	
	/** selection types: points, line, rectangle, ... */
	public enum Selection {NONE, POINT, POINTS, LINE, RECTANGLE, SQUARE, ANY};
	private Selection sel_mode;	// What types of selection are enabled
	private MapListener listener;	// who to call for selection events
	
	/** results of hydrological computation	*/
	public double max_height,		// maximum altitude (m MSL)
				  min_height,		// minimum altitude (m MSL)
				  min_slope,		// shallowest slope
				  max_slope,		// steepest slope
				  min_flux,			// minimum river flow (m^3/s)
				  max_flux,			// maximum river flow (m^3/s)
				  min_velocity,		// minimum water velocity (m/s)
				  max_velocity,		// maximum water velocity (m/s)
				  max_erosion,		// maximum soil loss due to erosion (m)
				  max_deposition;	// maximum soil gain due to sedimentation (m)

	/** rainfall	*/
	public double min_rain,			// minimum rainfall (cm/y)
				  max_rain;			// maximum rainfall (cm/y)

	private Parameters parms;
	
	private static final int MAP_DEBUG = 2;
	private static final long serialVersionUID = 1L;

	/**
	 * instantiate a displayable map widget
	 * 
	 * @param width
	 *            ... preferred width (in pixels)
	 * @param height
	 *            ... perferred height 9in pixels)
	 */
	public Map(int width, int height) {
		this.size = new Dimension(width, height);
		this.parms = Parameters.getInstance();
		this.isSubRegion = false;
		setWindow(-Parameters.x_extent/2, -Parameters.y_extent/2, Parameters.x_extent/2, Parameters.y_extent/2);
		
		this.addMouseMotionListener(this);
		this.addMouseListener(this);
		sel_mode = Selection.ANY;
	}
	
	/**
	 * create a new sub-region map
	 * 
	 * @param numpoints ... number of desired points in new Mesh
	 */
	public void subregion(int numpoints) {
		// compute x/y coordinate translation coefficients
		double xShift = map_x(sel_x0 + (sel_width/2));
		double yShift = map_y(sel_y0 + (sel_height/2));
		double xScale = Parameters.x_extent/map_width(sel_width);
		double yScale = Parameters.y_extent/map_height(sel_height);
		
		// generate a new mesh, w/requested number of points, where the
		Mesh newMesh = new Mesh();
		MeshPoint[] points = newMesh.makePoints(numpoints);
		newMesh.makeMesh(points);
		
		// allocate new attribute maps
		int newlen = newMesh.vertices.length;
		double[] h = new double[newlen];	// height map
		double[] e = new double[newlen];	// erosion map
		double[] r = new double[newlen];	// rain map
		double[] m = new double[newlen];	// soil/mineral map
		double[] f = new double[newlen];	// flora map
		double[] a = new double[newlen];	// fauna map
		double[] w = new double[newlen];	// incoming map
		String[] n = new String[newlen];	// name Map

		// interpolate per-point attributes for each mesh point
		for(int i = 0; i < newlen; i++) {
			// find the corresponding previous-map coordinates
			double x = (newMesh.vertices[i].x/xScale) + xShift;
			double y = (newMesh.vertices[i].y/yScale) + yShift;
			
			// find surrounding points from the previous map
			Vicinity poly = new Polygon(mesh, x, y);

			// interpolate values for height, rain, minerals, flora, fauna
			h[i] = poly.interpolate(heightMap);
			e[i] = poly.interpolate(erodeMap);
			r[i] = poly.interpolate(rainMap);
			m[i] = poly.nearest(soilMap);
			f[i] = poly.nearest(floraMap);
			a[i] = poly.nearest(faunaMap);
		}
		
		// reproduce all water flows INTO the new sub-region
		for (int i = 0; i < mesh.vertices.length; i++) {
			MeshPoint p = mesh.vertices[i];
			if (inTheBox(p.x, p.y)) {
				for (int j = 0; j < mesh.vertices.length; j++) {
					MeshPoint p2 = mesh.vertices[j];
					if (drainage.downHill[j] == i && !inTheBox(p2.x, p2.y)) {
						// we found a point of water entry into the box
						// find the closest point (in new mesh) to the source
						double x = (p2.x - xShift) * xScale;
						double y = (p2.y - yShift) * yScale;
						MeshPoint p3 = newMesh.choosePoint(x,y);
						w[p3.index] += fluxMap[p2.index];
						p3.immutable = true;
					}
				}
			}
		}
		
		// FIX find named points within subregion
		
		// install and attribute the new mesh
		setMesh(newMesh);
		rainMap = r;
		soilMap = m;
		floraMap = f;
		faunaMap = a;
		erodeMap = e;
		incoming = w;
		heightMap = h;
		nameMap = n;
		tileHeight = null;
		tileDepth = null;
		
		// instantiate hydrology engine, recompute drainage and water flow
		drainage = new Drainage(this);
		waterflow = new WaterFlow(this);
		
		if (parms.debug_level > 0) {
			parms.worldParms();
			region_stats();
		}
	}
	
	
	/**
	 * read a saved map in from a file
	 * 
	 * @param filename - of input file
	 */
	public void read(String filename) {
		// get load flora/fauna/mineral names and preview colors
		Placement p = new Placement(parms.flora_rules, null, null);
		setFloraColors(p.previewColors());
		setFloraNames(p.resourceNames());
		p = new Placement(parms.fauna_rules, null, null);
		setFaunaColors(p.previewColors());
		setFaunaNames(p.resourceNames());
		p = new Placement(parms.mineral_rules, null, null);
		setRockColors(p.previewColors());
		setRockNames(p.resourceNames());
		
		// read in the underlying mesh
		Mesh m = new Mesh();
		m.read(filename);
		setMesh(m);
		
		BufferedReader r;
		if (filename == null) {
			filename = String.format(WorldBuilder.DEFAULT_TEMPLATE, parms.points);
			InputStream s = getClass().getResourceAsStream(filename);
			r = new BufferedReader(new InputStreamReader(s));
		} else {
			try {
				r = new BufferedReader(new FileReader(filename));
			} catch (FileNotFoundException e) {
				System.err.println("FATAL: unable to open input file " + filename);
				return;
			}
		}
		
		// parsing state
		JsonParser parser = Json.createParser(r);
		String thisKey = "";		// last key read
		boolean inPoints = false;	// in points array
		boolean inPoIs = false;		// in points of interest
		int length = 0;				// expected number of points
		int points = 0;				// number of points read	
		
		// per-point parameters we are looking for
		double z = 0;
		double rain = 0;
		int soil = 0;
		int flora = 0;
		int fauna = 0;
		double influx = 0;
		isSubRegion = false;
		double x = 0;
		double y = 0;
		String name = null;
		String poi_type = "";
		String poi_name = "";
		
		// reallocate this every time
		this.poi_list = new LinkedList<POI>();
		
		while(parser.hasNext()) {
			JsonParser.Event e = parser.next();
			switch(e) {		
			case KEY_NAME:
				thisKey = parser.getString();
				if (length > 0) {
					if (thisKey.equals("points")) {
						inPoints = true;
						inPoIs = false;
						points = 0;
					} else if (thisKey.equals("pois")) {
						inPoIs = true;
						inPoints = false;
					} else if (thisKey.equals("mesh")) {
						inPoints = false;
						inPoIs = false;
					}
				}
				break;

			case VALUE_STRING:
			case VALUE_NUMBER:
				switch(thisKey) {
					case "length":
						length = new Integer(parser.getString());
						break;

					// per point attributes
					case "x":
						x = new Double(parser.getString());
						break;
					case "y":
						y = new Double(parser.getString());
						break;
					case "z":
						z = new Double(parser.getString());
						break;
						
					case "soil":
						soil = getSoilType(parser.getString());
						break;
						
					case "flora":
						flora = getFloraType(parser.getString());
						break;
						
					case "fauna":
						fauna = getFaunaType(parser.getString());
						break;
						
					case "rain":
						String s = parser.getString();
						int u = s.indexOf(Parameters.unit_r);
						if (u != -1)
							s = s.substring(0, u);
						rain = new Double(s);
						break;
						
					case "influx":
						s = parser.getString();
						u = s.indexOf(Parameters.unit_f);
						if (u != -1)
							s = s.substring(0, u);
						influx = new Double(s);
						break;
						
					case "type":
						poi_type = parser.getString();
						break;
						
					case "name":
						if (inPoIs)
							poi_name = parser.getString();
						else
							name = parser.getString();
						break;
						
					// world attributes
					case "sealevel":
						s = parser.getString();
						u = s.indexOf(Parameters.unit_z);
						if (u != -1)
							s = s.substring(0, u);
						parms.sea_level = new Double(s) / parms.z_range;
						break;
						
					case "amount":
						s = parser.getString();
						u = s.indexOf(Parameters.unit_r);
						if (u != -1)
							s = s.substring(0, u);
						parms.dAmount = new Integer(s);
						break;
						
					case "direction":
						parms.dDirection = new Integer(parser.getString());
						break;
						
					case "radius":
						s = parser.getString();
						u = s.indexOf(Parameters.unit_xy);
						if (u != -1)
							s = s.substring(0, u);
						parms.radius = new Integer(s);
						break;
						
					case "tilt":
						parms.tilt = new Double(parser.getString());
						break;
						
					case "xy_range":
						s = parser.getString();
						u = s.indexOf(Parameters.unit_xy);
						if (u != -1)
							s = s.substring(0, u);
						parms.xy_range = new Integer(s);
						break;
						
					case "z_range":
						s = parser.getString();
						u = s.indexOf(Parameters.unit_z);
						if (u != -1)
							s = s.substring(0, u);
						parms.z_range = new Integer(s);
						break;
						
					case "latitude":
						parms.latitude = new Double(parser.getString());
						break;
						
					case "longitude":
						parms.longitude = new Double(parser.getString());
						break;
						
					case "map_name":
						parms.map_name = parser.getString();
						break;
						
					case "parent_name":
						parms.parent_name = parser.getString();
						break;
						
					case "description":
						parms.description = parser.getString();
						break;
				}
				break;
				
			case VALUE_FALSE:
				if (thisKey.equals("subregion"))
					isSubRegion = false;
				break;

			case VALUE_TRUE:
				if (thisKey.equals("subregion"))
					isSubRegion = true;
				
			case END_OBJECT:
				if (inPoints) {
					heightMap[points] = z;
					soilMap[points] = soil;
					floraMap[points] = flora;
					faunaMap[points] = fauna;
					rainMap[points] = rain;
					incoming[points] = influx;
					nameMap[points] = name;
					points++;
				} else if (inPoIs) {
					poi_list.add(new POI(poi_type, poi_name, x, y));
				}
				break;
				
			case START_ARRAY:
				break;
				
			case END_ARRAY:
				if (inPoints)
					inPoints = false;
				else if (inPoIs)
					inPoIs = false;
				break;
				
			case START_OBJECT:
				if (inPoints) {
					influx = 0;
					soil = 0;
					flora = 0;
					fauna = 0;
					rain = 0;
					z = 0.0;
					name = null;
				} else if (inPoIs) {
					poi_name = "";
					poi_type = "";
				}
			default:
				break;
			}
		}
		parser.close();
		try {
			r.close();
		} catch (IOException e) {
			System.err.println("FATAL: close error on input file " + filename);			
		}
		
		parms.checkDefaults();	// Make sure defaults are consistent w/new world size
		
		// the topography ahd Hydrology engines
		drainage = new Drainage(this);
		waterflow = new WaterFlow(this);
		
		if (parms.debug_level > 0) {
			parms.worldParms();
			region_stats();
		}
	}
	
	/**
	 * summarize the world topography and hydrology
	 */
	public void region_stats() {
		System.out.println("Geographic Ranges");
		System.out.println(String.format("  altitude: %.1f-%.0f%s (MSL)",
								parms.altitude(min_height), 
								parms.altitude(max_height),
								Parameters.unit_z));
		System.out.println(String.format("  slope:    %.3f-%.3f", min_slope, max_slope));
		
		if (max_rain > 0) 
			System.out.println(String.format("  rain:     %.1f-%.1f%s",
								min_rain, max_rain, Parameters.unit_r));

		if (max_flux > 0)
			System.out.println(String.format("  rivers:   velocity=%.4f-%.2f%s, flow=%.3f-%.2f%s",
								min_velocity, max_velocity, Parameters.unit_v, 
								min_flux, max_flux, Parameters.unit_f));
		else
			System.out.println("  rivers:   NONE");
		
		if (max_erosion > 0 || max_deposition > 0)
			System.out.println(String.format("  erosion:  %.2f%s, deposition: %.2f%s",
								parms.height(max_erosion), Parameters.unit_z,
								parms.height(max_deposition), Parameters.unit_z));
		else
			System.out.println("  erosion:  NONE, deposition: NONE");
	}
	
	/**
	 * write a mesh of MapPoints out to a file
	 * @param filename - of the file to be (re)written
	 */
	public boolean write(String filename) {
		try {
			FileWriter output = new FileWriter(filename);
			final String T_FORMAT = "    \"subregion\": true,\n";
			final String S_FORMAT = "    \"sealevel\": \"%d%s\",\n";
			final String L_FORMAT = "    \"center\": { \"latitude\": \"%.6f\", \"longitude\": \"%.6f\" },\n";
			final String W_FORMAT = "    \"world\": { \"radius\": \"%d%s\", \"tilt\": \"%.1f\" },\n";
			final String Z_FORMAT = "    \"scale\": { \"xy_range\": \"%d%s\", \"z_range\": \"%d%s\" },\n";
			final String M_format = "    \"map_name\": \"%s\",\n";
			final String P_format = "    \"parent_name\": \"%s\",\n";
			final String D_format = "    \"description\": \"%s\",\n";
			final String I_FORMAT = "        { \"x\": %.7f, \"y\": %.7f, \"type\": \"%s\", \"name\": \"%s\" }";
			
			output.write( "{   \"length\": " + mesh.vertices.length + ",\n");
			output.write( String.format(M_format, parms.map_name));
			if (isSubRegion)
				output.write(T_FORMAT);
			if (parms.parent_name != null)
				output.write(String.format(P_format, parms.parent_name));
			
			// write out the world parameters:
			//	planetary radius and tilt, lat/lon, map span
			//	sea-level, incoming rainfall, erosion coefficient
			output.write(String.format(W_FORMAT, parms.radius, Parameters.unit_xy, parms.tilt));
			output.write(String.format(L_FORMAT, parms.latitude, parms.longitude));
			output.write(String.format(Z_FORMAT, parms.xy_range, Parameters.unit_xy,
					parms.z_range, Parameters.unit_z));	
			if (parms.sea_level != 0) {
				double l = parms.sea_level * parms.z_range;
				output.write(String.format(S_FORMAT, (int) l, Parameters.unit_z));
			}
			
			if (parms.description != "")
				output.write(String.format(D_format,  parms.description));
			
			// write out the points of interest
			if (poi_list.size() > 0) {
				output.write( "    \"pois\": [" );
				int numPoints = 0;
				for(Iterator<POI> it = poi_list.iterator(); it.hasNext();) {
					POI point = it.next();
					output.write((numPoints++ == 0) ? "\n" : ",\n");
					output.write(String.format(I_FORMAT, point.x, point.y, point.type, point.name ));
				}
				output.write(" ],\n");
			}
			
			// write out the points and per-point attributes
			output.write( "    \"points\": [" );
			for(int i = 0; i < mesh.vertices.length; i++) {
				output.write((i == 0) ? "\n" : ",\n");
				MeshPoint p = mesh.vertices[i];
				writePoint(output, p);
			}
			output.write(" ],\n");
			
			// then write out the neighbor connections
			int paths = 0;
			output.write( "    \"mesh\": [\n" );
			for(int i = 0; i < mesh.vertices.length; i++) {
				if (i != 0)
					output.write(",\n");
				MeshPoint m = mesh.vertices[i];
				output.write("        [ ");
				for(int n = 0; n < m.neighbors; n++) {
					if (n != 0)
						output.write(", ");
					output.write(String.format("%d",  m.neighbor[n].index));
					paths++;
				}
				output.write(" ]");
			}
		
			output.write( "\n    ]\n");
			output.write( "}\n");
			output.close();
			
			if (parms.debug_level > 0)
				System.out.println("saved " + mesh.vertices.length + " vertices, " + paths/2 + " unique paths to file " + filename);
				return true;
		} catch (IOException e) {
			System.err.println("Unable to create output file " + filename);
			return false;
		}
	}
	
	/**
	 * write out the description of a single point
	 * @param output ... open outputWriter
	 * @param meshpoint to write
	 * @throws IOException 
	 */
	private void writePoint(FileWriter output, MeshPoint p) throws IOException {
		output.write("        {");
		output.write(String.format(" \"x\": %.7f", p.x));
		output.write(String.format(", \"y\": %.7f", p.y));
		
		int x = p.index;
		if (heightMap[x] != 0)
			output.write(String.format(", \"z\": %.9f", heightMap[x]));
		output.write(String.format(", \"rain\": \"%.1f%s\"", rainMap[x], Parameters.unit_r));
		if (soilMap[x] != 0)
			output.write(String.format(", \"soil\": \"%s\"", rockNames[(int) Math.round(soilMap[x])]));
		if (floraMap[x] != 0)
			output.write(String.format(", \"flora\": \"%s\"", floraNames[(int) Math.round(floraMap[x])]));
		if (faunaMap[x] != 0)
			output.write(String.format(", \"fauna\": \"%s\"", faunaNames[(int) Math.round(faunaMap[x])]));
		if (incoming[x] != 0)
			output.write(String.format(", \"influx\": \"%.2f%s\"", incoming[x], Parameters.unit_f));
		if (nameMap[x] != null)
			output.write(String.format(", \"name\": \"%s\"",  nameMap[x]));
		output.write(" }");
	}
	
	/**
	 * return the Mesh underlying the current map
	 */
	public Mesh getMesh() { return mesh; }

	/**
	 * create a map around a new Mesh
	 * @param mesh to be used
	 */
	public void setMesh(Mesh mesh) {
		this.mesh = mesh;	
		if (mesh != null) {
			this.heightMap = new double[mesh.vertices.length];
			this.rainMap = new double[mesh.vertices.length];
			this.fluxMap = new double[mesh.vertices.length];
			this.erodeMap = new double[mesh.vertices.length];
			this.soilMap = new double[mesh.vertices.length];
			this.waterLevel = new double[mesh.vertices.length];
			this.floraMap = new double[mesh.vertices.length];
			this.faunaMap = new double[mesh.vertices.length];
			this.highLights = new Color[mesh.vertices.length];
			this.incoming = new double[mesh.vertices.length];
			this.nameMap = new String[mesh.vertices.length];
			this.poi_list = new LinkedList<POI>();
			this.drainage = new Drainage(this);
			this.waterflow = new WaterFlow(this);
			// these will be created by the first paint()
			this.poly_map = null;
			this.tileHeight = null;
			this.tileDepth = null;
		} else {
			this.poly_map = null;
			// this.nearest_map = null;
			// this.prox_map = null;
			this.heightMap = null;
			this.rainMap = null;
			this.fluxMap = null;
			this.erodeMap = null;
			this.soilMap = null;
			this.waterLevel = null;
			this.floraMap = null;
			this.faunaMap = null;
			this.incoming = null;
			this.nameMap = null;
			this.poi_list = null;
			this.highLights = null;
			this.drainage = null;
			this.waterflow = null;
			this.tileHeight = null;
			this.tileDepth = null;
		}
		
		repaint();
	}
	
	/**
	 * return heightmap (pre-erosion Z values) for the current mesh
	 */
	public double[] getHeightMap() {return heightMap;}

	/**
	 * update the height map for the current mesth	 * @param newHeight new set of Z values
	 */
	public double[] setHeightMap(double newHeight[]) {
		double old[] = heightMap; 
		// recompute drainage and waterflow
		heightMap = newHeight; 
		drainage.recompute();
		waterflow.recompute();
		tileHeight = null;
		tileDepth = null;
		repaint();
		
		return old;
	}
	
	/**
	 * return rainMap (cm of rainfall) for the current mesh
	 */
	public double[] getRainMap() {return rainMap;}

	/**
	 * update the rainfall map for the current mesth
	 * @param newRain new set of rainfall values
	 */
	public double[] setRainMap(double newRain[]) {
		double old[] = rainMap; 
		rainMap = newRain; 
		waterflow.recompute();
		tileDepth = null;
		repaint();

		return old;
	}
	
	/**
	 * record a change in sea level
	 * @param z_value new sea level
	 */
	public void setSeaLevel(double z_value) {
		parms.sea_level = z_value;
		drainage.recompute();
		waterflow.recompute();
		tileDepth = null;
		repaint();
	}
	
	/**
	 * return the current sea level Z-value
	 */
	public double getSeaLevel() {
		return parms.sea_level;
	}
	
	/**
	 * update the water-flow after a change to the incoming arterial river
	 * @param new_map new incoming flux per point
	 */
	public void setIncoming(double[] new_map) {
		incoming = new_map;
		waterflow.recompute();
		tileDepth = null;
		repaint(); 
	}
	
	/**
	 * return incoming off-map rivers
	 */
	public double[] getIncoming() { return incoming; }

	
	/**
	 * return map of soil type for the current mesh
	 */
	public double [] getSoilMap() {return soilMap;}

	/**
	 * update the soil map for the current mesh
	 * @param newSoil new set of soil types
	 */
	public double[] setSoilMap(double[] newSoil) {
		double[] old = soilMap;
		soilMap = newSoil;
		repaint();
		return old;
	}
	
	/**
	 * return type ID of a specified mineral/soil name
	 */
	public int getSoilType(String name) {
		for(int i = 0; i < rockNames.length; i++)
			if (rockNames[i] != null && name.equals(rockNames[i]))
				return i;
		return 0;
	}
	
	/**
	 * return ID-> mineral preview color map
	 */
	public Color[] getRockColors() {
		return rockColors;
	}
	
	/**
	 * return ID-> mineral name map
	 */
	public String[] getRockNames() {
		return rockNames;
	}
	
	/**
	 * update the mapping from rock types to preview colors
	 * @param newColors
	 */
	public void setRockColors(Color[] newColors) {
		rockColors = new Color[newColors.length];
		for(int i = 0; i < newColors.length; i++)
			rockColors[i] = newColors[i];
	}
	
	/**
	 * update the mapping from mineral types to names
	 * @param newNames
	 */
	public void setRockNames(String[] newNames) {
		rockNames = new String[newNames.length];
		for(int i = 0; i < newNames.length; i++)
			rockNames[i] = newNames[i];
	}
	
	/**
	 * return map of flora types for the current mesh
	 */
	public double[] getFloraMap() { return floraMap; }
	
	/**
	 * update the flora map for the current mesh
	 * @param newFlora new set of flora types
	 * @return previous flora map
	 */
	public double[] setFloraMap(double[] newFlora) {
		double[] old = floraMap;
		floraMap = newFlora;
		repaint();
		return old;
	}
	
	/**
	 * return type ID of a specified flora type
	 */
	public int getFloraType(String name) {
		for(int i = 0; i <floraNames.length; i++)
			if (floraNames[i] != null && name.equals(floraNames[i]))
				return i;
		return 0;
	}
	
	/**
	 * update the mapping from flora types to preview colors
	 * @param newColors
	 */
	public void setFloraColors(Color[] newColors) {
		floraColors = new Color[newColors.length];
		for(int i = 0; i < newColors.length; i++)
			floraColors[i] = newColors[i];
	}
	
	/**
	 * update the mapping from flora types to names
	 * @param newNames
	 */
	public void setFloraNames(String[] newNames) {
		floraNames = new String[newNames.length];
		for(int i = 0; i < newNames.length; i++)
			floraNames[i] = newNames[i];
	}
	
	/**
	 * update the flora map for the current mesh
	 * @param newFlora new set of flora types
	 * @return previous flora map
	 */
	public double[] setFaunaMap(double[] newFauna) {
		double[] old = faunaMap;
		faunaMap = newFauna;
		repaint();
		return old;
	}
	
	/**
	 * return map of flora types for the current mesh
	 */
	public double[] getFaunaMap() { return faunaMap; }
	
	/**
	 * return ID-> mineral name map
	 */
	public String[] getFaunaNames() {
		return faunaNames;
	}
	
	/**
	 * return type ID of a specified flora type
	 */
	public int getFaunaType(String name) {
		for(int i = 0; i <faunaNames.length; i++)
			if (faunaNames[i] != null && name.equals(faunaNames[i]))
				return i;
		return 0;
	}
	
	/**
	 * update the mapping from flora types to preview colors
	 * @param newColors
	 */
	public void setFaunaColors(Color[] newColors) {
		faunaColors = new Color[newColors.length];
		for(int i = 0; i < newColors.length; i++)
			faunaColors[i] = newColors[i];
	}
	
	/**
	 * update the mapping from flora types to names
	 * @param newNames
	 */
	public void setFaunaNames(String[] newNames) {
		faunaNames = new String[newNames.length];
		for(int i = 0; i < newNames.length; i++)
			faunaNames[i] = newNames[i];
	}
	
	/**
	 * return the list of per-point names/descriptions
	 */
	public String[] getNameMap() {
		return nameMap;
	}
	
	/**
	 * return the list of Points of Interest
	 */
	public LinkedList<POI> getPOI() {
		return poi_list;
	}
	
	/**
	 * add a POI
	 * @param point new POI to be added
	 */
	public void addPOI(POI point) {
		poi_list.add(point);
		if (parms.debug_level > 0)
			System.out.println(String.format("POI: %s(%s) at <%.6f,%.6f>",
							point.type, point.name,
							parms.latitude(point.y), parms.longitude(point.x)));
	}
	
	/*
	 * these arrays are regularly re-calculated from height/rain
	 * and so do not need to be explicitly SET
	 */

	/**
	 * return array of water flow through each mesh point
	 */
	public double[] getFluxMap() {return fluxMap;}

	/**
	 * return array of net erosion/deposition at each mesh point
	 */
	public double[] getErodeMap() {return erodeMap;}
	
	/**
	 * return array of nearest water level to each mesh point
	 */
	public double[] getWaterLevel() {return waterLevel;}

	/**
	 * return map from flora types into preview colors
	 */
	public Color[] getFloraColors() { return floraColors; }
	
	/**
	 * return map from flora types into preview colors
	 */
	public Color[] getFaunaColors() { return faunaColors; }
	
	
	/**
	 * return reference to the Drainage calculator
	 */
	public Drainage getDrainage() { return drainage; }
	
	/**
	 * return reference to the per-tile heights (not valid until paint)
	 */
	public double[][] getTileHeights() { return tileHeight; }
	
	/**
	 * return reference to the per-tile depths (in meters)
	 */
	public double[][] getTileDepths() { return tileDepth; }
	
	/**
	 * return MeshPoint to Cartesian translation matrix
	 * param type (NEIGBORS, POLOGYON, NEAREST)
	 */
	public Cartesian getCartesian(Cartesian.vicinity type) {
		//if (type == Cartesian.vicinity.NEIGHBORS)
		//	return prox_map;	
		//if (type == Cartesian.vicinity.NEAREST)
		//	return nearest_map;
		return poly_map;
	}
	
	/**
	 * enable/disable display elements
	 * 
	 * @param view to be enabled/disabled
	 * @param on ... should this be enabled or disabled
	 * @return current sense of that view
	 */
	public int setDisplay(int view, boolean on) {
		if (on)
			display |= view;
		else
			display &= ~view;
		if (mesh != null)
			repaint();
		return display;
	}
	
	/**
	 * return map (-0.5 to 0.5) x position for a screen column
	 */
	public double map_x(int screen_x) {
		double x = (double) screen_x / getWidth();
		double range = x_max - x_min;
		return x_min + (x * range);
	}

	/**
	 * return map (-0.5 to 0.5) y position for a screen row
	 */
	public double map_y(int screen_y) {
		double y = (double) screen_y / getHeight();
		double range = y_max - y_min;
		return y_min + (y * range);
	}
	
	/**
	 * return width (in map units) of a number of pixels
	 */
	public double map_width(int x_pixels) {
		double pixels = x_pixels;
		return (x_max - x_min) * pixels / getWidth();
	}
	
	/**
	 * return height (in map units) of a number of pixels
	 */
	public double map_height(int y_pixels) {
		double pixels = y_pixels;
		return (y_max - y_min) * pixels / getHeight();
	}
	
	/**
	 * return the Cartesian Map column for a Map Y coordinate
	 */
	public int map_col(double x) {
		int cols = getWidth()/TOPO_CELL;	// cols on the map
		double dx = cols * (x - x_min) / (x_max - x_min);
		return (int) dx;
	}

	/**
	 * return the Cartesian Map column for a Map Y coordinate
	 */
	public int map_row(double y) {
		int rows = getHeight()/TOPO_CELL;
		double dy = rows * (y - y_min) / (y_max - y_min);
		return (int) dy;
	}

	/**
	 * return pixel column for a given map x position
	 */
	public int screen_x(double x) {
		double X = getWidth() * (x - x_min)/(x_max - x_min);
		return (int) X;
	}
	
	/**
	 * return pixel row for a given map y position
	 */
	public int screen_y(double y) {
		double Y = getHeight() * (y - y_min)/(y_max - y_min);
		return (int) Y;
	}

	/**
	 * is a map position within the current display window
	 * @param x coordinate (e.g. -0.5 to 0.5)
	 * @param y coordinate (e.g. -0.5 to 0.5)
	 * @return boolean ... are those coordinates in the display window
	 */
	public boolean on_screen(double x, double y) {
		if (x < x_min || x > x_max)
			return false;
		if (y < y_min || y > y_max)
			return false;
		return true;
	}
	
	// description (screen coordinates) of the area to be highlighted
	private int sel_x0, sel_y0, sel_x1, sel_y1;		// line/rectangle ends
	private int x_start, y_start;		// where a drag started
	private int sel_height, sel_width;	// selected rectangle size
	private int sel_radius;				// selected point indicator size
	private boolean[] sel_points;		// which points are in selected group
	
	private Selection sel_type = Selection.NONE;	// type to be rendered
	private boolean selecting = false;	// selection in progress
	private boolean selected = false;	// selection complete
	
	/**
	 * register a listener for selection events
	 * 
	 * @param interested class to receive call-backs
	 */
	public void addMapListener(MapListener interested) {
		listener = interested;
	}

	/**
	 * un-register a selection event listener
	 * 
	 * @param which class to be removed
	 */
	public void removeMapListener(MapListener which) {
		if (listener == which)
			listener = null;
	}
	
	/**
	 * tell map-selection tool what kind of selection we expect
	 * @param type (RECTANGLE, POINT, LINE, ...)
	 */
	public void selectMode(Selection type) {
		if (type == Selection.LINE && sel_type == Selection.RECTANGLE) {
			// rectangles can be converted to lines
			sel_x1 = sel_x0 + sel_width;
			sel_y1 = sel_y0 + sel_height;
			sel_type = Selection.LINE;
			repaint();
		} else if (type == Selection.SQUARE && sel_type == Selection.RECTANGLE) {
			// square the selection
			int side = (sel_width + sel_height)/2;
			sel_width = side;
			sel_height = side;
			sel_x1 = sel_x0 + sel_width;
			sel_y1 = sel_y0 + sel_height;
			sel_type = Selection.SQUARE;
			repaint();
		} else if (type == Selection.POINT && sel_type == Selection.RECTANGLE) {
			// rectangles can also be (crudely) converted to points
			sel_x0 += sel_width/2;
			sel_y0 += sel_height/2;
			sel_type = Selection.POINT;
			repaint();
		} else if (type == Selection.POINTS && sel_type == Selection.RECTANGLE) {
			// rectangles can be converted to point groups
			selectPoints(sel_x0, sel_y0, sel_x0 + sel_width, sel_y0 + sel_height, false);
			repaint();
		} else if (type == Selection.NONE || sel_type != type) {
			// current selection is wrong type, clear it
			selected = false;
			if (sel_type == Selection.POINTS) {
				for(int i = 0; i < sel_points.length; i++)
					sel_points[i] = false;
			}
			sel_type = Selection.NONE;
			repaint();
		}
		sel_mode = type;
	}
	
	/**
	 * see if a selection has already been made and call listener
	 * 
	 * @param type desired type of selection
	 * @return boolean whether or not selection is in place
	 */
	public boolean checkSelection(Selection type) {
		// nothing has been selected
		if (!selected)
			return false;
		
		// there is a selection, but it is inappropriate
		if (type != Selection.ANY && sel_type != type) {
			selected = false;
			sel_type = Selection.NONE;
			repaint();
			return false;
		}
		
		// make the appropriate listener callback
		if (listener != null)
			switch (sel_type) {
			case POINT:
				listener.pointSelected(map_x(sel_x0), map_y(sel_y0));
				break;
			case LINE:
				listener.regionSelected(map_x(sel_x0),  map_y(sel_y0),
					    map_width(sel_x1 - sel_x0), map_height(sel_y1 - sel_y0),
					    true);
				break;
			case SQUARE:
			case RECTANGLE:
				listener.regionSelected(map_x(sel_x0),  map_y(sel_y0),
					    map_width(sel_width), map_height(sel_height),
					    true);
				break;
			case POINTS:
				listener.groupSelected(sel_points, true);
				break;
			default:
				break;
		}
		return true;
	}
	
	/**
	 * mouse click at an on-map location
	 */
	public void mouseClicked(MouseEvent e) {
		if (sel_mode == Selection.ANY || sel_mode == Selection.POINT) { 
			sel_radius = SELECT_RADIUS;
			sel_x0 = e.getX();
			sel_y0 = e.getY();
			selecting = false;
			
			sel_type = Selection.POINT;
			repaint();
			
			if (listener != null && 
				!listener.pointSelected(map_x(sel_x0), map_y(sel_y0)))
					sel_type = Selection.NONE;
		}
	}
	
	/**
	 * start the definition of region selection
	 */
	public void mousePressed(MouseEvent e) {
		if (sel_mode != Selection.NONE) {
			x_start = e.getX();
			y_start = e.getY();
			selecting = true;
			selected = false;
		}
	}
	
	/**
	 * extend/alter the region being selected
	 */
	public void mouseDragged(MouseEvent e) {
		if (!selecting)
			return;
		if (sel_mode == Selection.LINE) {
			selectLine(x_start, y_start, e.getX(), e.getY());
			if (listener != null &&
					!listener.regionSelected(map_x(sel_x0),  map_y(sel_y0),
										    map_width(sel_x1 - sel_x0), 
										    map_height(sel_y1 - sel_y0),
										    selected))
					sel_type = Selection.NONE;
		} else if (sel_mode == Selection.POINTS) {
			selectPoints(x_start, y_start, e.getX(), e.getY(),
					(e.getModifiers() & ActionEvent.CTRL_MASK) == ActionEvent.CTRL_MASK);
			if (listener != null && !listener.groupSelected(sel_points, selected))
				sel_type = Selection.NONE;
		} else if (sel_mode == Selection.SQUARE) {
			// figure out whether selection is taller or wider
			int x = e.getX();
			int dx = (x < x_start) ? x_start - x : x - x_start;
			int y = e.getY();
			int dy = (y < y_start) ? y_start - y : y - y_start;
			// square it
			if (dy > dx)
				x = (x > x_start) ? x_start + dy : x_start - dy;
			else
				y = (y > y_start) ? y_start + dx : y_start - dx;
			selectRect(x_start, y_start, x, y);
			if (listener != null &&
				!listener.regionSelected(map_x(sel_x0),  map_y(sel_y0),
									    map_width(sel_width), map_height(sel_height),
									    selected))
				sel_type = Selection.NONE;
		} else if (sel_mode == Selection.ANY || sel_mode == Selection.RECTANGLE) {
			selectRect(x_start, y_start, e.getX(), e.getY());
			if (listener != null &&
				!listener.regionSelected(map_x(sel_x0),  map_y(sel_y0),
									    map_width(sel_width), map_height(sel_height),
									    selected))
				sel_type = Selection.NONE;
		}
	}
	
	/**
	 * end the definition of a region selection
	 */
	public void mouseReleased(MouseEvent e) {
		if (selecting) {
			selected = true;
			mouseDragged(e);
			selecting = false;
		}
	}
	
	/** (perfunctory) */ public void mouseExited(MouseEvent e) { selecting = false; }
	/** (perfunctory) */ public void mouseEntered(MouseEvent e) {}
	/** (perfunctory) */ public void mouseMoved(MouseEvent e) {}
	
	/**
	 * highlight a line on the displayed map
	 * 
	 * @param x0	screen x
	 * @param y0	screen y
	 * @param x1	screen x
	 * @param y1	screen y
	 */
	public void selectLine(int x0, int y0, int x1, int y1) {
		sel_x0 = x0;
		sel_x1 = x1;
		sel_y0 = y0;
		sel_y1 = y1;
		sel_type = Selection.LINE;
		
		repaint();
	}
	
	/**
	 * highlight a rectangular selection on the displayed map
	 * 
	 * @param x0	screen x
	 * @param y0	screen y
	 * @param x1	screen x
	 * @param y1	screen y
	 */
	public void selectRect(int x0, int y0, int x1, int y1) {
		// normalize boxes defined upwards or to the left
		if (x1 > x0) {
			sel_x0 = x0;
			sel_width = x1 - x0;
		} else {
			sel_x0 = x1;
			sel_width = x0 - x1;
		}
		if (y1 > y0) {
			sel_y0 = y0;
			sel_height = y1 - y0;
		} else {
			sel_y0 = y1;
			sel_height = y0 - y1;
		}
		sel_type = Selection.RECTANGLE;
		repaint();
	}
	
	/**
	 * highlight points in rectangular selection on the displayed map
	 * 
	 * @param x0	screen x
	 * @param y0	screen y
	 * @param x1	screen x
	 * @param y1	screen y
	 * @param add	add these to already selected points
	 */
	public void selectPoints(int x0, int y0, int x1, int y1, boolean add) {
		// make sure we have a point selection map
		if (sel_points == null)
			sel_points = new boolean[mesh.vertices.length];
		
		// normalize boxes defined upwards or to the left
		if (x1 > x0) {
			sel_x0 = x0;
			sel_width = x1 - x0;
		} else {
			sel_x0 = x1;
			sel_width = x0 - x1;
		}
		if (y1 > y0) {
			sel_y0 = y0;
			sel_height = y1 - y0;
		} else {
			sel_y0 = y1;
			sel_height = y0 - y1;
		}
		
		// update selection status for every point in the box
		for(int i = 0; i < sel_points.length; i++)
			if (!add)
				sel_points[i] = inTheBox(mesh.vertices[i].x, mesh.vertices[i].y);
			else if (inTheBox(mesh.vertices[i].x, mesh.vertices[i].y))
				sel_points[i] = true;
		
		sel_type = Selection.POINTS;
		repaint();
	}
	
	/**
	 * return whether or not Map coordinates are within a selected box
	 */
	public boolean inTheBox(double x, double y) {

		if (x < map_x(sel_x0))
			return false;
		if (y < map_y(sel_y0))
			return false;
		if (x >= map_x(sel_x0 + sel_width))
			return false;
		if (y >= map_y(sel_y0 + sel_height))
			return false;
		return true;
	}
	
	/**
	 * highlight points (typically for diagnostic purposes
	 * 
	 * @param point number (-1 = reset)
	 * @param c Color for highlighing
	 */
	public void highlight(int point, Color c) {
		if (point >= 0) {
			highLights[point] = c;
			highlighting = true;
		} else {
			for(int i = 0; i < highLights.length; i++)
				highLights[i] = null;
			highlighting = false;
		}
	}

	/**
	 * highlight non-mesh point
	 *
	 * @param x - map X coordinate
	 * @param y - map Y coordinate
	 * @param c - color for highlighting
	 */
	public void highlight(double x, double y) {
		highlight_x = x;
		highlight_y = y;
		highlighting = true;
	}
	
	/**
	 * find the mesh point closest to a screen location
	 * @param screen_x
	 * @param screen_y
	 * @return nearest MeshPoint
	 */
	public MeshPoint choosePoint(int screen_x, int screen_y) {
		return(mesh.choosePoint(map_x(screen_x), map_y(screen_y)));
	}
	
	/*
	 * change the display window to the specified range
	 * @param x0 new left edge (map coordinate)
	 * @param y0 new upper edge (map coordinate)
	 * @param x1 new right edge (map coordinate)
	 * @param y1 new lower edge (map coordinate)
	 */
	public void setWindow(double x0, double y0, double x1, double y1) {
		x_min = (x1 >= x0) ? x0 : x1;
		y_min = (y1 >= y0) ? y0 : y1;
		x_max = (x1 >= x0) ? x1 : x0;
		y_max = (y1 >= y0) ? y1: y0;
		
		// force already digested maps to be regener
		poly_map = null;
		tileHeight = null;
		tileDepth = null;
		repaint();
		
		if (parms.debug_level >= MAP_DEBUG)
			System.out.println("Display window <" + x_min + ", " + y_min + "> to <" + x_max + ", " + y_max + ">");
	}

	/**
	 * repaint the entire displayed map pane
	 * @param g - Graphics component (pane) for displayed map
	 * 
	 *  Note: order of painting is carefully chosen to enable 
	 *	      layering of some things atop others
	 */
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		int height = getHeight();
		int width = getWidth();
		
		// make sure we have something to display
		if (mesh == null || mesh.vertices.length == 0) {
			setBackground(Color.WHITE);
			g.setColor(Color.BLACK);
			g.drawString("use menu File:New or File:Open to create a mesh", width / 4, height / 2);
			return;
		} else
			setBackground(Color.GRAY);
		
		// make sure the Cartesian translation is up-to-date
		if (poly_map == null ||
			poly_map.height != height/TOPO_CELL || poly_map.width != width/TOPO_CELL) {
			poly_map = new Cartesian(mesh, x_min, y_min, x_max, y_max, 
								width/TOPO_CELL, height/TOPO_CELL, Cartesian.vicinity.POLYGON);
		}
		
		// make sure we have an up-to-date per-tile altitude map
		if (tileHeight == null || 
				tileHeight.length != poly_map.height || tileHeight[0].length != poly_map.width) {
			tileHeight = poly_map.interpolate(heightMap);
			double[][] erosion = poly_map.interpolate(erodeMap);
			for(int i = 0; i < poly_map.height; i++)
				for(int j = 0; j < poly_map.width; j++)
					tileHeight[i][j] -= erosion[i][j];
		}
		
		// make sure we have an up-to-date per-tile depth map
		if (tileDepth == null || 
				tileDepth.length != poly_map.height || tileDepth[0].length != poly_map.width) {
			tileDepth = new double[poly_map.height][poly_map.width];
			for(int i = 0; i < poly_map.height; i++)
				for(int j = 0; j < poly_map.width; j++) {
					double water = poly_map.cells[i][j].nearestValid(waterLevel, WaterFlow.UNKNOWN);
					if (water > tileHeight[i][j])
						tileDepth[i][j] = parms.height(water - tileHeight[i][j]);
				}
		}
		
		// start by rendering backgrounds (rain or altitude)
		if ((display & SHOW_RAIN) != 0) {
				RainMap r = new RainMap(this);
				r.paint(g, width, height, TOPO_CELL);
		} else if ((display & SHOW_TOPO) != 0) {
				AltitudeMap a = new AltitudeMap(this);
				a.paint(g, width, height, TOPO_CELL);
		}
		
		if ((display & SHOW_ERODE) != 0 ) {
			ErodeMap e = new ErodeMap(this);
			e.paint(g, width, height, TOPO_CELL);
		}
		
		// see if we are rendering minerals
		if ((display & SHOW_ROCKS) != 0) {
			SoilMap s = new SoilMap(this);
			s.paint(g, width, height, TOPO_CELL);
		}
		
		// see if we are rendering plant cover
		if ((display & SHOW_FLORA) != 0) {
			FloraMap r = new FloraMap(this);
			r.paint(g, width, height, TOPO_CELL);
		}
		
		// see if we are rendering animal cover
		if ((display & SHOW_FAUNA) != 0) {
			FaunaMap r = new FaunaMap(this, null);
			r.paint(g, width, height, TOPO_CELL);
		}
			
		// see if we are rendering topographic lines
		if ((display & SHOW_TOPO) != 0) {
			TopoMap t = new TopoMap(this);
			t.paint(g, width, height, TOPO_CELL);
		}
		
		// see if we are rendering lakes and rivers
		if ((display & SHOW_WATER) != 0) {
			RiverMap r = new RiverMap(this);
			r.paint(g, width, height);
			WaterMap w = new WaterMap(this);
			w.paint(g, width, height, TOPO_CELL);		
		}
		
		// fish get painted on top of water
		// see if we are rendering animal cover
		if ((display & SHOW_FAUNA) != 0) {
			FaunaMap r = new FaunaMap(this, "Fish");
			r.paint(g, width, height, TOPO_CELL);
		}
		
		// see if we are rendering the mesh (debugging, put it on top)
		if ((display & SHOW_MESH) != 0) {
			g.setColor(MESH_COLOR);
			// for each mesh point
			for(int i = 0; i < mesh.vertices.length; i++) {
				MeshPoint m = mesh.vertices[i];
				// for each neighbor
				for(int j = 0; j < m.neighbors; j++) {
					MeshPoint n = m.neighbor[j];
					if (n.index < i)
						continue;	// we already got this one
					
					// see if it is completely off screen
					if (!on_screen(m.x, m.y) && !on_screen(n.x, n.y))
							continue;
					
					// draw it
					double x1 = screen_x(m.x);
					double y1 = screen_y(m.y);
					double x2 = screen_x(n.x);
					double y2 = screen_y(n.y);
					g.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
				}
			}
		}
		
		// see if we are rendering point indices (debugging, put it on top)
		if ((display & SHOW_POINTS) != 0) {
			// figure out how large our labels will be
			FontMetrics m = g.getFontMetrics();
			int h_offset = m.stringWidth("0000")/2;
			int v_offset = m.getHeight()/2;
			
			// put a label on every point
			g.setColor(POINT_COLOR);
			MeshPoint[] points = mesh.vertices;
			for (int i = 0; i < points.length; i++) {
				MeshPoint p = points[i];
				double x = screen_x(p.x) - SMALL_POINT / 2;
				double y = screen_y(p.y) - SMALL_POINT / 2;
				if (x >= 0 && y >= 0) {
					g.drawString(Integer.toString(p.index), 
								 (int) (x - h_offset), (int) (y + v_offset));
				}
			}
		}
		
		// see if we have points to highlight (debugging, put it on top)
		if (highlighting) {
			for(int i = 0; i < highLights.length; i++)
				if (highLights[i] != null) {
					g.setColor(highLights[i]);
					MeshPoint p = mesh.vertices[i];
					double x = screen_x(p.x) - LARGE_POINT / 2;
					double y = screen_y(p.y) - LARGE_POINT / 2;
					g.drawOval((int) x, (int) y, LARGE_POINT, LARGE_POINT);
				}
				
				if (highlight_x != 0 || highlight_y != 0) {
					g.setColor(SELECT_COLOR);
					double x = screen_x(highlight_x) - LARGE_POINT / 2;
					double y = screen_y(highlight_y) - LARGE_POINT / 2;
					g.drawOval((int) x, (int) y, LARGE_POINT, LARGE_POINT);
				}
		}
	
		// see if we have a selection area to highlight
		switch(sel_type) {
		case LINE:
			g.setColor(SELECT_COLOR);
			g.drawLine(sel_x0,  sel_y0,  sel_x1,  sel_y1);
			break;
		case POINT:
			g.setColor(SELECT_COLOR);
			g.drawOval(sel_x0, sel_y0, sel_radius, sel_radius);
			break;
		case SQUARE:
		case RECTANGLE:
			g.setColor(SELECT_COLOR);
			g.drawRect(sel_x0, sel_y0, sel_width, sel_height);
			break;
		case POINTS:
			g.setColor(SELECT_COLOR);
			for(int i = 0; i < sel_points.length; i++)
				if (sel_points[i])
					g.drawOval(screen_x(mesh.vertices[i].x), 
							   screen_y(mesh.vertices[i].y), 
							   SELECT_RADIUS, SELECT_RADIUS);
		case NONE:
		case ANY:
			break;
		}
	}
	
	/**
	 * linear interpolation of a (color) value within a range
	 * 
	 * @param min value in desired range
	 * @param max value in desired range
	 * @param value (0.0-1.0) to be scaled
	 # @return interpolated value between min and max
	 */
	public static double linear(int min, int max, double value) {
		if (value <= 0)
			return min;
		else if (value >= 1)
			return max;
		
		double ret = value * (max - min);
		return min + ret;
	}
	
	/**
	 * logarithmic interpolation of a (color) value within a range
	 * 
	 * @param min value in desired range
	 * @param max value in desired range
	 * @param value (0.0-1.0) to be scaled
	 * @param base - fraction of value corresponding to half of range
	 # @return interpolated value between min and max
	 */
	public static double logarithmic(int min, int max, double value, double base) {
		if (value <= 0)
			return min;
		else if (value >= 1)
			return max;
		
		double resid = 0.5;
		double ret = 0;
		while (value > 0) {
			if (value > base)
				ret += resid;
			else
				ret += resid * value / base;
			resid /= 2;
			value -= base;
		}
		return min + (ret * (max - min));
	}
	
	/**
	 * return minimum acceptable canvas size
	 */
	public Dimension getMinimumSize() {
		return new Dimension(MIN_WIDTH, MIN_HEIGHT);
	}

	/**
	 * return preferred canvas size
	 */
	public Dimension getPreferredSize() {
		return size;
	}
}
