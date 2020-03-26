package worldBuilder;

import java.awt.*;
import java.awt.event.*;
import java.util.Hashtable;

import javax.swing.*;
import javax.swing.event.*;

/**
 * a super-class for exporter dialogs - creates dialogs, handles
 * handles region selection, pumps data to an Exporter
 */ 
/* It is expected that the sub-classes will process the button
 * pushes and window events, as well as perhaps adding additional
 * control widgets to the CENTER controls JPanel.
 */
public class ExportBase extends JFrame implements WindowListener, MapListener {	
	protected Map map;				// map from which we export
	protected Parameters parms;
	// private String format;			// selected output format

	// standard (to all subclasses) widgets
	protected JTextField sel_name;	// name of region
	protected JButton accept;
	protected JButton cancel;
	protected JButton previewT;
	protected JButton previewF;
	protected JPanel controls;
	
	private JLabel sel_center;		// lat/lon of center
	private JLabel sel_km;			// size of region (in km)
	private JTextField sel_t_size;	// size of tile (in m)
	private JLabel sel_points;		// number of tiles in region
	private JSlider resolution;		// tile size @return selection
	
	// variables describing the region selection process
	protected boolean selected;			// selection completed
	protected boolean newSelection;		// selected area has changed
	protected int x_points, y_points;	// selection width/height (in tiles)
	private double box_x, box_y;		// selection box map coordinates
	private double box_width, box_height;	// selection box size (map units)
	private double x_km, y_km;			// selection box size (in km)
	
	// parameters for the tile size selection slider
	private static final int MAX_TILE_SIZE = 10000;
	private static final int TICS_PER_DECADE = 9;
	
	private static final int EXPORT_DEBUG = 2;
	
	private static final long serialVersionUID = 1L;
	
