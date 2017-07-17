package WorldBuilder;

import java.awt.Color;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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
		
		// create the widgets
		accept = new JButton("ACCEPT");
		accept.addActionListener(this);
		cancel = new JButton("CANCEL");
		cancel.addActionListener(this);
		axis = new JSlider();
		axis.addChangeListener(this);
		inclination = new JSlider();
		inclination.addChangeListener(this);
		
		// assemble them all into a dialog box
		JPanel controls = new JPanel(new GridLayout(3,2));
		controls.add(axis);
		controls.add(inclination);
		controls.add(new JLabel("Axis", JLabel.CENTER));
		controls.add(new JLabel("Inclination", JLabel.CENTER));
		controls.add(cancel);
		controls.add(accept);
		mainPane.add(controls);
		
		// display the dialog ox
		pack();
		setVisible(true);
		
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
	 * @param slope
	 */
	private void setAxis(double slope) {
		int x_mid = map.getWidth()/2;
		int y_mid = map.getHeight()/2;
		// FIX this is broken
		int x_start = x_mid/2;
		int dx = x_mid;
		int y_start = y_mid;
		int dy = 0;
		
		map.select(x_start,  y_start, dx, dy, Map.SEL_LINEAR);
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
			System.out.println("axis = " + axis.getValue());
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
