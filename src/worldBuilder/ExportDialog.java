package worldBuilder;

import java.awt.*;
import java.awt.event.*;
import java.util.Hashtable;

import javax.swing.*;
import javax.swing.event.*;

/**
 * SlopeDialog allows the user to choose an axis and inclination to
 * cause a uniform slope to the entire map.  
 */
public class ExportDialog extends JFrame implements ActionListener, ChangeListener, MouseListener, MouseMotionListener, WindowListener {	
	private Map map;
	private Parameters parms;

	private JTextField sel_name;
	private JLabel sel_center;
	private JLabel sel_km;
	private JLabel sel_t_size;
	private JLabel sel_points;
	private JSlider resolution;
	private JComboBox<String> format;
	private JButton accept;
	private JButton cancel;
	
	private boolean selecting;		// selection in progress
	private boolean selected;		// selection completed
	private int x_start, x_end, y_start, y_end;		// selection screen coordinates
	private int x_points, y_points;	// selection width/height (in tiles)
	private double x_km, y_km;		// selection width/height (in km)

	private static final int tile_sizes[] = {1, 5, 10, 50, 100, 500, 1000, 5000, 10000};
	
	private static final long serialVersionUID = 1L;
	
	public ExportDialog(Map map)  {
		// pick up references
		this.map = map;
		this.parms = Parameters.getInstance();
		
		// create the dialog box
		Container mainPane = getContentPane();
		int border = parms.dialogBorder;
		((JComponent) mainPane).setBorder(BorderFactory.createMatteBorder(border, border, border, border, Color.LIGHT_GRAY));
		setTitle("Export");
		addWindowListener( this );
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		
		// create the basic widgets
		Font fontSmall = new Font("Serif", Font.ITALIC, 10);
		Font fontLarge = new Font("Serif", Font.ITALIC, 15);
		sel_name = new JTextField();
		sel_name.setText("Happyville");
		JLabel nameLabel = new JLabel("Name of this region", JLabel.CENTER);
		nameLabel.setFont(fontLarge);
		
		format = new JComboBox<String>();
		format.addItem("json");
		format.addItem("RPGMaker");
		format.setSelectedItem("RPGMaker");
		
		accept = new JButton("EXPORT");
		cancel = new JButton("CANCEL");
		
		int dflt;
		for(dflt = 0; dflt < tile_sizes.length; dflt++)
			if (tile_sizes[dflt] >= 1000)
				break;
		resolution = new JSlider(JSlider.HORIZONTAL, 0, tile_sizes.length-1, dflt);
		resolution.setMajorTickSpacing(2);
		resolution.setMinorTickSpacing(1);
		resolution.setFont(fontSmall);
		resolution.setPaintTicks(true);

		Hashtable<Integer, JLabel> labels = new Hashtable<Integer, JLabel>();
		labels.put(0, new JLabel("1"));
		labels.put(2, new JLabel("10"));
		labels.put(4, new JLabel("100"));
		labels.put(6, new JLabel("1km"));
		labels.put(8, new JLabel("10km"));
		resolution.setLabelTable(labels);
		resolution.setPaintLabels(true);
		
		JLabel resolutionLabel = new JLabel("Tile size(m)", JLabel.CENTER);
		resolutionLabel.setFont(fontLarge);
		
		sel_center = new JLabel();
		sel_km = new JLabel();
		sel_t_size = new JLabel();
		sel_points = new JLabel("Select the area to be exported");

		/*
		 * Pack them into:
		 * 		a name (1x2 grid) name selection panel
		 * 		a vertical Box layout containing descriptions, sliders and buttons
		 * 		descriptions are a 1x3 layout of Labels
		 * 		sliders are a 1x2 grid layout
		 * 			each being a vertical Box w/label and slider
		 * 		buttons a horizontal Box layout
		 */
		JPanel namePanel = new JPanel(new GridLayout(2,1));
		namePanel.add(nameLabel);
		namePanel.add(sel_name);
		namePanel.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
		
		JPanel descPanel = new JPanel(new GridLayout(4,2));
		descPanel.setBorder(BorderFactory.createEmptyBorder(20,10,20,10));
		descPanel.add(new JLabel("Center"));
		descPanel.add(sel_center);
		descPanel.add(new JLabel("km"));
		descPanel.add(sel_km);
		descPanel.add(new JLabel("Tiles"));
		descPanel.add(sel_points);
		descPanel.add(new JLabel("Tile Size"));
		descPanel.add(sel_t_size);
		
		JPanel resPanel = new JPanel();
		resPanel.setLayout(new BoxLayout(resPanel, BoxLayout.PAGE_AXIS));
		resPanel.add(resolutionLabel);
		resPanel.add(resolution);

		JPanel sliders = new JPanel();
		sliders.setLayout(new BoxLayout(sliders, BoxLayout.LINE_AXIS));
		resPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 15));
		sliders.add(resPanel);
		
		JPanel buttons = new JPanel();
		buttons.setLayout(new BoxLayout(buttons, BoxLayout.LINE_AXIS));
		buttons.add(format);
		buttons.add(Box.createRigidArea(new Dimension(40,0)));
		buttons.add(cancel);
		buttons.add(Box.createRigidArea(new Dimension(40,0)));
		buttons.add(accept);
		buttons.setBorder(BorderFactory.createEmptyBorder(20, 100, 20, 10));
		
		JPanel controls = new JPanel();
		controls.setLayout(new BoxLayout(controls, BoxLayout.PAGE_AXIS));
		controls.add(namePanel);
		controls.add(sliders);
		controls.add(buttons);

		mainPane.add(descPanel, BorderLayout.NORTH);
		mainPane.add(controls, BorderLayout.SOUTH);
		pack();
		setVisible(true);
		
		// add the action listeners
		resolution.addChangeListener(this);
		accept.addActionListener(this);
		cancel.addActionListener(this);
		map.addMouseListener(this);
		map.addMouseMotionListener(this);
		
		selecting = false;
		selected = false;
	}
	
	// export box (in map coordinates)
	private double box_x, box_y;
	private double box_width, box_height;
	
	/**
	 * return the export row associated with a y coordinate
	 */
	int box_row(double y) {
		if (y < box_y || y >= box_y + box_height)
			return(-1);
		double dy = (y - box_y)/box_height;
		dy *= y_points;
		return (int) dy;
	}
	
	/**
	 * return the export column associated with an x coordinate
	 */
	int box_col(double x) {
		if (x < box_x || x >= box_x + box_width)
			return(-1);
		double dx = (x - box_x)/box_width;
		dx *= x_points;
		return (int) dx;
	}
	
	/**
	 * export a map as high resolution tiles
	 */
	public void export(String filename) {
		// create an appropriate exporter
		Exporter export;
		String f = (String) format.getSelectedItem();
		if (f.equals("json"))
			export = new JsonExporter(filename);
		else if (f.equals("RPGMaker"))
			export = new RpgmExporter(filename);
		else {
			System.err.println("Unknown export format: " + f);
			return;
		}
		export.name(sel_name.getText());
		export.dimensions(x_points, y_points);
		int meters = tile_sizes[resolution.getValue()];
		export.tileSize(meters);
		export.temps(parms.meanTemp(), parms.meanSummer(), parms.meanWinter());
		
		// figure out the selected region (in map coordinates)
		box_x = map.map_x(x_start);
		box_width = map.map_x(x_end) - box_x;
		if (box_width < 0) {
			box_x -= box_width;
			box_width = -box_width;
		}
		box_y = map.map_y(y_start);
		box_height = map.map_y(y_end) - box_y;
		if (box_height < 0) {
			box_y -= box_height;
			box_height = -box_height;
		}
		double lat = parms.latitude(box_y + box_height/2);
		double lon = parms.longitude(box_x + box_width/2);
		export.position(lat, lon);
	
		// get Cartesian interpolations of tile characteristics
		Cartesian cart = new Cartesian(map.getMesh(), box_x, box_y, box_x+box_width, box_y+box_height, x_points, y_points);
		
		export.heightMap(cart.interpolate(map.getHeightMap()));
		export.erodeMap(cart.interpolate(map.getErodeMap()));
		export.rainMap(cart.interpolate(map.getRainMap()));
		export.soilMap(cart.interpolate(map.getSoilMap()));
		double hydration[][] = cart.interpolate(map.getHydrationMap());
		add_rivers(hydration, meters);	// add rivers to hydration map
		export.waterMap(hydration);

		// and force it all out
		if (export.flush()) {
			if (parms.debug_level > 0) {
				System.out.println("Exported(" + f + ") " +
						x_points + "x" + y_points + 
						" " + meters + " meter tiles, centered at <" + 
						String.format("%9.6f", lat) + "," + 
						String.format("%9.6f", lon) + 
						"> to file " + filename);
			}
		} else {
			System.err.println("Unable to export map to file " + filename);
		}
	}
	
	/**
	 * overlay rivers on top of interpolated hydration map
	 * 
	 * Note: this cannot simply be interpolated like the rest of
	 * 		the maps because a river is not distributed over the
	 * 		entire MeshPoint, but only in specific tiles.
	 * 
	 * @param	cartesian hydration map (to update)
	 * @param 	tile size (in meters)
	 */
	private void add_rivers(double[][] hydration, int tilesize) {
		
		Mesh mesh = map.getMesh();
		double[] fluxMap = map.getFluxMap();
		int[] downHill = map.getDownHill();
		double[] hydroMap = map.getHydrationMap();
		double[] heightMap = map.getHeightMap();
		double[] erodeMap = map.getErodeMap();
		
		// consider all points in the Mesh
		for(int i = 0; i < mesh.vertices.length; i++) {
			// ignore any already under water
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
			int stroke = (width <= tilesize) ? 1 : (int) ((width + width - 1) / tilesize);
			
			// figure out the length, dx and dy (in tiles)
			int length = (int) dist / tilesize;
			double dx = (x1 - x0)/length;
			double dy = (y1 - y0)/length;
			
			// set depth for each point along the course
			double x = x0;
			double y = y0;
			while(length-- > 0) {
				// TODO stroke width for river export
				int r = box_row(y);
				int c = box_col(x);
				if (r >= 0 && c >= 0)
					hydration[r][c] = -depth;
				x += dx;
				y += dy;
			}		
		}
	}

	/**
	 * describe the selected area
	 */
	private void select(int x0, int y0, int x1, int y1, int res) {
		// selected area in map coordinates
		double X0 = map.map_x(x0);
		double X1 = map.map_x(x1);
		double dx = X1 - X0;
		if (dx < 0) {
			X0 = X1;
			dx *= -1;
		}
		double Y0 = map.map_y(y0);	
		double Y1 = map.map_y(y1);
		double dy = Y1 - Y0;
		if (dy < 0) {
			Y0 = -Y1;
			dy *= -1;
		}
	
		// selected area in km
		x_km = parms.km(dx);
		y_km = parms.km(dy);

		// selected area in tiles
		x_points = (int) (x_km * 1000 / res);
		y_points = (int) (y_km * 1000 / res);
		int tiles = x_points * y_points;
		
		sel_center.setText(String.format("%.6f, %.6f", parms.latitude((Y1+Y0)/2),  parms.longitude((X1+X0)/2)));
		sel_km.setText(String.format("%.1fx%.1f", x_km, y_km));
		sel_t_size.setText(res + " meters");
		sel_points.setText(x_points + "x" + y_points);
		sel_points.setForeground( tiles > parms.tiles_max ? Color.RED : Color.BLACK);
	}
	
	/**
	 * start defining a mountain range
	 */
	public void mousePressed(MouseEvent e) {
		x_start = e.getX();
		y_start = e.getY();
		selecting = true;
	}

	/**
	 * finish defining a export region
	 */
	public void mouseReleased(MouseEvent e) {
		if (selecting) {
			if (e.getX() >= x_start)
				x_end = e.getX();
			else {
				x_end = x_start;
				x_start = e.getX();
			}
			if (e.getY() >= y_start)
				y_end = e.getY();
			else {
				y_end = y_start;
				y_start = e.getY();
			}

			selecting = false;
			selected = true;
			select(x_start, y_start, x_end, y_end, tile_sizes[resolution.getValue()]);
		}
	}
	
	/**
	 * progress in region selection
	 */
	public void mouseDragged(MouseEvent e) {
		if (selecting) {
			map.selectRect(x_start, y_start, e.getX()-x_start, e.getY()-y_start);
			select(x_start, y_start, e.getX(), e.getY(), tile_sizes[resolution.getValue()]);
		}	
	}
	
	/**
	 * Window Close event handler ... implicit CANCEL
	 */
	public void windowClosing(WindowEvent e) {
		map.selectNone();
		this.dispose();
		map.removeMouseListener(this);
		map.removeMouseMotionListener(this);
	}
	
	/**
	 * updates to the axis/inclination sliders
	 */
	public void stateChanged(ChangeEvent e) {
		if (selected && e.getSource() == resolution) {
				select(x_start, y_start, x_end, y_end, tile_sizes[resolution.getValue()]);
		} 
	}

	/**
	 * click events on ACCEPT/CANCEL buttons
	 */
	public void actionPerformed(ActionEvent e) {
		// clear the selection
		map.selectNone();
		
		if (e.getSource() == accept && selected) {
			FileDialog d = new FileDialog(this, "Export", FileDialog.SAVE);
			d.setFile(sel_name.getText()+".json");
			d.setVisible(true);
			String export_file = d.getFile();
			if (export_file != null) {
				String dir = d.getDirectory();
				if (dir != null)
					export_file = dir + export_file;
				export(export_file);
			}
		}
		
		// discard the window
		this.dispose();
		map.removeMouseListener(this);
		map.removeMouseMotionListener(this);
	}

	public void mouseClicked(MouseEvent arg0) {}
	public void mouseMoved(MouseEvent arg0) {}
	public void mouseEntered(MouseEvent arg0) {}
	public void mouseExited(MouseEvent arg0) {}
	public void windowActivated(WindowEvent arg0) {}
	public void windowClosed(WindowEvent arg0) {}
	public void windowDeactivated(WindowEvent arg0) {}
	public void windowDeiconified(WindowEvent arg0) {}
	public void windowIconified(WindowEvent arg0) {}
	public void windowOpened(WindowEvent arg0) {}
}