package worldBuilder;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * SlopeDialog allows the user to choose an axis and inclination to
 * cause a uniform slope to the entire map.  
 */
public class RegionDialog extends JFrame implements ActionListener, MouseListener, MouseMotionListener, WindowListener {	
	private Map map;
	private Parameters parms;
	
	private JTextField sel_name;
	private JLabel sel_center;
	private JLabel sel_km;
	private JButton accept;
	private JButton cancel;
	private JComboBox<Integer> pointsChooser;
	private JComboBox<Integer> improveChooser;
	
	private boolean selecting;		// selection in progress
	private boolean selected;		// selection completed
	private int x_start, x_end, y_start, y_end;		// selection screen coordinates
	private double x_km, y_km;		// selection width/height (in km)
	private double lat, lon;		// center of selected region
	
	// mesh points per sub-region
	private static final int DEFAULT_POINTS = 1024;
	
	private static final long serialVersionUID = 1L;
	
	public RegionDialog(Map map)  {
		// pick up references
		this.map = map;
		this.parms = Parameters.getInstance();
		
		// create the dialog box
		Container mainPane = getContentPane();
		int border = parms.dialogBorder;
		((JComponent) mainPane).setBorder(BorderFactory.createMatteBorder(border, border, border, border, Color.LIGHT_GRAY));
		setTitle("Create Sub-Region");
		addWindowListener( this );
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		
		// create the basic widgets
		// Font fontSmall = new Font("Serif", Font.ITALIC, 10);
		Font fontLarge = new Font("Serif", Font.ITALIC, 15);
		sel_name = new JTextField();
		sel_name.setText("Happyville");
		JLabel nameLabel = new JLabel("Name of this region", JLabel.CENTER);
		nameLabel.setFont(fontLarge);
		
		pointsChooser = new JComboBox<Integer>();
		JLabel pointsLabel = new JLabel("Points", JLabel.CENTER);
		pointsLabel.setFont(fontLarge);
		for(int i = 256; i < 4096; i *= 2) {
			pointsChooser.addItem(i);
		}
		pointsChooser.setSelectedItem(DEFAULT_POINTS);
		improveChooser = new JComboBox<Integer>();
		JLabel improveLabel = new JLabel("Improvements", JLabel.CENTER);
		improveLabel.setFont(fontLarge);
		for(int i = 0; i <= 4; i++) {
			improveChooser.addItem(i);
		}
		improveChooser.setSelectedItem(parms.improvements);
		
		accept = new JButton("CREATE");
		cancel = new JButton("CANCEL");
		
		sel_km = new JLabel();
		sel_center = new JLabel("Select the area to be exported");

		/*
		 * Pack them into:
		 * 		a name (1x2 grid) name selection panel
		 * 		a vertical Box layout containing descriptions, sliders and buttons
		 * 		descriptions are a 1x3 layout of Labels
		 * 		sliders are a 1x2 grid layout
		 * 			each being a vertical Box w/label and slider
		 * 		buttons a horizontal Box layout
		 */
		JPanel namePanel = new JPanel(new GridLayout(2,1));
		namePanel.add(nameLabel);
		namePanel.add(sel_name);
		namePanel.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
		
		JPanel descPanel = new JPanel(new GridLayout(2,2));
		descPanel.setBorder(BorderFactory.createEmptyBorder(20,10,20,10));
		descPanel.add(new JLabel("Center"));
		descPanel.add(sel_center);
		descPanel.add(new JLabel("km"));
		descPanel.add(sel_km);
		
		JPanel p_panel = new JPanel();
		p_panel.setLayout(new BoxLayout(p_panel, BoxLayout.PAGE_AXIS));
		p_panel.add(pointsLabel);
		p_panel.add(pointsChooser);
		
		JPanel i_panel = new JPanel();
		i_panel.setLayout(new BoxLayout(i_panel, BoxLayout.PAGE_AXIS));
		i_panel.add(improveLabel);
		i_panel.add(improveChooser);
		
		JPanel buttons = new JPanel();
		buttons.setLayout(new BoxLayout(buttons, BoxLayout.LINE_AXIS));
		buttons.add(p_panel);
		buttons.add(Box.createRigidArea(new Dimension(40,0)));
		buttons.add(i_panel);
		buttons.add(Box.createRigidArea(new Dimension(40,0)));
		buttons.add(cancel);
		buttons.add(Box.createRigidArea(new Dimension(40,0)));
		buttons.add(accept);
		buttons.setBorder(BorderFactory.createEmptyBorder(20,100, 20, 10));
		
		JPanel controls = new JPanel();
		controls.setLayout(new BoxLayout(controls, BoxLayout.PAGE_AXIS));
		controls.add(namePanel);
		controls.add(buttons);

		mainPane.add(descPanel, BorderLayout.NORTH);
		mainPane.add(controls, BorderLayout.SOUTH);
		
		pack();
		setVisible(true);
		
		// add the action listeners
		accept.addActionListener(this);
		cancel.addActionListener(this);
		map.addMouseListener(this);
		map.addMouseMotionListener(this);
		
		selecting = false;
		selected = false;
	}

