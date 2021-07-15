package worldBuilder;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.*;

/**
 * the displayable, resizeable, 2D representation of a mesh and
 * the attributes of its points.
 */
public class MapWindow extends JPanel implements MouseListener, MouseMotionListener {
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
	
	public Map map;
	private Mesh mesh;
	private Parameters parms;

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
	
	// display colors
	private static final Color SELECT_COLOR = Color.WHITE;
	private static final Color POINT_COLOR = Color.PINK;
	private static final Color MESH_COLOR = Color.GREEN;
	
	private Color highLights[];		// points to highlight
	private boolean highlighting;	// are there points to highlight
	private double highlight_x, highlight_y;	// non-mesh-point highlighting

	private Cartesian poly_map;		// interpolation based on surrounding polygon
	private double tileHeight[][];	// altitude of each screen tile (Z units)
	private double tileDepth[][];	// depth u/w of each screen tile (meters)
	
	/** selection types: points, line, rectangle, ... */
	public enum Selection {NONE, POINT, POINTS, LINE, RECTANGLE, SQUARE, ANY};
	private Selection sel_mode;	// What types of selection are enabled
	private MapListener listener;	// who to call for selection events
	
	// icons to be placed on may (e.g. for cities)
	public BufferedImage[] iconImages;

	private static final int DISPLAY_DEBUG = 2;
	private static final long serialVersionUID = 1L;

	/**
	 * instantiate a displayable map widget
	 * 
	 * @param width
	 *            ... preferred width (in pixels)
	 * @param height
	 *            ... perferred height 9in pixels)
	 */
	public MapWindow(Map map, int width, int height) {
		this.map = map;
		this.size = new Dimension(width, height);
		this.parms = Parameters.getInstance();
		setWindow(-Parameters.x_extent/2, -Parameters.y_extent/2, Parameters.x_extent/2, Parameters.y_extent/2);
		this.poly_map = null;		// will be created by first paint
		this.mesh = map.mesh;
		if (this.mesh != null)
			this.highLights = new Color[mesh.vertices.length];
		
		this.addMouseMotionListener(this);
		this.addMouseListener(this);
		sel_mode = Selection.ANY;
		
		// load the map icons
		iconImages = new BufferedImage[CityDialog.typeList.length];
		for(int i = 0; i < CityDialog.typeList.length; i++) {
			String filename = CityDialog.typeList[i] + ".bmp";
			try {
				if (filename.charAt(0) == '/') {
					iconImages[i] = ImageIO.read(new File(filename));
				} else {
					InputStream s = getClass().getResourceAsStream(parms.icon_dir + "/" + filename);
					if (s == null)
						throw new IOException("not available to class loader");
					else
						iconImages[i] = ImageIO.read(s);
				}
			} catch (IOException x) {
				System.err.println("unable to open icon image " + filename);
			}
		}
	}
	
	public void newMesh(Mesh mesh) {
		this.mesh = mesh;
		this.poly_map = null;
		this.tileDepth = null;
		this.tileHeight = null;
		if (this.mesh != null)
			this.highLights = new Color[mesh.vertices.length];
	}
	
	public void newHeight() {
		this.tileDepth = null;
		this.tileHeight = null;
	}
	
