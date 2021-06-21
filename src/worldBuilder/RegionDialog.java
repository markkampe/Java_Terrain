package worldBuilder;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Dialog to enable the creation a consistent map of a sub-region of the current world.
 */
public class RegionDialog extends JFrame implements ActionListener, MapListener, WindowListener {	
	private Map map;
	private MapWindow window;
	private Parameters parms;
	
	private JLabel sel_center;
	private JLabel sel_km;
	private JTextField sel_name;
	private JButton accept;
	private JButton cancel;
	private JComboBox<Integer> pointsChooser;
	private JComboBox<Integer> improveChooser;
	
	private double x_km, y_km;		// selection width/height (in km)
	private double lat, lon;		// center of selected region
	private boolean selected;		// a region has been selected
	private double x0, y0;			// (map) <x,y> of upper left corner
	private double width, height;	// (map) size of selected region
	
	// mesh points per sub-region
	private static final int DEFAULT_POINTS = 1024;
	
	private static final long serialVersionUID = 1L;
	
	/**
	 * instantiate the widgets and register the listeners
	 */
	public RegionDialog(Map map)  {
		// pick up references
		this.map = map;
		this.window = map.window;
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
		JLabel nameLabel = new JLabel("Name of new map", JLabel.CENTER);
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
		window.addMapListener(this);
		
		window.selectMode(MapWindow.Selection.SQUARE);
		selected = window.checkSelection(MapWindow.Selection.SQUARE);
	}

	/**
	 * called whenever a region selection changes
	 * @param mx0	left most point (map coordinate)
	 * @param my0	upper most point (map coordinate)
	 * @param dx	width (in map units)
	 * @param dy	height (in map units)
	 * @param complete	boolean, has selection completed
	 * 
	 * @return	boolean	(should selection continue)
	 */
	public boolean regionSelected(double mx0, double my0, 
								  double dx, double dy, boolean complete) {
		// if he's done square the area
		if (complete)
			if (dx > dy)
				dy = dx;
			else
				dx = dy;
		
		// describe the selected area
		x0 = mx0;
		y0 = my0;
		width = dx;
		height = dy;
		x_km = parms.km(dx);
		y_km = parms.km(dy);
		lat = parms.latitude(my0 + dy/2);
		lon = parms.longitude(mx0 + dx/2);
		sel_center.setText(String.format("<%.6f, %.6f>", lat, lon));
		sel_km.setText(String.format("%.1fx%.1f (%s)", x_km, y_km, Parameters.unit_xy));
		
		selected = complete;
		return true;
	}

	/**
	 * Window Close event handler ... implicit CANCEL
	 */
	public void windowClosing(WindowEvent e) {
		window.selectMode(MapWindow.Selection.ANY);
		window.removeMapListener(this);
		this.dispose();
	}

	/**
	 * click events on ACCEPT/CANCEL buttons
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == accept && selected) {
			// create the new sub-region
			SubRegion s = new SubRegion((int) pointsChooser.getSelectedItem());
			s.newMap(map, x0, y0, width, height);
			
			// update the world location and size
			parms.xy_range = (int) ((x_km >= y_km) ? x_km : y_km);
			parms.latitude = lat;
			parms.longitude = lon;
			parms.parent_name = parms.map_name;
			parms.map_name = sel_name.getText();
			parms.checkDefaults();	// make sure defaults are consistent w/new world size
			
			// create a new map for the chosen subset
			int points = (int) pointsChooser.getSelectedItem();
			if (parms.debug_level > 0) {
				System.out.println("Expand " + (int) x_km + Parameters.unit_xy +
						" sub-region around <" + 
						String.format("%.6f", lat) + "," + String.format("%.6f", lon) + 
						"> to new " + points + " point mesh");
				parms.worldParms();
				map.region_stats();
			}
		}
		
		// clear the selection
		window.selectMode(MapWindow.Selection.ANY);
		
		// discard the dialog
		this.dispose();
	}

	/** (perfunctory) */ public boolean groupSelected(boolean[] selected, boolean complete) { return false; }
	/** (perfunctory) */ public boolean pointSelected(double x, double y) {return false;}
	/** (perfunctory) */ public void windowActivated(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowClosed(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowDeactivated(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowDeiconified(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowIconified(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowOpened(WindowEvent arg0) {}
}
