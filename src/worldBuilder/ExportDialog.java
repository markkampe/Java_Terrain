package worldBuilder;

import java.awt.*;
import java.awt.event.*;
import java.io.FileWriter;
import java.io.IOException;
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
	private String filename;
	
	private JTextField sel_name;
	private JLabel sel_file;
	private JLabel sel_pixels;
	private JLabel sel_center;
	private JLabel sel_km;
	private JLabel sel_t_size;
	private JLabel sel_points;
	private JSlider resolution;
	private JButton accept;
	private JButton cancel;
	
	private boolean selecting;		// selection in progress
	private boolean selected;		// selection completed
	private int x_start, x_end, y_start, y_end;		// selection screen coordinates
	private int x_points, y_points;	// selection width/height (in tiles)
	private double x_km, y_km;		// selection width/height (in km)

	private static final int tile_sizes[] = {1, 5, 10, 50, 100, 500, 1000, 5000, 10000};
	
	private static final int BORDER_WIDTH = 5;
	
	private static final long serialVersionUID = 1L;
	
	public ExportDialog(Map map, String filename)  {
		// pick up references
		this.filename = filename;
		this.map = map;
		this.parms = Parameters.getInstance();
		
		// create the dialog box
		Container mainPane = getContentPane();
		((JComponent) mainPane).setBorder(BorderFactory.createMatteBorder(BORDER_WIDTH, BORDER_WIDTH, BORDER_WIDTH, BORDER_WIDTH, Color.LIGHT_GRAY));
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
		
		accept = new JButton("EXPORT");
		cancel = new JButton("CANCEL");
		
		resolution = new JSlider(JSlider.HORIZONTAL, 0, 8, 2);
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
		
		sel_file = new JLabel(filename);
		sel_pixels = new JLabel();
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
		
		JPanel descPanel = new JPanel(new GridLayout(6,2));
		descPanel.setBorder(BorderFactory.createEmptyBorder(20,10,20,10));
		descPanel.add(new JLabel("File"));
		descPanel.add(sel_file);
		descPanel.add(new JLabel("Center"));
		descPanel.add(sel_center);
		descPanel.add(new JLabel("Pixels"));
		descPanel.add(sel_pixels);
		descPanel.add(new JLabel("km"));
		descPanel.add(sel_km);
		descPanel.add(new JLabel("Points"));
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
		buttons.add(cancel);
		buttons.add(Box.createRigidArea(new Dimension(40,0)));
		buttons.add(accept);
		buttons.setBorder(BorderFactory.createEmptyBorder(20,100, 20, 10));
		
		JPanel controls = new JPanel();
		controls.setLayout(new BoxLayout(controls, BoxLayout.PAGE_AXIS));
		controls.add(namePanel);
		controls.add(sliders);
		controls.add(buttons);

		mainPane.add(descPanel, BorderLayout.NORTH);
		mainPane.add(controls, BorderLayout.SOUTH);
		
		pack();
		setLocation(parms.dialogDX, parms.dialogDY);
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
	
	/**
	 * export a map as high resolution tiles
	 */
	public void export() {
		// figure out the selected region (in map coordinates)
		double x = map.x(x_start);
		double dx = map.x(x_end) - x;
		if (dx < 0) {
			x -= dx;
			dx = -dx;
		}
		double y = map.y(y_start);
		double dy = map.x(y_end) - y;
		if (dy < 0) {
			y -= dy;
			dy = -dy;
		}
		
		// get Cartesian interpolations of tile characteristics
		Cartesian cart = new Cartesian(map.getMesh(), x, y, x+dx, y+dy, x_points, y_points);
		double heights[][] = cart.interpolate(map.getHeightMap());
		double rain[][] = cart.interpolate(map.getRainMap());
		
		double lat = parms.latitude(y + dy/2);
		double lon = parms.longitude(x + dx/2);
		
		int meters = tile_sizes[resolution.getValue()];
		// create the appropriate height map
		// create an appropriate water map
		try {
			FileWriter output = new FileWriter(filename);
			final String FORMAT_S = " \"%s\": \"%s\"";
			final String FORMAT_D = " \"%s\": %d";
			final String FORMAT_DM = " \"%s\": \"%dm\"";
			final String FORMAT_DP = " \"%s\": \"%d%%\"";
			final String FORMAT_FM = " \"%s\": \"%.2fm\"";
			final String FORMAT_CM = " \"%s\": \"%.0fcm\"";
			final String FORMAT_L = " \"%s\": %.6f";
			final String FORMAT_O = " \"%s\": {";
			final String FORMAT_A = " \"%s\": [";
			final String NEW_POINT = "\n        { ";
			final String NEWLINE = "\n    ";
			final String COMMA = ", ";
			
			// write out the grid wrapper
			output.write("{");
			output.write(NEWLINE);
			output.write(String.format(FORMAT_S,  "name", sel_name.getText()));
			output.write(",");
			output.write(NEWLINE);
			output.write(String.format(FORMAT_O, "dimensions"));
				output.write(String.format(FORMAT_D, "height", y_points));
				output.write(COMMA);
				output.write(String.format(FORMAT_D, "width", x_points));
				output.write(" },");
				output.write(NEWLINE);
			output.write(String.format(FORMAT_DM, "tilesize", meters));
			output.write(",");
			output.write(NEWLINE);
			output.write(String.format(FORMAT_O, "center"));
				output.write(String.format(FORMAT_L, "latitude", lat));
				output.write(COMMA);
				output.write(String.format(FORMAT_L, "longitude", lon));
				output.write(" },");
			output.write(NEWLINE);
			output.write(String.format(FORMAT_A, "points"));
			
			boolean first = true;
			for(int r = 0; r < y_points; r++) {
				for(int c = 0; c < x_points; c++) {
					int hydration = parms.dAmount;	// FIX compute real hydration
					String soil = "alluvial";	// FIX compute real soil type
					
					if (first)
						first = false;
					else
						output.write(",");
					output.write(NEW_POINT);
					output.write(String.format(FORMAT_FM, "altitude", parms.altitude(heights[r][c])));
					output.write(COMMA);
					output.write(String.format(FORMAT_CM, "rainfall", rain[r][c]));
					output.write(COMMA);
					output.write(String.format(FORMAT_DP, "hydration", hydration));
					output.write(COMMA);
					output.write(String.format(FORMAT_S, "soil", soil));
					output.write(" }");
				}
			}
			output.write(NEWLINE);
			output.write("]\n");	// end of points
			output.write( "}\n");	// end of grid
			output.close();
		} catch (IOException e) {
			System.err.println("Unable to create output file " + filename);		
		}
	}
	
	/**
	 * describe the selected area
	 */
	private void select(int x0, int y0, int x1, int y1, int res) {
		// selected area in pixels
		int x_pixels = Math.abs(x1 - x0);
		int y_pixels = Math.abs(y1 - y0);
		
		// selected area in map coordinates
		double X0 = map.x(x0);
		double X1 = map.x(x1);
		double dx = X1 - X0;
		double Y0 = map.y(y0);	
		double Y1 = map.y(y1);
		double dy = Y1 - Y0;
	
		// selected area in km
		x_km = parms.km(dx);
		y_km = parms.km(dy);

		// selected area in tiles
		x_points = (int) x_km * 1000/res;
		y_points = (int) y_km * 1000/res;
		
		sel_pixels.setText(x_pixels + "x" + y_pixels);
		sel_center.setText(String.format("%.6f, %.6f", parms.latitude((Y1+Y0)/2),  parms.longitude((X1+X0)/2)));
		sel_km.setText((int) x_km + "x" + (int) y_km);
		sel_t_size.setText(res + " meters");
		sel_points.setText(x_points + "x" + y_points + " tiles");
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
			x_end = e.getX();
			y_end = e.getY();
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
			export();
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