	public double[][] getTileDepths() { return tileDepth; }
	public double[][] getTileHeights() { return tileHeight; }
	
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
	private int sel_x0, sel_y0, sel_x1, sel_y1;	// line/rectangle ends
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
			// adjust rectangular selection to be square
			sel_type = Selection.SQUARE;
			selectSquare(sel_x0, sel_y0, sel_x0+sel_width, sel_y0+sel_height);
			// selectSquare will repaint
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
			selectSquare(x_start, y_start, e.getX(), e.getY());
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
	 * highlight a (map-units) square selection area on the displayed map
	 * 
	 * @param x0 ... (px) starting x
	 * @param y0 ... (px) starting y
	 * @param x1 ... (px) ending x
	 * @param y1 ... (px) ending y
	 * 
	 * The given coordinates are in pixels (and our map window is not square).
	 * We want the selected area to have the same dimensions as a map window,
	 * so we must correct the slection to have the same aspect ratio as our
	 * (at this moment) map window.
	 */
	public void selectSquare(int x0, int y0, int x1, int y1) {
		// compute the selected area aspect ratio
		double dx = (x1 > x0) ? x1 - x0 : x0 - x1;
		double dy = (y1 > y0) ? y1 - y0 : y0 - y1;
		double aspect_ratio = dx / dy;
		
		// see if it is off-square and correct it
		double square = ((double) getWidth()) / getHeight();
		if (aspect_ratio > square)
			dy = dx / square;		// too short
		else if (aspect_ratio < square)
			dx = dy * square;		// too narrow
		double x = (x1 > x0) ? x0 + dx : x0 - dx;
		double y = (y1 > y0) ? y0 + dy : y0 - dy;
								x0, y0, x1, y1, x0, y0, (int) x, (int) y));
		// and now select the (corrected) rectangle
		selectRect(x0, y0, (int) x, (int) y);
		sel_type = Selection.SQUARE;
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
	 * select/deselect the entire screen
	 * @param on (boolean) select (vs unselect)
	 */
	public void selectAll(boolean on) {
		// make sure we have a point selection map
		if (sel_points == null)
			sel_points = new boolean[mesh.vertices.length];
		
		// set the status of every point
		for(int i = 0; i < sel_points.length; i++)
			sel_points[i] = on;
		sel_type = Selection.POINTS;
		repaint();
		
		// inform the listener of this selection
		if (listener != null && !listener.groupSelected(sel_points, true))
			sel_type = Selection.NONE;
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
		
		if (parms.debug_level >= DISPLAY_DEBUG)
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
			tileHeight = poly_map.interpolate(map.getHeightMap());
			double[][] erosion = poly_map.interpolate(map.getErodeMap());
			for(int i = 0; i < poly_map.height; i++)
				for(int j = 0; j < poly_map.width; j++)
					tileHeight[i][j] -= erosion[i][j];
		}
		
		// make sure we have an up-to-date per-tile depth map
		double waterLevel[] = map.getWaterLevel();
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
				RainMap r = new RainMap(this.map);
				r.paint(g, width, height, TOPO_CELL);
		} else if ((display & SHOW_TOPO) != 0) {
				AltitudeMap a = new AltitudeMap(this.map);
				a.paint(g, width, height, TOPO_CELL);
		}
		
		if ((display & SHOW_ERODE) != 0 ) {
			ErodeMap e = new ErodeMap(this.map);
			e.paint(g, width, height, TOPO_CELL);
		}
		
		// see if we are rendering minerals
		if ((display & SHOW_ROCKS) != 0) {
			SoilMap s = new SoilMap(this.map);
			s.paint(g, width, height, TOPO_CELL);
		}
		
		// see if we are rendering plant cover
		if ((display & SHOW_FLORA) != 0) {
			FloraMap r = new FloraMap(this.map);
			r.paint(g, width, height, TOPO_CELL);
		}
		
		// see if we are rendering animal cover
		if ((display & SHOW_FAUNA) != 0) {
			FaunaMap r = new FaunaMap(this.map, null);
			r.paint(g, width, height, TOPO_CELL);
		}
			
		// see if we are rendering topographic lines
		if ((display & SHOW_TOPO) != 0) {
			TopoMap t = new TopoMap(this.map);
			t.paint(g, width, height, TOPO_CELL);
		}
		
		// see if we are rendering lakes and rivers
		if ((display & SHOW_WATER) != 0) {
			RiverMap r = new RiverMap(this.map);
			r.paint(g, width, height);
			WaterMap w = new WaterMap(this.map);
			w.paint(g, width, height, TOPO_CELL);		
		}
		
		// fish get painted on top of water
		// see if we are rendering animal cover
		if ((display & SHOW_FAUNA) != 0) {
			FaunaMap r = new FaunaMap(this.map, "Fish");
			r.paint(g, width, height, TOPO_CELL);
		}
		
		// add capital/city/town/village icons and trade routes
		if ((display & SHOW_CITY) != 0) {
			CityMap c = new CityMap(this.map);
			c.paint(g, width, height);
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
