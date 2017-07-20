package worldBuilder;

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
	private boolean selected;		// selection completed
	private int x_start, x_end, y_start, y_end;		// selection start/end coordinates
	private int width;				// width of mountain range
	
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
		this.d_max = parms.xy_range/5;
	
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
		
		altitude = new JSlider(JSlider.HORIZONTAL, -this.a_max, this.a_max, 0);
		altitude.setMajorTickSpacing(niceTic(2*a_max,1));
		altitude.setMinorTickSpacing(niceTic(2*a_max,2));
		altitude.setFont(fontSmall);
		altitude.setPaintTicks(true);
		altitude.setPaintLabels(true);
		JLabel altitudeLabel = new JLabel("Altitude(m)", JLabel.CENTER);
		altitudeLabel.setFont(fontLarge);

		diameter = new JSlider(JSlider.HORIZONTAL, 0, d_max, d_max/5);
		diameter.setMajorTickSpacing(niceTic(d_max,1));
		diameter.setMinorTickSpacing(niceTic(d_max,2));
		diameter.setFont(fontSmall);
		diameter.setPaintTicks(true);
		diameter.setPaintLabels(true);
		JLabel diameterLabel = new JLabel("Diameter(km)", JLabel.CENTER);
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
		selected = false;
	}
	
	/**
	 * figure out an attractive JSlider tic spacing
	 */
	int niceTic(int full_scale, int level) {
		int mult = 1;
		while((full_scale/mult) % 10 == 0)
			mult *= 10;
		if (full_scale % 5 == 0)
			mult *= 5;
		else if (full_scale %3 == 0)
			mult *= 3;
		else if (full_scale %2 == 0)
			mult *= 2;
		if (level == 1)
			return mult;
		
		// second level
		if (mult % 5 == 0)
			return mult/5;
		else if (mult % 4 == 0)
			return (mult/4);
		else if (mult % 3 == 0)
			return (mult/3);
		else if (mult % 2 == 0)
			return (mult/2);
		else
			return mult;
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
		
		double X0 = (double) x_start/width - x_mid;
		double Y0 = (double) y_start/height - y_mid;
		double X1 = (double) x_end/width - x_mid;
		double Y1 = (double)y_end/width - y_mid;
		double A = (double) altitude.getValue()/(2*a_max);
		double D = (double) diameter.getValue()/parms.xy_range;
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
			selected = true;
			redraw();
		}	
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
	 * Window Close event handler ... implicit CANCEL
	 */
	public void windowClosing(WindowEvent e) {
		System.out.println("Closing Slope Dialog");
		map.selectNone();
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
		if (selected && (e.getSource() == altitude || e.getSource() == diameter)) {
				redraw();
		} 
	}

	/**
	 * click events on ACCEPT/CANCEL buttons
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == cancel) {
			map.selectNone();
			map.setMesh(oldMesh);
			map.repaint();
			oldMesh = null;
			this.dispose();
		} else if (e.getSource() == accept) {
			map.selectNone();
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