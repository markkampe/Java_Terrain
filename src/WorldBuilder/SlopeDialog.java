package WorldBuilder;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

/**
 * SlopeDialog allows the user to choose an axis and inclination to
 * cause a uniform slope to the entire map.
 */
public class SlopeDialog extends JFrame implements ActionListener, ChangeListener, WindowListener {	
	private Map map;
	private Mesh oldMesh;
	private Mesh newMesh;
	private Parameters parms;
	
	private JSlider axis;
	private JSlider inclination;
	private JButton accept;
	private JButton cancel;
	
	private int x0, y0, x1, y1;		// chosen slope axis
	
	private static final int BORDER_WIDTH = 5;
	
	private static final long serialVersionUID = 1L;
	
	public SlopeDialog(Map map)  {
		// pick up references
		this.map = map;
		this.oldMesh = map.getMesh();
		this.newMesh = new Mesh(this.oldMesh);
		this.parms = Parameters.getInstance();
		
		// create the dialog box
		Container mainPane = getContentPane();
		((JComponent) mainPane).setBorder(BorderFactory.createMatteBorder(BORDER_WIDTH, BORDER_WIDTH, BORDER_WIDTH, BORDER_WIDTH, Color.LIGHT_GRAY));
		setTitle("Define Whole-Map Slope");
		addWindowListener( this );
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		
		// create the basic widgets
		Font fontSmall = new Font("Serif", Font.ITALIC, 10);
		Font fontLarge = new Font("Serif", Font.ITALIC, 15);
		accept = new JButton("ACCEPT");
		cancel = new JButton("CANCEL");
		
		axis = new JSlider(JSlider.HORIZONTAL, -90, 90, 0);
		axis.setMajorTickSpacing(45);
		axis.setMinorTickSpacing(10);
		axis.setFont(fontSmall);
		axis.setPaintTicks(true);
		axis.setPaintLabels(true);
		JLabel axisLabel = new JLabel("Axis", JLabel.CENTER);
		axisLabel.setFont(fontLarge);

		
		inclination = new JSlider(JSlider.HORIZONTAL, -50, 50, 0);
		inclination.setMajorTickSpacing(25);
		inclination.setMinorTickSpacing(10);
		inclination.setFont(fontSmall);
		inclination.setPaintTicks(true);
		inclination.setPaintLabels(true);
		JLabel inclinationLabel = new JLabel("Inclination", JLabel.CENTER);
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
		
		// initialize the slope axis
		setAxis(0);
	}
	
	/**
	 * incline the entire map plane
	 * @param m		coordinate mesh
	 * @param slope (dy/dx)
	 * @param inclination (0-1.0)
	 */
	public static void incline(Mesh m, double slope, double inclination) {
		Parameters parms = Parameters.getInstance();
		
		// FIX ... for now, the axis is a horizontal line
		double a, b, c = 0;
		if (slope == 0) {
			a = 0;
			b = 1;
		} else if (slope > 1) {
			a = slope;
			b = -1;
		} else {
			a = -1;
			b = slope;
		}
		
		// height of every point is its distance (+/-) from the axis
		for(int i = 0; i < m.vertices.length; i++) {
			double z = m.vertices[i].distanceLine(a, b, c);
			z *= inclination * (parms.z_extent/2);
			m.vertices[i].z = z;
		}
	}

	/**
	 * display slope axis as a select line
	 * 
	 * @param angle (0 = horizontal)
	 */
	private void setAxis(int degrees) {
		
		// figure out the corners of my box
		int x_left = map.getWidth()/6;
		int x_right = 5 * x_left;
		int x_center = (x_left + x_right)/2;
		int x_len = 4 * x_left;
		int y_top = map.getHeight()/6;
		int y_bot = 5 * y_top;
		int y_center = (y_top + y_bot)/2;
		int y_len = 4 * y_top;
		
		// vertical lines are a special case
		if (degrees == -90 || degrees == 90) {
			x0 = x_center;
			x1 = x_center;
			y0 = y_top;
			y1 = y_bot;
		} else if (degrees == 0){
			x0 = x_left;
			x1 = x_right;
			y0 = x_center;
			y1 = x_center;
		} else {
			double radians = Math.PI * ((double) degrees)/180;
			double sin = Math.sin(radians);
			double cos = Math.cos(radians);
			double dy = -sin * y_len / (2 * cos);
			double dx = cos * x_len / (2 * sin);
			x0 = x_center - (int) dx;
			x1 = x_center + (int) dx;
			y0 = y_center - (int) dy;
			y1 = y_center + (int) dy;
		}
	
		// display the slope axis
		map.select(x0, y0, x1-x0, y1-y0, Map.SEL_LINEAR);
	}
	
	/**
	 * Window Close event handler ... implicit CANCEL
	 */
	public void windowClosing(WindowEvent e) {
		System.out.println("Closing Slope Dialog");
		map.select(0, 0, 0, 0,  Map.SEL_NONE);
		this.dispose();
	}
	
	/**
	 * updates to the axis/inclination sliders
	 */
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == axis) {
				setAxis(axis.getValue());
		} else if (e.getSource() == inclination) {
			System.out.println("inclination = " + inclination.getValue());
		}	
	}

	/**
	 * click events on ACCEPT/CANCEL buttons
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == cancel) {
			map.select(0,0,0,0, Map.SEL_NONE);
			System.out.println("CANCEL slope");
			this.dispose();
		} else if (e.getSource() == accept) {
			map.select(0,0,0,0, Map.SEL_NONE);
			System.out.println("ACCEPT slope");
			this.dispose();
		}
	}

	public void windowActivated(WindowEvent arg0) {}
	public void windowClosed(WindowEvent arg0) {}
	public void windowDeactivated(WindowEvent arg0) {}
	public void windowDeiconified(WindowEvent arg0) {}
	public void windowIconified(WindowEvent arg0) {}
	public void windowOpened(WindowEvent arg0) {}	
}
