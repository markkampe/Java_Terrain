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
	
	private JLabel sel_center;
	private JLabel sel_km;
	private JTextField sel_name;
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
		// Font fontSmall = new Font("Serif", Font.ITALIC, 10);
	
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
		sel_center = new JLabel("Select the area for the new region");

		/*
		 * Pack them into:
		 * 		a vertical Box layout containing descriptions, sliders and buttons
		 * 		descriptions are a 1x3 layout of Labels
		 * 		sliders are a 1x2 grid layout
		 * 			each being a vertical Box w/label and slider
		 * 		buttons a horizontal Box layout
		 */
		
		JPanel descPanel = new JPanel(new GridLayout(2,2));
		descPanel.setBorder(BorderFactory.createEmptyBorder(20,10,20,10));
		descPanel.add(new JLabel("Region Center <lat,lon>"));
		descPanel.add(sel_center);
		descPanel.add(new JLabel("Region Size"));
		descPanel.add(sel_km);
		
		sel_name = new JTextField();
		sel_name.setText(String.format("MAP%03d", MapIndex.nextID()));
		JLabel nameLabel = new JLabel("Name of new sub-region", JLabel.CENTER);
		nameLabel.setFont(fontLarge);
		JPanel namePanel = new JPanel(new GridLayout(2,1));
		namePanel.add(nameLabel);
		namePanel.add(sel_name);
		namePanel.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
		
		// create the basic widgets
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
		controls.add(buttons);

		mainPane.add(descPanel, BorderLayout.NORTH);
		mainPane.add(namePanel, BorderLayout.CENTER);
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
		// square the selection area
		int width = Math.abs(x1 - x0);
		int height = Math.abs(y1 - y0);
		if (width > height)
			if (y1 > y0)
				y1 = y0 + width;
			else
				y1 = y0 - width;
		else
			if (x1 > x0)
				x1 = x0 + height;
			else
				x1 = x0 - height;
		
		// selected area in map coordinates
		double mx0 = map.map_x(x0);
		double mx1 = map.map_x(x1);
		double dx = mx1 - mx0;
		if (dx < 0) {
			mx0 = mx1;
			dx *= -1;
		}
		double my0 = map.map_y(y0);	
		double my1 = map.map_y(y1);
		double dy = my1 - my0;
		if (dy < 0) {
			my0 = -my1;
			dy *= -1;
		}
		
		// update the selection display
		map.selectRect(x0, y0, x1-x0, y1-y0);
		
		// find selected area location and size
		x_km = parms.km(dx);
		y_km = parms.km(dy);
		lat = parms.latitude((my0+my1)/2);
		lon = parms.longitude((mx1+mx0)/2);

		sel_center.setText(String.format("<%.6f, %.6f>", lat, lon));
		sel_km.setText(String.format("%.1fx%.1f (%s)", x_km, y_km, Parameters.unit_xy));
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
			x_end = e.getX();
			y_end = e.getY();
			select(x_start, y_start, x_end, y_end);
			
			// and display the selected region
			selecting = false;
			selected = true;
		}
	}
	
	/**
	 * progress in region selection
	 */
	public void mouseDragged(MouseEvent e) {
		if (selecting) {
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
		if (e.getSource() == accept && selected) {
			// update the world location and size
			map.isSubRegion = false;		// leave location editing enabled	
			parms.xy_range = (int) ((x_km >= y_km) ? x_km : y_km);
			parms.latitude = lat;
			parms.longitude = lon;
			parms.parent_name = parms.map_name;
			parms.map_name = sel_name.getText();
			
			// create a new map for the chosen subset
			int points = (int) pointsChooser.getSelectedItem();
			if (parms.debug_level > 0)
				System.out.println("Expand " + (int) x_km + Parameters.unit_xy +
						" sub-region around <" + 
						String.format("%.6f", lat) + "," + String.format("%.6f", lon) + 
						"> to new " + points + " point mesh");
			map.subregion(points);
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