package worldBuilder;

import java.awt.*;
import java.awt.event.*;
import java.util.Hashtable;

import javax.swing.*;
import javax.swing.event.*;

/**
 * Dialog to create mountains and valleys.
 */
public class MountainDialog extends JFrame implements ActionListener, ChangeListener, MapListener, ItemListener, KeyListener, WindowListener {	
	
	private Map map;
	TerrainEngine te;
	private Parameters parms;
	
	private JCheckBox symmetric;
	private JComboBox<String> form;
	private JSlider altitude;
	private JSlider diameter1;
	private JSlider diameter2;
	private JSlider rounding1;
	private JSlider rounding2;
	private JButton accept;
	private JButton cancel;
	
	private boolean selected;		// selection completed
	private double x_start, x_end, y_start, y_end;		// selection start/end coordinates
	
	private int d_max;				// diameter: full scale
	private int a_max;				// altitude: full scale
	
	/**
	 * a LandForm is a macro for a collection of (size, shape) mountain parameters
	 */
	private static class LandForm {
		public String name;
		public int altitude;
		public int width;
		public int shape;
		
		private LandForm(String name, int shape, int width, int altitude) {
			this.name = name;
			this.altitude = altitude;
			this.width = width;
			this.shape = shape;
		}
	};
	
	// this is the list of known landforms
	private static final LandForm landforms[] = {
		//           name		shp	dia	altitude
		new LandForm("volcano",	0,	40,	1500),
		new LandForm("plateau",	8,	40,	500),
		new LandForm("caldera",	4,	20,	-250),
		new LandForm("canyon",	7,	1,	-300)
	};
	
	private static final long serialVersionUID = 1L;
	
