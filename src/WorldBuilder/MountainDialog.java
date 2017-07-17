package WorldBuilder;

import java.awt.Color;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
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
public class MountainDialog extends JFrame implements MouseMotionListener, MouseListener, ActionListener, ChangeListener, WindowListener {	
	private Map map;
	private Mesh oldMesh;
	private Mesh newMesh;
	private Parameters parms;
	
	private JSlider altitude;
	private JSlider diameter;
	private JButton accept;
	private JButton cancel;
	
	private int select_x;	// X coordinate of press
	private int select_y;	// Y coordinate of press
	private int select_type;	// type of selection in progress
	
	private static final int BORDER_WIDTH = 5;
	
	private static final long serialVersionUID = 1L;
	
	public MountainDialog(Map map)  {
		// pick up references
		this.map = map;
		this.oldMesh = map.getMesh();
		this.newMesh = new Mesh(this.oldMesh);
		this.parms = Parameters.getInstance();
		
		// register to receive mouse events (for selection)
		map.addMouseMotionListener(this);
		map.addMouseListener(this);
		
		// create the dialog box
		Container mainPane = getContentPane();
		((JComponent) mainPane).setBorder(BorderFactory.createMatteBorder(BORDER_WIDTH, BORDER_WIDTH, BORDER_WIDTH, BORDER_WIDTH, Color.LIGHT_GRAY));
		setTitle("Define Mountain/Range");
		addWindowListener( this );
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		
		// create the widgets
		altitude = new JSlider();
		altitude.addChangeListener(this);
		diameter = new JSlider();
		diameter.addChangeListener(this);
		accept = new JButton("ACCEPT");
		accept.addActionListener(this);
		cancel = new JButton("CANCEL");
		cancel.addActionListener(this);
		
		// assemble them all into a dialog box
		JPanel controls = new JPanel(new GridLayout(3,2));
		controls.add(altitude);
		controls.add(diameter);
		controls.add(new JLabel("Height", JLabel.CENTER));
		controls.add(new JLabel("Diameter", JLabel.CENTER));
		controls.add(cancel);
		controls.add(accept);
		mainPane.add(controls);
		
		// display the dialog box
		pack();
		setVisible(true);
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
	 * mouse-press ... start a selected area definition
	 */
	public void mousePressed(MouseEvent e) {
		select_x = e.getX();
		select_y = e.getY();
		select_type = Map.SEL_LINEAR;
	}

	/**
	 * mouse release ... create the mountains
	 */
	public void mouseReleased(MouseEvent arg0) {
		select_type = Map.SEL_NONE;
		map.select(0, 0, 0, 0, Map.SEL_NONE);
		// TODO draw the mountains
	}

	/**
	 * mouse drag ... update the selection line
	 */
	public void mouseDragged(MouseEvent e) {
		int dx = e.getX() - select_x;
		int dy = e.getY() - select_y;
		map.select(select_x,  select_y, dx, dy, select_type);
	}

	/**
	 * slider adjustments to altitude and diameter
	 */
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == altitude) {
			System.out.println("altitude = " + altitude.getValue());
		} else if (e.getSource() == diameter) {
			System.out.println("diameter = " + diameter.getValue());
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


	public void mouseClicked(MouseEvent arg0) {}
	public void mouseEntered(MouseEvent arg0) {}
	public void mouseExited(MouseEvent arg0) {}
	public void mouseMoved(MouseEvent arg0) {}


	


	
}