	/**
	 * describe the selected area
	 */
	private void select(int x0, int y0, int x1, int y1) {
		// selected area in map coordinates
		double X0 = map.map_x(x0);
		double X1 = map.map_x(x1);
		double dx = X1 - X0;
		if (dx < 0) {
			X0 = X1;
			dx *= -1;
		}
		double Y0 = map.map_y(y0);	
		double Y1 = map.map_y(y1);
		double dy = Y1 - Y0;
		if (dy < 0) {
			Y0 = -Y1;
			dy *= -1;
		}
	
		// find selected area location and size
		x_km = parms.km(dx);
		y_km = parms.km(dy);
		lat = parms.latitude((Y0+Y1)/2);
		lon = parms.longitude((X1+X0)/2);

		sel_center.setText(String.format("%.6f, %.6f", lat, lon));
		sel_km.setText(String.format("%.1fx%.1f", x_km, y_km));
	}
	
	/**
	 * create a new region for selected area at selected resolution
	 */
	private void newRegion(String filename) {
		int points = (int) pointsChooser.getSelectedItem();
		if (parms.debug_level > 0)
			System.out.println("Expand sub-region " +
					"around <" + String.format("%.6f", lat) + "," + String.format("%.6f", lon) + 
					"> to new " + points + " point mesh in "+ filename);

		System.out.println("Sub-Region Creation not yet implemented");
		
		/* FIX define new world parameters	*/
		/* FIX create new mesh with only contained points	*/
		/* FIX add new points (w/interpolated values) to mesh	*/
		/* FIX recreate map for new mesh	*/
		
		map.isSubRegion = true;
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
	 * finish defining a export region
	 */
	public void mouseReleased(MouseEvent e) {
		if (selecting) {
			if (e.getX() >= x_start)
				x_end = e.getX();
			else {
				x_end = x_start;
				x_start = e.getX();
			}
			if (e.getY() >= y_start)
				y_end = e.getY();
			else {
				y_end = y_start;
				y_start = e.getY();
			}

			selecting = false;
			selected = true;
			select(x_start, y_start, x_end, y_end);
		}
	}
	
	/**
	 * progress in region selection
	 */
	public void mouseDragged(MouseEvent e) {
		if (selecting) {
			map.selectRect(x_start, y_start, e.getX()-x_start, e.getY()-y_start);
			select(x_start, y_start, e.getX(), e.getY());
		}	
	}
	
	/**
	 * Window Close event handler ... implicit CANCEL
	 */
	public void windowClosing(WindowEvent e) {
		map.selectNone();
		this.dispose();
		map.removeMouseListener(this);
		map.removeMouseMotionListener(this);
	}

	/**
	 * click events on ACCEPT/CANCEL buttons
	 */
	public void actionPerformed(ActionEvent e) {
		// if this was an acceptance ...
		if (e.getSource() == accept && selected) {
			FileDialog d = new FileDialog(this, "New Sub-Region", FileDialog.SAVE);
			d.setFile(sel_name.getText()+".json");
			d.setVisible(true);
			String export_file = d.getFile();
			if (export_file != null) {
				String dir = d.getDirectory();
				if (dir != null)
					export_file = dir + export_file;
				newRegion(export_file);
			}
		}
		
		// clear the selection
		map.selectNone();
		
		// discard the dialog
		this.dispose();
		map.removeMouseListener(this);
		map.removeMouseMotionListener(this);
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