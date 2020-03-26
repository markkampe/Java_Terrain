package worldBuilder;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

/**
 * Dialog to choose axis and inclination for a constant slope to the map.
 */
public class SlopeDialog extends JFrame implements ActionListener, ChangeListener, WindowListener {	
	private Map map;
	private double[] oldHeight;	// per MeshPoint altitude at entry
	private double[] newHeight;	// edited per MeshPoint altitude
	private Parameters parms;
	
	private int i_max;			// maximum inclination
	
	private JSlider axis;
	private JSlider inclination;
	private JButton accept;
	private JButton cancel;
	
	private int x0, y0, x1, y1;		// chosen slope axis
	
	private static final long serialVersionUID = 1L;
	
	public SlopeDialog(Map map)  {
		// pick up references
		this.map = map;
		this.oldHeight = map.getHeightMap();
		this.parms = Parameters.getInstance();
		
		// copy the incoming height map
		this.newHeight = new double[oldHeight.length];
		for(int i = 0; i < oldHeight.length; i++)
			newHeight[i] = oldHeight[i];
		map.setHeightMap(newHeight);
		
		// create the dialog box
		Container mainPane = getContentPane();
		int border = parms.dialogBorder;
		((JComponent) mainPane).setBorder(BorderFactory.createMatteBorder(border, border, border, border, Color.LIGHT_GRAY));
		setTitle("Define Whole-Map Slope");
		addWindowListener( this );
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		
		// create the basic widgets
		Font fontSmall = new Font("Serif", Font.ITALIC, 10);
		Font fontLarge = new Font("Serif", Font.ITALIC, 15);
		accept = new JButton("ACCEPT");
		cancel = new JButton("CANCEL");
		
		axis = new JSlider(JSlider.HORIZONTAL, -90, 90, 0);
		axis.setMajorTickSpacing(Parameters.niceTics(-90, 90, true));
		axis.setMinorTickSpacing(Parameters.niceTics(-90, 90, false));
		axis.setFont(fontSmall);
		axis.setPaintTicks(true);
		axis.setPaintLabels(true);
		JLabel axisLabel = new JLabel("Axis(deg)", JLabel.CENTER);
		axisLabel.setFont(fontLarge);

		i_max = (100 * parms.z_range)/ parms.xy_range;	// max possible cm/km
		i_max /= 5;			// continental slope is much less than that
		inclination = new JSlider(JSlider.HORIZONTAL, -i_max, i_max, 0);
		inclination.setMajorTickSpacing(Parameters.niceTics(-i_max, i_max, true));
		inclination.setMinorTickSpacing(Parameters.niceTics(-i_max, i_max, false));
		inclination.setFont(fontSmall);
		inclination.setPaintTicks(true);
		inclination.setPaintLabels(true);
		String label = "Slope(" + Parameters.unit_s + ")";
		JLabel inclinationLabel = new JLabel(label, JLabel.CENTER);
		inclinationLabel.setFont(fontLarge);
		
		/*
		 * Pack them into:
		 * 		a vertical Box layout containing sliders and buttons
		 * 		sliders are a 1x2 grid layout
		 * 			each being a vertical Box w/label and slider
		 * 		buttons a horizontal Box layout
		 */
		JPanel axisPanel = new JPanel();
		axisPanel.setLayout(new BoxLayout(axisPanel, BoxLayout.PAGE_AXIS));
		axisPanel.add(axisLabel);
		axisPanel.add(axis);
		
		JPanel inclinationPanel = new JPanel();
		inclinationPanel.setLayout(new BoxLayout(inclinationPanel, BoxLayout.PAGE_AXIS));
		inclinationPanel.add(inclinationLabel);
		inclinationPanel.add(inclination);

		JPanel sliders = new JPanel();
		sliders.setLayout(new BoxLayout(sliders, BoxLayout.LINE_AXIS));
		axisPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 15));
		inclinationPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 0, 10));
		sliders.add(axisPanel);
		sliders.add(inclinationPanel);
		
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
		axis.addChangeListener(this);
		inclination.addChangeListener(this);
		accept.addActionListener(this);
		cancel.addActionListener(this);
		
		// disable selection
		map.selectMode(Map.Selection.NONE);
		
		// initialize the slope axis
		setAxis(0);
	}
	
	/**
	 * incline the entire map plane
	 *
	 * @param inclination (cm/km)
	 */
	public void incline(double inclination) {
		// convert inclination from word to map units
		double Zscale = inclination;
		Zscale *= parms.xy_range;	// slope times distance
		Zscale /= 100;				// cm->m
		Zscale /= parms.z_range;	// scaled to Z range
		
		// get map and display parameters
		Mesh m = map.getMesh();

		// height of every point is its distance (+/-) from the axis
		for(int i = 0; i < newHeight.length; i++) {
			double X0 = map.map_x(x0);
			double Y0 = map.map_y(y0);
			double X1 = map.map_x(x1);
			double Y1 = map.map_y(y1);
			double d = m.vertices[i].distanceLine(X0, Y0, X1, Y1);
			
			// make sure the new height is legal
			double newZ = Zscale * d + oldHeight[i];
			if (newZ > Parameters.z_extent/2)
				newHeight[i] = Parameters.z_extent/2;
			else if (newZ < -Parameters.z_extent/2)
				newHeight[i] = -Parameters.z_extent/2;
			else
				newHeight[i] = newZ;
		}
		
		// tell the map about the update
		map.setHeightMap(newHeight);
	}

	/**
	 * display slope axis as a select line
	 * 
	 * @param angle (0 = horizontal)
	 */
	private void setAxis(int degrees) {
		
		// figure out line center and length
		int x_center = map.getWidth()/2;
		int x_len = 3 * x_center / 2;
		int y_center = map.getHeight()/2;
		int y_len = 3 * y_center / 2;
		
		// vertical lines are a special case
		if (degrees == -90 || degrees == 90) {
			x0 = x_center;
			x1 = x_center;
			y0 = map.getHeight()/8;
			y1 = map.getHeight()*7/8;
		} else {
			double radians = Math.PI * ((double) degrees)/180;
			double sin = Math.sin(radians);
			double cos = Math.cos(radians);
			double dy = sin * y_len / 2;
			double dx = cos * x_len / 2;
			x0 = x_center - (int) dx;
			y0 = y_center - (int) dy;
			x1 = x_center + (int) dx;
			y1 = y_center + (int) dy;
		}
		
		// display the slope axis
		map.selectLine(x0, y0, x1, y1);
		incline(inclination.getValue());
	}
	
	/**
	 * Window Close event handler ... implicit CANCEL
	 */
	public void windowClosing(WindowEvent e) {
		map.selectMode(Map.Selection.ANY);
		if (oldHeight != null) {
			map.setHeightMap(oldHeight);
			map.repaint();
		}
		this.dispose();
	}
	
	/**
	 * updates to the axis/inclination sliders
	 */
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == axis) {
				setAxis(axis.getValue());
		} else if (e.getSource() == inclination) {
				incline(inclination.getValue());
		}	
	}

	/**
	 * click events on ACCEPT/CANCEL buttons
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == cancel) {
			map.selectMode(Map.Selection.ANY);
			map.setHeightMap(oldHeight);
			map.repaint();
			oldHeight = null;
			this.dispose();
		} else if (e.getSource() == accept) {
			map.selectMode(Map.Selection.ANY);
			int dir = axis.getValue();
			int slope = inclination.getValue();
			oldHeight = null;	// don't need this anymore
			this.dispose();
			
			if (parms.debug_level > 0) {
				final int cm_per_km = 100000;
				System.out.println("Incline continent: axis=" + dir + ", slope=" + slope + Parameters.unit_s +
						", max slope=" + String.format("%.1f", map.max_slope * cm_per_km) + Parameters.unit_s);
			}
			
			// by default, weather comes from off-shore
			if (dir >= 0)
				if (slope >= 0)
					parms.dDirection = dir - 180;
				else
					parms.dDirection = dir;
			else
				if (slope >= 0)
					parms.dDirection = dir + 180;
				else
					parms.dDirection = dir;
			
		}
	}

	public void windowActivated(WindowEvent arg0) {}
	public void windowClosed(WindowEvent arg0) {}
	public void windowDeactivated(WindowEvent arg0) {}
	public void windowDeiconified(WindowEvent arg0) {}
	public void windowIconified(WindowEvent arg0) {}
	public void windowOpened(WindowEvent arg0) {}	
}
