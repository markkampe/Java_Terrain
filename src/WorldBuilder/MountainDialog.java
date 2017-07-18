package WorldBuilder;

import java.awt.*;
import java.awt.event.*;

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
	private JButton accept;
	private JButton cancel;
	
	private boolean selecting;		// selection in progress
	private int x_start, x_end, y_start, y_end;		// selection start/end coordinates
	
	private static final int BORDER_WIDTH = 5;
	
	private static final long serialVersionUID = 1L;
	
	public MountainDialog(Map map)  {
		// pick up references
		this.map = map;
		this.oldMesh = map.getMesh();
		this.newMesh = new Mesh(this.oldMesh);
		map.setMesh(newMesh);
		this.parms = Parameters.getInstance();
		
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
		
		altitude = new JSlider(JSlider.HORIZONTAL, -5000, 5000, 0);
		altitude.setMajorTickSpacing(5000);
		altitude.setMinorTickSpacing(1000);
		altitude.setFont(fontSmall);
		altitude.setPaintTicks(true);
		altitude.setPaintLabels(true);
		JLabel altitudeLabel = new JLabel("Altitude", JLabel.CENTER);
		altitudeLabel.setFont(fontLarge);

		diameter = new JSlider(JSlider.HORIZONTAL, 0, 500, 50);
		diameter.setMajorTickSpacing(100);
		diameter.setMinorTickSpacing(50);
		diameter.setFont(fontSmall);
		diameter.setPaintTicks(true);
		diameter.setPaintLabels(true);
		JLabel diameterLabel = new JLabel("Diameter", JLabel.CENTER);
		diameterLabel.setFont(fontLarge);
		
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

		JPanel sliders = new JPanel();
		sliders.setLayout(new BoxLayout(sliders, BoxLayout.LINE_AXIS));
		altPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 15));
		diaPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 0, 10));
		sliders.add(altPanel);
		sliders.add(diaPanel);
		
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
		accept.addActionListener(this);
		cancel.addActionListener(this);
		map.addMouseListener(this);
		map.addMouseMotionListener(this);
		
		selecting = false;
	}
	
	/**
	 * add mountains to the specified area
	 */
	private void orogeny(double x0, double y0, double x1, double y1, double altitude, double diameter) {
		System.out.println("create mountains from <" + x0 + "," + y0 + "> to <" + x1 + "," + y1 + "> w/A=" + altitude + ", D=" + diameter);
	}
	
	private void redraw() {
		double x_mid = parms.x_extent/2;
		double y_mid = parms.y_extent/2;
		int width = map.getWidth();
		int height = map.getHeight();
		
		double X0 = x_start/width - x_mid;
		double Y0 = y_start/height - y_mid;
		double X1 = x_end/width - x_mid;
		double Y1 = y_end/width - y_mid;
		double A = altitude.getValue()/10000;
		double D = diameter.getValue()/1000;
		orogeny(X0, Y0, X1, Y1, A, D);
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
		if (selecting) {
			x_end = e.getX();
			y_end = e.getY();
			selecting = false;
			redraw();
		}
		
	}
	
	/**
	 * progress in region selection
	 */
	public void mouseDragged(MouseEvent e) {
		if (selecting) {
			map.select(x_start,  y_start,  e.getX()-x_start,  e.getY()-y_start, Map.SEL_LINEAR);
		}	
	}
	
	/**
	 * Window Close event handler ... implicit CANCEL
	 */
	public void windowClosing(WindowEvent e) {
		System.out.println("Closing Slope Dialog");
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
		if (e.getSource() == altitude || e.getSource() == diameter) {
				redraw();
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