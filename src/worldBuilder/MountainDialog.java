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
public class MountainDialog extends JFrame implements ActionListener, ChangeListener, MouseListener, MouseMotionListener, ItemListener, KeyListener, WindowListener {	
	
	private Map map;
	private double[] oldHeight;	// per MeshPoint altitude at entry
	private double[] newHeight;	// edited per MeshPoint altitude
	private Parameters parms;
	
	private JCheckBox symmetric;
	private JComboBox<String> form;
	private JSlider altitude;
	private JSlider diameter1;
	private JSlider diameter2;
	private JSlider rounding1;
	private JSlider rounding2;
	private JButton accept;
	private JButton cancel;
	
	private boolean selecting;		// selection in progress
	private boolean selected;		// selection completed
	private int x_start, x_end, y_start, y_end;		// selection start/end coordinates
	
	private int d_max;				// diameter: full scale
	private int a_max;				// altitude: full scale
	
	/**
	 * a LandForm is a macro for a collection of mountain settings
	 */
	private static class LandForm {
		public String name;
		public int altitude;
		public int width;
		public int shape;
		
		private LandForm(String name, int shape, int width, int altitude) {
			this.name = name;
			this.altitude = altitude;
			this.width = width;
			this.shape = shape;
		}
	};
	
	// this is the list of known landforms
	private static final LandForm landforms[] = {
		//           name		shp	dia	altitude
		new LandForm("volcano",	0,	40,	1500),
		new LandForm("plateau",	8,	40,	500),
		new LandForm("caldera",	4,	20,	-250),
		new LandForm("canyon",	7,	1,	-300)
	};
	
	private String placed;			// debug message
	private static final String POS_FMT = "%.6f";
	
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
		accept = new JButton("ACCEPT (Enter)");
		cancel = new JButton("CANCEL (Esc)");
		
		symmetric = new JCheckBox("Symmetric");
		symmetric.setFont(fontLarge);
		symmetric.setSelected(true);
		
		form = new JComboBox<String>();
		JLabel formLabel = new JLabel("Land Form", JLabel.CENTER);
		formLabel.setFont(fontLarge);
		for(int i = 0; i < landforms.length; i++) {
			form.addItem(landforms[i].name);
		}
		form.setSelectedIndex(0);
		
		altitude = new JSlider(JSlider.HORIZONTAL, -this.a_max, this.a_max, parms.dAltitude);
		altitude.setMajorTickSpacing(Parameters.niceTics(-a_max, a_max, true));
		if (a_max > 10)
			altitude.setMinorTickSpacing(Parameters.niceTics(-a_max, a_max, false));
		altitude.setFont(fontSmall);
		altitude.setPaintTicks(true);
		altitude.setPaintLabels(true);
		JLabel altitudeLabel = new JLabel("Altitude(m)", JLabel.CENTER);
		altitudeLabel.setFont(fontLarge);

		diameter1 = new JSlider(JSlider.HORIZONTAL, 0, d_max, parms.dDiameter);
		diameter1.setMajorTickSpacing(Parameters.niceTics(0, d_max,true));
		if (d_max > 10)
			diameter1.setMinorTickSpacing(Parameters.niceTics(0, d_max,false));
		diameter1.setFont(fontSmall);
		diameter1.setPaintTicks(true);
		diameter1.setPaintLabels(true);
		JLabel diameter1Label = new JLabel("Top/Right Width (km)", JLabel.CENTER);
		diameter1Label.setFont(fontLarge);
		
		diameter2 = new JSlider(JSlider.HORIZONTAL, 0, d_max, parms.dDiameter);
		diameter2.setMajorTickSpacing(Parameters.niceTics(0, d_max,true));
		if (d_max > 10)
			diameter2.setMinorTickSpacing(Parameters.niceTics(0, d_max,false));
		diameter2.setFont(fontSmall);
		diameter2.setPaintTicks(true);
		diameter2.setPaintLabels(true);
		JLabel diameter2Label = new JLabel("Bottom/Left Width (km)", JLabel.CENTER);
		diameter2Label.setFont(fontLarge);
		
		rounding1 = new JSlider(JSlider.HORIZONTAL, Parameters.CONICAL, Parameters.CYLINDRICAL, parms.dShape);
		rounding1.setMajorTickSpacing(4);
		rounding1.setMinorTickSpacing(1);
		rounding1.setFont(fontSmall);
		rounding1.setPaintTicks(true);
		JLabel roundLabel1 = new JLabel("Profile (top/right)", JLabel.CENTER);
		roundLabel1.setFont(fontLarge);
		rounding2 = new JSlider(JSlider.HORIZONTAL, Parameters.CONICAL, Parameters.CYLINDRICAL, parms.dShape);
		rounding2.setMajorTickSpacing(4);
		rounding2.setMinorTickSpacing(1);
		rounding2.setFont(fontSmall);
		rounding2.setPaintTicks(true);
		JLabel roundLabel2 = new JLabel("Profile (bottom/left)", JLabel.CENTER);
		roundLabel2.setFont(fontLarge);
		
