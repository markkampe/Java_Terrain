package worldBuilder;
import java.awt.Color;
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

/**
 * the displayable, resizeable, 2D representation of a mesh and
 * the attributes of its points.
 */
public class Map {
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
	
	// other limits
	private static final int MAX_STEPS = 200;	// max steps per route
	
	/** current map is a subRegion of a larger map */
	public boolean isSubRegion;
	/** underlying Mesh of Voronoi points */
	public Mesh mesh;
	public MapWindow window;
	
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
	private double heightMap[]; // Height of each mesh point (z)
	private double soilMap[];	// Soil type of each mesh point
	private double rainMap[];	// Rainfall of each mesh point (cm/y)
	private double fluxMap[];	// Water flow through each point (m^3/s)
	private double erodeMap[];	// erosion/deposition
	private double incoming[];	// incoming water from off-map (m^3/s)
	private double suspMap[];	// incoming sediment from off-map (m^3/s)
	private double e_factors[];	// exaggerated per point erosion
	private double s_factors[];	// exaggerated per point sedimentation
	private double floraMap[];	// assigned flora type
	private double faunaMap[];	// assigned fauna type
	private double waterLevel[];// level of nearest water body
	private LinkedList<TradeRoute> trade_routes;
	
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

	/**
	 * instantiate a map widget (with its many attributes)
	 * 
	 * @param width
	 *            ... preferred width (in pixels)
	 * @param height
	 *            ... preferred height 9in pixels)
	 */
	public Map(int width, int height) {
		this.parms = Parameters.getInstance();
		this.isSubRegion = false;
		this.window = new MapWindow(this, width, height);
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
		boolean inRoutes = false;	// in trade routes
		boolean inSteps = false;	// in trade route steps
		int length = 0;				// expected number of points
		int points = 0;				// number of points read	
		
		// per-point parameters we are looking for
		double x = 0, y = 0, z = 0;
		double rain = 0;
		int soil = 0;
		int flora = 0;
		int fauna = 0;
		double influx = 0;
		double suspended = 0;
		double erosion = 0;
		double e_factor = 1.0;
		double s_factor = 1.0;
		isSubRegion = false;
		String name = null;
		double route_cost = 0;
		int num_steps = 0;
		int[] route_steps = new int[MAX_STEPS];
		
		while(parser.hasNext()) {
			JsonParser.Event e = parser.next();
			switch(e) {		
			case KEY_NAME:
				thisKey = parser.getString();
				if (length > 0) {
					if (thisKey.equals("points")) {
						inPoints = true;
						inPoIs = false;
						inRoutes = false;
						inSteps = false;
						points = 0;
					} else if (thisKey.equals("pois")) {
						inPoIs = true;
						inPoints = false;
						inRoutes = false;
						inSteps = false;
					} else if (thisKey.equals("mesh")) {
						inPoints = false;
						inPoIs = false;
						inRoutes = false;
						inSteps = false;
					} else if (thisKey.equals("routes")) {
						inPoints = false;
						inPoIs = false;
						inRoutes = true;
						inSteps = false;
					} else if (thisKey.equals("steps")) {
						inPoints = false;
						inPoIs = false;
						inRoutes = true;
						inSteps = true;
						num_steps = 0;
					}
				}
				break;

			case VALUE_STRING:
			case VALUE_NUMBER:
				if (inSteps) {
					route_steps[num_steps++] = new Integer(parser.getString());
					break;
				}
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
						
					case "erosion":
						erosion = new Double(parser.getString());
						break;
						
					case "e_factor":
						e_factor = new Double(parser.getString());
						break;
						
					case "s_factor":
						s_factor = new Double(parser.getString());
						break;
						
					case "suspended":
						s = parser.getString();
						u = s.indexOf(Parameters.unit_f);
						if (u != -1)
							s = s.substring(0, u);
						suspended = new Double(s);
						break;
						
					case "name":
						name = parser.getString();
						break;
					
					// route attributes
					case "cost":
						route_cost = new Double(parser.getString());
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
					ignore(x, y);		// not currently used
					soilMap[points] = soil;
					floraMap[points] = flora;
					faunaMap[points] = fauna;
					rainMap[points] = rain;
					incoming[points] = influx;
					suspMap[points] = suspended;
					erodeMap[points] = erosion;
					e_factors[points] = e_factor;
					s_factors[points] = s_factor;
					nameMap[points] = name;
					points++;
				} else if (inRoutes) {
					TradeRoute route = new TradeRoute(route_steps, num_steps, route_cost);
					if (trade_routes == null)
						trade_routes = new LinkedList<TradeRoute>();
					trade_routes.add(route);
				}
				break;
				
			case START_ARRAY:
				break;
				
			case END_ARRAY:
				if (inPoints)
					inPoints = false;
				else if (inPoIs)
					inPoIs = false;
				else if (inSteps)
					inSteps = false;
				else if (inRoutes)
					inRoutes = false;
				break;
				
			case START_OBJECT:
				if (inPoints) {
					influx = 0;
					suspended = 0;
					erosion = 0;
					e_factor = 1.0;
					s_factor = 1.0;
					soil = 0;
					flora = 0;
					fauna = 0;
					rain = 0;
					z = 0.0;
					name = null;
				} else if (inRoutes) {
					route_cost = 0;
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
			final String C_FORMAT = "        { \"cost\": %.1f,\n";
			final String R_FORMAT = "          \"steps\": [ ";
			final int STEPS_PER_LINE = 12;
			
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
			
			// write out the trade routes
			if (trade_routes != null && trade_routes.size() > 0) {
				output.write( "    \"routes\": [" );
				int numRoutes = 0;
				for(Iterator<TradeRoute> it = trade_routes.iterator(); it.hasNext(); ) {
					TradeRoute r = it.next();
					output.write((numRoutes++ == 0) ? "\n" : ",\n");
					output.write(String.format(C_FORMAT, r.cost));
					output.write(String.format(R_FORMAT));
					for(int i = 0; i < r.path.length; i++) {
						if (i > 0)
							if (i%STEPS_PER_LINE == 0)
								output.write(",\n                     ");
							else
								output.write(", ");
						output.write(String.format("%d", r.path[i]));
					}
					output.write( " ] }");
				}
				output.write("\n    ],\n");
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
		if (erodeMap[x] != 0)
			output.write(String.format(", \"erosion\": %.9f", erodeMap[x]));
		if (rainMap[x] != 0)
			output.write(String.format(", \"rain\": \"%.1f%s\"", rainMap[x], Parameters.unit_r));
		if (soilMap[x] != 0)
			output.write(String.format(", \"soil\": \"%s\"", rockNames[(int) Math.round(soilMap[x])]));
		if (floraMap[x] != 0)
			output.write(String.format(", \"flora\": \"%s\"", floraNames[(int) Math.round(floraMap[x])]));
		if (faunaMap[x] != 0)
			output.write(String.format(", \"fauna\": \"%s\"", faunaNames[(int) Math.round(faunaMap[x])]));
		if (incoming[x] != 0)
			output.write(String.format(", \"influx\": \"%.5f%s\"", incoming[x], Parameters.unit_f));
		if (suspMap[x] != 0)
			output.write(String.format(", \"suspended\": \"%.5f%s\"", suspMap[x], Parameters.unit_f));
		if (e_factors[x] != 1.0)
			output.write(String.format(", \"e_factor\": \"%.3f\"",  e_factors[x]));
		if (s_factors[x] != 1.0)
			output.write(String.format(", \"s_factor\": \"%.3f\"",  s_factors[x]));
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
			this.e_factors = new double[mesh.vertices.length];
			this.s_factors = new double[mesh.vertices.length];
			
			this.incoming = new double[mesh.vertices.length];
			this.suspMap = new double[mesh.vertices.length];
			
			this.nameMap = new String[mesh.vertices.length];
			this.drainage = new Drainage(this);
			this.waterflow = new WaterFlow(this);
			this.trade_routes = new LinkedList<TradeRoute>();
		} else {
			this.heightMap = null;
			this.rainMap = null;
			this.fluxMap = null;
			this.erodeMap = null;
			this.e_factors = null;
			this.s_factors = null;
			this.soilMap = null;
			this.waterLevel = null;
			this.floraMap = null;
			this.faunaMap = null;
			this.incoming = null;
			this.suspMap = null;
			this.nameMap = null;
			this.drainage = null;
			this.waterflow = null;
			this.trade_routes = null;
		}
		window.newMesh(mesh);
		window.repaint();
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
		window.newHeight();
		window.repaint();
		
		return old;
	}
	
	public void setErodeMap(double[] newErode) {
		erodeMap = newErode;
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
		window.newHeight();
		window.repaint();

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
		window.newHeight();
		window.repaint();
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
		window.newHeight();
		window.repaint(); 
	}
	
	/**
	 * return incoming off-map rivers
	 */
	public double[] getIncoming() { return incoming; }
	
	/**
	 * update the suspended sediment after a change to the incoming arterial river
	 * @param new_map new incoming flux per point
	 */
	public void setSusp(double[] new_map) {
		suspMap = new_map;
		waterflow.recompute();
		window.newHeight();
		window.repaint(); 
	}
	
	/**
	 * return incoming silt from off-map rivers
	 */
	public double[] getSusp() { return suspMap; }

	
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
		window.repaint();
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
		window.repaint();
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
		window.repaint();
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
	 * return a descriptive name string for a chosen point
	 */
	public String pointName(int point) {
		if (point < 0)
			return("Ocean");
		String s = nameMap[point];
		if (s == null) {
			double x = mesh.vertices[point].x;
			double y = mesh.vertices[point].y;
			return(String.format("<%.6f,%.6f>", parms.latitude(x), parms.longitude(y)));
		}
		return(CityDialog.lexName(s));
		
	}
	
	/**
	 * return list of trade routes
	 */
	public LinkedList<TradeRoute> tradeRoutes() {
		return trade_routes;
	}
	
	/**
	 * set the list of trade routes
	 */
	public void tradeRoutes(LinkedList<TradeRoute> routes) {
		trade_routes = routes;
	}
	
	/**
	 * assoicate a name with the MeshPoint nearest <x,y>
	 * @param name new name string
	 * @param x (map) x coordinate
	 * @param y (map) y coordinate
	 */
	public void addName(String name, double x, double y) {
		MeshPoint p = mesh.choosePoint(x, y);
		nameMap[p.index] = name;
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
	public double[] getE_factors() {return e_factors;}
	public double[] getS_factors() { return s_factors;}
	
	/**
	 * update the erosion factors
	 */
	public double[] setE_factors(double[] factors) {
		double[] prev = e_factors;
		e_factors = factors;
		waterflow.recompute();
		window.newHeight();
		window.repaint(); 
		return prev;
	}
	
	/**
	 * update the sedimentation factors
	 */
	public double[] setS_factors(double[] factors) {
		double[] prev = s_factors;
		s_factors = factors;
		waterflow.recompute();
		window.newHeight();
		window.repaint(); 
		return prev;
	}
	
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
	
	/* turn-off unused variable warnings */
	private void ignore(double x, double y) {}
}
