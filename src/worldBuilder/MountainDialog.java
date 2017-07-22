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
	private Map map;
	private Mesh oldMesh;
	private Mesh newMesh;
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
	
	private static final int BORDER_WIDTH = 5;
	
	private static final long serialVersionUID = 1L;
	
	public MountainDialog(Map map)  {
		// pick up references
		this.map = map;
		this.oldMesh = map.getMesh();
		this.newMesh = new Mesh(this.oldMesh);
		map.setMesh(newMesh);
		this.parms = Parameters.getInstance();
		
		// calibrate full scale on the sliders
		this.a_max = parms.z_range/2;
		this.d_max = parms.maxDiameter();
	
		// create the dialog box
		Container mainPane = getContentPane();
		((JComponent) mainPane).setBorder(BorderFactory.createMatteBorder(BORDER_WIDTH, BORDER_WIDTH, BORDER_WIDTH, BORDER_WIDTH, Color.LIGHT_GRAY));
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
		
		rounding = new JSlider(JSlider.HORIZONTAL, Parameters.CONICAL, Parameters.SPHERICAL, parms.dShape);
		rounding.setMajorTickSpacing(4);
		rounding.setMinorTickSpacing(1);
		rounding.setFont(fontSmall);
		rounding.setPaintTicks(true);
		JLabel roundLabel = new JLabel("Profile", JLabel.CENTER);
		roundLabel.setFont(fontLarge);
		
		Hashtable<Integer, JLabel> labels = new Hashtable<Integer, JLabel>();
		labels.put(Parameters.CONICAL, new JLabel("cone"));
		labels.put(Parameters.SPHERICAL, new JLabel("circle"));
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
	 * compute the delta_h associated with placing one mountain
	 * 	find all points within the effective diameter
	 * 	compute the height as a function of the distance from center
	 */
	private void placeMountain(double x, double y) {
		// note the mountain parameters
		double alt = (double) altitude.getValue() / (parms.z_range/2) * parms.z_extent;
		int Fcone = Parameters.SPHERICAL - rounding.getValue();
		int Fcirc = rounding.getValue();
		double diam = (double) diameter.getValue();
		if (diam == 0)
				diam = 1;
		diam /= parms.xy_range * parms.x_extent;
		
		// see which points are within the scope of this mountain
		MapPoint centre = new MapPoint(x,y);
		for(int i = 0; i < newMesh.vertices.length; i++) {
			MapPoint p = newMesh.vertices[i];
			double d = centre.distance(p);
			if (d > diam)
				continue;
			
			// calculate the deltaH for this point
			double dh_cone = (diam - d) * alt / diam;
			double dh_circ = Math.cos(Math.PI*d/(4*diam)) * alt;
			double delta_h = ((Fcone * dh_cone) + (Fcirc * dh_circ))/Parameters.SPHERICAL;

			// make sure the new height is legal
			double newZ = p.z + delta_h;
			if (newZ > parms.z_extent/2)
				p.z = parms.z_extent/2;
			else if (newZ < -parms.z_extent/2)
				p.z = -parms.z_extent/2;
			else
				p.z = newZ;
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
		// reset to the height map we started with
		for(int i = 0; i < oldMesh.vertices.length; i++)
			newMesh.vertices[i].z = oldMesh.vertices[i].z;
		
		// convert screen coordinates into map coordinates
		double x_mid = parms.x_extent/2;
		double y_mid = parms.y_extent/2;
		int width = map.getWidth();
		int height = map.getHeight();
		double X0 = (double) x_start/width - x_mid;
		double Y0 = (double) y_start/height - y_mid;
		double X1 = (double) x_end/width - x_mid;
		double Y1 = (double) y_end/height - y_mid;

		// figure out how long the mountain range is (in map coordinates)
		double l = Math.sqrt(((X1-X0)*(X1-X0)) + (Y1-Y0)*(Y1-Y0));
		double d = (double) diameter.getValue();
		if (d == 0)
			d = 1;
		d /= parms.xy_range;
		double m = l/d;
		int mountains = (int) (m + 0.5);
		
		// how many mountains can we create
		if (mountains < 2) {
			// one mountain goes in the center
			placeMountain((X0+X1)/2, (Y0+Y1)/2);
		} else {
			// multiple mountains are evenly spaced along the line
			double X = X0;
			double Y = Y0;
			double dx = (X1 - X0)/mountains;
			double dy = (Y1 - Y0)/mountains;
			while( mountains >= 0 ) {
				placeMountain(X, Y);
				X += dx;
				Y += dy;
				mountains -= 1;
			}
		}
		map.repaint();
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
		if (oldMesh != null) {
			map.setMesh(oldMesh);
			map.repaint();
			oldMesh = null;
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
			map.setMesh(oldMesh);
			map.repaint();
			oldMesh = null;
			// clean up the graphics
			map.selectNone();
			this.dispose();
		} else if (e.getSource() == accept) {
			// save the current values as defaults
			parms.dDiameter = diameter.getValue();
			parms.dAltitude = altitude.getValue();
			parms.dShape = rounding.getValue();
			// discard previous height map
			oldMesh = null;
			// clean up the graphics
			map.selectNone();
			this.dispose();
		}
		map.removeMouseListener(this);
		map.removeMouseMotionListener(this);
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