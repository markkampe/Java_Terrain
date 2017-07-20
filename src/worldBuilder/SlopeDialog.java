package worldBuilder;

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
	
	private int i_max;			// maximum inclination angle
	
	private JSlider axis;
	private JSlider inclination;
	private JButton accept;
	private JButton cancel;
	
	private int x0, y0, x1, y1;		// chosen slope axis
	private double Zscale = 0;		// chosen inclination
	
	private static final int BORDER_WIDTH = 5;
	
	private static final long serialVersionUID = 1L;
	
	public SlopeDialog(Map map)  {
		// pick up references
		this.map = map;
		this.oldMesh = map.getMesh();
		this.newMesh = new Mesh(this.oldMesh);
		map.setMesh(newMesh);
		this.parms = Parameters.getInstance();
		
		// figure out the maximum allowable slope
		i_max = (parms.z_range + parms.xy_range - 1)/ parms.xy_range;
		if (i_max % 2 != 0)
			i_max++;
		int i_tic = (i_max % 5 == 0) ? 10 : 8;
		
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
		JLabel axisLabel = new JLabel("Axis(deg)", JLabel.CENTER);
		axisLabel.setFont(fontLarge);

		
		inclination = new JSlider(JSlider.HORIZONTAL, -i_max, i_max, 0);
		inclination.setMajorTickSpacing(i_max/2);
		inclination.setMinorTickSpacing(i_max/i_tic);
		inclination.setFont(fontSmall);
		inclination.setPaintTicks(true);
		inclination.setPaintLabels(true);
		JLabel inclinationLabel = new JLabel("Slope(m/km)", JLabel.CENTER);
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
	 *
	 * @param inclination (-1.0 ... +1.0)
	 */
	public void incline(double inclination) {
		Zscale = (double) inclination;
		Parameters parms = Parameters.getInstance();
		int width = map.getWidth();
		int height = map.getHeight();
		double Xmid = parms.x_extent/2;
		double Ymid = parms.y_extent/2;
		double Zmid = parms.z_extent/2;
		
		// height of every point is its distance (+/-) from the axis
		for(int i = 0; i < newMesh.vertices.length; i++) {
			double X0 = (double) x0/width - Xmid;
			double Y0 = (double) y0/height - Ymid;
			double X1 = (double) x1/width - Xmid;
			double Y1 = (double) y1/height - Ymid;
			double d = newMesh.vertices[i].distanceLine(X0, Y0, X1, Y1);
			newMesh.vertices[i].z = Zscale * d;
		}
		map.repaint();
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
			y0 = 0;
			y1 = map.getHeight();
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
		map.select(x0, y0, x1-x0, y1-y0, Map.SEL_LINEAR);
		incline(Zscale);
	}
	
	/**
	 * Window Close event handler ... implicit CANCEL
	 */
	public void windowClosing(WindowEvent e) {
		map.select(0, 0, 0, 0,  Map.SEL_NONE);
		if (oldMesh != null) {
			map.setMesh(oldMesh);
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
				double i = inclination.getValue();
				incline(i/i_max);
		}	
	}

	/**
	 * click events on ACCEPT/CANCEL buttons
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == cancel) {
			map.select(0,0,0,0, Map.SEL_NONE);
			map.setMesh(oldMesh);
			map.repaint();
			oldMesh = null;
			this.dispose();
		} else if (e.getSource() == accept) {
			map.select(0,0,0,0, Map.SEL_NONE);
			oldMesh = null;	// don't need this anymore
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
