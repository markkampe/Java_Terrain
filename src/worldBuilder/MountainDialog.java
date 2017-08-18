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
public class MountainDialog extends JFrame implements ActionListener, ChangeListener, MouseListener, MouseMotionListener, WindowListener {	
	
	private static final double MIN_MOUNTAIN = 0.05;	// minerals pop through	
	
	private Map map;
	private double[] oldHeight;	// per MeshPoint altitude at entry
	private double[] newHeight;	// edited per MeshPoint altitude
	private Parameters parms;
	
	private JSlider altitude;
	private JSlider diameter;
	private JSlider rounding;
	private JButton accept;
	private JButton cancel;
	
	private boolean selecting;		// selection in progress
	private boolean selected;		// selection completed
	private int x_start, x_end, y_start, y_end;		// selection start/end coordinates
	
	private int d_max;				// diameter: full scale
	private int a_max;				// altitude: full scale
	
	private String placed;			// debug message
	private static final String POS_FMT = "%.6f";
	
	private static final int DIALOG_OFFSET = 5;
	
	private static final long serialVersionUID = 1L;
	
	public MountainDialog(Map map)  {
		// pick up references
		this.map = map;
		this.oldHeight = map.getHeightMap();
		this.parms = Parameters.getInstance();
		
		// copy the current height map
		this.newHeight = new double[oldHeight.length];
		for(int i = 0; i < oldHeight.length; i++)
			newHeight[i] = oldHeight[i];
		map.setHeightMap(newHeight);
		
		// calibrate full scale on the sliders
		this.a_max = parms.z_range/2;
		this.d_max = parms.xy_range / parms.mountain_divisor;
	
		// create the dialog box
		Container mainPane = getContentPane();
		int border = parms.dialogBorder;
		((JComponent) mainPane).setBorder(BorderFactory.createMatteBorder(border, border, border, border, Color.LIGHT_GRAY));
		setTitle("Add Mountain(s)");
		addWindowListener( this );
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		
		// create the basic widgets
		Font fontSmall = new Font("Serif", Font.ITALIC, 10);
		Font fontLarge = new Font("Serif", Font.ITALIC, 15);
		accept = new JButton("ACCEPT");
		cancel = new JButton("CANCEL");
		
		altitude = new JSlider(JSlider.HORIZONTAL, -this.a_max, this.a_max, parms.dAltitude);
		altitude.setMajorTickSpacing(Parameters.niceTics(-a_max, a_max, true));
		altitude.setMinorTickSpacing(Parameters.niceTics(-a_max, a_max, false));
		altitude.setFont(fontSmall);
		altitude.setPaintTicks(true);
		altitude.setPaintLabels(true);
		JLabel altitudeLabel = new JLabel("Altitude(m)", JLabel.CENTER);
		altitudeLabel.setFont(fontLarge);

		diameter = new JSlider(JSlider.HORIZONTAL, 0, d_max, parms.dDiameter);
		diameter.setMajorTickSpacing(Parameters.niceTics(0, d_max,true));
		diameter.setMinorTickSpacing(Parameters.niceTics(0, d_max,false));
		diameter.setFont(fontSmall);
		diameter.setPaintTicks(true);
		diameter.setPaintLabels(true);
		JLabel diameterLabel = new JLabel("Diameter(km)", JLabel.CENTER);
		diameterLabel.setFont(fontLarge);
		
		rounding = new JSlider(JSlider.HORIZONTAL, Parameters.CONICAL, Parameters.CYLINDRICAL, parms.dShape);
		rounding.setMajorTickSpacing(4);
		rounding.setMinorTickSpacing(1);
		rounding.setFont(fontSmall);
		rounding.setPaintTicks(true);
		JLabel roundLabel = new JLabel("Profile", JLabel.CENTER);
		roundLabel.setFont(fontLarge);
		
		Hashtable<Integer, JLabel> labels = new Hashtable<Integer, JLabel>();
		labels.put(Parameters.CONICAL, new JLabel("cone"));
		labels.put(Parameters.SPHERICAL, new JLabel("round"));
		labels.put(Parameters.CYLINDRICAL, new JLabel("flat"));
		rounding.setLabelTable(labels);
		rounding.setPaintLabels(true);
		
		/*
		 * Pack them into:
		 * 		a vertical Box layout containing sliders and buttons
		 * 		sliders are a 1x2 grid layout
		 * 			each being a vertical Box w/label and slider
		 * 		buttons a horizontal Box layout
		 */
		JPanel altPanel = new JPanel();
		altPanel.setLayout(new BoxLayout(altPanel, BoxLayout.PAGE_AXIS));
		altPanel.add(altitudeLabel);
		altPanel.add(altitude);
		
		JPanel diaPanel = new JPanel();
		diaPanel.setLayout(new BoxLayout(diaPanel, BoxLayout.PAGE_AXIS));
		diaPanel.add(diameterLabel);
		diaPanel.add(diameter);
		
		JPanel rndPanel = new JPanel();
		rndPanel.setLayout(new BoxLayout(rndPanel, BoxLayout.PAGE_AXIS));
		rndPanel.add(roundLabel);
		rndPanel.add(rounding);

		JPanel sliders = new JPanel();
		sliders.setLayout(new BoxLayout(sliders, BoxLayout.LINE_AXIS));
		altPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 15));
		diaPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 0, 15));
		rndPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 0, 10));
		sliders.add(altPanel);
		sliders.add(diaPanel);
		sliders.add(rndPanel);
		
		JPanel buttons = new JPanel();
		buttons.setLayout(new BoxLayout(buttons, BoxLayout.LINE_AXIS));
		buttons.add(cancel);
		buttons.add(Box.createRigidArea(new Dimension(40,0)));
		buttons.add(accept);
		buttons.setBorder(BorderFactory.createEmptyBorder(20,100, 20, 10));

		mainPane.add(sliders);
		mainPane.add(buttons, BorderLayout.SOUTH);
		
		pack();
		setLocation(parms.dialogDX + DIALOG_OFFSET * parms.dialogDelta, parms.dialogDY + DIALOG_OFFSET * parms.dialogDelta);
		setVisible(true);
		
		// add the action listeners
		altitude.addChangeListener(this);
		diameter.addChangeListener(this);
		rounding.addChangeListener(this);
		accept.addActionListener(this);
		cancel.addActionListener(this);
		map.addMouseListener(this);
		map.addMouseMotionListener(this);
		
		selecting = false;
		selected = false;
	}

	/**
	 * @param Map
	 * @param x: (map) x coordinate
	 * @param y: (map) y coordinate
	 * @param radius: (map) radius (0 = full map)
	 * @param zMax: max (map) z value
	 * @param shape: curvature
	 *
	 * compute the delta_h associated with placing one mountain
	 * 	find all points within the effective diameter
	 * 	compute the height as a function of the distance from center
	 */
	public static void placeMountain(Map map, double x, double y, double radius, double zMax, int shape, int mineral) {
		// figure out the shape coefficients
		int fullscale = Parameters.CYLINDRICAL;
		int midscale = fullscale/2;
		double Fcone, Fcirc, Fcyl;
		if (shape <= midscale) {
			Fcone = (double) (midscale - shape) / midscale;
			Fcirc = (double) shape / midscale;
			Fcyl = 0;
		} else {	// circ-flat
			Fcone = 0;
			Fcirc = (double) (fullscale - shape) / midscale;
			Fcyl =	(double) (shape - midscale) / midscale;
		}
		
		// see which points are within the scope of this mountain
		Mesh m = map.getMesh();
		double heights[] = map.getHeightMap();
		double soil[] = map.getSoilMap();
		MeshPoint centre = new MeshPoint(x,y);
		for(int i = 0; i < heights.length; i++) {
			MeshPoint p = m.vertices[i];
			double d = centre.distance(p);
			if (d > radius)
				continue;
			
			// calculate the deltaH for this point
			double dh_cone = (radius - d) * zMax / radius;
			double dh_circ = Math.cos(Math.PI*d/(4*radius)) * zMax;
			double dh_cyl = zMax;
			double delta_h = (Fcone * dh_cone) + (Fcirc * dh_circ) + (Fcyl * dh_cyl);
			// TODO: asymmetric mountain profiles

			// make sure the new height is legal
			double newZ = heights[i] + delta_h;
			if (newZ > Parameters.z_extent/2)
				heights[i] = Parameters.z_extent/2;
			else if (newZ < -Parameters.z_extent/2)
				heights[i] = -Parameters.z_extent/2;
			else
				heights[i] = newZ;
			
			// if mountain is tall enough, set the mineral type
			if (newZ > MIN_MOUNTAIN)
				soil[i] = mineral;
		}
	}
	
	/**
	 * @param Map
	 * @param x: (map) x coordinate
	 * @param y: (map) y coordinate
	 * @param radius: (map) radius (0 = full map)
	 * @param zMax: max (map) z value
	 * @param shape: curvature
	 *
	 * compute the delta_h associated with placing a ridge
	 * 	find all points within the effective elipse
	 * 	compute the height as a function of the distance from center
	 */
	public static void placeRidge(Map map, double x0, double y0, double x1, double y1,
			double radius, double zMax, int shape, int mineral) {
		// figure out the shape coefficients
		int fullscale = Parameters.CYLINDRICAL;
		int midscale = fullscale/2;
		double Fcone, Fcirc, Fcyl;
		if (shape <= midscale) {
			Fcone = (double) (midscale - shape) / midscale;
			Fcirc = (double) shape / midscale;
			Fcyl = 0;
		} else {	// circ-flat
			Fcone = 0;
			Fcirc = (double) (fullscale - shape) / midscale;
			Fcyl =	(double) (shape - midscale) / midscale;
		}
		
		// see which points are within the scope of this mountain
		Mesh m = map.getMesh();
		double heights[] = map.getHeightMap();
		double soil[] = map.getSoilMap();
		MeshPoint first = new MeshPoint(x0,y0);
		MeshPoint second = new MeshPoint(x1,y1);
		double minDist = first.distance(second);
		double maxDist = minDist + radius;
		
		for(int i = 0; i < heights.length; i++) {
			MeshPoint p = m.vertices[i];
			double d0 = first.distance(p);
			double d1 = second.distance(p);
			if (d0 + d1 > maxDist)
				continue;
			
			// calculate the deltaH for this point
			double dist = d0 + d1 - minDist;
			double dh_cone = (radius - dist) * zMax / radius;
			double dh_circ = Math.cos(Math.PI*dist/(4*radius)) * zMax;
			double dh_cyl = zMax;
			double delta_h = (Fcone * dh_cone) + (Fcirc * dh_circ) + (Fcyl * dh_cyl);
			// TODO: asymmetric mountain profiles

			// make sure the new height is legal
			double newZ = heights[i] + delta_h;
			if (newZ > Parameters.z_extent/2)
				heights[i] = Parameters.z_extent/2;
			else if (newZ < -Parameters.z_extent/2)
				heights[i] = -Parameters.z_extent/2;
			else
				heights[i] = newZ;
			
			// if mountain is tall enough, set the mineral type
			if (newZ > MIN_MOUNTAIN)
				soil[i] = mineral;
		}
	}

	/**
	 * compute the delta_h associated with this mountain range
	 * 	1. reset the height map to its initial value
	 * 	2. figure out how long the mountain range is
	 * 	3. figure out how many mountains it contains
	 *  4. place the mountains along the range
	 *  5. map.repaint
	 */
	private void redraw() {
		placed = "";	// reset the debug message
		
		// reset to the height map we started with
		for(int i = 0; i < oldHeight.length; i++)
			newHeight[i] = oldHeight[i];
		
		// convert screen coordinates into map coordinates
		double X0 = map.x(x_start);
		double Y0 = map.y(y_start);
		double X1 = map.x(x_end);
		double Y1 = map.y(y_end);

		// turn the diameter into map units
		double d = (double) diameter.getValue();
		if (d == 0)
			d = 1;
		d /= parms.xy_range * Parameters.x_extent;
		
		// figure out how long the mountain range is (in map coordinates)
		double l = Math.sqrt(((X1-X0)*(X1-X0)) + (Y1-Y0)*(Y1-Y0));
		double m = l/d;
		int mountains = (int) (m + 0.5);
		
		// get the height
		int alt = altitude.getValue();
		double z = (double) alt / parms.z_range *  Parameters.z_extent;
		
		// get the shape
		int shape = rounding.getValue();
		
		// TODO: piece-wise mountain ranges
		// how many mountains can we create
		if (mountains < 2) {
			// one mountain goes in the center (likely volcanic)
			int mineral = (shape < (Parameters.CONICAL + Parameters.SPHERICAL)/2) ? Map.IGNIOUS : Map.METAMORPHIC;
			placeMountain(map, (X0+X1)/2, (Y0+Y1)/2, d/2, z, shape, mineral);
			placed = "Placed " + parms.km(d) + Parameters.unit_xy + " wide, " +
					alt + Parameters.unit_z + " " + Map.soil_names[mineral] + " mountain at <" +
					String.format(POS_FMT, parms.latitude(X0+X1/2)) + "," + String.format(POS_FMT, parms.longitude(Y0+Y1/2)) + 
					"> shape=" + shape + "/" + Parameters.CYLINDRICAL + "\n";
		} else {
			placeRidge(map, X0, Y0, X1, Y1, d/2, z, shape, Map.METAMORPHIC);
			placed = "Placed " + parms.km(d) + Parameters.unit_xy + " wide, " +
					alt + Parameters.unit_z + " " +
					Map.soil_names[Map.METAMORPHIC] + " ridge from <" +
					String.format(POS_FMT, parms.latitude(X0)) + "," + String.format(POS_FMT, parms.longitude(Y0)) + "> to <" +
							String.format(POS_FMT, parms.latitude(X1)) + "," + String.format(POS_FMT, parms.longitude(Y1)) + 
							"> shape=" + shape + "/" + Parameters.CYLINDRICAL + "\n";
		}
		// tell the map about the update
		map.setHeightMap(newHeight);
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
	 * finish defining a mountain range
	 */
	public void mouseReleased(MouseEvent e) {
		x_end = e.getX();
		y_end = e.getY();
		selecting = false;
		selected = true;
		redraw();	
		// TODO: multiple segment mountain ranges
	}
	
	public void mouseClicked(MouseEvent e) {}
	
	/**
	 * progress in region selection
	 */
	public void mouseDragged(MouseEvent e) {
		if (selecting) {
			map.selectLine(x_start,  y_start,  e.getX(),  e.getY());
		}	
	}
	
	/**
	 * Window Close event handler ... implicit CANCEL
	 */
	public void windowClosing(WindowEvent e) {
		map.selectNone();
		if (oldHeight != null) {
			map.setHeightMap(oldHeight);
			map.repaint();
			oldHeight = null;
		}
		this.dispose();
		map.removeMouseListener(this);
		map.removeMouseMotionListener(this);
	}
	
	/**
	 * updates to the axis/inclination/profile sliders
	 */
	public void stateChanged(ChangeEvent e) {
			if (selected)
				redraw();
	}

	/**
	 * click events on ACCEPT/CANCEL buttons
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == cancel) {
			// revert to previous height map
			map.setHeightMap(oldHeight);
			map.repaint();
			oldHeight = null;
			
			// clean up the graphics
			map.selectNone();
			this.dispose();
			
			// un-register for mouse events
			map.removeMouseListener(this);
			map.removeMouseMotionListener(this);
		} else if (e.getSource() == accept) {
			// save the current values as defaults
			parms.dDiameter = diameter.getValue();
			parms.dAltitude = altitude.getValue();
			parms.dShape = rounding.getValue();
			
			// save a new copy of current height map
			for(int i = 0; i < oldHeight.length; i++)
				oldHeight[i] = newHeight[i];
			
			// clean up the selection graphics
			map.selectNone();
			selected = false;
			selecting = false;
			
			if (!placed.equals("") && parms.debug_level > 0) {
				System.out.print(placed);
				System.out.println("   max slope=" + String.format("%.4f", map.max_slope));
			}
			placed = "";
		}
		
	}
	
	// perfunctory methods
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