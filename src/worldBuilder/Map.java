package worldBuilder;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.json.Json;
import javax.json.stream.JsonParser;
import javax.swing.*;

public class Map extends JPanel {
	/**
	 * a Map is the displayable resizeable form of a Mesh.
	 * 
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

	// types of displays
	public static final int SHOW_ALL = 0xff;
	public static final int SHOW_POINTS = 0x01;
	public static final int SHOW_MESH = 0x02;
	public static final int SHOW_TOPO = 0x04;
	public static final int SHOW_RAIN = 0x08;
	public static final int SHOW_WATER = 0x10;
	public static final int SHOW_ERODE = 0x20;
	public static final int SHOW_SOIL = 0x40;
	private int display;		// bitmask for enabled SHOWs
	
	// soil types
	public static final int SEDIMENTARY = 0;
	public static final int METAMORPHIC = 1;
	public static final int IGNEOUS = 2;
	public static final int ALLUVIAL = 3;
	public static final String soil_names[] = {
			"Sedimentary", "Metamorphic", "Igneous", "Alluvial"
	};
	
	// map size (in pixels)
	private static final int MIN_WIDTH = 400;	// min screen width
	private static final int MIN_HEIGHT = 400;	// min screen height
	private static final int SMALL_POINT = 2;	// width of a small point
	private static final int LARGE_POINT = 4;	// width of a large point
	private static final int TOPO_CELL = 5;		// pixels/topographic cell
												// CODE DEPENDS ON THIS CONSTANT
	private Dimension size;
	
	// displayed window offset and size
	double x_min, y_min, x_max, y_max;
	
	public boolean isSubRegion;		// sub-region of a larger map

	// display colors
	private static final Color SELECT_COLOR = Color.WHITE;
	private static final Color POINT_COLOR = Color.PINK;
	private static final Color MESH_COLOR = Color.GREEN;
	
	private Color highLights[];		// points to highlight
	private boolean highlighting;	// are there points to highlight
	
	// the interesting data
	private Mesh mesh;			// mesh of Voronoi points
	private Hydrology hydro;	// hydrology calculator
	
	// per MeshPoint information
	private double heightMap[]; // Height of each mesh point
	private double soilMap[];	// Soil type of each mesh point
	private double rainMap[];	// Rainfall of each mesh point
	private double fluxMap[];	// Water flow through each point
	private double erodeMap[];	// erosion/deposition
	private double hydrationMap[];	// soil hydration
	private double incoming[];	// incoming water from off-map
	private int downHill[];		// down-hill neighbor
	private Cartesian map;		// Cartesian translation of Voronoi Mesh
	private int erosion;		// number of erosion cycles
	
	// hydrological results
	public double max_slope;		// maximum slope
	public double max_flux;			// maximum river flow
	public double max_velocity;		// maximum water velocity
	public double max_erosion;		// maximum soil loss due to erosion
	public double max_deposition;	// maximum soil gain due to sedimentation

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
	 * @param background
	 *            ... background color
	 */
	public Map(int width, int height) {
		this.size = new Dimension(width, height);
		this.parms = Parameters.getInstance();
		this.isSubRegion = false;
		setWindow(-Parameters.x_extent/2, -Parameters.y_extent/2, Parameters.x_extent/2, Parameters.y_extent/2);
		selectNone();
	}
	