	/**
	 * common subset of exports
	 * 
	 * @param format ... perhaps not needed
	 * @param map ... the map from which we are exporting
	 */
	public ExportBase(String format, Map map)  {
		// pick up references
		this.map = map;
		// this.format = format;
		this.parms = Parameters.getInstance();;
		
		// create the dialog box
		int border = parms.dialogBorder;
		Container mainPane = getContentPane();
		((JComponent) mainPane).setBorder(BorderFactory.createMatteBorder(border, border, border, border, Color.LIGHT_GRAY));
		setTitle("Export - " + format);
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		
		// create the basic widgets
		Font fontSmall = new Font("Serif", Font.ITALIC, 10);
		Font fontLarge = new Font("Serif", Font.ITALIC, 15);
		sel_name = new JTextField();
		sel_name.setText(parms.map_name);
		JLabel nameLabel = new JLabel("Name of this region", JLabel.CENTER);
		nameLabel.setFont(fontLarge);
		
		accept = new JButton("EXPORT");
		cancel = new JButton("CANCEL");
		previewT = new JButton("PREVIEW TOPO");
		previewF = new JButton("PREVIEW FLORA");
	
		// the tile size slider is pretty tricky
		resolution = new JSlider(JSlider.HORIZONTAL, 0, meters_to_slider(MAX_TILE_SIZE), 
				meters_to_slider(parms.dTileSize));
		resolution.setMajorTickSpacing(TICS_PER_DECADE);
		// resolution.setMinorTickSpacing(TICS_Paxis/inclination ER_DECADE/2);
		resolution.setFont(fontSmall);
		resolution.setPaintTicks(true);
		Hashtable<Integer, JLabel> labels = new Hashtable<Integer, JLabel>();
		labels.put(meters_to_slider(1), new JLabel("1"));
		labels.put(meters_to_slider(10), new JLabel("10"));
		labels.put(meters_to_slider(100), new JLabel("100"));
		labels.put(meters_to_slider(1000), new JLabel("1km"));
		labels.put(meters_to_slider(10000), new JLabel("10km"));
		resolution.setLabelTable(labels);
		resolution.setPaintLabels(true);
		JLabel resolutionLabel = new JLabel("Tile size(m)", JLabel.CENTER);
		resolutionLabel.setFont(fontLarge);
		
		// create the region description fields
		sel_center = new JLabel();
		sel_km = new JLabel();
		sel_points = new JLabel("Select the area to be exported");
		sel_t_size = new JTextField();
		sel_t_size.setText(Integer.toString(parms.dTileSize));

		// NORTH: 4x2 grid of descriptions
		JPanel descPanel = new JPanel(new GridLayout(4,2));
		descPanel.setBorder(BorderFactory.createEmptyBorder(20,10,20,10));
		descPanel.add(new JLabel("Center (lat, lon)"));
		descPanel.add(sel_center);
		descPanel.add(new JLabel("Selected area (km)"));
		descPanel.add(sel_km);
		descPanel.add(new JLabel("Selected area (tiles)"));
		descPanel.add(sel_points);
		descPanel.add(new JLabel("Tile Size (m)"));
		descPanel.add(sel_t_size);
		mainPane.add(descPanel, BorderLayout.NORTH);

		// SOUTH: 3 buttons
		JPanel buttons = new JPanel(new GridLayout(1,7));
		buttons.add(cancel);
		buttons.add(new JLabel(" "));
		buttons.add(previewT);
		buttons.add(new JLabel("  "));
		buttons.add(previewF);
		buttons.add(new JLabel("  "));
		buttons.add(accept);
		buttons.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 10));
		mainPane.add(buttons, BorderLayout.SOUTH);
		
		// CENTER: rows, start w/tile size slider, map name
		JPanel namePanel = new JPanel(new GridLayout(2,1));
		namePanel.add(nameLabel);
		namePanel.add(sel_name);
		namePanel.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
		
		JPanel resPanel = new JPanel();
		resPanel.setLayout(new BoxLayout(resPanel, BoxLayout.PAGE_AXIS));
		resPanel.add(resolutionLabel);
		resPanel.add(resolution);

		JPanel sliders = new JPanel();
		sliders.setLayout(new BoxLayout(sliders, BoxLayout.LINE_AXIS));
		resPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 15));
		sliders.add(resPanel);
		
		controls = new JPanel();
		controls.setLayout(new BoxLayout(controls, BoxLayout.PAGE_AXIS));
		controls.add(sliders);
		controls.add(namePanel);
		mainPane.add(controls, BorderLayout.CENTER);
		
		// add the super-class action listeners
		// (in-place to prevent shadowing by sub-class listeners)
		map.addMapListener(this);
		sel_t_size.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int meters = Integer.parseInt(sel_t_size.getText());
				resolution.setValue(meters_to_slider(meters));
				sel_t_size.setText(Integer.toString(meters));
				tile_size(meters);
				newSelection = true;
			}
		});
		resolution.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				int meters = slider_to_meters(resolution.getValue());
				sel_t_size.setText(Integer.toString(meters));
				tile_size(meters);
				newSelection = true;
			}
		});
		
		selected = map.checkSelection(Map.Selection.RECTANGLE);
		newSelection = false;
		
		if (parms.debug_level >= EXPORT_DEBUG)
			System.out.println("new Export Base(" + format + ")");
	}
	
	/**
	 * return the export row associated with a y @return coordinate
	 */
	int box_row(double y) {
		if (y < box_y)
			return(0);
		if (y >= box_y + box_height)
			return(y_points - 1);
		double dy = (y - box_y)/box_height;
		dy *= y_points;
		return (int) dy;
	}
	
	/**
	 * return the export column associated with an x coordinate
	 */
	int box_col(double x) {
		if (x < box_x)
			return(0);
		if (x >= box_x + box_width)
			return(x_points - 1);
		double dx = (x - box_x)/box_width;
		dx *= x_points;
		return (int) dx;
	}
	
	/**
	 * export a map as high resolution tiles
	 */
	protected void export(Exporter export) {

		// set (and remember) the tile size
		int meters = Integer.parseInt(sel_t_size.getText());
		export.tileSize(meters);
		parms.dTileSize = meters;
		
		// export the temperature range
		export.temps(parms.meanTemp(), parms.meanSummer(), parms.meanWinter());
		
		double lat = parms.latitude(box_y + box_height/2);
		double lon = parms.longitude(box_x + box_width/2);
		export.position(lat, lon);
	
		// get Cartesian interpolations of tile characteristics
		Cartesian cart = new Cartesian(map.getMesh(), box_x, box_y, box_x+box_width, box_y+box_height, x_points, y_points);
		
		// TODO lose gratuitous interpolates
		//		only need to redo these if the region changes
		//		this might save a lot of time, and might enable
		//		continuous previews
		export.heightMap(cart.interpolate(map.getHeightMap()));
		export.erodeMap(cart.interpolate(map.getErodeMap()));
		export.rainMap(cart.interpolate(map.getRainMap()));
		export.soilMap(cart.interpolate(map.getSoilMap()));
		double hydration[][] = cart.interpolate(map.getHydrationMap());
		add_rivers(hydration, meters);	// add rivers to hydration map
		export.waterMap(hydration);
	}
	
	/**
	 * overlay rivers on top of interpolated hydration map
	 * 
	 * Note: this cannot simply be interpolated like the rest of
	 * 		the maps because a river is not distributed over the
	 * 		entire MeshPoint, but only in specific tiles.
	 * 
	 * @param	hydration (cartesian map to update)
	 * @param 	tilesize (in meters)
	 */
	protected void add_rivers(double[][] hydration, int tilesize) {
		
		Mesh mesh = map.getMesh();
		double[] fluxMap = map.getFluxMap();
		int[] downHill = map.getDownHill();
		double[] hydroMap = map.getHydrationMap();
		double[] heightMap = map.getHeightMap();
		double[] erodeMap = map.getErodeMap();
		
		// consider all points in the Mesh
		for(int i = 0; i < mesh.vertices.length; i++) {
			// ignore any source point already under water
			if (hydroMap[i] < 0)
				continue;
			
			// ignore any w/no downhill flow
			int d = downHill[i];
			if (d < 0)
				continue;
			
			// ignore any that fall below stream flux
			if (fluxMap[i] < parms.stream_flux)
				continue;
	
			// ignore flows that are entirely outside the box
			double x0 = mesh.vertices[i].x;
			double y0 = mesh.vertices[i].y;
			double x1 = mesh.vertices[d].x;
			double y1 = mesh.vertices[d].y;
			if (x0 < box_x && x1 < box_x)
				continue;		// all to the west
			if (x0 >= box_x + box_width && x1 >= box_x + box_width)
				continue;		// all to the east
			if (y0 < box_y && y1 < box_y)
				continue;		// all to the north
			if (y0 >= box_y + box_height && y1 >= box_y + box_height)
				continue;		// all to the south

			// figure out the length and slope
			double dist = 1000 * parms.km(mesh.vertices[i].distance(mesh.vertices[d]));
			double z0 = heightMap[i] - erodeMap[i];
			double z1 = heightMap[d] - erodeMap[d];
			double slope = parms.height(z0 - z1)/dist;
		
			// figure out the river depth and width
			double v = Hydrology.velocity(slope);
			double depth = Hydrology.depth(fluxMap[i],  v);
			double width = Hydrology.width(fluxMap[i],  v);
			
			// figure out how many tiles wide the river should be
			int stroke = (width <= tilesize) ? 1 : (int) ((width + width - 1) / tilesize);
			if (tilesize/width > 10 && fluxMap[i] >= parms.river_flux)
				stroke++;
			if (tilesize/width > 100 && fluxMap[i] >= parms.artery_flux)
				stroke++;
			
			// figure out starting and ending positions
			int r = box_row(y0);
			int rDest = box_row(y1);
			int c = box_col(x0);
			int cDest = box_col(x1);
			
			// figure out how far we have to go
			int drawn = 0;
			int dR = rDest - r;
			int dC = cDest - c;
			
			// fill the tiles between here and there with water
			while(drawn++ == 0 || dR != 0 || dC != 0) {
				// figure out which direction we want to move in
				if (Math.abs(dR) > Math.abs(dC)) { // vertical flow
					int start = c - (stroke/2);
					if (r >= 0 && r < y_points && start >= 0 && start + stroke <= x_points) {
						for(int j = 0; j < stroke; j++)
							if (hydration[r][start + j] > -depth)
								hydration[r][start + j] = -depth;
							else	// already deep water gets deeper
								hydration[r][start+j] -= depth;
					}
					// move on to the next row
					r += (dR>0) ? 1 : -1;
					dR = rDest - r;
				} else {	// horizontal flow or last stroke
					int start = r - (stroke/2);
					if (c >= 0 && c < x_points && start >= 0 && start + stroke <= y_points) {
						for(int j = 0; j < stroke; j++)
							if (hydration[start + j][c] > -depth)
								hydration[start + j][c] = -depth;
							else	// already deep water gets deeper
								hydration[start + j][c] -= depth;
					}
					// move on to the next column
					if (dC != 0) {
						c += (dC>0) ? 1 : -1;
						dC = cDest - c;
					}
				}
			}
		}
	}

	/**
	 * called whenever a region selection changes
	 * @param mx0	left most point (map coordinate)
	 * @param my0	upper most point (map coordinate)
	 * @param dx	width (in map units)
	 * @param dy	height (in map units)
	 * @param complete	boolean, has selection completed
	 * 
	 * @return	boolean	(should selection continue)
	 */
	public boolean regionSelected(double mx0, double my0, 
			  double dx, double dy, boolean complete) {

		// update the export box location
		box_x = mx0;
		box_y = my0;
		box_width = dx;
		box_height = dy;
		sel_center.setText(String.format("%.6f, %.6f", 
				parms.latitude(box_y + box_height/2),  
				parms.longitude(box_x + box_width/2)));
		
		// update the selected area size
		x_km = parms.km(box_width);
		y_km = parms.km(box_height);
		sel_km.setText(String.format("%.1fx%.1f", x_km, y_km));

		// and re-scale per tile size
		tile_size(Integer.parseInt(sel_t_size.getText()));
		
		selected = true;
		newSelection = true;
		return true;
	}
	
	/**
	 * update the expected export size based on a new tile size
	 * 
	 * @param meters
	 */
	private void tile_size(int meters) {
		x_points = (int) (x_km * 1000 / meters);
		y_points = (int) (y_km * 1000 / meters);
		int tiles = x_points * y_points;
		sel_points.setText(x_points + "x" + y_points);
		sel_points.setForeground( tiles > parms.tiles_max ? Color.RED : Color.BLACK);
	}
	
	/**
	 * @return the number of meters associated with a slider value
	 * @param slider
	 */
	private int slider_to_meters(int slider) {
		// get the power of ten
		int meters = 1;
		while(slider > TICS_PER_DECADE) {
			meters *= 10;
			slider -= TICS_PER_DECADE;
		}
		
		return(meters * (1 + slider));
	}
	
	/**
	 * @return the slider value associated with a number of meters
	 * @param meters
	 */
	private int meters_to_slider(int meters) {
		int slider = 0;
		while(slider_to_meters(slider+1) <= meters)
			slider++;
		return(slider);
	}

	/**
	 * Window Close event handler ... implicit CANCEL
	 */
	public void windowClosing(WindowEvent e) {
		map.selectMode(Map.Selection.ANY);
		map.removeMapListener(this);
		this.dispose();
		WorldBuilder.activeDialog = false;
	}
	
	// perfunctory handlers for events we don't care about
	public boolean pointSelected(double x, double y) { return false; }
	
	public void windowActivated(WindowEvent arg0) {}
	public void windowClosed(WindowEvent arg0) {}
	public void windowDeactivated(WindowEvent arg0) {}
	public void windowDeiconified(WindowEvent arg0) {}
	public void windowIconified(WindowEvent arg0) {}
	public void windowOpened(WindowEvent arg0) {}
	
	public void mouseClicked(MouseEvent arg0) {}
	public void mouseMoved(MouseEvent arg0) {}
	public void mouseEntered(MouseEvent arg0) {}
	public void mouseExited(MouseEvent arg0) {}
}