		Hashtable<Integer, JLabel> labels = new Hashtable<Integer, JLabel>();
		labels.put(Parameters.CONICAL, new JLabel("cone"));
		labels.put(Parameters.SPHERICAL, new JLabel("round"));
		labels.put(Parameters.CYLINDRICAL, new JLabel("flat"));
		rounding1.setLabelTable(labels);
		rounding1.setPaintLabels(true);
		rounding2.setLabelTable(labels);
		rounding2.setPaintLabels(true);
		
		/*
		 * Then pack all the controls into a 3x3 grid
		 */
		JPanel p0 = new JPanel();
		p0.add(symmetric);
		
		JPanel formPanel = new JPanel();
		formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.PAGE_AXIS));
		formPanel.add(formLabel);
		formPanel.add(form);
		formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 15));
		JPanel p6 = new JPanel();
		p6.add(formPanel);
		
		JPanel altPanel = new JPanel();
		altPanel.setLayout(new BoxLayout(altPanel, BoxLayout.PAGE_AXIS));
		altPanel.add(altitudeLabel);
		altPanel.add(altitude);
		
		JPanel diaPanel1 = new JPanel();
		diaPanel1.setLayout(new BoxLayout(diaPanel1, BoxLayout.PAGE_AXIS));
		diaPanel1.add(diameter1Label);
		diaPanel1.add(diameter1);
		
		JPanel diaPanel2 = new JPanel();
		diaPanel2.setLayout(new BoxLayout(diaPanel2, BoxLayout.PAGE_AXIS));
		diaPanel2.add(diameter2Label);
		diaPanel2.add(diameter2);
		
		JPanel rndPanel1 = new JPanel();
		rndPanel1.setLayout(new BoxLayout(rndPanel1, BoxLayout.PAGE_AXIS));
		rndPanel1.add(roundLabel1);
		rndPanel1.add(rounding1);
		
		JPanel rndPanel2 = new JPanel();
		rndPanel2.setLayout(new BoxLayout(rndPanel2, BoxLayout.PAGE_AXIS));
		rndPanel2.add(roundLabel2);
		rndPanel2.add(rounding2);
		
		JPanel p7 = new JPanel();
		p7.add(cancel);
		
		JPanel p8 = new JPanel();
		p8.add(accept);

		JPanel controls = new JPanel();
		controls.setLayout(new GridLayout(3,3));
		p0.setBorder(BorderFactory.createEmptyBorder(20, 15, 20, 15));
		altPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 15));
		diaPanel1.setBorder(BorderFactory.createEmptyBorder(10, 15, 0, 15));
		diaPanel2.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
		rndPanel1.setBorder(BorderFactory.createEmptyBorder(10, 15, 0, 10));
		rndPanel2.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 10));
		formPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
		p7.setBorder(BorderFactory.createEmptyBorder(20, 15, 20, 15));
		p8.setBorder(BorderFactory.createEmptyBorder(20, 15, 20, 15));
		controls.add(p0);
		controls.add(diaPanel1);
		controls.add(rndPanel1);
		controls.add(altPanel);
		controls.add(diaPanel2);
		controls.add(rndPanel2);
		controls.add(p6);
		controls.add(p7);
		controls.add(p8);
		
		mainPane.add(controls);
		
		pack();
		setVisible(true);
		
		// add the action listeners
		altitude.addChangeListener(this);
		diameter1.addChangeListener(this);
		diameter2.addChangeListener(this);
		rounding1.addChangeListener(this);
		rounding2.addChangeListener(this);
		accept.addActionListener(this);
		cancel.addActionListener(this);
		map.addMouseListener(this);
		map.addMouseMotionListener(this);
		map.addKeyListener(this);
		addKeyListener(this);
		symmetric.addItemListener(this);
		form.addActionListener(this);
		map.requestFocus();
		
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
	 * 
	 * NOTE:
	 * 	this is a static method, so it can be used w/o the dialog
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
		
		// figure out how high it has to be to pierce the sediment
		Parameters parms = Parameters.getInstance();	// this is a static method
		double minZ = parms.z(parms.sediment);
		
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
			
			// make sure the new height is legal
			double newZ = heights[i] + delta_h;
			if (newZ > Parameters.z_extent/2)
				heights[i] = Parameters.z_extent/2;
			else if (newZ < -Parameters.z_extent/2)
				heights[i] = -Parameters.z_extent/2;
			else
				heights[i] = newZ;
			
			// if mountain is tall enough, set the mineral type
			if (newZ > minZ)
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
	public void placeRidge(Map map, double x0, double y0, double x1, double y1,
			double radius1, double radius2, double zMax, 
			int shape1, int shape2, int mineral) {
		// figure out the shape coefficients
		int fullscale = Parameters.CYLINDRICAL;
		int midscale = fullscale/2;
		double fCone1, fCone2, fCirc1, fCirc2, fCyl1, fCyl2;
		if (shape1 <= midscale) {
			fCone1 = (double) (midscale - shape1) / midscale;
			fCirc1 = (double) shape1 / midscale;
			fCyl1 = 0;
		} else {	// circ-flat
			fCone1 = 0;
			fCirc1 = (double) (fullscale - shape1) / midscale;
			fCyl1 =	(double) (shape1 - midscale) / midscale;
		}
		if (shape2 <= midscale) {
			fCone2 = (double) (midscale - shape2) / midscale;
			fCirc2 = (double) shape2 / midscale;
			fCyl2 = 0;
		} else {	// circ-flat
			fCone2 = 0;
			fCirc2 = (double) (fullscale - shape2) / midscale;
			fCyl2 =	(double) (shape2 - midscale) / midscale;
		}
		
		// see which points are within the scope of this mountain
		Mesh m = map.getMesh();
		double heights[] = map.getHeightMap();
		double soil[] = map.getSoilMap();
		MeshPoint first = new MeshPoint(x0,y0);
		MeshPoint second = new MeshPoint(x1,y1);
		double minDist = first.distance(second);
		double maxDist1 = minDist + radius1;
		double maxDist2 = minDist + radius2;
		double minZ = parms.z(parms.sediment);
		
		for(int i = 0; i < heights.length; i++) {
			MeshPoint p = m.vertices[i];
			double d0 = first.distance(p);
			double d1 = second.distance(p);
			double d2 = p.distanceLine(x0,  y0,  x1,  y1);
			double max = (d2 > 0) ? maxDist1 : maxDist2;
			double radius = (d2 > 0) ? radius1 : radius2;
			if (d0 + d1 > max)
				continue;
			
			// SOMEDAY add rectangular ridges
			// SOMEDAY add continued off-map ridges
			// 		peak at edge rather than in radius from edge
			
			// calculate the deltaH for this point
			double dist = d0 + d1 - minDist;
			double dh_cone = (radius - dist) * zMax / radius;
			double dh_circ = Math.cos(Math.PI*dist/(4*radius)) * zMax;
			double dh_cyl = zMax;
			double delta_h1 = (fCone1 * dh_cone) + (fCirc1 * dh_circ) + (fCyl1 * dh_cyl);
			double delta_h2 = (fCone2 * dh_cone) + (fCirc2 * dh_circ) + (fCyl2 * dh_cyl);
			double delta_h = (d2 > 0) ? delta_h1 : delta_h2;

			// make sure the new height is legal
			double newZ = heights[i] + delta_h;
			if (newZ > Parameters.z_extent/2)
				heights[i] = Parameters.z_extent/2;
			else if (newZ < -Parameters.z_extent/2)
				heights[i] = -Parameters.z_extent/2;
			else
				heights[i] = newZ;
			
			// if rise pierces the sediment, set mineral type
			if (newZ > minZ)
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
		double X0 = map.map_x(x_start);
		double Y0 = map.map_y(y_start);
		double X1 = map.map_x(x_end);
		double Y1 = map.map_y(y_end);

		// turn the diameter into map units
		double d1 = (double) diameter1.getValue();
		if (d1 == 0)
			d1 = 1;
		d1 = parms.x(d1);
		double d2 = (double) diameter2.getValue();
		if (d2 == 0)
			d2 = 1;
		d2 = parms.x(d2);
		
		// figure out how long the mountain range is (in map coordinates)
		double l = Math.sqrt(((X1-X0)*(X1-X0)) + (Y1-Y0)*(Y1-Y0));
		double m = 2 * l/(d1 + d2);
		int mountains = (int) (m + 0.5);
		
		// get the height
		int alt = altitude.getValue();
		double z = parms.z((double) alt);
		
		// get the shape
		int shape1 = rounding1.getValue();
		int shape2 = rounding2.getValue();
		
		// how many mountains can we create
		if (mountains < 2) {
			// one mountain goes in the center (likely volcanic)
			int composition = (shape1 <= (Parameters.CONICAL + Parameters.SPHERICAL)/2) ? Map.IGNEOUS : Map.METAMORPHIC;
			placeMountain(map, (X0+X1)/2, (Y0+Y1)/2, d1/2, z, shape1, composition);
			String form;
			if (z < 0)
				form = "caldera";
			else if (shape1 >= (Parameters.SPHERICAL + Parameters.CYLINDRICAL)/2)
				form = "plateau";
			else
				form = "mountain";
			placed = "Placed " + parms.km(d1) + Parameters.unit_xy + " wide, " +
					alt + Parameters.unit_z + " " + Map.soil_names[composition] + " " + form + " at <" +
					String.format(POS_FMT, parms.latitude(X0+X1/2)) + "," + String.format(POS_FMT, parms.longitude(Y0+Y1/2)) + 
					"> shape=" + shape1 + "/" + Parameters.CYLINDRICAL + "\n";
		} else {
			int composition = (alt > 0) ? Map.METAMORPHIC : Map.SEDIMENTARY;
			placeRidge(map, X0, Y0, X1, Y1, d1/2, d2/2, z, shape1, shape2, composition);
			String form = (alt > 0) ? "ridge" : "trench";
			placed = "Placed " + parms.km(d1+d2)/2 + Parameters.unit_xy + " wide, " +
					alt + Parameters.unit_z + " " +
					Map.soil_names[composition] + " " + form + " from <" +
					String.format(POS_FMT, parms.latitude(X0)) + "," + String.format(POS_FMT, parms.longitude(Y0)) + "> to <" +
							String.format(POS_FMT, parms.latitude(X1)) + "," + String.format(POS_FMT, parms.longitude(Y1)) + 
							"> shape=" + shape1 + "," + shape2 + "/" + Parameters.CYLINDRICAL + "\n";
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
		map.selectNone();
		redraw();	
	}
	
	public void mouseClicked(MouseEvent e) {
		x_start = e.getX();
		y_start = e.getY();
		x_end = x_start + 1;
		y_end = y_start + 1;
		selecting = false;
		selected = true;
		map.selectNone();
		redraw();
	}
	
	/**
	 * progress in region selection
	 */
	public void mouseDragged(MouseEvent e) {
		if (selecting) {
			map.selectLine(x_start,  y_start,  e.getX(),  e.getY());
		}	
	}
	
	/**
	 * restore previous height map exit dialog
	 */
	private void cancelDialog() {
		map.selectNone();
		if (oldHeight != null) {
			map.setHeightMap(oldHeight);
			map.repaint();
			oldHeight = null;
		}
		this.dispose();
		map.removeMouseListener(this);
		map.removeMouseMotionListener(this);
		map.removeKeyListener(this);
		WorldBuilder.activeDialog = false;
	}
	
	/**
	 * make the most recently created mountain official
	 */
	private void acceptMountain() {
		// save the current values as defaults
		parms.dDiameter = diameter1.getValue();
		parms.dAltitude = altitude.getValue();
		parms.dShape = rounding1.getValue();
		
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
				
	/**
	 * Window Close event handler ... implicit CANCEL
	 */
	public void windowClosing(WindowEvent e) {
		cancelDialog();
	}
	
	/**
	 * updates to the diameter/rounding sliders
	 */
	public void stateChanged(ChangeEvent e) {
			if (symmetric.isSelected()) {
				if (e.getSource() == rounding1)
					rounding2.setValue(rounding1.getValue());
				if (e.getSource() == rounding2)
					rounding1.setValue(rounding2.getValue());
				if (e.getSource() == diameter1)
					diameter2.setValue(diameter1.getValue());
				if (e.getSource() == diameter2)
					diameter1.setValue(diameter2.getValue());
			}
			if (selected)
				redraw();
	}
	
	/**
	 * look for ENTER or ESC
	 */
	public void keyTyped(KeyEvent e) {
		int key = e.getKeyChar();
		if (key == KeyEvent.VK_ENTER && selected)
			acceptMountain();
		else if (key == KeyEvent.VK_ESCAPE)
			cancelDialog();	
	}

	/**
	 * click events on ACCEPT/CANCEL buttons or form selector
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == cancel) {
			cancelDialog();
		} else if (e.getSource() == accept && selected) {
			acceptMountain();
		} else if (e.getSource() == form) {
			int x = form.getSelectedIndex();
			altitude.setValue(landforms[x].altitude);
			rounding1.setValue(landforms[x].shape);
			rounding2.setValue(landforms[x].shape);
			diameter1.setValue(landforms[x].width);
			diameter1.setValue(landforms[x].width);
			if (selected)
				redraw();
		}
	}
	
	/**
	 * symmetric checkbox has changed state
	 * 
	 * @param e
	 */
	public void itemStateChanged(ItemEvent e) {
		if (symmetric.isSelected()) {
			diameter2.setValue(diameter1.getValue());
			rounding2.setValue(rounding1.getValue());
			
			if (selected)
				redraw();
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
	public void keyPressed(KeyEvent arg0) {}
	public void keyReleased(KeyEvent arg0) {}
	
	
}