	/**
	 * create a new sub-region map
	 * 
	 * @param points ... number of desired points in new Mesh
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
		double[] r = new double[newlen];	// rain map
		double[] s = new double[newlen];	// soil map
		double[] f = new double[newlen];	// incoming map

		// interpolate per-point attributes for each mesh point
		for(int i = 0; i < newlen; i++) {
			// find the corresponding previous-map coordinates
			double x = (newMesh.vertices[i].x/xScale) + xShift;
			double y = (newMesh.vertices[i].y/yScale) + yShift;
			
			// find nearest points from the previous map
			MeshPoint p = new MeshPoint(x,y);
			Vicinity nearest = new Vicinity();
			for(int j = 0; j < mesh.vertices.length; j++) {
				nearest.consider(j, p.distance(mesh.vertices[j]));
			}

			// interpolate values for height, rain, and soil
			h[i] = nearest.interpolate(heightMap);
			r[i] = nearest.interpolate(rainMap);
			s[i] = nearest.interpolate(soilMap);
		}
		
		// FIX sometimes arteries enter but don't flow
		// reproduce all water flows into the new sub-region
		for (int i = 0; i < mesh.vertices.length; i++) {
			MeshPoint p = mesh.vertices[i];
			if (inTheBox(p.x, p.y)) {
				for (int j = 0; j < mesh.vertices.length; j++) {
					MeshPoint p2 = mesh.vertices[j];
					if (downHill[j] == i && !inTheBox(p2.x, p2.y)) {
						// we found a point of water entry into the box
						// find the closest point (in new mesh) to the source
						double x = (p2.x - xShift) * xScale;
						double y = (p2.y - yShift) * yScale;
						MeshPoint p3 = newMesh.choosePoint(x,y);
						f[p3.index] += fluxMap[p2.index];
						p3.immutable = true;
					}
				}
			}
		}
		
		// install and attribute the new mesh
		setMesh(newMesh);
		rainMap = r;
		soilMap = s;
		incoming = f;
		setHeightMap(h);	// force the hydrology recalculation
	}
	
	
	
	/**
	 * read saved map from a file
	 * 
	 * @param name of input file
	 */
	public void read(String filename) {
		// start by reading in the underlying mesh
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
		int length = 0;				// expected number of points
		int points = 0;				// number of points read	
		
		// per-point parameters we are looking for
		double z = 0;
		double rain = 0;
		int soil = 0;
		double influx = 0;
		isSubRegion = false;
		
		while(parser.hasNext()) {
			JsonParser.Event e = parser.next();
			switch(e) {		
			case KEY_NAME:
				thisKey = parser.getString();
				if (length > 0) {
					if (thisKey.equals("points")) {
						inPoints = true;
						points = 0;
					} else if (thisKey.equals("mesh"))
						inPoints = false;
				}
				break;
				

			case VALUE_STRING:
			case VALUE_NUMBER:
				switch(thisKey) {
					case "length":
						length = new Integer(parser.getString());
						break;

					// per point attributes
					case "z":
						z = new Double(parser.getString());
						break;
						
					case "soil":
						String s = parser.getString();
						for(int i = 0; i < Map.soil_names.length; i++)
							if (s.equals(Map.soil_names[i])) {
								soil = i;
								break;
							}
						break;
						
					case "rain":
						s = parser.getString();
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
						
					case "cloudbase":
						s = parser.getString();
						u = s.indexOf(Parameters.unit_z);
						if (u != -1)
							s = s.substring(0, u);
						parms.dRainHeight = new Integer(s);
						break;
						
					case "erosion":
						parms.dErosion = new Integer(parser.getString());
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
					rainMap[points] = rain;
					incoming[points] = influx;
					points++;
				}
				break;
				
			case START_ARRAY:
				break;
				
			case END_ARRAY:
				if (inPoints)
					inPoints = false;
				break;
				
			case START_OBJECT:
				if (inPoints) {
					influx = 0;
					soil = 0;
					rain = 0;
					z = 0.0;
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
		
		RainDialog.rainFall(this, parms.dDirection, parms.dAmount);
		
		if (parms.debug_level > 0) {
			parms.worldParms();
			parms.rainParms(parms.dAmount);
		}
	}
	
	/**
	 * write a mesh of MapPoints out to a file
	 */
	public boolean write(String filename) {
		try {
			FileWriter output = new FileWriter(filename);
			final String T_FORMAT = "    \"subregion\": true,\n";
			final String S_FORMAT = "    \"sealevel\": \"%d%s\",\n";
			final String R_FORMAT = "    \"rainfall\": { \"amount\": \"%d%s\", \"direction\": %d, \"cloudbase\": \"%d%s\" },\n";
			final String E_FORMAT = "    \"erosion\": %d,\n";
			final String L_FORMAT = "    \"center\": { \"latitude\": \"%.6f\", \"longitude\": \"%.6f\" },\n";
			final String W_FORMAT = "    \"world\": { \"radius\": \"%d%s\", \"tilt\": \"%.1f\" },\n";
			final String Z_FORMAT = "    \"scale\": { \"xy_range\": \"%d%s\", \"z_range\": \"%d%s\" },\n";
			final String M_format = "    \"map_name\": \"%s\",\n";
			final String P_format = "    \"parent_name\": \"%s\",\n";
			final String D_format = "    \"description\": \"%s\",\n";
			
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
			output.write(String.format(R_FORMAT, parms.dAmount, Parameters.unit_r, 
					parms.dDirection, parms.dRainHeight, Parameters.unit_z));
			output.write(String.format(E_FORMAT, parms.dErosion));
			
			if (parms.description != "")
				output.write(String.format(D_format,  parms.description));
			
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
			output.write(String.format(", \"soil\": \"%s\"", Map.soil_names[(int) Math.round(soilMap[x])]));
		if (incoming[x] != 0)
			output.write(String.format(", \"influx\": \"%.2f%s\"", incoming[x], Parameters.unit_f));
		output.write(" }");
	}
	
	/**
	 * get/set routines for Mesh and per MeshPoint attributes
	 */
	public Mesh getMesh() { return mesh; }
	public void setMesh(Mesh mesh) {
		this.mesh = mesh;	
		if (mesh != null) {
			this.map = new Cartesian(mesh, x_min, y_min, x_max, y_max, getWidth()/TOPO_CELL, getHeight()/TOPO_CELL);
			this.heightMap = new double[mesh.vertices.length];
			this.rainMap = new double[mesh.vertices.length];
			this.downHill = new int[mesh.vertices.length];
			this.fluxMap = new double[mesh.vertices.length];
			this.erodeMap = new double[mesh.vertices.length];
			this.soilMap = new double[mesh.vertices.length];
			this.hydrationMap = new double[mesh.vertices.length];
			this.highLights = new Color[mesh.vertices.length];
			this.incoming = new double[mesh.vertices.length];
			this.hydro = new Hydrology(this);
			this.erosion = 1; // TODO s.b parms.dErosion;
			hydro.reCalculate(true);
		} else {
			this.map = null;
			this.heightMap = null;
			this.rainMap = null;
			this.downHill = null;
			this.fluxMap = null;
			this.erodeMap = null;
			this.soilMap = null;
			this.hydrationMap = null;
			this.incoming = null;
			this.highLights = null;
			this.hydro = null;
		}
		
		repaint();
	}
	
	/* Z value (pre-erosion) for each Mesh point */
	public double[] getHeightMap() {return heightMap;}
	public double[] setHeightMap(double newHeight[]) {
		double old[] = heightMap; 
		heightMap = newHeight; 
		for(int i = 0; i < erosion; i++)
			hydro.reCalculate(i == 0);
		repaint();
		return old;
	}

	/* rainfall (in cm) for each Mesh point */
	public double[] getRainMap() {return rainMap;}
	public double[] setRainMap(double newRain[]) {
		double old[] = rainMap; 
		rainMap = newRain; 
		for(int i = 0; i < erosion; i++)
			hydro.reCalculate(i == 0);
		repaint();
		return old;
	}
	
	/* Number of configured erosion cycles	*/
	public int getErosion() {return erosion;}
	public int setErosion(int cycles) {
		int old = erosion;
		erosion = cycles;
		for(int i = 0; i < erosion; i++)
			hydro.reCalculate(i == 0);
		repaint();
		return old;
	}
	
	/* incoming arterial river	*/
	public double[] getIncoming() { return incoming; }
	public void setIncoming() {repaint(); }
	
	/* soil type for each Mesh point	*/
	public double [] getSoilMap() {return soilMap;}
	public double[] setSoilMap(double[] newSoil) {
		double[] old = soilMap;
		soilMap = newSoil;
		repaint();
		return old;
	}
	
	/*
	 * these arrays are regularly re-calculated from height/rain
	 * and so do not need to be explicitly SET
	 */
	public int[] getDownHill() { return downHill; }
	public double[] getFluxMap() {return fluxMap;}
	public double[] getErodeMap() {return erodeMap;}
	public double [] getHydrationMap() { return hydrationMap; }
	
	/* Meshpoint to Cartesian translation matrix */
	public Cartesian getCartesian() {return map;}
	
	/**
	 * enable/disable display elements
	 * 
	 * @param view to be enabled/disabled
	 * @param on/off
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
	 * convert a screen coordinate into a map coordinate
	 */
	public double map_x(int screen_x) {
		double x = (double) screen_x / getWidth();
		double range = x_max - x_min;
		return x_min + (x * range);
	}

	public double map_y(int screen_y) {
		double y = (double) screen_y / getHeight();
		double range = y_max - y_min;
		return y_min + (y * range);
	}
	
	public double map_width(int x_pixels) {
		double pixels = x_pixels;
		return pixels/getWidth();
	}
	
	public double map_height(int y_pixels) {
		double pixels = y_pixels;
		return pixels/getHeight();
	}
	
	/**
	 * convert a map coordinate into a screen coordinate
	 */
	public int screen_x(double x) {
		double X = getWidth() * (x - x_min)/(x_max - x_min);
		return (int) X;
	}
	
	public int screen_y(double y) {
		double Y = getHeight() * (y - y_min)/(y_max - y_min);
		return (int) Y;
	}

	/**
	 * is a screen coordinate within the current display window
	 */
	public boolean on_screen(double x, double y) {
		if (x < x_min || x > x_max)
			return false;
		if (y < y_min || y > y_max)
			return false;
		return true;
	}
	
	// description (screen coordinates) of the area to be highlighted
	private int sel_x0, sel_y0, sel_x1, sel_y1;
	private int sel_height, sel_width;
	private int sel_radius;
	private enum Selection {NONE, CIRCLE, LINE, RECTANGLE};
	private Selection sel_type;
	
	/**
	 * highlight a rectangular selection
	 * 
	 * @param x0	screen x
	 * @param y0	screen y
	 * @param x1	screen x
	 * @param y1	screen y
	 */
	public void selectLine(int x0, int y0, int x1, int y1) {
		sel_x0 = x0;
		sel_y0 = y0;
		sel_x1 = x1;
		sel_y1 = y1;
		sel_type = Selection.LINE;
		
		repaint();
	}
	
	/**
	 * highlight a rectangular selection
	 * 
	 * @param x0	screen x
	 * @param y0	screen y
	 * @param x1	screen x
	 * @param y1	screen y
	 */
	public void selectRect(int x0, int y0, int width, int height) {
		// normalize boxes defined upwards or to the left
		if (width > 0) {
			sel_x0 = x0;
			sel_width = width;
		} else {
			sel_x0 = x0 + width;
			sel_width = -width;
		}
		if (height > 0) {
			sel_y0 = y0;
			sel_height = height;
		} else {
			sel_y0 = y0 + height;
			sel_height = -height;
		}
		sel_type = Selection.RECTANGLE;
		repaint();
	}
	
	/**
	 * determine whether or not a Map point is within a selected box
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
	 * highlight a circular selection
	 * 
	 * @param x		screen x
	 * @param y		screen y
	 * @param radius	screen radius
	 */
	public void selectCircle(int x, int y, int radius) {
		sel_x0 = x;
		sel_y0 = y;
		sel_radius = radius;
		sel_type = Selection.CIRCLE;
		
		repaint();
	}
	
	/**
	 * clear selection highlight
	 */
	public void selectNone() {
		sel_type = Selection.NONE;
		repaint();
	}
	
	/**
	 * highlight points (typically for diagnostic purposes
	 * 
	 * @param point number (-1 = reset)
	 * @param color
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
	 * @param map x/y upper left, lower right
	 */
	public void setWindow(double x0, double y0, double x1, double y1) {
		x_min = (x1 >= x0) ? x0 : x1;
		y_min = (y1 >= y0) ? y0 : y1;
		x_max = (x1 >= x0) ? x1 : x0;
		y_max = (y1 >= y0) ? y1: y0;
		map = new Cartesian(mesh, x_min, y_min, x_max, y_max, getWidth()/TOPO_CELL, getHeight()/TOPO_CELL);
		repaint();
		
		if (parms.debug_level >= MAP_DEBUG)
			System.out.println("Display window <" + x_min + ", " + y_min + "> to <" + x_max + ", " + y_max + ">");
	}

	/**
	 * repaint the map pane
	 * 
	 *  Note: order of painting is to enable layering of some things
	 *  	  atop others
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
		if (map.height != height/TOPO_CELL || map.width != width/TOPO_CELL)
			map = new Cartesian(mesh, x_min, y_min, x_max, y_max, width/TOPO_CELL, height/TOPO_CELL);
		
		// make sure all the rainfall/river/erosion data is up-to-date
		for(int i = 0; i < erosion; i++)
			hydro.reCalculate(i == 0);
		
		// start by rendering backgrounds (rain, altitude, erosion, and water bodies)
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
		
		// see if we are rendering points
		if ((display & SHOW_POINTS) != 0) {
			g.setColor(POINT_COLOR);
			MeshPoint[] points = mesh.vertices;
			for (int i = 0; i < points.length; i++) {
				MeshPoint p = points[i];
				double x = screen_x(p.x) - SMALL_POINT / 2;
				double y = screen_y(p.y) - SMALL_POINT / 2;
				if (x >= 0 && y >= 0)
					g.drawOval((int) x, (int) y, SMALL_POINT, SMALL_POINT);
			}
		}

		// see if we are rendering the mesh
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
		
		// see if we are rendering topographic lines
		if ((display & SHOW_TOPO) != 0) {
			TopoMap t = new TopoMap(this);
			t.paint(g, width, height, TOPO_CELL);
		}
		
		// see if we are rendering soil/hydration
		if ((display & SHOW_SOIL) != 0) {
			SoilMap s = new SoilMap(this);
			s.paint(g, width, height, TOPO_CELL);
		}
		
		// see if we are rendering rivers
		if ((display & SHOW_WATER) != 0) {
			RiverMap r = new RiverMap(this);
			r.paint(g, width, height);
			WaterMap w = new WaterMap(this);
			w.paint(g, width, height, TOPO_CELL);		
		}
		
		// see if we have points to highlight
		if (highlighting)
			for(int i = 0; i < highLights.length; i++)
				if (highLights[i] != null) {
					g.setColor(highLights[i]);
					MeshPoint p = mesh.vertices[i];
					double x = screen_x(p.x) - LARGE_POINT / 2;
					double y = screen_y(p.y) - LARGE_POINT / 2;
					g.drawOval((int) x, (int) y, LARGE_POINT, LARGE_POINT);
				}
	
		// see if we have a selection area to highlight
		switch(sel_type) {
		case LINE:
			g.setColor(SELECT_COLOR);
			g.drawLine(sel_x0,  sel_y0,  sel_x1,  sel_y1);
			break;
		case CIRCLE:
			g.setColor(SELECT_COLOR);
			g.drawOval(sel_x0, sel_y0, sel_radius, sel_radius);
			break;
		case RECTANGLE:
			g.setColor(SELECT_COLOR);
			g.drawRect(sel_x0, sel_y0, sel_width, sel_height);
			break;
		case NONE:
			break;
		}
	}
	
	/**
	 * linear interpolation of a (color) value within a range
	 * 
	 * @param min return value
	 * @param max return value
	 * @param value (0-1) to be scaled
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
	 * @param min
	 *            return value
	 * @param max
	 *            return value
	 * @param value
	 *            (0-1) to be scaled
	 * @param base
	 *            (result/2 for each increment)
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
	 * @return minimum acceptable canvas size
	 */
	public Dimension getMinimumSize() {
		return new Dimension(MIN_WIDTH, MIN_HEIGHT);
	}

	/**
	 * @return preferred canvas size
	 */
	public Dimension getPreferredSize() {
		return size;
	}
}