	/**
	 * instantiate the widgets and register the listeners
	 */
	public MountainDialog(Map map)  {
		// pick up references
		this.map = map;
		this.parms = Parameters.getInstance();

		// calibrate full scale on the sliders
		this.a_max = parms.z_range/2;
		this.d_max = parms.xy_range / parms.m_width_divisor;
	
		// create the dialog box
		Container mainPane = getContentPane();
		int border = parms.dialogBorder;
		((JComponent) mainPane).setBorder(BorderFactory.createMatteBorder(border, border, border, border, Color.LIGHT_GRAY));
		setTitle("Add Mountain(s)");
		addWindowListener( this );
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		
		// create the basic widgets
		Font fontSmall = new Font("Serif", Font.ITALIC, 10);
		Font fontLarge = new Font("Serif", Font.ITALIC, 15);
		accept = new JButton("ACCEPT (Enter)");
		cancel = new JButton("CANCEL (Esc)");
		
		symmetric = new JCheckBox("Symmetric");
		symmetric.setFont(fontLarge);
		symmetric.setSelected(true);
		
		form = new JComboBox<String>();
		JLabel formLabel = new JLabel("Land Form", JLabel.CENTER);
		formLabel.setFont(fontLarge);
		for(int i = 0; i < landforms.length; i++) {
			form.addItem(landforms[i].name);
		}
		form.setSelectedIndex(0);
		
		altitude = new JSlider(JSlider.HORIZONTAL, -this.a_max, this.a_max, parms.dAltitude);
		altitude.setMajorTickSpacing(Parameters.niceTics(-a_max, a_max, true));
		if (a_max > 10)
			altitude.setMinorTickSpacing(Parameters.niceTics(-a_max, a_max, false));
		altitude.setFont(fontSmall);
		altitude.setPaintTicks(true);
		altitude.setPaintLabels(true);
		JLabel altitudeLabel = new JLabel("Altitude(m)", JLabel.CENTER);
		altitudeLabel.setFont(fontLarge);

		diameter1 = new JSlider(JSlider.HORIZONTAL, 0, d_max, parms.dDiameter);
		diameter1.setMajorTickSpacing(Parameters.niceTics(0, d_max,true));
		if (d_max > 10)
			diameter1.setMinorTickSpacing(Parameters.niceTics(0, d_max,false));
		diameter1.setFont(fontSmall);
		diameter1.setPaintTicks(true);
		diameter1.setPaintLabels(true);
		JLabel diameter1Label = new JLabel("Top/Right Width (km)", JLabel.CENTER);
		diameter1Label.setFont(fontLarge);
		
		diameter2 = new JSlider(JSlider.HORIZONTAL, 0, d_max, parms.dDiameter);
		diameter2.setMajorTickSpacing(Parameters.niceTics(0, d_max,true));
		if (d_max > 10)
			diameter2.setMinorTickSpacing(Parameters.niceTics(0, d_max,false));
		diameter2.setFont(fontSmall);
		diameter2.setPaintTicks(true);
		diameter2.setPaintLabels(true);
		JLabel diameter2Label = new JLabel("Bottom/Left Width (km)", JLabel.CENTER);
		diameter2Label.setFont(fontLarge);
		
		rounding1 = new JSlider(JSlider.HORIZONTAL, Parameters.CONICAL, Parameters.CYLINDRICAL, parms.dShape);
		rounding1.setMajorTickSpacing(4);
		rounding1.setMinorTickSpacing(1);
		rounding1.setFont(fontSmall);
		rounding1.setPaintTicks(true);
		JLabel roundLabel1 = new JLabel("Profile (top/right)", JLabel.CENTER);
		roundLabel1.setFont(fontLarge);
		rounding2 = new JSlider(JSlider.HORIZONTAL, Parameters.CONICAL, Parameters.CYLINDRICAL, parms.dShape);
		rounding2.setMajorTickSpacing(4);
		rounding2.setMinorTickSpacing(1);
		rounding2.setFont(fontSmall);
		rounding2.setPaintTicks(true);
		JLabel roundLabel2 = new JLabel("Profile (bottom/left)", JLabel.CENTER);
		roundLabel2.setFont(fontLarge);
		
		Hashtable<Integer, JLabel> labels = new Hashtable<Integer, JLabel>();
		labels.put(Parameters.CONICAL, new JLabel("cone"));
		labels.put(Parameters.SPHERICAL, new JLabel("round"));
		labels.put(Parameters.CYLINDRICAL, new JLabel("flat"));
		rounding1.setLabelTable(labels);
		rounding1.setPaintLabels(true);
		rounding2.setLabelTable(labels);
		rounding2.setPaintLabels(true);
		
		/*
		 * Then pack all the controls into a 3x3 grid
		 */
		JPanel p0 = new JPanel();
		
		JPanel formPanel = new JPanel();
		formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.PAGE_AXIS));
		formPanel.add(formLabel);
		formPanel.add(form);
		formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 15));
		p0.add(formPanel);
		p0.add(symmetric);
		
		JPanel altPanel = new JPanel();
		altPanel.setLayout(new BoxLayout(altPanel, BoxLayout.PAGE_AXIS));
		altPanel.add(altitudeLabel);
		altPanel.add(altitude);
		
		JPanel diaPanel1 = new JPanel();
		diaPanel1.setLayout(new BoxLayout(diaPanel1, BoxLayout.PAGE_AXIS));
		diaPanel1.add(diameter1Label);
		diaPanel1.add(diameter1);
		
		JPanel diaPanel2 = new JPanel();
		diaPanel2.setLayout(new BoxLayout(diaPanel2, BoxLayout.PAGE_AXIS));
		diaPanel2.add(diameter2Label);
		diaPanel2.add(diameter2);
		
		JPanel rndPanel1 = new JPanel();
		rndPanel1.setLayout(new BoxLayout(rndPanel1, BoxLayout.PAGE_AXIS));
		rndPanel1.add(roundLabel1);
		rndPanel1.add(rounding1);
		
		JPanel rndPanel2 = new JPanel();
		rndPanel2.setLayout(new BoxLayout(rndPanel2, BoxLayout.PAGE_AXIS));
		rndPanel2.add(roundLabel2);
		rndPanel2.add(rounding2);
		
		JPanel p7 = new JPanel();
		p7.add(cancel);
		
		JPanel p8 = new JPanel();
		p8.add(accept);

		JPanel controls = new JPanel();
		controls.setLayout(new GridLayout(3,3));
		p0.setBorder(BorderFactory.createEmptyBorder(20, 15, 20, 15));
		altPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 15));
		diaPanel1.setBorder(BorderFactory.createEmptyBorder(10, 15, 0, 15));
		diaPanel2.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
		rndPanel1.setBorder(BorderFactory.createEmptyBorder(10, 15, 0, 10));
		rndPanel2.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 10));
		formPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
		p7.setBorder(BorderFactory.createEmptyBorder(20, 15, 20, 15));
		p8.setBorder(BorderFactory.createEmptyBorder(20, 15, 20, 15));
		controls.add(p0);
		controls.add(diaPanel1);
		controls.add(rndPanel1);
		controls.add(altPanel);
		controls.add(diaPanel2);
		controls.add(rndPanel2);
		controls.add(new JPanel());	// empty space
		controls.add(p7);
		controls.add(p8);
		
		mainPane.add(controls);
		pack();
		setVisible(true);
		
		// add the other widget action listeners
		altitude.addChangeListener(this);
		diameter1.addChangeListener(this);
		diameter2.addChangeListener(this);
		rounding1.addChangeListener(this);
		rounding2.addChangeListener(this);
		accept.addActionListener(this);
		cancel.addActionListener(this);
		map.addMapListener(this);
		map.addKeyListener(this);
		addKeyListener(this);
		symmetric.addItemListener(this);
		form.addActionListener(this);
		map.requestFocus();
		
		// set us up for line selection
		map.selectMode(Map.Selection.LINE);
		selected = map.checkSelection(Map.Selection.LINE);
		
		// instantiate a TerrainEngine
		te = new TerrainEngine(map);
	}

	
	/**
	 * compute the delta_h associated with this mountain range
	 * 	1. reset the height map to its initial value
	 * 	2. figure out how long the mountain range is
	 * 	3. figure out how many mountains it contains
	 *  4. place the mountains along the range
	 *  5. map.repaint
	 */
	private void redraw() {
		// turn the diameter into map units
		double d1 = (double) diameter1.getValue();
		if (d1 == 0)
			d1 = 1;
		d1 = parms.x(d1);
		double d2 = (double) diameter2.getValue();
		if (d2 == 0)
			d2 = 1;
		d2 = parms.x(d2);
		
		// get the height
		int alt = altitude.getValue();
		double z = parms.z((double) alt);
		
		// get the shape
		int shape1 = rounding1.getValue();
		int shape2 = rounding2.getValue();
		
		// tell the TerrainEngine to make it so
		if (symmetric.isSelected())
			te.ridge(x_start, y_start, x_end, y_end, z, d1/2, shape1);
		else
			te.ridge(x_start, y_start, x_end, y_end, z, d1/2, d2/2, shape1, shape2);
	}
	
	/**
	 * called whenever a (Map) region selection changes
	 * @param map_x		left most point (map coordinate)
	 * @param map_y		upper most point (map coordinate)
	 * @param width		(in map units, can be negative)
	 * @param height	(in map units, can be negative)
	 * @param complete	boolean, has selection completed
	 * 
	 * @return	boolean	(should selection continue)
	 */
	public boolean regionSelected(
			double map_x, double map_y, 
			double width, double height,
			boolean complete) {
		x_start = map_x;
		y_start = map_y;
		x_end = map_x + width;
		y_end = map_y + height;
		selected = complete;
		redraw();
		map.requestFocus();
		return true;
	}

	/**
	 * restore previous height map and exit dialog
	 */
	private void cancelDialog() {
		map.selectMode(Map.Selection.NONE);
		te.abort();
		
		map.removeMapListener(this);
		map.removeKeyListener(this);
		map.selectMode(Map.Selection.ANY);
		this.dispose();
		WorldBuilder.activeDialog = false;
	}
	
	/**
	 * make the most recently created mountain official
	 */
	private void acceptMountain() {
		// save the current values as defaults
		parms.dDiameter = diameter1.getValue();
		parms.dAltitude = altitude.getValue();
		parms.dShape = rounding1.getValue();
		
		// commit the heightMap updates
		te.commit();
		
		// clean up the selection graphics
		map.selectMode(Map.Selection.NONE);
		x_start = 0; y_start = 0; x_end = 0; y_end = 0;
		map.selectMode(Map.Selection.LINE);
		selected = false;
	}
				
	/**
	 * Window Close event handler ... implicit CANCEL
	 */
	public void windowClosing(WindowEvent e) {
		cancelDialog();
	}
	
	/**
	 * updates to the diameter/rounding sliders
	 */
	public void stateChanged(ChangeEvent e) {
			// if symmetric, changing one side changes both
			if (symmetric.isSelected()) {
				if (e.getSource() == rounding1)
					rounding2.setValue(rounding1.getValue());
				if (e.getSource() == rounding2)
					rounding1.setValue(rounding2.getValue());
				if (e.getSource() == diameter1)
					diameter2.setValue(diameter1.getValue());
				if (e.getSource() == diameter2)
					diameter1.setValue(diameter2.getValue());
			}
			
			if (selected)
				redraw();
			map.requestFocus();
	}
	
	/**
	 * look for ENTER or ESC
	 */
	public void keyTyped(KeyEvent e) {
		int key = e.getKeyChar();
		if (key == KeyEvent.VK_ENTER && selected)
			acceptMountain();
		else if (key == KeyEvent.VK_ESCAPE) {
			// cancel the last updates
			te.abort();	
			selected = false;
			map.selectMode(Map.Selection.NONE);
			map.selectMode(Map.Selection.LINE);
		}
	}

	/**
	 * click events on ACCEPT/CANCEL buttons or form selector
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == cancel) {
			cancelDialog();
		} else if (e.getSource() == accept && selected) {
			acceptMountain();
			map.requestFocus();
		} else if (e.getSource() == form) {
			int x = form.getSelectedIndex();
			altitude.setValue(landforms[x].altitude);
			rounding1.setValue(landforms[x].shape);
			rounding2.setValue(landforms[x].shape);
			diameter1.setValue(landforms[x].width);
			diameter1.setValue(landforms[x].width);
			if (selected)
				redraw();
			map.requestFocus();
		}
	}
	
	/**
	 * symmetric checkbox has changed state
	 * 
	 * @param e
	 */
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() == symmetric) {
			if (symmetric.isSelected()) {
				diameter2.setValue(diameter1.getValue());
				rounding2.setValue(rounding1.getValue());
				
				if (selected)
					redraw();
			}
		} 
		map.requestFocus();
	}
	
	/** (perfunctory) */ public boolean pointSelected(double map_x, double map_y) { return false; }
	/** (perfunctory) */ public boolean groupSelected(boolean[] selected, boolean complete) { return false; }
	/** (perfunctory) */ public void windowActivated(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowClosed(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowDeactivated(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowDeiconified(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowIconified(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowOpened(WindowEvent arg0) {}
	/** (perfunctory) */ public void keyPressed(KeyEvent arg0) {}
	/** (perfunctory) */ public void keyReleased(KeyEvent arg0) {}
}
